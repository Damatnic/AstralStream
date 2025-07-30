package com.astralplayer.nextplayer.feature.cloud

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections

/**
 * Service for Google Drive integration
 */
class GoogleDriveService(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleDriveService"
        private const val VIDEO_MIME_TYPES = "mimeType='video/mp4' or mimeType='video/webm' or mimeType='video/x-msvideo' or mimeType='video/quicktime'"
        private const val FIELDS = "files(id, name, mimeType, size, modifiedTime, webContentLink, thumbnailLink, parents)"
    }
    
    private var driveService: Drive? = null
    private var signInAccount: GoogleSignInAccount? = null
    
    /**
     * Initialize Google Sign-In client
     */
    fun getSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .build()
            
        return GoogleSignIn.getClient(context, signInOptions)
    }
    
    /**
     * Set up Drive service with signed-in account
     */
    suspend fun setupDriveService(account: GoogleSignInAccount): Boolean = withContext(Dispatchers.IO) {
        try {
            signInAccount = account
            
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_READONLY)
            )
            credential.selectedAccount = account.account
            
            driveService = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Astral Player")
                .build()
                
            Log.d(TAG, "Drive service initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup Drive service", e)
            false
        }
    }
    
    /**
     * Get account info
     */
    fun getAccountInfo(): CloudAccount? {
        val account = signInAccount ?: return null
        
        return CloudAccount(
            id = "gdrive_${account.id}",
            provider = CloudProvider.GOOGLE_DRIVE,
            email = account.email ?: "",
            displayName = account.displayName ?: "Google Drive",
            isConnected = true,
            lastSync = System.currentTimeMillis()
        )
    }
    
    /**
     * List video files from Google Drive
     */
    suspend fun listVideoFiles(pageToken: String? = null): List<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext emptyList()
            
            val result: FileList = service.files().list()
                .setQ(VIDEO_MIME_TYPES)
                .setSpaces("drive")
                .setFields(FIELDS)
                .setPageToken(pageToken)
                .setPageSize(100)
                .execute()
                
            result.files.mapNotNull { file ->
                convertToCloudFile(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list files", e)
            emptyList()
        }
    }
    
    /**
     * Search for files
     */
    suspend fun searchFiles(query: String): List<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext emptyList()
            
            val searchQuery = "name contains '$query' and ($VIDEO_MIME_TYPES)"
            
            val result: FileList = service.files().list()
                .setQ(searchQuery)
                .setSpaces("drive")
                .setFields(FIELDS)
                .setPageSize(50)
                .execute()
                
            result.files.mapNotNull { file ->
                convertToCloudFile(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search files", e)
            emptyList()
        }
    }
    
    /**
     * Download file from Google Drive
     */
    suspend fun downloadFile(fileId: String, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext false
            
            val outputStream = ByteArrayOutputStream()
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            
            // Write to file
            FileOutputStream(outputPath).use { fileOutputStream ->
                outputStream.writeTo(fileOutputStream)
            }
            
            Log.d(TAG, "File downloaded successfully: $outputPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download file", e)
            false
        }
    }
    
    /**
     * Get file metadata
     */
    suspend fun getFileMetadata(fileId: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            
            val file = service.files().get(fileId)
                .setFields("id, name, mimeType, size, modifiedTime, webContentLink, thumbnailLink")
                .execute()
                
            convertToCloudFile(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file metadata", e)
            null
        }
    }
    
    /**
     * Convert Google Drive File to CloudFile
     */
    private fun convertToCloudFile(file: File): CloudFile? {
        return try {
            CloudFile(
                id = file.id,
                name = file.name,
                path = "/${file.name}",
                size = file.getSize()?.toLong() ?: 0L,
                mimeType = file.mimeType,
                modifiedTime = file.modifiedTime?.value ?: System.currentTimeMillis(),
                isFolder = file.mimeType == "application/vnd.google-apps.folder",
                provider = CloudProvider.GOOGLE_DRIVE,
                downloadUrl = file.webContentLink,
                thumbnailUrl = file.thumbnailLink
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert file: ${file.name}", e)
            null
        }
    }
    
    /**
     * Check if service is connected
     */
    fun isConnected(): Boolean {
        return driveService != null && signInAccount != null
    }
    
    /**
     * Disconnect from Google Drive
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            driveService = null
            signInAccount = null
            
            // Sign out from Google
            getSignInClient().signOut()
            
            Log.d(TAG, "Disconnected from Google Drive")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect", e)
        }
    }
}