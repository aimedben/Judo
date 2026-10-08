package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JudoDarkColorScheme = darkColorScheme(
    primary = JudoRed,
    onPrimary = Color.White,
    primaryContainer = JudoRedDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFE0E0E0),
    onSecondary = Color(0xFF111111),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = Color.White,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkCardBorder,
    error = Color(0xFFFF5252),
    onError = Color.White
)

private val JudoLightColorScheme = lightColorScheme(
    primary = JudoRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = JudoRedDark,
    secondary = Color(0xFF263238),
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = Color(0xFF111111),
    background = LightBackground,
    onBackground = Color(0xFF111111),
    surface = LightSurface,
    onSurface = Color(0xFF111111),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF555555),
    outline = LightCardBorder,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek Judo dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) JudoDarkColorScheme else JudoLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
