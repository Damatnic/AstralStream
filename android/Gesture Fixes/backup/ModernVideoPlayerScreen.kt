package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerUiState
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerState
import com.astralplayer.nextplayer.feature.player.gestures.WorkingGestureHandler
import com.astralplayer.nextplayer.feature.player.gestures.GestureSettings
import com.astralplayer.nextplayer.feature.settings.ComprehensiveSettingsManager
import com.astralplayer.nextplayer.feature.settings.ComprehensiveSettingsScreen
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.*

@Composable
fun ModernVideoPlayerScreen(
    viewModel: PlayerViewModel,
    gestureSettings: GestureSettings = GestureSettings(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.playerState.collectAsState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    // Initialize comprehensive settings manager
    val settingsManager = remember { ComprehensiveSettingsManager(context) }
    val currentGestureSettings by settingsManager.gestureSettings.collectAsState()
    
    var showQuickSettings by remember { mutableStateOf(false) }
    var showFullSettings by remember { mutableStateOf(false) }
    var showSubtitles by remember { mutableStateOf(false) }
    var showQuality by remember { mutableStateOf(false) }
    var showAudioTracks by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    
    // Update screen dimensions in ViewModel
    LaunchedEffect(configuration) {
        with(density) {
            viewModel.updateScreenDimensions(configuration.screenWidthDp.dp.toPx())
        }
    }
    
    // Auto-hide controls after 4 seconds (standard video player behavior)
    LaunchedEffect(uiState.areControlsVisible, uiState.isPlaying, uiState.isScreenLocked) {
        if (uiState.areControlsVisible && uiState.isPlaying && !uiState.isScreenLocked) {
            delay(4000) // Standard 4-second delay
            viewModel.hideControls()
        }
    }
    
    // Show controls when user interacts
    LaunchedEffect(uiState.isPlaying) {
        if (!uiState.isPlaying) {
            // Always show controls when paused
            viewModel.toggleControlsVisibility()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Video player surface
        uiState.exoPlayer?.let { player ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false // We'll use custom controls
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Working gesture handler with MX Player features
        WorkingGestureHandler(
            viewModel = viewModel,
            gestureSettings = GestureSettings(
                general = com.astralplayer.nextplayer.feature.player.gestures.GeneralGestureSettings(
                    gesturesEnabled = currentGestureSettings.gesturesEnabled,
                    tapToToggleControls = true,
                    feedbackVibrationsEnabled = currentGestureSettings.hapticFeedback
                ),
                doubleTap = com.astralplayer.nextplayer.feature.player.gestures.DoubleTapGestureSettings(
                    enabled = currentGestureSettings.doubleTapToSeek,
                    seekAmount = (currentGestureSettings.seekAmount * 1000).toLong()
                ),
                vertical = com.astralplayer.nextplayer.feature.player.gestures.VerticalGestureSettings(
                    volumeGestureEnabled = currentGestureSettings.volumeGesture,
                    brightnessGestureEnabled = currentGestureSettings.brightnessGesture,
                    volumeSensitivity = currentGestureSettings.gestureSensitivity,
                    brightnessSensitivity = currentGestureSettings.gestureSensitivity
                ),
                horizontal = com.astralplayer.nextplayer.feature.player.gestures.HorizontalGestureSettings(
                    seekGestureEnabled = currentGestureSettings.swipeToSeek,
                    sensitivity = currentGestureSettings.gestureSensitivity
                ),
                longPress = com.astralplayer.nextplayer.feature.player.gestures.LongPressGestureSettings(
                    enabled = currentGestureSettings.longPressSpeed
                ),
                zoom = com.astralplayer.nextplayer.feature.player.gestures.ZoomGestureSettings(
                    enabled = currentGestureSettings.pinchToZoom,
                    sensitivity = currentGestureSettings.gestureSensitivity
                )
            ),
            onGestureStart = { 
                if (!uiState.areControlsVisible) {
                    viewModel.toggleControlsVisibility()
                }
            },
            onGestureEnd = { /* Controls will auto-hide based on playing state */ },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture visual feedback overlays
        GestureOverlays(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // MX Player-style controls overlay
        AnimatedVisibility(
            visible = uiState.areControlsVisible && !uiState.isScreenLocked,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            MxPlayerControlsOverlay(
                uiState = uiState,
                viewModel = viewModel,
                gestureSettings = gestureSettings,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onSeekTo = { position -> viewModel.seekTo(position) },
                onSeekForward = { viewModel.seekForward() },
                onSeekBackward = { viewModel.seekBackward() },
                onBack = onBack,
                onShowSettings = { showQuickSettings = true },
                onShowAdvancedSettings = { showFullSettings = true },
                onLockScreen = { viewModel.toggleScreenLock() },
                onShowSubtitles = { showSubtitles = true },
                onShowQuality = { showQuality = true },
                onShowAudioTracks = { showAudioTracks = true },
                onShowMoreOptions = { showMoreOptions = true }
            )
        }

        // Screen lock indicator
        if (uiState.isScreenLocked) {
            ScreenLockIndicator(
                onUnlock = { viewModel.toggleScreenLock() }
            )
        }

        // Loading indicator
        if (uiState.playerState == PlayerState.BUFFERING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CosmicLoadingIndicator()
            }
        }

        // Error message
        if (uiState.hasError) {
            ErrorOverlay(
                errorMessage = uiState.errorMessage ?: "Unknown error",
                onRetry = { viewModel.retry() }
            )
        }



        // Modern Quick Settings Menu (right side slide-in)
        if (showQuickSettings) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                ModernQuickSettingsMenu(
                    isVisible = showQuickSettings,
                    viewModel = viewModel,
                    settingsManager = settingsManager,
                    onDismiss = { showQuickSettings = false }
                )
            }
        }
        
        // Comprehensive Settings Screen (full overlay)
        ComprehensiveSettingsScreen(
            isVisible = showFullSettings,
            settingsManager = settingsManager,
            onDismiss = { showFullSettings = false }
        )
        
        // Subtitle Selection Dialog
        if (showSubtitles) {
            SubtitleSelectionDialog(
                player = uiState.exoPlayer,
                trackSelector = viewModel.trackSelector,
                onDismiss = { showSubtitles = false }
            )
        }
        
        // Quality Selection Dialog
        if (showQuality) {
            QualitySelectionDialog(
                player = uiState.exoPlayer,
                trackSelector = viewModel.trackSelector,
                onDismiss = { showQuality = false }
            )
        }
        
        // Audio Track Selection Dialog
        if (showAudioTracks) {
            AudioTrackSelectionDialog(
                player = uiState.exoPlayer,
                trackSelector = viewModel.trackSelector,
                onDismiss = { showAudioTracks = false }
            )
        }
        
        // More Options Dialog
        if (showMoreOptions) {
            MoreOptionsDialog(
                viewModel = viewModel,
                onDismiss = { showMoreOptions = false },
                onOpenSettings = { /* Open settings */ }
            )
        }
        
        // Sleep Timer Dialog
        if (uiState.showSleepTimerDialog) {
            SleepTimerDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.dismissSleepTimerDialog() }
            )
        }
        
        // Equalizer Dialog
        if (uiState.showEqualizerDialog) {
            EqualizerDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.dismissEqualizerDialog() }
            )
        }
        
        // Video Info Dialog
        if (uiState.showVideoInfoDialog) {
            VideoInfoDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.dismissVideoInfoDialog() }
            )
        }
        
        // TODO: Add long press seek overlay when needed
    }
}



@Composable
private fun ScreenLockIndicator(
    onUnlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onUnlock() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF00BCD4)
                )
                Text(
                    text = "Screen Locked",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ErrorOverlay(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E).copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Error",
                    tint = Color(0xFFFF4444),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Playback Error",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00BCD4)
                    )
                ) {
                    Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}



// Extension functions - renamed to avoid shadowing member functions
private fun PlayerViewModel.toggleControlsVisibilityExt() {
    // Implementation will be added when integrating with PlayerViewModel
}

private fun PlayerViewModel.hideControlsExt() {
    // Implementation will be added when integrating with PlayerViewModel
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