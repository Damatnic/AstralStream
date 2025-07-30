package com.astralplayer.nextplayer.feature.player.revolutionary

/**
 * Types of gestures supported by the video player
 */
enum class GestureType {
    /**
     * Single tap gesture
     */
    TAP,
    
    /**
     * Double tap gesture
     */
    DOUBLE_TAP,
    
    /**
     * Triple tap gesture
     */
    TRIPLE_TAP,
    
    /**
     * Two finger tap gesture
     */
    TWO_FINGER_TAP,
    
    /**
     * Long press gesture
     */
    LONG_PRESS,
    
    /**
     * Horizontal swipe for seeking
     */
    HORIZONTAL_SWIPE,
    
    /**
     * Vertical swipe on left side for brightness
     */
    VERTICAL_SWIPE_LEFT,
    
    /**
     * Vertical swipe on right side for volume
     */
    VERTICAL_SWIPE_RIGHT,
    
    /**
     * Pinch to zoom gesture
     */
    PINCH_ZOOM,
    
    /**
     * Pinch in gesture
     */
    PINCH_IN,
    
    /**
     * Pinch out gesture
     */
    PINCH_OUT,
    
    /**
     * Two finger rotation
     */
    ROTATION,
    
    /**
     * Three finger swipe
     */
    THREE_FINGER_SWIPE,
    
    /**
     * Swipe up gesture
     */
    SWIPE_UP,
    
    /**
     * Swipe down gesture
     */
    SWIPE_DOWN,
    
    /**
     * Swipe left gesture
     */
    SWIPE_LEFT,
    
    /**
     * Swipe right gesture
     */
    SWIPE_RIGHT,
    
    /**
     * Edge swipe from screen edges
     */
    EDGE_SWIPE,
    
    /**
     * Fling gesture for fast scrolling
     */
    FLING,
    
    /**
     * Multi-touch gesture
     */
    MULTI_TOUCH,
    
    /**
     * Custom gesture pattern
     */
    CUSTOM_PATTERN
}

/**
 * Extension functions for GestureType
 */
fun GestureType.isSwipeGesture(): Boolean {
    return this == GestureType.HORIZONTAL_SWIPE || 
           this == GestureType.VERTICAL_SWIPE_LEFT || 
           this == GestureType.VERTICAL_SWIPE_RIGHT ||
           this == GestureType.THREE_FINGER_SWIPE ||
           this == GestureType.EDGE_SWIPE
}

fun GestureType.isTouchGesture(): Boolean {
    return this == GestureType.TAP || 
           this == GestureType.DOUBLE_TAP || 
           this == GestureType.LONG_PRESS
}

fun GestureType.requiresMultiTouch(): Boolean {
    return this == GestureType.PINCH_ZOOM || 
           this == GestureType.ROTATION || 
           this == GestureType.THREE_FINGER_SWIPE ||
           this == GestureType.MULTI_TOUCH
}

fun GestureType.getDisplayName(): String {
    return when (this) {
        GestureType.TAP -> "Tap"
        GestureType.DOUBLE_TAP -> "Double Tap"
        GestureType.TRIPLE_TAP -> "Triple Tap"
        GestureType.TWO_FINGER_TAP -> "Two Finger Tap"
        GestureType.LONG_PRESS -> "Long Press"
        GestureType.HORIZONTAL_SWIPE -> "Horizontal Swipe"
        GestureType.VERTICAL_SWIPE_LEFT -> "Vertical Swipe (Left)"
        GestureType.VERTICAL_SWIPE_RIGHT -> "Vertical Swipe (Right)"
        GestureType.PINCH_ZOOM -> "Pinch to Zoom"
        GestureType.PINCH_IN -> "Pinch In"
        GestureType.PINCH_OUT -> "Pinch Out"
        GestureType.ROTATION -> "Rotation"
        GestureType.THREE_FINGER_SWIPE -> "Three Finger Swipe"
        GestureType.SWIPE_UP -> "Swipe Up"
        GestureType.SWIPE_DOWN -> "Swipe Down"
        GestureType.SWIPE_LEFT -> "Swipe Left"
        GestureType.SWIPE_RIGHT -> "Swipe Right"
        GestureType.EDGE_SWIPE -> "Edge Swipe"
        GestureType.FLING -> "Fling"
        GestureType.MULTI_TOUCH -> "Multi-Touch"
        GestureType.CUSTOM_PATTERN -> "Custom Pattern"
    }
}