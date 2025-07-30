package com.astralplayer.nextplayer.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.astralplayer.nextplayer.utils.ThumbnailGenerator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Background service for batch thumbnail generation and management
 */
class ThumbnailService(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    
    private val thumbnailGenerator = ThumbnailGenerator(context)
    private val thumbnailCache = ConcurrentHashMap<String, Bitmap>()
    private val generationQueue = mutableSetOf<String>()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _generationProgress = MutableStateFlow(0f)
    val generationProgress: StateFlow<Float> = _generationProgress.asStateFlow()
    
    /**
     * Preload thumbnails for a list of videos in the background
     */
    fun preloadThumbnails(videoUris: List<Uri>) {
        scope.launch {
            if (_isGenerating.value) return@launch
            
            _isGenerating.value = true
            _generationProgress.value = 0f
            
            try {
                val toGenerate = videoUris.filter { uri ->
                    val key = generateKey(uri.toString())
                    !thumbnailCache.containsKey(key) && !generationQueue.contains(key)
                }
                
                if (toGenerate.isEmpty()) {
                    _isGenerating.value = false
                    return@launch
                }
                
                toGenerate.forEachIndexed { index, uri ->
                    val key = generateKey(uri.toString())
                    generationQueue.add(key)
                    
                    try {
                        val bitmap = thumbnailGenerator.getThumbnail(uri)
                        bitmap?.let { thumbnailCache[key] = it }
                    } catch (e: Exception) {
                        // Continue with next thumbnail
                    } finally {
                        generationQueue.remove(key)
                        _generationProgress.value = (index + 1).toFloat() / toGenerate.size
                    }
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    /**
     * Get cached thumbnail or generate if not available
     */
    suspend fun getThumbnail(videoUri: Uri): Bitmap? {
        val key = generateKey(videoUri.toString())
        
        // Return from cache if available
        thumbnailCache[key]?.let { return it }
        
        // Generate and cache
        return try {
            val bitmap = thumbnailGenerator.getThumbnail(videoUri)
            bitmap?.let { thumbnailCache[key] = it }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get thumbnail at specific position for seek preview
     */
    suspend fun getThumbnailAtPosition(videoUri: Uri, positionUs: Long): Bitmap? {
        return try {
            thumbnailGenerator.getThumbnailAtPosition(videoUri, positionUs)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate multiple thumbnails for a video (e.g., for seek preview)
     */
    suspend fun generateSeekThumbnails(
        videoUri: Uri,
        count: Int = 10
    ): List<Pair<Long, Bitmap?>> = withContext(Dispatchers.IO) {
        try {
            // Get video duration first
            val retriever = android.media.MediaMetadataRetriever()
            val duration = try {
                retriever.setDataSource(context, videoUri)
                val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationStr?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                0L
            } finally {
                try { retriever.release() } catch (e: Exception) { /* ignore */ }
            }
            
            if (duration <= 0) return@withContext emptyList()
            
            // Generate thumbnails at even intervals
            val results = mutableListOf<Pair<Long, Bitmap?>>()
            for (i in 0 until count) {
                val position = (duration * i / count.toDouble()).toLong()
                val positionUs = position * 1000
                val bitmap = thumbnailGenerator.getThumbnailAtPosition(videoUri, positionUs)
                results.add(position to bitmap)
            }
            
            results
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clear thumbnail cache
     */
    fun clearCache() {
        thumbnailCache.clear()
        thumbnailGenerator.clearCache()
    }
    
    /**
     * Clear old thumbnails
     */
    fun clearOldThumbnails(maxAgeMillis: Long = 7 * 24 * 60 * 60 * 1000L) { // 7 days
        thumbnailGenerator.clearOldThumbnails(maxAgeMillis)
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): ThumbnailCacheStats {
        return ThumbnailCacheStats(
            cacheSize = thumbnailCache.size,
            isGenerating = _isGenerating.value,
            progress = _generationProgress.value
        )
    }
    
    private fun generateKey(input: String): String {
        return input.hashCode().toString()
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        thumbnailCache.clear()
    }
}

data class ThumbnailCacheStats(
    val cacheSize: Int,
    val isGenerating: Boolean,
    val progress: Float
)