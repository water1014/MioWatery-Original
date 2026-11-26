package com.water1014.miowatery.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalMioThemeMode = staticCompositionLocalOf<MutableState<String>> {
    error("MioWatery theme mode is not provided")
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F1419), onPrimary = Color.White,
    secondary = Color(0xFF6B7480), onSecondary = Color.White,
    background = Color(0xFFFFFFFF), onBackground = Color(0xFF0F1419),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF0F1419),
    surfaceVariant = Color(0xFFF3F4F5), onSurfaceVariant = Color(0xFF6B7480),
    outline = Color(0xFFEAECEE), error = Color(0xFFD93636),
    errorContainer = Color(0xFFFFEEEE), onErrorContainer = Color(0xFFD93636)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2F2F2), onPrimary = Color(0xFF000000),
    secondary = Color(0xFF71767B), onSecondary = Color(0xFF000000),
    background = Color(0xFF000000), onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF000000), onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF303030), onSurfaceVariant = Color(0xFF71767B),
    outline = Color(0xFF2F3336), error = Color(0xFFFF7B7B),
    errorContainer = Color(0xFF351C1C), onErrorContainer = Color(0xFFFF7B7B)
)

@Composable
fun MioWateryTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("miowatery_appearance", Context.MODE_PRIVATE)
    }
    val mode = remember(preferences) {
        mutableStateOf(preferences.getString("theme_mode", "light") ?: "light")
    }
    val useDarkTheme = when (mode.value) {
        "dark" -> true
        "auto" -> darkTheme
        else -> false
    }
    CompositionLocalProvider(LocalMioThemeMode provides mode) {
        MaterialTheme(
            colorScheme = if (useDarkTheme) DarkColors else LightColors,
            content = content
        )
    }
}
