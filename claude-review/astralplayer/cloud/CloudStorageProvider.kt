package com.astralplayer.cloud

import kotlinx.coroutines.flow.Flow
import java.io.InputStream

/**
 * Common interface for all cloud storage providers
 */
interface CloudStorageProvider {
    val providerName: String
    val providerId: String
    
    suspend fun authenticate(): Boolean
    suspend fun isAuthenticated(): Boolean
    suspend fun listFiles(path: String = ""): Flow<List<CloudFile>>
    suspend fun getDownloadStream(fileId: String): InputStream?
    suspend fun getStreamingUrl(fileId: String): String?
    suspend fun searchFiles(query: String): Flow<List<CloudFile>>
    suspend fun logout()
}

/**
 * Cloud file representation
 */
data class CloudFile(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val modifiedTime: Long,
    val isDirectory: Boolean,
    val mimeType: String,
    val downloadUrl: String?
)