package com.water1014.miowatery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F1419), onPrimary = Color.White,
    secondary = Color(0xFF6B7480), onSecondary = Color.White,
    background = Color.White, onBackground = Color(0xFF0F1419),
    surface = Color.White, onSurface = Color(0xFF0F1419),
    surfaceVariant = Color(0xFFF3F4F5), onSurfaceVariant = Color(0xFF6B7480),
    outline = Color(0xFFEAECEE), error = Color(0xFFD93636)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8EAED), onPrimary = Color(0xFF111318),
    secondary = Color(0xFF9AA2AC), onSecondary = Color(0xFF111318),
    background = Color(0xFF101216), onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF191D22), onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF242A31), onSurfaceVariant = Color(0xFF9AA2AC),
    outline = Color(0xFF343B44), error = Color(0xFFFF5B5B)
)
@Composable
fun MioWateryTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
