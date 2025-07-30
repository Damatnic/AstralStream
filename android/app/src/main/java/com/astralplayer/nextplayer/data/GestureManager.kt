package com.astralplayer.nextplayer.data

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

// Enhanced gesture settings with comprehensive configuration options
data class EnhancedGestureSettings(
    val general: GeneralGestureSettings = GeneralGestureSettings(),
    val seeking: SeekingGestureSettings = SeekingGestureSettings(),
    val volume: VolumeGestureSettings = VolumeGestureSettings(),
    val brightness: BrightnessGestureSettings = BrightnessGestureSettings(),
    val doubleTap: DoubleTapGestureSettings = DoubleTapGestureSettings(),
    val longPress: LongPressGestureSettings = LongPressGestureSettings(),
    val pinchZoom: PinchZoomGestureSettings = PinchZoomGestureSettings()
)

data class GeneralGestureSettings(
    val isEnabled: Boolean = true,
    val conflictResolutionEnabled: Boolean = true,
    val gestureDeadZone: Float = 20f, // pixels
    val minimumGestureDistance: Float = 10f, // pixels
    val isDebugMode: Boolean = false
)

data class SeekingGestureSettings(
    val isEnabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val showPreviewThumbnails: Boolean = true,
    val showTimeIndicator: Boolean = true,
    val minimumSwipeDistance: Float = 20f,
    val seekStepSize: Long = 5000L, // 5 seconds
    val enableFineSeek: Boolean = true,
    val fineSeekThreshold: Float = 0.3f
)

data class VolumeGestureSettings(
    val isEnabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val rightSideOnly: Boolean = true,
    val showVolumeOverlay: Boolean = true,
    val systemVolumeIntegration: Boolean = true
)

data class BrightnessGestureSettings(
    val isEnabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val leftSideOnly: Boolean = true,
    val showBrightnessOverlay: Boolean = true,
    val systemBrightnessIntegration: Boolean = true,
    val minimumBrightness: Float = 0.01f,
    val maximumBrightness: Float = 1.0f
)

data class DoubleTapGestureSettings(
    val isEnabled: Boolean = true,
    val seekAmount: Long = 10000L, // 10 seconds
    val tapTimeout: Long = 300L, // milliseconds
    val maxTapDistance: Float = 50f // pixels
)

data class LongPressGestureSettings(
    val isEnabled: Boolean = true,
    val triggerDuration: Long = 300L,
    val speedProgression: List<Float> = listOf(1f, 2f, 4f, 8f, 16f, 32f),
    val speedAccelerationInterval: Long = 1000L,
    val maxSpeed: Float = 32f,
    val enableDirectionChange: Boolean = true,
    val directionChangeThreshold: Float = 30f
)

data class PinchZoomGestureSettings(
    val isEnabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val minZoom: Float = 0.5f,
    val maxZoom: Float = 3.0f,
    val showZoomOverlay: Boolean = true
)

// Legacy settings for backward compatibility
data class GestureSettings(
    val isEnabled: Boolean = true,
    val horizontalSeekEnabled: Boolean = true,
    val verticalVolumeEnabled: Boolean = true,
    val verticalBrightnessEnabled: Boolean = true,
    val doubleTapSeekEnabled: Boolean = true,
    val longPressSpeedEnabled: Boolean = true,
    val seekSensitivity: Float = 1.0f,
    val volumeSensitivity: Float = 1.0f,
    val brightnessSensitivity: Float = 1.0f,
    val doubleTapSeekAmount: Long = 10000L // 10 seconds in milliseconds
)

// Enhanced gesture types and actions
enum class GestureType(val priority: Int) {
    LONG_PRESS(5),
    PINCH_ZOOM(4),
    DOUBLE_TAP(3),
    HORIZONTAL_SEEK(2),
    VERTICAL_VOLUME(2),
    VERTICAL_BRIGHTNESS(2),
    SINGLE_TAP(1)
}

enum class TouchSide { LEFT, RIGHT, CENTER }
enum class SeekDirection { FORWARD, BACKWARD }

sealed class GestureAction {
    data class Seek(val deltaMs: Long, val velocity: Float = 0f) : GestureAction()
    data class VolumeChange(val delta: Float, val side: TouchSide) : GestureAction()
    data class BrightnessChange(val delta: Float, val side: TouchSide) : GestureAction()
    data class DoubleTapSeek(val forward: Boolean, val amount: Long, val side: TouchSide) : GestureAction()
    object TogglePlayPause : GestureAction()
    data class LongPressSeek(val speed: Float, val direction: SeekDirection) : GestureAction()
    data class PinchZoom(val scale: Float, val center: Offset) : GestureAction()
    data class GestureConflict(val conflictingGestures: List<GestureType>) : GestureAction()
}

// Gesture detection data classes
data class DetectedGesture(
    val type: GestureType,
    val startPosition: Offset,
    val currentPosition: Offset,
    val startTime: Long,
    val velocity: Velocity = Velocity.Zero,
    val confidence: Float = 1.0f,
    val data: Map<String, Any> = emptyMap()
) {
    val priority: Int get() = type.priority
    val duration: Long get() = System.currentTimeMillis() - startTime
    val distance: Float get() = sqrt(
        (currentPosition.x - startPosition.x) * (currentPosition.x - startPosition.x) +
        (currentPosition.y - startPosition.y) * (currentPosition.y - startPosition.y)
    )
}

data class GestureState(
    val activeGesture: GestureType? = null,
    val gestureStartTime: Long = 0L,
    val gestureStartPosition: Offset = Offset.Zero,
    val currentGesturePosition: Offset = Offset.Zero,
    val gestureVelocity: Velocity = Velocity.Zero,
    val conflictingGestures: List<GestureType> = emptyList(),
    val isGestureActive: Boolean = false
)

// Gesture callbacks interface
data class GestureCallbacks(
    val onHorizontalSeek: (delta: Float, velocity: Float) -> Unit = { _, _ -> },
    val onVerticalVolumeChange: (delta: Float, side: TouchSide) -> Unit = { _, _ -> },
    val onVerticalBrightnessChange: (delta: Float, side: TouchSide) -> Unit = { _, _ -> },
    val onSingleTap: (position: Offset) -> Unit = { _ -> },
    val onDoubleTap: (position: Offset, side: TouchSide) -> Unit = { _, _ -> },
    val onLongPressStart: (position: Offset) -> Unit = { _ -> },
    val onLongPressUpdate: (position: Offset, speed: Float, direction: SeekDirection) -> Unit = { _, _, _ -> },
    val onLongPressEnd: () -> Unit = { },
    val onPinchZoom: (scale: Float, center: Offset) -> Unit = { _, _ -> },
    val onGestureConflict: (conflictingGestures: List<GestureType>) -> Unit = { _ -> }
)

// Enhanced gesture detector interface
interface EnhancedGestureDetector {
    suspend fun detectGestures(
        pointerInputScope: PointerInputScope,
        settings: EnhancedGestureSettings,
        callbacks: GestureCallbacks
    )
}

// Gesture conflict resolver
class GestureConflictResolver {
    fun resolveConflicts(
        activeGestures: List<DetectedGesture>,
        settings: EnhancedGestureSettings
    ): GestureResolution {
        return when {
            activeGestures.size == 1 -> GestureResolution.Execute(activeGestures.first())
            activeGestures.isEmpty() -> GestureResolution.None
            else -> {
                if (settings.general.conflictResolutionEnabled) {
                    val prioritizedGesture = prioritizeGestures(activeGestures, settings)
                    GestureResolution.Execute(prioritizedGesture)
                } else {
                    GestureResolution.Conflict(activeGestures)
                }
            }
        }
    }
    
    private fun prioritizeGestures(
        gestures: List<DetectedGesture>,
        settings: EnhancedGestureSettings
    ): DetectedGesture {
        // Priority order: Long Press > Pinch > Double Tap > Single Tap > Swipe
        // Also consider confidence and duration
        return gestures.maxByOrNull { gesture ->
            gesture.priority * 1000 + (gesture.confidence * 100).toInt() + 
            minOf(gesture.duration / 10, 50) // Cap duration bonus at 50
        } ?: gestures.first()
    }
}

sealed class GestureResolution {
    object None : GestureResolution()
    data class Execute(val gesture: DetectedGesture) : GestureResolution()
    data class Conflict(val conflictingGestures: List<DetectedGesture>) : GestureResolution()
}

// Enhanced gesture manager with multi-layer detection
class EnhancedGestureManager constructor() {
    
    private val _enhancedGestureSettings = MutableStateFlow(EnhancedGestureSettings())
    val enhancedGestureSettings: StateFlow<EnhancedGestureSettings> = _enhancedGestureSettings.asStateFlow()
    
    private val _gestureState = MutableStateFlow(GestureState())
    val gestureState: StateFlow<GestureState> = _gestureState.asStateFlow()
    
    internal val _lastGestureAction = MutableStateFlow<GestureAction?>(null)
    val lastGestureAction: StateFlow<GestureAction?> = _lastGestureAction.asStateFlow()
    
    private val conflictResolver = GestureConflictResolver()
    private val activeGestures = mutableListOf<DetectedGesture>()
    
    // Legacy support
    internal val _gestureSettings = MutableStateFlow(GestureSettings())
    val gestureSettings: StateFlow<GestureSettings> = _gestureSettings.asStateFlow()
    
    fun updateEnhancedSettings(settings: EnhancedGestureSettings) {
        // Validate settings before applying
        val validatedSettings = settings.validate()
        _enhancedGestureSettings.value = validatedSettings
        // Update legacy settings for backward compatibility
        updateLegacySettings(validatedSettings)
    }
    
    private fun updateLegacySettings(enhanced: EnhancedGestureSettings) {
        _gestureSettings.value = GestureSettings(
            isEnabled = enhanced.general.isEnabled,
            horizontalSeekEnabled = enhanced.seeking.isEnabled,
            verticalVolumeEnabled = enhanced.volume.isEnabled,
            verticalBrightnessEnabled = enhanced.brightness.isEnabled,
            doubleTapSeekEnabled = enhanced.doubleTap.isEnabled,
            longPressSpeedEnabled = enhanced.longPress.isEnabled,
            seekSensitivity = enhanced.seeking.sensitivity,
            volumeSensitivity = enhanced.volume.sensitivity,
            brightnessSensitivity = enhanced.brightness.sensitivity,
            doubleTapSeekAmount = enhanced.doubleTap.seekAmount
        )
    }
    
    fun addDetectedGesture(gesture: DetectedGesture) {
        activeGestures.add(gesture)
        updateGestureState()
        resolveGestureConflicts()
    }
    
    fun removeDetectedGesture(gestureType: GestureType) {
        activeGestures.removeAll { it.type == gestureType }
        updateGestureState()
        if (activeGestures.isNotEmpty()) {
            resolveGestureConflicts()
        } else {
            clearGestureState()
        }
    }
    
    private fun updateGestureState() {
        val currentGesture = activeGestures.maxByOrNull { it.priority }
        _gestureState.value = _gestureState.value.copy(
            activeGesture = currentGesture?.type,
            gestureStartTime = currentGesture?.startTime ?: 0L,
            gestureStartPosition = currentGesture?.startPosition ?: Offset.Zero,
            currentGesturePosition = currentGesture?.currentPosition ?: Offset.Zero,
            gestureVelocity = currentGesture?.velocity ?: Velocity.Zero,
            conflictingGestures = if (activeGestures.size > 1) activeGestures.map { it.type } else emptyList(),
            isGestureActive = activeGestures.isNotEmpty()
        )
    }
    
    private fun resolveGestureConflicts() {
        val resolution = conflictResolver.resolveConflicts(activeGestures, _enhancedGestureSettings.value)
        
        when (resolution) {
            is GestureResolution.Execute -> {
                // Execute the prioritized gesture
                executeGesture(resolution.gesture)
            }
            is GestureResolution.Conflict -> {
                // Handle conflict by notifying about conflicting gestures
                _lastGestureAction.value = GestureAction.GestureConflict(
                    resolution.conflictingGestures.map { it.type }
                )
            }
            is GestureResolution.None -> {
                // No action needed
            }
        }
    }
    
    private fun executeGesture(gesture: DetectedGesture) {
        // This will be implemented based on gesture type
        // For now, just update the active gesture
        _gestureState.value = _gestureState.value.copy(
            activeGesture = gesture.type,
            isGestureActive = true
        )
    }
    
    private fun clearGestureState() {
        _gestureState.value = GestureState()
    }
    
    fun getTouchSide(position: Offset, screenWidth: Float): TouchSide {
        val centerThreshold = screenWidth * 0.1f // 10% center zone
        return when {
            position.x < screenWidth / 2 - centerThreshold -> TouchSide.LEFT
            position.x > screenWidth / 2 + centerThreshold -> TouchSide.RIGHT
            else -> TouchSide.CENTER
        }
    }
    
    fun isInDeadZone(position: Offset, screenWidth: Float, screenHeight: Float): Boolean {
        val deadZone = _enhancedGestureSettings.value.general.gestureDeadZone
        return position.x < deadZone || 
               position.x > screenWidth - deadZone ||
               position.y < deadZone || 
               position.y > screenHeight - deadZone
    }
    
    fun clearLastAction() {
        _lastGestureAction.value = null
    }
}

// Legacy GestureManager for backward compatibility
class GestureManager constructor() {
    
    private val enhancedManager = EnhancedGestureManager()
    
    val gestureSettings: StateFlow<GestureSettings> = enhancedManager.gestureSettings
    val lastGestureAction: StateFlow<GestureAction?> = enhancedManager.lastGestureAction
    
    fun updateSettings(settings: GestureSettings) {
        enhancedManager._gestureSettings.value = settings
        // Convert to enhanced settings
        val enhanced = EnhancedGestureSettings(
            general = GeneralGestureSettings(isEnabled = settings.isEnabled),
            seeking = SeekingGestureSettings(
                isEnabled = settings.horizontalSeekEnabled,
                sensitivity = settings.seekSensitivity
            ),
            volume = VolumeGestureSettings(
                isEnabled = settings.verticalVolumeEnabled,
                sensitivity = settings.volumeSensitivity
            ),
            brightness = BrightnessGestureSettings(
                isEnabled = settings.verticalBrightnessEnabled,
                sensitivity = settings.brightnessSensitivity
            ),
            doubleTap = DoubleTapGestureSettings(
                isEnabled = settings.doubleTapSeekEnabled,
                seekAmount = settings.doubleTapSeekAmount
            ),
            longPress = LongPressGestureSettings(
                isEnabled = settings.longPressSpeedEnabled
            )
        )
        enhancedManager.updateEnhancedSettings(enhanced)
    }
    
    fun handleHorizontalDrag(dragAmount: Float, screenWidth: Float) {
        val settings = enhancedManager._gestureSettings.value
        if (!settings.horizontalSeekEnabled) return
        
        // Convert drag amount to seek time (in milliseconds)
        val seekDelta = (dragAmount / screenWidth) * 30000 * settings.seekSensitivity
        enhancedManager._lastGestureAction.value = GestureAction.Seek(seekDelta.toLong())
    }
    
    fun handleVerticalDrag(dragAmount: Float, screenHeight: Float, isLeftSide: Boolean) {
        val settings = enhancedManager._gestureSettings.value
        
        if (isLeftSide && settings.verticalBrightnessEnabled) {
            // Left side controls brightness
            val brightnessDelta = -(dragAmount / screenHeight) * settings.brightnessSensitivity
            val side = if (isLeftSide) TouchSide.LEFT else TouchSide.RIGHT
            enhancedManager._lastGestureAction.value = GestureAction.BrightnessChange(brightnessDelta, side)
        } else if (!isLeftSide && settings.verticalVolumeEnabled) {
            // Right side controls volume
            val volumeDelta = -(dragAmount / screenHeight) * settings.volumeSensitivity
            val side = if (isLeftSide) TouchSide.LEFT else TouchSide.RIGHT
            enhancedManager._lastGestureAction.value = GestureAction.VolumeChange(volumeDelta, side)
        }
    }
    
    fun handleSingleTap() {
        enhancedManager._lastGestureAction.value = GestureAction.TogglePlayPause
    }
    
    fun handleDoubleTap(isRightSide: Boolean) {
        val settings = enhancedManager._gestureSettings.value
        if (!settings.doubleTapSeekEnabled) return
        
        val amount = settings.doubleTapSeekAmount
        val side = if (isRightSide) TouchSide.RIGHT else TouchSide.LEFT
        enhancedManager._lastGestureAction.value = GestureAction.DoubleTapSeek(
            forward = isRightSide,
            amount = amount,
            side = side
        )
    }
    
    fun handleLongPress(speed: Float, forward: Boolean) {
        val settings = enhancedManager._gestureSettings.value
        if (!settings.longPressSpeedEnabled) return
        
        val direction = if (forward) SeekDirection.FORWARD else SeekDirection.BACKWARD
        enhancedManager._lastGestureAction.value = GestureAction.LongPressSeek(speed, direction)
    }
    
    fun clearLastAction() {
        enhancedManager._lastGestureAction.value = null
    }
}

// Enhanced gesture detector implementation
class MultiLayerGestureDetector : EnhancedGestureDetector {
    
    override suspend fun detectGestures(
        pointerInputScope: PointerInputScope,
        settings: EnhancedGestureSettings,
        callbacks: GestureCallbacks
    ) {
        with(pointerInputScope) {
            // Layer 1: Pinch/Zoom gestures (highest priority)
            if (settings.pinchZoom.isEnabled) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    if (abs(zoom - 1f) > 0.1f) { // Zoom threshold
                        callbacks.onPinchZoom(zoom, centroid)
                    }
                }
            }
            
            // Layer 2: Tap gestures
            detectTapGestures(
                onTap = { offset ->
                    callbacks.onSingleTap(offset)
                },
                onDoubleTap = { offset ->
                    val side = when {
                        offset.x < size.width / 3 -> TouchSide.LEFT
                        offset.x > size.width * 2 / 3 -> TouchSide.RIGHT
                        else -> TouchSide.CENTER
                    }
                    callbacks.onDoubleTap(offset, side)
                },
                onLongPress = { offset ->
                    if (settings.longPress.isEnabled) {
                        callbacks.onLongPressStart(offset)
                    }
                }
            )
            
            // Layer 3: Drag gestures
            detectDragGestures(
                onDragStart = { offset ->
                    // Initialize drag gesture detection
                },
                onDrag = { change, dragAmount ->
                    val isLeftSide = change.position.x < size.width / 2
                    val side = when {
                        change.position.x < size.width / 3 -> TouchSide.LEFT
                        change.position.x > size.width * 2 / 3 -> TouchSide.RIGHT
                        else -> TouchSide.CENTER
                    }
                    
                    // Determine gesture type based on drag direction and magnitude
                    val horizontalDrag = abs(dragAmount.x)
                    val verticalDrag = abs(dragAmount.y)
                    
                    if (horizontalDrag > verticalDrag && horizontalDrag > settings.seeking.minimumSwipeDistance) {
                        // Horizontal seek gesture
                        if (settings.seeking.isEnabled) {
                            val velocity = change.position.x - change.previousPosition.x
                            callbacks.onHorizontalSeek(dragAmount.x, velocity)
                        }
                    } else if (verticalDrag > horizontalDrag && verticalDrag > settings.general.minimumGestureDistance) {
                        // Vertical gestures (volume/brightness)
                        if (isLeftSide && settings.brightness.isEnabled && settings.brightness.leftSideOnly) {
                            callbacks.onVerticalBrightnessChange(dragAmount.y, TouchSide.LEFT)
                        } else if (!isLeftSide && settings.volume.isEnabled && settings.volume.rightSideOnly) {
                            callbacks.onVerticalVolumeChange(dragAmount.y, TouchSide.RIGHT)
                        } else if (!settings.brightness.leftSideOnly && !settings.volume.rightSideOnly) {
                            // Allow both sides for both gestures if not restricted
                            if (settings.brightness.isEnabled) {
                                callbacks.onVerticalBrightnessChange(dragAmount.y, side)
                            }
                            if (settings.volume.isEnabled) {
                                callbacks.onVerticalVolumeChange(dragAmount.y, side)
                            }
                        }
                    }
                }
            )
        }
    }
}

// Enhanced Composable modifier with multi-layer gesture detection
@Composable
fun Modifier.enhancedPlayerGestures(
    gestureManager: EnhancedGestureManager,
    screenWidth: Float,
    screenHeight: Float,
    callbacks: GestureCallbacks = GestureCallbacks()
): Modifier {
    val settings by gestureManager.enhancedGestureSettings.collectAsState()
    val detector = remember { MultiLayerGestureDetector() }
    
    return this.pointerInput(settings) {
        if (settings.general.isEnabled) {
            detector.detectGestures(this, settings, callbacks)
        }
    }
}

// Legacy Composable modifier for backward compatibility
@Composable
fun Modifier.playerGestures(
    gestureManager: GestureManager,
    screenWidth: Float,
    screenHeight: Float
): Modifier {
    return this.pointerInput(Unit) {
        detectTapGestures(
            onTap = { gestureManager.handleSingleTap() },
            onDoubleTap = { offset ->
                val isRightSide = offset.x > screenWidth / 2
                gestureManager.handleDoubleTap(isRightSide)
            }
        )
    }.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            val isLeftSide = change.position.x < screenWidth / 2
            
            if (abs(dragAmount.x) > abs(dragAmount.y)) {
                // Horizontal drag - seek
                gestureManager.handleHorizontalDrag(dragAmount.x, screenWidth)
            } else {
                // Vertical drag - volume/brightness
                gestureManager.handleVerticalDrag(dragAmount.y, screenHeight, isLeftSide)
            }
        }
    }
}