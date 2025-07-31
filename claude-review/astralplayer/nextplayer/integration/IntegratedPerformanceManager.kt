package com.astralplayer.nextplayer.integration

import android.content.Context
import android.os.PowerManager
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntegratedPerformanceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    private var monitoringJob: Job? = null
    private var isMonitoring = false
    
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitoringJob = scope.launch {
            while (isActive) {
                updateMetrics()
                delay(2000) // Update every 2 seconds
            }
        }
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    private suspend fun updateMetrics() {
        withContext(Dispatchers.Default) {
            try {
                val cpuUsage = getCPUUsage()
                val memoryUsage = getMemoryUsage()
                val gpuUsage = getGPUUsage()
                val batteryUsage = getBatteryUsage()
                val thermalState = getThermalState()
                
                _performanceMetrics.value = PerformanceMetrics(
                    cpuUsage = cpuUsage,
                    memoryUsage = memoryUsage,
                    gpuUsage = gpuUsage,
                    batteryUsage = batteryUsage,
                    thermalState = thermalState,
                    timestamp = System.currentTimeMillis()
                )
                
            } catch (e: Exception) {
                // Handle monitoring errors gracefully
            }
        }
    }
    
    private fun getCPUUsage(): Float {
        return try {
            val runtime = Runtime.getRuntime()
            val availableProcessors = runtime.availableProcessors()
            
            // Read CPU stats from /proc/stat
            val statFile = java.io.File("/proc/stat")
            if (statFile.exists()) {
                val statLine = statFile.readLines().firstOrNull() ?: return 0f
                val stats = statLine.split(" ").filter { it.isNotEmpty() }
                
                if (stats.size >= 5) {
                    val user = stats[1].toLongOrNull() ?: 0L
                    val nice = stats[2].toLongOrNull() ?: 0L
                    val system = stats[3].toLongOrNull() ?: 0L
                    val idle = stats[4].toLongOrNull() ?: 0L
                    
                    val total = user + nice + system + idle
                    val usage = if (total > 0) ((total - idle).toFloat() / total * 100f) else 0f
                    
                    usage.coerceIn(0f, 100f)
                } else 0f
            } else 0f
            
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun getMemoryUsage(): Float {
        return try {
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            
            (usedMemory.toFloat() / totalMemory * 100f).coerceIn(0f, 100f)
            
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun getGPUUsage(): Float {
        return try {
            // Try to read GPU usage from various system files
            val gpuFiles = listOf(
                "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
                "/sys/devices/platform/mali.0/utilization",
                "/sys/class/devfreq/qcom,gpubw.0/cur_freq"
            )
            
            for (filePath in gpuFiles) {
                val file = java.io.File(filePath)
                if (file.exists()) {
                    val value = file.readText().trim().toFloatOrNull()
                    if (value != null) {
                        return value.coerceIn(0f, 100f)
                    }
                }
            }
            
            0f // Default if no GPU stats available
            
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun getBatteryUsage(): Float {
        return try {
            // Estimate battery usage based on CPU and system load
            val cpuLoad = _performanceMetrics.value.cpuUsage
            val memoryLoad = _performanceMetrics.value.memoryUsage
            
            // Simple heuristic: higher CPU/memory usage = higher battery usage
            val estimatedUsage = (cpuLoad * 0.6f + memoryLoad * 0.4f)
            estimatedUsage.coerceIn(0f, 100f)
            
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun getThermalState(): ThermalState {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getThermalStateQ()
            } else {
                getThermalStateLegacy()
            }
        } catch (e: Exception) {
            ThermalState.NORMAL
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getThermalStateQ(): ThermalState {
        return try {
            val thermalService = context.getSystemService(Context.THERMAL_SERVICE) as? android.os.PowerManager.ThermalService
            
            // This is a simplified approach - actual implementation would use ThermalService
            when (powerManager.isPowerSaveMode) {
                true -> ThermalState.WARM // Device in power save mode suggests thermal concern
                false -> ThermalState.NORMAL
            }
        } catch (e: Exception) {
            ThermalState.NORMAL
        }
    }
    
    private fun getThermalStateLegacy(): ThermalState {
        return try {
            // Read thermal zone files on older Android versions
            val thermalFiles = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp"
            )
            
            for (filePath in thermalFiles) {
                val file = java.io.File(filePath)
                if (file.exists()) {
                    val temp = file.readText().trim().toIntOrNull()
                    if (temp != null) {
                        // Temperature is usually in millicelsius
                        val celsius = temp / 1000
                        return when {
                            celsius > 70 -> ThermalState.CRITICAL
                            celsius > 60 -> ThermalState.HOT
                            celsius > 50 -> ThermalState.WARM
                            else -> ThermalState.NORMAL
                        }
                    }
                }
            }
            
            ThermalState.NORMAL
            
        } catch (e: Exception) {
            ThermalState.NORMAL
        }
    }
    
    fun getCurrentMetrics(): PerformanceMetrics {
        return _performanceMetrics.value
    }
    
    fun getPerformanceScore(): Float {
        val metrics = _performanceMetrics.value
        
        // Calculate composite performance score (higher is better)
        val cpuScore = (100f - metrics.cpuUsage) / 100f
        val memoryScore = (100f - metrics.memoryUsage) / 100f
        val gpuScore = (100f - metrics.gpuUsage) / 100f
        val batteryScore = (100f - metrics.batteryUsage) / 100f
        val thermalScore = when (metrics.thermalState) {
            ThermalState.NORMAL -> 1.0f
            ThermalState.WARM -> 0.8f
            ThermalState.HOT -> 0.5f
            ThermalState.CRITICAL -> 0.2f
        }
        
        return (cpuScore + memoryScore + gpuScore + batteryScore + thermalScore) / 5f
    }
    
    fun getPerformanceRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val metrics = _performanceMetrics.value
        
        if (metrics.cpuUsage > 80f) {
            recommendations.add("High CPU usage: Consider reducing video enhancement quality")
        }
        
        if (metrics.memoryUsage > 85f) {
            recommendations.add("High memory usage: Reduce buffer sizes or disable features")
        }
        
        if (metrics.gpuUsage > 90f) {
            recommendations.add("GPU overloaded: Disable AI video enhancements")
        }
        
        if (metrics.batteryUsage > 85f) {
            recommendations.add("High battery usage: Enable power saving mode")
        }
        
        when (metrics.thermalState) {
            ThermalState.WARM -> recommendations.add("Device warming: Monitor thermal state")
            ThermalState.HOT -> recommendations.add("Device hot: Reduce processing intensity")
            ThermalState.CRITICAL -> recommendations.add("Critical temperature: Disable intensive features immediately")
            else -> {}
        }
        
        return recommendations
    }
    
    fun isPerformanceOptimal(): Boolean {
        val metrics = _performanceMetrics.value
        return metrics.cpuUsage < 70f && 
               metrics.memoryUsage < 80f && 
               metrics.gpuUsage < 80f && 
               metrics.batteryUsage < 75f && 
               metrics.thermalState == ThermalState.NORMAL
    }
    
    fun release() {
        stopMonitoring()
        scope.cancel()
    }
    
    // Data classes and enums
    data class PerformanceMetrics(
        val cpuUsage: Float = 0f,
        val memoryUsage: Float = 0f,
        val gpuUsage: Float = 0f,
        val batteryUsage: Float = 0f,
        val thermalState: ThermalState = ThermalState.NORMAL,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    enum class ThermalState {
        NORMAL,
        WARM,
        HOT,
        CRITICAL
    }
}