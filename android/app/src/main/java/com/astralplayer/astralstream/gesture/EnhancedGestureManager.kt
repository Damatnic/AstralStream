package com.astralplayer.astralstream.gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

class EnhancedGestureManager(
    private val context: Context,
    private val onSwipeUp: (Zone) -> Unit = {},
    private val onSwipeDown: (Zone) -> Unit = {},
    private val onSwipeLeft: () -> Unit = {},
    private val onSwipeRight: () -> Unit = {},
    private val onDoubleTap: (Zone) -> Unit = {},
    private val onLongPress: () -> Unit = {},
    private val onPinchZoom: (Float) -> Unit = {},
    private val onSingleTap: () -> Unit = {}
) {
    
    enum class Zone {
        LEFT, CENTER, RIGHT
    }
    
    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
        private const val LONG_PRESS_DURATION = 500L
        private const val DOUBLE_TAP_TIMEOUT = 300L
    }
    
    private var screenWidth = 0
    private var initialX = 0f
    private var initialY = 0f
    private var lastTapTime = 0L
    private var isLongPressing = false
    private var currentScale = 1.0f
    
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        
        override fun onDown(e: MotionEvent): Boolean {
            initialX = e.x
            initialY = e.y
            return true
        }
        
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT) {
                // Double tap detected
                val zone = getZone(e.x)
                onDoubleTap(zone)
                lastTapTime = 0L
            } else {
                lastTapTime = currentTime
                // Delay to check if it's a double tap
                context.mainExecutor.execute {
                    Thread.sleep(DOUBLE_TAP_TIMEOUT)
                    if (lastTapTime == currentTime) {
                        onSingleTap()
                    }
                }
            }
            return true
        }
        
        override fun onLongPress(e: MotionEvent) {
            isLongPressing = true
            onLongPress()
        }
        
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (e1 == null || isLongPressing) return false
            
            val deltaX = e2.x - e1.x
            val deltaY = e2.y - e1.y
            
            // Determine if this is a vertical or horizontal swipe
            if (abs(deltaY) > abs(deltaX)) {
                // Vertical swipe
                val zone = getZone(e1.x)
                if (abs(deltaY) > SWIPE_THRESHOLD) {
                    if (deltaY < 0) {
                        onSwipeUp(zone)
                    } else {
                        onSwipeDown(zone)
                    }
                    return true
                }
            } else {
                // Horizontal swipe
                if (abs(deltaX) > SWIPE_THRESHOLD) {
                    if (deltaX < 0) {
                        onSwipeLeft()
                    } else {
                        onSwipeRight()
                    }
                    return true
                }
            }
            
            return false
        }
        
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            
            val deltaX = e2.x - e1.x
            val deltaY = e2.y - e1.y
            
            if (abs(velocityY) > abs(velocityX)) {
                // Vertical fling
                if (abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    val zone = getZone(e1.x)
                    if (deltaY < 0) {
                        onSwipeUp(zone)
                    } else {
                        onSwipeDown(zone)
                    }
                    return true
                }
            } else {
                // Horizontal fling
                if (abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (deltaX < 0) {
                        onSwipeLeft()
                    } else {
                        onSwipeRight()
                    }
                    return true
                }
            }
            
            return false
        }
    })
    
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            currentScale *= detector.scaleFactor
            currentScale = currentScale.coerceIn(0.5f, 3.0f)
            onPinchZoom(currentScale)
            return true
        }
    })
    
    fun attachToView(view: View) {
        screenWidth = view.width
        
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isLongPressing = false
                }
            }
            
            // Handle both gesture detectors
            val handled = gestureDetector.onTouchEvent(event)
            scaleGestureDetector.onTouchEvent(event)
            
            handled
        }
    }
    
    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isLongPressing = false
            }
        }
        
        val handled = gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        
        return handled
    }
    
    private fun getZone(x: Float): Zone {
        return when {
            x < screenWidth / 3 -> Zone.LEFT
            x > 2 * screenWidth / 3 -> Zone.RIGHT
            else -> Zone.CENTER
        }
    }
    
    fun updateScreenDimensions(width: Int, height: Int) {
        screenWidth = width
    }
    
    fun release() {
        // Clean up resources if needed
    }
    
    // Additional gesture support for Compose
    fun detectGestures(offset: Offset, previousOffset: Offset?, action: Int): GestureResult? {
        if (previousOffset == null) return null
        
        val deltaX = offset.x - previousOffset.x
        val deltaY = offset.y - previousOffset.y
        
        return when {
            abs(deltaY) > abs(deltaX) && abs(deltaY) > SWIPE_THRESHOLD -> {
                val zone = when {
                    offset.x < screenWidth / 3 -> Zone.LEFT
                    offset.x > 2 * screenWidth / 3 -> Zone.RIGHT
                    else -> Zone.CENTER
                }
                
                if (deltaY < 0) {
                    GestureResult.SwipeUp(zone)
                } else {
                    GestureResult.SwipeDown(zone)
                }
            }
            abs(deltaX) > SWIPE_THRESHOLD -> {
                if (deltaX < 0) {
                    GestureResult.SwipeLeft
                } else {
                    GestureResult.SwipeRight
                }
            }
            else -> null
        }
    }
    
    sealed class GestureResult {
        data class SwipeUp(val zone: Zone) : GestureResult()
        data class SwipeDown(val zone: Zone) : GestureResult()
        object SwipeLeft : GestureResult()
        object SwipeRight : GestureResult()
        data class DoubleTap(val zone: Zone) : GestureResult()
        object LongPress : GestureResult()
        data class PinchZoom(val scale: Float) : GestureResult()
    }
}