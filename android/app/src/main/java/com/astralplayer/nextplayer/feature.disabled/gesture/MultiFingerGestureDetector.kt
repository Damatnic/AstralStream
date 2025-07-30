package com.astralplayer.nextplayer.feature.gesture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MultiFingerGestureDetector {
    
    private val _gestureState = MutableStateFlow(MultiFingerGestureState())
    val gestureState: StateFlow<MultiFingerGestureState> = _gestureState.asStateFlow()
    
    var onThreeFingerSwipe: ((direction: SwipeDirection) -> Unit)? = null
    var onPinchZoom: ((scale: Float) -> Unit)? = null
    
    fun detectThreeFingerSwipe(deltaX: Float, deltaY: Float) {
        val threshold = 100f
        
        if (kotlin.math.abs(deltaX) > threshold || kotlin.math.abs(deltaY) > threshold) {
            val direction = when {
                kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) -> {
                    if (deltaX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
                }
                else -> {
                    if (deltaY > 0) SwipeDirection.DOWN else SwipeDirection.UP
                }
            }
            onThreeFingerSwipe?.invoke(direction)
        }
    }
}

data class MultiFingerGestureState(
    val isThreeFingerActive: Boolean = false
)

enum class SwipeDirection {
    UP, DOWN, LEFT, RIGHT
}