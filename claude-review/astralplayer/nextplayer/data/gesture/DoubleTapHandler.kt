package com.astralplayer.nextplayer.data.gesture

import androidx.compose.ui.geometry.Offset
import com.astralplayer.nextplayer.data.*

/**
 * Handles double tap gestures for seeking
 */
class DoubleTapHandler(
    private val settings: DoubleTapGestureSettings,
    private val screenWidth: Float,
    private val onSeek: (Long, TouchSide) -> Unit
) {
    
    private var lastTapTime = 0L
    private var lastTapPosition = Offset.Zero
    private var tapCount = 0
    
    /**
     * Processes a tap event
     */
    fun processTap(position: Offset): Boolean {
        if (!settings.isEnabled) return false
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastTap = currentTime - lastTapTime
        val distanceFromLastTap = (position - lastTapPosition).getDistance()
        
        return if (timeSinceLastTap < settings.tapTimeout && 
                   distanceFromLastTap < settings.maxTapDistance) {
            // This is a double tap
            tapCount++
            if (tapCount == 2) {
                handleDoubleTap(position)
                reset()
                true
            } else {
                false
            }
        } else {
            // This might be the first tap of a double tap
            tapCount = 1
            lastTapTime = currentTime
            lastTapPosition = position
            false
        }
    }
    
    /**
     * Handles the double tap action
     */
    private fun handleDoubleTap(position: Offset) {
        val side = determineSide(position)
        val seekAmount = when (side) {
            TouchSide.LEFT -> -settings.seekAmount
            TouchSide.RIGHT -> settings.seekAmount
            TouchSide.CENTER -> 0L // No seek in center
        }
        
        if (seekAmount != 0L) {
            onSeek(seekAmount, side)
        }
    }
    
    /**
     * Determines which side of the screen was tapped
     */
    private fun determineSide(position: Offset): TouchSide {
        val leftBoundary = screenWidth * 0.35f
        val rightBoundary = screenWidth * 0.65f
        
        return when {
            position.x < leftBoundary -> TouchSide.LEFT
            position.x > rightBoundary -> TouchSide.RIGHT
            else -> TouchSide.CENTER
        }
    }
    
    /**
     * Checks if enough time has passed to reset the tap counter
     */
    fun checkTimeout(currentTime: Long): Boolean {
        if (tapCount > 0 && currentTime - lastTapTime > settings.tapTimeout) {
            reset()
            return true
        }
        return false
    }
    
    /**
     * Resets the handler state
     */
    fun reset() {
        tapCount = 0
        lastTapTime = 0L
        lastTapPosition = Offset.Zero
    }
    
    /**
     * Gets current tap info for UI feedback
     */
    fun getTapInfo(): DoubleTapInfo {
        return DoubleTapInfo(
            tapCount = tapCount,
            lastTapPosition = lastTapPosition,
            isWaitingForSecondTap = tapCount == 1,
            side = if (tapCount > 0) determineSide(lastTapPosition) else TouchSide.CENTER
        )
    }
    
    data class DoubleTapInfo(
        val tapCount: Int,
        val lastTapPosition: Offset,
        val isWaitingForSecondTap: Boolean,
        val side: TouchSide
    )
}