package com.astralplayer.nextplayer.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SubtitleDisplaySettings(
    val fontSize: Float = 16f,
    val fontColor: Color = Color.White,
    val backgroundColor: Color = Color.Black.copy(alpha = 0.7f),
    val position: SubtitlePosition = SubtitlePosition.BOTTOM_CENTER,
    val maxLines: Int = 2,
    val showBackground: Boolean = true,
    val fontWeight: FontWeight = FontWeight.Normal
)

enum class SubtitlePosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

data class CurrentSubtitle(
    val text: String,
    val startTime: Long,
    val endTime: Long,
    val language: String
)

class SubtitleRenderer constructor() {
    
    private val _currentSubtitle = MutableStateFlow<CurrentSubtitle?>(null)
    val currentSubtitle: StateFlow<CurrentSubtitle?> = _currentSubtitle.asStateFlow()
    
    private val _displaySettings = MutableStateFlow(SubtitleDisplaySettings())
    val displaySettings: StateFlow<SubtitleDisplaySettings> = _displaySettings.asStateFlow()
    
    private var subtitles: List<SubtitleEntry> = emptyList()
    
    fun setSubtitles(newSubtitles: List<SubtitleEntry>) {
        subtitles = newSubtitles.sortedBy { it.startTime }
    }
    
    fun updateDisplaySettings(settings: SubtitleDisplaySettings) {
        _displaySettings.value = settings
    }
    
    fun updateCurrentPosition(positionMs: Long) {
        val currentSub = findSubtitleAtPosition(positionMs)
        if (currentSub != _currentSubtitle.value) {
            _currentSubtitle.value = currentSub
        }
    }
    
    private fun findSubtitleAtPosition(positionMs: Long): CurrentSubtitle? {
        return subtitles.find { subtitle ->
            positionMs >= subtitle.startTime && positionMs <= subtitle.endTime
        }?.let { subtitle ->
            CurrentSubtitle(
                text = subtitle.text,
                startTime = subtitle.startTime,
                endTime = subtitle.endTime,
                language = subtitle.language
            )
        }
    }
    
    fun clearSubtitles() {
        subtitles = emptyList()
        _currentSubtitle.value = null
    }
    
    fun hasSubtitles(): Boolean = subtitles.isNotEmpty()
    
    fun getSubtitleCount(): Int = subtitles.size
}

@Composable
fun SubtitleOverlay(
    subtitleRenderer: SubtitleRenderer,
    modifier: Modifier = Modifier
) {
    val currentSubtitle by subtitleRenderer.currentSubtitle.collectAsState()
    val displaySettings by subtitleRenderer.displaySettings.collectAsState()
    
    currentSubtitle?.let { subtitle ->
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = getAlignment(displaySettings.position)
        ) {
            SubtitleText(
                text = subtitle.text,
                settings = displaySettings
            )
        }
    }
}

@Composable
private fun SubtitleText(
    text: String,
    settings: SubtitleDisplaySettings
) {
    val textModifier = if (settings.showBackground) {
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(settings.backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    } else {
        Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    }
    
    Text(
        text = text,
        modifier = textModifier,
        color = settings.fontColor,
        fontSize = settings.fontSize.sp,
        fontWeight = settings.fontWeight,
        textAlign = TextAlign.Center,
        maxLines = settings.maxLines,
        lineHeight = (settings.fontSize * 1.2f).sp
    )
}

private fun getAlignment(position: SubtitlePosition): Alignment {
    return when (position) {
        SubtitlePosition.TOP_LEFT -> Alignment.TopStart
        SubtitlePosition.TOP_CENTER -> Alignment.TopCenter
        SubtitlePosition.TOP_RIGHT -> Alignment.TopEnd
        SubtitlePosition.MIDDLE_LEFT -> Alignment.CenterStart
        SubtitlePosition.MIDDLE_CENTER -> Alignment.Center
        SubtitlePosition.MIDDLE_RIGHT -> Alignment.CenterEnd
        SubtitlePosition.BOTTOM_LEFT -> Alignment.BottomStart
        SubtitlePosition.BOTTOM_CENTER -> Alignment.BottomCenter
        SubtitlePosition.BOTTOM_RIGHT -> Alignment.BottomEnd
    }
}

// Extension functions for easier subtitle management
fun SubtitleRenderer.loadSubtitlesFromSpeechSegments(segments: List<SpeechSegment>) {
    val subtitleEntries = segments.map { segment ->
        SubtitleEntry(
            startTime = segment.startTime,
            endTime = segment.endTime,
            text = segment.text,
            language = segment.language,
            confidence = segment.confidence
        )
    }
    setSubtitles(subtitleEntries)
}

fun SubtitleRenderer.exportSubtitlesToSRT(): String {
    val subtitles = (currentSubtitle.value?.let { listOf(it) } ?: emptyList())
    return subtitles.mapIndexed { index, subtitle ->
        val startTime = formatSRTTime(subtitle.startTime)
        val endTime = formatSRTTime(subtitle.endTime)
        "${index + 1}\n$startTime --> $endTime\n${subtitle.text}\n"
    }.joinToString("\n")
}

private fun formatSRTTime(timeMs: Long): String {
    val hours = timeMs / 3600000
    val minutes = (timeMs % 3600000) / 60000
    val seconds = (timeMs % 60000) / 1000
    val milliseconds = timeMs % 1000
    return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
}