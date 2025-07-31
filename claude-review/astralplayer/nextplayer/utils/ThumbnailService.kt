package com.astralplayer.nextplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ThumbnailService(private val context: Context) {
    
    private val thumbnailGenerator = ThumbnailGenerator(context)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _thumbnailCache = MutableStateFlow<Map<String, String>>(emptyMap())
    val thumbnailCache: StateFlow<Map<String, String>> = _thumbnailCache.asStateFlow()
    
    companion object {
        private const val TEMP_THUMBNAIL_DIR = "temp_thumbnails"
        
        @Volatile
        private var INSTANCE: ThumbnailService? = null
        
        fun getInstance(context: Context): ThumbnailService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThumbnailService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Generate thumbnail URL for seek preview at specific position
     */
    fun generateSeekPreviewThumbnail(
        videoUri: Uri,
        positionMs: Long,
        onResult: (String?) -> Unit
    ) {
        serviceScope.launch {
            try {
                val positionUs = positionMs * 1000 // Convert to microseconds
                val bitmap = thumbnailGenerator.getThumbnailAtPosition(videoUri, positionUs)
                
                if (bitmap != null) {
                    val thumbnailUrl = saveBitmapToTempFile(bitmap, positionMs)
                    updateCacheOnMainThread(positionMs.toString(), thumbnailUrl)
                    onResult(thumbnailUrl)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
    
    /**
     * Generate thumbnail URL for video item
     */
    fun generateVideoThumbnail(
        videoUri: Uri,
        onResult: (String?) -> Unit
    ) {
        serviceScope.launch {
            try {
                val bitmap = thumbnailGenerator.getThumbnail(videoUri)
                
                if (bitmap != null) {
                    val thumbnailUrl = saveBitmapToTempFile(bitmap, System.currentTimeMillis())
                    val key = videoUri.toString()
                    updateCacheOnMainThread(key, thumbnailUrl)
                    onResult(thumbnailUrl)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
    
    /**
     * Generate multiple thumbnails for timeline scrubbing
     */
    fun generateTimelineThumbnails(
        videoUri: Uri,
        videoDurationMs: Long,
        thumbnailCount: Int = 10,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onComplete: (List<TimelineThumbnail>) -> Unit
    ) {
        serviceScope.launch {
            try {
                val thumbnails = mutableListOf<TimelineThumbnail>()
                val interval = videoDurationMs / thumbnailCount
                
                for (i in 0 until thumbnailCount) {
                    val positionMs = i * interval
                    val positionUs = positionMs * 1000
                    
                    val bitmap = thumbnailGenerator.getThumbnailAtPosition(videoUri, positionUs)
                    if (bitmap != null) {
                        val thumbnailUrl = saveBitmapToTempFile(bitmap, positionMs)
                        thumbnails.add(
                            TimelineThumbnail(
                                positionMs = positionMs,
                                thumbnailUrl = thumbnailUrl
                            )
                        )
                    }
                    
                    onProgress(i + 1, thumbnailCount)
                }
                
                onComplete(thumbnails)
            } catch (e: Exception) {
                onComplete(emptyList())
            }
        }
    }
    
    /**
     * Clear all temporary thumbnails
     */
    fun clearTempThumbnails() {
        serviceScope.launch {
            try {
                val tempDir = File(context.cacheDir, TEMP_THUMBNAIL_DIR)
                if (tempDir.exists()) {
                    tempDir.listFiles()?.forEach { it.delete() }
                }
                _thumbnailCache.value = emptyMap()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    /**
     * Clear old thumbnails (older than specified time)
     */
    fun clearOldThumbnails(maxAgeMs: Long = 24 * 60 * 60 * 1000L) { // 24 hours default
        serviceScope.launch {
            try {
                thumbnailGenerator.clearOldThumbnails(maxAgeMs)
                
                val tempDir = File(context.cacheDir, TEMP_THUMBNAIL_DIR)
                if (tempDir.exists()) {
                    val cutoffTime = System.currentTimeMillis() - maxAgeMs
                    tempDir.listFiles()?.forEach { file ->
                        if (file.lastModified() < cutoffTime) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    private fun saveBitmapToTempFile(bitmap: Bitmap, identifier: Long): String {
        val tempDir = File(context.cacheDir, TEMP_THUMBNAIL_DIR)
        tempDir.mkdirs()
        
        val file = File(tempDir, "thumb_${identifier}_${System.currentTimeMillis()}.jpg")
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        
        return file.absolutePath
    }
    
    private fun updateCacheOnMainThread(key: String, url: String) {
        serviceScope.launch(Dispatchers.Main) {
            val currentCache = _thumbnailCache.value.toMutableMap()
            currentCache[key] = url
            _thumbnailCache.value = currentCache
        }
    }
}

data class TimelineThumbnail(
    val positionMs: Long,
    val thumbnailUrl: String
)