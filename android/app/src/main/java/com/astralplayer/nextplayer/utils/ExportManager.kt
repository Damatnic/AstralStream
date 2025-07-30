package com.astralplayer.nextplayer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.astralplayer.nextplayer.data.database.PlaylistEntity
import com.astralplayer.nextplayer.data.database.PlaylistItemEntity
import com.astralplayer.nextplayer.ui.components.VideoStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration

data class PlaylistItem(
    val title: String,
    val videoPath: String,
    val duration: Long,
    val position: Int
)

class ExportManager(private val context: Context) {
    
    companion object {
        private const val EXPORT_DIR = "exports"
        private const val STATS_FILE_PREFIX = "video_stats"
        private const val PLAYLIST_FILE_PREFIX = "playlist"
        
        @Volatile
        private var INSTANCE: ExportManager? = null
        
        fun getInstance(context: Context): ExportManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExportManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    
    /**
     * Export video statistics to a shareable file
     */
    suspend fun exportVideoStats(
        videoStats: VideoStats,
        format: ExportFormat = ExportFormat.TXT
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val content = formatVideoStats(videoStats, format)
            val fileName = "${STATS_FILE_PREFIX}_${dateFormat.format(Date())}.${format.extension}"
            val file = saveToFile(fileName, content)
            
            ExportResult.Success(file, format, ExportType.VIDEO_STATS)
        } catch (e: Exception) {
            ExportResult.Error("Failed to export video stats: ${e.message}")
        }
    }
    
    /**
     * Export playlist to a shareable file
     */
    suspend fun exportPlaylist(
        playlist: PlaylistEntity,
        items: List<PlaylistItem>,
        format: ExportFormat = ExportFormat.M3U
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val content = formatPlaylist(playlist, items, format)
            val fileName = "${PLAYLIST_FILE_PREFIX}_${playlist.name}_${dateFormat.format(Date())}.${format.extension}"
            val file = saveToFile(fileName, content)
            
            ExportResult.Success(file, format, ExportType.PLAYLIST)
        } catch (e: Exception) {
            ExportResult.Error("Failed to export playlist: ${e.message}")
        }
    }
    
    /**
     * Export multiple playlists to a combined file
     */
    suspend fun exportMultiplePlaylists(
        playlistsWithItems: List<Pair<PlaylistEntity, List<PlaylistItem>>>,
        format: ExportFormat = ExportFormat.JSON
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val content = formatMultiplePlaylists(playlistsWithItems, format)
            val fileName = "playlists_backup_${dateFormat.format(Date())}.${format.extension}"
            val file = saveToFile(fileName, content)
            
            ExportResult.Success(file, format, ExportType.MULTIPLE_PLAYLISTS)
        } catch (e: Exception) {
            ExportResult.Error("Failed to export playlists: ${e.message}")
        }
    }
    
    /**
     * Share exported file using Android's share functionality
     */
    suspend fun shareExportedFile(exportResult: ExportResult.Success): Intent? = withContext(Dispatchers.IO) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportResult.file
            )
            
            Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(exportResult.format)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, getShareSubject(exportResult.type))
                putExtra(Intent.EXTRA_TEXT, getShareText(exportResult.type))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get share intent for quick sharing
     */
    fun getQuickShareIntent(content: String, type: ExportType): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            this.type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, getShareSubject(type))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    /**
     * Clear old export files
     */
    suspend fun clearOldExports(maxAgeMs: Long = 24 * 60 * 60 * 1000L) = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, EXPORT_DIR)
            if (exportDir.exists()) {
                val cutoffTime = System.currentTimeMillis() - maxAgeMs
                exportDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoffTime) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    private fun saveToFile(fileName: String, content: String): File {
        val exportDir = File(context.cacheDir, EXPORT_DIR)
        exportDir.mkdirs()
        
        val file = File(exportDir, fileName)
        FileWriter(file).use { writer ->
            writer.write(content)
        }
        
        return file
    }
    
    private fun formatVideoStats(stats: VideoStats, format: ExportFormat): String {
        return when (format) {
            ExportFormat.TXT -> formatVideoStatsTxt(stats)
            ExportFormat.JSON -> formatVideoStatsJson(stats)
            ExportFormat.CSV -> formatVideoStatsCsv(stats)
            ExportFormat.HTML -> formatVideoStatsHtml(stats)
            else -> formatVideoStatsTxt(stats)
        }
    }
    
    private fun formatVideoStatsTxt(stats: VideoStats): String {
        return buildString {
            appendLine("=== Video Statistics Report ===")
            appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine()
            appendLine("VIDEO INFORMATION")
            appendLine("================")
            appendLine("Title: ${stats.videoTitle.ifEmpty { "Unknown" }}")
            appendLine("Duration: ${formatDuration(stats.duration)}")
            appendLine("File Size: ${formatFileSize(stats.fileSize)}")
            appendLine("Resolution: ${stats.resolution.ifEmpty { "Unknown" }}")
            appendLine("Frame Rate: ${if (stats.frameRate > 0) "${stats.frameRate} fps" else "Unknown"}")
            appendLine("Video Bitrate: ${if (stats.bitrate > 0) "${stats.bitrate / 1000} kbps" else "Unknown"}")
            appendLine("Video Codec: ${stats.codec.ifEmpty { "Unknown" }}")
            appendLine("MIME Type: ${stats.mimeType.ifEmpty { "Unknown" }}")
            appendLine()
            appendLine("AUDIO INFORMATION")
            appendLine("================")
            appendLine("Audio Codec: ${stats.audioCodec.ifEmpty { "Unknown" }}")
            appendLine("Audio Bitrate: ${if (stats.audioBitrate > 0) "${stats.audioBitrate / 1000} kbps" else "Unknown"}")
            appendLine("Sample Rate: ${if (stats.audioSampleRate > 0) "${stats.audioSampleRate} Hz" else "Unknown"}")
            appendLine("Channels: ${if (stats.audioChannels > 0) getChannelDescription(stats.audioChannels) else "Unknown"}")
            appendLine()
            appendLine("PLAYBACK PERFORMANCE")
            appendLine("===================")
            appendLine("Playback Speed: ${stats.playbackSpeed}x")
            appendLine("Buffered Duration: ${formatDuration(stats.bufferedDuration)}")
            appendLine("Average Bitrate: ${if (stats.averageBitrate > 0) "${stats.averageBitrate / 1000} kbps" else "Unknown"}")
            appendLine("Network Speed: ${stats.networkSpeed.ifEmpty { "Unknown" }}")
            appendLine("Total Frames: ${if (stats.totalFrames > 0) stats.totalFrames.toString() else "Unknown"}")
            appendLine("Dropped Frames: ${stats.droppedFrames}")
            val dropPercentage = if (stats.totalFrames > 0) {
                (stats.droppedFrames.toFloat() / stats.totalFrames * 100)
            } else 0f
            appendLine("Drop Rate: ${"%.2f".format(dropPercentage)}%")
            appendLine()
            appendLine("SESSION STATISTICS")
            appendLine("=================")
            appendLine("Watch Time: ${formatDuration(stats.playbackTime)}")
            appendLine("Completion: ${(stats.completionPercentage * 100).toInt()}%")
            appendLine("Pause Count: ${stats.pauseCount}")
            appendLine("Seek Count: ${stats.seekCount}")
            appendLine("Battery Usage: ${"%.1f".format(stats.batteryUsage)}%")
            appendLine()
            appendLine("Report generated by Astral Player")
        }
    }
    
    private fun formatVideoStatsJson(stats: VideoStats): String {
        return buildString {
            appendLine("{")
            appendLine("  \"videoStats\": {")
            appendLine("    \"metadata\": {")
            appendLine("      \"exportedAt\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())}\",")
            appendLine("      \"exportedBy\": \"Astral Player\"")
            appendLine("    },")
            appendLine("    \"videoInfo\": {")
            appendLine("      \"title\": \"${stats.videoTitle.replace("\"", "\\\"")}\",")
            appendLine("      \"duration\": ${stats.duration.inWholeMilliseconds},")
            appendLine("      \"fileSize\": ${stats.fileSize},")
            appendLine("      \"resolution\": \"${stats.resolution}\",")
            appendLine("      \"frameRate\": ${stats.frameRate},")
            appendLine("      \"bitrate\": ${stats.bitrate},")
            appendLine("      \"codec\": \"${stats.codec}\",")
            appendLine("      \"mimeType\": \"${stats.mimeType}\"")
            appendLine("    },")
            appendLine("    \"audioInfo\": {")
            appendLine("      \"codec\": \"${stats.audioCodec}\",")
            appendLine("      \"bitrate\": ${stats.audioBitrate},")
            appendLine("      \"sampleRate\": ${stats.audioSampleRate},")
            appendLine("      \"channels\": ${stats.audioChannels}")
            appendLine("    },")
            appendLine("    \"performance\": {")
            appendLine("      \"playbackSpeed\": ${stats.playbackSpeed},")
            appendLine("      \"bufferedDuration\": ${stats.bufferedDuration.inWholeMilliseconds},")
            appendLine("      \"droppedFrames\": ${stats.droppedFrames},")
            appendLine("      \"totalFrames\": ${stats.totalFrames},")
            appendLine("      \"averageBitrate\": ${stats.averageBitrate},")
            appendLine("      \"networkSpeed\": \"${stats.networkSpeed}\"")
            appendLine("    },")
            appendLine("    \"session\": {")
            appendLine("      \"playbackTime\": ${stats.playbackTime.inWholeMilliseconds},")
            appendLine("      \"pauseCount\": ${stats.pauseCount},")
            appendLine("      \"seekCount\": ${stats.seekCount},")
            appendLine("      \"completionPercentage\": ${stats.completionPercentage},")
            appendLine("      \"batteryUsage\": ${stats.batteryUsage}")
            appendLine("    }")
            appendLine("  }")
            append("}")
        }
    }
    
    private fun formatVideoStatsCsv(stats: VideoStats): String {
        return buildString {
            appendLine("Field,Value")
            appendLine("Export Date,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            val safeTitle = stats.videoTitle.replace("\"", "\\\"")
            appendLine("Video Title,\"$safeTitle\"")
            appendLine("Duration (ms),${stats.duration.inWholeMilliseconds}")
            appendLine("File Size (bytes),${stats.fileSize}")
            appendLine("Resolution,\"${stats.resolution}\"")
            appendLine("Frame Rate,${stats.frameRate}")
            appendLine("Video Bitrate,${stats.bitrate}")
            appendLine("Video Codec,\"${stats.codec}\"")
            appendLine("MIME Type,\"${stats.mimeType}\"")
            appendLine("Audio Codec,\"${stats.audioCodec}\"")
            appendLine("Audio Bitrate,${stats.audioBitrate}")
            appendLine("Audio Sample Rate,${stats.audioSampleRate}")
            appendLine("Audio Channels,${stats.audioChannels}")
            appendLine("Playback Speed,${stats.playbackSpeed}")
            appendLine("Buffered Duration (ms),${stats.bufferedDuration.inWholeMilliseconds}")
            appendLine("Dropped Frames,${stats.droppedFrames}")
            appendLine("Total Frames,${stats.totalFrames}")
            appendLine("Average Bitrate,${stats.averageBitrate}")
            appendLine("Network Speed,\"${stats.networkSpeed}\"")
            appendLine("Playback Time (ms),${stats.playbackTime.inWholeMilliseconds}")
            appendLine("Pause Count,${stats.pauseCount}")
            appendLine("Seek Count,${stats.seekCount}")
            appendLine("Completion Percentage,${stats.completionPercentage}")
            appendLine("Battery Usage,${stats.batteryUsage}")
        }
    }
    
    private fun formatVideoStatsHtml(stats: VideoStats): String {
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html>")
            appendLine("<head>")
            appendLine("  <title>Video Statistics Report</title>")
            appendLine("  <meta charset=\"UTF-8\">")
            appendLine("  <style>")
            appendLine("    body { font-family: Arial, sans-serif; margin: 20px; }")
            appendLine("    .header { background: #2196F3; color: white; padding: 20px; border-radius: 5px; }")
            appendLine("    .section { margin: 20px 0; padding: 15px; background: #f5f5f5; border-radius: 5px; }")
            appendLine("    .stat { display: flex; justify-content: space-between; margin: 5px 0; }")
            appendLine("    .label { font-weight: bold; }")
            appendLine("    .value { font-family: monospace; }")
            appendLine("  </style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("  <div class=\"header\">")
            appendLine("    <h1>Video Statistics Report</h1>")
            appendLine("    <p>Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}</p>")
            appendLine("  </div>")
            appendLine("  <div class=\"section\">")
            appendLine("    <h2>Video Information</h2>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Title:</span><span class=\"value\">${stats.videoTitle.ifEmpty { "Unknown" }}</span></div>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Duration:</span><span class=\"value\">${formatDuration(stats.duration)}</span></div>")
            appendLine("    <div class=\"stat\"><span class=\"label\">File Size:</span><span class=\"value\">${formatFileSize(stats.fileSize)}</span></div>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Resolution:</span><span class=\"value\">${stats.resolution.ifEmpty { "Unknown" }}</span></div>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Frame Rate:</span><span class=\"value\">${if (stats.frameRate > 0) "${stats.frameRate} fps" else "Unknown"}</span></div>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Video Codec:</span><span class=\"value\">${stats.codec.ifEmpty { "Unknown" }}</span></div>")
            appendLine("  </div>")
            appendLine("  <div class=\"section\">")
            appendLine("    <h2>Performance Statistics</h2>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Dropped Frames:</span><span class=\"value\">${stats.droppedFrames}/${stats.totalFrames}</span></div>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Average Bitrate:</span><span class=\"value\">${stats.averageBitrate / 1000} kbps</span></div>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Playback Speed:</span><span class=\"value\">${stats.playbackSpeed}x</span></div>")
            appendLine("  </div>")
            appendLine("  <div class=\"section\">")
            appendLine("    <h2>Session Statistics</h2>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Watch Time:</span><span class=\"value\">${formatDuration(stats.playbackTime)}</span></div>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Completion:</span><span class=\"value\">${(stats.completionPercentage * 100).toInt()}%</span></div>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Pause Count:</span><span class=\"value\">${stats.pauseCount}</span></div>")
            appendLine("    <div class=\"stat\"><span class=\"label\">Seek Count:</span><span class=\"value\">${stats.seekCount}</span></div>")
            appendLine("  </div>")
            appendLine("  <p><em>Report generated by Astral Player</em></p>")
            appendLine("</body>")
            appendLine("</html>")
        }
    }
    
    private fun formatPlaylist(playlist: PlaylistEntity, items: List<PlaylistItem>, format: ExportFormat): String {
        return when (format) {
            ExportFormat.M3U -> formatPlaylistM3U(playlist, items)
            ExportFormat.JSON -> formatPlaylistJson(playlist, items)
            ExportFormat.TXT -> formatPlaylistTxt(playlist, items)
            else -> formatPlaylistM3U(playlist, items)
        }
    }
    
    private fun formatPlaylistM3U(playlist: PlaylistEntity, items: List<PlaylistItem>): String {
        return buildString {
            appendLine("#EXTM3U")
            appendLine("#PLAYLIST:${playlist.name}")
            appendLine("#DESCRIPTION:${playlist.description}")
            appendLine("#CREATED:${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(playlist.createdTime))}")
            appendLine()
            
            items.forEach { item ->
                if (item.duration > 0) {
                    appendLine("#EXTINF:${item.duration / 1000},${item.title}")
                } else {
                    appendLine("#EXTINF:-1,${item.title}")
                }
                appendLine(item.videoPath)
                appendLine()
            }
        }
    }
    
    private fun formatPlaylistJson(playlist: PlaylistEntity, items: List<PlaylistItem>): String {
        return buildString {
            appendLine("{")
            appendLine("  \"playlist\": {")
            appendLine("    \"name\": \"${playlist.name.replace("\"", "\\\"")}\",")
            appendLine("    \"description\": \"${playlist.description.replace("\"", "\\\"")}\",")
            appendLine("    \"createdAt\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(playlist.createdTime))}\",")
            appendLine("    \"itemCount\": ${items.size},")
            appendLine("    \"items\": [")
            items.forEachIndexed { index, item ->
                appendLine("      {")
                appendLine("        \"title\": \"${item.title.replace("\"", "\\\"")}\",")
                appendLine("        \"path\": \"${item.videoPath.replace("\"", "\\\"")}\",")
                appendLine("        \"duration\": ${item.duration},")
                appendLine("        \"position\": ${item.position}")
                appendLine("      }${if (index < items.size - 1) "," else ""}")
            }
            appendLine("    ]")
            appendLine("  }")
            append("}")
        }
    }
    
    private fun formatPlaylistTxt(playlist: PlaylistEntity, items: List<PlaylistItem>): String {
        return buildString {
            appendLine("=== Playlist Export ===")
            appendLine("Name: ${playlist.name}")
            appendLine("Description: ${playlist.description}")
            appendLine("Created: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(playlist.createdTime))}")
            appendLine("Items: ${items.size}")
            appendLine()
            appendLine("=== Playlist Items ===")
            items.forEachIndexed { index, item ->
                appendLine("${index + 1}. ${item.title}")
                appendLine("   Path: ${item.videoPath}")
                appendLine("   Duration: ${formatDuration(Duration.ZERO.plus(kotlin.time.Duration.parse("${item.duration}ms")))}")
                appendLine()
            }
            appendLine("Exported by Astral Player")
        }
    }
    
    private fun formatMultiplePlaylists(playlistsWithItems: List<Pair<PlaylistEntity, List<PlaylistItem>>>, format: ExportFormat): String {
        return when (format) {
            ExportFormat.JSON -> formatMultiplePlaylistsJson(playlistsWithItems)
            ExportFormat.TXT -> formatMultiplePlaylistsTxt(playlistsWithItems)
            else -> formatMultiplePlaylistsJson(playlistsWithItems)
        }
    }
    
    private fun formatMultiplePlaylistsJson(playlistsWithItems: List<Pair<PlaylistEntity, List<PlaylistItem>>>): String {
        return buildString {
            appendLine("{")
            appendLine("  \"playlistsBackup\": {")
            appendLine("    \"exportedAt\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())}\",")
            appendLine("    \"exportedBy\": \"Astral Player\",")
            appendLine("    \"playlistCount\": ${playlistsWithItems.size},")
            appendLine("    \"playlists\": [")
            playlistsWithItems.forEachIndexed { playlistIndex, (playlist, items) ->
                appendLine("      {")
                appendLine("        \"name\": \"${playlist.name.replace("\"", "\\\"")}\",")
                appendLine("        \"description\": \"${playlist.description.replace("\"", "\\\"")}\",")
                appendLine("        \"createdAt\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(playlist.createdTime))}\",")
                appendLine("        \"itemCount\": ${items.size},")
                appendLine("        \"items\": [")
                items.forEachIndexed { itemIndex, item ->
                    appendLine("          {")
                    appendLine("            \"title\": \"${item.title.replace("\"", "\\\"")}\",")
                    appendLine("            \"path\": \"${item.videoPath.replace("\"", "\\\"")}\",")
                    appendLine("            \"duration\": ${item.duration},")
                    appendLine("            \"position\": ${item.position}")
                    appendLine("          }${if (itemIndex < items.size - 1) "," else ""}")
                }
                appendLine("        ]")
                appendLine("      }${if (playlistIndex < playlistsWithItems.size - 1) "," else ""}")
            }
            appendLine("    ]")
            appendLine("  }")
            append("}")
        }
    }
    
    private fun formatMultiplePlaylistsTxt(playlistsWithItems: List<Pair<PlaylistEntity, List<PlaylistItem>>>): String {
        return buildString {
            appendLine("=== Astral Player Playlists Backup ===")
            appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("Total Playlists: ${playlistsWithItems.size}")
            appendLine()
            
            playlistsWithItems.forEachIndexed { index, (playlist, items) ->
                appendLine("PLAYLIST ${index + 1}: ${playlist.name}")
                appendLine("Description: ${playlist.description}")
                appendLine("Created: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(playlist.createdTime))}")
                appendLine("Items: ${items.size}")
                appendLine()
                items.forEachIndexed { itemIndex, item ->
                    appendLine("  ${itemIndex + 1}. ${item.title}")
                    appendLine("     ${item.videoPath}")
                }
                appendLine()
                appendLine("${"-".repeat(50)}")
                appendLine()
            }
            appendLine("Backup generated by Astral Player")
        }
    }
    
    private fun getMimeType(format: ExportFormat): String {
        return when (format) {
            ExportFormat.TXT -> "text/plain"
            ExportFormat.JSON -> "application/json"
            ExportFormat.CSV -> "text/csv"
            ExportFormat.HTML -> "text/html"
            ExportFormat.M3U -> "audio/x-mpegurl"
        }
    }
    
    private fun getShareSubject(type: ExportType): String {
        return when (type) {
            ExportType.VIDEO_STATS -> "Video Statistics Report"
            ExportType.PLAYLIST -> "Playlist Export"
            ExportType.MULTIPLE_PLAYLISTS -> "Playlists Backup"
        }
    }
    
    private fun getShareText(type: ExportType): String {
        return when (type) {
            ExportType.VIDEO_STATS -> "Video statistics report generated by Astral Player"
            ExportType.PLAYLIST -> "Playlist exported from Astral Player"
            ExportType.MULTIPLE_PLAYLISTS -> "Playlists backup from Astral Player"
        }
    }
    
    private fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.inWholeSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        if (bytes == 0L) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "${"%.2f".format(size)} ${units[unitIndex]}"
    }
    
    private fun getChannelDescription(channelCount: Int): String {
        return when (channelCount) {
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1 Surround"
            7 -> "6.1 Surround"
            8 -> "7.1 Surround"
            else -> "$channelCount channels"
        }
    }
}

sealed class ExportResult {
    data class Success(val file: File, val format: ExportFormat, val type: ExportType) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

enum class ExportFormat(val displayName: String, val extension: String) {
    TXT("Plain Text", "txt"),
    JSON("JSON", "json"),
    CSV("CSV", "csv"),
    HTML("HTML", "html"),
    M3U("M3U Playlist", "m3u")
}

enum class ExportType {
    VIDEO_STATS,
    PLAYLIST,
    MULTIPLE_PLAYLISTS
}