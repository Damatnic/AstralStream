package com.astralplayer.nextplayer.feature.player.gestures

/**
 * Settings class for brightness gesture configuration
 */
data class BrightnessGestureSettings(
    val isEnabled: Boolean = true,
    val leftSideOnly: Boolean = true,
    val sensitivity: Float = 1.0f,
    val leftSideSensitivity: Float = 1.0f,
    val minimumSwipeDistance: Float = 50f,
    val brightnessStep: Float = 0.02f,
    val minimumBrightness: Float = 0.01f,
    val maximumBrightness: Float = 1.0f,
    val systemBrightnessIntegration: Boolean = true
)