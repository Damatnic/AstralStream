package com.astralplayer.astralstream.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.astralplayer.astralstream.data.model.VideoFormat
import com.astralplayer.astralstream.data.model.VideoSource

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val path: String,
    val duration: Long = 0,
    val size: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val format: VideoFormat = VideoFormat.MP4,
    val source: VideoSource = VideoSource.LOCAL,
    val thumbnailPath: String? = null,
    val lastPlayedTime: Long? = null,
    val addedTime: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val folderId: Long? = null,
    val cloudProviderId: String? = null,
    val cloudFileId: String? = null,
    val subtitlePaths: List<String> = emptyList(),
    val audioTracks: List<String> = emptyList(),
    val chapters: List<ChapterInfo> = emptyList()
)

data class ChapterInfo(
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val thumbnailPath: String? = null
)