package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.data.gesture.LongPressSeekHandler
import com.astralplayer.nextplayer.data.SeekDirection

/**
 * MX Player/Next Player style long press seek overlay
 * Shows seeking direction and accumulated seek amount
 */
@Composable
fun DiscreteLongPressSpeedOverlay(
    speedInfo: LongPressSeekHandler.LongPressSeekInfo,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = speedInfo.isActive,
        enter = fadeIn() + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // MX Player/Next Player style indicator (speed or seek)
            if (speedInfo.totalSeekAmount == 0L && speedInfo.currentSpeed != 1.0f) {
                // This is center long press for speed control
                MxSpeedIndicator(
                    currentSpeed = speedInfo.currentSpeed,
                    modifier = Modifier
                )
            } else {
                // This is side long press for seeking
                MxSeekIndicator(
                    direction = speedInfo.direction,
                    totalSeekAmount = speedInfo.totalSeekAmount,
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
private fun MxSpeedIndicator(
    currentSpeed: Float,
    modifier: Modifier = Modifier
) {
    // Animated speed value for smooth transitions
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "speedTransition"
    )
    
    // Scale animation based on speed change
    val scaleAnimation by animateFloatAsState(
        targetValue = if (currentSpeed > 1.0f) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scaleAnimation"
    )
    
    Card(
        modifier = modifier.scale(scaleAnimation),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enhanced speed icon with multi-layer animation
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Background pulse
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
                
                // Rotating ring for speeds > 2x
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )
                
                if (currentSpeed > 2.0f) {
                    Canvas(modifier = Modifier.size(40.dp)) {
                        drawCircle(
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = pulseAlpha),
                            radius = size.minDimension / 2,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = when {
                        currentSpeed >= 4.0f -> androidx.compose.ui.graphics.Color(0xFFFF6B6B) // Red for very fast
                        currentSpeed >= 2.0f -> androidx.compose.ui.graphics.Color(0xFFFFD93D) // Yellow for fast
                        currentSpeed > 1.0f -> androidx.compose.ui.graphics.Color(0xFF6BCF7F) // Green for medium
                        else -> Color.White
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer {
                            if (currentSpeed > 2.0f) {
                                rotationZ = rotationAngle
                            }
                        }
                )
            }
            
            // Enhanced speed display with progress indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Animated speed text
                Text(
                    text = String.format("%.2fx", animatedSpeed),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Dynamic status text based on speed
                Text(
                    text = when {
                        currentSpeed >= 4.0f -> "ULTRA FAST"
                        currentSpeed >= 2.0f -> "FAST PLAYBACK"  
                        currentSpeed > 1.0f -> "SPEED UP"
                        currentSpeed < 1.0f -> "SLOW MOTION"
                        else -> "NORMAL SPEED"
                    },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp
                )
                
                // Speed level indicator (dots)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val speedLevels = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f)
                    val currentIndex = speedLevels.indexOfFirst { it >= currentSpeed }.takeIf { it >= 0 } ?: speedLevels.size - 1
                    
                    speedLevels.take(8).forEachIndexed { index, _ ->
                        val isActive = index <= currentIndex
                        val dotAlpha by animateFloatAsState(
                            targetValue = if (isActive) 1.0f else 0.3f,
                            animationSpec = tween(200),
                            label = "dotAlpha$index"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = dotAlpha))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MxSeekIndicator(
    direction: SeekDirection,
    totalSeekAmount: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Direction icon with pulsing animation
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            
            Icon(
                imageVector = if (direction == SeekDirection.FORWARD) 
                    Icons.Default.FastForward 
                else 
                    Icons.Default.FastRewind,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .scale(pulseScale)
            )
            
            // Seek amount text like MX Player/Next Player
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (totalSeekAmount >= 0) "+${totalSeekAmount / 1000}s" else "${totalSeekAmount / 1000}s",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "SEEKING",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Compact long press speed overlay for PiP mode
 */
@Composable
fun CompactLongPressSpeedOverlay(
    speedInfo: LongPressSeekHandler.LongPressSeekInfo,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = speedInfo.isActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(8.dp)
        ) {
            Icon(
                imageVector = if (speedInfo.direction == SeekDirection.FORWARD) 
                    Icons.Default.FastForward 
                else 
                    Icons.Default.FastRewind,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}