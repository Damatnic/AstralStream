package com.astralstream.nextplayer.models

data class SharedPlaylistMetadata(
    val id: String,
    val originalPlaylistId: String,
    val name: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val videoCount: Int,
    val totalDuration: Long,
    val tags: List<String>,
    val isPublic: Boolean,
    val createdAt: Long,
    val version: Int
)