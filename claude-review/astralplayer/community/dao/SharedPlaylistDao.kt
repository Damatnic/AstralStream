package com.astralplayer.community.dao

import androidx.room.*
import com.astralplayer.community.data.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedPlaylistDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: SharedPlaylistEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playlists: List<SharedPlaylistEntity>)
    
    @Update
    suspend fun update(playlist: SharedPlaylistEntity)
    
    @Delete
    suspend fun delete(playlist: SharedPlaylistEntity)
    
    @Query("SELECT * FROM shared_playlists WHERE id = :id")
    suspend fun getById(id: String): SharedPlaylistEntity?
    
    @Query("SELECT * FROM shared_playlists WHERE shareCode = :shareCode LIMIT 1")
    suspend fun getByShareCode(shareCode: String): SharedPlaylistEntity?
    
    @Query("SELECT * FROM shared_playlists WHERE creatorId = :userId ORDER BY createdAt DESC")
    suspend fun getMySharedPlaylists(userId: String): List<SharedPlaylistEntity>
    
    @Query("SELECT * FROM shared_playlists WHERE creatorId = :userId ORDER BY createdAt DESC")
    fun observeMySharedPlaylists(userId: String): Flow<List<SharedPlaylistEntity>>
    
    @Query("""
        SELECT * FROM shared_playlists 
        WHERE isPublic = 1 AND isActive = 1 
        ORDER BY viewCount + downloadCount DESC 
        LIMIT :limit
    """)
    suspend fun getTrendingPlaylists(limit: Int = 20): List<SharedPlaylistEntity>
    
    @Query("""
        SELECT * FROM shared_playlists 
        WHERE isPublic = 1 AND isActive = 1 
        ORDER BY viewCount + downloadCount DESC 
        LIMIT :limit
    """)
    fun observeTrendingPlaylists(limit: Int = 20): Flow<List<SharedPlaylistEntity>>
    
    @Query("""
        SELECT * FROM shared_playlists 
        WHERE category = :category AND isPublic = 1 AND isActive = 1 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """)
    suspend fun getPlaylistsByCategory(category: PlaylistCategory, limit: Int = 20): List<SharedPlaylistEntity>
    
    @Query("""
        SELECT * FROM shared_playlists 
        WHERE language = :language AND isPublic = 1 AND isActive = 1 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """)
    suspend fun getPlaylistsByLanguage(language: String, limit: Int = 20): List<SharedPlaylistEntity>
    
    @Query("""
        SELECT * FROM shared_playlists 
        WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') 
        AND isPublic = 1 AND isActive = 1 
        ORDER BY viewCount + downloadCount DESC 
        LIMIT :limit
    """)
    suspend fun searchPlaylists(query: String, limit: Int = 20): List<SharedPlaylistEntity>
    
    @Query("""
        SELECT * FROM shared_playlists 
        WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') 
        AND isPublic = 1 AND isActive = 1 
        ORDER BY viewCount + downloadCount DESC 
        LIMIT :limit
    """)
    fun observeSearchPlaylists(query: String, limit: Int = 20): Flow<List<SharedPlaylistEntity>>
    
    @Query("UPDATE shared_playlists SET viewCount = viewCount + 1 WHERE id = :id")
    suspend fun incrementViewCount(id: String)
    
    @Query("UPDATE shared_playlists SET downloadCount = downloadCount + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: String)
    
    @Query("UPDATE shared_playlists SET likeCount = likeCount + 1 WHERE id = :id")
    suspend fun incrementLikeCount(id: String)
    
    @Query("UPDATE shared_playlists SET likeCount = likeCount - 1 WHERE id = :id AND likeCount > 0")
    suspend fun decrementLikeCount(id: String)
    
    @Query("UPDATE shared_playlists SET isActive = 0 WHERE id = :id")
    suspend fun deactivatePlaylist(id: String)
    
    @Query("DELETE FROM shared_playlists WHERE expirationTime < :currentTime AND expirationTime > 0")
    suspend fun deleteExpiredPlaylists(currentTime: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM shared_playlists WHERE creatorId = :userId")
    suspend fun getMyPlaylistCount(userId: String): Int
    
    @Query("SELECT SUM(viewCount) FROM shared_playlists WHERE creatorId = :userId")
    suspend fun getMyTotalViews(userId: String): Int?
    
    @Query("SELECT SUM(downloadCount) FROM shared_playlists WHERE creatorId = :userId")
    suspend fun getMyTotalDownloads(userId: String): Int?
    
    @Query("""
        SELECT * FROM shared_playlists 
        WHERE createdAt > :timestamp 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """)
    fun observeRecentPlaylists(timestamp: Long, limit: Int = 20): Flow<List<SharedPlaylistEntity>>
    
    @Query("""
        SELECT * FROM shared_playlists 
        WHERE likeCount > :minLikes AND isPublic = 1 AND isActive = 1 
        ORDER BY likeCount DESC 
        LIMIT :limit
    """)
    suspend fun getMostLikedPlaylists(minLikes: Int = 10, limit: Int = 20): List<SharedPlaylistEntity>
    
    @Query("SELECT * FROM shared_playlists WHERE originalPlaylistId = :playlistId LIMIT 1")
    suspend fun getByOriginalPlaylistId(playlistId: String): SharedPlaylistEntity?
    
    @Transaction
    suspend fun deleteAllUserPlaylists(userId: String) {
        val playlists = getMySharedPlaylists(userId)
        playlists.forEach { delete(it) }
    }
}