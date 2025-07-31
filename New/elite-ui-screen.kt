// EliteVideoPlayerScreen.kt
// The elite UI that integrates all advanced features into your existing player

package com.astralplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.astralplayer.gesture.EnhancedGestureController
import com.astralplayer.presentation.player.EnhancedVideoPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EliteVideoPlayerScreen(
    viewModel: EnhancedVideoPlayerViewModel,
    player: Player,
    gestureController: EnhancedGestureController,
    onBackPressed: () -> Unit
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val subtitles by viewModel.subtitles.collectAsStateWithLifecycle()
    val gestureState by gestureController.gestureState.collectAsStateWithLifecycle()
    
    var showControls by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls) {
        if (showControls && playerState.isPlaying) {
            delay(3000)
            showControls = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        // Handle double tap for seek
                        val screenWidth = size.width
                        when {
                            offset.x < screenWidth * 0.3f -> {
                                // Seek backward 10 seconds
                                player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                                viewModel.showSeekAnimation(-10)
                            }
                            offset.x > screenWidth * 0.7f -> {
                                // Seek forward 10 seconds
                                player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration))
                                viewModel.showSeekAnimation(10)
                            }
                            else -> {
                                // Toggle play/pause
                                if (player.isPlaying) player.pause() else player.play()
                            }
                        }
                    }
                )
            }
    ) {
        // Video player view
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = false // We're using custom controls
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Gesture overlays
        AnimatedVisibility(
            visible = gestureState.currentGesture != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            GestureOverlay(gestureState)
        }
        
        // Subtitles
        AnimatedVisibility(
            visible = subtitles.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SubtitleDisplay(
                subtitles = subtitles,
                currentPosition = playerState.currentPosition
            )
        }
        
        // Control overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            ) {
                // Top controls
                TopControls(
                    title = "Premium Video",
                    onBackPressed = onBackPressed,
                    onSettingsClick = { showSettings = true },
                    modifier = Modifier.align(Alignment.TopStart)
                )
                
                // Center controls
                CenterControls(
                    isPlaying = playerState.isPlaying,
                    onPlayPauseClick = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // Bottom controls
                BottomControls(
                    currentPosition = playerState.currentPosition,
                    duration = playerState.duration,
                    bufferedPercentage = playerState.bufferedPercentage,
                    onSeek = { position -> player.seekTo(position) },
                    onSubtitleClick = { showSubtitleMenu = true },
                    onQualityClick = { showQualityMenu = true },
                    onFullscreenClick = { /* Handle fullscreen */ },
                    playbackSpeed = playerState.playbackSpeed,
                    onSpeedChange = { speed -> player.setPlaybackSpeed(speed) },
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
        
        // Settings menu
        if (showSettings) {
            SettingsDialog(
                onDismiss = { showSettings = false },
                viewModel = viewModel
            )
        }
        
        // Subtitle menu
        if (showSubtitleMenu) {
            SubtitleMenu(
                onDismiss = { showSubtitleMenu = false },
                onGenerateSubtitles = {
                    scope.launch {
                        viewModel.generateSubtitles()
                    }
                },
                onLoadSubtitles = {
                    // Handle subtitle file loading
                }
            )
        }
        
        // Quality menu
        if (showQualityMenu) {
            QualityMenu(
                currentQuality = playerState.quality,
                onQualitySelected = { quality ->
                    viewModel.setVideoQuality(quality)
                    showQualityMenu = false
                },
                onDismiss = { showQualityMenu = false }
            )
        }
    }
}

@Composable
private fun TopControls(
    title: String,
    onBackPressed: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        )
        
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CenterControls(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = isPlaying, label = "play_pause")
    
    val scale by transition.animateFloat(label = "scale") { playing ->
        if (playing) 1f else 1.2f
    }
    
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { onPlayPauseClick() }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun BottomControls(
    currentPosition: Long,
    duration: Long,
    bufferedPercentage: Int,
    onSeek: (Long) -> Unit,
    onSubtitleClick: () -> Unit,
    onQualityClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Progress bar
        VideoProgressBar(
            currentPosition = currentPosition,
            duration = duration,
            bufferedPercentage = bufferedPercentage,
            onSeek = onSeek
        )
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time display
            Text(
                text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            
            Row {
                // Subtitle button
                IconButton(onClick = onSubtitleClick) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Subtitles",
                        tint = Color.White
                    )
                }
                
                // Quality button
                IconButton(onClick = onQualityClick) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = "Quality",
                        tint = Color.White
                    )
                }
                
                // Speed button
                SpeedButton(
                    currentSpeed = playbackSpeed,
                    onSpeedChange = onSpeedChange
                )
                
                // Fullscreen button
                IconButton(onClick = onFullscreenClick) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPercentage: Int,
    onSeek: (Long) -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0f) }
    
    val progress = if (duration > 0) {
        currentPosition.toFloat() / duration.toFloat()
    } else 0f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newPosition = (offset.x / size.width) * duration
                    onSeek(newPosition.toLong())
                }
            }
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
        )
        
        // Buffered progress
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedPercentage / 100f)
                .height(4.dp)
                .align(Alignment.CenterStart)
                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        )
        
        // Current progress
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isSeeking) seekPosition else progress)
                .height(4.dp)
                .align(Alignment.CenterStart)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        
        // Seek handle
        Box(
            modifier = Modifier
                .offset(x = LocalDensity.current.run {
                    ((if (isSeeking) seekPosition else progress) * 
                    (size.width.toDp() - 16.dp)).coerceIn(0.dp, size.width.toDp() - 16.dp)
                })
                .size(16.dp)
                .align(Alignment.CenterStart)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}

@Composable
private fun GestureOverlay(gestureState: EnhancedGestureController.GestureState) {
    when (gestureState.currentGesture) {
        EnhancedGestureController.Gesture.SWIPE_UP -> {
            VolumeIndicator(increase = true)
        }
        EnhancedGestureController.Gesture.SWIPE_DOWN -> {
            VolumeIndicator(increase = false)
        }
        EnhancedGestureController.Gesture.SWIPE_LEFT -> {
            SeekIndicator(forward = false)
        }
        EnhancedGestureController.Gesture.SWIPE_RIGHT -> {
            SeekIndicator(forward = true)
        }
        EnhancedGestureController.Gesture.PINCH_ZOOM -> {
            ZoomIndicator(zoomLevel = gestureState.zoomLevel)
        }
        else -> {}
    }
}

@Composable
private fun SubtitleDisplay(
    subtitles: List<SubtitleEntry>,
    currentPosition: Long
) {
    val currentSubtitle = subtitles.find { subtitle ->
        currentPosition in subtitle.startTime..subtitle.endTime
    }
    
    AnimatedContent(
        targetState = currentSubtitle,
        transitionSpec = {
            fadeIn() with fadeOut()
        }
    ) { subtitle ->
        if (subtitle != null) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                Text(
                    text = subtitle.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}