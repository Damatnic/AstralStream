package com.astralplayer.nextplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.nextplayer.data.cloud.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for managing cloud storage functionality
 */
@HiltViewModel
class CloudStorageViewModel @Inject constructor() : ViewModel() {
    
    private val _connectedAccounts = MutableStateFlow<List<CloudAccount>>(emptyList())
    val connectedAccounts: StateFlow<List<CloudAccount>> = _connectedAccounts.asStateFlow()
    
    private val _cloudFiles = MutableStateFlow<List<CloudFile>>(emptyList())
    val cloudFiles: StateFlow<List<CloudFile>> = _cloudFiles.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    init {
        loadMockData()
    }
    
    /**
     * Load mock data for demonstration
     */
    private fun loadMockData() {
        viewModelScope.launch {
            _connectedAccounts.value = listOf(
                CloudAccount(
                    id = "1",
                    provider = CloudProvider.GOOGLE_DRIVE,
                    email = "user@gmail.com",
                    displayName = "Google Drive",
                    isConnected = true,
                    storageUsed = 5_000_000_000L,
                    storageTotal = 15_000_000_000L
                ),
                CloudAccount(
                    id = "2",
                    provider = CloudProvider.DROPBOX,
                    email = "user@dropbox.com",
                    displayName = "Dropbox",
                    isConnected = false,
                    storageUsed = 0L,
                    storageTotal = 2_000_000_000L
                )
            )
        }
    }
    
    /**
     * Connect to a cloud provider
     */
    fun connectProvider(provider: CloudProvider) {
        viewModelScope.launch {
            _isLoading.value = true
            // TODO: Implement actual cloud provider connection
            _isLoading.value = false
        }
    }
    
    /**
     * Disconnect from a cloud provider
     */
    fun disconnectProvider(accountId: String) {
        viewModelScope.launch {
            _connectedAccounts.value = _connectedAccounts.value.map { account ->
                if (account.id == accountId) {
                    account.copy(isConnected = false)
                } else {
                    account
                }
            }
        }
    }
    
    /**
     * Load files from cloud storage
     */
    fun loadCloudFiles(provider: CloudProvider) {
        viewModelScope.launch {
            _isLoading.value = true
            // TODO: Implement actual file loading from cloud
            _cloudFiles.value = generateMockFiles(provider)
            _isLoading.value = false
        }
    }
    
    /**
     * Sync local files with cloud
     */
    fun syncFiles() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            // TODO: Implement actual sync logic
            kotlinx.coroutines.delay(2000) // Simulate sync
            _syncStatus.value = SyncStatus.COMPLETED
        }
    }
    
    /**
     * Generate mock files for demonstration
     */
    private fun generateMockFiles(provider: CloudProvider): List<CloudFile> {
        val videoExtensions = listOf(".mp4", ".mkv", ".avi", ".mov")
        return (1..10).map { index ->
            CloudFile(
                id = "file_$index",
                name = "Video_$index${videoExtensions.random()}",
                path = "/Videos/Video_$index${videoExtensions.random()}",
                size = (100_000_000L..1_000_000_000L).random(),
                mimeType = "video/mp4",
                isVideo = true,
                modifiedTime = Date(),
                provider = provider,
                syncStatus = SyncStatus.values().random()
            )
        }
    }
}