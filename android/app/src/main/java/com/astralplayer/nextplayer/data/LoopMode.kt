package com.astralplayer.nextplayer.data

import androidx.media3.common.Player

/**
 * Loop mode options for video playback
 * Based on Next Player's implementation but enhanced for Astral Vu
 */
enum class LoopMode {
    OFF,     // No looping
    ONE,     // Repeat current video
    ALL;     // Repeat all videos in playlist
    
    /**
     * Convert to ExoPlayer repeat mode
     */
    fun toExoPlayerRepeatMode(): Int {
        return when (this) {
            OFF -> Player.REPEAT_MODE_OFF
            ONE -> Player.REPEAT_MODE_ONE
            ALL -> Player.REPEAT_MODE_ALL
        }
    }
    
    /**
     * Get next loop mode in cycle (OFF -> ONE -> ALL -> OFF)
     */
    fun next(): LoopMode {
        return when (this) {
            OFF -> ONE
            ONE -> ALL
            ALL -> OFF
        }
    }
    
    /**
     * Get display name for UI
     */
    fun getDisplayName(): String {
        return when (this) {
            OFF -> "Loop Off"
            ONE -> "Loop One"
            ALL -> "Loop All"
        }
    }
    
    /**
     * Get description for UI
     */
    fun getDescription(): String {
        return when (this) {
            OFF -> "No looping"
            ONE -> "Repeat current video"
            ALL -> "Repeat all videos"
        }
    }
}