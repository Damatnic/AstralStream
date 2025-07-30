package com.astralplayer.nextplayer.bookmark

data class Bookmark(
    val id: Long = 0,
    val videoUri: String,
    val position: Long,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "Default",
    val formattedPosition: String = "00:00",
    val formattedDate: String = ""
)