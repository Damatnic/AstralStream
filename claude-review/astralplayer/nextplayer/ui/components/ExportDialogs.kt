package com.astralplayer.nextplayer.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.AstralVuApplication
import com.astralplayer.nextplayer.data.database.PlaylistEntity
import com.astralplayer.nextplayer.data.database.PlaylistItemEntity
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.utils.ExportFormat
import com.astralplayer.nextplayer.utils.ExportManager
import com.astralplayer.nextplayer.utils.ExportResult
import com.astralplayer.nextplayer.utils.ExportType
import com.astralplayer.nextplayer.utils.PlaylistItem
import kotlinx.coroutines.launch

/**
 * Dialog for exporting video statistics with format selection
 */
@Composable
fun VideoStatsExportDialog(
    videoStats: VideoStats,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.TXT) }
    var isExporting by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val exportManager = remember { ExportManager.getInstance(context) }
    
    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null
                )
                Text("Export Video Statistics")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose export format:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // Format selection
                ExportFormatSelector(
                    formats = listOf(
                        ExportFormat.TXT,
                        ExportFormat.JSON,
                        ExportFormat.CSV,
                        ExportFormat.HTML
                    ),
                    selectedFormat = selectedFormat,
                    onFormatSelected = { selectedFormat = it },
                    enabled = !isExporting
                )
                
                // Export result display
                exportResult?.let { result ->
                    when (result) {
                        is ExportResult.Success -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Export completed successfully!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        is ExportResult.Error -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = result.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (isExporting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Exporting...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick share button (text only)
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val content = when (selectedFormat) {
                                    ExportFormat.TXT -> exportManager.exportVideoStats(videoStats, ExportFormat.TXT)
                                    else -> exportManager.exportVideoStats(videoStats, ExportFormat.TXT)
                                }
                                
                                if (content is ExportResult.Success) {
                                    val shareIntent = exportManager.getQuickShareIntent(
                                        content = "", // We'll read from file
                                        type = ExportType.VIDEO_STATS
                                    )
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Video Statistics"))
                                }
                            } catch (e: Exception) {
                                exportResult = ExportResult.Error("Failed to share: ${e.message}")
                            }
                        }
                    },
                    enabled = !isExporting
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
                
                // Export to file button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isExporting = true
                            try {
                                val result = exportManager.exportVideoStats(videoStats, selectedFormat)
                                exportResult = result
                                
                                if (result is ExportResult.Success) {
                                    // Share the exported file
                                    val shareIntent = exportManager.shareExportedFile(result)
                                    shareIntent?.let { intent ->
                                        context.startActivity(Intent.createChooser(intent, "Share Video Statistics"))
                                    }
                                }
                            } catch (e: Exception) {
                                exportResult = ExportResult.Error("Export failed: ${e.message}")
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

/**
 * Dialog for exporting playlists with format selection
 */
@Composable
fun PlaylistExportDialog(
    playlist: PlaylistEntity,
    playlistItems: List<PlaylistItemEntity>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.M3U) }
    var isExporting by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val exportManager = remember { ExportManager.getInstance(context) }
    
    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null
                )
                Text("Export Playlist")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Playlist info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${playlistItems.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (playlist.description.isNotBlank()) {
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Text(
                    text = "Choose export format:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // Format selection
                ExportFormatSelector(
                    formats = listOf(
                        ExportFormat.M3U,
                        ExportFormat.JSON,
                        ExportFormat.TXT
                    ),
                    selectedFormat = selectedFormat,
                    onFormatSelected = { selectedFormat = it },
                    enabled = !isExporting
                )
                
                // Export result display
                exportResult?.let { result ->
                    when (result) {
                        is ExportResult.Success -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Playlist exported successfully!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        is ExportResult.Error -> {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = result.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (isExporting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Exporting playlist...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isExporting = true
                        try {
                            // Convert PlaylistItemEntity to PlaylistItem
                            val application = context.applicationContext as AstralVuApplication
                            val database = application.database
                            val convertedItems = playlistItems.map { entity ->
                                val recentFile = database.recentFilesDao().getRecentFileById(entity.fileId)
                                PlaylistItem(
                                    title = recentFile?.title ?: "Unknown Video",
                                    videoPath = recentFile?.uri ?: "",
                                    duration = recentFile?.duration ?: 0L,
                                    position = entity.position
                                )
                            }
                            val result = exportManager.exportPlaylist(playlist, convertedItems, selectedFormat)
                            exportResult = result
                            
                            if (result is ExportResult.Success) {
                                // Share the exported file
                                val shareIntent = exportManager.shareExportedFile(result)
                                shareIntent?.let { intent ->
                                    context.startActivity(Intent.createChooser(intent, "Share Playlist"))
                                }
                            }
                        } catch (e: Exception) {
                            exportResult = ExportResult.Error("Export failed: ${e.message}")
                        } finally {
                            isExporting = false
                        }
                    }
                },
                enabled = !isExporting && playlistItems.isNotEmpty()
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

/**
 * Component for selecting export format
 */
@Composable
private fun ExportFormatSelector(
    formats: List<ExportFormat>,
    selectedFormat: ExportFormat,
    onFormatSelected: (ExportFormat) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.height(150.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(formats) { format ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedFormat == format,
                        onClick = { if (enabled) onFormatSelected(format) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedFormat == format,
                    onClick = { if (enabled) onFormatSelected(format) },
                    enabled = enabled
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = format.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface 
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = ".${format.extension}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Format icon
                Icon(
                    imageVector = getFormatIcon(format),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun getFormatIcon(format: ExportFormat): ImageVector {
    return when (format) {
        ExportFormat.TXT -> Icons.Default.Description
        ExportFormat.JSON -> Icons.Default.DataObject
        ExportFormat.CSV -> Icons.Default.TableChart
        ExportFormat.HTML -> Icons.Default.Language
        ExportFormat.M3U -> Icons.AutoMirrored.Filled.PlaylistPlay
    }
}