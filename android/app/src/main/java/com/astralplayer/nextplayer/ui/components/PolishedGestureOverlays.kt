package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.data.TouchSide
import com.astralplayer.nextplayer.data.SeekDirection
import com.astralplayer.nextplayer.data.gesture.*
import com.astralplayer.nextplayer.ui.animation.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Enhanced seek preview overlay with smooth animations
 */
@Composable
fun PolishedSeekPreviewOverlay(
    seekInfo: HorizontalSeekGestureHandler.SeekPreviewInfo,
    currentPosition: Long,
    videoDuration: Long,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
) {
    val slideAndFade = slideAndFadeTransition(
        visible = true,
        slideDistance = 30.dp,
        direction = SlideDirection.UP
    )
    
    val scaleAnimation by animateGestureScale(isActive = true)
    
    Box(
        modifier = modifier
            .offset(y = slideAndFade.offsetY)
            .alpha(slideAndFade.alpha)
            .scale(scaleAnimation)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 300.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated seek direction indicator
                AnimatedSeekIndicator(
                    isForward = seekInfo.isForward,
                    velocity = seekInfo.velocity
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Seek amount with animated text
                AnimatedSeekAmount(
                    seekDelta = seekInfo.seekDelta,
                    isForward = seekInfo.isForward
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Target time
                Text(
                    text = formatTime(seekInfo.targetPosition),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Progress bar with animation
                AnimatedProgressBar(
                    currentPosition = currentPosition,
                    targetPosition = seekInfo.targetPosition,
                    duration = videoDuration
                )
            }
        }
    }
}

@Composable
private fun AnimatedSeekIndicator(
    isForward: Boolean,
    velocity: Float
) {
    val rotation by animateFloatAsState(
        targetValue = if (isForward) 0f else 180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "seek_direction"
    )
    
    val iconScale by animateFloatAsState(
        targetValue = 1f + (velocity.coerceIn(0f, 2f) * 0.2f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "icon_scale"
    )
    
    Icon(
        imageVector = Icons.Default.FastForward,
        contentDescription = null,
        modifier = Modifier
            .size(32.dp)
            .rotate(rotation)
            .scale(iconScale),
        tint = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun AnimatedSeekAmount(
    seekDelta: Long,
    isForward: Boolean
) {
    val animatedValue by animateIntAsState(
        targetValue = (seekDelta / 1000).toInt(),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "seek_amount"
    )
    
    val color by animateColorAsState(
        targetValue = if (isForward) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiary
        },
        animationSpec = tween(200),
        label = "seek_color"
    )
    
    Text(
        text = "${if (isForward) "+" else "-"}${animatedValue}s",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
private fun AnimatedProgressBar(
    currentPosition: Long,
    targetPosition: Long,
    duration: Long
) {
    val currentProgress = (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    val targetProgress = (targetPosition.toFloat() / duration).coerceIn(0f, 1f)
    
    val animatedTarget by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "target_progress"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Current position
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(currentProgress)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        )
        
        // Target position indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .offset(x = (animatedTarget * 100).dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/**
 * Enhanced volume overlay with wave animation
 */
@Composable
fun PolishedVolumeOverlay(
    volumeInfo: VerticalGestureHandler.VolumeInfo,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateOverlayVisibility(visible)
    val scale by animateGestureScale(visible)
    
    if (alpha > 0.01f) {
        Box(
            modifier = modifier
                .alpha(alpha)
                .scale(scale)
                .padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 8.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Animated volume waves
                    VolumeWaveAnimation(
                        volumeLevel = volumeInfo.percentage,
                        isActive = visible
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when {
                                volumeInfo.isMuted -> Icons.Default.VolumeOff
                                volumeInfo.currentVolume < 0.3f -> Icons.Default.VolumeMute
                                volumeInfo.currentVolume < 0.7f -> Icons.Default.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        AnimatedPercentageText(
                            value = (volumeInfo.percentage * 100).toInt(),
                            suffix = "%"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeWaveAnimation(
    volumeLevel: Float,
    isActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "volume_wave")
    
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    
    val waveAmplitude by animateFloatAsState(
        targetValue = if (isActive) volumeLevel * 20f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wave_amplitude"
    )
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        if (waveAmplitude > 0) {
            drawVolumeWaves(
                center = center,
                phase = wavePhase,
                amplitude = waveAmplitude,
                volumeLevel = volumeLevel
            )
        }
    }
}

private fun DrawScope.drawVolumeWaves(
    center: Offset,
    phase: Float,
    amplitude: Float,
    volumeLevel: Float
) {
    val waveCount = 3
    val baseRadius = size.minDimension / 3f
    
    for (i in 0 until waveCount) {
        val waveAlpha = (1f - i * 0.3f) * volumeLevel
        val waveRadius = baseRadius + amplitude * (i + 1)
        val wavePhaseOffset = phase + i * 60f
        
        drawCircle(
            color = Color.White.copy(alpha = waveAlpha * 0.2f),
            radius = waveRadius,
            center = center,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(10f, 10f),
                    phase = wavePhaseOffset
                )
            )
        )
    }
}

/**
 * Enhanced brightness overlay with sun rays animation
 */
@Composable
fun PolishedBrightnessOverlay(
    brightnessInfo: VerticalGestureHandler.BrightnessInfo,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateOverlayVisibility(visible)
    val scale by animateGestureScale(visible)
    val rotation by animateFloatAsState(
        targetValue = if (visible) 360f else 0f,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "sun_rotation"
    )
    
    if (alpha > 0.01f) {
        Box(
            modifier = modifier
                .alpha(alpha)
                .scale(scale)
                .padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shadowElevation = 8.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Sun rays animation
                    SunRaysAnimation(
                        brightness = brightnessInfo.currentBrightness,
                        rotation = rotation,
                        isActive = visible
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (brightnessInfo.currentBrightness > 0.5f) {
                                Icons.Default.BrightnessHigh
                            } else {
                                Icons.Default.BrightnessLow
                            },
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        AnimatedPercentageText(
                            value = (brightnessInfo.currentBrightness * 100).toInt(),
                            suffix = "%"
                        )
                        
                        if (brightnessInfo.isAutoBrightness) {
                            Text(
                                text = "AUTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SunRaysAnimation(
    brightness: Float,
    rotation: Float,
    isActive: Boolean
) {
    val rayLength by animateFloatAsState(
        targetValue = if (isActive) brightness * 30f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ray_length"
    )
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .rotate(rotation)
    ) {
        if (rayLength > 0) {
            drawSunRays(
                center = center,
                rayLength = rayLength,
                brightness = brightness
            )
        }
    }
}

private fun DrawScope.drawSunRays(
    center: Offset,
    rayLength: Float,
    brightness: Float
) {
    val rayCount = 8
    val angleStep = 360f / rayCount
    
    for (i in 0 until rayCount) {
        val angle = i * angleStep
        val radians = Math.toRadians(angle.toDouble())
        
        val startRadius = size.minDimension / 3f
        val endRadius = startRadius + rayLength
        
        val startX = center.x + startRadius * cos(radians).toFloat()
        val startY = center.y + startRadius * sin(radians).toFloat()
        val endX = center.x + endRadius * cos(radians).toFloat()
        val endY = center.y + endRadius * sin(radians).toFloat()
        
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Yellow.copy(alpha = brightness * 0.8f),
                    Color.Yellow.copy(alpha = 0f)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Enhanced long press overlay with speed progression animation
 */
@Composable
fun PolishedLongPressSeekOverlay(
    seekInfo: LongPressSeekHandler.LongPressSeekInfo,
    modifier: Modifier = Modifier
) {
    val pulseScale by animatePulse(
        enabled = seekInfo.isActive,
        minScale = 0.95f,
        maxScale = 1.05f,
        duration = 500
    )
    
    Box(
        modifier = modifier
            .scale(pulseScale)
            .animateVisibility(
                visible = seekInfo.isActive,
                scale = true,
                fade = true
            )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 250.dp, max = 350.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Speed indicator with animation
                AnimatedSpeedIndicator(
                    currentSpeed = seekInfo.currentSpeed,
                    direction = seekInfo.direction
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Speed progression bar
                SpeedProgressionBar(
                    currentIndex = seekInfo.currentSpeedIndex,
                    maxIndex = seekInfo.maxSpeedIndex,
                    speedProgression = seekInfo.speedProgression
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Seek amount
                AnimatedSeekAmount(
                    seekDelta = seekInfo.totalSeekAmount,
                    isForward = seekInfo.direction == SeekDirection.FORWARD
                )
            }
        }
    }
}

@Composable
private fun AnimatedSpeedIndicator(
    currentSpeed: Float,
    direction: SeekDirection
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "speed_value"
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (direction == SeekDirection.BACKWARD) {
                Icons.Default.FastRewind
            } else {
                Icons.Default.FastForward
            },
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "${animatedSpeed.toInt()}x",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SpeedProgressionBar(
    currentIndex: Int,
    maxIndex: Int,
    speedProgression: List<Float>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        speedProgression.forEachIndexed { index, speed ->
            SpeedProgressionDot(
                isActive = index <= currentIndex,
                isPulsing = index == currentIndex,
                speed = speed
            )
        }
    }
}

@Composable
private fun SpeedProgressionDot(
    isActive: Boolean,
    isPulsing: Boolean,
    speed: Float
) {
    val scale by animateFloatAsState(
        targetValue = when {
            isPulsing -> 1.3f
            isActive -> 1f
            else -> 0.7f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "dot_scale"
    )
    
    val color by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(200),
        label = "dot_color"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .scale(scale)
                .background(color, CircleShape)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "${speed.toInt()}x",
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Animated percentage text
 */
@Composable
private fun AnimatedPercentageText(
    value: Int,
    suffix: String = ""
) {
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "percentage"
    )
    
    Text(
        text = "$animatedValue$suffix",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium
    )
}

// Helper functions
private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}