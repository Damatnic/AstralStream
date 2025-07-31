package com.astralplayer.nextplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.nextplayer.gesture.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import android.view.MotionEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInteropFilter

@OptIn(UnstableApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AdvancedGestureVideoPlayerScreen(
    playerView: PlayerView,
    advancedGestureManager: AdvancedGestureManager,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val exoPlayer = playerView.player as ExoPlayer
    
    var showControls by remember { mutableStateOf(true) }
    var showGestureVisualizer by remember { mutableStateOf(false) }
    var currentGesturePath by remember { mutableStateOf<List<GestureRecorder.GesturePoint>?>(null) }
    
    // Collect gesture states
    val currentGesture by advancedGestureManager.currentGesture.collectAsState()
    val lastActionResult by advancedGestureManager.lastActionResult.collectAsState()
    val voiceCommandState by advancedGestureManager.voiceCommandState.collectAsState()
    val recordingState by advancedGestureManager.recordingState.collectAsState()
    
    // Gesture visualization
    LaunchedEffect(Unit) {
        advancedGestureManager.gestureVisualization.collectLatest { visualization ->
            showGestureVisualizer = visualization.isVisible
            currentGesturePath = visualization.path
        }
    }
    
    // Handle gesture actions
    LaunchedEffect(lastActionResult) {
        lastActionResult?.let { result ->
            if (result.success) {
                when (result.action) {
                    is GestureAction.TogglePlayPause -> {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }
                    is GestureAction.Seek -> {
                        val newPosition = (exoPlayer.currentPosition + result.action.milliseconds)
                            .coerceIn(0, exoPlayer.duration)
                        exoPlayer.seekTo(newPosition)
                    }
                    is GestureAction.VolumeChange -> {
                        // Volume control handled by system
                        // Could integrate with AudioManager here
                    }
                    is GestureAction.BrightnessChange -> {
                        // Brightness control handled by system
                        // Could integrate with WindowManager here
                    }
                    is GestureAction.DoubleTapSeek -> {
                        val seekMs = result.action.seconds * 1000
                        val newPosition = (exoPlayer.currentPosition + seekMs)
                            .coerceIn(0, exoPlayer.duration)
                        exoPlayer.seekTo(newPosition)
                    }
                    is GestureAction.LongPressSeek -> {
                        exoPlayer.setPlaybackSpeed(result.action.speedMultiplier)
                    }
                    is GestureAction.PinchZoom -> {
                        // Handle video zoom
                        playerView.setScale(result.action.scaleFactor)
                    }
                    is GestureAction.SwipeNavigation -> {
                        when (result.action.direction) {
                            "next_video" -> {
                                // Navigate to next video in playlist
                                exoPlayer.seekToNext()
                            }
                            "previous_video" -> {
                                // Navigate to previous video in playlist
                                exoPlayer.seekToPrevious()
                            }
                        }
                    }
                    is GestureAction.Custom -> {
                        when (result.action.action) {
                            "toggle_settings" -> showControls = !showControls
                            "toggle_fullscreen" -> {
                                // Handle fullscreen toggle
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    
    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Player View with advanced gesture detection
        AndroidView(
            factory = { _ ->
                playerView.apply {
                    useController = false // We handle all controls
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    scope.launch {
                        advancedGestureManager.handleTouchEvent(event)
                    }
                    true
                }
        )
        
        // Gesture visualization overlay
        if (showGestureVisualizer && currentGesturePath != null) {
            GestureVisualizationOverlay(
                gesturePath = currentGesturePath!!,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Recording indicator
        if (recordingState == GestureRecorder.RecordingState.RECORDING) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
                    .background(
                        Color.Red.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Recording Gesture...",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        
        // Voice command indicator
        AnimatedVisibility(
            visible = voiceCommandState is VoiceCommandHandler.VoiceCommandState.LISTENING,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.8f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Listening",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Listening for voice commands...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Custom controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            AdvancedVideoControlsOverlay(
                playerView = playerView,
                advancedGestureManager = advancedGestureManager,
                onBack = onBack,
                onHideControls = { showControls = false }
            )
        }
        
        // Gesture feedback display
        currentGesture?.let { gesture ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = gesture.name.replace("_", " "),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun GestureVisualizationOverlay(
    gesturePath: List<GestureRecorder.GesturePoint>,
    modifier: Modifier = Modifier
) {
    val pathColor = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = modifier) {
        if (gesturePath.size > 1) {
            val path = Path()
            gesturePath.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x * size.width, point.y * size.height)
                } else {
                    path.lineTo(point.x * size.width, point.y * size.height)
                }
            }
            
            drawPath(
                path = path,
                color = pathColor,
                style = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = PathEffect.cornerPathEffect(16.dp.toPx())
                )
            )
        }
    }
}

@Composable
fun AdvancedVideoControlsOverlay(
    playerView: PlayerView,
    advancedGestureManager: AdvancedGestureManager,
    onBack: () -> Unit,
    onHideControls: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(alpha = 0.5f)
            )
    ) {
        // Top bar with back button and settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Row {
                // Voice command button
                IconButton(
                    onClick = {
                        advancedGestureManager.toggleVoiceCommands()
                    }
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice Commands",
                        tint = Color.White
                    )
                }
                
                // Gesture customization button
                IconButton(
                    onClick = {
                        // Open gesture customization screen
                    }
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = "Customize Gestures",
                        tint = Color.White
                    )
                }
                
                // Settings button
                IconButton(
                    onClick = {
                        // Open settings
                    }
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }
    }
}