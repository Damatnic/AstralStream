package com.astralplayer.community.dao

import androidx.room.*
import com.astralplayer.community.data.PlaylistRatingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistRatingDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rating: PlaylistRatingEntity)
    
    @Update
    suspend fun update(rating: PlaylistRatingEntity)
    
    @Delete
    suspend fun delete(rating: PlaylistRatingEntity)
    
    @Query("SELECT * FROM playlist_ratings WHERE playlistId = :playlistId AND userId = :userId LIMIT 1")
    suspend fun getUserRating(playlistId: String, userId: String): PlaylistRatingEntity?
    
    @Query("SELECT * FROM playlist_ratings WHERE playlistId = :playlistId ORDER BY createdAt DESC")
    suspend fun getPlaylistRatings(playlistId: String): List<PlaylistRatingEntity>
    
    @Query("SELECT * FROM playlist_ratings WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getUserRatings(userId: String): List<PlaylistRatingEntity>
    
    @Query("SELECT * FROM playlist_ratings WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeUserRatings(userId: String): Flow<List<PlaylistRatingEntity>>
    
    @Query("SELECT AVG(rating) FROM playlist_ratings WHERE playlistId = :playlistId")
    suspend fun getAverageRating(playlistId: String): Float?
    
    @Query("SELECT COUNT(*) FROM playlist_ratings WHERE playlistId = :playlistId")
    suspend fun getRatingCount(playlistId: String): Int
    
    @Query("SELECT COUNT(*) FROM playlist_ratings WHERE playlistId = :playlistId AND rating >= :minRating")
    suspend fun getPositiveRatingCount(playlistId: String, minRating: Float = 4.0f): Int
    
    @Query("""
        SELECT rating, COUNT(*) as count FROM playlist_ratings 
        WHERE playlistId = :playlistId 
        GROUP BY rating 
        ORDER BY rating DESC
    """)
    suspend fun getRatingDistribution(playlistId: String): List<RatingDistribution>
    
    @Query("""
        SELECT playlistId, AVG(rating) as avgRating, COUNT(*) as count 
        FROM playlist_ratings 
        GROUP BY playlistId 
        HAVING count >= :minCount 
        ORDER BY avgRating DESC 
        LIMIT :limit
    """)
    suspend fun getTopRatedPlaylists(minCount: Int = 5, limit: Int = 20): List<PlaylistRatingStats>
    
    @Query("SELECT * FROM playlist_ratings WHERE rating <= :maxRating AND review != '' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getLowRatingsWithReviews(maxRating: Float = 2.0f, limit: Int = 50): List<PlaylistRatingEntity>
    
    @Query("DELETE FROM playlist_ratings WHERE playlistId = :playlistId")
    suspend fun deleteAllRatingsForPlaylist(playlistId: String)
    
    @Query("SELECT EXISTS(SELECT 1 FROM playlist_ratings WHERE playlistId = :playlistId AND userId = :userId)")
    suspend fun hasUserRated(playlistId: String, userId: String): Boolean
    
    @Query("""
        SELECT * FROM playlist_ratings 
        WHERE review != '' 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """)
    fun observeRecentReviews(limit: Int = 20): Flow<List<PlaylistRatingEntity>>
    
    @Transaction
    suspend fun updateOrCreateRating(playlistId: String, userId: String, rating: Float, review: String = "") {
        val existing = getUserRating(playlistId, userId)
        if (existing != null) {
            update(existing.copy(rating = rating, review = review, updatedAt = System.currentTimeMillis()))
        } else {
            insert(PlaylistRatingEntity(
                playlistId = playlistId,
                userId = userId,
                rating = rating,
                review = review
            ))
        }
    }
}

data class RatingDistribution(
    val rating: Float,
    val count: Int
)

data class PlaylistRatingStats(
    val playlistId: String,
    val avgRating: Float,
    val count: Int
)