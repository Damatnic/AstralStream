package com.astralplayer.nextplayer.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoFile
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
import com.astralplayer.nextplayer.data.cloud.CloudProvider
import com.astralplayer.nextplayer.data.cloud.CloudAccount
import com.astralplayer.nextplayer.data.cloud.CloudFile
import com.astralplayer.nextplayer.data.cloud.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Storage") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onSyncFiles(null) },
                        enabled = syncStatus != SyncStatus.SYNCING
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sync All",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Sync status
            if (syncStatus == SyncStatus.SYNCING) {
                SyncStatusCard(syncStatus = syncStatus)
            }
            
            // Error display
            cloudError?.let { error ->
                ErrorCard(
                    error = error,
                    onDismiss = onClearError
                )
            }
            
            // Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connected accounts section
                item {
                    Text(
                        text = "Connected Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(accounts) { account ->
                    CloudAccountItem(
                        account = account,
                        onConnect = { onConnectAccount(account.provider) },
                        onDisconnect = { onDisconnectAccount(account.id) },
                        onSync = { onSyncFiles(account.provider) }
                    )
                }
                
                // Cloud files section
                if (files.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cloud Files",
                            style = MaterialTheme.typography.titleMedium,
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
    }
}

@Composable
private fun SyncStatusCard(syncStatus: SyncStatus) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = when (syncStatus) {
                    SyncStatus.SYNCING -> "Syncing files..."
                    SyncStatus.COMPLETED -> "Sync completed"
                    SyncStatus.FAILED -> "Sync failed"
                    SyncStatus.PAUSED -> "Sync paused"
                    SyncStatus.CANCELLED -> "Sync cancelled"
                    else -> "Ready to sync"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

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
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
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
                        getProviderColor(account.provider).copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getProviderIcon(account.provider),
                    contentDescription = account.provider.name,
                    tint = getProviderColor(account.provider),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Account info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                if (account.email.isNotEmpty()) {
                    Text(
                        text = account.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (account.isConnected && account.storageTotal > 0) {
                    val usedGB = account.storageUsed / 1_000_000_000f
                    val totalGB = account.storageTotal / 1_000_000_000f
                    
                    Text(
                        text = String.format("%.1f GB / %.1f GB used", usedGB, totalGB),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Action buttons
            if (account.isConnected) {
                IconButton(onClick = onSync) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = onDisconnect) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = "Disconnect",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Connect")
                }
            }
        }
    }
}

@Composable
private fun CloudFileItem(
    file: CloudFile,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon
            Icon(
                imageVector = if (file.isVideo) Icons.Default.VideoFile else Icons.Default.InsertDriveFile,
                contentDescription = if (file.isVideo) "Video" else "File",
                tint = if (file.isVideo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = formatDate(file.modifiedTime.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Download button
            if (file.isVideo) {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

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
            containerColor = MaterialTheme.colorScheme.errorContainer
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
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun getProviderIcon(provider: CloudProvider): ImageVector {
    return when (provider) {
        CloudProvider.GOOGLE_DRIVE -> Icons.Default.CloudQueue
        CloudProvider.DROPBOX -> Icons.Default.Cloud
        CloudProvider.ONEDRIVE -> Icons.Default.CloudSync
        CloudProvider.ICLOUD -> Icons.Default.CloudCircle
        CloudProvider.MEGA -> Icons.Default.CloudUpload
        CloudProvider.PCLOUD -> Icons.Default.CloudDone
        else -> Icons.Default.Cloud
    }
}

private fun getProviderColor(provider: CloudProvider): Color {
    return when (provider) {
        CloudProvider.GOOGLE_DRIVE -> Color(0xFF4285F4)
        CloudProvider.DROPBOX -> Color(0xFF0061FF)
        CloudProvider.ONEDRIVE -> Color(0xFF0078D4)
        CloudProvider.ICLOUD -> Color(0xFF007AFF)
        CloudProvider.MEGA -> Color(0xFFD9272E)
        CloudProvider.PCLOUD -> Color(0xFF00BCD4)
        else -> Color(0xFF9E9E9E)
    }
}

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

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    return formatter.format(Date(timestamp))
}