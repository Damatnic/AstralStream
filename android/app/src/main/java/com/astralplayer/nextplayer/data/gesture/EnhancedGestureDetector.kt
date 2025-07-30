package com.astralplayer.nextplayer.data.gesture

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.data.SeekDirection
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Enhanced multi-layer gesture detector with comprehensive gesture support
 */
class EnhancedGestureDetector(
    private val gestureManager: EnhancedGestureManager,
    private val callbacks: GestureCallbacks,
    private val screenWidth: Float,
    private val screenHeight: Float
) {
    
    private var longPressJob: Job? = null
    private var currentLongPressSpeed = 1f
    private var longPressStartTime = 0L
    private var longPressDirection = SeekDirection.FORWARD
    private var isLongPressActive = false
    
    /**
     * Main gesture detection function that layers multiple gesture types
     */
    suspend fun PointerInputScope.detectGestures(settings: EnhancedGestureSettings) {
        if (!settings.general.isEnabled) return
        
        // Run all gesture detectors in parallel
        coroutineScope {
            // Layer 1: Transform gestures (pinch/zoom)
            launch {
                if (settings.pinchZoom.isEnabled) {
                    detectPinchZoomGestures(settings.pinchZoom)
                }
            }
            
            // Layer 2: Tap gestures (single, double, long press)
            launch {
                detectTapGestures(settings)
            }
            
            // Layer 3: Drag gestures (seek, volume, brightness)
            launch {
                detectDragGestures(settings)
            }
        }
    }
    
    /**
     * Detects pinch/zoom gestures
     */
    private suspend fun PointerInputScope.detectPinchZoomGestures(settings: PinchZoomGestureSettings) {
        detectTransformGestures { centroid, pan, zoom, rotation ->
            if (abs(zoom - 1f) > 0.05f) { // Zoom threshold
                val adjustedZoom = 1f + (zoom - 1f) * settings.sensitivity
                
                gestureManager.addDetectedGesture(
                    DetectedGesture(
                        type = GestureType.PINCH_ZOOM,
                        startPosition = centroid,
                        currentPosition = centroid,
                        startTime = System.currentTimeMillis(),
                        data = mapOf("zoom" to adjustedZoom, "center" to centroid)
                    )
                )
                
                callbacks.onPinchZoom(adjustedZoom, centroid)
            }
        }
    }
    
    /**
     * Detects all tap-based gestures
     */
    private suspend fun PointerInputScope.detectTapGestures(settings: EnhancedGestureSettings) {
        var tapCount = 0
        var lastTapTime = 0L
        var lastTapPosition = Offset.Zero
        
        detectTapGestures(
            onTap = { offset ->
                val currentTime = System.currentTimeMillis()
                val timeSinceLastTap = currentTime - lastTapTime
                val distanceFromLastTap = (offset - lastTapPosition).getDistance()
                
                if (timeSinceLastTap < settings.doubleTap.tapTimeout && 
                    distanceFromLastTap < settings.doubleTap.maxTapDistance) {
                    // Double tap detected
                    tapCount++
                    if (tapCount == 2 && settings.doubleTap.isEnabled) {
                        handleDoubleTap(offset, settings.doubleTap)
                        tapCount = 0
                    }
                } else {
                    // Single tap
                    tapCount = 1
                    lastTapTime = currentTime
                    lastTapPosition = offset
                    
                    // Delay to check if it's actually a single tap
                    GlobalScope.launch {
                        delay(settings.doubleTap.tapTimeout)
                        if (tapCount == 1) {
                            handleSingleTap(offset)
                            tapCount = 0
                        }
                    }
                }
            },
            onLongPress = { offset ->
                if (settings.longPress.isEnabled) {
                    handleLongPressStart(offset, settings.longPress)
                }
            }
        )
    }
    
    /**
     * Detects drag gestures for seek, volume, and brightness
     */
    private suspend fun PointerInputScope.detectDragGestures(settings: EnhancedGestureSettings) {
        var isDragging = false
        var dragStartPosition = Offset.Zero
        var totalDragAmount = Offset.Zero
        var lastDragTime = 0L
        var velocityTracker = VelocityTracker()
        
        detectDragGestures(
            onDragStart = { offset ->
                isDragging = true
                dragStartPosition = offset
                totalDragAmount = Offset.Zero
                lastDragTime = System.currentTimeMillis()
                velocityTracker = VelocityTracker()
                
                // Cancel long press if dragging starts
                if (isLongPressActive && settings.longPress.enableDirectionChange) {
                    // Check if drag is horizontal enough for direction change
                    return@detectDragGestures
                }
                cancelLongPress()
            },
            onDragEnd = {
                isDragging = false
                val velocity = velocityTracker.calculateVelocity()
                
                // Process final gesture based on total drag
                processDragGesture(
                    totalDragAmount,
                    dragStartPosition,
                    velocity,
                    settings
                )
            },
            onDrag = { change, dragAmount ->
                totalDragAmount += dragAmount
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastDragTime).toFloat()
                lastDragTime = currentTime
                
                velocityTracker.addPosition(currentTime, change.position)
                
                // Handle long press direction change
                if (isLongPressActive && settings.longPress.enableDirectionChange) {
                    val horizontalDrag = abs(dragAmount.x)
                    if (horizontalDrag > settings.longPress.directionChangeThreshold) {
                        val newDirection = if (dragAmount.x > 0) SeekDirection.FORWARD else SeekDirection.BACKWARD
                        if (newDirection != longPressDirection) {
                            longPressDirection = newDirection
                            callbacks.onLongPressUpdate(change.position, currentLongPressSpeed, longPressDirection)
                        }
                    }
                    return@detectDragGestures
                }
                
                // Determine gesture type based on drag direction
                val angle = atan2(totalDragAmount.y, totalDragAmount.x)
                val horizontalComponent = abs(cos(angle))
                val verticalComponent = abs(sin(angle))
                
                when {
                    horizontalComponent > 0.7f && settings.seeking.isEnabled -> {
                        // Horizontal seek gesture
                        handleHorizontalSeek(dragAmount.x, deltaTime, settings.seeking)
                    }
                    verticalComponent > 0.7f -> {
                        // Vertical gesture (volume or brightness)
                        val side = gestureManager.getTouchSide(dragStartPosition, screenWidth)
                        handleVerticalGesture(dragAmount.y, side, settings)
                    }
                }
            }
        )
    }
    
    /**
     * Handles single tap gesture
     */
    private fun PointerInputScope.handleSingleTap(position: Offset) {
        gestureManager.addDetectedGesture(
            DetectedGesture(
                type = GestureType.SINGLE_TAP,
                startPosition = position,
                currentPosition = position,
                startTime = System.currentTimeMillis()
            )
        )
        callbacks.onSingleTap(position)
    }
    
    /**
     * Handles double tap gesture
     */
    private fun PointerInputScope.handleDoubleTap(position: Offset, settings: DoubleTapGestureSettings) {
        val side = gestureManager.getTouchSide(position, size.width.toFloat())
        
        gestureManager.addDetectedGesture(
            DetectedGesture(
                type = GestureType.DOUBLE_TAP,
                startPosition = position,
                currentPosition = position,
                startTime = System.currentTimeMillis(),
                data = mapOf("side" to side, "seekAmount" to settings.seekAmount)
            )
        )
        
        callbacks.onDoubleTap(position, side)
    }
    
    /**
     * Handles long press start
     */
    private fun PointerInputScope.handleLongPressStart(position: Offset, settings: LongPressGestureSettings) {
        isLongPressActive = true
        longPressStartTime = System.currentTimeMillis()
        currentLongPressSpeed = settings.speedProgression.firstOrNull() ?: 1f
        longPressDirection = if (position.x > size.width / 2) SeekDirection.FORWARD else SeekDirection.BACKWARD
        
        gestureManager.addDetectedGesture(
            DetectedGesture(
                type = GestureType.LONG_PRESS,
                startPosition = position,
                currentPosition = position,
                startTime = longPressStartTime
            )
        )
        
        callbacks.onLongPressStart(position)
        
        // Start speed progression
        longPressJob = CoroutineScope(Dispatchers.Main).launch {
            var speedIndex = 0
            while (isActive && isLongPressActive) {
                delay(settings.speedAccelerationInterval)
                
                if (speedIndex < settings.speedProgression.size - 1) {
                    speedIndex++
                    currentLongPressSpeed = settings.speedProgression[speedIndex]
                    callbacks.onLongPressUpdate(position, currentLongPressSpeed, longPressDirection)
                }
            }
        }
    }
    
    /**
     * Cancels active long press
     */
    private fun cancelLongPress() {
        if (isLongPressActive) {
            isLongPressActive = false
            longPressJob?.cancel()
            longPressJob = null
            callbacks.onLongPressEnd()
            gestureManager.removeDetectedGesture(GestureType.LONG_PRESS)
        }
    }
    
    /**
     * Handles horizontal seek gesture
     */
    private fun PointerInputScope.handleHorizontalSeek(dragAmount: Float, deltaTime: Float, settings: SeekingGestureSettings) {
        val velocity = if (deltaTime > 0) dragAmount / deltaTime else 0f
        val adjustedDragAmount = dragAmount * settings.sensitivity
        
        // Fine seek mode for small movements
        val finalDragAmount = if (settings.enableFineSeek && abs(velocity) < settings.fineSeekThreshold) {
            adjustedDragAmount * 0.5f // Reduce sensitivity for fine control
        } else {
            adjustedDragAmount
        }
        
        callbacks.onHorizontalSeek(finalDragAmount, velocity)
    }
    
    /**
     * Handles vertical gestures (volume/brightness)
     */
    private fun PointerInputScope.handleVerticalGesture(dragAmount: Float, side: TouchSide, settings: EnhancedGestureSettings) {
        val normalizedDelta = -dragAmount / size.height.toFloat() // Negative because up is negative
        
        when {
            side == TouchSide.LEFT && settings.brightness.isEnabled && settings.brightness.leftSideOnly -> {
                val adjustedDelta = normalizedDelta * settings.brightness.sensitivity
                callbacks.onVerticalBrightnessChange(adjustedDelta, side)
            }
            side == TouchSide.RIGHT && settings.volume.isEnabled && settings.volume.rightSideOnly -> {
                val adjustedDelta = normalizedDelta * settings.volume.sensitivity
                callbacks.onVerticalVolumeChange(adjustedDelta, side)
            }
            !settings.brightness.leftSideOnly && settings.brightness.isEnabled -> {
                val adjustedDelta = normalizedDelta * settings.brightness.sensitivity
                callbacks.onVerticalBrightnessChange(adjustedDelta, side)
            }
            !settings.volume.rightSideOnly && settings.volume.isEnabled -> {
                val adjustedDelta = normalizedDelta * settings.volume.sensitivity
                callbacks.onVerticalVolumeChange(adjustedDelta, side)
            }
        }
    }
    
    /**
     * Processes the final drag gesture
     */
    private fun PointerInputScope.processDragGesture(
        totalDrag: Offset,
        startPosition: Offset,
        velocity: Velocity,
        settings: EnhancedGestureSettings
    ) {
        val distance = totalDrag.getDistance()
        
        // Only process if drag was significant
        if (distance < settings.general.minimumGestureDistance) return
        
        val angle = atan2(totalDrag.y, totalDrag.x)
        val horizontalComponent = abs(cos(angle))
        
        if (horizontalComponent > 0.7f && settings.seeking.isEnabled) {
            // Final seek adjustment based on total drag
            val seekTime = (totalDrag.x / size.width.toFloat()) * 60000 * settings.seeking.sensitivity
            val direction = if (seekTime > 0) SeekDirection.FORWARD else SeekDirection.BACKWARD
            gestureManager._lastGestureAction.value = GestureAction.Seek(kotlin.math.abs(seekTime.toLong()), direction)
        }
    }
    
    /**
     * Velocity tracker for gesture velocity calculation
     */
    private class VelocityTracker {
        private val positions = mutableListOf<Pair<Long, Offset>>()
        private val maxSamples = 5
        
        fun addPosition(time: Long, position: Offset) {
            positions.add(time to position)
            if (positions.size > maxSamples) {
                positions.removeAt(0)
            }
        }
        
        fun calculateVelocity(): Velocity {
            if (positions.size < 2) return Velocity.Zero
            
            val recent = positions.takeLast(2)
            val timeDelta = (recent[1].first - recent[0].first).toFloat()
            if (timeDelta <= 0) return Velocity.Zero
            
            val positionDelta = recent[1].second - recent[0].second
            return Velocity(
                x = positionDelta.x / timeDelta * 1000f, // Convert to pixels per second
                y = positionDelta.y / timeDelta * 1000f
            )
        }
    }
}

/**
 * Extension function to create gesture detector modifier
 */
@Composable
fun Modifier.enhancedGestureDetector(
    gestureManager: EnhancedGestureManager,
    screenWidth: Float,
    screenHeight: Float,
    callbacks: GestureCallbacks = GestureCallbacks()
): Modifier {
    val settings by gestureManager.enhancedGestureSettings.collectAsState()
    val density = LocalDensity.current
    val detector = remember { EnhancedGestureDetector(gestureManager, callbacks, screenWidth, screenHeight) }
    
    return this.pointerInput(settings, density) {
        with(detector) {
            detectGestures(settings)
        }
    }
}