package com.astralplayer.astralstream.data.dao

import androidx.room.*
import com.astralplayer.astralstream.data.entity.CachedSubtitleEntity
import com.astralplayer.astralstream.data.entity.SubtitleSource
import com.astralplayer.astralstream.data.entity.SubtitleFormat
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleCacheDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subtitle: CachedSubtitleEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subtitles: List<CachedSubtitleEntity>)
    
    @Update
    suspend fun update(subtitle: CachedSubtitleEntity)
    
    @Delete
    suspend fun delete(subtitle: CachedSubtitleEntity)
    
    @Query("DELETE FROM cached_subtitles WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM cached_subtitles")
    suspend fun deleteAll()
    
    // Primary retrieval methods
    @Query("""
        SELECT * FROM cached_subtitles 
        WHERE videoId = :videoId AND language = :language 
        ORDER BY createdTime DESC 
        LIMIT 1
    """)
    suspend fun getSubtitle(videoId: String, language: String): CachedSubtitleEntity?
    
    @Query("""
        SELECT * FROM cached_subtitles 
        WHERE videoId = :videoId AND languageCode = :languageCode 
        ORDER BY createdTime DESC 
        LIMIT 1
    """)
    suspend fun getSubtitleByCode(videoId: String, languageCode: String): CachedSubtitleEntity?
    
    @Query("SELECT * FROM cached_subtitles WHERE id = :id")
    suspend fun getById(id: String): CachedSubtitleEntity?
    
    // Access tracking
    @Query("""
        UPDATE cached_subtitles 
        SET lastAccessTime = :timestamp, accessCount = accessCount + 1 
        WHERE id = :id
    """)
    suspend fun updateAccess(id: String, timestamp: Long)
    
    @Query("UPDATE cached_subtitles SET lastAccessTime = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: String, timestamp: Long)
    
    // Language queries
    @Query("SELECT DISTINCT language FROM cached_subtitles WHERE videoId = :videoId")
    fun getAvailableLanguages(videoId: String): Flow<List<String>>
    
    @Query("SELECT DISTINCT languageCode FROM cached_subtitles WHERE videoId = :videoId")
    fun getAvailableLanguageCodes(videoId: String): Flow<List<String>>
    
    @Query("""
        SELECT * FROM cached_subtitles 
        WHERE videoId = :videoId 
        ORDER BY language ASC
    """)
    fun getSubtitlesForVideo(videoId: String): Flow<List<CachedSubtitleEntity>>
    
    // Cache management queries
    @Query("SELECT SUM(fileSize) FROM cached_subtitles")
    suspend fun getTotalCacheSize(): Long
    
    @Query("SELECT COUNT(*) FROM cached_subtitles")
    suspend fun getCacheCount(): Int
    
    @Query("""
        SELECT * FROM cached_subtitles 
        ORDER BY lastAccessTime ASC 
        LIMIT :limit
    """)
    suspend fun getLeastRecentlyUsed(limit: Int): List<CachedSubtitleEntity>
    
    @Query("""
        SELECT * FROM cached_subtitles 
        WHERE lastAccessTime < :cutoffTime 
        ORDER BY lastAccessTime ASC
    """)
    suspend fun getOldEntries(cutoffTime: Long): List<CachedSubtitleEntity>
    
    @Query("DELETE FROM cached_subtitles WHERE createdTime < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    @Query("DELETE FROM cached_subtitles WHERE lastAccessTime < :timestamp")
    suspend fun deleteNotAccessedSince(timestamp: Long): Int
    
    @Query("""
        DELETE FROM cached_subtitles 
        WHERE id IN (
            SELECT id FROM cached_subtitles 
            ORDER BY lastAccessTime ASC 
            LIMIT :count
        )
    """)
    suspend fun deleteLeastRecentlyUsed(count: Int): Int
    
    // Search and filtering
    @Query("""
        SELECT * FROM cached_subtitles 
        WHERE videoTitle LIKE '%' || :query || '%' 
        OR language LIKE '%' || :query || '%'
        ORDER BY lastAccessTime DESC
    """)
    suspend fun search(query: String): List<CachedSubtitleEntity>
    
    @Query("SELECT * FROM cached_subtitles WHERE source = :source ORDER BY createdTime DESC")
    suspend fun getBySource(source: SubtitleSource): List<CachedSubtitleEntity>
    
    @Query("SELECT * FROM cached_subtitles WHERE format = :format ORDER BY createdTime DESC")
    suspend fun getByFormat(format: SubtitleFormat): List<CachedSubtitleEntity>
    
    @Query("""
        SELECT * FROM cached_subtitles 
        WHERE confidence >= :minConfidence 
        ORDER BY confidence DESC
    """)
    suspend fun getByMinimumConfidence(minConfidence: Float): List<CachedSubtitleEntity>
    
    // Statistics queries
    @Query("SELECT AVG(confidence) FROM cached_subtitles WHERE source = :source")
    suspend fun getAverageConfidenceForSource(source: SubtitleSource): Float?
    
    @Query("SELECT SUM(fileSize) FROM cached_subtitles WHERE source = :source")
    suspend fun getTotalSizeForSource(source: SubtitleSource): Long
    
    @Query("""
        SELECT language, COUNT(*) as count 
        FROM cached_subtitles 
        GROUP BY language 
        ORDER BY count DESC
    """)
    suspend fun getLanguageStatistics(): List<LanguageStats>
    
    @Query("""
        SELECT source, COUNT(*) as count, AVG(confidence) as avgConfidence 
        FROM cached_subtitles 
        GROUP BY source
    """)
    suspend fun getSourceStatistics(): List<SourceStats>
    
    // Maintenance queries
    @Query("""
        SELECT * FROM cached_subtitles 
        WHERE accessCount = 0 AND createdTime < :cutoffTime
    """)
    suspend fun getUnusedEntries(cutoffTime: Long): List<CachedSubtitleEntity>
    
    @Query("SELECT * FROM cached_subtitles WHERE checksum = :checksum")
    suspend fun findDuplicates(checksum: String): List<CachedSubtitleEntity>
    
    @Query("""
        UPDATE cached_subtitles 
        SET qualityScore = :score 
        WHERE id = :id
    """)
    suspend fun updateQualityScore(id: String, score: Float)
    
    // Export/Import helpers
    @Query("SELECT * FROM cached_subtitles ORDER BY createdTime DESC")
    suspend fun getAllForExport(): List<CachedSubtitleEntity>
    
    @Query("""
        SELECT * FROM cached_subtitles 
        WHERE createdTime BETWEEN :startTime AND :endTime 
        ORDER BY createdTime DESC
    """)
    suspend fun getByDateRange(startTime: Long, endTime: Long): List<CachedSubtitleEntity>
}

// Data classes for statistics
data class LanguageStats(
    val language: String,
    val count: Int
)

data class SourceStats(
    val source: SubtitleSource,
    val count: Int,
    val avgConfidence: Float
)