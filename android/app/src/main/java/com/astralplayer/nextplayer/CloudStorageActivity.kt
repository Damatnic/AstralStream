package com.astralplayer.nextplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.astralplayer.nextplayer.data.cloud.CloudProvider
import com.astralplayer.nextplayer.feature.cloud.CloudStorageManager
import com.astralplayer.nextplayer.feature.cloud.CloudStorageViewModel
import com.astralplayer.nextplayer.ui.screens.CloudStorageScreen
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

class CloudStorageActivity : ComponentActivity() {
    
    private lateinit var cloudStorageManager: CloudStorageManager
    private lateinit var viewModel: CloudStorageViewModel
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.handleGoogleSignInResult(account)
            } catch (e: ApiException) {
                // Handle sign-in failure
            }
        }
    }
    
    private var selectedProvider: CloudProvider? = null
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data?.data != null) {
            val fileUri = result.data!!.data!!
            selectedProvider?.let { provider ->
                viewModel.uploadFile(provider, fileUri)
            }
        }
        selectedProvider = null
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize cloud storage manager
        cloudStorageManager = CloudStorageManager(this)
        viewModel = CloudStorageViewModel(cloudStorageManager)
        
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val accounts by viewModel.connectedAccounts.collectAsState()
                    val files by viewModel.cloudFiles.collectAsState()
                    val syncStatus by viewModel.syncStatus.collectAsState()
                    val cloudError by viewModel.cloudError.collectAsState()
                    
                    CloudStorageScreen(
                        accounts = accounts,
                        files = files,
                        syncStatus = syncStatus,
                        cloudError = cloudError,
                        onConnectAccount = { provider ->
                            viewModel.connectAccount(provider, this)
                        },
                        onDisconnectAccount = { accountId ->
                            viewModel.disconnectAccount(accountId)
                        },
                        onSyncFiles = { provider ->
                            viewModel.syncFiles(provider)
                        },
                        onDownloadFile = { file ->
                            // Download to app's private storage
                            val localPath = "${filesDir.absolutePath}/${file.name}"
                            viewModel.downloadFile(file, localPath)
                        },
                        onUploadFile = { provider ->
                            // Launch file picker for upload
                            val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                                type = "video/*"
                                putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "audio/*"))
                                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                            }
                            try {
                                filePickerLauncher.launch(intent)
                                selectedProvider = provider
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    this@CloudStorageActivity,
                                    "No file manager app found",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onClearError = {
                            viewModel.clearError()
                        },
                        onNavigateBack = {
                            finish()
                        }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Handle Dropbox authentication callback
        viewModel.handleDropboxAuthentication()
    }
}