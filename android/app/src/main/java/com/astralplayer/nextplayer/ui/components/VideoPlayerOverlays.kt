package com.astralplayer.nextplayer.ui.components

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Video Player Overlay Components for AstralStream
 * Modern bubble-themed overlays for video playback feedback
 */

// ============= GESTURE FEEDBACK OVERLAYS =============

@Composable
fun VolumeOverlay(
    volume: Float,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + slideInHorizontally(),
        exit = fadeOut(animationSpec = tween(500)) + slideOutHorizontally(),
        modifier = modifier
    ) {
        GestureIndicatorCard(
            icon = when {
                volume == 0f -> Icons.Default.VolumeOff
                volume < 0.33f -> Icons.Default.VolumeMute
                volume < 0.66f -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
            },
            value = volume,
            label = "Volume",
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun BrightnessOverlay(
    brightness: Float,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + slideInHorizontally { it },
        exit = fadeOut(animationSpec = tween(500)) + slideOutHorizontally { it },
        modifier = modifier
    ) {
        GestureIndicatorCard(
            icon = when {
                brightness < 0.33f -> Icons.Default.BrightnessLow
                brightness < 0.66f -> Icons.Default.BrightnessMedium
                else -> Icons.Default.BrightnessHigh
            },
            value = brightness,
            label = "Brightness",
            color = Color(0xFFFFB74D) // Orange for brightness
        )
    }
}

@Composable
fun SeekOverlay(
    currentTime: Long,
    seekTime: Long,
    totalDuration: Long,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val seekDelta = seekTime - currentTime
    val targetTime = seekTime.coerceIn(0, totalDuration)
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) + scaleIn(),
        exit = fadeOut(animationSpec = tween(500)) + scaleOut(),
        modifier = modifier
    ) {
        BubbleCard(
            elevation = 12,
            cornerRadius = 24,
            containerColor = Color.Black.copy(alpha = 0.85f)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Seek Direction Icon
                Icon(
                    imageVector = when {
                        seekDelta > 0 -> Icons.Default.FastForward
                        seekDelta < 0 -> Icons.Default.FastRewind
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                // Seek Amount
                Text(
                    text = "${if (seekDelta > 0) "+" else ""}${seekDelta / 1000}s",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                // Target Time
                Text(
                    text = formatTime(targetTime),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                
                // Progress Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(targetTime.toFloat() / totalDuration.toFloat())
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedOverlay(
    speed: Float,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        BubbleCard(
            elevation = 12,
            cornerRadius = 20,
            containerColor = Color.Black.copy(alpha = 0.85f)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Text(
                    text = "${speed}x",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============= CONTROL LOCK OVERLAY =============

@Composable
fun ControlLockOverlay(
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLocked,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.TopEnd
        ) {
            BubbleIconButton(
                onClick = onToggleLock,
                icon = Icons.Default.Lock,
                size = 48,
                iconSize = 24,
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

// ============= BUFFERING OVERLAY =============

@Composable
fun BufferingOverlay(
    isBuffering: Boolean,
    bufferProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isBuffering,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            BubbleCard(
                elevation = 12,
                cornerRadius = 24,
                containerColor = Color.Black.copy(alpha = 0.8f)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    
                    Text(
                        text = "Buffering...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    
                    if (bufferProgress > 0) {
                        Text(
                            text = "${(bufferProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ============= PLAYBACK ERROR OVERLAY =============

@Composable
fun PlaybackErrorOverlay(
    error: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            BubbleCard(
                elevation = 12,
                cornerRadius = 24,
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Text(
                        text = "Playback Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    
                    error?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BubbleButton(
                            onClick = onRetry,
                            text = "Retry",
                            icon = Icons.Default.Refresh,
                            cornerRadius = 20
                        )
                        
                        BubbleButton(
                            onClick = onDismiss,
                            text = "Dismiss",
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            cornerRadius = 20
                        )
                    }
                }
            }
        }
    }
}

// ============= SUBTITLE OVERLAY =============

@Composable
fun SubtitleOverlay(
    subtitle: String?,
    style: SubtitleStyle = SubtitleStyle.Default,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !subtitle.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 64.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            subtitle?.let { text ->
                Box(
                    modifier = Modifier
                        .background(
                            color = style.backgroundColor,
                            shape = RoundedCornerShape(style.cornerRadius)
                        )
                        .padding(
                            horizontal = style.horizontalPadding,
                            vertical = style.verticalPadding
                        )
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = style.fontSize,
                            fontWeight = if (style.isBold) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = style.textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.blur(style.shadowBlur)
                    )
                }
            }
        }
    }
}

// ============= DOUBLE TAP SEEK OVERLAY =============

@Composable
fun DoubleTapSeekOverlay(
    seekAmount: Int,
    isForward: Boolean,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        val animatedScale by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        
        Box(
            modifier = modifier
                .scale(animatedScale)
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                
                Text(
                    text = "${if (isForward) "+" else "-"}${seekAmount}s",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============= HELPER COMPONENTS =============

@Composable
private fun GestureIndicatorCard(
    icon: ImageVector,
    value: Float,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    BubbleCard(
        modifier = modifier,
        elevation = 12,
        cornerRadius = 16,
        containerColor = Color.Black.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            
            // Vertical progress bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(value)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    color,
                                    color.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
            }
            
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============= DATA CLASSES =============

data class SubtitleStyle(
    val fontSize: sp = 18.sp,
    val textColor: Color = Color.White,
    val backgroundColor: Color = Color.Black.copy(alpha = 0.7f),
    val isBold: Boolean = false,
    val cornerRadius: dp = 4.dp,
    val horizontalPadding: dp = 12.dp,
    val verticalPadding: dp = 6.dp,
    val shadowBlur: dp = 0.dp
) {
    companion object {
        val Default = SubtitleStyle()
        val Large = SubtitleStyle(fontSize = 22.sp, isBold = true)
        val Transparent = SubtitleStyle(backgroundColor = Color.Transparent)
    }
}

// ============= UTILITY FUNCTIONS =============

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