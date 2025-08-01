package com.astralplayer.community.dao

import androidx.room.*
import com.astralplayer.community.data.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunitySubtitleDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subtitle: CommunitySubtitleEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subtitles: List<CommunitySubtitleEntity>)
    
    @Update
    suspend fun update(subtitle: CommunitySubtitleEntity)
    
    @Delete
    suspend fun delete(subtitle: CommunitySubtitleEntity)
    
    @Query("SELECT * FROM community_subtitles WHERE id = :id")
    suspend fun getById(id: String): CommunitySubtitleEntity?
    
    @Query("""
        SELECT * FROM community_subtitles 
        WHERE videoHash = :videoHash 
        AND (:language IS NULL OR language = :language) 
        ORDER BY upvotes - downvotes DESC, confidence DESC
    """)
    suspend fun getSubtitlesForVideo(videoHash: String, language: String? = null): List<CommunitySubtitleEntity>
    
    @Query("""
        SELECT * FROM community_subtitles 
        WHERE videoHash = :videoHash 
        ORDER BY upvotes - downvotes DESC, confidence DESC
    """)
    fun observeSubtitlesForVideo(videoHash: String): Flow<List<CommunitySubtitleEntity>>
    
    @Query("""
        SELECT * FROM community_subtitles 
        WHERE contributorId = :userId 
        ORDER BY createdAt DESC
    """)
    suspend fun getUserContributions(userId: String): List<CommunitySubtitleEntity>
    
    @Query("""
        SELECT * FROM community_subtitles 
        WHERE contributorId = :userId 
        ORDER BY createdAt DESC
    """)
    fun observeUserContributions(userId: String): Flow<List<CommunitySubtitleEntity>>
    
    @Query("""
        SELECT * FROM community_subtitles 
        WHERE verificationStatus = :status 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """)
    suspend fun getByVerificationStatus(status: SubtitleVerificationStatus, limit: Int = 50): List<CommunitySubtitleEntity>
    
    @Query("""
        SELECT * FROM community_subtitles 
        WHERE upvotes - downvotes >= :minNetVotes 
        AND verificationStatus != 'REJECTED' AND verificationStatus != 'HIDDEN' 
        ORDER BY upvotes - downvotes DESC 
        LIMIT :limit
    """)
    suspend fun getTopRatedSubtitles(minNetVotes: Int = 10, limit: Int = 50): List<CommunitySubtitleEntity>
    
    @Query("""
        SELECT * FROM community_subtitles 
        WHERE language = :language 
        ORDER BY downloadCount DESC 
        LIMIT :limit
    """)
    suspend fun getMostDownloadedByLanguage(language: String, limit: Int = 20): List<CommunitySubtitleEntity>
    
    @Query("""
        SELECT * FROM community_subtitles 
        WHERE createdAt > :timestamp 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """)
    fun observeRecentSubtitles(timestamp: Long, limit: Int = 50): Flow<List<CommunitySubtitleEntity>>
    
    @Query("UPDATE community_subtitles SET upvotes = upvotes + 1 WHERE id = :id")
    suspend fun incrementUpvotes(id: String)
    
    @Query("UPDATE community_subtitles SET downvotes = downvotes + 1 WHERE id = :id")
    suspend fun incrementDownvotes(id: String)
    
    @Query("UPDATE community_subtitles SET downloadCount = downloadCount + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: String)
    
    @Query("UPDATE community_subtitles SET reportCount = reportCount + 1 WHERE id = :id")
    suspend fun incrementReportCount(id: String)
    
    @Query("""
        UPDATE community_subtitles 
        SET verificationStatus = :status, verifiedAt = :verifiedAt, verifiedBy = :verifiedBy 
        WHERE id = :id
    """)
    suspend fun updateVerificationStatus(
        id: String,
        status: SubtitleVerificationStatus,
        verifiedAt: Long? = System.currentTimeMillis(),
        verifiedBy: String? = null
    )
    
    @Query("""
        SELECT COUNT(DISTINCT videoHash) FROM community_subtitles 
        WHERE contributorId = :userId
    """)
    suspend fun getUniqueVideoContributionCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM community_subtitles WHERE contributorId = :userId")
    suspend fun getUserContributionCount(userId: String): Int
    
    @Query("SELECT SUM(upvotes) FROM community_subtitles WHERE contributorId = :userId")
    suspend fun getUserTotalUpvotes(userId: String): Int?
    
    @Query("SELECT SUM(downloadCount) FROM community_subtitles WHERE contributorId = :userId")
    suspend fun getUserTotalDownloads(userId: String): Int?
    
    @Query("""
        SELECT language, COUNT(*) as count FROM community_subtitles 
        GROUP BY language 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getTopLanguages(limit: Int = 10): List<LanguageCount>
    
    @Query("""
        SELECT * FROM community_subtitles 
        WHERE isOfficial = 1 AND videoHash = :videoHash 
        ORDER BY createdAt DESC 
        LIMIT 1
    """)
    suspend fun getOfficialSubtitle(videoHash: String): CommunitySubtitleEntity?
    
    @Query("""
        SELECT * FROM community_subtitles 
        WHERE confidence >= :minConfidence 
        ORDER BY confidence DESC 
        LIMIT :limit
    """)
    suspend fun getHighConfidenceSubtitles(minConfidence: Float = 0.9f, limit: Int = 50): List<CommunitySubtitleEntity>
    
    @Query("""
        SELECT * FROM community_subtitles 
        WHERE reportCount > :threshold 
        ORDER BY reportCount DESC
    """)
    suspend fun getFlaggedSubtitles(threshold: Int = 3): List<CommunitySubtitleEntity>
    
    @Query("DELETE FROM community_subtitles WHERE verificationStatus = 'REJECTED' AND createdAt < :timestamp")
    suspend fun deleteOldRejectedSubtitles(timestamp: Long)
    
    @Transaction
    suspend fun replaceSubtitle(oldId: String, newSubtitle: CommunitySubtitleEntity) {
        delete(getById(oldId) ?: return)
        insert(newSubtitle)
    }
}

data class LanguageCount(
    val language: String,
    val count: Int
)