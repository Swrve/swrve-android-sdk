package com.swrve.sdk.sample.multiplepushproviders

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The accent colour used across the sample. Keep this in step with the
// accentColorHex passed to SwrveNotificationConfig in SampleApplication, so the
// app and the notifications it produces look like the same product.
private val AccentLight = Color(0xFF007AFF)
private val AccentDark = Color(0xFF5BA6FF)

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0051BF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000)
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF00173B),
    primaryContainer = Color(0xFF0E3E7F),
    onPrimaryContainer = Color(0xFFD7E6FF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E6E6)
)

@Composable
fun MultiplePushProvidersSampleTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        content = content
    )
}
