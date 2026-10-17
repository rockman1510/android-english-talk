package com.englishtalk.app.platform

import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesisVoiceQualityDefault
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.AVFAudio.AVSpeechUtteranceMaximumSpeechRate
import platform.AVFAudio.AVSpeechUtteranceMinimumSpeechRate
import platform.Foundation.NSLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import platform.darwin.NSObject
import kotlin.coroutines.resume

private fun configureAudioSession() {
    val session = AVAudioSession.sharedInstance()
    session.setCategory(
        AVAudioSessionCategoryPlayAndRecord,
        mode = AVAudioSessionModeDefault,
        options = AVAudioSessionCategoryOptionDefaultToSpeaker or AVAudioSessionCategoryOptionAllowBluetooth,
        error = null,
    )
    session.setActive(true, null)
}

class IosSpeechRecognizer : SpeechRecognizer {

    private var audioEngine: AVAudioEngine? = null
    private var request: SFSpeechAudioBufferRecognitionRequest? = null
    private var task: SFSpeechRecognitionTask? = null

    override val isSupported: Boolean = true

    override suspend fun requestPermission(): Boolean {
        val speechStatus = suspendCancellableCoroutine { cont ->
            SFSpeechRecognizer.requestAuthorization { status: SFSpeechRecognizerAuthorizationStatus ->
                cont.resume(status)
            }
        }
        if (speechStatus != SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
            return false
        }
        return suspendCancellableCoroutine { cont ->
            AVAudioSession.sharedInstance().requestRecordPermission { granted -> cont.resume(granted) }
        }
    }

    override fun listen(languageTag: String): Flow<SpeechEvent> = callbackFlow {
        val recognizer = SFSpeechRecognizer(locale = NSLocale(localeIdentifier = languageTag))
        if (!recognizer.available) {
            trySend(SpeechEvent.Error("Nhận dạng giọng nói hiện không khả dụng (kiểm tra kết nối mạng)."))
            close()
            awaitClose { }
            return@callbackFlow
        }

        configureAudioSession()
        val engine = AVAudioEngine()
        val req = SFSpeechAudioBufferRecognitionRequest().apply {
            shouldReportPartialResults = true
        }
        audioEngine = engine
        request = req

        val input = engine.inputNode
        val format = input.outputFormatForBus(0u)
        input.installTapOnBus(0u, bufferSize = 1024u, format = format) { buffer, _ ->
            if (buffer != null) req.appendAudioPCMBuffer(buffer)
        }
        engine.prepare()
        if (!engine.startAndReturnError(null)) {
            trySend(SpeechEvent.Error("Không mở được micro."))
            close()
        } else {
            trySend(SpeechEvent.Ready)
        }

        var lastText = ""
        task = recognizer.recognitionTaskWithRequest(req) { result, error ->
            if (result != null) {
                lastText = result.bestTranscription.formattedString
                if (result.final) {
                    trySend(SpeechEvent.Final(lastText))
                    close()
                } else {
                    trySend(SpeechEvent.Partial(lastText))
                }
            } else if (error != null) {
                if (lastText.isNotBlank()) trySend(SpeechEvent.Final(lastText))
                else trySend(SpeechEvent.Error("Không nghe rõ, hãy thử nói lại."))
                close()
            }
        }

        awaitClose { teardown() }
    }

    override fun stopListening() {
        // Stop capturing audio; the recognizer then delivers a final result.
        audioEngine?.stop()
        audioEngine?.inputNode?.removeTapOnBus(0u)
        request?.endAudio()
    }

    private fun teardown() {
        audioEngine?.stop()
        audioEngine?.inputNode?.removeTapOnBus(0u)
        request?.endAudio()
        task?.cancel()
        audioEngine = null
        request = null
        task = null
    }
}

class IosTextToSpeech : TextToSpeech {

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val synthesizer = AVSpeechSynthesizer()

    // Keep a strong reference: the synthesizer's delegate property is weak.
    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didStartSpeechUtterance: AVSpeechUtterance) {
            _isSpeaking.value = true
        }

        @ObjCSignatureOverride
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
            _isSpeaking.value = false
        }
    }

    init {
        synthesizer.delegate = delegate
    }

    override fun speak(text: String, accent: Accent, rate: Float) {
        configureAudioSession()
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = bestVoice(accent.languageTag)
        utterance.rate = (AVSpeechUtteranceDefaultSpeechRate * rate)
            .coerceIn(AVSpeechUtteranceMinimumSpeechRate, AVSpeechUtteranceMaximumSpeechRate)
        synthesizer.speakUtterance(utterance)
    }

    /** Picks an Enhanced/Premium voice when the user has downloaded one (Settings > Accessibility > Spoken Content). */
    private fun bestVoice(languageTag: String): AVSpeechSynthesisVoice? {
        val voices = AVSpeechSynthesisVoice.speechVoices()
            .filterIsInstance<AVSpeechSynthesisVoice>()
            .filter { it.language == languageTag }
        return voices.maxByOrNull { it.quality }
            ?.takeIf { it.quality > AVSpeechSynthesisVoiceQualityDefault }
            ?: AVSpeechSynthesisVoice.voiceWithLanguage(languageTag)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        _isSpeaking.value = false
    }
}
