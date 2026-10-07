package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val PixelProDarkColorScheme = darkColorScheme(
    primary = StudioCyan400,
    onPrimary = StudioNavy900,
    primaryContainer = StudioNavy700,
    onPrimaryContainer = StudioCyan300,
    secondary = StudioIndigo400,
    onSecondary = StudioNavy900,
    secondaryContainer = StudioNavy700,
    onSecondaryContainer = StudioIndigo400,
    tertiary = StudioAmber400,
    onTertiary = StudioNavy900,
    background = StudioSurfaceDark,
    onBackground = StudioTextLight,
    surface = StudioCardDark,
    onSurface = StudioTextLight,
    surfaceVariant = StudioNavy700,
    onSurfaceVariant = StudioTextDarkMuted,
    outline = StudioNavy600,
    error = StudioRose500
)

private val PixelProLightColorScheme = lightColorScheme(
    primary = StudioCyan500,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = StudioIndigo500,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF2FF),
    onSecondaryContainer = StudioIndigo600,
    tertiary = StudioAmber500,
    onTertiary = Color.White,
    background = StudioSurfaceLight,
    onBackground = StudioTextDark,
    surface = StudioCardLight,
    onSurface = StudioTextDark,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = StudioTextMuted,
    outline = Color(0xFFCBD5E1),
    error = StudioRose500
)

@Composable
fun PixelProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep signature PixelPro studio branding consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> PixelProDarkColorScheme
        else -> PixelProLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
