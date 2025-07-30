package com.astralplayer.nextplayer.feature.cloud

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.astralplayer.nextplayer.data.cloud.CloudProvider
import kotlinx.coroutines.launch

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
    
    fun uploadFile(provider: CloudProvider, fileUri: Uri) {
        viewModelScope.launch {
            // Extract filename from URI for remote path
            val fileName = fileUri.lastPathSegment ?: "uploaded_file"
            cloudManager.uploadFile(fileUri, provider, fileName)
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