package com.astralplayer.nextplayer.immersive

import androidx.media3.common.MediaItem
import java.util.*

/**
 * Data models for immersive media features
 */

// Core Viewing Modes
enum class ViewingMode {
    TRADITIONAL,
    VR_HEADSET,
    VR_CARDBOARD,
    SPHERICAL_360,
    AR_OVERLAY,
    IMMERSIVE_FULLSCREEN
}

// Spherical Projection Types
enum class SphericalProjection {
    EQUIRECTANGULAR,
    CUBIC,
    CYLINDRICAL
}

// AR Overlay Types
enum class AROverlayType {
    TEXT,
    IMAGE,
    VIDEO,
    MODEL_3D,
    INTERACTIVE_BUTTON
}

// AR Tracking Mode
enum class ARTrackingMode {
    WORLD_TRACKING,
    FACE_TRACKING,
    IMAGE_TRACKING,
    OBJECT_TRACKING
}

// Core Data Classes
data class Quaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float
) {
    companion object {
        fun identity() = Quaternion(0f, 0f, 0f, 1f)
    }
}

data class Vector3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    companion object {
        fun zero() = Vector3D(0f, 0f, 0f)
        fun one() = Vector3D(1f, 1f, 1f)
    }
}

// Configuration Classes
data class VRConfiguration(
    val ipd: Float = 63f, // Interpupillary distance in mm
    val enableEyeTracking: Boolean = false,
    val fovLeft: Float = 90f,
    val fovRight: Float = 90f,
    val enableHandTracking: Boolean = false
)

data class ARConfiguration(
    val enableCameraPassThrough: Boolean = true,
    val trackingMode: ARTrackingMode = ARTrackingMode.WORLD_TRACKING,
    val enableOcclusion: Boolean = false,
    val enableLightEstimation: Boolean = true
)

data class ImmersiveViewConfiguration(
    val adaptiveBrightness: Boolean = true,
    val hideSystemUI: Boolean = true,
    val enableGestureControls: Boolean = true,
    val autoRotateWithDevice: Boolean = true
)

data class ImmersiveConfiguration(
    val vrConfig: VRConfiguration = VRConfiguration(),
    val arConfig: ARConfiguration = ARConfiguration(),
    val immersiveConfig: ImmersiveViewConfiguration = ImmersiveViewConfiguration()
)

// AR Overlay Classes
data class AROverlay(
    val id: String,
    val type: AROverlayType,
    val content: String, // Text content, image path, etc.
    val position: Vector3D,
    val rotation: Quaternion,
    val scale: Vector3D,
    val isVisible: Boolean = true,
    val isInteractive: Boolean = false,
    val opacity: Float = 1f
)

data class AROverlayUpdate(
    val overlayId: String,
    val position: Vector3D? = null,
    val rotation: Quaternion? = null,
    val scale: Vector3D? = null,
    val opacity: Float? = null,
    val isVisible: Boolean? = null
)

// Session Management
data class ImmersiveSession(
    val id: String,
    val mediaItem: MediaItem,
    val viewingMode: ViewingMode,
    val configuration: ImmersiveConfiguration,
    val startTime: Long,
    val endTime: Long? = null
)

data class ImmersiveSessionStats(
    val duration: Long,
    val viewingMode: ViewingMode,
    val headMovements: Int,
    val modeChanges: Int,
    val overlaysUsed: Int
)

sealed class ImmersiveSessionResult {
    data class Success(val stats: ImmersiveSessionStats) : ImmersiveSessionResult()
    data class Error(val message: String) : ImmersiveSessionResult()
}

// Capabilities and Features
data class ImmersiveCapabilities(
    val supportsVR: Boolean,
    val supportsAR: Boolean,
    val supportsSpherical: Boolean,
    val supportsGyroscope: Boolean,
    val supportedProjections: List<SphericalProjection>
)

// Eye Tracking
data class EyeTrackingData(
    val leftEyePosition: Vector3D,
    val rightEyePosition: Vector3D,
    val gazeDirection: Vector3D,
    val pupilDilation: Float,
    val blinkState: BlinkState
)

enum class BlinkState {
    OPEN,
    CLOSING,
    CLOSED,
    OPENING
}

// VR Calibration
data class VRCalibrationData(
    val ipd: Float,
    val screenToLensDistance: Float,
    val fov: Float,
    val distortionCoefficients: FloatArray = floatArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VRCalibrationData

        if (ipd != other.ipd) return false
        if (screenToLensDistance != other.screenToLensDistance) return false
        if (fov != other.fov) return false
        if (!distortionCoefficients.contentEquals(other.distortionCoefficients)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ipd.hashCode()
        result = 31 * result + screenToLensDistance.hashCode()
        result = 31 * result + fov.hashCode()
        result = 31 * result + distortionCoefficients.contentHashCode()
        return result
    }
}

// Screenshot and Capture
data class ImmersiveScreenshot(
    val viewingMode: ViewingMode,
    val timestamp: Long,
    val imageData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImmersiveScreenshot

        if (viewingMode != other.viewingMode) return false
        if (timestamp != other.timestamp) return false
        if (!imageData.contentEquals(other.imageData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = viewingMode.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + imageData.contentHashCode()
        return result
    }
}

// Callbacks Interface
interface ImmersiveMediaCallbacks {
    fun onImmersiveMediaInitialized()
    fun onImmersiveSessionStarted(session: ImmersiveSession)
    fun onViewingModeChanged(mode: ViewingMode)
    fun onHeadOrientationChanged(orientation: Quaternion)
    fun onEyeTrackingUpdate(data: EyeTrackingData)
    fun onAROverlayAdded(overlay: AROverlay)
    fun onVRCalibrationComplete(data: VRCalibrationData)
    fun onImmersiveSessionEnded(result: ImmersiveSessionResult)
    fun onImmersiveError(error: String)
}