package com.astralplayer.nextplayer.feature.player.gestures

data class GestureSettings(
    val general: GeneralGestureSettings = GeneralGestureSettings(),
    val horizontal: HorizontalGestureSettings = HorizontalGestureSettings(),
    val vertical: VerticalGestureSettings = VerticalGestureSettings(),
    val longPress: LongPressGestureSettings = LongPressGestureSettings(),
    val zoom: ZoomGestureSettings = ZoomGestureSettings(),
    val doubleTap: DoubleTapGestureSettings = DoubleTapGestureSettings(),
    val ui: UISettings = UISettings()
)

data class GeneralGestureSettings(
    val gesturesEnabled: Boolean = true,
    val tapToToggleControls: Boolean = true,
    val gestureDetectionDelay: Long = 50L,
    val feedbackVibrationsEnabled: Boolean = true,
    val visualFeedbackEnabled: Boolean = true
)

data class HorizontalGestureSettings(
    val seekGestureEnabled: Boolean = true,
    val sensitivity: Float = 1f,
    val maxSeekTimePerSwipe: Long = 90000L, // 90 seconds
    val showSeekPreview: Boolean = true,
    val instantSeek: Boolean = false
)

data class VerticalGestureSettings(
    val volumeGestureEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val volumeSensitivity: Float = 1f,
    val brightnessSensitivity: Float = 1f,
    val leftSideForBrightness: Boolean = true,
    val rightSideForVolume: Boolean = true
)

data class LongPressGestureSettings(
    val enabled: Boolean = true,
    val duration: Long = 800L,
    val speedGestureEnabled: Boolean = true,
    val minSpeed: Float = 0.25f,
    val maxSpeed: Float = 5f,
    val defaultSpeed: Float = 2f,
    val showSpeedIndicator: Boolean = true
)

data class ZoomGestureSettings(
    val enabled: Boolean = true,
    val minZoom: Float = 0.5f,
    val maxZoom: Float = 4f,
    val sensitivity: Float = 1f,
    val doubleTapToFit: Boolean = true
)

data class DoubleTapGestureSettings(
    val enabled: Boolean = true,
    val seekAmount: Long = 10000L, // 10 seconds
    val maxTapInterval: Long = 300L,
    val centerTapToPause: Boolean = true,
    val showFeedbackAnimation: Boolean = true
)

/**
 * Long press actions
 */
enum class LongPressAction {
    SHOW_INFO, 
    PLAYBACK_SPEED, 
    ADVANCED_CONTROLS,
    VARIABLE_SPEED_SEEK // New action for MX Player style seeking
}

/**
 * Comprehensive long press settings
 */
data class LongPressSettings(
    val isEnabled: Boolean = true,
    val duration: Long = 500, // ms to trigger long press
    val action: LongPressAction = LongPressAction.VARIABLE_SPEED_SEEK,
    val longPressGestureEnabled: Boolean = true,
    val longPressDefaultSpeed: Float = 2.0f,
    val maxSeekSpeed: Float = 5.0f,
    val minSeekSpeed: Float = 0.5f,
    val speedChangeThreshold: Float = 0.2f, // Sensitivity for speed changes
    val directionChangeThreshold: Float = 50f, // Pixels to change direction
    val continuousSeekInterval: Long = 100L, // ms between seek updates
    val hapticFeedbackEnabled: Boolean = true,
    val adaptiveSpeed: Boolean = true,
    val showSpeedZones: Boolean = true,
    val defaultSpeed: Float = 2.0f,
    val maxSpeed: Float = 5.0f,
    // Additional parameters used by LongPressSeekSettingsManager
    val minSpeed: Float = 0.5f,
    val enablePreviewPlayback: Boolean = true,
    val autoResumeOnRelease: Boolean = true
) {
    fun validate(): LongPressSettings = copy(
        duration = duration.coerceIn(300L, 1000L),
        longPressDefaultSpeed = longPressDefaultSpeed.coerceIn(0.5f, 10f),
        maxSeekSpeed = maxSeekSpeed.coerceIn(2f, 10f),
        minSeekSpeed = minSeekSpeed.coerceIn(0.1f, 2f),
        speedChangeThreshold = speedChangeThreshold.coerceIn(0.1f, 1f),
        directionChangeThreshold = directionChangeThreshold.coerceIn(20f, 100f),
        continuousSeekInterval = continuousSeekInterval.coerceIn(50L, 500L)
    )
}

/**
 * UI layout settings
 */
data class UISettings(
    val landscapePlayBarPosition: PlayBarPosition = PlayBarPosition.BOTTOM,
    val showLandscapePlayBarOnSides: Boolean = false
)

/**
 * Play bar position options for landscape mode
 */
enum class PlayBarPosition {
    BOTTOM,     // Traditional bottom position
    LEFT,       // Left side of screen
    RIGHT       // Right side of screen
}