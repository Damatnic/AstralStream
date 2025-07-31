package com.astralplayer.nextplayer.vr

import android.net.Uri
import kotlinx.serialization.Serializable

// VR/AR State and Configuration
data class VRARState(
    val isInitialized: Boolean = false,
    val initializationTime: Long = 0L,
    val currentMode: VRARMode = VRARMode.NONE,
    val supportedModes: List<VRARMode> = emptyList(),
    val supportedProjections: List<ProjectionType> = emptyList(),
    val vrModeEnabled: Boolean = false,
    val arModeEnabled: Boolean = false,
    val vrConfig: VRModeConfig? = null,
    val arConfig: ARModeConfig? = null,
    val currentVideoUri: Uri? = null,
    val spatialFormat: SpatialVideoFormat? = null,
    val videoInfo: SpatialVideoInfo? = null,
    val spatialVideoLoaded: Boolean = false,
    val currentProjection: ProjectionType? = null,
    val projectionConfig: ProjectionConfig? = null,
    val headTrackingEnabled: Boolean = false,
    val stereoRenderingEnabled: Boolean = false,
    val gazeInteractionEnabled: Boolean = false,
    val handTrackingEnabled: Boolean = false,
    val cameraFeedEnabled: Boolean = false,
    val environmentTrackingEnabled: Boolean = false,
    val immersiveEffectsEnabled: Boolean = false,
    val immersiveEffectsConfig: ImmersiveEffectsConfig? = null,
    val gazeConfig: GazeInteractionConfig? = null,
    val handTrackingConfig: HandTrackingConfig? = null,
    val hasGyroscope: Boolean = false,
    val hasAccelerometer: Boolean = false,
    val hasMagnetometer: Boolean = false,
    val hasRotationVector: Boolean = false,
    val lastOrientationUpdate: Long = 0L
)

enum class VRARMode {
    NONE, VR, AR, MIXED_REALITY
}

enum class ProjectionType {
    EQUIRECTANGULAR, CUBEMAP, FISHEYE, STEREOSCOPIC, CYLINDRICAL, FLAT_SCREEN
}

enum class SpatialVideoFormat {
    EQUIRECTANGULAR_360,
    EQUIRECTANGULAR_180,
    CUBEMAP,
    FISHEYE,
    STEREOSCOPIC_SBS, // Side-by-side
    STEREOSCOPIC_TB   // Top-bottom
}

// VR Mode Configuration
data class VRModeConfig(
    val headTrackingConfig: HeadTrackingConfig = HeadTrackingConfig(),
    val stereoConfig: StereoRenderingConfig = StereoRenderingConfig(),
    val interactionConfig: VRInteractionConfig = VRInteractionConfig(),
    val renderingQuality: RenderingQuality = RenderingQuality.HIGH,
    val fieldOfView: Float = 90f,
    val ipd: Float = 63f, // Interpupillary distance in mm
    val enableComfortSettings: Boolean = true,
    val motionSicknessReduction: Boolean = true
)

data class HeadTrackingConfig(
    val enabled: Boolean = true,
    val predictionTime: Float = 18f, // milliseconds
    val smoothingFactor: Float = 0.8f,
    val deadZone: Float = 0.5f, // degrees
    val sensitivity: Float = 1.0f,
    val enablePositionalTracking: Boolean = false,
    val trackingMode: TrackingMode = TrackingMode.ORIENTATION_ONLY
)

enum class TrackingMode {
    ORIENTATION_ONLY, SIX_DOF, ROOM_SCALE
}

data class StereoRenderingConfig(
    val enabled: Boolean = true,
    val renderingMode: StereoRenderingMode = StereoRenderingMode.SIDE_BY_SIDE,
    val eyeSeparation: Float = 63f, // mm
    val convergenceDistance: Float = 2000f, // mm
    val enableAsynchronousTimeWarp: Boolean = true,
    val enableFoveatedRendering: Boolean = false,
    val antiAliasingLevel: Int = 4
)

enum class StereoRenderingMode {
    SIDE_BY_SIDE, TOP_BOTTOM, INTERLEAVED, ANAGLYPH
}

enum class RenderingQuality {
    LOW, MEDIUM, HIGH, ULTRA
}

data class VRInteractionConfig(
    val gestureConfig: VRGestureConfig = VRGestureConfig(),
    val gazeConfig: GazeInteractionConfig = GazeInteractionConfig(),
    val voiceConfig: VoiceInteractionConfig = VoiceInteractionConfig(),
    val controllerConfig: ControllerConfig = ControllerConfig()
)

data class VRGestureConfig(
    val enabled: Boolean = true,
    val supportedGestures: List<VRGesture> = listOf(
        VRGesture.TAP, VRGesture.SWIPE, VRGesture.PINCH, VRGesture.ROTATE
    ),
    val sensitivity: Float = 1.0f,
    val gestureTimeout: Long = 2000L
)

enum class VRGesture {
    TAP, DOUBLE_TAP, LONG_PRESS, SWIPE, PINCH, ROTATE, HEAD_NOD, HEAD_SHAKE
}

// AR Mode Configuration
data class ARModeConfig(
    val cameraConfig: CameraConfig = CameraConfig(),
    val trackingConfig: ARTrackingConfig = ARTrackingConfig(),
    val interactionConfig: ARInteractionConfig = ARInteractionConfig(),
    val renderingConfig: ARRenderingConfig = ARRenderingConfig(),
    val occlusionHandling: Boolean = true,
    val lightEstimation: Boolean = true,
    val planeDetection: Boolean = true
)

data class CameraConfig(
    val enabled: Boolean = true,
    val resolution: Pair<Int, Int> = Pair(1920, 1080),
    val frameRate: Float = 30f,
    val autoFocus: Boolean = true,
    val autoExposure: Boolean = true,
    val stabilization: Boolean = true
)

data class ARTrackingConfig(
    val environmentTracking: Boolean = true,
    val planeTracking: Boolean = true,
    val imageTracking: Boolean = false,
    val objectTracking: Boolean = false,
    val faceTracking: Boolean = false,
    val handTracking: Boolean = false,
    val trackingAccuracy: TrackingAccuracy = TrackingAccuracy.HIGH
)

enum class TrackingAccuracy {
    LOW, MEDIUM, HIGH, MAXIMUM
}

data class ARInteractionConfig(
    val touchEnabled: Boolean = true,
    val airTapEnabled: Boolean = false,
    val voiceEnabled: Boolean = false,
    val gestureEnabled: Boolean = true,
    val raycastingEnabled: Boolean = true
)

data class ARRenderingConfig(
    val occlusionMasking: Boolean = true,
    val shadowCasting: Boolean = true,
    val reflections: Boolean = false,
    val lightEstimation: Boolean = true,
    val hdrRendering: Boolean = false
)

// Gaze Interaction
data class GazeInteractionConfig(
    val enabled: Boolean = false,
    val dwellTime: Long = 1000L, // milliseconds
    val cursorVisible: Boolean = true,
    val cursorSize: Float = 0.02f,
    val interactionZones: List<InteractionZone> = emptyList(),
    val smoothingFactor: Float = 0.7f,
    val deadZone: Float = 1.0f // degrees
)

data class InteractionZone(
    val id: String,
    val bounds: BoundingBox,
    val action: InteractionAction,
    val dwellTime: Long = 1000L,
    val isActive: Boolean = true
)

data class BoundingBox(
    val center: Vector3,
    val size: Vector3,
    val rotation: Vector3 = Vector3(0f, 0f, 0f)
)

data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float
)

enum class InteractionAction {
    PLAY_PAUSE, SEEK_FORWARD, SEEK_BACKWARD, VOLUME_UP, VOLUME_DOWN, 
    FULLSCREEN, EXIT, MENU, SETTINGS, CUSTOM
}

// Hand Tracking
data class HandTrackingConfig(
    val enabled: Boolean = false,
    val trackingMode: HandTrackingMode = HandTrackingMode.SKELETON,
    val gestureConfig: GestureRecognitionConfig = GestureRecognitionConfig(),
    val handednessDetection: Boolean = true,
    val fingerTracking: Boolean = true,
    val confidenceThreshold: Float = 0.7f
)

enum class HandTrackingMode {
    BASIC, SKELETON, FULL_ARTICULATION
}

data class GestureRecognitionConfig(
    val enabled: Boolean = true,
    val supportedGestures: List<HandGesture> = listOf(
        HandGesture.POINT, HandGesture.GRAB, HandGesture.PINCH, HandGesture.THUMBS_UP
    ),
    val recognitionTimeout: Long = 500L,
    val confidenceThreshold: Float = 0.8f
)

enum class HandGesture {
    POINT, GRAB, PINCH, THUMBS_UP, THUMBS_DOWN, PEACE, FIST, OPEN_PALM, SWIPE
}

// Voice Interaction
data class VoiceInteractionConfig(
    val enabled: Boolean = false,
    val language: String = "en-US",
    val wakeWord: String = "Hey Player",
    val commandTimeout: Long = 3000L,
    val supportedCommands: List<VoiceCommand> = emptyList()
)

data class VoiceCommand(
    val command: String,
    val action: InteractionAction,
    val confidence: Float = 0.8f
)

// Controller Configuration
data class ControllerConfig(
    val enabled: Boolean = false,
    val controllerType: ControllerType = ControllerType.GENERIC,
    val buttonMappings: Map<String, InteractionAction> = emptyMap(),
    val hapticFeedback: Boolean = true,
    val trackpadSensitivity: Float = 1.0f
)

enum class ControllerType {
    GENERIC, OCULUS_TOUCH, VIVE_CONTROLLER, DAYDREAM, GEAR_VR
}

// Spatial Video Information
data class SpatialVideoInfo(
    val uri: Uri,
    val format: SpatialVideoFormat,
    val resolution: Pair<Int, Int>,
    val frameRate: Float,
    val duration: Long,
    val hasAudio: Boolean,
    val audioChannels: Int = 2,
    val bitrate: Long,
    val metadata: Map<String, String> = emptyMap()
)

// Projection Configuration
data class ProjectionConfig(
    val type: ProjectionType,
    val fieldOfView: Float,
    val aspectRatio: Float,
    val nearPlane: Float = 0.1f,
    val farPlane: Float = 1000f,
    val distortionCorrection: Boolean = true,
    val chromaCorrection: Boolean = false
)

data class ProjectionMapping(
    val format: SpatialVideoFormat,
    val uvMapping: Array<FloatArray>,
    val distortionCorrection: FloatArray,
    val fieldOfView: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProjectionMapping

        if (format != other.format) return false
        if (!uvMapping.contentDeepEquals(other.uvMapping)) return false
        if (!distortionCorrection.contentEquals(other.distortionCorrection)) return false
        if (fieldOfView != other.fieldOfView) return false

        return true
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + uvMapping.contentDeepHashCode()
        result = 31 * result + distortionCorrection.contentHashCode()
        result = 31 * result + fieldOfView.hashCode()
        return result
    }
}

// Immersive Effects
data class ImmersiveEffectsConfig(
    val spatialAudioEnabled: Boolean = false,
    val spatialAudioConfig: SpatialAudioConfig = SpatialAudioConfig(),
    val environmentalEffectsEnabled: Boolean = false,
    val environmentalEffectsConfig: EnvironmentalEffectsConfig = EnvironmentalEffectsConfig(),
    val hapticFeedbackEnabled: Boolean = false,
    val hapticConfig: HapticFeedbackConfig = HapticFeedbackConfig(),
    val visualEffectsEnabled: Boolean = false,
    val visualEffectsConfig: VisualEffectsConfig = VisualEffectsConfig(),
    val ambientLightingEnabled: Boolean = false,
    val windEffectsEnabled: Boolean = false
)

data class SpatialAudioConfig(
    val enabled: Boolean = true,
    val roomSize: RoomSize = RoomSize.MEDIUM,
    val reverbLevel: Float = 0.3f,
    val doppler: Boolean = true,
    val occlusionEnabled: Boolean = true,
    val reflectionEnabled: Boolean = true
)

enum class RoomSize {
    SMALL, MEDIUM, LARGE, OUTDOOR
}

data class EnvironmentalEffectsConfig(
    val ambientSounds: Boolean = true,
    val weatherEffects: Boolean = false,
    val timeOfDayEffects: Boolean = false,
    val seasonalEffects: Boolean = false,
    val environmentType: EnvironmentType = EnvironmentType.NEUTRAL
)

enum class EnvironmentType {
    NEUTRAL, FOREST, BEACH, CITY, SPACE, UNDERWATER, DESERT
}

data class HapticFeedbackConfig(
    val enabled: Boolean = false,
    val intensity: Float = 0.5f,
    val patterns: List<HapticPattern> = emptyList(),
    val audioDriven: Boolean = true,
    val visualDriven: Boolean = false
)

data class HapticPattern(
    val name: String,
    val duration: Long,
    val intensity: Float,
    val frequency: Float
)

data class VisualEffectsConfig(
    val particleEffects: Boolean = false,
    val lightingEffects: Boolean = true,
    val postProcessing: Boolean = true,
    val bloom: Boolean = false,
    val motionBlur: Boolean = false,
    val depthOfField: Boolean = false
)

// VR/AR Capabilities
data class VRARCapabilities(
    val supportedModes: List<VRARMode>,
    val supportedProjections: List<ProjectionType>,
    val maxResolution: Pair<Int, Int>,
    val maxFrameRate: Float,
    val hasSixDOFTracking: Boolean,
    val hasHandTracking: Boolean,
    val hasEyeTracking: Boolean,
    val supportsSpatialAudio: Boolean,
    val supportsHaptics: Boolean = false,
    val supportsWirelessStreaming: Boolean = false
)

// Metrics and Performance
data class VRARMetrics(
    val frameRate: Float,
    val renderingLatency: Long, // milliseconds
    val trackingAccuracy: Float, // 0.0 to 1.0
    val headTrackingLatency: Long, // milliseconds
    val batteryUsage: Float, // 0.0 to 1.0
    val thermalState: String,
    val memoryUsage: Long, // bytes
    val cpuUsage: Float, // 0.0 to 1.0
    val gpuUsage: Float, // 0.0 to 1.0
    val droppedFrames: Int = 0,
    val motionToPhotonLatency: Long = 0L,
    val lastUpdateTime: Long
)

data class VRRenderingInfo(
    val resolution: Pair<Int, Int>,
    val refreshRate: Float,
    val renderingAPI: String,
    val antiAliasing: String
)

data class ARTrackingInfo(
    val trackingState: String,
    val trackedFeatures: Int,
    val trackingAccuracy: Float,
    val environmentLighting: Float
)

// Events
sealed class VRAREvent {
    data class SystemInitialized(val timestamp: Long) : VRAREvent()
    data class VRModeEnabled(val config: VRModeConfig, val timestamp: Long) : VRAREvent()
    data class VRModeDisabled(val timestamp: Long) : VRAREvent()
    data class ARModeEnabled(val config: ARModeConfig, val timestamp: Long) : VRAREvent()
    data class ARModeDisabled(val timestamp: Long) : VRAREvent()
    data class SpatialVideoLoaded(val uri: Uri, val format: SpatialVideoFormat, val timestamp: Long) : VRAREvent()
    data class ProjectionChanged(val projectionType: ProjectionType, val timestamp: Long) : VRAREvent()
    data class HeadOrientationUpdated(val yaw: Float, val pitch: Float, val roll: Float, val timestamp: Long) : VRAREvent()
    data class GazeInteractionEnabled(val config: GazeInteractionConfig, val timestamp: Long) : VRAREvent()
    data class GazeInteractionDisabled(val timestamp: Long) : VRAREvent()
    data class HandTrackingEnabled(val config: HandTrackingConfig, val timestamp: Long) : VRAREvent()
    data class HandTrackingDisabled(val timestamp: Long) : VRAREvent()
    data class GestureDetected(val gesture: String, val confidence: Float, val timestamp: Long) : VRAREvent()
    data class ImmersiveEffectsApplied(val config: ImmersiveEffectsConfig, val timestamp: Long) : VRAREvent()
    data class SensorAccuracyChanged(val sensorType: Int, val accuracy: Int, val timestamp: Long) : VRAREvent()
    data class TrackingLost(val trackingType: String, val timestamp: Long) : VRAREvent()
    data class TrackingRecovered(val trackingType: String, val timestamp: Long) : VRAREvent()
    data class PerformanceWarning(val metric: String, val value: Float, val threshold: Float, val timestamp: Long) : VRAREvent()
    data class VRARError(val error: String, val timestamp: Long) : VRAREvent()
}

// Result Classes
data class VRARInitializationResult(
    val success: Boolean,
    val capabilities: VRARCapabilities? = null,
    val initializationTime: Long = 0L,
    val error: String? = null
)

data class VRModeResult(
    val success: Boolean,
    val enabledFeatures: List<String> = emptyList(),
    val renderingInfo: VRRenderingInfo? = null,
    val enableTime: Long = 0L,
    val error: String? = null
)

data class ARModeResult(
    val success: Boolean,
    val enabledFeatures: List<String> = emptyList(),
    val trackingInfo: ARTrackingInfo? = null,
    val enableTime: Long = 0L,
    val error: String? = null
)

data class SpatialVideoResult(
    val success: Boolean,
    val videoInfo: SpatialVideoInfo? = null,
    val textureIds: List<Int> = emptyList(),
    val projectionMapping: ProjectionMapping? = null,
    val loadTime: Long = 0L,
    val error: String? = null
)

data class ProjectionResult(
    val success: Boolean,
    val projectionType: ProjectionType? = null,
    val fieldOfView: Float = 0f,
    val aspectRatio: Float = 0f,
    val configureTime: Long = 0L,
    val error: String? = null
)

data class GazeInteractionResult(
    val success: Boolean,
    val trackingAccuracy: Float = 0f,
    val interactionZones: Int = 0,
    val enableTime: Long = 0L,
    val error: String? = null
)

data class HandTrackingResult(
    val success: Boolean,
    val trackingAccuracy: Float = 0f,
    val supportedGestures: List<HandGesture> = emptyList(),
    val enableTime: Long = 0L,
    val error: String? = null
)

data class ImmersiveEffectsResult(
    val success: Boolean,
    val appliedEffects: List<String> = emptyList(),
    val performanceImpact: Float = 0f,
    val applyTime: Long = 0L,
    val error: String? = null
)

// Texture Management
data class TextureResult(
    val textureIds: List<Int>,
    val format: String,
    val resolution: Pair<Int, Int>
)

// Component Interfaces (for implementation)
interface VRRenderer {
    fun configure(config: VRModeConfig)
    fun updateOrientation(rotationMatrix: FloatArray)
    fun cleanup()
}

interface StereoRenderer {
    fun configure(config: StereoRenderingConfig)
    fun updateOrientation(rotationMatrix: FloatArray)
    fun cleanup()
}

interface ProjectionRenderer {
    fun configure(config: ProjectionConfig)
    fun cleanup()
}

interface ARRenderer {
    fun configure(config: ARModeConfig)
    fun enableCameraFeed(config: CameraConfig)
    fun updateOrientation(rotationMatrix: FloatArray)
    fun cleanup()
}

interface VideoTextureManager {
    fun createSpatialTextures(videoInfo: SpatialVideoInfo): TextureResult
    fun cleanup()
}

interface SpatialVideoProcessor {
    fun configure(format: SpatialVideoFormat, videoInfo: SpatialVideoInfo)
    fun cleanup()
}

interface ImmersiveEffectsProcessor {
    fun cleanup()
}

interface VRGestureDetector {
    fun configure(config: VRGestureConfig)
    fun onTouchEvent(event: android.view.MotionEvent): Boolean
    fun cleanup()
}

interface GazeTracker {
    fun configure(config: GazeInteractionConfig)
    fun startTracking()
    fun getTrackingAccuracy(): Float
    fun cleanup()
}

interface HandTracker {
    fun configure(config: HandTrackingConfig)
    fun startTracking()
    fun getTrackingAccuracy(): Float
    fun cleanup()
}

// Basic Implementations for Testing
class VRRenderer(private val context: android.content.Context) : VRRenderer {
    override fun configure(config: VRModeConfig) {}
    override fun updateOrientation(rotationMatrix: FloatArray) {}
    override fun cleanup() {}
}

class StereoRenderer : StereoRenderer {
    override fun configure(config: StereoRenderingConfig) {}
    override fun updateOrientation(rotationMatrix: FloatArray) {}
    override fun cleanup() {}
}

class ProjectionRenderer : ProjectionRenderer {
    override fun configure(config: ProjectionConfig) {}
    override fun cleanup() {}
}

class ARRenderer(private val context: android.content.Context) : ARRenderer {
    override fun configure(config: ARModeConfig) {}
    override fun enableCameraFeed(config: CameraConfig) {}
    override fun updateOrientation(rotationMatrix: FloatArray) {}
    override fun cleanup() {}
}

class VideoTextureManager : VideoTextureManager {
    override fun createSpatialTextures(videoInfo: SpatialVideoInfo): TextureResult {
        return TextureResult(
            textureIds = listOf(1, 2), // Mock texture IDs
            format = "RGB",
            resolution = videoInfo.resolution
        )
    }
    override fun cleanup() {}
}

class SpatialVideoProcessor : SpatialVideoProcessor {
    override fun configure(format: SpatialVideoFormat, videoInfo: SpatialVideoInfo) {}
    override fun cleanup() {}
}

class ImmersiveEffectsProcessor : ImmersiveEffectsProcessor {
    override fun cleanup() {}
}

class VRGestureDetector : VRGestureDetector {
    override fun configure(config: VRGestureConfig) {}
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean = false
    override fun cleanup() {}
}

class GazeTracker : GazeTracker {
    override fun configure(config: GazeInteractionConfig) {}
    override fun startTracking() {}
    override fun getTrackingAccuracy(): Float = 0.9f
    override fun cleanup() {}
}

class HandTracker(private val context: android.content.Context) : HandTracker {
    override fun configure(config: HandTrackingConfig) {}
    override fun startTracking() {}
    override fun getTrackingAccuracy(): Float = 0.85f
    override fun cleanup() {}
}