package com.astralplayer.nextplayer.gesture

import android.content.Context
import android.view.MotionEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Advanced Gesture Manager for AstralStream
 * Manages multi-finger gestures, haptic feedback, and voice commands
 */
@Singleton
class AdvancedGestureManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val multiTouchGestureDetector: MultiTouchGestureDetector,
    @Named("GestureHaptic")
    private val hapticFeedbackManager: com.astralplayer.nextplayer.data.HapticFeedbackManager,
    private val gestureCustomizationRepository: GestureCustomizationRepository,
    private val gestureRecorder: GestureRecorder,
    private val voiceCommandHandler: VoiceCommandHandler
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _currentGesture = MutableStateFlow<GestureType?>(null)
    val currentGesture: StateFlow<GestureType?> = _currentGesture.asStateFlow()
    
    private val _lastActionResult = MutableStateFlow<GestureActionResult?>(null)
    val lastActionResult: StateFlow<GestureActionResult?> = _lastActionResult.asStateFlow()
    
    private val _voiceCommandState = MutableStateFlow<VoiceCommandHandler.VoiceCommandState>(VoiceCommandHandler.VoiceCommandState.IDLE)
    val voiceCommandState: StateFlow<VoiceCommandHandler.VoiceCommandState> = _voiceCommandState.asStateFlow()
    
    private val _recordingState = MutableStateFlow(GestureRecorder.RecordingState.IDLE)
    val recordingState: StateFlow<GestureRecorder.RecordingState> = _recordingState.asStateFlow()
    
    private val _gestureVisualization = MutableStateFlow(GestureVisualization())
    val gestureVisualization: StateFlow<GestureVisualization> = _gestureVisualization.asStateFlow()
    
    private var gestureMappings = mutableMapOf<GestureType, GestureAction>()
    private var customGestures = mutableListOf<GestureCustomizationRepository.CustomGesture>()
    private var gestureSettings = GestureCustomizationRepository.GestureSettings()
    
    suspend fun initialize() {
        // Load gesture mappings
        scope.launch {
            gestureCustomizationRepository.getGestureMappings().collect { mappings ->
                gestureMappings.clear()
                gestureMappings.putAll(mappings)
            }
        }
        
        // Load custom gestures
        scope.launch {
            gestureCustomizationRepository.getCustomGestures().collect { customs ->
                customGestures.clear()
                customGestures.addAll(customs)
            }
        }
        
        // Load settings
        scope.launch {
            gestureCustomizationRepository.getGestureSettings().collect { settings ->
                gestureSettings = settings
                updateVoiceCommandsState(settings.voiceCommandsEnabled)
            }
        }
        
        // Monitor voice command state
        scope.launch {
            voiceCommandHandler.voiceCommandState.collect { state ->
                _voiceCommandState.value = state
            }
        }
        
        // Monitor recording state
        scope.launch {
            gestureRecorder.recordingState.collect { state ->
                _recordingState.value = state
            }
        }
        
        // Handle voice commands
        scope.launch {
            voiceCommandHandler.lastCommand.collect { command ->
                command?.let { handleVoiceCommand(it) }
            }
        }
    }
    
    suspend fun handleTouchEvent(event: MotionEvent): Boolean {
        if (!gestureSettings.multiFingerGesturesEnabled && event.pointerCount > 1) {
            return false
        }
        
        val gestureResult = multiTouchGestureDetector.detectGesture(event)
        
        return when (gestureResult) {
            is GestureResult.Recognized -> {
                _currentGesture.value = gestureResult.gesture
                if (gestureSettings.gestureVisualizationEnabled) {
                    showGestureVisualization(gestureResult.gesture)
                }
                executeGesture(gestureResult.gesture, gestureResult.data)
                scope.launch {
                    kotlinx.coroutines.delay(500)
                    _currentGesture.value = null
                }
                true
            }
            is GestureResult.Recording -> {
                if (_recordingState.value == GestureRecorder.RecordingState.RECORDING) {
                    gestureRecorder.recordGesturePoint(gestureResult.point)
                    updateVisualizationPath(gestureResult.point)
                }
                true
            }
            is GestureResult.NotRecognized -> false
        }
    }
    
    private fun executeGesture(gesture: GestureType, data: GestureData) {
        // Check for custom gesture first
        val customGesture = customGestures.find { it.gesture.id == data.customData["gestureId"] }
        if (customGesture != null) {
            executeAction(customGesture.action, data)
            return
        }
        
        // Get mapped action for gesture
        val action = gestureMappings[gesture]
        if (action != null) {
            executeAction(action, data)
        } else {
            // No mapping found
            _lastActionResult.value = GestureActionResult(
                gesture = gesture,
                action = GestureAction.Custom("unmapped"),
                success = false,
                message = "No action mapped for gesture: ${gesture.name}"
            )
        }
    }
    
    private fun executeAction(action: GestureAction, data: GestureData) {
        if (gestureSettings.hapticFeedbackEnabled) {
            provideHapticFeedback(action)
        }
        
        _lastActionResult.value = GestureActionResult(
            gesture = data.customData["gesture"] as? GestureType ?: GestureType.CUSTOM,
            action = action,
            success = true,
            data = data
        )
    }
    
    private fun provideHapticFeedback(action: GestureAction) {
        val pattern = when (action) {
            is GestureAction.Seek -> com.astralplayer.nextplayer.data.HapticFeedbackManager.HapticPattern.SEEK_TICK
            is GestureAction.VolumeChange -> com.astralplayer.nextplayer.data.HapticFeedbackManager.HapticPattern.VOLUME_TICK
            is GestureAction.BrightnessChange -> com.astralplayer.nextplayer.data.HapticFeedbackManager.HapticPattern.BRIGHTNESS_TICK
            is GestureAction.DoubleTapSeek -> com.astralplayer.nextplayer.data.HapticFeedbackManager.HapticPattern.DOUBLE_TAP
            is GestureAction.TogglePlayPause -> com.astralplayer.nextplayer.data.HapticFeedbackManager.HapticPattern.TAP
            is GestureAction.LongPressSeek -> com.astralplayer.nextplayer.data.HapticFeedbackManager.HapticPattern.LONG_PRESS_START
            is GestureAction.PinchZoom -> com.astralplayer.nextplayer.data.HapticFeedbackManager.HapticPattern.ZOOM_FEEDBACK
            else -> com.astralplayer.nextplayer.data.HapticFeedbackManager.HapticPattern.TAP
        }
        hapticFeedbackManager.playHaptic(pattern)
    }
    
    private fun handleVoiceCommand(command: VoiceCommandHandler.VoiceCommand) {
        val action = when (command) {
            is VoiceCommandHandler.VoiceCommand.PLAY -> GestureAction.TogglePlayPause
            is VoiceCommandHandler.VoiceCommand.PAUSE -> GestureAction.TogglePlayPause
            is VoiceCommandHandler.VoiceCommand.NEXT -> GestureAction.SwipeNavigation("next_video")
            is VoiceCommandHandler.VoiceCommand.PREVIOUS -> GestureAction.SwipeNavigation("previous_video")
            is VoiceCommandHandler.VoiceCommand.SEEK_FORWARD -> GestureAction.Seek(command.seconds * 1000)
            is VoiceCommandHandler.VoiceCommand.SEEK_BACKWARD -> GestureAction.Seek(-command.seconds * 1000)
            is VoiceCommandHandler.VoiceCommand.VOLUME_UP -> GestureAction.VolumeChange(0.1f)
            is VoiceCommandHandler.VoiceCommand.VOLUME_DOWN -> GestureAction.VolumeChange(-0.1f)
            is VoiceCommandHandler.VoiceCommand.SPEED_UP -> GestureAction.LongPressSeek(1.25f)
            is VoiceCommandHandler.VoiceCommand.SPEED_DOWN -> GestureAction.LongPressSeek(0.75f)
            is VoiceCommandHandler.VoiceCommand.NORMAL_SPEED -> GestureAction.LongPressSeek(1.0f)
            is VoiceCommandHandler.VoiceCommand.RECORD_GESTURE -> {
                startGestureRecording()
                return
            }
            is VoiceCommandHandler.VoiceCommand.SAVE_GESTURE -> {
                stopGestureRecording()
                return
            }
            is VoiceCommandHandler.VoiceCommand.CANCEL_GESTURE -> {
                cancelGestureRecording()
                return
            }
            else -> GestureAction.Custom("voice_command")
        }
        
        executeAction(action, GestureData(customData = mapOf("source" to "voice")))
    }
    
    fun startGestureRecording() {
        gestureRecorder.startRecording()
        _gestureVisualization.value = GestureVisualization(isVisible = true)
    }
    
    fun stopGestureRecording() {
        val recordedGesture = gestureRecorder.stopRecording()
        _gestureVisualization.value = GestureVisualization(isVisible = false)
        
        recordedGesture?.let { gesture ->
            // Save the recorded gesture
            val customGesture = GestureCustomizationRepository.CustomGesture(
                id = gesture.id,
                name = "Custom Gesture ${customGestures.size + 1}",
                gesture = gesture,
                action = GestureAction.Custom("unassigned")
            )
            scope.launch {
                val updatedList = customGestures + customGesture
                gestureCustomizationRepository.saveCustomGestures(updatedList)
            }
        }
    }
    
    fun cancelGestureRecording() {
        gestureRecorder.cancelRecording()
        _gestureVisualization.value = GestureVisualization(isVisible = false)
    }
    
    fun toggleVoiceCommands() {
        if (voiceCommandHandler.voiceCommandState.value == VoiceCommandHandler.VoiceCommandState.IDLE) {
            voiceCommandHandler.startListening()
        } else {
            voiceCommandHandler.stopListening()
        }
    }
    
    private fun updateVoiceCommandsState(enabled: Boolean) {
        if (enabled) {
            voiceCommandHandler.initialize()
        } else {
            voiceCommandHandler.stopListening()
        }
    }
    
    private fun showGestureVisualization(gesture: GestureType) {
        scope.launch {
            _gestureVisualization.value = GestureVisualization(
                isVisible = true,
                gestureName = gesture.name
            )
            kotlinx.coroutines.delay(1000)
            _gestureVisualization.value = GestureVisualization(isVisible = false)
        }
    }
    
    private fun updateVisualizationPath(point: android.graphics.PointF) {
        val currentPath = _gestureVisualization.value.path?.toMutableList() ?: mutableListOf()
        currentPath.add(
            GestureRecorder.GesturePoint(
                x = point.x,
                y = point.y,
                timestamp = System.currentTimeMillis()
            )
        )
        _gestureVisualization.value = _gestureVisualization.value.copy(path = currentPath)
    }
    
    suspend fun updateGestureMapping(gestureType: GestureType, action: GestureAction) {
        val updatedMappings = gestureMappings.toMutableMap()
        updatedMappings[gestureType] = action
        gestureCustomizationRepository.saveGestureMappings(updatedMappings)
    }
    
    suspend fun updateGestureSettings(settings: GestureCustomizationRepository.GestureSettings) {
        gestureCustomizationRepository.saveGestureSettings(settings)
    }
    
    fun cleanup() {
        voiceCommandHandler.destroy()
    }
}

// Data classes for gesture system
data class GestureActionResult(
    val gesture: GestureType,
    val action: GestureAction,
    val success: Boolean,
    val message: String? = null,
    val data: GestureData? = null
)

data class GestureVisualization(
    val isVisible: Boolean = false,
    val path: List<GestureRecorder.GesturePoint>? = null,
    val gestureName: String? = null
)