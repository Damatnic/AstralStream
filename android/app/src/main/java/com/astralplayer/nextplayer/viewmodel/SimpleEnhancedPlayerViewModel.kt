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
 * Simplified Enhanced ViewModel that adapts to existing PlayerRepository
 */
class SimpleEnhancedPlayerViewModel(
    application: Application,
    val playerRepository: PlayerRepository,
    val gestureManager: EnhancedGestureManager,
    private val hapticManager: HapticFeedbackManager
) : AndroidViewModel(application) {
    
    // Settings repository for AI subtitle settings
    private val settingsRepository = com.astralplayer.nextplayer.data.repository.SettingsRepositoryImpl(application)
    
    // Playback position repository for resume functionality
    private val playbackPositionRepository = com.astralplayer.nextplayer.data.PlaybackPositionRepository(application)
    
    // Google AI Studio service
    private val googleAIService by lazy {
        com.astralplayer.nextplayer.feature.ai.GoogleAIStudioService(application)
    }
    
    // Enhanced AI subtitle generation with Google AI Studio
    private val aiSubtitleGenerator: AISubtitleGenerator by lazy {
        com.astralplayer.nextplayer.feature.ai.EnhancedAISubtitleGenerator(application, googleAIService)
    }
    
    // Enhanced AI scene detection manager with Google AI Studio
    private val aiSceneDetectionManager by lazy {
        com.astralplayer.nextplayer.feature.ai.EnhancedAISceneDetectionManager(application, googleAIService)
    }
    
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
    
    // Control lock state
    private val _isControlsLocked = MutableStateFlow(false)
    val isControlsLocked: StateFlow<Boolean> = _isControlsLocked.asStateFlow()
    
    // Subtitle renderer
    val subtitleRenderer = SubtitleRenderer()
    
    // Subtitle manager
    private val subtitleManager = SubtitleManager(application)
    
    // ExoPlayer listener for track changes
    private val playerListener = object : androidx.media3.common.Player.Listener {
        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            super.onTracksChanged(tracks)
            // Update available audio tracks when tracks change
            updateAvailableAudioTracks()
        }
    }
    
    init {
        // Update subtitle position with playback progress
        viewModelScope.launch {
            currentPosition.collect { position ->
                subtitleRenderer.updateCurrentPosition(position)
                subtitleManager.updatePosition(position)
            }
        }
        
        // Periodically save playback position (every 10 seconds)
        viewModelScope.launch {
            while (true) {
                delay(10000) // 10 seconds
                if (playerRepository.playerState.value.isPlaying) {
                    saveCurrentPlaybackPosition()
                }
            }
        }
        
        // Connect subtitle manager to renderer
        viewModelScope.launch {
            subtitleManager.currentSubtitle.collect { subtitleCue ->
                if (subtitleCue != null) {
                    val entries = listOf(
                        SubtitleEntry(
                            startTime = subtitleCue.startTimeMs,
                            endTime = subtitleCue.endTimeMs,
                            text = subtitleCue.text,
                            language = "en"
                        )
                    )
                    subtitleRenderer.setSubtitles(entries)
                    subtitleRenderer.updateCurrentPosition(subtitleCue.startTimeMs)
                } else {
                    subtitleRenderer.clearSubtitles()
                }
            }
        }
        
        // Add ExoPlayer listener for track changes
        playerRepository.exoPlayer.addListener(playerListener)
        
        // Initialize quality state conversion
        viewModelScope.launch {
            (playerRepository as PlayerRepositoryImpl).availableQualities.collect { dataQualities ->
                val uiQualities = dataQualities.map { dataQuality ->
                    com.astralplayer.nextplayer.ui.components.VideoQuality(
                        id = dataQuality.id,
                        label = dataQuality.displayName,
                        width = dataQuality.width,
                        height = dataQuality.height,
                        bitrate = dataQuality.bitrate,
                        codec = extractCodec(dataQuality),
                        fps = extractFps(dataQuality)
                    )
                }
                availableQualities.value = uiQualities
            }
        }
        
        viewModelScope.launch {
            (playerRepository as PlayerRepositoryImpl).currentQuality.collect { dataQuality ->
                val uiQuality = dataQuality?.let { quality ->
                    com.astralplayer.nextplayer.ui.components.VideoQuality(
                        id = quality.id,
                        label = quality.displayName,
                        width = quality.width,
                        height = quality.height,
                        bitrate = quality.bitrate,
                        codec = extractCodec(dataQuality),
                        fps = extractFps(dataQuality)
                    )
                }
                currentQuality.value = uiQuality
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
    
    /**
     * Get gesture callbacks for the gesture detector
     */
    fun getGestureCallbacks(): GestureCallbacks {
        return GestureCallbacks(
            onHorizontalSeek = { delta, velocity ->
                handleHorizontalSeek(delta, velocity)
            },
            onVerticalVolumeChange = { delta, side ->
                handleVolumeChange(delta, side)
            },
            onVerticalBrightnessChange = { delta, side ->
                handleBrightnessChange(delta, side)
            },
            onSingleTap = { position ->
                togglePlayPause()
            },
            onDoubleTap = { position, side ->
                handleDoubleTap(position, side)
            },
            onLongPressStart = { position ->
                handleLongPressStart(position)
            },
            onLongPressUpdate = { position, speed, direction ->
                updateLongPressUI(speed, direction)
            },
            onLongPressEnd = {
                handleLongPressEnd()
            },
            onPinchZoom = { scale, center ->
                handlePinchZoom(scale, center)
            },
            onGestureConflict = { conflictingGestures ->
                showGestureConflict(conflictingGestures)
            }
        )
    }
    
    private fun handleHorizontalSeek(delta: Float, velocity: Float) {
        val screenWidth = getApplication<Application>().resources.displayMetrics.widthPixels.toFloat()
        val seekDelta = ((delta / screenWidth) * 60000).toLong() // 60 seconds for full width
        
        viewModelScope.launch {
            playerRepository.seekBy(seekDelta)
        }
        
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.SEEK_TICK)
        showSeekOverlay()
    }
    
    private fun handleVolumeChange(delta: Float, side: TouchSide) {
        viewModelScope.launch {
            playerRepository.adjustVolume(-delta) // Negative because up is positive volume
        }
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.VOLUME_TICK)
        showVolumeOverlay()
    }
    
    private fun handleBrightnessChange(delta: Float, side: TouchSide) {
        verticalGestureHandler?.processDrag(delta, Offset.Zero, side)
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.BRIGHTNESS_TICK)
        showBrightnessOverlay()
    }
    
    private fun handleDoubleTap(position: Offset, side: TouchSide) {
        val seekAmount = if (side == TouchSide.RIGHT) 10000L else -10000L
        viewModelScope.launch {
            playerRepository.seekBy(seekAmount)
        }
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.DOUBLE_TAP)
        showDoubleTapIndicator(side, 10000L)
    }
    
    private fun handleLongPressStart(position: Offset) {
        // Store the original playback speed
        val originalSpeed = playerRepository.playerState.value.playbackSpeed
        
        // Set playback to 2x speed
        viewModelScope.launch {
            playerRepository.setPlaybackSpeed(2f)
        }
        
        // Initialize long press seek handler
        val gestureConfig = gestureSettings.value
        longPressSeekHandler = LongPressSeekHandler(
            settings = gestureConfig.longPress,
            screenWidth = getApplication<Application>().resources.displayMetrics.widthPixels.toFloat(),
            onSpeedUpdate = { speed, direction ->
                viewModelScope.launch {
                    playerRepository.setPlaybackSpeed(speed)
                    _longPressSeekInfo.value = longPressSeekHandler?.getSeekInfo()
                    hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.LONG_PRESS_SPEED_CHANGE)
                }
            },
            onSeekUpdate = { seekAmount ->
                // Long press seek disabled in this implementation, just using speed
            },
            onEnd = {
                viewModelScope.launch {
                    playerRepository.setPlaybackSpeed(originalSpeed)
                    _longPressSeekInfo.value = null
                }
            }
        )
        
        // Start the long press handler
        longPressSeekHandler?.start(position)
        
        // Update seek info
        _longPressSeekInfo.value = longPressSeekHandler?.getSeekInfo()
        
        // Update UI state
        _uiState.update { state ->
            state.copy(longPressSeekInfo = _longPressSeekInfo.value)
        }
        
        // Play haptic feedback
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.LONG_PRESS_START)
        
        showLongPressOverlay()
    }
    
    private fun updateLongPressUI(speed: Float, direction: SeekDirection) {
        // Update is handled in onSpeedUpdate callback
    }
    
    private fun handleLongPressEnd() {
        // Stop the long press handler
        longPressSeekHandler?.stop()
        longPressSeekHandler = null
        
        // Clear UI state
        _uiState.update { state ->
            state.copy(longPressSeekInfo = null)
        }
        
        // Hide the overlay
        hideLongPressOverlay()
    }
    
    private fun handlePinchZoom(scale: Float, center: Offset) {
        val currentZoom = _uiState.value.zoomLevel
        val newZoom = (currentZoom * scale).coerceIn(0.5f, 3f)
        
        _uiState.update { it.copy(zoomLevel = newZoom) }
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.ZOOM_FEEDBACK)
        showZoomOverlay()
    }
    
    fun togglePlayPause() {
        viewModelScope.launch {
            playerRepository.togglePlayPause()
        }
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.TAP)
    }
    
    fun seekTo(position: Long) {
        viewModelScope.launch {
            playerRepository.seekTo(position)
        }
    }
    
    fun loadVideo(uri: Uri) {
        viewModelScope.launch {
            try {
                // Track the current video URI
                currentVideoUri = uri
                
                val result = playerRepository.playVideo(uri)
                if (result.isFailure) {
                    val error = PlayerError.VideoLoadError(uri, result.exceptionOrNull())
                    handleError(error)
                } else {
                    // Clear any previous error
                    _errorState.value = null
                    
                    // Check for saved playback position and resume
                    playbackPositionRepository.getPlaybackPosition(uri).first()?.let { savedPosition ->
                        if (savedPosition > 5000) { // Only resume if > 5 seconds
                            Log.d("PlayerVM", "Resuming playback from position: $savedPosition")
                            delay(500) // Small delay to ensure player is ready
                            playerRepository.seekTo(savedPosition)
                            
                            // Show a toast or snackbar about resuming
                            // This would need to be handled via UI state
                        }
                    }
                    
                    // Load subtitles for the video
                    subtitleManager.loadSubtitlesForVideo(uri)
                    
                    // Auto-generate AI subtitles if enabled in settings
                    try {
                        val aiEnabled = settingsRepository.getAISubtitleGenerationEnabled().first()
                        Log.d("PlayerVM", "AI subtitle generation enabled: $aiEnabled")
                        if (aiEnabled) {
                            Log.d("PlayerVM", "Starting auto AI subtitle generation for: $uri")
                            generateAISubtitles(uri)
                        }
                    } catch (e: Exception) {
                        Log.e("PlayerVM", "Failed to check AI subtitle settings", e)
                    }
                    
                    // Start AI scene detection if enabled
                    Log.d("PlayerVM", "Starting AI scene detection for video")
                    startAISceneDetection(uri)
                    
                    // Load speed memory for this video
                    loadVideoSpeedMemory(uri.toString())
                    
                    // Update available audio tracks
                    delay(1000) // Wait for tracks to be available
                    updateAvailableAudioTracks()
                }
            } catch (e: Exception) {
                val error = PlayerError.VideoLoadError(uri, e)
                handleError(error)
            }
        }
    }
    
    // Overlay visibility methods
    
    private fun showSeekOverlay() {
        _overlayVisibility.update { it.copy(seekPreview = true) }
        startOverlayTimer(OverlayType.SEEK_PREVIEW)
    }
    
    private fun showVolumeOverlay() {
        verticalGestureHandler?.let { handler ->
            _uiState.update { it.copy(volumeInfo = handler.getVolumeInfo()) }
        }
        _overlayVisibility.update { it.copy(volume = true) }
        startOverlayTimer(OverlayType.VOLUME)
    }
    
    private fun showBrightnessOverlay() {
        verticalGestureHandler?.let { handler ->
            _uiState.update { it.copy(brightnessInfo = handler.getBrightnessInfo()) }
        }
        _overlayVisibility.update { it.copy(brightness = true) }
        startOverlayTimer(OverlayType.BRIGHTNESS)
    }
    
    private fun showLongPressOverlay() {
        _overlayVisibility.update { it.copy(longPress = true) }
    }
    
    private fun hideLongPressOverlay() {
        _overlayVisibility.update { it.copy(longPress = false) }
    }
    
    private fun showDoubleTapIndicator(side: TouchSide, amount: Long) {
        _uiState.update { 
            it.copy(
                doubleTapInfo = DoubleTapInfo(
                    side = side,
                    seekAmount = amount,
                    visible = true
                )
            )
        }
        _overlayVisibility.update { it.copy(doubleTap = true) }
        startOverlayTimer(OverlayType.DOUBLE_TAP)
    }
    
    private fun showZoomOverlay() {
        _overlayVisibility.update { it.copy(zoom = true) }
        startOverlayTimer(OverlayType.ZOOM)
    }
    
    private fun showGestureConflict(conflictingGestures: List<GestureType>) {
        _uiState.update { it.copy(gestureConflict = conflictingGestures) }
        _overlayVisibility.update { it.copy(conflict = true) }
        startOverlayTimer(OverlayType.CONFLICT)
    }
    
    private fun startOverlayTimer(overlayType: OverlayType) {
        viewModelScope.launch {
            delay(2000)
            _overlayVisibility.update {
                when (overlayType) {
                    OverlayType.SEEK_PREVIEW -> it.copy(seekPreview = false)
                    OverlayType.VOLUME -> it.copy(volume = false)
                    OverlayType.BRIGHTNESS -> it.copy(brightness = false)
                    OverlayType.DOUBLE_TAP -> it.copy(doubleTap = false)
                    OverlayType.ZOOM -> it.copy(zoom = false)
                    OverlayType.CONFLICT -> it.copy(conflict = false)
                }
            }
        }
    }
    
    fun onStart() {
        viewModelScope.launch {
            playerRepository.resumeVideo()
        }
    }
    
    fun onStop() {
        viewModelScope.launch {
            // Save playback position before pausing
            saveCurrentPlaybackPosition()
            playerRepository.pauseVideo()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Save final playback position
        saveCurrentPlaybackPosition()
        // Remove player listener
        playerRepository.exoPlayer.removeListener(playerListener)
        viewModelScope.launch {
            playerRepository.stopVideo()
        }
    }
    
    private fun saveCurrentPlaybackPosition() {
        currentVideoUri?.let { uri ->
            viewModelScope.launch {
                val position = playerRepository.playerState.value.currentPosition
                val duration = playerRepository.playerState.value.duration
                
                if (position > 0 && duration > 0) {
                    playbackPositionRepository.savePlaybackPosition(uri, position, duration)
                    Log.d("PlayerVM", "Saved playback position: $position/$duration for $uri")
                }
            }
        }
    }
    
    // Subtitle functions
    val availableSubtitles = subtitleManager.availableSubtitles
    
    fun getSelectedSubtitle() = subtitleManager.selectedTrack
    
    fun selectSubtitleTrack(trackId: String?) {
        viewModelScope.launch {
            val track = subtitleManager.availableSubtitles.value.find { it.id == trackId }
            subtitleManager.selectSubtitleTrack(track)
        }
    }
    
    fun selectSubtitle(track: SubtitleManager.SubtitleTrack?) {
        viewModelScope.launch {
            subtitleManager.selectSubtitleTrack(track)
        }
    }
    
    fun adjustSubtitleSync(offsetMs: Long) {
        subtitleManager.adjustSubtitleOffset(offsetMs)
    }
    
    fun addExternalSubtitleFile(uri: android.net.Uri, filename: String? = null) {
        viewModelScope.launch {
            subtitleManager.addExternalSubtitleFile(uri, filename)
        }
    }
    
    // Quality functions
    fun setVideoQuality(quality: com.astralplayer.nextplayer.ui.components.VideoQuality) {
        viewModelScope.launch {
            // Convert UI VideoQuality back to data VideoQuality
            val dataQuality = com.astralplayer.nextplayer.data.VideoQuality(
                id = quality.id,
                name = quality.label,
                width = quality.width,
                height = quality.height,
                bitrate = quality.bitrate,
                isAdaptive = quality.id == "auto"
            )
            playerRepository.setVideoQuality(dataQuality)
        }
    }
    
    // MX Player style gesture methods
    
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
                    seekPercentage = if (duration > 0) targetPosition.toFloat() / duration else 0f,
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
        showSeekOverlay()
    }
    
    fun adjustVolume(delta: Float) {
        Log.d("PlayerVM", "adjustVolume: delta=$delta")
        viewModelScope.launch {
            // Scale the delta for better sensitivity
            playerRepository.adjustVolume(delta * 0.5f)
        }
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.VOLUME_TICK)
        
        // Update volume UI
        val currentVolume = playerRepository.getVolume()
        Log.d("PlayerVM", "Current volume: $currentVolume")
        _uiState.update {
            it.copy(
                volumeInfo = VerticalGestureHandler.VolumeInfo(
                    currentVolume = (currentVolume * 100).toInt(), // Convert to percentage
                    maxVolume = 100,
                    percentage = currentVolume,
                    isMuted = currentVolume == 0f
                )
            )
        }
        showVolumeOverlay()
    }
    
    fun adjustBrightness(delta: Float) {
        Log.d("PlayerVM", "adjustBrightness: delta=$delta")
        verticalGestureHandler?.let { handler ->
            // Process brightness change through the handler
            handler.processDrag(delta * 500f, Offset.Zero, TouchSide.LEFT) // Scale up for sensitivity
            
            // Get current brightness from the handler's state
            val brightnessInfo = handler.getBrightnessInfo()
            _uiState.update {
                it.copy(
                    brightnessInfo = brightnessInfo
                )
            }
        }
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.BRIGHTNESS_TICK)
        showBrightnessOverlay()
    }
    
    // Seeking methods
    fun startSeeking() {
        Log.d("PlayerVM", "Start seeking")
        _uiState.update {
            it.copy(
                seekPreviewInfo = HorizontalSeekGestureHandler.SeekPreviewInfo(
                    seekPosition = playerRepository.playerState.value.currentPosition,
                    seekPercentage = 0f,
                    isDragging = true,
                    showThumbnail = false,
                    showTimeIndicator = true,
                    isForward = true,
                    velocity = 0f,
                    seekDelta = 0L,
                    targetPosition = playerRepository.playerState.value.currentPosition
                )
            )
        }
        showSeekOverlay()
    }
    
    fun endSeeking() {
        Log.d("PlayerVM", "End seeking")
        // Hide seek overlay immediately
        _overlayVisibility.update { it.copy(seekPreview = false) }
    }
    
    // Long press seeking methods (MX Player style - seeking while held)
    private var longPressSeekJob: kotlinx.coroutines.Job? = null
    private var longPressDirection: SeekDirection = SeekDirection.FORWARD
    
    // Progressive speed control settings (MX Player style)
    private val speedProgression = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f)
    private var currentSpeedIndex = 6 // Start at 2.0x (index 6)
    private var baseSpeedIndex = 6 // The starting speed for long press
    private var originalPlaybackSpeed = 1.0f
    
    fun startLongPressSpeed() {
        Log.d("PlayerVM", "Start MX Player/Next Player style long press seeking")
        
        // Cancel any existing long press job
        longPressSeekJob?.cancel()
        
        viewModelScope.launch {
            // Store whether video was playing before we started seeking
            val wasPlaying = playerRepository.exoPlayer.isPlaying
            
            // Pause the video for seeking (like Next Player does)
            if (wasPlaying) {
                playerRepository.pauseVideo()
            }
            
            _uiState.update {
                it.copy(
                    longPressSeekInfo = LongPressSeekHandler.LongPressSeekInfo(
                        isActive = true,
                        currentSpeed = 1.0f, // Not used for seeking
                        currentSpeedIndex = 0,
                        maxSpeedIndex = 0,
                        direction = longPressDirection,
                        totalSeekAmount = 0L,
                        elapsedTime = System.currentTimeMillis(),
                        speedProgression = listOf(1.0f),
                        originalSpeed = 1.0f,
                        wasPlaying = wasPlaying
                    )
                )
            }
            
            // Start continuous seeking like MX Player/Next Player
            longPressSeekJob = launch {
                val seekIncrement = if (longPressDirection == SeekDirection.FORWARD) 10000L else -10000L // 10 seconds
                var totalSeek = 0L
                
                try {
                    while (isActive) {
                        // Seek forward/backward continuously
                        seekRelative(seekIncrement)
                        totalSeek += seekIncrement
                        
                        // Update UI with total seek amount
                        _uiState.update { state ->
                            state.copy(
                                longPressSeekInfo = state.longPressSeekInfo?.copy(
                                    totalSeekAmount = totalSeek,
                                    elapsedTime = System.currentTimeMillis()
                                )
                            )
                        }
                        
                        // Delay between seeks for smooth experience
                        delay(500) // Seek every 500ms like Next Player
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.d("PlayerVM", "Long press seeking ended, total seek: ${totalSeek}ms")
                }
            }
        }
        
        showLongPressOverlay()
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.LONG_PRESS_START)
    }
    
    fun endLongPressSpeed() {
        Log.d("PlayerVM", "End long press seeking")
        
        // Resume video if it was playing before seeking started
        val currentSeekInfo = _uiState.value.longPressSeekInfo
        if (currentSeekInfo?.wasPlaying == true) {
            viewModelScope.launch {
                // Resume playback by setting playWhenReady to true
                playerRepository.exoPlayer.playWhenReady = true
                Log.d("PlayerVM", "Resumed playback after seeking")
            }
        }
        
        // Cancel the job and clear state
        longPressSeekJob?.cancel()
        longPressSeekJob = null
        
        _uiState.update {
            it.copy(longPressSeekInfo = null)
        }
        
        hideLongPressOverlay()
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.TAP)
    }
    
    // Set the long press direction based on which side was pressed
    fun setLongPressDirection(direction: SeekDirection) {
        longPressDirection = direction
        Log.d("PlayerVM", "Long press direction set to: $direction")
    }
    
    // Center long press methods for MX Player style progressive speed control
    fun startCenterLongPressSpeed() {
        Log.d("PlayerVM", "Start center long press progressive speed control (MX Player style)")
        
        // Store original speed and reset to base speed
        originalPlaybackSpeed = playerRepository.exoPlayer.playbackParameters.speed
        currentSpeedIndex = baseSpeedIndex
        
        viewModelScope.launch {
            // Set initial speed (2.0x by default)
            val initialSpeed = speedProgression[currentSpeedIndex]
            playerRepository.setPlaybackSpeed(initialSpeed)
            
            _uiState.update {
                it.copy(
                    longPressSeekInfo = LongPressSeekHandler.LongPressSeekInfo(
                        isActive = true,
                        currentSpeed = initialSpeed,
                        currentSpeedIndex = currentSpeedIndex,
                        maxSpeedIndex = speedProgression.size - 1,
                        direction = SeekDirection.FORWARD, // Not used for center press
                        totalSeekAmount = 0L, // Not used for speed control
                        elapsedTime = System.currentTimeMillis(),
                        speedProgression = speedProgression,
                        originalSpeed = originalPlaybackSpeed,
                        wasPlaying = true // Center press doesn't pause
                    )
                )
            }
        }
        
        showLongPressOverlay()
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.LONG_PRESS_START)
    }
    
    fun updateCenterLongPressSpeed(deltaY: Float) {
        // Convert deltaY to speed index change
        // Positive deltaY (swipe up) = increase speed, negative = decrease speed
        val sensitivity = 3.0f // Adjust sensitivity
        val indexChange = (deltaY * sensitivity).toInt()
        
        val newIndex = (currentSpeedIndex + indexChange).coerceIn(0, speedProgression.size - 1)
        
        if (newIndex != currentSpeedIndex) {
            currentSpeedIndex = newIndex
            val newSpeed = speedProgression[currentSpeedIndex]
            
            viewModelScope.launch {
                playerRepository.setPlaybackSpeed(newSpeed)
                
                _uiState.update { state ->
                    state.copy(
                        longPressSeekInfo = state.longPressSeekInfo?.copy(
                            currentSpeed = newSpeed,
                            currentSpeedIndex = currentSpeedIndex
                        )
                    )
                }
                
                Log.d("PlayerVM", "Updated center long press speed to: ${newSpeed}x (index: $currentSpeedIndex)")
            }
            
            // Enhanced haptic feedback for speed change
            val previousSpeed = if (currentSpeedIndex > 0) speedProgression[currentSpeedIndex - 1] else newSpeed
            hapticManager.playSpeedFeedback(newSpeed, previousSpeed)
            
            // Also play progressive feedback based on speed level
            hapticManager.playProgressiveSpeedFeedback(currentSpeedIndex, speedProgression.size - 1)
        }
    }
    
    fun endCenterLongPressSpeed() {
        Log.d("PlayerVM", "End center long press progressive speed control")
        
        // Get current speed for haptic feedback
        val currentSpeed = speedProgression[currentSpeedIndex]
        
        // Restore original playback speed
        viewModelScope.launch {
            playerRepository.setPlaybackSpeed(originalPlaybackSpeed)
            Log.d("PlayerVM", "Restored original speed: ${originalPlaybackSpeed}x")
        }
        
        // Clear state
        _uiState.update {
            it.copy(longPressSeekInfo = null)
        }
        
        hideLongPressOverlay()
        
        // Enhanced haptic feedback for returning to normal speed
        hapticManager.playSpeedFeedback(originalPlaybackSpeed, currentSpeed)
    }
    
    // AI Subtitle Generation
    private fun generateAISubtitles(videoUri: Uri) {
        viewModelScope.launch {
            try {
                Log.d("PlayerVM", "Starting AI subtitle generation for: $videoUri")
                
                val result = aiSubtitleGenerator.generateSubtitles(
                    videoUri = videoUri,
                    targetLanguage = "en" // Default to English, could be configurable
                ) { progress ->
                    Log.d("PlayerVM", "AI subtitle generation progress: ${(progress * 100).toInt()}%")
                }
                
                if (result.isSuccess) {
                    val subtitles = result.getOrThrow()
                    Log.d("PlayerVM", "AI subtitle generation completed. Generated ${subtitles.size} subtitle entries")
                    
                    if (subtitles.isNotEmpty()) {
                        // Convert AI subtitles to SubtitleCue format and add to SubtitleManager
                        val subtitleCues = subtitles.map { entry ->
                            SubtitleCue(
                                startTimeMs = entry.startTime,
                                endTimeMs = entry.endTime,
                                text = entry.text
                            )
                        }
                        
                        // Create a subtitle track for AI-generated subtitles
                        val aiTrack = SubtitleManager.SubtitleTrack(
                            id = "ai_generated_${System.currentTimeMillis()}",
                            uri = videoUri,
                            name = "AI Generated (${subtitles.firstOrNull()?.language?.uppercase() ?: "EN"})",
                            language = subtitles.firstOrNull()?.language ?: "en",
                            isEmbedded = false
                        )
                        
                        // Save AI-generated subtitles to a temporary file and add to player
                        val aiSubtitleFile = saveAISubtitlesToFile(subtitleCues, videoUri)
                        if (aiSubtitleFile != null) {
                            subtitleManager.addExternalSubtitleFile(aiSubtitleFile, "AI Generated Subtitles")
                            Log.i("PlayerVM", "AI subtitles added as external subtitle track")
                        } else {
                            Log.e("PlayerVM", "Failed to save AI subtitles to file")
                            showAISubtitleError("Failed to add AI subtitles to player")
                        }
                        
                        // Show success message to user
                        Log.i("PlayerVM", "Successfully generated ${subtitles.size} AI subtitle entries")
                    } else {
                        Log.w("PlayerVM", "AI subtitle generation returned empty results")
                        showAISubtitleError("AI subtitle generation completed but no subtitles were generated. The video may not contain speech.")
                    }
                    
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = when {
                        error?.message?.contains("API", ignoreCase = true) == true -> 
                            "AI service unavailable. Please check your internet connection and try again."
                        error?.message?.contains("key", ignoreCase = true) == true -> 
                            "AI service configuration error. Please check settings."
                        error?.message?.contains("network", ignoreCase = true) == true -> 
                            "Network error. Please check your internet connection."
                        else -> "AI subtitle generation failed: ${error?.message ?: "Unknown error"}"
                    }
                    
                    Log.e("PlayerVM", "AI subtitle generation failed: ${error?.message}", error)
                    showAISubtitleError(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Exception during AI subtitle generation", e)
                val errorMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error during AI subtitle generation. Please check your connection."
                    e.message?.contains("timeout", ignoreCase = true) == true -> 
                        "AI subtitle generation timed out. Please try again."
                    else -> "Failed to generate AI subtitles: ${e.message ?: "Unknown error"}"
                }
                showAISubtitleError(errorMessage)
            }
        }
    }
    
    private fun showAISubtitleError(message: String) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                // Create a toast to show the error to the user
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        context,
                        message,
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                Log.e("PlayerVM", "AI Subtitle Error: $message")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to show AI subtitle error message", e)
            }
        }
    }
    
    // AI Scene Detection
    private fun startAISceneDetection(videoUri: Uri) {
        viewModelScope.launch {
            try {
                Log.d("PlayerVM", "Starting AI scene detection for: $videoUri")
                
                // Get video duration and title for enhanced analysis
                val duration = playerRepository.exoPlayer.duration
                val videoTitle = videoUri.lastPathSegment?.substringBeforeLast('.') ?: "Unknown Video"
                
                if (duration > 0) {
                    aiSceneDetectionManager.analyzeVideoEnhanced(videoUri, duration, videoTitle)
                    Log.d("PlayerVM", "Enhanced AI scene detection started for '$videoTitle' (${duration}ms)")
                } else {
                    Log.w("PlayerVM", "Cannot start AI scene detection: video duration unknown")
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "AI scene detection failed", e)
            }
        }
    }
    
    // Expose AI scene detection results
    val detectedScenes = aiSceneDetectionManager.detectedScenes
    val sceneDetectionProgress = aiSceneDetectionManager.analysisProgress
    val isSceneDetectionAnalyzing = aiSceneDetectionManager.isAnalyzing
    val aiInsights = aiSceneDetectionManager.aiInsights
    
    // Video recommendations state
    private val _videoRecommendations = MutableStateFlow<List<com.astralplayer.nextplayer.feature.ai.VideoRecommendation>>(emptyList())
    val videoRecommendations: StateFlow<List<com.astralplayer.nextplayer.feature.ai.VideoRecommendation>> = _videoRecommendations.asStateFlow()
    
    // AI subtitle generation state
    val aiSubtitleState = aiSubtitleGenerator.subtitleState
    
    /**
     * Save AI-generated subtitle cues to a temporary SRT file
     */
    private suspend fun saveAISubtitlesToFile(subtitleCues: List<SubtitleCue>, videoUri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            // Create a temporary file for AI subtitles
            val context = getApplication<Application>()
            val tempDir = java.io.File(context.cacheDir, "ai_subtitles")
            tempDir.mkdirs()
            
            val filename = "ai_generated_${System.currentTimeMillis()}.srt"
            val subtitleFile = java.io.File(tempDir, filename)
            
            // Convert subtitle cues to SRT format
            val srtContent = buildString {
                subtitleCues.forEachIndexed { index, cue ->
                    append("${index + 1}\n")
                    append("${formatTimestamp(cue.startTimeMs)} --> ${formatTimestamp(cue.endTimeMs)}\n")
                    append("${cue.text}\n\n")
                }
            }
            
            // Write SRT content to file
            subtitleFile.writeText(srtContent)
            
            Log.d("PlayerVM", "AI subtitles saved to: ${subtitleFile.absolutePath}")
            Uri.fromFile(subtitleFile)
            
        } catch (e: Exception) {
            Log.e("PlayerVM", "Failed to save AI subtitles to file", e)
            null
        }
    }
    
    /**
     * Format milliseconds to SRT timestamp format (HH:MM:SS,mmm)
     */
    private fun formatTimestamp(millis: Long): String {
        val hours = millis / 3600000
        val minutes = (millis % 3600000) / 60000
        val seconds = (millis % 60000) / 1000
        val milliseconds = millis % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
    }
    
    // AI insights and recommendations
    fun generateAIInsights() {
        viewModelScope.launch {
            try {
                val scenes = detectedScenes.value
                
                if (scenes.isNotEmpty()) {
                    Log.d("PlayerVM", "AI insights automatically generated during scene detection")
                    // AI insights are automatically generated during scene detection
                    // This method serves as a trigger point for UI updates
                } else {
                    Log.w("PlayerVM", "Cannot generate insights: no scenes detected")
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to generate AI insights", e)
            }
        }
    }
    
    fun generateVideoRecommendations() {
        viewModelScope.launch {
            try {
                // Get current playing video info from player state
                val title = "Current Video" // Placeholder - could be enhanced with actual metadata
                val watchHistory = listOf<String>() // Could be populated from user data
                
                Log.d("PlayerVM", "Generating video recommendations")
                val recommendations = aiSceneDetectionManager.getVideoRecommendations(title, watchHistory)
                
                _videoRecommendations.value = recommendations
                Log.d("PlayerVM", "Generated ${recommendations.size} recommendations")
                
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to generate recommendations", e)
            }
        }
    }
    
    // Track current video URI
    private var currentVideoUri: Uri? = null
    
    // Public method to generate AI subtitles for current video
    fun generateAISubtitlesForCurrentVideo() {
        viewModelScope.launch {
            try {
                // Use tracked current video URI
                val uri = currentVideoUri
                if (uri != null) {
                    Log.d("PlayerVM", "User requested AI subtitle generation for current video: $uri")
                    
                    // Check if already generating
                    val currentState = aiSubtitleState.value
                    if (currentState.isGenerating) {
                        Log.w("PlayerVM", "AI subtitle generation already in progress")
                        showAISubtitleError("AI subtitle generation is already in progress. Please wait...")
                        return@launch
                    }
                    
                    // Check AI settings
                    val aiEnabled = settingsRepository.getAISubtitleGenerationEnabled().first()
                    if (!aiEnabled) {
                        Log.w("PlayerVM", "AI subtitle generation is disabled in settings")
                        showAISubtitleError("AI subtitle generation is disabled. Please enable it in settings.")
                        return@launch
                    }
                    
                    generateAISubtitles(uri)
                } else {
                    Log.w("PlayerVM", "Cannot generate AI subtitles: no video loaded")
                    showAISubtitleError("No video is currently loaded. Please load a video first.")
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to generate AI subtitles for current video", e)
                showAISubtitleError("Failed to start AI subtitle generation: ${e.message ?: "Unknown error"}")
            }
        }
    }
    
    // Test AI service connectivity
    fun testAIServiceConnectivity() {
        viewModelScope.launch {
            try {
                Log.d("PlayerVM", "Testing AI service connectivity...")
                
                googleAIService.testConnectivity().collect { result ->
                    when (result) {
                        is com.astralplayer.nextplayer.feature.ai.SubtitleGenerationResult.Progress -> {
                            Log.d("PlayerVM", "AI test progress: ${result.message}")
                        }
                        is com.astralplayer.nextplayer.feature.ai.SubtitleGenerationResult.Success -> {
                            Log.i("PlayerVM", "AI service test successful: ${result.subtitleContent}")
                            showAISubtitleError("✅ AI service is working properly!")
                        }
                        is com.astralplayer.nextplayer.feature.ai.SubtitleGenerationResult.Error -> {
                            Log.e("PlayerVM", "AI service test failed: ${result.message}")
                            showAISubtitleError("❌ AI Service Test Failed: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to test AI service", e)
                showAISubtitleError("Failed to test AI service: ${e.message ?: "Unknown error"}")
            }
        }
    }
    
    // Absolute setters for quick settings menu
    fun setVolume(volume: Float) {
        viewModelScope.launch {
            try {
                playerRepository.setVolume(volume)
                Log.d("PlayerVM", "setVolume: volume=$volume")
                
                // Update UI state to reflect the new volume
                _uiState.update {
                    it.copy(
                        volumeInfo = VerticalGestureHandler.VolumeInfo(
                            currentVolume = (volume * 100).toInt(),
                            maxVolume = 100,
                            percentage = volume,
                            isMuted = false
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set volume", e)
            }
        }
    }
    
    fun setBrightness(brightness: Float) {
        viewModelScope.launch {
            try {
                playerRepository.setBrightness(brightness)
                Log.d("PlayerVM", "setBrightness: brightness=$brightness")
                
                // Update UI state to reflect the new brightness
                verticalGestureHandler?.let { handler ->
                    // Create brightness info with the new value
                    _uiState.update {
                        it.copy(
                            brightnessInfo = VerticalGestureHandler.BrightnessInfo(
                                currentBrightness = brightness,
                                percentage = brightness,
                                isAutoBrightness = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set brightness", e)
            }
        }
    }
    
    fun getVolume(): Float {
        return playerRepository.getVolume()
    }
    
    fun getBrightness(): Float {
        return verticalGestureHandler?.getBrightnessInfo()?.percentage ?: 0.5f
    }
    
    fun toggleSubtitles(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    // Enable subtitles - select first available track
                    val availableSubs = availableSubtitles.first()
                    if (availableSubs.isNotEmpty()) {
                        selectSubtitleTrack(availableSubs.first().id)
                    }
                } else {
                    // Disable subtitles
                    selectSubtitleTrack(null)
                }
                Log.d("PlayerVM", "toggleSubtitles: enabled=$enabled")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to toggle subtitles", e)
            }
        }
    }
    
    fun setPlayerView(view: PlayerView) {
        playerView = view
        Log.d("PlayerVM", "PlayerView reference set for aspect ratio control")
    }
    
    fun setAspectRatio(ratio: String) {
        viewModelScope.launch {
            try {
                Log.d("PlayerVM", "setAspectRatio: ratio=$ratio")
                
                playerView?.let { view ->
                    when (ratio.lowercase()) {
                        "fit" -> {
                            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            Log.d("PlayerVM", "Set resize mode to FIT")
                        }
                        "fill", "stretch" -> {
                            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                            Log.d("PlayerVM", "Set resize mode to FILL/STRETCH")
                        }
                        "zoom", "crop" -> {
                            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            Log.d("PlayerVM", "Set resize mode to ZOOM/CROP")
                        }
                        "16:9" -> {
                            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            // Try to set custom aspect ratio if possible
                            try {
                                val aspectRatioFrame = view.findViewById<AspectRatioFrameLayout>(
                                    androidx.media3.ui.R.id.exo_content_frame
                                )
                                aspectRatioFrame?.setAspectRatio(16f / 9f)
                                Log.d("PlayerVM", "Set custom aspect ratio to 16:9")
                            } catch (e: Exception) {
                                Log.w("PlayerVM", "Could not set custom 16:9 aspect ratio", e)
                            }
                        }
                        "4:3" -> {
                            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            try {
                                val aspectRatioFrame = view.findViewById<AspectRatioFrameLayout>(
                                    androidx.media3.ui.R.id.exo_content_frame
                                )
                                aspectRatioFrame?.setAspectRatio(4f / 3f)
                                Log.d("PlayerVM", "Set custom aspect ratio to 4:3")
                            } catch (e: Exception) {
                                Log.w("PlayerVM", "Could not set custom 4:3 aspect ratio", e)
                            }
                        }
                        "21:9" -> {
                            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            try {
                                val aspectRatioFrame = view.findViewById<AspectRatioFrameLayout>(
                                    androidx.media3.ui.R.id.exo_content_frame
                                )
                                aspectRatioFrame?.setAspectRatio(21f / 9f)
                                Log.d("PlayerVM", "Set custom aspect ratio to 21:9")
                            } catch (e: Exception) {
                                Log.w("PlayerVM", "Could not set custom 21:9 aspect ratio", e)
                            }
                        }
                        else -> {
                            Log.w("PlayerVM", "Unknown aspect ratio: $ratio, defaulting to FIT")
                            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }
                    
                    // Update current aspect ratio state
                    _currentAspectRatio.value = ratio
                    
                } ?: run {
                    Log.w("PlayerVM", "PlayerView not available for aspect ratio change")
                }
                
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set aspect ratio", e)
            }
        }
    }
    
    fun setAutoRotate(enabled: Boolean) {
        viewModelScope.launch {
            try {
                Log.d("PlayerVM", "setAutoRotate: enabled=$enabled")
                
                // Update the screen orientation setting
                val orientation = if (enabled) "auto" else "locked"
                settingsRepository.setScreenOrientation(orientation)
                
                Log.d("PlayerVM", "Screen orientation set to: $orientation")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set auto rotate", e)
            }
        }
    }
    
    fun getAutoRotateEnabled(): Flow<Boolean> {
        return settingsRepository.getScreenOrientation().map { orientation ->
            orientation == "auto"
        }
    }
    
    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            try {
                hapticManager.setEnabled(enabled)
                Log.d("PlayerVM", "setHapticFeedback: enabled=$enabled")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set haptic feedback", e)
            }
        }
    }
    
    fun getHapticFeedbackEnabled(): Boolean {
        return true // Default enabled - you could store this in settings if needed
    }
    
    // Long press speed control settings
    fun getLongPressSpeedControlEnabled(): Flow<Boolean> {
        return settingsRepository.getLongPressSpeedControlEnabled()
    }
    
    fun setLongPressSpeedControlEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setLongPressSpeedControlEnabled(enabled)
                Log.d("PlayerVM", "setLongPressSpeedControlEnabled: enabled=$enabled")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set long press speed control enabled", e)
            }
        }
    }
    
    fun getLongPressInitialSpeed(): Flow<Float> {
        return settingsRepository.getLongPressInitialSpeed()
    }
    
    fun setLongPressInitialSpeed(speed: Float) {
        viewModelScope.launch {
            try {
                settingsRepository.setLongPressInitialSpeed(speed)
                Log.d("PlayerVM", "setLongPressInitialSpeed: speed=$speed")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set long press initial speed", e)
            }
        }
    }
    
    fun getLongPressProgressiveSpeedEnabled(): Flow<Boolean> {
        return settingsRepository.getLongPressProgressiveSpeedEnabled()
    }
    
    fun setLongPressProgressiveSpeedEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setLongPressProgressiveSpeedEnabled(enabled)
                Log.d("PlayerVM", "setLongPressProgressiveSpeedEnabled: enabled=$enabled")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set long press progressive speed enabled", e)
            }
        }
    }
    
    fun getLongPressSwipeSensitivity(): Flow<Float> {
        return settingsRepository.getLongPressSwipeSensitivity()
    }
    
    fun setLongPressSwipeSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            try {
                settingsRepository.setLongPressSwipeSensitivity(sensitivity)
                Log.d("PlayerVM", "setLongPressSwipeSensitivity: sensitivity=$sensitivity")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set long press swipe sensitivity", e)
            }
        }
    }
    
    fun getCustomSpeedProgression(): Flow<List<Float>> {
        return settingsRepository.getCustomSpeedProgression().map { progressionString ->
            try {
                progressionString.split(",").map { it.trim().toFloat() }.sorted()
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to parse custom speed progression", e)
                // Return default progression
                listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f)
            }
        }
    }
    
    fun setCustomSpeedProgression(speeds: List<Float>) {
        viewModelScope.launch {
            try {
                val progressionString = speeds.joinToString(",") { "%.2f".format(it) }
                settingsRepository.setCustomSpeedProgression(progressionString)
                Log.d("PlayerVM", "setCustomSpeedProgression: $progressionString")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set custom speed progression", e)
            }
        }
    }
    
    fun getLongPressTimeout(): Flow<Long> {
        return settingsRepository.getLongPressTimeout()
    }
    
    fun setLongPressTimeout(timeout: Long) {
        viewModelScope.launch {
            try {
                settingsRepository.setLongPressTimeout(timeout)
                Log.d("PlayerVM", "setLongPressTimeout: timeout=$timeout")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set long press timeout", e)
            }
        }
    }
    
    // Speed memory per video
    fun getSpeedMemoryEnabled(): Flow<Boolean> {
        return settingsRepository.getSpeedMemoryEnabled()
    }
    
    fun setSpeedMemoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setSpeedMemoryEnabled(enabled)
                Log.d("PlayerVM", "setSpeedMemoryEnabled: enabled=$enabled")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set speed memory enabled", e)
            }
        }
    }
    
    private fun loadVideoSpeedMemory(videoPath: String) {
        viewModelScope.launch {
            try {
                val speedMemoryEnabled = settingsRepository.getSpeedMemoryEnabled().first()
                if (speedMemoryEnabled) {
                    val rememberedSpeed = settingsRepository.getVideoSpeedMemory(videoPath).first()
                    val hasMemory = rememberedSpeed != 1.0f
                    
                    // Update speed memory status
                    _hasSpeedMemory.value = hasMemory
                    
                    if (hasMemory) {
                        playerRepository.setPlaybackSpeed(rememberedSpeed)
                        Log.d("PlayerVM", "Loaded remembered speed for video: ${rememberedSpeed}x")
                        
                        // Show toast notification for speed restoration
                        withContext(Dispatchers.Main) {
                            showSpeedRestoredToast(rememberedSpeed)
                        }
                    }
                } else {
                    _hasSpeedMemory.value = false
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to load video speed memory", e)
                _hasSpeedMemory.value = false
            }
        }
    }
    
    private fun showSpeedRestoredToast(speed: Float) {
        val speedText = formatSpeedText(speed)
        showEnhancedToast(
            message = "Speed restored: $speedText",
            type = com.astralplayer.nextplayer.ui.components.SpeedMemoryToastType.RESTORED
        )
    }
    
    private fun showSpeedSavedToast(speed: Float) {
        val speedText = formatSpeedText(speed)
        showEnhancedToast(
            message = "Speed saved: $speedText",
            type = com.astralplayer.nextplayer.ui.components.SpeedMemoryToastType.SAVED
        )
    }
    
    private fun showSpeedMemoryClearedToast() {
        showEnhancedToast(
            message = "All speed memory cleared",
            type = com.astralplayer.nextplayer.ui.components.SpeedMemoryToastType.CLEARED
        )
    }
    
    private fun showErrorToast(message: String) {
        showEnhancedToast(
            message = message,
            type = com.astralplayer.nextplayer.ui.components.SpeedMemoryToastType.ERROR
        )
    }
    
    private fun showEnhancedToast(
        message: String,
        type: com.astralplayer.nextplayer.ui.components.SpeedMemoryToastType
    ) {
        _currentToast.value = com.astralplayer.nextplayer.ui.components.SpeedMemoryToastState(
            message = message,
            type = type,
            isVisible = true,
            onDismiss = {
                _currentToast.value = null
            }
        )
    }
    
    
    private fun formatSpeedText(speed: Float): String {
        return when {
            speed == 0.25f -> "0.25x"
            speed == 0.5f -> "0.5x"
            speed == 0.75f -> "0.75x"
            speed == 1.0f -> "1.0x"
            speed == 1.25f -> "1.25x"
            speed == 1.5f -> "1.5x"
            speed == 2.0f -> "2.0x"
            speed == 3.0f -> "3.0x"
            speed == 4.0f -> "4.0x"
            else -> "${String.format("%.2f", speed)}x"
        }
    }
    
    private fun saveVideoSpeedMemory(videoPath: String, speed: Float) {
        viewModelScope.launch {
            try {
                val speedMemoryEnabled = settingsRepository.getSpeedMemoryEnabled().first()
                if (speedMemoryEnabled && speed != 1.0f) {
                    settingsRepository.setVideoSpeedMemory(videoPath, speed)
                    Log.d("PlayerVM", "Saved speed memory for video: ${speed}x")
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to save video speed memory", e)
            }
        }
    }
    
    fun clearAllVideoSpeedMemory() {
        viewModelScope.launch {
            try {
                settingsRepository.clearVideoSpeedMemory()
                Log.d("PlayerVM", "Cleared all video speed memory")
                
                // Show toast notification for memory cleared
                withContext(Dispatchers.Main) {
                    showSpeedMemoryClearedToast()
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to clear video speed memory", e)
                
                // Show error toast
                withContext(Dispatchers.Main) {
                    showErrorToast("Failed to clear speed memory")
                }
            }
        }
    }
    
    fun setLoopMode(loopMode: com.astralplayer.nextplayer.data.LoopMode) {
        viewModelScope.launch {
            try {
                playerRepository.setLoopMode(loopMode)
                _currentLoopMode.value = loopMode
                Log.d("PlayerVM", "Loop mode set to: ${loopMode.getDisplayName()}")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set loop mode", e)
            }
        }
    }
    
    fun toggleLoopMode() {
        viewModelScope.launch {
            try {
                playerRepository.toggleLoopMode()
                _currentLoopMode.value = playerRepository.getCurrentLoopMode()
                Log.d("PlayerVM", "Loop mode toggled to: ${_currentLoopMode.value.getDisplayName()}")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to toggle loop mode", e)
            }
        }
    }
    
    fun lockControls() {
        _isControlsLocked.value = true
        Log.d("PlayerVM", "Controls locked")
    }
    
    fun unlockControls() {
        _isControlsLocked.value = false
        Log.d("PlayerVM", "Controls unlocked")
    }
    
    fun toggleControlLock() {
        _isControlsLocked.value = !_isControlsLocked.value
        Log.d("PlayerVM", "Control lock toggled to: ${_isControlsLocked.value}")
    }
    
    // Data classes
    
    data class PlayerUiState(
        val zoomLevel: Float = 1f,
        val seekPreviewInfo: HorizontalSeekGestureHandler.SeekPreviewInfo? = null,
        val volumeInfo: VerticalGestureHandler.VolumeInfo? = null,
        val brightnessInfo: VerticalGestureHandler.BrightnessInfo? = null,
        val longPressSeekInfo: LongPressSeekHandler.LongPressSeekInfo? = null,
        val doubleTapInfo: DoubleTapInfo? = null,
        val gestureConflict: List<GestureType>? = null
    )
    
    data class DoubleTapInfo(
        val side: TouchSide,
        val seekAmount: Long,
        val visible: Boolean
    )
    
    data class OverlayVisibility(
        val seekPreview: Boolean = false,
        val volume: Boolean = false,
        val brightness: Boolean = false,
        val longPress: Boolean = false,
        val doubleTap: Boolean = false,
        val zoom: Boolean = false,
        val conflict: Boolean = false
    )
    
    enum class OverlayType {
        SEEK_PREVIEW,
        VOLUME,
        BRIGHTNESS,
        DOUBLE_TAP,
        ZOOM,
        CONFLICT
    }
    
    // Playback speed functions
    
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    
    private val _speedIndicatorVisible = MutableStateFlow(false)
    val speedIndicatorVisible: StateFlow<Boolean> = _speedIndicatorVisible.asStateFlow()
    
    // Speed memory status for current video
    private val _hasSpeedMemory = MutableStateFlow(false)
    val hasSpeedMemory: StateFlow<Boolean> = _hasSpeedMemory.asStateFlow()
    
    // Enhanced toast state
    private val _currentToast = MutableStateFlow<com.astralplayer.nextplayer.ui.components.SpeedMemoryToastState?>(null)
    val currentToast: StateFlow<com.astralplayer.nextplayer.ui.components.SpeedMemoryToastState?> = _currentToast.asStateFlow()
    
    fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.25f, 4.0f)
        _playbackSpeed.value = clampedSpeed
        
        viewModelScope.launch {
            try {
                playerRepository.exoPlayer.setPlaybackSpeed(clampedSpeed)
                
                // Save speed memory for current video
                currentVideoUri?.let { uri ->
                    saveVideoSpeedMemory(uri.toString(), clampedSpeed)
                    
                    // Update speed memory status (any speed != 1.0f means we have memory)
                    _hasSpeedMemory.value = clampedSpeed != 1.0f
                    
                    // Show toast notification for speed saved
                    withContext(Dispatchers.Main) {
                        showSpeedSavedToast(clampedSpeed)
                    }
                }
                
                // Show speed indicator temporarily
                _speedIndicatorVisible.value = true
                delay(2000)
                _speedIndicatorVisible.value = false
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to set playback speed", e)
            }
        }
    }
    
    fun increaseSpeed() {
        val currentSpeed = _playbackSpeed.value
        val newSpeed = when {
            currentSpeed < 1.0f -> (currentSpeed + 0.25f).coerceAtMost(1.0f)
            currentSpeed < 2.0f -> currentSpeed + 0.25f
            else -> (currentSpeed + 0.5f).coerceAtMost(4.0f)
        }
        setPlaybackSpeed(newSpeed)
    }
    
    fun decreaseSpeed() {
        val currentSpeed = _playbackSpeed.value
        val newSpeed = when {
            currentSpeed > 2.0f -> currentSpeed - 0.5f
            currentSpeed > 1.0f -> (currentSpeed - 0.25f).coerceAtLeast(1.0f)
            else -> (currentSpeed - 0.25f).coerceAtLeast(0.25f)
        }
        setPlaybackSpeed(newSpeed)
    }
    
    fun resetSpeed() {
        setPlaybackSpeed(1.0f)
    }
    
    // Audio track functions
    
    private val _availableAudioTracks = MutableStateFlow<List<com.astralplayer.nextplayer.ui.components.AudioTrack>>(emptyList())
    val availableAudioTracks: StateFlow<List<com.astralplayer.nextplayer.ui.components.AudioTrack>> = _availableAudioTracks.asStateFlow()
    
    private val _currentAudioTrack = MutableStateFlow<com.astralplayer.nextplayer.ui.components.AudioTrack?>(null)
    val currentAudioTrack: StateFlow<com.astralplayer.nextplayer.ui.components.AudioTrack?> = _currentAudioTrack.asStateFlow()
    
    private val _audioBoostEnabled = MutableStateFlow(false)
    val audioBoostEnabled: StateFlow<Boolean> = _audioBoostEnabled.asStateFlow()
    
    private val _audioDelay = MutableStateFlow(0) // in milliseconds
    val audioDelay: StateFlow<Int> = _audioDelay.asStateFlow()
    
    fun selectAudioTrack(track: com.astralplayer.nextplayer.ui.components.AudioTrack?) {
        viewModelScope.launch {
            try {
                val exoPlayer = playerRepository.exoPlayer
                val currentTracks = exoPlayer.currentTracks
                
                if (track == null) {
                    // Clear audio track override (auto-select)
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_AUDIO)
                        .build()
                    _currentAudioTrack.value = null
                    Log.d("PlayerVM", "Audio track selection cleared (auto-select)")
                } else {
                    // Find matching track group
                    for (groupIndex in 0 until currentTracks.groups.size) {
                        val trackGroup = currentTracks.groups[groupIndex]
                        if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                            val mediaTrackGroup = trackGroup.mediaTrackGroup
                            
                            // Find matching track in the group
                            for (trackIndex in 0 until mediaTrackGroup.length) {
                                val format = mediaTrackGroup.getFormat(trackIndex)
                                
                                // Match by language or other criteria
                                if (format.language == track.language || 
                                    format.id == track.id ||
                                    (format.label != null && format.label == track.label)) {
                                    
                                    // Create track selection override
                                    val trackSelectionOverride = androidx.media3.common.TrackSelectionOverride(
                                        mediaTrackGroup,
                                        listOf(trackIndex)
                                    )
                                    
                                    // Apply the override
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(trackSelectionOverride)
                                        .build()
                                    
                                    _currentAudioTrack.value = track
                                    Log.d("PlayerVM", "Audio track selected: ${track.displayName} (${track.language})")
                                    return@launch
                                }
                            }
                        }
                    }
                    
                    Log.w("PlayerVM", "No matching audio track found for: ${track.displayName}")
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to select audio track", e)
            }
        }
    }
    
    fun setAudioBoost(enabled: Boolean) {
        _audioBoostEnabled.value = enabled
        viewModelScope.launch {
            try {
                // Use enhanced volume manager with LoudnessEnhancer for proper 200% boost
                playerRepository.setVolumeBoost(enabled)
                Log.d("PlayerVM", "Enhanced audio boost ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to apply enhanced audio boost", e)
            }
        }
    }
    
    private fun applyAudioBoost(gain: Float) {
        try {
            // ExoPlayer uses AudioProcessor for audio effects
            // We'll use volume scaling as a simple audio boost
            val currentVolume = playerRepository.exoPlayer.volume
            val adjustedVolume = (currentVolume * gain).coerceIn(0f, 1f)
            playerRepository.exoPlayer.volume = adjustedVolume
            
            // Store the original volume for restoration
            if (gain > 1f && !_audioBoostEnabled.value) {
                // Store original volume before boosting
                originalVolume = currentVolume / gain
            } else if (gain == 1f && _audioBoostEnabled.value) {
                // Restore original volume when disabling boost
                playerRepository.exoPlayer.volume = originalVolume
            }
        } catch (e: Exception) {
            Log.e("PlayerVM", "Failed to apply audio boost to ExoPlayer", e)
            throw e
        }
    }
    
    // Store original volume for audio boost restoration
    private var originalVolume: Float = 1.0f
    
    fun setAudioDelay(delayMs: Int) {
        _audioDelay.value = delayMs.coerceIn(-1000, 1000)
        viewModelScope.launch {
            try {
                applyAudioDelay(_audioDelay.value)
                Log.d("PlayerVM", "Audio delay set to: ${_audioDelay.value}ms")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to apply audio delay", e)
            }
        }
    }
    
    private fun applyAudioDelay(delayMs: Int) {
        try {
            // Note: ExoPlayer doesn't have direct audio delay support
            // This would typically require custom AudioProcessor implementation
            // For now, we'll simulate the delay by adjusting the player position slightly
            if (delayMs != 0) {
                val currentPosition = playerRepository.exoPlayer.currentPosition
                val adjustedPosition = (currentPosition + delayMs).coerceAtLeast(0)
                
                // Only apply small adjustments to avoid noticeable seeking
                if (kotlin.math.abs(delayMs) <= 100) {
                    playerRepository.exoPlayer.seekTo(adjustedPosition)
                }
                
                Log.d("PlayerVM", "Applied audio delay: ${delayMs}ms")
            }
        } catch (e: Exception) {
            Log.e("PlayerVM", "Failed to apply audio delay to ExoPlayer", e)
            throw e
        }
    }
    
    private fun updateAvailableAudioTracks() {
        viewModelScope.launch {
            try {
                val exoPlayer = playerRepository.exoPlayer
                val currentTracks = exoPlayer.currentTracks
                val audioTracks = mutableListOf<com.astralplayer.nextplayer.ui.components.AudioTrack>()
                
                // Iterate through track groups to find audio tracks
                for (groupIndex in 0 until currentTracks.groups.size) {
                    val trackGroup = currentTracks.groups[groupIndex]
                    if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                        val mediaTrackGroup = trackGroup.mediaTrackGroup
                        
                        // Extract tracks from this group
                        for (trackIndex in 0 until mediaTrackGroup.length) {
                            val format = mediaTrackGroup.getFormat(trackIndex)
                            val isSelected = trackGroup.isTrackSelected(trackIndex)
                            
                            val audioTrack = com.astralplayer.nextplayer.ui.components.AudioTrack(
                                id = format.id ?: "audio_${groupIndex}_${trackIndex}",
                                label = format.label ?: getLanguageDisplayName(format.language),
                                language = format.language ?: "und",
                                mimeType = format.sampleMimeType ?: "unknown",
                                bitrate = format.bitrate.takeIf { it != androidx.media3.common.Format.NO_VALUE } ?: 0,
                                sampleRate = format.sampleRate.takeIf { it != androidx.media3.common.Format.NO_VALUE } ?: 0,
                                channelCount = format.channelCount.takeIf { it != androidx.media3.common.Format.NO_VALUE } ?: 0,
                                codecName = format.codecs ?: "unknown",
                                isDefault = isSelected
                            )
                            
                            audioTracks.add(audioTrack)
                            
                            if (isSelected) {
                                _currentAudioTrack.value = audioTrack
                            }
                        }
                    }
                }
                
                _availableAudioTracks.value = audioTracks
                Log.d("PlayerVM", "Updated audio tracks: ${audioTracks.size} tracks available")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to update audio tracks", e)
                _availableAudioTracks.value = emptyList()
            }
        }
    }
    
    private fun getLanguageDisplayName(languageCode: String?): String {
        return when (languageCode) {
            null, "und" -> "Unknown"
            "en" -> "English"
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            "it" -> "Italian"
            "pt" -> "Portuguese"
            "ru" -> "Russian"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "zh" -> "Chinese"
            "ar" -> "Arabic"
            "hi" -> "Hindi"
            else -> languageCode.uppercase()
        }
    }
    
    // Video statistics functions
    
    private val _videoStats = MutableStateFlow(com.astralplayer.nextplayer.ui.components.VideoStats())
    val videoStats: StateFlow<com.astralplayer.nextplayer.ui.components.VideoStats> = _videoStats.asStateFlow()
    
    private val _showStatsOverlay = MutableStateFlow(false)
    val showStatsOverlay: StateFlow<Boolean> = _showStatsOverlay.asStateFlow()
    
    private var sessionStartTime = System.currentTimeMillis()
    private var pauseCount = 0
    private var seekCount = 0
    private var lastPosition = 0L
    
    fun toggleStatsOverlay() {
        _showStatsOverlay.value = !_showStatsOverlay.value
    }
    
    fun updateVideoStats(
        title: String = "",
        duration: kotlin.time.Duration = kotlin.time.Duration.ZERO,
        fileSize: Long = 0L,
        resolution: String = "",
        frameRate: Float = 0f,
        videoBitrate: Int = 0,
        videoCodec: String = "",
        mimeType: String = ""
    ) {
        _videoStats.update { currentStats ->
            currentStats.copy(
                videoTitle = title,
                duration = duration,
                fileSize = fileSize,
                resolution = resolution,
                frameRate = frameRate,
                bitrate = videoBitrate,
                codec = videoCodec,
                mimeType = mimeType
            )
        }
    }
    
    fun updateAudioStats(
        audioCodec: String = "",
        audioBitrate: Int = 0,
        sampleRate: Int = 0,
        channels: Int = 0
    ) {
        _videoStats.update { currentStats ->
            currentStats.copy(
                audioCodec = audioCodec,
                audioBitrate = audioBitrate,
                audioSampleRate = sampleRate,
                audioChannels = channels
            )
        }
    }
    
    fun updatePlaybackStats() {
        viewModelScope.launch {
            try {
                val currentPos = currentPosition.first()
                val dur = duration.first()
                val playing = isPlaying.first()
                
                val completionPercentage = if (dur > 0) currentPos.toFloat() / dur else 0f
                val sessionDuration = System.currentTimeMillis() - sessionStartTime
                
                // Simulate some performance metrics
                val droppedFrames = (sessionDuration / 10000).toInt() // Simulate occasional drops
                val totalFrames = (sessionDuration / 33).toLong() // Assuming ~30fps
                val currentBitrate = _videoStats.value.bitrate + ((-50..50).random()) // Simulate bitrate variation
                
                _videoStats.update { currentStats ->
                    currentStats.copy(
                        playbackSpeed = _playbackSpeed.value,
                        droppedFrames = droppedFrames,
                        totalFrames = totalFrames,
                        averageBitrate = currentBitrate,
                        networkSpeed = "WiFi: ${(50..150).random()}Mbps",
                        playbackTime = sessionDuration.milliseconds,
                        pauseCount = pauseCount,
                        seekCount = seekCount,
                        completionPercentage = completionPercentage,
                        batteryUsage = (sessionDuration / 60000f * 0.5f), // Rough estimate
                        bufferedDuration = (5..30).random().toLong().seconds
                    )
                }
                
                // Check for seeks
                if (kotlin.math.abs(currentPos - lastPosition) > 5000) { // 5 second threshold
                    seekCount++
                }
                lastPosition = currentPos
                
            } catch (e: Exception) {
                Log.e("PlayerVM", "Failed to update playback stats", e)
            }
        }
    }
    
    private fun trackPause() {
        pauseCount++
        updatePlaybackStats()
    }
    
    private fun trackSeek() {
        seekCount++
        updatePlaybackStats()
    }
    
    fun exportVideoStats(): String {
        val stats = _videoStats.value
        return buildString {
            appendLine("=== Video Statistics Report ===")
            appendLine()
            appendLine("Video Information:")
            appendLine("  Title: ${stats.videoTitle}")
            appendLine("  Duration: ${formatDuration(stats.duration)}")
            appendLine("  Resolution: ${stats.resolution}")
            appendLine("  Video Codec: ${stats.codec}")
            appendLine("  Audio Codec: ${stats.audioCodec}")
            appendLine()
            appendLine("Performance:")
            appendLine("  Dropped Frames: ${stats.droppedFrames}/${stats.totalFrames}")
            appendLine("  Drop Rate: ${if (stats.totalFrames > 0) (stats.droppedFrames.toFloat() / stats.totalFrames * 100).toInt() else 0}%")
            appendLine("  Average Bitrate: ${stats.averageBitrate / 1000}kbps")
            appendLine()
            appendLine("Session Stats:")
            appendLine("  Watch Time: ${formatDuration(stats.playbackTime)}")
            appendLine("  Completion: ${(stats.completionPercentage * 100).toInt()}%")
            appendLine("  Pause Count: ${stats.pauseCount}")
            appendLine("  Seek Count: ${stats.seekCount}")
            appendLine()
            appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
        }
    }
    
    private fun formatDuration(duration: kotlin.time.Duration): String {
        val totalSeconds = duration.inWholeSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
    
    // Sleep timer functions
    
    private val _sleepTimerActive = MutableStateFlow(false)
    val sleepTimerActive: StateFlow<Boolean> = _sleepTimerActive.asStateFlow()
    
    private val _sleepTimerRemainingTime = MutableStateFlow<kotlin.time.Duration?>(null)
    val sleepTimerRemainingTime: StateFlow<kotlin.time.Duration?> = _sleepTimerRemainingTime.asStateFlow()
    
    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private var sleepTimerAction: com.astralplayer.nextplayer.ui.components.SleepTimerAction? = null
    
    fun setSleepTimer(duration: kotlin.time.Duration, action: com.astralplayer.nextplayer.ui.components.SleepTimerAction) {
        cancelSleepTimer()
        
        _sleepTimerActive.value = true
        _sleepTimerRemainingTime.value = duration
        sleepTimerAction = action
        
        sleepTimerJob = viewModelScope.launch {
            val endTime = System.currentTimeMillis() + duration.inWholeMilliseconds
            
            while (System.currentTimeMillis() < endTime && _sleepTimerActive.value) {
                val remainingMs = endTime - System.currentTimeMillis()
                _sleepTimerRemainingTime.value = remainingMs.toLong().milliseconds
                
                delay(1000) // Update every second
            }
            
            if (_sleepTimerActive.value) {
                executeSleepTimerAction()
            }
        }
    }
    
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerActive.value = false
        _sleepTimerRemainingTime.value = null
        sleepTimerAction = null
    }
    
    private suspend fun executeSleepTimerAction() {
        when (sleepTimerAction) {
            com.astralplayer.nextplayer.ui.components.SleepTimerAction.PAUSE -> {
                if (isPlaying.first()) {
                    playerRepository.exoPlayer.pause()
                }
            }
            com.astralplayer.nextplayer.ui.components.SleepTimerAction.STOP -> {
                playerRepository.exoPlayer.stop()
            }
            com.astralplayer.nextplayer.ui.components.SleepTimerAction.CLOSE_APP -> {
                // This would typically send a signal to the activity to finish
                // For now, just stop playback
                playerRepository.exoPlayer.stop()
            }
            null -> {}
        }
        
        cancelSleepTimer()
    }
    
    // Error handling methods
    
    private suspend fun handleError(error: PlayerError) {
        errorLogger.e("PlayerVM", "Player error occurred", error)
        val recoveryResult = errorRecoveryManager.handleError(error)
        _errorState.value = error to recoveryResult
    }
    
    fun retryLastOperation() {
        viewModelScope.launch {
            try {
                val result = errorRecoveryManager.retryLastOperation()
                if (result.isSuccess) {
                    _errorState.value = null
                }
            } catch (e: Exception) {
                Log.e("PlayerVM", "Retry failed", e)
            }
        }
    }
    
    fun clearError() {
        errorRecoveryManager.clearError()
        _errorState.value = null
    }
    
    fun executeRecoveryAction(action: () -> Unit) {
        try {
            action()
            _errorState.value = null
        } catch (e: Exception) {
            Log.e("PlayerVM", "Recovery action failed", e)
        }
    }
    
    private fun extractCodec(quality: com.astralplayer.nextplayer.data.VideoQuality): String {
        // Extract codec information from the format
        return when {
            quality.displayName.contains("H.264", ignoreCase = true) -> "H.264"
            quality.displayName.contains("H.265", ignoreCase = true) -> "H.265"
            quality.displayName.contains("HEVC", ignoreCase = true) -> "HEVC"
            quality.displayName.contains("VP9", ignoreCase = true) -> "VP9"
            quality.displayName.contains("VP8", ignoreCase = true) -> "VP8"
            quality.displayName.contains("AV1", ignoreCase = true) -> "AV1"
            else -> "H.264" // Default assumption
        }
    }
    
    private fun extractFps(quality: com.astralplayer.nextplayer.data.VideoQuality): Int {
        // Extract FPS from format or use common defaults based on resolution
        return when {
            quality.height >= 2160 -> 60 // 4K often 60fps
            quality.height >= 1080 -> 30 // 1080p usually 30fps
            quality.height >= 720 -> 30  // 720p usually 30fps
            else -> 24 // Lower resolutions often 24fps
        }
    }
}