package com.astralplayer.astralstream.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "playback_states",
    foreignKeys = [
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaybackStateEntity(
    @PrimaryKey
    val videoId: Long,
    val position: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val audioTrackIndex: Int = 0,
    val subtitleTrackIndex: Int = -1,
    val volume: Float = 1.0f,
    val brightness: Float = 0.5f,
    val lastUpdated: Long = System.currentTimeMillis()
)