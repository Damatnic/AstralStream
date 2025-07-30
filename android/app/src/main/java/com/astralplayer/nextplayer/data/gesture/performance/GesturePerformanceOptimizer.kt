package com.astralplayer.nextplayer.data.gesture.performance

import android.os.SystemClock
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import com.astralplayer.nextplayer.data.GestureType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Optimizes gesture detection performance through various techniques
 */
@Stable
class GesturePerformanceOptimizer(
    private val scope: CoroutineScope
) {
    // Gesture event throttling
    private val gestureThrottler = GestureEventThrottler()
    
    // Frame skip detector
    private val frameSkipDetector = FrameSkipDetector()
    
    // Gesture prediction
    private val gesturePredictor = GesturePredictor()
    
    // Performance metrics
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    /**
     * Optimize pointer input processing
     */
    fun optimizePointerInput(
        changes: List<PointerInputChange>,
        onOptimizedChange: (PointerInputChange) -> Unit
    ) {
        // Skip if too many events
        if (frameSkipDetector.shouldSkipFrame()) {
            updateMetrics { it.copy(skippedFrames = it.skippedFrames + 1) }
            return
        }
        
        // Process only significant changes
        changes.forEach { change ->
            if (isSignificantChange(change)) {
                onOptimizedChange(change)
            }
        }
    }
    
    /**
     * Throttle gesture events to prevent overwhelming the system
     */
    fun throttleGestureEvent(
        gestureType: GestureType,
        event: () -> Unit
    ) {
        gestureThrottler.throttle(gestureType, event)
    }
    
    /**
     * Predict next gesture position for smoother interaction
     */
    fun predictNextPosition(
        currentPosition: Offset,
        velocity: Offset,
        deltaTime: Float
    ): Offset {
        return gesturePredictor.predictPosition(currentPosition, velocity, deltaTime)
    }
    
    /**
     * Batch gesture updates for better performance
     */
    fun batchGestureUpdates(
        updates: List<GestureUpdate>,
        onBatchComplete: (List<GestureUpdate>) -> Unit
    ) {
        scope.launch {
            val batchChannel = Channel<GestureUpdate>(Channel.UNLIMITED)
            
            // Collect updates
            updates.forEach { batchChannel.send(it) }
            
            // Process batch after delay
            delay(16) // One frame at 60fps
            
            val batch = mutableListOf<GestureUpdate>()
            while (!batchChannel.isEmpty) {
                batch.add(batchChannel.receive())
            }
            
            if (batch.isNotEmpty()) {
                onBatchComplete(batch)
            }
        }
    }
    
    /**
     * Check if pointer change is significant enough to process
     */
    private fun isSignificantChange(change: PointerInputChange): Boolean {
        val positionChange = change.position - change.previousPosition
        val threshold = 2f // pixels
        
        return abs(positionChange.x) > threshold || abs(positionChange.y) > threshold
    }
    
    /**
     * Update performance metrics
     */
    private fun updateMetrics(update: (PerformanceMetrics) -> PerformanceMetrics) {
        _performanceMetrics.value = update(_performanceMetrics.value)
    }
    
    data class PerformanceMetrics(
        val averageFrameTime: Float = 0f,
        val skippedFrames: Int = 0,
        val throttledEvents: Int = 0,
        val predictedPositions: Int = 0
    )
    
    data class GestureUpdate(
        val gestureType: GestureType,
        val position: Offset,
        val timestamp: Long
    )
}

/**
 * Throttles gesture events to prevent overwhelming the system
 */
class GestureEventThrottler {
    private val lastEventTimes = ConcurrentHashMap<GestureType, Long>()
    private val throttleIntervals = mapOf(
        GestureType.HORIZONTAL_SEEK to 16L, // 60fps
        GestureType.VERTICAL_VOLUME to 32L, // 30fps
        GestureType.VERTICAL_BRIGHTNESS to 32L,
        GestureType.SINGLE_TAP to 100L,
        GestureType.DOUBLE_TAP to 50L,
        GestureType.LONG_PRESS to 100L,
        GestureType.PINCH_ZOOM to 16L
    )
    
    /**
     * Throttle gesture event
     */
    fun throttle(gestureType: GestureType, event: () -> Unit) {
        val currentTime = SystemClock.elapsedRealtime()
        val lastTime = lastEventTimes[gestureType] ?: 0L
        val interval = throttleIntervals[gestureType] ?: 16L
        
        if (currentTime - lastTime >= interval) {
            lastEventTimes[gestureType] = currentTime
            event()
        }
    }
}

/**
 * Detects when frames should be skipped to maintain performance
 */
class FrameSkipDetector {
    private val frameTimeBuffer = CircularBuffer<Long>(10)
    private var lastFrameTime = 0L
    private val targetFrameTime = 16L // 60fps
    
    /**
     * Check if current frame should be skipped
     */
    fun shouldSkipFrame(): Boolean {
        val currentTime = SystemClock.elapsedRealtime()
        
        if (lastFrameTime != 0L) {
            val frameTime = currentTime - lastFrameTime
            frameTimeBuffer.add(frameTime)
            
            // Skip if average frame time is too high
            val averageFrameTime = frameTimeBuffer.average()
            if (averageFrameTime > targetFrameTime * 2) {
                return true
            }
        }
        
        lastFrameTime = currentTime
        return false
    }
}

/**
 * Predicts gesture positions for smoother interaction
 */
class GesturePredictor {
    private val positionHistory = mutableListOf<PositionSnapshot>()
    private val maxHistorySize = 5
    
    /**
     * Predict next position based on velocity
     */
    fun predictPosition(
        currentPosition: Offset,
        velocity: Offset,
        deltaTime: Float
    ): Offset {
        // Add to history
        positionHistory.add(
            PositionSnapshot(
                position = currentPosition,
                velocity = velocity,
                timestamp = SystemClock.elapsedRealtime()
            )
        )
        
        // Limit history size
        if (positionHistory.size > maxHistorySize) {
            positionHistory.removeAt(0)
        }
        
        // Simple linear prediction
        val predictedX = currentPosition.x + velocity.x * deltaTime
        val predictedY = currentPosition.y + velocity.y * deltaTime
        
        return Offset(predictedX, predictedY)
    }
    
    /**
     * Get smoothed velocity from history
     */
    fun getSmoothedVelocity(): Offset {
        if (positionHistory.size < 2) return Offset.Zero
        
        val velocities = positionHistory.windowed(2) { (prev, curr) ->
            val deltaTime = (curr.timestamp - prev.timestamp) / 1000f
            if (deltaTime > 0) {
                Offset(
                    (curr.position.x - prev.position.x) / deltaTime,
                    (curr.position.y - prev.position.y) / deltaTime
                )
            } else {
                Offset.Zero
            }
        }
        
        // Average velocities
        val avgX = velocities.map { it.x }.average().toFloat()
        val avgY = velocities.map { it.y }.average().toFloat()
        
        return Offset(avgX, avgY)
    }
    
    data class PositionSnapshot(
        val position: Offset,
        val velocity: Offset,
        val timestamp: Long
    )
}

/**
 * Circular buffer for efficient performance tracking
 */
class CircularBuffer<T : Number>(private val size: Int) {
    private val buffer = mutableListOf<T>()
    private var index = 0
    
    fun add(value: T) {
        if (buffer.size < size) {
            buffer.add(value)
        } else {
            buffer[index] = value
            index = (index + 1) % size
        }
    }
    
    fun average(): Double {
        if (buffer.isEmpty()) return 0.0
        return buffer.sumOf { it.toDouble() } / buffer.size
    }
}

/**
 * Memory-efficient gesture state pooling
 */
class GestureStatePool {
    private val pool = mutableListOf<GestureState>()
    private val maxPoolSize = 10
    
    /**
     * Obtain a gesture state from pool
     */
    fun obtain(): GestureState {
        return if (pool.isNotEmpty()) {
            pool.removeAt(pool.lastIndex).apply { reset() }
        } else {
            GestureState()
        }
    }
    
    /**
     * Return gesture state to pool
     */
    fun recycle(state: GestureState) {
        if (pool.size < maxPoolSize) {
            state.reset()
            pool.add(state)
        }
    }
    
    class GestureState {
        var startPosition: Offset = Offset.Zero
        var currentPosition: Offset = Offset.Zero
        var startTime: Long = 0L
        var gestureType: GestureType? = null
        
        fun reset() {
            startPosition = Offset.Zero
            currentPosition = Offset.Zero
            startTime = 0L
            gestureType = null
        }
    }
}

/**
 * Optimized gesture calculation utilities
 */
object OptimizedGestureCalculations {
    /**
     * Fast distance calculation without sqrt
     */
    fun fastDistanceSquared(p1: Offset, p2: Offset): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return dx * dx + dy * dy
    }
    
    /**
     * Fast angle calculation using approximation
     */
    fun fastAngle(delta: Offset): Float {
        // Use atan2 approximation for better performance
        val x = delta.x
        val y = delta.y
        
        if (x == 0f) return if (y > 0) 90f else -90f
        
        val angle = Math.toDegrees(kotlin.math.atan2(y.toDouble(), x.toDouble())).toFloat()
        return angle
    }
    
    /**
     * Optimized gesture direction detection
     */
    fun getGestureDirection(delta: Offset, threshold: Float = 20f): GestureDirection {
        val absX = abs(delta.x)
        val absY = abs(delta.y)
        
        return when {
            absX < threshold && absY < threshold -> GestureDirection.NONE
            absX > absY -> {
                if (delta.x > 0) GestureDirection.RIGHT else GestureDirection.LEFT
            }
            else -> {
                if (delta.y > 0) GestureDirection.DOWN else GestureDirection.UP
            }
        }
    }
    
    enum class GestureDirection {
        NONE, UP, DOWN, LEFT, RIGHT
    }
}

/**
 * Performance monitoring for gesture system
 */
class GesturePerformanceMonitor {
    private val _performanceData = MutableStateFlow(PerformanceData())
    val performanceData: StateFlow<PerformanceData> = _performanceData.asStateFlow()
    private var gestureStartTime = 0L
    private var frameCount = 0
    private var lastFrameTime = 0L
    
    /**
     * Start monitoring a gesture
     */
    fun startGesture(gestureType: GestureType) {
        gestureStartTime = SystemClock.elapsedRealtime()
        frameCount = 0
    }
    
    /**
     * Record frame
     */
    fun recordFrame() {
        val currentTime = SystemClock.elapsedRealtime()
        
        if (lastFrameTime != 0L) {
            val frameTime = currentTime - lastFrameTime
            updatePerformanceData { data ->
                data.copy(
                    lastFrameTime = frameTime,
                    averageFrameTime = (data.averageFrameTime * frameCount + frameTime) / (frameCount + 1)
                )
            }
        }
        
        lastFrameTime = currentTime
        frameCount++
    }
    
    /**
     * End gesture monitoring
     */
    fun endGesture() {
        val gestureDuration = SystemClock.elapsedRealtime() - gestureStartTime
        updatePerformanceData { data ->
            data.copy(
                lastGestureDuration = gestureDuration,
                gesturesPerSecond = if (gestureDuration > 0) {
                    1000f / gestureDuration
                } else 0f
            )
        }
    }
    
    private fun updatePerformanceData(update: (PerformanceData) -> PerformanceData) {
        _performanceData.value = update(_performanceData.value)
    }
    
    data class PerformanceData(
        val lastFrameTime: Long = 0L,
        val averageFrameTime: Float = 0f,
        val lastGestureDuration: Long = 0L,
        val gesturesPerSecond: Float = 0f,
        val isPerformanceOptimal: Boolean = true
    ) {
        fun shouldReduceQuality(): Boolean {
            return averageFrameTime > 20f // Below 50fps
        }
    }
}