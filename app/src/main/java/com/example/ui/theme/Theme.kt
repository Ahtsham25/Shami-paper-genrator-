package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ShamiEmeraldLight,
    onPrimary = Color.White,
    primaryContainer = ShamiNavyPrimary,
    onPrimaryContainer = Color.White,
    secondary = ShamiGold,
    onSecondary = InkDark,
    secondaryContainer = Color(0xFF3B2F13),
    onSecondaryContainer = ShamiGoldBright,
    tertiary = Color(0xFF38BDF8),
    background = PaperBackgroundDark,
    onBackground = InkLight,
    surface = PaperSurfaceDark,
    onSurface = InkLight,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = InkLightMuted
)

private val LightColorScheme = lightColorScheme(
    primary = ShamiNavyDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = ShamiNavyDark,
    secondary = ShamiEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = ShamiGold,
    onTertiary = InkDark,
    background = PaperBackgroundLight,
    onBackground = InkDark,
    surface = PaperSurfaceLight,
    onSurface = InkDark,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = InkMuted
)

@Composable
fun PaperMakerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
