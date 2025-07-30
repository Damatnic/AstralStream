package com.astralplayer.astralstream.core.data.mappers

import com.astralplayer.astralstream.core.database.entities.VideoStreamInfoEntity
import com.astralplayer.astralstream.core.model.VideoStreamInfo

fun VideoStreamInfoEntity.toVideoStreamInfo() = VideoStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    frameRate = frameRate,
    frameWidth = frameWidth,
    frameHeight = frameHeight,
)
