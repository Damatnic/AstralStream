package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.export.VideoExportManager
import kotlinx.coroutines.launch
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VideoExportDialog(
    videoUri: Uri,
    videoTitle: String,
    videoDurationMs: Long,
    onExport: (VideoExportManager.ExportOptions) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(VideoExportManager.ExportFormat.MP4) }
    var selectedQuality by remember { mutableStateOf(VideoExportManager.ExportQuality.HIGH) }
    var includeAudio by remember { mutableStateOf(true) }
    var enableTrimming by remember { mutableStateOf(false) }
    var trimStartMs by remember { mutableStateOf(0L) }
    var trimEndMs by remember { mutableStateOf(videoDurationMs) }
    var customResolution by remember { mutableStateOf(false) }
    var targetWidth by remember { mutableStateOf("") }
    var targetHeight by remember { mutableStateOf("") }
    var customBitrate by remember { mutableStateOf(false) }
    var targetBitrate by remember { mutableStateOf("") }
    
    val dateFormat = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()) }
    val timestamp = remember { dateFormat.format(Date()) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Export Video",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = videoTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Format selection
                    Column {
                        Text(
                            text = "Output Format",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            VideoExportManager.ExportFormat.values().forEach { format ->
                                FilterChip(
                                    selected = selectedFormat == format,
                                    onClick = { selectedFormat = format },
                                    label = { Text(format.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    // Quality selection
                    Column {
                        Text(
                            text = "Quality",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VideoExportManager.ExportQuality.values().forEach { quality ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedQuality == quality,
                                        onClick = { selectedQuality = quality }
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = quality.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = when (quality) {
                                                VideoExportManager.ExportQuality.LOW -> "50% bitrate - Smaller file size"
                                                VideoExportManager.ExportQuality.MEDIUM -> "75% bitrate - Good balance"
                                                VideoExportManager.ExportQuality.HIGH -> "100% bitrate - High quality"
                                                VideoExportManager.ExportQuality.ULTRA -> "150% bitrate - Best quality"
                                                VideoExportManager.ExportQuality.ORIGINAL -> "Keep original bitrate"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Audio option
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Include Audio",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            
                            Switch(
                                checked = includeAudio,
                                onCheckedChange = { includeAudio = it }
                            )
                        }
                    }
                    
                    // Trimming section
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCut,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Trim Video",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            
                            Switch(
                                checked = enableTrimming,
                                onCheckedChange = { enableTrimming = it }
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = enableTrimming,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Trim range slider
                                RangeSlider(
                                    value = trimStartMs.toFloat()..trimEndMs.toFloat(),
                                    onValueChange = { range ->
                                        trimStartMs = range.start.toLong()
                                        trimEndMs = range.endInclusive.toLong()
                                    },
                                    valueRange = 0f..videoDurationMs.toFloat(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Start: ${formatTime(trimStartMs)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "End: ${formatTime(trimEndMs)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                Text(
                                    text = "Duration: ${formatTime(trimEndMs - trimStartMs)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                    
                    // Advanced options
                    Column {
                        Text(
                            text = "Advanced Options",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Custom resolution
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (customResolution)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Custom Resolution",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Switch(
                                        checked = customResolution,
                                        onCheckedChange = { customResolution = it }
                                    )
                                }
                                
                                AnimatedVisibility(visible = customResolution) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = targetWidth,
                                            onValueChange = { targetWidth = it },
                                            label = { Text("Width") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        
                                        Text("×")
                                        
                                        OutlinedTextField(
                                            value = targetHeight,
                                            onValueChange = { targetHeight = it },
                                            label = { Text("Height") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Custom bitrate
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (customBitrate)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Custom Bitrate",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Switch(
                                        checked = customBitrate,
                                        onCheckedChange = { customBitrate = it }
                                    )
                                }
                                
                                AnimatedVisibility(visible = customBitrate) {
                                    OutlinedTextField(
                                        value = targetBitrate,
                                        onValueChange = { targetBitrate = it },
                                        label = { Text("Bitrate (Mbps)") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val exportDir = File(context.getExternalFilesDir(null), "exports")
                            exportDir.mkdirs()
                            
                            val outputFileName = "${videoTitle}_${timestamp}_${selectedQuality.name.lowercase()}${selectedFormat.extension}"
                            val outputPath = File(exportDir, outputFileName).absolutePath
                            
                            val options = VideoExportManager.ExportOptions(
                                sourceUri = videoUri,
                                outputPath = outputPath,
                                format = selectedFormat,
                                quality = selectedQuality,
                                trimStartMs = if (enableTrimming) trimStartMs else 0L,
                                trimEndMs = if (enableTrimming) trimEndMs else -1L,
                                includeAudio = includeAudio,
                                targetWidth = if (customResolution) targetWidth.toIntOrNull() ?: -1 else -1,
                                targetHeight = if (customResolution) targetHeight.toIntOrNull() ?: -1 else -1,
                                targetBitrate = if (customBitrate) {
                                    (targetBitrate.toFloatOrNull() ?: 0f * 1_000_000).toInt()
                                } else -1
                            )
                            
                            onExport(options)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
    } else {
        String.format("%02d:%02d", minutes, seconds % 60)
    }
}