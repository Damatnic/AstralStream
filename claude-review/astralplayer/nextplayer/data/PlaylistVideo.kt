package com.astralplayer.nextplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_videos")
data class PlaylistVideo(
    @PrimaryKey val id: Long = 0,
    val playlistId: Long,
    val uri: String,
    val title: String,
    val duration: Long = 0L,
    val position: Int = 0
)