package com.astralplayer.nextplayer.models

enum class SubtitleLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Spanish"),
    FRENCH("fr", "French"),
    GERMAN("de", "German"),
    ITALIAN("it", "Italian"),
    PORTUGUESE("pt", "Portuguese"),
    RUSSIAN("ru", "Russian"),
    JAPANESE("ja", "Japanese"),
    KOREAN("ko", "Korean"),
    CHINESE("zh", "Chinese"),
    ARABIC("ar", "Arabic"),
    HINDI("hi", "Hindi");
    
    companion object {
        fun fromCode(code: String): SubtitleLanguage? {
            return values().find { it.code == code }
        }
    }
}

enum class SubtitleStyle {
    DEFAULT,
    CLASSIC,
    MODERN,
    OUTLINE,
    SHADOW,
    TRANSPARENT_BG
}