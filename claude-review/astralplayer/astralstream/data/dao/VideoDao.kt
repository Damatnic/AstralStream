package com.astralplayer.astralstream.data.dao

import androidx.room.*
import com.astralplayer.astralstream.data.entity.VideoEntity
import com.astralplayer.astralstream.data.model.VideoSource
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    
    @Query("SELECT * FROM videos ORDER BY addedTime DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>
    
    @Query("SELECT * FROM videos WHERE id = :videoId")
    suspend fun getVideoById(videoId: Long): VideoEntity?
    
    @Query("SELECT * FROM videos WHERE path = :path LIMIT 1")
    suspend fun getVideoByPath(path: String): VideoEntity?
    
    @Query("SELECT * FROM videos WHERE source = :source ORDER BY addedTime DESC")
    fun getVideosBySource(source: VideoSource): Flow<List<VideoEntity>>
    
    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY addedTime DESC")
    fun getFavoriteVideos(): Flow<List<VideoEntity>>
    
    @Query("SELECT * FROM videos WHERE lastPlayedTime IS NOT NULL ORDER BY lastPlayedTime DESC LIMIT :limit")
    fun getRecentlyPlayedVideos(limit: Int = 20): Flow<List<VideoEntity>>
    
    @Query("SELECT * FROM videos ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedVideos(limit: Int = 20): Flow<List<VideoEntity>>
    
    @Query("SELECT * FROM videos WHERE folderId = :folderId ORDER BY title ASC")
    fun getVideosInFolder(folderId: Long): Flow<List<VideoEntity>>
    
    @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchVideos(query: String): Flow<List<VideoEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)
    
    @Update
    suspend fun updateVideo(video: VideoEntity)
    
    @Delete
    suspend fun deleteVideo(video: VideoEntity)
    
    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideoById(videoId: Long)
    
    @Query("DELETE FROM videos WHERE path = :path")
    suspend fun deleteVideoByPath(path: String)
    
    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :videoId")
    suspend fun updateFavoriteStatus(videoId: Long, isFavorite: Boolean)
    
    @Query("UPDATE videos SET playCount = playCount + 1, lastPlayedTime = :timestamp WHERE id = :videoId")
    suspend fun incrementPlayCount(videoId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE videos SET thumbnailPath = :thumbnailPath WHERE id = :videoId")
    suspend fun updateThumbnail(videoId: Long, thumbnailPath: String)
    
    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideoCount(): Int
    
    @Query("SELECT SUM(size) FROM videos")
    suspend fun getTotalSize(): Long?
    
    @Query("SELECT SUM(duration) FROM videos")
    suspend fun getTotalDuration(): Long?
}