package com.astralplayer.nextplayer.data.cloud

import java.util.Date

// Ensure CloudProvider is imported if not already defined
import com.astralplayer.nextplayer.data.cloud.CloudProvider

/**
 * Represents a cloud storage account
 */
data class CloudAccount(
    val id: String,
    val provider: CloudProvider,
    val email: String,
    val displayName: String,
    val isConnected: Boolean = false,
    val lastSyncTime: Date? = null,
    val storageUsed: Long = 0L,
    val storageTotal: Long = 0L
)

/**
 * Represents a file stored in cloud storage
 */
data class CloudFile(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String,
    val isVideo: Boolean,
    val thumbnailUrl: String? = null,
    val downloadUrl: String? = null,
    val modifiedTime: Date,
    val provider: CloudProvider,
    val syncStatus: SyncStatus = SyncStatus.IDLE
)


/**
 * Result wrapper for cloud operations
 */
sealed class CloudOperationResult<out T> {
    data class Success<T>(val data: T) : CloudOperationResult<T>()
    data class Error(val exception: Exception) : CloudOperationResult<Nothing>()
    object Loading : CloudOperationResult<Nothing>()
}