package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * Enhanced long press visual feedback with comprehensive indicators
 * Implements requirements 3.3: Comprehensive long press visual feedback
 */
@Composable
fun EnhancedLongPressVisualFeedback(
    seekState: LongPressSeekState,
    currentPosition: Long,
    duration: Long,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && seekState.isActive,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Main overlay container
            Card(
                modifier = Modifier
                    .width(320.dp)
                    .height(180.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Speed indicator with animated icon
                    SpeedIndicatorSection(
                        speed = seekState.seekSpeed,
                        direction = seekState.direction,
                        speedLevel = seekState.accelerationLevel
                    )
                    
                    // Progress bar with position indicators
                    PositionProgressBar(
                        currentPosition = currentPosition,
                        duration = duration,
                        seekPreviewPosition = seekState.seekPreviewPosition
                    )
                    
                    // Time display with remaining duration
                    TimeDisplaySection(
                        currentPosition = currentPosition,
                        duration = duration,
                        seekPreviewPosition = seekState.seekPreviewPosition
                    )
                }
            }
            
            // Animated speed visualization rings
            AnimatedSpeedRings(
                speed = seekState.seekSpeed,
                direction = seekState.direction,
                isActive = seekState.isActive
            )
        }
    }
}

/**
 * Speed indicator section with animated icons and progress
 */
@Composable
private fun SpeedIndicatorSection(
    speed: Float,
    direction: SeekDirection,
    speedLevel: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Animated direction icon
        AnimatedDirectionIcon(
            direction = direction,
            speed = speed
        )
        
        // Speed text and level indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${speed}x",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Speed level dots
            SpeedLevelIndicator(
                currentLevel = speedLevel,
                maxLevels = 6
            )
        }
        
        // Speed direction text
        Text(
            text = when (direction) {
                SeekDirection.FORWARD -> "Fast Forward"
                SeekDirection.BACKWARD -> "Rewind"
                SeekDirection.NONE -> "Seeking"
            },
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
    }
}

/**
 * Animated direction icon with rotation and scaling
 */
@Composable
private fun AnimatedDirectionIcon(
    direction: SeekDirection,
    speed: Float
) {
    val rotationAnimation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2000 / speed).toInt().coerceAtLeast(500),
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )
    
    val scaleAnimation by animateFloatAsState(
        targetValue = 1f + (speed - 1f) * 0.2f,
        animationSpec = tween(300),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        getDirectionColor(direction).copy(alpha = 0.3f),
                        getDirectionColor(direction).copy(alpha = 0.1f)
                    )
                ),
                shape = CircleShape
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getDirectionIcon(direction, speed.toInt()),
            contentDescription = null,
            tint = getDirectionColor(direction),
            modifier = Modifier
                .size(32.dp)
                .rotate(if (direction == SeekDirection.FORWARD) rotationAnimation else -rotationAnimation)
                .scale(scaleAnimation)
        )
    }
}

/**
 * Speed level indicator with animated dots
 */
@Composable
private fun SpeedLevelIndicator(
    currentLevel: Int,
    maxLevels: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(maxLevels) { index ->
            val isActive = index <= currentLevel
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.3f,
                animationSpec = tween(300),
                label = "alpha"
            )
            
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        Color.White.copy(alpha = animatedAlpha),
                        CircleShape
                    )
            )
        }
    }
}

/**
 * Position progress bar with seek preview
 */
@Composable
private fun PositionProgressBar(
    currentPosition: Long,
    duration: Long,
    seekPreviewPosition: Long
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val previewProgress = if (duration > 0) seekPreviewPosition.toFloat() / duration else 0f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(4.dp)
                )
        )
        
        // Current progress
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(
                    Color.White.copy(alpha = 0.6f),
                    RoundedCornerShape(4.dp)
                )
        )
        
        // Seek preview indicator
        if (previewProgress != progress) {
            val previewColor = if (previewProgress > progress) {
                Color.Green.copy(alpha = 0.8f)
            } else {
                Color.Red.copy(alpha = 0.8f)
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(previewProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        previewColor,
                        RoundedCornerShape(4.dp)
                    )
            )
        }
        
        // Current position indicator
        Box(
            modifier = Modifier
                .offset(x = (progress * 280).dp) // Approximate width calculation
                .size(12.dp)
                .background(Color.White, CircleShape)
                .align(Alignment.CenterStart)
        )
    }
}

/**
 * Time display section with current and remaining time
 */
@Composable
private fun TimeDisplaySection(
    currentPosition: Long,
    duration: Long,
    seekPreviewPosition: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Current time
        Text(
            text = formatTime(currentPosition),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        // Seek preview time (if different from current)
        if (seekPreviewPosition != currentPosition) {
            Text(
                text = "→ ${formatTime(seekPreviewPosition)}",
                color = if (seekPreviewPosition > currentPosition) Color.Green else Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Remaining time
        Text(
            text = "-${formatTime(duration - currentPosition)}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}

/**
 * Animated speed visualization rings
 */
@Composable
private fun AnimatedSpeedRings(
    speed: Float,
    direction: SeekDirection,
    isActive: Boolean
) {
    if (!isActive) return
    
    val ringCount = min(5, speed.toInt())
    val baseColor = getDirectionColor(direction)
    
    repeat(ringCount) { index ->
        val delay = index * 200
        val animatedScale by rememberInfiniteTransition(label = "ring_scale").animateFloat(
            initialValue = 0.5f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    delayMillis = delay,
                    easing = LinearOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring_scale"
        )
        
        val animatedAlpha by rememberInfiniteTransition(label = "ring_alpha").animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    delayMillis = delay,
                    easing = LinearOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring_alpha"
        )
        
        Box(
            modifier = Modifier
                .size((100 * animatedScale).dp)
                .background(
                    baseColor.copy(alpha = animatedAlpha * 0.3f),
                    CircleShape
                )
        )
    }
}

/**
 * Comprehensive long press overlay with all indicators
 */
@Composable
fun ComprehensiveLongPressOverlay(
    seekState: LongPressSeekState,
    viewModel: PlayerViewModel,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    
    // Update position and duration from viewModel
    LaunchedEffect(isVisible) {
        if (isVisible) {
            while (isVisible && seekState.isActive) {
                // In a real implementation, get these from viewModel
                currentPosition = seekState.currentPosition
                duration = 100000L // Placeholder
                delay(100)
            }
        }
    }
    
    AnimatedVisibility(
        visible = isVisible && seekState.isActive,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { it / 2 }
        ),
        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { it / 2 }
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Background blur effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
            
            // Main content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enhanced speed visualization
                EnhancedSpeedVisualization(
                    speed = seekState.seekSpeed,
                    direction = seekState.direction,
                    speedLevel = seekState.accelerationLevel
                )
                
                // Position and time information
                PositionInformationCard(
                    currentPosition = currentPosition,
                    duration = duration,
                    seekPreviewPosition = seekState.seekPreviewPosition,
                    direction = seekState.direction
                )
                
                // Speed progression indicator
                SpeedProgressionIndicator(
                    currentSpeed = seekState.seekSpeed,
                    maxSpeed = seekState.maxSpeed,
                    elapsedTime = seekState.elapsedTime
                )
            }
        }
    }
}

/**
 * Enhanced speed visualization with rotating elements
 */
@Composable
private fun EnhancedSpeedVisualization(
    speed: Float,
    direction: SeekDirection,
    speedLevel: Int
) {
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer rotating ring
        val outerRotation by rememberInfiniteTransition(label = "outer_rotation").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (3000 / speed).toInt().coerceAtLeast(500),
                    easing = LinearEasing
                )
            ),
            label = "outer_rotation"
        )
        
        // Inner pulsing circle
        val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (1000 / speed).toInt().coerceAtLeast(300),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        
        // Outer ring
        Box(
            modifier = Modifier
                .size(100.dp)
                .rotate(if (direction == SeekDirection.FORWARD) outerRotation else -outerRotation)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            getDirectionColor(direction).copy(alpha = 0.8f),
                            getDirectionColor(direction).copy(alpha = 0.2f),
                            getDirectionColor(direction).copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Inner circle
        Box(
            modifier = Modifier
                .size((60 * pulseScale).dp)
                .background(
                    getDirectionColor(direction).copy(alpha = 0.9f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = getDirectionIcon(direction, speedLevel),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "${speed}x",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Position information card with detailed time display
 */
@Composable
private fun PositionInformationCard(
    currentPosition: Long,
    duration: Long,
    seekPreviewPosition: Long,
    direction: SeekDirection
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Current time display
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Seek preview (if different)
            if (seekPreviewPosition != currentPosition) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (direction == SeekDirection.FORWARD) 
                            Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = getDirectionColor(direction),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formatTime(seekPreviewPosition),
                        color = getDirectionColor(direction),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Duration and remaining time
            Text(
                text = "/ ${formatTime(duration)}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Speed progression indicator showing acceleration timeline
 */
@Composable
private fun SpeedProgressionIndicator(
    currentSpeed: Float,
    maxSpeed: Float,
    elapsedTime: Long
) {
    val speedLevels = listOf(1f, 2f, 4f, 8f, 16f, 32f)
    val currentLevelIndex = speedLevels.indexOfFirst { it >= currentSpeed }.coerceAtLeast(0)
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        speedLevels.forEachIndexed { index, speed ->
            val isActive = index <= currentLevelIndex
            val isNext = index == currentLevelIndex + 1
            
            Box(
                modifier = Modifier
                    .size(if (isActive) 12.dp else 8.dp)
                    .background(
                        when {
                            isActive -> Color.White
                            isNext -> Color.White.copy(alpha = 0.5f)
                            else -> Color.White.copy(alpha = 0.2f)
                        },
                        CircleShape
                    )
            )
        }
    }
}

/**
 * Utility functions
 */
private fun getDirectionColor(direction: SeekDirection): Color {
    return when (direction) {
        SeekDirection.FORWARD -> Color(0xFF4CAF50) // Green
        SeekDirection.BACKWARD -> Color(0xFFFF9800) // Orange
        SeekDirection.NONE -> Color(0xFF2196F3) // Blue
    }
}

private fun getDirectionIcon(direction: SeekDirection, speedLevel: Int): ImageVector {
    return when (direction) {
        SeekDirection.FORWARD -> when {
            speedLevel >= 4 -> Icons.Default.FastForward
            speedLevel >= 2 -> Icons.Default.SkipNext
            else -> Icons.Default.PlayArrow
        }
        SeekDirection.BACKWARD -> when {
            speedLevel >= 4 -> Icons.Default.FastRewind
            speedLevel >= 2 -> Icons.Default.SkipPrevious
            else -> Icons.Default.Replay
        }
        SeekDirection.NONE -> Icons.Default.PlayArrow
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}