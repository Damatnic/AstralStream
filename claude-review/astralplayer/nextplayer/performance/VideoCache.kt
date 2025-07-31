package com.astralplayer.nextplayer.performance

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val maxMemorySize = (Runtime.getRuntime().maxMemory() / 8).toInt() // Use 1/8 of available memory
    private val maxDiskSize = 100 * 1024 * 1024L // 100MB disk cache
    
    private val memoryCache = LruCache<String, ByteArray>(maxMemorySize)
    private val cacheDir = File(context.cacheDir, "video_cache")
    
    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    data class CacheEntry(
        val key: String,
        val data: ByteArray,
        val timestamp: Long,
        val size: Long
    )
    
    suspend fun cacheVideoSegment(uri: Uri, segmentData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = generateCacheKey(uri.toString())
            
            // Store in memory cache
            memoryCache.put(key, segmentData)
            
            // Store in disk cache
            val cacheFile = File(cacheDir, key)
            FileOutputStream(cacheFile).use { output ->
                output.write(segmentData)
            }
            
            // Clean up old cache if needed
            cleanupCache()
            
            Log.d("VideoCache", "Cached video segment for $uri, size: ${segmentData.size}")
            true
        } catch (e: Exception) {
            Log.e("VideoCache", "Failed to cache video segment", e)
            false
        }
    }
    
    suspend fun getCachedVideoSegment(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val key = generateCacheKey(uri.toString())
            
            // Check memory cache first
            memoryCache.get(key)?.let { return@withContext it }
            
            // Check disk cache
            val cacheFile = File(cacheDir, key)
            if (cacheFile.exists()) {
                val data = FileInputStream(cacheFile).use { input ->
                    input.readBytes()
                }
                
                // Put back in memory cache
                memoryCache.put(key, data)
                
                Log.d("VideoCache", "Retrieved cached video segment for $uri")
                return@withContext data
            }
            
            null
        } catch (e: Exception) {
            Log.e("VideoCache", "Failed to retrieve cached video segment", e)
            null
        }
    }
    
    fun isCached(uri: Uri): Boolean {
        val key = generateCacheKey(uri.toString())
        return memoryCache.get(key) != null || File(cacheDir, key).exists()
    }
    
    suspend fun preloadVideo(uri: Uri, priority: CachePriority = CachePriority.NORMAL) = withContext(Dispatchers.IO) {
        try {
            if (isCached(uri)) {
                Log.d("VideoCache", "Video already cached: $uri")
                return@withContext
            }
            
            // Simulate preloading video segments
            // In a real implementation, this would load the actual video data
            val simulatedData = ByteArray(1024) { it.toByte() }
            cacheVideoSegment(uri, simulatedData)
            
            Log.d("VideoCache", "Preloaded video: $uri with priority: $priority")
        } catch (e: Exception) {
            Log.e("VideoCache", "Failed to preload video", e)
        }
    }
    
    private suspend fun cleanupCache() = withContext(Dispatchers.IO) {
        try {
            val cacheFiles = cacheDir.listFiles() ?: return@withContext
            val totalSize = cacheFiles.sumOf { it.length() }
            
            if (totalSize > maxDiskSize) {
                // Sort by last modified time and delete oldest files
                val sortedFiles = cacheFiles.sortedBy { it.lastModified() }
                var deletedSize = 0L
                
                for (file in sortedFiles) {
                    if (totalSize - deletedSize <= maxDiskSize) break
                    
                    deletedSize += file.length()
                    file.delete()
                    Log.d("VideoCache", "Deleted old cache file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("VideoCache", "Failed to cleanup cache", e)
        }
    }
    
    fun clearCache() {
        memoryCache.evictAll()
        cacheDir.listFiles()?.forEach { it.delete() }
        Log.d("VideoCache", "Cache cleared")
    }
    
    fun getCacheStats(): CacheStats {
        val diskFiles = cacheDir.listFiles() ?: emptyArray()
        val diskSize = diskFiles.sumOf { it.length() }
        val memorySize = memoryCache.size()
        
        return CacheStats(
            memoryCacheSize = memorySize,
            diskCacheSize = diskSize,
            totalCachedItems = diskFiles.size + memoryCache.size(),
            hitRate = 0.0f // Would need to track hits/misses for real implementation
        )
    }
    
    private fun generateCacheKey(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    enum class CachePriority {
        LOW, NORMAL, HIGH
    }
    
    data class CacheStats(
        val memoryCacheSize: Int,
        val diskCacheSize: Long,
        val totalCachedItems: Int,
        val hitRate: Float
    )
}