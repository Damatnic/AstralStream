package com.astralplayer.astralstream.core.data.mappers

import com.astralplayer.astralstream.core.database.entities.SubtitleStreamInfoEntity
import com.astralplayer.astralstream.core.model.SubtitleStreamInfo

fun SubtitleStreamInfoEntity.toSubtitleStreamInfo() = SubtitleStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
)
