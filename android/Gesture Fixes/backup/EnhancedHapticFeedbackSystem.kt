package com.astralplayer.nextplayer.feature.player.gestures

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * Enhanced haptic feedback system with contextual patterns
 * Implements requirements 5.1, 5.2: Haptic feedback system
 */

/**
 * Haptic feedback patterns for different gesture types
 */
enum class HapticPattern {
    // Basic patterns
    LIGHT_TAP,          // Single light tap
    MEDIUM_TAP,         // Medium intensity tap
    HEAVY_TAP,          // Heavy intensity tap
    
    // Gesture-specific patterns
    VOLUME_ADJUSTMENT,   // Volume up/down feedback
    BRIGHTNESS_ADJUSTMENT, // Brightness adjustment feedback
    SEEK_FEEDBACK,      // Seeking feedback
    LONG_PRESS_START,   // Long press initiation
    LONG_PRESS_SPEED_CHANGE, // Speed level changes
    DIRECTION_CHANGE,   // Direction change feedback
    
    // Advanced patterns
    SUCCESS_PATTERN,    // Success confirmation
    ERROR_PATTERN,      // Error indication
    WARNING_PATTERN,    // Warning indication
    
    // Custom patterns
    PULSE_PATTERN,      // Rhythmic pulsing
    WAVE_PATTERN,       // Wave-like vibration
    HEARTBEAT_PATTERN   // Heartbeat-like rhythm
}

/**
 * Haptic intensity levels
 */
enum class HapticIntensity {
    DISABLED,   // No haptic feedback
    LIGHT,      // Subtle feedback
    MEDIUM,     // Moderate feedback
    STRONG,     // Strong feedback
    CUSTOM      // Custom intensity
}

/**
 * Haptic feedback configuration
 */
data class HapticFeedbackConfig(
    val globalIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val customIntensity: Float = 0.5f, // 0.0 to 1.0
    val enabledGestures: Set<GestureType> = setOf(
        GestureType.VOLUME,
        GestureType.BRIGHTNESS,
        GestureType.SEEK,
        GestureType.LONG_PRESS,
        GestureType.DIRECTION_CHANGE
    ),
    val patternOverrides: Map<HapticPattern, VibrationPattern> = emptyMap()
)

/**
 * Gesture types for haptic feedback
 */
enum class GestureType {
    VOLUME,
    BRIGHTNESS,
    SEEK,
    LONG_PRESS,
    DIRECTION_CHANGE,
    TAP,
    DOUBLE_TAP,
    ZOOM
}

/**
 * Custom vibration pattern definition
 */
data class VibrationPattern(
    val timings: LongArray,
    val amplitudes: IntArray? = null,
    val repeat: Int = -1 // -1 for no repeat
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as VibrationPattern
        
        if (!timings.contentEquals(other.timings)) return false
        if (amplitudes != null) {
            if (other.amplitudes == null) return false
            if (!amplitudes.contentEquals(other.amplitudes)) return false
        } else if (other.amplitudes != null) return false
        if (repeat != other.repeat) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = timings.contentHashCode()
        result = 31 * result + (amplitudes?.contentHashCode() ?: 0)
        result = 31 * result + repeat
        return result
    }
}

/**
 * Enhanced haptic feedback manager
 */
class EnhancedHapticFeedbackManager(
    private val context: Context,
    private var config: HapticFeedbackConfig = HapticFeedbackConfig()
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    private val isHapticSupported = vibrator?.hasVibrator() == true
    
    /**
     * Updates the haptic feedback configuration
     */
    fun updateConfig(newConfig: HapticFeedbackConfig) {
        config = newConfig
    }
    
    /**
     * Provides haptic feedback for a specific pattern
     */
    fun provideHapticFeedback(
        pattern: HapticPattern,
        gestureType: GestureType? = null,
        customIntensity: Float? = null
    ) {
        if (!isHapticSupported || config.globalIntensity == HapticIntensity.DISABLED) {
            return
        }
        
        // Check if gesture type is enabled
        if (gestureType != null && !config.enabledGestures.contains(gestureType)) {
            return
        }
        
        val vibrationPattern = getVibrationPattern(pattern)
        val intensity = customIntensity ?: getIntensityForPattern(pattern)
        
        executeVibration(vibrationPattern, intensity)
    }
    
    /**
     * Provides contextual haptic feedback for volume adjustments
     */
    fun provideVolumeHapticFeedback(volumeLevel: Float, isIncreasing: Boolean) {
        if (!config.enabledGestures.contains(GestureType.VOLUME)) return
        
        val pattern = when {
            volumeLevel >= 1.0f -> HapticPattern.WARNING_PATTERN // Max volume warning
            volumeLevel <= 0.0f -> HapticPattern.ERROR_PATTERN   // Muted
            else -> HapticPattern.VOLUME_ADJUSTMENT
        }
        
        val intensity = when {
            volumeLevel >= 0.8f -> 0.8f // Stronger feedback at high volume
            volumeLevel <= 0.2f -> 0.3f // Lighter feedback at low volume
            else -> 0.5f
        }
        
        provideHapticFeedback(pattern, GestureType.VOLUME, intensity)
    }
    
    /**
     * Provides contextual haptic feedback for brightness adjustments
     */
    fun provideBrightnessHapticFeedback(brightnessLevel: Float, isIncreasing: Boolean) {
        if (!config.enabledGestures.contains(GestureType.BRIGHTNESS)) return
        
        val pattern = when {
            brightnessLevel >= 1.0f -> HapticPattern.SUCCESS_PATTERN // Max brightness
            brightnessLevel <= 0.0f -> HapticPattern.WARNING_PATTERN // Min brightness
            else -> HapticPattern.BRIGHTNESS_ADJUSTMENT
        }
        
        val intensity = brightnessLevel * 0.6f + 0.2f // Scale intensity with brightness
        
        provideHapticFeedback(pattern, GestureType.BRIGHTNESS, intensity)
    }
    
    /**
     * Provides haptic feedback for seek operations
     */
    fun provideSeekHapticFeedback(seekSpeed: Float, direction: SeekDirection) {
        if (!config.enabledGestures.contains(GestureType.SEEK)) return
        
        val intensity = min(1.0f, seekSpeed * 0.3f + 0.2f)
        provideHapticFeedback(HapticPattern.SEEK_FEEDBACK, GestureType.SEEK, intensity)
    }
    
    /**
     * Provides haptic feedback for long press speed changes
     */
    fun provideLongPressSpeedChangeHapticFeedback(speedLevel: Int, maxSpeedLevel: Int) {
        if (!config.enabledGestures.contains(GestureType.LONG_PRESS)) return
        
        val intensity = (speedLevel.toFloat() / maxSpeedLevel) * 0.8f + 0.2f
        provideHapticFeedback(HapticPattern.LONG_PRESS_SPEED_CHANGE, GestureType.LONG_PRESS, intensity)
    }
    
    /**
     * Provides haptic feedback for direction changes
     */
    fun provideDirectionChangeHapticFeedback(confidence: Float) {
        if (!config.enabledGestures.contains(GestureType.DIRECTION_CHANGE)) return
        
        val intensity = confidence * 0.7f + 0.3f
        provideHapticFeedback(HapticPattern.DIRECTION_CHANGE, GestureType.DIRECTION_CHANGE, intensity)
    }
    
    /**
     * Provides rhythmic haptic feedback for continuous gestures
     */
    suspend fun provideRhythmicHapticFeedback(
        pattern: HapticPattern,
        gestureType: GestureType,
        intervalMs: Long = 200L,
        maxDuration: Long = 2000L
    ) {
        if (!config.enabledGestures.contains(gestureType)) return
        
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxDuration) {
            provideHapticFeedback(pattern, gestureType)
            delay(intervalMs)
        }
    }
    
    /**
     * Gets the vibration pattern for a haptic pattern
     */
    private fun getVibrationPattern(pattern: HapticPattern): VibrationPattern {
        // Check for custom overrides first
        config.patternOverrides[pattern]?.let { return it }
        
        return when (pattern) {
            HapticPattern.LIGHT_TAP -> VibrationPattern(
                timings = longArrayOf(0, 50),
                amplitudes = intArrayOf(0, 80)
            )
            
            HapticPattern.MEDIUM_TAP -> VibrationPattern(
                timings = longArrayOf(0, 100),
                amplitudes = intArrayOf(0, 150)
            )
            
            HapticPattern.HEAVY_TAP -> VibrationPattern(
                timings = longArrayOf(0, 150),
                amplitudes = intArrayOf(0, 255)
            )
            
            HapticPattern.VOLUME_ADJUSTMENT -> VibrationPattern(
                timings = longArrayOf(0, 30, 20, 30),
                amplitudes = intArrayOf(0, 100, 0, 100)
            )
            
            HapticPattern.BRIGHTNESS_ADJUSTMENT -> VibrationPattern(
                timings = longArrayOf(0, 40, 10, 40),
                amplitudes = intArrayOf(0, 120, 0, 120)
            )
            
            HapticPattern.SEEK_FEEDBACK -> VibrationPattern(
                timings = longArrayOf(0, 25, 15, 25, 15, 25),
                amplitudes = intArrayOf(0, 80, 0, 80, 0, 80)
            )
            
            HapticPattern.LONG_PRESS_START -> VibrationPattern(
                timings = longArrayOf(0, 200),
                amplitudes = intArrayOf(0, 180)
            )
            
            HapticPattern.LONG_PRESS_SPEED_CHANGE -> VibrationPattern(
                timings = longArrayOf(0, 60, 40, 60),
                amplitudes = intArrayOf(0, 140, 0, 140)
            )
            
            HapticPattern.DIRECTION_CHANGE -> VibrationPattern(
                timings = longArrayOf(0, 80, 30, 80),
                amplitudes = intArrayOf(0, 160, 0, 160)
            )
            
            HapticPattern.SUCCESS_PATTERN -> VibrationPattern(
                timings = longArrayOf(0, 50, 50, 100, 50, 150),
                amplitudes = intArrayOf(0, 100, 0, 150, 0, 200)
            )
            
            HapticPattern.ERROR_PATTERN -> VibrationPattern(
                timings = longArrayOf(0, 100, 100, 100, 100, 100),
                amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            )
            
            HapticPattern.WARNING_PATTERN -> VibrationPattern(
                timings = longArrayOf(0, 150, 100, 150),
                amplitudes = intArrayOf(0, 200, 0, 200)
            )
            
            HapticPattern.PULSE_PATTERN -> VibrationPattern(
                timings = longArrayOf(0, 100, 200, 100, 200, 100),
                amplitudes = intArrayOf(0, 120, 0, 120, 0, 120),
                repeat = 0
            )
            
            HapticPattern.WAVE_PATTERN -> VibrationPattern(
                timings = longArrayOf(0, 50, 50, 100, 50, 150, 50, 100, 50, 50),
                amplitudes = intArrayOf(0, 80, 0, 120, 0, 160, 0, 120, 0, 80)
            )
            
            HapticPattern.HEARTBEAT_PATTERN -> VibrationPattern(
                timings = longArrayOf(0, 100, 100, 100, 400),
                amplitudes = intArrayOf(0, 150, 0, 150, 0),
                repeat = 0
            )
        }
    }
    
    /**
     * Gets the intensity for a specific pattern
     */
    private fun getIntensityForPattern(pattern: HapticPattern): Float {
        return when (config.globalIntensity) {
            HapticIntensity.DISABLED -> 0f
            HapticIntensity.LIGHT -> 0.3f
            HapticIntensity.MEDIUM -> 0.6f
            HapticIntensity.STRONG -> 1.0f
            HapticIntensity.CUSTOM -> config.customIntensity
        }
    }
    
    /**
     * Executes the vibration with the specified pattern and intensity
     */
    private fun executeVibration(pattern: VibrationPattern, intensity: Float) {
        if (!isHapticSupported || vibrator == null) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val adjustedAmplitudes = pattern.amplitudes?.map { amplitude ->
                    (amplitude * intensity).toInt().coerceIn(0, 255)
                }?.toIntArray()
                
                val vibrationEffect = if (adjustedAmplitudes != null) {
                    VibrationEffect.createWaveform(pattern.timings, adjustedAmplitudes, pattern.repeat)
                } else {
                    VibrationEffect.createWaveform(pattern.timings, pattern.repeat)
                }
                
                vibrator.vibrate(vibrationEffect)
            } else {
                // Fallback for older Android versions
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern.timings, pattern.repeat)
            }
        } catch (e: Exception) {
            // Handle vibration errors gracefully
            e.printStackTrace()
        }
    }
    
    /**
     * Cancels any ongoing vibration
     */
    fun cancelVibration() {
        vibrator?.cancel()
    }
}

/**
 * Composable function to provide haptic feedback manager
 */
@Composable
fun rememberHapticFeedbackManager(
    config: HapticFeedbackConfig = HapticFeedbackConfig()
): EnhancedHapticFeedbackManager {
    val context = LocalContext.current
    return remember(config) {
        EnhancedHapticFeedbackManager(context, config)
    }
}

/**
 * Haptic feedback settings for user preferences
 */
data class HapticFeedbackSettings(
    val isEnabled: Boolean = true,
    val globalIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val customIntensity: Float = 0.5f,
    val enabledGestures: Map<GestureType, Boolean> = mapOf(
        GestureType.VOLUME to true,
        GestureType.BRIGHTNESS to true,
        GestureType.SEEK to true,
        GestureType.LONG_PRESS to true,
        GestureType.DIRECTION_CHANGE to true,
        GestureType.TAP to false,
        GestureType.DOUBLE_TAP to true,
        GestureType.ZOOM to false
    ),
    val patternPreferences: Map<HapticPattern, Boolean> = mapOf(
        HapticPattern.SUCCESS_PATTERN to true,
        HapticPattern.ERROR_PATTERN to true,
        HapticPattern.WARNING_PATTERN to true
    )
) {
    /**
     * Converts settings to haptic feedback configuration
     */
    fun toConfig(): HapticFeedbackConfig {
        return HapticFeedbackConfig(
            globalIntensity = if (isEnabled) globalIntensity else HapticIntensity.DISABLED,
            customIntensity = customIntensity,
            enabledGestures = enabledGestures.filterValues { it }.keys.toSet()
        )
    }
}

/**
 * Haptic feedback utilities
 */
object HapticFeedbackUtils {
    /**
     * Creates a custom vibration pattern
     */
    fun createCustomPattern(
        pulseCount: Int,
        pulseDuration: Long = 50L,
        pauseDuration: Long = 50L,
        amplitude: Int = 150
    ): VibrationPattern {
        val timings = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()
        
        timings.add(0) // Initial delay
        amplitudes.add(0)
        
        repeat(pulseCount) { index ->
            timings.add(pulseDuration)
            amplitudes.add(amplitude)
            
            if (index < pulseCount - 1) {
                timings.add(pauseDuration)
                amplitudes.add(0)
            }
        }
        
        return VibrationPattern(
            timings = timings.toLongArray(),
            amplitudes = amplitudes.toIntArray()
        )
    }
    
    /**
     * Creates a wave pattern with varying intensity
     */
    fun createWavePattern(
        waveCount: Int = 3,
        waveDuration: Long = 200L,
        maxAmplitude: Int = 200
    ): VibrationPattern {
        val timings = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()
        
        timings.add(0)
        amplitudes.add(0)
        
        repeat(waveCount) {
            val steps = 10
            val stepDuration = waveDuration / steps
            
            repeat(steps) { step ->
                timings.add(stepDuration)
                val progress = step.toFloat() / (steps - 1)
                val amplitude = (sin(progress * PI) * maxAmplitude).toInt()
                amplitudes.add(amplitude)
            }
        }
        
        return VibrationPattern(
            timings = timings.toLongArray(),
            amplitudes = amplitudes.toIntArray()
        )
    }
    
    /**
     * Validates haptic feedback settings
     */
    fun validateSettings(settings: HapticFeedbackSettings): HapticFeedbackSettings {
        return settings.copy(
            customIntensity = settings.customIntensity.coerceIn(0f, 1f)
        )
    }
}