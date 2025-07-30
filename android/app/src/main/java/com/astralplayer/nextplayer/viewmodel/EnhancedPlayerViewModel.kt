package com.astralplayer.nextplayer.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.view.Window
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.data.gesture.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Enhanced ViewModel for video player with comprehensive gesture support
 */
class EnhancedPlayerViewModel(
    application: Application,
    private val playerRepository: PlayerRepository,
    val gestureManager: EnhancedGestureManager,
    private val settingsSerializer: GestureSettingsSerializer,
    private val hapticManager: HapticFeedbackManager
) : AndroidViewModel(application) {
    
    // Player state flows
    val playerState = playerRepository.playerState
    val currentPosition = playerRepository.playerState.map { it.currentPosition }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), 0L
    )
    val duration = playerRepository.playerState.map { it.duration }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), 0L
    )
    val isPlaying = playerRepository.playerState.map { it.isPlaying }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), false
    )
    val bufferedPercentage = playerRepository.playerState.map { it.bufferedPercentage }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), 0
    )
    
    // Gesture settings flow
    val gestureSettings = gestureManager.enhancedGestureSettings
    
    // UI state
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    // Gesture handlers
    private var horizontalSeekHandler: HorizontalSeekGestureHandler? = null
    private var verticalGestureHandler: VerticalGestureHandler? = null
    private var longPressHandler: LongPressSeekHandler? = null
    private var doubleTapHandler: DoubleTapHandler? = null
    
    // Overlay visibility timers
    private val _overlayVisibilityFlow = MutableStateFlow(OverlayVisibility())
    val overlayVisibility: StateFlow<OverlayVisibility> = _overlayVisibilityFlow.asStateFlow()
    
    init {
        // Load gesture settings
        viewModelScope.launch {
            gestureManager.enhancedGestureSettings.collect { settings ->
                initializeGestureHandlers(settings)
            }
        }
        
        // Monitor gesture actions
        viewModelScope.launch {
            gestureManager.lastGestureAction.collect { action ->
                action?.let { handleGestureAction(it) }
            }
        }
    }
    
    /**
     * Initialize gesture handlers with current settings
     */
    private fun initializeGestureHandlers(settings: EnhancedGestureSettings) {
        val context = getApplication<Application>()
        val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
        
        horizontalSeekHandler = HorizontalSeekGestureHandler(
            screenWidth = screenWidth,
            settings = settings.seeking
        )
        
        doubleTapHandler = DoubleTapHandler(
            settings = settings.doubleTap,
            screenWidth = screenWidth,
            onSeek = { amount, side ->
                handleDoubleTapSeek(amount, side)
            }
        )
        
        longPressHandler = LongPressSeekHandler(
            settings = settings.longPress,
            screenWidth = screenWidth,
            onSpeedUpdate = { speed, direction ->
                updateLongPressUI(speed, direction)
            },
            onSeekUpdate = { seekAmount ->
                seekRelative(seekAmount)
            },
            onEnd = {
                hideLongPressOverlay()
            }
        )
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
                doubleTapHandler?.processTap(position)
            },
            onLongPressStart = { position ->
                longPressHandler?.start(position)
            },
            onLongPressUpdate = { position, speed, direction ->
                // Already handled by longPressHandler
            },
            onLongPressEnd = {
                longPressHandler?.stop()
            },
            onPinchZoom = { scale, center ->
                handlePinchZoom(scale, center)
            },
            onGestureConflict = { conflictingGestures ->
                showGestureConflict(conflictingGestures)
            }
        )
    }
    
    /**
     * Handle horizontal seek gesture
     */
    private fun handleHorizontalSeek(delta: Float, velocity: Float) {
        horizontalSeekHandler?.let { handler ->
            val seekAction = handler.processDrag(
                dragAmount = delta,
                currentTime = System.currentTimeMillis(),
                currentPosition = Offset.Zero // Not used in current implementation
            )
            
            seekAction?.let {
                playerRepository.handleGestureAction(it)
                hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.SEEK_TICK)
                updateSeekPreviewUI()
            }
        }
    }
    
    /**
     * Handle volume change gesture
     */
    private fun handleVolumeChange(delta: Float, side: TouchSide) {
        verticalGestureHandler?.let { handler ->
            val volumeAction = handler.processDrag(
                dragAmount = delta,
                startPosition = Offset.Zero,
                side = side
            )
            
            volumeAction?.let {
                if (it is GestureAction.VolumeChange) {
                    playerRepository.handleGestureAction(it)
                    hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.VOLUME_TICK)
                    showVolumeOverlay()
                }
            }
        }
    }
    
    /**
     * Handle brightness change gesture
     */
    private fun handleBrightnessChange(delta: Float, side: TouchSide) {
        verticalGestureHandler?.let { handler ->
            val brightnessAction = handler.processDrag(
                dragAmount = delta,
                startPosition = Offset.Zero,
                side = side
            )
            
            brightnessAction?.let {
                if (it is GestureAction.BrightnessChange) {
                    // Brightness is handled by the handler itself
                    hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.BRIGHTNESS_TICK)
                    showBrightnessOverlay()
                }
            }
        }
    }
    
    /**
     * Handle double tap seek
     */
    private fun handleDoubleTapSeek(amount: Long, side: TouchSide) {
        val action = GestureAction.DoubleTapSeek(
            forward = side == TouchSide.RIGHT,
            amount = amount,
            side = side
        )
        playerRepository.handleGestureAction(action)
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.DOUBLE_TAP)
        showDoubleTapIndicator(side, amount)
    }
    
    /**
     * Handle pinch zoom gesture
     */
    private fun handlePinchZoom(scale: Float, center: Offset) {
        val currentZoom = _uiState.value.zoomLevel
        val newZoom = (currentZoom * scale).coerceIn(0.5f, 3f)
        
        _uiState.update { it.copy(zoomLevel = newZoom) }
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.ZOOM_FEEDBACK)
        showZoomOverlay()
    }
    
    /**
     * Toggle play/pause
     */
    private fun togglePlayPause() {
        playerRepository.handleGestureAction(GestureAction.TogglePlayPause)
        hapticManager.playHaptic(HapticFeedbackManager.HapticPattern.TAP)
    }
    
    /**
     * Seek relative to current position
     */
    private fun seekRelative(deltaMs: Long) {
        viewModelScope.launch {
            val currentPos = currentPosition.value
            val dur = duration.value
            val newPosition = (currentPos + deltaMs).coerceIn(0, dur)
            playerRepository.seekTo(newPosition)
        }
    }
    
    /**
     * Seek to absolute position
     */
    fun seekTo(position: Long) {
        viewModelScope.launch {
            playerRepository.seekTo(position)
        }
    }
    
    // UI update methods
    
    private fun updateSeekPreviewUI() {
        horizontalSeekHandler?.let { handler ->
            val previewInfo = handler.getSeekPreviewInfo(
                currentPosition = currentPosition.value,
                videoDuration = duration.value
            )
            _uiState.update { it.copy(seekPreviewInfo = previewInfo) }
        }
        
        showSeekOverlay()
    }
    
    private fun updateLongPressUI(speed: Float, direction: SeekDirection) {
        longPressHandler?.let { handler ->
            _uiState.update { it.copy(longPressSeekInfo = handler.getSeekInfo()) }
        }
        showLongPressOverlay()
    }
    
    private fun showSeekOverlay() {
        _overlayVisibilityFlow.update { it.copy(seekPreview = true) }
        startOverlayTimer(OverlayType.SEEK_PREVIEW)
    }
    
    private fun showVolumeOverlay() {
        verticalGestureHandler?.let { handler ->
            _uiState.update { it.copy(volumeInfo = handler.getVolumeInfo()) }
        }
        _overlayVisibilityFlow.update { it.copy(volume = true) }
        startOverlayTimer(OverlayType.VOLUME)
    }
    
    private fun showBrightnessOverlay() {
        verticalGestureHandler?.let { handler ->
            _uiState.update { it.copy(brightnessInfo = handler.getBrightnessInfo()) }
        }
        _overlayVisibilityFlow.update { it.copy(brightness = true) }
        startOverlayTimer(OverlayType.BRIGHTNESS)
    }
    
    private fun showLongPressOverlay() {
        _overlayVisibilityFlow.update { it.copy(longPress = true) }
    }
    
    private fun hideLongPressOverlay() {
        _overlayVisibilityFlow.update { it.copy(longPress = false) }
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
        _overlayVisibilityFlow.update { it.copy(doubleTap = true) }
        startOverlayTimer(OverlayType.DOUBLE_TAP)
    }
    
    private fun showZoomOverlay() {
        _overlayVisibilityFlow.update { it.copy(zoom = true) }
        startOverlayTimer(OverlayType.ZOOM)
    }
    
    private fun showGestureConflict(conflictingGestures: List<GestureType>) {
        _uiState.update { it.copy(gestureConflict = conflictingGestures) }
        _overlayVisibilityFlow.update { it.copy(conflict = true) }
        startOverlayTimer(OverlayType.CONFLICT)
    }
    
    private fun startOverlayTimer(overlayType: OverlayType) {
        viewModelScope.launch {
            delay(2000) // Show for 2 seconds
            _overlayVisibilityFlow.update {
                when (overlayType) {
                    OverlayType.SEEK_PREVIEW -> it.copy(seekPreview = false)
                    OverlayType.VOLUME -> it.copy(volume = false)
                    OverlayType.BRIGHTNESS -> it.copy(brightness = false)
                    OverlayType.DOUBLE_TAP -> it.copy(doubleTap = false)
                    OverlayType.ZOOM -> it.copy(zoom = false)
                    OverlayType.CONFLICT -> it.copy(conflict = false)
                    else -> it
                }
            }
        }
    }
    
    /**
     * Handle generic gesture action
     */
    private fun handleGestureAction(action: GestureAction) {
        playerRepository.handleGestureAction(action)
        hapticManager.playGestureFeedback(action)
    }
    
    /**
     * Load video
     */
    fun loadVideo(uri: Uri) {
        viewModelScope.launch {
            playerRepository.playVideo(uri)
        }
    }
    
    /**
     * Lifecycle methods
     */
    fun onStart() {
        viewModelScope.launch {
            playerRepository.resumeVideo()
        }
    }
    
    fun onStop() {
        viewModelScope.launch {
            playerRepository.pauseVideo()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        playerRepository.release()
        longPressHandler?.cancel()
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
}