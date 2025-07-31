package com.astralplayer.astralstream.data.dao

import androidx.room.*
import com.astralplayer.astralstream.data.entity.PlaylistEntity
import com.astralplayer.astralstream.data.entity.PlaylistType
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    
    @Query("SELECT * FROM playlists ORDER BY lastModifiedTime DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?
    
    @Query("SELECT * FROM playlists WHERE playlistType = :type")
    fun getPlaylistsByType(type: PlaylistType): Flow<List<PlaylistEntity>>
    
    @Query("SELECT * FROM playlists WHERE isSystemPlaylist = 0 ORDER BY name ASC")
    fun getCustomPlaylists(): Flow<List<PlaylistEntity>>
    
    @Query("SELECT * FROM playlists WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchPlaylists(query: String): Flow<List<PlaylistEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long
    
    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)
    
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)
    
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)
    
    @Query("UPDATE playlists SET videoCount = :count, totalDuration = :duration, lastModifiedTime = :timestamp WHERE id = :playlistId")
    suspend fun updatePlaylistStats(
        playlistId: Long,
        count: Int,
        duration: Long,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE playlists SET thumbnailPath = :thumbnailPath WHERE id = :playlistId")
    suspend fun updatePlaylistThumbnail(playlistId: Long, thumbnailPath: String)
}