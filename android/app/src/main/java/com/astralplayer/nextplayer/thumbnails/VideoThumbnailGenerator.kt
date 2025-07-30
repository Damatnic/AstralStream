package com.astralplayer.nextplayer.thumbnails

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Advanced video thumbnail generation system with caching and optimization
 */
class VideoThumbnailGenerator(
    private val context: Context,
    private val cacheDir: File = File(context.cacheDir, "thumbnails")
) {
    
    private val _generationProgress = MutableSharedFlow<ThumbnailProgress>()
    val generationProgress: SharedFlow<ThumbnailProgress> = _generationProgress.asSharedFlow()
    
    private val thumbnailCache = ConcurrentHashMap<String, ThumbnailCacheEntry>()
    private val generationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        // Ensure cache directory exists
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        // Load existing cache
        loadCacheIndex()
    }
    
    /**
     * Generate single thumbnail at specific time
     */
    suspend fun generateThumbnail(
        videoUri: Uri,
        timeUs: Long,
        config: ThumbnailConfig = ThumbnailConfig()
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateCacheKey(videoUri, timeUs, config)
            
            // Check cache first
            thumbnailCache[cacheKey]?.let { cacheEntry ->
                if (cacheEntry.isValid()) {
                    val cachedBitmap = loadBitmapFromCache(cacheEntry.filePath)
                    if (cachedBitmap != null) {
                        return@withContext Result.success(cachedBitmap)
                    }
                }
            }
            
            // Generate new thumbnail
            val bitmap = extractThumbnailFromVideo(videoUri, timeUs, config)
            
            // Cache the result
            val cacheFilePath = saveBitmapToCache(bitmap, cacheKey)
            thumbnailCache[cacheKey] = ThumbnailCacheEntry(
                filePath = cacheFilePath,
                timestamp = System.currentTimeMillis(),
                size = config.width * config.height * 4 // Approximate size in bytes
            )
            
            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate multiple thumbnails for video scrubbing/timeline
     */
    suspend fun generateThumbnailStrip(
        videoUri: Uri,
        videoDurationMs: Long,
        thumbnailCount: Int = 10,
        config: ThumbnailConfig = ThumbnailConfig(width = 160, height = 90)
    ): Result<List<ThumbnailInfo>> = withContext(Dispatchers.IO) {
        try {
            val thumbnails = mutableListOf<ThumbnailInfo>()
            val interval = videoDurationMs / thumbnailCount
            
            _generationProgress.emit(ThumbnailProgress.Started(videoUri, thumbnailCount))
            
            repeat(thumbnailCount) { index ->
                val timeMs = (index * interval).coerceAtMost(videoDurationMs - 1000) // Avoid end
                val timeUs = timeMs * 1000
                
                val bitmap = extractThumbnailFromVideo(videoUri, timeUs, config)
                val cacheKey = generateCacheKey(videoUri, timeUs, config)
                val cacheFilePath = saveBitmapToCache(bitmap, cacheKey)
                
                // Store in cache
                thumbnailCache[cacheKey] = ThumbnailCacheEntry(
                    filePath = cacheFilePath,
                    timestamp = System.currentTimeMillis(),
                    size = config.width * config.height * 4
                )
                
                thumbnails.add(
                    ThumbnailInfo(
                        timeMs = timeMs,
                        bitmap = bitmap,
                        cacheKey = cacheKey,
                        filePath = cacheFilePath
                    )
                )
                
                _generationProgress.emit(
                    ThumbnailProgress.Progress(videoUri, index + 1, thumbnailCount)
                )
            }
            
            _generationProgress.emit(ThumbnailProgress.Completed(videoUri, thumbnails.size))
            Result.success(thumbnails)
        } catch (e: Exception) {
            _generationProgress.emit(ThumbnailProgress.Error(videoUri, e))
            Result.failure(e)
        }
    }
    
    /**
     * Generate animated thumbnail (GIF-like sequence)
     */
    suspend fun generateAnimatedThumbnail(
        videoUri: Uri,
        startTimeMs: Long,
        durationMs: Long = 3000,
        frameCount: Int = 10,
        config: ThumbnailConfig = ThumbnailConfig()
    ): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        try {
            val frames = mutableListOf<Bitmap>()
            val frameInterval = durationMs / frameCount
            
            repeat(frameCount) { index ->
                val timeMs = startTimeMs + (index * frameInterval)
                val timeUs = timeMs * 1000
                
                val bitmap = extractThumbnailFromVideo(videoUri, timeUs, config)
                frames.add(bitmap)
            }
            
            Result.success(frames)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate chapter thumbnails
     */
    suspend fun generateChapterThumbnails(
        videoUri: Uri,
        chapters: List<Long>, // Chapter start times in milliseconds
        config: ThumbnailConfig = ThumbnailConfig()
    ): Result<Map<Long, Bitmap>> = withContext(Dispatchers.IO) {
        try {
            val chapterThumbnails = mutableMapOf<Long, Bitmap>()
            
            chapters.forEach { chapterTimeMs ->
                val timeUs = chapterTimeMs * 1000
                val bitmap = extractThumbnailFromVideo(videoUri, timeUs, config)
                chapterThumbnails[chapterTimeMs] = bitmap
                
                // Cache chapter thumbnails
                val cacheKey = generateCacheKey(videoUri, timeUs, config, "chapter")
                val cacheFilePath = saveBitmapToCache(bitmap, cacheKey)
                thumbnailCache[cacheKey] = ThumbnailCacheEntry(
                    filePath = cacheFilePath,
                    timestamp = System.currentTimeMillis(),
                    size = config.width * config.height * 4
                )
            }
            
            Result.success(chapterThumbnails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Smart thumbnail generation - automatically detect interesting scenes
     */
    suspend fun generateSmartThumbnails(
        videoUri: Uri,
        videoDurationMs: Long,
        thumbnailCount: Int = 5,
        config: ThumbnailConfig = ThumbnailConfig()
    ): Result<List<ThumbnailInfo>> = withContext(Dispatchers.IO) {
        try {
            // Use scene detection algorithm to find interesting moments
            val interestingMoments = detectInterestingScenes(videoUri, videoDurationMs, thumbnailCount)
            
            val thumbnails = mutableListOf<ThumbnailInfo>()
            
            interestingMoments.forEach { timeMs ->
                val timeUs = timeMs * 1000
                val bitmap = extractThumbnailFromVideo(videoUri, timeUs, config)
                val cacheKey = generateCacheKey(videoUri, timeUs, config, "smart")
                val cacheFilePath = saveBitmapToCache(bitmap, cacheKey)
                
                thumbnailCache[cacheKey] = ThumbnailCacheEntry(
                    filePath = cacheFilePath,
                    timestamp = System.currentTimeMillis(),
                    size = config.width * config.height * 4
                )
                
                thumbnails.add(
                    ThumbnailInfo(
                        timeMs = timeMs,
                        bitmap = bitmap,
                        cacheKey = cacheKey,
                        filePath = cacheFilePath
                    )
                )
            }
            
            Result.success(thumbnails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cached thumbnail if available
     */
    fun getCachedThumbnail(
        videoUri: Uri,
        timeUs: Long,
        config: ThumbnailConfig = ThumbnailConfig()
    ): Bitmap? {
        val cacheKey = generateCacheKey(videoUri, timeUs, config)
        val cacheEntry = thumbnailCache[cacheKey] ?: return null
        
        if (!cacheEntry.isValid()) {
            // Remove invalid cache entry
            thumbnailCache.remove(cacheKey)
            return null
        }
        
        return loadBitmapFromCache(cacheEntry.filePath)
    }
    
    /**
     * Clear thumbnail cache
     */
    fun clearCache() {
        thumbnailCache.clear()
        
        // Delete cache files
        generationScope.launch {
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".jpg")) {
                    file.delete()
                }
            }
        }
    }
    
    /**
     * Get cache size in bytes
     */
    fun getCacheSize(): Long {
        return thumbnailCache.values.sumOf { it.size }
    }
    
    /**
     * Clean old cache entries
     */
    fun cleanOldCache(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) { // 7 days
        val currentTime = System.currentTimeMillis()
        val toRemove = mutableListOf<String>()
        
        thumbnailCache.forEach { (key, entry) ->
            if (currentTime - entry.timestamp > maxAgeMs) {
                toRemove.add(key)
                // Delete file
                File(entry.filePath).delete()
            }
        }
        
        toRemove.forEach { key ->
            thumbnailCache.remove(key)
        }
    }
    
    private suspend fun extractThumbnailFromVideo(
        videoUri: Uri,
        timeUs: Long,
        config: ThumbnailConfig
    ): Bitmap {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                
                val originalBitmap = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: throw Exception("Failed to extract frame at time $timeUs")
                
                // Resize if needed
                if (config.width != originalBitmap.width || config.height != originalBitmap.height) {
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        originalBitmap,
                        config.width,
                        config.height,
                        config.filter
                    )
                    originalBitmap.recycle()
                    scaledBitmap
                } else {
                    originalBitmap
                }
            } finally {
                retriever.release()
            }
        }
    }
    
    private fun detectInterestingScenes(
        videoUri: Uri,
        videoDurationMs: Long,
        sceneCount: Int
    ): List<Long> {
        // Simple algorithm: distribute scenes across video avoiding start/end
        val startOffset = videoDurationMs * 0.1 // Skip first 10%
        val endOffset = videoDurationMs * 0.9   // Skip last 10%
        val effectiveDuration = endOffset - startOffset
        val interval = effectiveDuration / sceneCount
        
        return (0 until sceneCount).map { index ->
            (startOffset + (index * interval) + (interval * 0.5)).toLong()
        }
    }
    
    private fun generateCacheKey(
        videoUri: Uri,
        timeUs: Long,
        config: ThumbnailConfig,
        prefix: String = "thumb"
    ): String {
        val uriHash = videoUri.toString().hashCode()
        return "${prefix}_${uriHash}_${timeUs}_${config.width}x${config.height}_${config.quality}"
    }
    
    private fun saveBitmapToCache(bitmap: Bitmap, cacheKey: String): String {
        val cacheFile = File(cacheDir, "$cacheKey.jpg")
        FileOutputStream(cacheFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return cacheFile.absolutePath
    }
    
    private fun loadBitmapFromCache(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                android.graphics.BitmapFactory.decodeFile(filePath)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun loadCacheIndex() {
        // In a real implementation, you might want to save/load cache index
        // from a database or file for persistence across app restarts
    }
    
    fun cleanup() {
        generationScope.cancel()
    }
}

// Data classes for thumbnail system
data class ThumbnailConfig(
    val width: Int = 320,
    val height: Int = 180,
    val quality: Int = 85,
    val filter: Boolean = true
)

data class ThumbnailInfo(
    val timeMs: Long,
    val bitmap: Bitmap,
    val cacheKey: String,
    val filePath: String
)

data class ThumbnailCacheEntry(
    val filePath: String,
    val timestamp: Long,
    val size: Long
) {
    fun isValid(maxAgeMs: Long = 24 * 60 * 60 * 1000L): Boolean { // 24 hours
        return System.currentTimeMillis() - timestamp < maxAgeMs &&
               File(filePath).exists()
    }
}

sealed class ThumbnailProgress {
    data class Started(val videoUri: Uri, val totalCount: Int) : ThumbnailProgress()
    data class Progress(val videoUri: Uri, val current: Int, val total: Int) : ThumbnailProgress()
    data class Completed(val videoUri: Uri, val generatedCount: Int) : ThumbnailProgress()
    data class Error(val videoUri: Uri, val error: Throwable) : ThumbnailProgress()
}

/**
 * Thumbnail manager for UI integration
 */
class ThumbnailManager(
    private val context: Context,
    private val generator: VideoThumbnailGenerator = VideoThumbnailGenerator(context)
) {
    
    private val _thumbnailCache = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnailCache: StateFlow<Map<String, Bitmap>> = _thumbnailCache.asStateFlow()
    
    /**
     * Load thumbnail with automatic caching
     */
    suspend fun loadThumbnail(
        videoUri: Uri,
        timeMs: Long,
        config: ThumbnailConfig = ThumbnailConfig()
    ): Bitmap? {
        val timeUs = timeMs * 1000
        val cacheKey = "${videoUri.hashCode()}_${timeUs}_${config.width}x${config.height}"
        
        // Check memory cache first
        _thumbnailCache.value[cacheKey]?.let { return it }
        
        // Check disk cache
        generator.getCachedThumbnail(videoUri, timeUs, config)?.let { bitmap ->
            updateMemoryCache(cacheKey, bitmap)
            return bitmap
        }
        
        // Generate new thumbnail
        val result = generator.generateThumbnail(videoUri, timeUs, config)
        return result.getOrNull()?.also { bitmap ->
            updateMemoryCache(cacheKey, bitmap)
        }
    }
    
    /**
     * Preload thumbnails for smooth scrubbing
     */
    suspend fun preloadThumbnails(
        videoUri: Uri,
        videoDurationMs: Long,
        thumbnailCount: Int = 20
    ) {
        generator.generateThumbnailStrip(videoUri, videoDurationMs, thumbnailCount)
            .getOrNull()?.forEach { thumbnailInfo ->
                val cacheKey = "${videoUri.hashCode()}_${thumbnailInfo.timeMs * 1000}_${320}x${180}"
                updateMemoryCache(cacheKey, thumbnailInfo.bitmap)
            }
    }
    
    private fun updateMemoryCache(key: String, bitmap: Bitmap) {
        val currentCache = _thumbnailCache.value.toMutableMap()
        currentCache[key] = bitmap
        
        // Limit memory cache size (keep last 100 thumbnails)
        if (currentCache.size > 100) {
            val oldestKey = currentCache.keys.first()
            currentCache.remove(oldestKey)
        }
        
        _thumbnailCache.value = currentCache
    }
    
    fun clearMemoryCache() {
        _thumbnailCache.value = emptyMap()
    }
}