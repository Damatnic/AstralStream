package com.astralplayer.nextplayer.feature.playback

import android.content.Context
import android.os.CountDownTimer
import android.os.SystemClock
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * Sleep Timer Manager
 * Manages sleep timer functionality to automatically stop playback after a set duration
 */
class SleepTimerManager(
    private val context: Context
) {
    private var player: Player? = null
    private var timer: CountDownTimer? = null
    
    private val _isActive = MutableStateFlow(false)
    val isActive = _isActive.asStateFlow()
    
    private val _remainingTime = MutableStateFlow(0L) // in milliseconds
    val remainingTime = _remainingTime.asStateFlow()
    
    private val _endTime = MutableStateFlow(0L) // System time when timer will end
    val endTime = _endTime.asStateFlow()
    
    private val _fadeOutEnabled = MutableStateFlow(true)
    val fadeOutEnabled = _fadeOutEnabled.asStateFlow()
    
    private val _fadeOutDuration = MutableStateFlow(10000L) // 10 seconds default
    val fadeOutDuration = _fadeOutDuration.asStateFlow()
    
    private var originalVolume = 1.0f
    private var fadeOutTimer: CountDownTimer? = null
    
    /**
     * Initialize with a player instance
     */
    fun setPlayer(newPlayer: Player) {
        player = newPlayer
        originalVolume = newPlayer.volume
    }
    
    /**
     * Set a sleep timer for the specified duration
     * @param minutes Duration in minutes
     */
    fun setTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelTimer()
            return
        }
        
        val durationMillis = TimeUnit.MINUTES.toMillis(minutes.toLong())
        startTimer(durationMillis)
    }
    
    /**
     * Set a sleep timer to end at a specific time
     * @param targetTimeMillis Target system time in milliseconds
     */
    fun setTimerUntil(targetTimeMillis: Long) {
        val currentTime = System.currentTimeMillis()
        if (targetTimeMillis <= currentTime) {
            cancelTimer()
            return
        }
        
        val durationMillis = targetTimeMillis - currentTime
        startTimer(durationMillis)
    }
    
    /**
     * Extend the current timer by additional minutes
     * @param additionalMinutes Minutes to add to current timer
     */
    fun extendTimer(additionalMinutes: Int) {
        if (!_isActive.value || additionalMinutes <= 0) return
        
        val additionalMillis = TimeUnit.MINUTES.toMillis(additionalMinutes.toLong())
        val newDuration = _remainingTime.value + additionalMillis
        
        // Cancel current timer and start new one with extended duration
        timer?.cancel()
        startTimer(newDuration)
    }
    
    /**
     * Enable or disable fade out effect
     */
    fun setFadeOutEnabled(enabled: Boolean) {
        _fadeOutEnabled.value = enabled
    }
    
    /**
     * Set fade out duration
     * @param seconds Duration in seconds
     */
    fun setFadeOutDuration(seconds: Int) {
        _fadeOutDuration.value = TimeUnit.SECONDS.toMillis(seconds.toLong())
    }
    
    /**
     * Cancel the active timer
     */
    fun cancelTimer() {
        timer?.cancel()
        timer = null
        
        fadeOutTimer?.cancel()
        fadeOutTimer = null
        
        // Restore original volume if fade out was in progress
        player?.let { 
            if (it.volume < originalVolume) {
                it.volume = originalVolume
            }
        }
        
        _isActive.value = false
        _remainingTime.value = 0
        _endTime.value = 0
    }
    
    /**
     * Pause the timer (keep state but stop countdown)
     */
    fun pauseTimer() {
        if (!_isActive.value) return
        
        timer?.cancel()
        // Keep remaining time and end time as they are
    }
    
    /**
     * Resume a paused timer
     */
    fun resumeTimer() {
        if (!_isActive.value || _remainingTime.value <= 0) return
        
        startTimer(_remainingTime.value)
    }
    
    /**
     * Get formatted remaining time string
     */
    fun getFormattedRemainingTime(): String {
        val totalSeconds = _remainingTime.value / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Get sleep timer presets
     */
    fun getPresets(): List<TimerPreset> {
        return listOf(
            TimerPreset(5, "5 minutes"),
            TimerPreset(10, "10 minutes"),
            TimerPreset(15, "15 minutes"),
            TimerPreset(20, "20 minutes"),
            TimerPreset(30, "30 minutes"),
            TimerPreset(45, "45 minutes"),
            TimerPreset(60, "1 hour"),
            TimerPreset(90, "1.5 hours"),
            TimerPreset(120, "2 hours"),
            TimerPreset(0, "End of video", isSpecial = true)
        )
    }
    
    private fun startTimer(durationMillis: Long) {
        timer?.cancel()
        
        _endTime.value = System.currentTimeMillis() + durationMillis
        _isActive.value = true
        
        timer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingTime.value = millisUntilFinished
                
                // Start fade out if enabled and within fade out duration
                if (_fadeOutEnabled.value && millisUntilFinished <= _fadeOutDuration.value) {
                    startFadeOut(millisUntilFinished)
                }
            }
            
            override fun onFinish() {
                _remainingTime.value = 0
                handleTimerFinish()
            }
        }.start()
    }
    
    private fun startFadeOut(remainingMillis: Long) {
        if (fadeOutTimer != null) return // Already fading out
        
        player?.let { p ->
            originalVolume = p.volume
            val fadeSteps = 20
            val stepDuration = remainingMillis / fadeSteps
            
            fadeOutTimer = object : CountDownTimer(remainingMillis, stepDuration) {
                override fun onTick(millisUntilFinished: Long) {
                    val progress = 1f - (millisUntilFinished.toFloat() / remainingMillis)
                    val volume = originalVolume * (1f - progress)
                    p.volume = volume.coerceIn(0f, 1f)
                }
                
                override fun onFinish() {
                    p.volume = 0f
                }
            }.start()
        }
    }
    
    private fun handleTimerFinish() {
        player?.let { p ->
            // Pause playback
            p.pause()
            
            // Reset to beginning if desired
            // p.seekTo(0)
            
            // Restore volume
            p.volume = originalVolume
        }
        
        _isActive.value = false
        _endTime.value = 0
        
        // Could trigger a notification or callback here
        onTimerComplete?.invoke()
    }
    
    /**
     * Callback when timer completes
     */
    var onTimerComplete: (() -> Unit)? = null
    
    /**
     * Check if we should set "End of video" timer
     */
    fun checkEndOfVideoTimer() {
        player?.let { p ->
            if (p.duration > 0) {
                val remainingVideoTime = p.duration - p.currentPosition
                if (remainingVideoTime > 0) {
                    startTimer(remainingVideoTime)
                }
            }
        }
    }
    
    /**
     * Clean up resources
     */
    fun release() {
        cancelTimer()
        player = null
        onTimerComplete = null
    }
    
    data class TimerPreset(
        val minutes: Int,
        val label: String,
        val isSpecial: Boolean = false
    )
}