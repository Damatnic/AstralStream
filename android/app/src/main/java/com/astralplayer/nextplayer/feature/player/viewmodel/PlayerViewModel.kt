package com.astralplayer.nextplayer.feature.player.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.math.max
import kotlin.math.min

@UnstableApi
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    
    // Player instance
    var player: ExoPlayer? = null
       private set
    
    // Track selector for quality/audio/subtitle selection
    var trackSelector: DefaultTrackSelector? = null
        private set
    
    private val context: Context = application.applicationContext
    
    // State flows
    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()
    
    private val _volumeState = MutableStateFlow(1f)
    val volumeState: StateFlow<Float> = _volumeState.asStateFlow()
    
    private val _brightnessState = MutableStateFlow(0.5f)
    val brightnessState: StateFlow<Float> = _brightnessState.asStateFlow()
    
    // Loop state
    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()
    
    // Screen orientation state
    private val _screenOrientation = MutableStateFlow("auto")
    val screenOrientation: StateFlow<String> = _screenOrientation.asStateFlow()
    
    // Repeat mode state
    private val _repeatMode = MutableStateFlow("off")
    val repeatMode: StateFlow<String> = _repeatMode.asStateFlow()
    
    // Subtitle sync offset
    private val _subtitleOffset = MutableStateFlow(0L)
    val subtitleOffset: StateFlow<Long> = _subtitleOffset.asStateFlow()
    
    // Sleep timer states
    private val _sleepTimerState = MutableStateFlow<SleepTimer?>(null)
    val sleepTimerState: StateFlow<SleepTimer?> = _sleepTimerState.asStateFlow()
    
    // Jobs for coroutines
    private var positionUpdateJob: Job? = null
    private var hideControlsJob: Job? = null
    private var sleepTimerJob: Job? = null
    
    // Audio manager for volume control
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // Vibrator for haptic feedback
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    
    // Playback speed
    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    
    init {
        // Initialize brightness with system brightness
        initializeSystemBrightness()
    }
    
    fun initializePlayer(exoPlayer: ExoPlayer, uri: Uri, title: String) {
        player = exoPlayer
        
        // Add player listener
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackState(playbackState)
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startPositionUpdate()
                } else {
                    stopPositionUpdate()
                }
            }
            
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _playerState.update { 
                    it.copy(
                        videoWidth = videoSize.width,
                        videoHeight = videoSize.height
                    ) 
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                _playerState.update { 
                    it.copy(
                        hasError = true,
                        errorMessage = error.message ?: "Unknown error"
                    ) 
                }
            }
        })
        
        // Update UI state
        _playerState.update { 
            it.copy(
                videoUri = uri,
                videoTitle = title,
                isLoading = false
            ) 
        }
        
        // Start position updates
        if (player?.isPlaying == true) {
            startPositionUpdate()
        }
    }
    
    private fun updatePlaybackState(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> {
                _playerState.update { it.copy(isBuffering = true, isLoading = false) }
            }
            Player.STATE_READY -> {
                _playerState.update { 
                    it.copy(
                        isBuffering = false, 
                        isLoading = false,
                        duration = player?.duration ?: 0L
                    ) 
                }
            }
            Player.STATE_ENDED -> {
                _playerState.update { it.copy(isEnded = true) }
            }
            Player.STATE_IDLE -> {
                _playerState.update { it.copy(isLoading = true) }
            }
        }
    }
    
    private fun initializeSystemBrightness() {
        try {
            val brightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            ) / 255f
            _brightnessState.value = brightness
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error getting system brightness", e)
        }
    }
    
    private fun startPositionUpdate() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                player?.let { player ->
                    _playerState.update { 
                        it.copy(
                            currentPosition = player.currentPosition,
                            bufferedPosition = player.bufferedPosition
                        ) 
                    }
                }
                delay(100) // Update every 100ms
            }
        }
    }
    
    private fun stopPositionUpdate() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    fun playPause() {
        player?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }
    
    fun seekTo(position: Long) {
        player?.seekTo(position)
    }
    
    fun seekForward(seconds: Int = 10) {
        player?.let { player ->
            val newPosition = (player.currentPosition + seconds * 1000).coerceAtMost(player.duration)
            player.seekTo(newPosition)
        }
    }
    
    fun seekBackward(seconds: Int = 10) {
        player?.let { player ->
            val newPosition = (player.currentPosition - seconds * 1000).coerceAtLeast(0)
            player.seekTo(newPosition)
        }
    }
    
    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        player?.volume = clampedVolume
        _volumeState.value = clampedVolume
    }
    
    fun setBrightness(brightness: Float) {
        _brightnessState.value = brightness.coerceIn(0f, 1f)
    }
    
    fun toggleLoop() {
        player?.let { player ->
            val newLoopMode = if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                Player.REPEAT_MODE_OFF
            } else {
                Player.REPEAT_MODE_ONE
            }
            player.repeatMode = newLoopMode
            _isLooping.value = newLoopMode == Player.REPEAT_MODE_ONE
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }
    
    fun showControls() {
        _playerState.update { it.copy(areControlsVisible = true) }
        scheduleHideControls()
    }
    
    fun hideControls() {
        _playerState.update { it.copy(areControlsVisible = false) }
    }
    
    fun toggleControls() {
        if (_playerState.value.areControlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }
    
    private fun scheduleHideControls() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(3000) // Hide after 3 seconds
            if (player?.isPlaying == true) {
                hideControls()
            }
        }
    }
    
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        
        if (minutes > 0) {
            val endTime = System.currentTimeMillis() + (minutes * 60 * 1000)
            _sleepTimerState.value = SleepTimer(
                durationMinutes = minutes,
                endTime = endTime,
                isActive = true
            )
            
            sleepTimerJob = viewModelScope.launch {
                while (isActive && System.currentTimeMillis() < endTime) {
                    delay(1000)
                }
                // Timer expired
                player?.pause()
                _sleepTimerState.value = null
            }
        } else {
            _sleepTimerState.value = null
        }
    }
    
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerState.value = null
    }
    
    fun release() {
        stopPositionUpdate()
        hideControlsJob?.cancel()
        sleepTimerJob?.cancel()
        player?.release()
        player = null
    }
    
    override fun onCleared() {
        super.onCleared()
        release()
    }
}

// Data classes
data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLoading: Boolean = true,
    val isEnded: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val currentPosition: Long = 0L,
    val bufferedPosition: Long = 0L,
    val duration: Long = 0L,
    val videoUri: Uri? = null,
    val videoTitle: String = "",
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val areControlsVisible: Boolean = true
)

data class SleepTimer(
    val durationMinutes: Int,
    val endTime: Long,
    val isActive: Boolean
)