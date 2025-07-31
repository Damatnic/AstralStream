package com.astralplayer.nextplayer.gesture

import android.graphics.PointF

/**
 * Gesture-related data models and enums
 */

// Gesture types that can be detected
enum class GestureType {
    // Single finger gestures
    TAP,
    DOUBLE_TAP,
    DOUBLE_TAP_LEFT,
    DOUBLE_TAP_RIGHT,
    LONG_PRESS,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    
    // Two finger gestures
    PINCH_ZOOM_IN,
    PINCH_ZOOM_OUT,
    TWO_FINGER_TAP,
    TWO_FINGER_SWIPE_UP,
    TWO_FINGER_SWIPE_DOWN,
    TWO_FINGER_SWIPE_LEFT,
    TWO_FINGER_SWIPE_RIGHT,
    TWO_FINGER_ROTATE,
    
    // Three finger gestures
    THREE_FINGER_TAP,
    THREE_FINGER_SWIPE_UP,
    THREE_FINGER_SWIPE_DOWN,
    THREE_FINGER_SWIPE_LEFT,
    THREE_FINGER_SWIPE_RIGHT,
    
    // Four+ finger gestures
    FOUR_FINGER_TAP,
    FOUR_FINGER_SWIPE,
    
    // Voice commands
    VOICE_COMMAND,
    
    // Custom gestures
    CUSTOM,
    
    // Special types
    SINGLE_TAP,
    HORIZONTAL_SEEK,
    VERTICAL_VOLUME,
    VERTICAL_BRIGHTNESS
}

// Haptic feedback patterns
enum class HapticPattern {
    LIGHT_TAP,
    MEDIUM_TAP,
    HEAVY_TAP,
    DOUBLE_TAP,
    LONG_PRESS,
    SWIPE,
    PINCH,
    MULTI_FINGER,
    SUCCESS,
    ERROR,
    WARNING,
    GESTURE_DETECTED,
    GESTURE_COMPLETE
}

// Actions that can be triggered by gestures
sealed class GestureAction {
    data class Seek(val milliseconds: Int) : GestureAction()
    data class VolumeChange(val delta: Float) : GestureAction()
    data class BrightnessChange(val delta: Float) : GestureAction()
    data class DoubleTapSeek(val seconds: Int) : GestureAction()
    object TogglePlayPause : GestureAction()
    data class LongPressSeek(val speedMultiplier: Float) : GestureAction()
    data class PinchZoom(val scaleFactor: Float) : GestureAction()
    data class SwipeNavigation(val direction: String) : GestureAction()
    data class Custom(val action: String) : GestureAction()
    object GestureConflict : GestureAction()
}

// Result of gesture detection
sealed class GestureResult {
    data class Recognized(
        val gesture: GestureType,
        val data: GestureData
    ) : GestureResult()
    
    data class Recording(
        val point: PointF
    ) : GestureResult()
    
    object NotRecognized : GestureResult()
}

// Data associated with a recognized gesture
data class GestureData(
    val x: Float = 0f,
    val y: Float = 0f,
    val distance: Float = 0f,
    val velocity: Float = 0f,
    val angle: Float = 0f,
    val scaleFactor: Float = 1f,
    val fingerCount: Int = 1,
    val duration: Long = 0L,
    val customData: Map<String, Any> = emptyMap()
)

// Gesture conflict resolution
data class GestureConflict(
    val gesture1: GestureType,
    val gesture2: GestureType,
    val resolution: ConflictResolution
)

enum class ConflictResolution {
    PREFER_FIRST,
    PREFER_SECOND,
    DISABLE_BOTH,
    ASK_USER
}

// Training mode for custom gestures
data class GestureTrainingSession(
    val sessionId: String,
    val gestureType: GestureType,
    val samples: MutableList<GestureRecorder.RecordedGesture> = mutableListOf(),
    val requiredSamples: Int = 3,
    val startTime: Long = System.currentTimeMillis()
) {
    val isComplete: Boolean
        get() = samples.size >= requiredSamples
    
    val progress: Float
        get() = samples.size.toFloat() / requiredSamples
}

// Accessibility settings for gestures
data class GestureAccessibilitySettings(
    val extraTapTime: Boolean = false,
    val tapTimeoutMs: Long = 500L,
    val reducedMotionSensitivity: Boolean = false,
    val sensitivityMultiplier: Float = 0.7f,
    val stickyEdges: Boolean = false,
    val audioFeedback: Boolean = false,
    val visualGuides: Boolean = false
)

// Gesture analytics
data class GestureAnalytics(
    val gestureType: GestureType,
    val count: Int,
    val successRate: Float,
    val averageExecutionTime: Long,
    val lastUsed: Long
)

// Gesture zone configuration
data class GestureZone(
    val id: String,
    val bounds: Bounds,
    val allowedGestures: Set<GestureType>,
    val priority: Int = 0
) {
    data class Bounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        fun contains(x: Float, y: Float): Boolean {
            return x >= left && x <= right && y >= top && y <= bottom
        }
    }
}