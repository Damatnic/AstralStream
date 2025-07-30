package com.astralplayer.nextplayer.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gesture-specific theme configuration
 */
@Immutable
data class GestureColors(
    val seekPrimary: Color,
    val seekSecondary: Color,
    val volumePrimary: Color,
    val volumeSecondary: Color,
    val brightnessPrimary: Color,
    val brightnessSecondary: Color,
    val doubleTapPrimary: Color,
    val doubleTapSecondary: Color,
    val longPressPrimary: Color,
    val longPressSecondary: Color,
    val errorPrimary: Color,
    val errorSecondary: Color,
    val successPrimary: Color,
    val successSecondary: Color,
    val overlayBackground: Color,
    val overlayContent: Color
)

@Immutable
data class GestureDimensions(
    val overlayCornerRadius: Dp = 16.dp,
    val overlayElevation: Dp = 8.dp,
    val overlayPadding: Dp = 16.dp,
    val iconSize: Dp = 32.dp,
    val iconSizeLarge: Dp = 40.dp,
    val progressBarHeight: Dp = 4.dp,
    val dotSize: Dp = 12.dp,
    val minOverlayWidth: Dp = 200.dp,
    val maxOverlayWidth: Dp = 350.dp
)

@Immutable
data class GestureAnimations(
    val fadeIn: AnimationSpec<Float> = tween(300, easing = FastOutSlowInEasing),
    val fadeOut: AnimationSpec<Float> = tween(200, easing = LinearOutSlowInEasing),
    val scaleIn: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    ),
    val scaleOut: AnimationSpec<Float> = tween(200),
    val slideIn: AnimationSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
    val slideOut: AnimationSpec<Dp> = tween(200),
    val colorTransition: AnimationSpec<Color> = tween(200),
    val progressAnimation: AnimationSpec<Float> = tween(300, easing = FastOutSlowInEasing),
    val pulseAnimation: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(1000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
)

/**
 * Light theme gesture colors
 */
val LightGestureColors = GestureColors(
    seekPrimary = Color(0xFF1976D2),
    seekSecondary = Color(0xFF42A5F5),
    volumePrimary = Color(0xFF388E3C),
    volumeSecondary = Color(0xFF66BB6A),
    brightnessPrimary = Color(0xFFF57C00),
    brightnessSecondary = Color(0xFFFFB74D),
    doubleTapPrimary = Color(0xFF7B1FA2),
    doubleTapSecondary = Color(0xFFAB47BC),
    longPressPrimary = Color(0xFFD32F2F),
    longPressSecondary = Color(0xFFEF5350),
    errorPrimary = Color(0xFFB00020),
    errorSecondary = Color(0xFFCF6679),
    successPrimary = Color(0xFF00C853),
    successSecondary = Color(0xFF69F0AE),
    overlayBackground = Color(0xF5FFFFFF),
    overlayContent = Color(0xDE000000)
)

/**
 * Dark theme gesture colors
 */
val DarkGestureColors = GestureColors(
    seekPrimary = Color(0xFF64B5F6),
    seekSecondary = Color(0xFF90CAF9),
    volumePrimary = Color(0xFF81C784),
    volumeSecondary = Color(0xFFA5D6A7),
    brightnessPrimary = Color(0xFFFFB74D),
    brightnessSecondary = Color(0xFFFFCC80),
    doubleTapPrimary = Color(0xFFBA68C8),
    doubleTapSecondary = Color(0xFFCE93D8),
    longPressPrimary = Color(0xFFEF5350),
    longPressSecondary = Color(0xFFE57373),
    errorPrimary = Color(0xFFCF6679),
    errorSecondary = Color(0xFFFF8A95),
    successPrimary = Color(0xFF69F0AE),
    successSecondary = Color(0xFFB9F6CA),
    overlayBackground = Color(0xF5121212),
    overlayContent = Color(0xDEFFFFFF)
)

/**
 * Gesture theme provider
 */
object GestureTheme {
    val colors: GestureColors
        @Composable
        get() = LocalGestureColors.current
    
    val dimensions: GestureDimensions
        @Composable
        get() = LocalGestureDimensions.current
    
    val animations: GestureAnimations
        @Composable
        get() = LocalGestureAnimations.current
}

internal val LocalGestureColors = staticCompositionLocalOf { LightGestureColors }
internal val LocalGestureDimensions = staticCompositionLocalOf { GestureDimensions() }
internal val LocalGestureAnimations = staticCompositionLocalOf { GestureAnimations() }

/**
 * Provides gesture theme
 */
@Composable
fun ProvideGestureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: GestureColors = if (darkTheme) DarkGestureColors else LightGestureColors,
    dimensions: GestureDimensions = GestureDimensions(),
    animations: GestureAnimations = GestureAnimations(),
    content: @Composable () -> Unit
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalGestureColors provides colors,
        LocalGestureDimensions provides dimensions,
        LocalGestureAnimations provides animations,
        content = content
    )
}

/**
 * Dynamic color adjustments
 */
object GestureColorUtils {
    /**
     * Get contrasting color for overlays
     */
    fun getContrastingColor(backgroundColor: Color): Color {
        return if (backgroundColor.luminance() > 0.5f) {
            Color.Black.copy(alpha = 0.87f)
        } else {
            Color.White.copy(alpha = 0.87f)
        }
    }
    
    /**
     * Adjust color for emphasis
     */
    fun emphasize(color: Color, factor: Float = 1.2f): Color {
        val hsl = color.toHsl()
        return Color.hsl(
            hue = hsl[0],
            saturation = (hsl[1] * factor).coerceIn(0f, 1f),
            lightness = hsl[2]
        )
    }
    
    /**
     * Create gradient colors
     */
    fun createGradient(baseColor: Color, steps: Int = 3): List<Color> {
        val hsl = baseColor.toHsl()
        return List(steps) { i ->
            val factor = i.toFloat() / (steps - 1)
            Color.hsl(
                hue = hsl[0],
                saturation = hsl[1],
                lightness = (hsl[2] + (1f - hsl[2]) * factor * 0.3f).coerceIn(0f, 1f),
                alpha = baseColor.alpha
            )
        }
    }
}

/**
 * Extension to convert Color to HSL
 */
private fun Color.toHsl(): FloatArray {
    val r = red
    val g = green
    val b = blue
    
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    
    val lightness = (max + min) / 2f
    
    val saturation = if (delta == 0f) {
        0f
    } else {
        delta / (1f - kotlin.math.abs(2f * lightness - 1f))
    }
    
    val hue = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta + if (g < b) 6f else 0f) / 6f
        max == g -> ((b - r) / delta + 2f) / 6f
        else -> ((r - g) / delta + 4f) / 6f
    } * 360f
    
    return floatArrayOf(hue, saturation, lightness)
}

/**
 * Gesture elevation levels
 */
object GestureElevation {
    val low = 4.dp
    val medium = 8.dp
    val high = 12.dp
    val overlay = 16.dp
}

/**
 * Gesture opacity levels
 */
object GestureOpacity {
    const val disabled = 0.38f
    const val medium = 0.60f
    const val high = 0.87f
    const val full = 1.00f
}