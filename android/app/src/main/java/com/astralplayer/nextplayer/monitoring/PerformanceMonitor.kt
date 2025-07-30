package com.astralplayer.nextplayer.monitoring

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

/**
 * Comprehensive performance monitoring system for AstralStream
 */
class PerformanceMonitor(private val context: Context) {
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    private val _alerts = MutableSharedFlow<PerformanceAlert>()
    val alerts: SharedFlow<PerformanceAlert> = _alerts.asSharedFlow()
    
    private val operationTimings = ConcurrentHashMap<String, MutableList<Long>>()
    private val monitoringScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var isMonitoring = false
    
    /**
     * Start monitoring system performance
     */
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        monitoringScope.launch {
            // Monitor system resources every 5 seconds
            while (isMonitoring) {
                updateSystemMetrics()
                delay(5000)
            }
        }
        
        monitoringScope.launch {
            // Monitor for performance issues every second
            while (isMonitoring) {
                checkPerformanceAlerts()
                delay(1000)
            }
        }
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitoringScope.cancel()
    }
    
    /**
     * Track operation performance
     */
    suspend fun <T> trackOperation(operationName: String, operation: suspend () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = try {
            operation()
        } catch (e: Exception) {
            recordOperationError(operationName, e)
            throw e
        }
        
        val duration = System.currentTimeMillis() - startTime
        recordOperationTiming(operationName, duration)
        
        return result
    }
    
    /**
     * Monitor ExoPlayer performance
     */
    fun monitorPlayer(player: ExoPlayer) {
        monitoringScope.launch {
            while (isMonitoring) {
                updatePlayerMetrics(player)
                delay(2000)
            }
        }
    }
    
    private fun updateSystemMetrics() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        // Native heap info
        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapUsed = Debug.getNativeHeapAllocatedSize()
        
        // CPU usage (approximation)
        val cpuUsage = getCpuUsage()
        
        val currentMetrics = _performanceMetrics.value
        _performanceMetrics.value = currentMetrics.copy(
            systemMemory = SystemMemoryInfo(
                totalMemory = memoryInfo.totalMem,
                availableMemory = memoryInfo.availMem,
                usedMemory = memoryInfo.totalMem - memoryInfo.availMem,
                memoryPressure = when {
                    memoryInfo.lowMemory -> MemoryPressure.HIGH
                    (memoryInfo.availMem.toFloat() / memoryInfo.totalMem) < 0.2f -> MemoryPressure.MEDIUM
                    else -> MemoryPressure.LOW
                }
            ),
            appMemory = AppMemoryInfo(
                heapUsed = usedMemory,
                heapMax = maxMemory,
                heapFree = runtime.freeMemory(),
                nativeHeapSize = nativeHeapSize,
                nativeHeapUsed = nativeHeapUsed
            ),
            cpuUsage = cpuUsage,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun updatePlayerMetrics(player: ExoPlayer) {
        val videoFormat = player.videoFormat
        val audioFormat = player.audioFormat
        
        val currentMetrics = _performanceMetrics.value
        _performanceMetrics.value = currentMetrics.copy(
            playerMetrics = PlayerMetrics(
                isPlaying = player.isPlaying,
                playbackState = player.playbackState,
                bufferPercentage = player.bufferedPercentage,
                totalBufferedDuration = player.totalBufferedDuration,
                playbackSpeed = player.playbackParameters.speed,
                videoWidth = videoFormat?.width ?: 0,
                videoHeight = videoFormat?.height ?: 0,
                videoBitrate = videoFormat?.bitrate ?: 0,
                audioChannels = audioFormat?.channelCount ?: 0,
                audioSampleRate = audioFormat?.sampleRate ?: 0,
                droppedVideoFrames = player.videoDecoderCounters?.droppedBufferCount ?: 0,
                renderedVideoFrames = player.videoDecoderCounters?.renderedOutputBufferCount ?: 0
            )
        )
    }
    
    private fun recordOperationTiming(operationName: String, duration: Long) {
        val timings = operationTimings.getOrPut(operationName) { mutableListOf() }
        timings.add(duration)
        
        // Keep only last 100 measurements per operation
        if (timings.size > 100) {
            timings.removeAt(0)
        }
        
        // Update metrics with aggregated data
        val currentMetrics = _performanceMetrics.value
        val operationStats = timings.let { list ->
            OperationStats(
                operationName = operationName,
                averageTime = list.average(),
                minTime = list.minOrNull() ?: 0L,
                maxTime = list.maxOrNull() ?: 0L,
                totalCalls = list.size,
                recentCalls = list.takeLast(10).size
            )
        }
        
        val updatedOperations = currentMetrics.operationStats.toMutableMap()
        updatedOperations[operationName] = operationStats
        
        _performanceMetrics.value = currentMetrics.copy(
            operationStats = updatedOperations
        )
    }
    
    private fun recordOperationError(operationName: String, error: Throwable) {
        val currentMetrics = _performanceMetrics.value
        val errorCount = currentMetrics.errorCounts.getOrDefault(operationName, 0) + 1
        
        _performanceMetrics.value = currentMetrics.copy(
            errorCounts = currentMetrics.errorCounts + (operationName to errorCount)
        )
    }
    
    private fun checkPerformanceAlerts() {
        val metrics = _performanceMetrics.value
        
        // Memory pressure alert
        if (metrics.systemMemory.memoryPressure == MemoryPressure.HIGH) {
            emitAlert(PerformanceAlert.HighMemoryUsage(
                "System is low on memory (${metrics.systemMemory.availableMemory / 1024 / 1024}MB available)"
            ))
        }
        
        // App memory alert
        val heapUsagePercent = (metrics.appMemory.heapUsed.toFloat() / metrics.appMemory.heapMax) * 100
        if (heapUsagePercent > 85) {
            emitAlert(PerformanceAlert.HighHeapUsage(
                "App heap usage is high (${heapUsagePercent.toInt()}%)"
            ))
        }
        
        // CPU usage alert
        if (metrics.cpuUsage > 80) {
            emitAlert(PerformanceAlert.HighCpuUsage(
                "CPU usage is high (${metrics.cpuUsage}%)"
            ))
        }
        
        // Video frame drops alert
        val playerMetrics = metrics.playerMetrics
        if (playerMetrics.droppedVideoFrames > 0 && playerMetrics.renderedVideoFrames > 0) {
            val dropRate = (playerMetrics.droppedVideoFrames.toFloat() / 
                           (playerMetrics.droppedVideoFrames + playerMetrics.renderedVideoFrames)) * 100
            if (dropRate > 5) { // More than 5% frame drops
                emitAlert(PerformanceAlert.HighFrameDrops(
                    "Video frame drop rate is high (${dropRate.toInt()}%)"
                ))
            }
        }
        
        // Slow operation alerts
        metrics.operationStats.forEach { (operationName, stats) ->
            if (stats.averageTime > getSlowOperationThreshold(operationName)) {
                emitAlert(PerformanceAlert.SlowOperation(
                    "Operation '$operationName' is running slowly (avg: ${stats.averageTime.toInt()}ms)"
                ))
            }
        }
    }
    
    private fun emitAlert(alert: PerformanceAlert) {
        if (!_alerts.tryEmit(alert)) {
            // Alert buffer is full, log warning
            android.util.Log.w("PerformanceMonitor", "Alert buffer full, dropping alert: $alert")
        }
    }
    
    private fun getCpuUsage(): Int {
        // Simplified CPU usage calculation
        // In a real implementation, you might want to use a more sophisticated method
        return try {
            val pid = Process.myPid()
            val statFile = "/proc/$pid/stat"
            val stat = java.io.File(statFile).readText()
            // Parse CPU times and calculate usage percentage
            // This is a simplified version - a real implementation would track over time
            50 // Placeholder value
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getSlowOperationThreshold(operationName: String): Long {
        return when (operationName) {
            "video_load" -> 5000L // 5 seconds
            "subtitle_generate" -> 10000L // 10 seconds
            "chapter_detect" -> 3000L // 3 seconds
            "export_video" -> 30000L // 30 seconds
            else -> 2000L // 2 seconds default
        }
    }
    
    /**
     * Get performance summary
     */
    fun getPerformanceSummary(): PerformanceSummary {
        val metrics = _performanceMetrics.value
        
        val memoryUsagePercent = ((metrics.systemMemory.usedMemory.toFloat() / 
                                 metrics.systemMemory.totalMemory) * 100).toInt()
        
        val heapUsagePercent = ((metrics.appMemory.heapUsed.toFloat() / 
                               metrics.appMemory.heapMax) * 100).toInt()
        
        val totalOperations = metrics.operationStats.values.sumOf { it.totalCalls }
        val totalErrors = metrics.errorCounts.values.sum()
        val errorRate = if (totalOperations > 0) (totalErrors.toFloat() / totalOperations) * 100 else 0f
        
        return PerformanceSummary(
            memoryUsagePercent = memoryUsagePercent,
            heapUsagePercent = heapUsagePercent,
            cpuUsage = metrics.cpuUsage,
            totalOperations = totalOperations,
            errorRate = errorRate,
            averageOperationTime = metrics.operationStats.values.map { it.averageTime }.average().takeIf { !it.isNaN() } ?: 0.0,
            isHealthy = memoryUsagePercent < 80 && heapUsagePercent < 80 && metrics.cpuUsage < 70 && errorRate < 5
        )
    }
}

// Data classes for performance metrics
data class PerformanceMetrics(
    val systemMemory: SystemMemoryInfo = SystemMemoryInfo(),
    val appMemory: AppMemoryInfo = AppMemoryInfo(),
    val cpuUsage: Int = 0,
    val playerMetrics: PlayerMetrics = PlayerMetrics(),
    val operationStats: Map<String, OperationStats> = emptyMap(),
    val errorCounts: Map<String, Int> = emptyMap(),
    val lastUpdated: Long = 0L
)

data class SystemMemoryInfo(
    val totalMemory: Long = 0L,
    val availableMemory: Long = 0L,
    val usedMemory: Long = 0L,
    val memoryPressure: MemoryPressure = MemoryPressure.LOW
)

data class AppMemoryInfo(
    val heapUsed: Long = 0L,
    val heapMax: Long = 0L,
    val heapFree: Long = 0L,
    val nativeHeapSize: Long = 0L,
    val nativeHeapUsed: Long = 0L
)

data class PlayerMetrics(
    val isPlaying: Boolean = false,
    val playbackState: Int = 0,
    val bufferPercentage: Int = 0,
    val totalBufferedDuration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val videoBitrate: Int = 0,
    val audioChannels: Int = 0,
    val audioSampleRate: Int = 0,
    val droppedVideoFrames: Long = 0L,
    val renderedVideoFrames: Long = 0L
)

data class OperationStats(
    val operationName: String,
    val averageTime: Double,
    val minTime: Long,
    val maxTime: Long,
    val totalCalls: Int,
    val recentCalls: Int
)

data class PerformanceSummary(
    val memoryUsagePercent: Int,
    val heapUsagePercent: Int,
    val cpuUsage: Int,
    val totalOperations: Int,
    val errorRate: Float,
    val averageOperationTime: Double,
    val isHealthy: Boolean
)

enum class MemoryPressure {
    LOW, MEDIUM, HIGH
}

sealed class PerformanceAlert {
    data class HighMemoryUsage(val message: String) : PerformanceAlert()
    data class HighHeapUsage(val message: String) : PerformanceAlert()
    data class HighCpuUsage(val message: String) : PerformanceAlert()
    data class HighFrameDrops(val message: String) : PerformanceAlert()
    data class SlowOperation(val message: String) : PerformanceAlert()
}