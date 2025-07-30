package com.astralplayer.nextplayer.data

// Temporarily simplified without Room annotations
data class RecentFile(
    val id: String,
    val uri: String,
    val title: String,
    val duration: Long, // in milliseconds
    val lastPosition: Long = 0, // last played position in milliseconds
    val lastPlayed: Long = System.currentTimeMillis(), // timestamp
    val thumbnailPath: String? = null,
    val fileSize: Long = 0,
    val mimeType: String? = null,
    val isCloudFile: Boolean = false,
    val cloudProvider: String? = null,
    val cloudFileId: String? = null,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val tags: String = "",
    val metadata: String = ""
)