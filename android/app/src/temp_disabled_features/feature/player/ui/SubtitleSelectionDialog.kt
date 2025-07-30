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
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

data class SubtitleTrack(
    val groupIndex: Int,
    val trackIndex: Int,
    val format: Format,
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

@Composable
fun SubtitleSelectionDialog(
    player: ExoPlayer?,
    trackSelector: DefaultTrackSelector?,
    onDismiss: () -> Unit
) {
    val subtitleTracks = remember(player, trackSelector) {
        getSubtitleTracks(player, trackSelector)
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
                        text = "Subtitles",
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
                
                // Subtitle options
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Off option
                    item {
                        SubtitleOption(
                            title = "Off",
                            subtitle = "No subtitles",
                            isSelected = subtitleTracks.none { it.isSelected },
                            onClick = {
                                // Disable all subtitle tracks
                                trackSelector?.let { selector ->
                                    selector.parameters = selector.buildUponParameters()
                                        .setRendererDisabled(C.TRACK_TYPE_TEXT, true)
                                        .build()
                                }
                                onDismiss()
                            }
                        )
                    }
                    
                    // Available subtitle tracks
                    items(subtitleTracks) { track ->
                        SubtitleOption(
                            title = track.label,
                            subtitle = track.language ?: "Unknown language",
                            isSelected = track.isSelected,
                            onClick = {
                                // Enable subtitle renderer and select track
                                trackSelector?.let { selector ->
                                    selector.buildUponParameters()?.let { builder ->
                                        selector.parameters = builder
                                            .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                                            .setOverrideForType(
                                                TrackSelectionOverride(
                                                    player?.currentTracks?.groups?.get(track.groupIndex)?.mediaTrackGroup!!,
                                                    track.trackIndex
                                                )
                                            )
                                            .build()
                                    }
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
private fun SubtitleOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF00BCD4).copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.05f)
            )
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color(0xFF00BCD4),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun getSubtitleTracks(player: ExoPlayer?, trackSelector: DefaultTrackSelector?): List<SubtitleTrack> {
    val tracks = mutableListOf<SubtitleTrack>()
    
    player?.currentTracks?.groups?.forEachIndexed { groupIndex, group ->
        if (group.type == C.TRACK_TYPE_TEXT) {
            val trackGroup = group.mediaTrackGroup
            val selectedTrackIndex = getSelectedTrackIndex(group, trackSelector)
            
            for (trackIndex in 0 until trackGroup.length) {
                val format = trackGroup.getFormat(trackIndex)
                val label = format.label ?: "Subtitle ${trackIndex + 1}"
                val language = format.language
                
                tracks.add(
                    SubtitleTrack(
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        format = format,
                        label = label,
                        language = language,
                        isSelected = trackIndex == selectedTrackIndex
                    )
                )
            }
        }
    }
    
    return tracks
}

private fun getSelectedTrackIndex(group: androidx.media3.common.Tracks.Group, trackSelector: DefaultTrackSelector?): Int {
    for (i in 0 until group.length) {
        if (group.isTrackSelected(i)) {
            return i
        }
    }
    return -1
}