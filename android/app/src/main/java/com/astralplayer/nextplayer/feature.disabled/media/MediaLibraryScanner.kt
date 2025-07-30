package com.astralplayer.nextplayer.feature.media

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Comprehensive media library scanner for AstralStream
 * Scans device for video files, generates thumbnails, and extracts metadata
 */
class MediaLibraryScanner(private val context: Context) {
    
    companion object {
        private const val TAG = "MediaLibraryScanner"
        
        // Supported video formats
        private val SUPPORTED_VIDEO_FORMATS = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", 
            "3gp", "3g2", "ts", "mts", "m2ts", "vob", "asf", "rm", 
            "rmvb", "ogv", "divx", "xvid", "f4v"
        )
        
        // Video projections for MediaStore queries
        private val VIDEO_PROJECTION = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BITRATE
        )
    }
    
    private val _scanningState = MutableStateFlow(ScanningState.IDLE)
    val scanningState: Flow<ScanningState> = _scanningState.asStateFlow()
    
    private val _videoLibrary = MutableStateFlow<List<VideoFile>>(emptyList())
    val videoLibrary: Flow<List<VideoFile>> = _videoLibrary.asStateFlow()
    
    private val _scanProgress = MutableStateFlow(0)
    val scanProgress: Flow<Int> = _scanProgress.asStateFlow()
    
    /**
     * Start comprehensive media library scan
     */
    suspend fun scanMediaLibrary(
        includeHidden: Boolean = false,
        generateThumbnails: Boolean = true
    ) {
        withContext(Dispatchers.IO) {
            try {
                _scanningState.value = ScanningState.SCANNING
                Log.d(TAG, "Starting media library scan")
                
                val videoFiles = mutableListOf<VideoFile>()
                
                // Scan using MediaStore (primary method)
                val mediaStoreFiles = scanWithMediaStore()
                videoFiles.addAll(mediaStoreFiles)
                
                // Scan file system for additional files (secondary method)
                if (includeHidden) {
                    val fileSystemFiles = scanFileSystem()
                    // Merge with MediaStore results, avoiding duplicates
                    val existingPaths = mediaStoreFiles.map { it.path }.toSet()
                    videoFiles.addAll(fileSystemFiles.filter { it.path !in existingPaths })
                }
                
                Log.d(TAG, "Found ${videoFiles.size} video files")
                
                // Generate thumbnails if requested
                if (generateThumbnails) {
                    _scanningState.value = ScanningState.GENERATING_THUMBNAILS
                    generateThumbnailsForVideos(videoFiles)
                }
                
                // Sort by date modified (newest first)
                val sortedFiles = videoFiles.sortedByDescending { it.dateModified }
                
                _videoLibrary.value = sortedFiles
                _scanningState.value = ScanningState.COMPLETED
                
                Log.d(TAG, "Media library scan completed with ${sortedFiles.size} videos")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during media library scan", e)
                _scanningState.value = ScanningState.ERROR
            }
        }
    }
    
    /**
     * Scan videos using MediaStore API (Android's media database)
     */
    private suspend fun scanWithMediaStore(): List<VideoFile> {
        return withContext(Dispatchers.IO) {
            val videoFiles = mutableListOf<VideoFile>()
            val contentResolver = context.contentResolver
            
            try {
                val cursor = contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    VIDEO_PROJECTION,
                    "${MediaStore.Video.Media.SIZE} > ?",
                    arrayOf("1024"), // Minimum size 1KB
                    "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
                )
                
                cursor?.use { c ->
                    val idColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val pathColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val durationColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val dateModifiedColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                    val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                    val widthColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                    val heightColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                    
                    var processedCount = 0
                    val totalCount = c.count
                    
                    while (c.moveToNext()) {
                        try {
                            val id = c.getLong(idColumn)
                            val name = c.getString(nameColumn) ?: "Unknown"
                            val path = c.getString(pathColumn) ?: continue
                            val size = c.getLong(sizeColumn)
                            val duration = c.getLong(durationColumn)
                            val dateAdded = c.getLong(dateAddedColumn) * 1000 // Convert to milliseconds
                            val dateModified = c.getLong(dateModifiedColumn) * 1000
                            val mimeType = c.getString(mimeTypeColumn) ?: ""
                            val width = c.getInt(widthColumn)
                            val height = c.getInt(heightColumn)
                            
                            // Verify file still exists
                            val file = File(path)
                            if (file.exists() && file.isFile) {
                                val videoFile = VideoFile(
                                    id = id,
                                    name = name,
                                    path = path,
                                    uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString()),
                                    size = size,
                                    duration = duration,
                                    dateAdded = dateAdded,
                                    dateModified = dateModified,
                                    mimeType = mimeType,
                                    width = width,
                                    height = height,
                                    parentFolder = file.parent ?: "",
                                    thumbnailPath = null,
                                    resolution = if (width > 0 && height > 0) "${width}x${height}" else "Unknown"
                                )
                                
                                videoFiles.add(videoFile)
                            }
                            
                            processedCount++
                            _scanProgress.value = (processedCount * 100 / totalCount)
                            
                        } catch (e: Exception) {
                            Log.w(TAG, "Error processing MediaStore entry", e)
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error querying MediaStore", e)
            }
            
            videoFiles
        }
    }
    
    /**
     * Scan file system directly for video files (fallback method)
     */
    private suspend fun scanFileSystem(): List<VideoFile> {
        return withContext(Dispatchers.IO) {
            val videoFiles = mutableListOf<VideoFile>()
            
            try {
                // Common directories to scan
                val directoriesToScan = listOf(
                    "/storage/emulated/0/DCIM",
                    "/storage/emulated/0/Movies",
                    "/storage/emulated/0/Download",
                    "/storage/emulated/0/Videos",
                    "/storage/emulated/0/WhatsApp/Media/WhatsApp Video",
                    "/storage/emulated/0/Telegram/Telegram Video"
                )
                
                directoriesToScan.forEach { dirPath ->
                    val directory = File(dirPath)
                    if (directory.exists() && directory.isDirectory) {
                        scanDirectory(directory, videoFiles)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during file system scan", e)
            }
            
            videoFiles
        }
    }
    
    /**
     * Recursively scan a directory for video files
     */
    private fun scanDirectory(directory: File, videoFiles: MutableList<VideoFile>) {
        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isDirectory && !file.name.startsWith(".") -> {
                        // Recursively scan subdirectories (avoid hidden directories)
                        scanDirectory(file, videoFiles)
                    }
                    file.isFile && isVideoFile(file) -> {
                        val videoFile = createVideoFileFromFile(file)
                        if (videoFile != null) {
                            videoFiles.add(videoFile)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied accessing directory: ${directory.path}")
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning directory: ${directory.path}", e)
        }
    }
    
    /**
     * Check if file is a supported video format
     */
    private fun isVideoFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return SUPPORTED_VIDEO_FORMATS.contains(extension) && file.length() > 1024 // Minimum 1KB
    }
    
    /**
     * Create VideoFile object from File
     */
    private fun createVideoFileFromFile(file: File): VideoFile? {
        return try {
            val uri = Uri.fromFile(file)
            val metadata = extractVideoMetadata(file.path)
            
            VideoFile(
                id = file.path.hashCode().toLong(),
                name = file.name,
                path = file.path,
                uri = uri,
                size = file.length(),
                duration = metadata.duration,
                dateAdded = file.lastModified(),
                dateModified = file.lastModified(),
                mimeType = getMimeTypeForFile(file),
                width = metadata.width,
                height = metadata.height,
                parentFolder = file.parent ?: "",
                thumbnailPath = null,
                resolution = if (metadata.width > 0 && metadata.height > 0) {
                    "${metadata.width}x${metadata.height}"
                } else "Unknown"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error creating VideoFile for: ${file.path}", e)
            null
        }
    }
    
    /**
     * Extract metadata from video file
     */
    private fun extractVideoMetadata(filePath: String): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            
            VideoMetadata(duration, width, height)
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting metadata for: $filePath", e)
            VideoMetadata(0L, 0, 0)
        } finally {
            try {
                retriever.release()
            } catch (e: IOException) {
                Log.w(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }
    }
    
    /**
     * Generate thumbnails for video files
     */
    private suspend fun generateThumbnailsForVideos(videoFiles: List<VideoFile>) {
        withContext(Dispatchers.IO) {
            val thumbnailsDir = File(context.cacheDir, "video_thumbnails")
            if (!thumbnailsDir.exists()) {
                thumbnailsDir.mkdirs()
            }
            
            videoFiles.forEachIndexed { index, videoFile ->
                try {
                    val thumbnailFile = File(thumbnailsDir, "${videoFile.id}.jpg")
                    
                    if (!thumbnailFile.exists()) {
                        val thumbnail = generateThumbnail(videoFile.path)
                        if (thumbnail != null) {
                            saveThumbnailToFile(thumbnail, thumbnailFile)
                            // Update the video file with thumbnail path
                            val updatedFile = videoFile.copy(thumbnailPath = thumbnailFile.path)
                            // Update in the list (this is a simplified approach)
                        }
                    } else {
                        // Thumbnail already exists
                        val updatedFile = videoFile.copy(thumbnailPath = thumbnailFile.path)
                    }
                    
                    _scanProgress.value = ((index + 1) * 100 / videoFiles.size)
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Error generating thumbnail for: ${videoFile.path}", e)
                }
            }
        }
    }
    
    /**
     * Generate thumbnail for a video file
     */
    private fun generateThumbnail(videoPath: String): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use new API for Android 10+
                ThumbnailUtils.createVideoThumbnail(
                    File(videoPath),
                    Size(320, 240),
                    null
                )
            } else {
                // Use legacy API for older versions
                @Suppress("DEPRECATION")
                ThumbnailUtils.createVideoThumbnail(
                    videoPath,
                    MediaStore.Video.Thumbnails.MINI_KIND
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error creating thumbnail for: $videoPath", e)
            null
        }
    }
    
    /**
     * Save bitmap thumbnail to file
     */
    private fun saveThumbnailToFile(bitmap: Bitmap, file: File) {
        try {
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error saving thumbnail to file: ${file.path}", e)
        }
    }
    
    /**
     * Get MIME type for file based on extension
     */
    private fun getMimeTypeForFile(file: File): String {
        return when (file.extension.lowercase()) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "flv" -> "video/x-flv"
            "webm" -> "video/webm"
            "m4v" -> "video/x-m4v"
            "3gp" -> "video/3gpp"
            "3g2" -> "video/3gpp2"
            else -> "video/*"
        }
    }
    
    /**
     * Get videos by folder
     */
    fun getVideosByFolder(): Map<String, List<VideoFile>> {
        return _videoLibrary.value.groupBy { 
            File(it.parentFolder).name.takeIf { it.isNotEmpty() } ?: "Root"
        }
    }
    
    /**
     * Search videos by name
     */
    fun searchVideos(query: String): List<VideoFile> {
        if (query.isBlank()) return _videoLibrary.value
        
        val lowerQuery = query.lowercase()
        return _videoLibrary.value.filter { videoFile ->
            videoFile.name.lowercase().contains(lowerQuery) ||
            videoFile.parentFolder.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * Get videos by duration range
     */
    fun getVideosByDuration(minDuration: Long, maxDuration: Long): List<VideoFile> {
        return _videoLibrary.value.filter { it.duration in minDuration..maxDuration }
    }
    
    /**
     * Get recently added videos
     */
    fun getRecentlyAddedVideos(limitDays: Int = 7): List<VideoFile> {
        val cutoffTime = System.currentTimeMillis() - (limitDays * 24 * 60 * 60 * 1000L)
        return _videoLibrary.value.filter { it.dateAdded > cutoffTime }
    }
    
    /**
     * Get video statistics
     */
    fun getLibraryStatistics(): LibraryStatistics {
        val videos = _videoLibrary.value
        val totalSize = videos.sumOf { it.size }
        val totalDuration = videos.sumOf { it.duration }
        val averageSize = if (videos.isNotEmpty()) totalSize / videos.size else 0L
        val averageDuration = if (videos.isNotEmpty()) totalDuration / videos.size else 0L
        
        return LibraryStatistics(
            totalVideos = videos.size,
            totalSize = totalSize,
            totalDuration = totalDuration,
            averageSize = averageSize,
            averageDuration = averageDuration,
            folders = getVideosByFolder().size
        )
    }
}

/**
 * Data classes for media library
 */
data class VideoFile(
    val id: Long,
    val name: String,
    val path: String,
    val uri: Uri,
    val size: Long,
    val duration: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val parentFolder: String,
    val thumbnailPath: String?,
    val resolution: String
) {
    val formattedSize: String
        get() = when {
            size >= 1024 * 1024 * 1024 -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.1f KB", size / 1024.0)
            else -> "$size B"
        }
    
    val formattedDuration: String
        get() {
            val seconds = duration / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, secs)
            } else {
                String.format("%d:%02d", minutes, secs)
            }
        }
    
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dateModified))
}

data class VideoMetadata(
    val duration: Long,
    val width: Int,
    val height: Int
)

data class LibraryStatistics(
    val totalVideos: Int,
    val totalSize: Long,
    val totalDuration: Long,
    val averageSize: Long,
    val averageDuration: Long,
    val folders: Int
)

enum class ScanningState {
    IDLE,
    SCANNING,
    GENERATING_THUMBNAILS,
    COMPLETED,
    ERROR
}