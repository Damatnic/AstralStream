package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.data.gesture.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Seek preview overlay with thumbnail support
 */
@Composable
fun SeekPreviewOverlay(
    seekInfo: HorizontalSeekGestureHandler.SeekPreviewInfo,
    currentPosition: Long,
    videoDuration: Long,
    thumbnailUrl: String? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = seekInfo.isDragging,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.8f),
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            // Thumbnail preview
            if (seekInfo.showThumbnail && thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 160.dp, height = 90.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Time indicator
            if (seekInfo.showTimeIndicator) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(seekInfo.seekPosition),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Icon(
                        imageVector = if (seekInfo.seekPosition > currentPosition) 
                            Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = "${if (seekInfo.seekPosition > currentPosition) "+" else ""}${formatTime(seekInfo.seekPosition - currentPosition)}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                
                // Progress bar
                LinearProgressIndicator(
                    progress = { seekInfo.seekPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Volume overlay with visual indicator
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VolumeOverlay(
    volumeInfo: VerticalGestureHandler.VolumeInfo,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.8f),
                    RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Volume icon
                Icon(
                    imageVector = when {
                        volumeInfo.isMuted -> Icons.Default.VolumeOff
                        volumeInfo.percentage < 0.3f -> Icons.Default.VolumeMute
                        volumeInfo.percentage < 0.7f -> Icons.Default.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                
                // Volume level bars
                VolumeIndicator(
                    level = volumeInfo.percentage,
                    modifier = Modifier.size(width = 48.dp, height = 120.dp)
                )
                
                // Percentage text
                Text(
                    text = "${(volumeInfo.percentage * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Brightness overlay with visual indicator
 */
@Composable
fun BrightnessOverlay(
    brightnessInfo: VerticalGestureHandler.BrightnessInfo,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.8f),
                    RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Brightness icon
                Icon(
                    imageVector = when {
                        brightnessInfo.percentage < 0.3f -> Icons.Default.BrightnessLow
                        brightnessInfo.percentage < 0.7f -> Icons.Default.BrightnessMedium
                        else -> Icons.Default.BrightnessHigh
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                
                // Brightness sun indicator
                BrightnessIndicator(
                    level = brightnessInfo.percentage,
                    modifier = Modifier.size(120.dp)
                )
                
                // Percentage text
                Text(
                    text = "${(brightnessInfo.percentage * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (brightnessInfo.isAutoBrightness) {
                    Text(
                        text = "Auto",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Long press seek overlay with speed indicator
 */
@Composable
fun LongPressSeekOverlay(
    seekInfo: LongPressSeekHandler.LongPressSeekInfo,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = seekInfo.isActive,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.85f),
                    CircleShape
                )
                .size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speed indicator
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                
                // Speed text with animation
                AnimatedContent(
                    targetState = seekInfo.formattedSpeed,
                    transitionSpec = {
                        slideInVertically { it } + fadeIn() togetherWith
                        slideOutVertically { -it } + fadeOut()
                    }
                ) { speed ->
                    Text(
                        text = speed,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Speed progression indicator
                SpeedProgressionIndicator(
                    currentIndex = seekInfo.currentSpeedIndex,
                    maxIndex = seekInfo.maxSpeedIndex,
                    speedProgression = seekInfo.speedProgression,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Next speed hint
                seekInfo.nextSpeed?.let { nextSpeed ->
                    Text(
                        text = "→ ${nextSpeed}x",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Double tap seek indicator
 */
@Composable
fun DoubleTapSeekIndicator(
    side: TouchSide,
    seekAmount: Long,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        val isForward = side == TouchSide.RIGHT
        
        Box(
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    CircleShape
                )
                .size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "${if (isForward) "+" else "-"}${seekAmount / 1000}s",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Custom volume level indicator
 */
@Composable
private fun VolumeIndicator(
    level: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = 10
        val barHeight = size.height / barCount
        val barWidth = size.width * 0.7f
        val spacing = barHeight * 0.2f
        
        for (i in 0 until barCount) {
            val barLevel = 1f - (i.toFloat() / barCount)
            val isActive = level >= barLevel
            
            drawRoundRect(
                color = if (isActive) Color.White else Color.White.copy(alpha = 0.3f),
                topLeft = Offset(
                    x = (size.width - barWidth) / 2,
                    y = i * barHeight + spacing / 2
                ),
                size = Size(barWidth, barHeight - spacing),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
        }
    }
}

/**
 * Custom brightness sun indicator
 */
@Composable
private fun BrightnessIndicator(
    level: Float,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level,
        animationSpec = tween(300)
    )
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 4
        
        // Draw sun
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center
        )
        
        // Draw rays
        val rayCount = 12
        val rayLength = radius * 0.8f * animatedLevel
        
        for (i in 0 until rayCount) {
            val angle = (i * 360f / rayCount) * PI / 180
            val startOffset = Offset(
                x = center.x + cos(angle).toFloat() * (radius + 10),
                y = center.y + sin(angle).toFloat() * (radius + 10)
            )
            val endOffset = Offset(
                x = center.x + cos(angle).toFloat() * (radius + 10 + rayLength),
                y = center.y + sin(angle).toFloat() * (radius + 10 + rayLength)
            )
            
            drawLine(
                color = Color.White.copy(alpha = animatedLevel),
                start = startOffset,
                end = endOffset,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Speed progression indicator
 */
@Composable
private fun SpeedProgressionIndicator(
    currentIndex: Int,
    maxIndex: Int,
    speedProgression: List<Float>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        speedProgression.forEachIndexed { index, speed ->
            val isActive = index <= currentIndex
            val isCurrent = index == currentIndex
            
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 12.dp else 8.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary 
                               else Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
            
            if (index < speedProgression.size - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

/**
 * Pinch zoom overlay
 */
@Composable
fun PinchZoomOverlay(
    zoomLevel: Float,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (zoomLevel > 1f) Icons.Default.ZoomIn else Icons.Default.ZoomOut,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = String.format("%.1fx", zoomLevel),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Gesture conflict indicator
 */
@Composable
fun GestureConflictIndicator(
    conflictingGestures: List<GestureType>,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Gesture Conflict Detected",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = conflictingGestures.joinToString(" vs ") { it.name },
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Formats time in seconds to readable format
 */
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