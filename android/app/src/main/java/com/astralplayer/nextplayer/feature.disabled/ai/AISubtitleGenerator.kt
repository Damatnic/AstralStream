package com.astralplayer.nextplayer.feature.ai

interface AISubtitleGenerator {
    suspend fun generateSubtitles(videoPath: String): List<SubtitleEntry>
    suspend fun translateSubtitles(subtitles: List<SubtitleEntry>, targetLanguage: String): List<SubtitleEntry>
    fun isLanguageSupported(language: String): Boolean
    fun getSupportedLanguages(): List<String>
    fun clearCache()
}

data class SubtitleEntry(
    val startTime: Long,
    val endTime: Long,
    val text: String
)

enum class AISubtitleState {
    IDLE,
    GENERATING,
    COMPLETED,
    ERROR
}