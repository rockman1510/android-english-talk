package com.englishtalk.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.englishtalk.app.platform.Accent
import com.englishtalk.app.platform.PlatformServices
import com.englishtalk.app.platform.SpeechEvent
import com.englishtalk.app.platform.SpeechRecognizer
import com.englishtalk.app.platform.TextToSpeech
import com.englishtalk.app.ui.App
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/** Desktop build is for quick UI previews: typing replaces the microphone, TTS is silent. */
fun main() = application {
    val services = PlatformServices(
        speechRecognizer = object : SpeechRecognizer {
            override val isSupported = false
            override suspend fun requestPermission() = false
            override fun listen(languageTag: String): Flow<SpeechEvent> = emptyFlow()
            override fun stopListening() = Unit
        },
        textToSpeech = object : TextToSpeech {
            override val isSpeaking: StateFlow<Boolean> = MutableStateFlow(false)
            override fun speak(text: String, accent: Accent, rate: Float) = println("[TTS ${accent.languageTag}] $text")
            override fun stop() = Unit
        },
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "EnglishTalk",
        state = rememberWindowState(width = 420.dp, height = 860.dp),
    ) {
        App(services)
    }
}
