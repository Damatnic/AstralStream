package com.astralplayer.nextplayer.data.gesture

import androidx.compose.ui.geometry.Offset
import com.astralplayer.nextplayer.data.*
import kotlinx.coroutines.*
import kotlin.math.abs

/**
 * MX Player-style long press seek handler with speed progression
 */
class LongPressSeekHandler(
    private val settings: LongPressGestureSettings,
    private val screenWidth: Float,
    private val onSpeedUpdate: (Float, SeekDirection) -> Unit,
    private val onSeekUpdate: (Long) -> Unit,
    private val onEnd: () -> Unit
) {
    
    private var seekJob: Job? = null
    private var speedProgressionJob: Job? = null
    private var currentSpeedIndex = 0
    private var currentDirection = SeekDirection.FORWARD
    private var isActive = false
    private var startTime = 0L
    private var totalSeekAmount = 0L
    private var lastUpdateTime = 0L
    
    /**
     * Starts the long press seek
     */
    fun start(position: Offset) {
        if (!settings.isEnabled || isActive) return
        
        isActive = true
        startTime = System.currentTimeMillis()
        lastUpdateTime = startTime
        currentSpeedIndex = 0
        currentDirection = if (position.x > screenWidth / 2) SeekDirection.FORWARD else SeekDirection.BACKWARD
        totalSeekAmount = 0L
        
        // Start with the first speed
        val initialSpeed = settings.speedProgression.getOrNull(0) ?: 1f
        onSpeedUpdate(initialSpeed, currentDirection)
        
        // Start seeking
        startSeeking(initialSpeed)
        
        // Start speed progression
        startSpeedProgression()
    }
    
    /**
     * Handles drag during long press for direction change
     */
    fun handleDrag(dragAmount: Offset, currentPosition: Offset): Boolean {
        if (!isActive || !settings.enableDirectionChange) return false
        
        val horizontalDrag = abs(dragAmount.x)
        if (horizontalDrag < settings.directionChangeThreshold) return true
        
        // Determine new direction based on drag
        val newDirection = if (dragAmount.x > 0) SeekDirection.FORWARD else SeekDirection.BACKWARD
        
        if (newDirection != currentDirection) {
            currentDirection = newDirection
            val currentSpeed = settings.speedProgression.getOrNull(currentSpeedIndex) ?: 1f
            onSpeedUpdate(currentSpeed, currentDirection)
            
            // Restart seeking with new direction
            seekJob?.cancel()
            startSeeking(currentSpeed)
        }
        
        return true // Consume the drag event
    }
    
    /**
     * Starts the seeking coroutine
     */
    private fun startSeeking(speed: Float) {
        seekJob?.cancel()
        seekJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastUpdateTime
                lastUpdateTime = currentTime
                
                // Calculate seek amount based on speed and direction
                val seekAmount = (deltaTime * speed * if (currentDirection == SeekDirection.FORWARD) 1 else -1).toLong()
                totalSeekAmount += seekAmount
                
                // Update seek position
                onSeekUpdate(seekAmount)
                
                // Update at ~30 FPS
                delay(33)
            }
        }
    }
    
    /**
     * Starts speed progression coroutine
     */
    private fun startSpeedProgression() {
        speedProgressionJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && currentSpeedIndex < settings.speedProgression.size - 1) {
                delay(settings.speedAccelerationInterval)
                
                if (isActive) {
                    currentSpeedIndex++
                    val newSpeed = settings.speedProgression[currentSpeedIndex]
                    
                    // Check max speed limit
                    if (newSpeed <= settings.maxSpeed) {
                        onSpeedUpdate(newSpeed, currentDirection)
                        
                        // Restart seeking with new speed
                        seekJob?.cancel()
                        startSeeking(newSpeed)
                    }
                }
            }
        }
    }
    
    /**
     * Stops the long press seek
     */
    fun stop() {
        if (!isActive) return
        
        isActive = false
        seekJob?.cancel()
        speedProgressionJob?.cancel()
        seekJob = null
        speedProgressionJob = null
        
        onEnd()
    }
    
    /**
     * Gets current seek info for UI display
     */
    fun getSeekInfo(): LongPressSeekInfo {
        val elapsedTime = if (isActive) System.currentTimeMillis() - startTime else 0L
        val currentSpeed = settings.speedProgression.getOrNull(currentSpeedIndex) ?: 1f
        
        return LongPressSeekInfo(
            isActive = isActive,
            currentSpeed = currentSpeed,
            currentSpeedIndex = currentSpeedIndex,
            maxSpeedIndex = settings.speedProgression.size - 1,
            direction = currentDirection,
            totalSeekAmount = totalSeekAmount,
            elapsedTime = elapsedTime,
            speedProgression = settings.speedProgression
        )
    }
    
    /**
     * Cancels the long press seek
     */
    fun cancel() {
        stop()
    }
    
    data class LongPressSeekInfo(
        val isActive: Boolean,
        val currentSpeed: Float,
        val currentSpeedIndex: Int,
        val maxSpeedIndex: Int,
        val direction: SeekDirection,
        val totalSeekAmount: Long,
        val elapsedTime: Long,
        val speedProgression: List<Float>,
        val originalSpeed: Float = 1f,
        val wasPlaying: Boolean = false
    ) {
        val speedPercentage: Float
            get() = if (maxSpeedIndex > 0) currentSpeedIndex.toFloat() / maxSpeedIndex else 0f
        
        val formattedSpeed: String
            get() = "${currentSpeed}x"
        
        val nextSpeed: Float?
            get() = speedProgression.getOrNull(currentSpeedIndex + 1)
        
        val timeUntilNextSpeed: Long
            get() = if (currentSpeedIndex < maxSpeedIndex) {
                ((currentSpeedIndex + 1) * 1000L) - elapsedTime
            } else 0L
    }
}