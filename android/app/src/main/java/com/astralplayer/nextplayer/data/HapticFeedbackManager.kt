package com.astralplayer.nextplayer.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

/**
 * Manages haptic feedback for gestures
 */
class HapticFeedbackManager(private val context: Context) {
    
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService<VibratorManager>()
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    private var isEnabled = true
    private var intensityMultiplier = 1.0f
    
    /**
     * Haptic patterns for different gesture types
     */
    enum class HapticPattern(
        val duration: Long,
        val amplitude: Int,
        val pattern: LongArray? = null
    ) {
        // Light patterns
        TAP(10, 50),
        DOUBLE_TAP(15, 80),
        
        // Medium patterns
        SEEK_TICK(5, 30),
        VOLUME_TICK(8, 40),
        BRIGHTNESS_TICK(8, 40),
        
        // Strong patterns
        LONG_PRESS_START(50, 150),
        LONG_PRESS_SPEED_CHANGE(30, 120),
        DIRECTION_CHANGE(40, 100),
        
        // Speed-specific patterns
        SPEED_UP(
            duration = 80,
            amplitude = 90,
            pattern = longArrayOf(0, 20, 20, 40)
        ),
        SPEED_DOWN(
            duration = 100,
            amplitude = 70,
            pattern = longArrayOf(0, 40, 20, 20)
        ),
        SPEED_ULTRA_FAST(
            duration = 150,
            amplitude = 180,
            pattern = longArrayOf(0, 15, 10, 15, 10, 15)
        ),
        SPEED_SLOW_MOTION(
            duration = 200,
            amplitude = 60,
            pattern = longArrayOf(0, 100, 50, 100)
        ),
        SPEED_RETURN_NORMAL(
            duration = 120,
            amplitude = 100,
            pattern = longArrayOf(0, 60, 30, 30)
        ),
        
        // Complex patterns
        GESTURE_CONFLICT(
            duration = 200,
            amplitude = 80,
            pattern = longArrayOf(0, 50, 50, 50)
        ),
        
        ZOOM_FEEDBACK(20, 60),
        
        // Success/Error patterns
        SUCCESS(
            duration = 150,
            amplitude = 100,
            pattern = longArrayOf(0, 30, 30, 60)
        ),
        
        ERROR(
            duration = 200,
            amplitude = 150,
            pattern = longArrayOf(0, 100, 50, 100)
        );
    }
    
    /**
     * Sets whether haptic feedback is enabled
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * Sets the intensity multiplier (0.0 - 2.0)
     */
    fun setIntensity(multiplier: Float) {
        intensityMultiplier = multiplier.coerceIn(0f, 2f)
    }
    
    /**
     * Plays haptic feedback for a gesture action
     */
    fun playGestureFeedback(action: GestureAction) {
        when (action) {
            is GestureAction.Seek -> playPattern(HapticPattern.SEEK_TICK)
            is GestureAction.VolumeChange -> playPattern(HapticPattern.VOLUME_TICK)
            is GestureAction.BrightnessChange -> playPattern(HapticPattern.BRIGHTNESS_TICK)
            is GestureAction.DoubleTapSeek -> playPattern(HapticPattern.DOUBLE_TAP)
            is GestureAction.TogglePlayPause -> playPattern(HapticPattern.TAP)
            is GestureAction.LongPressSeek -> playPattern(HapticPattern.LONG_PRESS_START)
            is GestureAction.PinchZoom -> playPattern(HapticPattern.ZOOM_FEEDBACK)
            is GestureAction.GestureConflict -> playPattern(HapticPattern.GESTURE_CONFLICT)
        }
    }
    
    /**
     * Plays haptic feedback for specific events
     */
    fun playHaptic(pattern: HapticPattern) {
        playPattern(pattern)
    }
    
    /**
     * Plays continuous haptic feedback (for dragging)
     */
    fun playContinuousHaptic(
        intensity: Float = 0.5f,
        duration: Long = 10
    ) {
        if (!isEnabled || vibrator == null) return
        
        val adjustedIntensity = (intensity * intensityMultiplier * 255).toInt()
            .coerceIn(1, 255)
        
        vibrate(duration, adjustedIntensity)
    }
    
    /**
     * Plays a haptic pattern
     */
    private fun playPattern(pattern: HapticPattern) {
        if (!isEnabled || vibrator == null) return
        
        val adjustedAmplitude = (pattern.amplitude * intensityMultiplier).toInt()
            .coerceIn(1, 255)
        
        if (pattern.pattern != null) {
            // Complex pattern
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = IntArray(pattern.pattern.size) { index ->
                    if (index % 2 == 0) 0 else adjustedAmplitude
                }
                val effect = VibrationEffect.createWaveform(pattern.pattern, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern.pattern, -1)
            }
        } else {
            // Simple pattern
            vibrate(pattern.duration, adjustedAmplitude)
        }
    }
    
    /**
     * Basic vibration helper
     */
    private fun vibrate(duration: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(duration, amplitude)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }
    
    /**
     * Cancels any ongoing vibration
     */
    fun cancel() {
        vibrator?.cancel()
    }
    
    /**
     * Checks if the device has a vibrator
     */
    fun hasVibrator(): Boolean = vibrator?.hasVibrator() == true
    
    /**
     * Plays speed-specific haptic feedback based on speed value and direction
     */
    fun playSpeedFeedback(currentSpeed: Float, previousSpeed: Float) {
        if (!isEnabled || vibrator == null) return
        
        val pattern = when {
            // Speed thresholds for different feedback
            currentSpeed >= 4.0f && previousSpeed < 4.0f -> HapticPattern.SPEED_ULTRA_FAST
            currentSpeed < 1.0f && previousSpeed >= 1.0f -> HapticPattern.SPEED_SLOW_MOTION
            currentSpeed == 1.0f && previousSpeed != 1.0f -> HapticPattern.SPEED_RETURN_NORMAL
            currentSpeed > previousSpeed -> HapticPattern.SPEED_UP
            currentSpeed < previousSpeed -> HapticPattern.SPEED_DOWN
            else -> return // No change or minimal change
        }
        
        playPattern(pattern)
    }
    
    /**
     * Plays progressive haptic feedback based on speed level (0-10 scale)
     */
    fun playProgressiveSpeedFeedback(speedLevel: Int, maxLevel: Int = 10) {
        if (!isEnabled || vibrator == null) return
        
        // Create dynamic feedback based on speed level
        val intensity = (speedLevel.toFloat() / maxLevel * 0.8f + 0.2f) // 0.2 to 1.0
        val duration = when {
            speedLevel <= 2 -> 50L
            speedLevel <= 5 -> 30L
            speedLevel <= 8 -> 20L
            else -> 15L
        }
        
        playContinuousHaptic(intensity, duration)
    }
    
    /**
     * Creates contextual haptic feedback based on gesture state
     */
    fun createContextualFeedback(
        gestureType: GestureType,
        intensity: Float,
        duration: Long? = null
    ) {
        val pattern = when (gestureType) {
            GestureType.SINGLE_TAP -> HapticPattern.TAP
            GestureType.DOUBLE_TAP -> HapticPattern.DOUBLE_TAP
            GestureType.LONG_PRESS -> HapticPattern.LONG_PRESS_START
            GestureType.HORIZONTAL_SEEK -> HapticPattern.SEEK_TICK
            GestureType.VERTICAL_VOLUME -> HapticPattern.VOLUME_TICK
            GestureType.VERTICAL_BRIGHTNESS -> HapticPattern.BRIGHTNESS_TICK
            GestureType.PINCH_ZOOM -> HapticPattern.ZOOM_FEEDBACK
        }
        
        if (duration != null) {
            playContinuousHaptic(intensity, duration)
        } else {
            playPattern(pattern)
        }
    }
    
    companion object {
        /**
         * Predefined haptic intensities
         */
        object Intensity {
            const val LIGHT = 0.3f
            const val MEDIUM = 0.6f
            const val STRONG = 1.0f
            const val EXTRA_STRONG = 1.5f
        }
    }
}