package com.englishtalk.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Good = Color(0xFF2E9E5B)
val Close = Color(0xFFE09B1A)
val Bad = Color(0xFFD64545)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4CF0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E1FF),
    onPrimaryContainer = Color(0xFF1B1066),
    secondary = Color(0xFF00A699),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF5F0),
    onSecondaryContainer = Color(0xFF00322D),
    tertiary = Color(0xFFFF7A45),
    tertiaryContainer = Color(0xFFFFE2D6),
    onTertiaryContainer = Color(0xFF4A1500),
    background = Color(0xFFF7F6FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEBF5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC3BBFF),
    onPrimary = Color(0xFF2A1C8C),
    primaryContainer = Color(0xFF4134C4),
    onPrimaryContainer = Color(0xFFE5E1FF),
    secondary = Color(0xFF5FDBCB),
    secondaryContainer = Color(0xFF00504A),
    onSecondaryContainer = Color(0xFFCFF5F0),
    tertiary = Color(0xFFFFB596),
    tertiaryContainer = Color(0xFF7A2E0E),
    onTertiaryContainer = Color(0xFFFFE2D6),
    background = Color(0xFF121118),
    surface = Color(0xFF1B1A22),
    surfaceVariant = Color(0xFF2A2833),
)

@Composable
fun EnglishTalkTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
