package com.astralplayer.astralstream.core.data.mappers

import com.astralplayer.astralstream.core.database.entities.AudioStreamInfoEntity
import com.astralplayer.astralstream.core.model.AudioStreamInfo

fun AudioStreamInfoEntity.toAudioStreamInfo() = AudioStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    sampleFormat = sampleFormat,
    sampleRate = sampleRate,
    channels = channels,
    channelLayout = channelLayout,
)
