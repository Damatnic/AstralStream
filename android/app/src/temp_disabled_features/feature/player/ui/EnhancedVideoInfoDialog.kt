package com.astralplayer.nextplayer.feature.player.ui

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

data class VideoMetadata(
    val title: String,
    val duration: Long,
    val fileSize: Long,
    val resolution: String,
    val frameRate: Float,
    val bitrate: Long,
    val codec: String,
    val audioCodec: String,
    val audioChannels: Int,
    val audioSampleRate: Int,
    val filePath: String,
    val dateCreated: Long,
    val dateModified: Long
)

@Composable
fun EnhancedVideoInfoDialog(
    videoUri: Uri?,
    player: ExoPlayer?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val videoMetadata = remember(videoUri) {
        extractVideoMetadata(videoUri, player)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            ),
            shape = RoundedCornerShape(16.dp)
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
                    Text(
                        text = "Video Information",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00BCD4)
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
                
                // Content
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // File Information
                    item {
                        InfoSection(
                            title = "File Information",
                            icon = Icons.Default.Info
                        ) {
                            InfoRow("Title", videoMetadata.title)
                            InfoRow("File Path", videoMetadata.filePath)
                            InfoRow("File Size", formatFileSize(videoMetadata.fileSize))
                            InfoRow("Date Created", formatDate(videoMetadata.dateCreated))
                            InfoRow("Date Modified", formatDate(videoMetadata.dateModified))
                        }
                    }
                    
                    // Video Properties
                    item {
                        InfoSection(
                            title = "Video Properties",
                            icon = Icons.Default.VideoSettings
                        ) {
                            InfoRow("Duration", formatDuration(videoMetadata.duration))
                            InfoRow("Resolution", videoMetadata.resolution)
                            InfoRow("Frame Rate", "${videoMetadata.frameRate} fps")
                            InfoRow("Video Codec", videoMetadata.codec)
                            InfoRow("Bitrate", formatBitrate(videoMetadata.bitrate))
                        }
                    }
                    
                    // Audio Properties
                    item {
                        InfoSection(
                            title = "Audio Properties",
                            icon = Icons.Default.AudioFile
                        ) {
                            InfoRow("Audio Codec", videoMetadata.audioCodec)
                            InfoRow("Channels", "${videoMetadata.audioChannels} channels")
                            InfoRow("Sample Rate", "${videoMetadata.audioSampleRate} Hz")
                        }
                    }
                    
                    // Playback Statistics
                    item {
                        InfoSection(
                            title = "Playback Statistics",
                            icon = Icons.Default.Analytics
                        ) {
                            val currentPosition = player?.currentPosition ?: 0L
                            val bufferedPercentage = player?.bufferedPercentage ?: 0
                            val playbackSpeed = player?.playbackParameters?.speed ?: 1.0f
                            
                            InfoRow("Current Position", formatDuration(currentPosition))
                            InfoRow("Buffered", "$bufferedPercentage%")
                            InfoRow("Playback Speed", "${playbackSpeed}x")
                            InfoRow("Player State", getPlayerStateString(player))
                        }
                    }
                    
                    // Technical Details
                    item {
                        InfoSection(
                            title = "Technical Details",
                            icon = Icons.Default.Settings
                        ) {
                            InfoRow("Container Format", getContainerFormat(videoUri))
                            InfoRow("Color Space", "BT.709") // Placeholder
                            InfoRow("HDR Support", "No") // Placeholder
                            InfoRow("Hardware Acceleration", "Enabled") // Placeholder
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF00BCD4),
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00BCD4)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

// Helper functions
private fun extractVideoMetadata(videoUri: Uri?, player: ExoPlayer?): VideoMetadata {
    if (videoUri == null) {
        return VideoMetadata(
            title = "Unknown",
            duration = 0L,
            fileSize = 0L,
            resolution = "Unknown",
            frameRate = 0f,
            bitrate = 0L,
            codec = "Unknown",
            audioCodec = "Unknown",
            audioChannels = 0,
            audioSampleRate = 0,
            filePath = "Unknown",
            dateCreated = 0L,
            dateModified = 0L
        )
    }
    
    val file = if (videoUri.scheme == "file") File(videoUri.path ?: "") else null
    val videoFormat = player?.videoFormat
    val audioFormat = player?.audioFormat
    
    return VideoMetadata(
        title = file?.nameWithoutExtension ?: "Unknown",
        duration = player?.duration ?: 0L,
        fileSize = file?.length() ?: 0L,
        resolution = if (videoFormat != null) "${videoFormat.width}x${videoFormat.height}" else "Unknown",
        frameRate = videoFormat?.frameRate ?: 0f,
        bitrate = videoFormat?.bitrate?.toLong() ?: 0L,
        codec = videoFormat?.codecs ?: "Unknown",
        audioCodec = audioFormat?.codecs ?: "Unknown",
        audioChannels = audioFormat?.channelCount ?: 0,
        audioSampleRate = audioFormat?.sampleRate ?: 0,
        filePath = videoUri.path ?: "Unknown",
        dateCreated = file?.lastModified() ?: 0L,
        dateModified = file?.lastModified() ?: 0L
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return "Unknown"
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatBitrate(bitrate: Long): String {
    return if (bitrate > 0) {
        "${bitrate / 1000} kbps"
    } else {
        "Unknown"
    }
}

private fun getPlayerStateString(player: ExoPlayer?): String {
    return when (player?.playbackState) {
        ExoPlayer.STATE_IDLE -> "Idle"
        ExoPlayer.STATE_BUFFERING -> "Buffering"
        ExoPlayer.STATE_READY -> if (player.playWhenReady) "Playing" else "Paused"
        ExoPlayer.STATE_ENDED -> "Ended"
        else -> "Unknown"
    }
}

private fun getContainerFormat(videoUri: Uri?): String {
    val extension = videoUri?.path?.substringAfterLast('.', "")?.uppercase()
    return when (extension) {
        "MP4" -> "MPEG-4"
        "MKV" -> "Matroska"
        "AVI" -> "Audio Video Interleave"
        "MOV" -> "QuickTime"
        "WMV" -> "Windows Media Video"
        "FLV" -> "Flash Video"
        "WEBM" -> "WebM"
        else -> extension ?: "Unknown"
    }
}