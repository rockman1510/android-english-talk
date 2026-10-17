package com.englishtalk.app.ui.vocabulary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishtalk.app.data.Catalog
import com.englishtalk.app.data.VocabDeck
import com.englishtalk.app.ui.AppContainer
import com.englishtalk.app.ui.components.AppTopBar
import com.englishtalk.app.ui.components.ErrorBanner
import com.englishtalk.app.ui.components.LoadingRow
import com.englishtalk.app.ui.components.SectionTitle
import kotlinx.coroutines.launch
import kotlin.time.Clock

@Composable
fun VocabularyScreen(container: AppContainer, onOpenDeck: (VocabDeck) -> Unit) {
    val customDecks by container.repository.customDecks.collectAsState()
    val scope = rememberCoroutineScope()
    var topic by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Từ vựng")
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🤖 AI tạo bộ từ theo chủ đề", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            placeholder = { Text("VD: phỏng vấn IT, nấu ăn, bóng đá…") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Button(
                            enabled = topic.isNotBlank() && !generating,
                            onClick = {
                                generating = true
                                error = null
                                scope.launch {
                                    try {
                                        val prefs = container.repository.preferences.value
                                        val words = container.tutor.generateVocabulary(prefs.aiConfig, prefs.profile, topic.trim())
                                        val deck = VocabDeck(
                                            id = "ai_${Clock.System.now().toEpochMilliseconds()}",
                                            emoji = "✨",
                                            title = topic.trim().replaceFirstChar { it.uppercase() },
                                            words = words,
                                            aiGenerated = true,
                                        )
                                        container.repository.addDeck(deck)
                                        topic = ""
                                        onOpenDeck(deck)
                                    } catch (e: Exception) {
                                        if (e is kotlinx.coroutines.CancellationException) throw e
                                        error = e.message
                                    } finally {
                                        generating = false
                                    }
                                }
                            },
                        ) { Text("Tạo bộ từ") }
                        if (generating) LoadingRow("Đang tạo bộ từ…")
                    }
                }
            }
            error?.let { item { ErrorBanner(it) } }

            if (customDecks.isNotEmpty()) {
                item { SectionTitle("Bộ từ của bạn") }
                items(customDecks, key = { it.id }) { deck ->
                    DeckRow(deck, onClick = { onOpenDeck(deck) }, onDelete = { container.repository.deleteDeck(deck.id) })
                }
            }
            item { SectionTitle("Bộ từ có sẵn") }
            items(Catalog.decks, key = { it.id }) { deck ->
                DeckRow(deck, onClick = { onOpenDeck(deck) }, onDelete = null)
            }
        }
    }
}

@Composable
private fun DeckRow(deck: VocabDeck, onClick: () -> Unit, onDelete: (() -> Unit)?) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(deck.emoji, fontSize = 30.sp)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(deck.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${deck.words.size} từ", style = MaterialTheme.typography.bodySmall)
            }
            if (onDelete != null) TextButton(onClick = onDelete) { Text("Xoá") }
        }
    }
}
