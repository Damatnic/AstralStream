package com.astralplayer.astralstream.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "subtitles",
    foreignKeys = [
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubtitleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: Long,
    val language: String,
    val languageCode: String,
    val filePath: String? = null,
    val isEmbedded: Boolean = false,
    val isAIGenerated: Boolean = false,
    val format: SubtitleFormat = SubtitleFormat.SRT,
    val addedTime: Long = System.currentTimeMillis()
)

enum class SubtitleFormat {
    SRT,
    VTT,
    ASS,
    SSA,
    TTML,
    DFXP
}