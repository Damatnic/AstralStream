package com.astralplayer.nextplayer

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.astralplayer.nextplayer.feature.pip.PipManager
import com.astralplayer.nextplayer.data.PlayerRepository
import com.astralplayer.nextplayer.data.RecentFile
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.data.repository.RecentFilesRepository
import com.astralplayer.nextplayer.data.repository.RecentFilesRepositoryImpl
import com.astralplayer.nextplayer.data.repository.PlaylistRepository
import com.astralplayer.nextplayer.data.repository.PlaylistRepositoryImpl
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import com.astralplayer.nextplayer.utils.IntentUtils
import com.astralplayer.nextplayer.utils.CodecManager
import kotlinx.coroutines.launch
import com.astralplayer.nextplayer.data.EnhancedGestureManager
import com.astralplayer.nextplayer.data.HapticFeedbackManager
import com.astralplayer.nextplayer.viewmodel.SimpleEnhancedPlayerViewModel
import com.astralplayer.nextplayer.ui.screens.EnhancedVideoPlayerScreen
import com.astralplayer.nextplayer.utils.ErrorHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoPlayerActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "VideoPlayerActivity"
    }
    
    private lateinit var playerRepository: PlayerRepository
    private lateinit var pipManager: PipManager
    private lateinit var viewModel: SimpleEnhancedPlayerViewModel
    private lateinit var database: AstralVuDatabase
    private lateinit var recentFilesRepository: RecentFilesRepository
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var codecManager: CodecManager
    
    private var isInPipMode = false
    private var videoUri: Uri? = null
    private var videoTitle: String? = null
    private var isFromExternal = false
    private var isAdultContentMode = false
    private var isStreamingMode = false
    
    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lifecycleScope.launch {
                when (intent.action) {
                    PipManager.ACTION_PLAY_PAUSE -> viewModel.togglePlayPause()
                    PipManager.ACTION_SEEK_FORWARD -> {
                        playerRepository.playerState.value.currentPosition.let { pos ->
                            viewModel.seekTo(pos + 10000)
                        }
                    }
                    PipManager.ACTION_SEEK_BACKWARD -> {
                        playerRepository.playerState.value.currentPosition.let { pos ->
                            viewModel.seekTo(pos - 10000)
                        }
                    }
                    PipManager.ACTION_NEXT -> {
                        playerRepository.skipToNext()
                    }
                    PipManager.ACTION_PREVIOUS -> {
                        playerRepository.skipToPrevious()
                    }
                }
                updatePipParams()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on for video playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Log intent details for debugging
        IntentUtils.logIntentDetails(intent, TAG)
        
        // Initialize dependencies
        val application = application as AstralVuApplication
        codecManager = application.codecManager
        database = application.database
        recentFilesRepository = RecentFilesRepositoryImpl(database.recentFilesDao())
        playlistRepository = PlaylistRepositoryImpl(database.playlistDao())
        
        // Initialize player components
        val exoPlayer = com.astralplayer.nextplayer.di.PlayerModule.createExoPlayer(this, codecManager)
        playerRepository = com.astralplayer.nextplayer.di.PlayerModule.createPlayerRepository(exoPlayer, this)
        pipManager = PipManager(this)
        
        // Create gesture manager and haptic feedback
        val hapticManager = HapticFeedbackManager(this)
        val gestureManager = EnhancedGestureManager()
        
        // Create viewModel
        viewModel = SimpleEnhancedPlayerViewModel(
            application = application,
            playerRepository = playerRepository,
            gestureManager = gestureManager,
            hapticManager = hapticManager
        )
        
        // Handle intent
        if (!handleIntent(intent)) {
            Log.e(TAG, "Failed to handle intent, finishing activity")
            finish()
            return
        }
        
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    videoUri?.let { uri ->
                        EnhancedVideoPlayerScreen(
                            viewModel = viewModel,
                            videoUri = uri,
                            videoTitle = videoTitle ?: "Unknown Video",
                            playlistRepository = playlistRepository,
                            onBack = { handleBackPressed() },
                            isInPipMode = isInPipMode,
                            onEnterPip = { enterPipMode() }
                        )
                    }
                }
            }
        }
        
        // Setup PiP and other features
        setupPictureInPicture()
        setupFullscreenMode()
        
        // Observe video size changes for PiP
        observeVideoSizeChanges()
        
        // Register PiP action receiver
        registerPipActionReceiver()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "Received new intent")
        IntentUtils.logIntentDetails(intent, TAG)
        
        // Handle new intent (for singleTop launch mode)
        if (handleIntent(intent)) {
            // Reload video with new URI
            videoUri?.let { uri ->
                lifecycleScope.launch {
                    viewModel.loadVideo(uri)
                }
            }
        }
    }
    
    private fun handleIntent(intent: Intent): Boolean {
        return try {
            // Extract basic information
            videoUri = intent.data
            videoTitle = extractVideoTitle(intent)
            isFromExternal = intent.getBooleanExtra("from_external", false)
            isAdultContentMode = intent.getBooleanExtra("adult_content_mode", false)
            isStreamingMode = intent.getBooleanExtra("streaming_mode", false)
            
            Log.d(TAG, "Handling intent - URI: $videoUri, Title: $videoTitle")
            Log.d(TAG, "External: $isFromExternal, Adult: $isAdultContentMode, Streaming: $isStreamingMode")
            
            if (videoUri == null) {
                ErrorHandler.handleError(
                    context = this,
                    throwable = IllegalArgumentException("No video URI provided"),
                    userMessage = "No video selected",
                    errorType = ErrorHandler.ErrorType.FILE_ACCESS
                )
                return false
            }
            
            // Validate URI
            if (!IntentUtils.isVideoUri(videoUri!!, intent.type)) {
                Log.w(TAG, "URI does not appear to be a video: $videoUri")
                // Continue anyway, let ExoPlayer handle it
            }
            
            // Handle playlist mode
            val playlistMode = intent.getBooleanExtra("playlist_mode", false)
            if (playlistMode) {
                handlePlaylistMode(intent)
            }
            
            // Configure player for content type
            configurePlayerForContent()
            
            // Save to recent files
            saveToRecentFiles()
            
            // Load video
            lifecycleScope.launch {
                try {
                    viewModel.loadVideo(videoUri!!)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading video", e)
                    ErrorHandler.handleError(
                        context = this@VideoPlayerActivity,
                        throwable = e,
                        userMessage = "Failed to load video",
                        errorType = ErrorHandler.ErrorType.PLAYBACK
                    )
                    finish()
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error handling intent", e)
            ErrorHandler.handleError(
                context = this,
                throwable = e,
                userMessage = "Failed to load video",
                errorType = ErrorHandler.ErrorType.FILE_ACCESS
            )
            false
        }
    }
    
    private fun extractVideoTitle(intent: Intent): String {
        // Try various title sources
        intent.getStringExtra("video_title")?.let { return it }
        intent.getStringExtra("title")?.let { return it }
        intent.getStringExtra(Intent.EXTRA_TITLE)?.let { return it }
        intent.getStringExtra(Intent.EXTRA_SUBJECT)?.let { return it }
        
        // Extract from URI
        val uri = intent.data
        return when {
            uri == null -> "Unknown Video"
            uri.scheme == "content" -> {
                // Try to get display name from content resolver
                try {
                    contentResolver.query(
                        uri,
                        arrayOf("_display_name"),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex("_display_name")
                            if (nameIndex >= 0) cursor.getString(nameIndex) else null
                        } else null
                    } ?: uri.lastPathSegment ?: "Unknown Video"
                } catch (e: Exception) {
                    Log.w(TAG, "Could not query content resolver", e)
                    uri.lastPathSegment ?: "Unknown Video"
                }
            }
            uri.scheme == "file" -> {
                val path = uri.path ?: ""
                path.substringAfterLast('/').substringBeforeLast('.')
                    .takeIf { it.isNotEmpty() } ?: "Unknown Video"
            }
            uri.scheme in setOf("http", "https") -> {
                uri.lastPathSegment?.substringBeforeLast('.')
                    ?.replace(Regex("[._-]"), " ")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() } ?: "Streaming Video"
            }
            else -> uri.lastPathSegment ?: "Unknown Video"
        }
    }
    
    private fun handlePlaylistMode(intent: Intent) {
        val playlistShuffle = intent.getBooleanExtra("playlist_shuffle", false)
        val playlistBundles = intent.getParcelableArrayListExtra<Bundle>("playlist_videos")
        
        playlistBundles?.let { bundles ->
            val playlistVideos = bundles.mapNotNull { bundle ->
                val uriString = bundle.getString("uri")
                val title = bundle.getString("title")
                val duration = bundle.getLong("duration", 0L)
                if (uriString != null && title != null) {
                    com.astralplayer.nextplayer.data.PlaylistVideo(
                        uri = Uri.parse(uriString),
                        title = title,
                        duration = duration
                    )
                } else null
            }
            
            if (playlistVideos.isNotEmpty()) {
                lifecycleScope.launch {
                    playerRepository.playPlaylist(playlistVideos, playlistShuffle)
                }
            }
        }
    }
    
    private fun configurePlayerForContent() {
        lifecycleScope.launch {
            val settingsRepository = (application as AstralVuApplication).settingsRepository
            
            // Apply adult content optimizations if needed
            if (isAdultContentMode) {
                Log.d(TAG, "Applying adult content optimizations")
                settingsRepository.setEnhancedCodecSupport(true)
                settingsRepository.setVolumeBoostEnabled(true)
                settingsRepository.setFullScreenMode(true)
            }
            
            // Apply streaming optimizations if needed
            if (isStreamingMode) {
                Log.d(TAG, "Applying streaming optimizations")
                settingsRepository.setNetworkBuffering(true)
                settingsRepository.setAdaptiveStreaming(true)
            }
            
            // Set up codec preferences
            if (codecManager.codecInfo.value.isDtsSupported) {
                settingsRepository.setDtsDecodingEnabled(true)
            }
            
            if (codecManager.codecInfo.value.isAc3Supported) {
                settingsRepository.setAc3DecodingEnabled(true)
            }
        }
    }
    
    private fun saveToRecentFiles() {
        videoUri?.let { uri ->
            lifecycleScope.launch {
                try {
                    val recentFile = RecentFile(
                        id = uri.toString().hashCode().toString(),
                        uri = uri.toString(),
                        title = videoTitle ?: "Unknown Video",
                        duration = 0L, // Will be updated when video loads
                        lastPosition = 0L,
                        lastPlayed = System.currentTimeMillis()
                    )
                    recentFilesRepository.insertRecentFile(recentFile)
                    Log.d(TAG, "Saved to recent files: ${recentFile.title}")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not save to recent files", e)
                }
            }
        }
    }
    
    private fun setupPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Enable PiP for video content
            Log.d(TAG, "Setting up Picture-in-Picture support")
        }
    }
    
    private fun setupFullscreenMode() {
        // Hide system UI for immersive video experience
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }
    
    private fun observeVideoSizeChanges() {
        lifecycleScope.launch {
            playerRepository.playerState.collect { playerState ->
                if (isInPipMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val videoSize = playerState.videoSize
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        pipManager.updateAspectRatioFromVideo(
                            width = videoSize.width,
                            height = videoSize.height,
                            isPlaying = playerState.isPlaying
                        )
                    }
                }
            }
        }
    }
    
    private fun registerPipActionReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val filter = IntentFilter().apply {
                addAction(PipManager.ACTION_PLAY_PAUSE)
                addAction(PipManager.ACTION_SEEK_FORWARD)
                addAction(PipManager.ACTION_SEEK_BACKWARD)
                addAction(PipManager.ACTION_NEXT)
                addAction(PipManager.ACTION_PREVIOUS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(pipActionReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(pipActionReceiver, filter)
            }
            Log.d(TAG, "Registered PiP action receiver")
        }
    }
    
    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPipMode) {
            lifecycleScope.launch {
                val playerState = playerRepository.playerState.value
                val isPlaying = playerState.isPlaying
                val videoSize = playerState.videoSize
                
                val aspectRatio = if (videoSize.width > 0 && videoSize.height > 0) {
                    pipManager.calculateAspectRatio(videoSize.width, videoSize.height)
                } else {
                    Rational(16, 9) // Fallback to 16:9
                }
                
                pipManager.updatePipParams(
                    aspectRatio = aspectRatio,
                    isPlaying = isPlaying
                )
            }
        }
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        // Auto-enter PiP when user leaves the app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isFromExternal) {
            enterPipMode()
        }
    }
    
    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lifecycleScope.launch {
                val playerState = playerRepository.playerState.value
                val isPlaying = playerState.isPlaying
                val videoSize = playerState.videoSize
                
                val aspectRatio = if (videoSize.width > 0 && videoSize.height > 0) {
                    pipManager.calculateAspectRatio(videoSize.width, videoSize.height)
                } else {
                    Rational(16, 9) // Fallback to 16:9
                }
                
                Log.d(TAG, "Entering PiP mode with aspect ratio: $aspectRatio")
                pipManager.enterPipMode(
                    aspectRatio = aspectRatio,
                    isPlaying = isPlaying
                )
            }
        }
    }
    
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        
        Log.d(TAG, "PiP mode changed: $isInPictureInPictureMode")
        
        // Update enhanced PiP manager state
        pipManager.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        
        // Update UI based on PiP mode
        if (isInPictureInPictureMode) {
            // Hide UI controls in PiP mode
            Log.d(TAG, "Entered PiP mode - hiding controls")
        } else {
            // Show UI controls when exiting PiP mode
            Log.d(TAG, "Exited PiP mode - showing controls")
            // Re-enable fullscreen mode
            setupFullscreenMode()
        }
    }
    
    private fun handleBackPressed() {
        if (isFromExternal) {
            // If opened from external app, finish activity
            finish()
        } else {
            // If opened from within app, go to main activity
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            finish()
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        // Save current position to recent files
        videoUri?.let { uri ->
            lifecycleScope.launch {
                try {
                    val currentPosition = playerRepository.playerState.value.currentPosition
                    val duration = playerRepository.playerState.value.duration
                    
                    val recentFile = RecentFile(
                        id = uri.toString().hashCode().toString(),
                        uri = uri.toString(),
                        title = videoTitle ?: "Unknown Video",
                        duration = duration,
                        lastPosition = currentPosition,
                        lastPlayed = System.currentTimeMillis()
                    )
                    recentFilesRepository.updateRecentFile(recentFile)
                    Log.d(TAG, "Updated recent file position: ${currentPosition}ms")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not update recent file position", e)
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Re-apply fullscreen mode when resuming
        if (!isInPipMode) {
            setupFullscreenMode()
        }
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    override fun onStop() {
        super.onStop()
        
        // Only pause if not in PiP mode
        if (!isInPipMode) {
            viewModel.onStop()
        }
        
        Log.d(TAG, "Activity stopped, PiP mode: $isInPipMode")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        Log.d(TAG, "Destroying VideoPlayerActivity")
        
        // Release player resources
        try {
            playerRepository.release()
            Log.d(TAG, "Player repository released")
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing player repository", e)
        }
        
        // Unregister PiP receiver
        try {
            unregisterReceiver(pipActionReceiver)
            Log.d(TAG, "PiP receiver unregistered")
        } catch (e: Exception) {
            Log.w(TAG, "PiP receiver was not registered", e)
        }
        
        // Clear screen on flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        Log.d(TAG, "Configuration changed: ${newConfig.orientation}")
        
        // Handle orientation changes
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Log.d(TAG, "Switched to landscape mode")
                setupFullscreenMode()
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                Log.d(TAG, "Switched to portrait mode")
                // Allow portrait for some content types
                if (!isStreamingMode && !isAdultContentMode) {
                    setupFullscreenMode()
                }
            }
        }
    }
    
    // Handle media button events
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            android.view.KeyEvent.KEYCODE_SPACE -> {
                viewModel.togglePlayPause()
                true
            }
            android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                viewModel.seekRelative(30000) // 30 seconds forward
                true
            }
            android.view.KeyEvent.KEYCODE_MEDIA_REWIND,
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                viewModel.seekRelative(-10000) // 10 seconds backward
                true
            }
            android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                viewModel.adjustVolume(0.1f)
                true
            }
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                viewModel.adjustVolume(-0.1f)
                true
            }
            android.view.KeyEvent.KEYCODE_BACK -> {
                if (isInPipMode) {
                    // Don't handle back in PiP mode
                    return false
                }
                handleBackPressed()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}