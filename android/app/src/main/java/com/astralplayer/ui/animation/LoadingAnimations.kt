package com.astralplayer.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Beautiful loading animations for AstralStream
 */

/**
 * Shimmer effect for loading skeletons
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    widthOfShadowBrush: Int = 500,
    angleOfAxisY: Float = 270f,
    durationMillis: Int = 1000,
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surface.copy(alpha = 1.0f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = (durationMillis + widthOfShadowBrush).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate"
    )
    
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset(
            x = translateAnimation.value - widthOfShadowBrush,
            y = 0f
        ),
        end = androidx.compose.ui.geometry.Offset(
            x = translateAnimation.value,
            y = angleOfAxisY
        ),
    )
    
    Box(
        modifier = modifier.background(brush = brush)
    )
}

/**
 * Skeleton loading for video items
 */
@Composable
fun VideoItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail skeleton
        Box(
            modifier = Modifier
                .size(120.dp, 80.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            ShimmerEffect()
        }
        
        // Text skeleton
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                ShimmerEffect()
            }
            
            // Subtitle
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                ShimmerEffect()
            }
        }
    }
}

/**
 * Pulsing dots loading indicator
 */
@Composable
fun PulsingDotsIndicator(
    modifier: Modifier = Modifier,
    dotSize: Dp = 12.dp,
    delayUnit: Int = 300,
    color: Color = MaterialTheme.colorScheme.primary
) {
    @Composable
    fun Dot(
        scale: Float
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .scale(scale)
                .clip(CircleShape)
                .background(color)
        )
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    @Composable
    fun animateScaleWithDelay(delay: Int) = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = delayUnit * 4
                0f at delay with LinearEasing
                1f at delay + delayUnit with LinearEasing
                0f at delay + delayUnit * 2
            }
        ),
        label = "dot_scale"
    )
    
    val scale1 = animateScaleWithDelay(0)
    val scale2 = animateScaleWithDelay(delayUnit)
    val scale3 = animateScaleWithDelay(delayUnit * 2)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSize / 2)
    ) {
        Dot(scale1.value)
        Dot(scale2.value)
        Dot(scale3.value)
    }
}

/**
 * Circular progress with text
 */
@Composable
fun CircularProgressWithText(
    progress: Float,
    modifier: Modifier = Modifier,
    text: String = "${(progress * 100).toInt()}%"
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 4.dp
        )
        
        androidx.compose.material3.Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * Wave loading animation
 */
@Composable
fun WaveLoadingAnimation(
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val delays = listOf(0, 160, 320, 480, 640)
    
    val animations = delays.map { delay ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1200
                    0.3f at 0
                    1f at delay
                    0.3f at delay + 320
                    0.3f at 1200
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_bar"
        )
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        animations.forEach { animatedValue ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .scale(scaleY = animatedValue.value)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Fade in/out loading overlay
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = { PulsingDotsIndicator() }
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}