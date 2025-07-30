package com.astralplayer.astralstream.core.data.mappers

import com.astralplayer.astralstream.core.common.Utils
import com.astralplayer.astralstream.core.database.entities.AudioStreamInfoEntity
import com.astralplayer.astralstream.core.database.entities.SubtitleStreamInfoEntity
import com.astralplayer.astralstream.core.database.relations.MediumWithInfo
import com.astralplayer.astralstream.core.model.Video
import java.util.Date

fun MediumWithInfo.toVideo() = Video(
    id = mediumEntity.mediaStoreId,
    path = mediumEntity.path,
    parentPath = mediumEntity.parentPath,
    duration = mediumEntity.duration,
    uriString = mediumEntity.uriString,
    nameWithExtension = mediumEntity.name,
    width = mediumEntity.width,
    height = mediumEntity.height,
    size = mediumEntity.size,
    dateModified = mediumEntity.modified,
    format = mediumEntity.format,
    thumbnailPath = mediumEntity.thumbnailPath,
    playbackPosition = mediumEntity.playbackPosition,
    lastPlayedAt = mediumEntity.lastPlayedTime?.let { Date(it) },
    formattedDuration = Utils.formatDurationMillis(mediumEntity.duration),
    formattedFileSize = Utils.formatFileSize(mediumEntity.size),
    videoStream = videoStreamInfo?.toVideoStreamInfo(),
    audioStreams = audioStreamsInfo.map(AudioStreamInfoEntity::toAudioStreamInfo),
    subtitleStreams = subtitleStreamsInfo.map(SubtitleStreamInfoEntity::toSubtitleStreamInfo),
)
