package com.astralplayer.community.dao

import androidx.room.*
import com.astralplayer.community.data.SubtitleVoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleVoteDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vote: SubtitleVoteEntity)
    
    @Update
    suspend fun update(vote: SubtitleVoteEntity)
    
    @Delete
    suspend fun delete(vote: SubtitleVoteEntity)
    
    @Query("SELECT * FROM subtitle_votes WHERE subtitleId = :subtitleId AND userId = :userId LIMIT 1")
    suspend fun getUserVote(subtitleId: String, userId: String): SubtitleVoteEntity?
    
    @Query("SELECT * FROM subtitle_votes WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getUserVotes(userId: String): List<SubtitleVoteEntity>
    
    @Query("SELECT * FROM subtitle_votes WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeUserVotes(userId: String): Flow<List<SubtitleVoteEntity>>
    
    @Query("SELECT COUNT(*) FROM subtitle_votes WHERE subtitleId = :subtitleId AND isUpvote = 1")
    suspend fun getUpvoteCount(subtitleId: String): Int
    
    @Query("SELECT COUNT(*) FROM subtitle_votes WHERE subtitleId = :subtitleId AND isUpvote = 0")
    suspend fun getDownvoteCount(subtitleId: String): Int
    
    @Query("SELECT COUNT(*) FROM subtitle_votes WHERE userId = :userId AND isUpvote = 1")
    suspend fun getUserUpvoteCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM subtitle_votes WHERE userId = :userId AND isUpvote = 0")
    suspend fun getUserDownvoteCount(userId: String): Int
    
    @Query("DELETE FROM subtitle_votes WHERE subtitleId = :subtitleId")
    suspend fun deleteAllVotesForSubtitle(subtitleId: String)
    
    @Query("DELETE FROM subtitle_votes WHERE userId = :userId")
    suspend fun deleteAllUserVotes(userId: String)
    
    @Query("""
        SELECT subtitleId, SUM(CASE WHEN isUpvote = 1 THEN weight ELSE -weight END) as score 
        FROM subtitle_votes 
        GROUP BY subtitleId 
        ORDER BY score DESC 
        LIMIT :limit
    """)
    suspend fun getTopVotedSubtitleIds(limit: Int = 50): List<SubtitleScore>
    
    @Query("""
        SELECT * FROM subtitle_votes 
        WHERE createdAt > :timestamp 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """)
    fun observeRecentVotes(timestamp: Long, limit: Int = 100): Flow<List<SubtitleVoteEntity>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM subtitle_votes WHERE subtitleId = :subtitleId AND userId = :userId)")
    suspend fun hasUserVoted(subtitleId: String, userId: String): Boolean
    
    @Transaction
    suspend fun toggleVote(subtitleId: String, userId: String, isUpvote: Boolean) {
        val existingVote = getUserVote(subtitleId, userId)
        if (existingVote != null) {
            if (existingVote.isUpvote == isUpvote) {
                // Same vote, remove it
                delete(existingVote)
            } else {
                // Different vote, update it
                update(existingVote.copy(isUpvote = isUpvote))
            }
        } else {
            // No existing vote, create new one
            insert(SubtitleVoteEntity(
                subtitleId = subtitleId,
                userId = userId,
                isUpvote = isUpvote
            ))
        }
    }
}

data class SubtitleScore(
    val subtitleId: String,
    val score: Float
)