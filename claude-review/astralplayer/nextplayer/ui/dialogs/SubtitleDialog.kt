package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

data class SubtitleTrack(
    val trackGroup: TrackGroup,
    val trackIndex: Int,
    val format: Format,
    val language: String,
    val label: String,
    val isSelected: Boolean
)

@Composable
fun SubtitleSelectionDialog(
    exoPlayer: ExoPlayer,
    trackSelector: DefaultTrackSelector?,
    onDismiss: () -> Unit
) {
    var subtitleTracks by remember { mutableStateOf<List<SubtitleTrack>>(emptyList()) }
    var subtitlesEnabled by remember { mutableStateOf(true) }
    
    // Extract subtitle tracks
    LaunchedEffect(exoPlayer) {
        val tracks = exoPlayer.currentTracks
        val subtitles = mutableListOf<SubtitleTrack>()
        
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_TEXT) {
                val group = trackGroup.mediaTrackGroup
                for (i in 0 until group.length) {
                    val format = group.getFormat(i)
                    val language = format.language ?: "Unknown"
                    val label = format.label ?: language
                    
                    subtitles.add(
                        SubtitleTrack(
                            trackGroup = group,
                            trackIndex = i,
                            format = format,
                            language = language,
                            label = label,
                            isSelected = trackGroup.isTrackSelected(i)
                        )
                    )
                }
            }
        }
        
        subtitleTracks = subtitles
        subtitlesEnabled = trackSelector?.parameters?.getRendererDisabled(2) == false
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subtitles",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Enable/Disable toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            subtitlesEnabled = !subtitlesEnabled
                            trackSelector?.let { selector ->
                                val params = selector.parameters.buildUpon()
                                    .setRendererDisabled(2, !subtitlesEnabled)
                                    .build()
                                selector.parameters = params
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show subtitles",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Switch(
                        checked = subtitlesEnabled,
                        onCheckedChange = { enabled ->
                            subtitlesEnabled = enabled
                            trackSelector?.let { selector ->
                                val params = selector.parameters.buildUpon()
                                    .setRendererDisabled(2, !enabled)
                                    .build()
                                selector.parameters = params
                            }
                        }
                    )
                }
                
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle tracks
                if (subtitleTracks.isEmpty()) {
                    Text(
                        text = "No subtitles available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn {
                        // Off option
                        item {
                            SubtitleTrackItem(
                                label = "Off",
                                language = "",
                                isSelected = !subtitlesEnabled,
                                onClick = {
                                    trackSelector?.let { selector ->
                                        val params = selector.parameters.buildUpon()
                                            .setRendererDisabled(2, true)
                                            .build()
                                        selector.parameters = params
                                    }
                                    subtitlesEnabled = false
                                }
                            )
                        }
                        
                        // Available tracks
                        items(subtitleTracks) { track ->
                            SubtitleTrackItem(
                                label = track.label,
                                language = track.language,
                                isSelected = track.isSelected && subtitlesEnabled,
                                onClick = {
                                    // Simple subtitle selection
                                    // TODO: Implement proper track selection when TrackSelector is available
                                    subtitlesEnabled = true
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Add subtitle file button
                OutlinedButton(
                    onClick = { /* TODO: Implement subtitle file picker */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add subtitle file")
                }
            }
        }
    }
}

@Composable
fun SubtitleTrackItem(
    label: String,
    language: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (language.isNotEmpty() && language != label) {
                Text(
                    text = language,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}