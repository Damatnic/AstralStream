package com.astralplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.download.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
    private val offlineVideoRepository: OfflineVideoRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()
    
    init {
        observeDownloads()
        loadStorageInfo()
    }
    
    private fun observeDownloads() {
        // Observe download queue
        downloadManager.downloadQueue
            .onEach { downloads ->
                _uiState.update { state ->
                    state.copy(
                        downloads = downloads,
                        hasCompletedDownloads = downloads.any { it.status == DownloadStatus.COMPLETED }
                    )
                }
            }
            .launchIn(viewModelScope)
        
        // Observe active downloads
        downloadManager.activeDownloads
            .onEach { activeDownloads ->
                _uiState.update { state ->
                    state.copy(activeDownloads = activeDownloads)
                }
            }
            .launchIn(viewModelScope)
    }
    
    private fun loadStorageInfo() {
        viewModelScope.launch {
            val storageInfo = downloadManager.getStorageInfo()
            _uiState.update { state ->
                state.copy(storageInfo = storageInfo)
            }
        }
    }
    
    fun pauseDownload(downloadId: String) {
        downloadManager.pauseDownload(downloadId)
    }
    
    fun resumeDownload(downloadId: String) {
        downloadManager.resumeDownload(downloadId)
    }
    
    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            downloadManager.cancelDownload(downloadId)
            loadStorageInfo() // Update storage info after deletion
        }
    }
    
    fun clearCompleted() {
        viewModelScope.launch {
            downloadManager.clearCompleted()
            loadStorageInfo()
        }
    }
}

data class DownloadsUiState(
    val downloads: List<DownloadTask> = emptyList(),
    val activeDownloads: Map<String, DownloadProgress> = emptyMap(),
    val storageInfo: StorageInfo = StorageInfo(0, 0, 0, 0),
    val hasCompletedDownloads: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)