package com.astralplayer.astralstream.data.dao

import androidx.room.*
import com.astralplayer.astralstream.data.entity.SubtitleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleDao {
    
    @Query("SELECT * FROM subtitles WHERE videoId = :videoId ORDER BY language ASC")
    fun getSubtitlesForVideo(videoId: Long): Flow<List<SubtitleEntity>>
    
    @Query("SELECT * FROM subtitles WHERE id = :subtitleId")
    suspend fun getSubtitleById(subtitleId: Long): SubtitleEntity?
    
    @Query("SELECT * FROM subtitles WHERE videoId = :videoId AND languageCode = :languageCode LIMIT 1")
    suspend fun getSubtitleByLanguage(videoId: Long, languageCode: String): SubtitleEntity?
    
    @Query("SELECT * FROM subtitles WHERE isAIGenerated = 1 ORDER BY addedTime DESC")
    fun getAIGeneratedSubtitles(): Flow<List<SubtitleEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitle(subtitle: SubtitleEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitles(subtitles: List<SubtitleEntity>)
    
    @Update
    suspend fun updateSubtitle(subtitle: SubtitleEntity)
    
    @Delete
    suspend fun deleteSubtitle(subtitle: SubtitleEntity)
    
    @Query("DELETE FROM subtitles WHERE id = :subtitleId")
    suspend fun deleteSubtitleById(subtitleId: Long)
    
    @Query("DELETE FROM subtitles WHERE videoId = :videoId")
    suspend fun deleteSubtitlesForVideo(videoId: Long)
    
    @Query("DELETE FROM subtitles WHERE videoId = :videoId AND isAIGenerated = 1")
    suspend fun deleteAISubtitlesForVideo(videoId: Long)
}