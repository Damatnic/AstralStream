package com.astralplayer.nextplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.astralplayer.nextplayer.ui.components.*
import com.astralplayer.nextplayer.ui.dialogs.SubtitleSelectionDialog
import com.astralplayer.nextplayer.ui.dialogs.SubtitleStyleDialog
import com.astralplayer.nextplayer.ui.dialogs.AudioEqualizerDialog
import com.astralplayer.nextplayer.viewmodel.EnhancedPlayerViewModel
import com.astralplayer.nextplayer.data.repository.PlaylistRepository
import com.astralplayer.nextplayer.data.repository.SettingsRepository
import com.astralplayer.nextplayer.feature.playback.ABRepeatManager
import com.astralplayer.nextplayer.feature.playback.FrameNavigator
import com.astralplayer.nextplayer.feature.display.AspectRatioManager
import com.astralplayer.nextplayer.feature.subtitle.AdvancedSubtitleManager
import com.astralplayer.nextplayer.feature.subtitle.SubtitleStyle
import com.astralplayer.nextplayer.feature.ai.AISubtitleGenerator as EnhancedAISubtitleGenerator
import com.astralplayer.nextplayer.audio.AudioEqualizerManager
import com.astralplayer.nextplayer.feature.playback.SleepTimerManager
import com.astralplayer.nextplayer.feature.playback.PlaybackScheduler
import com.astralplayer.nextplayer.ui.dialogs.PlaybackScheduleDialog
import com.astralplayer.nextplayer.ui.components.SleepTimerIndicator
import com.astralplayer.nextplayer.ui.dialogs.SleepTimerDialog as EnhancedSleepTimerDialog
import com.astralplayer.nextplayer.bookmark.VideoBookmarkManager
import com.astralplayer.nextplayer.ui.dialogs.VideoBookmarksDialog
import com.astralplayer.nextplayer.ui.components.ChapterNavigationBar
import com.astralplayer.nextplayer.ui.components.CompactChapterList
import com.astralplayer.nextplayer.gesture.mxPlayerGestures
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Advanced Video Player Screen with comprehensive bubble UI design
 * Features gesture controls, overlays, and modern playback interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedVideoPlayerScreen(
    viewModel: EnhancedPlayerViewModel,
    videoUri: String,
    videoTitle: String,
    playlistRepository: PlaylistRepository,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    
    // Managers and state
    val abRepeatManager = remember { ABRepeatManager() }
    val frameNavigator = remember { FrameNavigator(viewModel.getExoPlayer()) }
    val aspectRatioManager = remember { AspectRatioManager() }
    val aiSubtitleGenerator = remember { EnhancedAISubtitleGenerator(context) }
    val subtitleManager = remember { 
        AdvancedSubtitleManager(
            context = context,
            exoPlayer = viewModel.getExoPlayer(),
            aiSubtitleGenerator = aiSubtitleGenerator
        )
    }
    val audioEqualizerManager = remember { 
        AudioEqualizerManager(viewModel.getExoPlayer().audioSessionId)
    }
    
    val sleepTimerManager = remember { 
        SleepTimerManager(context).apply {
            setPlayer(viewModel.getExoPlayer())
        }
    }
    
    val playbackScheduler = remember { 
        PlaybackScheduler(context)
    }
    
    val bookmarkManager = remember {
        VideoBookmarkManager(
            context = context,
            thumbnailGenerator = com.astralplayer.nextplayer.utils.ThumbnailGenerator(context)
        )
    }
    
    // UI State
    var showControls by remember { mutableStateOf(true) }
    var showQuickSettings by remember { mutableStateOf(false) }
    var showPlaybackSpeed by remember { mutableStateOf(false) }
    var showSubtitleSettings by remember { mutableStateOf(false) }
    var showSubtitleStyle by remember { mutableStateOf(false) }
    var showAudioTracks by remember { mutableStateOf(false) }
    var showVideoInfo by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showABRepeat by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    
    // Playback state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var bufferedPercentage by remember { mutableStateOf(0) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var volume by remember { mutableStateOf(1f) }
    var brightness by remember { mutableStateOf(0.5f) }
    
    // Gesture feedback states
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var showSeekOverlay by remember { mutableStateOf(false) }
    var seekDelta by remember { mutableStateOf(0L) }
    
    // Subtitle states
    val currentSubtitles by subtitleManager.currentSubtitles.collectAsState()
    val activeSubtitle by subtitleManager.activeSubtitle.collectAsState()
    val currentCues by subtitleManager.currentCues.collectAsState()
    val subtitleStyle by subtitleManager.subtitleStyle.collectAsState()
    
    // Bookmark and chapter states
    val chapters by bookmarkManager.getChaptersForVideo(videoUri).collectAsState(initial = null)
    
    // Auto-hide controls
    LaunchedEffect(showControls, isLocked) {
        if (showControls && !isLocked) {
            delay(5000)
            showControls = false
        }
    }
    
    // Update playback state
    LaunchedEffect(Unit) {
        viewModel.playbackState.collect { state ->
            isPlaying = state.isPlaying
            currentPosition = state.currentPosition
            duration = state.duration
            bufferedPercentage = state.bufferedPercentage
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            audioEqualizerManager.release()
            subtitleManager.release()
            sleepTimerManager.release()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!isLocked) {
                            showControls = !showControls
                        }
                    }
                )
            }
    ) {
        // Video Player Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = viewModel.getExoPlayer().also { exoPlayer ->
                        val mediaItem = androidx.media3.common.MediaItem.fromUri(videoUri)
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .mxPlayerGestures(
                    gestureManager = viewModel.gestureManager,
                    screenWidth = configuration.screenWidthDp.toFloat(),
                    screenHeight = configuration.screenHeightDp.toFloat()
                )
        )
        
        // Gesture Overlays
        AnimatedVisibility(
            visible = showVolumeOverlay,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            VolumeGestureOverlay(
                volume = volume,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp)
            )
        }
        
        AnimatedVisibility(
            visible = showBrightnessOverlay,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            BrightnessGestureOverlay(
                brightness = brightness,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
            )
        }
        
        AnimatedVisibility(
            visible = showSeekOverlay,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            SeekGestureOverlay(
                currentPosition = currentPosition,
                seekDelta = seekDelta,
                duration = duration,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Main Controls
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
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
                // Top Controls
                TopControlBar(
                    title = videoTitle,
                    onBack = onBack,
                    onSettings = { showQuickSettings = true },
                    onInfo = { showVideoInfo = true },
                    onLock = { isLocked = true },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                
                // Center Controls
                CenterPlaybackControls(
                    isPlaying = isPlaying,
                    onPlayPause = {
                        isPlaying = !isPlaying
                        if (isPlaying) viewModel.play() else viewModel.pause()
                    },
                    onSeekBack = {
                        viewModel.seekTo(currentPosition - 10000)
                    },
                    onSeekForward = {
                        viewModel.seekTo(currentPosition + 10000)
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // Bottom Controls
                BottomControlBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    bufferedPercentage = bufferedPercentage,
                    isPlaying = isPlaying,
                    playbackSpeed = playbackSpeed,
                    onSeek = { position ->
                        viewModel.seekTo(position)
                    },
                    onPlayPause = {
                        isPlaying = !isPlaying
                        if (isPlaying) viewModel.play() else viewModel.pause()
                    },
                    onSpeedClick = { showPlaybackSpeed = true },
                    onSubtitleClick = { showSubtitleSettings = true },
                    onAudioClick = { showAudioTracks = true },
                    onABRepeatClick = { showABRepeat = true },
                    onPipClick = onEnterPip,
                    onTimerClick = { showSleepTimer = true },
                    onScheduleClick = { showScheduleDialog = true },
                    onBookmarksClick = { showBookmarks = true },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        
        // Chapter Navigation
        AnimatedVisibility(
            visible = showControls && !isLocked && chapters != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ChapterNavigationBar(
                chapters = chapters,
                currentPosition = currentPosition,
                onChapterClick = { chapter ->
                    viewModel.seekTo(chapter.bookmark.position)
                },
                modifier = Modifier.padding(top = 100.dp)
            )
        }
        
        // Lock Screen Overlay
        AnimatedVisibility(
            visible = isLocked,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            LockScreenOverlay(
                onUnlock = { isLocked = false }
            )
        }
        
        // Subtitle Overlay
        if (activeSubtitle != null && currentCues.isNotEmpty()) {
            SubtitleOverlay(
                subtitle = currentCues.firstOrNull()?.text?.toString(),
                style = com.astralplayer.nextplayer.ui.components.SubtitleStyle(
                    fontSize = subtitleStyle.textSize.sp,
                    textColor = Color(subtitleStyle.textColor),
                    backgroundColor = Color(subtitleStyle.backgroundColor),
                    isBold = subtitleStyle.fontScale > 1.0f,
                    cornerRadius = subtitleStyle.cornerRadius.dp,
                    horizontalPadding = subtitleStyle.horizontalPadding.dp,
                    verticalPadding = subtitleStyle.verticalPadding.dp
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Quick Settings Menu
        if (showQuickSettings) {
            BubbleQuickSettingsMenu(
                onDismiss = { showQuickSettings = false },
                onSpeedClick = {
                    showQuickSettings = false
                    showPlaybackSpeed = true
                },
                onQualityClick = { /* Handle quality */ },
                onAudioTrackClick = {
                    showQuickSettings = false
                    showAudioTracks = true
                },
                onSubtitleClick = {
                    showQuickSettings = false
                    showSubtitleSettings = true
                },
                onSleepTimerClick = {
                    showQuickSettings = false
                    showSleepTimer = true
                }
            )
        }
        
        // Playback Speed Dialog
        if (showPlaybackSpeed) {
            PlaybackSpeedDialog(
                currentSpeed = playbackSpeed,
                onSpeedSelected = { speed ->
                    playbackSpeed = speed
                    viewModel.setPlaybackSpeed(speed)
                    showPlaybackSpeed = false
                },
                onDismiss = { showPlaybackSpeed = false }
            )
        }
        
        
        // AB Repeat Dialog
        if (showABRepeat) {
            ABRepeatDialog(
                abRepeatManager = abRepeatManager,
                currentPosition = currentPosition,
                onDismiss = { showABRepeat = false }
            )
        }
        
        // Subtitle Selection Dialog
        if (showSubtitleSettings) {
            SubtitleSelectionDialog(
                subtitleManager = subtitleManager,
                onDismiss = { showSubtitleSettings = false },
                onSubtitleSelected = { subtitle ->
                    // Subtitle selection is handled by the dialog
                    showSubtitleSettings = false
                }
            )
        }
        
        // Subtitle Style Dialog
        if (showSubtitleStyle) {
            SubtitleStyleDialog(
                currentStyle = subtitleStyle,
                onStyleChanged = { newStyle ->
                    subtitleManager.updateSubtitleStyle(newStyle)
                },
                onDismiss = { showSubtitleStyle = false }
            )
        }
        
        // Audio Equalizer Dialog
        if (showAudioTracks) {
            AudioEqualizerDialog(
                equalizerManager = audioEqualizerManager,
                onDismiss = { showAudioTracks = false }
            )
        }
        
        // Sleep Timer Dialog
        if (showSleepTimer) {
            EnhancedSleepTimerDialog(
                onTimerSet = { minutes ->
                    sleepTimerManager.setTimer(minutes)
                    showSleepTimer = false
                },
                onDismiss = { showSleepTimer = false }
            )
        }
        
        // Playback Schedule Dialog
        if (showScheduleDialog) {
            PlaybackScheduleDialog(
                scheduler = playbackScheduler,
                currentVideoUri = videoUri,
                currentVideoTitle = videoTitle,
                onDismiss = { showScheduleDialog = false }
            )
        }
        
        // Sleep Timer Indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
        ) {
            SleepTimerIndicator(
                sleepTimerManager = sleepTimerManager,
                onClick = { showSleepTimer = true }
            )
        }
        
        // Bookmarks Dialog
        if (showBookmarks) {
            VideoBookmarksDialog(
                bookmarkManager = bookmarkManager,
                videoUri = videoUri,
                videoTitle = videoTitle,
                currentPosition = currentPosition,
                duration = duration,
                onSeekTo = { position ->
                    viewModel.seekTo(position)
                },
                onDismiss = { showBookmarks = false }
            )
        }
    }
}

@Composable
private fun TopControlBar(
    title: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onInfo: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            BubbleIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                size = 40,
                iconSize = 20,
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BubbleIconButton(
                onClick = onInfo,
                icon = Icons.Default.Info,
                size = 40,
                iconSize = 20,
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            )
            
            BubbleIconButton(
                onClick = onSettings,
                icon = Icons.Default.Settings,
                size = 40,
                iconSize = 20,
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            )
            
            BubbleIconButton(
                onClick = onLock,
                icon = Icons.Default.Lock,
                size = 40,
                iconSize = 20,
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            )
        }
    }
}

@Composable
private fun CenterPlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BubbleIconButton(
            onClick = onSeekBack,
            icon = Icons.Default.Replay10,
            size = 64,
            iconSize = 32,
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White
        )
        
        BubbleIconButton(
            onClick = onPlayPause,
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            size = 80,
            iconSize = 40,
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            contentColor = Color.White
        )
        
        BubbleIconButton(
            onClick = onSeekForward,
            icon = Icons.Default.Forward10,
            size = 64,
            iconSize = 32,
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White
        )
    }
}

@Composable
private fun BottomControlBar(
    currentPosition: Long,
    duration: Long,
    bufferedPercentage: Int,
    isPlaying: Boolean,
    playbackSpeed: Float,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onSpeedClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onAudioClick: () -> Unit,
    onABRepeatClick: () -> Unit,
    onPipClick: () -> Unit,
    onTimerClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress Bar
        VideoProgressBar(
            currentPosition = currentPosition,
            duration = duration,
            bufferedPercentage = bufferedPercentage,
            onSeek = onSeek
        )
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BubbleIconButton(
                    onClick = onPlayPause,
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    size = 40,
                    iconSize = 20,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
                
                Text(
                    text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            
            // Right Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speed Control
                BubbleCard(
                    onClick = onSpeedClick,
                    elevation = 0,
                    cornerRadius = 20,
                    containerColor = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                
                BubbleIconButton(
                    onClick = onSubtitleClick,
                    icon = Icons.Default.Subtitles,
                    size = 40,
                    iconSize = 20,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
                
                BubbleIconButton(
                    onClick = onAudioClick,
                    icon = Icons.Default.AudioTrack,
                    size = 40,
                    iconSize = 20,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
                
                BubbleIconButton(
                    onClick = onABRepeatClick,
                    icon = Icons.Default.Repeat,
                    size = 40,
                    iconSize = 20,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
                
                BubbleIconButton(
                    onClick = onTimerClick,
                    icon = Icons.Default.Timer,
                    size = 40,
                    iconSize = 20,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
                
                BubbleIconButton(
                    onClick = onScheduleClick,
                    icon = Icons.Default.Schedule,
                    size = 40,
                    iconSize = 20,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
                
                BubbleIconButton(
                    onClick = onBookmarksClick,
                    icon = Icons.Default.Bookmarks,
                    size = 40,
                    iconSize = 20,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
                
                BubbleIconButton(
                    onClick = onPipClick,
                    icon = Icons.Default.PictureInPicture,
                    size = 40,
                    iconSize = 20,
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            }
        }
    }
}

@Composable
private fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPercentage: Int,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            // Buffered Progress
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(bufferedPercentage / 100f)
                    .background(Color.White.copy(alpha = 0.5f))
            )
            
            // Played Progress
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(
                        if (isDragging) dragPosition 
                        else currentPosition.toFloat() / duration.toFloat()
                    )
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Seek indicator
        if (isDragging) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = formatTime((dragPosition * duration).toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.align(
                        when {
                            dragPosition < 0.1f -> Alignment.CenterStart
                            dragPosition > 0.9f -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun VolumeGestureOverlay(
    volume: Float,
    modifier: Modifier = Modifier
) {
    GestureOverlayIndicator(
        icon = if (volume > 0) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
        value = volume,
        label = "Volume",
        modifier = modifier
    )
}

@Composable
private fun BrightnessGestureOverlay(
    brightness: Float,
    modifier: Modifier = Modifier
) {
    GestureOverlayIndicator(
        icon = Icons.Default.BrightnessHigh,
        value = brightness,
        label = "Brightness",
        modifier = modifier
    )
}

@Composable
private fun SeekGestureOverlay(
    currentPosition: Long,
    seekDelta: Long,
    duration: Long,
    modifier: Modifier = Modifier
) {
    val targetPosition = (currentPosition + seekDelta).coerceIn(0, duration)
    
    BubbleCard(
        modifier = modifier,
        elevation = 12,
        cornerRadius = 20,
        containerColor = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (seekDelta > 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            
            Text(
                text = "${if (seekDelta > 0) "+" else ""}${seekDelta / 1000}s",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = formatTime(targetPosition),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun GestureOverlayIndicator(
    icon: ImageVector,
    value: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    BubbleCard(
        modifier = modifier,
        elevation = 12,
        cornerRadius = 16,
        containerColor = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            
            // Progress Indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(1f - value)
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun LockScreenOverlay(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { /* Consume clicks */ },
        contentAlignment = Alignment.Center
    ) {
        BubbleCard(
            onClick = onUnlock,
            elevation = 12,
            cornerRadius = 24,
            containerColor = Color.Black.copy(alpha = 0.8f)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = "Unlock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "Tap to unlock controls",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Playback Speed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                speeds.forEach { speed ->
                    BubbleCard(
                        onClick = { onSpeedSelected(speed) },
                        cornerRadius = 12,
                        elevation = if (speed == currentSpeed) 6 else 2,
                        containerColor = if (speed == currentSpeed)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${speed}x",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal,
                                color = if (speed == currentSpeed)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (speed == currentSpeed) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            BubbleButton(
                onClick = onDismiss,
                text = "Done",
                cornerRadius = 20
            )
        }
    )
}

@Composable
private fun SleepTimerDialog(
    onTimerSet: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timerOptions = listOf(15, 30, 45, 60, 90, 120)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Sleep Timer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timerOptions.forEach { minutes ->
                    BubbleCard(
                        onClick = { onTimerSet(minutes) },
                        cornerRadius = 12,
                        elevation = 2
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "$minutes minutes",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            BubbleButton(
                onClick = onDismiss,
                text = "Cancel",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cornerRadius = 20
            )
        }
    )
}

@Composable
private fun ABRepeatDialog(
    abRepeatManager: ABRepeatManager,
    currentPosition: Long,
    onDismiss: () -> Unit
) {
    var pointA by remember { mutableStateOf(abRepeatManager.pointA) }
    var pointB by remember { mutableStateOf(abRepeatManager.pointB) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "A-B Repeat",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Point A
                BubbleCard(
                    cornerRadius = 12,
                    elevation = 2
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Point A",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (pointA != null) formatTime(pointA!!) else "--:--",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            BubbleButton(
                                onClick = {
                                    pointA = currentPosition
                                    abRepeatManager.setPointA(currentPosition)
                                },
                                text = "Set",
                                cornerRadius = 16,
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }
                
                // Point B
                BubbleCard(
                    cornerRadius = 12,
                    elevation = 2
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Point B",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (pointB != null) formatTime(pointB!!) else "--:--",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            BubbleButton(
                                onClick = {
                                    pointB = currentPosition
                                    abRepeatManager.setPointB(currentPosition)
                                },
                                text = "Set",
                                cornerRadius = 16,
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }
                
                // Clear Button
                if (pointA != null || pointB != null) {
                    BubbleButton(
                        onClick = {
                            pointA = null
                            pointB = null
                            abRepeatManager.clear()
                        },
                        text = "Clear A-B Repeat",
                        icon = Icons.Default.Clear,
                        cornerRadius = 20,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            BubbleButton(
                onClick = onDismiss,
                text = "Done",
                cornerRadius = 20
            )
        }
    )
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

// Legacy compatibility wrapper
@Composable
fun AdvancedVideoPlayerScreen(
    viewModel: EnhancedPlayerViewModel,
    videoUri: String,
    videoTitle: String,
    playlistRepository: PlaylistRepository,
    onBack: () -> Unit,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsRepository = remember { 
        com.astralplayer.nextplayer.data.repository.SettingsRepositoryImpl(context) 
    }
    
    AdvancedVideoPlayerScreen(
        viewModel = viewModel,
        videoUri = videoUri,
        videoTitle = videoTitle,
        playlistRepository = playlistRepository,
        settingsRepository = settingsRepository,
        onBack = onBack,
        isInPipMode = isInPipMode,
        onEnterPip = onEnterPip,
        modifier = modifier
    )
}