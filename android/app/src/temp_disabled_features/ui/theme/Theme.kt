package com.astralplayer.nextplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Application theme options
 */
enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
    COSMIC,
    HIGH_CONTRAST
}

// Standard light and dark schemes
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00BCD4),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFFFF4081),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00BCD4),
    secondary = Color(0xFF03DAC6),
    tertiary = Color(0xFFFF4081),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

// Cosmic theme with vibrant, mystical palette
private val CosmicColorScheme = lightColorScheme(
    primary = Color(0xFF673AB7),
    secondary = Color(0xFF9C27B0),
    tertiary = Color(0xFFFFC107),
    background = Color(0xFF2A0344),
    surface = Color(0xFF330055),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFEDE7F6),
    onSurface = Color(0xFFEDE7F6)
)

// High-contrast theme for accessibility
private val HighContrastColorScheme = lightColorScheme(
    primary = Color.Yellow,
    secondary = Color.White,
    tertiary = Color.Cyan,
    background = Color.Black,
    surface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.Black
)

/**
 * Applies the selected theme across the app
 * @param appTheme the chosen AppTheme
 */
@Composable
fun AstralTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    // Determine whether to use dark scheme when SYSTEM is selected
    val useDark = when (appTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.COSMIC -> false  // cosmic is custom light-based
        AppTheme.HIGH_CONTRAST -> false
    }

    val colorScheme = when (appTheme) {
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.COSMIC -> CosmicColorScheme
        AppTheme.HIGH_CONTRAST -> HighContrastColorScheme
        AppTheme.SYSTEM -> if (useDark) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Alias maintaining compatibility
 */
@Composable
fun NextPlayerTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    AstralTheme(appTheme = appTheme, content = content)
}