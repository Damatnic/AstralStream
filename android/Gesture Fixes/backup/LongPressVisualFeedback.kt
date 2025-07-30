package com.astralplayer.nextplayer.feature.player.gestures

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Comprehensive long press visual feedback overlay
 */
@Composable
fun LongPressSeekOverlay(
    state: LongPressSeekState,
    totalDuration: Long,
    settings: LongPressGestureSettings,
    modifier: Modifier = Modifier
) {
    if (!state.isActive || !settings.showSpeedIndicator) return
    
    val density = LocalDensity.current
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "longPressSeek")
    
    val rotationAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (state.direction == SeekDirection.FORWARD) 360f else -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2000 / state.currentSpeed).toInt().coerceIn(200, 2000),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scaleAnimation by animateFloatAsState(
        targetValue = if (state.isActive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .scale(scaleAnimation)
            .alpha(scaleAnimation),
        contentAlignment = Alignment.Center
    ) {
        // Main overlay card
        Card(
            modifier = Modifier
                .padding(48.dp)
                .alpha(0.95f),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E).copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                getDirectionColor(state.direction).copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            radius = 300f
                        )
                    )
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Speed indicator with rotating arrows
                    SpeedIndicatorSection(
                        speed = state.currentSpeed,
                        speedLevel = state.speedLevel,
                        direction = state.direction,
                        rotationAnimation = rotationAnimation,
                        pulseAnimation = pulseAnimation
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Position and time display
                    PositionDisplaySection(
                        currentPosition = state.currentPosition,
                        totalDuration = totalDuration,
                        startPosition = state.startPosition
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Speed progression indicator
                    SpeedProgressionIndicator(
                        currentSpeedLevel = state.speedLevel,
                        speedProgression = settings.speedProgression,
                        maxSpeed = settings.maxSpeed
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Direction and control hints
                    ControlHintsSection(
                        direction = state.direction,
                        elapsedTime = state.elapsedTime,
                        settings = settings
                    )
                }
            }
        }
        
        // Background speed visualization
        SpeedVisualizationBackground(
            speed = state.currentSpeed,
            direction = state.direction,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SpeedIndicatorSection(
    speed: Float,
    speedLevel: Int,
    direction: SeekDirection,
    rotationAnimation: Float,
    pulseAnimation: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Direction icon with rotation
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(pulseAnimation)
                .rotate(rotationAnimation),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getDirectionIcon(direction, speedLevel),
                contentDescription = null,
                tint = getDirectionColor(direction),
                modifier = Modifier.size(48.dp)
            )
        }
        
        // Speed text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${speed.roundToInt()}x",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "SPEED",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Speed level dots
        SpeedLevelDots(
            currentLevel = speedLevel,
            maxLevel = 5,
            color = getDirectionColor(direction)
        )
    }
}

@Composable
private fun PositionDisplaySection(
    currentPosition: Long,
    totalDuration: Long,
    startPosition: Long
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Current position
        Text(
            text = formatTime(currentPosition),
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        // Duration and progress
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "/ ${formatTime(totalDuration)}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
            
            // Progress indicator
            val progress = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF00BCD4),
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
        
        // Seek delta
        val seekDelta = currentPosition - startPosition
        if (abs(seekDelta) > 1000) {
            Text(
                text = "${if (seekDelta > 0) "+" else ""}${formatTime(abs(seekDelta))}",
                color = if (seekDelta > 0) Color(0xFF4CAF50) else Color(0xFFFF9800),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SpeedProgressionIndicator(
    currentSpeedLevel: Int,
    speedProgression: List<Float>,
    maxSpeed: Float
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        speedProgression.forEachIndexed { index, speed ->
            val isActive = index <= currentSpeedLevel
            val isCurrent = index == currentSpeedLevel
            
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 12.dp else 8.dp)
                    .background(
                        color = when {
                            isCurrent -> Color(0xFF00BCD4)
                            isActive -> Color(0xFF00BCD4).copy(alpha = 0.6f)
                            else -> Color.White.copy(alpha = 0.3f)
                        },
                        shape = CircleShape
                    )
                    .scale(if (isCurrent) 1.2f else 1f)
            )
        }
    }
}

@Composable
private fun ControlHintsSection(
    direction: SeekDirection,
    elapsedTime: Long,
    settings: LongPressGestureSettings
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Direction hint
        if (settings.enableDirectionChange) {
            Text(
                text = "Swipe horizontally to change direction",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        
        // Speed hint
        if (elapsedTime < 3000) {
            Text(
                text = "Hold longer for higher speeds",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
        
        // Current action
        Text(
            text = when (direction) {
                SeekDirection.FORWARD -> "Fast Forward"
                SeekDirection.BACKWARD -> "Rewind"
                SeekDirection.NONE -> "Seeking"
            },
            color = getDirectionColor(direction),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SpeedLevelDots(
    currentLevel: Int,
    maxLevel: Int,
    color: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(maxLevel) { index ->
            val isActive = index <= currentLevel
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = if (isActive) color else Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun SpeedVisualizationBackground(
    speed: Float,
    direction: SeekDirection,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    val animationSpeed = (speed * 100).toInt().coerceIn(50, 1000)
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (direction == SeekDirection.FORWARD) 1f else -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "backgroundOffset"
    )
    
    Canvas(modifier = modifier.alpha(0.1f)) {
        val particleCount = (speed * 5).toInt().coerceIn(5, 50)
        val color = getDirectionColor(direction)
        
        repeat(particleCount) { index ->
            val x = (size.width * (index.toFloat() / particleCount + offset)) % size.width
            val y = size.height * (index * 0.618f % 1f) // Golden ratio for distribution
            val particleSize = (speed * 2f).coerceIn(2f, 8f)
            
            drawCircle(
                color = color,
                radius = particleSize,
                center = Offset(x, y),
                alpha = 0.3f
            )
        }
    }
}

/**
 * Compact speed indicator for minimal overlay mode
 */
@Composable
fun CompactLongPressIndicator(
    state: LongPressSeekState,
    modifier: Modifier = Modifier
) {
    if (!state.isActive) return
    
    val scaleAnimation by animateFloatAsState(
        targetValue = if (state.isActive) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "compactScale"
    )
    
    Box(
        modifier = modifier
            .scale(scaleAnimation)
            .alpha(scaleAnimation),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (state.direction == SeekDirection.FORWARD) 
                        Icons.Default.FastForward else Icons.Default.FastRewind,
                    contentDescription = null,
                    tint = getDirectionColor(state.direction),
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "${state.currentSpeed.roundToInt()}x",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = formatTime(state.currentPosition),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Speed transition animation component
 */
@Composable
fun SpeedTransitionAnimation(
    oldSpeed: Float,
    newSpeed: Float,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    val scaleAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1.2f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        finishedListener = { onAnimationEnd() },
        label = "speedTransition"
    )
    
    val alphaAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "speedTransitionAlpha"
    )
    
    LaunchedEffect(newSpeed) {
        delay(200)
        isVisible = false
    }
    
    Box(
        modifier = modifier
            .scale(scaleAnimation)
            .alpha(alphaAnimation),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF00BCD4).copy(alpha = 0.9f)
            )
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${newSpeed.roundToInt()}x",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (newSpeed > oldSpeed) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper functions
private fun getDirectionIcon(direction: SeekDirection, speedLevel: Int): ImageVector {
    return when (direction) {
        SeekDirection.FORWARD -> when {
            speedLevel >= 4 -> Icons.Default.FastForward
            speedLevel >= 2 -> Icons.Default.PlayArrow
            else -> Icons.Default.SkipNext
        }
        SeekDirection.BACKWARD -> when {
            speedLevel >= 4 -> Icons.Default.FastRewind
            speedLevel >= 2 -> Icons.Default.PlayArrow
            else -> Icons.Default.SkipPrevious
        }
        SeekDirection.NONE -> Icons.Default.PlayArrow
    }
}

private fun getDirectionColor(direction: SeekDirection): Color {
    return when (direction) {
        SeekDirection.FORWARD -> Color(0xFF4CAF50)
        SeekDirection.BACKWARD -> Color(0xFFFF9800)
        SeekDirection.NONE -> Color(0xFF00BCD4)
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * Custom drawing functions for advanced visual effects
 */
fun DrawScope.drawSpeedRipple(
    center: Offset,
    radius: Float,
    speed: Float,
    color: Color,
    alpha: Float = 1f
) {
    val rippleCount = (speed / 2).toInt().coerceIn(1, 5)
    
    repeat(rippleCount) { index ->
        val rippleRadius = radius * (1f + index * 0.3f)
        val rippleAlpha = alpha * (1f - index * 0.2f)
        
        drawCircle(
            color = color,
            radius = rippleRadius,
            center = center,
            alpha = rippleAlpha,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

fun DrawScope.drawSpeedArcs(
    center: Offset,
    radius: Float,
    speed: Float,
    direction: SeekDirection,
    color: Color
) {
    val arcCount = (speed / 4).toInt().coerceIn(1, 8)
    val sweepAngle = 60f
    val startAngle = if (direction == SeekDirection.FORWARD) -30f else 150f
    
    repeat(arcCount) { index ->
        val arcRadius = radius * (0.8f + index * 0.1f)
        val arcAlpha = 1f - index * 0.15f
        
        rotate(degrees = index * 45f, pivot = center) {
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2),
                alpha = arcAlpha,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}