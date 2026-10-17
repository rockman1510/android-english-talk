package com.englishtalk.app.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.tts.UtteranceProgressListener
import android.speech.SpeechRecognizer as AndroidRecognizer
import android.speech.tts.TextToSpeech as AndroidTts
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import java.util.UUID

class AndroidSpeechRecognizer(private val activity: ComponentActivity) : SpeechRecognizer {

    private var pendingPermission: CompletableDeferred<Boolean>? = null
    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingPermission?.complete(granted)
            pendingPermission = null
        }

    private var recognizer: AndroidRecognizer? = null

    override val isSupported: Boolean
        get() = AndroidRecognizer.isRecognitionAvailable(activity)

    override suspend fun requestPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) return true
        val deferred = CompletableDeferred<Boolean>()
        pendingPermission = deferred
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        return deferred.await()
    }

    // Collected on the main thread (Compose scope), as SpeechRecognizer requires.
    override fun listen(languageTag: String): Flow<SpeechEvent> = callbackFlow {
        val sr = recognizer ?: AndroidRecognizer.createSpeechRecognizer(activity).also { recognizer = it }
        var lastPartial = ""
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechEvent.Ready)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(AndroidRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    lastPartial = text
                    trySend(SpeechEvent.Partial(text))
                }
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(AndroidRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                trySend(SpeechEvent.Final(text ?: lastPartial))
                close()
            }

            override fun onError(error: Int) {
                if (lastPartial.isNotBlank() && error == AndroidRecognizer.ERROR_NO_MATCH) {
                    trySend(SpeechEvent.Final(lastPartial))
                } else {
                    trySend(SpeechEvent.Error(errorMessage(error)))
                }
                close()
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Give learners time to think mid-sentence.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }
        sr.startListening(intent)

        awaitClose { sr.cancel() }
    }

    override fun stopListening() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorMessage(code: Int) = when (code) {
        AndroidRecognizer.ERROR_NO_MATCH, AndroidRecognizer.ERROR_SPEECH_TIMEOUT ->
            "Không nghe rõ, hãy thử nói lại to và rõ hơn."
        AndroidRecognizer.ERROR_NETWORK, AndroidRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Lỗi mạng khi nhận dạng giọng nói."
        AndroidRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Ứng dụng chưa có quyền dùng micro."
        AndroidRecognizer.ERROR_RECOGNIZER_BUSY -> "Bộ nhận dạng đang bận, thử lại sau giây lát."
        AndroidRecognizer.ERROR_AUDIO -> "Lỗi thu âm."
        else -> "Lỗi nhận dạng giọng nói ($code)."
    }
}

class AndroidTextToSpeech(context: Context) : TextToSpeech {

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var ready = false
    private var pending: (() -> Unit)? = null

    private val tts: AndroidTts = AndroidTts(context) { status ->
        ready = status == AndroidTts.SUCCESS
        if (ready) {
            pending?.invoke()
            pending = null
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }

    override fun speak(text: String, accent: Accent, rate: Float) {
        val action = {
            val locale = Locale.forLanguageTag(accent.languageTag)
            tts.language = locale
            // Prefer the highest-quality installed voice for that accent.
            runCatching {
                tts.voices
                    ?.filter { it.locale == locale && !it.isNetworkConnectionRequired && AndroidTts.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
                    ?.maxByOrNull { it.quality }
                    ?.let { tts.voice = it }
            }
            tts.setSpeechRate(rate)
            tts.speak(text, AndroidTts.QUEUE_FLUSH, null, UUID.randomUUID().toString())
            Unit
        }
        if (ready) action() else pending = action
    }

    override fun stop() {
        pending = null
        tts.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts.shutdown()
    }
}
