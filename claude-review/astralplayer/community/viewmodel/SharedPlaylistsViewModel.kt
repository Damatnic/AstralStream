package com.astralplayer.community.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.community.data.PlaylistCategory
import com.astralplayer.community.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedPlaylistsViewModel @Inject constructor(
    private val repository: PlaylistSharingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SharedPlaylistsUiState())
    val uiState: StateFlow<SharedPlaylistsUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Load my shared playlists
            repository.getMySharedPlaylists()
                .catch { e ->
                    _uiState.update { 
                        it.copy(error = "Failed to load shared playlists: ${e.message}")
                    }
                }
                .collect { playlists ->
                    _uiState.update { 
                        it.copy(
                            mySharedPlaylists = playlists,
                            isLoading = false
                        )
                    }
                }
        }
        
        // Load available local playlists
        loadLocalPlaylists()
    }
    
    private fun loadLocalPlaylists() {
        // Mock implementation - in real app would load from local database
        val mockPlaylists = listOf(
            LocalPlaylist(
                id = "local1",
                title = "My Favorites",
                videoCount = 12,
                duration = 7200000L
            ),
            LocalPlaylist(
                id = "local2",
                title = "Watch Later",
                videoCount = 8,
                duration = 4800000L
            ),
            LocalPlaylist(
                id = "local3",
                title = "Educational Videos",
                videoCount = 25,
                duration = 15000000L
            )
        )
        _uiState.update { it.copy(availablePlaylists = mockPlaylists) }
    }
    
    fun selectPlaylist(playlist: LocalPlaylist) {
        _uiState.update { it.copy(selectedPlaylist = playlist) }
    }
    
    fun sharePlaylist(
        title: String,
        description: String,
        category: PlaylistCategory,
        isPublic: Boolean,
        password: String?,
        expirationDays: Int
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val selectedPlaylist = _uiState.value.selectedPlaylist ?: return@launch
            
            val result = repository.sharePlaylist(
                playlistId = selectedPlaylist.id,
                title = title,
                description = description,
                category = category,
                isPublic = isPublic,
                password = password,
                expirationDays = expirationDays
            )
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    shareResult = result,
                    error = if (result.isFailure) result.exceptionOrNull()?.message else null
                )
            }
            
            if (result.isSuccess) {
                loadData() // Reload to show new shared playlist
            }
        }
    }
    
    fun importPlaylist(shareCode: String, password: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = repository.importPlaylist(shareCode, password)
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    error = if (result.isFailure) {
                        result.exceptionOrNull()?.message
                    } else {
                        null
                    }
                )
            }
            
            if (result.isSuccess) {
                _uiState.update { 
                    it.copy(
                        importSuccess = true,
                        importMessage = "Playlist imported successfully: ${result.getOrNull()?.title}"
                    )
                }
            }
        }
    }
    
    fun revokePlaylist(shareCode: String) {
        viewModelScope.launch {
            val result = repository.revokeSharedPlaylist(shareCode)
            if (result.isSuccess) {
                loadData() // Reload to update status
            } else {
                _uiState.update { 
                    it.copy(error = "Failed to revoke playlist: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
    
    fun copyShareLink(shareUrl: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Playlist Share Link", shareUrl)
        clipboard.setPrimaryClip(clip)
        
        _uiState.update { 
            it.copy(toastMessage = "Share link copied to clipboard")
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearMessages() {
        _uiState.update { 
            it.copy(
                importSuccess = false,
                importMessage = null,
                toastMessage = null
            )
        }
    }
}

data class SharedPlaylistsUiState(
    val mySharedPlaylists: List<MySharedPlaylist> = emptyList(),
    val availablePlaylists: List<LocalPlaylist> = emptyList(),
    val selectedPlaylist: LocalPlaylist? = null,
    val shareResult: Result<ShareResult>? = null,
    val importSuccess: Boolean = false,
    val importMessage: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val toastMessage: String? = null
)

data class LocalPlaylist(
    val id: String,
    val title: String,
    val videoCount: Int,
    val duration: Long
)