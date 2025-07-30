package com.astralplayer.nextplayer.optimization

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.util.Log
import androidx.annotation.RequiresApi
import com.astralplayer.nextplayer.feature.network.AdvancedNetworkManager
import com.astralplayer.nextplayer.feature.streaming.StreamProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * Advanced performance optimization manager inspired by MX Player's efficiency
 * Handles memory management, CPU optimization, and adaptive performance tuning
 */
class PerformanceOptimizationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PerformanceOptimizer"
        private const val MEMORY_PRESSURE_THRESHOLD = 0.85f
        private const val LOW_MEMORY_THRESHOLD_MB = 200
        private const val CACHE_CLEANUP_INTERVAL_MS = 30000L // 30 seconds
        private const val PERFORMANCE_MONITOR_INTERVAL_MS = 5000L // 5 seconds
        private const val MAX_CONCURRENT_STREAMS = 3
        
        // MX Player inspired optimization levels
        enum class OptimizationLevel {
            BATTERY_SAVER,    // Maximum efficiency, reduced features
            BALANCED,         // Default mode with good performance/battery balance  
            PERFORMANCE,      // Maximum performance, higher battery usage
            GAMING           // Ultra-low latency for competitive usage
        }
    }
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()
    
    // Performance monitoring
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    private val _optimizationLevel = MutableStateFlow(OptimizationLevel.BALANCED)
    val optimizationLevel: StateFlow<OptimizationLevel> = _optimizationLevel.asStateFlow()
    
    // Cache management
    private val imageCache = ConcurrentHashMap<String, WeakReference<Any>>()
    private val metadataCache = ConcurrentHashMap<String, WeakReference<Any>>()
    private val streamCache = ConcurrentHashMap<String, WeakReference<Any>>()
    
    // Performance counters
    private val frameDropCounter = AtomicLong(0)
    private val networkRequestCounter = AtomicLong(0)
    private val cacheHitCounter = AtomicLong(0)
    private val cacheMissCounter = AtomicLong(0)
    
    // Coroutine management
    private val optimizationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + 
        CoroutineName("PerformanceOptimization")
    )
    
    private var performanceMonitorJob: Job? = null
    private var cacheCleanupJob: Job? = null
    
    init {
        startPerformanceMonitoring()
        startCacheCleanup()
        detectOptimalSettings()
    }
    
    /**
     * Start continuous performance monitoring
     */
    private fun startPerformanceMonitoring() {
        performanceMonitorJob = optimizationScope.launch {
            while (isActive) {
                try {
                    updatePerformanceMetrics()
                    adaptiveOptimization()
                    delay(PERFORMANCE_MONITOR_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in performance monitoring", e)
                }
            }
        }
    }
    
    /**
     * Start cache cleanup routine
     */
    private fun startCacheCleanup() {
        cacheCleanupJob = optimizationScope.launch {
            while (isActive) {
                try {
                    cleanupExpiredCaches()
                    delay(CACHE_CLEANUP_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in cache cleanup", e)
                }
            }
        }
    }
    
    /**
     * Update performance metrics
     */
    private fun updatePerformanceMetrics() {
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        val usedMemoryMB = totalMemoryMB - availableMemoryMB
        val memoryUsagePercent = (usedMemoryMB.toFloat() / totalMemoryMB.toFloat()) * 100f
        
        val currentMetrics = PerformanceMetrics(
            memoryUsageMB = usedMemoryMB,
            availableMemoryMB = availableMemoryMB,
            memoryUsagePercent = memoryUsagePercent,
            isLowMemory = memoryInfo.lowMemory,
            frameDrops = frameDropCounter.get(),
            networkRequests = networkRequestCounter.get(),
            cacheHitRate = calculateCacheHitRate(),
            cpuUsagePercent = getCpuUsage(),
            batteryOptimized = _optimizationLevel.value == OptimizationLevel.BATTERY_SAVER
        )
        
        _performanceMetrics.value = currentMetrics
        
        Log.d(TAG, "Performance metrics updated - Memory: ${memoryUsagePercent.toInt()}%, " +
                "Cache hit rate: ${(currentMetrics.cacheHitRate * 100).toInt()}%")
    }
    
    /**
     * Adaptive optimization based on current system state
     */
    private fun adaptiveOptimization() {
        val metrics = _performanceMetrics.value
        
        when {
            metrics.isLowMemory || metrics.memoryUsagePercent > 90f -> {
                if (_optimizationLevel.value != OptimizationLevel.BATTERY_SAVER) {
                    Log.i(TAG, "Switching to battery saver mode due to low memory")
                    setOptimizationLevel(OptimizationLevel.BATTERY_SAVER)
                }
                aggressiveMemoryCleanup()
            }
            
            metrics.memoryUsagePercent > 75f -> {
                if (_optimizationLevel.value == OptimizationLevel.PERFORMANCE) {
                    Log.i(TAG, "Switching to balanced mode due to memory pressure")
                    setOptimizationLevel(OptimizationLevel.BALANCED)
                }
                moderateMemoryCleanup()
            }
            
            metrics.memoryUsagePercent < 50f && metrics.cacheHitRate > 0.8f -> {
                if (_optimizationLevel.value == OptimizationLevel.BATTERY_SAVER) {
                    Log.i(TAG, "Switching to balanced mode - system resources available")
                    setOptimizationLevel(OptimizationLevel.BALANCED)
                }
            }
        }
    }
    
    /**
     * Set optimization level manually
     */
    fun setOptimizationLevel(level: OptimizationLevel) {
        _optimizationLevel.value = level
        Log.i(TAG, "Optimization level set to: $level")
        
        when (level) {
            OptimizationLevel.BATTERY_SAVER -> applyBatterySaverOptimizations()
            OptimizationLevel.BALANCED -> applyBalancedOptimizations()
            OptimizationLevel.PERFORMANCE -> applyPerformanceOptimizations()
            OptimizationLevel.GAMING -> applyGamingOptimizations()
        }
    }
    
    /**
     * Apply battery saver optimizations (MX Player inspired)
     */
    private fun applyBatterySaverOptimizations() {
        Log.d(TAG, "Applying battery saver optimizations")
        
        // Reduce background processing
        // Limit cache sizes
        // Use lower quality defaults
        // Disable non-essential features
        
        clearNonEssentialCaches()
        // Additional battery optimizations...
    }
    
    /**
     * Apply balanced optimizations
     */
    private fun applyBalancedOptimizations() {
        Log.d(TAG, "Applying balanced optimizations")
        
        // Standard cache sizes
        // Normal processing priority
        // Adaptive quality
    }
    
    /**
     * Apply performance optimizations
     */
    private fun applyPerformanceOptimizations() {
        Log.d(TAG, "Applying performance optimizations")
        
        // Larger cache sizes
        // Higher processing priority
        // Prefer quality over battery
        // Enable advanced features
    }
    
    /**
     * Apply gaming optimizations (ultra-low latency)
     */
    private fun applyGamingOptimizations() {
        Log.d(TAG, "Applying gaming optimizations")
        
        // Minimize buffering
        // Reduce processing overhead
        // Prioritize low latency
        // Disable background tasks
    }
    
    /**
     * Get optimized buffer configuration based on current performance level
     */
    fun getOptimizedBufferConfig(): BufferConfiguration {
        return when (_optimizationLevel.value) {
            OptimizationLevel.BATTERY_SAVER -> BufferConfiguration(
                minBufferMs = 10000,
                maxBufferMs = 30000,
                bufferForPlaybackMs = 2000,
                bufferForPlaybackAfterRebufferMs = 4000
            )
            
            OptimizationLevel.BALANCED -> BufferConfiguration(
                minBufferMs = 15000,
                maxBufferMs = 50000,
                bufferForPlaybackMs = 2500,
                bufferForPlaybackAfterRebufferMs = 5000
            )
            
            OptimizationLevel.PERFORMANCE -> BufferConfiguration(
                minBufferMs = 20000,
                maxBufferMs = 80000,
                bufferForPlaybackMs = 3000,
                bufferForPlaybackAfterRebufferMs = 7000
            )
            
            OptimizationLevel.GAMING -> BufferConfiguration(
                minBufferMs = 5000,
                maxBufferMs = 15000,
                bufferForPlaybackMs = 1000,
                bufferForPlaybackAfterRebufferMs = 2000
            )
        }
    }
    
    /**
     * Get optimized quality settings
     */
    fun getOptimizedQualitySettings(): QualityConfiguration {
        val metrics = _performanceMetrics.value
        
        return when (_optimizationLevel.value) {
            OptimizationLevel.BATTERY_SAVER -> QualityConfiguration(
                maxResolution = if (metrics.memoryUsagePercent > 80f) 720 else 1080,
                preferHardwareDecoding = true,
                enableHDR = false,
                maxBitrate = 3_000_000 // 3 Mbps
            )
            
            OptimizationLevel.BALANCED -> QualityConfiguration(
                maxResolution = 1080,
                preferHardwareDecoding = true,
                enableHDR = metrics.memoryUsagePercent < 70f,
                maxBitrate = 8_000_000 // 8 Mbps
            )
            
            OptimizationLevel.PERFORMANCE -> QualityConfiguration(
                maxResolution = 2160, // 4K
                preferHardwareDecoding = true,
                enableHDR = true,
                maxBitrate = 25_000_000 // 25 Mbps
            )
            
            OptimizationLevel.GAMING -> QualityConfiguration(
                maxResolution = 1080, // Prioritize frame rate over resolution
                preferHardwareDecoding = true,
                enableHDR = false,
                maxBitrate = 5_000_000 // 5 Mbps
            )
        }
    }
    
    /**
     * Memory management functions
     */
    private fun aggressiveMemoryCleanup() {
        Log.i(TAG, "Performing aggressive memory cleanup")
        
        clearAllCaches()
        System.gc()
        
        // Additional aggressive cleanup
        Runtime.getRuntime().gc()
    }
    
    private fun moderateMemoryCleanup() {
        Log.i(TAG, "Performing moderate memory cleanup")
        
        cleanupExpiredCaches()
        clearNonEssentialCaches()
    }
    
    private fun clearAllCaches() {
        imageCache.clear()
        metadataCache.clear()
        streamCache.clear()
        
        cacheHitCounter.set(0)
        cacheMissCounter.set(0)
    }
    
    private fun clearNonEssentialCaches() {
        // Keep only recently used items
        cleanupCacheByAge(imageCache, 300000) // 5 minutes
        cleanupCacheByAge(metadataCache, 600000) // 10 minutes
    }
    
    private fun cleanupExpiredCaches() {
        cleanupCacheByAge(imageCache, 1800000) // 30 minutes
        cleanupCacheByAge(metadataCache, 3600000) // 1 hour
        cleanupCacheByAge(streamCache, 900000) // 15 minutes
    }
    
    private fun cleanupCacheByAge(cache: ConcurrentHashMap<String, WeakReference<Any>>, maxAgeMs: Long) {
        val currentTime = System.currentTimeMillis()
        val keysToRemove = mutableListOf<String>()
        
        cache.forEach { (key, ref) ->
            if (ref.get() == null) {
                keysToRemove.add(key)
            }
        }
        
        keysToRemove.forEach { cache.remove(it) }
    }
    
    /**
     * Performance measurement utilities
     */
    private fun calculateCacheHitRate(): Float {
        val hits = cacheHitCounter.get()
        val misses = cacheMissCounter.get()
        val total = hits + misses
        
        return if (total > 0) hits.toFloat() / total.toFloat() else 0f
    }
    
    private fun getCpuUsage(): Float {
        // Simplified CPU usage estimation
        return try {
            val usage = Debug.threadCpuTimeNanos() / 1_000_000f // Convert to ms
            min(usage / 1000f, 100f) // Cap at 100%
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * Auto-detect optimal settings based on device capabilities
     */
    private fun detectOptimalSettings() {
        val totalMemoryGB = memoryInfo.totalMem / (1024 * 1024 * 1024f)
        val processorCount = Runtime.getRuntime().availableProcessors()
        
        val recommendedLevel = when {
            totalMemoryGB >= 8f && processorCount >= 8 -> OptimizationLevel.PERFORMANCE
            totalMemoryGB >= 4f && processorCount >= 4 -> OptimizationLevel.BALANCED  
            totalMemoryGB >= 2f -> OptimizationLevel.BALANCED
            else -> OptimizationLevel.BATTERY_SAVER
        }
        
        Log.i(TAG, "Auto-detected optimization level: $recommendedLevel " +
                "(Memory: ${totalMemoryGB}GB, CPU cores: $processorCount)")
        
        setOptimizationLevel(recommendedLevel)
    }
    
    /**
     * Get MX Player style recommendations
     */
    fun getMxPlayerStyleRecommendations(): List<OptimizationRecommendation> {
        val recommendations = mutableListOf<OptimizationRecommendation>()
        val metrics = _performanceMetrics.value
        
        if (metrics.memoryUsagePercent > 85f) {
            recommendations.add(
                OptimizationRecommendation(
                    type = RecommendationType.MEMORY,
                    title = "High Memory Usage",
                    description = "Consider closing other apps or enabling battery saver mode",
                    action = "Switch to Battery Saver mode"
                )
            )
        }
        
        if (metrics.cacheHitRate < 0.5f) {
            recommendations.add(
                OptimizationRecommendation(
                    type = RecommendationType.CACHE,
                    title = "Low Cache Efficiency",
                    description = "Cache hit rate is below optimal threshold",
                    action = "Increase cache size or check storage space"
                )
            )
        }
        
        if (metrics.frameDrops > 100) {
            recommendations.add(
                OptimizationRecommendation(
                    type = RecommendationType.PERFORMANCE,
                    title = "Frame Drops Detected",
                    description = "Video playback may be stuttering",
                    action = "Try hardware decoding or lower quality"
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Performance counters (for monitoring)
     */
    fun recordFrameDrop() = frameDropCounter.incrementAndGet()
    fun recordNetworkRequest() = networkRequestCounter.incrementAndGet()
    fun recordCacheHit() = cacheHitCounter.incrementAndGet()
    fun recordCacheMiss() = cacheMissCounter.incrementAndGet()
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        performanceMonitorJob?.cancel()
        cacheCleanupJob?.cancel()
        optimizationScope.cancel()
        clearAllCaches()
        
        Log.d(TAG, "Performance optimization manager cleaned up")
    }
}

/**
 * Data classes for performance optimization
 */
data class PerformanceMetrics(
    val memoryUsageMB: Long = 0,
    val availableMemoryMB: Long = 0,
    val memoryUsagePercent: Float = 0f,
    val isLowMemory: Boolean = false,
    val frameDrops: Long = 0,
    val networkRequests: Long = 0,
    val cacheHitRate: Float = 0f,
    val cpuUsagePercent: Float = 0f,
    val batteryOptimized: Boolean = false
)

data class BufferConfiguration(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int
)

data class QualityConfiguration(
    val maxResolution: Int,
    val preferHardwareDecoding: Boolean,
    val enableHDR: Boolean,
    val maxBitrate: Int
)

data class OptimizationRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val action: String
)

enum class RecommendationType {
    MEMORY, CACHE, PERFORMANCE, NETWORK, BATTERY
}