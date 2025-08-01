package com.astralplayer.community.dao

import androidx.room.*
import com.astralplayer.community.data.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleReportDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: SubtitleReportEntity)
    
    @Update
    suspend fun update(report: SubtitleReportEntity)
    
    @Delete
    suspend fun delete(report: SubtitleReportEntity)
    
    @Query("SELECT * FROM subtitle_reports WHERE id = :id")
    suspend fun getById(id: String): SubtitleReportEntity?
    
    @Query("SELECT * FROM subtitle_reports WHERE subtitleId = :subtitleId ORDER BY createdAt DESC")
    suspend fun getReportsForSubtitle(subtitleId: String): List<SubtitleReportEntity>
    
    @Query("SELECT * FROM subtitle_reports WHERE reporterId = :userId ORDER BY createdAt DESC")
    suspend fun getUserReports(userId: String): List<SubtitleReportEntity>
    
    @Query("SELECT * FROM subtitle_reports WHERE status = :status ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getReportsByStatus(status: ReportStatus, limit: Int = 50): List<SubtitleReportEntity>
    
    @Query("SELECT * FROM subtitle_reports WHERE status = :status ORDER BY createdAt DESC")
    fun observeReportsByStatus(status: ReportStatus): Flow<List<SubtitleReportEntity>>
    
    @Query("SELECT COUNT(*) FROM subtitle_reports WHERE subtitleId = :subtitleId")
    suspend fun getReportCount(subtitleId: String): Int
    
    @Query("SELECT COUNT(*) FROM subtitle_reports WHERE subtitleId = :subtitleId AND status = :status")
    suspend fun getReportCountByStatus(subtitleId: String, status: ReportStatus): Int
    
    @Query("SELECT COUNT(*) FROM subtitle_reports WHERE reporterId = :userId")
    suspend fun getUserReportCount(userId: String): Int
    
    @Query("""
        UPDATE subtitle_reports 
        SET status = :status, resolvedAt = :resolvedAt, resolvedBy = :resolvedBy, resolution = :resolution 
        WHERE id = :id
    """)
    suspend fun resolveReport(
        id: String,
        status: ReportStatus,
        resolvedAt: Long = System.currentTimeMillis(),
        resolvedBy: String,
        resolution: String
    )
    
    @Query("SELECT * FROM subtitle_reports WHERE severity = :severity AND status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getPendingReportsBySeverity(severity: ReportSeverity): List<SubtitleReportEntity>
    
    @Query("""
        SELECT reason, COUNT(*) as count FROM subtitle_reports 
        WHERE subtitleId = :subtitleId 
        GROUP BY reason 
        ORDER BY count DESC
    """)
    suspend fun getReportReasonDistribution(subtitleId: String): List<ReasonCount>
    
    @Query("""
        SELECT subtitleId, COUNT(*) as reportCount FROM subtitle_reports 
        WHERE status = 'PENDING' 
        GROUP BY subtitleId 
        HAVING reportCount >= :threshold 
        ORDER BY reportCount DESC
    """)
    suspend fun getHighlyReportedSubtitles(threshold: Int = 3): List<ReportedSubtitle>
    
    @Query("DELETE FROM subtitle_reports WHERE status = 'RESOLVED' AND resolvedAt < :timestamp")
    suspend fun deleteOldResolvedReports(timestamp: Long)
    
    @Query("SELECT EXISTS(SELECT 1 FROM subtitle_reports WHERE subtitleId = :subtitleId AND reporterId = :userId)")
    suspend fun hasUserReported(subtitleId: String, userId: String): Boolean
    
    @Transaction
    suspend fun bulkResolve(reportIds: List<String>, status: ReportStatus, resolvedBy: String, resolution: String) {
        reportIds.forEach { id ->
            resolveReport(id, status, System.currentTimeMillis(), resolvedBy, resolution)
        }
    }
}

data class ReasonCount(
    val reason: SubtitleReportReason,
    val count: Int
)

data class ReportedSubtitle(
    val subtitleId: String,
    val reportCount: Int
)