package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Velocity
import android.graphics.Bitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Enhanced gesture settings with comprehensive MX Player-style features
 */
@Serializable
@Immutable
data class EnhancedGestureSettings(
    val general: GeneralGestureSettings = GeneralGestureSettings(),
    val seeking: SeekingGestureSettings = SeekingGestureSettings(),
    val volume: VolumeGestureSettings = VolumeGestureSettings(),
    val brightness: BrightnessGestureSettings = BrightnessGestureSettings(),
    val doubleTap: DoubleTapGestureSettings = DoubleTapGestureSettings(),
    val longPress: LongPressGestureSettings = LongPressGestureSettings(),
    val pinchZoom: PinchZoomGestureSettings = PinchZoomGestureSettings(),
    val advanced: AdvancedGestureSettings = AdvancedGestureSettings(),
    val accessibility: AccessibilityGestureSettings = AccessibilityGestureSettings()
) {
    fun validate(): EnhancedGestureSettings = copy(
        general = general.validate(),
        seeking = seeking.validate(),
        volume = volume.validate(),
        brightness = brightness.validate(),
        doubleTap = doubleTap.validate(),
        longPress = longPress.validate(),
        pinchZoom = pinchZoom.validate(),
        advanced = advanced.validate(),
        accessibility = accessibility.validate()
    )
}

@Serializable
@Immutable
data class GeneralGestureSettings(
    val gesturesEnabled: Boolean = true,
    val hapticFeedback: Boolean = true,
    val visualFeedback: Boolean = true,
    val gestureTimeout: Long = 2000L,
    val overlayOpacity: Float = 0.9f,
    val animationDuration: Long = 300L,
    val debugMode: Boolean = false,
    val conflictResolution: ConflictResolutionStrategy = ConflictResolutionStrategy.PRIORITY_BASED
) {
    fun validate(): GeneralGestureSettings = copy(
        gestureTimeout = gestureTimeout.coerceIn(500L, 5000L),
        overlayOpacity = overlayOpacity.coerceIn(0.1f, 1.0f),
        animationDuration = animationDuration.coerceIn(100L, 1000L)
    )
}

@Serializable
@Immutable
data class SeekingGestureSettings(
    val isEnabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val showPreviewThumbnails: Boolean = true,
    val previewThumbnailSize: ThumbnailSize = ThumbnailSize.MEDIUM,
    val showTimeIndicator: Boolean = true,
    val showProgressBar: Boolean = true,
    val minimumSwipeDistance: Float = 20f,
    val seekStepSize: Long = 5000L, // 5 seconds
    val enableFineSeek: Boolean = true,
    val fineSeekThreshold: Float = 0.3f,
    val velocityMultiplier: Float = 1.0f,
    val enableSeekPreview: Boolean = true
) {
    fun validate(): SeekingGestureSettings = copy(
        sensitivity = sensitivity.coerceIn(0.1f, 3.0f),
        minimumSwipeDistance = minimumSwipeDistance.coerceIn(10f, 100f),
        seekStepSize = seekStepSize.coerceIn(1000L, 30000L),
        fineSeekThreshold = fineSeekThreshold.coerceIn(0.1f, 1.0f),
        velocityMultiplier = velocityMultiplier.coerceIn(0.1f, 5.0f)
    )
}

@Serializable
@Immutable
data class VolumeGestureSettings(
    val isEnabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val rightSideOnly: Boolean = true,
    val showVolumeOverlay: Boolean = true,
    val overlayStyle: OverlayStyle = OverlayStyle.MODERN,
    val systemVolumeIntegration: Boolean = true,
    val volumeBoostEnabled: Boolean = false,
    val volumeBoostLimit: Float = 1.5f,
    val minimumSwipeDistance: Float = 15f,
    val volumeStep: Float = 0.05f
) {
    fun validate(): VolumeGestureSettings = copy(
        sensitivity = sensitivity.coerceIn(0.1f, 3.0f),
        volumeBoostLimit = volumeBoostLimit.coerceIn(1.0f, 2.0f),
        minimumSwipeDistance = minimumSwipeDistance.coerceIn(10f, 50f),
        volumeStep = volumeStep.coerceIn(0.01f, 0.2f)
    )
}

@Serializable
@Immutable
data class BrightnessGestureSettings(
    val isEnabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val leftSideOnly: Boolean = true,
    val showBrightnessOverlay: Boolean = true,
    val overlayStyle: OverlayStyle = OverlayStyle.MODERN,
    val systemBrightnessIntegration: Boolean = true,
    val minimumBrightness: Float = 0.01f,
    val maximumBrightness: Float = 1.0f,
    val minimumSwipeDistance: Float = 15f,
    val brightnessStep: Float = 0.05f
) {
    fun validate(): BrightnessGestureSettings = copy(
        sensitivity = sensitivity.coerceIn(0.1f, 3.0f),
        minimumBrightness = minimumBrightness.coerceIn(0.01f, 0.5f),
        maximumBrightness = maximumBrightness.coerceIn(0.5f, 1.0f),
        minimumSwipeDistance = minimumSwipeDistance.coerceIn(10f, 50f),
        brightnessStep = brightnessStep.coerceIn(0.01f, 0.2f)
    )
}

@Serializable
@Immutable
data class DoubleTapGestureSettings(
    val isEnabled: Boolean = true,
    val leftSideSeekAmount: Long = 10000L, // 10 seconds backward
    val rightSideSeekAmount: Long = 10000L, // 10 seconds forward
    val showAnimation: Boolean = true,
    val tapTimeout: Long = 300L,
    val enableLeftSide: Boolean = true,
    val enableRightSide: Boolean = true,
    val centerDeadZone: Float = 0.2f // 20% of screen width
) {
    fun validate(): DoubleTapGestureSettings = copy(
        leftSideSeekAmount = leftSideSeekAmount.coerceIn(5000L, 60000L),
        rightSideSeekAmount = rightSideSeekAmount.coerceIn(5000L, 60000L),
        tapTimeout = tapTimeout.coerceIn(200L, 500L),
        centerDeadZone = centerDeadZone.coerceIn(0.0f, 0.5f)
    )
}

@Serializable
@Immutable
data class LongPressGestureSettings(
    val isEnabled: Boolean = true,
    val triggerDuration: Long = 300L,
    val speedProgression: List<Float> = listOf(1f, 2f, 4f, 8f, 16f, 32f),
    val speedAccelerationInterval: Long = 1000L,
    val maxSpeed: Float = 32f,
    val enableDirectionChange: Boolean = true,
    val directionChangeThreshold: Float = 30f,
    val showSpeedIndicator: Boolean = true,
    val enableHapticFeedback: Boolean = true,
    val pauseVideoWhileSeeking: Boolean = false,
    val continuousSeekInterval: Long = 50L,
    val speedChangeThreshold: Float = 0.2f
) {
    fun validate(): LongPressGestureSettings = copy(
        triggerDuration = triggerDuration.coerceIn(200L, 1000L),
        speedAccelerationInterval = speedAccelerationInterval.coerceIn(500L, 2000L),
        maxSpeed = maxSpeed.coerceIn(2f, 64f),
        directionChangeThreshold = directionChangeThreshold.coerceIn(20f, 100f),
        continuousSeekInterval = continuousSeekInterval.coerceIn(30L, 200L),
        speedChangeThreshold = speedChangeThreshold.coerceIn(0.1f, 1.0f)
    )
}

@Serializable
@Immutable
data class PinchZoomGestureSettings(
    val isEnabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val minZoom: Float = 0.5f,
    val maxZoom: Float = 4.0f,
    val showZoomOverlay: Boolean = true,
    val resetOnDoubleTap: Boolean = true,
    val smoothZooming: Boolean = true,
    val maintainAspectRatio: Boolean = true
) {
    fun validate(): PinchZoomGestureSettings = copy(
        sensitivity = sensitivity.coerceIn(0.1f, 3.0f),
        minZoom = minZoom.coerceIn(0.25f, 1.0f),
        maxZoom = maxZoom.coerceIn(2.0f, 8.0f)
    )
}

@Serializable
@Immutable
data class AdvancedGestureSettings(
    val adaptiveLearningEnabled: Boolean = true,
    val gestureRecordingEnabled: Boolean = false,
    val customGesturesEnabled: Boolean = false,
    val analyticsEnabled: Boolean = true,
    val performanceMonitoring: Boolean = false,
    val batteryOptimization: Boolean = true,
    val memoryOptimization: Boolean = true
) {
    fun validate(): AdvancedGestureSettings = this
}

@Serializable
@Immutable
data class AccessibilityGestureSettings(
    val accessibilityEnabled: Boolean = false,
    val voiceControlEnabled: Boolean = false,
    val highContrastMode: Boolean = false,
    val largeTextMode: Boolean = false,
    val simplifiedGestures: Boolean = false,
    val audioFeedback: Boolean = false,
    val vibrationAmplification: Float = 1.0f,
    val gestureDescriptions: Boolean = true
) {
    fun validate(): AccessibilityGestureSettings = copy(
        vibrationAmplification = vibrationAmplification.coerceIn(0.5f, 2.0f)
    )
}

// Enums and supporting types
@Serializable
enum class ThumbnailSize(val pixels: Int) {
    SMALL(120), MEDIUM(160), LARGE(200)
}

@Serializable
enum class OverlayStyle {
    CLASSIC, MODERN, MINIMAL, COSMIC
}

@Serializable
enum class ConflictResolutionStrategy {
    PRIORITY_BASED, FIRST_DETECTED, LAST_DETECTED, USER_CHOICE
}

@Serializable
enum class TouchSide {
    LEFT, RIGHT, CENTER
}

@Serializable
enum class GestureType(val priority: Int) {
    LONG_PRESS(5),
    PINCH_ZOOM(4),
    DOUBLE_TAP(3),
    HORIZONTAL_SEEK(2),
    VERTICAL_VOLUME(2),
    VERTICAL_BRIGHTNESS(2),
    SINGLE_TAP(1)
}

@Serializable
enum class SeekDirection {
    FORWARD, BACKWARD, NONE
}

// Enhanced player state data models
@Immutable
data class EnhancedPlayerUiState(
    // Existing state properties
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPercentage: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val brightness: Float = 0.5f,
    val zoomLevel: Float = 1.0f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val aspectRatio: Float = 16f / 9f,
    
    // Enhanced gesture state
    val gestureState: GestureState = GestureState(),
    val overlayState: OverlayState = OverlayState(),
    val seekPreviewState: SeekPreviewState = SeekPreviewState(),
    val longPressSeekState: EnhancedLongPressSeekState = EnhancedLongPressSeekState(),
    
    // Accessibility state
    val accessibilityEnabled: Boolean = false,
    val voiceControlEnabled: Boolean = false,
    val highContrastMode: Boolean = false,
    
    // Advanced features
    val adaptiveGesturesEnabled: Boolean = false,
    val gestureRecordingMode: Boolean = false,
    val customGestures: List<CustomGesture> = emptyList(),
    
    // Error and status
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val isAIProcessing: Boolean = false
)

@Immutable
data class GestureState(
    val activeGesture: GestureType? = null,
    val gestureStartTime: Long = 0L,
    val gestureStartPosition: Offset = Offset.Zero,
    val currentGesturePosition: Offset = Offset.Zero,
    val gestureVelocity: Velocity = Velocity.Zero,
    val conflictingGestures: List<GestureType> = emptyList(),
    val gestureIntensity: Float = 0f,
    val gestureDirection: Float = 0f
)

@Immutable
data class OverlayState(
    val showSeekOverlay: Boolean = false,
    val showVolumeOverlay: Boolean = false,
    val showBrightnessOverlay: Boolean = false,
    val showZoomOverlay: Boolean = false,
    val showLongPressOverlay: Boolean = false,
    val overlayOpacity: Float = 0.9f,
    val overlayAnimationDuration: Long = 300L,
    val overlayStyle: OverlayStyle = OverlayStyle.MODERN
)

@Immutable
data class SeekPreviewState(
    val isVisible: Boolean = false,
    @Transient val currentThumbnail: Bitmap? = null,
    val targetPosition: Long = 0L,
    val previewText: String = "",
    val isForward: Boolean = true,
    val seekDelta: Long = 0L,
    val thumbnailGenerating: Boolean = false
)

@Immutable
data class EnhancedLongPressSeekState(
    val isActive: Boolean = false,
    val startPosition: Long = 0L,
    val currentPosition: Long = 0L,
    val targetPosition: Long = 0L,
    val speed: Float = 1f,
    val direction: SeekDirection = SeekDirection.FORWARD,
    val speedLevel: Int = 0,
    val elapsedTime: Long = 0L,
    val touchPosition: Offset = Offset.Zero,
    val showSpeedIndicator: Boolean = false,
    val speedHistory: List<Float> = emptyList(),
    val directionChangeCount: Int = 0,
    val isAccelerating: Boolean = false,
    val speedMultiplier: Float = 1f
)

// Custom gesture support
@Serializable
@Immutable
data class CustomGesture(
    val id: String,
    val name: String,
    val pattern: List<GesturePoint>,
    val action: String,
    val isEnabled: Boolean = true,
    val sensitivity: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Immutable
data class GesturePoint(
    val x: Float,
    val y: Float,
    val timestamp: Long,
    val pressure: Float = 1.0f
)

// Gesture usage analytics
@Serializable
@Immutable
data class GestureUsageData(
    val gestureType: GestureType,
    val frequency: Int = 0,
    val averageDuration: Long = 0L,
    val averageDistance: Float = 0f,
    val averageVelocity: Float = 0f,
    val preferredSensitivity: Float = 1.0f,
    val successRate: Float = 1.0f,
    val lastUsed: Long = 0L,
    val contextualData: Map<String, String> = emptyMap()
)

@Serializable
@Immutable
data class AdaptiveGestureProfile(
    val userId: String,
    val deviceType: DeviceType,
    val usageData: Map<GestureType, GestureUsageData> = emptyMap(),
    val adaptedSettings: EnhancedGestureSettings,
    val learningEnabled: Boolean = true,
    val confidenceLevel: Float = 0.5f,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
enum class DeviceType {
    PHONE, TABLET, FOLDABLE, TV
}

// Default settings factory
object EnhancedGestureSettingsFactory {
    fun createDefault(): EnhancedGestureSettings = EnhancedGestureSettings()
    
    fun createForDevice(deviceType: DeviceType): EnhancedGestureSettings {
        return when (deviceType) {
            DeviceType.PHONE -> createDefault().copy(
                seeking = SeekingGestureSettings(sensitivity = 1.0f),
                volume = VolumeGestureSettings(sensitivity = 1.0f),
                brightness = BrightnessGestureSettings(sensitivity = 1.0f)
            )
            DeviceType.TABLET -> createDefault().copy(
                seeking = SeekingGestureSettings(sensitivity = 0.8f),
                volume = VolumeGestureSettings(sensitivity = 0.8f),
                brightness = BrightnessGestureSettings(sensitivity = 0.8f)
            )
            DeviceType.FOLDABLE -> createDefault().copy(
                seeking = SeekingGestureSettings(sensitivity = 0.9f),
                volume = VolumeGestureSettings(sensitivity = 0.9f),
                brightness = BrightnessGestureSettings(sensitivity = 0.9f)
            )
            DeviceType.TV -> createDefault().copy(
                seeking = SeekingGestureSettings(sensitivity = 0.6f),
                volume = VolumeGestureSettings(sensitivity = 0.6f),
                brightness = BrightnessGestureSettings(sensitivity = 0.6f)
            )
        }
    }
    
    fun createAccessibilityOptimized(): EnhancedGestureSettings {
        return createDefault().copy(
            accessibility = AccessibilityGestureSettings(
                accessibilityEnabled = true,
                simplifiedGestures = true,
                audioFeedback = true,
                vibrationAmplification = 1.5f,
                gestureDescriptions = true
            ),
            general = GeneralGestureSettings(
                hapticFeedback = true,
                visualFeedback = true,
                overlayOpacity = 1.0f,
                animationDuration = 500L
            )
        )
    }
}