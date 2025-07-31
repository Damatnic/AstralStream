package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.export.VideoExportManager

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExportProgressDialog(
    exportState: VideoExportManager.ExportState,
    exportProgress: Float,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onOpenFile: (String) -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (exportState is VideoExportManager.ExportState.Completed ||
                exportState is VideoExportManager.ExportState.Error) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon based on state
                AnimatedContent(
                    targetState = exportState,
                    transitionSpec = {
                        fadeIn() with fadeOut()
                    }
                ) { state ->
                    when (state) {
                        is VideoExportManager.ExportState.Preparing,
                        is VideoExportManager.ExportState.Exporting -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                        }
                        is VideoExportManager.ExportState.Completed -> {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        is VideoExportManager.ExportState.Error -> {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }
                }
                
                // Title
                Text(
                    text = when (exportState) {
                        is VideoExportManager.ExportState.Preparing -> "Preparing Export"
                        is VideoExportManager.ExportState.Exporting -> "Exporting Video"
                        is VideoExportManager.ExportState.Completed -> "Export Completed"
                        is VideoExportManager.ExportState.Error -> "Export Failed"
                        else -> "Export"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                // Status message
                AnimatedContent(
                    targetState = exportState,
                    transitionSpec = {
                        fadeIn() with fadeOut()
                    }
                ) { state ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (state) {
                            is VideoExportManager.ExportState.Preparing -> {
                                Text(
                                    text = "Analyzing video...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is VideoExportManager.ExportState.Exporting -> {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Progress bar
                                LinearProgressIndicator(
                                    progress = exportProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                
                                Text(
                                    text = "${(exportProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            is VideoExportManager.ExportState.Completed -> {
                                Text(
                                    text = "Video exported successfully!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Folder,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Saved to:",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = state.outputPath.substringAfterLast("/"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            is VideoExportManager.ExportState.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
                
                // Action buttons
                AnimatedContent(
                    targetState = exportState,
                    transitionSpec = {
                        fadeIn() with fadeOut()
                    }
                ) { state ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (state) {
                            is VideoExportManager.ExportState.Preparing,
                            is VideoExportManager.ExportState.Exporting -> {
                                OutlinedButton(
                                    onClick = onCancel,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel")
                                }
                            }
                            is VideoExportManager.ExportState.Completed -> {
                                OutlinedButton(
                                    onClick = { onOpenFile(state.outputPath) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open")
                                }
                                
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Done")
                                }
                            }
                            is VideoExportManager.ExportState.Error -> {
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Close")
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}