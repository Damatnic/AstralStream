package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Advanced long press seek handler with MX Player-style speed progression
 */
class AdvancedLongPressSeekHandler(
    private val onSeekStart: (position: Offset, direction: SeekDirection) -> Unit,
    private val onSeekUpdate: (position: Long, speed: Float, direction: SeekDirection, speedLevel: Int) -> Unit,
    private val onSeekEnd: (finalPosition: Long, success: Boolean) -> Unit,
    private val onSpeedChange: (newSpeed: Float, speedLevel: Int) -> Unit,
    private val onDirectionChange: (newDirection: SeekDirection) -> Unit,
    private val getCurrentPosition: () -> Long,
    private val getDuration: () -> Long,
    private val onHapticFeedback: (intensity: HapticIntensity) -> Unit
) {
    
    private var isActive = false
    private var startPosition = Offset.Zero
    private var currentPosition = Offset.Zero
    private var startTime = 0L
    private var startVideoPosition = 0L
    private var currentVideoPosition = 0L
    
    private var currentSpeed = 1f
    private var currentSpeedLevel = 0
    private var currentDirection = SeekDirection.FORWARD
    private var lastDirectionChange = 0L
    
    private var speedAccelerationJob: Job? = null
    private var continuousSeekJob: Job? = null
    private var speedHistory = mutableListOf<Float>()
    
    suspend fun PointerInputScope.detectLongPressSeek(
        settings: LongPressGestureSettings
    ) = coroutineScope {
        awaitEachGesture {
            if (!settings.isEnabled) return@awaitEachGesture
            
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            
            // Start long press detection
            val longPressJob = launch {
                delay(settings.triggerDuration)
                if (isActive) return@launch // Already activated by movement
                
                startLongPressSeek(firstDown, settings, size)
            }
            
            var currentPointer = firstDown
            var hasMovedSignificantly = false
            
            do {
                val event = awaitPointerEvent()
                currentPointer = event.changes.firstOrNull { !it.isConsumed } ?: currentPointer
                
                // Check for significant movement that might indicate a different gesture
                val distance = (currentPointer.position - firstDown.position).getDistance()
                if (!hasMovedSignificantly && distance > settings.directionChangeThreshold) {
                    hasMovedSignificantly = true
                    longPressJob.cancel() // Cancel long press if user is clearly doing something else
                }
                
                if (isActive) {
                    updateLongPressSeek(currentPointer, settings, size)
                    currentPointer.consume()
                }
                
            } while (event.changes.any { !it.isConsumed })
            
            longPressJob.cancel()
            endLongPressSeek(settings)
        }
    }
    
    private fun CoroutineScope.startLongPressSeek(
        firstDown: PointerInputChange,
        settings: LongPressGestureSettings,
        screenSize: androidx.compose.ui.unit.IntSize
    ) {
        if (isActive) return
        
        isActive = true
        startPosition = firstDown.position
        currentPosition = firstDown.position
        startTime = System.currentTimeMillis()
        startVideoPosition = getCurrentPosition()
        currentVideoPosition = startVideoPosition
        
        // Determine initial direction based on touch position
        val screenWidth = screenSize.width.toFloat()
        currentDirection = if (firstDown.position.x > screenWidth / 2) {
            SeekDirection.FORWARD
        } else {
            SeekDirection.BACKWARD
        }
        
        // Initialize speed progression
        currentSpeed = settings.speedProgression.firstOrNull() ?: 1f
        currentSpeedLevel = 0
        speedHistory.clear()
        speedHistory.add(currentSpeed)
        
        // Provide initial haptic feedback
        onHapticFeedback(HapticIntensity.STRONG)
        
        // Notify start
        onSeekStart(startPosition, currentDirection)
        
        // Start speed acceleration
        startSpeedAcceleration(settings)
        
        // Start continuous seeking
        startContinuousSeeking(settings)
    }
    
    private fun CoroutineScope.startSpeedAcceleration(settings: LongPressGestureSettings) {
        speedAccelerationJob = launch {
            while (isActive && currentSpeedLevel < settings.speedProgression.size - 1) {
                delay(settings.speedAccelerationInterval)
                
                if (isActive) {
                    currentSpeedLevel++
                    val newSpeed = settings.speedProgression.getOrNull(currentSpeedLevel) ?: settings.maxSpeed
                    
                    if (newSpeed != currentSpeed) {
                        currentSpeed = newSpeed.coerceAtMost(settings.maxSpeed)
                        speedHistory.add(currentSpeed)
                        
                        // Provide haptic feedback for speed change
                        onHapticFeedback(HapticIntensity.MEDIUM)
                        
                        // Notify speed change
                        onSpeedChange(currentSpeed, currentSpeedLevel)
                    }
                }
            }
        }
    }
    
    private fun CoroutineScope.startContinuousSeeking(settings: LongPressGestureSettings) {
        continuousSeekJob = launch {
            var lastSeekTime = System.currentTimeMillis()
            
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastSeekTime
                
                if (deltaTime >= settings.continuousSeekInterval) {
                    performSeekStep(deltaTime, settings)
                    lastSeekTime = currentTime
                }
                
                delay(settings.continuousSeekInterval)
            }
        }
    }
    
    private fun updateLongPressSeek(
        pointer: PointerInputChange,
        settings: LongPressGestureSettings,
        screenSize: androidx.compose.ui.unit.IntSize
    ) {
        if (!isActive) return
        
        currentPosition = pointer.position
        
        // Check for direction change based on horizontal movement
        if (settings.enableDirectionChange) {
            val horizontalDelta = currentPosition.x - startPosition.x
            val currentTime = System.currentTimeMillis()
            
            if (abs(horizontalDelta) > settings.directionChangeThreshold &&
                currentTime - lastDirectionChange > 500L) { // Prevent rapid direction changes
                
                val newDirection = if (horizontalDelta > 0) SeekDirection.FORWARD else SeekDirection.BACKWARD
                
                if (newDirection != currentDirection) {
                    currentDirection = newDirection
                    lastDirectionChange = currentTime
                    
                    // Provide haptic feedback for direction change
                    onHapticFeedback(HapticIntensity.MEDIUM)
                    
                    // Notify direction change
                    onDirectionChange(currentDirection)
                }
            }
            
            // Adjust speed based on swipe intensity
            val swipeIntensity = abs(horizontalDelta) / (screenSize.width.toFloat() / 2)
            val speedMultiplier = 1f + (swipeIntensity * 2f).coerceAtMost(3f)
            val adjustedSpeed = (currentSpeed * speedMultiplier).coerceAtMost(settings.maxSpeed)
            
            if (abs(adjustedSpeed - currentSpeed) > settings.speedChangeThreshold) {
                currentSpeed = adjustedSpeed
                onSpeedChange(currentSpeed, currentSpeedLevel)
            }
        }
    }
    
    private fun performSeekStep(deltaTime: Long, settings: LongPressGestureSettings) {
        val videoDuration = getDuration()
        if (videoDuration <= 0) return
        
        // Calculate seek amount based on current speed and time delta
        val seekAmountMs = calculateSeekAmount(currentSpeed, deltaTime)
        
        // Apply direction
        val directedSeekAmount = when (currentDirection) {
            SeekDirection.FORWARD -> seekAmountMs
            SeekDirection.BACKWARD -> -seekAmountMs
            SeekDirection.NONE -> 0L
        }
        
        // Calculate new position
        val newPosition = (currentVideoPosition + directedSeekAmount).coerceIn(0L, videoDuration)
        
        // Only update if position actually changed
        if (newPosition != currentVideoPosition) {
            currentVideoPosition = newPosition
            
            // Notify update
            onSeekUpdate(currentVideoPosition, currentSpeed, currentDirection, currentSpeedLevel)
        }
    }
    
    private fun calculateSeekAmount(speed: Float, deltaTimeMs: Long): Long {
        // Base seek amount per second at 1x speed
        val baseSeekPerSecond = when {
            speed <= 1f -> 1000L    // 1 second at 1x
            speed <= 2f -> 2000L    // 2 seconds at 2x
            speed <= 4f -> 4000L    // 4 seconds at 4x
            speed <= 8f -> 8000L    // 8 seconds at 8x
            speed <= 16f -> 16000L  // 16 seconds at 16x
            else -> 32000L          // 32 seconds at 32x
        }
        
        // Scale by actual time delta and speed
        return (baseSeekPerSecond * speed * deltaTimeMs / 1000f).toLong()
    }
    
    private fun endLongPressSeek(settings: LongPressGestureSettings) {
        if (!isActive) return
        
        // Cancel jobs
        speedAccelerationJob?.cancel()
        continuousSeekJob?.cancel()
        
        val success = isActive && (System.currentTimeMillis() - startTime) > settings.triggerDuration
        val finalPosition = currentVideoPosition
        
        // Provide end haptic feedback
        if (success) {
            onHapticFeedback(HapticIntensity.LIGHT)
        }
        
        // Notify end
        onSeekEnd(finalPosition, success)
        
        // Reset state
        resetState()
    }
    
    private fun resetState() {
        isActive = false
        startPosition = Offset.Zero
        currentPosition = Offset.Zero
        startTime = 0L
        startVideoPosition = 0L
        currentVideoPosition = 0L
        currentSpeed = 1f
        currentSpeedLevel = 0
        currentDirection = SeekDirection.FORWARD
        lastDirectionChange = 0L
        speedHistory.clear()
        
        speedAccelerationJob?.cancel()
        continuousSeekJob?.cancel()
        speedAccelerationJob = null
        continuousSeekJob = null
    }
    
    // Public methods for external control
    fun forceEndSeek() {
        if (isActive) {
            endLongPressSeek(LongPressGestureSettings()) // Use default settings for cleanup
        }
    }
    
    fun getCurrentSeekState(): LongPressSeekState {
        return LongPressSeekState(
            isActive = isActive,
            initialTouchPosition = startPosition,
            currentTouchPosition = currentPosition,
            startPosition = startVideoPosition,
            currentPosition = currentVideoPosition,
            seekDirection = currentDirection,
            speedMultiplier = currentSpeed,
            seekPreviewPosition = currentVideoPosition,
            originalPosition = startVideoPosition,
            seekSpeed = currentSpeed,
            direction = currentDirection,
            showSpeedIndicator = isActive,
            accelerationLevel = currentSpeedLevel,
            isAccelerating = isActive,
            defaultSpeed = 2f,
            maxSpeed = 32f,
            isRewindMode = currentDirection == SeekDirection.BACKWARD,
            elapsedTime = if (isActive) System.currentTimeMillis() - startTime else 0L
        )
    }
}

// LongPressSeekState is imported from separate file

/**
 * Speed progression calculator for different seeking patterns
 */
class SpeedProgressionCalculator {
    
    companion object {
        /**
         * MX Player style progression: 1x → 2x → 4x → 8x → 16x → 32x
         */
        fun getMXPlayerProgression(): List<Float> = listOf(1f, 2f, 4f, 8f, 16f, 32f)
        
        /**
         * Smooth progression: 1x → 1.5x → 2x → 3x → 4x → 6x → 8x
         */
        fun getSmoothProgression(): List<Float> = listOf(1f, 1.5f, 2f, 3f, 4f, 6f, 8f)
        
        /**
         * Linear progression: 1x → 2x → 3x → 4x → 5x → 6x
         */
        fun getLinearProgression(): List<Float> = listOf(1f, 2f, 3f, 4f, 5f, 6f)
        
        /**
         * Exponential progression: 1x → 2x → 4x → 8x → 16x → 32x → 64x
         */
        fun getExponentialProgression(): List<Float> = listOf(1f, 2f, 4f, 8f, 16f, 32f, 64f)
        
        /**
         * Custom progression based on user preferences
         */
        fun getCustomProgression(
            startSpeed: Float = 1f,
            maxSpeed: Float = 32f,
            steps: Int = 6,
            progressionType: ProgressionType = ProgressionType.EXPONENTIAL
        ): List<Float> {
            if (steps <= 1) return listOf(startSpeed)
            
            val progression = mutableListOf<Float>()
            
            when (progressionType) {
                ProgressionType.LINEAR -> {
                    val step = (maxSpeed - startSpeed) / (steps - 1)
                    for (i in 0 until steps) {
                        progression.add(startSpeed + i * step)
                    }
                }
                ProgressionType.EXPONENTIAL -> {
                    val ratio = (maxSpeed / startSpeed).pow(1f / (steps - 1))
                    for (i in 0 until steps) {
                        progression.add(startSpeed * ratio.pow(i.toFloat()))
                    }
                }
                ProgressionType.LOGARITHMIC -> {
                    for (i in 0 until steps) {
                        val t = i.toFloat() / (steps - 1)
                        val logValue = ln(1 + t * (E - 1)).toFloat()
                        progression.add(startSpeed + (maxSpeed - startSpeed) * logValue)
                    }
                }
                ProgressionType.SMOOTH_STEP -> {
                    for (i in 0 until steps) {
                        val t = i.toFloat() / (steps - 1)
                        val smoothValue = t * t * (3 - 2 * t)
                        progression.add(startSpeed + (maxSpeed - startSpeed) * smoothValue)
                    }
                }
            }
            
            return progression
        }
    }
    
    enum class ProgressionType {
        LINEAR, EXPONENTIAL, LOGARITHMIC, SMOOTH_STEP
    }
}

/**
 * Long press seek analytics for performance monitoring and adaptive learning
 */
class LongPressSeekAnalytics {
    private val seekData = mutableListOf<LongPressSeekData>()
    
    data class LongPressSeekData(
        val startTime: Long,
        val endTime: Long,
        val startPosition: Long,
        val endPosition: Long,
        val maxSpeed: Float,
        val averageSpeed: Float,
        val directionChanges: Int,
        val success: Boolean,
        val accuracy: Float // How close to intended position
    )
    
    fun recordSeekSession(
        startTime: Long,
        endTime: Long,
        startPosition: Long,
        endPosition: Long,
        speedHistory: List<Float>,
        directionChanges: Int,
        success: Boolean
    ) {
        val maxSpeed = speedHistory.maxOrNull() ?: 1f
        val averageSpeed = speedHistory.average().toFloat()
        val accuracy = calculateAccuracy(startPosition, endPosition, endTime - startTime)
        
        val data = LongPressSeekData(
            startTime, endTime, startPosition, endPosition,
            maxSpeed, averageSpeed, directionChanges, success, accuracy
        )
        
        seekData.add(data)
        
        // Limit data size
        if (seekData.size > 1000) {
            seekData.removeAt(0)
        }
    }
    
    private fun calculateAccuracy(startPos: Long, endPos: Long, duration: Long): Float {
        // Simple accuracy calculation based on seek efficiency
        val seekDistance = abs(endPos - startPos)
        val expectedEfficiency = seekDistance.toFloat() / duration // ms per ms of video
        return (expectedEfficiency * 1000f).coerceAtMost(1f)
    }
    
    fun getAverageAccuracy(): Float {
        return if (seekData.isNotEmpty()) {
            seekData.map { it.accuracy }.average().toFloat()
        } else {
            1f
        }
    }
    
    fun getPreferredMaxSpeed(): Float {
        return if (seekData.isNotEmpty()) {
            seekData.map { it.maxSpeed }.average().toFloat()
        } else {
            8f
        }
    }
    
    fun getAverageDirectionChanges(): Float {
        return if (seekData.isNotEmpty()) {
            seekData.map { it.directionChanges }.average().toFloat()
        } else {
            0f
        }
    }
    
    fun getSuccessRate(): Float {
        return if (seekData.isNotEmpty()) {
            seekData.count { it.success }.toFloat() / seekData.size
        } else {
            1f
        }
    }
    
    fun getSuggestedSettings(currentSettings: LongPressGestureSettings): LongPressGestureSettings {
        if (seekData.isEmpty()) return currentSettings
        
        val accuracy = getAverageAccuracy()
        val preferredMaxSpeed = getPreferredMaxSpeed()
        val avgDirectionChanges = getAverageDirectionChanges()
        
        return currentSettings.copy(
            maxSpeed = preferredMaxSpeed.coerceIn(2f, 64f),
            speedAccelerationInterval = if (accuracy < 0.6f) {
                (currentSettings.speedAccelerationInterval * 1.2f).toLong() // Slower acceleration
            } else {
                currentSettings.speedAccelerationInterval
            },
            directionChangeThreshold = if (avgDirectionChanges > 3f) {
                currentSettings.directionChangeThreshold * 1.2f // Make direction changes harder
            } else {
                currentSettings.directionChangeThreshold
            }
        )
    }
}

// HapticIntensity is imported from EnhancedHapticFeedbackSystem