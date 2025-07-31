package com.astralplayer.nextplayer.viewmodel

import android.app.Application
import android.net.Uri
import android.view.Window
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.data.gesture.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
/**
 * Simple Enhanced ViewModel without Hilt
 */
class SimpleEnhancedPlayerViewModel(
    application: Application,
    private val playerRepository: PlayerRepository? = null,
    private val exoPlayer: ExoPlayer? = null
) : AndroidViewModel(application) {
    
    // Default constructor for tests - simplified
    constructor() : this(
        application = android.app.Application(),
        playerRepository = null,
        exoPlayer = null
    )
    
    // Current player reference for tests
    private var currentPlayer: ExoPlayer? = exoPlayer
    
    private val gestureManager = EnhancedGestureManager()
    private val hapticManager = HapticFeedbackManager(application)
    
    // Simple state flows - using Player states from Media3
    val playerState = playerRepository?.playerState ?: MutableStateFlow(Player.STATE_IDLE)
    val gestureSettings = gestureManager.enhancedGestureSettings
    
    // UI state - simplified
    private val _uiState = MutableStateFlow(
        SimplePlayerUiState(
            isPlaying = false,
            currentPosition = 0L,
            duration = 0L,
            playbackSpeed = 1.0f
        )
    )
    val uiState: StateFlow<SimplePlayerUiState> = _uiState.asStateFlow()
    
    // Overlay visibility - simplified  
    private val _overlayVisibility = MutableStateFlow(
        SimpleOverlayVisibility(
            seekPreview = false,
            volume = false,
            brightness = false,
            doubleTap = false,
            longPress = false
        )
    )
    val overlayVisibility: StateFlow<SimpleOverlayVisibility> = _overlayVisibility.asStateFlow()
    
    fun setupVerticalGestureHandler(window: Window) {
        // Setup simplified gesture handler
        Log.d("SimplePlayerVM", "Setting up gesture handler")
    }
    
    fun loadVideo(uri: Uri) {
        viewModelScope.launch {
            try {
                playerRepository?.playVideo(uri)
                Log.d("SimplePlayerVM", "Video loaded: $uri")
            } catch (e: Exception) {
                Log.e("SimplePlayerVM", "Failed to load video", e)
            }
        }
    }
    
    // Test methods
    fun setPlayer(player: ExoPlayer?) {
        currentPlayer = player
    }
    
    fun getCurrentPlayer(): ExoPlayer? = currentPlayer
    
    fun togglePlayPause() {
        viewModelScope.launch {
            playerRepository?.togglePlayPause()
            currentPlayer?.let { player ->
                player.playWhenReady = !player.playWhenReady
            }
        }
    }
    
    fun seekTo(position: Long) {
        viewModelScope.launch {
            playerRepository?.seekTo(position)
            currentPlayer?.seekTo(position)
        }
    }
    
    fun seekRelative(delta: Long) {
        viewModelScope.launch {
            playerRepository?.seekBy(delta)
            currentPlayer?.let { player ->
                val newPosition = (player.currentPosition + delta).coerceAtLeast(0)
                player.seekTo(newPosition)
            }
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            playerRepository?.setPlaybackSpeed(speed)
            currentPlayer?.setPlaybackSpeed(speed)
        }
    }
    
    fun onStart() {
        viewModelScope.launch {
            playerRepository?.resumeVideo()
            currentPlayer?.let { player ->
                if (!player.playWhenReady) {
                    player.playWhenReady = true
                }
            }
        }
    }
    
    fun onStop() {
        viewModelScope.launch {
            playerRepository?.pauseVideo()
            currentPlayer?.playWhenReady = false
        }
    }
    
    // Simple gesture callbacks - return mock implementation for now
    fun getGestureCallbacks() = object {
        fun onHorizontalSeek(deltaX: Float, velocity: Float) {
            Log.d("SimplePlayerVM", "Horizontal seek: $deltaX")
        }
        
        fun onVerticalVolumeChange(deltaY: Float, side: TouchSide) {
            Log.d("SimplePlayerVM", "Volume change: $deltaY")
        }
        
        fun onVerticalBrightnessChange(deltaY: Float, side: TouchSide) {
            Log.d("SimplePlayerVM", "Brightness change: $deltaY")
        }
        
        fun onDoubleTap(position: androidx.compose.ui.geometry.Offset, side: TouchSide) {
            val seekAmount = when (side) {
                TouchSide.LEFT -> -10000L
                TouchSide.RIGHT -> 10000L
                TouchSide.CENTER -> {
                    togglePlayPause()
                    return
                }
            }
            seekRelative(seekAmount)
        }
        
        fun onLongPressStart(position: androidx.compose.ui.geometry.Offset) {
            Log.d("SimplePlayerVM", "Long press start")
        }
        
        fun onLongPressEnd() {
            Log.d("SimplePlayerVM", "Long press end")
        }
        
        fun onPinchZoom(scale: Float, center: androidx.compose.ui.geometry.Offset) {
            Log.d("SimplePlayerVM", "Pinch zoom: $scale")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d("SimplePlayerVM", "ViewModel cleared")
    }
}

// Simplified data classes
data class SimplePlayerUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val seekPreviewInfo: HorizontalSeekGestureHandler.SeekPreviewInfo? = null,
    val volumeInfo: VerticalGestureHandler.VolumeInfo? = null,
    val brightnessInfo: VerticalGestureHandler.BrightnessInfo? = null,
    val doubleTapInfo: DoubleTapInfo? = null,
    val longPressSeekInfo: LongPressSeekHandler.LongPressSeekInfo? = null,
    val zoomLevel: Float = 1.0f
)

data class SimpleOverlayVisibility(
    val seekPreview: Boolean = false,
    val volume: Boolean = false,
    val brightness: Boolean = false,
    val doubleTap: Boolean = false,
    val longPress: Boolean = false
)

data class DoubleTapInfo(
    val side: TouchSide,
    val seekAmount: Long
)