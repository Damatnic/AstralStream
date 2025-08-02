package com.astralstream.nextplayer.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shared_playlists")
data class SharedPlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val videoCount: Int,
    val totalDuration: Long,
    val tags: String, // JSON array stored as string
    val isPublic: Boolean,
    val likes: Int,
    val plays: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val localPlaylistId: String? = null
)