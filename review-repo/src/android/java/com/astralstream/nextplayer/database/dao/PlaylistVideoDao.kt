package com.astralstream.nextplayer.database.dao

import androidx.room.*
import com.astralstream.nextplayer.database.entities.PlaylistVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistVideoDao {
    
    @Query("SELECT * FROM playlist_videos WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getVideosForPlaylist(playlistId: String): List<PlaylistVideoEntity>
    
    @Query("SELECT * FROM playlist_videos WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getVideosForPlaylistFlow(playlistId: String): Flow<List<PlaylistVideoEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: PlaylistVideoEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<PlaylistVideoEntity>)
    
    @Update
    suspend fun update(video: PlaylistVideoEntity)
    
    @Delete
    suspend fun delete(video: PlaylistVideoEntity)
    
    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId")
    suspend fun deleteAllForPlaylist(playlistId: String)
}