package com.astralplayer.nextplayer.feature.ai.realtime

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Predictive subtitle caching system
 */
class PredictiveSubtitleCache(private val context: Context) {
    
    companion object {
        private const val TAG = "PredictiveSubtitleCache"
    }
    
    private val memoryCache = ConcurrentHashMap<String, CachedSubtitles>()
    private val diskCache = SubtitleDiskCache(context)
    private var cacheHits = AtomicLong(0)
    private var cacheRequests = AtomicLong(0)
    
    suspend fun getInstant(videoUri: Uri, language: String): List<SubtitleEntry>? {
        cacheRequests.incrementAndGet()
        
        val cacheKey = generateCacheKey(videoUri, language)
        
        // Check memory cache first (instant)
        memoryCache[cacheKey]?.let { cached ->
            if (!cached.isExpired()) {
                cacheHits.incrementAndGet()
                Log.d(TAG, "Memory cache hit for $cacheKey")
                return cached.subtitles
            } else {
                memoryCache.remove(cacheKey)
            }
        }
        
        // Check disk cache (should be very fast)
        return try {
            diskCache.getInstant(cacheKey)?.also {
                cacheHits.incrementAndGet()
                // Store in memory for next time
                memoryCache[cacheKey] = CachedSubtitles(it, System.currentTimeMillis())
                Log.d(TAG, "Disk cache hit for $cacheKey")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Disk cache read failed", e)
            null
        }
    }
    
    suspend fun store(videoUri: Uri, language: String, subtitles: List<SubtitleEntry>) {
        val cacheKey = generateCacheKey(videoUri, language)
        val cached = CachedSubtitles(subtitles, System.currentTimeMillis())
        
        // Store in memory cache
        memoryCache[cacheKey] = cached
        
        // Store in disk cache asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                diskCache.store(cacheKey, subtitles)
            } catch (e: Exception) {
                Log.w(TAG, "Disk cache write failed", e)
            }
        }
    }
    
    fun hasCache(videoUri: Uri, language: String = "en"): Boolean {
        val cacheKey = generateCacheKey(videoUri, language)
        return memoryCache.containsKey(cacheKey) || diskCache.hasCache(cacheKey)
    }
    
    fun getCacheHitRate(): Double {
        val requests = cacheRequests.get()
        return if (requests > 0) {
            (cacheHits.get().toDouble() / requests) * 100
        } else 0.0
    }
    
    private fun generateCacheKey(videoUri: Uri, language: String): String {
        return "${videoUri.toString().hashCode()}_$language"
    }
    
    fun cleanup() {
        memoryCache.clear()
        diskCache.cleanup()
    }
}

// Disk cache implementation
class SubtitleDiskCache(private val context: Context) {
    
    private val cacheDir = context.cacheDir.resolve("subtitles")
    
    init {
        cacheDir.mkdirs()
    }
    
    suspend fun getInstant(cacheKey: String): List<SubtitleEntry>? {
        val file = cacheDir.resolve("$cacheKey.srt")
        
        return if (file.exists()) {
            try {
                // Read cached subtitles (simplified for demo)
                val lines = file.readLines()
                parseSubtitleFile(lines)
            } catch (e: Exception) {
                Log.e("SubtitleDiskCache", "Failed to read cache", e)
                null
            }
        } else {
            null
        }
    }
    
    suspend fun store(cacheKey: String, subtitles: List<SubtitleEntry>) {
        val file = cacheDir.resolve("$cacheKey.srt")
        
        try {
            file.writeText(subtitlesToSRT(subtitles))
        } catch (e: Exception) {
            Log.e("SubtitleDiskCache", "Failed to write cache", e)
        }
    }
    
    fun hasCache(cacheKey: String): Boolean {
        return cacheDir.resolve("$cacheKey.srt").exists()
    }
    
    fun cleanup() {
        // Clean up old cache files
        cacheDir.listFiles()?.forEach { file ->
            if (System.currentTimeMillis() - file.lastModified() > 7 * 24 * 60 * 60 * 1000) {
                file.delete()
            }
        }
    }
    
    private fun parseSubtitleFile(lines: List<String>): List<SubtitleEntry> {
        // Simplified SRT parsing
        val subtitles = mutableListOf<SubtitleEntry>()
        var i = 0
        
        while (i < lines.size) {
            // Skip empty lines and subtitle numbers
            while (i < lines.size && (lines[i].isBlank() || lines[i].toIntOrNull() != null)) {
                i++
            }
            
            if (i >= lines.size) break
            
            // Parse timestamp line
            val timeLine = lines[i]
            if (timeLine.contains("-->")) {
                val times = timeLine.split("-->")
                val startTime = parseSRTTime(times[0].trim())
                val endTime = parseSRTTime(times[1].trim())
                
                i++
                
                // Parse text lines
                val textLines = mutableListOf<String>()
                while (i < lines.size && lines[i].isNotBlank()) {
                    textLines.add(lines[i])
                    i++
                }
                
                subtitles.add(
                    SubtitleEntry(
                        startTime = startTime,
                        endTime = endTime,
                        text = textLines.joinToString(" "),
                        language = "en",
                        confidence = 1.0f
                    )
                )
            } else {
                i++
            }
        }
        
        return subtitles
    }
    
    private fun parseSRTTime(time: String): Long {
        // Parse SRT time format: 00:00:00,000
        val parts = time.split(":", ",")
        if (parts.size >= 4) {
            val hours = parts[0].toLongOrNull() ?: 0
            val minutes = parts[1].toLongOrNull() ?: 0
            val seconds = parts[2].toLongOrNull() ?: 0
            val millis = parts[3].toLongOrNull() ?: 0
            
            return hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
        }
        return 0
    }
    
    private fun subtitlesToSRT(subtitles: List<SubtitleEntry>): String {
        return subtitles.mapIndexed { index, subtitle ->
            val startTime = formatSRTTime(subtitle.startTime)
            val endTime = formatSRTTime(subtitle.endTime)
            
            "${index + 1}\n$startTime --> $endTime\n${subtitle.text}\n"
        }.joinToString("\n")
    }
    
    private fun formatSRTTime(timeMs: Long): String {
        val hours = timeMs / 3600000
        val minutes = (timeMs % 3600000) / 60000
        val seconds = (timeMs % 60000) / 1000
        val milliseconds = timeMs % 1000
        
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
    }
}

// Performance monitoring
class SubtitlePerformanceMonitor {
    
    private val successTimes = mutableListOf<Long>()
    private var fallbackCount = 0
    private var failureCount = 0
    
    fun recordSuccess(timeMs: Long) {
        synchronized(successTimes) {
            successTimes.add(timeMs)
            // Keep only last 100 records
            if (successTimes.size > 100) {
                successTimes.removeAt(0)
            }
        }
    }
    
    fun recordFallback() {
        fallbackCount++
    }
    
    fun recordFailure() {
        failureCount++
    }
    
    fun getAverageSuccessTime(): Double {
        return synchronized(successTimes) {
            if (successTimes.isEmpty()) 0.0 else successTimes.average()
        }
    }
    
    fun getFallbackRate(): Double {
        val total = successTimes.size + fallbackCount + failureCount
        return if (total > 0) (fallbackCount.toDouble() / total) * 100 else 0.0
    }
}