package com.astralplayer.nextplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.astralplayer.nextplayer.data.SubtitleEntry
import com.astralplayer.nextplayer.data.SubtitleRenderer
import com.astralplayer.nextplayer.data.SubtitleManager

@Composable
fun SubtitleSelectionDialog(
    subtitleRenderer: SubtitleRenderer?,
    availableSubtitles: List<com.astralplayer.nextplayer.data.SubtitleManager.SubtitleTrack>,
    currentTrackId: String?,
    onTrackSelected: (com.astralplayer.nextplayer.data.SubtitleManager.SubtitleTrack?) -> Unit,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddSubtitleFile: (android.net.Uri, String?) -> Unit,
    onGenerateAISubtitles: () -> Unit = {},
    onTestAIService: () -> Unit = {},
    isGeneratingAISubtitles: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showFilePicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subtitles")
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Subtitle settings"
                    )
                }
            }
        },
        text = {
            LazyColumn {
                // None option
                item {
                    SubtitleTrackItem(
                        track = null,
                        isSelected = currentTrackId == null,
                        onClick = { 
                            onTrackSelected(null)
                            onDismiss()
                        }
                    )
                }
                
                // Available subtitle tracks
                items(availableSubtitles) { track ->
                    SubtitleTrackItem(
                        track = track,
                        isSelected = track.id == currentTrackId,
                        onClick = { 
                            onTrackSelected(track)
                            onDismiss()
                        }
                    )
                }
                
                // Add subtitle option
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFilePicker = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Add subtitle file",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Generate subtitles with AI
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                    val application = context.applicationContext as com.astralplayer.nextplayer.AstralVuApplication
                    val settingsRepository = application.settingsRepository
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                coroutineScope.launch {
                                    val aiEnabled = settingsRepository.getAISubtitleGenerationEnabled().first()
                                    if (aiEnabled) {
                                        if (!isGeneratingAISubtitles) {
                                            // Generate AI subtitles
                                            onGenerateAISubtitles()
                                        }
                                    } else {
                                        android.widget.Toast.makeText(
                                            context, 
                                            "Please enable AI subtitle generation in settings", 
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isGeneratingAISubtitles) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (isGeneratingAISubtitles) "Generating AI subtitles..." else "Generate with AI",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Test AI service connectivity
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onTestAIService()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Test AI Service",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = modifier
    )
    
    // Show file picker dialog
    if (showFilePicker) {
        SubtitleFilePickerDialog(
            onFileSelected = { uri ->
                val filename = uri.lastPathSegment
                onAddSubtitleFile(uri, filename)
                showFilePicker = false
            },
            onDismiss = { showFilePicker = false }
        )
    }
}

@Composable
private fun SubtitleTrackItem(
    track: SubtitleManager.SubtitleTrack?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track?.name ?: "None",
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (track != null) {
                Text(
                    text = when {
                        track.isEmbedded -> "Embedded • ${track.language ?: "Unknown"}"
                        else -> track.language ?: "Unknown"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SubtitleSettingsSheet(
    subtitleRenderer: SubtitleRenderer,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displaySettings by subtitleRenderer.displaySettings.collectAsState()
    var fontSize by remember { mutableStateOf(displaySettings.fontSize) }
    var showBackground by remember { mutableStateOf(displaySettings.showBackground) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Subtitle Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Font size
            Text(
                text = "Font Size",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Small")
                Slider(
                    value = fontSize,
                    onValueChange = { 
                        fontSize = it
                        subtitleRenderer.updateDisplaySettings(
                            displaySettings.copy(fontSize = it)
                        )
                    },
                    valueRange = 12f..32f,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                )
                Text("Large")
            }
            
            Text(
                text = "${fontSize.toInt()}sp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Background toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Background")
                Switch(
                    checked = showBackground,
                    onCheckedChange = { 
                        showBackground = it
                        subtitleRenderer.updateDisplaySettings(
                            displaySettings.copy(showBackground = it)
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Position selection
            Text(
                text = "Position",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = displaySettings.position == com.astralplayer.nextplayer.data.SubtitlePosition.TOP_CENTER,
                    onClick = {
                        subtitleRenderer.updateDisplaySettings(
                            displaySettings.copy(position = com.astralplayer.nextplayer.data.SubtitlePosition.TOP_CENTER)
                        )
                    },
                    label = { Text("Top") }
                )
                FilterChip(
                    selected = displaySettings.position == com.astralplayer.nextplayer.data.SubtitlePosition.MIDDLE_CENTER,
                    onClick = {
                        subtitleRenderer.updateDisplaySettings(
                            displaySettings.copy(position = com.astralplayer.nextplayer.data.SubtitlePosition.MIDDLE_CENTER)
                        )
                    },
                    label = { Text("Center") }
                )
                FilterChip(
                    selected = displaySettings.position == com.astralplayer.nextplayer.data.SubtitlePosition.BOTTOM_CENTER,
                    onClick = {
                        subtitleRenderer.updateDisplaySettings(
                            displaySettings.copy(position = com.astralplayer.nextplayer.data.SubtitlePosition.BOTTOM_CENTER)
                        )
                    },
                    label = { Text("Bottom") }
                )
            }
        }
    }
}