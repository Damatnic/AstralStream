package com.astralplayer.core.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Real-time performance monitoring
 * Implements PerformanceAgent requirements for 60 FPS and memory optimization
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TARGET_FPS = 60
        private const val TARGET_FRAME_TIME_MS = 16.67f // 60 FPS
        private const val MEMORY_WARNING_THRESHOLD_MB = 150
        private const val MEMORY_CRITICAL_THRESHOLD_MB = 200
        private const val MONITORING_INTERVAL_MS = 1000L
    }
    
    private val _performanceState = MutableStateFlow(PerformanceMetrics())
    val performanceState: StateFlow<PerformanceMetrics> = _performanceState.asStateFlow()
    
    private var frameCallback: Choreographer.FrameCallback? = null
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var droppedFrames = 0
    
    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false
    
    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        startFrameMonitoring()
        startMemoryMonitoring()
        startCpuMonitoring()
        
        Timber.d("Performance monitoring started")
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        
        frameCallback?.let {
            Choreographer.getInstance().removeFrameCallback(it)
        }
        
        handler.removeCallbacksAndMessages(null)
        
        Timber.d("Performance monitoring stopped")
    }
    
    /**
     * Monitor frame rate and jank
     */
    private fun startFrameMonitoring() {
        frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isMonitoring) return
                
                if (lastFrameTime != 0L) {
                    val frameDuration = TimeUnit.NANOSECONDS.toMillis(frameTimeNanos - lastFrameTime)
                    
                    // Detect dropped frames (jank)
                    if (frameDuration > TARGET_FRAME_TIME_MS * 2) {
                        droppedFrames++
                        Timber.w("Frame dropped! Duration: ${frameDuration}ms")
                    }
                    
                    frameCount++
                }
                
                lastFrameTime = frameTimeNanos
                
                // Continue monitoring
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        
        Choreographer.getInstance().postFrameCallback(frameCallback!!)
        
        // Calculate FPS every second
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isMonitoring) return
                
                val fps = frameCount
                val jankPercentage = if (frameCount > 0) {
                    (droppedFrames.toFloat() / frameCount * 100).roundToInt()
                } else 0
                
                _performanceState.value = _performanceState.value.copy(
                    currentFps = fps,
                    averageFps = calculateAverageFps(fps),
                    droppedFrames = droppedFrames,
                    jankPercentage = jankPercentage,
                    isSmoothPlayback = fps >= TARGET_FPS - 5 // Allow small margin
                )
                
                // Reset counters
                frameCount = 0
                droppedFrames = 0
                
                handler.postDelayed(this, MONITORING_INTERVAL_MS)
            }
        }, MONITORING_INTERVAL_MS)
    }
    
    /**
     * Monitor memory usage
     */
    private fun startMemoryMonitoring() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isMonitoring) return
                
                val memInfo = getMemoryInfo()
                
                _performanceState.value = _performanceState.value.copy(
                    memoryUsageMB = memInfo.usedMemoryMB,
                    totalMemoryMB = memInfo.totalMemoryMB,
                    availableMemoryMB = memInfo.availableMemoryMB,
                    memoryWarning = when {
                        memInfo.usedMemoryMB > MEMORY_CRITICAL_THRESHOLD_MB -> MemoryWarning.CRITICAL
                        memInfo.usedMemoryMB > MEMORY_WARNING_THRESHOLD_MB -> MemoryWarning.HIGH
                        else -> MemoryWarning.NORMAL
                    }
                )
                
                // Check for memory leaks
                if (memInfo.usedMemoryMB > MEMORY_CRITICAL_THRESHOLD_MB) {
                    Timber.e("Critical memory usage: ${memInfo.usedMemoryMB}MB")
                    triggerMemoryOptimization()
                }
                
                handler.postDelayed(this, MONITORING_INTERVAL_MS * 5) // Check every 5 seconds
            }
        }, MONITORING_INTERVAL_MS)
    }
    
    /**
     * Monitor CPU usage
     */
    private fun startCpuMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (!isMonitoring) return
                    
                    val cpuUsage = getCpuUsage()
                    
                    _performanceState.value = _performanceState.value.copy(
                        cpuUsagePercent = cpuUsage,
                        isHighCpuUsage = cpuUsage > 80
                    )
                    
                    handler.postDelayed(this, MONITORING_INTERVAL_MS * 3) // Check every 3 seconds
                }
            }, MONITORING_INTERVAL_MS)
        }
    }
    
    /**
     * Get current memory info
     */
    private fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L // Convert to MB
        val maxMemory = runtime.maxMemory() / 1048576L
        
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return MemoryInfo(
            usedMemoryMB = usedMemory.toInt(),
            totalMemoryMB = maxMemory.toInt(),
            availableMemoryMB = (memoryInfo.availMem / 1048576L).toInt(),
            lowMemory = memoryInfo.lowMemory
        )
    }
    
    /**
     * Get CPU usage percentage
     */
    private fun getCpuUsage(): Int {
        return try {
            // This is a simplified implementation
            // In production, you'd use more sophisticated CPU monitoring
            val cores = Runtime.getRuntime().availableProcessors()
            val loadAverage = Debug.threadCpuTimeNanos() / 1_000_000_000.0
            ((loadAverage / cores) * 100).toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Calculate rolling average FPS
     */
    private val fpsHistory = mutableListOf<Int>()
    private fun calculateAverageFps(currentFps: Int): Int {
        fpsHistory.add(currentFps)
        if (fpsHistory.size > 60) { // Keep last 60 samples
            fpsHistory.removeAt(0)
        }
        return fpsHistory.average().roundToInt()
    }
    
    /**
     * Trigger memory optimization when critical
     */
    private fun triggerMemoryOptimization() {
        // Clear image cache
        System.gc()
        
        // Notify app to reduce memory usage
        _performanceState.value = _performanceState.value.copy(
            needsMemoryOptimization = true
        )
    }
    
    /**
     * Get performance report
     */
    fun getPerformanceReport(): PerformanceReport {
        val metrics = _performanceState.value
        
        return PerformanceReport(
            averageFps = metrics.averageFps,
            minFps = fpsHistory.minOrNull() ?: 0,
            maxFps = fpsHistory.maxOrNull() ?: 0,
            jankPercentage = metrics.jankPercentage,
            averageMemoryMB = metrics.memoryUsageMB,
            peakMemoryMB = metrics.totalMemoryMB,
            performanceScore = calculatePerformanceScore(metrics),
            recommendations = generatePerformanceRecommendations(metrics)
        )
    }
    
    private fun calculatePerformanceScore(metrics: PerformanceMetrics): Int {
        var score = 100
        
        // FPS impact (40% weight)
        val fpsScore = (metrics.averageFps.toFloat() / TARGET_FPS * 40).roundToInt()
        score = score.coerceAtMost(60 + fpsScore)
        
        // Memory impact (30% weight)
        if (metrics.memoryUsageMB > MEMORY_WARNING_THRESHOLD_MB) {
            score -= ((metrics.memoryUsageMB - MEMORY_WARNING_THRESHOLD_MB) * 0.5).roundToInt()
        }
        
        // Jank impact (30% weight)
        score -= metrics.jankPercentage
        
        return score.coerceIn(0, 100)
    }
    
    private fun generatePerformanceRecommendations(metrics: PerformanceMetrics): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (metrics.averageFps < TARGET_FPS - 10) {
            recommendations.add("Enable hardware acceleration for better frame rate")
        }
        
        if (metrics.memoryUsageMB > MEMORY_WARNING_THRESHOLD_MB) {
            recommendations.add("High memory usage detected. Consider clearing cache")
        }
        
        if (metrics.jankPercentage > 5) {
            recommendations.add("UI jank detected. Review heavy operations on main thread")
        }
        
        return recommendations
    }
    
    data class PerformanceMetrics(
        val currentFps: Int = 0,
        val averageFps: Int = 0,
        val droppedFrames: Int = 0,
        val jankPercentage: Int = 0,
        val memoryUsageMB: Int = 0,
        val totalMemoryMB: Int = 0,
        val availableMemoryMB: Int = 0,
        val cpuUsagePercent: Int = 0,
        val isSmoothPlayback: Boolean = true,
        val memoryWarning: MemoryWarning = MemoryWarning.NORMAL,
        val isHighCpuUsage: Boolean = false,
        val needsMemoryOptimization: Boolean = false
    )
    
    data class MemoryInfo(
        val usedMemoryMB: Int,
        val totalMemoryMB: Int,
        val availableMemoryMB: Int,
        val lowMemory: Boolean
    )
    
    data class PerformanceReport(
        val averageFps: Int,
        val minFps: Int,
        val maxFps: Int,
        val jankPercentage: Int,
        val averageMemoryMB: Int,
        val peakMemoryMB: Int,
        val performanceScore: Int,
        val recommendations: List<String>
    )
    
    enum class MemoryWarning {
        NORMAL, HIGH, CRITICAL
    }
}