package com.astralplayer.nextplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Enhanced video thumbnail manager with improved caching, loading states and error handling
 * Supports both local and remote video files with async loading
 */
object EnhancedVideoThumbnailManager {
    
    private const val TAG = "VideoThumbnailManager"
    private const val THUMBNAIL_WIDTH = 320
    private const val THUMBNAIL_HEIGHT = 180
    private const val CACHE_SIZE = 20 * 1024 * 1024 // 20MB memory cache
    private const val MAX_RETRIES = 3
    
    // In-memory cache for fast access
    private val memoryCache = LruCache<String, Bitmap>(CACHE_SIZE)
    
    // Mutex to prevent concurrent generation of same thumbnail
    private val generationMutex = Mutex()
    private val ongoingGenerations = mutableSetOf<String>()
    
    /**
     * Get thumbnail with multiple fallback strategies
     */
    suspend fun getThumbnail(context: Context, videoUri: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Check memory cache first
                memoryCache.get(videoUri)?.let { cached ->
                    Log.d(TAG, "Returning cached thumbnail for: ${videoUri.take(50)}...")
                    return@withContext cached
                }
                
                // 2. Check disk cache
                loadThumbnailFromDisk(context, videoUri)?.let { diskCached ->
                    memoryCache.put(videoUri, diskCached)
                    Log.d(TAG, "Loaded thumbnail from disk for: ${videoUri.take(50)}...")
                    return@withContext diskCached
                }
                
                // 3. Generate new thumbnail
                generateAndCacheThumbnail(context, videoUri)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting thumbnail for: ${videoUri.take(50)}...", e)
                null
            }
        }
    }
    
    /**
     * Generate thumbnail with retry logic and concurrent access protection
     */
    private suspend fun generateAndCacheThumbnail(context: Context, videoUri: String): Bitmap? {
        return generationMutex.withLock {
            // Check if already being generated
            if (ongoingGenerations.contains(videoUri)) {
                Log.d(TAG, "Thumbnail generation already in progress for: ${videoUri.take(50)}...")
                return@withLock null
            }
            
            ongoingGenerations.add(videoUri)
            
            try {
                var bitmap: Bitmap? = null
                var attempts = 0
                
                while (bitmap == null && attempts < MAX_RETRIES) {
                    attempts++
                    
                    bitmap = try {
                        generateThumbnailInternal(context, videoUri, attempts)
                    } catch (e: Exception) {
                        Log.w(TAG, "Thumbnail generation attempt $attempts failed for: ${videoUri.take(50)}...", e)
                        null
                    }
                    
                    if (bitmap == null && attempts < MAX_RETRIES) {
                        kotlinx.coroutines.delay(500L * attempts) // Exponential backoff
                    }
                }
                
                bitmap?.let { validBitmap ->
                    // Cache in memory
                    memoryCache.put(videoUri, validBitmap)
                    
                    // Cache on disk
                    saveThumbnailToDisk(context, validBitmap, videoUri)
                    
                    Log.d(TAG, "Successfully generated thumbnail for: ${videoUri.take(50)}...")
                }
                
                bitmap
                
            } finally {
                ongoingGenerations.remove(videoUri)
            }
        }
    }
    
    /**
     * Internal thumbnail generation with multiple time positions
     */
    private suspend fun generateThumbnailInternal(context: Context, videoUri: String, attempt: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            
            try {
                // Handle different URI schemes
                when {
                    videoUri.startsWith("http://") || videoUri.startsWith("https://") -> {
                        retriever.setDataSource(videoUri, hashMapOf())
                    }
                    videoUri.startsWith("content://") -> {
                        retriever.setDataSource(context, Uri.parse(videoUri))
                    }
                    else -> {
                        retriever.setDataSource(videoUri)
                    }
                }
                
                // Get video duration for smart frame selection
                val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationString?.toLongOrNull() ?: 0L
                
                // Try different time positions based on attempt
                val timePositions = when (attempt) {
                    1 -> listOf(1000000L, 5000000L, 10000000L) // 1s, 5s, 10s
                    2 -> listOf(duration / 4, duration / 2, duration * 3 / 4) // 25%, 50%, 75%
                    else -> listOf(2000000L, duration / 8) // 2s, 12.5%
                }.filter { it > 0 && it < duration }.takeIf { it.isNotEmpty() } ?: listOf(1000000L)
                
                for (timePosition in timePositions) {
                    try {
                        val bitmap = retriever.getFrameAtTime(timePosition, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (bitmap != null && !bitmap.isRecycled) {
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true)
                            if (bitmap != scaledBitmap) {
                                bitmap.recycle()
                            }
                            return@withContext scaledBitmap
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Failed to get frame at time $timePosition for attempt $attempt")
                        continue
                    }
                }
                
                null
                
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing MediaMetadataRetriever", e)
                }
            }
        }
    }
    
    /**
     * Load thumbnail from disk cache
     */
    private fun loadThumbnailFromDisk(context: Context, videoUri: String): Bitmap? {
        return try {
            val thumbnailPath = getThumbnailPath(context, videoUri)
            if (File(thumbnailPath).exists()) {
                BitmapFactory.decodeFile(thumbnailPath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading thumbnail from disk", e)
            null
        }
    }
    
    /**
     * Save thumbnail to disk cache
     */
    private suspend fun saveThumbnailToDisk(context: Context, bitmap: Bitmap, videoUri: String) {
        withContext(Dispatchers.IO) {
            try {
                val thumbnailDir = File(context.cacheDir, "thumbnails")
                if (!thumbnailDir.exists()) {
                    thumbnailDir.mkdirs()
                }
                
                val fileName = "${videoUri.hashCode()}.jpg"
                val file = File(thumbnailDir, fileName)
                
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                
                Log.d(TAG, "Saved thumbnail to disk: ${file.absolutePath}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving thumbnail to disk", e)
            }
        }
    }
    
    /**
     * Get thumbnail file path
     */
    fun getThumbnailPath(context: Context, videoUri: String): String {
        val thumbnailDir = File(context.cacheDir, "thumbnails")
        val fileName = "${videoUri.hashCode()}.jpg"
        return File(thumbnailDir, fileName).absolutePath
    }
    
    /**
     * Check if thumbnail exists on disk
     */
    fun thumbnailExists(context: Context, videoUri: String): Boolean {
        val path = getThumbnailPath(context, videoUri)
        return File(path).exists() && File(path).length() > 0
    }
    
    /**
     * Clear memory cache
     */
    fun clearMemoryCache() {
        memoryCache.evictAll()
        Log.d(TAG, "Memory cache cleared")
    }
    
    /**
     * Clear disk cache
     */
    fun clearDiskCache(context: Context) {
        try {
            val thumbnailDir = File(context.cacheDir, "thumbnails")
            thumbnailDir.listFiles()?.forEach { file ->
                file.delete()
            }
            Log.d(TAG, "Disk cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing disk cache", e)
        }
    }
    
    /**
     * Preload thumbnails for a list of URIs
     */
    suspend fun preloadThumbnails(context: Context, videoUris: List<String>) {
        withContext(Dispatchers.IO) {
            videoUris.forEach { uri ->
                try {
                    getThumbnail(context, uri)
                    kotlinx.coroutines.delay(100) // Small delay to prevent overwhelming
                } catch (e: Exception) {
                    Log.w(TAG, "Error preloading thumbnail for: ${uri.take(50)}...", e)
                }
            }
        }
    }
}

// Legacy compatibility
object VideoThumbnailManager {
    suspend fun generateThumbnail(context: Context, videoUri: String): Bitmap? {
        return EnhancedVideoThumbnailManager.getThumbnail(context, videoUri)
    }
    
    suspend fun saveThumbnail(context: Context, bitmap: Bitmap, videoUri: String): String? {
        return EnhancedVideoThumbnailManager.getThumbnailPath(context, videoUri)
    }
    
    fun getThumbnailPath(context: Context, videoUri: String): String {
        return EnhancedVideoThumbnailManager.getThumbnailPath(context, videoUri)
    }
    
    fun thumbnailExists(context: Context, videoUri: String): Boolean {
        return EnhancedVideoThumbnailManager.thumbnailExists(context, videoUri)
    }
}