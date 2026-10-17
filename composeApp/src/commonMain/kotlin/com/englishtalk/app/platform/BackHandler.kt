package com.englishtalk.app.platform

import androidx.compose.runtime.Composable

/** System back button/gesture support (Android). No-op where the platform has none. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
