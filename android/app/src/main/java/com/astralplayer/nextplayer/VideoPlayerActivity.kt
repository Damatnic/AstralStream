package com.astralplayer.nextplayer

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import com.astralplayer.nextplayer.gesture.AdvancedGestureManager
import com.astralplayer.nextplayer.enhancement.SmartVideoEnhancementEngine
import com.astralplayer.nextplayer.performance.PerformanceMonitor
import com.astralplayer.nextplayer.performance.BatteryOptimizer
import com.astralplayer.nextplayer.audio.AudioPresetManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import com.astralplayer.nextplayer.ui.screens.SimpleVideoPlayerWithGestures
import com.astralplayer.nextplayer.ui.screens.AdvancedGestureVideoPlayerScreen
import com.astralplayer.nextplayer.utils.CodecManager
import com.astralplayer.nextplayer.utils.IntentUtils
import kotlinx.coroutines.launch

@AndroidEntryPoint
@UnstableApi
open class VideoPlayerActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "VideoPlayerActivity"
    }
    
    protected lateinit var exoPlayer: ExoPlayer
    protected lateinit var playerRepository: PlayerRepository
    protected lateinit var codecManager: CodecManager
    protected lateinit var playerView: PlayerView
    protected lateinit var gestureManager: EnhancedGestureManager
    protected lateinit var hapticManager: HapticFeedbackManager
    protected lateinit var settingsSerializer: GestureSettingsSerializer
    
    @Inject
    lateinit var advancedGestureManager: AdvancedGestureManager
    
    @Inject
    lateinit var enhancementEngine: SmartVideoEnhancementEngine
    
    @Inject
    lateinit var performanceMonitor: PerformanceMonitor
    
    @Inject
    lateinit var batteryOptimizer: BatteryOptimizer
    
    @Inject
    lateinit var audioPresetManager: AudioPresetManager
    
    private var videoUri: Uri? = null
    private var videoTitle: String? = null
    private var isInPipMode = false
    
    protected fun getPlayer(): ExoPlayer? = if (::exoPlayer.isInitialized) exoPlayer else null
    
    private fun supportsPiP(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "onCreate: Starting VideoPlayerActivity")
        
        // Initialize core components
        initializePlayer()
        
        // Initialize Week1 features
        performanceMonitor.trackFrameMetrics(this)
        batteryOptimizer.registerPowerModeCallback { mode ->
            // Adjust video quality based on power mode
            val quality = batteryOptimizer.getOptimalVideoQuality()
            Log.d(TAG, "Power mode changed: $mode, optimal quality: $quality")
        }
        
        // Handle intent
        handleIntent(intent)
        
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    videoUri?.let { uri ->
                        // Use advanced gesture video player if initialized
                        if (::advancedGestureManager.isInitialized) {
                            AdvancedGestureVideoPlayerScreen(
                                playerView = playerView,
                                advancedGestureManager = advancedGestureManager,
                                onBack = { finish() }
                            )
                        } else {
                            // Fallback to simple player
                            SimpleVideoPlayerWithGestures(
                                playerView = playerView,
                                onBack = { finish() }
                            )
                        }
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
            
            // Initialize advanced gesture system
            lifecycleScope.launch {
                advancedGestureManager.initialize()
            }
            
            // Initialize video enhancement engine
            lifecycleScope.launch {
                enhancementEngine.initialize(exoPlayer)
            }
            
            // Create PlayerView
            playerView = PlayerView(this).apply {
                player = exoPlayer
                useController = true // Use default ExoPlayer controls for simplicity
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
            
            Log.d(TAG, "Player initialized successfully")
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
    
    fun enterPiPMode() {
        if (supportsPiP() && !isInPipMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val aspectRatio = Rational(16, 9) // Default aspect ratio
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .build()
                    enterPictureInPictureMode(params)
                    Log.d(TAG, "Entering PiP mode")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to enter PiP mode", e)
                }
            }
        }
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (supportsPiP() && exoPlayer.isPlaying) {
            enterPiPMode()
        }
    }
    
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        
        // Update UI for PiP mode
        if (isInPictureInPictureMode) {
            // Hide controls in PiP mode
            playerView.useController = false
            Log.d(TAG, "Entered PiP mode")
        } else {
            // Show controls when exiting PiP
            playerView.useController = true
            Log.d(TAG, "Exited PiP mode")
        }
    }
}