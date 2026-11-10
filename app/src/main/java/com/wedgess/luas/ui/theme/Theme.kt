package com.wedgess.luas.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = TransitGreenLight,
    onPrimary = Color(0xFF00391F),
    primaryContainer = TransitGreenContainerDark,
    onPrimaryContainer = TransitGreenContainer,
    secondary = Color(0xFFB6CCB9),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF394B3D),
    onSecondaryContainer = Color(0xFFD2E8D4),
    tertiary = TransitBlueLight,
    onTertiary = Color(0xFF00363C),
    tertiaryContainer = Color(0xFF1E4D53),
    onTertiaryContainer = Color(0xFFBCEBF1),
    background = TransitNight,
    onBackground = TransitMist,
    surface = TransitNight,
    onSurface = TransitMist,
    surfaceVariant = TransitSlate,
    surfaceContainer = Color(0xFF1C211D),
    surfaceContainerHigh = Color(0xFF272C27),
    surfaceContainerHighest = Color(0xFF323731),
    onSurfaceVariant = Color(0xFFC1C9BF),
    outline = Color(0xFF8B938A)
)

private val LightColorScheme = lightColorScheme(
    primary = TransitGreen,
    onPrimary = Color.White,
    primaryContainer = TransitGreenContainer,
    onPrimaryContainer = Color(0xFF002110),
    secondary = Color(0xFF506453),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D4),
    onSecondaryContainer = Color(0xFF0D1F12),
    tertiary = TransitBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEBF1),
    onTertiaryContainer = Color(0xFF002024),
    background = TransitPaper,
    onBackground = TransitInk,
    surface = TransitPaper,
    onSurface = TransitInk,
    surfaceVariant = TransitMist,
    surfaceContainer = Color(0xFFEBF0E8),
    surfaceContainerHigh = Color(0xFFE5EAE2),
    surfaceContainerHighest = TransitMist,
    onSurfaceVariant = TransitSlate,
    outline = Color(0xFF717970)
)

@Composable
fun LuasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
