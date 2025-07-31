package com.astralplayer.nextplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Utility class for generating and caching video thumbnails
 */
class ThumbnailGenerator(private val context: Context) {
    
    companion object {
        private const val THUMBNAIL_WIDTH = 320
        private const val THUMBNAIL_HEIGHT = 180
        private const val THUMBNAIL_QUALITY = 80
        private const val CACHE_SIZE = 50 * 1024 * 1024 // 50MB
        private const val THUMBNAIL_DIR = "thumbnails"
    }
    
    // In-memory cache for thumbnails
    private val memoryCache = object : LruCache<String, Bitmap>(CACHE_SIZE) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }
    
    /**
     * Generate or retrieve a thumbnail for the given video URI
     */
    suspend fun getThumbnail(videoUri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val key = generateKey(videoUri.toString())
            
            // Check memory cache first
            memoryCache.get(key)?.let { return@withContext it }
            
            // Check disk cache
            val cachedFile = getCachedThumbnailFile(key)
            if (cachedFile.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(cachedFile.absolutePath)
                bitmap?.let {
                    memoryCache.put(key, it)
                    return@withContext it
                }
            }
            
            // Generate new thumbnail
            val bitmap = generateThumbnail(videoUri)
            bitmap?.let {
                // Save to caches
                memoryCache.put(key, it)
                saveThumbnailToDisk(it, cachedFile)
            }
            
            return@withContext bitmap
        } catch (e: Exception) {
            android.util.Log.e("ThumbnailGenerator", "Error generating thumbnail for $videoUri", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Generate thumbnail at a specific position (in microseconds)
     */
    suspend fun getThumbnailAtPosition(
        videoUri: Uri,
        positionUs: Long
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val key = "${generateKey(videoUri.toString())}_$positionUs"
            
            // Check memory cache first
            memoryCache.get(key)?.let { return@withContext it }
            
            // Generate new thumbnail
            val bitmap = generateThumbnailAtPosition(videoUri, positionUs)
            bitmap?.let {
                // Save to memory cache only (position-specific thumbnails are temporary)
                memoryCache.put(key, it)
            }
            
            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Clear all cached thumbnails
     */
    fun clearCache() {
        memoryCache.evictAll()
        
        // Clear disk cache
        val thumbnailDir = File(context.cacheDir, THUMBNAIL_DIR)
        if (thumbnailDir.exists()) {
            thumbnailDir.listFiles()?.forEach { it.delete() }
        }
    }
    
    /**
     * Clear thumbnails older than the specified time
     */
    fun clearOldThumbnails(maxAgeMillis: Long) {
        val thumbnailDir = File(context.cacheDir, THUMBNAIL_DIR)
        if (thumbnailDir.exists()) {
            val cutoffTime = System.currentTimeMillis() - maxAgeMillis
            thumbnailDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        }
    }
    
    private fun generateThumbnail(videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            when {
                videoUri.scheme == "content" || videoUri.scheme == "file" -> {
                    retriever.setDataSource(context, videoUri)
                }
                videoUri.scheme == "http" || videoUri.scheme == "https" -> {
                    retriever.setDataSource(videoUri.toString(), HashMap())
                }
                else -> {
                    retriever.setDataSource(videoUri.toString())
                }
            }
            
            // Get frame at 10% of video duration for better representation
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            val frameTime = if (duration > 0) (duration * 1000 * 0.1).toLong() else 0L
            
            val bitmap = retriever.getFrameAtTime(frameTime)
            if (bitmap != null) {
                android.util.Log.d("ThumbnailGenerator", "Successfully generated thumbnail for $videoUri at time $frameTime")
                scaleBitmap(bitmap)
            } else {
                android.util.Log.w("ThumbnailGenerator", "Failed to get frame for $videoUri at time $frameTime")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ThumbnailGenerator", "Exception generating thumbnail for $videoUri", e)
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    private fun generateThumbnailAtPosition(videoUri: Uri, positionUs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            when {
                videoUri.scheme == "content" || videoUri.scheme == "file" -> {
                    retriever.setDataSource(context, videoUri)
                }
                videoUri.scheme == "http" || videoUri.scheme == "https" -> {
                    retriever.setDataSource(videoUri.toString(), HashMap())
                }
                else -> {
                    retriever.setDataSource(videoUri.toString())
                }
            }
            
            val bitmap = retriever.getFrameAtTime(positionUs)
            bitmap?.let { scaleBitmap(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        
        if (aspectRatio > THUMBNAIL_WIDTH.toFloat() / THUMBNAIL_HEIGHT.toFloat()) {
            targetWidth = THUMBNAIL_WIDTH
            targetHeight = (THUMBNAIL_WIDTH / aspectRatio).toInt()
        } else {
            targetHeight = THUMBNAIL_HEIGHT
            targetWidth = (THUMBNAIL_HEIGHT * aspectRatio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
    
    private fun saveThumbnailToDisk(bitmap: Bitmap, file: File) {
        try {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getCachedThumbnailFile(key: String): File {
        val thumbnailDir = File(context.cacheDir, THUMBNAIL_DIR)
        thumbnailDir.mkdirs()
        return File(thumbnailDir, "$key.jpg")
    }
    
    private fun generateKey(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}