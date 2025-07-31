package com.astralplayer.domain.model

/**
 * Domain model for video metadata
 * Part of clean architecture implementation
 */
data class VideoMetadata(
    val id: String,
    val title: String,
    val uri: String,
    val duration: Long,
    val width: Int,
    val height: Int,
    val format: String,
    val codec: String,
    val bitrate: Long,
    val frameRate: Float,
    val isPlayable: Boolean,
    val hasAudio: Boolean,
    val audioChannels: Int,
    val audioSampleRate: Int,
    val fileSize: Long,
    val lastModified: Long,
    val thumbnailUri: String? = null,
    val chapterCount: Int = 0,
    val subtitleTracks: List<SubtitleTrack> = emptyList()
) {
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height else 16f / 9f
    
    val qualityLabel: String
        get() = when {
            height >= 2160 -> "4K Ultra HD"
            height >= 1440 -> "2K QHD"
            height >= 1080 -> "Full HD"
            height >= 720 -> "HD"
            height >= 480 -> "SD"
            else -> "Low Quality"
        }
    
    val isHighQuality: Boolean
        get() = height >= 720
    
    data class SubtitleTrack(
        val id: String,
        val language: String,
        val label: String,
        val isDefault: Boolean = false,
        val isForced: Boolean = false
    )
}