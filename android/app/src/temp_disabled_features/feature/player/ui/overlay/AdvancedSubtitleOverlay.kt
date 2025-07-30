package com.astralplayer.nextplayer.feature.player.ui.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.feature.subtitles.*

/**
 * Enhanced subtitle overlay component with professional features
 */
@Composable
fun AdvancedSubtitleOverlay(
    modifier: Modifier = Modifier,
    subtitles: SubtitleModel?,
    currentPosition: Long,
    settings: AdvancedSubtitleSettings = AdvancedSubtitleSettings()
) {
    if (subtitles == null) return

    // Parse subtitle content based on format
    val cues = remember(subtitles) {
        when (subtitles.format) {
            SubtitleFormat.SRT -> SrtParser.parse(subtitles.content)
            SubtitleFormat.VTT -> VttParser.parse(subtitles.content) 
            SubtitleFormat.SSA, SubtitleFormat.ASS -> AssParser.parse(subtitles.content)
            SubtitleFormat.XML -> TtmlParser.parse(subtitles.content)
            else -> emptyList()
        }
    }

    // Find current subtitle cue based on playback position
    val currentCues by remember(currentPosition, cues) {
        derivedStateOf {
            cues.filter { cue ->
                currentPosition >= cue.startTime && currentPosition <= cue.endTime
            }
        }
    }

    // Determine if we need to show multiple cues (for multi-line dialogs)
    val shouldShowMultipleCues = currentCues.size > 1 && settings.allowMultipleLines

    Box(modifier = modifier.fillMaxSize()) {
        // Subtitle display area
        Box(
            modifier = Modifier
                .align(settings.position.toAlignment())
                .fillMaxWidth(if (settings.position == SubtitlePosition.BOTTOM) settings.width else 1f)
                .padding(
                    bottom = if (settings.position == SubtitlePosition.BOTTOM) 
                              settings.verticalMargin.dp + 48.dp // Extra padding to avoid controls
                            else settings.verticalMargin.dp,
                    top = if (settings.position == SubtitlePosition.TOP) 
                           settings.verticalMargin.dp
                         else 0.dp,
                    start = settings.horizontalMargin.dp,
                    end = settings.horizontalMargin.dp
                )
        ) {
            if (shouldShowMultipleCues) {
                // Show all current cues stacked
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    currentCues.forEach { cue ->
                        AnimatedSubtitleText(
                            text = cue.text,
                            settings = settings
                        )
                    }
                }
            } else {
                // Show only the first cue with animation
                AnimatedVisibility(
                    visible = currentCues.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    AnimatedSubtitleText(
                        text = currentCues.firstOrNull()?.text ?: "",
                        settings = settings
                    )
                }
            }
        }
    }
}

/**
 * Animated subtitle text with advanced styling
 */
@Composable
private fun AnimatedSubtitleText(
    text: String,
    settings: AdvancedSubtitleSettings
) {
    // Parse special formatting markers in text
    val parsedText = parseSubtitleText(text)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(settings.cornerRadius.dp))
            .background(
                if (settings.backgroundColor.alpha > 0f) {
                    settings.backgroundColor
                } else {
                    Color.Transparent
                }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .then(
                if (settings.textShadow) {
                    Modifier.graphicsLayer(alpha = 0.99f)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = parsedText,
            color = settings.textColor,
            fontSize = settings.fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            letterSpacing = settings.letterSpacing.sp,
            style = TextStyle(
                shadow = if (settings.textShadow) {
                    Shadow(
                        color = settings.shadowColor,
                        blurRadius = settings.shadowRadius
                    )
                } else null,
                fontFamily = when (settings.fontFamily) {
                    SubtitleFontFamily.DEFAULT -> FontFamily.Default
                    SubtitleFontFamily.SANS_SERIF -> FontFamily.SansSerif
                    SubtitleFontFamily.SERIF -> FontFamily.Serif
                    SubtitleFontFamily.MONOSPACE -> FontFamily.Monospace
                    SubtitleFontFamily.CURSIVE -> FontFamily.Cursive
                }
            ),
            lineHeight = (settings.fontSize * settings.lineHeight).sp,
            modifier = Modifier.drawWithContent {
                // First draw the original content
                drawContent()

                // Then draw the outline if enabled
                if (settings.outline) {
                    drawIntoCanvas { _ ->
                        // Outline drawing logic would go here
                        // Using Paint and other variables as needed
                    }
                    // Simplified outline drawing
                    // Simplified text outline drawing
                }
            }
        )
    }
}

/**
 * Parse special formatting in subtitle text (e.g., <i>italic</i>)
 */
private fun parseSubtitleText(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var currentIndex = 0

    // Simple pattern matching for basic HTML-like tags
    val italicPattern = "<i>(.*?)</i>".toRegex()
    val boldPattern = "<b>(.*?)</b>".toRegex()
    val underlinePattern = "<u>(.*?)</u>".toRegex()

    val plainText = text
        .replace(italicPattern) { it.groupValues[1] }
        .replace(boldPattern) { it.groupValues[1] }
        .replace(underlinePattern) { it.groupValues[1] }

    builder.append(plainText)

    // Apply styles
    italicPattern.findAll(text).forEach { match ->
        val start = plainText.indexOf(match.groupValues[1], currentIndex)
        if (start >= 0) {
            val end = start + match.groupValues[1].length
            builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
            currentIndex = end
        }
    }

    currentIndex = 0
    boldPattern.findAll(text).forEach { match ->
        val start = plainText.indexOf(match.groupValues[1], currentIndex)
        if (start >= 0) {
            val end = start + match.groupValues[1].length
            builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
            currentIndex = end
        }
    }

    currentIndex = 0
    underlinePattern.findAll(text).forEach { match ->
        val start = plainText.indexOf(match.groupValues[1], currentIndex)
        if (start >= 0) {
            val end = start + match.groupValues[1].length
            builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            currentIndex = end
        }
    }

    return builder.toAnnotatedString()
}

/**
 * Helper function to convert SubtitlePosition to Alignment
 */
fun SubtitlePosition.toAlignment(): Alignment = when(this) {
    SubtitlePosition.TOP, SubtitlePosition.TOP_CENTER, SubtitlePosition.TOP_LEFT, SubtitlePosition.TOP_RIGHT -> Alignment.TopCenter
    SubtitlePosition.CENTER, SubtitlePosition.MIDDLE, SubtitlePosition.MIDDLE_CENTER, SubtitlePosition.MIDDLE_LEFT, SubtitlePosition.MIDDLE_RIGHT -> Alignment.Center
    SubtitlePosition.BOTTOM, SubtitlePosition.BOTTOM_CENTER, SubtitlePosition.BOTTOM_LEFT, SubtitlePosition.BOTTOM_RIGHT -> Alignment.BottomCenter
}

/**
 * Enhanced subtitle display settings
 */
// Using the consolidated AdvancedSubtitleSettings from models package

// Parsers moved to their own files in the models package