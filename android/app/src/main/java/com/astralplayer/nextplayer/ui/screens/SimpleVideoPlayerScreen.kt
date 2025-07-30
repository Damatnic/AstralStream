package com.astralplayer.nextplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import com.astralplayer.nextplayer.viewmodel.SimpleEnhancedPlayerViewModel
import com.astralplayer.nextplayer.VideoPlayerActivity
import com.astralplayer.nextplayer.ui.dialogs.SubtitleSelectionDialog
import com.astralplayer.nextplayer.ui.dialogs.PlaybackSpeedDialog
import com.astralplayer.nextplayer.ui.dialogs.VideoQualityDialog
import com.astralplayer.nextplayer.ui.dialogs.AudioTrackSelectionDialog
import com.astralplayer.nextplayer.ui.dialogs.ABRepeatDialog
import com.astralplayer.nextplayer.ui.dialogs.VideoFiltersDialog
import com.astralplayer.nextplayer.ui.dialogs.VideoExportDialog
import com.astralplayer.nextplayer.ui.dialogs.ExportProgressDialog
import com.astralplayer.nextplayer.data.ABRepeatManager
import com.astralplayer.nextplayer.data.VideoFiltersManager
import kotlinx.coroutines.delay
import android.os.Build
import androidx.media3.exoplayer.ExoPlayer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.cast.ChromecastManager
import com.astralplayer.nextplayer.ui.components.ChromecastMiniController
import com.astralplayer.nextplayer.ui.components.ChromecastExpandedController
import com.astralplayer.nextplayer.ui.components.ChapterIndicator
import com.astralplayer.nextplayer.chapters.VideoChapterManager
import com.astralplayer.nextplayer.ui.dialogs.VideoChaptersDialog
import com.google.android.gms.cast.framework.CastButtonFactory

@Composable
fun VideoPlayerScreen(playerView: PlayerView) {
    AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(UnstableApi::class)
@Composable
fun SimpleVideoPlayerScreen(
    playerView: PlayerView,
    viewModel: SimpleEnhancedPlayerViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var showControls by remember { mutableStateOf(true) }
    
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
    
    AndroidView(
        factory = { playerView },
        modifier = modifier.fillMaxSize()
    )
}

@OptIn(UnstableApi::class)
@Composable
fun SimpleVideoPlayerWithGestures(
    playerView: PlayerView,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showABRepeatDialog by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportProgressDialog by remember { mutableStateOf(false) }
    var showChaptersDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val exoPlayer = playerView.player as ExoPlayer
    val abRepeatManager = remember { ABRepeatManager(exoPlayer, scope) }
    val abRepeatState by abRepeatManager.abRepeatState.collectAsState()
    val filtersManager = remember { VideoFiltersManager(exoPlayer) }
    val filtersState by filtersManager.filterState.collectAsState()
    
    // Export manager
    val context = LocalContext.current
    val exportManager = remember { com.astralplayer.nextplayer.export.VideoExportManager(context) }
    val exportState by exportManager.exportState.collectAsState()
    val exportProgress by exportManager.exportProgress.collectAsState()
    
    // Chromecast manager
    val chromecastManager = remember { ChromecastManager(context) }
    val castState by chromecastManager.castState.collectAsState()
    var showChromecastControls by remember { mutableStateOf(false) }
    
    // Chapter manager
    val chapterManager = remember { VideoChapterManager(context, exoPlayer) }
    val chapters by chapterManager.chapters.collectAsState()
    val currentChapterIndex by chapterManager.currentChapterIndex.collectAsState()
    
    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }
    
    // Detect chapters when media item changes
    LaunchedEffect(exoPlayer.currentMediaItem) {
        exoPlayer.currentMediaItem?.let { mediaItem ->
            val duration = exoPlayer.duration
            if (duration > 0) {
                chapterManager.detectChapters(mediaItem, duration)
            }
        }
    }
    
    // Update current chapter position
    LaunchedEffect(Unit) {
        while (true) {
            if (exoPlayer.isPlaying) {
                chapterManager.updateCurrentChapter(exoPlayer.currentPosition)
            }
            delay(1000) // Update every second
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
                    useController = !showControls // Use our custom controls when showing
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = { offset ->
                            // Simple double-tap seek: left = -10s, right = +10s
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                // Seek backward 10 seconds
                                playerView.player?.let { player ->
                                    val newPosition = (player.currentPosition - 10000).coerceAtLeast(0)
                                    player.seekTo(newPosition)
                                }
                            } else {
                                // Seek forward 10 seconds
                                playerView.player?.let { player ->
                                    val newPosition = (player.currentPosition + 10000).coerceAtMost(player.duration)
                                    player.seekTo(newPosition)
                                }
                            }
                        }
                    )
                }
        )
        
        // Custom controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            VideoControlsOverlay(
                playerView = playerView,
                onBack = onBack,
                onHideControls = { showControls = false },
                onShowSubtitles = { showSubtitleDialog = true },
                onShowSpeed = { showSpeedDialog = true },
                onShowQuality = { showQualityDialog = true },
                onShowAudio = { showAudioDialog = true },
                onShowABRepeat = { showABRepeatDialog = true },
                onShowFilters = { showFiltersDialog = true },
                onShowExport = { showExportDialog = true },
                onCast = {
                    // Start casting current video
                    val videoUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
                    val title = exoPlayer.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Video"
                    val position = exoPlayer.currentPosition
                    
                    if (chromecastManager.isConnected() && videoUri != null) {
                        chromecastManager.castVideo(
                            videoUri = videoUri,
                            title = title,
                            position = position
                        )
                        showChromecastControls = true
                    } else {
                        // Show cast dialog
                        chromecastManager.showCastDialog()
                    }
                },
                castState = castState,
                chapters = chapters,
                currentChapterIndex = currentChapterIndex,
                onShowChapters = { showChaptersDialog = true },
                onPreviousChapter = { chapterManager.previousChapter() },
                onNextChapter = { chapterManager.nextChapter() }
            )
        }
    }
    
    // Dialogs
    if (showSubtitleDialog) {
        SubtitleSelectionDialog(
            exoPlayer = playerView.player as ExoPlayer,
            trackSelector = null, // TODO: Pass track selector
            onDismiss = { showSubtitleDialog = false }
        )
    }
    
    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = playerView.player?.playbackParameters?.speed ?: 1.0f,
            onSpeedSelected = { speed ->
                playerView.player?.setPlaybackSpeed(speed)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }
    
    if (showQualityDialog) {
        VideoQualityDialog(
            exoPlayer = playerView.player as ExoPlayer,
            onQualitySelected = { quality ->
                // TODO: Implement quality change logic
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }
    
    if (showAudioDialog) {
        AudioTrackSelectionDialog(
            exoPlayer = playerView.player as ExoPlayer,
            trackSelector = null, // TODO: Pass track selector
            onDismiss = { showAudioDialog = false }
        )
    }
    
    if (showABRepeatDialog) {
        ABRepeatDialog(
            abRepeatState = abRepeatState,
            onSetPointA = { abRepeatManager.setPointA() },
            onSetPointB = { abRepeatManager.setPointB() },
            onClear = { abRepeatManager.clearPoints() },
            onToggle = { abRepeatManager.toggleABRepeat() },
            onDismiss = { showABRepeatDialog = false },
            formattedPointA = abRepeatManager.getFormattedPointA(),
            formattedPointB = abRepeatManager.getFormattedPointB()
        )
    }
    
    if (showFiltersDialog) {
        VideoFiltersDialog(
            filterState = filtersState,
            onBrightnessChange = { filtersManager.setBrightness(it) },
            onContrastChange = { filtersManager.setContrast(it) },
            onSaturationChange = { filtersManager.setSaturation(it) },
            onHueChange = { filtersManager.setHue(it) },
            onRotationChange = { filtersManager.setRotation(it) },
            onZoomChange = { filtersManager.setZoom(it) },
            onGrayscaleToggle = { filtersManager.toggleGrayscale() },
            onInvertToggle = { filtersManager.toggleInverted() },
            onReset = { filtersManager.resetFilters() },
            onDismiss = { showFiltersDialog = false }
        )
    }
    
    if (showExportDialog) {
        // Get video info
        val videoUri = remember {
            // Try to get URI from the player's current media item
            exoPlayer.currentMediaItem?.localConfiguration?.uri 
                ?: android.net.Uri.parse("") // Fallback empty URI
        }
        val videoTitle = remember {
            exoPlayer.currentMediaItem?.mediaMetadata?.title?.toString() 
                ?: "video"
        }
        val videoDuration = remember {
            if (exoPlayer.duration > 0) exoPlayer.duration else 0L
        }
        
        VideoExportDialog(
            videoUri = videoUri,
            videoTitle = videoTitle,
            videoDurationMs = videoDuration,
            onExport = { options ->
                showExportDialog = false
                showExportProgressDialog = true
                
                // Start export - TODO: Fix export implementation
                // scope.launch {
                //     exportManager.exportVideo(options)
                // }
            },
            onDismiss = { showExportDialog = false }
        )
    }
    
    if (showExportProgressDialog) {
        ExportProgressDialog(
            exportState = exportState,
            exportProgress = exportProgress,
            onCancel = {
                exportManager.cancelExport()
                showExportProgressDialog = false
            },
            onDismiss = {
                showExportProgressDialog = false
            },
            onOpenFile = { filePath ->
                // Open the exported file
                try {
                    val file = java.io.File(filePath)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        context,
                        "Unable to open file: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
    
    // Add ChromecastMiniController at the bottom
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        ChromecastMiniController(
            chromecastManager = chromecastManager,
            onExpand = { showChromecastControls = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    
    // Show expanded Chromecast controls
    if (showChromecastControls && castState == ChromecastManager.CastState.CONNECTED) {
        Dialog(
            onDismissRequest = { showChromecastControls = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            ChromecastExpandedController(
                chromecastManager = chromecastManager,
                videoTitle = exoPlayer.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Video",
                onCollapse = { showChromecastControls = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    
    // Show chapters dialog
    if (showChaptersDialog) {
        VideoChaptersDialog(
            chapters = chapters,
            currentChapterIndex = currentChapterIndex,
            onChapterSelected = { index ->
                chapterManager.jumpToChapter(index)
                showChaptersDialog = false
            },
            onAddChapter = {
                // TODO: Implement add chapter functionality
            },
            onDeleteChapter = { index ->
                chapterManager.removeChapter(index)
            },
            onDismiss = { showChaptersDialog = false }
        )
    }
    
    // Clean up on dispose
    DisposableEffect(chromecastManager) {
        onDispose {
            chromecastManager.release()
        }
    }
}

@Composable
fun VideoControlsOverlay(
    playerView: PlayerView,
    onBack: () -> Unit,
    onHideControls: () -> Unit,
    onShowSubtitles: () -> Unit,
    onShowSpeed: () -> Unit,
    onShowQuality: () -> Unit,
    onShowAudio: () -> Unit,
    onShowABRepeat: () -> Unit,
    onShowFilters: () -> Unit,
    onShowExport: () -> Unit,
    onCast: () -> Unit,
    castState: ChromecastManager.CastState,
    chapters: List<com.astralplayer.nextplayer.chapters.VideoChapter>,
    currentChapterIndex: Int,
    onShowChapters: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top gradient and back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            // Top bar buttons
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                // Subtitle button
                IconButton(onClick = onShowSubtitles) {
                    Icon(
                        imageVector = Icons.Filled.ClosedCaption,
                        contentDescription = "Subtitles",
                        tint = Color.White
                    )
                }
                
                // Speed button
                IconButton(onClick = onShowSpeed) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = "Playback Speed",
                        tint = Color.White
                    )
                }
                
                // Quality button
                IconButton(onClick = onShowQuality) {
                    Icon(
                        imageVector = Icons.Filled.HighQuality,
                        contentDescription = "Video Quality",
                        tint = Color.White
                    )
                }
                
                // Audio track button
                IconButton(onClick = onShowAudio) {
                    Icon(
                        imageVector = Icons.Filled.Audiotrack,
                        contentDescription = "Audio Track",
                        tint = Color.White
                    )
                }
                
                // A-B repeat button
                IconButton(onClick = onShowABRepeat) {
                    Icon(
                        imageVector = Icons.Filled.Repeat,
                        contentDescription = "A-B Repeat",
                        tint = Color.White
                    )
                }
                
                // Filters button
                IconButton(onClick = onShowFilters) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Video Filters",
                        tint = Color.White
                    )
                }
                
                // Export button
                IconButton(onClick = onShowExport) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = "Export Video",
                        tint = Color.White
                    )
                }
                
                // Chapters button
                if (chapters.isNotEmpty()) {
                    IconButton(onClick = onShowChapters) {
                        Icon(
                            imageVector = Icons.Filled.ListAlt,
                            contentDescription = "Chapters",
                            tint = Color.White
                        )
                    }
                }
                
                // Cast button
                IconButton(onClick = onCast) {
                    Icon(
                        imageVector = when (castState) {
                            ChromecastManager.CastState.CONNECTED -> Icons.Filled.CastConnected
                            ChromecastManager.CastState.CONNECTING -> Icons.Filled.Cast
                            else -> Icons.Filled.Cast
                        },
                        contentDescription = "Cast",
                        tint = when (castState) {
                            ChromecastManager.CastState.CONNECTED -> MaterialTheme.colorScheme.primary
                            ChromecastManager.CastState.CONNECTING -> Color.Yellow
                            else -> Color.White
                        }
                    )
                }
                
                // PiP button (for Android O and above)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    IconButton(
                        onClick = {
                            val activity = context as? VideoPlayerActivity
                            activity?.enterPiPMode()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureInPicture,
                            contentDescription = "Picture in Picture",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        
        // Center play/pause button
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(80.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
                .clickable {
                    playerView.player?.let { player ->
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (playerView.player?.isPlaying == true) 
                    Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        // Bottom gradient and controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Progress bar
                val player = playerView.player
                var currentPosition by remember { mutableStateOf(0L) }
                var duration by remember { mutableStateOf(0L) }
                
                // Update position every second
                LaunchedEffect(player) {
                    while (true) {
                        player?.let {
                            currentPosition = it.currentPosition
                            duration = if (it.duration > 0) it.duration else 1L
                        }
                        delay(1000)
                    }
                }
                
                // Chapter indicator
                if (chapters.isNotEmpty() && currentChapterIndex >= 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ChapterIndicator(
                            currentChapter = chapters.getOrNull(currentChapterIndex),
                            currentChapterIndex = currentChapterIndex,
                            totalChapters = chapters.size,
                            onClick = onShowChapters
                        )
                    }
                }
                
                // Progress indicator
                LinearProgressIndicator(
                    progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // Seek backward
                IconButton(
                    onClick = {
                        playerView.player?.let { player ->
                            val newPosition = (player.currentPosition - 10000).coerceAtLeast(0)
                            player.seekTo(newPosition)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay10,
                        contentDescription = "Seek backward",
                        tint = Color.White
                    )
                }
                
                // Previous (chapter or video)
                IconButton(
                    onClick = {
                        if (chapters.isNotEmpty()) {
                            onPreviousChapter()
                        } else {
                            // TODO: Previous video
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = if (chapters.isNotEmpty()) "Previous Chapter" else "Previous",
                        tint = Color.White
                    )
                }
                
                // Play/Pause
                IconButton(
                    onClick = {
                        playerView.player?.let { player ->
                            if (player.isPlaying) {
                                player.pause()
                            } else {
                                player.play()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (playerView.player?.isPlaying == true) 
                            Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Next (chapter or video)
                IconButton(
                    onClick = {
                        if (chapters.isNotEmpty()) {
                            onNextChapter()
                        } else {
                            // TODO: Next video
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = if (chapters.isNotEmpty()) "Next Chapter" else "Next",
                        tint = Color.White
                    )
                }
                
                // Seek forward
                IconButton(
                    onClick = {
                        playerView.player?.let { player ->
                            val newPosition = (player.currentPosition + 10000).coerceAtMost(player.duration)
                            player.seekTo(newPosition)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forward10,
                        contentDescription = "Seek forward",
                        tint = Color.White
                    )
                }
                }
            }
        }
    }
}