package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Velocity
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Enhanced multi-layer gesture detector with conflict resolution
 */
interface EnhancedGestureDetector {
    suspend fun detectGestures(
        pointerInputScope: PointerInputScope,
        settings: EnhancedGestureSettings,
        callbacks: GestureCallbacks
    )
}

/**
 * Comprehensive gesture callbacks for all gesture types
 */
data class GestureCallbacks(
    val onHorizontalSeek: (delta: Float, velocity: Float, position: Offset) -> Unit,
    val onVerticalVolumeChange: (delta: Float, side: TouchSide, position: Offset) -> Unit,
    val onVerticalBrightnessChange: (delta: Float, side: TouchSide, position: Offset) -> Unit,
    val onSingleTap: (position: Offset) -> Unit,
    val onDoubleTap: (position: Offset, side: TouchSide) -> Unit,
    val onLongPressStart: (position: Offset) -> Unit,
    val onLongPressUpdate: (position: Offset, speed: Float, direction: SeekDirection) -> Unit,
    val onLongPressEnd: () -> Unit,
    val onPinchZoom: (scale: Float, center: Offset) -> Unit,
    val onGestureConflict: (conflictingGestures: List<GestureType>) -> Unit,
    val onGestureStart: (gestureType: GestureType, position: Offset) -> Unit,
    val onGestureEnd: (gestureType: GestureType, success: Boolean) -> Unit
)

/**
 * Detected gesture with metadata
 */
data class DetectedGesture(
    val type: GestureType,
    val startPosition: Offset,
    val currentPosition: Offset,
    val startTime: Long,
    val velocity: Velocity = Velocity.Zero,
    val confidence: Float = 1.0f,
    val priority: Int = type.priority,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Implementation of the enhanced gesture detector
 */
class EnhancedGestureDetectorImpl : EnhancedGestureDetector {
    
    private val conflictResolver = GestureConflictResolver()
    private var activeGestures = mutableListOf<DetectedGesture>()
    private var gestureJobs = mutableMapOf<GestureType, Job>()
    
    override suspend fun detectGestures(
        pointerInputScope: PointerInputScope,
        settings: EnhancedGestureSettings,
        callbacks: GestureCallbacks
    ) = with(pointerInputScope) {
        coroutineScope {
            awaitEachGesture {
                if (!settings.general.gesturesEnabled) return@awaitEachGesture
                
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                val startTime = System.currentTimeMillis()
                
                // Start gesture detection jobs
                startGestureDetectionJobs(firstDown, startTime, settings, callbacks)
                
                // Track pointer movements
                var currentPointer = firstDown
                do {
                    val event = awaitPointerEvent()
                    currentPointer = event.changes.firstOrNull { !it.isConsumed } ?: currentPointer
                    
                    // Update active gestures
                    updateActiveGestures(currentPointer, startTime, settings, callbacks)
                    
                } while (event.changes.any { !it.isConsumed })
                
                // Clean up when all pointers are released
                cleanupGestures(callbacks)
            }
        }
    }
    
    private fun CoroutineScope.startGestureDetectionJobs(
        firstDown: PointerInputChange,
        startTime: Long,
        settings: EnhancedGestureSettings,
        callbacks: GestureCallbacks
    ) {
        // Single tap detection
        if (settings.doubleTap.isEnabled || settings.general.gesturesEnabled) {
            gestureJobs[GestureType.SINGLE_TAP] = launch {
                detectSingleTap(firstDown, startTime, settings, callbacks)
            }
        }
        
        // Double tap detection
        if (settings.doubleTap.isEnabled) {
            gestureJobs[GestureType.DOUBLE_TAP] = launch {
                detectDoubleTap(firstDown, startTime, settings, callbacks)
            }
        }
        
        // Long press detection
        if (settings.longPress.isEnabled) {
            gestureJobs[GestureType.LONG_PRESS] = launch {
                detectLongPress(firstDown, startTime, settings, callbacks)
            }
        }
        
        // Swipe gesture detection
        if (settings.seeking.isEnabled || settings.volume.isEnabled || settings.brightness.isEnabled) {
            gestureJobs[GestureType.HORIZONTAL_SEEK] = launch {
                detectSwipeGestures(firstDown, startTime, settings, callbacks)
            }
        }
    }
    
    private suspend fun detectSingleTap(
        firstDown: PointerInputChange,
        startTime: Long,
        settings: EnhancedGestureSettings,
        callbacks: GestureCallbacks
    ) {
        delay(settings.doubleTap.tapTimeout)
        
        // Check if this is still a potential single tap
        if (activeGestures.none { it.type == GestureType.DOUBLE_TAP || it.type == GestureType.LONG_PRESS }) {
            val gesture = DetectedGesture(
                type = GestureType.SINGLE_TAP,
                startPosition = firstDown.position,
                currentPosition = firstDown.position,
                startTime = startTime
            )
            
            if (resolveGestureConflict(gesture, settings)) {
                callbacks.onGestureStart(GestureType.SINGLE_TAP, firstDown.position)
                callbacks.onSingleTap(firstDown.position)
                callbacks.onGestureEnd(GestureType.SINGLE_TAP, true)
            }
        }
    }
    
    private suspend fun detectDoubleTap(
        firstDown: PointerInputChange,
        startTime: Long,
        settings: EnhancedGestureSettings,
        callbacks: GestureCallbacks
    ) {
        // Wait for potential second tap
        delay(settings.doubleTap.tapTimeout)
        
        // This would need more sophisticated logic to detect actual double taps
        // For now, we'll implement a basic version
        val gesture = DetectedGesture(
            type = GestureType.DOUBLE_TAP,
            startPosition = firstDown.position,
            currentPosition = firstDown.position,
            startTime = startTime,
            confidence = 0.8f
        )
        
        if (resolveGestureConflict(gesture, settings)) {
            val side = determineTouchSide(firstDown.position, size.width.toFloat(), settings.doubleTap.centerDeadZone)
            callbacks.onGestureStart(GestureType.DOUBLE_TAP, firstDown.position)
            callbacks.onDoubleTap(firstDown.position, side)
            callbacks.onGestureEnd(GestureType.DOUBLE_TAP, true)
        }
    }
    
    private suspend fun detectLongPress(
        firstDown: PointerInputChange,
        startTime: Long,
        settings: EnhancedGestureSettings,
        callbacks: GestureCallbacks
    ) {
        delay(settings.longPress.triggerDuration)
        
        val gesture = DetectedGesture(
            type = GestureType.LONG_PRESS,
            startPosition = firstDown.position,
            currentPosition = firstDown.position,
            startTime = startTime
        )
        
        if (resolveGestureConflict(gesture, settings)) {
            callbacks.onGestureStart(GestureType.LONG_PRESS, firstDown.position)
            callbacks.onLongPressStart(firstDown.position)
            
            // Continue long press tracking
            var lastPosition = firstDown.position
            var currentSpeed = 1f
            var currentDirection = SeekDirection.FORWARD
            
            while (isActive) {
                delay(settings.longPress.continuousSeekInterval)
                
                // Calculate speed and direction based on movement
                // This is a simplified version - real implementation would track actual pointer movement
                callbacks.onLongPressUpdate(lastPosition, currentSpeed, currentDirection)
            }
        }
    }
    
    private suspend fun detectSwipeGestures(
        firstDown: PointerInputChange,
        startTime: Long,
        settings: EnhancedGestureSettings,
        callbacks: GestureCallbacks
    ) {
        var startPosition = firstDown.position
        var lastPosition = firstDown.position
        var totalDelta = Offset.Zero
        var gestureStarted = false
        var gestureType: GestureType? = null
        
        while (isActive) {
            delay(16) // ~60fps update rate
            
            // This would be updated by the main gesture loop
            val currentPosition = lastPosition // Placeholder
            val delta = currentPosition - lastPosition
            totalDelta += delta
            
            val distance = sqrt(totalDelta.x * totalDelta.x + totalDelta.y * totalDelta.y)
            val isHorizontal = abs(totalDelta.x) > abs(totalDelta.y)
            val isVertical = abs(totalDelta.y) > abs(totalDelta.x)
            
            // Determine gesture type based on movement
            if (!gestureStarted && distance > 20f) { // Minimum distance threshold
                gestureType = when {
                    isHorizontal && settings.seeking.isEnabled -> GestureType.HORIZONTAL_SEEK
                    isVertical && settings.volume.isEnabled && currentPosition.x > size.width / 2 -> GestureType.VERTICAL_VOLUME
                    isVertical && settings.brightness.isEnabled && currentPosition.x < size.width / 2 -> GestureType.VERTICAL_BRIGHTNESS
                    else -> null
                }
                
                gestureType?.let { type ->
                    val gesture = DetectedGesture(
                        type = type,
                        startPosition = startPosition,
                        currentPosition = currentPosition,
                        startTime = startTime
                    )
                    
                    if (resolveGestureConflict(gesture, settings)) {
                        gestureStarted = true
                        callbacks.onGestureStart(type, startPosition)
                    }
                }
            }
            
            // Continue gesture if started
            if (gestureStarted && gestureType != null) {
                val velocity = calculateVelocity(delta, 16f)
                
                when (gestureType) {
                    GestureType.HORIZONTAL_SEEK -> {
                        callbacks.onHorizontalSeek(delta.x * settings.seeking.sensitivity, velocity.x, currentPosition)
                    }
                    GestureType.VERTICAL_VOLUME -> {
                        val side = if (currentPosition.x > size.width / 2) TouchSide.RIGHT else TouchSide.LEFT
                        callbacks.onVerticalVolumeChange(-delta.y * settings.volume.sensitivity, side, currentPosition)
                    }
                    GestureType.VERTICAL_BRIGHTNESS -> {
                        val side = if (currentPosition.x < size.width / 2) TouchSide.LEFT else TouchSide.RIGHT
                        callbacks.onVerticalBrightnessChange(-delta.y * settings.brightness.sensitivity, side, currentPosition)
                    }
                    else -> {}
                }
            }
            
            lastPosition = currentPosition
        }
        
        // Gesture ended
        if (gestureStarted && gestureType != null) {
            callbacks.onGestureEnd(gestureType, true)
        }
    }
    
    private fun updateActiveGestures(
        currentPointer: PointerInputChange,
        startTime: Long,
        settings: EnhancedGestureSettings,
        callbacks: GestureCallbacks
    ) {
        // Update positions and states of active gestures
        activeGestures.forEach { gesture ->
            // Update gesture state based on current pointer position
            // This is where we'd update the gesture's current position, velocity, etc.
        }
    }
    
    private fun cleanupGestures(callbacks: GestureCallbacks) {
        // Cancel all active gesture jobs
        gestureJobs.values.forEach { it.cancel() }
        gestureJobs.clear()
        
        // End any active long press
        if (activeGestures.any { it.type == GestureType.LONG_PRESS }) {
            callbacks.onLongPressEnd()
        }
        
        // Clear active gestures
        activeGestures.clear()
    }
    
    private fun resolveGestureConflict(
        newGesture: DetectedGesture,
        settings: EnhancedGestureSettings
    ): Boolean {
        val conflictingGestures = activeGestures.filter { 
            it.type != newGesture.type && isGestureConflicting(it, newGesture) 
        }
        
        if (conflictingGestures.isEmpty()) {
            activeGestures.add(newGesture)
            return true
        }
        
        val resolution = conflictResolver.resolveConflicts(
            conflictingGestures + newGesture,
            settings
        )
        
        return when (resolution) {
            is GestureResolution.Execute -> {
                if (resolution.gesture == newGesture) {
                    // Remove conflicting gestures and add new one
                    activeGestures.removeAll(conflictingGestures)
                    activeGestures.add(newGesture)
                    true
                } else {
                    false
                }
            }
            is GestureResolution.Conflict -> {
                // Let the callback handle the conflict
                false
            }
            GestureResolution.None -> false
        }
    }
    
    private fun isGestureConflicting(gesture1: DetectedGesture, gesture2: DetectedGesture): Boolean {
        // Define which gestures conflict with each other
        return when {
            gesture1.type == GestureType.SINGLE_TAP && gesture2.type == GestureType.DOUBLE_TAP -> true
            gesture1.type == GestureType.SINGLE_TAP && gesture2.type == GestureType.LONG_PRESS -> true
            gesture1.type == GestureType.DOUBLE_TAP && gesture2.type == GestureType.LONG_PRESS -> true
            gesture1.type == GestureType.HORIZONTAL_SEEK && gesture2.type == GestureType.VERTICAL_VOLUME -> true
            gesture1.type == GestureType.HORIZONTAL_SEEK && gesture2.type == GestureType.VERTICAL_BRIGHTNESS -> true
            else -> false
        }
    }
    
    private fun determineTouchSide(position: Offset, screenWidth: Float, deadZone: Float): TouchSide {
        val deadZoneWidth = screenWidth * deadZone
        val leftBoundary = screenWidth / 2 - deadZoneWidth / 2
        val rightBoundary = screenWidth / 2 + deadZoneWidth / 2
        
        return when {
            position.x < leftBoundary -> TouchSide.LEFT
            position.x > rightBoundary -> TouchSide.RIGHT
            else -> TouchSide.CENTER
        }
    }
    
    private fun calculateVelocity(delta: Offset, deltaTimeMs: Float): Velocity {
        val deltaTimeSeconds = deltaTimeMs / 1000f
        return Velocity(
            x = delta.x / deltaTimeSeconds,
            y = delta.y / deltaTimeSeconds
        )
    }
}

/**
 * Gesture conflict resolver with priority-based resolution
 */
class GestureConflictResolver {
    
    fun resolveConflicts(
        activeGestures: List<DetectedGesture>,
        settings: EnhancedGestureSettings
    ): GestureResolution {
        return when {
            activeGestures.size == 1 -> GestureResolution.Execute(activeGestures.first())
            activeGestures.isEmpty() -> GestureResolution.None
            else -> {
                when (settings.general.conflictResolution) {
                    ConflictResolutionStrategy.PRIORITY_BASED -> {
                        val prioritizedGesture = prioritizeGestures(activeGestures)
                        GestureResolution.Execute(prioritizedGesture)
                    }
                    ConflictResolutionStrategy.FIRST_DETECTED -> {
                        val firstGesture = activeGestures.minByOrNull { it.startTime }
                        firstGesture?.let { GestureResolution.Execute(it) } ?: GestureResolution.None
                    }
                    ConflictResolutionStrategy.LAST_DETECTED -> {
                        val lastGesture = activeGestures.maxByOrNull { it.startTime }
                        lastGesture?.let { GestureResolution.Execute(it) } ?: GestureResolution.None
                    }
                    ConflictResolutionStrategy.USER_CHOICE -> {
                        GestureResolution.Conflict(activeGestures)
                    }
                }
            }
        }
    }
    
    private fun prioritizeGestures(gestures: List<DetectedGesture>): DetectedGesture {
        // Priority order: Long Press > Pinch > Double Tap > Single Tap > Swipe
        return gestures.maxByOrNull { gesture ->
            gesture.priority + (gesture.confidence * 10).toInt()
        } ?: gestures.first()
    }
}

/**
 * Gesture resolution result
 */
sealed class GestureResolution {
    object None : GestureResolution()
    data class Execute(val gesture: DetectedGesture) : GestureResolution()
    data class Conflict(val conflictingGestures: List<DetectedGesture>) : GestureResolution()
}

/**
 * Gesture state tracker for managing active gestures
 */
class GestureStateTracker {
    private val activeGestures = mutableMapOf<GestureType, DetectedGesture>()
    private val gestureHistory = mutableListOf<DetectedGesture>()
    
    fun addGesture(gesture: DetectedGesture) {
        activeGestures[gesture.type] = gesture
        gestureHistory.add(gesture)
        
        // Keep history limited to prevent memory issues
        if (gestureHistory.size > 100) {
            gestureHistory.removeAt(0)
        }
    }
    
    fun removeGesture(gestureType: GestureType) {
        activeGestures.remove(gestureType)
    }
    
    fun getActiveGestures(): List<DetectedGesture> = activeGestures.values.toList()
    
    fun getGestureHistory(): List<DetectedGesture> = gestureHistory.toList()
    
    fun isGestureActive(gestureType: GestureType): Boolean = activeGestures.containsKey(gestureType)
    
    fun clear() {
        activeGestures.clear()
    }
}