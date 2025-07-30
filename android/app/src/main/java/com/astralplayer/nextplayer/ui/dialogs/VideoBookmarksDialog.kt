package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.astralplayer.nextplayer.bookmark.VideoBookmark
import com.astralplayer.nextplayer.bookmark.VideoBookmarkManager
import com.astralplayer.nextplayer.bookmark.VideoChapter
import com.astralplayer.nextplayer.ui.components.*
import kotlinx.coroutines.flow.collectAsState
import kotlinx.coroutines.launch
import java.io.File

/**
 * Bookmarks Management Dialog
 * Allows users to view, add, edit, and navigate to bookmarks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoBookmarksDialog(
    bookmarkManager: VideoBookmarkManager,
    videoUri: String,
    videoTitle: String,
    currentPosition: Long,
    duration: Long,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val bookmarks by bookmarkManager.getBookmarksForVideo(videoUri).collectAsState(initial = emptyList())
    var showAddBookmark by remember { mutableStateOf(false) }
    var editingBookmark by remember { mutableStateOf<VideoBookmark?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Bookmarks, 1: Chapters
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        BubbleCard(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            elevation = 24,
            cornerRadius = 32,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                BookmarksDialogHeader(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onClose = onDismiss,
                    onAdd = { showAddBookmark = true }
                )
                
                // Content
                when (selectedTab) {
                    0 -> {
                        // Bookmarks tab
                        if (bookmarks.isEmpty()) {
                            EmptyBookmarksState(
                                onAddBookmark = { showAddBookmark = true },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(bookmarks) { bookmark ->
                                    BookmarkItem(
                                        bookmark = bookmark,
                                        onSeekTo = {
                                            onSeekTo(bookmark.position)
                                            onDismiss()
                                        },
                                        onEdit = { editingBookmark = bookmark },
                                        onDelete = {
                                            scope.launch {
                                                bookmarkManager.deleteBookmark(bookmark.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Chapters tab
                        ChaptersTab(
                            bookmarkManager = bookmarkManager,
                            videoUri = videoUri,
                            videoTitle = videoTitle,
                            duration = duration,
                            onSeekTo = onSeekTo,
                            onDismiss = onDismiss
                        )
                    }
                }
                
                // Add current position button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    BubbleButton(
                        onClick = { showAddBookmark = true },
                        text = "Bookmark Current Position",
                        icon = Icons.Default.BookmarkAdd,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
    
    // Add/Edit bookmark dialog
    if (showAddBookmark || editingBookmark != null) {
        AddEditBookmarkDialog(
            bookmarkManager = bookmarkManager,
            videoUri = videoUri,
            videoTitle = videoTitle,
            currentPosition = currentPosition,
            duration = duration,
            existingBookmark = editingBookmark,
            onDismiss = {
                showAddBookmark = false
                editingBookmark = null
            }
        )
    }
}

@Composable
private fun BookmarksDialogHeader(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onClose: () -> Unit,
    onAdd: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bookmarks & Chapters",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BubbleIconButton(
                    onClick = onAdd,
                    icon = Icons.Default.Add,
                    size = 36,
                    iconSize = 20,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
                
                BubbleIconButton(
                    onClick = onClose,
                    icon = Icons.Default.Close,
                    size = 36,
                    iconSize = 20
                )
            }
        }
        
        // Tab selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = { Text("Bookmarks") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = { Text("Chapters") }
            )
        }
    }
}

@Composable
private fun EmptyBookmarksState(
    onAddBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Bookmarks Yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Add bookmarks to quickly jump to your favorite moments",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        BubbleButton(
            onClick = onAddBookmark,
            text = "Add First Bookmark",
            icon = Icons.Default.Add,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun BookmarkItem(
    bookmark: VideoBookmark,
    onSeekTo: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    BubbleCard(
        onClick = onSeekTo,
        elevation = 2,
        cornerRadius = 20,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            bookmark.thumbnailPath?.let { path ->
                if (File(path).exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(File(path)),
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 80.dp, height = 60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    ThumbnailPlaceholder(
                        modifier = Modifier
                            .size(width = 80.dp, height = 60.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            } ?: ThumbnailPlaceholder(
                modifier = Modifier
                    .size(width = 80.dp, height = 60.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            
            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(bookmark.color))
                    )
                    
                    Text(
                        text = bookmark.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    text = formatTime(bookmark.position),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                bookmark.note?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Actions
            Box {
                BubbleIconButton(
                    onClick = { showMenu = true },
                    icon = Icons.Default.MoreVert,
                    size = 32,
                    iconSize = 18
                )
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThumbnailPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ChaptersTab(
    bookmarkManager: VideoBookmarkManager,
    videoUri: String,
    videoTitle: String,
    duration: Long,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val chapters by bookmarkManager.getChaptersForVideo(videoUri).collectAsState(initial = null)
    
    if (chapters == null || chapters.chapters.isEmpty()) {
        // No chapters state
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.List,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Chapters",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Generate chapters to navigate through video sections",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            BubbleButton(
                onClick = {
                    scope.launch {
                        bookmarkManager.autoGenerateChapters(
                            videoUri = videoUri,
                            videoTitle = videoTitle,
                            duration = duration
                        )
                    }
                },
                text = "Auto-Generate Chapters",
                icon = Icons.Default.AutoAwesome,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    } else {
        // Show chapters
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Auto-generated indicator
            if (chapters.autoGenerated) {
                item {
                    BubbleCard(
                        elevation = 1,
                        cornerRadius = 12,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Text(
                                text = "Auto-generated chapters",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            items(chapters.chapters) { chapter ->
                ChapterItem(
                    chapter = chapter,
                    onSeekTo = {
                        onSeekTo(chapter.bookmark.position)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: VideoChapter,
    onSeekTo: () -> Unit
) {
    BubbleCard(
        onClick = onSeekTo,
        elevation = 2,
        cornerRadius = 16,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter number
            BubbleCard(
                elevation = 0,
                cornerRadius = 12,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${chapter.chapterIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            // Chapter info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = chapter.bookmark.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatTime(chapter.bookmark.position),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val duration = chapter.endPosition?.let { it - chapter.bookmark.position } ?: 0
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Thumbnail
            chapter.bookmark.thumbnailPath?.let { path ->
                if (File(path).exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(File(path)),
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 60.dp, height = 45.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditBookmarkDialog(
    bookmarkManager: VideoBookmarkManager,
    videoUri: String,
    videoTitle: String,
    currentPosition: Long,
    duration: Long,
    existingBookmark: VideoBookmark?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEditing = existingBookmark != null
    
    var title by remember { 
        mutableStateOf(existingBookmark?.title ?: "Bookmark at ${formatTime(currentPosition)}")
    }
    var note by remember { mutableStateOf(existingBookmark?.note ?: "") }
    var selectedColor by remember { 
        mutableStateOf(existingBookmark?.color ?: 0xFF2196F3.toInt())
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        BubbleCard(
            elevation = 16,
            cornerRadius = 24,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Text(
                    text = if (isEditing) "Edit Bookmark" else "Add Bookmark",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Position info
                BubbleCard(
                    elevation = 1,
                    cornerRadius = 12,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = formatTime(existingBookmark?.position ?: currentPosition),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Note input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3
                )
                
                // Color picker
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val colors = listOf(
                            0xFF2196F3.toInt(), // Blue
                            0xFF4CAF50.toInt(), // Green
                            0xFFFF9800.toInt(), // Orange
                            0xFFF44336.toInt(), // Red
                            0xFF9C27B0.toInt(), // Purple
                            0xFF795548.toInt(), // Brown
                            0xFF607D8B.toInt()  // Blue Grey
                        )
                        
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .clickable { selectedColor = color }
                            ) {
                                if (selectedColor == color) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BubbleButton(
                        onClick = onDismiss,
                        text = "Cancel",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    BubbleButton(
                        onClick = {
                            scope.launch {
                                if (isEditing && existingBookmark != null) {
                                    bookmarkManager.updateBookmark(
                                        bookmarkId = existingBookmark.id,
                                        title = title,
                                        note = note.ifBlank { null },
                                        color = selectedColor
                                    )
                                } else {
                                    bookmarkManager.addBookmark(
                                        videoUri = videoUri,
                                        videoTitle = videoTitle,
                                        position = currentPosition,
                                        duration = duration,
                                        title = title,
                                        note = note.ifBlank { null },
                                        color = selectedColor
                                    )
                                }
                                onDismiss()
                            }
                        },
                        text = if (isEditing) "Update" else "Add",
                        icon = Icons.Default.Check,
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
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

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    
    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}