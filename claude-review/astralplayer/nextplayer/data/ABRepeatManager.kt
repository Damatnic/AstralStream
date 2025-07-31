package com.astralplayer.nextplayer.data

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages A-B repeat functionality for video playback
 */
class ABRepeatManager(
    private val exoPlayer: ExoPlayer,
    private val scope: CoroutineScope
) {
    private val _abRepeatState = MutableStateFlow(ABRepeatState())
    val abRepeatState: StateFlow<ABRepeatState> = _abRepeatState.asStateFlow()
    
    private var repeatJob: Job? = null
    
    data class ABRepeatState(
        val isEnabled: Boolean = false,
        val pointA: Long? = null,
        val pointB: Long? = null,
        val isSettingPoint: Boolean = false,
        val currentPoint: ABPoint = ABPoint.NONE
    )
    
    enum class ABPoint {
        NONE, A, B
    }
    
    fun toggleABRepeat() {
        val currentState = _abRepeatState.value
        if (currentState.isEnabled) {
            // Disable A-B repeat
            disableABRepeat()
        } else {
            // Enable A-B repeat and start setting point A
            _abRepeatState.value = ABRepeatState(
                isEnabled = true,
                isSettingPoint = true,
                currentPoint = ABPoint.A
            )
        }
    }
    
    fun setPointA() {
        val currentPosition = exoPlayer.currentPosition
        _abRepeatState.value = _abRepeatState.value.copy(
            pointA = currentPosition,
            currentPoint = ABPoint.B,
            isSettingPoint = true
        )
    }
    
    fun setPointB() {
        val currentPosition = exoPlayer.currentPosition
        val pointA = _abRepeatState.value.pointA
        
        if (pointA != null && currentPosition > pointA) {
            _abRepeatState.value = _abRepeatState.value.copy(
                pointB = currentPosition,
                currentPoint = ABPoint.NONE,
                isSettingPoint = false
            )
            startABRepeatLoop()
        } else {
            // Invalid point B (before point A), reset
            _abRepeatState.value = ABRepeatState(
                isEnabled = true,
                isSettingPoint = true,
                currentPoint = ABPoint.A
            )
        }
    }
    
    fun setABPoints() {
        when (_abRepeatState.value.currentPoint) {
            ABPoint.A -> setPointA()
            ABPoint.B -> setPointB()
            ABPoint.NONE -> {
                // If both points are set, clear and start over
                if (_abRepeatState.value.pointA != null && _abRepeatState.value.pointB != null) {
                    _abRepeatState.value = ABRepeatState(
                        isEnabled = true,
                        isSettingPoint = true,
                        currentPoint = ABPoint.A
                    )
                }
            }
        }
    }
    
    private fun startABRepeatLoop() {
        repeatJob?.cancel()
        
        val pointA = _abRepeatState.value.pointA ?: return
        val pointB = _abRepeatState.value.pointB ?: return
        
        // Seek to point A immediately
        exoPlayer.seekTo(pointA)
        
        repeatJob = scope.launch {
            while (isActive && _abRepeatState.value.isEnabled) {
                val currentPosition = exoPlayer.currentPosition
                
                // Check if we've reached or passed point B
                if (currentPosition >= pointB) {
                    exoPlayer.seekTo(pointA)
                }
                
                // Check position every 100ms
                delay(100)
            }
        }
    }
    
    fun disableABRepeat() {
        repeatJob?.cancel()
        repeatJob = null
        _abRepeatState.value = ABRepeatState()
    }
    
    fun clearPoints() {
        disableABRepeat()
    }
    
    /**
     * Format time for display
     */
    fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    fun getFormattedPointA(): String? {
        return _abRepeatState.value.pointA?.let { formatTime(it) }
    }
    
    fun getFormattedPointB(): String? {
        return _abRepeatState.value.pointB?.let { formatTime(it) }
    }
    
    fun onDestroy() {
        disableABRepeat()
    }
}