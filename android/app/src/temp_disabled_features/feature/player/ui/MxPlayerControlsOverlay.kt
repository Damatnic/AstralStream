package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerUiState
import kotlinx.coroutines.delay

@Composable
fun MxPlayerControlsOverlay(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    @Suppress("UNUSED_PARAMETER") gestureSettings: com.astralplayer.nextplayer.feature.player.gestures.GestureSettings = com.astralplayer.nextplayer.feature.player.gestures.GestureSettings(),
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onBack: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAdvancedSettings: () -> Unit = {},
    onLockScreen: () -> Unit,
    onShowSubtitles: () -> Unit,
    onShowQuality: () -> Unit,
    onShowAudioTracks: () -> Unit,
    onShowMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationState = remember { MutableTransitionState(false) }
    
    LaunchedEffect(uiState.areControlsVisible) {
        animationState.targetState = uiState.areControlsVisible
    }
    
    AnimatedVisibility(
        visibleState = animationState,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically { it / 4 },
        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically { it / 4 }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        ) {
            // Top control bar - MX Player style
            MxPlayerTopBar(
                title = uiState.videoTitle,
                onBack = onBack,
                onSettings = onShowSettings,
                onLock = onLockScreen,
                modifier = Modifier.align(Alignment.TopStart)
            )
            
            // Center playback controls
            MxPlayerCenterControls(
                isPlaying = uiState.isPlaying,
                onPlayPause = onPlayPauseClick,
                onSeekBackward = onSeekBackward,
                onSeekForward = onSeekForward,
                modifier = Modifier.align(Alignment.Center)
            )
            
            // Bottom control bar
            MxPlayerBottomBar(
                uiState = uiState,
                onSeekTo = onSeekTo,
                onShowSubtitles = onShowSubtitles,
                onShowQuality = onShowQuality,
                onShowAudioTracks = onShowAudioTracks,
                onShowMoreOptions = onShowMoreOptions,
                onSpeedChange = { viewModel.cyclePlaybackSpeed() },
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun MxPlayerTopBar(
    title: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MxPlayerControlButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 200.dp)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MxPlayerControlButton(
                onClick = onSettings,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            MxPlayerControlButton(
                onClick = onLock,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Lock Screen",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MxPlayerCenterControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonScale by animateFloatAsState(
        targetValue = if (isPlaying) 0.9f else 1f,
        animationSpec = tween(200),
        label = "button_scale"
    )
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Seek backward
        MxPlayerControlButton(
            onClick = onSeekBackward,
            modifier = Modifier
                .size(64.dp)
                .alpha(0.9f)
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = "Seek backward 10s",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Play/Pause - MX Player style large circular button
        MxPlayerControlButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size((80 * buttonScale).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFF7C3AED),
                            Color(0xFF5B21B6)
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        
        // Seek forward
        MxPlayerControlButton(
            onClick = onSeekForward,
            modifier = Modifier
                .size(64.dp)
                .alpha(0.9f)
        ) {
            Icon(
                imageVector = Icons.Filled.Forward10,
                contentDescription = "Seek forward 10s",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun MxPlayerBottomBar(
    uiState: PlayerUiState,
    onSeekTo: (Long) -> Unit,
    onShowSubtitles: () -> Unit,
    onShowQuality: () -> Unit,
    onShowAudioTracks: () -> Unit,
    onShowMoreOptions: () -> Unit,
    onSpeedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Quick action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MxPlayerQuickButton(
                icon = Icons.Filled.Speed,
                label = "${uiState.playbackSpeed}x",
                onClick = onSpeedChange
            )
            MxPlayerQuickButton(
                icon = Icons.Filled.Subtitles,
                label = "CC",
                onClick = onShowSubtitles
            )
            MxPlayerQuickButton(
                icon = Icons.Filled.HighQuality,
                label = "Quality",
                onClick = onShowQuality
            )
            MxPlayerQuickButton(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = "Audio",
                onClick = onShowAudioTracks
            )
            MxPlayerQuickButton(
                icon = Icons.Filled.MoreVert,
                label = "More",
                onClick = onShowMoreOptions
            )
        }
        
        // Progress bar - MX Player style
        MxPlayerSeekBar(
            value = if (uiState.duration > 0) {
                uiState.currentPosition.toFloat() / uiState.duration.toFloat()
            } else 0f,
            onValueChange = { progress ->
                val newPosition = (progress * uiState.duration).toLong()
                onSeekTo(newPosition)
            },
            bufferedPercentage = uiState.bufferedPercentage / 100f,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Time display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(uiState.currentPosition),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTime(uiState.duration),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun MxPlayerQuickButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun MxPlayerControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    @Suppress("UNUSED_VARIABLE")
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(100),
        label = "button_press_scale"
    )
    
    Box(
        modifier = modifier
            .clickable {
                onClick()
            }
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.3f),
                        Color(0xFF7C3AED).copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun MxPlayerSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    bufferedPercentage: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.height(28.dp)) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
                .background(
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        
        // Buffered track
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedPercentage.coerceIn(0f, 1f))
                .height(4.dp)
                .align(Alignment.CenterStart)
                .background(
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        
        // Progress track
        Box(
            modifier = Modifier
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .height(4.dp)
                .align(Alignment.CenterStart)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFF7C3AED)
                        )
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        
        // Seek thumb
        Box(
            modifier = Modifier
                .offset(x = (value * (modifier.toString().length * 8)).dp) // Approximate calculation
                .size(14.dp)
                .align(Alignment.CenterStart)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFF5B21B6)
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Invisible slider for interaction
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0f),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
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