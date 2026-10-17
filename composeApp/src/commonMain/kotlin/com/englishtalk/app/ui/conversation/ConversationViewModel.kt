package com.englishtalk.app.ui.conversation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.englishtalk.app.ai.ApiMessage
import com.englishtalk.app.ai.SessionSummary
import com.englishtalk.app.ai.TurnFeedback
import com.englishtalk.app.data.Scenario
import com.englishtalk.app.ui.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface ChatItem {
    val id: Int

    data class Tutor(
        override val id: Int,
        val text: String,
        val translation: String,
    ) : ChatItem

    data class Learner(
        override val id: Int,
        val text: String,
        val feedback: TurnFeedback? = null,
    ) : ChatItem
}

class ConversationViewModel(
    private val container: AppContainer,
    private val scenario: Scenario,
    customTopic: String?,
    private val scope: CoroutineScope,
    /** Called with each new tutor reply so the screen can read it aloud. */
    private val onTutorReply: (String) -> Unit,
) {
    private val repository = container.repository
    private val systemPrompt = container.tutor.conversationSystemPrompt(
        repository.preferences.value.profile, scenario, customTopic,
    )

    /** Raw Messages API history; assistant turns are kept exactly as returned. */
    private val history = mutableListOf<ApiMessage>()
    private var nextId = 0

    val items = mutableStateListOf<ChatItem>()
    var suggestions by mutableStateOf<List<String>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var summary by mutableStateOf<SessionSummary?>(null)
        private set
    var isSummarizing by mutableStateOf(false)
        private set

    private var pendingRetry: (() -> Unit)? = null
    private var job: Job? = null

    fun start() {
        if (history.isNotEmpty() || isLoading) return
        repository.recordConversationStarted()
        requestTurn(ApiMessage.user("[START]"), learnerItemId = null)
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || isLoading) return
        val item = ChatItem.Learner(nextId++, clean)
        items.add(item)
        suggestions = emptyList()
        repository.recordSpokenTurn()
        requestTurn(ApiMessage.user(clean), learnerItemId = item.id)
    }

    private fun requestTurn(userMessage: ApiMessage, learnerItemId: Int?) {
        error = null
        isLoading = true
        history.add(userMessage)
        job = scope.launch {
            try {
                val (turn, assistant) = container.tutor.conversationTurn(
                    repository.preferences.value.aiConfig, systemPrompt, history,
                )
                history.add(assistant)
                if (learnerItemId != null) {
                    val index = items.indexOfFirst { it.id == learnerItemId }
                    if (index >= 0) items[index] = (items[index] as ChatItem.Learner).copy(feedback = turn.feedback)
                }
                items.add(ChatItem.Tutor(nextId++, turn.reply, turn.replyTranslation))
                suggestions = turn.suggestions
                onTutorReply(turn.reply)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Keep history strictly alternating: drop the unanswered user turn.
                history.removeAt(history.lastIndex)
                error = e.message ?: "Đã xảy ra lỗi."
                pendingRetry = { requestTurn(userMessage, learnerItemId) }
            } finally {
                isLoading = false
            }
        }
    }

    fun retry() {
        val r = pendingRetry ?: return
        pendingRetry = null
        r()
    }

    fun finish() {
        if (items.none { it is ChatItem.Learner }) {
            summary = SessionSummary()
            return
        }
        isSummarizing = true
        error = null
        scope.launch {
            try {
                summary = container.tutor.summarizeSession(
                    repository.preferences.value.aiConfig, systemPrompt, history,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                error = e.message
                pendingRetry = { finish() }
            } finally {
                isSummarizing = false
            }
        }
    }

    fun dismissSummary() {
        summary = null
    }
}
