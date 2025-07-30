package com.astralplayer.nextplayer.feature.playback

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FrameNavigator(private val exoPlayer: ExoPlayer) {
    
    private val _isFrameMode = MutableStateFlow(false)
    val isFrameMode: StateFlow<Boolean> = _isFrameMode.asStateFlow()
    
    private val _currentFrame = MutableStateFlow(0L)
    val currentFrame: StateFlow<Long> = _currentFrame.asStateFlow()
    
    private var frameRate: Float = 30f
    private var frameDurationMs: Long = 33L // 1000ms / 30fps
    
    fun setFrameRate(fps: Float) {
        frameRate = fps
        frameDurationMs = (1000f / fps).toLong()
    }
    
    fun enterFrameMode() {
        _isFrameMode.value = true
        exoPlayer.pause()
        updateCurrentFrame()
    }
    
    fun exitFrameMode() {
        _isFrameMode.value = false
    }
    
    fun nextFrame() {
        if (!_isFrameMode.value) return
        
        val currentPosition = exoPlayer.currentPosition
        val nextPosition = currentPosition + frameDurationMs
        val duration = exoPlayer.duration
        
        if (nextPosition < duration) {
            exoPlayer.seekTo(nextPosition)
            updateCurrentFrame()
        }
    }
    
    fun previousFrame() {
        if (!_isFrameMode.value) return
        
        val currentPosition = exoPlayer.currentPosition
        val prevPosition = (currentPosition - frameDurationMs).coerceAtLeast(0)
        
        exoPlayer.seekTo(prevPosition)
        updateCurrentFrame()
    }
    
    fun seekToFrame(frameNumber: Long) {
        if (!_isFrameMode.value) return
        
        val position = frameNumber * frameDurationMs
        val duration = exoPlayer.duration
        
        if (position <= duration) {
            exoPlayer.seekTo(position)
            updateCurrentFrame()
        }
    }
    
    private fun updateCurrentFrame() {
        val currentPosition = exoPlayer.currentPosition
        _currentFrame.value = currentPosition / frameDurationMs
    }
    
    fun getCurrentFrameInfo(): FrameInfo {
        val position = exoPlayer.currentPosition
        val frameNumber = position / frameDurationMs
        val totalFrames = exoPlayer.duration / frameDurationMs
        
        return FrameInfo(
            frameNumber = frameNumber,
            totalFrames = totalFrames,
            timePosition = position,
            frameRate = frameRate
        )
    }
}

data class FrameInfo(
    val frameNumber: Long,
    val totalFrames: Long,
    val timePosition: Long,
    val frameRate: Float
)