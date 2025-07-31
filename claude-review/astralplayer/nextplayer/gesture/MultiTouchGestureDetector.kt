package com.astralplayer.nextplayer.gesture

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.VelocityTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Advanced multi-touch gesture detector supporting up to 4 simultaneous fingers
 */
class MultiTouchGestureDetector @Inject constructor(
    private val context: Context
) {
    private var velocityTracker: VelocityTracker? = null
    private val touchPoints = mutableMapOf<Int, TouchPoint>()
    private var lastGestureTime = 0L
    private var gestureStartTime = 0L
    
    private val _currentGesture = MutableStateFlow<GestureType?>(null)
    val currentGesture: StateFlow<GestureType?> = _currentGesture
    
    fun detectGesture(event: MotionEvent): GestureResult {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.clear()
                velocityTracker = velocityTracker ?: VelocityTracker.obtain()
                velocityTracker?.addMovement(event)
                gestureStartTime = System.currentTimeMillis()
                
                val pointerId = event.getPointerId(0)
                touchPoints[pointerId] = TouchPoint(
                    id = pointerId,
                    startX = event.x,
                    startY = event.y,
                    currentX = event.x,
                    currentY = event.y
                )
            }
            
            MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                touchPoints[pointerId] = TouchPoint(
                    id = pointerId,
                    startX = event.getX(pointerIndex),
                    startY = event.getY(pointerIndex),
                    currentX = event.getX(pointerIndex),
                    currentY = event.getY(pointerIndex)
                )
            }
            
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    touchPoints[pointerId]?.apply {
                        currentX = event.getX(i)
                        currentY = event.getY(i)
                    }
                }
                
                return analyzeGesture(event)
            }
            
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                touchPoints.remove(pointerId)
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val result = finalizeGesture(event)
                cleanup()
                return result
            }
        }
        
        return GestureResult.NotRecognized
    }
    
    private fun analyzeGesture(event: MotionEvent): GestureResult {
        val fingerCount = touchPoints.size
        
        return when (fingerCount) {
            1 -> analyzeSingleFingerGesture()
            2 -> analyzeTwoFingerGesture()
            3 -> analyzeThreeFingerGesture()
            4 -> analyzeFourFingerGesture()
            else -> GestureResult.NotRecognized
        }
    }
    
    private fun analyzeSingleFingerGesture(): GestureResult {
        val touch = touchPoints.values.first()
        val deltaX = touch.currentX - touch.startX
        val deltaY = touch.currentY - touch.startY
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
        
        return when {
            distance < TOUCH_SLOP -> {
                // Potential tap - wait for ACTION_UP
                GestureResult.NotRecognized
            }
            abs(deltaX) > abs(deltaY) * 2 -> {
                // Horizontal swipe
                _currentGesture.value = if (deltaX > 0) {
                    GestureType.SWIPE_RIGHT
                } else {
                    GestureType.SWIPE_LEFT
                }
                GestureResult.Recognized(
                    gesture = _currentGesture.value!!,
                    data = GestureData(
                        distance = distance,
                        velocity = calculateVelocity(),
                        angle = atan2(deltaY, deltaX)
                    )
                )
            }
            abs(deltaY) > abs(deltaX) * 2 -> {
                // Vertical swipe
                _currentGesture.value = if (deltaY > 0) {
                    GestureType.SWIPE_DOWN
                } else {
                    GestureType.SWIPE_UP
                }
                GestureResult.Recognized(
                    gesture = _currentGesture.value!!,
                    data = GestureData(
                        distance = distance,
                        velocity = calculateVelocity(),
                        angle = atan2(deltaY, deltaX)
                    )
                )
            }
            else -> GestureResult.NotRecognized
        }
    }
    
    private fun analyzeTwoFingerGesture(): GestureResult {
        val touches = touchPoints.values.toList()
        if (touches.size != 2) return GestureResult.NotRecognized
        
        val touch1 = touches[0]
        val touch2 = touches[1]
        
        // Calculate pinch/zoom
        val startDistance = calculateDistance(
            touch1.startX, touch1.startY,
            touch2.startX, touch2.startY
        )
        val currentDistance = calculateDistance(
            touch1.currentX, touch1.currentY,
            touch2.currentX, touch2.currentY
        )
        
        val scaleFactor = currentDistance / startDistance
        
        return when {
            scaleFactor > 1.1f -> {
                _currentGesture.value = GestureType.PINCH_ZOOM_IN
                GestureResult.Recognized(
                    gesture = GestureType.PINCH_ZOOM_IN,
                    data = GestureData(scaleFactor = scaleFactor)
                )
            }
            scaleFactor < 0.9f -> {
                _currentGesture.value = GestureType.PINCH_ZOOM_OUT
                GestureResult.Recognized(
                    gesture = GestureType.PINCH_ZOOM_OUT,
                    data = GestureData(scaleFactor = scaleFactor)
                )
            }
            else -> {
                // Check for two-finger swipe
                analyzeTwoFingerSwipe(touch1, touch2)
            }
        }
    }
    
    private fun analyzeTwoFingerSwipe(touch1: TouchPoint, touch2: TouchPoint): GestureResult {
        val avgDeltaX = ((touch1.currentX - touch1.startX) + (touch2.currentX - touch2.startX)) / 2
        val avgDeltaY = ((touch1.currentY - touch1.startY) + (touch2.currentY - touch2.startY)) / 2
        val distance = sqrt(avgDeltaX * avgDeltaX + avgDeltaY * avgDeltaY)
        
        return when {
            distance < TOUCH_SLOP -> GestureResult.NotRecognized
            abs(avgDeltaX) > abs(avgDeltaY) -> {
                _currentGesture.value = if (avgDeltaX > 0) {
                    GestureType.TWO_FINGER_SWIPE_RIGHT
                } else {
                    GestureType.TWO_FINGER_SWIPE_LEFT
                }
                GestureResult.Recognized(
                    gesture = _currentGesture.value!!,
                    data = GestureData(distance = distance)
                )
            }
            else -> {
                _currentGesture.value = if (avgDeltaY > 0) {
                    GestureType.TWO_FINGER_SWIPE_DOWN
                } else {
                    GestureType.TWO_FINGER_SWIPE_UP
                }
                GestureResult.Recognized(
                    gesture = _currentGesture.value!!,
                    data = GestureData(distance = distance)
                )
            }
        }
    }
    
    private fun analyzeThreeFingerGesture(): GestureResult {
        val touches = touchPoints.values.toList()
        if (touches.size != 3) return GestureResult.NotRecognized
        
        // Calculate average movement
        val avgDeltaX = touches.map { it.currentX - it.startX }.average()
        val avgDeltaY = touches.map { it.currentY - it.startY }.average()
        val distance = sqrt(avgDeltaX * avgDeltaX + avgDeltaY * avgDeltaY)
        
        return when {
            distance < TOUCH_SLOP -> {
                // Three-finger tap
                _currentGesture.value = GestureType.THREE_FINGER_TAP
                GestureResult.Recognized(
                    gesture = GestureType.THREE_FINGER_TAP,
                    data = GestureData()
                )
            }
            abs(avgDeltaX) > abs(avgDeltaY) -> {
                _currentGesture.value = if (avgDeltaX > 0) {
                    GestureType.THREE_FINGER_SWIPE_RIGHT
                } else {
                    GestureType.THREE_FINGER_SWIPE_LEFT
                }
                GestureResult.Recognized(
                    gesture = _currentGesture.value!!,
                    data = GestureData(distance = distance)
                )
            }
            else -> {
                _currentGesture.value = if (avgDeltaY > 0) {
                    GestureType.THREE_FINGER_SWIPE_DOWN
                } else {
                    GestureType.THREE_FINGER_SWIPE_UP
                }
                GestureResult.Recognized(
                    gesture = _currentGesture.value!!,
                    data = GestureData(distance = distance)
                )
            }
        }
    }
    
    private fun analyzeFourFingerGesture(): GestureResult {
        val touches = touchPoints.values.toList()
        if (touches.size < 4) return GestureResult.NotRecognized
        
        // Four+ finger gestures are typically system gestures
        _currentGesture.value = GestureType.FOUR_FINGER_TAP
        return GestureResult.Recognized(
            gesture = GestureType.FOUR_FINGER_TAP,
            data = GestureData(fingerCount = touches.size)
        )
    }
    
    private fun finalizeGesture(event: MotionEvent): GestureResult {
        val duration = System.currentTimeMillis() - gestureStartTime
        
        if (touchPoints.size == 1 && duration < TAP_TIMEOUT) {
            val touch = touchPoints.values.first()
            val distance = calculateDistance(
                touch.startX, touch.startY,
                touch.currentX, touch.currentY
            )
            
            if (distance < TOUCH_SLOP) {
                // Check for double tap
                val timeSinceLastGesture = System.currentTimeMillis() - lastGestureTime
                lastGestureTime = System.currentTimeMillis()
                
                return if (timeSinceLastGesture < DOUBLE_TAP_TIMEOUT) {
                    _currentGesture.value = GestureType.DOUBLE_TAP
                    GestureResult.Recognized(
                        gesture = GestureType.DOUBLE_TAP,
                        data = GestureData(
                            x = touch.currentX,
                            y = touch.currentY
                        )
                    )
                } else {
                    _currentGesture.value = GestureType.TAP
                    GestureResult.Recognized(
                        gesture = GestureType.TAP,
                        data = GestureData(
                            x = touch.currentX,
                            y = touch.currentY
                        )
                    )
                }
            }
        }
        
        return GestureResult.NotRecognized
    }
    
    private fun calculateVelocity(): Float {
        velocityTracker?.computeCurrentVelocity(1000)
        val vx = velocityTracker?.xVelocity ?: 0f
        val vy = velocityTracker?.yVelocity ?: 0f
        return sqrt(vx * vx + vy * vy)
    }
    
    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
    
    private fun cleanup() {
        touchPoints.clear()
        velocityTracker?.recycle()
        velocityTracker = null
        _currentGesture.value = null
    }
    
    private data class TouchPoint(
        val id: Int,
        val startX: Float,
        val startY: Float,
        var currentX: Float,
        var currentY: Float
    )
    
    companion object {
        private const val TOUCH_SLOP = 8f
        private const val TAP_TIMEOUT = 300L
        private const val DOUBLE_TAP_TIMEOUT = 300L
    }
}