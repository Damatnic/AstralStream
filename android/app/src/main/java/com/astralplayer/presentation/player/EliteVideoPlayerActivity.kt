package com.astralplayer.presentation.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.core.codec.CodecManager
import com.astralplayer.core.intent.VideoIntentHandler
import com.astralplayer.domain.repository.PlayerRepository
import com.astralplayer.presentation.theme.AstralPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class EliteVideoPlayerActivity : ComponentActivity() {
    
    @Inject lateinit var playerRepository: PlayerRepository
    @Inject lateinit var codecManager: CodecManager
    @Inject lateinit var intentHandler: VideoIntentHandler
    
    private val viewModel: VideoPlayerViewModel by viewModels()
    private var exoPlayer: ExoPlayer? = null
    
    companion object {
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_RESUME_POSITION = "resume_position"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.d("EliteVideoPlayerActivity created")
        
        // Configure immersive mode
        setupImmersiveMode()
        
        // Handle intent
        handleIntent(intent)
        
        setContent {
            AstralPlayerTheme {
                EliteVideoPlayerScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() },
                    onFullscreenToggle = { isFullscreen ->
                        if (isFullscreen) {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("New intent received")
        handleIntent(intent)
    }
    
    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Hide system bars initially
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
    
    private fun handleIntent(intent: Intent) {
        try {
            when (intent.action) {
                Intent.ACTION_VIEW -> handleViewIntent(intent)
                Intent.ACTION_SEND -> handleSendIntent(intent)
                else -> {
                    Timber.w("Unsupported intent action: ${intent.action}")
                    finish()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error handling intent")
            // Show error to user and finish
            finish()
        }
    }
    
    private fun handleViewIntent(intent: Intent) {
        if (!intentHandler.isVideoIntent(intent)) {
            Timber.w("Intent is not a video intent")
            finish()
            return
        }
        
        val videoInfo = intentHandler.extractVideoInfo(intent)
        
        Timber.d("Loading video: ${videoInfo.title}")
        Timber.d("URI: ${videoInfo.uri}")
        Timber.d("MIME type: ${videoInfo.mimeType}")
        Timber.d("Is streaming: ${videoInfo.isStreaming}")
        Timber.d("Is adult content: ${videoInfo.isAdultContent}")
        
        // Apply codec optimizations based on content type
        when {
            videoInfo.isAdultContent -> {
                codecManager.applyAdultContentOptimizations()
                Timber.d("Applied adult content optimizations")
            }
            videoInfo.isStreaming -> {
                codecManager.configureForStreaming(videoInfo.streamType)
                Timber.d("Configured for streaming: ${videoInfo.streamType}")
            }
        }
        
        // Configure player repository
        configurePlayerForContent(videoInfo)
        
        // Load video in ViewModel
        viewModel.loadVideo(videoInfo)
        
        // Handle resume position if provided
        intent.getLongExtra(EXTRA_RESUME_POSITION, -1L).let { position ->
            if (position > 0) {
                viewModel.seekTo(position)
            }
        }
    }
    
    private fun handleSendIntent(intent: Intent) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text.isNullOrBlank()) {
            Timber.w("Send intent has no text")
            finish()
            return
        }
        
        // Try to parse as video URL
        try {
            val uri = intentHandler.createVideoUri(text)
            val videoInfo = VideoIntentHandler.VideoInfo(
                uri = uri,
                title = "Shared Video",
                mimeType = null,
                referrer = null,
                userAgent = null,
                headers = emptyMap(),
                isStreaming = intentHandler.isStreamingUrl(uri),
                streamType = intentHandler.determineStreamType(uri, null),
                isAdultContent = false,
                requiresSpecialHandling = false
            )
            
            viewModel.loadVideo(videoInfo)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse shared URL: $text")
            finish()
        }
    }
    
    private fun configurePlayerForContent(videoInfo: VideoIntentHandler.VideoInfo) {
        when {
            videoInfo.streamType.isHLS -> {
                playerRepository.configureHLS()
                Timber.d("Configured player for HLS")
            }
            videoInfo.streamType.isDASH -> {
                playerRepository.configureDASH()
                Timber.d("Configured player for DASH")
            }
            videoInfo.streamType.isRTMP -> {
                playerRepository.configureRTMP()
                Timber.d("Configured player for RTMP")
            }
            videoInfo.isAdultContent -> {
                playerRepository.configureForAdultContent()
                Timber.d("Configured player for adult content")
            }
            else -> {
                playerRepository.configureDefault()
                Timber.d("Configured player for default playback")
            }
        }
        
        // Apply headers if present
        if (videoInfo.headers.isNotEmpty()) {
            playerRepository.setHeaders(videoInfo.headers)
            Timber.d("Applied ${videoInfo.headers.size} headers")
        }
    }
    
    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.onPause()
        
        // Save playback position
        val videoId = intent.getLongExtra(EXTRA_VIDEO_ID, -1L)
        if (videoId > 0) {
            viewModel.savePlaybackPosition(videoId)
        }
    }
    
    override fun onStop() {
        super.onStop()
        viewModel.onStop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        Timber.d("EliteVideoPlayerActivity destroyed")
    }
    
    override fun onBackPressed() {
        if (viewModel.isFullscreen.value) {
            viewModel.exitFullscreen()
        } else {
            super.onBackPressed()
        }
    }
}

@Composable
private fun EliteVideoPlayerScreen(
    viewModel: VideoPlayerViewModel,
    onBackPressed: () -> Unit,
    onFullscreenToggle: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle fullscreen changes
    LaunchedEffect(uiState.isFullscreen) {
        onFullscreenToggle(uiState.isFullscreen)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            uiState.isLoading -> {
                LoadingIndicator()
            }
            uiState.error != null -> {
                ErrorDisplay(
                    error = uiState.error,
                    onRetry = { viewModel.retry() },
                    onBack = onBackPressed
                )
            }
            else -> {
                VideoPlayerContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBackPressed = onBackPressed
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading video...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ErrorDisplay(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Error loading video",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
                OutlinedButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerContent(
    uiState: VideoPlayerUiState,
    viewModel: VideoPlayerViewModel,
    onBackPressed: () -> Unit
) {
    // TODO: Implement actual video player UI with ExoPlayer
    // This will be the main video player interface
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = "Video Player UI\n${uiState.videoInfo?.title ?: \"No Title\"}",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
    }
}