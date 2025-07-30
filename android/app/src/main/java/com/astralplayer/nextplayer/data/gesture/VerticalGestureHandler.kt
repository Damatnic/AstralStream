package com.astralplayer.nextplayer.data.gesture

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.Window
import androidx.compose.ui.geometry.Offset
import com.astralplayer.nextplayer.data.*
import kotlin.math.abs

/**
 * Handles vertical gestures for volume and brightness control
 */
class VerticalGestureHandler(
    private val context: Context,
    private val window: Window?,
    private val screenHeight: Float,
    private val volumeSettings: VolumeGestureSettings,
    private val brightnessSettings: BrightnessGestureSettings
) {
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    
    private var initialVolume = 0
    private var initialBrightness = 0f
    private var totalDragAmount = 0f
    private var currentVolume = 0
    private var currentBrightness = 0f
    
    init {
        updateInitialValues()
    }
    
    /**
     * Updates initial values for volume and brightness
     */
    fun updateInitialValues() {
        initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        currentVolume = initialVolume
        
        initialBrightness = getCurrentBrightness()
        currentBrightness = initialBrightness
    }
    
    /**
     * Processes vertical drag for volume or brightness
     */
    fun processDrag(
        dragAmount: Float,
        startPosition: Offset,
        side: TouchSide
    ): GestureAction? {
        val normalizedDelta = -dragAmount / screenHeight // Negative because up is negative
        totalDragAmount += normalizedDelta
        
        return when {
            shouldHandleBrightness(side) -> handleBrightnessChange(normalizedDelta, side)
            shouldHandleVolume(side) -> handleVolumeChange(normalizedDelta, side)
            else -> null
        }
    }
    
    /**
     * Determines if brightness should be handled for the given side
     */
    private fun shouldHandleBrightness(side: TouchSide): Boolean {
        return brightnessSettings.isEnabled && (
            (brightnessSettings.leftSideOnly && side == TouchSide.LEFT) ||
            (!brightnessSettings.leftSideOnly && !volumeSettings.rightSideOnly)
        )
    }
    
    /**
     * Determines if volume should be handled for the given side
     */
    private fun shouldHandleVolume(side: TouchSide): Boolean {
        return volumeSettings.isEnabled && (
            (volumeSettings.rightSideOnly && side == TouchSide.RIGHT) ||
            (!volumeSettings.rightSideOnly && !brightnessSettings.leftSideOnly)
        )
    }
    
    /**
     * Handles brightness change
     */
    private fun handleBrightnessChange(normalizedDelta: Float, side: TouchSide): GestureAction.BrightnessChange {
        val brightnessDelta = normalizedDelta * brightnessSettings.sensitivity
        
        if (brightnessSettings.systemBrightnessIntegration && window != null) {
            currentBrightness = (initialBrightness + totalDragAmount * brightnessSettings.sensitivity)
                .coerceIn(brightnessSettings.minimumBrightness, brightnessSettings.maximumBrightness)
            
            // Apply brightness to window
            val layoutParams = window.attributes
            layoutParams.screenBrightness = currentBrightness
            window.attributes = layoutParams
        }
        
        return GestureAction.BrightnessChange(brightnessDelta, side)
    }
    
    /**
     * Handles volume change
     */
    private fun handleVolumeChange(normalizedDelta: Float, side: TouchSide): GestureAction.VolumeChange {
        val volumeDelta = normalizedDelta * volumeSettings.sensitivity
        
        if (volumeSettings.systemVolumeIntegration) {
            currentVolume = (initialVolume + (totalDragAmount * maxVolume * volumeSettings.sensitivity).toInt())
                .coerceIn(0, maxVolume)
            
            // Apply volume to system
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                currentVolume,
                0 // No UI flags to avoid system volume UI
            )
        }
        
        return GestureAction.VolumeChange(volumeDelta, side)
    }
    
    /**
     * Gets current system brightness
     */
    private fun getCurrentBrightness(): Float {
        return try {
            if (window != null && window.attributes.screenBrightness >= 0) {
                window.attributes.screenBrightness
            } else {
                val brightnessInt = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    125
                )
                brightnessInt / 255f
            }
        } catch (e: Exception) {
            0.5f // Default to 50% brightness
        }
    }
    
    /**
     * Resets the handler state
     */
    fun reset() {
        totalDragAmount = 0f
        updateInitialValues()
    }
    
    /**
     * Gets current volume info for UI display
     */
    fun getVolumeInfo(): VolumeInfo {
        val volumePercentage = if (maxVolume > 0) currentVolume.toFloat() / maxVolume else 0f
        return VolumeInfo(
            currentVolume = currentVolume,
            maxVolume = maxVolume,
            percentage = volumePercentage,
            isMuted = currentVolume == 0
        )
    }
    
    /**
     * Gets current brightness info for UI display
     */
    fun getBrightnessInfo(): BrightnessInfo {
        return BrightnessInfo(
            currentBrightness = currentBrightness,
            percentage = currentBrightness,
            isAutoBrightness = isAutoBrightnessEnabled()
        )
    }
    
    /**
     * Checks if auto brightness is enabled
     */
    private fun isAutoBrightnessEnabled(): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (e: Exception) {
            false
        }
    }
    
    data class VolumeInfo(
        val currentVolume: Int,
        val maxVolume: Int,
        val percentage: Float,
        val isMuted: Boolean
    )
    
    data class BrightnessInfo(
        val currentBrightness: Float,
        val percentage: Float,
        val isAutoBrightness: Boolean
    )
}