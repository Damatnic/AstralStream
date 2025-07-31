package com.astralplayer.nextplayer.chapters

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.regex.Pattern

/**
 * Manages video chapters detection and navigation
 */
class VideoChapterManager(
    private val context: Context,
    private val exoPlayer: ExoPlayer
) {
    private val _chapters = MutableStateFlow<List<VideoChapter>>(emptyList())
    val chapters: StateFlow<List<VideoChapter>> = _chapters.asStateFlow()
    
    private val _currentChapterIndex = MutableStateFlow(-1)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()
    
    /**
     * Detect chapters from various sources
     */
    fun detectChapters(mediaItem: MediaItem, videoDuration: Long) {
        val detectedChapters = mutableListOf<VideoChapter>()
        
        // 1. Try to get chapters from media metadata
        mediaItem.mediaMetadata.extras?.let { extras ->
            // Some videos might have chapter data in metadata
            val chapterCount = extras.getInt("chapter_count", 0)
            if (chapterCount > 0) {
                for (i in 0 until chapterCount) {
                    val title = extras.getString("chapter_${i}_title") ?: "Chapter ${i + 1}"
                    val start = extras.getLong("chapter_${i}_start", 0L)
                    val end = extras.getLong("chapter_${i}_end", 0L)
                    
                    if (end > start) {
                        detectedChapters.add(
                            VideoChapter(
                                title = title,
                                startTimeMs = start,
                                endTimeMs = end
                            )
                        )
                    }
                }
            }
        }
        
        // 2. Try to detect from filename patterns (e.g., "Video_Part1_00-05-30.mp4")
        if (detectedChapters.isEmpty()) {
            mediaItem.localConfiguration?.uri?.lastPathSegment?.let { filename ->
                detectChaptersFromFilename(filename, videoDuration)?.let { chapters ->
                    detectedChapters.addAll(chapters)
                }
            }
        }
        
        // 3. Try to load from external chapter file (.chp or .chapters)
        if (detectedChapters.isEmpty()) {
            mediaItem.localConfiguration?.uri?.let { uri ->
                loadChaptersFromFile(uri)?.let { chapters ->
                    detectedChapters.addAll(chapters)
                }
            }
        }
        
        // 4. Auto-generate chapters if none found (divide into equal segments)
        if (detectedChapters.isEmpty() && videoDuration > 300000) { // > 5 minutes
            detectedChapters.addAll(generateAutoChapters(videoDuration))
        }
        
        _chapters.value = detectedChapters.sortedBy { it.startTimeMs }
    }
    
    /**
     * Detect chapters from filename patterns
     */
    private fun detectChaptersFromFilename(filename: String, duration: Long): List<VideoChapter>? {
        // Pattern for timestamps in filename (e.g., "00-05-30" or "00h05m30s")
        val timePattern = Pattern.compile("(\\d{1,2})[:-]?(\\d{2})[:-]?(\\d{2})")
        val matcher = timePattern.matcher(filename)
        
        val timestamps = mutableListOf<Long>()
        while (matcher.find()) {
            val hours = matcher.group(1)?.toIntOrNull() ?: 0
            val minutes = matcher.group(2)?.toIntOrNull() ?: 0
            val seconds = matcher.group(3)?.toIntOrNull() ?: 0
            
            val timeMs = (hours * 3600 + minutes * 60 + seconds) * 1000L
            timestamps.add(timeMs)
        }
        
        return if (timestamps.size >= 2) {
            timestamps.zipWithNext { start, end ->
                VideoChapter(
                    title = "Part ${timestamps.indexOf(start) + 1}",
                    startTimeMs = start,
                    endTimeMs = end
                )
            }.toMutableList().apply {
                // Add last chapter to video end
                if (timestamps.isNotEmpty()) {
                    add(
                        VideoChapter(
                            title = "Part ${timestamps.size}",
                            startTimeMs = timestamps.last(),
                            endTimeMs = duration
                        )
                    )
                }
            }
        } else null
    }
    
    /**
     * Load chapters from external file
     */
    private fun loadChaptersFromFile(videoUri: Uri): List<VideoChapter>? {
        try {
            val videoPath = videoUri.path ?: return null
            val videoFile = File(videoPath)
            val chapterFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.chapters")
            
            if (!chapterFile.exists()) {
                // Try .chp extension
                val chpFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.chp")
                if (!chpFile.exists()) return null
                return parseChapterFile(chpFile)
            }
            
            return parseChapterFile(chapterFile)
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Parse chapter file format:
     * 00:00:00 Introduction
     * 00:05:30 Chapter 1 - Getting Started
     * 00:15:45 Chapter 2 - Advanced Topics
     */
    private fun parseChapterFile(file: File): List<VideoChapter> {
        val chapters = mutableListOf<VideoChapter>()
        val lines = file.readLines()
        
        val timePattern = Pattern.compile("^(\\d{1,2}):(\\d{2}):(\\d{2})\\s+(.+)$")
        
        lines.forEachIndexed { index, line ->
            val matcher = timePattern.matcher(line.trim())
            if (matcher.matches()) {
                val hours = matcher.group(1)?.toIntOrNull() ?: 0
                val minutes = matcher.group(2)?.toIntOrNull() ?: 0
                val seconds = matcher.group(3)?.toIntOrNull() ?: 0
                val title = matcher.group(4) ?: "Chapter ${index + 1}"
                
                val startTimeMs = (hours * 3600 + minutes * 60 + seconds) * 1000L
                
                // Find end time from next chapter or use Long.MAX_VALUE for last chapter
                val endTimeMs = if (index < lines.size - 1) {
                    // Look for next timestamp
                    val nextLine = lines.subList(index + 1, lines.size).firstOrNull { nextLine ->
                        timePattern.matcher(nextLine.trim()).matches()
                    }
                    
                    nextLine?.let {
                        val nextMatcher = timePattern.matcher(it.trim())
                        if (nextMatcher.matches()) {
                            val nextHours = nextMatcher.group(1)?.toIntOrNull() ?: 0
                            val nextMinutes = nextMatcher.group(2)?.toIntOrNull() ?: 0
                            val nextSeconds = nextMatcher.group(3)?.toIntOrNull() ?: 0
                            (nextHours * 3600 + nextMinutes * 60 + nextSeconds) * 1000L
                        } else Long.MAX_VALUE
                    } ?: Long.MAX_VALUE
                } else {
                    Long.MAX_VALUE
                }
                
                chapters.add(
                    VideoChapter(
                        title = title,
                        startTimeMs = startTimeMs,
                        endTimeMs = endTimeMs
                    )
                )
            }
        }
        
        // Update last chapter's end time to actual video duration
        if (chapters.isNotEmpty() && chapters.last().endTimeMs == Long.MAX_VALUE) {
            val duration = exoPlayer.duration
            if (duration > 0) {
                chapters[chapters.lastIndex] = chapters.last().copy(endTimeMs = duration)
            }
        }
        
        return chapters
    }
    
    /**
     * Generate auto chapters by dividing video into equal segments
     */
    private fun generateAutoChapters(duration: Long): List<VideoChapter> {
        val chapterDuration = 600000L // 10 minutes per chapter
        val chapterCount = ((duration / chapterDuration) + 1).toInt()
        
        return List(chapterCount) { index ->
            val startTime = index * chapterDuration
            val endTime = minOf((index + 1) * chapterDuration, duration)
            
            VideoChapter(
                title = "Chapter ${index + 1}",
                startTimeMs = startTime,
                endTimeMs = endTime
            )
        }
    }
    
    /**
     * Update current chapter based on playback position
     */
    fun updateCurrentChapter(position: Long) {
        val chapters = _chapters.value
        if (chapters.isEmpty()) {
            _currentChapterIndex.value = -1
            return
        }
        
        val currentIndex = chapters.indexOfFirst { it.containsPosition(position) }
        _currentChapterIndex.value = currentIndex
    }
    
    /**
     * Jump to specific chapter
     */
    fun jumpToChapter(chapterIndex: Int) {
        val chapters = _chapters.value
        if (chapterIndex in chapters.indices) {
            exoPlayer.seekTo(chapters[chapterIndex].startTimeMs)
        }
    }
    
    /**
     * Jump to next chapter
     */
    fun nextChapter() {
        val currentIndex = _currentChapterIndex.value
        if (currentIndex < _chapters.value.size - 1) {
            jumpToChapter(currentIndex + 1)
        }
    }
    
    /**
     * Jump to previous chapter
     */
    fun previousChapter() {
        val currentIndex = _currentChapterIndex.value
        val currentPosition = exoPlayer.currentPosition
        
        if (currentIndex > 0) {
            val currentChapter = _chapters.value.getOrNull(currentIndex)
            
            // If we're more than 3 seconds into the chapter, restart current chapter
            if (currentChapter != null && currentPosition > currentChapter.startTimeMs + 3000) {
                jumpToChapter(currentIndex)
            } else {
                jumpToChapter(currentIndex - 1)
            }
        } else if (currentIndex == 0) {
            // Restart first chapter
            jumpToChapter(0)
        }
    }
    
    /**
     * Add custom chapter
     */
    fun addCustomChapter(title: String, startTimeMs: Long, endTimeMs: Long) {
        val newChapter = VideoChapter(
            title = title,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs
        )
        
        val updatedChapters = (_chapters.value + newChapter).sortedBy { it.startTimeMs }
        _chapters.value = updatedChapters
    }
    
    /**
     * Remove chapter
     */
    fun removeChapter(chapterIndex: Int) {
        val chapters = _chapters.value.toMutableList()
        if (chapterIndex in chapters.indices) {
            chapters.removeAt(chapterIndex)
            _chapters.value = chapters
        }
    }
    
    /**
     * Save chapters to file
     */
    fun saveChaptersToFile(videoUri: Uri) {
        try {
            val videoPath = videoUri.path ?: return
            val videoFile = File(videoPath)
            val chapterFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}.chapters")
            
            val content = _chapters.value.joinToString("\n") { chapter ->
                "${VideoChapter.formatTime(chapter.startTimeMs)} ${chapter.title}"
            }
            
            chapterFile.writeText(content)
        } catch (e: Exception) {
            // Handle error
        }
    }
}