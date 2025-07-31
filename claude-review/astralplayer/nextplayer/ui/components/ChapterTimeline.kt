package com.astralplayer.nextplayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.chapters.VideoChapter

/**
 * Timeline component that displays video chapters with markers
 */
@Composable
fun ChapterTimeline(
    chapters: List<VideoChapter>,
    currentPosition: Long,
    duration: Long,
    currentChapterIndex: Int,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var timelineWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Timeline with chapters
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .onSizeChanged { size ->
                    timelineWidth = size.width
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { /* Handle click to seek */ }
            ) {
                if (duration > 0 && timelineWidth > 0) {
                    drawChapterTimeline(
                        chapters = chapters,
                        currentPosition = currentPosition,
                        duration = duration,
                        currentChapterIndex = currentChapterIndex,
                        timelineWidth = timelineWidth.toFloat()
                    )
                }
            }
        }
        
        // Current chapter info
        currentChapterIndex.takeIf { it >= 0 }?.let { index ->
            chapters.getOrNull(index)?.let { chapter ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chapter ${index + 1}:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(
                            text = "${chapter.getFormattedStartTime()} - ${VideoChapter.formatTime(chapter.endTimeMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draw the chapter timeline on canvas
 */
private fun DrawScope.drawChapterTimeline(
    chapters: List<VideoChapter>,
    currentPosition: Long,
    duration: Long,
    currentChapterIndex: Int,
    timelineWidth: Float
) {
    val timelineHeight = size.height
    val trackHeight = 8.dp.toPx()
    val trackY = (timelineHeight - trackHeight) / 2
    
    // Draw background track
    drawRoundRect(
        color = Color.White.copy(alpha = 0.1f),
        topLeft = Offset(0f, trackY),
        size = Size(timelineWidth, trackHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2)
    )
    
    // Draw chapters
    chapters.forEachIndexed { index, chapter ->
        val startX = (chapter.startTimeMs.toFloat() / duration) * timelineWidth
        val endX = (chapter.endTimeMs.toFloat() / duration) * timelineWidth
        val width = endX - startX
        
        // Draw chapter segment
        drawRoundRect(
            color = if (index == currentChapterIndex) {
                Color(0xFF4CAF50) // Current chapter - green
            } else if (index % 2 == 0) {
                Color.White.copy(alpha = 0.3f)
            } else {
                Color.White.copy(alpha = 0.2f)
            },
            topLeft = Offset(startX, trackY),
            size = Size(width, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f)
        )
        
        // Draw chapter marker
        if (index > 0) {
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(startX, trackY - 4.dp.toPx()),
                end = Offset(startX, trackY + trackHeight + 4.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
    
    // Draw progress
    val progressX = (currentPosition.toFloat() / duration) * timelineWidth
    drawRoundRect(
        color = Color(0xFF2196F3), // Blue progress
        topLeft = Offset(0f, trackY),
        size = Size(progressX, trackHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2)
    )
    
    // Draw playhead
    drawCircle(
        color = Color.White,
        radius = 6.dp.toPx(),
        center = Offset(progressX, trackY + trackHeight / 2)
    )
    
    // Draw playhead outline
    drawCircle(
        color = Color(0xFF2196F3),
        radius = 6.dp.toPx(),
        center = Offset(progressX, trackY + trackHeight / 2),
        style = Stroke(width = 2.dp.toPx())
    )
}

/**
 * Compact chapter indicator for video controls
 */
@Composable
fun ChapterIndicator(
    currentChapter: VideoChapter?,
    currentChapterIndex: Int,
    totalChapters: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${currentChapterIndex + 1}/$totalChapters",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            currentChapter?.let { chapter ->
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
            }
        }
    }
}