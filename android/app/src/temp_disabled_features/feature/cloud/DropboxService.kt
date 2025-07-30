package com.astralplayer.nextplayer.feature.cloud

import android.content.Context
import android.util.Log
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Date

/**
 * Service for Dropbox integration
 */
class DropboxService(private val context: Context) {
    
    companion object {
        private const val TAG = "DropboxService"
        private const val APP_KEY = "YOUR_DROPBOX_APP_KEY" // Replace with actual app key
        private const val VIDEO_EXTENSIONS = listOf(".mp4", ".webm", ".avi", ".mov", ".mkv", ".flv", ".wmv")
    }
    
    private var client: DbxClientV2? = null
    private var credential: DbxCredential? = null
    
    /**
     * Initialize Dropbox authentication
     */
    fun startAuthentication() {
        Auth.startOAuth2Authentication(context, APP_KEY)
    }
    
    /**
     * Complete authentication and setup client
     */
    suspend fun completeAuthentication(): Boolean = withContext(Dispatchers.IO) {
        try {
            val authDbxCredential = Auth.getDbxCredential()
            if (authDbxCredential != null) {
                credential = authDbxCredential
                
                val config = DbxRequestConfig.newBuilder("AstralPlayer/1.0").build()
                client = DbxClientV2(config, authDbxCredential)
                
                Log.d(TAG, "Dropbox authentication successful")
                true
            } else {
                Log.e(TAG, "No Dropbox credential available")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete authentication", e)
            false
        }
    }
    
    /**
     * Get account info
     */
    suspend fun getAccountInfo(): CloudAccount? = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext null
            val account = dbxClient.users().currentAccount
            
            val spaceUsage = dbxClient.users().spaceUsage
            
            CloudAccount(
                id = "dropbox_${account.accountId}",
                provider = CloudProvider.DROPBOX,
                email = account.email,
                displayName = account.name.displayName,
                isConnected = true,
                storageUsed = spaceUsage.used,
                storageTotal = if (spaceUsage.allocation.isIndividual) {
                    spaceUsage.allocation.individualValue.allocated
                } else {
                    0L
                },
                lastSync = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get account info", e)
            null
        }
    }
    
    /**
     * List video files from Dropbox
     */
    suspend fun listVideoFiles(path: String = ""): List<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext emptyList()
            
            val result = dbxClient.files().listFolder(path)
            val videoFiles = mutableListOf<CloudFile>()
            
            processListFolderResult(result, videoFiles)
            
            // Continue with cursor if there are more results
            var hasMore = result.hasMore
            var cursor = result.cursor
            
            while (hasMore) {
                val continueResult = dbxClient.files().listFolderContinue(cursor)
                processListFolderResult(continueResult, videoFiles)
                hasMore = continueResult.hasMore
                cursor = continueResult.cursor
            }
            
            videoFiles
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list files", e)
            emptyList()
        }
    }
    
    /**
     * Process list folder result
     */
    private fun processListFolderResult(result: ListFolderResult, videoFiles: MutableList<CloudFile>) {
        for (metadata in result.entries) {
            when (metadata) {
                is FileMetadata -> {
                    if (isVideoFile(metadata.name)) {
                        videoFiles.add(convertToCloudFile(metadata))
                    }
                }
                is FolderMetadata -> {
                    // Add folder for navigation
                    videoFiles.add(convertToCloudFile(metadata))
                }
            }
        }
    }
    
    /**
     * Search for files
     */
    suspend fun searchFiles(query: String): List<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext emptyList()
            
            val searchResult = dbxClient.files()
                .searchV2(query)
                
            val videoFiles = mutableListOf<CloudFile>()
            
            for (searchMatch in searchResult.matches) {
                val metadata = searchMatch.metadata
                if (metadata is SearchMatchTypeV2.Tag.METADATA) {
                    val metadataValue = metadata.metadataValue.metadata
                    when (metadataValue) {
                        is FileMetadata -> {
                            if (isVideoFile(metadataValue.name)) {
                                videoFiles.add(convertToCloudFile(metadataValue))
                            }
                        }
                        is FolderMetadata -> {
                            videoFiles.add(convertToCloudFile(metadataValue))
                        }
                    }
                }
            }
            
            videoFiles
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search files", e)
            emptyList()
        }
    }
    
    /**
     * Download file from Dropbox
     */
    suspend fun downloadFile(path: String, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext false
            
            FileOutputStream(outputPath).use { outputStream ->
                dbxClient.files().download(path).download(outputStream)
            }
            
            Log.d(TAG, "File downloaded successfully: $outputPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download file", e)
            false
        }
    }
    
    /**
     * Upload file to Dropbox
     */
    suspend fun uploadFile(localPath: String, remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext false
            
            java.io.File(localPath).inputStream().use { inputStream ->
                dbxClient.files().uploadBuilder(remotePath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(inputStream)
            }
            
            Log.d(TAG, "File uploaded successfully: $remotePath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file", e)
            false
        }
    }
    
    /**
     * Get temporary link for streaming
     */
    suspend fun getTemporaryLink(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val dbxClient = client ?: return@withContext null
            val result = dbxClient.files().getTemporaryLink(path)
            result.link
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get temporary link", e)
            null
        }
    }
    
    /**
     * Check if file is a video
     */
    private fun isVideoFile(filename: String): Boolean {
        return VIDEO_EXTENSIONS.any { filename.lowercase().endsWith(it) }
    }
    
    /**
     * Convert Dropbox FileMetadata to CloudFile
     */
    private fun convertToCloudFile(metadata: FileMetadata): CloudFile {
        return CloudFile(
            id = metadata.id,
            name = metadata.name,
            path = metadata.pathLower ?: metadata.pathDisplay ?: "",
            size = metadata.size,
            mimeType = getMimeType(metadata.name),
            modifiedTime = metadata.serverModified.time,
            isFolder = false,
            provider = CloudProvider.DROPBOX,
            downloadUrl = null, // Use getTemporaryLink for streaming
            thumbnailUrl = null
        )
    }
    
    /**
     * Convert Dropbox FolderMetadata to CloudFile
     */
    private fun convertToCloudFile(metadata: FolderMetadata): CloudFile {
        return CloudFile(
            id = metadata.id,
            name = metadata.name,
            path = metadata.pathLower ?: metadata.pathDisplay ?: "",
            size = 0L,
            mimeType = "application/vnd.dropbox.folder",
            modifiedTime = System.currentTimeMillis(),
            isFolder = true,
            provider = CloudProvider.DROPBOX,
            downloadUrl = null,
            thumbnailUrl = null
        )
    }
    
    /**
     * Get MIME type from filename
     */
    private fun getMimeType(filename: String): String {
        return when {
            filename.endsWith(".mp4", true) -> "video/mp4"
            filename.endsWith(".webm", true) -> "video/webm"
            filename.endsWith(".avi", true) -> "video/x-msvideo"
            filename.endsWith(".mov", true) -> "video/quicktime"
            filename.endsWith(".mkv", true) -> "video/x-matroska"
            filename.endsWith(".flv", true) -> "video/x-flv"
            filename.endsWith(".wmv", true) -> "video/x-ms-wmv"
            else -> "video/*"
        }
    }
    
    /**
     * Check if service is connected
     */
    fun isConnected(): Boolean {
        return client != null && credential != null
    }
    
    /**
     * Disconnect from Dropbox
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            client = null
            credential = null
            
            // Clear stored credentials
            Auth.getDbxCredential()?.let {
                // Note: Dropbox SDK doesn't provide a direct way to revoke tokens
                // User needs to revoke access from Dropbox account settings
            }
            
            Log.d(TAG, "Disconnected from Dropbox")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect", e)
        }
    }
}