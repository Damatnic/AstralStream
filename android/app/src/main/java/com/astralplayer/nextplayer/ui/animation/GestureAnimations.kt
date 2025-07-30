package com.astralplayer.nextplayer.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Smooth fade in/out animation for overlays
 */
@Composable
fun animateOverlayVisibility(
    visible: Boolean,
    duration: Int = 300,
    delayMillis: Int = 0
): State<Float> {
    val targetAlpha = if (visible) 1f else 0f
    return animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(
            durationMillis = duration,
            delayMillis = delayMillis,
            easing = if (visible) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "overlay_visibility"
    )
}

/**
 * Animated scale for gesture feedback
 */
@Composable
fun animateGestureScale(
    isActive: Boolean,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f
): State<Float> {
    val targetScale = if (isActive) maxScale else minScale
    return animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "gesture_scale"
    )
}

/**
 * Ripple effect for tap gestures
 */
@Composable
fun rememberRippleAnimation(): RippleAnimationState {
    val rippleState = remember { RippleAnimationState() }
    
    LaunchedEffect(rippleState.isAnimating) {
        if (rippleState.isAnimating) {
            rippleState.animate()
        }
    }
    
    return rippleState
}

class RippleAnimationState {
    var isAnimating by mutableStateOf(false)
    var scale by mutableStateOf(0f)
    var alpha by mutableStateOf(1f)
    
    suspend fun animate() {
        val animationSpec = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
        
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = animationSpec
        ) { value, _ ->
            scale = value * 2f
            alpha = 1f - value
        }
        
        isAnimating = false
        scale = 0f
        alpha = 1f
    }
    
    fun trigger() {
        isAnimating = true
    }
}

/**
 * Smooth slide animation for seek preview
 */
fun Modifier.animateSeekPreview(
    offset: Dp,
    visible: Boolean
): Modifier = composed {
    val offsetAnimation by animateDpAsState(
        targetValue = if (visible) 0.dp else offset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "seek_preview_offset"
    )
    
    val alphaAnimation by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "seek_preview_alpha"
    )
    
    this
        .offset(y = offsetAnimation)
        .alpha(alphaAnimation)
}

/**
 * Elastic scale animation for double tap
 */
@Composable
fun animateDoubleTapScale(): State<Float> {
    var isAnimating by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        finishedListener = { isAnimating = false },
        label = "double_tap_scale"
    )
    
    return remember {
        derivedStateOf {
            scale
        }
    }
}

/**
 * Progress animation for long press
 */
@Composable
fun animateLongPressProgress(
    isActive: Boolean,
    duration: Long = 2000L
): State<Float> {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = duration.toInt(),
                    easing = LinearEasing
                )
            ) { value, _ ->
                progress = value
            }
        } else {
            progress = 0f
        }
    }
    
    return rememberUpdatedState(progress)
}

/**
 * Smooth zoom animation
 */
fun Modifier.animateZoom(
    scale: Float,
    isAnimating: Boolean
): Modifier = composed {
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = if (isAnimating) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        } else {
            tween(300)
        },
        label = "zoom_scale"
    )
    
    graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
        transformOrigin = TransformOrigin.Center
    }
}

/**
 * Pulse animation for active elements
 */
@Composable
fun animatePulse(
    enabled: Boolean = true,
    minScale: Float = 0.95f,
    maxScale: Float = 1.05f,
    duration: Int = 1000
): State<Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    return if (enabled) {
        infiniteTransition.animateFloat(
            initialValue = minScale,
            targetValue = maxScale,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        rememberUpdatedState(1f)
    }
}

/**
 * Slide and fade transition for overlays
 */
@Composable
fun slideAndFadeTransition(
    visible: Boolean,
    slideDistance: Dp = 20.dp,
    direction: SlideDirection = SlideDirection.UP
): SlideAndFadeState {
    val offsetY by animateDpAsState(
        targetValue = when {
            !visible -> when (direction) {
                SlideDirection.UP -> -slideDistance
                SlideDirection.DOWN -> slideDistance
            }
            else -> 0.dp
        },
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "slide_offset"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (visible) 350 else 250,
            easing = LinearEasing
        ),
        label = "fade_alpha"
    )
    
    return SlideAndFadeState(offsetY, alpha)
}

data class SlideAndFadeState(
    val offsetY: Dp,
    val alpha: Float
)

enum class SlideDirection {
    UP, DOWN
}

/**
 * Gesture feedback animation specs
 */
object GestureFeedbackAnimations {
    val seekFeedback = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val volumeFeedback = tween<Float>(
        durationMillis = 150,
        easing = FastOutSlowInEasing
    )
    
    val brightnessFeedback = tween<Float>(
        durationMillis = 150,
        easing = FastOutSlowInEasing
    )
    
    val doubleTapFeedback = spring<Float>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    val longPressFeedback = tween<Float>(
        durationMillis = 200,
        easing = LinearOutSlowInEasing
    )
    
    val zoomFeedback = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Animated visibility with multiple effects
 */
fun Modifier.animateVisibility(
    visible: Boolean,
    scale: Boolean = true,
    fade: Boolean = true,
    slide: Boolean = false,
    slideDistance: Dp = 10.dp
): Modifier = composed {
    val alphaAnimation = if (fade) {
        animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(200),
            label = "visibility_alpha"
        ).value
    } else 1f
    
    val scaleAnimation = if (scale) {
        animateFloatAsState(
            targetValue = if (visible) 1f else 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "visibility_scale"
        ).value
    } else 1f
    
    val offsetAnimation = if (slide) {
        animateDpAsState(
            targetValue = if (visible) 0.dp else slideDistance,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "visibility_offset"
        ).value
    } else 0.dp
    
    graphicsLayer {
        alpha = alphaAnimation
        scaleX = scaleAnimation
        scaleY = scaleAnimation
        translationY = offsetAnimation.toPx()
    }
}

/**
 * Smooth progress indicator animation
 */
@Composable
fun animateProgress(
    progress: Float,
    animationSpec: AnimationSpec<Float> = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )
): State<Float> {
    return animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = animationSpec,
        label = "progress"
    )
}

/**
 * Stagger animation for list items
 */
@Composable
fun <T> animateStagger(
    items: List<T>,
    delayBetween: Int = 50,
    content: @Composable (item: T, animationState: StaggerAnimationState) -> Unit
) {
    items.forEachIndexed { index, item ->
        val animationState = rememberStaggerAnimation(
            index = index,
            delayMillis = index * delayBetween
        )
        
        content(item, animationState)
    }
}

@Composable
fun rememberStaggerAnimation(
    index: Int,
    delayMillis: Int
): StaggerAnimationState {
    val state = remember(index) { StaggerAnimationState() }
    
    LaunchedEffect(index) {
        delay(delayMillis.toLong())
        state.animate()
    }
    
    return state
}

class StaggerAnimationState {
    var alpha by mutableStateOf(0f)
    var offsetY by mutableStateOf(20.dp)
    var scale by mutableStateOf(0.8f)
    
    suspend fun animate() {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { value, _ ->
            alpha = value
            offsetY = (20 * (1 - value)).dp
            scale = 0.8f + 0.2f * value
        }
    }
}

/**
 * Gesture hint animation
 */
@Composable
fun animateGestureHint(
    show: Boolean,
    delay: Long = 1000L
): State<Float> {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(show) {
        if (show) {
            delay(delay)
            visible = true
            delay(3000)
            visible = false
        } else {
            visible = false
        }
    }
    
    return animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "gesture_hint"
    )
}