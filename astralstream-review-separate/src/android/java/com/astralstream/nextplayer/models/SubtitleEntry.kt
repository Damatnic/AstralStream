package com.astralstream.nextplayer.models

data class SubtitleEntry(
    val entries: List<SubtitleLine>,
    val language: String,
    val confidence: Float? = null
)

data class SubtitleLine(
    val startTime: Long,
    val endTime: Long,
    val text: String
)