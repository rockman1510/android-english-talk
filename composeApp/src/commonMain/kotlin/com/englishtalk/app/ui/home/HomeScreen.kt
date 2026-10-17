package com.englishtalk.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishtalk.app.data.Catalog
import com.englishtalk.app.data.Scenario
import com.englishtalk.app.ui.AppContainer
import com.englishtalk.app.ui.Tab
import com.englishtalk.app.ui.components.ErrorBanner
import com.englishtalk.app.ui.components.SectionTitle

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenTab: (Tab) -> Unit,
    onStartScenario: (Scenario, String?) -> Unit,
) {
    val progress by container.repository.progress.collectAsState()
    val prefs by container.repository.preferences.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Xin chào! 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Luyện nói tiếng Anh như người bản xứ cùng gia sư AI.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (prefs.apiKey.isBlank()) {
            ErrorBanner(
                "Bạn chưa nhập Claude API key — các tính năng AI sẽ chưa hoạt động.",
                actionLabel = "Cài đặt",
                onAction = { onOpenTab(Tab.SETTINGS) },
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 36.sp)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(
                            "${progress.streakDays} ngày liên tiếp",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("Tổng cộng ${progress.totalXp} XP", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                val goal = prefs.dailyGoalXp.coerceAtLeast(1)
                Text("Mục tiêu hôm nay: ${progress.todayXp}/$goal XP", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (progress.todayXp.toFloat() / goal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("💬", "${progress.spokenTurns}", "Lượt nói", Modifier.weight(1f))
            StatTile("🎯", "${progress.averagePronunciation}", "Điểm phát âm TB", Modifier.weight(1f))
            StatTile("📚", "${progress.wordsReviewed}", "Từ đã ôn", Modifier.weight(1f))
        }

        SectionTitle("Bắt đầu nhanh")
        FeatureCard(
            emoji = "🤖",
            title = "Trò chuyện với gia sư AI",
            subtitle = "Nói chuyện tự do, AI sửa lỗi ngữ pháp và gợi ý cách nói tự nhiên hơn.",
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) { onStartScenario(Catalog.scenarios.first(), null) }
        FeatureCard(
            emoji = "🎭",
            title = "Nhập vai tình huống thực tế",
            subtitle = "Gọi cà phê, phỏng vấn, sân bay, khách sạn… như ngoài đời thật.",
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) { onOpenTab(Tab.TALK) }
        FeatureCard(
            emoji = "🗣️",
            title = "Luyện phát âm giọng bản xứ",
            subtitle = "Nghe giọng Mỹ/Anh/Úc, nói theo và được chấm điểm từng từ.",
            color = MaterialTheme.colorScheme.primaryContainer,
        ) { onOpenTab(Tab.PRONOUNCE) }
        FeatureCard(
            emoji = "🧠",
            title = "Từ vựng & thành ngữ thông dụng",
            subtitle = "Flashcard có phát âm, AI tạo bộ từ theo chủ đề bạn muốn.",
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) { onOpenTab(Tab.VOCAB) }
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun FeatureCard(emoji: String, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 32.sp)
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
