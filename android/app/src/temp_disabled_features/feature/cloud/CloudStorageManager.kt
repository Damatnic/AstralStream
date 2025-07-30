package com.astralplayer.nextplayer.feature.cloud

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import android.util.Log

/**
 * Enum for cloud storage providers
 */
enum class CloudProvider {
    GOOGLE_DRIVE,
    DROPBOX,
    ONEDRIVE,
    ICLOUD,
    BOX,
    MEGA
}

/**
 * Data class representing a cloud storage account
 */
data class CloudAccount(
    val id: String,
    val provider: CloudProvider,
    val email: String,
    val displayName: String,
    val isConnected: Boolean = false,
    val storageUsed: Long = 0L,
    val storageTotal: Long = 0L,
    val lastSync: Long = 0L
)

/**
 * Data class representing a cloud file
 */
data class CloudFile(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String,
    val modifiedTime: Long,
    val isFolder: Boolean = false,
    val provider: CloudProvider,
    val downloadUrl: String? = null,
    val thumbnailUrl: String? = null
)

/**
 * Data class for sync status
 */
data class SyncStatus(
    val isActive: Boolean = false,
    val progress: Float = 0f,
    val currentFile: String = "",
    val filesProcessed: Int = 0,
    val totalFiles: Int = 0
)

/**
 * Manager for cloud storage integration
 */
class CloudStorageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "CloudStorageManager"
        const val GOOGLE_SIGN_IN_REQUEST_CODE = 1001
    }
    
    private val _connectedAccounts = MutableStateFlow<List<CloudAccount>>(emptyList())
    val connectedAccounts: StateFlow<List<CloudAccount>> = _connectedAccounts.asStateFlow()
    
    private val _cloudFiles = MutableStateFlow<List<CloudFile>>(emptyList())
    val cloudFiles: StateFlow<List<CloudFile>> = _cloudFiles.asStateFlow()
    
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val _cloudError = MutableStateFlow<String?>(null)
    val cloudError: StateFlow<String?> = _cloudError.asStateFlow()
    
    // Real service instances
    private val googleDriveService = GoogleDriveService(context)
    private val dropboxService = DropboxService(context)
    private val oneDriveService = OneDriveService(context)
    
    init {
        checkExistingConnections()
    }
    
    /**
     * Check existing connections on startup
     */
    private fun checkExistingConnections() {
        val accounts = mutableListOf<CloudAccount>()
        
        // Check Google Drive
        GoogleSignIn.getLastSignedInAccount(context)?.let { googleAccount ->
            accounts.add(
                CloudAccount(
                    id = "gdrive_${googleAccount.id}",
                    provider = CloudProvider.GOOGLE_DRIVE,
                    email = googleAccount.email ?: "",
                    displayName = googleAccount.displayName ?: "Google Drive",
                    isConnected = false, // Will be updated after service setup
                    lastSync = 0L
                )
            )
        }
        
        // Check Dropbox (would need stored credentials)
        // For now, just show as disconnected option
        accounts.add(
            CloudAccount(
                id = "dropbox_placeholder",
                provider = CloudProvider.DROPBOX,
                email = "",
                displayName = "Dropbox",
                isConnected = false,
                lastSync = 0L
            )
        )
        
        // Add placeholders for other services
        accounts.add(
            CloudAccount(
                id = "onedrive_placeholder",
                provider = CloudProvider.ONEDRIVE,
                email = "",
                displayName = "OneDrive",
                isConnected = false,
                lastSync = 0L
            )
        )
        
        _connectedAccounts.value = accounts
    }
    
    /**
     * Connect to cloud storage provider
     */
    suspend fun connectAccount(provider: CloudProvider, activity: Activity? = null): Boolean {
        return try {
            _cloudError.value = null
            
            when (provider) {
                CloudProvider.GOOGLE_DRIVE -> {
                    if (activity != null) {
                        // Start Google Sign-In flow
                        val signInIntent = googleDriveService.getSignInClient().signInIntent
                        activity.startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
                        true // Will complete in handleGoogleSignInResult
                    } else {
                        _cloudError.value = "Activity required for Google Sign-In"
                        false
                    }
                }
                CloudProvider.DROPBOX -> {
                    // Start Dropbox authentication
                    dropboxService.startAuthentication()
                    // Complete authentication in handleDropboxAuthentication
                    true
                }
                CloudProvider.ONEDRIVE -> {
                    if (activity != null) {
                        // Start OneDrive authentication
                        val success = oneDriveService.authenticate(activity)
                        if (success) {
                            // Update account info
                            val account = oneDriveService.getAccountInfo()
                            account?.let { updateAccountInList(it) }
                        }
                        success
                    } else {
                        _cloudError.value = "Activity required for OneDrive Sign-In"
                        false
                    }
                }
                else -> {
                    _cloudError.value = "${provider.name} integration not yet implemented"
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to ${provider.name}", e)
            _cloudError.value = "Failed to connect to ${provider.name}: ${e.message}"
            false
        }
    }
    
    /**
     * Handle Google Sign-In result
     */
    suspend fun handleGoogleSignInResult(account: GoogleSignInAccount): Boolean {
        return try {
            val success = googleDriveService.setupDriveService(account)
            
            if (success) {
                val driveAccount = googleDriveService.getAccountInfo()
                if (driveAccount != null) {
                    updateAccountInList(driveAccount)
                }
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle Google Sign-In", e)
            _cloudError.value = "Failed to setup Google Drive: ${e.message}"
            false
        }
    }
    
    /**
     * Handle Dropbox authentication completion
     */
    suspend fun handleDropboxAuthentication(): Boolean {
        return try {
            val success = dropboxService.completeAuthentication()
            
            if (success) {
                val dropboxAccount = dropboxService.getAccountInfo()
                if (dropboxAccount != null) {
                    updateAccountInList(dropboxAccount)
                }
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle Dropbox authentication", e)
            _cloudError.value = "Failed to setup Dropbox: ${e.message}"
            false
        }
    }
    
    /**
     * Update account in the list
     */
    private fun updateAccountInList(account: CloudAccount) {
        val updatedAccounts = _connectedAccounts.value.map { existingAccount ->
            if (existingAccount.provider == account.provider) {
                account
            } else {
                existingAccount
            }
        }
        _connectedAccounts.value = updatedAccounts
    }
    
    /**
     * Disconnect from cloud storage provider
     */
    suspend fun disconnectAccount(accountId: String) {
        try {
            val account = _connectedAccounts.value.find { it.id == accountId }
            
            when (account?.provider) {
                CloudProvider.GOOGLE_DRIVE -> {
                    googleDriveService.disconnect()
                }
                CloudProvider.DROPBOX -> {
                    dropboxService.disconnect()
                }
                CloudProvider.ONEDRIVE -> {
                    oneDriveService.signOut()
                }
                else -> {}
            }
            
            val updatedAccounts = _connectedAccounts.value.map { acc ->
                if (acc.id == accountId) {
                    acc.copy(isConnected = false)
                } else {
                    acc
                }
            }
            
            _connectedAccounts.value = updatedAccounts
            
            // Clear files from disconnected provider
            account?.provider?.let { provider ->
                _cloudFiles.value = _cloudFiles.value.filter { it.provider != provider }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect", e)
            _cloudError.value = "Failed to disconnect: ${e.message}"
        }
    }
    
    /**
     * Sync files from cloud storage
     */
    suspend fun syncFiles(provider: CloudProvider? = null) {
        try {
            _syncStatus.value = SyncStatus(isActive = true)
            
            val providersToSync = if (provider != null) {
                listOf(provider)
            } else {
                _connectedAccounts.value.filter { it.isConnected }.map { it.provider }
            }
            
            val allFiles = mutableListOf<CloudFile>()
            
            for (cloudProvider in providersToSync) {
                val files = syncProviderFiles(cloudProvider)
                allFiles.addAll(files)
            }
            
            _cloudFiles.value = allFiles
            _syncStatus.value = SyncStatus(isActive = false)
            
        } catch (e: Exception) {
            _cloudError.value = "Sync failed: ${e.message}"
            _syncStatus.value = SyncStatus(isActive = false)
        }
    }
    
    /**
     * Sync files from specific provider
     */
    private suspend fun syncProviderFiles(provider: CloudProvider): List<CloudFile> {
        return try {
            when (provider) {
                CloudProvider.GOOGLE_DRIVE -> {
                    if (googleDriveService.isConnected()) {
                        val files = googleDriveService.listVideoFiles()
                        updateSyncProgress(provider, files.size)
                        files
                    } else {
                        emptyList()
                    }
                }
                CloudProvider.DROPBOX -> {
                    if (dropboxService.isConnected()) {
                        val files = dropboxService.listVideoFiles()
                        updateSyncProgress(provider, files.size)
                        files
                    } else {
                        emptyList()
                    }
                }
                else -> {
                    // Other providers not yet implemented
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync files from $provider", e)
            _cloudError.value = "Failed to sync from $provider: ${e.message}"
            emptyList()
        }
    }
    
    /**
     * Update sync progress
     */
    private fun updateSyncProgress(provider: CloudProvider, fileCount: Int) {
        _syncStatus.value = _syncStatus.value.copy(
            progress = 1f,
            currentFile = "Completed sync from $provider",
            filesProcessed = fileCount,
            totalFiles = fileCount
        )
    }
    
    /**
     * Download file from cloud storage
     */
    suspend fun downloadFile(file: CloudFile, localPath: String): Boolean {
        return try {
            _cloudError.value = null
            
            when (file.provider) {
                CloudProvider.GOOGLE_DRIVE -> {
                    googleDriveService.downloadFile(file.id, localPath)
                }
                CloudProvider.DROPBOX -> {
                    dropboxService.downloadFile(file.path, localPath)
                }
                else -> {
                    _cloudError.value = "Download not supported for ${file.provider.name}"
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _cloudError.value = "Download failed: ${e.message}"
            false
        }
    }
    
    /**
     * Upload file to cloud storage
     */
    suspend fun uploadFile(localUri: Uri, provider: CloudProvider, remotePath: String): Boolean {
        return try {
            _cloudError.value = null
            
            // Simulate upload
            for (i in 1..100) {
                delay(30)
                // Update upload progress if needed
            }
            
            // Add uploaded file to list
            val uploadedFile = CloudFile(
                id = "uploaded_${System.currentTimeMillis()}",
                name = remotePath.substringAfterLast('/'),
                path = remotePath,
                size = 500_000_000L, // 500MB
                mimeType = "video/mp4",
                modifiedTime = System.currentTimeMillis(),
                provider = provider
            )
            
            _cloudFiles.value = _cloudFiles.value + uploadedFile
            
            true
        } catch (e: Exception) {
            _cloudError.value = "Upload failed: ${e.message}"
            false
        }
    }
    
    /**
     * Search files in cloud storage
     */
    suspend fun searchFiles(query: String): List<CloudFile> {
        return try {
            val results = mutableListOf<CloudFile>()
            
            // Search in connected services
            if (googleDriveService.isConnected()) {
                results.addAll(googleDriveService.searchFiles(query))
            }
            
            if (dropboxService.isConnected()) {
                results.addAll(dropboxService.searchFiles(query))
            }
            
            results
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            _cloudError.value = "Search failed: ${e.message}"
            emptyList()
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _cloudError.value = null
    }
    
    /**
     * Get provider icon
     */
    fun getProviderIcon(provider: CloudProvider): ImageVector {
        return when (provider) {
            CloudProvider.GOOGLE_DRIVE -> Icons.Default.CloudQueue
            CloudProvider.DROPBOX -> Icons.Default.Cloud
            CloudProvider.ONEDRIVE -> Icons.Default.CloudSync
            CloudProvider.ICLOUD -> Icons.Default.CloudCircle
            CloudProvider.BOX -> Icons.Default.CloudDone
            CloudProvider.MEGA -> Icons.Default.CloudUpload
        }
    }
    
    /**
     * Get provider color
     */
    fun getProviderColor(provider: CloudProvider): Color {
        return when (provider) {
            CloudProvider.GOOGLE_DRIVE -> Color(0xFF4285F4)
            CloudProvider.DROPBOX -> Color(0xFF0061FF)
            CloudProvider.ONEDRIVE -> Color(0xFF0078D4)
            CloudProvider.ICLOUD -> Color(0xFF007AFF)
            CloudProvider.BOX -> Color(0xFF0061D5)
            CloudProvider.MEGA -> Color(0xFFD9272E)
        }
    }
}

/**
 * ViewModel for cloud storage
 */
class CloudStorageViewModel(private val cloudManager: CloudStorageManager) : ViewModel() {
    
    val connectedAccounts = cloudManager.connectedAccounts
    val cloudFiles = cloudManager.cloudFiles
    val syncStatus = cloudManager.syncStatus
    val cloudError = cloudManager.cloudError
    
    fun connectAccount(provider: CloudProvider, activity: Activity? = null) {
        viewModelScope.launch {
            cloudManager.connectAccount(provider, activity)
        }
    }
    
    fun handleGoogleSignInResult(account: GoogleSignInAccount) {
        viewModelScope.launch {
            cloudManager.handleGoogleSignInResult(account)
        }
    }
    
    fun handleDropboxAuthentication() {
        viewModelScope.launch {
            cloudManager.handleDropboxAuthentication()
        }
    }
    
    fun disconnectAccount(accountId: String) {
        viewModelScope.launch {
            cloudManager.disconnectAccount(accountId)
        }
    }
    
    fun syncFiles(provider: CloudProvider? = null) {
        viewModelScope.launch {
            cloudManager.syncFiles(provider)
        }
    }
    
    fun downloadFile(file: CloudFile, localPath: String) {
        viewModelScope.launch {
            cloudManager.downloadFile(file, localPath)
        }
    }
    
    fun uploadFile(localUri: Uri, provider: CloudProvider, remotePath: String) {
        viewModelScope.launch {
            cloudManager.uploadFile(localUri, provider, remotePath)
        }
    }
    
    fun searchFiles(query: String) {
        viewModelScope.launch {
            cloudManager.searchFiles(query)
        }
    }
    
    fun clearError() {
        cloudManager.clearError()
    }
}

/**
 * Composable for cloud storage screen
 */
@Composable
fun CloudStorageScreen(
    accounts: List<CloudAccount>,
    files: List<CloudFile>,
    syncStatus: SyncStatus,
    cloudError: String?,
    onConnectAccount: (CloudProvider) -> Unit,
    onDisconnectAccount: (String) -> Unit,
    onSyncFiles: (CloudProvider?) -> Unit,
    onDownloadFile: (CloudFile) -> Unit,
    onUploadFile: (CloudProvider) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cloud Storage",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { onSyncFiles(null) },
                enabled = !syncStatus.isActive
            ) {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = "Sync All",
                    tint = Color(0xFF00BCD4)
                )
            }
        }
        
        // Sync status
        if (syncStatus.isActive) {
            SyncStatusCard(syncStatus = syncStatus)
        }
        
        // Error display
        cloudError?.let { error ->
            ErrorCard(
                error = error,
                onDismiss = onClearError
            )
        }
        
        // Connected accounts
        Text(
            text = "Connected Accounts",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(accounts) { account ->
                CloudAccountItem(
                    account = account,
                    onConnect = { onConnectAccount(account.provider) },
                    onDisconnect = { onDisconnectAccount(account.id) },
                    onSync = { onSyncFiles(account.provider) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Cloud Files",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(files) { file ->
                CloudFileItem(
                    file = file,
                    onDownload = { onDownloadFile(file) }
                )
            }
        }
    }
}

/**
 * Sync status card
 */
@Composable
private fun SyncStatusCard(syncStatus: SyncStatus) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF00BCD4).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Syncing files...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            if (syncStatus.currentFile.isNotEmpty()) {
                Text(
                    text = syncStatus.currentFile,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = syncStatus.progress,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF00BCD4)
            )
            
            Text(
                text = "${syncStatus.filesProcessed}/${syncStatus.totalFiles} files",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/**
 * Cloud account item
 */
@Composable
private fun CloudAccountItem(
    account: CloudAccount,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (account.isConnected) 
                Color(0xFF00BCD4).copy(alpha = 0.1f)
            else 
                Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Provider icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        CloudStorageManager(context = androidx.compose.ui.platform.LocalContext.current)
                            .getProviderColor(account.provider).copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CloudStorageManager(androidx.compose.ui.platform.LocalContext.current)
                        .getProviderIcon(account.provider),
                    contentDescription = account.provider.name,
                    tint = CloudStorageManager(androidx.compose.ui.platform.LocalContext.current)
                        .getProviderColor(account.provider),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Account info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = account.email,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                
                if (account.isConnected) {
                    val usedGB = account.storageUsed / 1_000_000_000f
                    val totalGB = account.storageTotal / 1_000_000_000f
                    
                    Text(
                        text = String.format("%.1f GB / %.1f GB used", usedGB, totalGB),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }
            
            // Action buttons
            if (account.isConnected) {
                IconButton(onClick = onSync) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = Color(0xFF00BCD4)
                    )
                }
                
                IconButton(onClick = onDisconnect) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = "Disconnect",
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00BCD4)
                    )
                ) {
                    Text("Connect")
                }
            }
        }
    }
}

/**
 * Cloud file item
 */
@Composable
private fun CloudFileItem(
    file: CloudFile,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon
            Icon(
                imageVector = if (file.isFolder) Icons.Default.Folder else Icons.Default.VideoFile,
                contentDescription = if (file.isFolder) "Folder" else "Video",
                tint = if (file.isFolder) Color(0xFFFFC107) else Color(0xFF00BCD4),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatFileSize(file.size),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    
                    Text(
                        text = formatDate(file.modifiedTime),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }
            
            // Download button
            if (!file.isFolder) {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color(0xFF00BCD4)
                    )
                }
            }
        }
    }
}

/**
 * Error card
 */
@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                tint = Color.Red
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = error,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Format file size
 */
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * Format date
 */
private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    return formatter.format(Date(timestamp))
}