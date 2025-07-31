package com.astralplayer.astralstream

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.astralstream.player.AdvancedPlayerConfiguration
import com.astralplayer.astralstream.ui.components.EnhancedVideoPlayerScreen
import com.astralplayer.astralstream.ui.theme.AstralStreamTheme
import com.astralplayer.astralstream.viewmodel.VideoPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.astralplayer.astralstream.gesture.EnhancedGestureManager
import com.astralplayer.astralstream.ai.AISubtitleGenerator
import com.astralplayer.astralstream.cloud.CloudStorageManager

@AndroidEntryPoint
class VideoPlayerActivityElite : ComponentActivity() {
    
    private val videoPlayerViewModel: VideoPlayerViewModel by viewModels()
    
    @Inject lateinit var playerConfiguration: AdvancedPlayerConfiguration
    @Inject lateinit var aiSubtitleGenerator: AISubtitleGenerator
    @Inject lateinit var cloudStorageManager: CloudStorageManager
    
    private var exoPlayer: ExoPlayer? = null
    private lateinit var gestureManager: EnhancedGestureManager
    
    companion object {
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_IS_CLOUD = "is_cloud"
        const val EXTRA_CLOUD_PROVIDER = "cloud_provider"
        const val NAME = "AstralStream Elite"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up full screen and keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        enableEdgeToEdge()
        
        // Get video details from intent
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: ""
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Video"
        val videoId = intent.getLongExtra(EXTRA_VIDEO_ID, -1)
        val isCloud = intent.getBooleanExtra(EXTRA_IS_CLOUD, false)
        val cloudProvider = intent.getStringExtra(EXTRA_CLOUD_PROVIDER)
        
        // Initialize gesture manager
        initializeGestureManager()
        
        // Initialize ExoPlayer with advanced configuration
        initializePlayer(videoUrl, isCloud)
        
        // Load video data
        if (videoId > 0) {
            videoPlayerViewModel.loadVideo(videoId)
        }
        
        // Start AI subtitle generation if needed
        if (videoPlayerViewModel.isAISubtitlesEnabled()) {
            generateAISubtitles(videoUrl)
        }
        
        setContent {
            val isDarkTheme by videoPlayerViewModel.isDarkTheme.collectAsState()
            val isFullscreen by videoPlayerViewModel.isFullscreen.collectAsState()
            val showControls by videoPlayerViewModel.showControls.collectAsState()
            val currentPosition by videoPlayerViewModel.currentPosition.collectAsState()
            val duration by videoPlayerViewModel.duration.collectAsState()
            val isPlaying by videoPlayerViewModel.isPlaying.collectAsState()
            val isBuffering by videoPlayerViewModel.isBuffering.collectAsState()
            val subtitles by videoPlayerViewModel.subtitles.collectAsState()
            val playbackSpeed by videoPlayerViewModel.playbackSpeed.collectAsState()
            val videoQuality by videoPlayerViewModel.videoQuality.collectAsState()
            
            // Update orientation based on fullscreen state
            LaunchedEffect(isFullscreen) {
                requestedOrientation = if (isFullscreen) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
            }
            
            // Update playback position periodically
            LaunchedEffect(isPlaying) {
                while (isPlaying) {
                    exoPlayer?.let { player ->
                        videoPlayerViewModel.updatePosition(player.currentPosition)
                    }
                    delay(100) // Update every 100ms for smooth progress
                }
            }
            
            AstralStreamTheme(darkTheme = isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    exoPlayer?.let { player ->
                        EnhancedVideoPlayerScreen(
                            player = player,
                            videoUri = Uri.parse(videoUrl),
                            videoTitle = videoTitle,
                            isFullscreen = isFullscreen,
                            showControls = showControls,
                            currentPosition = currentPosition,
                            duration = duration,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            subtitles = subtitles,
                            playbackSpeed = playbackSpeed,
                            videoQuality = videoQuality,
                            gestureManager = gestureManager,
                            onPlayPauseClick = {
                                if (player.isPlaying) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            },
                            onSeekTo = { position ->
                                player.seekTo(position.toLong())
                                videoPlayerViewModel.updatePosition(position.toLong())
                            },
                            onFullscreenToggle = {
                                videoPlayerViewModel.toggleFullscreen()
                            },
                            onControlsVisibilityChanged = { visible ->
                                videoPlayerViewModel.setControlsVisible(visible)
                            },
                            onSpeedChange = { speed ->
                                player.setPlaybackSpeed(speed)
                                videoPlayerViewModel.setPlaybackSpeed(speed)
                            },
                            onQualityChange = { quality ->
                                videoPlayerViewModel.setVideoQuality(quality)
                                // Implement quality switching logic
                            },
                            onSubtitleToggle = {
                                videoPlayerViewModel.toggleSubtitles()
                            },
                            onBackPressed = {
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
    
    private fun initializeGestureManager() {
        gestureManager = EnhancedGestureManager(
            context = this,
            onSwipeUp = { zone ->
                when (zone) {
                    EnhancedGestureManager.Zone.LEFT -> {
                        // Increase brightness
                        videoPlayerViewModel.adjustBrightness(0.1f)
                    }
                    EnhancedGestureManager.Zone.RIGHT -> {
                        // Increase volume
                        videoPlayerViewModel.adjustVolume(0.1f)
                    }
                }
            },
            onSwipeDown = { zone ->
                when (zone) {
                    EnhancedGestureManager.Zone.LEFT -> {
                        // Decrease brightness
                        videoPlayerViewModel.adjustBrightness(-0.1f)
                    }
                    EnhancedGestureManager.Zone.RIGHT -> {
                        // Decrease volume
                        videoPlayerViewModel.adjustVolume(-0.1f)
                    }
                }
            },
            onSwipeLeft = {
                // Seek backward 10 seconds
                exoPlayer?.let { player ->
                    player.seekTo(maxOf(0, player.currentPosition - 10000))
                }
            },
            onSwipeRight = {
                // Seek forward 10 seconds
                exoPlayer?.let { player ->
                    player.seekTo(minOf(player.duration, player.currentPosition + 10000))
                }
            },
            onDoubleTap = { zone ->
                when (zone) {
                    EnhancedGestureManager.Zone.LEFT -> {
                        // Seek backward 10 seconds
                        exoPlayer?.let { player ->
                            player.seekTo(maxOf(0, player.currentPosition - 10000))
                        }
                    }
                    EnhancedGestureManager.Zone.RIGHT -> {
                        // Seek forward 10 seconds
                        exoPlayer?.let { player ->
                            player.seekTo(minOf(player.duration, player.currentPosition + 10000))
                        }
                    }
                }
            },
            onLongPress = {
                // Toggle 2x speed
                exoPlayer?.let { player ->
                    val currentSpeed = player.playbackParameters.speed
                    val newSpeed = if (currentSpeed == 2.0f) 1.0f else 2.0f
                    player.setPlaybackSpeed(newSpeed)
                    videoPlayerViewModel.setPlaybackSpeed(newSpeed)
                }
            },
            onPinchZoom = { scaleFactor ->
                // Zoom control
                videoPlayerViewModel.setZoomLevel(scaleFactor)
            }
        )
    }
    
    private fun initializePlayer(videoUrl: String, isCloud: Boolean) {
        exoPlayer = playerConfiguration.createPlayer().apply {
            val mediaItem = if (isCloud) {
                // Handle cloud streaming with authentication headers
                MediaItem.Builder()
                    .setUri(videoUrl)
                    .setRequestMetadata(
                        MediaItem.RequestMetadata.Builder()
                            .setExtras(cloudStorageManager.getAuthHeaders())
                            .build()
                    )
                    .build()
            } else {
                MediaItem.fromUri(videoUrl)
            }
            
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            videoPlayerViewModel.onVideoReady(duration)
                        }
                        Player.STATE_ENDED -> {
                            videoPlayerViewModel.onVideoEnded()
                        }
                        Player.STATE_BUFFERING -> {
                            videoPlayerViewModel.setBuffering(true)
                        }
                        else -> {
                            videoPlayerViewModel.setBuffering(false)
                        }
                    }
                }
                
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    videoPlayerViewModel.setPlaying(isPlaying)
                }
                
                override fun onPlayerError(error: PlaybackException) {
                    // Error recovery system
                    handlePlaybackError(error)
                }
            })
        }
    }
    
    private fun generateAISubtitles(videoUrl: String) {
        lifecycleScope.launch {
            try {
                aiSubtitleGenerator.generate(Uri.parse(videoUrl)) { subtitles ->
                    videoPlayerViewModel.setSubtitles(subtitles)
                }
            } catch (e: Exception) {
                // Handle subtitle generation error
                videoPlayerViewModel.onSubtitleGenerationError(e)
            }
        }
    }
    
    private fun handlePlaybackError(error: PlaybackException) {
        // Implement error recovery system
        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                // Retry with lower quality
                videoPlayerViewModel.retryWithLowerQuality()
            }
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> {
                // Switch to software decoder
                videoPlayerViewModel.switchToSoftwareDecoder()
            }
            else -> {
                // Show error dialog
                videoPlayerViewModel.showError(error.message ?: "Playback error")
            }
        }
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && exoPlayer?.isPlaying == true) {
            enterPictureInPictureMode()
        }
    }
    
    private fun enterPictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val visibleRect = Rect()
            val aspectRatio = Rational(16, 9)
            
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setSourceRectHint(visibleRect)
                .build()
                
            enterPictureInPictureMode(params)
        }
    }
    
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        videoPlayerViewModel.setPictureInPictureMode(isInPictureInPictureMode)
    }
    
    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
        
        // Save playback position and speed
        exoPlayer?.let { player ->
            val videoId = intent.getLongExtra(EXTRA_VIDEO_ID, -1)
            if (videoId > 0) {
                videoPlayerViewModel.savePlaybackState(
                    videoId = videoId,
                    position = player.currentPosition,
                    speed = player.playbackParameters.speed
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (!isInPictureInPictureMode) {
            exoPlayer?.play()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        gestureManager.release()
        aiSubtitleGenerator.release()
    }
}