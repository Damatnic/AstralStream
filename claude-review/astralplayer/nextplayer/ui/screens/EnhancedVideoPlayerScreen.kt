package com.astralplayer.nextplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astralplayer.nextplayer.data.TouchSide
import com.astralplayer.nextplayer.data.SeekDirection
import com.astralplayer.nextplayer.data.GestureType
import com.astralplayer.nextplayer.data.PlayerUiState
import com.astralplayer.nextplayer.viewmodel.EnhancedPlayerViewModel
import kotlinx.coroutines.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

@OptIn(UnstableApi::class)
@Composable
fun EnhancedVideoPlayerScreen(
    playerView: PlayerView,
    viewModel: EnhancedPlayerViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val playerState by viewModel.playerState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val overlayVisibility by viewModel.overlayVisibility.collectAsState()
    val gestureSettings by viewModel.gestureSettings.collectAsState()
    
    var showControls by remember { mutableStateOf(true) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls, playerState.isPlaying) {
        if (showControls && playerState.isPlaying) {
            delay(3000)
            showControls = false
        }
    }
    
    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onStart()
                Lifecycle.Event.ON_STOP -> viewModel.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Player View with gesture detection
        AndroidView(
            factory = { _ ->
                playerView.apply {
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = uiState.zoomLevel
                    scaleY = uiState.zoomLevel
                }
                .pointerInput(Unit) {
                    val gestureCallbacks = viewModel.getGestureCallbacks()
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1f) {
                            gestureCallbacks.onPinchZoom(zoom, androidx.compose.ui.geometry.Offset.Zero)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = { offset ->
                            val side = when {
                                offset.x < size.width * 0.3f -> TouchSide.LEFT
                                offset.x > size.width * 0.7f -> TouchSide.RIGHT
                                else -> TouchSide.CENTER
                            }
                            viewModel.getGestureCallbacks().onDoubleTap(offset, side)
                        },
                        onLongPress = { offset ->
                            viewModel.getGestureCallbacks().onLongPressStart(offset)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            viewModel.getGestureCallbacks().onLongPressEnd()
                        }
                    ) { _, dragAmount ->
                        val isHorizontal = kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)
                        
                        if (isHorizontal && gestureSettings.seeking.isEnabled) {
                            viewModel.getGestureCallbacks().onHorizontalSeek(dragAmount.x, 0f)
                        } else {
                            val side = when {
                                dragAmount.x < size.width * 0.5f -> TouchSide.LEFT
                                else -> TouchSide.RIGHT
                            }
                            
                            if (side == TouchSide.LEFT && gestureSettings.brightness.isEnabled) {
                                viewModel.getGestureCallbacks().onVerticalBrightnessChange(-dragAmount.y, side)
                            } else if (side == TouchSide.RIGHT && gestureSettings.volume.isEnabled) {
                                viewModel.getGestureCallbacks().onVerticalVolumeChange(-dragAmount.y, side)
                            }
                        }
                    }
                }
        )
        
        // Loading indicator
        if (playerState.isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Gesture overlays
        AnimatedVisibility(
            visible = overlayVisibility.seekPreview,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            uiState.seekPreviewInfo?.let { info ->
                SeekPreviewOverlay(info)
            }
        }
        
        AnimatedVisibility(
            visible = overlayVisibility.volume,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            uiState.volumeInfo?.let { info ->
                VolumeOverlay(info)
            }
        }
        
        AnimatedVisibility(
            visible = overlayVisibility.brightness,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            uiState.brightnessInfo?.let { info ->
                BrightnessOverlay(info)
            }
        }
        
        AnimatedVisibility(
            visible = overlayVisibility.doubleTap,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            uiState.doubleTapInfo?.let { info ->
                DoubleTapIndicator(info)
            }
        }
        
        AnimatedVisibility(
            visible = overlayVisibility.longPress,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            uiState.longPressSeekInfo?.let { info ->
                LongPressOverlay(info)
            }
        }
        
        // Controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            PlayerControls(
                playerState = playerState,
                onBack = onBack,
                onPlayPause = { viewModel.togglePlayPause() },
                onSeekForward = { viewModel.seekRelative(10000) },
                onSeekBackward = { viewModel.seekRelative(-10000) },
                onSeek = { viewModel.seekTo(it) },
                onSpeedClick = { showSpeedDialog = true },
                onSubtitleClick = { showSubtitleDialog = true },
                onQualityClick = { showQualityDialog = true },
                onSettingsClick = { showSettingsDialog = true }
            )
        }
        
        // Dialogs
        if (showSpeedDialog) {
            SpeedSelectionDialog(
                currentSpeed = playerState.playbackSpeed,
                onSpeedSelected = { speed ->
                    viewModel.setPlaybackSpeed(speed)
                    showSpeedDialog = false
                },
                onDismiss = { showSpeedDialog = false }
            )
        }
        
        if (showSettingsDialog) {
            PlayerSettingsDialog(
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
private fun PlayerControls(
    playerState: PlayerUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onQualityClick: () -> Unit,
    onSettingsClick: () -> Unit
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
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Text(
                text = playerState.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Video",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            
            // Quick settings
            IconButton(onClick = onSubtitleClick) {
                Icon(
                    imageVector = Icons.Default.ClosedCaption,
                    contentDescription = "Subtitles",
                    tint = Color.White
                )
            }
            
            IconButton(onClick = onQualityClick) {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = "Quality",
                    tint = Color.White
                )
            }
            
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Settings",
                    tint = Color.White
                )
            }
        }
        
        // Center controls
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            IconButton(
                onClick = { /* Handle previous */ },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Rewind button
            IconButton(
                onClick = onSeekBackward,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Rewind 10 seconds",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Play/Pause button
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            // Forward button
            IconButton(
                onClick = onSeekForward,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Forward 10 seconds",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Next button
            IconButton(
                onClick = { /* Handle next */ },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            // Time and duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(playerState.currentPosition),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatTime(playerState.duration),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Seek bar
            Slider(
                value = if (playerState.duration > 0) {
                    playerState.currentPosition.toFloat() / playerState.duration.toFloat()
                } else 0f,
                onValueChange = { value ->
                    val position = (value * playerState.duration).toLong()
                    onSeek(position)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            
            // Additional controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Speed button
                TextButton(
                    onClick = onSpeedClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${playerState.playbackSpeed}x",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Loop button
                IconButton(onClick = { /* Handle loop */ }) {
                    Icon(
                        imageVector = Icons.Default.Loop,
                        contentDescription = "Loop",
                        tint = Color.White
                    )
                }
                
                // Picture-in-picture
                IconButton(onClick = { /* Handle PiP */ }) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Picture in Picture",
                        tint = Color.White
                    )
                }
                
                // Cast button
                IconButton(onClick = { /* Handle cast */ }) {
                    Icon(
                        imageVector = Icons.Default.CastConnected,
                        contentDescription = "Cast",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SeekPreviewOverlay(info: com.astralplayer.nextplayer.data.gesture.HorizontalSeekGestureHandler.SeekPreviewInfo) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (info.seekDelta > 0) "+${info.seekDelta / 1000}s" else "${info.seekDelta / 1000}s",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = formatTime(info.seekPosition),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun VolumeOverlay(info: com.astralplayer.nextplayer.data.gesture.VerticalGestureHandler.VolumeInfo) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (info.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = info.percentage,
                    modifier = Modifier
                        .width(100.dp)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(info.percentage * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BrightnessOverlay(info: com.astralplayer.nextplayer.data.gesture.VerticalGestureHandler.BrightnessInfo) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Brightness7,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = info.percentage,
                    modifier = Modifier
                        .width(100.dp)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(info.percentage * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DoubleTapIndicator(info: EnhancedPlayerViewModel.DoubleTapInfo) {
    val alignment = when (info.side) {
        TouchSide.LEFT -> Alignment.CenterStart
        TouchSide.RIGHT -> Alignment.CenterEnd
        TouchSide.CENTER -> Alignment.Center
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier.padding(48.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            ),
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (info.side == TouchSide.RIGHT) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "${info.seekAmount / 1000}s",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun LongPressOverlay(info: com.astralplayer.nextplayer.data.gesture.LongPressSeekHandler.LongPressSeekInfo) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Fast ${if (info.direction == SeekDirection.FORWARD) "Forward" else "Rewind"}",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${info.currentSpeed}x",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${info.totalSeekAmount / 1000}s",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed") },
        text = {
            Column {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedSelected(speed) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSpeed == speed,
                            onClick = { onSpeedSelected(speed) }
                        )
                        Text(
                            text = "${speed}x",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PlayerSettingsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Player Settings") },
        text = {
            Column {
                // Add settings options here
                Text("• Audio Track")
                Text("• Subtitle Style")
                Text("• Video Filters")
                Text("• Aspect Ratio")
                Text("• Loop Mode")
                Text("• Sleep Timer")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
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

