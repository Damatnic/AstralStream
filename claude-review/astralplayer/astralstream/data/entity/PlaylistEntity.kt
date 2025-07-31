package com.astralplayer.astralstream.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val thumbnailPath: String? = null,
    val createdTime: Long = System.currentTimeMillis(),
    val lastModifiedTime: Long = System.currentTimeMillis(),
    val videoCount: Int = 0,
    val totalDuration: Long = 0,
    val isSystemPlaylist: Boolean = false,
    val playlistType: PlaylistType = PlaylistType.CUSTOM
)

enum class PlaylistType {
    CUSTOM,
    RECENTLY_PLAYED,
    FAVORITES,
    WATCH_LATER,
    MOST_PLAYED,
    RECENTLY_ADDED
}