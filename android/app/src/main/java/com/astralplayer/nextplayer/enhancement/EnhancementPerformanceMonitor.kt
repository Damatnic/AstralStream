package com.astralplayer.nextplayer.enhancement

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLES30
import android.os.Debug
import android.os.Process
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors performance metrics for video enhancement
 */
@Singleton
class EnhancementPerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _performanceData = MutableStateFlow<PerformanceData>(PerformanceData())
    val performanceData: StateFlow<PerformanceData> = _performanceData.asStateFlow()
    
    private var isMonitoring = false
    private var monitoringJob: Job? = null
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitoringJob = scope.launch {
            while (isMonitoring) {
                updatePerformanceMetrics()
                delay(MONITORING_INTERVAL_MS)
            }
        }
        
        Log.i(TAG, "Performance monitoring started")
    }
    
    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        
        Log.i(TAG, "Performance monitoring stopped")
    }
    
    /**
     * Get current GPU usage percentage
     */
    fun getGPUUsage(): Float {
        return try {
            // Try to read GPU usage from different possible sources
            readGPUUsageFromKgsl() ?: readGPUUsageFromMali() ?: readGPUUsageFromAdreno() ?: 0f
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read GPU usage: ${e.message}")
            0f
        }
    }
    
    /**
     * Get current CPU usage percentage
     */
    fun getCPUUsage(): Float {
        return try {
            val currentData = _performanceData.value
            calculateCPUUsage(currentData.previousCpuTime, currentData.previousIdleTime)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read CPU usage: ${e.message}")
            0f
        }
    }
    
    /**
     * Get memory usage information
     */
    fun getMemoryUsage(): MemoryUsage {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        
        return MemoryUsage(
            totalRAM = memoryInfo.totalMem,
            availableRAM = memoryInfo.availMem,
            usedRAM = memoryInfo.totalMem - memoryInfo.availMem,
            heapSize = runtime.totalMemory(),
            heapUsed = runtime.totalMemory() - runtime.freeMemory(),
            heapMax = runtime.maxMemory(),
            nativeHeap = Debug.getNativeHeapSize(),
            nativeHeapUsed = Debug.getNativeHeapAllocatedSize()
        )
    }
    
    /**
     * Get OpenGL performance metrics
     */
    fun getOpenGLMetrics(): OpenGLMetrics {
        return try {
            val renderer = GLES30.glGetString(GLES30.GL_RENDERER)
            val version = GLES30.glGetString(GLES30.GL_VERSION)
            val vendor = GLES30.glGetString(GLES30.GL_VENDOR)
            
            val maxTextureSize = IntArray(1)
            GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)
            
            val maxViewportDims = IntArray(2)
            GLES30.glGetIntegerv(GLES30.GL_MAX_VIEWPORT_DIMS, maxViewportDims, 0)
            
            OpenGLMetrics(
                renderer = renderer ?: "Unknown",
                version = version ?: "Unknown",
                vendor = vendor ?: "Unknown",
                maxTextureSize = maxTextureSize[0],
                maxViewportWidth = maxViewportDims[0],
                maxViewportHeight = maxViewportDims[1]
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get OpenGL metrics: ${e.message}")
            OpenGLMetrics()
        }
    }
    
    /**
     * Record frame processing time
     */
    fun recordFrameTime(processingTimeMs: Float) {
        val currentData = _performanceData.value
        val frameHistory = currentData.frameTimesMs.toMutableList()
        
        frameHistory.add(processingTimeMs)
        if (frameHistory.size > MAX_FRAME_HISTORY) {
            frameHistory.removeAt(0)
        }
        
        val avgFrameTime = frameHistory.average().toFloat()
        val fps = if (avgFrameTime > 0) 1000f / avgFrameTime else 0f
        
        _performanceData.value = currentData.copy(
            frameTimesMs = frameHistory,
            averageFrameTimeMs = avgFrameTime,
            currentFPS = fps,
            targetFPS = 60f, // Assuming 60fps target
            droppedFrames = if (processingTimeMs > 16.67f) currentData.droppedFrames + 1 else currentData.droppedFrames
        )
    }
    
    /**
     * Check if performance is within acceptable limits
     */
    fun isPerformanceAcceptable(): Boolean {
        val data = _performanceData.value
        return data.cpuUsage < CPU_USAGE_THRESHOLD &&
               data.gpuUsage < GPU_USAGE_THRESHOLD &&
               data.averageFrameTimeMs < FRAME_TIME_THRESHOLD &&
               data.memoryUsage.heapUsed < data.memoryUsage.heapMax * MEMORY_USAGE_THRESHOLD
    }
    
    /**
     * Get performance recommendations
     */
    fun getPerformanceRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val data = _performanceData.value
        
        if (data.cpuUsage > CPU_USAGE_THRESHOLD) {
            recommendations.add("High CPU usage detected. Consider reducing AI model complexity.")
        }
        
        if (data.gpuUsage > GPU_USAGE_THRESHOLD) {
            recommendations.add("High GPU usage detected. Consider reducing shader complexity or resolution.")
        }
        
        if (data.averageFrameTimeMs > FRAME_TIME_THRESHOLD) {
            recommendations.add("Frame processing time too high. Consider optimizing enhancement algorithms.")
        }
        
        val memoryUsage = data.memoryUsage.heapUsed.toFloat() / data.memoryUsage.heapMax
        if (memoryUsage > MEMORY_USAGE_THRESHOLD) {
            recommendations.add("High memory usage detected. Consider releasing unused resources.")
        }
        
        if (data.droppedFrames > DROPPED_FRAMES_THRESHOLD) {
            recommendations.add("Too many dropped frames. Consider reducing enhancement quality.")
        }
        
        return recommendations
    }
    
    private suspend fun updatePerformanceMetrics() {
        val cpuUsage = getCPUUsage()
        val gpuUsage = getGPUUsage()
        val memoryUsage = getMemoryUsage()
        
        val currentData = _performanceData.value
        _performanceData.value = currentData.copy(
            cpuUsage = cpuUsage,
            gpuUsage = gpuUsage,
            memoryUsage = memoryUsage,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun readGPUUsageFromKgsl(): Float? {
        return try {
            val file = File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage")
            if (file.exists()) {
                file.readText().trim().toFloatOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun readGPUUsageFromMali(): Float? {
        return try {
            val file = File("/sys/devices/platform/mali.0/utilization")
            if (file.exists()) {
                file.readText().trim().toFloatOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun readGPUUsageFromAdreno(): Float? {
        return try {
            val file = File("/sys/class/devfreq/qcom,gpubw.0/cur_freq")
            if (file.exists()) {
                // This is a rough approximation based on frequency
                val currentFreq = file.readText().trim().toLongOrNull() ?: return null
                val maxFreqFile = File("/sys/class/devfreq/qcom,gpubw.0/max_freq")
                val maxFreq = if (maxFreqFile.exists()) {
                    maxFreqFile.readText().trim().toLongOrNull() ?: return null
                } else return null
                
                (currentFreq.toFloat() / maxFreq * 100f).coerceIn(0f, 100f)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateCPUUsage(prevCpuTime: Long, prevIdleTime: Long): Float {
        return try {
            val statFile = File("/proc/stat")
            val statLine = statFile.readLines().firstOrNull() ?: return 0f
            
            val parts = statLine.split(" ").filter { it.isNotEmpty() }
            if (parts.size < 5) return 0f
            
            val user = parts[1].toLong()
            val nice = parts[2].toLong()
            val system = parts[3].toLong()
            val idle = parts[4].toLong()
            
            val totalCpuTime = user + nice + system + idle
            
            val cpuTimeDiff = totalCpuTime - prevCpuTime
            val idleTimeDiff = idle - prevIdleTime
            
            if (cpuTimeDiff <= 0) return 0f
            
            val usage = ((cpuTimeDiff - idleTimeDiff).toFloat() / cpuTimeDiff * 100f).coerceIn(0f, 100f)
            
            // Update previous values for next calculation
            val currentData = _performanceData.value
            _performanceData.value = currentData.copy(
                previousCpuTime = totalCpuTime,
                previousIdleTime = idle
            )
            
            usage
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        stopMonitoring()
        scope.cancel()
    }
    
    companion object {
        private const val TAG = "EnhancementPerformanceMonitor"
        private const val MONITORING_INTERVAL_MS = 1000L
        private const val MAX_FRAME_HISTORY = 60
        
        // Performance thresholds
        private const val CPU_USAGE_THRESHOLD = 80f
        private const val GPU_USAGE_THRESHOLD = 90f
        private const val FRAME_TIME_THRESHOLD = 16.67f // 60fps
        private const val MEMORY_USAGE_THRESHOLD = 0.8f
        private const val DROPPED_FRAMES_THRESHOLD = 5
    }
    
    /**
     * Performance data container
     */
    data class PerformanceData(
        val cpuUsage: Float = 0f,
        val gpuUsage: Float = 0f,
        val memoryUsage: MemoryUsage = MemoryUsage(),
        val frameTimesMs: List<Float> = emptyList(),
        val averageFrameTimeMs: Float = 0f,
        val currentFPS: Float = 0f,
        val targetFPS: Float = 60f,
        val droppedFrames: Int = 0,
        val timestamp: Long = System.currentTimeMillis(),
        val previousCpuTime: Long = 0,
        val previousIdleTime: Long = 0
    )
    
    /**
     * Memory usage information
     */
    data class MemoryUsage(
        val totalRAM: Long = 0,
        val availableRAM: Long = 0,
        val usedRAM: Long = 0,
        val heapSize: Long = 0,
        val heapUsed: Long = 0,
        val heapMax: Long = 0,
        val nativeHeap: Long = 0,
        val nativeHeapUsed: Long = 0
    )
    
    /**
     * OpenGL metrics
     */
    data class OpenGLMetrics(
        val renderer: String = "Unknown",
        val version: String = "Unknown",
        val vendor: String = "Unknown",
        val maxTextureSize: Int = 0,
        val maxViewportWidth: Int = 0,
        val maxViewportHeight: Int = 0
    )
}