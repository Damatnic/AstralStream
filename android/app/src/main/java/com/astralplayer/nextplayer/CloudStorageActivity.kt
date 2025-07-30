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
import com.astralplayer.nextplayer.viewmodel.CloudStorageViewModel
import androidx.activity.viewModels
import com.astralplayer.nextplayer.ui.screens.CloudStorageScreen
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
class CloudStorageActivity : ComponentActivity() {
    
    private val viewModel: CloudStorageViewModel by viewModels()
    
    // Launcher for file picker
    private var selectedProvider: CloudProvider? = null
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedProvider?.let { provider ->
                // TODO: Implement file upload
            }
        }
        selectedProvider = null
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ViewModel is initialized by Hilt
        
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val accounts by viewModel.connectedAccounts.collectAsState()
                    val files by viewModel.cloudFiles.collectAsState()
                    val syncStatus by viewModel.syncStatus.collectAsState()
                    
                    CloudStorageScreen(
                        accounts = accounts,
                        files = files,
                        syncStatus = syncStatus,
                        cloudError = null,
                        onConnectAccount = { provider ->
                            viewModel.connectProvider(provider)
                        },
                        onDisconnectAccount = { accountId ->
                            viewModel.disconnectProvider(accountId)
                        },
                        onSyncFiles = { provider ->
                            viewModel.syncFiles()
                        },
                        onDownloadFile = { file ->
                            // TODO: Implement file download
                        },
                        onUploadFile = { provider ->
                            selectedProvider = provider
                            filePickerLauncher.launch("video/*")
                        },
                        onClearError = {
                            // TODO: Implement error clearing
                        },
                        onNavigateBack = {
                            finish()
                        }
                    )
                }
            }
        }
    }
    
}