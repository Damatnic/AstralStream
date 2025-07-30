package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs
import kotlin.math.sqrt

class HorizontalSwipeHandler(
    private val onSeekStart: () -> Unit,
    private val onSeek: (deltaMs: Long) -> Unit,
    private val onSeekEnd: (totalDeltaMs: Long) -> Unit,
    private val onGestureStart: (type: GestureType) -> Unit,
    private val onGestureEnd: (type: GestureType, success: Boolean) -> Unit
) {
    
    private var isActive = false
    private var startPosition = Offset.Zero
    private var accumulatedSeekMs = 0L
    private var lastSeekMs = 0L
    
    suspend fun PointerInputScope.detectHorizontalSwipes(
        settings: SeekGestureSettings
    ) = coroutineScope {
        if (!settings.isEnabled) return@coroutineScope
        
        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            startGesture(firstDown, size)
            
            var currentPointer = firstDown
            do {
                val event = awaitPointerEvent()
                currentPointer = event.changes.firstOrNull { !it.isConsumed } ?: currentPointer
                
                if (isActive) {
                    updateGesture(currentPointer, settings, size)
                    currentPointer.consume()
                }
                
            } while (event.changes.any { it.pressed })
            
            endGesture()
        }
    }
    
    private fun startGesture(
        firstDown: PointerInputChange,
        screenSize: androidx.compose.ui.unit.IntSize
    ) {
        startPosition = firstDown.position
        accumulatedSeekMs = 0L
        lastSeekMs = 0L
        isActive = false
    }
    
    private fun updateGesture(
        pointer: PointerInputChange,
        settings: SeekGestureSettings,
        screenSize: androidx.compose.ui.unit.IntSize
    ) {
        val currentPosition = pointer.position
        val deltaX = currentPosition.x - startPosition.x
        val deltaY = currentPosition.y - startPosition.y
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
        
        if (!isActive && distance >= settings.minimumSwipeDistance) {
            val horizontalRatio = abs(deltaX) / (abs(deltaY) + 1f)
            if (horizontalRatio >= 2.0f) { // Must be more horizontal than vertical
                isActive = true
                onGestureStart(GestureType.HORIZONTAL_SEEK)
                onSeekStart()
            }
        }
        
        if (!isActive) return
        
        // Calculate seek amount based on horizontal distance
        val percentageMoved = deltaX / screenSize.width
        val seekRangeMs = if (settings.dynamicSeekRange) {
            calculateDynamicSeekRange(abs(deltaX), screenSize.width.toFloat())
        } else {
            settings.seekRangeSeconds * 1000
        }
        
        val seekMs = (percentageMoved * seekRangeMs * settings.sensitivity).toLong()
        val deltaSeekMs = seekMs - lastSeekMs
        
        if (abs(deltaSeekMs) >= settings.seekStepMs) {
            accumulatedSeekMs += deltaSeekMs
            lastSeekMs = seekMs
            onSeek(deltaSeekMs)
        }
    }
    
    private fun endGesture() {
        if (isActive) {
            onSeekEnd(accumulatedSeekMs)
            onGestureEnd(GestureType.HORIZONTAL_SEEK, abs(accumulatedSeekMs) > 1000)
        }
        
        isActive = false
        accumulatedSeekMs = 0L
        lastSeekMs = 0L
    }
    
    private fun calculateDynamicSeekRange(distance: Float, screenWidth: Float): Int {
        return when {
            distance < screenWidth * 0.1f -> 10_000 // 10 seconds for small swipes
            distance < screenWidth * 0.3f -> 30_000 // 30 seconds for medium swipes
            distance < screenWidth * 0.5f -> 60_000 // 60 seconds for large swipes
            else -> 120_000 // 2 minutes for very large swipes
        }
    }
}

data class SeekGestureSettings(
    val isEnabled: Boolean = true,
    val minimumSwipeDistance: Float = 50f,
    val sensitivity: Float = 1.0f,
    val seekRangeSeconds: Int = 30,
    val seekStepMs: Long = 100,
    val dynamicSeekRange: Boolean = true,
    val showPreview: Boolean = true,
    val hapticFeedback: Boolean = true
)