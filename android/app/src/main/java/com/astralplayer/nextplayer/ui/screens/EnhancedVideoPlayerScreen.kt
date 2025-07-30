package com.astralplayer.nextplayer.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.collectAsState
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.astralplayer.nextplayer.PlaylistActivity
import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.data.gesture.mxStyleGestures
import com.astralplayer.nextplayer.data.gesture.MxGestureCallbacks
import com.astralplayer.nextplayer.ui.components.*
import com.astralplayer.nextplayer.ui.components.ControlLockOverlay
import com.astralplayer.nextplayer.ui.components.LockControlButton
import com.astralplayer.nextplayer.ui.components.ErrorDialog
import com.astralplayer.nextplayer.ui.components.LoadingErrorScreen
import com.astralplayer.nextplayer.ui.components.PlaybackSpeedDialog
import com.astralplayer.nextplayer.ui.components.PlaybackSpeedIndicator
import com.astralplayer.nextplayer.ui.components.SleepTimerDialog
import com.astralplayer.nextplayer.ui.components.SleepTimerIndicator
import com.astralplayer.nextplayer.ui.components.CodecPackDialog
import com.astralplayer.nextplayer.ui.components.AudioTrackSelectionDialog
import com.astralplayer.nextplayer.ui.components.AudioTrackSettingsSheet
import com.astralplayer.nextplayer.ui.components.VideoStatsDialog
import com.astralplayer.nextplayer.ui.components.VideoStatsOverlay
import com.astralplayer.nextplayer.ui.components.VideoStatsExportDialog
import com.astralplayer.nextplayer.ui.components.PlaylistExportDialog
import com.astralplayer.nextplayer.ui.components.BubbleQuickSettingsMenu
import com.astralplayer.nextplayer.ui.components.PictureInPictureOverlay
import com.astralplayer.nextplayer.ui.components.SpeedMemoryIndicator
import com.astralplayer.nextplayer.ui.components.FloatingSpeedMemoryIndicator
import com.astralplayer.nextplayer.ui.components.SpeedMemoryToastContainer
import com.astralplayer.nextplayer.feature.ai.AISceneDetectionScreen
import com.astralplayer.nextplayer.utils.ThumbnailService
import com.astralplayer.nextplayer.viewmodel.SimpleEnhancedPlayerViewModel
import kotlinx.coroutines.launch

/**
 * Enhanced video player screen with comprehensive gesture support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedVideoPlayerScreen(
    viewModel: SimpleEnhancedPlayerViewModel,
    videoUri: Uri,
    videoTitle: String = "Unknown Video",
    playlistRepository: com.astralplayer.nextplayer.data.repository.PlaylistRepository? = null,
    onBack: () -> Unit,
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Collect states
    val playerState by viewModel.playerState.collectAsState(initial = Player.STATE_IDLE)
    val currentPosition by viewModel.currentPosition.collectAsState(initial = 0L)
    val duration by viewModel.duration.collectAsState(initial = 0L)
    val isPlaying by viewModel.isPlaying.collectAsState(initial = false)
    val bufferedPercentage by viewModel.bufferedPercentage.collectAsState(initial = 0)
    
    val uiState by viewModel.uiState.collectAsState()
    val overlayVisibility by viewModel.overlayVisibility.collectAsState()
    val gestureSettings by viewModel.gestureSettings.collectAsState()
    val errorState by viewModel.errorState.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val speedIndicatorVisible by viewModel.speedIndicatorVisible.collectAsState()
    val sleepTimerActive by viewModel.sleepTimerActive.collectAsState()
    val sleepTimerRemainingTime by viewModel.sleepTimerRemainingTime.collectAsState()
    val availableAudioTracks by viewModel.availableAudioTracks.collectAsState()
    val currentAudioTrack by viewModel.currentAudioTrack.collectAsState()
    val videoStats by viewModel.videoStats.collectAsState()
    val audioBoostEnabled by viewModel.audioBoostEnabled.collectAsState()
    val audioDelay by viewModel.audioDelay.collectAsState()
    
    // Speed memory state
    val hasSpeedMemory by viewModel.hasSpeedMemory.collectAsState()
    val currentToast by viewModel.currentToast.collectAsState()
    
    // AI Scene Detection states
    val detectedScenes by viewModel.detectedScenes.collectAsState()
    val sceneDetectionProgress by viewModel.sceneDetectionProgress.collectAsState()
    val isSceneDetectionAnalyzing by viewModel.isSceneDetectionAnalyzing.collectAsState()
    
    // Control lock state
    val isControlsLocked by viewModel.isControlsLocked.collectAsState()
    
    // Screen dimensions
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Thumbnail service
    val context = LocalContext.current
    val thumbnailService = remember { ThumbnailService.getInstance(context) }
    var currentThumbnailUrl by remember { mutableStateOf<String?>(null) }
    
    // Controls visibility
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsTimer by remember { mutableStateOf(0L) }
    
    // Dialog states
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleSettings by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showQualitySettings by remember { mutableStateOf(false) }
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showAudioTrackSettings by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showVideoStatsDialog by remember { mutableStateOf(false) }
    var showVideoStatsOverlay by remember { mutableStateOf(false) }
    var showVideoStatsExportDialog by remember { mutableStateOf(false) }
    var showAISceneDetectionDialog by remember { mutableStateOf(false) }
    var showCodecPackDialog by remember { mutableStateOf(false) }
    
    // Load video on first composition
    LaunchedEffect(videoUri) {
        viewModel.loadVideo(videoUri)
    }
    
    // Cleanup thumbnails when component is disposed
    DisposableEffect(Unit) {
        onDispose {
            thumbnailService.clearOldThumbnails(maxAgeMs = 5 * 60 * 1000L) // 5 minutes
        }
    }
    
    // Auto-hide controls
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            kotlinx.coroutines.delay(3000)
            controlsVisible = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .mxStyleGestures(
                callbacks = MxGestureCallbacks(
                    onSingleTap = {
                        if (!isControlsLocked) {
                            controlsVisible = !controlsVisible
                        }
                    },
                    onDoubleTapLeft = {
                        if (!isControlsLocked) {
                            viewModel.seekRelative(-10000L) // Seek backward 10 seconds
                        }
                    },
                    onDoubleTapRight = {
                        if (!isControlsLocked) {
                            viewModel.seekRelative(10000L) // Seek forward 10 seconds
                        }
                    },
                    onSeek = { deltaX, velocity ->
                        if (!isControlsLocked) {
                            // More sensitive seeking: 30 seconds for full screen width
                            val seekDelta = (deltaX / screenWidth) * 30000f
                            viewModel.seekRelative(seekDelta.toLong())
                        }
                    },
                    onSeekStart = {
                        if (!isControlsLocked) {
                            viewModel.startSeeking()
                        }
                    },
                    onSeekEnd = {
                        if (!isControlsLocked) {
                            viewModel.endSeeking()
                        }
                    },
                    onVolumeChange = { deltaY ->
                        if (!isControlsLocked) {
                            viewModel.adjustVolume(deltaY) // Already normalized
                        }
                    },
                    onBrightnessChange = { deltaY ->
                        if (!isControlsLocked) {
                            viewModel.adjustBrightness(deltaY) // Already normalized
                        }
                    },
                    onLongPressStart = { side ->
                        if (!isControlsLocked) {
                            // Set direction based on side (left = backward, right = forward)
                            val direction = if (side == com.astralplayer.nextplayer.data.TouchSide.LEFT) {
                                com.astralplayer.nextplayer.data.SeekDirection.BACKWARD
                            } else {
                                com.astralplayer.nextplayer.data.SeekDirection.FORWARD
                            }
                            viewModel.setLongPressDirection(direction)
                            viewModel.startLongPressSpeed()
                        }
                    },
                    onLongPressEnd = {
                        if (!isControlsLocked) {
                            viewModel.endLongPressSpeed()
                        }
                    },
                    onCenterLongPressStart = {
                        if (!isControlsLocked) {
                            viewModel.startCenterLongPressSpeed()
                        }
                    },
                    onCenterLongPressEnd = {
                        if (!isControlsLocked) {
                            viewModel.endCenterLongPressSpeed()
                        }
                    },
                    onCenterLongPressSpeedChange = { deltaY ->
                        if (!isControlsLocked) {
                            viewModel.updateCenterLongPressSpeed(deltaY)
                        }
                    }
                )
            )
    ) {
        // Video player view
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false // We'll use our own controls
                    player = viewModel.playerRepository.exoPlayer
                    
                    // Set PlayerView reference for aspect ratio control
                    viewModel.setPlayerView(this)
                    
                    // Setup vertical gesture handler with window
                    (context as? android.app.Activity)?.window?.let { window ->
                        viewModel.setupVerticalGestureHandler(window)
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = uiState.zoomLevel
                    scaleY = uiState.zoomLevel
                }
        )
        
        // Custom controls overlay
        AnimatedVisibility(
            visible = controlsVisible && !isInPipMode && !isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PlayerControlsOverlay(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                bufferedPercentage = bufferedPercentage,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onSeekTo = { position -> viewModel.seekTo(position) },
                onBack = onBack,
                onSubtitleClick = { showSubtitleDialog = true },
                onQualityClick = { showQualityDialog = true },
                onSpeedClick = { showPlaybackSpeedDialog = true },
                onSleepTimerClick = { showSleepTimerDialog = true },
                onAudioTrackClick = { showAudioTrackDialog = true },
                onPipClick = onEnterPip,
                onPlaylistClick = { showAddToPlaylistDialog = true },
                onStatsClick = { showVideoStatsDialog = true },
                onAISceneDetectionClick = { showAISceneDetectionDialog = true },
                onLoopModeClick = { viewModel.toggleLoopMode() },
                currentLoopMode = viewModel.currentLoopMode.collectAsState().value,
                onLockClick = { viewModel.lockControls() },
                detectedScenes = detectedScenes,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Gesture feedback overlays
        
        // Seek preview overlay
        if (overlayVisibility.seekPreview) {
            uiState.seekPreviewInfo?.let { seekInfo ->
                // Generate thumbnail for seek position
                LaunchedEffect(seekInfo.seekPosition) {
                    if (seekInfo.showThumbnail) {
                        thumbnailService.generateSeekPreviewThumbnail(
                            videoUri = videoUri,
                            positionMs = seekInfo.seekPosition
                        ) { thumbnailUrl ->
                            currentThumbnailUrl = thumbnailUrl
                        }
                    }
                }
                
                SeekPreviewOverlay(
                    seekInfo = seekInfo,
                    currentPosition = currentPosition,
                    videoDuration = duration,
                    thumbnailUrl = if (seekInfo.showThumbnail) currentThumbnailUrl else null,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        
        // Volume overlay
        if (overlayVisibility.volume) {
            uiState.volumeInfo?.let { volumeInfo ->
                VolumeOverlay(
                    volumeInfo = volumeInfo,
                    visible = true,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
        
        // Brightness overlay
        if (overlayVisibility.brightness) {
            uiState.brightnessInfo?.let { brightnessInfo ->
                BrightnessOverlay(
                    brightnessInfo = brightnessInfo,
                    visible = true,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }
        
        // Long press speed overlay - discrete speed indicator
        if (overlayVisibility.longPress) {
            uiState.longPressSeekInfo?.let { speedInfo ->
                com.astralplayer.nextplayer.ui.components.DiscreteLongPressSpeedOverlay(
                    speedInfo = speedInfo,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        
        // Double tap indicator
        if (overlayVisibility.doubleTap) {
            uiState.doubleTapInfo?.let { doubleTapInfo ->
                val alignment = when (doubleTapInfo.side) {
                    TouchSide.LEFT -> Alignment.CenterStart
                    TouchSide.RIGHT -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
                DoubleTapSeekIndicator(
                    side = doubleTapInfo.side,
                    seekAmount = doubleTapInfo.seekAmount,
                    visible = true,
                    modifier = Modifier
                        .align(alignment)
                        .padding(horizontal = 80.dp)
                )
            }
        }
        
        // Zoom overlay
        if (overlayVisibility.zoom) {
            PinchZoomOverlay(
                zoomLevel = uiState.zoomLevel,
                visible = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
            )
        }
        
        // Gesture conflict indicator
        if (overlayVisibility.conflict) {
            uiState.gestureConflict?.let { conflictingGestures ->
                GestureConflictIndicator(
                    conflictingGestures = conflictingGestures,
                    visible = true,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }
        }
        
        // Playback speed indicator
        PlaybackSpeedIndicator(
            currentSpeed = playbackSpeed,
            isVisible = speedIndicatorVisible,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
        )
        
        // Sleep timer indicator
        SleepTimerIndicator(
            isActive = sleepTimerActive,
            remainingTime = sleepTimerRemainingTime,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 120.dp, end = 16.dp)
        )
        
        // Speed memory indicator
        FloatingSpeedMemoryIndicator(
            hasSpeedMemory = hasSpeedMemory,
            currentSpeed = playbackSpeed,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 160.dp, end = 16.dp)
        )
        
        // Subtitle overlay
        viewModel.subtitleRenderer?.let { renderer ->
            SubtitleOverlay(
                subtitleRenderer = renderer,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Debug overlay (if enabled)
        if (gestureSettings.general.isDebugMode) {
            GestureDebugOverlay(
                gestureState = viewModel.gestureManager.gestureState.collectAsState().value,
                settings = gestureSettings,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
        
        // Dialogs
        if (showSubtitleDialog) {
            val availableSubtitles by viewModel.availableSubtitles.collectAsState()
            val selectedTrack by viewModel.getSelectedSubtitle().collectAsState()
            val aiSubtitleState by viewModel.aiSubtitleState.collectAsState()
            
            SubtitleSelectionDialog(
                subtitleRenderer = viewModel.subtitleRenderer,
                availableSubtitles = availableSubtitles,
                currentTrackId = selectedTrack?.id,
                onTrackSelected = { track ->
                    viewModel.selectSubtitleTrack(track?.id)
                },
                onDismiss = { showSubtitleDialog = false },
                onSettingsClick = {
                    showSubtitleDialog = false
                    showSubtitleSettings = true
                },
                onAddSubtitleFile = { uri, filename ->
                    viewModel.addExternalSubtitleFile(uri, filename)
                },
                onGenerateAISubtitles = {
                    viewModel.generateAISubtitlesForCurrentVideo()
                },
                onTestAIService = {
                    viewModel.testAIServiceConnectivity()
                },
                isGeneratingAISubtitles = aiSubtitleState.isGenerating
            )
        }
        
        if (showSubtitleSettings) {
            viewModel.subtitleRenderer?.let { renderer ->
                SubtitleSettingsSheet(
                    subtitleRenderer = renderer,
                    onDismiss = { showSubtitleSettings = false }
                )
            }
        }
        
        if (showQualityDialog) {
            val availableQualities by viewModel.availableQualities.collectAsState()
            val currentQuality by viewModel.currentQuality.collectAsState()
            
            QualitySelectionDialog(
                availableQualities = availableQualities,
                currentQuality = currentQuality,
                onQualitySelected = { quality ->
                    viewModel.setVideoQuality(quality)
                    showQualityDialog = false
                },
                onDismiss = { showQualityDialog = false },
                onSettingsClick = {
                    showQualityDialog = false
                    showQualitySettings = true
                }
            )
        }
        
        if (showQualitySettings) {
            QualitySettingsSheet(
                onDismiss = { showQualitySettings = false }
            )
        }
        
        // Playback speed dialog
        if (showPlaybackSpeedDialog) {
            PlaybackSpeedDialog(
                currentSpeed = playbackSpeed,
                onSpeedSelected = { speed ->
                    viewModel.setPlaybackSpeed(speed)
                    showPlaybackSpeedDialog = false
                },
                onDismiss = { showPlaybackSpeedDialog = false }
            )
        }
        
        // Sleep timer dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                isTimerActive = sleepTimerActive,
                remainingTime = sleepTimerRemainingTime,
                onTimerSet = { duration, action ->
                    viewModel.setSleepTimer(duration, action)
                    showSleepTimerDialog = false
                },
                onTimerCancel = {
                    viewModel.cancelSleepTimer()
                },
                onDismiss = { showSleepTimerDialog = false }
            )
        }
        
        // Codec pack dialog
        if (showCodecPackDialog) {
            CodecPackDialog(
                codecPackManager = viewModel.codecPackManager,
                onDismiss = { showCodecPackDialog = false }
            )
        }
        
        // Audio track dialog
        if (showAudioTrackDialog) {
            AudioTrackSelectionDialog(
                availableAudioTracks = availableAudioTracks,
                currentTrackId = currentAudioTrack?.id,
                onTrackSelected = { track ->
                    viewModel.selectAudioTrack(track)
                    showAudioTrackDialog = false
                },
                onDismiss = { showAudioTrackDialog = false }
            )
        }
        
        if (showAudioTrackSettings) {
            val context = LocalContext.current
            val application = context.applicationContext as com.astralplayer.nextplayer.AstralVuApplication
            AudioTrackSettingsSheet(
                onDismiss = { showAudioTrackSettings = false },
                isAudioBoostEnabled = audioBoostEnabled,
                onAudioBoostToggle = { enabled ->
                    viewModel.setAudioBoost(enabled)
                },
                audioDelayMs = audioDelay,
                onAudioDelayChange = { delayMs ->
                    viewModel.setAudioDelay(delayMs)
                },
                settingsRepository = application.settingsRepository
            )
        }
        
        // Add to playlist dialog
        if (showAddToPlaylistDialog && playlistRepository != null) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            
            AddToPlaylistDialog(
                videoUri = videoUri.toString(),
                videoTitle = videoTitle,
                playlistsFlow = playlistRepository.getAllPlaylists(),
                onDismiss = { showAddToPlaylistDialog = false },
                onCreateNewPlaylist = {
                    // Navigate to create playlist screen
                    val intent = Intent(context, PlaylistActivity::class.java).apply {
                        putExtra("action", "create")
                    }
                    context.startActivity(intent)
                },
                onAddToPlaylist = { playlistId ->
                    coroutineScope.launch {
                        playlistRepository.addVideoToPlaylist(
                            playlistId = playlistId,
                            videoUri = videoUri.toString(),
                            videoTitle = videoTitle
                        )
                    }
                }
            )
        }
        
        // Video stats dialog
        if (showVideoStatsDialog) {
            VideoStatsDialog(
                videoStats = videoStats,
                onDismiss = { showVideoStatsDialog = false },
                onExportStats = {
                    showVideoStatsExportDialog = true
                }
            )
        }
        
        // Video Stats Export Dialog
        if (showVideoStatsExportDialog) {
            VideoStatsExportDialog(
                videoStats = videoStats,
                onDismiss = { showVideoStatsExportDialog = false }
            )
        }
        
        // Bubble Quick Settings Menu (MX Player style)
        BubbleQuickSettingsMenu(
            currentPlaybackSpeed = playbackSpeed,
            onPlaybackSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
            currentBrightness = uiState.brightnessInfo?.percentage ?: viewModel.getBrightness(),
            onBrightnessChange = { brightness -> 
                viewModel.setBrightness(brightness)
            },
            currentVolume = uiState.volumeInfo?.percentage ?: viewModel.getVolume(),
            onVolumeChange = { volume ->
                viewModel.setVolume(volume)
            },
            isSubtitlesEnabled = viewModel.getSelectedSubtitle().collectAsState().value != null,
            onSubtitlesToggle = { enabled ->
                viewModel.toggleSubtitles(enabled)
            },
            availableSubtitles = viewModel.availableSubtitles.collectAsState().value,
            selectedSubtitle = viewModel.getSelectedSubtitle().collectAsState().value,
            onSubtitleSelect = { subtitle ->
                viewModel.selectSubtitle(subtitle)
            },
            selectedAspectRatio = viewModel.currentAspectRatio.collectAsState().value,
            onAspectRatioChange = { ratio ->
                viewModel.setAspectRatio(ratio)
            },
            isAutoRotateEnabled = viewModel.getAutoRotateEnabled().collectAsState(initial = true).value,
            onAutoRotateToggle = { enabled ->
                viewModel.setAutoRotate(enabled)
            },
            isHapticFeedbackEnabled = viewModel.getHapticFeedbackEnabled(),  
            onHapticFeedbackToggle = { enabled ->
                viewModel.setHapticFeedback(enabled)
            },
            isVolumeBoostEnabled = audioBoostEnabled,
            onVolumeBoostToggle = { enabled ->
                viewModel.setAudioBoost(enabled)
            },
            currentLoopMode = viewModel.currentLoopMode.collectAsState().value,
            onLoopModeChange = { viewModel.toggleLoopMode() },
            audioDelay = audioDelay.toLong(),
            onAudioDelayChange = { delay ->
                viewModel.setAudioDelay(delay.toInt())
            },
            sleepTimerActive = sleepTimerActive,
            onSleepTimerClick = { showSleepTimerDialog = true },
            onEnterPiP = onEnterPip,
            onCodecPacksClick = { showCodecPackDialog = true },
            showStatsOverlay = showVideoStatsOverlay,
            onStatsToggle = { showVideoStatsOverlay = it },
            // Long press speed control settings
            longPressSpeedEnabled = viewModel.getLongPressSpeedControlEnabled().collectAsState(initial = true).value,
            onLongPressSpeedEnabledChange = { enabled -> viewModel.setLongPressSpeedControlEnabled(enabled) },
            longPressInitialSpeed = viewModel.getLongPressInitialSpeed().collectAsState(initial = 2.0f).value,
            onLongPressInitialSpeedChange = { speed -> viewModel.setLongPressInitialSpeed(speed) },
            longPressProgressiveEnabled = viewModel.getLongPressProgressiveSpeedEnabled().collectAsState(initial = true).value,
            onLongPressProgressiveEnabledChange = { enabled -> viewModel.setLongPressProgressiveSpeedEnabled(enabled) },
            longPressSwipeSensitivity = viewModel.getLongPressSwipeSensitivity().collectAsState(initial = 1.0f).value,
            onLongPressSwipeSensitivityChange = { sensitivity -> viewModel.setLongPressSwipeSensitivity(sensitivity) },
            // Speed memory per video settings
            speedMemoryEnabled = viewModel.getSpeedMemoryEnabled().collectAsState(initial = true).value,
            onSpeedMemoryEnabledChange = { enabled -> viewModel.setSpeedMemoryEnabled(enabled) },
            onClearAllSpeedMemory = { viewModel.clearAllVideoSpeedMemory() }
        )
        
        // AI Scene Detection Dialog
        if (showAISceneDetectionDialog) {
            Dialog(
                onDismissRequest = { showAISceneDetectionDialog = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AISceneDetectionScreen(
                        scenes = detectedScenes,
                        isAnalyzing = isSceneDetectionAnalyzing,
                        progress = sceneDetectionProgress,
                        onAnalyzeVideo = {
                            // Re-trigger analysis - handled by viewModel automatically
                        },
                        onSceneClick = { scene ->
                            // Seek to scene start time
                            viewModel.seekTo(scene.startTime)
                            showAISceneDetectionDialog = false
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        // Video stats overlay
        VideoStatsOverlay(
            isVisible = showVideoStatsOverlay,
            droppedFrames = videoStats.droppedFrames,
            totalFrames = videoStats.totalFrames,
            currentBitrate = videoStats.averageBitrate,
            bufferHealth = videoStats.bufferedDuration,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 160.dp, start = 16.dp)
        )
        
        // Control lock overlay
        ControlLockOverlay(
            isLocked = isControlsLocked,
            onUnlock = { viewModel.unlockControls() },
            modifier = Modifier.fillMaxSize()
        )
        
        // Picture-in-Picture overlay controls
        PictureInPictureOverlay(
            isInPiPMode = isInPipMode,
            isPlaying = isPlaying,
            onPlayPause = { viewModel.togglePlayPause() },
            onClose = { onBack() },
            currentPosition = currentPosition,
            duration = duration,
            onSeek = { progress -> viewModel.seekTo((progress * duration).toLong()) },
            modifier = Modifier.fillMaxSize()
        )
        
        // Enhanced speed memory toast notifications
        SpeedMemoryToastContainer(
            currentToast = currentToast,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 50.dp)
        )
        
        // Error handling
        errorState?.let { (error, recoveryResult) ->
            when (recoveryResult) {
                is ErrorRecoveryResult.Fatal -> {
                    LoadingErrorScreen(
                        error = error,
                        onRetry = { viewModel.retryLastOperation() },
                        onBack = onBack,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    ErrorDialog(
                        error = error,
                        recoveryResult = recoveryResult,
                        onDismiss = { viewModel.clearError() },
                        onRetry = { viewModel.retryLastOperation() },
                        onAction = {
                            when (recoveryResult) {
                                is ErrorRecoveryResult.UserAction -> {
                                    viewModel.executeRecoveryAction(recoveryResult.action)
                                }
                                else -> {}
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Player controls overlay
 */
@Composable
private fun PlayerControlsOverlay(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    bufferedPercentage: Int,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onBack: () -> Unit,
    onSubtitleClick: () -> Unit,
    onQualityClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onAudioTrackClick: () -> Unit,
    onPipClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onStatsClick: () -> Unit,
    onAISceneDetectionClick: () -> Unit,
    onLoopModeClick: () -> Unit,
    currentLoopMode: LoopMode,
    onLockClick: () -> Unit,
    detectedScenes: List<com.astralplayer.nextplayer.feature.ai.DetectedScene> = emptyList(),
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Gradient background
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
        
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            // Settings buttons removed - all settings now in bubble quick settings menu
        }
        
        // Center play/pause button
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.Center)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        
        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            // Lock button in bottom right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                LockControlButton(
                    onLock = onLockClick,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .padding(8.dp)
                )
            }
            
            // Seek bar
            VideoSeekBar(
                currentPosition = currentPosition,
                duration = duration,
                bufferedPercentage = bufferedPercentage,
                onSeekTo = onSeekTo,
                detectedScenes = detectedScenes,
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
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Video seek bar
 */
@Composable
private fun VideoSeekBar(
    currentPosition: Long,
    duration: Long,
    bufferedPercentage: Int,
    onSeekTo: (Long) -> Unit,
    detectedScenes: List<com.astralplayer.nextplayer.feature.ai.DetectedScene> = emptyList(),
    modifier: Modifier = Modifier
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0f) }
    
    Box(modifier = modifier) {
        // Main seek bar
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
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
        
        // Scene markers overlay
        if (detectedScenes.isNotEmpty() && duration > 0) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp) // Account for slider track padding
            ) {
                val trackWidth = size.width - 32.dp.toPx() // Account for thumb size
                
                detectedScenes.forEach { scene ->
                    val sceneProgress = scene.startTime.toFloat() / duration
                    val markerX = sceneProgress * trackWidth + 16.dp.toPx()
                    
                    val markerColor = when (scene.sceneType) {
                        com.astralplayer.nextplayer.feature.ai.SceneType.ACTION -> androidx.compose.ui.graphics.Color.Red
                        com.astralplayer.nextplayer.feature.ai.SceneType.DIALOGUE -> androidx.compose.ui.graphics.Color.Blue
                        com.astralplayer.nextplayer.feature.ai.SceneType.LANDSCAPE -> androidx.compose.ui.graphics.Color.Green
                        else -> androidx.compose.ui.graphics.Color.Yellow
                    }
                    
                    drawLine(
                        color = markerColor.copy(alpha = 0.9f),
                        start = androidx.compose.ui.geometry.Offset(markerX, 0f),
                        end = androidx.compose.ui.geometry.Offset(markerX, size.height),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }
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