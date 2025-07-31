package com.astralplayer.nextplayer.data.gesture

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.astralplayer.nextplayer.data.EnhancedGestureSettings
import com.astralplayer.nextplayer.data.gesture.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages device-specific adaptations for gesture system
 */
class DeviceAdaptationManager(
    private val context: Context
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics()
    
    private val _deviceProfile = MutableStateFlow(detectDeviceProfile())
    val deviceProfile: StateFlow<DeviceProfile> = _deviceProfile.asStateFlow()
    
    private val _orientationState = MutableStateFlow(detectOrientation())
    val orientationState: StateFlow<OrientationState> = _orientationState.asStateFlow()
    
    init {
        updateDeviceMetrics()
    }
    
    /**
     * Update device metrics and profile
     */
    fun updateDeviceMetrics() {
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        _deviceProfile.value = detectDeviceProfile()
        _orientationState.value = detectOrientation()
    }
    
    /**
     * Detect device profile based on screen characteristics
     */
    private fun detectDeviceProfile(): DeviceProfile {
        val screenSizeInches = getScreenSizeInches()
        val aspectRatio = getAspectRatio()
        val density = displayMetrics.density
        
        val deviceType = when {
            screenSizeInches >= 10 -> DeviceType.TABLET
            screenSizeInches >= 7 -> DeviceType.LARGE_PHONE
            screenSizeInches >= 5 -> DeviceType.NORMAL_PHONE
            else -> DeviceType.SMALL_PHONE
        }
        
        val formFactor = when {
            isFoldable() -> FormFactor.FOLDABLE
            aspectRatio > 2.0f -> FormFactor.TALL_PHONE
            deviceType == DeviceType.TABLET -> FormFactor.TABLET
            else -> FormFactor.STANDARD
        }
        
        return DeviceProfile(
            deviceType = deviceType,
            formFactor = formFactor,
            screenWidthDp = (displayMetrics.widthPixels / density).toInt(),
            screenHeightDp = (displayMetrics.heightPixels / density).toInt(),
            screenDensity = density,
            screenSizeInches = screenSizeInches,
            aspectRatio = aspectRatio,
            hasNotch = hasNotch(),
            hasCutout = hasCutout(),
            isRoundScreen = isRoundScreen(),
            refreshRate = getRefreshRate()
        )
    }
    
    /**
     * Detect current orientation state
     */
    private fun detectOrientation(): OrientationState {
        val orientation = context.resources.configuration.orientation
        val rotation = windowManager.defaultDisplay.rotation
        
        return OrientationState(
            orientation = when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> Orientation.LANDSCAPE
                Configuration.ORIENTATION_PORTRAIT -> Orientation.PORTRAIT
                else -> Orientation.UNDEFINED
            },
            rotation = when (rotation) {
                android.view.Surface.ROTATION_0 -> 0
                android.view.Surface.ROTATION_90 -> 90
                android.view.Surface.ROTATION_180 -> 180
                android.view.Surface.ROTATION_270 -> 270
                else -> 0
            },
            isReverseLandscape = rotation == android.view.Surface.ROTATION_270,
            isReversePortrait = rotation == android.view.Surface.ROTATION_180
        )
    }
    
    /**
     * Get adapted gesture settings based on device profile
     */
    fun getAdaptedGestureSettings(baseSettings: EnhancedGestureSettings): EnhancedGestureSettings {
        val profile = _deviceProfile.value
        val orientation = _orientationState.value
        
        return baseSettings.copy(
            general = baseSettings.general.copy(
                minimumGestureDistance = adaptSwipeThreshold(baseSettings.general.minimumGestureDistance, profile)
            ),
            seeking = baseSettings.seeking.copy(
                sensitivity = adaptSeekSensitivity(baseSettings.seeking.sensitivity, profile),
                fineSeekThreshold = adaptFineSeekThreshold(baseSettings.seeking.fineSeekThreshold, profile)
            ),
            volume = baseSettings.volume.copy(
                sensitivity = adaptVolumeThreshold(baseSettings.volume.sensitivity, profile, orientation)
            ),
            brightness = baseSettings.brightness.copy(
                sensitivity = adaptBrightnessThreshold(baseSettings.brightness.sensitivity, profile, orientation)
            ),
            doubleTap = baseSettings.doubleTap.copy(
                tapTimeout = adaptDoubleTapTimeout(baseSettings.doubleTap.tapTimeout, profile),
                maxTapDistance = adaptMaxTapDistance(baseSettings.doubleTap.maxTapDistance, profile)
            ),
            longPress = baseSettings.longPress.copy(
                triggerDuration = adaptLongPressDelay(baseSettings.longPress.triggerDuration, profile)
            ),
            pinchZoom = baseSettings.pinchZoom.copy(
                minZoom = adaptPinchScale(baseSettings.pinchZoom.minZoom, profile),
                maxZoom = adaptPinchScale(baseSettings.pinchZoom.maxZoom, profile)
            )
        )
    }
    
    /**
     * Adapt sensitivity based on device characteristics
     */
    private fun adaptSensitivity(
        baseSensitivity: Float,
        profile: DeviceProfile,
        orientation: OrientationState
    ): Float {
        var sensitivity = baseSensitivity
        
        // Adjust for device type
        sensitivity *= when (profile.deviceType) {
            DeviceType.TABLET -> 0.8f // Reduce sensitivity on tablets
            DeviceType.LARGE_PHONE -> 0.9f
            DeviceType.NORMAL_PHONE -> 1.0f
            DeviceType.SMALL_PHONE -> 1.1f // Increase on small phones
        }
        
        // Adjust for form factor
        sensitivity *= when (profile.formFactor) {
            FormFactor.FOLDABLE -> if (isFolded()) 1.2f else 0.9f
            FormFactor.TALL_PHONE -> 0.95f
            else -> 1.0f
        }
        
        // Adjust for orientation
        if (orientation.orientation == Orientation.LANDSCAPE) {
            sensitivity *= 1.1f // Slightly increase in landscape
        }
        
        return sensitivity.coerceIn(0.1f, 2.0f)
    }
    
    /**
     * Adapt swipe threshold for device size
     */
    private fun adaptSwipeThreshold(baseThreshold: Float, profile: DeviceProfile): Float {
        return baseThreshold * when (profile.deviceType) {
            DeviceType.TABLET -> 1.5f // Larger threshold for tablets
            DeviceType.LARGE_PHONE -> 1.2f
            DeviceType.NORMAL_PHONE -> 1.0f
            DeviceType.SMALL_PHONE -> 0.8f
        }
    }
    
    /**
     * Adapt seek sensitivity for screen size
     */
    private fun adaptSeekSensitivity(baseSensitivity: Float, profile: DeviceProfile): Float {
        // Larger screens need less sensitive seeking
        val sizeFactor = (profile.screenSizeInches / 6.0f).coerceIn(0.5f, 1.5f)
        return baseSensitivity / sizeFactor
    }
    
    /**
     * Adapt fine seek threshold
     */
    private fun adaptFineSeekThreshold(baseThreshold: Float, profile: DeviceProfile): Float {
        return baseThreshold * profile.screenDensity
    }
    
    /**
     * Adapt volume threshold for orientation
     */
    private fun adaptVolumeThreshold(
        baseThreshold: Float,
        profile: DeviceProfile,
        orientation: OrientationState
    ): Float {
        var threshold = baseThreshold
        
        // In landscape, vertical space is limited
        if (orientation.orientation == Orientation.LANDSCAPE) {
            threshold *= 0.8f
        }
        
        // Adjust for screen height
        val heightFactor = (profile.screenHeightDp / 800f).coerceIn(0.7f, 1.3f)
        threshold *= heightFactor
        
        return threshold
    }
    
    /**
     * Adapt brightness threshold
     */
    private fun adaptBrightnessThreshold(
        baseThreshold: Float,
        profile: DeviceProfile,
        orientation: OrientationState
    ): Float {
        // Similar to volume but might have different scaling
        return adaptVolumeThreshold(baseThreshold, profile, orientation)
    }
    
    /**
     * Adapt double tap timeout for device performance
     */
    private fun adaptDoubleTapTimeout(baseTimeout: Long, profile: DeviceProfile): Long {
        // Slower devices might need more time
        val performanceFactor = when {
            profile.refreshRate >= 120 -> 0.9f // Fast device
            profile.refreshRate >= 90 -> 0.95f
            profile.refreshRate >= 60 -> 1.0f
            else -> 1.1f // Slower device
        }
        
        return (baseTimeout * performanceFactor).toLong()
    }
    
    /**
     * Adapt max tap distance for screen density
     */
    private fun adaptMaxTapDistance(baseDistance: Float, profile: DeviceProfile): Float {
        return baseDistance * profile.screenDensity
    }
    
    /**
     * Adapt long press delay
     */
    private fun adaptLongPressDelay(baseDelay: Long, profile: DeviceProfile): Long {
        // Tablets might need slightly longer delays
        return when (profile.deviceType) {
            DeviceType.TABLET -> (baseDelay * 1.1f).toLong()
            else -> baseDelay
        }
    }
    
    /**
     * Adapt pinch zoom scale limits
     */
    private fun adaptPinchScale(baseScale: Float, profile: DeviceProfile): Float {
        // Smaller screens might need different zoom limits
        return when (profile.deviceType) {
            DeviceType.SMALL_PHONE -> baseScale * 0.8f
            DeviceType.TABLET -> baseScale * 1.2f
            else -> baseScale
        }
    }
    
    // Helper methods
    
    private fun getScreenSizeInches(): Float {
        val widthInches = displayMetrics.widthPixels / displayMetrics.xdpi
        val heightInches = displayMetrics.heightPixels / displayMetrics.ydpi
        return kotlin.math.sqrt(widthInches * widthInches + heightInches * heightInches)
    }
    
    private fun getAspectRatio(): Float {
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        return kotlin.math.max(width, height) / kotlin.math.min(width, height)
    }
    
    private fun hasNotch(): Boolean {
        // Simple heuristic for notch detection
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            getAspectRatio() > 2.0f
    }
    
    private fun hasCutout(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            windowManager.defaultDisplay.cutout != null
        } else false
    }
    
    private fun isRoundScreen(): Boolean {
        return context.resources.configuration.isScreenRound
    }
    
    private fun getRefreshRate(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = windowManager.defaultDisplay
            display.mode.refreshRate.toInt()
        } else {
            60 // Default refresh rate
        }
    }
    
    private fun isFoldable(): Boolean {
        // Check for foldable device characteristics
        return context.packageManager.hasSystemFeature("com.samsung.feature.device_category_foldable") ||
            context.packageManager.hasSystemFeature("com.google.android.feature.FOLDABLE")
    }
    
    private fun isFolded(): Boolean {
        // This would need actual fold state detection
        // For now, use aspect ratio as a heuristic
        return getAspectRatio() < 1.5f && isFoldable()
    }
    
    /**
     * Device profile data
     */
    data class DeviceProfile(
        val deviceType: DeviceType,
        val formFactor: FormFactor,
        val screenWidthDp: Int,
        val screenHeightDp: Int,
        val screenDensity: Float,
        val screenSizeInches: Float,
        val aspectRatio: Float,
        val hasNotch: Boolean,
        val hasCutout: Boolean,
        val isRoundScreen: Boolean,
        val refreshRate: Int
    )
    
    /**
     * Orientation state
     */
    data class OrientationState(
        val orientation: Orientation,
        val rotation: Int,
        val isReverseLandscape: Boolean,
        val isReversePortrait: Boolean
    )
    
    enum class DeviceType {
        SMALL_PHONE,
        NORMAL_PHONE,
        LARGE_PHONE,
        TABLET
    }
    
    enum class FormFactor {
        STANDARD,
        TALL_PHONE,
        TABLET,
        FOLDABLE
    }
    
    enum class Orientation {
        PORTRAIT,
        LANDSCAPE,
        UNDEFINED
    }
}

/**
 * Composable helper to observe device adaptation
 */
@Composable
fun rememberDeviceAdaptation(): DeviceAdaptationState {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    return produceState(
        initialValue = DeviceAdaptationState(
            orientation = when (configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> DeviceAdaptationManager.Orientation.LANDSCAPE
                Configuration.ORIENTATION_PORTRAIT -> DeviceAdaptationManager.Orientation.PORTRAIT
                else -> DeviceAdaptationManager.Orientation.UNDEFINED
            },
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            density = density.density,
            fontScale = density.fontScale
        ),
        configuration,
        density
    ) {
        value = DeviceAdaptationState(
            orientation = when (configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> DeviceAdaptationManager.Orientation.LANDSCAPE
                Configuration.ORIENTATION_PORTRAIT -> DeviceAdaptationManager.Orientation.PORTRAIT
                else -> DeviceAdaptationManager.Orientation.UNDEFINED
            },
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            density = density.density,
            fontScale = density.fontScale
        )
    }.value
}

data class DeviceAdaptationState(
    val orientation: DeviceAdaptationManager.Orientation,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val density: Float,
    val fontScale: Float
)