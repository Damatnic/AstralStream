package com.astralplayer.nextplayer.data.dao

import androidx.room.*
import com.astralplayer.nextplayer.data.entities.PlaybackHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insertHistory(history: PlaybackHistory)
    
    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<PlaybackHistory>>
}