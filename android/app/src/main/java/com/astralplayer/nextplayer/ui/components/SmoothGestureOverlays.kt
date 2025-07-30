package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * MX Player-style smooth seek overlay with fluid animations
 */
@Composable
fun SmoothSeekOverlay(
    seekAmount: Long,
    currentPosition: Long,
    duration: Long,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Smooth animations
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val primaryColor = MaterialTheme.colorScheme.primary
    
    if (animatedAlpha > 0.01f) {
        Box(
            modifier = modifier
                .alpha(animatedAlpha)
                .scale(animatedScale),
            contentAlignment = Alignment.Center
        ) {
            // Blur background
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.75f),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animated seek icon
                    AnimatedSeekIcon(
                        isForward = seekAmount > 0,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Seek amount text with smooth transition
                    AnimatedSeekText(
                        seekAmount = seekAmount,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    
                    // Current position
                    Text(
                        text = formatTime(currentPosition + seekAmount),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }
            
            // Progress arc
            Canvas(
                modifier = Modifier.size(160.dp)
            ) {
                drawSmoothProgressArc(
                    progress = (currentPosition + seekAmount).toFloat() / duration,
                    color = primaryColor
                )
            }
        }
    }
}

/**
 * Animated seek icon with smooth rotation
 */
@Composable
private fun AnimatedSeekIcon(
    isForward: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "seek")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isForward) 360f else -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Icon(
        imageVector = if (isForward) Icons.Default.RotateRight else Icons.Default.RotateLeft,
        contentDescription = null,
        tint = Color.White,
        modifier = modifier.rotate(rotation)
    )
}

/**
 * Animated seek amount text
 */
@Composable
private fun AnimatedSeekText(
    seekAmount: Long,
    style: androidx.compose.ui.text.TextStyle
) {
    val animatedValue by animateIntAsState(
        targetValue = (seekAmount / 1000).toInt(),
        animationSpec = tween(
            durationMillis = 150,
            easing = FastOutSlowInEasing
        ),
        label = "seekValue"
    )
    
    Text(
        text = "${if (animatedValue >= 0) "+" else ""}${animatedValue}s",
        style = style
    )
}

/**
 * MX Player-style volume/brightness overlay
 */
@Composable
fun SmoothVolumeOverlay(
    volume: Float,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    SmoothVerticalOverlay(
        value = volume,
        isVisible = isVisible,
        icon = when {
            volume == 0f -> Icons.Filled.VolumeOff
            volume < 0.3f -> Icons.Filled.VolumeMute
            volume < 0.7f -> Icons.Filled.VolumeDown
            else -> Icons.Filled.VolumeUp
        },
        modifier = modifier
    )
}

@Composable
fun SmoothBrightnessOverlay(
    brightness: Float,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    SmoothVerticalOverlay(
        value = brightness,
        isVisible = isVisible,
        icon = when {
            brightness < 0.3f -> Icons.Default.Brightness3
            brightness < 0.7f -> Icons.Default.Brightness5
            else -> Icons.Default.Brightness7
        },
        modifier = modifier
    )
}

/**
 * Generic vertical overlay for volume/brightness
 */
@Composable
private fun SmoothVerticalOverlay(
    value: Float,
    isVisible: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 150,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )
    
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "value"
    )
    
    if (animatedAlpha > 0.01f) {
        Box(
            modifier = modifier
                .alpha(animatedAlpha)
                .width(60.dp)
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(30.dp),
                color = Color.Black.copy(alpha = 0.75f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Icon
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .width(4.dp)
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        // Animated fill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedValue)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                        )
                    }
                    
                    // Percentage text
                    Text(
                        text = "${(animatedValue * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Smooth double tap indicator
 */
@Composable
fun SmoothDoubleTapIndicator(
    isVisible: Boolean,
    isForward: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )
    
    // Ripple effect
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rippleScale"
    )
    
    if (animatedAlpha > 0.01f) {
        Box(
            modifier = modifier
                .alpha(animatedAlpha)
                .scale(rippleScale),
            contentAlignment = Alignment.Center
        ) {
            // Ripple circles
            repeat(3) { index ->
                val delay = index * 200
                val delayedAlpha by animateFloatAsState(
                    targetValue = if (isVisible) 0.3f - (index * 0.1f) else 0f,
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = delay,
                        easing = FastOutSlowInEasing
                    ),
                    label = "rippleAlpha$index"
                )
                
                Canvas(
                    modifier = Modifier
                        .size(100.dp + (index * 20).dp)
                        .alpha(delayedAlpha)
                ) {
                    drawCircle(
                        color = Color.White,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            
            // Center icon
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isForward) 
                            Icons.Default.FastForward 
                        else 
                            Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

/**
 * Draw smooth progress arc
 */
private fun DrawScope.drawSmoothProgressArc(
    progress: Float,
    color: Color
) {
    val strokeWidth = 3.dp.toPx()
    val startAngle = -90f
    val sweepAngle = progress * 360f
    
    drawArc(
        color = color.copy(alpha = 0.3f),
        startAngle = startAngle,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = strokeWidth)
    )
    
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(
                color.copy(alpha = 0.5f),
                color,
                color.copy(alpha = 0.5f)
            )
        ),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}

private fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = milliseconds / (1000 * 60 * 60)
    
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}