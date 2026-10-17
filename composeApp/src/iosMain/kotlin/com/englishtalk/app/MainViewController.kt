package com.englishtalk.app

import androidx.compose.ui.window.ComposeUIViewController
import com.englishtalk.app.platform.IosSpeechRecognizer
import com.englishtalk.app.platform.IosTextToSpeech
import com.englishtalk.app.platform.PlatformServices
import com.englishtalk.app.ui.App

private val services by lazy { PlatformServices(IosSpeechRecognizer(), IosTextToSpeech()) }

/** Entry point used by the SwiftUI app (iosApp/iosApp/ContentView.swift). */
fun MainViewController() = ComposeUIViewController { App(services) }
