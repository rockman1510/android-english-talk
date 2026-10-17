package com.englishtalk.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.englishtalk.app.ai.TutorService
import com.englishtalk.app.data.AppRepository
import com.englishtalk.app.data.Scenario
import com.englishtalk.app.data.SoundLesson
import com.englishtalk.app.data.VocabDeck
import com.englishtalk.app.platform.PlatformBackHandler
import com.englishtalk.app.platform.PlatformServices
import com.englishtalk.app.ui.conversation.ConversationScreen
import com.englishtalk.app.ui.conversation.ScenarioListScreen
import com.englishtalk.app.ui.home.HomeScreen
import com.englishtalk.app.ui.pronunciation.PronunciationListScreen
import com.englishtalk.app.ui.pronunciation.PronunciationPracticeScreen
import com.englishtalk.app.ui.settings.SettingsScreen
import com.englishtalk.app.ui.theme.EnglishTalkTheme
import com.englishtalk.app.ui.vocabulary.FlashcardScreen
import com.englishtalk.app.ui.vocabulary.VocabularyScreen

val LocalServices = staticCompositionLocalOf<PlatformServices> { error("PlatformServices not provided") }

class AppContainer(
    val repository: AppRepository = AppRepository(),
    val tutor: TutorService = TutorService(),
)

enum class Tab(val emoji: String, val label: String) {
    HOME("🏠", "Trang chủ"),
    TALK("💬", "Hội thoại"),
    PRONOUNCE("🗣️", "Phát âm"),
    VOCAB("📚", "Từ vựng"),
    SETTINGS("⚙️", "Cài đặt"),
}

sealed interface Route {
    data class Root(val tab: Tab) : Route
    data class Conversation(val scenario: Scenario, val topic: String?) : Route
    data class Pronunciation(val lesson: SoundLesson) : Route
    data class Flashcards(val deck: VocabDeck) : Route
}

class Navigator {
    val stack = mutableStateListOf<Route>(Route.Root(Tab.HOME))
    val current: Route get() = stack.last()

    fun push(route: Route) {
        stack.add(route)
    }

    fun selectTab(tab: Tab) {
        stack.clear()
        stack.add(Route.Root(tab))
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}

@Composable
fun App(services: PlatformServices, container: AppContainer = remember { AppContainer() }) {
    val navigator = remember { Navigator() }
    LaunchedEffect(Unit) { container.repository.refreshDay() }

    CompositionLocalProvider(LocalServices provides services) {
        EnglishTalkTheme {
            PlatformBackHandler(enabled = navigator.stack.size > 1) { navigator.back() }
            when (val route = navigator.current) {
                is Route.Root -> Scaffold(
                    bottomBar = {
                        NavigationBar {
                            Tab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = route.tab == tab,
                                    onClick = { navigator.selectTab(tab) },
                                    icon = { Text(tab.emoji) },
                                    label = { Text(tab.label, maxLines = 1) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when (route.tab) {
                            Tab.HOME -> HomeScreen(
                                container = container,
                                onOpenTab = navigator::selectTab,
                                onStartScenario = { s, topic -> navigator.push(Route.Conversation(s, topic)) },
                            )
                            Tab.TALK -> ScenarioListScreen(
                                onStart = { s, topic -> navigator.push(Route.Conversation(s, topic)) },
                            )
                            Tab.PRONOUNCE -> PronunciationListScreen(
                                onOpen = { navigator.push(Route.Pronunciation(it)) },
                            )
                            Tab.VOCAB -> VocabularyScreen(
                                container = container,
                                onOpenDeck = { navigator.push(Route.Flashcards(it)) },
                            )
                            Tab.SETTINGS -> SettingsScreen(container = container)
                        }
                    }
                }
                is Route.Conversation -> ConversationScreen(
                    container = container,
                    scenario = route.scenario,
                    customTopic = route.topic,
                    onBack = { navigator.back() },
                )
                is Route.Pronunciation -> PronunciationPracticeScreen(
                    container = container,
                    lesson = route.lesson,
                    onBack = { navigator.back() },
                )
                is Route.Flashcards -> FlashcardScreen(
                    container = container,
                    deck = route.deck,
                    onBack = { navigator.back() },
                )
            }
        }
    }
}
