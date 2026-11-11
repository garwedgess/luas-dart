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
    primary = Color(0xFF88CCFF),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF214C69),
    onPrimaryContainer = Color(0xFFCBE7FF),
    secondary = Color(0xFFB6CCB9),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF394B3D),
    onSecondaryContainer = Color(0xFFD2E8D4),
    tertiary = TransitBlueLight,
    onTertiary = Color(0xFF00363C),
    tertiaryContainer = Color(0xFF1E4D53),
    onTertiaryContainer = Color(0xFFBCEBF1),
    background = Color(0xFF0C0F17),
    onBackground = Color(0xFFF1F1F7),
    surface = Color(0xFF0C0F17),
    onSurface = Color(0xFFF1F1F7),
    surfaceVariant = Color(0xFF303541),
    surfaceContainer = Color(0xFF161A24),
    surfaceContainerHigh = Color(0xFF222733),
    surfaceContainerHighest = Color(0xFF343A48),
    onSurfaceVariant = Color(0xFFC6CAD6),
    outline = Color(0xFF9299AA),
    outlineVariant = Color(0xFF424959)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF17638D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBE7FF),
    onPrimaryContainer = Color(0xFF00344F),
    secondary = Color(0xFF506453),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D4),
    onSecondaryContainer = Color(0xFF0D1F12),
    tertiary = TransitBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEBF1),
    onTertiaryContainer = Color(0xFF002024),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C24),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C24),
    surfaceVariant = TransitMist,
    surfaceContainer = Color(0xFFEEF0F8),
    surfaceContainerHigh = Color(0xFFE7EAF3),
    surfaceContainerHighest = Color(0xFFE0E3ED),
    onSurfaceVariant = Color(0xFF444B59),
    outline = Color(0xFF717970)
)

@Composable
fun LuasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // The reference palette is the default; callers can opt into wallpaper colours on Android 12+.
    dynamicColor: Boolean = false,
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
