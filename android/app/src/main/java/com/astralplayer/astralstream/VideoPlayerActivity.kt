package com.astralplayer.astralstream

import android.content.pm.ActivityInfo
import android.net.Uri
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
import androidx.media3.common.MimeTypes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.datasource.DefaultDataSource
import com.astralplayer.astralstream.ui.components.VideoPlayer
import com.astralplayer.astralstream.ui.theme.AstralStreamTheme
import com.astralplayer.astralstream.viewmodel.VideoPlayerViewModel
import com.astralplayer.astralstream.ai.AISubtitleGenerator
// import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.io.File
// import javax.inject.Inject

// @AndroidEntryPoint
class VideoPlayerActivity : ComponentActivity() {
    
    private val videoPlayerViewModel: VideoPlayerViewModel by viewModels()
    private var exoPlayer: ExoPlayer? = null
    
    // @Inject
    // lateinit var aiSubtitleGenerator: AISubtitleGenerator
    private val aiSubtitleGenerator by lazy { AISubtitleGenerator(this) }
    
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
                            // Start AI subtitle generation when video is ready
                            startAISubtitleGeneration(videoUrl)
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
        aiSubtitleGenerator.release()
    }
    
    private fun startAISubtitleGeneration(videoUrl: String) {
        lifecycleScope.launch {
            try {
                val videoUri = Uri.parse(videoUrl)
                aiSubtitleGenerator.generate(videoUri) { subtitles ->
                    // Apply subtitles to player when generated
                    applySubtitlesToPlayer(subtitles)
                }
            } catch (e: Exception) {
                // Handle error silently - subtitles are optional
            }
        }
    }
    
    private fun applySubtitlesToPlayer(subtitles: List<AISubtitleGenerator.Subtitle>) {
        exoPlayer?.let { player ->
            try {
                // Create SRT subtitle file
                val subtitleContent = createSRTContent(subtitles)
                val subtitleFile = File(cacheDir, "ai_subtitles_${System.currentTimeMillis()}.srt")
                subtitleFile.writeText(subtitleContent)
                
                // Create subtitle media source
                val subtitleUri = Uri.fromFile(subtitleFile)
                val dataSourceFactory = DefaultDataSource.Factory(this)
                
                val subtitleSource = SingleSampleMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(
                        MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                            .setLanguage("en")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build(),
                        C.TIME_UNSET
                    )
                
                // Get current media item and create merged source
                player.currentMediaItem?.let { mediaItem ->
                    val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                    
                    val mergedSource = MergingMediaSource(videoSource, subtitleSource)
                    
                    // Save current position
                    val currentPosition = player.currentPosition
                    val wasPlaying = player.isPlaying
                    
                    // Set new media source with subtitles
                    player.setMediaSource(mergedSource)
                    player.prepare()
                    player.seekTo(currentPosition)
                    player.playWhenReady = wasPlaying
                }
                
            } catch (e: Exception) {
                // Handle error silently - subtitles are optional
            }
        }
    }
    
    private fun createSRTContent(subtitles: List<AISubtitleGenerator.Subtitle>): String {
        val builder = StringBuilder()
        subtitles.forEachIndexed { index, subtitle ->
            builder.append("${index + 1}\n")
            builder.append("${formatTime(subtitle.startTime)} --> ${formatTime(subtitle.endTime)}\n")
            builder.append("${subtitle.translatedText ?: subtitle.text}\n\n")
        }
        return builder.toString()
    }
    
    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return String.format(
            "%02d:%02d:%02d,%03d",
            hours,
            minutes % 60,
            seconds % 60,
            millis % 1000
        )
    }
}