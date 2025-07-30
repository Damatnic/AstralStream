package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerUiState
import com.astralplayer.nextplayer.feature.player.gestures.*
import com.astralplayer.nextplayer.data.SettingsDataStore
import kotlinx.coroutines.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.astralplayer.nextplayer.viewmodel.SettingsViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedVideoPlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val playerState by viewModel.playerState.collectAsState()
    val userSettings by settingsViewModel.settings.collectAsState()
    
    var showControls by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    
    // Gesture feedback states
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableStateOf(0.5f) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var currentBrightness by remember { mutableStateOf(0.5f) }
    var showSeekOverlay by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf(SeekDirection.FORWARD) }
    var seekAmount by remember { mutableStateOf(0L) }
    var showDoubleTapOverlay by remember { mutableStateOf(false) }
    var doubleTapPosition by remember { mutableStateOf(Offset.Zero) }
    var doubleTapForward by remember { mutableStateOf(true) }
    var showSpeedOverlay by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(1f) }
    var showZoomOverlay by remember { mutableStateOf(false) }
    var currentZoom by remember { mutableStateOf(1f) }
    
    // Gesture settings based on user preferences
    val gestureSettings = remember(userSettings) { 
        GestureSettings(
            general = GeneralGestureSettings(
                gesturesEnabled = userSettings.enableGestures
            ),
            doubleTap = DoubleTapGestureSettings(
                enabled = userSettings.doubleTapSeek
            ),
            longPress = LongPressGestureSettings(
                speedGestureEnabled = userSettings.longPressSpeed
            )
        )
    }
    
    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls && playerState.isPlaying) {
            delay(3000)
            showControls = false
        }
    }
    
    // Create gesture callbacks
    val gestureCallbacks = remember {
        object : GestureCallbacks {
            override fun onVolumeChange(level: Float, delta: Float, side: TouchSide) {
                currentVolume = level
                showVolumeOverlay = true
                viewModel.setVolume(level)
            }
            
            override fun onBrightnessChange(level: Float, delta: Float, side: TouchSide) {
                currentBrightness = level
                showBrightnessOverlay = true
                viewModel.setBrightness(level)
            }
            
            override fun onSeekStart() {
                showControls = true
            }
            
            override fun onSeek(deltaMs: Long) {
                seekAmount = deltaMs
                seekDirection = if (deltaMs > 0) SeekDirection.FORWARD else SeekDirection.BACKWARD
                showSeekOverlay = true
                viewModel.seekBy(deltaMs)
            }
            
            override fun onSeekEnd(totalDeltaMs: Long) {
                // Optionally show final seek amount
            }
            
            override fun onSingleTap() {
                showControls = !showControls
            }
            
            override fun onDoubleTapLeft() {
                doubleTapPosition = Offset(0.2f, 0.5f)
                doubleTapForward = false
                showDoubleTapOverlay = true
                viewModel.seekBy(-10000) // 10 seconds back
            }
            
            override fun onDoubleTapCenter() {
                viewModel.togglePlayPause()
            }
            
            override fun onDoubleTapRight() {
                doubleTapPosition = Offset(0.8f, 0.5f)
                doubleTapForward = true
                showDoubleTapOverlay = true
                viewModel.seekBy(10000) // 10 seconds forward
            }
            
            override fun onZoomStart() {
                showZoomOverlay = true
            }
            
            override fun onZoom(scaleFactor: Float) {
                currentZoom *= scaleFactor
                currentZoom = currentZoom.coerceIn(0.5f, 4f)
                viewModel.setZoom(currentZoom)
            }
            
            override fun onZoomEnd() {
                // Zoom ended
            }
            
            override fun onLongPress(position: Offset) {
                // Default to playback speed toggle for long press
                currentSpeed = if (currentSpeed == 1f) 2f else 1f
                showSpeedOverlay = true
                viewModel.setPlaybackSpeed(currentSpeed)
            }
            
            override fun onGestureStart(type: GestureType, side: TouchSide) {
                // Optional: Show gesture start feedback
            }
            
            override fun onGestureEnd(type: GestureType, success: Boolean) {
                // Hide overlays after delay
                when (type) {
                    GestureType.VERTICAL_VOLUME -> {
                        GlobalScope.launch {
                            delay(1000)
                            showVolumeOverlay = false
                        }
                    }
                    GestureType.VERTICAL_BRIGHTNESS -> {
                        GlobalScope.launch {
                            delay(1000)
                            showBrightnessOverlay = false
                        }
                    }
                    GestureType.HORIZONTAL_SEEK -> {
                        GlobalScope.launch {
                            delay(1000)
                            showSeekOverlay = false
                        }
                    }
                    GestureType.DOUBLE_TAP -> {
                        GlobalScope.launch {
                            delay(800)
                            showDoubleTapOverlay = false
                        }
                    }
                    GestureType.ZOOM -> {
                        GlobalScope.launch {
                            delay(1000)
                            showZoomOverlay = false
                        }
                    }
                    GestureType.LONG_PRESS -> {
                        GlobalScope.launch {
                            delay(1500)
                            showSpeedOverlay = false
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    
    val gestureManager = remember { GestureManager(context, gestureCallbacks) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Player View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    with(gestureManager) {
                        Modifier.applyGestures(
                            settings = gestureSettings,
                            size = androidx.compose.ui.unit.IntSize(
                                density.run { 1080.dp.roundToPx() },
                                density.run { 1920.dp.roundToPx() }
                            )
                        )
                    }
                )
        )
        
        // Gesture feedback overlays
        VolumeOverlay(
            visible = showVolumeOverlay,
            volume = currentVolume
        )
        
        BrightnessOverlay(
            visible = showBrightnessOverlay,
            brightness = currentBrightness
        )
        
        SeekOverlay(
            visible = showSeekOverlay,
            seekDirection = seekDirection,
            seekAmount = seekAmount
        )
        
        DoubleTapSeekOverlay(
            visible = showDoubleTapOverlay,
            position = with(density) { 
                Offset(
                    doubleTapPosition.x * 1080.dp.toPx(),
                    doubleTapPosition.y * 1920.dp.toPx()
                )
            },
            seekAmount = 10,
            isForward = doubleTapForward
        )
        
        ZoomIndicator(
            visible = showZoomOverlay,
            zoomLevel = currentZoom
        )
        
        PlaybackSpeedIndicator(
            visible = showSpeedOverlay,
            speed = currentSpeed
        )
        
        // Controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            PlayerControlsOverlay(
                playerState = playerState,
                onBack = onBack,
                onPlayPause = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) },
                onShowSettings = { showSettings = true }
            )
        }
        
        // Settings dialog
        if (showSettings) {
            PlayerSettingsDialog(
                onDismiss = { showSettings = false },
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun PlayerControlsOverlay(
    playerState: PlayerUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onShowSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top gradient with back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = playerState.videoTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                )
                
                IconButton(onClick = onShowSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Center play/pause button
        Box(
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        // Bottom controls with timeline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            VideoTimelineControls(
                currentPosition = playerState.currentPosition,
                duration = playerState.duration,
                bufferedPosition = (playerState.bufferedPercentage * playerState.duration / 100).toLong(),
                onSeek = onSeek
            )
        }
    }
}

@Composable
private fun PlayerSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: PlayerViewModel
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Player Settings") },
        text = {
            Column {
                Text("Quality, Audio, Subtitles, and more options")
                // Add actual settings options here
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}