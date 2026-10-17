package com.englishtalk.app.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Native accents offered by the platform text-to-speech engines. */
enum class Accent(val languageTag: String, val label: String) {
    US("en-US", "Mỹ (US)"),
    UK("en-GB", "Anh (UK)"),
    AU("en-AU", "Úc (AU)"),
}

sealed interface SpeechEvent {
    data object Ready : SpeechEvent
    data class Partial(val text: String) : SpeechEvent
    data class Final(val text: String) : SpeechEvent
    data class Error(val message: String) : SpeechEvent
}

/**
 * Speech-to-text. Android uses android.speech.SpeechRecognizer,
 * iOS uses SFSpeechRecognizer + AVAudioEngine.
 */
interface SpeechRecognizer {
    val isSupported: Boolean

    /** Asks for microphone / speech-recognition permission if needed. */
    suspend fun requestPermission(): Boolean

    /**
     * Starts listening while the flow is collected. Emits partial results and
     * completes after a [SpeechEvent.Final] or [SpeechEvent.Error].
     */
    fun listen(languageTag: String = "en-US"): Flow<SpeechEvent>

    /** Stops capturing audio; the recognizer then delivers its final result. */
    fun stopListening()
}

/** Text-to-speech with native English voices. */
interface TextToSpeech {
    val isSpeaking: StateFlow<Boolean>

    /** [rate] 1.0 = normal speed, 0.5 = slow (useful for shadowing). */
    fun speak(text: String, accent: Accent, rate: Float = 1.0f)

    fun stop()
}

class PlatformServices(
    val speechRecognizer: SpeechRecognizer,
    val textToSpeech: TextToSpeech,
)
