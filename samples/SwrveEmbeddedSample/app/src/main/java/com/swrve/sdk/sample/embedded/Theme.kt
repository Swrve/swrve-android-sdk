package com.swrve.sdk.sample.embedded

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0051BF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF03DAC5),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF018786),
    onSecondaryContainer = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5BA6FF),
    onPrimary = Color(0xFF00173B),
    primaryContainer = Color(0xFF0E3E7F),
    onPrimaryContainer = Color(0xFFD7E6FF),
    secondary = Color(0xFF66E0D3),
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF00504A),
    onSecondaryContainer = Color(0xFFB5FFF7),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E6E6)
)

@Composable
fun SwrveSampleTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    val typography = Typography()
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}