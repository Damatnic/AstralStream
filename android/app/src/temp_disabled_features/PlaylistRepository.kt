package com.astralplayer.nextplayer

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("playlists_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val playlistsKey = "playlists"
    
    suspend fun getAllPlaylists(): List<VideoPlaylist> = withContext(Dispatchers.IO) {
        val json = prefs.getString(playlistsKey, null) ?: return@withContext emptyList()
        
        return@withContext try {
            val type = object : TypeToken<List<VideoPlaylist>>() {}.type
            gson.fromJson<List<VideoPlaylist>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getPlaylist(id: Long): VideoPlaylist? = withContext(Dispatchers.IO) {
        getAllPlaylists().find { it.id == id }
    }
    
    suspend fun createPlaylist(playlist: VideoPlaylist) = withContext(Dispatchers.IO) {
        val playlists = getAllPlaylists().toMutableList()
        playlists.add(playlist)
        savePlaylists(playlists)
    }
    
    suspend fun updatePlaylist(playlist: VideoPlaylist) = withContext(Dispatchers.IO) {
        val playlists = getAllPlaylists().toMutableList()
        val index = playlists.indexOfFirst { it.id == playlist.id }
        if (index != -1) {
            playlists[index] = playlist.copy(dateModified = System.currentTimeMillis())
            savePlaylists(playlists)
        }
    }
    
    suspend fun deletePlaylist(playlist: VideoPlaylist) = withContext(Dispatchers.IO) {
        val playlists = getAllPlaylists().toMutableList()
        playlists.removeAll { it.id == playlist.id }
        savePlaylists(playlists)
    }
    
    suspend fun updatePlaylist(playlistId: Long, newName: String, newDescription: String) = withContext(Dispatchers.IO) {
        val playlists = getAllPlaylists().toMutableList()
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            playlists[index] = playlists[index].copy(
                name = newName,
                description = newDescription,
                dateModified = System.currentTimeMillis()
            )
            savePlaylists(playlists)
        }
    }
    
    suspend fun addVideoToPlaylist(playlistId: Long, videoId: Long) = withContext(Dispatchers.IO) {
        val playlists = getAllPlaylists().toMutableList()
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = playlists[index]
            if (!playlist.videoIds.contains(videoId)) {
                playlists[index] = playlist.copy(
                    videoIds = playlist.videoIds + videoId,
                    dateModified = System.currentTimeMillis()
                )
                savePlaylists(playlists)
            }
        }
    }
    
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: Long) = withContext(Dispatchers.IO) {
        val playlists = getAllPlaylists().toMutableList()
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = playlists[index]
            playlists[index] = playlist.copy(
                videoIds = playlist.videoIds.filter { it != videoId },
                dateModified = System.currentTimeMillis()
            )
            savePlaylists(playlists)
        }
    }
    
    suspend fun reorderVideosInPlaylist(playlistId: Long, videoIds: List<Long>) = withContext(Dispatchers.IO) {
        val playlists = getAllPlaylists().toMutableList()
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            playlists[index] = playlists[index].copy(
                videoIds = videoIds,
                dateModified = System.currentTimeMillis()
            )
            savePlaylists(playlists)
        }
    }
    
    suspend fun getPlaylistsContainingVideo(videoId: Long): List<VideoPlaylist> = withContext(Dispatchers.IO) {
        getAllPlaylists().filter { playlist ->
            playlist.videoIds.contains(videoId)
        }
    }
    
    private fun savePlaylists(playlists: List<VideoPlaylist>) {
        val json = gson.toJson(playlists)
        prefs.edit().putString(playlistsKey, json).apply()
    }
    
    suspend fun clearAllPlaylists() = withContext(Dispatchers.IO) {
        prefs.edit().remove(playlistsKey).apply()
    }
}