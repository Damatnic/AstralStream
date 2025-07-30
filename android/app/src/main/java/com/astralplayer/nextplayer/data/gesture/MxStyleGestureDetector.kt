package com.astralplayer.nextplayer.data.gesture

import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import com.astralplayer.nextplayer.data.TouchSide
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlin.math.abs
import android.util.Log

/**
 * MX Player style gesture zones:
 * - Left 40%: Brightness control (vertical swipe)
 * - Right 40%: Volume control (vertical swipe)  
 * - Center 20%: Horizontal seek (horizontal swipe)
 * - Double tap left half: Seek backward
 * - Double tap right half: Seek forward
 * - Single tap: Show/hide controls
 * - Long press left half: Fast rewind
 * - Long press right half: Fast forward
 */
data class MxGestureZones(
    val deadZoneTop: Float = 0.08f, // Top 8% is dead zone (for status bar)
    val deadZoneBottom: Float = 0.08f, // Bottom 8% is dead zone (for navigation bar)
    val leftZoneWidth: Float = 0.4f, // Left zone takes 40% of width
    val rightZoneWidth: Float = 0.4f, // Right zone takes 40% of width
    val centerZoneWidth: Float = 0.2f, // Center zone takes 20% of width
    val minimumSwipeDistance: Float = 10f, // Minimum swipe distance in pixels (lowered for better sensitivity)
    val doubleTapTimeout: Long = 300L,
    val longPressTimeout: Long = 800L // For long press detection (MX Player style)
)

sealed class MxGestureType {
    object SingleTap : MxGestureType()
    data class DoubleTap(val side: TouchSide) : MxGestureType()
    data class VerticalSwipe(val side: TouchSide, val isVolume: Boolean) : MxGestureType()
    object HorizontalSwipe : MxGestureType()
    data class LongPress(val side: TouchSide) : MxGestureType()
    object CenterLongPress : MxGestureType()
}

data class MxGestureCallbacks(
    val onSingleTap: () -> Unit = {},
    val onDoubleTapLeft: () -> Unit = {},
    val onDoubleTapRight: () -> Unit = {},
    val onSeek: (deltaX: Float, velocity: Float) -> Unit = { _, _ -> },
    val onSeekStart: () -> Unit = {},
    val onSeekEnd: () -> Unit = {},
    val onVolumeChange: (deltaY: Float) -> Unit = {},
    val onBrightnessChange: (deltaY: Float) -> Unit = {},
    val onLongPressStart: (side: TouchSide) -> Unit = {},
    val onLongPressEnd: () -> Unit = {},
    val onCenterLongPressStart: () -> Unit = {},
    val onCenterLongPressEnd: () -> Unit = {},
    val onCenterLongPressSpeedChange: (deltaY: Float) -> Unit = {}
)

/**
 * MX Player style gesture detector with proper zone boundaries
 */
fun Modifier.mxStyleGestures(
    zones: MxGestureZones = MxGestureZones(),
    callbacks: MxGestureCallbacks
): Modifier = pointerInput(zones) {
    val width = size.width.toFloat()
    val height = size.height.toFloat()
    
    // Calculate zone boundaries
    val deadZoneTop = height * zones.deadZoneTop
    val deadZoneBottom = height * (1f - zones.deadZoneBottom)
    
    // Calculate the three zones: left 40%, center 20%, right 40%
    val leftZoneRight = width * zones.leftZoneWidth // 0 to 40% of width
    val centerZoneLeft = leftZoneRight // 40% of width
    val centerZoneRight = width * (zones.leftZoneWidth + zones.centerZoneWidth) // 60% of width
    val rightZoneLeft = centerZoneRight // 60% of width
    
    Log.d("MxGesture", "Zone boundaries: left=[0, $leftZoneRight], center=[$centerZoneLeft, $centerZoneRight], right=[$rightZoneLeft, $width]")
    
    // State for gesture detection
    var gestureStartPosition: Offset? = null
    var activeGesture: MxGestureType? = null
    var lastTapTime = 0L
    var lastTapPosition: Offset? = null
    var isLongPressActive = false
    var longPressJob: kotlinx.coroutines.Job? = null
    
    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val downPosition = down.position
            val downTime = down.uptimeMillis
            
            // Check if this is in a dead zone
            if (downPosition.y < deadZoneTop || downPosition.y > deadZoneBottom) {
                Log.d("MxGesture", "Touch in dead zone: y=${downPosition.y}, top=$deadZoneTop, bottom=$deadZoneBottom")
                return@awaitEachGesture
            }
            
            Log.d("MxGesture", "Touch down at: x=${downPosition.x}, y=${downPosition.y}")
            
            gestureStartPosition = downPosition
            
            // Determine which zone and side the touch is in
            val touchZone = when {
                downPosition.x <= leftZoneRight -> "left"
                downPosition.x >= rightZoneLeft -> "right"
                else -> "center"
            }
            
            // Determine side for double tap and long press (left half vs right half)
            val touchSide = when {
                downPosition.x < width / 2 -> TouchSide.LEFT
                else -> TouchSide.RIGHT
            }
            
            // Determine if this is a center zone long press for speed control
            val isCenterLongPress = touchZone == "center"
            
            Log.d("MxGesture", "Touch in zone: $touchZone, side: $touchSide")
            
            // Start long press detection
            longPressJob?.cancel()
            longPressJob = launch {
                delay(zones.longPressTimeout)
                if (!isLongPressActive) {
                    isLongPressActive = true
                    if (isCenterLongPress) {
                        activeGesture = MxGestureType.CenterLongPress
                        Log.d("MxGesture", "Center long press detected - starting 2x speed")
                        callbacks.onCenterLongPressStart()
                    } else {
                        activeGesture = MxGestureType.LongPress(touchSide)
                        Log.d("MxGesture", "Long press detected on $touchSide side - starting seeking")
                        callbacks.onLongPressStart(touchSide)
                    }
                }
            }
            
            // Check for double tap
            val timeSinceLastTap = downTime - lastTapTime
            val isDoubleTap = timeSinceLastTap < zones.doubleTapTimeout && 
                              lastTapPosition?.let { 
                                  (it - downPosition).getDistance() < 50f 
                              } ?: false
            
            val drag = awaitTouchSlopOrCancellation(down.id) { change, over ->
                change.consume()
                
                // Determine gesture type based on initial movement
                val deltaX = abs(over.x)
                val deltaY = abs(over.y)
                
                // Only cancel long press if it's not a center zone vertical swipe (for speed control)
                val isCenterVerticalSwipe = deltaY > zones.minimumSwipeDistance && touchZone == "center"
                if (!isCenterVerticalSwipe) {
                    longPressJob?.cancel()
                    isLongPressActive = false
                }
                
                when {
                    // Horizontal swipe in center zone (seek)
                    deltaX > zones.minimumSwipeDistance &&
                    touchZone == "center" -> {
                        activeGesture = MxGestureType.HorizontalSwipe
                        Log.d("MxGesture", "Detected horizontal swipe (seek) in center zone")
                        callbacks.onSeekStart()
                    }
                    
                    // Vertical swipe in left zone (brightness)
                    deltaY > zones.minimumSwipeDistance &&
                    touchZone == "left" -> {
                        activeGesture = MxGestureType.VerticalSwipe(TouchSide.LEFT, false)
                        Log.d("MxGesture", "Detected vertical swipe in left zone (brightness)")
                    }
                    
                    // Vertical swipe in right zone (volume)
                    deltaY > zones.minimumSwipeDistance &&
                    touchZone == "right" -> {
                        activeGesture = MxGestureType.VerticalSwipe(TouchSide.RIGHT, true)
                        Log.d("MxGesture", "Detected vertical swipe in right zone (volume)")
                    }
                    
                    // Vertical swipe in center zone while long press is active (speed control)
                    deltaY > zones.minimumSwipeDistance &&
                    touchZone == "center" && isLongPressActive -> {
                        // Keep the long press active but allow vertical swipe for speed control
                        Log.d("MxGesture", "Detected vertical swipe in center zone during long press (speed control)")
                        // Don't change activeGesture, let long press continue
                    }
                    
                    // Allow horizontal swipe anywhere if it's dominant
                    deltaX > deltaY * 2 && deltaX > zones.minimumSwipeDistance -> {
                        activeGesture = MxGestureType.HorizontalSwipe
                        Log.d("MxGesture", "Detected strong horizontal swipe (seek)")
                        callbacks.onSeekStart()
                    }
                }
            }
            
            if (drag != null && activeGesture != null) {
                // Handle drag gesture
                drag(drag.id) { change ->
                    change.consume()
                    val currentPosition = change.position
                    val startPos = gestureStartPosition ?: return@drag
                    
                    when (activeGesture) {
                        is MxGestureType.HorizontalSwipe -> {
                            val deltaX = currentPosition.x - startPos.x
                            val velocity = change.position.x - change.previousPosition.x
                            Log.d("MxGesture", "Seek drag: deltaX=$deltaX, velocity=$velocity")
                            callbacks.onSeek(deltaX, velocity)
                        }
                        is MxGestureType.VerticalSwipe -> {
                            val deltaY = -(currentPosition.y - startPos.y) / height // Normalize and invert
                            if ((activeGesture as MxGestureType.VerticalSwipe).isVolume) {
                                Log.d("MxGesture", "Volume change: deltaY=$deltaY")
                                callbacks.onVolumeChange(deltaY)
                            } else {
                                Log.d("MxGesture", "Brightness change: deltaY=$deltaY")
                                callbacks.onBrightnessChange(deltaY)
                            }
                        }
                        is MxGestureType.CenterLongPress -> {
                            // Handle vertical swipe during center long press for speed control
                            val deltaY = -(currentPosition.y - startPos.y) / height // Normalize and invert
                            Log.d("MxGesture", "Center long press speed change: deltaY=$deltaY")
                            callbacks.onCenterLongPressSpeedChange(deltaY)
                        }
                        else -> {}
                    }
                }
                // Handle up event for seek end
                if (activeGesture is MxGestureType.HorizontalSwipe) {
                    Log.d("MxGesture", "Seek gesture ended")
                    callbacks.onSeekEnd()
                }
            } else {
                // Handle tap or release after no drag
                val up = awaitPointerEvent(PointerEventPass.Final)
                if (up.changes.all { it.changedToUp() }) {
                    // Cancel long press
                    longPressJob?.cancel()
                    
                    if (isLongPressActive) {
                        // Long press was active, handle release
                        when (activeGesture) {
                            is MxGestureType.CenterLongPress -> {
                                Log.d("MxGesture", "Center long press ended")
                                callbacks.onCenterLongPressEnd()
                            }
                            is MxGestureType.LongPress -> {
                                Log.d("MxGesture", "Long press ended")
                                callbacks.onLongPressEnd()
                            }
                            else -> {
                                callbacks.onLongPressEnd()
                            }
                        }
                        isLongPressActive = false
                    } else {
                        // Handle taps
                        when {
                            isDoubleTap -> {
                                // Double tap
                                when {
                                    downPosition.x < width / 2 -> {
                                        Log.d("MxGesture", "Double tap left detected")
                                        callbacks.onDoubleTapLeft()
                                    }
                                    else -> {
                                        Log.d("MxGesture", "Double tap right detected")
                                        callbacks.onDoubleTapRight()
                                    }
                                }
                                lastTapTime = 0L // Reset to prevent triple tap
                                lastTapPosition = null
                            }
                            else -> {
                                // Single tap
                                Log.d("MxGesture", "Single tap detected")
                                callbacks.onSingleTap()
                                lastTapTime = downTime
                                lastTapPosition = downPosition
                            }
                        }
                    }
                }
            }
            
            // Reset state
            gestureStartPosition = null
            activeGesture = null
        }
    }
}

// Extension function to get distance between two offsets
private fun Offset.getDistance(): Float {
    return kotlin.math.sqrt(x * x + y * y)
}