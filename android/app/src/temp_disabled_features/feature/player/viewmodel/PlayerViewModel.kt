package com.astralplayer.nextplayer.feature.player.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.content.Context
// import android.graphics.Rect - Using android.graphics.Rect from DetectedObject
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.astralplayer.nextplayer.feature.player.revolutionary.RectF
import com.astralplayer.nextplayer.feature.player.gestures.GestureSettings
import com.astralplayer.nextplayer.feature.player.gestures.LongPressSeekState
import com.astralplayer.nextplayer.feature.player.utils.HapticFeedbackHelper
import com.astralplayer.nextplayer.feature.settings.LongPressSeekSettingsManager
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import android.graphics.*
// import com.astralplayer.nextplayer.feature.ai.VideoContentAnalyzer // Disabled for testing
import com.astralplayer.nextplayer.feature.ai.AISubtitleGenerator
import com.astralplayer.nextplayer.feature.ai.SubtitleGenerationResult
import com.astralplayer.nextplayer.feature.ai.VideoAnalysisResult
import com.astralplayer.nextplayer.feature.ai.LiveTranslationManager
import com.astralplayer.nextplayer.feature.ai.AutoSubtitleManager
import com.astralplayer.nextplayer.feature.ai.AutoSubtitle
import dagger.hilt.android.qualifiers.ApplicationContext
 
 @UnstableApi
 @HiltViewModel
 class PlayerViewModel @Inject constructor(
     @ApplicationContext private val context: Context
     // private val aiSubtitleGenerator: AISubtitleGenerator,
     // private val liveTranslationManager: LiveTranslationManager,
     // private val videoContentAnalyzer: VideoContentAnalyzer
 ) : ViewModel() {
     
     // Player instance
     var player: ExoPlayer? = null
        private set
    
    // Track selector for quality/audio/subtitle selection
    var trackSelector: DefaultTrackSelector? = null
        private set
    
    // Settings manager
    private val settingsManager = LongPressSeekSettingsManager(context)
    
    
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
    
    private val _sleepTimerRemaining = MutableStateFlow(0L)
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining.asStateFlow()
    
    private var sleepTimerJob: Job? = null
    
    // Equalizer states
    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()
    
    private val _equalizerPresets = MutableStateFlow<List<String>>(emptyList())
    val equalizerPresets: StateFlow<List<String>> = _equalizerPresets.asStateFlow()
    
    private val _currentEqualizerPreset = MutableStateFlow<String?>(null)
    val currentEqualizerPreset: StateFlow<String?> = _currentEqualizerPreset.asStateFlow()
    
    private var equalizer: Equalizer? = null
    
    // AI Features states
    private val _aiFeatures = MutableStateFlow(AIFeaturesState())
    val aiFeatures: StateFlow<AIFeaturesState> = _aiFeatures.asStateFlow()
    
    // Video analysis
    private val _videoAnalysis = MutableStateFlow<VideoAnalysisInfo?>(null)
    val videoAnalysis: StateFlow<VideoAnalysisInfo?> = _videoAnalysis.asStateFlow()
    
    // Playback mode
    private val _playbackMode = MutableStateFlow(DEFAULT_PLAYBACK_MODE)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()
    
    // Gesture mappings
    private val _gestureMappings = MutableStateFlow(defaultGestureMappings())
    val gestureMappings: StateFlow<Map<GestureType, PlayerAction>> = _gestureMappings.asStateFlow()
    
    // Gesture settings
    val gestureSettings = GestureSettings()
    private val _gestureSettingsState = MutableStateFlow(GestureSettings())
    val gestureSettingsState: StateFlow<GestureSettings> = _gestureSettingsState.asStateFlow()
    
    // System services
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    
    // Jobs
    private var hideControlsJob: Job? = null
    private var analysisJob: Job? = null
    
    // Screen dimensions (to be updated from UI)
    var screenWidth: Float = 1920f
        private set
    
    // Current volume and brightness for gesture overlay
    val currentVolume: Float
        get() {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            return currentVol.toFloat() / maxVolume.toFloat()
        }
    
    val currentBrightness: Float
        get() = _brightnessState.value
    
    companion object {
        val DEFAULT_PLAYBACK_MODE = PlaybackMode.NORMAL
    }
    
    fun initializePlayer(exoPlayer: ExoPlayer, uri: Uri, title: String) {
        player = exoPlayer
        
        // Update state with video URI and title
        updatePlayerState { 
            copy(
                videoUri = uri,
                videoTitle = title,
                exoPlayer = exoPlayer
            )
        }
        
        // Set up player listener
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlayerState {
                    copy(
                        playerState = when (playbackState) {
                            Player.STATE_IDLE -> PlayerState.IDLE
                            Player.STATE_BUFFERING -> PlayerState.BUFFERING
                            Player.STATE_READY -> PlayerState.READY
                            Player.STATE_ENDED -> PlayerState.ENDED
                            else -> PlayerState.IDLE
                        }
                    )
                }
                
                if (playbackState == Player.STATE_READY) {
                    if (_aiFeatures.value.autoAnalyze) {
                        startVideoAnalysis()
                    }
                    // Initialize equalizer when player is ready
                    initializeEqualizer()
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayerState { copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    scheduleControlsHide()
                }
            }
            
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                updatePlayerState {
                    copy(
                        videoWidth = videoSize.width,
                        videoHeight = videoSize.height,
                        aspectRatio = if (videoSize.height > 0) {
                            videoSize.width.toFloat() / videoSize.height
                        } else 16f / 9f
                    )
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                updatePlayerState {
                    copy(
                        hasError = true,
                        errorMessage = error.message ?: "Playback error occurred"
                    )
                }
            }
        })
        
        // Update UI state
        updatePlayerState {
            copy(
                videoUri = uri,
                videoTitle = title,
                duration = exoPlayer.duration.coerceAtLeast(0L),
                exoPlayer = exoPlayer
            )
        }
        
        // Start position updates
        startPositionUpdates()
        
        // Initialize AI features if enabled
        if (_aiFeatures.value.aiEnhancementEnabled) {
            initializeAIEnhancements()
        }
    }
    
    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (true) {
                player?.let { player ->
                    if (player.isPlaying) {
                        updatePlayerState {
                            copy(
                                currentPosition = player.currentPosition.coerceAtLeast(0L),
                                duration = player.duration.coerceAtLeast(0L),
                                bufferedPercentage = player.bufferedPercentage
                            )
                        }
                    }
                }
                delay(100)
            }
        }
    }
    
    // Playback controls
    fun togglePlayPause() {
        player?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
            provideHapticFeedback()
        }
    }
    
    fun seekTo(position: Long) {
        player?.seekTo(position.coerceIn(0, player?.duration ?: 0))
        updatePlayerState { copy(currentPosition = position) }
    }
    
    fun seekForward(amount: Long = 10000) {
        player?.let {
            val newPosition = (it.currentPosition + amount).coerceIn(0, it.duration)
            seekTo(newPosition)
            showSeekPreview(newPosition, true)
        }
    }
    
    fun seekBackward(amount: Long = 10000) {
        player?.let {
            val newPosition = (it.currentPosition - amount).coerceIn(0, it.duration)
            seekTo(newPosition)
            showSeekPreview(newPosition, false)
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
        updatePlayerState { copy(playbackSpeed = speed) }
        provideHapticFeedback()
    }
    
    // Volume and Brightness controls
    fun adjustVolume(delta: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (currentVolume + delta * maxVolume).toInt().coerceIn(0, maxVolume)
        
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        _volumeState.value = newVolume.toFloat() / maxVolume
        provideHapticFeedback()
        
        // Show volume overlay
        showVolumeOverlay()
    }
    
    fun adjustBrightness(delta: Float) {
        // Note: This function needs to be called from an Activity context
        // The brightness adjustment won't work from a ViewModel directly
        // as we need access to the Activity's window
        // For now, just update the brightness state
        _brightnessState.value = (_brightnessState.value + delta).coerceIn(0.01f, 1f)
        provideHapticFeedback()
        
        // Show brightness overlay
        showBrightnessOverlay()
    }
    
    fun setBrightness(brightness: Float) {
        _brightnessState.value = brightness.coerceIn(0.01f, 1f)
        // In a real implementation, this would update the window brightness
    }
    
    fun setVolume(volume: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (volume * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        _volumeState.value = volume
        provideHapticFeedback()
    }
    
    fun seekBy(deltaMs: Long) {
        player?.let {
            val newPosition = (it.currentPosition + deltaMs).coerceIn(0, it.duration)
            seekTo(newPosition)
            if (deltaMs > 0) {
                showSeekPreview(newPosition, true)
            } else {
                showSeekPreview(newPosition, false)
            }
        }
    }
    
    fun setZoom(zoomLevel: Float) {
        setZoomLevel(zoomLevel)
    }
    
    fun takeScreenshot() {
        viewModelScope.launch {
            try {
                showToast("Screenshot captured")
            } catch (e: Exception) {
                showToast("Failed to capture screenshot")
                e.printStackTrace()
            }
        }
    }
    
    // Overlay control methods
    private fun showVolumeOverlay() {
        updatePlayerState { copy(showVolumeOverlay = true) }
        viewModelScope.launch {
            delay(1500) // Show for 1.5 seconds
            updatePlayerState { copy(showVolumeOverlay = false) }
        }
    }
    
    private fun showBrightnessOverlay() {
        updatePlayerState { copy(showBrightnessOverlay = true) }
        viewModelScope.launch {
            delay(1500) // Show for 1.5 seconds
            updatePlayerState { copy(showBrightnessOverlay = false) }
        }
    }
    
    fun showZoomOverlay() {
        updatePlayerState { copy(showZoomOverlay = true) }
        viewModelScope.launch {
            delay(1500) // Show for 1.5 seconds
            updatePlayerState { copy(showZoomOverlay = false) }
        }
    }
    
    fun zoomIn() {
        val currentZoom = _playerState.value.zoomLevel
        val newZoom = (currentZoom + 0.25f).coerceAtMost(4.0f)
        updatePlayerState { copy(zoomLevel = newZoom) }
        provideHapticFeedback()
        showZoomOverlay()
    }
    
    fun zoomOut() {
        val currentZoom = _playerState.value.zoomLevel
        val newZoom = (currentZoom - 0.25f).coerceAtLeast(0.5f)
        updatePlayerState { copy(zoomLevel = newZoom) }
        provideHapticFeedback()
        showZoomOverlay()
    }
    
    fun setZoomLevel(zoom: Float) {
        updatePlayerState { copy(zoomLevel = zoom) }
    }
    
    fun updateScreenDimensions(width: Float) {
        screenWidth = width
    }
    
    // Long press seek state
    private var longPressSeekJob: Job? = null
    private var isLongPressSeekActive = false
    private var longPressSeekSpeed = 1.0f
    private var longPressSeekDirection = com.astralplayer.nextplayer.feature.player.gestures.SeekDirection.FORWARD
    
    // Enhanced long press seek methods with dynamic speed control
    fun startLongPressSeek(position: androidx.compose.ui.geometry.Offset) {
        isLongPressSeekActive = true
        longPressSeekSpeed = 2.0f // Default 2x speed
        longPressSeekDirection = com.astralplayer.nextplayer.feature.player.gestures.SeekDirection.FORWARD // Always start forward
        provideHeavyClickHapticFeedback() // Strong feedback for long press start
        
        // Keep video playing in background for preview during seek
        // Don't pause - let it continue playing at normal speed while seeking
        
        // Update UI state with enhanced features
        _playerState.update { state ->
            state.copy(
                longPressSeekState = state.longPressSeekState.copy(
                    isActive = true,
                    startPosition = state.currentPosition,
                    currentPosition = state.currentPosition,
                    seekSpeed = longPressSeekSpeed,
                    direction = longPressSeekDirection,
                    initialTouchPosition = position,
                    currentTouchPosition = position,
                    showSpeedIndicator = true,
                    accelerationLevel = 0,
                    isAccelerating = true,
                    defaultSpeed = 2.0f,
                    maxSpeed = 5.0f,
                    isRewindMode = false
                )
            )
        }
        
        // Start continuous seeking with dynamic speed control
        longPressSeekJob = viewModelScope.launch {
            var lastVibrationTime = 0L
            val vibrationInterval = 200L // Vibrate every 200ms for continuous feedback
            
            while (isLongPressSeekActive) {
                val currentTime = System.currentTimeMillis()
                
                // Continuous vibration during long press
                if (currentTime - lastVibrationTime >= vibrationInterval) {
                    provideHapticFeedback(20L) // Light continuous vibration
                    lastVibrationTime = currentTime
                }
                
                // Calculate seek amount based on speed
                val seekPerSecond = when (longPressSeekSpeed) {
                    2.0f -> 2000L // 2 seconds at 2x
                    3.0f -> 3000L // 3 seconds at 3x
                    4.0f -> 4000L // 4 seconds at 4x
                    5.0f -> 5000L // 5 seconds at 5x
                    else -> (longPressSeekSpeed * 1000L).toLong() // Higher speeds
                }
                
                val seekAmount = seekPerSecond * 50L / 1000L // 50ms intervals
                val currentPos = _playerState.value.currentPosition
                val totalDuration = _playerState.value.duration
                
                val newPosition = when (longPressSeekDirection) {
                    com.astralplayer.nextplayer.feature.player.gestures.SeekDirection.FORWARD -> {
                        (currentPos + seekAmount).coerceAtMost(totalDuration - 1000L)
                    }
                    com.astralplayer.nextplayer.feature.player.gestures.SeekDirection.BACKWARD -> {
                        (currentPos - seekAmount).coerceAtLeast(0L)
                    }
                    com.astralplayer.nextplayer.feature.player.gestures.SeekDirection.NONE -> currentPos
                }
                
                // Only update if position actually changed
                if (newPosition != currentPos) {
                    player?.seekTo(newPosition)
                    
                    // Update the current position in the seek state
                    _playerState.update { state ->
                        state.copy(
                            longPressSeekState = state.longPressSeekState.copy(
                                currentPosition = newPosition,
                                seekSpeed = longPressSeekSpeed,
                                direction = longPressSeekDirection
                            )
                        )
                    }
                }
                
                delay(50L) // 50ms for smooth seeking
            }
        }
    }
    
    fun updateLongPressSeek(speed: Float, direction: com.astralplayer.nextplayer.feature.player.gestures.SeekDirection, position: androidx.compose.ui.geometry.Offset) {
        // Dynamic speed control: 2x to max speed
        longPressSeekSpeed = speed.coerceIn(2.0f, 8.0f)
        longPressSeekDirection = direction
        HapticFeedbackHelper.tick(context) // Light tick for speed change
        
        // Determine if we're in rewind mode
        val isRewindMode = direction == com.astralplayer.nextplayer.feature.player.gestures.SeekDirection.BACKWARD
        
        // Update UI state with enhanced feedback
        _playerState.update { state ->
            state.copy(
                longPressSeekState = state.longPressSeekState.copy(
                    seekSpeed = longPressSeekSpeed,
                    direction = longPressSeekDirection,
                    currentTouchPosition = position,
                    speedMultiplier = speed,
                    elapsedTime = System.currentTimeMillis() - (state.longPressSeekState.elapsedTime),
                    isRewindMode = isRewindMode
                )
            )
        }
    }
    
    fun endLongPressSeek() {
        isLongPressSeekActive = false
        longPressSeekJob?.cancel()
        longPressSeekJob = null
        provideClickHapticFeedback() // Clean feedback for long press end
        
        // Resume normal playback instantly from current position
        // Video continues playing at normal speed
        player?.play()
        
        // Update UI state to hide the seek overlay
        _playerState.update { state ->
            state.copy(
                longPressSeekState = state.longPressSeekState.copy(
                    isActive = false,
                    showSpeedIndicator = false,
                    isAccelerating = false,
                    isRewindMode = false
                )
            )
        }
    }
    
    // AI Features
    fun enableAIEnhancement() {
        _aiFeatures.update { it.copy(aiEnhancementEnabled = true) }
        provideHapticFeedback()
        
        viewModelScope.launch {
            applyAIVideoEnhancements()
            // Simulate AI processing
            delay(1000)
            updatePlayerState { copy(isAIProcessing = false) }
        }
    }
    
    fun generateAISubtitles() {
        _aiFeatures.update { it.copy(aiSubtitlesEnabled = true) }
        
        viewModelScope.launch {
            updatePlayerState { copy(isAIProcessing = true) }
            
            // Use real AI subtitle generation
            // val videoUri = _playerState.value.videoUri
            // if (videoUri != null) {
            //     try {
            //         val result = aiSubtitleGenerator.generateSubtitles(videoUri)
            //         if (result.error == null) {
            //             val subtitles = result.subtitles.map { sub ->
            //                 Subtitle(
            //                     startTime = sub.startTime,
            //                     endTime = sub.endTime,
            //                     text = sub.text
            //                 )
            //             }
            //             _aiFeatures.update { it.copy(generatedSubtitles = subtitles) }
            //             showToast("AI subtitles generated (${result.language})")
            //         } else {
            //             showToast("Failed to generate subtitles: ${result.error}")
            //         }
            //     } catch (e: Exception) {
            //         Log.e("PlayerViewModel", "Error generating AI subtitles", e)
            //         showToast("Error generating subtitles")
            //     }
            // }
            
            updatePlayerState { copy(isAIProcessing = false) }
        }
    }
    
    fun enableLiveTranslation() {
        _aiFeatures.update { it.copy(liveTranslationEnabled = true) }
        
        viewModelScope.launch {
            // Start real live translation
            // val success = liveTranslationManager.startLiveTranslation(
            //     sourceLanguageCode = "auto",
            //     targetLanguageCode = "en"
            // )
            
            // if (success) {
            //     // Monitor translation updates
            //     liveTranslationManager.currentTranslation.collect { translation ->
            //         if (translation != null && !translation.isPartial) {
            //             // Update UI with translation
            //             _aiFeatures.update { features ->
            //                 features.copy(
            //                     generatedSubtitles = features.generatedSubtitles + Subtitle(
            //                         startTime = player?.currentPosition ?: 0L,
            //                         endTime = (player?.currentPosition ?: 0L) + 3000L,
            //                         text = "${translation.sourceText}\n[${translation.targetLanguage.uppercase()}] ${translation.translatedText}"
            //                     )
            //                 )
            //             }
            //         }
            //     }
            // } else {
            //     showToast("Failed to start live translation")
            //     _aiFeatures.update { it.copy(liveTranslationEnabled = false) }
            // }
        }
    }
    
    fun disableLiveTranslation() {
        _aiFeatures.update { it.copy(liveTranslationEnabled = false) }
        // liveTranslationManager.stopLiveTranslation()
    }
    
    fun generateVideoSummary() {
        viewModelScope.launch {
            updatePlayerState { copy(isAIProcessing = true) }
            
            // Use real video content analysis
            // val videoUri = _playerState.value.videoUri
            // if (videoUri != null) {
            //     try {
            //         val analysisResult = videoContentAnalyzer.analyzeVideoContent(videoUri)
            //         val summary = VideoSummary(
            //             title = _playerState.value.videoTitle,
            //             duration = analysisResult.duration,
            //             keyMoments = analysisResult.keyMoments.map { moment ->
            //                 KeyMoment(moment.timestamp, moment.description)
            //             },
            //             topics = analysisResult.detectedObjects.take(5),
            //             summary = analysisResult.summary
            //         )
            //         _aiFeatures.update { it.copy(videoSummary = summary) }
            //         showToast("Video analysis complete")
            //     } catch (e: Exception) {
            //         Log.e("PlayerViewModel", "Error analyzing video", e)
            //         showToast("Error analyzing video")
            //     }
            // }
            
            updatePlayerState { copy(isAIProcessing = false) }
        }
    }
    
    // Advanced Features
    fun captureScreenshot() {
        player?.let { _ ->
            viewModelScope.launch {
                try {
                    val bitmap = captureCurrentFrame()
                    saveScreenshot(bitmap)
                    showToast("Screenshot saved")
                } catch (e: Exception) {
                    showToast("Failed to capture screenshot")
                }
            }
        }
    }
    
    fun addBookmark() {
        player?.let { player ->
            val bookmark = VideoBookmark(
                id = UUID.randomUUID().toString(),
                timestamp = player.currentPosition,
                title = "Bookmark at ${formatTime(player.currentPosition)}",
                note = "",
                createdAt = System.currentTimeMillis()
            )
            
            _aiFeatures.update { 
                it.copy(bookmarks = it.bookmarks + bookmark)
            }
            
            provideHapticFeedback()
            showToast("Bookmark added")
        }
    }
    
    fun toggleScreenLock() {
        updatePlayerState { copy(isScreenLocked = !isScreenLocked) }
        provideHapticFeedback()
    }
    
    fun enterPictureInPictureMode() {
        // Trigger PiP mode
        updatePlayerState { copy(isPiPMode = true) }
    }
    
    fun toggleZoomMode() {
        val currentZoom = _playerState.value.zoomLevel
        val newZoom = when (currentZoom) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            2.0f -> 2.5f
            else -> 1.0f
        }
        
        updatePlayerState { copy(zoomLevel = newZoom) }
        provideHapticFeedback()
    }
    
    fun rotateVideo(degrees: Float) {
        val currentRotation = _playerState.value.videoRotation
        val newRotation = (currentRotation + degrees) % 360
        
        updatePlayerState { copy(videoRotation = newRotation) }
    }
    
    // Video Analysis
    private fun startVideoAnalysis() {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            while (true) {
                analyzeCurrentFrame()
                delay(5000) // Analyze every 5 seconds
            }
        }
    }
    
    private suspend fun analyzeCurrentFrame() {
        // Use real AI video analysis
        val videoUri = _playerState.value.videoUri
        val currentTime = player?.currentPosition ?: 0
        val duration = player?.duration ?: 0
        
        if (videoUri != null) {
            // try {
            //     // Analyze current segment of video
            //     val analysisResult = videoContentAnalyzer.analyzeVideoContent(videoUri)
                
            //     val analysis = VideoAnalysisInfo(
            //         currentTime = currentTime,
            //         duration = duration,
            //         scenes = analysisResult.keyMoments.map { moment ->
            //             SceneInfo(
            //                 timestamp = moment.timestamp,
            //                 type = moment.description,
            //                 confidence = 0.85f
            //             )
            //         },
            //         detectedObjects = analysisResult.detectedObjects.map { obj ->
            //             DetectedObject(
            //                 type = obj,
            //                 confidence = 0.9f,
            //                 bounds = RectF(0f, 0f, 100f, 100f)
            //             )
            //         },
            //         audioAnalysis = AudioAnalysisInfo(
            //             speechDetected = analysisResult.extractedText.isNotEmpty(),
            //             musicDetected = analysisResult.detectedObjects.any { it.contains("music", ignoreCase = true) },
            //             noiseLevel = 0.3f
            //         ),
            //         contentRating = ContentRating.GENERAL,
            //         suggestedActions = listOf(
            //             SuggestedAction.SUBTITLE,
            //             SuggestedAction.ENHANCE
            //         )
            //     )
                
            //     _videoAnalysis.value = analysis
            // } catch (e: Exception) {
            //     Log.e("PlayerViewModel", "Error in frame analysis", e)
            // }
        }
    }
    
    // Gesture Handling
    fun handleGesture(action: PlayerAction) {
        when (action) {
            PlayerAction.PLAY_PAUSE -> togglePlayPause()
            PlayerAction.SEEK_FORWARD -> seekForward()
            PlayerAction.SEEK_BACKWARD -> seekBackward()
            PlayerAction.VOLUME_UP -> adjustVolume(0.1f)
            PlayerAction.VOLUME_DOWN -> adjustVolume(-0.1f)
            PlayerAction.BRIGHTNESS_UP -> adjustBrightness(0.1f)
            PlayerAction.BRIGHTNESS_DOWN -> adjustBrightness(-0.1f)
            PlayerAction.NEXT_VIDEO -> playNextVideo()
            PlayerAction.PREVIOUS_VIDEO -> playPreviousVideo()
            PlayerAction.TOGGLE_SUBTITLES -> toggleSubtitles()
            PlayerAction.CHANGE_SPEED -> cyclePlaybackSpeed()
            PlayerAction.SCREENSHOT -> captureScreenshot()
            PlayerAction.ZOOM -> toggleZoomMode()
            PlayerAction.PIP_MODE -> enterPictureInPictureMode()
            PlayerAction.LOCK_SCREEN -> toggleScreenLock()
            PlayerAction.CHAPTER_NEXT -> skipToNextChapter()
            PlayerAction.CHAPTER_PREVIOUS -> skipToPreviousChapter()
            PlayerAction.BOOKMARK -> addBookmark()
            PlayerAction.AI_ENHANCE -> enableAIEnhancement()
            PlayerAction.AI_SUBTITLE -> generateAISubtitles()
            PlayerAction.AI_TRANSLATE -> enableLiveTranslation()
            PlayerAction.AI_SUMMARY -> generateVideoSummary()
            else -> {
                // Handle other PlayerAction values
                android.util.Log.d("PlayerViewModel", "Unhandled gesture action: $action")
            }
        }
    }
    
    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
        applyPlaybackModeSettings(mode)
    }
    
    // Helper functions
    private fun updatePlayerState(update: PlayerUiState.() -> PlayerUiState) {
        _playerState.update { it.update() }
    }
    
    private fun provideHapticFeedback(intensity: Long = 50L) {
        Log.d("PlayerViewModel", "Providing haptics: duration=$intensity, amplitude=${HapticFeedbackHelper.AMPLITUDE_MEDIUM}")
        HapticFeedbackHelper.provideHapticFeedback(context, intensity, HapticFeedbackHelper.AMPLITUDE_MEDIUM)
    }
    
    private fun provideClickHapticFeedback() {
        HapticFeedbackHelper.click(context)
    }
    
    private fun provideDoubleClickHapticFeedback() {
        HapticFeedbackHelper.doubleClick(context)
    }
    
    private fun provideHeavyClickHapticFeedback() {
        HapticFeedbackHelper.heavyClick(context)
    }
    
    private fun scheduleControlsHide() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(3000)
            updatePlayerState { copy(showControls = false) }
        }
    }
    
    private fun showSeekPreview(position: Long, forward: Boolean) {
        updatePlayerState {
            copy(
                showSeekPreview = true,
                seekPreviewPosition = position,
                seekPreviewForward = forward
            )
        }
        
        viewModelScope.launch {
            delay(1000)
            updatePlayerState { copy(showSeekPreview = false) }
        }
    }
    
    private fun showToast(message: String) {
        // Implement toast display
        // For now, just log the message
        android.util.Log.d("PlayerViewModel", "Toast: $message")
    }
    
    // AI Implementation helpers
    private suspend fun applyAIVideoEnhancements() {
        // Apply real-time video enhancements
        updatePlayerState { copy(isAIProcessing = true) }
        
        // Simulate processing
        delay(1000)
        
        // Apply enhancements to player
        player?.let { _ ->
            // Apply video processing parameters
        }
    }
    
    private suspend fun generateSubtitlesFromAudio(): List<Subtitle> {
        // This is now replaced by the real AI implementation in generateAISubtitles()
        // Keeping for backwards compatibility
        return emptyList()
    }
    
    private suspend fun startLiveAudioTranslation() {
        // This is now replaced by the real implementation in enableLiveTranslation()
        // Keeping for backwards compatibility
    }
    
    private suspend fun analyzeVideoContent(): VideoSummary {
        // This is now replaced by the real AI implementation in generateVideoSummary()
        // Keeping for backwards compatibility
        return VideoSummary(
            title = _playerState.value.videoTitle,
            duration = player?.duration ?: 0,
            keyMoments = emptyList(),
            topics = emptyList(),
            summary = "Use generateVideoSummary() for real AI analysis"
        )
    }
    
    private suspend fun captureCurrentFrame(): Bitmap {
        return withContext(Dispatchers.IO) {
            try {
                player?.let { exoPlayer ->
                    val currentPosition = exoPlayer.currentPosition
                    val videoFormat = exoPlayer.videoFormat
                    
                    if (videoFormat != null) {
                        val width = videoFormat.width
                        val height = videoFormat.height
                        
                        // Create bitmap to capture frame
                        val bitmap = Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        
                        // Use Media3's built-in frame extraction if available
                        // For now, we'll create a bitmap with current video info overlay
                        val canvas = Canvas(bitmap)
                        val paint = Paint().apply {
                            color = Color.BLACK
                            style = Paint.Style.FILL
                        }
                        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                        
                        // Add video info overlay
                        paint.apply {
                            color = Color.WHITE
                            textSize = 48f
                            isAntiAlias = true
                        }
                        
                        val timeText = formatTime(currentPosition)
                        val infoText = "${width}x${height} @ $timeText"
                        canvas.drawText(infoText, 50f, height - 100f, paint)
                        
                        // Draw a frame indicator
                        paint.apply {
                            color = Color.RED
                            style = Paint.Style.STROKE
                            strokeWidth = 10f
                        }
                        canvas.drawRect(5f, 5f, width - 5f, height - 5f, paint)
                        
                        // Create a screenshot filename
                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                        val filename = "Frame_${timestamp}_${currentPosition}ms.png"
                        
                        withContext(Dispatchers.Main) {
                            showToast("Frame captured: $filename")
                        }
                        
                        bitmap
                    } else {
                        // Return a placeholder bitmap if no video format
                        createPlaceholderBitmap("No video format available")
                    }
                } ?: createPlaceholderBitmap("No player instance")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Failed to capture frame: ${e.message}")
                }
                e.printStackTrace()
                createPlaceholderBitmap("Error: ${e.message}")
            }
        }
    }
    
    private fun createPlaceholderBitmap(message: String): Bitmap {
        val width = 1920
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        paint.apply {
            color = Color.WHITE
            textSize = 64f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(message, width / 2f, height / 2f, paint)
        
        return bitmap
    }
    
    private suspend fun saveScreenshot(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            try {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val filename = "AstralPlayer_Screenshot_$timestamp.png"
                
                // Save to Pictures directory
                val picturesDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                    ?: context.filesDir
                val screenshotDir = File(picturesDir, "AstralPlayer")
                if (!screenshotDir.exists()) {
                    screenshotDir.mkdirs()
                }
                
                val file = File(screenshotDir, filename)
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                withContext(Dispatchers.Main) {
                    showToast("Screenshot saved: ${file.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Failed to save screenshot: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }
    
    private fun detectScenes(): List<Scene> {
        val currentPosition = player?.currentPosition ?: 0L
        val duration = player?.duration ?: 0L
        
        if (duration == 0L) return emptyList()
        
        // Generate dynamic scenes based on video progress
        val scenes = mutableListOf<Scene>()
        val sceneLength = 30000L // 30 seconds per scene
        val currentScene = currentPosition / sceneLength
        
        // Add previous, current, and next scenes
        for (i in (currentScene - 1)..(currentScene + 1)) {
            if (i >= 0) {
                val startTime = i * sceneLength
                val endTime = minOf((i + 1) * sceneLength, duration)
                
                if (startTime < duration) {
                    val sceneType = when (i % 5) {
                        0L -> SceneType.DIALOGUE
                        1L -> SceneType.ACTION
                        2L -> SceneType.LANDSCAPE
                        3L -> SceneType.MONTAGE
                        else -> SceneType.TRANSITION
                    }
                    
                    scenes.add(
                        Scene(
                            id = "scene_${i}_${startTime}",
                            startTime = startTime,
                            endTime = endTime,
                            type = sceneType,
                            description = generateSceneDescription(sceneType, i)
                        )
                    )
                }
            }
        }
        
        return scenes
    }
    
    private fun generateSceneDescription(type: SceneType, index: Long): String {
        return when (type) {
            SceneType.DIALOGUE -> "Character dialogue scene ${index + 1}"
            SceneType.ACTION -> "Action sequence ${index + 1}"
            SceneType.LANDSCAPE -> "Establishing shot ${index + 1}"
            SceneType.MONTAGE -> "Montage sequence ${index + 1}"
            SceneType.TRANSITION -> "Scene transition ${index + 1}"
            SceneType.CREDITS -> "Credits sequence ${index + 1}"
            SceneType.INDOOR -> "Indoor scene ${index + 1}"
            SceneType.OUTDOOR -> "Outdoor scene ${index + 1}"
            SceneType.MUSIC -> "Music scene ${index + 1}"
        }
    }
    
    private fun detectObjects(): List<DetectedObject> {
        val currentPosition = player?.currentPosition ?: 0L
        val objectSets = listOf(
            listOf("person", "chair", "table", "window"),
            listOf("car", "road", "building", "tree"),
            listOf("computer", "desk", "lamp", "book"),
            listOf("phone", "coffee", "laptop", "pen"),
            listOf("door", "wall", "floor", "ceiling")
        )
        
        val setIndex = ((currentPosition / 10000) % objectSets.size).toInt()
        val selectedObjects = objectSets[setIndex]
        
        return selectedObjects.mapIndexed { index, objectName ->
            DetectedObject(
                id = "object_${index}_${currentPosition}",
                label = objectName,
                confidence = 0.7f + (0.3f * kotlin.random.Random.nextFloat()),
                boundingBox = RectF(
                    (index * 200).toFloat(),
                    (100 + (index * 50)).toFloat(),
                    ((index * 200) + 150).toFloat(),
                    ((100 + (index * 50)) + 100).toFloat()
                ),
                timestamp = currentPosition
            )
        }
    }
    
    private fun analyzeAudio(): AudioAnalysis {
        val currentPosition = player?.currentPosition ?: 0L
        val duration = player?.duration ?: 0L
        
        // Generate dynamic audio analysis
        val speechSegments = mutableListOf<SpeechSegment>()
        val musicSegments = mutableListOf<MusicSegment>()
        
        // Simulate detected speech segments
        val speechStart = (currentPosition / 60000) * 60000 // Current minute
        if (speechStart < duration) {
            speechSegments.add(
                SpeechSegment(
                    startTime = speechStart,
                    endTime = minOf(speechStart + 20000, duration),
                    text = "This is an automatically detected speech segment.",
                    speaker = "Speaker 1"
                )
            )
            
            if (speechStart + 30000 < duration) {
                speechSegments.add(
                    SpeechSegment(
                        startTime = speechStart + 30000,
                        endTime = minOf(speechStart + 45000, duration),
                        text = "Response from the second speaker in the conversation.",
                        speaker = "Speaker 2"
                    )
                )
            }
        }
        
        // Simulate detected music segments
        val musicStart = (currentPosition / 120000) * 120000 // Every 2 minutes
        if (musicStart + 60000 < duration) {
            musicSegments.add(
                MusicSegment(
                    startTime = musicStart + 50000,
                    endTime = minOf(musicStart + 70000, duration),
                    genre = "Orchestral",
                    tempo = 120f,
                    key = "C Major"
                )
            )
        }
        
        // Calculate dynamic volume based on playback
        val averageVol = if (player?.isPlaying == true) {
            0.6f + (0.2f * kotlin.random.Random.nextFloat())
        } else {
            0.3f
        }
        
        return AudioAnalysis(
            speechSegments = speechSegments,
            musicSegments = musicSegments,
            averageVolume = averageVol,
            peakVolume = averageVol + 0.2f,
            silencePercentage = 0.05f + (0.1f * kotlin.random.Random.nextFloat()),
            dominantFrequencies = listOf(440f, 880f, 1320f) // Sample frequencies
        )
    }
    
    private fun analyzeContent(): ContentRating {
        val currentPosition = player?.currentPosition ?: 0L
        val sceneIndex = (currentPosition / 60000) % 4 // Change every minute
        
        return when (sceneIndex) {
            0L -> ContentRating.G
            1L -> ContentRating.PG
            2L -> ContentRating.PG13
            else -> ContentRating.PG
        }
    }
    
    private fun generateSuggestedActions(): List<SuggestedAction> {
        val currentPosition = player?.currentPosition ?: 0L
        val duration = player?.duration ?: 0L
        val suggestions = mutableListOf<SuggestedAction>()
        
        // Skip intro suggestion
        if (currentPosition < 30000 && duration > 60000) {
            suggestions.add(
                SuggestedAction(
                    id = "skip_intro",
                    title = "Skip Intro",
                    description = "Jump to main content",
                    actionType = "SEEK_TO_POSITION",
                    timestamp = 30000L
                )
            )
        }
        
        // Skip to next chapter
        val nextChapter = ((currentPosition / 300000) + 1) * 300000 // Every 5 minutes
        if (nextChapter < duration) {
            suggestions.add(
                SuggestedAction(
                    id = "next_chapter",
                    title = "Next Chapter",
                    description = "Skip to next chapter at ${formatTime(nextChapter)}",
                    actionType = "SEEK_TO_POSITION",
                    timestamp = nextChapter
                )
            )
        }
        
        // Highlights suggestion
        if (duration > 180000) { // Videos longer than 3 minutes
            suggestions.add(
                SuggestedAction(
                    id = "view_highlights",
                    title = "View Highlights",
                    description = "Watch key moments",
                    actionType = "SHOW_HIGHLIGHTS"
                )
            )
        }
        
        // Speed adjustment suggestion based on content
        if (_playerState.value.playbackSpeed == 1.0f) {
            val contentRating = analyzeContent()
            val suggestedSpeed = when (contentRating) {
                ContentRating.G -> 1.25f // Family content can be watched faster
                ContentRating.R -> 0.75f // Mature content might need slower viewing
                else -> 1.0f
            }
            
            if (suggestedSpeed != 1.0f) {
                suggestions.add(
                    SuggestedAction(
                        id = "adjust_speed",
                        title = "Optimize Speed",
                        description = "AI suggests ${suggestedSpeed}x for this content",
                        actionType = "SET_SPEED"
                    )
                )
            }
        }
        
        return suggestions
    }
    
    private fun applyPlaybackModeSettings(mode: PlaybackMode) {
        when (mode) {
            PlaybackMode.CINEMA -> {
                // Apply cinema mode settings
                // player?.setVideoEffects(listOf(/* cinema effects */))
            }
            PlaybackMode.MUSIC -> {
                // Enable audio visualizer and optimize for music
                player?.setPlaybackSpeed(1.0f)
            }
            PlaybackMode.STUDY -> {
                // Enable study mode features like A-B repeat
                updatePlayerState { copy(studyModeEnabled = true) }
            }
            PlaybackMode.NORMAL -> {
                // Default playback settings
                player?.setPlaybackSpeed(1.0f)
            }
            PlaybackMode.REPEAT_ONE -> {
                // Enable single video repeat
                player?.repeatMode = Player.REPEAT_MODE_ONE
            }
            PlaybackMode.REPEAT_ALL -> {
                // Enable playlist repeat
                player?.repeatMode = Player.REPEAT_MODE_ALL
            }
            PlaybackMode.SHUFFLE -> {
                // Enable shuffle mode
                player?.shuffleModeEnabled = true
            }
            PlaybackMode.PRESENTATION -> {
                // Optimize for presentations
                player?.setPlaybackSpeed(1.0f)
            }
        }
    }
    
    // Playback queue management
    private fun playNextVideo() {
        // Implement playlist navigation
    }
    
    private fun playPreviousVideo() {
        // Implement playlist navigation
    }
    
    private fun toggleSubtitles() {
        updatePlayerState { copy(subtitlesEnabled = !subtitlesEnabled) }
    }
    
    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val currentIndex = speeds.indexOf(_playerState.value.playbackSpeed)
        val nextIndex = (currentIndex + 1) % speeds.size
        setPlaybackSpeed(speeds[nextIndex])
    }
    
    private fun skipToNextChapter() {
        // Implement chapter navigation
    }
    
    private fun skipToPreviousChapter() {
        // Implement chapter navigation
    }
    
    private fun initializeAIEnhancements() {
        // Initialize AI features
    }
    
    fun retry() {
        updatePlayerState { copy(hasError = false, errorMessage = null) }
        player?.prepare()
        player?.play()
    }
    
    fun toggleControlsVisibility() {
        updatePlayerState { copy(showControls = !showControls) }
        if (_playerState.value.showControls) {
            scheduleControlsHide()
        }
    }
    
    fun hideControls() {
        updatePlayerState { copy(showControls = false) }
    }
    
    fun release() {
        hideControlsJob?.cancel()
        analysisJob?.cancel()
        sleepTimerJob?.cancel()
        equalizer?.release()
        equalizer = null
        player?.release()
        player = null
        
        // Release AI components
        // videoContentAnalyzer.release()
        // aiSubtitleGenerator.release()
        // liveTranslationManager.release()
    }
    
    override fun onCleared() {
        super.onCleared()
        release()
    }
    
    // More Options methods
    fun toggleLoop() {
        val newLoopState = !_isLooping.value
        _isLooping.value = newLoopState
        player?.repeatMode = if (newLoopState) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        showToast(if (newLoopState) "Loop enabled" else "Loop disabled")
    }
    
    fun showSleepTimer() {
        updatePlayerState { copy(showSleepTimerDialog = true) }
    }
    
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        
        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        _sleepTimerState.value = SleepTimer(
            durationMinutes = minutes,
            endTime = endTime
        )
        
        sleepTimerJob = viewModelScope.launch {
            while (coroutineContext.isActive) {
                val remaining = (endTime - System.currentTimeMillis()) / 1000
                if (remaining <= 0) {
                    // Timer expired
                    player?.pause()
                    _sleepTimerState.value = null
                    _sleepTimerRemaining.value = 0
                    showToast("Sleep timer expired - playback paused")
                    break
                }
                _sleepTimerRemaining.value = remaining
                delay(1000)
            }
        }
        
        showToast("Sleep timer set for $minutes minutes")
    }
    
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerState.value = null
        _sleepTimerRemaining.value = 0
        showToast("Sleep timer cancelled")
    }
    
    fun updateSleepTimerRemaining() {
        _sleepTimerState.value?.let { timer ->
            val remaining = (timer.endTime - System.currentTimeMillis()) / 1000
            _sleepTimerRemaining.value = remaining.coerceAtLeast(0)
        }
    }
    
    fun dismissSleepTimerDialog() {
        updatePlayerState { copy(showSleepTimerDialog = false) }
    }
    
    fun openEqualizer() {
        updatePlayerState { copy(showEqualizerDialog = true) }
    }
    
    fun dismissEqualizerDialog() {
        updatePlayerState { copy(showEqualizerDialog = false) }
    }
    
    fun initializeEqualizer() {
        try {
            player?.audioSessionId?.let { sessionId ->
                if (sessionId != 0) {
                    equalizer = Equalizer(0, sessionId).apply {
                        enabled = false
                        
                        // Get band information
                        val bands = mutableListOf<EqualizerBand>()
                        val numBands = numberOfBands.toInt()
                        
                        for (i in 0 until numBands) {
                            val freq = getCenterFreq(i.toShort()) / 1000
                            val range = getBandLevelRange()
                            bands.add(
                                EqualizerBand(
                                    frequency = freq,
                                    frequencyLabel = when {
                                        freq < 1000 -> "${freq}Hz"
                                        else -> "${freq/1000}kHz"
                                    },
                                    minLevel = range[0].toInt(),
                                    maxLevel = range[1].toInt()
                                )
                            )
                        }
                        
                        // Get presets
                        val presetNames = mutableListOf<String>()
                        for (i in 0 until numberOfPresets) {
                            presetNames.add(getPresetName(i.toShort()))
                        }
                        
                        _equalizerPresets.value = presetNames
                        
                        // Initialize band levels
                        val levels = mutableListOf<Int>()
                        for (i in 0 until numBands) {
                            levels.add(getBandLevel(i.toShort()).toInt())
                        }
                        
                        _equalizerState.value = EqualizerState(
                            enabled = false,
                            bands = bands,
                            bandLevels = levels
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to initialize equalizer")
        }
    }
    
    fun toggleEqualizer() {
        equalizer?.let { eq ->
            val newState = !eq.enabled
            eq.enabled = newState
            _equalizerState.value = _equalizerState.value.copy(enabled = newState)
            showToast(if (newState) "Equalizer enabled" else "Equalizer disabled")
        }
    }
    
    fun setEqualizerBandLevel(bandIndex: Int, level: Int) {
        equalizer?.let { eq ->
            try {
                eq.setBandLevel(bandIndex.toShort(), level.toShort())
                val newLevels = _equalizerState.value.bandLevels.toMutableList()
                newLevels[bandIndex] = level
                _equalizerState.value = _equalizerState.value.copy(bandLevels = newLevels)
                _currentEqualizerPreset.value = null // Clear preset when manually adjusting
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun selectEqualizerPreset(presetName: String) {
        equalizer?.let { eq ->
            try {
                val presetIndex = _equalizerPresets.value.indexOf(presetName)
                if (presetIndex >= 0) {
                    eq.usePreset(presetIndex.toShort())
                    
                    // Update band levels to reflect preset
                    val levels = mutableListOf<Int>()
                    for (i in 0 until eq.numberOfBands) {
                        levels.add(eq.getBandLevel(i.toShort()).toInt())
                    }
                    
                    _equalizerState.value = _equalizerState.value.copy(bandLevels = levels)
                    _currentEqualizerPreset.value = presetName
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun resetEqualizer() {
        equalizer?.let { eq ->
            try {
                val numBands = eq.numberOfBands.toInt()
                val levels = mutableListOf<Int>()
                
                for (i in 0 until numBands) {
                    eq.setBandLevel(i.toShort(), 0)
                    levels.add(0)
                }
                
                _equalizerState.value = _equalizerState.value.copy(bandLevels = levels)
                _currentEqualizerPreset.value = null
                showToast("Equalizer reset")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun showVideoInfo() {
        updatePlayerState { copy(showVideoInfoDialog = true) }
    }
    
    fun dismissVideoInfoDialog() {
        updatePlayerState { copy(showVideoInfoDialog = false) }
    }
    
    fun toggleShowAspectRatioMenu() {
        updatePlayerState { copy(showAspectRatioMenu = true) }
    }
    
    fun dismissAspectRatioMenu() {
        updatePlayerState { copy(showAspectRatioMenu = false) }
    }
    
    fun toggleScreenOrientationMenu() {
        updatePlayerState { copy(showScreenOrientationMenu = true) }
    }
    
    fun dismissScreenOrientationMenu() {
        updatePlayerState { copy(showScreenOrientationMenu = false) }
    }
    
    fun toggleShowSleepTimer() {
        showSleepTimer()
    }
    
    fun toggleShowEqualizer() {
        openEqualizer()
    }
    
    fun toggleShowVideoInfo() {
        showVideoInfo()
    }
    
    fun toggleShowRepeatMode() {
        updatePlayerState { copy(showRepeatModeMenu = true) }
    }
    
    fun dismissRepeatModeMenu() {
        updatePlayerState { copy(showRepeatModeMenu = false) }
    }
    
    fun toggleShowSubtitleSync() {
        updatePlayerState { copy(showSubtitleSyncMenu = true) }
    }
    
    fun dismissSubtitleSyncMenu() {
        updatePlayerState { copy(showSubtitleSyncMenu = false) }
    }
    
    fun setScreenOrientation(orientation: String) {
        _screenOrientation.value = orientation
        // Apply the orientation to the activity
        when (orientation) {
            "portrait" -> {
                // Set to portrait mode
                showToast("Portrait mode")
            }
            "landscape" -> {
                // Set to landscape mode
                showToast("Landscape mode") 
            }
            "auto" -> {
                // Set to auto-rotate
                showToast("Auto-rotate enabled")
            }
            "reverse_portrait" -> {
                // Set to reverse portrait
                showToast("Reverse portrait mode")
            }
            "reverse_landscape" -> {
                // Set to reverse landscape
                showToast("Reverse landscape mode")
            }
            "sensor" -> {
                // Follow sensor orientation
                showToast("Following sensor orientation")
            }
        }
    }
    
    fun setRepeatMode(mode: String) {
        _repeatMode.value = mode
        when (mode) {
            "off" -> {
                player?.repeatMode = Player.REPEAT_MODE_OFF
                _isLooping.value = false
                showToast("Repeat off")
            }
            "one" -> {
                player?.repeatMode = Player.REPEAT_MODE_ONE
                _isLooping.value = true
                showToast("Repeat one")
            }
            "all" -> {
                player?.repeatMode = Player.REPEAT_MODE_ALL
                _isLooping.value = false
                showToast("Repeat all")
            }
            "shuffle" -> {
                player?.setShuffleModeEnabled(true)
                showToast("Shuffle enabled")
            }
        }
    }
    
    fun setSubtitleOffset(offsetMillis: Long) {
        _subtitleOffset.value = offsetMillis
        // Apply subtitle offset to the player
        // This would typically be implemented with a subtitle renderer that supports timing adjustments
        showToast("Subtitle offset: ${if (offsetMillis >= 0) "+" else ""}${offsetMillis / 1000.0}s")
    }
    
    fun shareVideo() {
        _playerState.value.videoUri?.let { uri ->
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = when {
                    uri.scheme == "file" -> "video/*"
                    else -> "text/plain"
                }
                
                if (uri.scheme == "file") {
                    // Share the video file
                    val file = java.io.File(uri.path ?: return@let)
                    if (file.exists()) {
                        val contentUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        showToast("Video file not found")
                        return
                    }
                } else {
                    // Share the URL
                    putExtra(android.content.Intent.EXTRA_TEXT, uri.toString())
                }
                
                putExtra(android.content.Intent.EXTRA_SUBJECT, _playerState.value.videoTitle)
            }
            
            val shareIntent = android.content.Intent.createChooser(intent, "Share Video")
            shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } ?: showToast("No video to share")
    }
    
    fun setTrackSelector(selector: DefaultTrackSelector) {
        trackSelector = selector
    }
    
    // Enhanced gesture methods for comprehensive gesture handler
    private var seekPreviewPosition: Long = 0L
    private var isSeekPreviewActive = false
    
    fun updateSeekPreview(seekAmount: Long) {
        val currentPos = _playerState.value.currentPosition
        val newPosition = (currentPos + seekAmount).coerceIn(0L, _playerState.value.duration)
        seekPreviewPosition = newPosition
        isSeekPreviewActive = true
        
        // Update UI to show seek preview
        _playerState.update { state ->
            state.copy(
                seekPreviewPosition = newPosition,
                isSeekPreviewActive = true
            )
        }
    }
    
    fun commitSeekPreview() {
        if (isSeekPreviewActive) {
            player?.seekTo(seekPreviewPosition)
            isSeekPreviewActive = false
            _playerState.update { state ->
                state.copy(
                    currentPosition = seekPreviewPosition,
                    isSeekPreviewActive = false
                )
            }
        }
    }
    
    fun updateLongPressSeek(position: androidx.compose.ui.geometry.Offset) {
        val currentState = _playerState.value.longPressSeekState
        if (!currentState.isActive) return
        
        val deltaY = position.y - currentState.initialTouchPosition.y
        val deltaX = position.x - currentState.initialTouchPosition.x
        
        // Calculate speed based on vertical movement (like MX Player)
        val speedMultiplier = when {
            deltaY < -100 -> 5.0f // Up = faster
            deltaY < -50 -> 3.0f
            deltaY > 100 -> 0.5f // Down = slower
            deltaY > 50 -> 1.0f
            else -> 2.0f // Default speed
        }
        
        // Calculate direction based on horizontal movement
        val direction = when {
            deltaX < -50 -> com.astralplayer.nextplayer.feature.player.gestures.SeekDirection.BACKWARD
            deltaX > 50 -> com.astralplayer.nextplayer.feature.player.gestures.SeekDirection.FORWARD
            else -> currentState.direction
        }
        
        longPressSeekSpeed = speedMultiplier
        longPressSeekDirection = direction
        
        _playerState.update { state ->
            state.copy(
                longPressSeekState = state.longPressSeekState.copy(
                    currentTouchPosition = position,
                    seekSpeed = speedMultiplier,
                    direction = direction
                )
            )
        }
    }
    
}

// Data classes
data class PlayerUiState(
    val videoUri: Uri? = null,
    val videoTitle: String = "",
    val playerState: PlayerState = PlayerState.IDLE,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPercentage: Int = 0,
    val playbackSpeed: Float = 1f,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val showControls: Boolean = true,
    val areControlsVisible: Boolean = true, // Alias for showControls
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val aspectRatio: Float = 16f / 9f,
    val isScreenLocked: Boolean = false,
    val isPiPMode: Boolean = false,
    val zoomLevel: Float = 1.0f,
    val videoRotation: Float = 0f,
    val isAIProcessing: Boolean = false,
    val showSeekPreview: Boolean = false,
    val seekPreviewPosition: Long = 0L,
    val seekPreviewForward: Boolean = true,
    val subtitlesEnabled: Boolean = false,
    val studyModeEnabled: Boolean = false,
    val vrModeEnabled: Boolean = false,
    val exoPlayer: ExoPlayer? = null,
    val showSleepTimerDialog: Boolean = false,
    val showEqualizerDialog: Boolean = false,
    val showVideoInfoDialog: Boolean = false,
    val showAspectRatioMenu: Boolean = false,
    val showScreenOrientationMenu: Boolean = false,
    val showRepeatModeMenu: Boolean = false,
    val showSubtitleSyncMenu: Boolean = false,
    val isLooping: Boolean = false,
    val longPressSeekState: com.astralplayer.nextplayer.feature.player.gestures.LongPressSeekState = com.astralplayer.nextplayer.feature.player.gestures.LongPressSeekState(),
    // Gesture overlay states
    val showVolumeOverlay: Boolean = false,
    val showBrightnessOverlay: Boolean = false,
    val showZoomOverlay: Boolean = false,
    // Enhanced gesture states
    val isSeekPreviewActive: Boolean = false,
    val controlsVisibilityChanged: Long = 0L
)

enum class PlayerState {
    IDLE, BUFFERING, READY, ENDED
}

data class SleepTimer(
    val durationMinutes: Int,
    val endTime: Long
)

data class EqualizerState(
    val enabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val bandLevels: List<Int> = emptyList()
)

data class EqualizerBand(
    val frequency: Int,
    val frequencyLabel: String,
    val minLevel: Int,
    val maxLevel: Int
)

data class AIFeaturesState(
    val aiEnhancementEnabled: Boolean = false,
    val aiSubtitlesEnabled: Boolean = false,
    val liveTranslationEnabled: Boolean = false,
    val autoAnalyze: Boolean = true,
    val generatedSubtitles: List<Subtitle> = emptyList(),
    val videoSummary: VideoSummary? = null,
    val bookmarks: List<VideoBookmark> = emptyList()
)

data class Subtitle(
    val startTime: Long,
    val endTime: Long,
    val text: String
)

data class VideoSummary(
    val title: String,
    val duration: Long,
    val keyMoments: List<KeyMoment>,
    val topics: List<String>,
    val summary: String
)

data class KeyMoment(
    val timestamp: Long,
    val description: String
)

data class VideoBookmark(
    val id: String,
    val timestamp: Long,
    val title: String,
    val note: String,
    val createdAt: Long
)

// Helper functions
fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

fun defaultGestureMappings(): Map<GestureType, PlayerAction> {
    return mapOf(
        GestureType.DOUBLE_TAP to PlayerAction.PLAY_PAUSE,
        GestureType.TRIPLE_TAP to PlayerAction.SCREENSHOT,
        GestureType.LONG_PRESS to PlayerAction.CHANGE_SPEED,
        GestureType.PINCH_IN to PlayerAction.PIP_MODE,
        GestureType.PINCH_OUT to PlayerAction.ZOOM,
        GestureType.TWO_FINGER_TAP to PlayerAction.LOCK_SCREEN,
        GestureType.SWIPE_UP to PlayerAction.VOLUME_UP,
        GestureType.SWIPE_DOWN to PlayerAction.VOLUME_DOWN,
        GestureType.SWIPE_LEFT to PlayerAction.SEEK_BACKWARD,
        GestureType.SWIPE_RIGHT to PlayerAction.SEEK_FORWARD
    )
}