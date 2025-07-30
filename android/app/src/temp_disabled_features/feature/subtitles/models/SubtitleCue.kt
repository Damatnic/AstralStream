package com.astralplayer.nextplayer.feature.subtitles.models

/**
 * Represents a single subtitle cue with timing and text
 */
data class SubtitleCue(
    val id: String,
    val startTime: Long, // in milliseconds
    val endTime: Long,   // in milliseconds
    val text: String
)