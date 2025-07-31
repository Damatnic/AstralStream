package com.astralplayer.domain.model

/**
 * Domain model for subtitles
 * Supports multiple formats and AI-generated content
 */
data class Subtitle(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val language: String,
    val confidence: Float = 1.0f,
    val speaker: String? = null,
    val style: SubtitleStyle? = null,
    val metadata: SubtitleMetadata = SubtitleMetadata()
) {
    val duration: Long
        get() = endTime - startTime
    
    val formattedTime: String
        get() = "${formatTime(startTime)} --> ${formatTime(endTime)}"
    
    private fun formatTime(millis: Long): String {
        val hours = millis / 3600000
        val minutes = (millis % 3600000) / 60000
        val seconds = (millis % 60000) / 1000
        val ms = millis % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, ms)
    }
    
    data class SubtitleStyle(
        val fontSize: Int = 16,
        val fontColor: String = "#FFFFFF",
        val backgroundColor: String = "#80000000",
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val position: Position = Position.BOTTOM_CENTER
    )
    
    enum class Position {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }
    
    data class SubtitleMetadata(
        val provider: String = "unknown",
        val isAIGenerated: Boolean = false,
        val generatedAt: Long = System.currentTimeMillis(),
        val originalLanguage: String? = null,
        val isTranslated: Boolean = false,
        val keywords: List<String> = emptyList()
    )
}