package com.astralplayer.nextplayer.feature.player.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object HapticFeedbackHelper {

    private const val TAG = "HapticFeedbackHelper"

    // Define standard amplitude levels if you use them often
    const val AMPLITUDE_LOW = 50
    const val AMPLITUDE_MEDIUM = 120
    const val AMPLITUDE_HIGH = 200

    fun provideHapticFeedback(
        context: Context,
        durationMillis: Long,
        amplitude: Int
    ) {
        if (durationMillis <= 0) {
            Log.w(TAG, "Vibration duration must be positive. Skipping haptic feedback.")
            return
        }

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (!vibrator.hasVibrator()) {
            Log.w(TAG, "Device does not have a vibrator. Skipping haptic feedback.")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val validAmplitude = when {
                amplitude == VibrationEffect.DEFAULT_AMPLITUDE -> {
                    // DEFAULT_AMPLITUDE is fine as is
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
                amplitude in 1..255 -> {
                    // Amplitude is within the valid custom range
                    amplitude
                }
                amplitude == 0 -> {
                    Log.w(TAG, "Amplitude was 0. Using DEFAULT_AMPLITUDE instead.")
                    VibrationEffect.DEFAULT_AMPLITUDE // Or use 1 if you prefer a minimal custom vibration
                }
                amplitude < 0 -> { // Other negative values are invalid
                    Log.w(TAG, "Amplitude was $amplitude (invalid). Using DEFAULT_AMPLITUDE instead.")
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
                else -> { // amplitude > 255
                    Log.w(TAG, "Amplitude was $amplitude. Clamping to 255.")
                    255
                }
            }

            try {
                val effect = VibrationEffect.createOneShot(durationMillis, validAmplitude)
                vibrator.vibrate(effect)
            } catch (e: IllegalArgumentException) {
                // This catch is a fallback, the validation should prevent it.
                Log.e(TAG, "Error creating VibrationEffect: ${e.message}. Defaulting to simple vibration.")
                // Fallback to simple vibration if createOneShot still fails for an unexpected reason
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMillis)
            }
        } else {
            // Fallback for older API levels (deprecated in API 26)
            // These older versions don't support amplitude control in the same way
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMillis)
        }
    }

    // Overload for convenience if you often use default amplitude
    fun provideHapticFeedback(context: Context, durationMillis: Long) {
        provideHapticFeedback(context, durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
    }
    
    /**
     * Provide click haptic feedback
     */
    fun click(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            }
        } else {
            provideHapticFeedback(context, 10)
        }
    }
    
    /**
     * Provide double click haptic feedback
     */
    fun doubleClick(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            }
        } else {
            provideHapticFeedback(context, 20)
        }
    }
    
    /**
     * Provide heavy click haptic feedback
     */
    fun heavyClick(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            }
        } else {
            provideHapticFeedback(context, 40)
        }
    }
    
    /**
     * Provide tick haptic feedback (for continuous actions)
     */
    fun tick(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            }
        } else {
            provideHapticFeedback(context, 5)
        }
    }
}