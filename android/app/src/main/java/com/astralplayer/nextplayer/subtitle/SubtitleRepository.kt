package com.astralplayer.nextplayer.subtitle

import android.content.Context
import androidx.room.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing generated subtitles
 */
@Singleton
class SubtitleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subtitleDao: SubtitleDao
) {
    
    private val subtitleDir = File(context.filesDir, "subtitles").apply {
        if (!exists()) mkdirs()
    }
    
    /**
     * Save generated subtitles
     */
    suspend fun saveSubtitles(
        mediaId: String,
        subtitles: List<AdvancedAISubtitleGenerator.SubtitleEntry>,
        language: String,
        format: AdvancedAISubtitleGenerator.SubtitleFormat
    ) {
        // Save to database
        val subtitleEntity = SubtitleEntity(
            mediaId = mediaId,
            language = language,
            format = format.name,
            generatedAt = System.currentTimeMillis(),
            confidence = 0.9f,
            filePath = ""
        )
        
        val id = subtitleDao.insertSubtitle(subtitleEntity)
        
        // Save to file
        val file = File(subtitleDir, "${mediaId}_${language}_$id.${format.name.lowercase()}")
        val content = when (format) {
            AdvancedAISubtitleGenerator.SubtitleFormat.SRT -> generateSRT(subtitles)
            AdvancedAISubtitleGenerator.SubtitleFormat.VTT -> generateVTT(subtitles)
            else -> generateSRT(subtitles)
        }
        
        file.writeText(content)
        
        // Update entity with file path
        subtitleDao.updateFilePath(id, file.absolutePath)
    }
    
    /**
     * Get subtitles for a media item
     */
    suspend fun getSubtitles(mediaId: String): List<SubtitleInfo> {
        return subtitleDao.getSubtitlesForMedia(mediaId).map { entity ->
            SubtitleInfo(
                id = entity.id,
                mediaId = entity.mediaId,
                language = entity.language,
                format = entity.format,
                filePath = entity.filePath,
                generatedAt = entity.generatedAt,
                confidence = entity.confidence
            )
        }
    }
    
    /**
     * Check if offline subtitles exist
     */
    fun hasOfflineSubtitles(mediaId: String): Boolean {
        return subtitleDao.hasSubtitles(mediaId)
    }
    
    /**
     * Delete subtitles
     */
    suspend fun deleteSubtitles(subtitleId: Long) {
        val subtitle = subtitleDao.getSubtitleById(subtitleId)
        subtitle?.let {
            // Delete file
            File(it.filePath).delete()
            // Delete from database
            subtitleDao.deleteSubtitle(it)
        }
    }
    
    /**
     * Generate SRT format
     */
    private fun generateSRT(subtitles: List<AdvancedAISubtitleGenerator.SubtitleEntry>): String {
        return subtitles.mapIndexed { index, subtitle ->
            val startTime = formatSRTTime(subtitle.startTimeMs)
            val endTime = formatSRTTime(subtitle.endTimeMs)
            "${index + 1}\n$startTime --> $endTime\n${subtitle.text}\n"
        }.joinToString("\n")
    }
    
    /**
     * Generate WebVTT format
     */
    private fun generateVTT(subtitles: List<AdvancedAISubtitleGenerator.SubtitleEntry>): String {
        val header = "WEBVTT\n\n"
        val content = subtitles.map { subtitle ->
            val startTime = formatVTTTime(subtitle.startTimeMs)
            val endTime = formatVTTTime(subtitle.endTimeMs)
            "$startTime --> $endTime\n${subtitle.text}\n"
        }.joinToString("\n")
        
        return header + content
    }
    
    private fun formatSRTTime(timeMs: Long): String {
        val hours = timeMs / 3600000
        val minutes = (timeMs % 3600000) / 60000
        val seconds = (timeMs % 60000) / 1000
        val millis = timeMs % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
    
    private fun formatVTTTime(timeMs: Long): String {
        val hours = timeMs / 3600000
        val minutes = (timeMs % 3600000) / 60000
        val seconds = (timeMs % 60000) / 1000
        val millis = timeMs % 1000
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
    
    data class SubtitleInfo(
        val id: Long,
        val mediaId: String,
        val language: String,
        val format: String,
        val filePath: String,
        val generatedAt: Long,
        val confidence: Float
    )
}

// Room entities and DAO
@Entity(tableName = "subtitles")
data class SubtitleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: String,
    val language: String,
    val format: String,
    val filePath: String,
    val generatedAt: Long,
    val confidence: Float
)

@Dao
interface SubtitleDao {
    @Insert
    suspend fun insertSubtitle(subtitle: SubtitleEntity): Long
    
    @Query("SELECT * FROM subtitles WHERE mediaId = :mediaId ORDER BY generatedAt DESC")
    suspend fun getSubtitlesForMedia(mediaId: String): List<SubtitleEntity>
    
    @Query("SELECT * FROM subtitles WHERE id = :id")
    suspend fun getSubtitleById(id: Long): SubtitleEntity?
    
    @Query("SELECT COUNT(*) > 0 FROM subtitles WHERE mediaId = :mediaId")
    fun hasSubtitles(mediaId: String): Boolean
    
    @Delete
    suspend fun deleteSubtitle(subtitle: SubtitleEntity)
    
    @Query("UPDATE subtitles SET filePath = :filePath WHERE id = :id")
    suspend fun updateFilePath(id: Long, filePath: String)
}