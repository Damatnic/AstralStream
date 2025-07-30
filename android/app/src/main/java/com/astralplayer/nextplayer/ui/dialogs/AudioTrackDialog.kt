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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

data class AudioTrack(
    val trackGroup: TrackGroup,
    val trackIndex: Int,
    val format: Format,
    val language: String,
    val label: String,
    val bitrate: Int,
    val sampleRate: Int,
    val channels: Int,
    val isSelected: Boolean
)

@Composable
fun AudioTrackSelectionDialog(
    exoPlayer: ExoPlayer,
    trackSelector: DefaultTrackSelector?,
    onDismiss: () -> Unit
) {
    var audioTracks by remember { mutableStateOf<List<AudioTrack>>(emptyList()) }
    var selectedTrack by remember { mutableStateOf<AudioTrack?>(null) }
    
    // Extract audio tracks
    LaunchedEffect(exoPlayer) {
        val tracks = exoPlayer.currentTracks
        val audios = mutableListOf<AudioTrack>()
        
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                val group = trackGroup.mediaTrackGroup
                for (i in 0 until group.length) {
                    val format = group.getFormat(i)
                    val language = format.language ?: "Unknown"
                    val label = format.label ?: when (language) {
                        "en" -> "English"
                        "es" -> "Spanish"
                        "fr" -> "French"
                        "de" -> "German"
                        "ja" -> "Japanese"
                        "ko" -> "Korean"
                        "zh" -> "Chinese"
                        "hi" -> "Hindi"
                        else -> language
                    }
                    
                    val audioTrack = AudioTrack(
                        trackGroup = group,
                        trackIndex = i,
                        format = format,
                        language = language,
                        label = label,
                        bitrate = format.bitrate,
                        sampleRate = format.sampleRate,
                        channels = format.channelCount,
                        isSelected = trackGroup.isTrackSelected(i)
                    )
                    
                    audios.add(audioTrack)
                    if (audioTrack.isSelected) {
                        selectedTrack = audioTrack
                    }
                }
            }
        }
        
        audioTracks = audios
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
                        text = "Audio Track",
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
                
                // Audio info card
                selectedTrack?.let { track ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Current Track",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Icon(
                                    imageVector = Icons.Filled.AudioFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${track.label} • ${track.channels}ch • ${track.sampleRate/1000}kHz",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Audio tracks list
                if (audioTracks.isEmpty()) {
                    Text(
                        text = "No audio tracks available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn {
                        items(audioTracks) { track ->
                            AudioTrackItem(
                                track = track,
                                isSelected = track == selectedTrack,
                                onClick = {
                                    selectedTrack = track
                                    // TODO: Implement actual track selection when TrackSelector is available
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioTrackItem(
    track: AudioTrack,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language
                if (track.language.isNotEmpty() && track.language != track.label) {
                    Text(
                        text = track.language,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Format info
                Text(
                    text = buildString {
                        append("${track.channels}ch")
                        if (track.sampleRate > 0) {
                            append(" • ${track.sampleRate/1000}kHz")
                        }
                        if (track.bitrate > 0) {
                            append(" • ${track.bitrate/1000}kbps")
                        }
                    },
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