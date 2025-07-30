package com.astralplayer.nextplayer.feature.thumbnails

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.min

data class ThumbnailInfo(
    val videoPath: String,
    val timestamp: Long,
    val bitmap: Bitmap?,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ThumbnailCacheEntry(
    val bitmap: Bitmap,
    val lastAccessed: Long,
    val fileSize: Long
)

class VideoThumbnailManager(private val context: Context) {
    
    private val thumbnailCache = mutableMapOf<String, ThumbnailCacheEntry>()
    private val maxCacheSize = 50 * 1024 * 1024 // 50MB cache limit
    private var currentCacheSize = 0L
    
    private val cacheDir = File(context.cacheDir, "video_thumbnails").apply {
        if (!exists()) mkdirs()
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Generate thumbnail for video at specific timestamp
     */
    suspend fun generateThumbnail(
        videoPath: String,
        timestampUs: Long = 0L,
        width: Int = 320,
        height: Int = 180
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(videoPath, timestampUs, width, height)
        
        // Check memory cache first
        thumbnailCache[cacheKey]?.let { entry ->
            entry.copy(lastAccessed = System.currentTimeMillis())
            return@withContext entry.bitmap
        }
        
        // Check disk cache
        val diskCacheFile = File(cacheDir, "$cacheKey.jpg")
        if (diskCacheFile.exists()) {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeFile(diskCacheFile.absolutePath)
                if (bitmap != null) {
                    addToMemoryCache(cacheKey, bitmap)
                    return@withContext bitmap
                }
            } catch (e: Exception) {
                diskCacheFile.delete()
            }
        }
        
        // Generate new thumbnail
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            val originalBitmap = if (timestampUs > 0) {
                retriever.getFrameAtTime(timestampUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } else {
                retriever.frameAtTime
            }
            
            retriever.release()
            
            originalBitmap?.let { bitmap ->
                val scaledBitmap = scaleBitmap(bitmap, width, height)
                
                // Save to disk cache
                saveToDiskCache(diskCacheFile, scaledBitmap)
                
                // Add to memory cache
                addToMemoryCache(cacheKey, scaledBitmap)
                
                scaledBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Generate multiple thumbnails for video scrubbing
     */
    suspend fun generateThumbnailStrip(
        videoPath: String,
        count: Int = 10,
        width: Int = 160,
        height: Int = 90
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val thumbnails = mutableListOf<Bitmap>()
        
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationString?.toLongOrNull() ?: 0L
            
            if (duration > 0) {
                val interval = duration * 1000 / count // Convert to microseconds
                
                repeat(count) { index ->
                    val timestamp = index * interval
                    try {
                        val bitmap = retriever.getFrameAtTime(
                            timestamp,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                        bitmap?.let { 
                            thumbnails.add(scaleBitmap(it, width, height))
                        }
                    } catch (e: Exception) {
                        // Skip this thumbnail if extraction fails
                    }
                }
            }
            
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        thumbnails
    }
    
    /**
     * Get video metadata for thumbnail generation
     */
    suspend fun getVideoMetadata(videoPath: String): VideoMetadata? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            
            retriever.release()
            
            VideoMetadata(
                duration = duration,
                width = width,
                height = height,
                rotation = rotation,
                aspectRatio = if (height > 0) width.toFloat() / height else 16f / 9f
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Preload thumbnails for a list of videos
     */
    fun preloadThumbnails(videoPaths: List<String>) {
        coroutineScope.launch {
            videoPaths.forEach { path ->
                launch {
                    generateThumbnail(path)
                }
            }
        }
    }
    
    /**
     * Clear cache to free memory
     */
    fun clearCache() {
        thumbnailCache.clear()
        currentCacheSize = 0L
        
        // Clear disk cache
        cacheDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            memoryEntries = thumbnailCache.size,
            memorySizeBytes = currentCacheSize,
            diskEntries = cacheDir.listFiles()?.size ?: 0,
            diskSizeBytes = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        )
    }
    
    private fun generateCacheKey(videoPath: String, timestamp: Long, width: Int, height: Int): String {
        val input = "$videoPath:$timestamp:$width:$height"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    private fun scaleBitmap(original: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val aspectRatio = original.width.toFloat() / original.height.toFloat()
        val targetAspectRatio = targetWidth.toFloat() / targetHeight.toFloat()
        
        val (scaledWidth, scaledHeight) = if (aspectRatio > targetAspectRatio) {
            targetWidth to (targetWidth / aspectRatio).toInt()
        } else {
            (targetHeight * aspectRatio).toInt() to targetHeight
        }
        
        return Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true)
    }
    
    private fun addToMemoryCache(key: String, bitmap: Bitmap) {
        val bitmapSize = bitmap.byteCount.toLong()
        
        // Remove old entries if cache is full
        while (currentCacheSize + bitmapSize > maxCacheSize && thumbnailCache.isNotEmpty()) {
            val oldestEntry = thumbnailCache.minByOrNull { it.value.lastAccessed }
            oldestEntry?.let { (oldKey, oldEntry) ->
                thumbnailCache.remove(oldKey)
                currentCacheSize -= oldEntry.fileSize
            }
        }
        
        thumbnailCache[key] = ThumbnailCacheEntry(
            bitmap = bitmap,
            lastAccessed = System.currentTimeMillis(),
            fileSize = bitmapSize
        )
        currentCacheSize += bitmapSize
    }
    
    private fun saveToDiskCache(file: File, bitmap: Bitmap) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun release() {
        coroutineScope.cancel()
        clearCache()
    }
}

data class VideoMetadata(
    val duration: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val aspectRatio: Float
)

data class CacheStats(
    val memoryEntries: Int,
    val memorySizeBytes: Long,
    val diskEntries: Int,
    val diskSizeBytes: Long
)

class ThumbnailViewModel : ViewModel() {
    
    private val _thumbnailState = MutableStateFlow<Map<String, ThumbnailInfo>>(emptyMap())
    val thumbnailState: StateFlow<Map<String, ThumbnailInfo>> = _thumbnailState.asStateFlow()
    
    private lateinit var thumbnailManager: VideoThumbnailManager
    
    fun initialize(context: Context) {
        thumbnailManager = VideoThumbnailManager(context)
    }
    
    fun loadThumbnail(videoPath: String, timestamp: Long = 0L) {
        viewModelScope.launch {
            // Set loading state
            _thumbnailState.value = _thumbnailState.value + (videoPath to ThumbnailInfo(
                videoPath = videoPath,
                timestamp = timestamp,
                bitmap = null,
                isLoading = true
            ))
            
            try {
                val bitmap = thumbnailManager.generateThumbnail(videoPath, timestamp)
                _thumbnailState.value = _thumbnailState.value + (videoPath to ThumbnailInfo(
                    videoPath = videoPath,
                    timestamp = timestamp,
                    bitmap = bitmap,
                    isLoading = false
                ))
            } catch (e: Exception) {
                _thumbnailState.value = _thumbnailState.value + (videoPath to ThumbnailInfo(
                    videoPath = videoPath,
                    timestamp = timestamp,
                    bitmap = null,
                    isLoading = false,
                    error = e.message
                ))
            }
        }
    }
    
    fun preloadThumbnails(videoPaths: List<String>) {
        thumbnailManager.preloadThumbnails(videoPaths)
    }
    
    fun clearCache() {
        thumbnailManager.clearCache()
        _thumbnailState.value = emptyMap()
    }
    
    override fun onCleared() {
        super.onCleared()
        if (::thumbnailManager.isInitialized) {
            thumbnailManager.release()
        }
    }
}

@Composable
fun VideoThumbnail(
    videoPath: String,
    timestamp: Long = 0L,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showDuration: Boolean = false,
    viewModel: ThumbnailViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val thumbnailState by viewModel.thumbnailState.collectAsState()
    
    LaunchedEffect(videoPath) {
        viewModel.initialize(context)
        viewModel.loadThumbnail(videoPath, timestamp)
    }
    
    val thumbnailInfo = thumbnailState[videoPath]
    
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        when {
            thumbnailInfo?.isLoading == true -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00BCD4),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            thumbnailInfo?.bitmap != null -> {
                // Show thumbnail
                Image(
                    bitmap = thumbnailInfo.bitmap.asImageBitmap(),
                    contentDescription = "Video thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            
            thumbnailInfo?.error != null -> {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.VideoFile,
                        contentDescription = "Video",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            else -> {
                // Default state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.VideoFile,
                        contentDescription = "Video",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        // Duration overlay
        if (showDuration) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(timestamp),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ThumbnailStrip(
    videoPath: String,
    thumbnailCount: Int = 10,
    onThumbnailClick: (Int, Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var thumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    
    LaunchedEffect(videoPath) {
        isLoading = true
        val manager = VideoThumbnailManager(context)
        thumbnails = manager.generateThumbnailStrip(videoPath, thumbnailCount)
        isLoading = false
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isLoading) {
            repeat(thumbnailCount) {
                Box(
                    modifier = Modifier
                        .size(60.dp, 34.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00BCD4),
                        strokeWidth = 1.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            thumbnails.forEachIndexed { index, bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Thumbnail $index",
                    modifier = Modifier
                        .size(60.dp, 34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            val timestamp = (index * 1000L) // Simplified timestamp calculation
                            onThumbnailClick(index, timestamp)
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}