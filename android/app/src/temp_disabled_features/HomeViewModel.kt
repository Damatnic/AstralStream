package com.astralplayer.nextplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen of the application.
 * Manages UI state and business logic for the home screen.
 */
class HomeViewModel : ViewModel() {

    // UI state for the home screen
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Recent files list
    private val _recentFiles = MutableStateFlow<List<VideoFile>>(emptyList())
    val recentFiles: StateFlow<List<VideoFile>> = _recentFiles.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadRecentFiles()
    }

    /**
     * Loads the list of recently played video files
     */
    fun loadRecentFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // In a real implementation, this would load from a repository
            // For now, we'll use sample data
            _recentFiles.value = getSampleVideoFiles()
            
            _isLoading.value = false
        }
    }

    /**
     * Updates the UI state based on user actions
     */
    fun updateUiState(newState: HomeUiState) {
        _uiState.value = newState
    }

    /**
     * Sample data for testing
     */
    private fun getSampleVideoFiles(): List<VideoFile> {
        return listOf(
            VideoFile(
                id = 1,
                title = "Sample Video 1",
                path = "/storage/emulated/0/Movies/sample1.mp4",
                duration = 120000, // 2 minutes in milliseconds
                thumbnailPath = null,
                lastPlayedPosition = 60000, // 1 minute in milliseconds
                lastPlayedDate = System.currentTimeMillis() - 86400000 // 1 day ago
            ),
            VideoFile(
                id = 2,
                title = "Sample Video 2",
                path = "/storage/emulated/0/Movies/sample2.mp4",
                duration = 300000, // 5 minutes in milliseconds
                thumbnailPath = null,
                lastPlayedPosition = 0,
                lastPlayedDate = System.currentTimeMillis() - 172800000 // 2 days ago
            )
        )
    }
}

/**
 * Represents the UI state for the home screen
 */
data class HomeUiState(
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val isGridView: Boolean = true
)

/**
 * Data class representing a video file
 */
data class VideoFile(
    val id: Long,
    val title: String,
    val path: String,
    val duration: Long, // in milliseconds
    val thumbnailPath: String?,
    val lastPlayedPosition: Long, // in milliseconds
    val lastPlayedDate: Long // timestamp
)