package com.astralplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astralplayer.download.DownloadStatus
import com.astralplayer.download.DownloadTask
import com.astralplayer.download.StorageInfo
import com.astralplayer.ui.theme.AstralStreamTheme
import com.astralplayer.viewmodel.DownloadsViewModel
import java.text.DecimalFormat
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.hasCompletedDownloads) {
                        TextButton(onClick = { viewModel.clearCompleted() }) {
                            Text("Clear Completed")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Storage info
            StorageInfoCard(
                storageInfo = uiState.storageInfo,
                modifier = Modifier.padding(16.dp)
            )
            
            // Downloads list
            AnimatedContent(
                targetState = uiState.downloads.isEmpty(),
                label = "downloads_content"
            ) { isEmpty ->
                if (isEmpty) {
                    EmptyDownloadsState(
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.downloads,
                            key = { it.id }
                        ) { task ->
                            DownloadItem(
                                task = task,
                                progress = uiState.activeDownloads[task.id],
                                onPause = { viewModel.pauseDownload(task.id) },
                                onResume = { viewModel.resumeDownload(task.id) },
                                onCancel = { viewModel.cancelDownload(task.id) },
                                onPlay = { task.filePath?.let(onPlayVideo) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageInfoCard(
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Storage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatBytes(storageInfo.usedSpace)} / ${formatBytes(storageInfo.totalSpace)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(60.dp)
                ) {
                    CircularProgressIndicator(
                        progress = (storageInfo.usedSpace.toFloat() / storageInfo.totalSpace),
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 6.dp
                    )
                    Text(
                        text = "${((storageInfo.usedSpace.toFloat() / storageInfo.totalSpace) * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = storageInfo.usedSpace.toFloat() / storageInfo.totalSpace,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${storageInfo.downloadCount} videos",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${formatBytes(storageInfo.freeSpace)} free",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DownloadItem(
    task: DownloadTask,
    progress: com.astralplayer.download.DownloadProgress?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.metadata.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip(
                            onClick = { },
                            colors = ChipDefaults.chipColors(
                                containerColor = getStatusColor(task.status)
                            ),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(
                                text = task.status.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        
                        if (task.status == DownloadStatus.DOWNLOADING && progress != null) {
                            Text(
                                text = "${formatBytes(progress.downloadSpeed)}/s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                DownloadActions(
                    status = task.status,
                    onPause = onPause,
                    onResume = onResume,
                    onCancel = onCancel,
                    onPlay = onPlay
                )
            }
            
            if (task.status == DownloadStatus.DOWNLOADING && progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Column {
                    LinearProgressIndicator(
                        progress = progress.progress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${progress.progress}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "${formatBytes(progress.downloadedBytes)} / ${formatBytes(progress.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadActions(
    status: DownloadStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (status) {
            DownloadStatus.DOWNLOADING -> {
                IconButton(onClick = onPause) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                }
            }
            DownloadStatus.PAUSED, DownloadStatus.QUEUED -> {
                IconButton(onClick = onResume) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            DownloadStatus.COMPLETED -> {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayCircle, contentDescription = "Play")
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            DownloadStatus.FAILED -> {
                IconButton(onClick = onResume) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun EmptyDownloadsState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "No downloads yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Downloaded videos will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getStatusColor(status: DownloadStatus): Color {
    return when (status) {
        DownloadStatus.DOWNLOADING -> Color(0xFF4CAF50)
        DownloadStatus.COMPLETED -> Color(0xFF2196F3)
        DownloadStatus.FAILED -> Color(0xFFF44336)
        DownloadStatus.PAUSED -> Color(0xFFFF9800)
        else -> Color.Gray
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    
    return DecimalFormat("#,##0.#").format(
        bytes / Math.pow(1024.0, digitGroups.toDouble())
    ) + " " + units[digitGroups]
}