package com.astralplayer.nextplayer.data

/**
 * Sealed class representing different gesture actions
 */
sealed class GestureAction {
    
    /**
     * Seeking gesture (horizontal swipe)
     */
    data class Seek(
        val deltaMs: Long,
        val direction: SeekDirection,
        val startPosition: Long = 0L,
        val endPosition: Long = 0L
    ) : GestureAction()
    
    /**
     * Volume change gesture (vertical swipe on right side)
     */
    data class VolumeChange(
        val delta: Float,
        val currentVolume: Float = 0f,
        val targetVolume: Float = 0f
    ) : GestureAction()
    
    /**
     * Brightness change gesture (vertical swipe on left side)
     */
    data class BrightnessChange(
        val delta: Float,
        val currentBrightness: Float = 0f,
        val targetBrightness: Float = 0f
    ) : GestureAction()
    
    /**
     * Double tap to seek
     */
    data class DoubleTapSeek(
        val forward: Boolean,
        val amount: Long = 10000L, // 10 seconds default
        val position: Float = 0.5f // 0.0 = left, 1.0 = right
    ) : GestureAction()
    
    /**
     * Toggle play/pause (single tap)
     */
    data object TogglePlayPause : GestureAction()
    
    /**
     * Long press seek (hold to seek continuously)
     */
    data class LongPressSeek(
        val direction: SeekDirection,
        val speed: Float = 2.0f,
        val startTime: Long = System.currentTimeMillis()
    ) : GestureAction()
    
    /**
     * Pinch to zoom gesture
     */
    data class PinchZoom(
        val scaleFactor: Float,
        val focusX: Float,
        val focusY: Float
    ) : GestureAction()
    
    /**
     * Swipe gesture for navigation
     */
    data class SwipeNavigation(
        val direction: SwipeDirection,
        val velocity: Float
    ) : GestureAction()
    
    /**
     * Gesture conflict (multiple gestures detected)
     */
    data class GestureConflict(
        val conflictingGestures: List<GestureAction>
    ) : GestureAction()
    
    /**
     * Custom gesture action
     */
    data class Custom(
        val type: String,
        val data: Map<String, Any> = emptyMap()
    ) : GestureAction()
}

/**
 * Seek direction for gestures
 */
enum class SeekDirection {
    FORWARD,
    BACKWARD;
    
    fun getDisplayName(): String {
        return when (this) {
            FORWARD -> "Forward"
            BACKWARD -> "Backward"
        }
    }
    
    fun getSeekAmount(defaultAmount: Long = 10000L): Long {
        return when (this) {
            FORWARD -> defaultAmount
            BACKWARD -> -defaultAmount
        }
    }
}

/**
 * Swipe direction for navigation gestures
 */
enum class SwipeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT;
    
    fun isVertical(): Boolean = this == UP || this == DOWN
    fun isHorizontal(): Boolean = this == LEFT || this == RIGHT
}

/**
 * Touch side for vertical gestures
 */
enum class TouchSide {
    LEFT,
    RIGHT,
    CENTER;
    
    companion object {
        fun fromX(x: Float, screenWidth: Float): TouchSide {
            val ratio = x / screenWidth
            return when {
                ratio < 0.33f -> LEFT
                ratio > 0.67f -> RIGHT
                else -> CENTER
            }
        }
    }
}

/**
 * Gesture sensitivity settings
 */
data class GestureSensitivity(
    val seekSensitivity: Float = 1.0f,
    val volumeSensitivity: Float = 1.0f,
    val brightnessSensitivity: Float = 1.0f,
    val doubleTapSensitivity: Float = 1.0f
) {
    companion object {
        val DEFAULT = GestureSensitivity()
        val LOW = GestureSensitivity(0.5f, 0.5f, 0.5f, 0.8f)
        val HIGH = GestureSensitivity(1.5f, 1.5f, 1.5f, 1.2f)
    }
}