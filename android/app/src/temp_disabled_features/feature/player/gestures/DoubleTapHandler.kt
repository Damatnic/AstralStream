package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.*
import kotlin.math.abs

class DoubleTapHandler(
    private val onSingleTap: () -> Unit,
    private val onDoubleTapLeft: () -> Unit,
    private val onDoubleTapCenter: () -> Unit,
    private val onDoubleTapRight: () -> Unit,
    private val onGestureStart: (type: GestureType) -> Unit,
    private val onGestureEnd: (type: GestureType, success: Boolean) -> Unit
) {
    
    private var lastTapTime = 0L
    private var lastTapPosition = Offset.Zero
    private var tapJob: Job? = null
    
    suspend fun PointerInputScope.detectDoubleTaps(
        settings: DoubleTapSettings
    ) = coroutineScope {
        if (!settings.isEnabled) return@coroutineScope
        
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val downTime = System.currentTimeMillis()
            val downPosition = down.position
            
            // Wait for up
            val up = waitForUpOrCancellation()
            if (up == null) {
                // Cancelled, might be a long press
                return@awaitEachGesture
            }
            
            val tapDuration = System.currentTimeMillis() - downTime
            if (tapDuration > settings.maxTapDuration) {
                // Too long for a tap
                return@awaitEachGesture
            }
            
            // Check if this is a double tap
            val timeSinceLastTap = downTime - lastTapTime
            val distanceFromLastTap = (downPosition - lastTapPosition).getDistance()
            
            if (timeSinceLastTap <= settings.doubleTapTimeout && 
                distanceFromLastTap <= settings.maxDoubleTapDistance) {
                // Double tap detected
                tapJob?.cancel()
                handleDoubleTap(downPosition, settings, size)
            } else {
                // Might be a single tap
                tapJob?.cancel()
                tapJob = launch {
                    delay(settings.doubleTapTimeout.toLong())
                    handleSingleTap()
                }
            }
            
            lastTapTime = downTime
            lastTapPosition = downPosition
        }
    }
    
    private fun handleSingleTap() {
        onGestureStart(GestureType.SINGLE_TAP)
        onSingleTap()
        onGestureEnd(GestureType.SINGLE_TAP, true)
    }
    
    private fun handleDoubleTap(
        position: Offset,
        settings: DoubleTapSettings,
        screenSize: androidx.compose.ui.unit.IntSize
    ) {
        onGestureStart(GestureType.DOUBLE_TAP)
        
        val zone = determineZone(position, screenSize.width.toFloat())
        when (zone) {
            TapZone.LEFT -> onDoubleTapLeft()
            TapZone.CENTER -> onDoubleTapCenter()
            TapZone.RIGHT -> onDoubleTapRight()
        }
        
        onGestureEnd(GestureType.DOUBLE_TAP, true)
    }
    
    private fun determineZone(position: Offset, screenWidth: Float): TapZone {
        return when {
            position.x < screenWidth * 0.33f -> TapZone.LEFT
            position.x > screenWidth * 0.67f -> TapZone.RIGHT
            else -> TapZone.CENTER
        }
    }
    
    private suspend fun AwaitPointerEventScope.waitForUpOrCancellation(): PointerInputChange? {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull()
            
            if (change == null || !change.pressed) {
                return change
            }
        }
    }
    
    private fun Offset.getDistance(): Float {
        return kotlin.math.sqrt(x * x + y * y)
    }
    
    enum class TapZone {
        LEFT, CENTER, RIGHT
    }
}

data class DoubleTapSettings(
    val isEnabled: Boolean = true,
    val doubleTapTimeout: Int = 300, // milliseconds
    val maxTapDuration: Long = 200, // milliseconds
    val maxDoubleTapDistance: Float = 100f, // pixels
    val leftAction: DoubleTapAction = DoubleTapAction.SEEK_BACKWARD,
    val centerAction: DoubleTapAction = DoubleTapAction.PLAY_PAUSE,
    val rightAction: DoubleTapAction = DoubleTapAction.SEEK_FORWARD,
    val seekAmount: Int = 10 // seconds
)

enum class DoubleTapAction {
    SEEK_FORWARD,
    SEEK_BACKWARD,
    PLAY_PAUSE,
    SHOW_INFO,
    TOGGLE_FULLSCREEN,
    NONE
}