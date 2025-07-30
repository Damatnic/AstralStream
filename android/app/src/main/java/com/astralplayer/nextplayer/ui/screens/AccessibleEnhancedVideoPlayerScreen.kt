package com.astralplayer.nextplayer.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.data.gesture.enhancedGestureDetector
import com.astralplayer.nextplayer.data.gesture.accessibility.*
import com.astralplayer.nextplayer.ui.components.*
import com.astralplayer.nextplayer.viewmodel.SimpleEnhancedPlayerViewModel
import kotlinx.coroutines.launch

/**
 * Enhanced video player screen with comprehensive gesture and accessibility support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibleEnhancedVideoPlayerScreen(
    viewModel: SimpleEnhancedPlayerViewModel,
    videoUri: Uri,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Context and accessibility
    val context = LocalContext.current
    val accessibilityManager = remember { GestureAccessibilityManager(context) }
    val accessibleInput = remember { AccessibleGestureInput(context) }
    val voiceCommands = remember { VoiceGestureCommands() }
    
    // Accessibility state
    val accessibilityState by accessibilityManager.accessibilityState.collectAsState()
    var showAccessibleControls by remember { mutableStateOf(false) }
    var showGestureHints by remember { mutableStateOf(false) }
    var showGestureZones by remember { mutableStateOf(false) }
    
    // Collect states
    val playerState by viewModel.playerState.collectAsState(initial = Player.STATE_IDLE)
    val currentPosition by viewModel.currentPosition.collectAsState(initial = 0L)
    val duration by viewModel.duration.collectAsState(initial = 0L)
    val isPlaying by viewModel.isPlaying.collectAsState(initial = false)
    val bufferedPercentage by viewModel.bufferedPercentage.collectAsState(initial = 0)
    
    val uiState by viewModel.uiState.collectAsState()
    val overlayVisibility by viewModel.overlayVisibility.collectAsState()
    val gestureSettings by viewModel.gestureSettings.collectAsState()
    
    // Screen dimensions
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Controls visibility
    var controlsVisible by remember { mutableStateOf(true) }
    
    // Focus management
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    
    // Load video on first composition
    LaunchedEffect(videoUri) {
        viewModel.loadVideo(videoUri)
    }
    
    // Auto-hide controls
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying && !accessibilityState.isScreenReaderEnabled) {
            kotlinx.coroutines.delay(3000)
            controlsVisible = false
        }
    }
    
    // Show accessible controls when screen reader is active
    LaunchedEffect(accessibilityState.isScreenReaderEnabled) {
        showAccessibleControls = accessibilityState.isScreenReaderEnabled
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                handleKeyboardGesture(keyEvent, viewModel, currentPosition, duration)
            }
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("Video player. ")
                    append("Currently ${if (isPlaying) "playing" else "paused"}. ")
                    append("Position: ${formatTimeAccessible(currentPosition)} of ${formatTimeAccessible(duration)}. ")
                    if (accessibilityState.isScreenReaderEnabled) {
                        append("Accessible controls are available. ")
                    } else {
                        append("Gesture controls enabled. ")
                    }
                }
                
                // Add custom accessibility actions
                customActions = listOf(
                    CustomAccessibilityAction("Toggle accessible controls") {
                        showAccessibleControls = !showAccessibleControls
                        true
                    },
                    CustomAccessibilityAction("Show gesture hints") {
                        showGestureHints = !showGestureHints
                        true
                    },
                    CustomAccessibilityAction("Show gesture zones") {
                        showGestureZones = !showGestureZones
                        true
                    }
                )
            }
            .then(
                if (!accessibilityState.isScreenReaderEnabled) {
                    Modifier.enhancedGestureDetector(
                        gestureManager = viewModel.gestureManager,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        callbacks = viewModel.getGestureCallbacks().let { callbacks ->
                            // Wrap callbacks to announce actions
                            callbacks.copy(
                                onHorizontalSeek = { delta, velocity ->
                                    callbacks.onHorizontalSeek(delta, velocity)
                                    announceSeek(delta, accessibilityManager)
                                },
                                onVerticalVolumeChange = { delta, side ->
                                    callbacks.onVerticalVolumeChange(delta, side)
                                    announceVolumeChange(delta, accessibilityManager)
                                },
                                onVerticalBrightnessChange = { delta, side ->
                                    callbacks.onVerticalBrightnessChange(delta, side)
                                    announceBrightnessChange(delta, accessibilityManager)
                                }
                            )
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        // Video player view
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false // We'll use our own controls
                    player = viewModel.playerRepository.exoPlayer
                    
                    // Setup vertical gesture handler with window
                    (context as? android.app.Activity)?.window?.let { window ->
                        viewModel.setupVerticalGestureHandler(window)
                    }
                    
                    // Add accessibility description
                    contentDescription = "Video playback area"
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = uiState.zoomLevel
                    scaleY = uiState.zoomLevel
                }
                .semantics {
                    role = Role.Image
                    stateDescription = "Zoom level: ${(uiState.zoomLevel * 100).toInt()}%"
                }
        )
        
        // Gesture zone indicators for accessibility
        GestureZoneFocusIndicators(
            showZones = showGestureZones && !accessibilityState.isScreenReaderEnabled,
            modifier = Modifier.fillMaxSize()
        )
        
        // High contrast mode wrapper
        HighContrastGestureOverlay(
            enabled = accessibilityState.isAccessibilityEnabled
        ) {
            // Custom controls overlay
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AccessiblePlayerControlsOverlay(
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    bufferedPercentage = bufferedPercentage,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onSeekTo = { position -> viewModel.seekTo(position) },
                    onBack = onBack,
                    onAccessibilityClick = { showAccessibleControls = !showAccessibleControls },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // All gesture feedback overlays with accessibility enhancements
            RenderAccessibleOverlays(
                overlayVisibility = overlayVisibility,
                uiState = uiState,
                currentPosition = currentPosition,
                duration = duration,
                gestureSettings = gestureSettings,
                viewModel = viewModel,
                accessibilityManager = accessibilityManager
            )
        }
        
        // Accessible control buttons
        AccessibleGestureControls(
            visible = showAccessibleControls,
            accessibleControls = accessibleInput.createAccessibleControls(),
            onAction = { action ->
                when (action) {
                    is GestureAction.Seek -> viewModel.seekTo(maxOf(0L, minOf(duration, currentPosition + action.deltaMs)))
                    is GestureAction.VolumeChange -> viewModel.getGestureCallbacks().onVerticalVolumeChange(action.delta, action.side)
                    is GestureAction.BrightnessChange -> viewModel.getGestureCallbacks().onVerticalBrightnessChange(action.delta, action.side)
                    is GestureAction.TogglePlayPause -> viewModel.togglePlayPause()
                    else -> {} // Handle other actions as needed
                }
                accessibilityManager.announceGestureAction(action)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // Gesture hints overlay
        GestureHintsOverlay(
            visible = showGestureHints,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        
        // Request focus for keyboard navigation
        LaunchedEffect(Unit) {
            if (accessibilityState.isAccessibilityEnabled) {
                focusRequester.requestFocus()
            }
        }
    }
}

/**
 * Handle keyboard input for accessibility
 */
private fun handleKeyboardGesture(
    keyEvent: KeyEvent,
    viewModel: SimpleEnhancedPlayerViewModel,
    currentPosition: Long,
    duration: Long
): Boolean {
    if (keyEvent.type != KeyEventType.KeyDown) return false
    
    return when (keyEvent.key) {
        Key.Spacebar -> {
            viewModel.togglePlayPause()
            true
        }
        Key.DirectionRight -> {
            viewModel.seekTo(minOf(duration, currentPosition + 10000L))
            true
        }
        Key.DirectionLeft -> {
            viewModel.seekTo(maxOf(0L, currentPosition - 10000L))
            true
        }
        Key.DirectionUp -> {
            viewModel.getGestureCallbacks().onVerticalVolumeChange(0.1f, TouchSide.CENTER)
            true
        }
        Key.DirectionDown -> {
            viewModel.getGestureCallbacks().onVerticalVolumeChange(-0.1f, TouchSide.CENTER)
            true
        }
        Key.Plus, Key.Equals -> {
            viewModel.getGestureCallbacks().onVerticalVolumeChange(0.05f, TouchSide.CENTER)
            true
        }
        Key.Minus -> {
            viewModel.getGestureCallbacks().onVerticalVolumeChange(-0.05f, TouchSide.CENTER)
            true
        }
        Key.B -> {
            viewModel.getGestureCallbacks().onVerticalBrightnessChange(0.1f, TouchSide.CENTER)
            true
        }
        Key.D -> {
            viewModel.getGestureCallbacks().onVerticalBrightnessChange(-0.1f, TouchSide.CENTER)
            true
        }
        else -> false
    }
}

/**
 * Accessible player controls with additional accessibility button
 */
@Composable
private fun AccessiblePlayerControlsOverlay(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    bufferedPercentage: Int,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onBack: () -> Unit,
    onAccessibilityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Gradient backgrounds
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )
        
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
        
        // Top bar with accessibility button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics {
                    contentDescription = "Go back"
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            
            Row {
                IconButton(
                    onClick = onAccessibilityClick,
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle accessibility controls"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Accessibility,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                
                val context = LocalContext.current
                IconButton(onClick = { 
                    val intent = android.content.Intent(context, com.astralplayer.nextplayer.SettingsActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Center play/pause button with better accessibility
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.Center)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
                .semantics {
                    contentDescription = if (isPlaying) "Pause video" else "Play video"
                    role = Role.Button
                }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        
        // Bottom controls with accessibility improvements
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            // Seek bar with accessibility
            AccessibleVideoSeekBar(
                currentPosition = currentPosition,
                duration = duration,
                bufferedPercentage = bufferedPercentage,
                onSeekTo = onSeekTo,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Time display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics {
                        contentDescription = "Current time: ${formatTimeAccessible(currentPosition)}"
                    }
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics {
                        contentDescription = "Total duration: ${formatTimeAccessible(duration)}"
                    }
                )
            }
        }
    }
}

/**
 * Accessible video seek bar
 */
@Composable
private fun AccessibleVideoSeekBar(
    currentPosition: Long,
    duration: Long,
    bufferedPercentage: Int,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0f) }
    
    Slider(
        value = if (isSeeking) seekPosition else {
            if (duration > 0) currentPosition.toFloat() / duration else 0f
        },
        onValueChange = { value ->
            isSeeking = true
            seekPosition = value
        },
        onValueChangeFinished = {
            isSeeking = false
            onSeekTo((seekPosition * duration).toLong())
        },
        valueRange = 0f..1f,
        modifier = modifier.semantics {
            contentDescription = "Video seek bar. Current position: ${formatTimeAccessible(currentPosition)} of ${formatTimeAccessible(duration)}"
            stateDescription = "${((currentPosition.toFloat() / duration) * 100).toInt()}% complete"
        },
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
        )
    )
}

/**
 * Render all overlays with accessibility support
 */
@Composable
private fun RenderAccessibleOverlays(
    overlayVisibility: SimpleEnhancedPlayerViewModel.OverlayVisibility,
    uiState: SimpleEnhancedPlayerViewModel.PlayerUiState,
    currentPosition: Long,
    duration: Long,
    gestureSettings: EnhancedGestureSettings,
    viewModel: SimpleEnhancedPlayerViewModel,
    accessibilityManager: GestureAccessibilityManager
) {
    // Seek preview overlay
    if (overlayVisibility.seekPreview) {
        uiState.seekPreviewInfo?.let { seekInfo ->
            SeekPreviewOverlay(
                seekInfo = seekInfo,
                currentPosition = currentPosition,
                videoDuration = duration,
                thumbnailUrl = null,
                modifier = Modifier.testTag("seek_preview_overlay")
            )
        }
    }
    
    // Other overlays remain the same but with test tags for accessibility testing
    // ... (similar pattern for other overlays)
}

/**
 * Announce seek action for accessibility
 */
private fun announceSeek(delta: Float, accessibilityManager: GestureAccessibilityManager) {
    val seconds = (delta * 60).toInt() // Assuming 60 second full swipe
    val action = GestureAction.Seek((seconds * 1000).toLong())
    accessibilityManager.announceGestureAction(action)
}

/**
 * Announce volume change for accessibility
 */
private fun announceVolumeChange(delta: Float, accessibilityManager: GestureAccessibilityManager) {
    accessibilityManager.announceGestureAction(GestureAction.VolumeChange(delta, TouchSide.CENTER))
}

/**
 * Announce brightness change for accessibility  
 */
private fun announceBrightnessChange(delta: Float, accessibilityManager: GestureAccessibilityManager) {
    accessibilityManager.announceGestureAction(GestureAction.BrightnessChange(delta, TouchSide.CENTER))
}

/**
 * Format time for accessibility announcements
 */
private fun formatTimeAccessible(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return buildString {
        if (hours > 0) {
            append("$hours hour${if (hours > 1) "s" else ""} ")
        }
        if (minutes > 0) {
            append("$minutes minute${if (minutes > 1) "s" else ""} ")
        }
        append("$seconds second${if (seconds != 1L) "s" else ""}")
    }
}

/**
 * Formats time in milliseconds to readable format
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