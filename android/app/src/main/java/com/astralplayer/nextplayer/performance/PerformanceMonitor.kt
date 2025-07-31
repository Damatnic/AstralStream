package com.astralplayer.nextplayer.performance

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val measurements = ConcurrentHashMap<String, Long>()
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    data class PerformanceMetrics(
        val avgFrameTime: Float = 0f,
        val droppedFrames: Int = 0,
        val memoryUsage: Long = 0,
        val cpuUsage: Float = 0f,
        val networkLatency: Long = 0,
        val diskIOTime: Long = 0
    )
    
    fun startMeasure(operation: String): Long {
        val startTime = System.nanoTime()
        measurements[operation] = startTime
        return startTime
    }
    
    fun endMeasure(operation: String, startTime: Long? = null): Long {
        val endTime = System.nanoTime()
        val actualStartTime = startTime ?: measurements[operation] ?: return 0
        val duration = (endTime - actualStartTime) / 1_000_000 // Convert to milliseconds
        
        Log.d("PerformanceMonitor", "$operation took ${duration}ms")
        
        // Update metrics based on operation type
        updateMetricsForOperation(operation, duration)
        
        measurements.remove(operation)
        return duration
    }
    
    private fun updateMetricsForOperation(operation: String, duration: Long) {
        scope.launch {
            val current = _performanceMetrics.value
            val updated = when {
                operation.contains("frame") -> current.copy(avgFrameTime = duration.toFloat())
                operation.contains("memory") -> current.copy(memoryUsage = duration)
                operation.contains("network") -> current.copy(networkLatency = duration)
                operation.contains("disk") || operation.contains("io") -> current.copy(diskIOTime = duration)
                else -> current
            }
            _performanceMetrics.value = updated
        }
    }
    
    fun trackMemoryUsage() {
        scope.launch {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val current = _performanceMetrics.value
            _performanceMetrics.value = current.copy(memoryUsage = usedMemory / (1024 * 1024)) // MB
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.N)
    fun trackFrameMetrics(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.window.addOnFrameMetricsAvailableListener({ _, frameMetrics, _ ->
                val frameTime = frameMetrics.getMetric(android.view.FrameMetrics.TOTAL_DURATION) / 1_000_000f
                val current = _performanceMetrics.value
                _performanceMetrics.value = current.copy(avgFrameTime = frameTime)
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        }
    }
    
    fun measureNetworkLatency(url: String, callback: (Long) -> Unit) {
        scope.launch {
            val startTime = System.currentTimeMillis()
            try {
                // Simplified ping measurement
                val process = Runtime.getRuntime().exec("ping -c 1 $url")
                process.waitFor()
                val latency = System.currentTimeMillis() - startTime
                callback(latency)
                
                val current = _performanceMetrics.value
                _performanceMetrics.value = current.copy(networkLatency = latency)
            } catch (e: Exception) {
                Log.e("PerformanceMonitor", "Failed to measure network latency", e)
                callback(-1)
            }
        }
    }
    
    fun logSystemInfo() {
        Log.d("PerformanceMonitor", """
            === System Information ===
            Device: ${Build.MODEL}
            OS Version: ${Build.VERSION.RELEASE}
            SDK: ${Build.VERSION.SDK_INT}
            CPU Cores: ${Runtime.getRuntime().availableProcessors()}
            Max Memory: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)}MB
            Available Memory: ${Runtime.getRuntime().freeMemory() / (1024 * 1024)}MB
        """.trimIndent())
    }
}