package com.runanywhere.kotlin_starter_example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Color palette with vibrant, modern colors
val PrimaryDark = Color(0xFF0A0E1A)
val PrimaryMid = Color(0xFF1A1F35)
val SurfaceCard = Color(0xFF1E2536)
val AccentCyan = Color(0xFF14B8A6) // More vibrant teal
val AccentViolet = Color(0xFF8B5CF6)
val AccentPink = Color(0xFFEC4899)
val AccentGreen = Color(0xFF10B981)
val AccentOrange = Color(0xFFF97316)
val AccentBlue = Color(0xFF3B82F6) // New vibrant blue
val TextPrimary = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF94A3B8)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentViolet,
    tertiary = AccentPink,
    background = PrimaryDark,
    surface = SurfaceCard,
    surfaceVariant = PrimaryMid,
    primaryContainer = AccentBlue.copy(alpha = 0.15f),
    secondaryContainer = AccentViolet.copy(alpha = 0.15f),
    tertiaryContainer = AccentPink.copy(alpha = 0.15f),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFEF4444).copy(alpha = 0.15f),
    onErrorContainer = Color(0xFFEF4444)
)

@Composable
fun KotlinStarterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
