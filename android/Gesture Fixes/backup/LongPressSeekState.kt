package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.ui.geometry.Offset

data class LongPressSeekState(
    val isActive: Boolean = false,
    // Touch positions (for gesture tracking)
    val initialTouchPosition: Offset = Offset.Zero,
    val currentTouchPosition: Offset = Offset.Zero,
    // Video timeline positions (in milliseconds)
    val startPosition: Long = 0L,
    val currentPosition: Long = 0L,
    val seekDirection: SeekDirection = SeekDirection.NONE,
    val speedMultiplier: Float = 1f,
    val seekPreviewPosition: Long = 0L,
    val originalPosition: Long = 0L,
    // Additional properties for enhanced seeking
    val seekSpeed: Float = 1f,
    val direction: SeekDirection = SeekDirection.NONE,
    val showSpeedIndicator: Boolean = false,
    val accelerationLevel: Int = 0,
    val isAccelerating: Boolean = false,
    val defaultSpeed: Float = 2f,
    val maxSpeed: Float = 5f,
    val isRewindMode: Boolean = false,
    val elapsedTime: Long = 0L
)