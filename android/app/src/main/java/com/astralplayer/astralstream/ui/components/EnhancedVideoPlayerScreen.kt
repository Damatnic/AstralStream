package com.astralplayer.astralstream.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.astralplayer.astralstream.ai.AISubtitleGenerator
import com.astralplayer.astralstream.gesture.EnhancedGestureManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedVideoPlayerScreen(
    player: ExoPlayer,
    videoUri: Uri,
    videoTitle: String,
    isFullscreen: Boolean,
    showControls: Boolean,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    subtitles: List<AISubtitleGenerator.Subtitle>,
    playbackSpeed: Float,
    videoQuality: String,
    gestureManager: EnhancedGestureManager,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onFullscreenToggle: () -> Unit,
    onControlsVisibilityChanged: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onQualityChange: (String) -> Unit,
    onSubtitleToggle: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    
    // UI State
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf<Float?>(null) }
    var volumeLevel by remember { mutableStateOf(1f) }
    var brightnessLevel by remember { mutableStateOf(0.5f) }
    
    // Gesture State
    var gestureZone by remember { mutableStateOf<EnhancedGestureManager.Zone?>(null) }
    var gestureType by remember { mutableStateOf<GestureType?>(null) }
    var gestureProgress by remember { mutableStateOf(0f) }
    
    // Animation State
    val controlsAlpha by animateFloatAsState(
        targetValue = if (showControls) 1f else 0f,
        animationSpec = tween(300)
    )
    
    val gestureIndicatorAlpha by animateFloatAsState(
        targetValue = if (gestureType != null) 1f else 0f,
        animationSpec = tween(200)
    )
    
    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            onControlsVisibilityChanged(false)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onControlsVisibilityChanged(!showControls)
                    },
                    onDoubleTap = { offset ->
                        val zone = when {
                            offset.x < size.width / 3 -> EnhancedGestureManager.Zone.LEFT
                            offset.x > 2 * size.width / 3 -> EnhancedGestureManager.Zone.RIGHT
                            else -> EnhancedGestureManager.Zone.CENTER
                        }
                        
                        // Double tap to seek
                        when (zone) {
                            EnhancedGestureManager.Zone.LEFT -> {
                                val newPosition = (currentPosition - 10000).coerceAtLeast(0)
                                onSeekTo(newPosition.toFloat())
                            }
                            EnhancedGestureManager.Zone.RIGHT -> {
                                val newPosition = (currentPosition + 10000).coerceAtMost(duration)
                                onSeekTo(newPosition.toFloat())
                            }
                            else -> {}
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val x = change.position.x
                    val y = change.position.y
                    val zone = when {
                        x < size.width / 3 -> EnhancedGestureManager.Zone.LEFT
                        x > 2 * size.width / 3 -> EnhancedGestureManager.Zone.RIGHT
                        else -> EnhancedGestureManager.Zone.CENTER
                    }
                    
                    // Vertical swipes for brightness/volume
                    val deltaY = change.previousPosition.y - y
                    if (abs(deltaY) > 5) {
                        when (zone) {
                            EnhancedGestureManager.Zone.LEFT -> {
                                // Brightness control
                                brightnessLevel = (brightnessLevel + deltaY / size.height).coerceIn(0f, 1f)
                                gestureType = GestureType.BRIGHTNESS
                                gestureProgress = brightnessLevel
                            }
                            EnhancedGestureManager.Zone.RIGHT -> {
                                // Volume control
                                volumeLevel = (volumeLevel + deltaY / size.height).coerceIn(0f, 1f)
                                gestureType = GestureType.VOLUME
                                gestureProgress = volumeLevel
                            }
                            else -> {}
                        }
                        gestureZone = zone
                    }
                }
            }
    ) {
        // Video Player View
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Gradient overlays for better control visibility
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                // Bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
            }
        }
        
        // Gesture Indicator
        AnimatedVisibility(
            visible = gestureType != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GestureIndicator(
                type = gestureType,
                progress = gestureProgress,
                modifier = Modifier.alpha(gestureIndicatorAlpha)
            )
        }
        
        // Buffering Indicator
        AnimatedVisibility(
            visible = isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }
        
        // Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(controlsAlpha)
            ) {
                // Top Controls
                TopControls(
                    title = videoTitle,
                    onBackPressed = onBackPressed,
                    onMoreClick = { showMoreMenu = true },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                
                // Center Controls
                CenterControls(
                    isPlaying = isPlaying,
                    onPlayPauseClick = onPlayPauseClick,
                    onSeekBackward = {
                        val newPosition = (currentPosition - 10000).coerceAtLeast(0)
                        onSeekTo(newPosition.toFloat())
                    },
                    onSeekForward = {
                        val newPosition = (currentPosition + 10000).coerceAtMost(duration)
                        onSeekTo(newPosition.toFloat())
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // Bottom Controls
                BottomControls(
                    currentPosition = currentPosition,
                    duration = duration,
                    bufferedPosition = player.bufferedPosition,
                    isFullscreen = isFullscreen,
                    playbackSpeed = playbackSpeed,
                    videoQuality = videoQuality,
                    subtitlesEnabled = subtitles.isNotEmpty(),
                    onSeekTo = onSeekTo,
                    onFullscreenToggle = onFullscreenToggle,
                    onSpeedClick = { showSpeedMenu = true },
                    onQualityClick = { showQualityMenu = true },
                    onSubtitleToggle = onSubtitleToggle,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        
        // Current Subtitle
        if (subtitles.isNotEmpty()) {
            val currentSubtitle = subtitles.find { subtitle ->
                currentPosition >= subtitle.startTime && currentPosition <= subtitle.endTime
            }
            
            currentSubtitle?.let { subtitle ->
                SubtitleOverlay(
                    subtitle = subtitle,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (showControls) 100.dp else 40.dp)
                )
            }
        }
        
        // Speed Menu
        SpeedSelectionMenu(
            visible = showSpeedMenu,
            currentSpeed = playbackSpeed,
            onSpeedSelected = { speed ->
                onSpeedChange(speed)
                showSpeedMenu = false
            },
            onDismiss = { showSpeedMenu = false }
        )
        
        // Quality Menu
        QualitySelectionMenu(
            visible = showQualityMenu,
            currentQuality = videoQuality,
            onQualitySelected = { quality ->
                onQualityChange(quality)
                showQualityMenu = false
            },
            onDismiss = { showQualityMenu = false }
        )
        
        // More Menu
        MoreOptionsMenu(
            visible = showMoreMenu,
            onDismiss = { showMoreMenu = false }
        )
    }
    
    // Update gesture manager after a delay
    LaunchedEffect(gestureType) {
        if (gestureType != null) {
            delay(1000)
            gestureType = null
        }
    }
}

@Composable
private fun TopControls(
    title: String,
    onBackPressed: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
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
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CenterControls(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onSeekBackward,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Seek Backward",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        IconButton(
            onClick = onSeekForward,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Forward10,
                contentDescription = "Seek Forward",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun BottomControls(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    isFullscreen: Boolean,
    playbackSpeed: Float,
    videoQuality: String,
    subtitlesEnabled: Boolean,
    onSeekTo: (Float) -> Unit,
    onFullscreenToggle: () -> Unit,
    onSpeedClick: () -> Unit,
    onQualityClick: () -> Unit,
    onSubtitleToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Progress Bar
        VideoProgressBar(
            currentPosition = currentPosition,
            duration = duration,
            bufferedPosition = bufferedPosition,
            onSeekTo = onSeekTo
        )
        
        // Time and Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Display
            Text(
                text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                color = Color.White,
                fontSize = 14.sp
            )
            
            // Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subtitle Toggle
                IconButton(
                    onClick = onSubtitleToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (subtitlesEnabled) Icons.Default.Subtitles else Icons.Outlined.SubtitlesOff,
                        contentDescription = "Subtitles",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Speed Control
                TextButton(
                    onClick = onSpeedClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        fontSize = 14.sp
                    )
                }
                
                // Quality Control
                TextButton(
                    onClick = onQualityClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = videoQuality,
                        fontSize = 14.sp
                    )
                }
                
                // Fullscreen Toggle
                IconButton(
                    onClick = onFullscreenToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
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
    bufferedPosition: Long,
    onSeekTo: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragPosition = (offset.x / size.width * duration).coerceIn(0f, duration.toFloat())
                    },
                    onDragEnd = {
                        onSeekTo(dragPosition)
                        isDragging = false
                    },
                    onDrag = { _, dragAmount ->
                        dragPosition = (dragPosition + dragAmount.x / size.width * duration)
                            .coerceIn(0f, duration.toFloat())
                    }
                )
            }
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.3f))
        )
        
        // Buffered Progress
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = if (duration > 0) bufferedPosition.toFloat() / duration else 0f)
                .height(4.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.5f))
        )
        
        // Current Progress
        Box(
            modifier = Modifier
                .fillMaxWidth(
                    fraction = if (duration > 0) {
                        if (isDragging) dragPosition / duration else currentPosition.toFloat() / duration
                    } else 0f
                )
                .height(4.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        
        // Thumb
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (size.width * if (duration > 0) {
                            if (isDragging) dragPosition / duration else currentPosition.toFloat() / duration
                        } else 0f).roundToInt(),
                        y = 0
                    )
                }
                .size(16.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .scale(if (isDragging) 1.3f else 1f)
        )
    }
}

@Composable
private fun SubtitleOverlay(
    subtitle: AISubtitleGenerator.Subtitle,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = subtitle.translatedText ?: subtitle.text,
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GestureIndicator(
    type: GestureType?,
    progress: Float,
    modifier: Modifier = Modifier
) {
    when (type) {
        GestureType.BRIGHTNESS -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BrightnessHigh,
                    contentDescription = "Brightness",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.width(120.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
        GestureType.VOLUME -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = when {
                        progress == 0f -> Icons.Default.VolumeOff
                        progress < 0.5f -> Icons.Default.VolumeDown
                        else -> Icons.Default.VolumeUp
                    },
                    contentDescription = "Volume",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.width(120.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
        else -> {}
    }
}

@Composable
private fun SpeedSelectionMenu(
    visible: Boolean,
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Playback Speed") },
            text = {
                Column {
                    listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSpeedSelected(speed) }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${speed}x")
                            if (speed == currentSpeed) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QualitySelectionMenu(
    visible: Boolean,
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Video Quality") },
            text = {
                Column {
                    listOf("Auto", "1080p", "720p", "480p", "360p", "240p").forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQualitySelected(quality) }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = quality)
                            if (quality == currentQuality) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MoreOptionsMenu(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("More Options") },
            text = {
                Column {
                    listOf(
                        "Audio Track" to Icons.Default.AudioTrack,
                        "Sleep Timer" to Icons.Default.Timer,
                        "Equalizer" to Icons.Default.Equalizer,
                        "Cast" to Icons.Default.Cast,
                        "Share" to Icons.Default.Share
                    ).forEach { (option, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* Handle option */ }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = option,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = option)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

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

private enum class GestureType {
    BRIGHTNESS, VOLUME, SEEK
}