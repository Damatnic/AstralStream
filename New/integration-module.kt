// EnhancedVideoPlayerActivity.kt
// This is the upgraded version of your existing video player activity
// It integrates all the elite features while maintaining your current functionality

package com.astralplayer.presentation.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.analytics.AnalyticsManager
import com.astralplayer.cache.AdvancedCacheManager
import com.astralplayer.core.browser.BrowserIntentHandler
import com.astralplayer.core.error.GlobalErrorHandler
import com.astralplayer.core.intent.VideoIntentHandler
import com.astralplayer.crash.CrashReporter
import com.astralplayer.domain.player.VideoPlayer
import com.astralplayer.features.ai.EnhancedAISubtitleGenerator
import com.astralplayer.gesture.EnhancedGestureController
import com.astralplayer.memory.MemoryManager
import com.astralplayer.offline.OfflinePlaybackManager
import com.astralplayer.player.performance.PerformanceOptimizer
import com.astralplayer.security.SecurityManager
import com.astralplayer.streaming.AdaptiveStreamingManager
import com.astralplayer.ui.screens.EliteVideoPlayerScreen
import com.astralplayer.ui.theme.AstralPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@UnstableApi
class EnhancedVideoPlayerActivity : ComponentActivity() {
    
    // Injected dependencies - all the elite components
    @Inject lateinit var exoPlayer: ExoPlayer
    @Inject lateinit var videoIntentHandler: VideoIntentHandler
    @Inject lateinit var browserIntentHandler: BrowserIntentHandler
    @Inject lateinit var aiSubtitleGenerator: EnhancedAISubtitleGenerator
    @Inject lateinit var performanceOptimizer: PerformanceOptimizer
    @Inject lateinit var adaptiveStreamingManager: AdaptiveStreamingManager
    @Inject lateinit var offlinePlaybackManager: OfflinePlaybackManager
    @Inject lateinit var gestureController: EnhancedGestureController
    @Inject lateinit var securityManager: SecurityManager
    @Inject lateinit var cacheManager: AdvancedCacheManager
    @Inject lateinit var memoryManager: MemoryManager
    @Inject lateinit var analyticsManager: AnalyticsManager
    @Inject lateinit var crashReporter: CrashReporter
    @Inject lateinit var globalErrorHandler: GlobalErrorHandler
    
    private val viewModel: EnhancedVideoPlayerViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up full screen immersive mode
        setupImmersiveMode()
        
        // Initialize performance optimizations
        initializePerformanceOptimizations()
        
        // Handle the intent (browser or direct video)
        handleVideoIntent(intent)
        
        // Set up the Compose UI
        setContent {
            AstralPlayerTheme {
                EliteVideoPlayerScreen(
                    viewModel = viewModel,
                    player = exoPlayer,
                    gestureController = gestureController,
                    onBackPressed = { finish() }
                )
            }
        }
        
        // Initialize analytics
        analyticsManager.logEvent(
            AnalyticsEvent(
                name = "player_opened",
                parameters = mapOf(
                    "source" to (intent.getStringExtra("source") ?: "unknown")
                )
            )
        )
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleVideoIntent(intent)
    }
    
    private fun setupImmersiveMode() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }
    
    private fun initializePerformanceOptimizations() {
        lifecycleScope.launch {
            try {
                // Configure ExoPlayer with optimal settings
                val loadControl = performanceOptimizer.createOptimalLoadControl()
                
                // Set up adaptive streaming
                adaptiveStreamingManager.configureAdaptiveStreaming(exoPlayer)
                
                // Monitor memory usage
                memoryManager.monitorMemoryUsage()
                
                // Prepare cache
                exoPlayer.setMediaSourceFactory(
                    DefaultMediaSourceFactory(context)
                        .setDataSourceFactory(
                            CacheDataSource.Factory()
                                .setCache(cacheManager.getVideoCache())
                                .setUpstreamDataSourceFactory(
                                    DefaultDataSource.Factory(context)
                                )
                        )
                )
                
            } catch (e: Exception) {
                globalErrorHandler.handleError(e, "Performance Initialization")
            }
        }
    }
    
    private fun handleVideoIntent(intent: Intent) {
        lifecycleScope.launch(globalErrorHandler.coroutineExceptionHandler) {
            try {
                // Check if this is from a browser
                val browserData = browserIntentHandler.extractBrowserData(intent)
                
                val videoInfo = if (browserData.isVideoUrl) {
                    // Handle browser video with all metadata
                    VideoInfo(
                        uri = Uri.parse(browserData.extractedVideoUrl ?: browserData.originalUrl),
                        title = intent.getStringExtra("title") ?: "Video",
                        headers = browserData.headers,
                        cookies = browserData.cookies,
                        referrer = browserData.referrer,
                        userAgent = browserData.userAgent
                    )
                } else {
                    // Handle direct video intent
                    videoIntentHandler.extractVideoInfo(intent)
                }
                
                // Load video with all optimizations
                loadVideo(videoInfo)
                
                // Start AI subtitle generation if enabled
                if (viewModel.isSubtitleGenerationEnabled) {
                    aiSubtitleGenerator.autoGenerateSubtitles(
                        videoUri = videoInfo.uri.toString(),
                        targetLanguage = viewModel.preferredSubtitleLanguage,
                        onSubtitleReady = { subtitle ->
                            viewModel.addSubtitle(subtitle)
                        }
                    )
                }
                
            } catch (e: Exception) {
                globalErrorHandler.handleError(e, "Video Intent Handling")
                showErrorDialog(e.message ?: "Failed to load video")
            }
        }
    }
    
    private fun loadVideo(videoInfo: VideoInfo) {
        // Build media item with all metadata
        val mediaItem = MediaItem.Builder()
            .setUri(videoInfo.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(videoInfo.title)
                    .build()
            )
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setExtras(Bundle().apply {
                        videoInfo.headers.forEach { (key, value) ->
                            putString("header_$key", value)
                        }
                    })
                    .build()
            )
            .build()
        
        // Configure data source with headers and cookies
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(videoInfo.headers)
            .setUserAgent(videoInfo.userAgent ?: "AstralStream/2.0")
        
        // Apply cookies if available
        if (videoInfo.cookies.isNotEmpty()) {
            val cookieHeader = videoInfo.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            dataSourceFactory.setDefaultRequestProperties(
                mapOf("Cookie" to cookieHeader)
            )
        }
        
        // Set media item and prepare player
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        
        // Log playback start
        analyticsManager.logVideoPlayback(
            videoId = videoInfo.uri.toString(),
            duration = 0,
            quality = "auto"
        )
    }
    
    private fun showErrorDialog(message: String) {
        // Show Material You error dialog
        AlertDialog(
            onDismissRequest = { finish() },
            title = { Text("Playback Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { finish() }) {
                    Text("OK")
                }
            }
        )
    }
    
    override fun onResume() {
        super.onResume()
        exoPlayer.playWhenReady = true
    }
    
    override fun onPause() {
        super.onPause()
        exoPlayer.playWhenReady = false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up resources
        memoryManager.cleanupResources()
        exoPlayer.release()
        
        // Log session end
        analyticsManager.logEvent(
            AnalyticsEvent(
                name = "player_closed",
                parameters = mapOf(
                    "duration" to exoPlayer.currentPosition,
                    "completion_percentage" to if (exoPlayer.duration > 0) {
                        (exoPlayer.currentPosition * 100 / exoPlayer.duration)
                    } else 0
                )
            )
        )
    }
    
    data class VideoInfo(
        val uri: Uri,
        val title: String,
        val headers: Map<String, String> = emptyMap(),
        val cookies: Map<String, String> = emptyMap(),
        val referrer: String? = null,
        val userAgent: String? = null
    )
}

// Enhanced ViewModel with all elite features
@HiltViewModel
class EnhancedVideoPlayerViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val subtitleRepository: SubtitleRepository,
    private val preferencesManager: PreferencesManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val _subtitles = MutableStateFlow<List<SubtitleEntry>>(emptyList())
    val subtitles: StateFlow<List<SubtitleEntry>> = _subtitles.asStateFlow()
    
    val isSubtitleGenerationEnabled: Boolean
        get() = preferencesManager.getBoolean("subtitle_generation_enabled", true)
    
    val preferredSubtitleLanguage: String
        get() = preferencesManager.getString("subtitle_language", "en") ?: "en"
    
    fun addSubtitle(subtitle: SubtitleEntry) {
        _subtitles.value = _subtitles.value + subtitle
    }
    
    fun updatePlayerState(state: PlayerState) {
        _playerState.value = state
    }
    
    data class PlayerState(
        val isPlaying: Boolean = false,
        val currentPosition: Long = 0,
        val duration: Long = 0,
        val bufferedPercentage: Int = 0,
        val playbackSpeed: Float = 1.0f,
        val volume: Float = 1.0f,
        val quality: String = "auto"
    )
}