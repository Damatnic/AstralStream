package com.astralplayer.nextplayer.data.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.debugInspectorInfo
import com.astralplayer.nextplayer.data.GestureAction
import com.astralplayer.nextplayer.data.TouchSide
import com.astralplayer.nextplayer.data.EnhancedGestureSettings
import com.astralplayer.nextplayer.data.GestureCallbacks
import com.astralplayer.nextplayer.data.GestureType
import com.astralplayer.nextplayer.data.EnhancedGestureManager
import com.astralplayer.nextplayer.data.gesture.performance.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * Optimized gesture detector with performance enhancements
 */
class OptimizedEnhancedGestureDetector(
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val settings: EnhancedGestureSettings,
    private val callbacks: GestureCallbacks,
    private val performanceOptimizer: GesturePerformanceOptimizer,
    private val scope: CoroutineScope
) {
    // Gesture handlers with object pooling
    private val statePool = GestureStatePool()
    private val performanceMonitor = GesturePerformanceMonitor()
    
    // Optimized event channels
    private val gestureEventChannel = Channel<GestureEvent>(Channel.BUFFERED)
    private val feedbackChannel = Channel<HapticFeedback>(Channel.CONFLATED)
    
    // Cached calculations
    private val screenCenter = Offset(screenWidth / 2f, screenHeight / 2f)
    private val leftBoundary = screenWidth * 0.33f
    private val rightBoundary = screenWidth * 0.67f
    
    init {
        // Start gesture event processor
        scope.launch {
            processGestureEvents()
        }
        
        // Start haptic feedback processor
        scope.launch {
            processFeedbackEvents()
        }
    }
    
    /**
     * Optimized drag detection
     */
    suspend fun detectOptimizedDrag(
        pointerInputScope: PointerInputScope
    ) {
        val gestureState = statePool.obtain()
        performanceMonitor.startGesture(GestureType.HORIZONTAL_SEEK)
        
        try {
            pointerInputScope.detectDragGestures(
                onDragStart = { offset ->
                    gestureState.startPosition = offset
                    gestureState.startTime = System.currentTimeMillis()
                    gestureState.currentPosition = offset
                },
                onDrag = { change, _ ->
                    // Skip if performance is poor
                    if (performanceMonitor.performanceData.value.shouldReduceQuality()) {
                        change.consume()
                        return@detectDragGestures
                    }
                    
                    performanceMonitor.recordFrame()
                    
                    // Use optimized calculations
                    val delta = change.position - gestureState.startPosition
                    val direction = OptimizedGestureCalculations.getGestureDirection(delta)
                    
                    when (direction) {
                        OptimizedGestureCalculations.GestureDirection.LEFT,
                        OptimizedGestureCalculations.GestureDirection.RIGHT -> {
                            handleOptimizedHorizontalDrag(change, gestureState)
                        }
                        OptimizedGestureCalculations.GestureDirection.UP,
                        OptimizedGestureCalculations.GestureDirection.DOWN -> {
                            handleOptimizedVerticalDrag(change, gestureState)
                        }
                        else -> {}
                    }
                    
                    gestureState.currentPosition = change.position
                },
                onDragEnd = {
                    performanceMonitor.endGesture()
                    statePool.recycle(gestureState)
                }
            )
        } catch (e: CancellationException) {
            statePool.recycle(gestureState)
            throw e
        }
    }
    
    /**
     * Optimized tap detection with debouncing
     */
    suspend fun detectOptimizedTaps(
        pointerInputScope: PointerInputScope
    ) {
        var lastTapTime = 0L
        val doubleTapTimeout = settings.doubleTap.tapTimeout
        
        pointerInputScope.detectTapGestures(
            onTap = { offset ->
                val currentTime = System.currentTimeMillis()
                
                // Debounce rapid taps
                if (currentTime - lastTapTime < 50) return@detectTapGestures
                
                scope.launch {
                    gestureEventChannel.send(
                        GestureEvent.Tap(offset, currentTime)
                    )
                }
                
                lastTapTime = currentTime
            },
            onDoubleTap = { offset ->
                if (!settings.doubleTap.isEnabled) return@detectTapGestures
                
                scope.launch {
                    gestureEventChannel.send(
                        GestureEvent.DoubleTap(offset, determineSide(offset))
                    )
                }
            },
            onLongPress = { offset ->
                if (!settings.longPress.isEnabled) return@detectTapGestures
                
                scope.launch {
                    gestureEventChannel.send(
                        GestureEvent.LongPress(offset)
                    )
                }
            }
        )
    }
    
    /**
     * Handle optimized horizontal drag
     */
    private fun handleOptimizedHorizontalDrag(
        change: PointerInputChange,
        gestureState: GestureStatePool.GestureState
    ) {
        val delta = change.position.x - gestureState.currentPosition.x
        val velocity = performanceOptimizer.predictNextPosition(
            change.position,
            Offset(delta, 0f),
            0.016f // 16ms frame time
        )
        
        // Throttle events
        performanceOptimizer.throttleGestureEvent(GestureType.HORIZONTAL_SEEK) {
            callbacks.onHorizontalSeek(delta, velocity.x)
        }
        
        change.consume()
    }
    
    /**
     * Handle optimized vertical drag
     */
    private fun handleOptimizedVerticalDrag(
        change: PointerInputChange,
        gestureState: GestureStatePool.GestureState
    ) {
        val side = determineSide(change.position)
        val delta = change.position.y - gestureState.currentPosition.y
        
        val gestureType = if (side == TouchSide.RIGHT) {
            GestureType.VERTICAL_VOLUME
        } else {
            GestureType.VERTICAL_BRIGHTNESS
        }
        
        // Throttle events
        performanceOptimizer.throttleGestureEvent(gestureType) {
            when (side) {
                TouchSide.RIGHT -> callbacks.onVerticalVolumeChange(delta, side)
                TouchSide.LEFT -> callbacks.onVerticalBrightnessChange(delta, side)
                else -> {}
            }
        }
        
        change.consume()
    }
    
    /**
     * Process gesture events asynchronously
     */
    private suspend fun processGestureEvents() {
        gestureEventChannel.consumeAsFlow()
            .buffer(Channel.BUFFERED)
            .collect { event ->
                when (event) {
                    is GestureEvent.Tap -> {
                        callbacks.onSingleTap(event.position)
                    }
                    is GestureEvent.DoubleTap -> {
                        callbacks.onDoubleTap(event.position, event.side)
                        feedbackChannel.send(HapticFeedback.DOUBLE_TAP)
                    }
                    is GestureEvent.LongPress -> {
                        callbacks.onLongPressStart(event.position)
                        feedbackChannel.send(HapticFeedback.LONG_PRESS)
                    }
                }
            }
    }
    
    /**
     * Process haptic feedback with batching
     */
    private suspend fun processFeedbackEvents() {
        feedbackChannel.consumeAsFlow()
            .sample(16) // Limit to 60Hz
            .collect { feedback ->
                // Trigger haptic feedback
                // Implementation depends on haptic manager
            }
    }
    
    /**
     * Optimized side determination using cached boundaries
     */
    private fun determineSide(position: Offset): TouchSide {
        return when {
            position.x < leftBoundary -> TouchSide.LEFT
            position.x > rightBoundary -> TouchSide.RIGHT
            else -> TouchSide.CENTER
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        gestureEventChannel.close()
        feedbackChannel.close()
    }
    
    // Event types
    sealed class GestureEvent {
        data class Tap(val position: Offset, val timestamp: Long) : GestureEvent()
        data class DoubleTap(val position: Offset, val side: TouchSide) : GestureEvent()
        data class LongPress(val position: Offset) : GestureEvent()
    }
    
    enum class HapticFeedback {
        TAP, DOUBLE_TAP, LONG_PRESS, SEEK
    }
}

/**
 * Optimized gesture detector modifier
 */
fun Modifier.optimizedGestureDetector(
    gestureManager: EnhancedGestureManager,
    screenWidth: Float,
    screenHeight: Float,
    callbacks: GestureCallbacks,
    performanceOptimizer: GesturePerformanceOptimizer
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "optimizedGestureDetector"
        properties["screenWidth"] = screenWidth
        properties["screenHeight"] = screenHeight
    }
) {
    val scope = rememberCoroutineScope()
    val settings by gestureManager.enhancedGestureSettings.collectAsState()
    
    if (!settings.general.isEnabled) return@composed this
    
    val detector = remember(screenWidth, screenHeight) {
        OptimizedEnhancedGestureDetector(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            settings = settings,
            callbacks = callbacks,
            performanceOptimizer = performanceOptimizer,
            scope = scope
        )
    }
    
    DisposableEffect(detector) {
        onDispose {
            detector.cleanup()
        }
    }
    
    pointerInput(detector, settings) {
        coroutineScope {
            // Launch parallel gesture detection
            launch {
                while (isActive) {
                    detector.detectOptimizedDrag(this@pointerInput)
                }
            }
            
            launch {
                while (isActive) {
                    detector.detectOptimizedTaps(this@pointerInput)
                }
            }
            
            // Pinch zoom with performance optimization
            if (settings.pinchZoom.isEnabled) {
                launch {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        if (zoom != 1f) {
                            performanceOptimizer.throttleGestureEvent(
                                GestureType.PINCH_ZOOM
                            ) {
                                callbacks.onPinchZoom(zoom, pan)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Gesture event batcher for improved performance
 */
class GestureEventBatcher(
    private val batchSize: Int = 5,
    private val batchTimeoutMs: Long = 16
) {
    private val eventBuffer = mutableListOf<BatchableEvent>()
    private var lastBatchTime = 0L
    
    /**
     * Add event to batch
     */
    fun addEvent(event: BatchableEvent): List<BatchableEvent>? {
        eventBuffer.add(event)
        val currentTime = System.currentTimeMillis()
        
        return if (eventBuffer.size >= batchSize || 
            currentTime - lastBatchTime > batchTimeoutMs) {
            val batch = eventBuffer.toList()
            eventBuffer.clear()
            lastBatchTime = currentTime
            batch
        } else {
            null
        }
    }
    
    interface BatchableEvent {
        val timestamp: Long
        val priority: Int
    }
}

/**
 * Performance-aware gesture settings adapter
 */
class PerformanceAwareSettingsAdapter {
    /**
     * Adapt settings based on current performance
     */
    fun adaptSettings(
        baseSettings: EnhancedGestureSettings,
        performanceData: GesturePerformanceMonitor.PerformanceData
    ): EnhancedGestureSettings {
        return if (performanceData.shouldReduceQuality()) {
            baseSettings.copy(
                general = baseSettings.general.copy(
                    // Keep existing general settings
                    minimumGestureDistance = baseSettings.general.minimumGestureDistance * 1.2f
                ),
                seeking = baseSettings.seeking.copy(
                    // Disable fine seek in low performance
                    enableFineSeek = false
                ),
                pinchZoom = baseSettings.pinchZoom.copy(
                    // Reduce sensitivity for better performance
                    sensitivity = baseSettings.pinchZoom.sensitivity * 0.8f
                )
            )
        } else {
            baseSettings
        }
    }
}