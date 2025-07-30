package com.astralplayer.nextplayer.feature.subtitles

enum class SubtitleFormat {
    SRT,
    VTT,
    ASS,
    SSA,
    TTML,
    XML,
    UNKNOWN
}

data class SubtitleModel(
    val id: String,
    val format: SubtitleFormat,
    val language: String,
    val content: String,
    val isDefault: Boolean = false
)

enum class SubtitlePosition {
    TOP,
    TOP_CENTER,
    TOP_LEFT,
    TOP_RIGHT,
    CENTER,
    MIDDLE,
    MIDDLE_CENTER,
    MIDDLE_LEFT,
    MIDDLE_RIGHT,
    BOTTOM,
    BOTTOM_CENTER,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

enum class SubtitleFontFamily {
    DEFAULT,
    SERIF,
    SANS_SERIF,
    MONOSPACE,
    CURSIVE
}

data class AdvancedSubtitleSettings(
    val fontSize: Float = 16f,
    val fontFamily: SubtitleFontFamily = SubtitleFontFamily.DEFAULT,
    val position: SubtitlePosition = SubtitlePosition.BOTTOM,
    val backgroundColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0x80000000),
    val textColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    val isEnabled: Boolean = true,
    val width: Float = 0.9f,
    val verticalMargin: Float = 16f,
    val horizontalMargin: Float = 16f,
    val allowMultipleLines: Boolean = true,
    val cornerRadius: Float = 4f,
    val textShadow: Boolean = true,
    val shadowColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black,
    val shadowRadius: Float = 4f,
    val letterSpacing: Float = 0f,
    val lineHeight: Float = 1.2f,
    val outline: Boolean = false
)