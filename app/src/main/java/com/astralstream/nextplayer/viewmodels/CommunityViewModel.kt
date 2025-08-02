package com.astralstream.nextplayer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralstream.nextplayer.community.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val repository: CommunityRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val stats: CommunityStats,
            val topContributors: List<Contributor>,
            val recentActivity: List<Activity>,
            val popularPlaylists: List<SharedPlaylist>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }
    
    data class CommunityStats(
        val totalUsers: Int,
        val sharedPlaylists: Int,
        val contributedSubtitles: Int
    )
    
    data class Contributor(
        val userId: String,
        val username: String,
        val contributions: Int,
        val rank: Int
    )
    
    data class Activity(
        val id: String,
        val userId: String,
        val username: String,
        val type: ActivityType,
        val action: String,
        val details: String?,
        val timestamp: Long,
        val actionable: Boolean
    )
    
    enum class ActivityType {
        SHARED_PLAYLIST,
        CONTRIBUTED_SUBTITLE,
        LIKED_PLAYLIST,
        JOINED_COMMUNITY
    }
    
    data class SharedPlaylist(
        val id: String,
        val name: String,
        val description: String,
        val creatorName: String,
        val videoCount: Int,
        val totalDuration: Long,
        val likes: Int,
        val plays: Int
    )
    
    fun refreshCommunityData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val stats = repository.getCommunityStats()
                val contributors = repository.getTopContributors()
                val activity = repository.getRecentActivity()
                val playlists = repository.getPopularPlaylists()
                
                _uiState.value = UiState.Success(
                    stats = stats,
                    topContributors = contributors,
                    recentActivity = activity,
                    popularPlaylists = playlists
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun searchCommunity(query: String) {
        viewModelScope.launch {
            repository.searchCommunity(query)
            refreshCommunityData()
        }
    }
    
    fun sharePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.sharePlaylist(playlistId)
        }
    }
    
    fun startSubtitleContribution(videoId: String) {
        viewModelScope.launch {
            repository.startSubtitleContribution(videoId)
        }
    }
}