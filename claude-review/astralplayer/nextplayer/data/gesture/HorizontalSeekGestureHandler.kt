package com.astralplayer.nextplayer.data.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import com.astralplayer.nextplayer.data.GestureAction
import com.astralplayer.nextplayer.data.SeekDirection
import com.astralplayer.nextplayer.data.SeekingGestureSettings
import kotlin.math.abs

/**
 * Handles horizontal seek gestures with velocity tracking and fine control
 */
class HorizontalSeekGestureHandler(
    private val screenWidth: Float,
    private val settings: SeekingGestureSettings
) {
    
    private var totalSeekAmount = 0f
    private var lastSeekTime = 0L
    private var velocityHistory = mutableListOf<Float>()
    private val maxVelocitySamples = 5
    
    /**
     * Processes horizontal drag for seeking
     */
    fun processDrag(
        dragAmount: Float,
        currentTime: Long,
        currentPosition: Offset
    ): GestureAction.Seek? {
        if (!settings.isEnabled) return null
        
        // Calculate velocity
        val deltaTime = if (lastSeekTime > 0) currentTime - lastSeekTime else 16L
        val velocity = if (deltaTime > 0) dragAmount / deltaTime.toFloat() * 1000f else 0f
        
        // Track velocity history
        velocityHistory.add(velocity)
        if (velocityHistory.size > maxVelocitySamples) {
            velocityHistory.removeAt(0)
        }
        
        // Calculate average velocity for smoother control
        val avgVelocity = velocityHistory.average().toFloat()
        
        // Apply sensitivity and fine seek mode
        val adjustedDragAmount = if (settings.enableFineSeek && abs(avgVelocity) < settings.fineSeekThreshold) {
            dragAmount * settings.sensitivity * 0.5f // Fine control mode
        } else {
            dragAmount * settings.sensitivity
        }
        
        totalSeekAmount += adjustedDragAmount
        lastSeekTime = currentTime
        
        // Convert drag to seek time
        val seekDelta = (adjustedDragAmount / screenWidth) * 60000L // 60 seconds for full screen width
        
        return if (abs(seekDelta) > 100) { // Minimum threshold to avoid tiny seeks
            val direction = if (seekDelta > 0) SeekDirection.FORWARD else SeekDirection.BACKWARD
            GestureAction.Seek(kotlin.math.abs(seekDelta.toLong()), direction)
        } else null
    }
    
    /**
     * Finalizes the seek gesture
     */
    fun finalizeDrag(totalDrag: Offset, velocity: Float): GestureAction.Seek? {
        if (!settings.isEnabled || abs(totalDrag.x) < settings.minimumSwipeDistance) return null
        
        // Calculate total seek time based on full drag distance
        val totalSeekTime = (totalDrag.x / screenWidth) * 60000L * settings.sensitivity
        
        // Apply velocity boost for fast swipes
        val velocityBoost = if (abs(velocity) > 1000f) {
            (velocity / 1000f).coerceIn(-2f, 2f)
        } else 0f
        
        val finalSeekTime = (totalSeekTime * (1f + velocityBoost * 0.2f)).toLong()
        
        val direction = if (finalSeekTime > 0) SeekDirection.FORWARD else SeekDirection.BACKWARD
        return GestureAction.Seek(kotlin.math.abs(finalSeekTime), direction)
    }
    
    /**
     * Resets the handler state
     */
    fun reset() {
        totalSeekAmount = 0f
        lastSeekTime = 0L
        velocityHistory.clear()
    }
    
    /**
     * Gets seek preview information
     */
    fun getSeekPreviewInfo(currentPosition: Long, videoDuration: Long): SeekPreviewInfo {
        val seekPosition = (currentPosition + totalSeekAmount.toLong()).coerceIn(0L, videoDuration)
        val seekPercentage = if (videoDuration > 0) seekPosition.toFloat() / videoDuration else 0f
        val avgVelocity = if (velocityHistory.isNotEmpty()) velocityHistory.average().toFloat() else 0f
        
        return SeekPreviewInfo(
            seekPosition = seekPosition,
            seekPercentage = seekPercentage,
            isDragging = totalSeekAmount != 0f,
            showThumbnail = settings.showPreviewThumbnails,
            showTimeIndicator = settings.showTimeIndicator,
            isForward = totalSeekAmount > 0f,
            velocity = avgVelocity,
            seekDelta = totalSeekAmount.toLong(),
            targetPosition = seekPosition
        )
    }
    
    data class SeekPreviewInfo(
        val seekPosition: Long,
        val seekPercentage: Float,
        val isDragging: Boolean,
        val showThumbnail: Boolean,
        val showTimeIndicator: Boolean,
        val isForward: Boolean = true,
        val velocity: Float = 0f,
        val seekDelta: Long = 0L,
        val targetPosition: Long = seekPosition
    )
}