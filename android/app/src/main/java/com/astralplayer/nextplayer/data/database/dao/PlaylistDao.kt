package com.astralplayer.nextplayer.data.database.dao

import androidx.room.*
import com.astralplayer.nextplayer.data.database.entity.PlaylistEntity
import com.astralplayer.nextplayer.data.database.entity.PlaylistVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    
    // Playlist operations
    @Query("SELECT * FROM playlists ORDER BY modified_date DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?
    
    @Query("SELECT * FROM playlists WHERE is_favorite = 1 ORDER BY modified_date DESC")
    fun getFavoritePlaylists(): Flow<List<PlaylistEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long
    
    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)
    
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)
    
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)
    
    // Playlist video operations
    @Query("SELECT * FROM playlist_videos WHERE playlist_id = :playlistId ORDER BY position ASC")
    fun getVideosForPlaylist(playlistId: Long): Flow<List<PlaylistVideoEntity>>
    
    @Query("SELECT * FROM playlist_videos WHERE playlist_id = :playlistId ORDER BY position ASC")
    suspend fun getVideosForPlaylistSync(playlistId: Long): List<PlaylistVideoEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistVideo(video: PlaylistVideoEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistVideos(videos: List<PlaylistVideoEntity>)
    
    @Update
    suspend fun updatePlaylistVideo(video: PlaylistVideoEntity)
    
    @Delete
    suspend fun deletePlaylistVideo(video: PlaylistVideoEntity)
    
    @Query("DELETE FROM playlist_videos WHERE id = :videoId")
    suspend fun deletePlaylistVideoById(videoId: Long)
    
    @Query("DELETE FROM playlist_videos WHERE playlist_id = :playlistId")
    suspend fun deleteAllVideosFromPlaylist(playlistId: Long)
    
    // Utility queries
    @Query("UPDATE playlists SET video_count = (SELECT COUNT(*) FROM playlist_videos WHERE playlist_id = :playlistId) WHERE id = :playlistId")
    suspend fun updateVideoCount(playlistId: Long)
    
    @Query("UPDATE playlists SET total_duration = (SELECT SUM(duration) FROM playlist_videos WHERE playlist_id = :playlistId) WHERE id = :playlistId")
    suspend fun updateTotalDuration(playlistId: Long)
    
    @Query("UPDATE playlists SET modified_date = :date WHERE id = :playlistId")
    suspend fun updateModifiedDate(playlistId: Long, date: Long = System.currentTimeMillis())
    
    @Query("UPDATE playlist_videos SET position = :newPosition WHERE id = :videoId")
    suspend fun updateVideoPosition(videoId: Long, newPosition: Int)
    
    @Query("SELECT EXISTS(SELECT 1 FROM playlist_videos WHERE playlist_id = :playlistId AND video_path = :videoPath)")
    suspend fun isVideoInPlaylist(playlistId: Long, videoPath: String): Boolean
    
    @Transaction
    suspend fun addVideoToPlaylist(playlistId: Long, video: PlaylistVideoEntity) {
        insertPlaylistVideo(video)
        updateVideoCount(playlistId)
        updateTotalDuration(playlistId)
        updateModifiedDate(playlistId)
    }
    
    @Transaction
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: Long) {
        deletePlaylistVideoById(videoId)
        updateVideoCount(playlistId)
        updateTotalDuration(playlistId)
        updateModifiedDate(playlistId)
    }
    
    @Transaction
    suspend fun reorderPlaylistVideos(videos: List<PlaylistVideoEntity>) {
        videos.forEachIndexed { index, video ->
            updateVideoPosition(video.id, index)
        }
        if (videos.isNotEmpty()) {
            updateModifiedDate(videos.first().playlistId)
        }
    }
}