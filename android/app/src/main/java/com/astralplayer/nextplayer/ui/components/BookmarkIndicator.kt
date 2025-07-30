package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.bookmark.VideoBookmark
import kotlinx.coroutines.delay

/**
 * Bookmark Indicator
 * Shows when the current playback position is near a bookmark
 */
@Composable
fun BookmarkIndicator(
    bookmark: VideoBookmark?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = bookmark != null,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier
    ) {
        bookmark?.let {
            BubbleCard(
                elevation = 8,
                cornerRadius = 20,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(bookmark.color))
                    )
                    
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = bookmark.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Bookmark Progress Markers
 * Shows bookmark positions on the progress bar
 */
@Composable
fun BookmarkProgressMarkers(
    bookmarks: List<VideoBookmark>,
    duration: Long,
    modifier: Modifier = Modifier
) {
    if (bookmarks.isEmpty() || duration <= 0) return
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        bookmarks.forEach { bookmark ->
            val progress = bookmark.position.toFloat() / duration
            
            // Bookmark marker
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(x = (progress * 100).dp)
                    .clip(CircleShape)
                    .background(Color(bookmark.color))
            )
        }
    }
}

/**
 * Bookmark Quick Jump Button
 * Shows a button to quickly jump to the nearest bookmark
 */
@Composable
fun BookmarkQuickJump(
    nearestBookmark: VideoBookmark?,
    onJumpToBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = nearestBookmark != null,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        BubbleButton(
            onClick = onJumpToBookmark,
            icon = Icons.Default.BookmarkBorder,
            text = "Jump to bookmark",
            cornerRadius = 24,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    }
}

/**
 * Bookmark Toast
 * Shows a toast notification when a bookmark is added
 */
@Composable
fun BookmarkToast(
    message: String?,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(message) {
        if (message != null) {
            visible = true
            delay(2000)
            visible = false
        }
    }
    
    AnimatedVisibility(
        visible = visible && message != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        BubbleCard(
            elevation = 12,
            cornerRadius = 24,
            containerColor = MaterialTheme.colorScheme.inverseSurface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = message ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }
}