package com.astralplayer.nextplayer.feature.player.gestures

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Enhanced vertical gesture handler for volume and brightness control
 */
class VerticalGestureHandler(
    private val context: Context,
    private val onVolumeChange: (level: Float, delta: Float, side: TouchSide) -> Unit,
    private val onBrightnessChange: (level: Float, delta: Float, side: TouchSide) -> Unit,
    private val onGestureStart: (type: GestureType, side: TouchSide) -> Unit,
    private val onGestureEnd: (type: GestureType, success: Boolean) -> Unit
) {
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val volumeController = VolumeController(audioManager)
    private val brightnessController = BrightnessController(context)
    
    private var isActive = false
    private var gestureType: GestureType? = null
    private var startPosition = Offset.Zero
    private var startTime = 0L
    private var touchSide = TouchSide.CENTER
    private var accumulatedDelta = 0f
    
    suspend fun PointerInputScope.detectVerticalGestures(
        volumeSettings: VolumeGestureSettings,
        brightnessSettings: BrightnessGestureSettings
    ) = coroutineScope {
        awaitEachGesture {
            if (!volumeSettings.isEnabled && !brightnessSettings.isEnabled) return@awaitEachGesture
            
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            startGesture(firstDown, volumeSettings, brightnessSettings, size)
            
            var currentPointer = firstDown
            do {
                val event = awaitPointerEvent()
                currentPointer = event.changes.firstOrNull { !it.isConsumed } ?: currentPointer
                
                if (isActive) {
                    updateGesture(currentPointer, volumeSettings, brightnessSettings, size)
                    currentPointer.consume()
                }
                
            } while (event.changes.any { it.pressed })
            
            endGesture()
        }
    }
    
    private fun startGesture(
        firstDown: PointerInputChange,
        volumeSettings: VolumeGestureSettings,
        brightnessSettings: BrightnessGestureSettings,
        screenSize: androidx.compose.ui.unit.IntSize
    ) {
        startPosition = firstDown.position
        startTime = System.currentTimeMillis()
        touchSide = determineTouchSide(firstDown.position, screenSize.width.toFloat())
        accumulatedDelta = 0f
        isActive = false // Will be activated when minimum distance is reached
        gestureType = null
    }
    
    private fun updateGesture(
        pointer: PointerInputChange,
        volumeSettings: VolumeGestureSettings,
        brightnessSettings: BrightnessGestureSettings,
        screenSize: androidx.compose.ui.unit.IntSize
    ) {
        val currentPosition = pointer.position
        val deltaX = currentPosition.x - startPosition.x
        val deltaY = currentPosition.y - startPosition.y
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
        
        // Determine which gesture to activate based on side and settings
        if (!isActive && distance >= maxOf(volumeSettings.minimumSwipeDistance, brightnessSettings.minimumSwipeDistance)) {
            val verticalRatio = abs(deltaY) / (abs(deltaX) + 1f)
            if (verticalRatio >= 1.5f) { // Must be more vertical than horizontal
                gestureType = when {
                    touchSide == TouchSide.RIGHT && volumeSettings.isEnabled && volumeSettings.rightSideOnly -> GestureType.VERTICAL_VOLUME
                    touchSide == TouchSide.LEFT && brightnessSettings.isEnabled && brightnessSettings.leftSideOnly -> GestureType.VERTICAL_BRIGHTNESS
                    touchSide == TouchSide.RIGHT && brightnessSettings.isEnabled && !brightnessSettings.leftSideOnly -> GestureType.VERTICAL_BRIGHTNESS
                    touchSide == TouchSide.LEFT && volumeSettings.isEnabled && !volumeSettings.rightSideOnly -> GestureType.VERTICAL_VOLUME
                    else -> null
                }
                
                gestureType?.let { type ->
                    isActive = true
                    onGestureStart(type, touchSide)
                }
            }
        }
        
        if (!isActive || gestureType == null) return
        
        // Calculate delta and apply sensitivity
        val rawDelta = -deltaY // Negative because up should increase values
        val settings = when (gestureType) {
            GestureType.VERTICAL_VOLUME -> volumeSettings
            GestureType.VERTICAL_BRIGHTNESS -> brightnessSettings
            else -> return
        }
        
        val sensitivity = when (gestureType) {
            GestureType.VERTICAL_VOLUME -> if (touchSide == TouchSide.RIGHT) volumeSettings.rightSideSensitivity else volumeSettings.sensitivity
            GestureType.VERTICAL_BRIGHTNESS -> if (touchSide == TouchSide.LEFT) brightnessSettings.leftSideSensitivity else brightnessSettings.sensitivity
            else -> 1f
        }
        
        val adjustedDelta = rawDelta * sensitivity
        accumulatedDelta = adjustedDelta
        
        // Apply the gesture
        when (gestureType) {
            GestureType.VERTICAL_VOLUME -> {
                val volumeDelta = adjustedDelta / screenSize.height * volumeSettings.volumeStep
                val newLevel = volumeController.adjustVolume(volumeDelta, volumeSettings)
                onVolumeChange(newLevel, volumeDelta, touchSide)
            }
            GestureType.VERTICAL_BRIGHTNESS -> {
                val brightnessDelta = adjustedDelta / screenSize.height * brightnessSettings.brightnessStep
                val newLevel = brightnessController.adjustBrightness(brightnessDelta, brightnessSettings)
                onBrightnessChange(newLevel, brightnessDelta, touchSide)
            }
            else -> {}
        }
    }
    
    private fun endGesture() {
        val success = isActive && abs(accumulatedDelta) > 10f // Minimum meaningful change
        
        gestureType?.let { type ->
            onGestureEnd(type, success)
        }
        
        // Reset state
        isActive = false
        gestureType = null
        accumulatedDelta = 0f
    }
    
    private fun determineTouchSide(position: Offset, screenWidth: Float): TouchSide {
        return when {
            position.x < screenWidth * 0.4f -> TouchSide.LEFT
            position.x > screenWidth * 0.6f -> TouchSide.RIGHT
            else -> TouchSide.CENTER
        }
    }
}

/**
 * Volume controller with system integration and boost support
 */
class VolumeController(private val audioManager: AudioManager) {
    
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    private var boostedMaxVolume = maxVolume
    
    fun adjustVolume(delta: Float, settings: VolumeGestureSettings): Float {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val currentLevel = currentVolume.toFloat() / maxVolume
        
        // Calculate new level
        var newLevel = (currentLevel + delta).coerceIn(0f, 1f)
        
        // Apply volume boost if enabled
        if (settings.volumeBoostEnabled && newLevel > 1f) {
            boostedMaxVolume = (maxVolume * settings.volumeBoostLimit).toInt()
            newLevel = newLevel.coerceAtMost(settings.volumeBoostLimit)
        } else {
            boostedMaxVolume = maxVolume
        }
        
        // Set system volume
        if (settings.systemVolumeIntegration) {
            val targetVolume = (newLevel * maxVolume).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        }
        
        return newLevel
    }
    
    fun getCurrentLevel(): Float {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return currentVolume.toFloat() / maxVolume
    }
    
    fun setVolume(level: Float, settings: VolumeGestureSettings) {
        val clampedLevel = level.coerceIn(0f, if (settings.volumeBoostEnabled) settings.volumeBoostLimit else 1f)
        val targetVolume = (clampedLevel * maxVolume).toInt().coerceIn(0, maxVolume)
        
        if (settings.systemVolumeIntegration) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        }
    }
}

/**
 * Brightness controller with system integration
 */
class BrightnessController(private val context: Context) {
    
    private var currentBrightness = 0.5f
    
    init {
        // Initialize with current system brightness
        try {
            val systemBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            currentBrightness = systemBrightness / 255f
        } catch (e: Settings.SettingNotFoundException) {
            currentBrightness = 0.5f
        }
    }
    
    fun adjustBrightness(delta: Float, settings: BrightnessGestureSettings): Float {
        val newLevel = (currentBrightness + delta).coerceIn(
            settings.minimumBrightness,
            settings.maximumBrightness
        )
        
        setBrightness(newLevel, settings)
        return newLevel
    }
    
    fun setBrightness(level: Float, settings: BrightnessGestureSettings) {
        val clampedLevel = level.coerceIn(settings.minimumBrightness, settings.maximumBrightness)
        currentBrightness = clampedLevel
        
        // Set system brightness if integration is enabled
        if (settings.systemBrightnessIntegration) {
            try {
                val brightnessValue = (clampedLevel * 255).toInt()
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightnessValue
                )
            } catch (e: Exception) {
                // Handle permission issues gracefully
                // In a real app, you'd need WRITE_SETTINGS permission
            }
        }
    }
    
    fun getCurrentLevel(): Float = currentBrightness
}

/**
 * Vertical gesture analytics for adaptive learning
 */
class VerticalGestureAnalytics {
    private val volumeGestureData = mutableListOf<VerticalGestureData>()
    private val brightnessGestureData = mutableListOf<VerticalGestureData>()
    
    data class VerticalGestureData(
        val startTime: Long,
        val endTime: Long,
        val startLevel: Float,
        val endLevel: Float,
        val distance: Float,
        val side: TouchSide,
        val success: Boolean,
        val precision: Float // How precisely the user reached their intended level
    )
    
    fun recordVolumeGesture(
        startTime: Long,
        endTime: Long,
        startLevel: Float,
        endLevel: Float,
        distance: Float,
        side: TouchSide,
        success: Boolean
    ) {
        val precision = calculatePrecision(startLevel, endLevel, distance)
        val data = VerticalGestureData(
            startTime, endTime, startLevel, endLevel,
            distance, side, success, precision
        )
        
        volumeGestureData.add(data)
        limitDataSize(volumeGestureData)
    }
    
    fun recordBrightnessGesture(
        startTime: Long,
        endTime: Long,
        startLevel: Float,
        endLevel: Float,
        distance: Float,
        side: TouchSide,
        success: Boolean
    ) {
        val precision = calculatePrecision(startLevel, endLevel, distance)
        val data = VerticalGestureData(
            startTime, endTime, startLevel, endLevel,
            distance, side, success, precision
        )
        
        brightnessGestureData.add(data)
        limitDataSize(brightnessGestureData)
    }
    
    private fun calculatePrecision(startLevel: Float, endLevel: Float, distance: Float): Float {
        val levelChange = abs(endLevel - startLevel)
        val expectedDistance = levelChange * 500f // Rough estimate
        return if (expectedDistance > 0) {
            1f - abs(distance - expectedDistance) / expectedDistance
        } else {
            1f
        }
    }
    
    private fun limitDataSize(data: MutableList<VerticalGestureData>) {
        if (data.size > 500) {
            data.removeAt(0)
        }
    }
    
    fun getVolumeSensitivitySuggestion(currentSensitivity: Float): Float {
        return getSensitivitySuggestion(volumeGestureData, currentSensitivity)
    }
    
    fun getBrightnessSensitivitySuggestion(currentSensitivity: Float): Float {
        return getSensitivitySuggestion(brightnessGestureData, currentSensitivity)
    }
    
    private fun getSensitivitySuggestion(data: List<VerticalGestureData>, currentSensitivity: Float): Float {
        if (data.isEmpty()) return currentSensitivity
        
        val averagePrecision = data.map { it.precision }.average().toFloat()
        val averageDistance = data.map { it.distance }.average().toFloat()
        
        return when {
            averagePrecision < 0.6f && averageDistance > 300f -> currentSensitivity * 0.9f // Too sensitive
            averagePrecision < 0.6f && averageDistance < 150f -> currentSensitivity * 1.1f // Not sensitive enough
            else -> currentSensitivity
        }.coerceIn(0.1f, 3.0f)
    }
    
    fun getVolumeSuccessRate(): Float {
        return if (volumeGestureData.isNotEmpty()) {
            volumeGestureData.count { it.success }.toFloat() / volumeGestureData.size
        } else {
            1f
        }
    }
    
    fun getBrightnessSuccessRate(): Float {
        return if (brightnessGestureData.isNotEmpty()) {
            brightnessGestureData.count { it.success }.toFloat() / brightnessGestureData.size
        } else {
            1f
        }
    }
    
    fun getPreferredVolumeSide(): TouchSide {
        val sideUsage = volumeGestureData.groupBy { it.side }
        return sideUsage.maxByOrNull { it.value.size }?.key ?: TouchSide.RIGHT
    }
    
    fun getPreferredBrightnessSide(): TouchSide {
        val sideUsage = brightnessGestureData.groupBy { it.side }
        return sideUsage.maxByOrNull { it.value.size }?.key ?: TouchSide.LEFT
    }
}

/**
 * Smooth curve interpolator for natural gesture response
 */
class GestureCurveInterpolator {
    
    /**
     * Apply easing curve to gesture input for more natural feel
     */
    fun applyEasingCurve(input: Float, curveType: CurveType = CurveType.EASE_OUT): Float {
        val normalizedInput = input.coerceIn(-1f, 1f)
        val absInput = abs(normalizedInput)
        val sign = if (normalizedInput >= 0) 1f else -1f
        
        val easedValue = when (curveType) {
            CurveType.LINEAR -> absInput
            CurveType.EASE_IN -> absInput * absInput
            CurveType.EASE_OUT -> 1f - (1f - absInput) * (1f - absInput)
            CurveType.EASE_IN_OUT -> {
                if (absInput < 0.5f) {
                    2f * absInput * absInput
                } else {
                    1f - 2f * (1f - absInput) * (1f - absInput)
                }
            }
            CurveType.SMOOTH -> {
                // Smooth step function
                absInput * absInput * (3f - 2f * absInput)
            }
        }
        
        return easedValue * sign
    }
    
    enum class CurveType {
        LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, SMOOTH
    }
}

/**
 * Dead zone handler for preventing accidental gestures
 */
class DeadZoneHandler {
    
    fun isInDeadZone(
        position: Offset,
        screenSize: androidx.compose.ui.unit.IntSize,
        deadZones: List<DeadZone>
    ): Boolean {
        return deadZones.any { deadZone ->
            isPositionInDeadZone(position, screenSize, deadZone)
        }
    }
    
    private fun isPositionInDeadZone(
        position: Offset,
        screenSize: androidx.compose.ui.unit.IntSize,
        deadZone: DeadZone
    ): Boolean {
        val normalizedX = position.x / screenSize.width
        val normalizedY = position.y / screenSize.height
        
        return normalizedX >= deadZone.left &&
                normalizedX <= deadZone.right &&
                normalizedY >= deadZone.top &&
                normalizedY <= deadZone.bottom
    }
    
    data class DeadZone(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val name: String = ""
    )
    
    companion object {
        fun createCenterDeadZone(widthPercent: Float = 0.2f, heightPercent: Float = 0.3f): DeadZone {
            val halfWidth = widthPercent / 2f
            val halfHeight = heightPercent / 2f
            return DeadZone(
                left = 0.5f - halfWidth,
                top = 0.5f - halfHeight,
                right = 0.5f + halfWidth,
                bottom = 0.5f + halfHeight,
                name = "center"
            )
        }
        
        fun createEdgeDeadZones(edgeSize: Float = 0.05f): List<DeadZone> {
            return listOf(
                DeadZone(0f, 0f, edgeSize, 1f, "left_edge"),
                DeadZone(1f - edgeSize, 0f, 1f, 1f, "right_edge"),
                DeadZone(0f, 0f, 1f, edgeSize, "top_edge"),
                DeadZone(0f, 1f - edgeSize, 1f, 1f, "bottom_edge")
            )
        }
    }
}