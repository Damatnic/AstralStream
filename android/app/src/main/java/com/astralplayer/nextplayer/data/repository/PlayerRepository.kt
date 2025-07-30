package com.astralplayer.nextplayer.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.util.Log

class PlayerRepository {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition
    
    val playerState = MutableStateFlow(PlayerState())

    fun updatePlayingState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun updatePosition(position: Long) {
        _currentPosition.value = position
    }
    
    fun skipToNext() {
        Log.d("PlayerRepository", "Skip to next track")
        // Implementation would handle playlist navigation
        val currentState = playerState.value
        playerState.value = currentState.copy(currentPosition = 0L)
    }
    
    fun skipToPrevious() {
        Log.d("PlayerRepository", "Skip to previous track")
        // Implementation would handle playlist navigation
        val currentState = playerState.value
        playerState.value = currentState.copy(currentPosition = 0L)
    }
    
    fun playPlaylist(videos: List<com.astralplayer.nextplayer.data.PlaylistVideo>, shuffle: Boolean) {
        Log.d("PlayerRepository", "Playing playlist with ${videos.size} videos, shuffle: $shuffle")
        if (videos.isNotEmpty()) {
            val currentState = playerState.value
            playerState.value = currentState.copy(
                currentPosition = 0L,
                isPlaying = true
            )
        }
    }
    
    fun release() {
        Log.d("PlayerRepository", "Releasing player resources")
        _isPlaying.value = false
        _currentPosition.value = 0L
        playerState.value = PlayerState()
    }
}

data class PlayerState(
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isPlaying: Boolean = false,
    val videoSize: VideoSize = VideoSize(0, 0)
)

data class VideoSize(
    val width: Int,
    val height: Int
)