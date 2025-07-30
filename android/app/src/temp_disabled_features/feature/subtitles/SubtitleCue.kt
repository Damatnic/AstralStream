package com.astralplayer.nextplayer.feature.subtitles

data class SubtitleCue(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val style: SubtitleStyle? = null
)

data class SubtitleStyle(
    val fontFamily: String? = null,
    val fontSize: Float? = null,
    val color: String? = null,
    val backgroundColor: String? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false
)