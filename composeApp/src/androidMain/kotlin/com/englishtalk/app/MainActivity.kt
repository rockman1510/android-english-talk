package com.englishtalk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.englishtalk.app.platform.AndroidSpeechRecognizer
import com.englishtalk.app.platform.AndroidTextToSpeech
import com.englishtalk.app.platform.PlatformServices
import com.englishtalk.app.ui.App

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognizer: AndroidSpeechRecognizer
    private lateinit var textToSpeech: AndroidTextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Must be created before the activity is STARTED (registers a permission launcher).
        speechRecognizer = AndroidSpeechRecognizer(this)
        textToSpeech = AndroidTextToSpeech(applicationContext)
        val services = PlatformServices(speechRecognizer, textToSpeech)

        setContent { App(services) }
    }

    override fun onDestroy() {
        textToSpeech.shutdown()
        speechRecognizer.destroy()
        super.onDestroy()
    }
}
