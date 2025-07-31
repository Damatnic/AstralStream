package com.astralplayer.astralstream.data.dao

import androidx.room.*
import com.astralplayer.astralstream.data.entity.PlaybackStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackStateDao {
    
    @Query("SELECT * FROM playback_states WHERE videoId = :videoId")
    suspend fun getPlaybackState(videoId: Long): PlaybackStateEntity?
    
    @Query("SELECT * FROM playback_states WHERE videoId = :videoId")
    fun getPlaybackStateFlow(videoId: Long): Flow<PlaybackStateEntity?>
    
    @Query("SELECT * FROM playback_states ORDER BY lastUpdated DESC")
    fun getAllPlaybackStates(): Flow<List<PlaybackStateEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackState(state: PlaybackStateEntity)
    
    @Update
    suspend fun updatePlaybackState(state: PlaybackStateEntity)
    
    @Delete
    suspend fun deletePlaybackState(state: PlaybackStateEntity)
    
    @Query("DELETE FROM playback_states WHERE videoId = :videoId")
    suspend fun deletePlaybackStateForVideo(videoId: Long)
    
    @Query("DELETE FROM playback_states WHERE lastUpdated < :timestamp")
    suspend fun deleteOldPlaybackStates(timestamp: Long)
    
    @Transaction
    suspend fun insertOrUpdate(state: PlaybackStateEntity) {
        val existing = getPlaybackState(state.videoId)
        if (existing != null) {
            updatePlaybackState(state)
        } else {
            insertPlaybackState(state)
        }
    }
}