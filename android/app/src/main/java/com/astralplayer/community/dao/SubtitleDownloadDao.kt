package com.astralplayer.community.dao

import androidx.room.*
import com.astralplayer.community.data.SubtitleDownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleDownloadDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: SubtitleDownloadEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(downloads: List<SubtitleDownloadEntity>)
    
    @Query("SELECT COUNT(*) FROM subtitle_downloads WHERE subtitleId = :subtitleId")
    suspend fun getDownloadCount(subtitleId: String): Int
    
    @Query("SELECT COUNT(*) FROM subtitle_downloads WHERE userId = :userId")
    suspend fun getUserDownloadCount(userId: String): Int
    
    @Query("SELECT * FROM subtitle_downloads WHERE userId = :userId ORDER BY downloadedAt DESC LIMIT :limit")
    suspend fun getUserRecentDownloads(userId: String, limit: Int = 50): List<SubtitleDownloadEntity>
    
    @Query("SELECT * FROM subtitle_downloads WHERE userId = :userId ORDER BY downloadedAt DESC")
    fun observeUserDownloads(userId: String): Flow<List<SubtitleDownloadEntity>>
    
    @Query("""
        SELECT subtitleId, COUNT(*) as count FROM subtitle_downloads 
        GROUP BY subtitleId 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getMostDownloadedSubtitleIds(limit: Int = 50): List<DownloadCount>
    
    @Query("""
        SELECT subtitleId, COUNT(*) as count FROM subtitle_downloads 
        WHERE downloadedAt > :since 
        GROUP BY subtitleId 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getTrendingDownloads(since: Long, limit: Int = 20): List<DownloadCount>
    
    @Query("""
        SELECT DATE(downloadedAt / 1000, 'unixepoch') as date, COUNT(*) as count 
        FROM subtitle_downloads 
        WHERE downloadedAt > :since 
        GROUP BY date 
        ORDER BY date DESC
    """)
    suspend fun getDailyDownloadStats(since: Long): List<DailyDownloadStat>
    
    @Query("SELECT COUNT(DISTINCT subtitleId) FROM subtitle_downloads WHERE userId = :userId")
    suspend fun getUniqueSubtitleDownloadCount(userId: String): Int
    
    @Query("DELETE FROM subtitle_downloads WHERE downloadedAt < :timestamp")
    suspend fun deleteOldDownloadRecords(timestamp: Long)
    
    @Query("""
        SELECT COUNT(*) FROM subtitle_downloads 
        WHERE subtitleId = :subtitleId AND downloadedAt > :since
    """)
    suspend fun getRecentDownloadCount(subtitleId: String, since: Long): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM subtitle_downloads WHERE subtitleId = :subtitleId AND userId = :userId)")
    suspend fun hasUserDownloaded(subtitleId: String, userId: String): Boolean
    
    @Query("""
        SELECT userAgent, COUNT(*) as count FROM subtitle_downloads 
        GROUP BY userAgent 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getTopUserAgents(limit: Int = 10): List<UserAgentCount>
}

data class DownloadCount(
    val subtitleId: String,
    val count: Int
)

data class DailyDownloadStat(
    val date: String,
    val count: Int
)

data class UserAgentCount(
    val userAgent: String,
    val count: Int
)