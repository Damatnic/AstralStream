package com.astralvu.performance

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

class PerformanceOptimizationManager(private val context: Context) {
    
    private val performanceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val memoryManager = MemoryManager()
    private val performanceMetrics = ConcurrentHashMap<String, PerformanceMetric>()
    private val activeMonitors = mutableListOf<PerformanceMonitor>()
    
    data class PerformanceMetric(
        val name: String,
        var totalTime: Long = 0,
        var count: Int = 0,
        var minTime: Long = Long.MAX_VALUE,
        var maxTime: Long = 0,
        var lastTime: Long = 0,
        val timestamp: Long = SystemClock.elapsedRealtime()
    ) {
        val averageTime: Long get() = if (count > 0) totalTime / count else 0
    }
    
    interface PerformanceMonitor {
        fun onPerformanceUpdate(metrics: Map<String, PerformanceMetric>)
    }
    
    inner class MemoryManager {
        private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        private val memoryInfo = ActivityManager.MemoryInfo()
        private var lastMemoryWarning = 0L
        private val memoryWarningThreshold = 5000L // 5 seconds between warnings
        
        fun trimMemory(level: Int) {
            when (level) {
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                    // App is running but memory is getting low
                    performLightMemoryCleanup()
                }
                
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                    // App is still running but system is low on memory
                    performModerateMemoryCleanup()
                }
                
                ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                    // UI is hidden, good time to release UI resources
                    performUIMemoryCleanup()
                }
                
                ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
                ComponentCallbacks2.TRIM_MEMORY_MODERATE,
                ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                    // App is in background, release as much as possible
                    performAggressiveMemoryCleanup()
                }
            }
            
            logMemoryStats("Memory trim level $level")
        }
        
        fun getMemoryInfo(): MemoryInfo {
            activityManager.getMemoryInfo(memoryInfo)
            
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            
            return MemoryInfo(
                availableMemory = memoryInfo.availMem,
                totalMemory = memoryInfo.totalMem,
                lowMemoryThreshold = memoryInfo.threshold,
                isLowMemory = memoryInfo.lowMemory,
                appUsedMemory = usedMemory,
                appMaxMemory = maxMemory,
                appMemoryPercentage = (usedMemory.toFloat() / maxMemory * 100).toInt()
            )
        }
        
        fun checkMemoryPressure(): MemoryPressure {
            val info = getMemoryInfo()
            return when {
                info.isLowMemory -> MemoryPressure.CRITICAL
                info.appMemoryPercentage > 90 -> MemoryPressure.HIGH
                info.appMemoryPercentage > 75 -> MemoryPressure.MODERATE
                else -> MemoryPressure.LOW
            }
        }
        
        private fun performLightMemoryCleanup() {
            System.gc()
            logMemoryStats("Light cleanup")
        }
        
        private fun performModerateMemoryCleanup() {
            // Clear image caches
            clearCaches()
            System.gc()
            logMemoryStats("Moderate cleanup")
        }
        
        private fun performUIMemoryCleanup() {
            // Release UI-specific resources
            clearCaches()
            System.gc()
            logMemoryStats("UI cleanup")
        }
        
        private fun performAggressiveMemoryCleanup() {
            // Clear all caches and non-essential data
            clearCaches()
            performanceMetrics.clear()
            System.gc()
            logMemoryStats("Aggressive cleanup")
        }
        
        private fun clearCaches() {
            // Clear app-specific caches
            try {
                context.cacheDir.deleteRecursively()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache", e)
            }
        }
        
        private fun logMemoryStats(action: String) {
            val info = getMemoryInfo()
            Log.i(TAG, buildString {
                appendLine("Memory Stats - $action")
                appendLine("  Available: ${formatBytes(info.availableMemory)}")
                appendLine("  Total: ${formatBytes(info.totalMemory)}")
                appendLine("  App Used: ${formatBytes(info.appUsedMemory)} (${info.appMemoryPercentage}%)")
                appendLine("  Low Memory: ${info.isLowMemory}")
            })
        }
        
        fun requestMemoryWarning() {
            val now = SystemClock.elapsedRealtime()
            if (now - lastMemoryWarning > memoryWarningThreshold) {
                lastMemoryWarning = now
                Log.w(TAG, "Memory pressure detected: ${checkMemoryPressure()}")
            }
        }
    }
    
    data class MemoryInfo(
        val availableMemory: Long,
        val totalMemory: Long,
        val lowMemoryThreshold: Long,
        val isLowMemory: Boolean,
        val appUsedMemory: Long,
        val appMaxMemory: Long,
        val appMemoryPercentage: Int
    )
    
    enum class MemoryPressure {
        LOW, MODERATE, HIGH, CRITICAL
    }
    
    // Performance measurement functions
    inline fun <T> measure(metricName: String, block: () -> T): T {
        val startTime = SystemClock.elapsedRealtimeNanos()
        return try {
            block()
        } finally {
            val endTime = SystemClock.elapsedRealtimeNanos()
            val duration = (endTime - startTime) / 1_000_000 // Convert to milliseconds
            recordMetric(metricName, duration)
        }
    }
    
    suspend inline fun <T> measureSuspend(metricName: String, crossinline block: suspend () -> T): T {
        var result: T
        val duration = measureTimeMillis {
            result = block()
        }
        recordMetric(metricName, duration)
        return result
    }
    
    fun recordMetric(metricName: String, duration: Long) {
        val metric = performanceMetrics.getOrPut(metricName) { 
            PerformanceMetric(metricName) 
        }
        
        synchronized(metric) {
            metric.totalTime += duration
            metric.count++
            metric.minTime = minOf(metric.minTime, duration)
            metric.maxTime = maxOf(metric.maxTime, duration)
            metric.lastTime = duration
        }
        
        // Notify monitors
        if (activeMonitors.isNotEmpty()) {
            performanceScope.launch {
                val metrics = performanceMetrics.toMap()
                activeMonitors.forEach { monitor ->
                    monitor.onPerformanceUpdate(metrics)
                }
            }
        }
    }
    
    fun startPerformanceMonitoring() {
        performanceScope.launch {
            while (isActive) {
                checkPerformanceHealth()
                delay(10000) // Check every 10 seconds
            }
        }
    }
    
    private fun checkPerformanceHealth() {
        // Check memory pressure
        val memoryPressure = memoryManager.checkMemoryPressure()
        if (memoryPressure >= MemoryPressure.HIGH) {
            memoryManager.requestMemoryWarning()
        }
        
        // Check for performance anomalies
        performanceMetrics.values.forEach { metric ->
            if (metric.count > 10 && metric.maxTime > metric.averageTime * 5) {
                Log.w(TAG, "Performance anomaly detected for ${metric.name}: max=${metric.maxTime}ms, avg=${metric.averageTime}ms")
            }
        }
    }
    
    fun registerMonitor(monitor: PerformanceMonitor) {
        activeMonitors.add(monitor)
    }
    
    fun unregisterMonitor(monitor: PerformanceMonitor) {
        activeMonitors.remove(monitor)
    }
    
    fun getMetrics(): Map<String, PerformanceMetric> {
        return performanceMetrics.toMap()
    }
    
    fun clearMetrics() {
        performanceMetrics.clear()
    }
    
    fun optimizeForVideoPlayback() {
        // Set thread priorities for video playback
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY)
        
        // Request high performance mode if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.getSystemService(Context.POWER_SERVICE)?.let { powerManager ->
                // Power optimizations
            }
        }
        
        Log.i(TAG, "Optimized for video playback")
    }
    
    fun optimizeForBackgroundPlayback() {
        // Lower thread priority for background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
        
        // Reduce memory usage
        memoryManager.trimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        
        Log.i(TAG, "Optimized for background playback")
    }
    
    fun cleanup() {
        performanceScope.cancel()
        performanceMetrics.clear()
        activeMonitors.clear()
    }
    
    companion object {
        private const val TAG = "PerformanceManager"
        
        fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
            }
        }
    }
}