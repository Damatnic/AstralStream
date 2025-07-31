package com.astralplayer.cloud

import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dropbox cloud storage service implementation
 */
@Singleton
class DropboxService @Inject constructor(
    private val context: Context
) : CloudStorageProvider {
    
    companion object {
        private const val CLIENT_IDENTIFIER = "AstralStream/1.0"
        private const val APP_KEY = "YOUR_DROPBOX_APP_KEY" // Replace with actual app key
    }
    
    private var client: DbxClientV2? = null
    
    override val providerName = "Dropbox"
    override val providerId = "dropbox"
    
    /**
     * Initialize Dropbox authentication
     */
    override suspend fun authenticate(): Boolean = withContext(Dispatchers.Main) {
        try {
            Auth.startOAuth2Authentication(context, APP_KEY)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to start Dropbox authentication")
            false
        }
    }
    
    /**
     * Complete authentication after OAuth callback
     */
    suspend fun completeAuthentication(): Boolean = withContext(Dispatchers.IO) {
        try {
            val credential = Auth.getDbxCredential()
            if (credential != null) {
                initializeClient(credential)
                saveCredential(credential)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to complete Dropbox authentication")
            false
        }
    }
    
    /**
     * Check if authenticated
     */
    override suspend fun isAuthenticated(): Boolean {
        return client != null || loadSavedCredential() != null
    }
    
    /**
     * List files in directory
     */
    override suspend fun listFiles(path: String): Flow<List<CloudFile>> = flow {
        ensureClient()
        
        try {
            val result = client?.files()?.listFolder(path.ifEmpty { "" })
            val files = result?.entries?.mapNotNull { entry ->
                when (entry) {
                    is FileMetadata -> {
                        if (isVideoFile(entry.name)) {
                            CloudFile(
                                id = entry.id,
                                name = entry.name,
                                path = entry.pathLower ?: "",
                                size = entry.size,
                                modifiedTime = entry.clientModified.time,
                                isDirectory = false,
                                mimeType = getMimeType(entry.name),
                                downloadUrl = null // Generated on demand
                            )
                        } else null
                    }
                    is FolderMetadata -> CloudFile(
                        id = entry.id,
                        name = entry.name,
                        path = entry.pathLower ?: "",
                        size = 0,
                        modifiedTime = 0,
                        isDirectory = true,
                        mimeType = "inode/directory",
                        downloadUrl = null
                    )
                    else -> null
                }
            } ?: emptyList()
            
            emit(files)
            
            // Handle pagination
            var hasMore = result?.hasMore ?: false
            var cursor = result?.cursor
            
            while (hasMore && cursor != null) {
                val moreResult = client?.files()?.listFolderContinue(cursor)
                val moreFiles = moreResult?.entries?.mapNotNull { entry ->
                    when (entry) {
                        is FileMetadata -> {
                            if (isVideoFile(entry.name)) {
                                CloudFile(
                                    id = entry.id,
                                    name = entry.name,
                                    path = entry.pathLower ?: "",
                                    size = entry.size,
                                    modifiedTime = entry.clientModified.time,
                                    isDirectory = false,
                                    mimeType = getMimeType(entry.name),
                                    downloadUrl = null
                                )
                            } else null
                        }
                        is FolderMetadata -> CloudFile(
                            id = entry.id,
                            name = entry.name,
                            path = entry.pathLower ?: "",
                            size = 0,
                            modifiedTime = 0,
                            isDirectory = true,
                            mimeType = "inode/directory",
                            downloadUrl = null
                        )
                        else -> null
                    }
                } ?: emptyList()
                
                emit(files + moreFiles)
                hasMore = moreResult?.hasMore ?: false
                cursor = moreResult?.cursor
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to list Dropbox files")
            emit(emptyList())
        }
    }
    
    /**
     * Get download stream for file
     */
    override suspend fun getDownloadStream(fileId: String): InputStream? = withContext(Dispatchers.IO) {
        ensureClient()
        
        try {
            client?.files()?.download(fileId)?.inputStream
        } catch (e: Exception) {
            Timber.e(e, "Failed to get Dropbox download stream")
            null
        }
    }
    
    /**
     * Get streaming URL for file
     */
    override suspend fun getStreamingUrl(fileId: String): String? = withContext(Dispatchers.IO) {
        ensureClient()
        
        try {
            val result = client?.files()?.getTemporaryLink(fileId)
            result?.link
        } catch (e: Exception) {
            Timber.e(e, "Failed to get Dropbox streaming URL")
            null
        }
    }
    
    /**
     * Search for files
     */
    override suspend fun searchFiles(query: String): Flow<List<CloudFile>> = flow {
        ensureClient()
        
        try {
            val searchResult = client?.files()?.searchV2(query)
            val files = searchResult?.matches?.mapNotNull { match ->
                val metadata = match.metadata?.metadataValue
                when (metadata) {
                    is FileMetadata -> {
                        if (isVideoFile(metadata.name)) {
                            CloudFile(
                                id = metadata.id,
                                name = metadata.name,
                                path = metadata.pathLower ?: "",
                                size = metadata.size,
                                modifiedTime = metadata.clientModified.time,
                                isDirectory = false,
                                mimeType = getMimeType(metadata.name),
                                downloadUrl = null
                            )
                        } else null
                    }
                    else -> null
                }
            } ?: emptyList()
            
            emit(files)
        } catch (e: Exception) {
            Timber.e(e, "Failed to search Dropbox files")
            emit(emptyList())
        }
    }
    
    /**
     * Logout from Dropbox
     */
    override suspend fun logout() {
        client = null
        clearSavedCredential()
    }
    
    /**
     * Initialize Dropbox client
     */
    private fun initializeClient(credential: DbxCredential) {
        val config = DbxRequestConfig.newBuilder(CLIENT_IDENTIFIER).build()
        client = DbxClientV2(config, credential)
    }
    
    /**
     * Ensure client is initialized
     */
    private suspend fun ensureClient() {
        if (client == null) {
            val credential = loadSavedCredential()
            if (credential != null) {
                initializeClient(credential)
            } else {
                throw IllegalStateException("Not authenticated with Dropbox")
            }
        }
    }
    
    /**
     * Save credential securely
     */
    private fun saveCredential(credential: DbxCredential) {
        // In production, use encrypted shared preferences
        val prefs = context.getSharedPreferences("dropbox_auth", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("access_token", credential.accessToken)
            .putString("refresh_token", credential.refreshToken)
            .putLong("expires_at", credential.expiresAt)
            .apply()
    }
    
    /**
     * Load saved credential
     */
    private fun loadSavedCredential(): DbxCredential? {
        val prefs = context.getSharedPreferences("dropbox_auth", Context.MODE_PRIVATE)
        val accessToken = prefs.getString("access_token", null) ?: return null
        val refreshToken = prefs.getString("refresh_token", null) ?: return null
        val expiresAt = prefs.getLong("expires_at", 0)
        
        return DbxCredential(accessToken, expiresAt, refreshToken, APP_KEY)
    }
    
    /**
     * Clear saved credential
     */
    private fun clearSavedCredential() {
        val prefs = context.getSharedPreferences("dropbox_auth", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
    
    /**
     * Check if file is video
     */
    private fun isVideoFile(fileName: String): Boolean {
        val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "3gp")
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in videoExtensions
    }
    
    /**
     * Get MIME type for file
     */
    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            else -> "video/*"
        }
    }
}