package com.astralplayer.nextplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    
    private val playlistRepository = PlaylistRepository(application)
    
    private val _playlists = MutableStateFlow<List<VideoPlaylist>>(emptyList())
    val playlists: StateFlow<List<VideoPlaylist>> = _playlists
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private var deletedPlaylist: VideoPlaylist? = null
    private var currentSortType = PlaylistSortType.DATE_MODIFIED
    
    init {
        loadPlaylists()
    }
    
    private fun loadPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            _playlists.value = playlistRepository.getAllPlaylists()
            sortPlaylists(currentSortType)
            _isLoading.value = false
        }
    }
    
    fun createPlaylist(name: String, description: String) {
        viewModelScope.launch {
            val playlist = VideoPlaylist(
                id = System.currentTimeMillis(),
                name = name,
                description = description,
                dateCreated = System.currentTimeMillis(),
                dateModified = System.currentTimeMillis()
            )
            playlistRepository.createPlaylist(playlist)
            loadPlaylists()
        }
    }
    
    fun deletePlaylist(playlist: VideoPlaylist) {
        viewModelScope.launch {
            deletedPlaylist = playlist
            playlistRepository.deletePlaylist(playlist)
            loadPlaylists()
        }
    }
    
    fun undoDelete() {
        deletedPlaylist?.let { playlist ->
            viewModelScope.launch {
                playlistRepository.createPlaylist(playlist)
                loadPlaylists()
                deletedPlaylist = null
            }
        }
    }
    
    fun sortPlaylists(sortType: PlaylistSortType) {
        currentSortType = sortType
        _playlists.value = when (sortType) {
            PlaylistSortType.NAME -> _playlists.value.sortedBy { it.name.lowercase() }
            PlaylistSortType.DATE_CREATED -> _playlists.value.sortedByDescending { it.dateCreated }
            PlaylistSortType.DATE_MODIFIED -> _playlists.value.sortedByDescending { it.dateModified }
            PlaylistSortType.VIDEO_COUNT -> _playlists.value.sortedByDescending { it.videoIds.size }
        }
    }
    
    fun updatePlaylist(playlistId: Long, newName: String, newDescription: String) {
        viewModelScope.launch {
            playlistRepository.updatePlaylist(playlistId, newName, newDescription)
            loadPlaylists()
        }
    }
}

class PlaylistViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PlaylistViewModel(application) as T
    }
}

enum class PlaylistSortType {
    NAME, DATE_CREATED, DATE_MODIFIED, VIDEO_COUNT
}