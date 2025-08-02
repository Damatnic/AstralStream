package com.astralstream.nextplayer.models

data class SharedPlaylist(
    val id: String,
    val name: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val videoCount: Int,
    val totalDuration: Long,
    val tags: List<String>,
    val isPublic: Boolean,
    val likes: Int,
    val plays: Int,
    val createdAt: Long,
    val updatedAt: Long
)