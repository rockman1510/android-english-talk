package com.englishtalk.app.ui.pronunciation

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.englishtalk.app.data.Catalog
import com.englishtalk.app.data.PracticeSentence
import com.englishtalk.app.data.SoundLesson
import com.englishtalk.app.ui.components.AppTopBar

@Composable
fun PronunciationListScreen(onOpen: (SoundLesson) -> Unit) {
    var custom by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Luyện phát âm")
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✍️ Luyện câu của riêng bạn", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = custom,
                            onValueChange = { custom = it },
                            placeholder = { Text("Nhập câu tiếng Anh bất kỳ…") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            enabled = custom.isNotBlank(),
                            onClick = {
                                val sentences = custom.split(Regex("(?<=[.!?])\\s+"))
                                    .filter { it.isNotBlank() }
                                    .map { PracticeSentence(it.trim(), "") }
                                onOpen(
                                    SoundLesson(
                                        id = "custom",
                                        title = "Câu của bạn",
                                        subtitle = "",
                                        tip = "Nghe giọng bản xứ trước, sau đó nói theo. Bấm \"AI phân tích\" để nhận mẹo chi tiết.",
                                        sentences = sentences,
                                    )
                                )
                            },
                        ) { Text("Luyện ngay") }
                    }
                }
            }
            items(Catalog.soundLessons, key = { it.id }) { lesson ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(lesson) },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(lesson.subtitle, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("${lesson.sentences.size} câu", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
