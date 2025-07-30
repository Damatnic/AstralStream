package com.astralplayer.nextplayer.feature.player.chapters

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a video chapter
 */
data class VideoChapter(
    val id: String,
    val title: String,
    val startTime: Long, // in milliseconds
    val endTime: Long,   // in milliseconds
    val thumbnail: Bitmap? = null,
    val description: String = "",
    val isUserCreated: Boolean = false
) {
    val duration: Long get() = endTime - startTime
    
    fun formatStartTime(): String {
        val totalSeconds = startTime / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    fun formatDuration(): String {
        val totalSeconds = duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * Manager for video chapters functionality
 */
class VideoChaptersManager(private val context: Context) {
    
    private val _chapters = MutableStateFlow<List<VideoChapter>>(emptyList())
    val chapters: StateFlow<List<VideoChapter>> = _chapters.asStateFlow()
    
    private val _currentChapter = MutableStateFlow<VideoChapter?>(null)
    val currentChapter: StateFlow<VideoChapter?> = _currentChapter.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Load chapters from video metadata or create automatic chapters
     */
    suspend fun loadChapters(videoUri: Uri, videoDuration: Long) {
        _isLoading.value = true
        
        withContext(Dispatchers.IO) {
            try {
                // Try to extract chapters from metadata first
                val metadataChapters = extractChaptersFromMetadata(videoUri)
                
                if (metadataChapters.isNotEmpty()) {
                    _chapters.value = metadataChapters
                } else {
                    // Create automatic chapters based on duration
                    val autoChapters = createAutomaticChapters(videoUri, videoDuration)
                    _chapters.value = autoChapters
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to simple time-based chapters
                _chapters.value = createSimpleTimeChapters(videoDuration)
            }
        }
        
        _isLoading.value = false
    }
    
    /**
     * Extract chapters from video metadata
     */
    private suspend fun extractChaptersFromMetadata(videoUri: Uri): List<VideoChapter> {
        return withContext(Dispatchers.IO) {
            val chapters = mutableListOf<VideoChapter>()
            
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)
                
                // Try to get chapter information (this is format-dependent)
                // For now, we'll create chapters based on scene changes or intervals
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                
                if (duration > 0) {
                    chapters.addAll(createSceneBasedChapters(retriever, duration))
                }
                
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            chapters
        }
    }
    
    /**
     * Create chapters based on scene analysis
     */
    private suspend fun createSceneBasedChapters(
        retriever: MediaMetadataRetriever,
        duration: Long
    ): List<VideoChapter> {
        val chapters = mutableListOf<VideoChapter>()
        val chapterInterval = maxOf(duration / 10, 30000L) // At least 30 seconds per chapter
        
        var currentTime = 0L
        var chapterIndex = 1
        
        while (currentTime < duration) {
            val endTime = minOf(currentTime + chapterInterval, duration)
            
            try {
                val thumbnail = retriever.getFrameAtTime(
                    currentTime * 1000, // Convert to microseconds
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                
                chapters.add(
                    VideoChapter(
                        id = "chapter_$chapterIndex",
                        title = "Chapter $chapterIndex",
                        startTime = currentTime,
                        endTime = endTime,
                        thumbnail = thumbnail,
                        description = "Auto-generated chapter"
                    )
                )
            } catch (e: Exception) {
                // Add chapter without thumbnail
                chapters.add(
                    VideoChapter(
                        id = "chapter_$chapterIndex",
                        title = "Chapter $chapterIndex",
                        startTime = currentTime,
                        endTime = endTime,
                        description = "Auto-generated chapter"
                    )
                )
            }
            
            currentTime = endTime
            chapterIndex++
        }
        
        return chapters
    }
    
    /**
     * Create automatic chapters based on video analysis
     */
    private suspend fun createAutomaticChapters(videoUri: Uri, duration: Long): List<VideoChapter> {
        return withContext(Dispatchers.IO) {
            val chapters = mutableListOf<VideoChapter>()
            
            // Create chapters every 5 minutes or based on video length
            val chapterDuration = when {
                duration < 300000 -> duration / 3 // Short videos: 3 chapters
                duration < 1800000 -> 300000L // Medium videos: 5-minute chapters
                else -> 600000L // Long videos: 10-minute chapters
            }
            
            var currentTime = 0L
            var chapterIndex = 1
            
            while (currentTime < duration) {
                val endTime = minOf(currentTime + chapterDuration, duration)
                
                chapters.add(
                    VideoChapter(
                        id = "auto_chapter_$chapterIndex",
                        title = "Part $chapterIndex",
                        startTime = currentTime,
                        endTime = endTime,
                        description = "Automatically generated chapter"
                    )
                )
                
                currentTime = endTime
                chapterIndex++
            }
            
            return@withContext chapters
        }
    }
    
    /**
     * Create simple time-based chapters as fallback
     */
    private fun createSimpleTimeChapters(duration: Long): List<VideoChapter> {
        val chapters = mutableListOf<VideoChapter>()
        val chapterCount = maxOf(duration / 600000L, 1L).toInt() // 10-minute chapters minimum
        val chapterDuration = duration / chapterCount
        
        repeat(chapterCount) { index ->
            val startTime = index * chapterDuration
            val endTime = if (index == chapterCount - 1) duration else (index + 1) * chapterDuration
            
            chapters.add(
                VideoChapter(
                    id = "simple_chapter_${index + 1}",
                    title = "Chapter ${index + 1}",
                    startTime = startTime,
                    endTime = endTime,
                    description = "Time-based chapter"
                )
            )
        }
        
        return chapters
    }
    
    /**
     * Update current chapter based on playback position
     */
    fun updateCurrentChapter(position: Long) {
        val current = _chapters.value.find { chapter ->
            position >= chapter.startTime && position < chapter.endTime
        }
        
        if (current != _currentChapter.value) {
            _currentChapter.value = current
        }
    }
    
    /**
     * Get chapter at specific position
     */
    fun getChapterAt(position: Long): VideoChapter? {
        return _chapters.value.find { chapter ->
            position >= chapter.startTime && position < chapter.endTime
        }
    }
    
    /**
     * Get next chapter
     */
    fun getNextChapter(currentPosition: Long): VideoChapter? {
        return _chapters.value.find { it.startTime > currentPosition }
    }
    
    /**
     * Get previous chapter
     */
    fun getPreviousChapter(currentPosition: Long): VideoChapter? {
        return _chapters.value.findLast { it.startTime < currentPosition }
    }
    
    /**
     * Add user-created chapter
     */
    fun addUserChapter(
        title: String,
        startTime: Long,
        endTime: Long,
        description: String = ""
    ) {
        val newChapter = VideoChapter(
            id = "user_chapter_${System.currentTimeMillis()}",
            title = title,
            startTime = startTime,
            endTime = endTime,
            description = description,
            isUserCreated = true
        )
        
        val updatedChapters = (_chapters.value + newChapter).sortedBy { it.startTime }
        _chapters.value = updatedChapters
    }
    
    /**
     * Remove chapter
     */
    fun removeChapter(chapterId: String) {
        _chapters.value = _chapters.value.filter { it.id != chapterId }
    }
    
    /**
     * Clear all chapters
     */
    fun clearChapters() {
        _chapters.value = emptyList()
        _currentChapter.value = null
    }
}

/**
 * Composable for displaying video chapters
 */
@Composable
fun VideoChaptersOverlay(
    chapters: List<VideoChapter>,
    currentChapter: VideoChapter?,
    currentPosition: Long,
    onChapterClick: (VideoChapter) -> Unit,
    onAddChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChaptersList by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        // Current chapter indicator
        currentChapter?.let { chapter ->
            CurrentChapterIndicator(
                chapter = chapter,
                currentPosition = currentPosition,
                onClick = { showChaptersList = !showChaptersList }
            )
        }
        
        // Chapters timeline
        if (chapters.isNotEmpty()) {
            ChaptersTimeline(
                chapters = chapters,
                currentChapter = currentChapter,
                currentPosition = currentPosition,
                onChapterClick = onChapterClick
            )
        }
        
        // Chapters list (expandable)
        AnimatedVisibility(
            visible = showChaptersList,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            ChaptersList(
                chapters = chapters,
                currentChapter = currentChapter,
                onChapterClick = { chapter ->
                    onChapterClick(chapter)
                    showChaptersList = false
                },
                onAddChapter = onAddChapter
            )
        }
    }
}

/**
 * Current chapter indicator
 */
@Composable
private fun CurrentChapterIndicator(
    chapter: VideoChapter,
    currentPosition: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PlayCircle,
                contentDescription = "Current Chapter",
                tint = Color(0xFF00BCD4),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                val progress = ((currentPosition - chapter.startTime).toFloat() / chapter.duration.toFloat()).coerceIn(0f, 1f)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = chapter.formatStartTime(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp),
                        color = Color(0xFF00BCD4),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    
                    Text(
                        text = chapter.formatDuration(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "Expand",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Chapters timeline
 */
@Composable
private fun ChaptersTimeline(
    chapters: List<VideoChapter>,
    currentChapter: VideoChapter?,
    currentPosition: Long,
    onChapterClick: (VideoChapter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        itemsIndexed(chapters) { index, chapter ->
            ChapterTimelineItem(
                chapter = chapter,
                isActive = chapter.id == currentChapter?.id,
                onClick = { onChapterClick(chapter) }
            )
        }
    }
}

/**
 * Chapter timeline item
 */
@Composable
private fun ChapterTimelineItem(
    chapter: VideoChapter,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        // Thumbnail or placeholder
        Box(
            modifier = Modifier
                .size(60.dp, 34.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isActive) Color(0xFF00BCD4).copy(alpha = 0.3f)
                    else Color.White.copy(alpha = 0.1f)
                )
                .border(
                    width = if (isActive) 2.dp else 0.dp,
                    color = Color(0xFF00BCD4),
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (chapter.thumbnail != null) {
                Image(
                    bitmap = chapter.thumbnail.asImageBitmap(),
                    contentDescription = chapter.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.VideoFile,
                    contentDescription = chapter.title,
                    tint = if (isActive) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = chapter.formatStartTime(),
            color = if (isActive) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Chapters list
 */
@Composable
private fun ChaptersList(
    chapters: List<VideoChapter>,
    currentChapter: VideoChapter?,
    onChapterClick: (VideoChapter) -> Unit,
    onAddChapter: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chapters (${chapters.size})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onAddChapter) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Chapter",
                        tint = Color(0xFF00BCD4)
                    )
                }
            }
            
            // Chapters list
            LazyColumn {
                items(chapters) { chapter ->
                    ChapterListItem(
                        chapter = chapter,
                        isActive = chapter.id == currentChapter?.id,
                        onClick = { onChapterClick(chapter) }
                    )
                }
            }
        }
    }
}

/**
 * Chapter list item
 */
@Composable
private fun ChapterListItem(
    chapter: VideoChapter,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isActive) Color(0xFF00BCD4).copy(alpha = 0.2f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp, 27.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (chapter.thumbnail != null) {
                Image(
                    bitmap = chapter.thumbnail.asImageBitmap(),
                    contentDescription = chapter.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.VideoFile,
                    contentDescription = chapter.title,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                color = if (isActive) Color(0xFF00BCD4) else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (chapter.description.isNotEmpty()) {
                Text(
                    text = chapter.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = chapter.formatStartTime(),
                color = if (isActive) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = chapter.formatDuration(),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
        
        if (chapter.isUserCreated) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Person,
                contentDescription = "User Created",
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}