package com.astralplayer.community.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.community.api.ActivityResponse
import com.astralplayer.community.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val playlistRepository: PlaylistSharingRepository,
    private val subtitleRepository: SubtitleContributionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            launch { loadTrendingPlaylists() }
            launch { loadTopContributors() }
            launch { loadRecentActivities() }
        }
    }
    
    fun refreshPlaylists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadTrendingPlaylists()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
    
    private suspend fun loadTrendingPlaylists() {
        playlistRepository.getTrendingPlaylists()
            .catch { e ->
                _uiState.update { 
                    it.copy(error = "Failed to load playlists: ${e.message}")
                }
            }
            .collect { playlists ->
                _uiState.update { it.copy(trendingPlaylists = playlists) }
            }
    }
    
    private suspend fun loadTopContributors() {
        val result = subtitleRepository.getTopContributors()
        if (result.isSuccess) {
            _uiState.update { it.copy(topContributors = result.getOrDefault(emptyList())) }
        }
    }
    
    private suspend fun loadRecentActivities() {
        // Mock recent activities for now
        val mockActivities = listOf(
            ActivityResponse(
                id = "1",
                type = "playlist_shared",
                title = "New playlist shared",
                description = "MovieBuff shared 'Best Action Movies 2024'",
                actorName = "MovieBuff",
                timestamp = System.currentTimeMillis() - 3600000
            ),
            ActivityResponse(
                id = "2",
                type = "subtitle_contributed",
                title = "New subtitle contribution",
                description = "SubtitlePro added English subtitles",
                actorName = "SubtitlePro",
                timestamp = System.currentTimeMillis() - 7200000
            )
        )
        _uiState.update { it.copy(recentActivities = mockActivities) }
    }
    
    fun searchPlaylists(query: String) {
        viewModelScope.launch {
            val result = playlistRepository.searchPlaylists(query)
            if (result.isSuccess) {
                _uiState.update { it.copy(searchResults = result.getOrDefault(emptyList())) }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class CommunityUiState(
    val trendingPlaylists: List<TrendingPlaylist> = emptyList(),
    val topContributors: List<Contributor> = emptyList(),
    val recentActivities: List<ActivityResponse> = emptyList(),
    val recentSubtitles: List<CommunitySubtitle> = emptyList(),
    val searchResults: List<TrendingPlaylist> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null
)

enum class CommunityTab(
    val title: String,
    val icon: ImageVector
) {
    PLAYLISTS("Playlists", Icons.Default.PlaylistPlay),
    SUBTITLES("Subtitles", Icons.Default.Subtitles),
    ACTIVITY("Activity", Icons.Default.Timeline)
}