package com.astralplayer.nextplayer.data.dao

import androidx.room.*
import com.astralplayer.nextplayer.data.entities.VideoMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM video_metadata ORDER BY lastPlayed DESC")
    fun getAllVideos(): Flow<List<VideoMetadata>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoMetadata)
    
    @Update
    suspend fun updateVideo(video: VideoMetadata)
    
    @Query("UPDATE video_metadata SET playbackPosition = :position WHERE videoPath = :path")
    suspend fun updatePlaybackPosition(path: String, position: Long)
}