package com.englishtalk.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.englishtalk.app.ai.ClaudeClient
import com.englishtalk.app.data.CefrLevel
import com.englishtalk.app.platform.Accent
import com.englishtalk.app.ui.AppContainer
import com.englishtalk.app.ui.LocalServices
import com.englishtalk.app.ui.components.AppTopBar
import com.englishtalk.app.ui.components.SectionTitle
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(container: AppContainer) {
    val repo = container.repository
    val prefs by repo.preferences.collectAsState()
    val tts = LocalServices.current.textToSpeech
    var showKey by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Cài đặt")
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SectionTitle("🤖 Gia sư AI (Claude)")
            OutlinedTextField(
                value = prefs.apiKey,
                onValueChange = { v -> repo.updatePreferences { it.copy(apiKey = v) } },
                label = { Text("Claude API key") },
                placeholder = { Text("sk-ant-…") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Ẩn" else "Hiện") } },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Lấy key tại console.anthropic.com. Key chỉ lưu trên thiết bị này. Khi phát hành thật, nên đặt key " +
                    "ở máy chủ trung gian (proxy) và điền địa chỉ proxy bên dưới.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ClaudeClient.MODELS.forEach { (id, label) ->
                Row(
                    Modifier.fillMaxWidth().clickable { repo.updatePreferences { it.copy(model = id) } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = prefs.model == id, onClick = { repo.updatePreferences { it.copy(model = id) } })
                    Text(label)
                }
            }
            OutlinedTextField(
                value = prefs.baseUrl,
                onValueChange = { v -> repo.updatePreferences { it.copy(baseUrl = v) } },
                label = { Text("API base URL (proxy)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("🎓 Trình độ của bạn")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CefrLevel.entries.forEach { level ->
                    FilterChip(
                        selected = prefs.level == level,
                        onClick = { repo.updatePreferences { it.copy(level = level) } },
                        label = { Text(level.label) },
                    )
                }
            }

            SectionTitle("🗣️ Giọng bản xứ")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Accent.entries.forEach { accent ->
                    FilterChip(
                        selected = prefs.accent == accent,
                        onClick = { repo.updatePreferences { it.copy(accent = accent) } },
                        label = { Text(accent.label) },
                    )
                }
            }
            Text("Tốc độ đọc: ${(prefs.speechRate * 100).roundToInt()}%")
            Slider(
                value = prefs.speechRate,
                onValueChange = { v -> repo.updatePreferences { it.copy(speechRate = (v * 20).roundToInt() / 20f) } },
                valueRange = 0.5f..1.5f,
            )
            OutlinedButton(onClick = {
                tts.speak("Hi there! This is how I sound. Let's practice speaking English together.", prefs.accent, prefs.speechRate)
            }) { Text("🔊 Nghe thử giọng") }

            SectionTitle("⚙️ Tuỳ chọn")
            ToggleRow("Tự động đọc câu trả lời của AI", prefs.autoPlayReplies) { v ->
                repo.updatePreferences { it.copy(autoPlayReplies = v) }
            }
            ToggleRow("Hiện bản dịch tiếng Việt", prefs.showTranslation) { v ->
                repo.updatePreferences { it.copy(showTranslation = v) }
            }
            Text("Mục tiêu mỗi ngày: ${prefs.dailyGoalXp} XP")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(20, 50, 100, 200).forEach { goal ->
                    FilterChip(
                        selected = prefs.dailyGoalXp == goal,
                        onClick = { repo.updatePreferences { it.copy(dailyGoalXp = goal) } },
                        label = { Text("$goal XP") },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
