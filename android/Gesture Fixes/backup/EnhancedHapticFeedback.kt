package com.astralplayer.nextplayer.feature.player.gestures

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.*

/**
 * Enhanced haptic feedback manager with contextual patterns and customizable intensity
 */
class EnhancedHapticFeedbackManager(private val context: Context) {
    
    companion object {
        private const val TAG = "EnhancedHapticFeedback"
        
        // Haptic pattern constants
        private val LIGHT_TICK_PATTERN = longArrayOf(0, 10)
        private val MEDIUM_TICK_PATTERN = longArrayOf(0, 25)
        private val STRONG_TICK_PATTERN = longArrayOf(0, 50)
        
        private val DOUBLE_TICK_PATTERN = longArrayOf(0, 15, 50, 15)
        private val TRIPLE_TICK_PATTERN = longArrayOf(0, 10, 30, 10, 30, 10)
        
        private val SEEK_START_PATTERN = longArrayOf(0, 30, 20, 15)
        private val SEEK_END_PATTERN = longArrayOf(0, 20, 10, 30)
        
        private val DIRECTION_CHANGE_PATTERN = longArrayOf(0, 25, 15, 25)
        private val SPEED_CHANGE_PATTERN = longArrayOf(0, 15, 10, 15, 10, 15)
        
        private val ERROR_PATTERN = longArrayOf(0, 100, 50, 100, 50, 100)
        private val SUCCESS_PATTERN = longArrayOf(0, 30, 20, 15, 10, 10)
        
        private val VOLUME_TICK_PATTERN = longArrayOf(0, 12)
        private val BRIGHTNESS_TICK_PATTERN = longArrayOf(0, 8)
        private val ZOOM_TICK_PATTERN = longArrayOf(0, 20)
    }
    
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    private var isHapticEnabled = true
    private var globalIntensityMultiplier = 1.0f
    private var lastHapticTime = 0L
    private val hapticThrottleMs = 16L // Minimum time between haptic events (60fps)
    
    /**
     * Enable or disable haptic feedback globally
     */
    fun setHapticEnabled(enabled: Boolean) {
        isHapticEnabled = enabled
        Log.d(TAG, "Haptic feedback ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Set global intensity multiplier (0.0 to 2.0)
     */
    fun setGlobalIntensity(intensity: Float) {
        globalIntensityMultiplier = intensity.coerceIn(0f, 2f)
        Log.d(TAG, "Global haptic intensity set to $globalIntensityMultiplier")
    }
    
    /**
     * Provide seek gesture feedback
     */
    fun provideSeekFeedback(intensity: HapticIntensity = HapticIntensity.LIGHT) {
        when (intensity) {
            HapticIntensity.LIGHT -> performHaptic(LIGHT_TICK_PATTERN, intArrayOf(50))
            HapticIntensity.MEDIUM -> performHaptic(MEDIUM_TICK_PATTERN, intArrayOf(100))
            HapticIntensity.STRONG -> performHaptic(STRONG_TICK_PATTERN, intArrayOf(150))
        }
    }
    
    /**
     * Provide volume adjustment feedback
     */
    fun provideVolumeFeedback(intensity: HapticIntensity = HapticIntensity.MEDIUM) {
        val amplitudes = when (intensity) {
            HapticIntensity.LIGHT -> intArrayOf(40)
            HapticIntensity.MEDIUM -> intArrayOf(80)
            HapticIntensity.STRONG -> intArrayOf(120)
        }
        performHaptic(VOLUME_TICK_PATTERN, amplitudes)
    }
    
    /**
     * Provide brightness adjustment feedback
     */
    fun provideBrightnessFeedback(intensity: HapticIntensity = HapticIntensity.MEDIUM) {
        val amplitudes = when (intensity) {
            HapticIntensity.LIGHT -> intArrayOf(30)
            HapticIntensity.MEDIUM -> intArrayOf(60)
            HapticIntensity.STRONG -> intArrayOf(90)
        }
        performHaptic(BRIGHTNESS_TICK_PATTERN, amplitudes)
    }
    
    /**
     * Provide long press start feedback
     */
    fun provideLongPressStartFeedback() {
        performHaptic(SEEK_START_PATTERN, intArrayOf(120, 80))
    }
    
    /**
     * Provide long press speed change feedback
     */
    fun provideLongPressSpeedChangeFeedback() {
        performHaptic(SPEED_CHANGE_PATTERN, intArrayOf(60, 40, 60))
    }
    
    /**
     * Provide long press direction change feedback
     */
    fun provideLongPressDirectionChangeFeedback() {
        performHaptic(DIRECTION_CHANGE_PATTERN, intArrayOf(80, 80))
    }
    
    /**
     * Provide long press end feedback
     */
    fun provideLongPressEndFeedback() {
        performHaptic(SEEK_END_PATTERN, intArrayOf(60, 40))
    }
    
    /**
     * Provide double tap feedback
     */
    fun provideDoubleTapFeedback() {
        performHaptic(DOUBLE_TICK_PATTERN, intArrayOf(70, 70))
    }
    
    /**
     * Provide zoom feedback
     */
    fun provideZoomFeedback() {
        performHaptic(ZOOM_TICK_PATTERN, intArrayOf(90))
    }
    
    /**
     * Provide error feedback
     */
    fun provideErrorFeedback() {
        performHaptic(ERROR_PATTERN, intArrayOf(150, 150, 150))
    }
    
    /**
     * Provide success feedback
     */
    fun provideSuccessFeedback() {
        performHaptic(SUCCESS_PATTERN, intArrayOf(100, 80, 60, 40, 20))
    }
    
    /**
     * Provide custom haptic feedback with pattern and amplitudes
     */
    fun provideCustomFeedback(pattern: LongArray, amplitudes: IntArray) {
        performHaptic(pattern, amplitudes)
    }
    
    /**
     * Provide continuous haptic feedback for long press seeking
     */
    fun startContinuousSeekFeedback(
        speed: Float,
        onStop: () -> Unit
    ): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            val interval = (200L / speed).toLong().coerceIn(50L, 500L)
            val amplitude = (50 + speed * 10).toInt().coerceIn(30, 120)
            
            while (isActive) {
                performHaptic(longArrayOf(0, 10), intArrayOf(amplitude))
                delay(interval)
            }
            onStop()
        }
    }
    
    /**
     * Provide rhythmic feedback for speed progression
     */
    fun provideSpeedProgressionFeedback(speedLevel: Int) {
        val pattern = when (speedLevel) {
            0 -> LIGHT_TICK_PATTERN
            1 -> DOUBLE_TICK_PATTERN
            2 -> TRIPLE_TICK_PATTERN
            3 -> longArrayOf(0, 15, 10, 15, 10, 15, 10, 15)
            4 -> longArrayOf(0, 20, 5, 20, 5, 20, 5, 20, 5, 20)
            else -> longArrayOf(0, 25, 5, 25, 5, 25, 5, 25, 5, 25, 5, 25)
        }
        
        val baseAmplitude = 60 + speedLevel * 15
        val amplitudes = IntArray(pattern.size / 2) { baseAmplitude }
        
        performHaptic(pattern, amplitudes)
    }
    
    /**
     * Provide adaptive feedback based on gesture accuracy
     */
    fun provideAdaptiveFeedback(accuracy: Float, gestureType: GestureType) {
        val intensity = when {
            accuracy > 0.9f -> HapticIntensity.LIGHT // High accuracy = subtle feedback
            accuracy > 0.7f -> HapticIntensity.MEDIUM // Medium accuracy = normal feedback
            else -> HapticIntensity.STRONG // Low accuracy = strong feedback for guidance
        }
        
        when (gestureType) {
            GestureType.HORIZONTAL_SEEK -> provideSeekFeedback(intensity)
            GestureType.VERTICAL_VOLUME -> provideVolumeFeedback(intensity)
            GestureType.VERTICAL_BRIGHTNESS -> provideBrightnessFeedback(intensity)
            GestureType.LONG_PRESS -> provideLongPressSpeedChangeFeedback()
            GestureType.DOUBLE_TAP -> provideDoubleTapFeedback()
            GestureType.PINCH_ZOOM -> provideZoomFeedback()
            else -> provideSeekFeedback(intensity)
        }
    }
    
    /**
     * Core haptic feedback execution
     */
    private fun performHaptic(pattern: LongArray, amplitudes: IntArray) {
        if (!isHapticEnabled || vibrator == null) return
        
        // Throttle haptic feedback to prevent overwhelming
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastHapticTime < hapticThrottleMs) return
        lastHapticTime = currentTime
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Apply global intensity multiplier
                val adjustedAmplitudes = amplitudes.map { 
                    (it * globalIntensityMultiplier).toInt().coerceIn(1, 255)
                }.toIntArray()
                
                val effect = VibrationEffect.createWaveform(pattern, adjustedAmplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                // Fallback for older Android versions
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to provide haptic feedback", e)
        }
    }
    
    /**
     * Check if haptic feedback is available on this device
     */
    fun isHapticAvailable(): Boolean {
        return vibrator?.hasVibrator() == true
    }
    
    /**
     * Get haptic capabilities of the device
     */
    fun getHapticCapabilities(): HapticCapabilities {
        if (vibrator == null) return HapticCapabilities.NONE
        
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl() -> 
                HapticCapabilities.ADVANCED
            vibrator.hasVibrator() -> 
                HapticCapabilities.BASIC
            else -> 
                HapticCapabilities.NONE
        }
    }
    
    enum class HapticCapabilities {
        NONE, BASIC, ADVANCED
    }
}

/**
 * Haptic pattern builder for creating custom feedback patterns
 */
class HapticPatternBuilder {
    private val timings = mutableListOf<Long>()
    private val amplitudes = mutableListOf<Int>()
    
    init {
        timings.add(0L) // Always start with 0 delay
    }
    
    /**
     * Add a vibration with specified duration and amplitude
     */
    fun vibrate(durationMs: Long, amplitude: Int = 100): HapticPatternBuilder {
        timings.add(durationMs)
        amplitudes.add(amplitude.coerceIn(1, 255))
        return this
    }
    
    /**
     * Add a pause with specified duration
     */
    fun pause(durationMs: Long): HapticPatternBuilder {
        timings.add(durationMs)
        amplitudes.add(0)
        return this
    }
    
    /**
     * Add a quick tick
     */
    fun tick(amplitude: Int = 80): HapticPatternBuilder {
        return vibrate(15, amplitude)
    }
    
    /**
     * Add a short pause
     */
    fun shortPause(): HapticPatternBuilder {
        return pause(20)
    }
    
    /**
     * Add a medium pause
     */
    fun mediumPause(): HapticPatternBuilder {
        return pause(50)
    }
    
    /**
     * Add a long pause
     */
    fun longPause(): HapticPatternBuilder {
        return pause(100)
    }
    
    /**
     * Build the final pattern
     */
    fun build(): Pair<LongArray, IntArray> {
        return Pair(timings.toLongArray(), amplitudes.toIntArray())
    }
}

/**
 * Predefined haptic patterns for common gestures
 */
object HapticPatterns {
    
    /**
     * Light confirmation tick
     */
    fun lightTick(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .tick(60)
            .build()
    }
    
    /**
     * Medium confirmation tick
     */
    fun mediumTick(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .tick(100)
            .build()
    }
    
    /**
     * Strong confirmation tick
     */
    fun strongTick(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .tick(150)
            .build()
    }
    
    /**
     * Double tap confirmation
     */
    fun doubleTap(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .tick(80)
            .shortPause()
            .tick(80)
            .build()
    }
    
    /**
     * Long press start pattern
     */
    fun longPressStart(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .vibrate(40, 120)
            .shortPause()
            .vibrate(20, 80)
            .build()
    }
    
    /**
     * Speed increase pattern
     */
    fun speedIncrease(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .tick(60)
            .pause(15)
            .tick(80)
            .pause(15)
            .tick(100)
            .build()
    }
    
    /**
     * Direction change pattern
     */
    fun directionChange(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .tick(90)
            .shortPause()
            .tick(90)
            .build()
    }
    
    /**
     * Error pattern
     */
    fun error(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .vibrate(100, 150)
            .mediumPause()
            .vibrate(100, 150)
            .mediumPause()
            .vibrate(100, 150)
            .build()
    }
    
    /**
     * Success pattern
     */
    fun success(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .tick(100)
            .shortPause()
            .tick(80)
            .pause(15)
            .tick(60)
            .pause(10)
            .tick(40)
            .pause(10)
            .tick(20)
            .build()
    }
    
    /**
     * Volume adjustment pattern
     */
    fun volumeAdjust(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .vibrate(12, 80)
            .build()
    }
    
    /**
     * Brightness adjustment pattern
     */
    fun brightnessAdjust(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .vibrate(8, 60)
            .build()
    }
    
    /**
     * Zoom adjustment pattern
     */
    fun zoomAdjust(): Pair<LongArray, IntArray> {
        return HapticPatternBuilder()
            .vibrate(20, 90)
            .build()
    }
}

/**
 * Haptic feedback analytics for learning user preferences
 */
class HapticFeedbackAnalytics {
    private val feedbackData = mutableListOf<HapticFeedbackData>()
    
    data class HapticFeedbackData(
        val timestamp: Long,
        val gestureType: GestureType,
        val intensity: HapticIntensity,
        val pattern: String,
        val userResponse: UserResponse // How user reacted to the feedback
    )
    
    enum class UserResponse {
        POSITIVE, // User continued gesture smoothly
        NEGATIVE, // User stopped or hesitated
        NEUTRAL   // No clear response
    }
    
    fun recordFeedback(
        gestureType: GestureType,
        intensity: HapticIntensity,
        pattern: String,
        userResponse: UserResponse
    ) {
        val data = HapticFeedbackData(
            timestamp = System.currentTimeMillis(),
            gestureType = gestureType,
            intensity = intensity,
            pattern = pattern,
            userResponse = userResponse
        )
        
        feedbackData.add(data)
        
        // Limit data size
        if (feedbackData.size > 1000) {
            feedbackData.removeAt(0)
        }
    }
    
    fun getPreferredIntensity(gestureType: GestureType): HapticIntensity {
        val gestureData = feedbackData.filter { it.gestureType == gestureType }
        if (gestureData.isEmpty()) return HapticIntensity.MEDIUM
        
        val intensityScores = gestureData.groupBy { it.intensity }
            .mapValues { (_, data) ->
                data.count { it.userResponse == UserResponse.POSITIVE }.toFloat() / data.size
            }
        
        return intensityScores.maxByOrNull { it.value }?.key ?: HapticIntensity.MEDIUM
    }
    
    fun getOverallSatisfactionRate(): Float {
        return if (feedbackData.isNotEmpty()) {
            feedbackData.count { it.userResponse == UserResponse.POSITIVE }.toFloat() / feedbackData.size
        } else {
            0.8f // Default assumption
        }
    }
    
    fun getSuggestedGlobalIntensity(): Float {
        val satisfactionRate = getOverallSatisfactionRate()
        return when {
            satisfactionRate > 0.8f -> 1.0f // Current intensity is good
            satisfactionRate > 0.6f -> 0.8f // Reduce intensity slightly
            satisfactionRate > 0.4f -> 1.2f // Increase intensity slightly
            else -> 0.6f // Significantly reduce intensity
        }
    }
}