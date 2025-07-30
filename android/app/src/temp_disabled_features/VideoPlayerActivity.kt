package com.astralplayer.nextplayer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.astralplayer.nextplayer.feature.player.ui.EnhancedVideoPlayerScreen
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import com.astralplayer.nextplayer.feature.player.viewmodel.TestViewModel
import com.astralplayer.nextplayer.ui.theme.AstralTheme
import com.astralplayer.nextplayer.data.SettingsDataStore
import com.astralplayer.nextplayer.feature.ai.AutoSubtitleManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class VideoPlayerActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsDataStore: SettingsDataStore
    
    private lateinit var autoSubtitleManager: AutoSubtitleManager
    
    private val playerViewModel: PlayerViewModel by viewModels()
    private val testViewModel: TestViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply settings
        lifecycleScope.launch {
            settingsDataStore.settings.collect { settings ->
                // Keep screen on during playback based on user preference
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
        
        // Enable auto-rotate based on sensor and user preference
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        
        // Get video URI from intent
        val videoUri = intent.data
        val videoTitle = intent.getStringExtra("video_title") ?: "Video"
        
        if (videoUri == null) {
            Toast.makeText(this, "No video to play", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize AutoSubtitleManager for automatic subtitle generation
        autoSubtitleManager = AutoSubtitleManager(this)
        
        // Initialize ExoPlayer with proper configuration
        initializePlayer(videoUri, videoTitle)
        
        // 🎯 AUTOMATICALLY START SUBTITLE GENERATION when video loads
        lifecycleScope.launch {
            Log.i("VideoPlayerActivity", "🎬 Auto-generating subtitles for: $videoTitle")
            val success = autoSubtitleManager.onVideoLoaded(videoUri, videoTitle)
            if (success) {
                Log.i("VideoPlayerActivity", "✅ Subtitle generation started successfully")
                Toast.makeText(this@VideoPlayerActivity, "Generating AI subtitles...", Toast.LENGTH_SHORT).show()
            } else {
                Log.w("VideoPlayerActivity", "⚠️ Failed to start subtitle generation")
            }
        }
        
        setContent {
            AstralTheme {
                val context = LocalContext.current
                val playerState by playerViewModel.playerState.collectAsState()
                
                EnhancedVideoPlayerScreen(
                    viewModel = playerViewModel,
                    onBack = { finish() }
                )
                
                // Handle player errors
                LaunchedEffect(playerState.hasError) {
                    if (playerState.hasError) {
                        Toast.makeText(
                            context, 
                            "Error playing video: ${playerState.errorMessage}", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun initializePlayer(uri: Uri, title: String) {
        try {
            Log.d("VideoPlayerActivity", "Initializing player with URI: $uri")
            
            // Create data source factory with proper headers
            val dataSourceFactory = DefaultDataSource.Factory(
                this,
                DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(15000)
                    .setReadTimeoutMs(15000)
                    .setUserAgent("AstralPlayer/1.0")
            )
            
            // Create media source factory
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            
            // Create track selector for quality selection
            val trackSelector = DefaultTrackSelector(this).apply {
                setParameters(
                    buildUponParameters()
                        .setMaxVideoSizeSd()
                        .setPreferredAudioLanguage("en")
                        .build()
                )
            }
            
            // Build ExoPlayer
            val player = ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    // Configure player
                    playWhenReady = true
                    repeatMode = Player.REPEAT_MODE_OFF
                    
                    // Add listener for player events
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_IDLE -> Log.d("VideoPlayer", "Player idle")
                                Player.STATE_BUFFERING -> Log.d("VideoPlayer", "Buffering...")
                                Player.STATE_READY -> Log.d("VideoPlayer", "Ready to play")
                                Player.STATE_ENDED -> Log.d("VideoPlayer", "Playback ended")
                            }
                        }
                        
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Log.e("VideoPlayer", "Player error: ${error.message}", error)
                            Toast.makeText(
                                this@VideoPlayerActivity, 
                                "Playback error: ${error.message}", 
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    })
                }
            
            // Create MediaItem with proper metadata
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .build()
                )
                .build()
            
            // Set media item and prepare
            player.setMediaItem(mediaItem)
            player.prepare()
            
            // Initialize view model with player and track selector
            playerViewModel.initializePlayer(player, uri, title)
            playerViewModel.setTrackSelector(trackSelector)
            
            Log.d("VideoPlayerActivity", "Player initialized successfully")
            
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Error initializing player", e)
            Toast.makeText(
                this, 
                "Failed to initialize player: ${e.message}", 
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
    
    override fun onStart() {
        super.onStart()
        playerViewModel.player?.play()
    }
    
    override fun onStop() {
        super.onStop()
        playerViewModel.player?.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up AutoSubtitleManager
        if (::autoSubtitleManager.isInitialized) {
            autoSubtitleManager.release()
        }
        playerViewModel.release()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Handle orientation changes
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Log.d("VideoPlayerActivity", "Switched to landscape mode")
                // Hide system UI for full-screen experience in landscape
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                Log.d("VideoPlayerActivity", "Switched to portrait mode")
                // Show system UI in portrait mode
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        
        // Update screen dimensions in view model
        val displayMetrics = resources.displayMetrics
        val screenWidth = if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            displayMetrics.widthPixels.toFloat()
        } else {
            displayMetrics.heightPixels.toFloat()
        }
        playerViewModel.updateScreenDimensions(screenWidth)
    }
}