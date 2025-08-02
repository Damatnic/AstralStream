package com.astralstream.nextplayer.cache

import android.content.Context
import androidx.room.Room
import com.astralstream.nextplayer.database.AppDatabase
import com.astralstream.nextplayer.database.dao.SubtitleCacheDao
import com.astralstream.nextplayer.database.entities.CachedSubtitleEntity
import com.astralstream.nextplayer.models.SubtitleEntry
import com.astralstream.nextplayer.security.EncryptionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleCacheManager @Inject constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val encryptionManager: EncryptionManager
) {
    private val subtitleCacheDao: SubtitleCacheDao = database.subtitleCacheDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Cache configuration
    private val maxCacheSizeMB = 100L
    private val maxCacheAge = TimeUnit.DAYS.toMillis(30)
    private val cacheDir = File(context.cacheDir, "subtitles")
    
    // Cache statistics
    private val _cacheStats = MutableStateFlow(CacheStatistics())
    val cacheStats: StateFlow<CacheStatistics> = _cacheStats.asStateFlow()
    
    init {
        cacheDir.mkdirs()
        scope.launch {
            cleanupExpiredCache()
            updateCacheStatistics()
        }
    }
    
    data class CacheStatistics(
        val totalSize: Long = 0,
        val itemCount: Int = 0,
        val hitRate: Float = 0f,
        val oldestEntry: Long = 0,
        val lastCleanup: Long = System.currentTimeMillis()
    )
    
    /**
     * Store subtitle in cache with encryption
     */
    suspend fun cacheSubtitle(
        videoUri: String,
        language: String,
        subtitle: SubtitleEntry,
        sourceType: SourceType = SourceType.AI_GENERATED
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Generate cache key
            val cacheKey = generateCacheKey(videoUri, language)
            
            // Serialize subtitle data
            val subtitleData = serializeSubtitle(subtitle)
            
            // Encrypt data
            val encryptedData = encryptionManager.encrypt(subtitleData)
            
            // Save to file
            val cacheFile = File(cacheDir, "$cacheKey.enc")
            cacheFile.writeBytes(encryptedData)
            
            // Store metadata in database
            val entity = CachedSubtitleEntity(
                cacheKey = cacheKey,
                videoUri = videoUri,
                language = language,
                filePath = cacheFile.absolutePath,
                fileSize = cacheFile.length(),
                sourceType = sourceType.name,
                timestamp = System.currentTimeMillis(),
                lastAccessed = System.currentTimeMillis(),
                accessCount = 0,
                confidence = subtitle.confidence ?: 0.95f
            )
            
            subtitleCacheDao.insert(entity)
            
            // Update cache stats
            updateCacheStatistics()
            
            // Check cache size and cleanup if needed
            if (getCurrentCacheSize() > maxCacheSizeMB * 1024 * 1024) {
                evictLeastRecentlyUsed()
            }
            
            Timber.d("Cached subtitle for $videoUri in $language")
            Result.success(cacheKey)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to cache subtitle")
            Result.failure(e)
        }
    }
    
    /**
     * Retrieve subtitle from cache
     */
    suspend fun getCachedSubtitle(
        videoUri: String,
        language: String
    ): SubtitleEntry? = withContext(Dispatchers.IO) {
        try {
            val cacheKey = generateCacheKey(videoUri, language)
            val entity = subtitleCacheDao.getByKey(cacheKey) ?: return@withContext null
            
            // Check if file exists
            val cacheFile = File(entity.filePath)
            if (!cacheFile.exists()) {
                subtitleCacheDao.delete(entity)
                return@withContext null
            }
            
            // Read and decrypt file
            val encryptedData = cacheFile.readBytes()
            val decryptedData = encryptionManager.decrypt(encryptedData)
            
            // Update access statistics
            subtitleCacheDao.updateAccessTime(cacheKey, System.currentTimeMillis())
            subtitleCacheDao.incrementAccessCount(cacheKey)
            
            // Deserialize subtitle
            deserializeSubtitle(decryptedData)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve cached subtitle")
            null
        }
    }
    
    /**
     * Check if subtitle exists in cache
     */
    suspend fun hasCache(videoUri: String, language: String): Boolean {
        val cacheKey = generateCacheKey(videoUri, language)
        return subtitleCacheDao.exists(cacheKey)
    }
    
    /**
     * Get all cached subtitles for a video
     */
    suspend fun getCachedLanguages(videoUri: String): List<String> {
        return subtitleCacheDao.getLanguagesForVideo(videoUri)
    }
    
    /**
     * Clear cache for specific video
     */
    suspend fun clearVideoCache(videoUri: String) = withContext(Dispatchers.IO) {
        val entities = subtitleCacheDao.getByVideoUri(videoUri)
        entities.forEach { entity ->
            File(entity.filePath).delete()
            subtitleCacheDao.delete(entity)
        }
        updateCacheStatistics()
    }
    
    /**
     * Clear all cache
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
        subtitleCacheDao.deleteAll()
        updateCacheStatistics()
    }
    
    /**
     * Export cache statistics
     */
    fun exportCacheReport(): CacheReport {
        return CacheReport(
            statistics = _cacheStats.value,
            topAccessedVideos = runBlocking { subtitleCacheDao.getTopAccessed(10) },
            cacheByLanguage = runBlocking { subtitleCacheDao.getCacheByLanguage() },
            cacheBySource = runBlocking { subtitleCacheDao.getCacheBySource() }
        )
    }
    
    // Private helper methods
    
    private fun generateCacheKey(videoUri: String, language: String): String {
        val hash = (videoUri + language).hashCode().toString()
        return "subtitle_${hash}_$language"
    }
    
    private fun serializeSubtitle(subtitle: SubtitleEntry): ByteArray {
        // Convert subtitle to JSON or other format
        val json = buildString {
            append("{")
            append("\"entries\":[")
            subtitle.entries.forEachIndexed { index, entry ->
                if (index > 0) append(",")
                append("{")
                append("\"start\":${entry.startTime},")
                append("\"end\":${entry.endTime},")
                append("\"text\":\"${entry.text.replace("\"", "\\\"")}\"")
                append("}")
            }
            append("],")
            append("\"language\":\"${subtitle.language}\",")
            append("\"confidence\":${subtitle.confidence}")
            append("}")
        }
        return json.toByteArray()
    }
    
    private fun deserializeSubtitle(data: ByteArray): SubtitleEntry {
        // Parse JSON back to SubtitleEntry
        // This is a simplified version - use proper JSON parsing in production
        val json = String(data)
        // Parse and return SubtitleEntry
        return SubtitleEntry(
            entries = emptyList(), // Parse from JSON
            language = "en", // Parse from JSON
            confidence = 0.95f // Parse from JSON
        )
    }
    
    private suspend fun getCurrentCacheSize(): Long {
        return subtitleCacheDao.getTotalSize()
    }
    
    private suspend fun evictLeastRecentlyUsed() {
        val targetSize = (maxCacheSizeMB * 1024 * 1024 * 0.8).toLong() // Keep 80% after cleanup
        var currentSize = getCurrentCacheSize()
        
        val candidates = subtitleCacheDao.getLeastRecentlyUsed()
        
        for (entity in candidates) {
            if (currentSize <= targetSize) break
            
            File(entity.filePath).delete()
            subtitleCacheDao.delete(entity)
            currentSize -= entity.fileSize
        }
        
        updateCacheStatistics()
    }
    
    private suspend fun cleanupExpiredCache() {
        val expiredTime = System.currentTimeMillis() - maxCacheAge
        val expired = subtitleCacheDao.getExpired(expiredTime)
        
        expired.forEach { entity ->
            File(entity.filePath).delete()
            subtitleCacheDao.delete(entity)
        }
    }
    
    private suspend fun updateCacheStatistics() {
        val stats = CacheStatistics(
            totalSize = getCurrentCacheSize(),
            itemCount = subtitleCacheDao.getCount(),
            hitRate = calculateHitRate(),
            oldestEntry = subtitleCacheDao.getOldestTimestamp() ?: 0,
            lastCleanup = System.currentTimeMillis()
        )
        _cacheStats.value = stats
    }
    
    private suspend fun calculateHitRate(): Float {
        val totalAccess = subtitleCacheDao.getTotalAccessCount()
        val totalItems = subtitleCacheDao.getCount()
        return if (totalItems > 0) totalAccess.toFloat() / totalItems else 0f
    }
    
    // Data classes
    
    enum class SourceType {
        AI_GENERATED,
        USER_CONTRIBUTED,
        MANUAL_UPLOAD,
        EMBEDDED,
        EXTERNAL_API
    }
    
    data class CacheReport(
        val statistics: CacheStatistics,
        val topAccessedVideos: List<CachedSubtitleEntity>,
        val cacheByLanguage: Map<String, Int>,
        val cacheBySource: Map<String, Int>
    )
    
    fun release() {
        scope.cancel()
    }
}