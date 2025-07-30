package com.astralplayer.nextplayer.feature.player.gestures

/**
 * Settings class for volume gesture configuration
 */
data class VolumeGestureSettings(
    val isEnabled: Boolean = true,
    val rightSideOnly: Boolean = true,
    val sensitivity: Float = 1.0f,
    val rightSideSensitivity: Float = 1.0f,
    val minimumSwipeDistance: Float = 50f,
    val volumeStep: Float = 0.02f,
    val volumeBoostEnabled: Boolean = false,
    val volumeBoostLimit: Float = 1.5f,
    val systemVolumeIntegration: Boolean = true
)