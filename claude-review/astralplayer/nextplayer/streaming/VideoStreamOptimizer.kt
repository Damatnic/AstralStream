package com.astralplayer.nextplayer.streaming

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * Advanced video streaming optimization system for enhanced playback performance
 */
class VideoStreamOptimizer(
    private val context: Context,
    private val player: ExoPlayer
) {
    
    private val _optimizationStatus = MutableStateFlow(OptimizationStatus.Idle)
    val optimizationStatus: StateFlow<OptimizationStatus> = _optimizationStatus.asStateFlow()
    
    private val _streamingMetrics = MutableSharedFlow<StreamingMetrics>()
    val streamingMetrics: SharedFlow<StreamingMetrics> = _streamingMetrics.asSharedFlow()
    
    private val optimizationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private var adaptiveBitrateManager = AdaptiveBitrateManager()
    private var bufferOptimizer = BufferOptimizer()
    private var networkAnalyzer = NetworkAnalyzer(connectivityManager)
    private var cacheManager = StreamingCacheManager(context)
    
    private var currentOptimizationConfig = StreamOptimizationConfig()
    private var isOptimizationActive = false
    
    /**
     * Initialize streaming optimization with configuration
     */
    suspend fun initializeOptimization(config: StreamOptimizationConfig = StreamOptimizationConfig()) {
        currentOptimizationConfig = config
        _optimizationStatus.value = OptimizationStatus.Initializing
        
        try {
            // Initialize components
            adaptiveBitrateManager.initialize(config.bitrateConfig)
            bufferOptimizer.initialize(config.bufferConfig)
            networkAnalyzer.initialize()
            cacheManager.initialize(config.cacheConfig)
            
            // Start monitoring
            startOptimizationMonitoring()
            
            _optimizationStatus.value = OptimizationStatus.Active
            isOptimizationActive = true
            
        } catch (e: Exception) {
            _optimizationStatus.value = OptimizationStatus.Error(e)
            throw e
        }
    }
    
    /**
     * Optimize video stream for current conditions
     */
    suspend fun optimizeStream(mediaUri: Uri): StreamOptimizationResult = withContext(Dispatchers.IO) {
        try {
            _optimizationStatus.value = OptimizationStatus.Optimizing
            
            // Analyze current conditions
            val networkInfo = networkAnalyzer.getCurrentNetworkInfo()
            val deviceCapabilities = analyzeDeviceCapabilities()
            val contentInfo = analyzeContent(mediaUri)
            
            // Generate optimization strategy
            val strategy = generateOptimizationStrategy(networkInfo, deviceCapabilities, contentInfo)
            
            // Apply optimizations
            val bitrateResult = adaptiveBitrateManager.optimizeBitrate(strategy.targetBitrate, networkInfo)
            val bufferResult = bufferOptimizer.optimizeBuffer(strategy.bufferStrategy, networkInfo)
            val cacheResult = cacheManager.optimizeCache(strategy.cacheStrategy, contentInfo)
            
            // Configure player
            applyPlayerOptimizations(strategy)
            
            val result = StreamOptimizationResult(
                originalBitrate = contentInfo.bitrate,
                optimizedBitrate = strategy.targetBitrate,
                bufferConfiguration = bufferResult,
                cacheConfiguration = cacheResult,
                networkOptimizations = strategy.networkOptimizations,
                estimatedImprovement = calculateImprovementEstimate(strategy, networkInfo)
            )
            
            _optimizationStatus.value = OptimizationStatus.Active
            result
            
        } catch (e: Exception) {
            _optimizationStatus.value = OptimizationStatus.Error(e)
            throw e
        }
    }
    
    /**
     * Start real-time adaptive streaming
     */
    suspend fun startAdaptiveStreaming(mediaUri: Uri): Flow<AdaptiveStreamingEvent> = flow {
        try {
            emit(AdaptiveStreamingEvent.Started(mediaUri))
            
            while (isOptimizationActive && currentCoroutineContext().isActive) {
                // Monitor network conditions
                val networkMetrics = networkAnalyzer.getRealtimeMetrics()
                val playbackMetrics = getPlaybackMetrics()
                val bufferMetrics = getBufferMetrics()
                
                // Analyze current performance
                val performanceAnalysis = analyzeCurrentPerformance(
                    networkMetrics, playbackMetrics, bufferMetrics
                )
                
                // Make adaptive adjustments
                when {
                    performanceAnalysis.requiresBitrateAdjustment -> {
                        val newBitrate = adaptiveBitrateManager.adjustBitrate(
                            networkMetrics, performanceAnalysis
                        )
                        emit(AdaptiveStreamingEvent.BitrateChanged(newBitrate, performanceAnalysis.reason))
                    }
                    
                    performanceAnalysis.requiresBufferAdjustment -> {
                        val newBufferConfig = bufferOptimizer.adjustBuffer(
                            networkMetrics, performanceAnalysis
                        )
                        emit(AdaptiveStreamingEvent.BufferConfigChanged(newBufferConfig))
                    }
                    
                    performanceAnalysis.requiresCacheAdjustment -> {
                        val cacheAdjustment = cacheManager.adjustCache(performanceAnalysis)
                        emit(AdaptiveStreamingEvent.CacheConfigChanged(cacheAdjustment))
                    }
                }
                
                // Emit metrics
                val currentMetrics = StreamingMetrics(
                    timestamp = System.currentTimeMillis(),
                    networkMetrics = networkMetrics,
                    playbackMetrics = playbackMetrics,
                    bufferMetrics = bufferMetrics,
                    currentBitrate = adaptiveBitrateManager.getCurrentBitrate(),
                    quality = calculateQualityScore(performanceAnalysis)
                )
                
                _streamingMetrics.emit(currentMetrics)
                emit(AdaptiveStreamingEvent.MetricsUpdate(currentMetrics))
                
                delay(currentOptimizationConfig.monitoringInterval)
            }
            
        } catch (e: Exception) {
            emit(AdaptiveStreamingEvent.Error(e))
        }
    }
    
    /**
     * Preload optimization for better startup performance
     */
    suspend fun preloadOptimization(mediaUris: List<Uri>): PreloadResult = withContext(Dispatchers.IO) {
        val preloadResults = mutableMapOf<Uri, PreloadInfo>()
        
        mediaUris.forEach { uri ->
            try {
                // Analyze content without loading
                val contentInfo = analyzeContentMetadata(uri)
                
                // Determine optimal initial settings
                val networkInfo = networkAnalyzer.getCurrentNetworkInfo()
                val initialStrategy = generateOptimizationStrategy(
                    networkInfo, analyzeDeviceCapabilities(), contentInfo
                )
                
                // Preload first segments
                val preloadedSegments = cacheManager.preloadInitialSegments(
                    uri, initialStrategy.preloadAmount
                )
                
                // Prepare adaptive bitrate ladder
                val bitrateOptions = adaptiveBitrateManager.prepareBitrateOptions(
                    contentInfo, networkInfo
                )
                
                preloadResults[uri] = PreloadInfo(
                    contentInfo = contentInfo,
                    initialStrategy = initialStrategy,
                    preloadedSegments = preloadedSegments,
                    bitrateOptions = bitrateOptions,
                    estimatedStartupTime = calculateStartupTime(initialStrategy, networkInfo)
                )
                
            } catch (e: Exception) {
                preloadResults[uri] = PreloadInfo(
                    contentInfo = ContentInfo.empty(),
                    initialStrategy = OptimizationStrategy.default(),
                    preloadedSegments = 0,
                    bitrateOptions = emptyList(),
                    estimatedStartupTime = 0L,
                    error = e
                )
            }
        }
        
        PreloadResult(preloadResults)
    }
    
    /**
     * Handle network changes dynamically
     */
    suspend fun handleNetworkChange(networkType: NetworkType, capabilities: NetworkCapabilities?) {
        if (!isOptimizationActive) return
        
        try {
            val newNetworkInfo = networkAnalyzer.analyzeNetworkChange(networkType, capabilities)
            
            // Immediate adjustments for network changes
            when (newNetworkInfo.connectionQuality) {
                ConnectionQuality.POOR -> {
                    // Aggressive optimization for poor connections
                    adaptiveBitrateManager.setMinimumBitrate()
                    bufferOptimizer.enableAgressiveBuffering()
                    cacheManager.enableOfflineMode()
                }
                
                ConnectionQuality.GOOD -> {
                    // Balanced optimization
                    adaptiveBitrateManager.setBalancedBitrate(newNetworkInfo)
                    bufferOptimizer.setNormalBuffering()
                    cacheManager.setNormalMode()
                }
                
                ConnectionQuality.EXCELLENT -> {
                    // High quality optimization
                    adaptiveBitrateManager.setMaximumBitrate(newNetworkInfo)
                    bufferOptimizer.setMinimalBuffering()
                    cacheManager.enableHighQualityMode()
                }
            }
            
            _streamingMetrics.emit(
                StreamingMetrics.networkChange(newNetworkInfo, System.currentTimeMillis())
            )
            
        } catch (e: Exception) {
            _optimizationStatus.value = OptimizationStatus.Error(e)
        }
    }
    
    /**
     * Get current streaming statistics
     */
    fun getStreamingStatistics(): StreamingStatistics {
        return StreamingStatistics(
            totalPlaybackTime = getTotalPlaybackTime(),
            bufferHealthScore = bufferOptimizer.getHealthScore(),
            networkEfficiency = networkAnalyzer.getEfficiencyScore(),
            cacheHitRatio = cacheManager.getCacheHitRatio(),
            averageBitrate = adaptiveBitrateManager.getAverageBitrate(),
            bitrateStability = adaptiveBitrateManager.getStabilityScore(),
            stallEvents = getStallEventCount(),
            qualityChanges = getBitrateChangeCount(),
            dataUsage = getTotalDataUsage(),
            energyEfficiency = calculateEnergyEfficiency()
        )
    }
    
    // Private implementation methods
    private fun startOptimizationMonitoring() {
        optimizationScope.launch {
            while (isOptimizationActive) {
                try {
                    // Monitor system resources
                    val resourceUsage = monitorSystemResources()
                    
                    // Adjust optimization based on resource availability
                    if (resourceUsage.cpuUsage > 80f) {
                        // Reduce optimization intensity
                        adaptiveBitrateManager.setLowResourceMode()
                        bufferOptimizer.setLowResourceMode()
                    } else if (resourceUsage.cpuUsage < 40f) {
                        // Enable advanced optimizations
                        adaptiveBitrateManager.setHighPerformanceMode()
                        bufferOptimizer.setHighPerformanceMode()
                    }
                    
                    // Monitor thermal state
                    val thermalState = getThermalState()
                    if (thermalState == ThermalState.THROTTLING) {
                        adaptiveBitrateManager.enableThermalProtection()
                        bufferOptimizer.enableThermalProtection()
                    }
                    
                    delay(5000) // Monitor every 5 seconds
                } catch (e: Exception) {
                    // Log error but continue monitoring
                }
            }
        }
    }
    
    private fun analyzeDeviceCapabilities(): DeviceCapabilities {
        return DeviceCapabilities(
            maxSupportedResolution = getMaxSupportedResolution(),
            hardwareDecoding = getHardwareDecodingCapabilities(),
            memoryCapacity = getAvailableMemory(),
            processingPower = getCpuCapabilities(),
            displayCapabilities = getDisplayCapabilities(),
            batteryOptimization = isBatteryOptimizationEnabled()
        )
    }
    
    private suspend fun analyzeContent(uri: Uri): ContentInfo = withContext(Dispatchers.IO) {
        try {
            // This would typically use MediaMetadataRetriever or similar
            ContentInfo(
                uri = uri,
                duration = estimateContentDuration(uri),
                bitrate = estimateContentBitrate(uri),
                resolution = estimateContentResolution(uri),
                format = getContentFormat(uri),
                hasMultipleBitrates = checkMultipleBitrates(uri),
                estimatedSize = estimateContentSize(uri)
            )
        } catch (e: Exception) {
            ContentInfo.empty()
        }
    }
    
    private fun analyzeContentMetadata(uri: Uri): ContentInfo {
        // Quick metadata analysis without full content loading
        return ContentInfo.fromUri(uri)
    }
    
    private fun generateOptimizationStrategy(
        networkInfo: NetworkInfo,
        deviceCapabilities: DeviceCapabilities,
        contentInfo: ContentInfo
    ): OptimizationStrategy {
        
        val targetBitrate = calculateOptimalBitrate(networkInfo, deviceCapabilities, contentInfo)
        val bufferStrategy = calculateOptimalBufferStrategy(networkInfo, deviceCapabilities)
        val cacheStrategy = calculateOptimalCacheStrategy(networkInfo, contentInfo)
        val networkOptimizations = calculateNetworkOptimizations(networkInfo)
        val preloadAmount = calculateOptimalPreloadAmount(networkInfo, contentInfo)
        
        return OptimizationStrategy(
            targetBitrate = targetBitrate,
            bufferStrategy = bufferStrategy,
            cacheStrategy = cacheStrategy,
            networkOptimizations = networkOptimizations,
            preloadAmount = preloadAmount
        )
    }
    
    private fun calculateOptimalBitrate(
        networkInfo: NetworkInfo,
        deviceCapabilities: DeviceCapabilities,
        contentInfo: ContentInfo
    ): Int {
        val networkCapacity = networkInfo.bandwidth * 0.8f // 80% utilization
        val deviceLimit = deviceCapabilities.maxSupportedBitrate
        val contentMax = contentInfo.bitrate
        
        return minOf(networkCapacity.toInt(), deviceLimit, contentMax)
    }
    
    private fun calculateOptimalBufferStrategy(
        networkInfo: NetworkInfo,
        deviceCapabilities: DeviceCapabilities
    ): BufferStrategy {
        return when (networkInfo.connectionQuality) {
            ConnectionQuality.POOR -> BufferStrategy.AGGRESSIVE
            ConnectionQuality.FAIR -> BufferStrategy.BALANCED
            ConnectionQuality.GOOD -> BufferStrategy.NORMAL
            ConnectionQuality.EXCELLENT -> BufferStrategy.MINIMAL
        }
    }
    
    private fun calculateOptimalCacheStrategy(
        networkInfo: NetworkInfo,
        contentInfo: ContentInfo
    ): CacheStrategy {
        val availableStorage = getAvailableStorageSpace()
        val contentSize = contentInfo.estimatedSize
        
        return when {
            networkInfo.connectionQuality == ConnectionQuality.POOR -> CacheStrategy.AGGRESSIVE
            availableStorage > contentSize * 2 -> CacheStrategy.NORMAL
            availableStorage > contentSize -> CacheStrategy.CONSERVATIVE
            else -> CacheStrategy.MINIMAL
        }
    }
    
    private fun applyPlayerOptimizations(strategy: OptimizationStrategy) {
        // Apply optimization strategy to ExoPlayer
        val loadControl = bufferOptimizer.createOptimizedLoadControl(strategy.bufferStrategy)
        
        // This would require player reconfiguration in real implementation
        // player.setLoadControl(loadControl)
    }
    
    private fun analyzeCurrentPerformance(
        networkMetrics: NetworkMetrics,
        playbackMetrics: PlaybackMetrics,
        bufferMetrics: BufferMetrics
    ): PerformanceAnalysis {
        
        val stallDetected = playbackMetrics.stallCount > 0
        val bufferUnderrun = bufferMetrics.currentBufferMs < bufferMetrics.targetBufferMs * 0.3f
        val networkDegraded = networkMetrics.bandwidth < networkMetrics.averageBandwidth * 0.7f
        val excessiveBuffer = bufferMetrics.currentBufferMs > bufferMetrics.targetBufferMs * 2.0f
        
        return PerformanceAnalysis(
            requiresBitrateAdjustment = stallDetected || bufferUnderrun || networkDegraded,
            requiresBufferAdjustment = bufferUnderrun || excessiveBuffer,
            requiresCacheAdjustment = networkDegraded,
            reason = when {
                stallDetected -> "Stall detected"
                bufferUnderrun -> "Buffer underrun"
                networkDegraded -> "Network degraded"
                excessiveBuffer -> "Excessive buffering"
                else -> "No adjustment needed"
            }
        )
    }
    
    // Utility methods with placeholder implementations
    private fun getPlaybackMetrics(): PlaybackMetrics = PlaybackMetrics()
    private fun getBufferMetrics(): BufferMetrics = BufferMetrics()
    private fun calculateQualityScore(analysis: PerformanceAnalysis): Float = 0.8f
    private fun getTotalPlaybackTime(): Long = 0L
    private fun getStallEventCount(): Int = 0
    private fun getBitrateChangeCount(): Int = 0
    private fun getTotalDataUsage(): Long = 0L
    private fun calculateEnergyEfficiency(): Float = 0.8f
    private fun monitorSystemResources(): ResourceUsage = ResourceUsage()
    private fun getThermalState(): ThermalState = ThermalState.NORMAL
    private fun getMaxSupportedResolution(): String = "1080p"
    private fun getHardwareDecodingCapabilities(): List<String> = emptyList()
    private fun getAvailableMemory(): Long = 0L
    private fun getCpuCapabilities(): CpuInfo = CpuInfo()
    private fun getDisplayCapabilities(): DisplayInfo = DisplayInfo()
    private fun isBatteryOptimizationEnabled(): Boolean = false
    private fun estimateContentDuration(uri: Uri): Long = 0L
    private fun estimateContentBitrate(uri: Uri): Int = 1000000
    private fun estimateContentResolution(uri: Uri): String = "720p"
    private fun getContentFormat(uri: Uri): String = "mp4"
    private fun checkMultipleBitrates(uri: Uri): Boolean = false
    private fun estimateContentSize(uri: Uri): Long = 0L
    private fun calculateStartupTime(strategy: OptimizationStrategy, networkInfo: NetworkInfo): Long = 1000L
    private fun calculateImprovementEstimate(strategy: OptimizationStrategy, networkInfo: NetworkInfo): Float = 0.2f
    private fun getAvailableStorageSpace(): Long = 0L
    private fun calculateOptimalPreloadAmount(networkInfo: NetworkInfo, contentInfo: ContentInfo): Long = 5000L
    private fun calculateNetworkOptimizations(networkInfo: NetworkInfo): List<String> = emptyList()
    
    fun cleanup() {
        isOptimizationActive = false
        optimizationScope.cancel()
        adaptiveBitrateManager.cleanup()
        bufferOptimizer.cleanup()
        networkAnalyzer.cleanup()
        cacheManager.cleanup()
    }
}

// Data classes and enums for streaming optimization
sealed class OptimizationStatus {
    object Idle : OptimizationStatus()
    object Initializing : OptimizationStatus()
    object Optimizing : OptimizationStatus()
    object Active : OptimizationStatus()
    data class Error(val exception: Exception) : OptimizationStatus()
}

enum class ConnectionQuality { POOR, FAIR, GOOD, EXCELLENT }
enum class NetworkType { WIFI, CELLULAR, ETHERNET, UNKNOWN }
enum class BufferStrategy { MINIMAL, NORMAL, BALANCED, AGGRESSIVE }
enum class CacheStrategy { MINIMAL, CONSERVATIVE, NORMAL, AGGRESSIVE }
enum class ThermalState { NORMAL, LIGHT, MODERATE, SEVERE, THROTTLING }

data class StreamOptimizationConfig(
    val bitrateConfig: BitrateConfig = BitrateConfig(),
    val bufferConfig: BufferConfig = BufferConfig(),
    val cacheConfig: CacheConfig = CacheConfig(),
    val monitoringInterval: Long = 2000L,
    val enableAdaptiveBitrate: Boolean = true,
    val enableSmartCaching: Boolean = true,
    val enableNetworkOptimization: Boolean = true
)

data class BitrateConfig(
    val minBitrate: Int = 200000,
    val maxBitrate: Int = 8000000,
    val initialBitrate: Int = 1000000,
    val adaptationSensitivity: Float = 1.0f
)

data class BufferConfig(
    val minBufferMs: Long = 2000L,
    val maxBufferMs: Long = 30000L,
    val bufferForPlayback: Long = 5000L,
    val rebufferTimeout: Long = 10000L
)

data class CacheConfig(
    val maxCacheSize: Long = 500 * 1024 * 1024, // 500MB
    val preloadDuration: Long = 30000L, // 30 seconds
    val enablePersistentCache: Boolean = true
)

// Additional data classes for comprehensive streaming optimization
data class NetworkInfo(
    val type: NetworkType,
    val bandwidth: Float,
    val latency: Long,
    val connectionQuality: ConnectionQuality,
    val isMetered: Boolean,
    val averageBandwidth: Float
)

data class DeviceCapabilities(
    val maxSupportedResolution: String,
    val hardwareDecoding: List<String>,
    val memoryCapacity: Long,
    val processingPower: CpuInfo,
    val displayCapabilities: DisplayInfo,
    val batteryOptimization: Boolean,
    val maxSupportedBitrate: Int = 10000000
)

data class ContentInfo(
    val uri: Uri,
    val duration: Long,
    val bitrate: Int,
    val resolution: String,
    val format: String,
    val hasMultipleBitrates: Boolean,
    val estimatedSize: Long
) {
    companion object {
        fun empty() = ContentInfo(Uri.EMPTY, 0L, 0, "", "", false, 0L)
        fun fromUri(uri: Uri) = ContentInfo(uri, 0L, 1000000, "720p", "mp4", false, 0L)
    }
}

data class OptimizationStrategy(
    val targetBitrate: Int,
    val bufferStrategy: BufferStrategy,
    val cacheStrategy: CacheStrategy,
    val networkOptimizations: List<String>,
    val preloadAmount: Long
) {
    companion object {
        fun default() = OptimizationStrategy(1000000, BufferStrategy.NORMAL, CacheStrategy.NORMAL, emptyList(), 5000L)
    }
}

data class StreamOptimizationResult(
    val originalBitrate: Int,
    val optimizedBitrate: Int,
    val bufferConfiguration: String,
    val cacheConfiguration: String,
    val networkOptimizations: List<String>,
    val estimatedImprovement: Float
)

data class StreamingMetrics(
    val timestamp: Long,
    val networkMetrics: NetworkMetrics,
    val playbackMetrics: PlaybackMetrics,
    val bufferMetrics: BufferMetrics,
    val currentBitrate: Int,
    val quality: Float
) {
    companion object {
        fun networkChange(networkInfo: NetworkInfo, timestamp: Long) = StreamingMetrics(
            timestamp, NetworkMetrics(), PlaybackMetrics(), BufferMetrics(), 1000000, 0.8f
        )
    }
}

// Supporting data classes
data class NetworkMetrics(
    val bandwidth: Float = 1000000f,
    val latency: Long = 50L,
    val packetLoss: Float = 0.01f,
    val jitter: Long = 10L,
    val averageBandwidth: Float = 1000000f
)

data class PlaybackMetrics(
    val stallCount: Int = 0,
    val rebufferTime: Long = 0L,
    val startupTime: Long = 1000L,
    val seekLatency: Long = 500L
)

data class BufferMetrics(
    val currentBufferMs: Long = 5000L,
    val targetBufferMs: Long = 10000L,
    val bufferHealthScore: Float = 0.8f
)

data class PerformanceAnalysis(
    val requiresBitrateAdjustment: Boolean,
    val requiresBufferAdjustment: Boolean,
    val requiresCacheAdjustment: Boolean,
    val reason: String
)

data class PreloadResult(
    val results: Map<Uri, PreloadInfo>
)

data class PreloadInfo(
    val contentInfo: ContentInfo,
    val initialStrategy: OptimizationStrategy,
    val preloadedSegments: Int,
    val bitrateOptions: List<Int>,
    val estimatedStartupTime: Long,
    val error: Exception? = null
)

data class StreamingStatistics(
    val totalPlaybackTime: Long,
    val bufferHealthScore: Float,
    val networkEfficiency: Float,
    val cacheHitRatio: Float,
    val averageBitrate: Int,
    val bitrateStability: Float,
    val stallEvents: Int,
    val qualityChanges: Int,
    val dataUsage: Long,
    val energyEfficiency: Float
)

data class ResourceUsage(
    val cpuUsage: Float = 25f,
    val memoryUsage: Long = 100 * 1024 * 1024,
    val batteryLevel: Float = 80f
)

data class CpuInfo(
    val cores: Int = 8,
    val maxFrequency: Long = 2400000,
    val architecture: String = "arm64"
)

data class DisplayInfo(
    val width: Int = 1080,
    val height: Int = 2400,
    val density: Float = 3.0f,
    val refreshRate: Float = 60f
)

sealed class AdaptiveStreamingEvent {
    data class Started(val uri: Uri) : AdaptiveStreamingEvent()
    data class BitrateChanged(val newBitrate: Int, val reason: String) : AdaptiveStreamingEvent()
    data class BufferConfigChanged(val newConfig: String) : AdaptiveStreamingEvent()
    data class CacheConfigChanged(val adjustment: String) : AdaptiveStreamingEvent()
    data class MetricsUpdate(val metrics: StreamingMetrics) : AdaptiveStreamingEvent()
    data class Error(val exception: Exception) : AdaptiveStreamingEvent()
}