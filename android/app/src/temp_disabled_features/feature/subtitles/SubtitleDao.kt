package com.astralplayer.nextplayer.feature.subtitles

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for subtitle operations
 */
@Dao
interface SubtitleDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitle(subtitle: SubtitleEntity)
    
    @Query("SELECT * FROM subtitles")
    fun getAllSubtitles(): Flow<List<SubtitleEntity>>
    
    @Query("SELECT * FROM subtitles WHERE id = :id")
    suspend fun getSubtitleById(id: String): SubtitleEntity?
    
    @Query("SELECT * FROM subtitles WHERE mediaId = :mediaId AND language = :language LIMIT 1")
    suspend fun getSubtitleForMediaAndLanguage(mediaId: String, language: String): SubtitleEntity?
    
    @Query("SELECT * FROM subtitles WHERE mediaId = :mediaId")
    fun getAllSubtitlesForMedia(mediaId: String): Flow<List<SubtitleEntity>>
    
    @Query("DELETE FROM subtitles WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM subtitles WHERE mediaId = :mediaId")
    suspend fun deleteAllForMedia(mediaId: String)
}