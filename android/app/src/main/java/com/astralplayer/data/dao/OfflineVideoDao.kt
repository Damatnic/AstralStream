package com.astralplayer.data.dao

import androidx.room.*
import com.astralplayer.data.entity.OfflineVideoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for offline video operations
 */
@Dao
interface OfflineVideoDao {
    
    @Query("SELECT * FROM offline_videos ORDER BY downloadedAt DESC")
    fun getAllOfflineVideos(): Flow<List<OfflineVideoEntity>>
    
    @Query("SELECT * FROM offline_videos WHERE videoId = :videoId")
    suspend fun getOfflineVideo(videoId: String): OfflineVideoEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: OfflineVideoEntity)
    
    @Query("UPDATE offline_videos SET watchProgress = :progress, lastPlayedAt = :lastPlayedAt WHERE videoId = :videoId")
    suspend fun updateWatchProgress(videoId: String, progress: Long, lastPlayedAt: Long)
    
    @Query("DELETE FROM offline_videos WHERE videoId = :videoId")
    suspend fun delete(videoId: String)
    
    @Query("SELECT SUM(fileSize) FROM offline_videos")
    suspend fun getTotalStorageUsed(): Long?
    
    @Query("SELECT * FROM offline_videos WHERE title LIKE :query ORDER BY downloadedAt DESC")
    fun searchVideos(query: String): Flow<List<OfflineVideoEntity>>
}