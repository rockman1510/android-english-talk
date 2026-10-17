package com.englishtalk.app.ui.conversation

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishtalk.app.data.Catalog
import com.englishtalk.app.data.Scenario
import com.englishtalk.app.ui.components.AppTopBar

@Composable
fun ScenarioListScreen(onStart: (Scenario, String?) -> Unit) {
    var topicFor by remember { mutableStateOf<Scenario?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Hội thoại với AI")
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Chọn một tình huống. Gia sư AI sẽ nhập vai, trò chuyện bằng giọng bản xứ và sửa lỗi cho bạn sau mỗi câu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(Catalog.scenarios, key = { it.id }) { scenario ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (scenario.customTopic) topicFor = scenario else onStart(scenario, null)
                    },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(scenario.emoji, fontSize = 34.sp)
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(scenario.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(scenario.titleVi, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "🎯 ${scenario.goal}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AssistChip(onClick = {}, label = { Text(scenario.level) })
                    }
                }
            }
        }
    }

    topicFor?.let { scenario ->
        var topic by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { topicFor = null },
            title = { Text("Bạn muốn nói về chủ đề gì?") },
            text = {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    placeholder = { Text("VD: du lịch, phim ảnh, công việc… (có thể để trống)") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    topicFor = null
                    onStart(scenario, topic.ifBlank { null })
                }) { Text("Bắt đầu") }
            },
            dismissButton = { TextButton(onClick = { topicFor = null }) { Text("Huỷ") } },
        )
    }
}
