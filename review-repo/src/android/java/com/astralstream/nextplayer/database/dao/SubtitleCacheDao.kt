package com.astralstream.nextplayer.database.dao

import androidx.room.*
import com.astralstream.nextplayer.database.entities.CachedSubtitleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleCacheDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subtitle: CachedSubtitleEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subtitles: List<CachedSubtitleEntity>)
    
    @Update
    suspend fun update(subtitle: CachedSubtitleEntity)
    
    @Delete
    suspend fun delete(subtitle: CachedSubtitleEntity)
    
    @Query("DELETE FROM cached_subtitles")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM cached_subtitles WHERE cacheKey = :key LIMIT 1")
    suspend fun getByKey(key: String): CachedSubtitleEntity?
    
    @Query("SELECT * FROM cached_subtitles WHERE videoUri = :videoUri")
    suspend fun getByVideoUri(videoUri: String): List<CachedSubtitleEntity>
    
    @Query("SELECT language FROM cached_subtitles WHERE videoUri = :videoUri")
    suspend fun getLanguagesForVideo(videoUri: String): List<String>
    
    @Query("SELECT EXISTS(SELECT 1 FROM cached_subtitles WHERE cacheKey = :key)")
    suspend fun exists(key: String): Boolean
    
    @Query("SELECT COUNT(*) FROM cached_subtitles")
    suspend fun getCount(): Int
    
    @Query("SELECT SUM(fileSize) FROM cached_subtitles")
    suspend fun getTotalSize(): Long
    
    @Query("UPDATE cached_subtitles SET lastAccessed = :time WHERE cacheKey = :key")
    suspend fun updateAccessTime(key: String, time: Long)
    
    @Query("UPDATE cached_subtitles SET accessCount = accessCount + 1 WHERE cacheKey = :key")
    suspend fun incrementAccessCount(key: String)
    
    @Query("SELECT * FROM cached_subtitles ORDER BY lastAccessed ASC")
    suspend fun getLeastRecentlyUsed(): List<CachedSubtitleEntity>
    
    @Query("SELECT * FROM cached_subtitles WHERE timestamp < :expiredTime")
    suspend fun getExpired(expiredTime: Long): List<CachedSubtitleEntity>
    
    @Query("SELECT * FROM cached_subtitles ORDER BY accessCount DESC LIMIT :limit")
    suspend fun getTopAccessed(limit: Int): List<CachedSubtitleEntity>
    
    @Query("SELECT MIN(timestamp) FROM cached_subtitles")
    suspend fun getOldestTimestamp(): Long?
    
    @Query("SELECT SUM(accessCount) FROM cached_subtitles")
    suspend fun getTotalAccessCount(): Int
    
    @Query("SELECT language, COUNT(*) as count FROM cached_subtitles GROUP BY language")
    suspend fun getCacheByLanguage(): Map<String, Int>
    
    @Query("SELECT sourceType, COUNT(*) as count FROM cached_subtitles GROUP BY sourceType")
    suspend fun getCacheBySource(): Map<String, Int>
    
    @Query("SELECT * FROM cached_subtitles WHERE isUserContributed = 1 ORDER BY timestamp DESC")
    fun getUserContributedSubtitles(): Flow<List<CachedSubtitleEntity>>
    
    @Query("SELECT * FROM cached_subtitles WHERE contributorId = :userId")
    suspend fun getByContributor(userId: String): List<CachedSubtitleEntity>
    
    @Query("DELETE FROM cached_subtitles WHERE timestamp < :timestamp AND accessCount < :minAccess")
    suspend fun cleanupOldUnusedCache(timestamp: Long, minAccess: Int)
}