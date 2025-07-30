package com.astralplayer.astralstream.core.data.mappers

import com.astralplayer.astralstream.core.data.models.VideoState
import com.astralplayer.astralstream.core.database.converter.UriListConverter
import com.astralplayer.astralstream.core.database.entities.MediumEntity

fun MediumEntity.toVideoState(): VideoState {
    return VideoState(
        path = path,
        title = name,
        position = playbackPosition.takeIf { it != 0L },
        audioTrackIndex = audioTrackIndex,
        subtitleTrackIndex = subtitleTrackIndex,
        playbackSpeed = playbackSpeed,
        externalSubs = UriListConverter.fromStringToList(externalSubs),
        videoScale = videoScale,
        thumbnailPath = thumbnailPath,
    )
}
