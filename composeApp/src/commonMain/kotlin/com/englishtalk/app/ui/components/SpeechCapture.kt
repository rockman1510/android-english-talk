package com.englishtalk.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.englishtalk.app.platform.SpeechEvent
import com.englishtalk.app.platform.SpeechRecognizer
import com.englishtalk.app.platform.TextToSpeech
import com.englishtalk.app.ui.LocalServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** UI state holder around [SpeechRecognizer]: tap once to talk, tap again to finish. */
class SpeechCaptureState(
    private val recognizer: SpeechRecognizer,
    private val tts: TextToSpeech,
    private val scope: CoroutineScope,
) {
    var isListening by mutableStateOf(false)
        private set
    var partialText by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val isSupported get() = recognizer.isSupported

    private var job: Job? = null

    fun toggle(languageTag: String = "en-US", onResult: (String) -> Unit) {
        if (isListening) recognizer.stopListening() else start(languageTag, onResult)
    }

    fun start(languageTag: String = "en-US", onResult: (String) -> Unit) {
        job?.cancel()
        tts.stop() // don't let the recognizer hear the app's own voice
        error = null
        partialText = ""
        job = scope.launch {
            if (!recognizer.requestPermission()) {
                error = "Cần quyền micro và nhận dạng giọng nói. Hãy cấp quyền trong Cài đặt của máy."
                return@launch
            }
            isListening = true
            try {
                recognizer.listen(languageTag).collect { event ->
                    when (event) {
                        is SpeechEvent.Partial -> partialText = event.text
                        is SpeechEvent.Final -> {
                            partialText = event.text
                            if (event.text.isNotBlank()) onResult(event.text)
                        }
                        is SpeechEvent.Error -> error = event.message
                        SpeechEvent.Ready -> Unit
                    }
                }
            } finally {
                isListening = false
            }
        }
    }

    fun clearError() {
        error = null
    }

    fun cancel() {
        job?.cancel()
        isListening = false
    }
}

@Composable
fun rememberSpeechCapture(): SpeechCaptureState {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val state = remember { SpeechCaptureState(services.speechRecognizer, services.textToSpeech, scope) }
    DisposableEffect(state) { onDispose { state.cancel() } }
    return state
}
