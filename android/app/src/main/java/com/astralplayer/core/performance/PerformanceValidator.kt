package com.astralplayer.core.performance

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import androidx.annotation.RequiresApi
import com.astralplayer.core.codec.CodecManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceValidator @Inject constructor(
    private val context: Context,
    private val codecManager: CodecManager
) {
    
    data class PerformanceReport(
        val deviceModel: String,
        val androidVersion: Int,
        val supportedCodecs: List<String>,
        val hardwareAcceleration: Boolean,
        val hdrSupport: Boolean,
        val maxResolution: String,
        val networkCapabilities: NetworkCapabilities,
        val memoryInfo: MemoryInfo,
        val storageInfo: StorageInfo,
        val overallScore: Int
    )
    
    data class NetworkCapabilities(
        val wifiEnabled: Boolean,
        val mobileDataEnabled: Boolean,
        val downloadSpeedMbps: Double,
        val latencyMs: Long
    )
    
    data class MemoryInfo(
        val totalMemoryMB: Long,
        val availableMemoryMB: Long,
        val lowMemoryThreshold: Long
    )
    
    data class StorageInfo(
        val totalStorageGB: Long,
        val availableStorageGB: Long,
        val cacheDirectoryMB: Long
    )
    
    suspend fun validatePerformance(): PerformanceReport = withContext(Dispatchers.Default) {
        Timber.i("Starting comprehensive performance validation")
        
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = Build.VERSION.SDK_INT
        
        val supportedCodecs = getSupportedCodecs()
        val hardwareAcceleration = checkHardwareAcceleration()
        val hdrSupport = checkHDRSupport()
        val maxResolution = getMaxSupportedResolution()
        val networkCapabilities = analyzeNetworkCapabilities()
        val memoryInfo = analyzeMemoryInfo()
        val storageInfo = analyzeStorageInfo()
        
        val overallScore = calculateOverallScore(
            supportedCodecs.size,
            hardwareAcceleration,
            hdrSupport,
            networkCapabilities,
            memoryInfo,
            storageInfo
        )
        
        val report = PerformanceReport(
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            supportedCodecs = supportedCodecs,
            hardwareAcceleration = hardwareAcceleration,
            hdrSupport = hdrSupport,
            maxResolution = maxResolution,
            networkCapabilities = networkCapabilities,
            memoryInfo = memoryInfo,
            storageInfo = storageInfo,
            overallScore = overallScore
        )
        
        Timber.i("Performance validation completed. Overall score: $overallScore")
        logPerformanceReport(report)
        
        return@withContext report
    }
    
    private fun getSupportedCodecs(): List<String> {
        val supportedCodecs = mutableListOf<String>()
        
        try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            
            for (codecInfo in codecList.codecInfos) {
                if (!codecInfo.isEncoder) {
                    for (type in codecInfo.supportedTypes) {
                        if (type.startsWith("video/")) {
                            supportedCodecs.add("${codecInfo.name} ($type)")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error analyzing supported codecs")
        }
        
        return supportedCodecs.distinct()
    }
    
    private fun checkHardwareAcceleration(): Boolean {
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            codecList.codecInfos.any { codecInfo ->
                !codecInfo.isEncoder &&
                codecInfo.supportedTypes.any { it.startsWith("video/") } &&
                codecInfo.isHardwareAccelerated
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking hardware acceleration")
            false
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.N)
    private fun checkHDRSupport(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val display = context.display
                display?.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true
            } catch (e: Exception) {
                Timber.e(e, "Error checking HDR support")
                false
            }
        } else {
            false
        }
    }
    
    private fun getMaxSupportedResolution(): String {
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            var maxWidth = 0
            var maxHeight = 0
            
            for (codecInfo in codecList.codecInfos) {
                if (!codecInfo.isEncoder) {
                    for (type in codecInfo.supportedTypes) {
                        if (type.startsWith("video/")) {
                            try {
                                val capabilities = codecInfo.getCapabilitiesForType(type)
                                val videoCapabilities = capabilities.videoCapabilities
                                if (videoCapabilities != null) {
                                    maxWidth = maxOf(maxWidth, videoCapabilities.supportedWidths.upper)
                                    maxHeight = maxOf(maxHeight, videoCapabilities.supportedHeights.upper)
                                }
                            } catch (e: Exception) {
                                // Skip this codec
                            }
                        }
                    }
                }
            }
            
            when {
                maxWidth >= 3840 && maxHeight >= 2160 -> "4K (3840x2160)"
                maxWidth >= 2560 && maxHeight >= 1440 -> "QHD (2560x1440)"
                maxWidth >= 1920 && maxHeight >= 1080 -> "FHD (1920x1080)"
                maxWidth >= 1280 && maxHeight >= 720 -> "HD (1280x720)"
                else -> "SD (${maxWidth}x${maxHeight})"
            }
        } catch (e: Exception) {
            Timber.e(e, "Error determining max resolution")
            "Unknown"
        }
    }
    
    private fun analyzeNetworkCapabilities(): NetworkCapabilities {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        
        val wifiEnabled = try {
            connectivityManager.getNetworkInfo(android.net.ConnectivityManager.TYPE_WIFI)?.isConnected == true
        } catch (e: Exception) {
            false
        }
        
        val mobileDataEnabled = try {
            connectivityManager.getNetworkInfo(android.net.ConnectivityManager.TYPE_MOBILE)?.isConnected == true
        } catch (e: Exception) {
            false
        }
        
        // Simplified network speed estimation
        val downloadSpeedMbps = estimateDownloadSpeed()
        val latencyMs = estimateLatency()
        
        return NetworkCapabilities(
            wifiEnabled = wifiEnabled,
            mobileDataEnabled = mobileDataEnabled,
            downloadSpeedMbps = downloadSpeedMbps,
            latencyMs = latencyMs
        )
    }
    
    private fun estimateDownloadSpeed(): Double {
        // This is a simplified estimation - in a real app you might want to
        // perform actual speed tests or use network type to estimate
        return 25.0 // Assume moderate broadband
    }
    
    private fun estimateLatency(): Long {
        // Simplified latency estimation
        return 50L // Assume 50ms latency
    }
    
    private fun analyzeMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        return MemoryInfo(
            totalMemoryMB = memInfo.totalMem / (1024 * 1024),
            availableMemoryMB = memInfo.availMem / (1024 * 1024),
            lowMemoryThreshold = memInfo.threshold / (1024 * 1024)
        )
    }
    
    private fun analyzeStorageInfo(): StorageInfo {
        val cacheDir = context.cacheDir
        val totalSpace = cacheDir.totalSpace / (1024 * 1024 * 1024)
        val freeSpace = cacheDir.freeSpace / (1024 * 1024 * 1024)
        val cacheSize = cacheDir.length() / (1024 * 1024)
        
        return StorageInfo(
            totalStorageGB = totalSpace,
            availableStorageGB = freeSpace,
            cacheDirectoryMB = cacheSize
        )
    }
    
    private fun calculateOverallScore(
        codecCount: Int,
        hasHardwareAcceleration: Boolean,
        hasHDRSupport: Boolean,
        networkCapabilities: NetworkCapabilities,
        memoryInfo: MemoryInfo,
        storageInfo: StorageInfo
    ): Int {
        var score = 0
        
        // Codec support (0-30 points)
        score += minOf(30, codecCount * 2)
        
        // Hardware acceleration (0-20 points)
        if (hasHardwareAcceleration) score += 20
        
        // HDR support (0-10 points)
        if (hasHDRSupport) score += 10
        
        // Network capabilities (0-20 points)
        if (networkCapabilities.wifiEnabled || networkCapabilities.mobileDataEnabled) {
            score += 10
            if (networkCapabilities.downloadSpeedMbps > 10) score += 10
        }
        
        // Memory (0-15 points)
        when {
            memoryInfo.totalMemoryMB > 6000 -> score += 15
            memoryInfo.totalMemoryMB > 4000 -> score += 10
            memoryInfo.totalMemoryMB > 2000 -> score += 5
        }
        
        // Storage (0-5 points)
        if (storageInfo.availableStorageGB > 2) score += 5
        
        return minOf(100, score)
    }
    
    private fun logPerformanceReport(report: PerformanceReport) {
        Timber.i("=== Elite Performance Report ===")
        Timber.i("Device: ${report.deviceModel}")
        Timber.i("Android: API ${report.androidVersion}")
        Timber.i("Supported Codecs: ${report.supportedCodecs.size}")
        Timber.i("Hardware Acceleration: ${report.hardwareAcceleration}")
        Timber.i("HDR Support: ${report.hdrSupport}")
        Timber.i("Max Resolution: ${report.maxResolution}")
        Timber.i("Memory: ${report.memoryInfo.availableMemoryMB}MB / ${report.memoryInfo.totalMemoryMB}MB")
        Timber.i("Storage: ${report.storageInfo.availableStorageGB}GB available")
        Timber.i("Overall Score: ${report.overallScore}/100")
        Timber.i("=== End Performance Report ===")
    }
    
    fun validateAdultContentOptimizations(): Boolean {
        return try {
            codecManager.validateAdultContentSupport()
        } catch (e: Exception) {
            Timber.e(e, "Failed to validate adult content optimizations")
            false
        }
    }
    
    fun validateStreamingCapabilities(): Boolean {
        return try {
            val hlsSupport = codecManager.supportsHLS()
            val dashSupport = codecManager.supportsDASH()
            val rtmpSupport = codecManager.supportsRTMP()
            
            Timber.d("Streaming support - HLS: $hlsSupport, DASH: $dashSupport, RTMP: $rtmpSupport")
            
            hlsSupport && dashSupport
        } catch (e: Exception) {
            Timber.e(e, "Failed to validate streaming capabilities")
            false
        }
    }
}