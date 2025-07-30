package com.astralplayer.nextplayer.feature.player.revolutionary

/**
 * Different playback modes for the video player
 */
enum class PlaybackMode {
    /**
     * Normal playback - play once and stop
     */
    NORMAL,
    
    /**
     * Repeat current video
     */
    REPEAT_ONE,
    
    /**
     * Repeat all videos in playlist
     */
    REPEAT_ALL,
    
    /**
     * Shuffle mode - random order
     */
    SHUFFLE,
    
    /**
     * Study mode - enhanced for learning with features like A-B repeat
     */
    STUDY,
    
    /**
     * Cinema mode - optimized for movie watching
     */
    CINEMA,
    
    /**
     * Music mode - optimized for music videos
     */
    MUSIC,
    
    /**
     * Presentation mode - optimized for presentations
     */
    PRESENTATION
}

/**
 * Extension functions for PlaybackMode
 */
fun PlaybackMode.isRepeating(): Boolean {
    return this == PlaybackMode.REPEAT_ONE || this == PlaybackMode.REPEAT_ALL
}

fun PlaybackMode.requiresPlaylist(): Boolean {
    return this == PlaybackMode.REPEAT_ALL || this == PlaybackMode.SHUFFLE
}

fun PlaybackMode.getDisplayName(): String {
    return when (this) {
        PlaybackMode.NORMAL -> "Normal"
        PlaybackMode.REPEAT_ONE -> "Repeat One"
        PlaybackMode.REPEAT_ALL -> "Repeat All"
        PlaybackMode.SHUFFLE -> "Shuffle"
        PlaybackMode.STUDY -> "Study Mode"
        PlaybackMode.CINEMA -> "Cinema Mode"
        PlaybackMode.MUSIC -> "Music Mode"
        PlaybackMode.PRESENTATION -> "Presentation Mode"
    }
}