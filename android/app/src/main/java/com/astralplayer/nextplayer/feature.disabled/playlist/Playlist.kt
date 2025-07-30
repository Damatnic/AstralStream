package com.astralplayer.nextplayer.feature.playlist

import kotlinx.serialization.Serializable

/**
 * Playlist data class
 * Represents a video playlist
 */
@Serializable
data class Playlist(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val videoCount: Int = 0,
    val totalDuration: Long = 0,
    val thumbnailPath: String? = null,
    val isDefault: Boolean = false
)