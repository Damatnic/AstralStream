package com.astralplayer.nextplayer.feature.player.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerUiState
import com.astralplayer.nextplayer.feature.player.gestures.SeekDirection

/**
 * Visual feedback overlays for gesture interactions
 */
@Composable
fun GestureOverlays(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        
        // Volume overlay (right side)
        VolumeOverlay(
            volume = viewModel.currentVolume,
            isVisible = uiState.showVolumeOverlay,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        
        // Brightness overlay (left side)
        BrightnessOverlay(
            brightness = viewModel.currentBrightness,
            isVisible = uiState.showBrightnessOverlay,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        
        // Seek preview overlay (center)
        SeekPreviewOverlay(
            isVisible = uiState.showSeekPreview,
            position = uiState.seekPreviewPosition,
            isForward = uiState.seekPreviewForward,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Long press seek overlay
        LongPressSeekOverlay(
            seekState = uiState.longPressSeekState,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Zoom level indicator
        ZoomOverlay(
            zoomLevel = uiState.zoomLevel,
            isVisible = uiState.showZoomOverlay,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun VolumeOverlay(
    volume: Float,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .width(60.dp)
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(30.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        volume == 0f -> Icons.Default.VolumeOff
                        volume < 0.5f -> Icons.Default.VolumeDown
                        else -> Icons.Default.VolumeUp
                    },
                    contentDescription = "Volume",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                // Volume bar
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .weight(1f)
                        .background(
                            Color.White.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(volume)
                            .background(
                                Color(0xFF00BCD4),
                                RoundedCornerShape(4.dp)
                            )
                            .align(Alignment.BottomCenter)
                    )
                }
                
                Text(
                    text = "${(volume * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BrightnessOverlay(
    brightness: Float,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .width(60.dp)
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(30.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        brightness < 0.3f -> Icons.Default.BrightnessLow
                        brightness < 0.7f -> Icons.Default.BrightnessMedium
                        else -> Icons.Default.BrightnessHigh
                    },
                    contentDescription = "Brightness",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                // Brightness bar
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .weight(1f)
                        .background(
                            Color.White.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(brightness)
                            .background(
                                Color(0xFFFFC107),
                                RoundedCornerShape(4.dp)
                            )
                            .align(Alignment.BottomCenter)
                    )
                }
                
                Text(
                    text = "${(brightness * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SeekPreviewOverlay(
    isVisible: Boolean,
    position: Long,
    isForward: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                    contentDescription = if (isForward) "Forward" else "Backward",
                    tint = Color(0xFF00BCD4),
                    modifier = Modifier.size(32.dp)
                )
                
                Text(
                    text = formatTime(position),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LongPressSeekOverlay(
    seekState: com.astralplayer.nextplayer.feature.player.gestures.LongPressSeekState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = seekState.isActive,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speed indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (seekState.direction) {
                            SeekDirection.FORWARD -> Icons.Default.FastForward
                            SeekDirection.BACKWARD -> Icons.Default.FastRewind
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = "Seek Direction",
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Text(
                        text = "${seekState.seekSpeed}x",
                        color = Color(0xFF00BCD4),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Position indicator
                Text(
                    text = formatTime(seekState.currentPosition),
                    color = Color.White,
                    fontSize = 16.sp
                )
                
                // Speed zones indicator
                if (seekState.showSpeedIndicator) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(5) { index ->
                            val speed = (index + 1) * 1f
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (seekState.seekSpeed >= speed) 
                                            Color(0xFF00BCD4) 
                                        else 
                                            Color.White.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomOverlay(
    zoomLevel: Float,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = "${(zoomLevel * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper function for time formatting
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