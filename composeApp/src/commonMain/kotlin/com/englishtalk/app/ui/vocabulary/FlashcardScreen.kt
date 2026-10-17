package com.englishtalk.app.ui.vocabulary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishtalk.app.data.VocabDeck
import com.englishtalk.app.scoring.PronunciationScorer
import com.englishtalk.app.ui.AppContainer
import com.englishtalk.app.ui.LocalServices
import com.englishtalk.app.ui.components.AppTopBar
import com.englishtalk.app.ui.components.EmojiButton
import com.englishtalk.app.ui.components.ErrorBanner
import com.englishtalk.app.ui.components.MicButton
import com.englishtalk.app.ui.components.ScoreCircle
import com.englishtalk.app.ui.components.rememberSpeechCapture

/** Flashcards with a simple "learn again" queue: unknown cards go back to the end. */
@Composable
fun FlashcardScreen(container: AppContainer, deck: VocabDeck, onBack: () -> Unit) {
    val tts = LocalServices.current.textToSpeech
    val prefs by container.repository.preferences.collectAsState()
    val speech = rememberSpeechCapture()

    val queue = remember { mutableStateListOf(*deck.words.toTypedArray()) }
    var known by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    var sayScore by remember { mutableStateOf<Int?>(null) }
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(350))

    fun next(knewIt: Boolean) {
        if (queue.isEmpty()) return
        val card = queue.removeAt(0)
        if (knewIt) known++ else queue.add(card)
        container.repository.recordWordReviewed()
        flipped = false
        sayScore = null
        tts.stop()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        AppTopBar("${deck.emoji} ${deck.title}", onBack = { tts.stop(); onBack() })
        val total = deck.words.size.coerceAtLeast(1)
        LinearProgressIndicator(
            progress = { known.toFloat() / total },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Text(
            "Đã thuộc $known/${deck.words.size}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(16.dp, 8.dp),
        )

        val card = queue.firstOrNull()
        if (card == null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🎉", fontSize = 64.sp)
                Text("Bạn đã thuộc hết bộ từ này!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Quay lại") }
            }
            return@Column
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12 * density
                    }
                    .clickable { flipped = !flipped },
            ) {
                Column(
                    Modifier.fillMaxSize().padding(20.dp).graphicsLayer { if (rotation > 90f) rotationY = 180f },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                ) {
                    if (rotation <= 90f) {
                        Text(card.word, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(card.ipa, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Chạm để xem nghĩa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(card.meaning, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        Text(
                            "\"${card.example}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clickable { tts.speak(card.example, prefs.accent, prefs.speechRate) },
                        )
                        Text("Chạm câu ví dụ để nghe", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        speech.error?.let { ErrorBanner(it, "Đóng") { speech.clearError() } }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmojiButton("🔊", "Nghe") { tts.speak(card.word, prefs.accent, prefs.speechRate) }
            if (speech.isSupported) {
                MicButton(listening = speech.isListening, size = 60.dp) {
                    speech.toggle(prefs.accent.languageTag) { heard ->
                        sayScore = PronunciationScorer.score(card.word, heard).score
                    }
                }
            }
            sayScore?.let { ScoreCircle(it, size = 60.dp) }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = { next(false) }, modifier = Modifier.weight(1f)) { Text("🔁 Học lại") }
            Button(onClick = { next(true) }, modifier = Modifier.weight(1f)) { Text("✅ Đã thuộc") }
        }
    }
}
