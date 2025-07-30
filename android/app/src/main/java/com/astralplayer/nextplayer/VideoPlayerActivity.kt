package com.astralplayer.nextplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.astralplayer.nextplayer.data.PlayerRepository
import com.astralplayer.nextplayer.di.PlayerModule
import com.astralplayer.nextplayer.data.EnhancedGestureManager
import com.astralplayer.nextplayer.data.HapticFeedbackManager
import com.astralplayer.nextplayer.data.gesture.GestureSettingsSerializer
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import com.astralplayer.nextplayer.ui.screens.EnhancedVideoPlayerScreen
import com.astralplayer.nextplayer.utils.CodecManager
import com.astralplayer.nextplayer.utils.IntentUtils
import com.astralplayer.nextplayer.viewmodel.EnhancedPlayerViewModel
import com.astralplayer.nextplayer.viewmodel.EnhancedPlayerViewModelFactory
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch

@UnstableApi
open class VideoPlayerActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "VideoPlayerActivity"
    }
    
    protected lateinit var exoPlayer: ExoPlayer
    protected lateinit var playerView: PlayerView
    protected lateinit var playerRepository: PlayerRepository
    protected lateinit var codecManager: CodecManager
    protected lateinit var enhancedPlayerViewModel: EnhancedPlayerViewModel
    protected lateinit var gestureManager: EnhancedGestureManager
    protected lateinit var hapticManager: HapticFeedbackManager
    protected lateinit var settingsSerializer: GestureSettingsSerializer
    
    private var videoUri: Uri? = null
    private var videoTitle: String? = null
    
    protected fun getPlayer(): ExoPlayer? = if (::exoPlayer.isInitialized) exoPlayer else null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "onCreate: Starting VideoPlayerActivity")
        
        // Initialize core components
        initializePlayer()
        
        // Handle intent
        handleIntent(intent)
        
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    videoUri?.let { uri ->
                        EnhancedVideoPlayerScreen(
                            playerView = playerView,
                            viewModel = enhancedPlayerViewModel,
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }
    
    private fun initializePlayer() {
        try {
            // Initialize CodecManager
            codecManager = CodecManager(this)
            
            // Create ExoPlayer with codec optimizations
            exoPlayer = PlayerModule.createExoPlayer(this, codecManager)
            
            // Create PlayerRepository
            playerRepository = PlayerModule.createPlayerRepository(exoPlayer, this)
            
            // Initialize gesture components
            gestureManager = EnhancedGestureManager()
            hapticManager = HapticFeedbackManager(this)
            settingsSerializer = GestureSettingsSerializer(this)
            
            // Create PlayerView
            playerView = PlayerView(this).apply {
                player = exoPlayer
                useController = false // We'll use our custom controls
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
            
            // Create ViewModel
            val viewModelFactory = EnhancedPlayerViewModelFactory(
                application = application,
                playerRepository = playerRepository,
                gestureManager = gestureManager,
                settingsSerializer = settingsSerializer,
                hapticManager = hapticManager
            )
            enhancedPlayerViewModel = ViewModelProvider(this, viewModelFactory)
                .get(EnhancedPlayerViewModel::class.java)
            
            // Setup vertical gesture handler with window
            enhancedPlayerViewModel.setupVerticalGestureHandler(window)
            
            Log.d(TAG, "Player initialized successfully with gesture support")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize player", e)
        }
    }
    
    private fun handleIntent(intent: Intent) {
        try {
            val uri = intent.data
            val title = intent.getStringExtra("video_title")
                ?: intent.getStringExtra(Intent.EXTRA_TITLE)
                ?: uri?.lastPathSegment
                ?: "Unknown Video"
            
            if (uri != null) {
                videoUri = uri
                videoTitle = title
                loadVideo(uri)
                // Also load in ViewModel
                enhancedPlayerViewModel.loadVideo(uri)
                Log.d(TAG, "Loading video: $uri")
            } else {
                Log.w(TAG, "No video URI provided in intent")
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling intent", e)
            finish()
        }
    }
    
    private fun loadVideo(uri: Uri) {
        try {
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            Log.d(TAG, "Video loaded and playing: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load video: $uri", e)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    override fun onStop() {
        super.onStop()
        exoPlayer.playWhenReady = false
    }
    
    override fun onStart() {
        super.onStart()
        if (::exoPlayer.isInitialized) {
            exoPlayer.playWhenReady = true
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
    }
}