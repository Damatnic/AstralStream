package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

data class AudioTrack(
    val groupIndex: Int,
    val trackIndex: Int,
    val format: Format,
    val label: String,
    val language: String?,
    val channels: Int,
    val sampleRate: Int,
    val bitrate: Long,
    val isSelected: Boolean
)

@Composable
fun AudioTrackSelectionDialog(
    player: ExoPlayer?,
    trackSelector: DefaultTrackSelector?,
    onDismiss: () -> Unit
) {
    val audioTracks = remember(player, trackSelector) {
        getAudioTracks(player, trackSelector)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio Track",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Audio track options
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(audioTracks) { track ->
                        AudioTrackOption(
                            track = track,
                            onClick = {
                                // Select audio track
                                val group = player?.currentTracks?.groups?.get(track.groupIndex)
                                if (group != null && trackSelector != null) {
                                    trackSelector.parameters = trackSelector.buildUponParameters()
                                        .setOverrideForType(
                                            TrackSelectionOverride(
                                                group.mediaTrackGroup,
                                                track.trackIndex
                                            )
                                        )
                                        .build()
                                }
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTrackOption(
    track: AudioTrack,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (track.isSelected) Color(0xFF00BCD4).copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.05f)
            )
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Language
                if (track.language != null) {
                    AudioInfoChip(
                        icon = Icons.Default.Language,
                        text = track.language.uppercase()
                    )
                }
                
                // Channels
                AudioInfoChip(
                    icon = Icons.Default.Speaker,
                    text = getChannelConfig(track.channels)
                )
                
                // Sample rate
                AudioInfoChip(
                    icon = Icons.Default.GraphicEq,
                    text = "${track.sampleRate / 1000}kHz"
                )
                
                // Bitrate
                if (track.bitrate > 0) {
                    AudioInfoChip(
                        icon = Icons.Default.Speed,
                        text = formatBitrate(track.bitrate)
                    )
                }
            }
        }
        
        if (track.isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color(0xFF00BCD4),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AudioInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

private fun getAudioTracks(player: ExoPlayer?, trackSelector: DefaultTrackSelector?): List<AudioTrack> {
    val tracks = mutableListOf<AudioTrack>()
    
    player?.currentTracks?.groups?.forEachIndexed { groupIndex, group ->
        if (group.type == C.TRACK_TYPE_AUDIO) {
            val trackGroup = group.mediaTrackGroup
            val selectedTrackIndex = getSelectedTrackIndex(group)
            
            for (trackIndex in 0 until trackGroup.length) {
                val format = trackGroup.getFormat(trackIndex)
                val label = format.label ?: "Audio ${trackIndex + 1}"
                val language = format.language
                
                tracks.add(
                    AudioTrack(
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        format = format,
                        label = label,
                        language = language,
                        channels = format.channelCount,
                        sampleRate = format.sampleRate,
                        bitrate = format.bitrate.toLong(),
                        isSelected = trackIndex == selectedTrackIndex
                    )
                )
            }
        }
    }
    
    return tracks
}

private fun getSelectedTrackIndex(group: androidx.media3.common.Tracks.Group): Int {
    for (i in 0 until group.length) {
        if (group.isTrackSelected(i)) {
            return i
        }
    }
    return -1
}

private fun getChannelConfig(channels: Int): String {
    return when (channels) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> "$channels ch"
    }
}

private fun formatBitrate(bitrate: Long): String {
    return when {
        bitrate <= 0 -> ""
        bitrate < 1_000 -> "$bitrate bps"
        bitrate < 1_000_000 -> "${bitrate / 1_000}kbps"
        else -> "%.1fMbps".format(bitrate / 1_000_000.0)
    }
}