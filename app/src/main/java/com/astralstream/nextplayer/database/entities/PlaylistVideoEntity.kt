package com.astralstream.nextplayer.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_videos")
data class PlaylistVideoEntity(
    @PrimaryKey
    val id: String,
    val playlistId: String,
    val videoUri: String,
    val title: String,
    val duration: Long,
    val position: Int,
    val addedAt: Long
)