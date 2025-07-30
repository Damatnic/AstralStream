package com.astralplayer.nextplayer.feature.cloud

import android.content.Context
import android.util.Log
import com.astralplayer.nextplayer.data.cloud.CloudProvider
import com.dropbox.core.*
import com.dropbox.core.android.Auth
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.Metadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Service for Dropbox integration with real Dropbox SDK
 */
class DropboxService(private val context: Context) {
    
    companion object {
        private const val TAG = "DropboxService"
        private const val APP_KEY = "your_dropbox_app_key" // Replace with your actual app key
    }
    
    private var dbxClient: DbxClientV2? = null
    private val requestConfig = DbxRequestConfig.newBuilder("AstralPlayer").build()
    
    /**
     * Start Dropbox authentication
     */
    fun startAuthentication() {
        Log.d(TAG, "Starting Dropbox authentication")
        try {
            Auth.startOAuth2Authentication(context, APP_KEY)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Dropbox authentication", e)
        }
    }
    
    /**
     * Complete authentication process
     */
    suspend fun completeAuthentication(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val accessToken = Auth.getOAuth2Token()
            if (accessToken != null) {
                dbxClient = DbxClientV2(requestConfig, accessToken)
                Log.d(TAG, "Dropbox authentication successful")
                true
            } else {
                Log.d(TAG, "No Dropbox access token found")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dropbox authentication failed", e)
            false
        }
    }
    
    /**
     * Get account info
     */
    suspend fun getAccountInfo(): CloudAccount? = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = dbxClient ?: return@withContext null
            val account = client.users().currentAccount
            CloudAccount(
                id = "dropbox_${account.accountId}",
                provider = CloudProvider.DROPBOX,
                email = account.email,
                displayName = account.name.displayName,
                isConnected = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Dropbox account info", e)
            null
        }
    }
    
    /**
     * List video files from Dropbox
     */
    suspend fun listVideoFiles(): List<CloudFile> = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = dbxClient ?: return@withContext emptyList()
            Log.d(TAG, "Listing Dropbox files")
            
            val videoFiles = mutableListOf<CloudFile>()
            val videoExtensions = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp")
            
            var result = client.files().listFolder("")
            
            do {
                for (metadata in result.entries) {
                    if (metadata is FileMetadata) {
                        val extension = metadata.name.substringAfterLast(".", "").lowercase()
                        if (extension in videoExtensions) {
                            videoFiles.add(
                                CloudFile(
                                    id = metadata.id ?: metadata.pathLower ?: metadata.name,
                                    name = metadata.name,
                                    path = metadata.pathLower ?: "/${metadata.name}",
                                    size = metadata.size,
                                    mimeType = "video/$extension",
                                    modifiedTime = metadata.clientModified?.time ?: System.currentTimeMillis(),
                                    provider = CloudProvider.DROPBOX,
                                    isFolder = false
                                )
                            )
                        }
                    }
                }
                
                if (result.hasMore) {
                    result = client.files().listFolderContinue(result.cursor)
                }
            } while (result.hasMore)
            
            Log.d(TAG, "Found ${videoFiles.size} video files in Dropbox")
            videoFiles
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list Dropbox files", e)
            emptyList()
        }
    }
    
    /**
     * Search files
     */
    suspend fun searchFiles(query: String): List<CloudFile> = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = dbxClient ?: return@withContext emptyList()
            Log.d(TAG, "Searching Dropbox files for: $query")
            
            val searchResult = client.files().searchV2(query)
            val videoFiles = mutableListOf<CloudFile>()
            val videoExtensions = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp")
            
            for (match in searchResult.matches) {
                val metadata = match.metadata.metadataValue
                if (metadata is FileMetadata) {
                    val extension = metadata.name.substringAfterLast(".", "").lowercase()
                    if (extension in videoExtensions) {
                        videoFiles.add(
                            CloudFile(
                                id = metadata.id ?: metadata.pathLower ?: metadata.name,
                                name = metadata.name,
                                path = metadata.pathLower ?: "/${metadata.name}",
                                size = metadata.size,
                                mimeType = "video/$extension",
                                modifiedTime = metadata.clientModified?.time ?: System.currentTimeMillis(),
                                provider = CloudProvider.DROPBOX,
                                isFolder = false
                            )
                        )
                    }
                }
            }
            
            Log.d(TAG, "Found ${videoFiles.size} video files matching '$query'")
            videoFiles
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search Dropbox files", e)
            emptyList()
        }
    }
    
    /**
     * Download file from Dropbox
     */
    suspend fun downloadFile(path: String, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = dbxClient ?: return@withContext false
            Log.d(TAG, "Downloading Dropbox file: $path -> $outputPath")
            
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            FileOutputStream(outputFile).use { outputStream ->
                client.files().download(path).download(outputStream)
            }
            
            Log.d(TAG, "Successfully downloaded file to $outputPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download Dropbox file", e)
            false
        }
    }
    
    /**
     * Upload file to Dropbox
     */
    suspend fun uploadFile(localPath: String, remotePath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = dbxClient ?: return@withContext false
            Log.d(TAG, "Uploading file to Dropbox: $localPath -> $remotePath")
            
            val localFile = File(localPath)
            if (!localFile.exists()) {
                Log.e(TAG, "Local file does not exist: $localPath")
                return@withContext false
            }
            
            FileInputStream(localFile).use { inputStream ->
                client.files().uploadBuilder("/$remotePath")
                    .uploadAndFinish(inputStream)
            }
            
            Log.d(TAG, "Successfully uploaded file to $remotePath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file to Dropbox", e)
            false
        }
    }
    
    /**
     * Check if service is connected
     */
    fun isConnected(): Boolean {
        return dbxClient != null
    }
    
    /**
     * Disconnect from Dropbox
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        dbxClient = null
        Log.d(TAG, "Disconnected from Dropbox")
    }
}