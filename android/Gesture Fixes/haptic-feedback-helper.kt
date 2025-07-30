package com.astralplayer.nextplayer.feature.player.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Helper class for providing haptic feedback
 */
class HapticFeedbackHelper(private val context: Context) {
    
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Provide haptic feedback with specified intensity
     * @param intensity Duration in milliseconds (used as intensity indicator)
     */
    fun provideHapticFeedback(intensity: Long = 50L) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (intensity) {
                in 0..50 -> {
                    // Light feedback
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            20, 
                            VibrationEffect.DEFAULT_AMPLITUDE / 2
                        )
                    )
                }
                in 51..100 -> {
                    // Medium feedback
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            40, 
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                }
                else -> {
                    // Strong feedback
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            60, 
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                }
            }
        } else {
            // Fallback for older devices
            @Suppress("DEPRECATION")
            vibrator.vibrate(intensity)
        }
    }
    
    /**
     * Provide click haptic feedback
     */
    fun click() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            provideHapticFeedback(10)
        }
    }
    
    /**
     * Provide double click haptic feedback
     */
    fun doubleClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            provideHapticFeedback(20)
        }
    }
    
    /**
     * Provide heavy click haptic feedback
     */
    fun heavyClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            provideHapticFeedback(40)
        }
    }
    
    /**
     * Provide tick haptic feedback (for continuous actions)
     */
    fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            provideHapticFeedback(5)
        }
    }
}