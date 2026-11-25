package com.water1014.miowatery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF34363B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF777B83),
    background = Color(0xFFF6F6F4),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEDEB),
    outline = Color(0xFFE2E2DF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE4E5E8),
    secondary = Color(0xFFB8BBC2),
    background = Color(0xFF151619),
    surface = Color(0xFF202125),
    surfaceVariant = Color(0xFF2B2D32)
)

@Composable
fun MioWateryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
