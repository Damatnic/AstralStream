package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.bookmark.VideoChapter
import com.astralplayer.nextplayer.bookmark.VideoChapters

/**
 * Chapter Navigation Bar
 * Shows video chapters as a horizontal scrollable list
 */
@Composable
fun ChapterNavigationBar(
    chapters: VideoChapters?,
    currentPosition: Long,
    onChapterClick: (VideoChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    if (chapters == null || chapters.chapters.isEmpty()) return
    
    val currentChapter = chapters.chapters.find { chapter ->
        currentPosition >= chapter.bookmark.position && 
        (chapter.endPosition == null || currentPosition < chapter.endPosition)
    }
    
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Current chapter indicator
            currentChapter?.let { chapter ->
                CurrentChapterIndicator(chapter)
            }
            
            // Chapter list
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(chapters.chapters) { chapter ->
                    ChapterChip(
                        chapter = chapter,
                        isActive = chapter == currentChapter,
                        onClick = { onChapterClick(chapter) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentChapterIndicator(
    chapter: VideoChapter
) {
    BubbleCard(
        elevation = 2,
        cornerRadius = 16,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = "Chapter ${chapter.chapterIndex + 1}: ${chapter.bookmark.title}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChapterChip(
    chapter: VideoChapter,
    isActive: Boolean,
    onClick: () -> Unit
) {
    BubbleCard(
        onClick = onClick,
        elevation = if (isActive) 4 else 1,
        cornerRadius = 20,
        containerColor = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Chapter ${chapter.chapterIndex + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
            
            Text(
                text = chapter.bookmark.title,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = formatTime(chapter.bookmark.position),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                }
            )
        }
    }
}

/**
 * Chapter Progress Indicator
 * Shows chapter progress on the seek bar
 */
@Composable
fun ChapterProgressIndicator(
    chapters: VideoChapters?,
    duration: Long,
    modifier: Modifier = Modifier
) {
    if (chapters == null || chapters.chapters.isEmpty() || duration <= 0) return
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        chapters.chapters.forEach { chapter ->
            val startProgress = chapter.bookmark.position.toFloat() / duration
            val endProgress = (chapter.endPosition ?: duration).toFloat() / duration
            val width = endProgress - startProgress
            
            // Chapter marker
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(width)
                    .offset(x = (startProgress * 100).dp)
                    .background(
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            
            // Chapter divider
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .offset(x = (startProgress * 100).dp)
                    .background(Color.White.copy(alpha = 0.5f))
            )
        }
    }
}

/**
 * Compact Chapter List
 * For displaying in limited space
 */
@Composable
fun CompactChapterList(
    chapters: VideoChapters?,
    currentPosition: Long,
    onChapterClick: (VideoChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    if (chapters == null || chapters.chapters.isEmpty()) return
    
    val currentChapterIndex = chapters.chapters.indexOfFirst { chapter ->
        currentPosition >= chapter.bookmark.position && 
        (chapter.endPosition == null || currentPosition < chapter.endPosition)
    }.coerceAtLeast(0)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous chapter
        BubbleIconButton(
            onClick = {
                if (currentChapterIndex > 0) {
                    onChapterClick(chapters.chapters[currentChapterIndex - 1])
                }
            },
            icon = Icons.Default.SkipPrevious,
            size = 32,
            iconSize = 16,
            enabled = currentChapterIndex > 0,
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White
        )
        
        // Current chapter info
        BubbleCard(
            elevation = 0,
            cornerRadius = 16,
            containerColor = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (currentChapterIndex >= 0) {
                    "${currentChapterIndex + 1}/${chapters.chapters.size} • ${chapters.chapters[currentChapterIndex].bookmark.title}"
                } else {
                    "No chapter"
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        
        // Next chapter
        BubbleIconButton(
            onClick = {
                if (currentChapterIndex < chapters.chapters.size - 1) {
                    onChapterClick(chapters.chapters[currentChapterIndex + 1])
                }
            },
            icon = Icons.Default.SkipNext,
            size = 32,
            iconSize = 16,
            enabled = currentChapterIndex < chapters.chapters.size - 1,
            containerColor = Color.Black.copy(alpha = 0.5f),
            contentColor = Color.White
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}