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
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
import com.astralplayer.nextplayer.data.EnhancedGestureManager
import com.astralplayer.nextplayer.data.HapticFeedbackManager
import com.astralplayer.nextplayer.viewmodel.SimpleEnhancedPlayerViewModel
import com.astralplayer.nextplayer.ui.screens.EnhancedVideoPlayerScreen
import com.astralplayer.nextplayer.utils.ErrorHandler
import android.util.Log
// import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint
class VideoPlayerActivity : ComponentActivity() {
    
    private lateinit var playerRepository: PlayerRepository
    private lateinit var pipManager: PipManager
    private lateinit var viewModel: SimpleEnhancedPlayerViewModel
    private lateinit var database: AstralVuDatabase
    private lateinit var recentFilesRepository: RecentFilesRepository
    private lateinit var playlistRepository: PlaylistRepository
    
    private var isInPipMode = false
    private var videoUri: Uri? = null
    private var videoTitle: String? = null
    
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
        
        // Initialize dependencies manually (temporary fix)
        val exoPlayer = com.astralplayer.nextplayer.di.PlayerModule.createExoPlayer(this)
        playerRepository = com.astralplayer.nextplayer.di.PlayerModule.createPlayerRepository(exoPlayer, this)
        pipManager = PipManager(this)
        
        // Initialize database and repositories
        database = (application as AstralVuApplication).database
        recentFilesRepository = RecentFilesRepositoryImpl(database.recentFilesDao())
        playlistRepository = PlaylistRepositoryImpl(database.playlistDao())
        
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
        
        // Get video URI from intent with error handling
        try {
            videoUri = intent.data
            videoTitle = intent.getStringExtra("video_title") ?: intent.getStringExtra("title") ?: videoUri?.lastPathSegment ?: "Unknown Video"
            
            if (videoUri == null) {
                ErrorHandler.handleError(
                    context = this,
                    throwable = IllegalArgumentException("No video URI provided"),
                    userMessage = "No video selected",
                    errorType = ErrorHandler.ErrorType.FILE_ACCESS
                )
                finish()
                return
            }
        } catch (e: Exception) {
            ErrorHandler.handleError(
                context = this,
                throwable = e,
                userMessage = "Failed to load video",
                errorType = ErrorHandler.ErrorType.FILE_ACCESS
            )
            finish()
            return
        }
        
        // Handle playlist mode
        val playlistMode = intent.getBooleanExtra("playlist_mode", false)
        val playlistShuffle = intent.getBooleanExtra("playlist_shuffle", false)
        if (playlistMode) {
            val playlistBundles = intent.getParcelableArrayListExtra<android.os.Bundle>("playlist_videos")
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
        
        // Save to recent files
        videoUri?.let { uri ->
            lifecycleScope.launch {
                val recentFile = RecentFile(
                    id = uri.toString().hashCode().toString(),
                    uri = uri.toString(),
                    title = videoTitle ?: "Unknown Video",
                    duration = 0L, // Will be updated when video loads
                    lastPosition = 0L,
                    lastPlayed = System.currentTimeMillis()
                )
                recentFilesRepository.insertRecentFile(recentFile)
            }
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
                            onBack = { finish() },
                            isInPipMode = isInPipMode,
                            onEnterPip = { enterPipMode() }
                        )
                    }
                }
            }
        }
        
        // Observe video size changes and update PiP params accordingly
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
        
        // Register PiP action receiver
        registerPipActionReceiver()
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
        
        // Enter PiP mode when user leaves the app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        
        // Update enhanced PiP manager state
        pipManager.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        
        // Update UI based on PiP mode
        if (isInPictureInPictureMode) {
            // Hide UI controls in PiP mode
        } else {
            // Show UI controls when exiting PiP mode
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Save current position to recent files
        videoUri?.let { uri ->
            lifecycleScope.launch {
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
            }
        }
    }
    
    override fun onStop() {
        super.onStop()
        viewModel.onStop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        playerRepository.release()
        
        // Unregister PiP receiver
        try {
            unregisterReceiver(pipActionReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}