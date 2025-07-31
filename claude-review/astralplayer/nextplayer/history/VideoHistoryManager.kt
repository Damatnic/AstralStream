package com.astralplayer.nextplayer.history

import android.content.Context
import android.net.Uri
import androidx.room.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * Comprehensive video history tracking system
 */
class VideoHistoryManager(
    private val context: Context,
    private val database: VideoHistoryDatabase
) {
    
    private val historyDao = database.historyDao()
    private val _recentlyWatched = MutableStateFlow<List<VideoHistoryEntry>>(emptyList())
    val recentlyWatched: StateFlow<List<VideoHistoryEntry>> = _recentlyWatched.asStateFlow()
    
    private val _watchingStats = MutableStateFlow(WatchingStats())
    val watchingStats: StateFlow<WatchingStats> = _watchingStats.asStateFlow()
    
    private val historyScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        // Load recent history
        historyScope.launch {
            historyDao.getRecentHistory(50).collect { entries ->
                _recentlyWatched.value = entries
            }
        }
        
        // Update stats periodically
        historyScope.launch {
            while (true) {
                updateWatchingStats()
                delay(60000) // Update every minute
            }
        }
    }
    
    /**
     * Record video play event
     */
    suspend fun recordPlayEvent(
        videoUri: Uri,
        videoTitle: String? = null,
        videoDurationMs: Long = 0L,
        currentPositionMs: Long = 0L
    ) {
        withContext(Dispatchers.IO) {
            val existing = historyDao.getEntryByUri(videoUri.toString())
            
            if (existing != null) {
                // Update existing entry
                val updated = existing.copy(
                    lastWatchedAt = System.currentTimeMillis(),
                    watchCount = existing.watchCount + 1,
                    lastPositionMs = currentPositionMs,
                    isCompleted = currentPositionMs >= (videoDurationMs * 0.9), // 90% watched = completed
                    totalWatchTimeMs = existing.totalWatchTimeMs // Will be updated by recordProgress
                )
                historyDao.update(updated)
            } else {
                // Create new entry
                val newEntry = VideoHistoryEntry(
                    videoUri = videoUri.toString(),
                    videoTitle = videoTitle ?: extractTitleFromUri(videoUri),
                    videoDurationMs = videoDurationMs,
                    firstWatchedAt = System.currentTimeMillis(),
                    lastWatchedAt = System.currentTimeMillis(),
                    watchCount = 1,
                    lastPositionMs = currentPositionMs,
                    totalWatchTimeMs = 0L,
                    isCompleted = false,
                    isFavorite = false
                )
                historyDao.insert(newEntry)
            }
        }
    }
    
    /**
     * Record video progress during playback
     */
    suspend fun recordProgress(
        videoUri: Uri,
        currentPositionMs: Long,
        videoDurationMs: Long,
        sessionStartTime: Long = System.currentTimeMillis()
    ) {
        withContext(Dispatchers.IO) {
            val entry = historyDao.getEntryByUri(videoUri.toString()) ?: return@withContext
            
            val watchTime = System.currentTimeMillis() - sessionStartTime
            val isCompleted = currentPositionMs >= (videoDurationMs * 0.9)
            
            val updated = entry.copy(
                lastPositionMs = currentPositionMs,
                lastWatchedAt = System.currentTimeMillis(),
                totalWatchTimeMs = entry.totalWatchTimeMs + watchTime,
                isCompleted = isCompleted,
                videoDurationMs = if (entry.videoDurationMs == 0L) videoDurationMs else entry.videoDurationMs
            )
            
            historyDao.update(updated)
        }
    }
    
    /**
     * Record video pause/stop event
     */
    suspend fun recordPauseEvent(
        videoUri: Uri,
        currentPositionMs: Long,
        sessionWatchTimeMs: Long
    ) {
        withContext(Dispatchers.IO) {
            val entry = historyDao.getEntryByUri(videoUri.toString()) ?: return@withContext
            
            val updated = entry.copy(
                lastPositionMs = currentPositionMs,
                totalWatchTimeMs = entry.totalWatchTimeMs + sessionWatchTimeMs,
                lastWatchedAt = System.currentTimeMillis()
            )
            
            historyDao.update(updated)
        }
    }
    
    /**
     * Get video history entry
     */
    suspend fun getHistoryEntry(videoUri: Uri): VideoHistoryEntry? {
        return withContext(Dispatchers.IO) {
            historyDao.getEntryByUri(videoUri.toString())
        }
    }
    
    /**
     * Get recent history with filters
     */
    fun getRecentHistory(
        limit: Int = 50,
        includeCompleted: Boolean = true,
        searchQuery: String? = null
    ): Flow<List<VideoHistoryEntry>> {
        return if (searchQuery.isNullOrBlank()) {
            if (includeCompleted) {
                historyDao.getRecentHistory(limit)
            } else {
                historyDao.getRecentIncompleteHistory(limit)
            }
        } else {
            historyDao.searchHistory(searchQuery, limit)
        }
    }
    
    /**
     * Get most watched videos
     */
    fun getMostWatched(limit: Int = 20): Flow<List<VideoHistoryEntry>> {
        return historyDao.getMostWatched(limit)
    }
    
    /**
     * Get recently completed videos
     */
    fun getRecentlyCompleted(limit: Int = 20): Flow<List<VideoHistoryEntry>> {
        return historyDao.getRecentlyCompleted(limit)
    }
    
    /**
     * Get watching statistics
     */
    suspend fun getWatchingStats(): WatchingStats {
        return withContext(Dispatchers.IO) {
            val totalVideos = historyDao.getTotalVideoCount()
            val completedVideos = historyDao.getCompletedVideoCount()
            val totalWatchTime = historyDao.getTotalWatchTime()
            val averageWatchTime = if (totalVideos > 0) totalWatchTime / totalVideos else 0L
            
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val todayWatchTime = historyDao.getWatchTimeSince(today)
            val todayVideoCount = historyDao.getVideoCountSince(today)
            
            val thisWeek = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val weekWatchTime = historyDao.getWatchTimeSince(thisWeek)
            val weekVideoCount = historyDao.getVideoCountSince(thisWeek)
            
            WatchingStats(
                totalVideosWatched = totalVideos,
                completedVideos = completedVideos,
                totalWatchTimeMs = totalWatchTime,
                averageWatchTimeMs = averageWatchTime,
                todayWatchTimeMs = todayWatchTime,
                todayVideoCount = todayVideoCount,
                weekWatchTimeMs = weekWatchTime,
                weekVideoCount = weekVideoCount,
                completionRate = if (totalVideos > 0) (completedVideos.toFloat() / totalVideos) * 100 else 0f
            )
        }
    }
    
    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(videoUri: Uri) {
        withContext(Dispatchers.IO) {
            val entry = historyDao.getEntryByUri(videoUri.toString()) ?: return@withContext
            val updated = entry.copy(isFavorite = !entry.isFavorite)
            historyDao.update(updated)
        }
    }
    
    /**
     * Get favorite videos
     */
    fun getFavorites(): Flow<List<VideoHistoryEntry>> {
        return historyDao.getFavorites()
    }
    
    /**
     * Remove video from history
     */
    suspend fun removeFromHistory(videoUri: Uri) {
        withContext(Dispatchers.IO) {
            historyDao.deleteByUri(videoUri.toString())
        }
    }
    
    /**
     * Clear all history
     */
    suspend fun clearAllHistory() {
        withContext(Dispatchers.IO) {
            historyDao.deleteAll()
        }
    }
    
    /**
     * Clear old history entries
     */
    suspend fun clearOldHistory(maxAgeMs: Long = 30L * 24 * 60 * 60 * 1000) { // 30 days
        withContext(Dispatchers.IO) {
            val cutoffTime = System.currentTimeMillis() - maxAgeMs
            historyDao.deleteOlderThan(cutoffTime)
        }
    }
    
    /**
     * Get viewing patterns
     */
    suspend fun getViewingPatterns(): ViewingPatterns {
        return withContext(Dispatchers.IO) {
            val hourlyViews = historyDao.getHourlyViewingPattern()
            val dailyViews = historyDao.getDailyViewingPattern()
            val genrePreferences = historyDao.getGenrePreferences() // Based on video categories
            
            ViewingPatterns(
                hourlyPattern = hourlyViews,
                dailyPattern = dailyViews,
                preferredGenres = genrePreferences,
                peakViewingHour = hourlyViews.maxByOrNull { it.second }?.first ?: 20, // Default to 8 PM
                averageSessionLength = historyDao.getAverageSessionLength()
            )
        }
    }
    
    /**
     * Export history data
     */
    suspend fun exportHistory(): String {
        return withContext(Dispatchers.IO) {
            val entries = historyDao.getAllHistory()
            val stats = getWatchingStats()
            
            buildString {
                appendLine("AstralStream Video History Export")
                appendLine("Generated: ${Date()}")
                appendLine()
                appendLine("Statistics:")
                appendLine("Total Videos: ${stats.totalVideosWatched}")
                appendLine("Completed Videos: ${stats.completedVideos}")
                appendLine("Total Watch Time: ${formatDuration(stats.totalWatchTimeMs)}")
                appendLine("Completion Rate: ${String.format("%.1f", stats.completionRate)}%")
                appendLine()
                appendLine("History:")
                
                entries.forEach { entry ->
                    appendLine("Title: ${entry.videoTitle}")
                    appendLine("URI: ${entry.videoUri}")
                    appendLine("Watch Count: ${entry.watchCount}")
                    appendLine("Total Watch Time: ${formatDuration(entry.totalWatchTimeMs)}")
                    appendLine("Last Watched: ${Date(entry.lastWatchedAt)}")
                    appendLine("Completed: ${if (entry.isCompleted) "Yes" else "No"}")
                    appendLine("Favorite: ${if (entry.isFavorite) "Yes" else "No"}")
                    appendLine("---")
                }
            }
        }
    }
    
    private suspend fun updateWatchingStats() {
        _watchingStats.value = getWatchingStats()
    }
    
    private fun extractTitleFromUri(uri: Uri): String {
        return uri.lastPathSegment?.let { segment ->
            // Remove file extension
            val lastDotIndex = segment.lastIndexOf('.')
            if (lastDotIndex > 0) {
                segment.substring(0, lastDotIndex)
            } else {
                segment
            }
        } ?: "Unknown Video"
    }
    
    private fun formatDuration(durationMs: Long): String {
        val hours = durationMs / (1000 * 60 * 60)
        val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (durationMs % (1000 * 60)) / 1000
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
    
    fun cleanup() {
        historyScope.cancel()
    }
}

// Database entities and DAOs
@Entity(tableName = "video_history")
data class VideoHistoryEntry(
    @PrimaryKey val videoUri: String,
    val videoTitle: String,
    val videoDurationMs: Long,
    val firstWatchedAt: Long,
    val lastWatchedAt: Long,
    val watchCount: Int,
    val lastPositionMs: Long,
    val totalWatchTimeMs: Long,
    val isCompleted: Boolean,
    val isFavorite: Boolean
)

@Dao
interface VideoHistoryDao {
    @Query("SELECT * FROM video_history ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<VideoHistoryEntry>>
    
    @Query("SELECT * FROM video_history WHERE isCompleted = 0 ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentIncompleteHistory(limit: Int): Flow<List<VideoHistoryEntry>>
    
    @Query("SELECT * FROM video_history WHERE videoTitle LIKE '%' || :query || '%' ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun searchHistory(query: String, limit: Int): Flow<List<VideoHistoryEntry>>
    
    @Query("SELECT * FROM video_history ORDER BY watchCount DESC LIMIT :limit")
    fun getMostWatched(limit: Int): Flow<List<VideoHistoryEntry>>
    
    @Query("SELECT * FROM video_history WHERE isCompleted = 1 ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentlyCompleted(limit: Int): Flow<List<VideoHistoryEntry>>
    
    @Query("SELECT * FROM video_history WHERE isFavorite = 1 ORDER BY lastWatchedAt DESC")
    fun getFavorites(): Flow<List<VideoHistoryEntry>>
    
    @Query("SELECT * FROM video_history WHERE videoUri = :uri")
    suspend fun getEntryByUri(uri: String): VideoHistoryEntry?
    
    @Query("SELECT * FROM video_history ORDER BY firstWatchedAt ASC")
    suspend fun getAllHistory(): List<VideoHistoryEntry>
    
    @Query("SELECT COUNT(*) FROM video_history")
    suspend fun getTotalVideoCount(): Int
    
    @Query("SELECT COUNT(*) FROM video_history WHERE isCompleted = 1")
    suspend fun getCompletedVideoCount(): Int
    
    @Query("SELECT SUM(totalWatchTimeMs) FROM video_history")
    suspend fun getTotalWatchTime(): Long
    
    @Query("SELECT SUM(totalWatchTimeMs) FROM video_history WHERE lastWatchedAt >= :since")
    suspend fun getWatchTimeSince(since: Long): Long
    
    @Query("SELECT COUNT(*) FROM video_history WHERE lastWatchedAt >= :since")
    suspend fun getVideoCountSince(since: Long): Int
    
    @Query("SELECT AVG(totalWatchTimeMs) FROM video_history WHERE totalWatchTimeMs > 0")
    suspend fun getAverageSessionLength(): Long
    
    // For viewing patterns - simplified queries
    @Query("SELECT strftime('%H', datetime(lastWatchedAt/1000, 'unixepoch')) as hour, COUNT(*) as count FROM video_history GROUP BY hour ORDER BY hour")
    suspend fun getHourlyViewingPattern(): List<Pair<Int, Int>>
    
    @Query("SELECT strftime('%w', datetime(lastWatchedAt/1000, 'unixepoch')) as day, COUNT(*) as count FROM video_history GROUP BY day ORDER BY day")
    suspend fun getDailyViewingPattern(): List<Pair<Int, Int>>
    
    @Query("SELECT 'Unknown' as genre, COUNT(*) as count FROM video_history GROUP BY genre ORDER BY count DESC LIMIT 5")
    suspend fun getGenrePreferences(): List<Pair<String, Int>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VideoHistoryEntry)
    
    @Update
    suspend fun update(entry: VideoHistoryEntry)
    
    @Query("DELETE FROM video_history WHERE videoUri = :uri")
    suspend fun deleteByUri(uri: String)
    
    @Query("DELETE FROM video_history")
    suspend fun deleteAll()
    
    @Query("DELETE FROM video_history WHERE lastWatchedAt < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}

@Database(
    entities = [VideoHistoryEntry::class],
    version = 1,
    exportSchema = false
)
abstract class VideoHistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): VideoHistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: VideoHistoryDatabase? = null
        
        fun getDatabase(context: Context): VideoHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoHistoryDatabase::class.java,
                    "video_history_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Data classes for statistics and patterns
data class WatchingStats(
    val totalVideosWatched: Int = 0,
    val completedVideos: Int = 0,
    val totalWatchTimeMs: Long = 0L,
    val averageWatchTimeMs: Long = 0L,
    val todayWatchTimeMs: Long = 0L,
    val todayVideoCount: Int = 0,
    val weekWatchTimeMs: Long = 0L,
    val weekVideoCount: Int = 0,
    val completionRate: Float = 0f
)

data class ViewingPatterns(
    val hourlyPattern: List<Pair<Int, Int>>, // Hour to count
    val dailyPattern: List<Pair<Int, Int>>,  // Day of week to count
    val preferredGenres: List<Pair<String, Int>>, // Genre to count
    val peakViewingHour: Int,
    val averageSessionLength: Long
)