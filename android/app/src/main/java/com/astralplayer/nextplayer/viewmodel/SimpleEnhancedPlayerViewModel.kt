package com.astralplayer.nextplayer.viewmodel

import android.app.Application
import android.net.Uri
import android.view.Window
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.data.gesture.*
import com.astralplayer.nextplayer.utils.SubtitleCue
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import android.util.Log
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Core Enhanced ViewModel without AI dependencies
 */
class SimpleEnhancedPlayerViewModel(
    application: Application,
    val playerRepository: PlayerRepository,
    val gestureManager: EnhancedGestureManager,
    private val hapticManager: HapticFeedbackManager
) : AndroidViewModel(application) {
    
    // Settings repository
    private val settingsRepository = com.astralplayer.nextplayer.data.repository.SettingsRepositoryImpl(application)
    
    // Playback position repository for resume functionality
    private val playbackPositionRepository = com.astralplayer.nextplayer.data.PlaybackPositionRepository(application)
    
    // Error handling
    private val networkManager = com.astralplayer.nextplayer.data.NetworkConnectivityManager(application)
    private val errorLogger = com.astralplayer.nextplayer.utils.ErrorLogger.getInstance(application)
    private val errorRecoveryManager = com.astralplayer.nextplayer.data.ErrorRecoveryManagerImpl(application, networkManager)
    val codecPackManager = com.astralplayer.nextplayer.feature.codec.CodecPackManager(application)
    
    // Error state
    private val _errorState = MutableStateFlow<Pair<PlayerError, ErrorRecoveryResult>?>(null)
    val errorState: StateFlow<Pair<PlayerError, ErrorRecoveryResult>?> = _errorState.asStateFlow()
    
    // Adapt PlayerRepository state to simple flows
    val playerState = playerRepository.playerState.map { it.playbackState }
    val currentPosition = playerRepository.playerState.map { it.currentPosition }
    val duration = playerRepository.playerState.map { it.duration }
    val isPlaying = playerRepository.playerState.map { it.isPlaying }
    val bufferedPercentage = playerRepository.playerState.map { it.bufferedPercentage }
    
    // Gesture settings flow
    val gestureSettings = gestureManager.enhancedGestureSettings
    
    // Quality management - convert data.VideoQuality to ui.components.VideoQuality
    val availableQualities = MutableStateFlow<List<com.astralplayer.nextplayer.ui.components.VideoQuality>>(emptyList())
    val currentQuality = MutableStateFlow<com.astralplayer.nextplayer.ui.components.VideoQuality?>(null)
    
    // UI state
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    // Overlay visibility
    private val _overlayVisibility = MutableStateFlow(OverlayVisibility())
    val overlayVisibility: StateFlow<OverlayVisibility> = _overlayVisibility.asStateFlow()
    
    // Gesture handlers
    private var verticalGestureHandler: VerticalGestureHandler? = null
    private var longPressSeekHandler: LongPressSeekHandler? = null
    
    // Long press seek info
    private val _longPressSeekInfo = MutableStateFlow<LongPressSeekHandler.LongPressSeekInfo?>(null)
    val longPressSeekInfo: StateFlow<LongPressSeekHandler.LongPressSeekInfo?> = _longPressSeekInfo.asStateFlow()
    
    // PlayerView reference for aspect ratio control
    private var playerView: PlayerView? = null
    
    // Current aspect ratio state
    private val _currentAspectRatio = MutableStateFlow("16:9")
    val currentAspectRatio: StateFlow<String> = _currentAspectRatio.asStateFlow()
    
    // Loop mode state
    private val _currentLoopMode = MutableStateFlow(com.astralplayer.nextplayer.data.LoopMode.OFF)
    val currentLoopMode: StateFlow<com.astralplayer.nextplayer.data.LoopMode> = _currentLoopMode.asStateFlow()
    
    init {
        // Initialize video quality tracking
        initializeQualityTracking()
    }
    
    private fun initializeQualityTracking() {
        // Convert PlayerRepository data qualities to UI qualities
        viewModelScope.launch {
            try {
                (playerRepository as? PlayerRepositoryImpl)?.availableQualities?.collect { dataQualities ->
                    val uiQualities = dataQualities.map { quality ->
                        com.astralplayer.nextplayer.ui.components.VideoQuality(
                            id = quality.id,
                            label = quality.label,
                            width = quality.width,
                            height = quality.height,
                            bitrate = quality.bitrate,
                            codec = extractCodec(quality),
                            fps = extractFps(quality)
                        )
                    }
                    availableQualities.value = uiQualities
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Error tracking available qualities", e)
                availableQualities.value = emptyList()
            }
        }
        
        viewModelScope.launch {
            try {
                (playerRepository as? PlayerRepositoryImpl)?.currentQuality?.collect { dataQuality ->
                    val uiQuality = dataQuality?.let { quality ->
                        com.astralplayer.nextplayer.ui.components.VideoQuality(
                            id = quality.id,
                            label = quality.label,
                            width = quality.width,
                            height = quality.height,
                            bitrate = quality.bitrate,
                            codec = extractCodec(dataQuality),
                            fps = extractFps(dataQuality)
                        )
                    }
                    currentQuality.value = uiQuality
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Error tracking current quality", e)
                currentQuality.value = null
            }
        }
    }
    
    /**
     * Setup vertical gesture handler with window reference
     */
    fun setupVerticalGestureHandler(window: Window) {
        gestureSettings.value.let { settings ->
            val context = getApplication<Application>()
            val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
            
            verticalGestureHandler = VerticalGestureHandler(
                context = context,
                window = window,
                screenHeight = screenHeight,
                volumeSettings = settings.volume,
                brightnessSettings = settings.brightness
            )
        }
    }
    
    fun setupLongPressSeekHandler() {
        gestureSettings.value.let { settings ->
            val context = getApplication<Application>()
            val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
            
            longPressSeekHandler = LongPressSeekHandler(
                settings = settings.longPress,
                screenWidth = screenWidth,
                onSpeedUpdate = { speed, direction -> 
                    // Handle speed update
                },
                onSeekUpdate = { seekAmount -> 
                    // Handle seek update
                },
                onEnd = {
                    // Handle long press end
                }
            )
        }
    }
    
    // Basic player controls
    fun play() {
        viewModelScope.launch {
            playerRepository.resumeVideo()
        }
    }
    
    fun pause() {
        viewModelScope.launch {
            playerRepository.pauseVideo()
        }
    }
    
    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            playerRepository.seekTo(positionMs)
        }
    }
    
    fun seekRelative(deltaMs: Long) {
        Log.d("PlayerVM", "seekRelative: deltaMs=$deltaMs")
        viewModelScope.launch {
            playerRepository.seekBy(deltaMs)
        }
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.SEEK_TICK)
        
        // Show seek preview overlay
        val currentPosition = playerRepository.playerState.value.currentPosition
        val duration = playerRepository.playerState.value.duration
        val targetPosition = (currentPosition + deltaMs).coerceIn(0, duration)
        Log.d("PlayerVM", "Seek from $currentPosition to $targetPosition (duration: $duration)")
        
        _uiState.update {
            it.copy(
                seekPreviewInfo = HorizontalSeekGestureHandler.SeekPreviewInfo(
                    seekPosition = targetPosition,
                    seekPercentage = if (duration > 0) targetPosition.toFloat() / duration.toFloat() else 0f,
                    isDragging = false,
                    showThumbnail = false,
                    showTimeIndicator = true,
                    isForward = deltaMs > 0,
                    velocity = 0f,
                    seekDelta = deltaMs,
                    targetPosition = targetPosition
                )
            )
        }
        
        // Hide seek preview after delay
        Handler(Looper.getMainLooper()).postDelayed({
            _uiState.update {
                it.copy(seekPreviewInfo = null)
            }
        }, 1500)
    }
    
    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            playerRepository.setPlaybackSpeed(speed)
        }
    }
    
    fun setVolume(volume: Float) {
        viewModelScope.launch {
            try {
                playerRepository.setVolume(volume)
                Log.d("PlayerVM", "setVolume: volume=$volume")
                
                // Update UI state to reflect the new volume
                _uiState.update {
                    it.copy(
                        volumeOverlayInfo = VolumeOverlayInfo(
                            volume = volume,
                            isVisible = true
                        )
                    )
                }
                
                // Hide volume overlay after delay
                Handler(Looper.getMainLooper()).postDelayed({
                    _uiState.update {
                        it.copy(volumeOverlayInfo = null)
                    }
                }, 2000)
                
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set volume", e)
            }
        }
    }
    
    // Helper method to extract codec from VideoQuality
    private fun extractCodec(quality: com.astralplayer.nextplayer.data.VideoQuality): String {
        // Use the codec property directly or extract from label
        return when {
            quality.codec.isNotEmpty() -> quality.codec
            quality.label.contains("H.264", ignoreCase = true) -> "H.264"
            quality.label.contains("H.265", ignoreCase = true) -> "H.265"
            quality.label.contains("HEVC", ignoreCase = true) -> "HEVC"
            quality.label.contains("VP9", ignoreCase = true) -> "VP9"
            quality.label.contains("VP8", ignoreCase = true) -> "VP8"
            quality.label.contains("AV1", ignoreCase = true) -> "AV1"
            else -> "H.264" // Default assumption
        }
    }
    
    private fun extractFps(quality: com.astralplayer.nextplayer.data.VideoQuality): Int {
        // Use the fps property from VideoQuality
        return quality.fps
    }
    
    override fun onCleared() {
        super.onCleared()
        verticalGestureHandler = null
        longPressSeekHandler = null
    }
}

// Data classes for UI state
data class PlayerUiState(
    val seekPreviewInfo: HorizontalSeekGestureHandler.SeekPreviewInfo? = null,
    val volumeOverlayInfo: VolumeOverlayInfo? = null,
    val brightnessOverlayInfo: BrightnessOverlayInfo? = null,
    val longPressSpeedInfo: LongPressSpeedInfo? = null
)

data class OverlayVisibility(
    val controlsVisible: Boolean = true,
    val gestureOverlaysVisible: Boolean = true
)

data class VolumeOverlayInfo(
    val volume: Float,
    val isVisible: Boolean = true
)

data class BrightnessOverlayInfo(
    val brightness: Float,
    val isVisible: Boolean = true
)

data class LongPressSpeedInfo(
    val speed: Float,
    val isVisible: Boolean = true
)