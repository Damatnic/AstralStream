package com.astralplayer.astralstream

import android.content.pm.ActivityInfo
import android.os.Bundle
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.astralstream.ui.components.VideoPlayer
import com.astralplayer.astralstream.ui.theme.AstralStreamTheme
import com.astralplayer.astralstream.viewmodel.VideoPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class VideoPlayerActivity : ComponentActivity() {
    
    private val videoPlayerViewModel: VideoPlayerViewModel by viewModels()
    private var exoPlayer: ExoPlayer? = null
    
    companion object {
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_VIDEO_ID = "video_id"
        const name = "AstralStream"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up full screen and keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        
        // Get video details from intent
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: ""
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Video"
        val videoId = intent.getLongExtra(EXTRA_VIDEO_ID, -1)
        
        // Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
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
            })
        }
        
        // Load video data
        if (videoId > 0) {
            videoPlayerViewModel.loadVideo(videoId)
        }
        
        setContent {
            val isDarkTheme by videoPlayerViewModel.isDarkTheme.collectAsState()
            val isFullscreen by videoPlayerViewModel.isFullscreen.collectAsState()
            val showControls by videoPlayerViewModel.showControls.collectAsState()
            val currentPosition by videoPlayerViewModel.currentPosition.collectAsState()
            val duration by videoPlayerViewModel.duration.collectAsState()
            val isPlaying by videoPlayerViewModel.isPlaying.collectAsState()
            val isBuffering by videoPlayerViewModel.isBuffering.collectAsState()
            
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
                    delay(1000) // Update every second
                }
            }
            
            AstralStreamTheme(darkTheme = isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    exoPlayer?.let { player ->
                        VideoPlayer(
                            player = player,
                            title = videoTitle,
                            isFullscreen = isFullscreen,
                            showControls = showControls,
                            currentPosition = currentPosition,
                            duration = duration,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
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
                            onBackPressed = {
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
        
        // Save playback position
        exoPlayer?.let { player ->
            val videoId = intent.getLongExtra(EXTRA_VIDEO_ID, -1)
            if (videoId > 0) {
                videoPlayerViewModel.savePlaybackPosition(videoId, player.currentPosition)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}