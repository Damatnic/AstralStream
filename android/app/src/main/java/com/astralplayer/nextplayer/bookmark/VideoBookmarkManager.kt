package com.astralplayer.nextplayer.bookmark

import android.content.Context
import android.graphics.Bitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.astralplayer.nextplayer.utils.ThumbnailGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bookmarks")

/**
 * Bookmark Manager
 * Manages video bookmarks and chapters
 */
class VideoBookmarkManager(
    private val context: Context,
    private val thumbnailGenerator: ThumbnailGenerator
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
    }
    
    companion object {
        private val BOOKMARKS_KEY = stringPreferencesKey("bookmarks")
        private val CHAPTERS_KEY = stringPreferencesKey("chapters")
        
        private const val THUMBNAIL_DIR = "bookmark_thumbnails"
        private const val CHAPTER_THUMBNAIL_DIR = "chapter_thumbnails"
    }
    
    /**
     * Add a bookmark at the current position
     */
    suspend fun addBookmark(
        videoUri: String,
        videoTitle: String,
        position: Long,
        duration: Long,
        title: String,
        note: String? = null,
        color: Int = 0xFF2196F3.toInt(),
        generateThumbnail: Boolean = true
    ): VideoBookmark {
        val bookmarkId = System.currentTimeMillis()
        
        // Generate thumbnail if requested
        val thumbnailPath = if (generateThumbnail) {
            try {
                val uri = android.net.Uri.parse(videoUri)
                val thumbnail = thumbnailGenerator.getThumbnailAtPosition(uri, position * 1000) // Convert to microseconds
                thumbnail?.let { saveThumbnail(it, bookmarkId.toString()) }
            } catch (e: Exception) {
                null
            }
        } else null
        
        val bookmark = VideoBookmark(
            id = bookmarkId,
            videoUri = videoUri,
            videoTitle = videoTitle,
            position = position,
            duration = duration,
            title = title,
            note = note,
            thumbnailPath = thumbnailPath,
            color = color,
            isChapter = false
        )
        
        saveBookmark(bookmark)
        return bookmark
    }
    
    /**
     * Get all bookmarks for a video
     */
    fun getBookmarksForVideo(videoUri: String): Flow<List<VideoBookmark>> {
        return context.dataStore.data.map { preferences ->
            val bookmarksJson = preferences[BOOKMARKS_KEY] ?: "[]"
            try {
                val allBookmarks = json.decodeFromString<List<VideoBookmark>>(bookmarksJson)
                allBookmarks.filter { it.videoUri == videoUri && !it.isChapter }
                    .sortedBy { it.position }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Get all bookmarks
     */
    fun getAllBookmarks(): Flow<List<VideoBookmark>> {
        return context.dataStore.data.map { preferences ->
            val bookmarksJson = preferences[BOOKMARKS_KEY] ?: "[]"
            try {
                val bookmarks = json.decodeFromString<List<VideoBookmark>>(bookmarksJson)
                bookmarks.filter { !it.isChapter }.sortedByDescending { it.createdAt }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Update a bookmark
     */
    suspend fun updateBookmark(
        bookmarkId: Long,
        title: String? = null,
        note: String? = null,
        color: Int? = null
    ) {
        val bookmarks = getAllBookmarksSync().toMutableList()
        val index = bookmarks.indexOfFirst { it.id == bookmarkId }
        if (index != -1) {
            val bookmark = bookmarks[index]
            bookmarks[index] = bookmark.copy(
                title = title ?: bookmark.title,
                note = note ?: bookmark.note,
                color = color ?: bookmark.color
            )
            saveAllBookmarks(bookmarks)
        }
    }
    
    /**
     * Delete a bookmark
     */
    suspend fun deleteBookmark(bookmarkId: Long) {
        val bookmarks = getAllBookmarksSync().toMutableList()
        val bookmark = bookmarks.find { it.id == bookmarkId }
        if (bookmark != null) {
            bookmarks.remove(bookmark)
            saveAllBookmarks(bookmarks)
            
            // Delete thumbnail if exists
            bookmark.thumbnailPath?.let { path ->
                File(path).delete()
            }
        }
    }
    
    /**
     * Create chapters for a video
     */
    suspend fun createChapters(
        videoUri: String,
        videoTitle: String,
        chapters: List<Pair<Long, String>>, // Position to title
        duration: Long,
        autoGenerated: Boolean = false
    ): VideoChapters {
        // Create chapter bookmarks
        val chapterBookmarks = chapters.mapIndexed { index, (position, title) ->
            val bookmarkId = System.currentTimeMillis() + index
            
            // Generate thumbnail
            val thumbnailPath = try {
                val uri = android.net.Uri.parse(videoUri)
                val thumbnail = thumbnailGenerator.getThumbnailAtPosition(uri, position * 1000) // Convert to microseconds
                thumbnail?.let { saveThumbnail(it, "chapter_$bookmarkId", isChapter = true) }
            } catch (e: Exception) {
                null
            }
            
            VideoBookmark(
                id = bookmarkId,
                videoUri = videoUri,
                videoTitle = videoTitle,
                position = position,
                duration = duration,
                title = title,
                thumbnailPath = thumbnailPath,
                isChapter = true
            )
        }
        
        // Save chapter bookmarks
        chapterBookmarks.forEach { saveBookmark(it) }
        
        // Create chapters with end positions
        val videoChapters = chapterBookmarks.mapIndexed { index, bookmark ->
            val endPosition = if (index < chapterBookmarks.size - 1) {
                chapterBookmarks[index + 1].position
            } else {
                duration
            }
            
            VideoChapter(
                bookmark = bookmark,
                chapterIndex = index,
                endPosition = endPosition
            )
        }
        
        val chaptersData = VideoChapters(
            videoUri = videoUri,
            videoTitle = videoTitle,
            chapters = videoChapters,
            autoGenerated = autoGenerated
        )
        
        // Save chapters data
        saveChapters(chaptersData)
        return chaptersData
    }
    
    /**
     * Get chapters for a video
     */
    fun getChaptersForVideo(videoUri: String): Flow<VideoChapters?> {
        return context.dataStore.data.map { preferences ->
            val chaptersJson = preferences[CHAPTERS_KEY] ?: "{}"
            try {
                val allChapters = json.decodeFromString<Map<String, VideoChapters>>(chaptersJson)
                allChapters[videoUri]
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Auto-generate chapters using scene detection
     */
    suspend fun autoGenerateChapters(
        videoUri: String,
        videoTitle: String,
        duration: Long,
        minChapterDuration: Long = 60000 // 1 minute minimum
    ): VideoChapters {
        // Simple time-based chapter generation
        // In a real implementation, this would use scene detection
        val chapterCount = (duration / minChapterDuration).toInt().coerceIn(2, 20)
        val chapterDuration = duration / chapterCount
        
        val chapters = (0 until chapterCount).map { index ->
            val position = index * chapterDuration
            val title = when (index) {
                0 -> "Introduction"
                chapterCount - 1 -> "Conclusion"
                else -> "Chapter ${index + 1}"
            }
            position to title
        }
        
        return createChapters(
            videoUri = videoUri,
            videoTitle = videoTitle,
            chapters = chapters,
            duration = duration,
            autoGenerated = true
        )
    }
    
    /**
     * Get the nearest bookmark/chapter to a position
     */
    suspend fun getNearestBookmark(
        videoUri: String,
        position: Long,
        includeChapters: Boolean = true
    ): VideoBookmark? {
        val bookmarks = getAllBookmarksSync()
            .filter { it.videoUri == videoUri }
            .filter { if (includeChapters) true else !it.isChapter }
        
        return bookmarks.minByOrNull { kotlin.math.abs(it.position - position) }
    }
    
    /**
     * Export bookmarks to JSON
     */
    suspend fun exportBookmarks(videoUri: String? = null): String {
        val bookmarks = if (videoUri != null) {
            getAllBookmarksSync().filter { it.videoUri == videoUri }
        } else {
            getAllBookmarksSync()
        }
        
        return json.encodeToString(bookmarks)
    }
    
    /**
     * Import bookmarks from JSON
     */
    suspend fun importBookmarks(jsonData: String, replaceExisting: Boolean = false) {
        try {
            val importedBookmarks = json.decodeFromString<List<VideoBookmark>>(jsonData)
            
            if (replaceExisting) {
                saveAllBookmarks(importedBookmarks)
            } else {
                val existingBookmarks = getAllBookmarksSync().toMutableList()
                // Avoid duplicate IDs
                val maxId = existingBookmarks.maxOfOrNull { it.id } ?: 0
                val adjustedBookmarks = importedBookmarks.mapIndexed { index, bookmark ->
                    bookmark.copy(id = maxId + index + 1)
                }
                existingBookmarks.addAll(adjustedBookmarks)
                saveAllBookmarks(existingBookmarks)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid bookmark data", e)
        }
    }
    
    private suspend fun saveBookmark(bookmark: VideoBookmark) {
        val bookmarks = getAllBookmarksSync().toMutableList()
        bookmarks.add(bookmark)
        saveAllBookmarks(bookmarks)
    }
    
    private suspend fun saveAllBookmarks(bookmarks: List<VideoBookmark>) {
        context.dataStore.edit { preferences ->
            preferences[BOOKMARKS_KEY] = json.encodeToString(bookmarks)
        }
    }
    
    private suspend fun saveChapters(chapters: VideoChapters) {
        context.dataStore.edit { preferences ->
            val existingJson = preferences[CHAPTERS_KEY] ?: "{}"
            val chaptersMap = try {
                json.decodeFromString<MutableMap<String, VideoChapters>>(existingJson)
            } catch (e: Exception) {
                mutableMapOf()
            }
            
            chaptersMap[chapters.videoUri] = chapters
            preferences[CHAPTERS_KEY] = json.encodeToString(chaptersMap)
        }
    }
    
    private suspend fun getAllBookmarksSync(): List<VideoBookmark> {
        val preferences = context.dataStore.data.first()
        val bookmarksJson = preferences[BOOKMARKS_KEY] ?: "[]"
        return try {
            json.decodeFromString(bookmarksJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveThumbnail(
        bitmap: Bitmap,
        filename: String,
        isChapter: Boolean = false
    ): String {
        val dir = File(context.filesDir, if (isChapter) CHAPTER_THUMBNAIL_DIR else THUMBNAIL_DIR)
        if (!dir.exists()) dir.mkdirs()
        
        val file = File(dir, "$filename.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        
        return file.absolutePath
    }
    
    /**
     * Clean up orphaned thumbnails
     */
    suspend fun cleanupThumbnails() {
        val bookmarks = getAllBookmarksSync()
        val validPaths = bookmarks.mapNotNull { it.thumbnailPath }.toSet()
        
        // Clean bookmark thumbnails
        File(context.filesDir, THUMBNAIL_DIR).listFiles()?.forEach { file ->
            if (file.absolutePath !in validPaths) {
                file.delete()
            }
        }
        
        // Clean chapter thumbnails
        File(context.filesDir, CHAPTER_THUMBNAIL_DIR).listFiles()?.forEach { file ->
            if (file.absolutePath !in validPaths) {
                file.delete()
            }
        }
    }
}