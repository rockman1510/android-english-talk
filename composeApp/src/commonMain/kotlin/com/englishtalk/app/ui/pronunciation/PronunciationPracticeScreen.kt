package com.englishtalk.app.ui.pronunciation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishtalk.app.ai.PronunciationAnalysis
import com.englishtalk.app.data.SoundLesson
import com.englishtalk.app.scoring.PronunciationResult
import com.englishtalk.app.scoring.PronunciationScorer
import com.englishtalk.app.scoring.WordStatus
import com.englishtalk.app.ui.AppContainer
import com.englishtalk.app.ui.LocalServices
import com.englishtalk.app.ui.components.AppTopBar
import com.englishtalk.app.ui.components.EmojiButton
import com.englishtalk.app.ui.components.ErrorBanner
import com.englishtalk.app.ui.components.LoadingRow
import com.englishtalk.app.ui.components.MicButton
import com.englishtalk.app.ui.components.ScoreCircle
import com.englishtalk.app.ui.components.rememberSpeechCapture
import com.englishtalk.app.ui.theme.Bad
import com.englishtalk.app.ui.theme.Close
import com.englishtalk.app.ui.theme.Good
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PronunciationPracticeScreen(container: AppContainer, lesson: SoundLesson, onBack: () -> Unit) {
    val tts = LocalServices.current.textToSpeech
    val prefs by container.repository.preferences.collectAsState()
    val speech = rememberSpeechCapture()
    val scope = rememberCoroutineScope()

    var index by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<PronunciationResult?>(null) }
    var analysis by remember { mutableStateOf<PronunciationAnalysis?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    if (lesson.sentences.isEmpty()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(lesson.title, onBack = onBack)
            Text("Chưa có câu nào để luyện.", modifier = Modifier.padding(16.dp))
        }
        return
    }
    val sentence = lesson.sentences[index]

    fun goTo(i: Int) {
        index = i.coerceIn(0, lesson.sentences.lastIndex)
        result = null
        analysis = null
        aiError = null
        tts.stop()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        AppTopBar(lesson.title, onBack = { tts.stop(); onBack() })
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("💡 ${lesson.tip}", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }

            Text(
                "Câu ${index + 1}/${lesson.sentences.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val r = result
                    if (r == null) {
                        Text(sentence.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            r.words.forEach { w ->
                                val color = when (w.status) {
                                    WordStatus.CORRECT -> Good
                                    WordStatus.CLOSE -> Close
                                    WordStatus.WRONG, WordStatus.MISSING -> Bad
                                }
                                Text(
                                    w.display,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = color,
                                    textDecoration = if (w.status == WordStatus.MISSING) TextDecoration.Underline else null,
                                )
                            }
                        }
                    }
                    analysis?.sentenceIpa?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    if (sentence.meaning.isNotBlank()) {
                        Text(sentence.meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EmojiButton("🔊", "Nghe") { tts.speak(sentence.text, prefs.accent, prefs.speechRate) }
                        EmojiButton("🐢", "Chậm") { tts.speak(sentence.text, prefs.accent, prefs.speechRate * 0.55f) }
                    }
                }
            }

            if (speech.isListening) {
                Text(
                    speech.partialText.ifBlank { "Đang nghe… hãy đọc to câu trên" },
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            speech.error?.let { ErrorBanner(it, "Đóng") { speech.clearError() } }

            result?.let { r ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ScoreCircle(r.score)
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                r.score >= 90 -> "Xuất sắc! Nghe như người bản xứ 🎉"
                                r.score >= 70 -> "Tốt lắm! Chỉ cần chỉnh vài từ."
                                r.score >= 50 -> "Khá ổn, hãy luyện lại các từ màu cam/đỏ."
                                else -> "Hãy nghe chậm rồi thử lại nhé."
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("Máy nghe được: \"${r.recognized}\"", style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedButton(
                    enabled = !analyzing,
                    onClick = {
                        analyzing = true
                        aiError = null
                        scope.launch {
                            try {
                                analysis = container.tutor.analyzePronunciation(
                                    prefs.aiConfig, prefs.profile, sentence.text, r.recognized, r.problemWords,
                                )
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                aiError = e.message
                            } finally {
                                analyzing = false
                            }
                        }
                    },
                ) { Text("🤖 AI phân tích & hướng dẫn sửa") }
            }

            if (analyzing) LoadingRow("AI đang phân tích phát âm…")
            aiError?.let { ErrorBanner(it) }

            analysis?.let { a ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(a.summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        a.tips.forEach { tip ->
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${tip.word}  ${tip.ipa}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { tts.speak(tip.word, prefs.accent, prefs.speechRate * 0.7f) }) { Text("🔊") }
                                }
                                if (tip.issue.isNotBlank()) Text("⚠️ ${tip.issue}", style = MaterialTheme.typography.bodySmall)
                                if (tip.howTo.isNotBlank()) Text("👉 ${tip.howTo}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { goTo(index - 1) }, enabled = index > 0) { Text("← Trước") }
            if (speech.isSupported) {
                MicButton(listening = speech.isListening) {
                    speech.toggle(prefs.accent.languageTag) { heard ->
                        val r = PronunciationScorer.score(sentence.text, heard)
                        result = r
                        analysis = null
                        container.repository.recordPronunciation(r.score)
                    }
                }
            } else {
                Text("Thiết bị không hỗ trợ nhận dạng giọng nói", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { goTo(index + 1) }, enabled = index < lesson.sentences.lastIndex) { Text("Tiếp →") }
        }
        Spacer(Modifier.height(4.dp))
    }
}
