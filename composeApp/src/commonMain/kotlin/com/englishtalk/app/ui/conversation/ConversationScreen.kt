package com.englishtalk.app.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.englishtalk.app.ai.SessionSummary
import com.englishtalk.app.data.Scenario
import com.englishtalk.app.ui.AppContainer
import com.englishtalk.app.ui.LocalServices
import com.englishtalk.app.ui.components.AppTopBar
import com.englishtalk.app.ui.components.EmojiButton
import com.englishtalk.app.ui.components.ErrorBanner
import com.englishtalk.app.ui.components.LoadingRow
import com.englishtalk.app.ui.components.MicButton
import com.englishtalk.app.ui.components.ScoreCircle
import com.englishtalk.app.ui.components.SectionTitle
import com.englishtalk.app.ui.components.rememberSpeechCapture
import com.englishtalk.app.ui.theme.Good

@Composable
fun ConversationScreen(
    container: AppContainer,
    scenario: Scenario,
    customTopic: String?,
    onBack: () -> Unit,
) {
    val tts = LocalServices.current.textToSpeech
    val prefs by container.repository.preferences.collectAsState()
    val scope = rememberCoroutineScope()
    val vm = remember {
        ConversationViewModel(container, scenario, customTopic, scope) { reply ->
            val p = container.repository.preferences.value
            if (p.autoPlayReplies) tts.speak(reply, p.accent, p.speechRate)
        }
    }
    val speech = rememberSpeechCapture()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.start() }
    LaunchedEffect(vm.items.size, vm.isLoading) {
        val count = vm.items.size + if (vm.isLoading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        AppTopBar(
            title = "${scenario.emoji} ${scenario.title}",
            onBack = { tts.stop(); onBack() },
            actions = { TextButton(onClick = { tts.stop(); vm.finish() }) { Text("Kết thúc") } },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(vm.items, key = { it.id }) { item ->
                when (item) {
                    is ChatItem.Tutor -> TutorBubble(
                        item = item,
                        showTranslation = prefs.showTranslation,
                        onSpeak = { tts.speak(item.text, prefs.accent, prefs.speechRate) },
                        onSpeakSlow = { tts.speak(item.text, prefs.accent, prefs.speechRate * 0.6f) },
                    )
                    is ChatItem.Learner -> LearnerBubble(
                        item = item,
                        onSpeak = { text -> tts.speak(text, prefs.accent, prefs.speechRate) },
                    )
                }
            }
            if (vm.isLoading) item { LoadingRow("Gia sư đang trả lời…") }
        }

        vm.error?.let { ErrorBanner(it, "Thử lại") { vm.retry() } }
        speech.error?.let { ErrorBanner(it, "Đóng") { speech.clearError() } }
        if (vm.isSummarizing) LoadingRow("Đang tổng kết buổi học…")

        if (vm.suggestions.isNotEmpty() && !speech.isListening) {
            Text(
                "💡 Gợi ý câu trả lời (chạm để nghe):",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(vm.suggestions) { s ->
                    SuggestionChip(
                        onClick = {
                            input = s
                            tts.speak(s, prefs.accent, prefs.speechRate)
                        },
                        label = { Text(s, maxLines = 2) },
                    )
                }
            }
        }

        if (speech.isListening) {
            Text(
                speech.partialText.ifBlank { "Đang nghe… hãy nói bằng tiếng Anh" },
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập hoặc bấm mic để nói…") },
                maxLines = 3,
                shape = RoundedCornerShape(24.dp),
            )
            if (input.isNotBlank()) {
                TextButton(onClick = { vm.send(input); input = "" }, enabled = !vm.isLoading) { Text("Gửi") }
            }
            if (speech.isSupported) {
                MicButton(listening = speech.isListening, enabled = !vm.isLoading, size = 56.dp) {
                    speech.toggle(prefs.accent.languageTag) { text -> vm.send(text) }
                }
            }
        }
    }

    vm.summary?.let { summary ->
        SummaryDialog(summary = summary, onSpeak = { tts.speak(it, prefs.accent, prefs.speechRate) }) {
            vm.dismissSummary()
            onBack()
        }
    }
}

@Composable
private fun TutorBubble(
    item: ChatItem.Tutor,
    showTranslation: Boolean,
    onSpeak: () -> Unit,
    onSpeakSlow: () -> Unit,
) {
    var revealTranslation by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(item.text, style = MaterialTheme.typography.bodyLarge)
                if (showTranslation && item.translation.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    if (revealTranslation) {
                        Text(
                            item.translation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Xem bản dịch",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { revealTranslation = true },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EmojiButton("🔊", onClick = onSpeak)
                    EmojiButton("🐢", onClick = onSpeakSlow)
                }
            }
        }
    }
}

@Composable
private fun LearnerBubble(item: ChatItem.Learner, onSpeak: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(
                item.text,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(12.dp),
            )
        }
        val fb = item.feedback ?: return@Column
        Spacer(Modifier.height(4.dp))
        Surface(
            color = if (fb.hasIssue) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (fb.hasIssue) {
                    Text("✏️ Sửa lỗi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    if (fb.corrected.isNotBlank()) {
                        Text(fb.corrected, style = MaterialTheme.typography.bodyMedium, color = Good, fontWeight = FontWeight.SemiBold)
                    }
                    if (fb.explanation.isNotBlank()) Text(fb.explanation, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("✅ Câu của bạn đúng rồi!", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                if (fb.moreNatural.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "🌟 Người bản xứ nói: ${fb.moreNatural}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        TextButton(onClick = { onSpeak(fb.moreNatural) }) { Text("🔊") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryDialog(summary: SessionSummary, onSpeak: (String) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Tổng kết buổi học 🎉") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (summary.strengths.isEmpty() && summary.improvements.isEmpty()) {
                    Text("Hãy nói ít nhất một câu để nhận được nhận xét nhé!")
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ScoreCircle(summary.score) }
                    SectionTitle("👍 Điểm mạnh")
                    summary.strengths.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    SectionTitle("📈 Cần cải thiện")
                    summary.improvements.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    if (summary.usefulPhrases.isNotEmpty()) {
                        SectionTitle("💬 Cụm từ nên học")
                        summary.usefulPhrases.forEach { phrase ->
                            Text(
                                "🔊 $phrase",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSpeak(phrase) }
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Xong") } },
    )
}
