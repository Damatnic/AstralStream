package com.astralplayer.nextplayer.data.gesture

import android.content.res.Configuration
import androidx.compose.ui.geometry.Offset
import com.astralplayer.nextplayer.data.GestureAction
import com.astralplayer.nextplayer.data.TouchSide
import com.astralplayer.nextplayer.data.GestureType
import com.astralplayer.nextplayer.data.EnhancedGestureSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles gesture detection with orientation awareness
 */
class OrientationAwareGestureHandler(
    private val deviceAdaptationManager: DeviceAdaptationManager
) {
    private val _orientationGestureState = MutableStateFlow(OrientationGestureState())
    val orientationGestureState: StateFlow<OrientationGestureState> = _orientationGestureState.asStateFlow()
    
    /**
     * Process gesture with orientation adaptation
     */
    fun processOrientationAwareGesture(
        gestureType: GestureType,
        position: Offset,
        delta: Offset,
        screenSize: Offset,
        baseSettings: EnhancedGestureSettings
    ): GestureAction? {
        val orientation = deviceAdaptationManager.orientationState.value
        val adaptedSettings = deviceAdaptationManager.getAdaptedGestureSettings(baseSettings)
        
        return when (gestureType) {
            GestureType.HORIZONTAL_SEEK -> processOrientationAwareSeek(
                position, delta, screenSize, orientation, adaptedSettings
            )
            GestureType.VERTICAL_VOLUME -> processOrientationAwareVolume(
                position, delta, screenSize, orientation, adaptedSettings
            )
            GestureType.VERTICAL_BRIGHTNESS -> processOrientationAwareBrightness(
                position, delta, screenSize, orientation, adaptedSettings
            )
            GestureType.DOUBLE_TAP -> processOrientationAwareDoubleTap(
                position, screenSize, orientation, adaptedSettings
            )
            else -> null
        }
    }
    
    /**
     * Process seek gesture with orientation awareness
     */
    private fun processOrientationAwareSeek(
        position: Offset,
        delta: Offset,
        screenSize: Offset,
        orientation: DeviceAdaptationManager.OrientationState,
        settings: EnhancedGestureSettings
    ): GestureAction.Seek {
        val effectiveDelta = when (orientation.orientation) {
            DeviceAdaptationManager.Orientation.LANDSCAPE -> {
                // In landscape, horizontal space is more available
                delta.x * 1.2f
            }
            DeviceAdaptationManager.Orientation.PORTRAIT -> {
                // In portrait, be more sensitive to horizontal swipes
                delta.x * 0.8f
            }
            else -> delta.x
        }
        
        // Adjust for reverse orientations
        val adjustedDelta = if (orientation.isReverseLandscape || orientation.isReversePortrait) {
            -effectiveDelta
        } else {
            effectiveDelta
        }
        
        val seekAmount = (adjustedDelta / screenSize.x) * settings.seeking.seekStepSize
        
        return GestureAction.Seek(seekAmount.toLong())
    }
    
    /**
     * Process volume gesture with orientation awareness
     */
    private fun processOrientationAwareVolume(
        position: Offset,
        delta: Offset,
        screenSize: Offset,
        orientation: DeviceAdaptationManager.OrientationState,
        settings: EnhancedGestureSettings
    ): GestureAction.VolumeChange? {
        // Determine which side is "right" based on orientation
        val effectiveSide = getEffectiveSide(position, screenSize, orientation)
        
        if (effectiveSide != TouchSide.RIGHT) return null
        
        val effectiveDelta = when (orientation.orientation) {
            DeviceAdaptationManager.Orientation.LANDSCAPE -> {
                // Less vertical space in landscape
                -delta.y * 1.5f
            }
            DeviceAdaptationManager.Orientation.PORTRAIT -> {
                -delta.y
            }
            else -> -delta.y
        }
        
        val volumeChange = (effectiveDelta / screenSize.y) * settings.volume.sensitivity
        
        return GestureAction.VolumeChange(volumeChange, effectiveSide)
    }
    
    /**
     * Process brightness gesture with orientation awareness
     */
    private fun processOrientationAwareBrightness(
        position: Offset,
        delta: Offset,
        screenSize: Offset,
        orientation: DeviceAdaptationManager.OrientationState,
        settings: EnhancedGestureSettings
    ): GestureAction.BrightnessChange? {
        // Determine which side is "left" based on orientation
        val effectiveSide = getEffectiveSide(position, screenSize, orientation)
        
        if (effectiveSide != TouchSide.LEFT) return null
        
        val effectiveDelta = when (orientation.orientation) {
            DeviceAdaptationManager.Orientation.LANDSCAPE -> {
                -delta.y * 1.5f
            }
            DeviceAdaptationManager.Orientation.PORTRAIT -> {
                -delta.y
            }
            else -> -delta.y
        }
        
        val brightnessChange = (effectiveDelta / screenSize.y) * settings.brightness.sensitivity
        
        return GestureAction.BrightnessChange(brightnessChange, effectiveSide)
    }
    
    /**
     * Process double tap with orientation awareness
     */
    private fun processOrientationAwareDoubleTap(
        position: Offset,
        screenSize: Offset,
        orientation: DeviceAdaptationManager.OrientationState,
        settings: EnhancedGestureSettings
    ): GestureAction.DoubleTapSeek {
        val effectiveSide = getEffectiveSide(position, screenSize, orientation)
        val isForward = effectiveSide == TouchSide.RIGHT
        
        // Adjust seek amount based on orientation
        val seekAmount = when (orientation.orientation) {
            DeviceAdaptationManager.Orientation.LANDSCAPE -> {
                // Might want longer seeks in landscape (watching full screen)
                settings.doubleTap.seekAmount * 1.5f
            }
            else -> settings.doubleTap.seekAmount.toFloat()
        }.toLong()
        
        return GestureAction.DoubleTapSeek(
            forward = isForward,
            amount = seekAmount,
            side = effectiveSide
        )
    }
    
    /**
     * Get effective touch side based on orientation
     */
    private fun getEffectiveSide(
        position: Offset,
        screenSize: Offset,
        orientation: DeviceAdaptationManager.OrientationState
    ): TouchSide {
        val normalizedX = position.x / screenSize.x
        
        return when (orientation.rotation) {
            0 -> { // Normal portrait
                when {
                    normalizedX < 0.33f -> TouchSide.LEFT
                    normalizedX > 0.67f -> TouchSide.RIGHT
                    else -> TouchSide.CENTER
                }
            }
            90 -> { // Landscape (rotated left)
                when {
                    normalizedX < 0.33f -> TouchSide.LEFT
                    normalizedX > 0.67f -> TouchSide.RIGHT
                    else -> TouchSide.CENTER
                }
            }
            180 -> { // Reverse portrait
                when {
                    normalizedX < 0.33f -> TouchSide.RIGHT // Inverted
                    normalizedX > 0.67f -> TouchSide.LEFT  // Inverted
                    else -> TouchSide.CENTER
                }
            }
            270 -> { // Reverse landscape (rotated right)
                when {
                    normalizedX < 0.33f -> TouchSide.RIGHT // Inverted
                    normalizedX > 0.67f -> TouchSide.LEFT  // Inverted
                    else -> TouchSide.CENTER
                }
            }
            else -> {
                // Default behavior
                when {
                    normalizedX < 0.33f -> TouchSide.LEFT
                    normalizedX > 0.67f -> TouchSide.RIGHT
                    else -> TouchSide.CENTER
                }
            }
        }
    }
    
    /**
     * Update orientation state
     */
    fun updateOrientationState(
        orientation: DeviceAdaptationManager.Orientation,
        rotation: Int
    ) {
        _orientationGestureState.value = _orientationGestureState.value.copy(
            currentOrientation = orientation,
            currentRotation = rotation,
            lastOrientationChange = System.currentTimeMillis()
        )
    }
    
    /**
     * Get gesture zones for current orientation
     */
    fun getOrientationAwareGestureZones(
        screenSize: Offset,
        orientation: DeviceAdaptationManager.OrientationState
    ): GestureZones {
        return when (orientation.orientation) {
            DeviceAdaptationManager.Orientation.LANDSCAPE -> {
                // In landscape, might want different zone sizes
                GestureZones(
                    leftZoneWidth = screenSize.x * 0.25f, // Smaller side zones
                    rightZoneWidth = screenSize.x * 0.25f,
                    centerZoneWidth = screenSize.x * 0.5f, // Larger center zone
                    topZoneHeight = screenSize.y * 0.2f,
                    bottomZoneHeight = screenSize.y * 0.2f
                )
            }
            DeviceAdaptationManager.Orientation.PORTRAIT -> {
                // Standard portrait zones
                GestureZones(
                    leftZoneWidth = screenSize.x * 0.3f,
                    rightZoneWidth = screenSize.x * 0.3f,
                    centerZoneWidth = screenSize.x * 0.4f,
                    topZoneHeight = screenSize.y * 0.15f,
                    bottomZoneHeight = screenSize.y * 0.15f
                )
            }
            else -> {
                // Default zones
                GestureZones(
                    leftZoneWidth = screenSize.x * 0.3f,
                    rightZoneWidth = screenSize.x * 0.3f,
                    centerZoneWidth = screenSize.x * 0.4f,
                    topZoneHeight = screenSize.y * 0.15f,
                    bottomZoneHeight = screenSize.y * 0.15f
                )
            }
        }
    }
    
    data class OrientationGestureState(
        val currentOrientation: DeviceAdaptationManager.Orientation = DeviceAdaptationManager.Orientation.PORTRAIT,
        val currentRotation: Int = 0,
        val lastOrientationChange: Long = 0,
        val isTransitioning: Boolean = false
    )
    
    data class GestureZones(
        val leftZoneWidth: Float,
        val rightZoneWidth: Float,
        val centerZoneWidth: Float,
        val topZoneHeight: Float,
        val bottomZoneHeight: Float
    )
}

/**
 * Tablet-specific gesture adaptations
 */
class TabletGestureAdapter {
    /**
     * Adapt gestures for tablet form factor
     */
    fun adaptGesturesForTablet(
        baseSettings: EnhancedGestureSettings,
        screenSize: Offset,
        isLandscape: Boolean
    ): EnhancedGestureSettings {
        return baseSettings.copy(
            general = baseSettings.general.copy(
                // Tablets need larger gesture distances
                minimumGestureDistance = baseSettings.general.minimumGestureDistance * 1.5f
            ),
            seeking = baseSettings.seeking.copy(
                // More precise seeking on larger screens
                sensitivity = baseSettings.seeking.sensitivity * 0.7f,
                enableFineSeek = true,
                fineSeekThreshold = 0.3f // Enable fine seek more easily
            ),
            doubleTap = baseSettings.doubleTap.copy(
                // Larger tap areas on tablets
                maxTapDistance = baseSettings.doubleTap.maxTapDistance * 1.5f
            ),
            pinchZoom = baseSettings.pinchZoom.copy(
                // More zoom range on tablets
                maxZoom = 4.0f
            )
        )
    }
    
    /**
     * Get tablet-optimized gesture zones
     */
    fun getTabletGestureZones(screenSize: Offset, isLandscape: Boolean): Map<String, GestureZone> {
        return if (isLandscape) {
            mapOf(
                "volume" to GestureZone(
                    startX = screenSize.x * 0.7f,
                    endX = screenSize.x,
                    startY = 0f,
                    endY = screenSize.y,
                    description = "Volume control zone"
                ),
                "brightness" to GestureZone(
                    startX = 0f,
                    endX = screenSize.x * 0.3f,
                    startY = 0f,
                    endY = screenSize.y,
                    description = "Brightness control zone"
                ),
                "seek" to GestureZone(
                    startX = screenSize.x * 0.3f,
                    endX = screenSize.x * 0.7f,
                    startY = screenSize.y * 0.2f,
                    endY = screenSize.y * 0.8f,
                    description = "Seek control zone"
                )
            )
        } else {
            // Portrait tablet zones
            mapOf(
                "volume" to GestureZone(
                    startX = screenSize.x * 0.6f,
                    endX = screenSize.x,
                    startY = 0f,
                    endY = screenSize.y,
                    description = "Volume control zone"
                ),
                "brightness" to GestureZone(
                    startX = 0f,
                    endX = screenSize.x * 0.4f,
                    startY = 0f,
                    endY = screenSize.y,
                    description = "Brightness control zone"
                ),
                "seek" to GestureZone(
                    startX = screenSize.x * 0.2f,
                    endX = screenSize.x * 0.8f,
                    startY = screenSize.y * 0.3f,
                    endY = screenSize.y * 0.7f,
                    description = "Seek control zone"
                )
            )
        }
    }
    
    data class GestureZone(
        val startX: Float,
        val endX: Float,
        val startY: Float,
        val endY: Float,
        val description: String
    )
}

/**
 * Foldable device gesture adaptations
 */
class FoldableGestureAdapter {
    /**
     * Adapt gestures for foldable devices
     */
    fun adaptGesturesForFoldable(
        baseSettings: EnhancedGestureSettings,
        isFolded: Boolean,
        foldPosition: FoldPosition
    ): EnhancedGestureSettings {
        return if (isFolded) {
            // Folded state - act more like a phone
            baseSettings.copy(
                general = baseSettings.general.copy(
                    // Reduce dead zone when folded
                    gestureDeadZone = baseSettings.general.gestureDeadZone * 1.2f
                ),
                doubleTap = baseSettings.doubleTap.copy(
                    // Smaller tap distance when folded
                    maxTapDistance = baseSettings.doubleTap.maxTapDistance * 0.8f
                )
            )
        } else {
            // Unfolded state - act more like a tablet
            baseSettings.copy(
                general = baseSettings.general.copy(
                    // Larger dead zone when unfolded
                    gestureDeadZone = baseSettings.general.gestureDeadZone * 0.8f
                ),
                pinchZoom = baseSettings.pinchZoom.copy(
                    // Better zoom on larger screen
                    maxZoom = 3.5f
                )
            )
        }
    }
    
    enum class FoldPosition {
        HORIZONTAL,
        VERTICAL,
        UNKNOWN
    }
}