package com.astralplayer.nextplayer.feature.cloud

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Stub OneDrive service - OneDrive support disabled per user request
 * This is kept to maintain compatibility with existing code
 */
class OneDriveService(private val context: Context) {
    
    companion object {
        private const val TAG = "OneDriveService"
    }
    
    /**
     * OneDrive authentication - disabled
     */
    suspend fun authenticate(activity: Activity): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "OneDrive service disabled - use Google Drive instead")
        false
    }
    
    /**
     * Get account info - returns null since OneDrive is disabled
     */
    suspend fun getAccountInfo(): CloudAccount? = withContext(Dispatchers.IO) {
        Log.d(TAG, "OneDrive service disabled")
        null
    }
    
    /**
     * List video files - returns empty list since OneDrive is disabled
     */
    suspend fun listVideoFiles(): List<CloudFile> = withContext(Dispatchers.IO) {
        Log.d(TAG, "OneDrive service disabled")
        emptyList()
    }
    
    /**
     * Download file - always fails since OneDrive is disabled
     */
    suspend fun downloadFile(cloudFile: CloudFile, localFile: File): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "OneDrive service disabled")
        false
    }
    
    /**
     * Get storage quota - returns null since OneDrive is disabled
     */
    suspend fun getStorageQuota(): Pair<Long, Long>? = withContext(Dispatchers.IO) {
        Log.d(TAG, "OneDrive service disabled")
        null
    }
    
    /**
     * Sign out - no-op since OneDrive is disabled
     */
    suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "OneDrive service disabled")
        true
    }
    
    /**
     * Check if signed in - always false since OneDrive is disabled
     */
    fun isSignedIn(): Boolean {
        return false
    }
}