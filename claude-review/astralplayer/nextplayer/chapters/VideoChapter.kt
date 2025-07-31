package com.astralplayer.nextplayer.chapters

import android.net.Uri

/**
 * Data class representing a video chapter
 */
data class VideoChapter(
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val thumbnailUri: Uri? = null,
    val description: String? = null
) {
    val durationMs: Long get() = endTimeMs - startTimeMs
    
    /**
     * Check if a given time position falls within this chapter
     */
    fun containsPosition(positionMs: Long): Boolean {
        return positionMs in startTimeMs..endTimeMs
    }
    
    /**
     * Get formatted start time string
     */
    fun getFormattedStartTime(): String {
        return formatTime(startTimeMs)
    }
    
    /**
     * Get formatted duration string
     */
    fun getFormattedDuration(): String {
        return formatTime(durationMs)
    }
    
    companion object {
        /**
         * Format milliseconds to MM:SS or HH:MM:SS format
         */
        fun formatTime(milliseconds: Long): String {
            val seconds = milliseconds / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
            } else {
                String.format("%02d:%02d", minutes, seconds % 60)
            }
        }
    }
}