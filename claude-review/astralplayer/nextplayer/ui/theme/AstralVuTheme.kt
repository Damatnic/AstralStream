package com.astralplayer.nextplayer.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Enhanced Color Schemes for AstralVu with Bubble Design
private val AstralVuDarkColors = darkColorScheme(
    // Primary colors - Deep purple with blue undertones for space theme
    primary = Color(0xFF8B5CF6), // Vibrant purple
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF6D28D9), // Darker purple
    onPrimaryContainer = Color(0xFFE9D5FF),
    
    // Secondary colors - Cyan/teal for accent
    secondary = Color(0xFF06B6D4), // Cyan
    onSecondary = Color(0xFF003544),
    secondaryContainer = Color(0xFF0E7490), // Darker cyan
    onSecondaryContainer = Color(0xFFCFFAFE),
    
    // Tertiary colors - Pink/rose for highlights
    tertiary = Color(0xFFEC4899), // Hot pink
    onTertiary = Color(0xFF2D1B69),
    tertiaryContainer = Color(0xFFBE185D), // Darker pink
    onTertiaryContainer = Color(0xFFFDF2F8),
    
    // Background colors - Deep space theme with subtle gradients
    background = Color(0xFF0F0F23), // Very dark blue-purple
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1E1B4B), // Dark purple-blue
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF312E81), // Medium purple-blue
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    // Container colors for cards and components
    surfaceContainer = Color(0xFF1E1B4B),
    surfaceContainerHigh = Color(0xFF312E81),
    surfaceContainerHighest = Color(0xFF3730A3),
    
    // Error colors
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFDC2626),
    onErrorContainer = Color(0xFFFEF2F2),
    
    // Outline colors for borders
    outline = Color(0xFF6366F1),
    outlineVariant = Color(0xFF4338CA)
)

private val AstralVuLightColors = lightColorScheme(
    // Primary colors - Vibrant purple theme
    primary = Color(0xFF7C3AED), // Purple
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE), // Light purple
    onPrimaryContainer = Color(0xFF4C1D95),
    
    // Secondary colors - Sky blue
    secondary = Color(0xFF0EA5E9), // Sky blue
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE), // Light blue
    onSecondaryContainer = Color(0xFF0C4A6E),
    
    // Tertiary colors - Rose/pink
    tertiary = Color(0xFFE11D48), // Rose
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFF1F2), // Light rose
    onTertiaryContainer = Color(0xFF881337),
    
    // Background colors - Clean white with subtle tints
    background = Color(0xFFFCFCFD), // Almost white with purple tint
    onBackground = Color(0xFF1E1B4B),
    surface = Color(0xFFFFFFFF), // Pure white
    onSurface = Color(0xFF1E1B4B),
    surfaceVariant = Color(0xFFF8FAFC), // Very light gray
    onSurfaceVariant = Color(0xFF64748B),
    
    // Container colors
    surfaceContainer = Color(0xFFF8FAFC),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    
    // Error colors
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFF991B1B),
    
    // Outline colors
    outline = Color(0xFF8B5CF6),
    outlineVariant = Color(0xFFCAC4D0)
)

// Enhanced Typography for AstralVu
private val AstralVuTypography = Typography(
    // Display styles for large headings
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    
    // Headline styles for section headers
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    
    // Title styles for cards and components
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Body styles for content
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // Label styles for buttons and small text
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// Enhanced Shapes for bubble design
private val AstralVuShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Bubble theme data class for custom properties
data class BubbleTheme(
    val bubbleElevation: androidx.compose.ui.unit.Dp = 8.dp,
    val bubbleCornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    val bubbleAnimationDuration: Int = 300,
    val glassmorphismAlpha: Float = 0.9f,
    val gradientColors: List<Color> = emptyList(),
    val shadowColor: Color = Color.Black.copy(alpha = 0.1f)
)

// Composition local for bubble theme
val LocalBubbleTheme = compositionLocalOf { 
    BubbleTheme() 
}

// Animation specifications for bubble components
object BubbleAnimations {
    val standardEasing = EaseInOutCubic
    val fastDuration = 150
    val normalDuration = 300
    val slowDuration = 500
    
    val fadeIn = fadeIn(animationSpec = tween(normalDuration, easing = standardEasing))
    val fadeOut = fadeOut(animationSpec = tween(normalDuration, easing = standardEasing))
    
    val slideInFromTop = slideInVertically(
        animationSpec = tween(normalDuration, easing = standardEasing),
        initialOffsetY = { -it }
    )
    
    val slideOutToTop = slideOutVertically(
        animationSpec = tween(normalDuration, easing = standardEasing),
        targetOffsetY = { -it }
    )
    
    val slideInFromBottom = slideInVertically(
        animationSpec = tween(normalDuration, easing = standardEasing),
        initialOffsetY = { it }
    )
    
    val slideOutToBottom = slideOutVertically(
        animationSpec = tween(normalDuration, easing = standardEasing),
        targetOffsetY = { it }
    )
    
    val slideInFromLeft = slideInHorizontally(
        animationSpec = tween(normalDuration, easing = standardEasing),
        initialOffsetX = { -it }
    )
    
    val slideOutToLeft = slideOutHorizontally(
        animationSpec = tween(normalDuration, easing = standardEasing),
        targetOffsetX = { -it }
    )
    
    val slideInFromRight = slideInHorizontally(
        animationSpec = tween(normalDuration, easing = standardEasing),
        initialOffsetX = { it }
    )
    
    val slideOutToRight = slideOutHorizontally(
        animationSpec = tween(normalDuration, easing = standardEasing),
        targetOffsetX = { it }
    )
    
    val scaleIn = scaleIn(
        animationSpec = tween(normalDuration, easing = standardEasing),
        initialScale = 0.8f
    )
    
    val scaleOut = scaleOut(
        animationSpec = tween(normalDuration, easing = standardEasing),
        targetScale = 0.8f
    )
    
    // Combined entrance animations
    val bubbleEnter = fadeIn + scaleIn + slideInFromBottom
    val bubbleExit = fadeOut + scaleOut + slideOutToBottom
    
    val dialogEnter = fadeIn + scaleIn
    val dialogExit = fadeOut + scaleOut
    
    val menuEnter = fadeIn + slideInFromTop
    val menuExit = fadeOut + slideOutToTop
}

// Color utilities for gradients and effects
object BubbleColors {
    fun createGradientBrush(
        colors: List<Color>,
        isVertical: Boolean = true
    ): Brush {
        return if (isVertical) {
            Brush.verticalGradient(colors)
        } else {
            Brush.horizontalGradient(colors)
        }
    }
    
    fun createRadialGradientBrush(
        colors: List<Color>,
        center: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Unspecified,
        radius: Float = Float.POSITIVE_INFINITY
    ): Brush {
        return Brush.radialGradient(
            colors = colors,
            center = center,
            radius = radius
        )
    }
    
    @Composable
    fun dynamicSurfaceColor(
        darkTheme: Boolean = isSystemInDarkTheme(),
        alpha: Float = 0.9f
    ): Color {
        return if (darkTheme) {
            AstralVuDarkColors.surface.copy(alpha = alpha)
        } else {
            AstralVuLightColors.surface.copy(alpha = alpha)
        }
    }
    
    @Composable
    fun glassmorphismColor(
        darkTheme: Boolean = isSystemInDarkTheme()
    ): Color {
        return if (darkTheme) {
            Color.White.copy(alpha = 0.1f)
        } else {
            Color.White.copy(alpha = 0.8f)
        }
    }
}

// Main AstralVu Theme Composable with enhanced features
@Composable
fun AstralVuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false for consistent branding
    bubbleTheme: BubbleTheme = BubbleTheme(
        gradientColors = if (darkTheme) {
            listOf(
                AstralVuDarkColors.background,
                AstralVuDarkColors.surface.copy(alpha = 0.8f)
            )
        } else {
            listOf(
                AstralVuLightColors.background,
                AstralVuLightColors.surface.copy(alpha = 0.8f)
            )
        }
    ),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AstralVuDarkColors else AstralVuLightColors

    CompositionLocalProvider(
        LocalBubbleTheme provides bubbleTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AstralVuTypography,
            shapes = AstralVuShapes,
            content = content
        )
    }
}

// Extensions for easy access to theme properties
@Composable
fun bubbleTheme(): BubbleTheme = LocalBubbleTheme.current

// Preview helpers
@Composable
fun AstralVuPreview(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    AstralVuTheme(darkTheme = darkTheme) {
        Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}

// Note: AstralPlayerTheme is defined in Theme.kt for legacy compatibility