package com.cypy.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark theme colors (Light Blue / Slate Dark)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),          // Sky Blue
    primaryContainer = Color(0xFF0288D1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF80D8FF),        // Soft Cyan
    background = Color(0xFF0F172A),       // Slate Dark
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    outline = Color(0xFF38BDF8),
)

// Light theme colors (Vibrant Light Blue)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0288D1),          // Sky Blue
    primaryContainer = Color(0xFFE0F2FE),  // Very Soft Light Blue
    onPrimaryContainer = Color(0xFF0369A1),// Dark Sky Blue Text
    secondary = Color(0xFF00B0FF),
    background = Color(0xFFF8FAFC),       // Crisp Off-White Light Blue
    surface = Color.White,
    surfaceVariant = Color(0xFFE0F2FE),   // Soft Blue Container
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    outline = Color(0xFF38BDF8),
)


@Composable
fun CypyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
