package com.astralplayer.nextplayer.streaming

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * Advanced buffer optimization for smooth streaming experience
 */
class BufferOptimizer {
    
    private val _bufferEvents = MutableSharedFlow<BufferEvent>()
    val bufferEvents: SharedFlow<BufferEvent> = _bufferEvents.asSharedFlow()
    
    private var currentBufferConfig = BufferConfiguration()
    private var bufferHistory = mutableListOf<BufferSnapshot>()
    private var performanceMetrics = BufferPerformanceMetrics()
    
    private var config = BufferConfig()
    private var isInitialized = false
    private var bufferStrategy = BufferStrategy.NORMAL
    
    private val bufferScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Initialize buffer optimization
     */
    suspend fun initialize(bufferConfig: BufferConfig) {
        config = bufferConfig
        currentBufferConfig = createInitialBufferConfiguration(bufferConfig)
        isInitialized = true
        
        startBufferMonitoring()
        _bufferEvents.emit(BufferEvent.Initialized(currentBufferConfig))
    }
    
    /**
     * Optimize buffer configuration
     */
    suspend fun optimizeBuffer(strategy: BufferStrategy, networkInfo: NetworkInfo): String {
        if (!isInitialized) return "Buffer optimizer not initialized"
        
        try {
            bufferStrategy = strategy
            val newConfig = calculateOptimalBufferConfig(strategy, networkInfo)
            
            if (shouldUpdateBufferConfig(newConfig)) {
                val previousConfig = currentBufferConfig
                currentBufferConfig = newConfig
                
                recordBufferConfigChange(previousConfig, newConfig, "Strategy optimization")
                _bufferEvents.emit(BufferEvent.ConfigurationChanged(previousConfig, newConfig))
                
                return "Buffer optimized for ${strategy.name.lowercase()} strategy"
            }
            
            return "Buffer configuration maintained"
            
        } catch (e: Exception) {
            _bufferEvents.emit(BufferEvent.Error(e))
            return "Buffer optimization failed: ${e.message}"
        }
    }
    
    /**
     * Adjust buffer based on real-time conditions
     */
    suspend fun adjustBuffer(
        networkMetrics: NetworkMetrics,
        performanceAnalysis: PerformanceAnalysis
    ): String {
        if (!isInitialized) return "Not initialized"
        
        val adjustment = when {
            performanceAnalysis.reason.contains("underrun") -> {
                adjustForBufferUnderrun(networkMetrics)
            }
            performanceAnalysis.reason.contains("excessive") -> {
                adjustForExcessiveBuffering(networkMetrics)
            }
            performanceAnalysis.reason.contains("stall") -> {
                adjustForStallRecovery(networkMetrics)
            }
            performanceAnalysis.reason.contains("network degraded") -> {
                adjustForNetworkDegradation(networkMetrics)
            }
            else -> {
                adjustForOptimalPerformance(networkMetrics)
            }
        }
        
        if (adjustment.shouldUpdate) {
            val previousConfig = currentBufferConfig
            currentBufferConfig = adjustment.newConfig
            
            recordBufferConfigChange(previousConfig, adjustment.newConfig, adjustment.reason)
            _bufferEvents.emit(BufferEvent.ConfigurationChanged(previousConfig, adjustment.newConfig))
        }
        
        return adjustment.description
    }
    
    /**
     * Set specific buffer strategies
     */
    suspend fun enableAgressiveBuffering() {
        bufferStrategy = BufferStrategy.AGGRESSIVE
        val aggressiveConfig = createAggressiveBufferConfig()
        updateBufferConfiguration(aggressiveConfig, "Aggressive buffering enabled")
    }
    
    suspend fun setNormalBuffering() {
        bufferStrategy = BufferStrategy.NORMAL
        val normalConfig = createNormalBufferConfig()
        updateBufferConfiguration(normalConfig, "Normal buffering restored")
    }
    
    suspend fun setMinimalBuffering() {
        bufferStrategy = BufferStrategy.MINIMAL
        val minimalConfig = createMinimalBufferConfig()
        updateBufferConfiguration(minimalConfig, "Minimal buffering enabled")
    }
    
    /**
     * Performance mode adjustments
     */
    suspend fun setLowResourceMode() {
        val lowResourceConfig = createLowResourceBufferConfig()
        updateBufferConfiguration(lowResourceConfig, "Low resource mode enabled")
    }
    
    suspend fun setHighPerformanceMode() {
        val highPerformanceConfig = createHighPerformanceBufferConfig()
        updateBufferConfiguration(highPerformanceConfig, "High performance mode enabled")
    }
    
    suspend fun enableThermalProtection() {
        val thermalConfig = createThermalProtectionBufferConfig()
        updateBufferConfiguration(thermalConfig, "Thermal protection enabled")
    }
    
    /**
     * Create optimized load control for ExoPlayer
     */
    fun createOptimizedLoadControl(strategy: BufferStrategy): String {
        // In real implementation, this would return ExoPlayer LoadControl
        val config = when (strategy) {
            BufferStrategy.MINIMAL -> createMinimalLoadControlConfig()
            BufferStrategy.NORMAL -> createNormalLoadControlConfig()
            BufferStrategy.BALANCED -> createBalancedLoadControlConfig()
            BufferStrategy.AGGRESSIVE -> createAggressiveLoadControlConfig()
        }
        
        return "LoadControl configured for ${strategy.name.lowercase()} strategy: $config"
    }
    
    /**
     * Get buffer health metrics
     */
    fun getHealthScore(): Float {
        if (bufferHistory.isEmpty()) return 1.0f
        
        val recentSnapshots = bufferHistory.takeLast(20)
        val underrunEvents = recentSnapshots.count { it.bufferLevel < it.targetLevel * 0.3f }
        val overrunEvents = recentSnapshots.count { it.bufferLevel > it.targetLevel * 2.0f }
        val stallEvents = recentSnapshots.count { it.isStalled }
        
        val healthScore = 1.0f - ((underrunEvents + overrunEvents + stallEvents * 2).toFloat() / recentSnapshots.size)
        return maxOf(0.0f, healthScore)
    }
    
    fun getBufferStatistics(): BufferStatistics {
        val recent = bufferHistory.takeLast(50)
        
        return BufferStatistics(
            currentConfiguration = currentBufferConfig,
            averageBufferLevel = if (recent.isNotEmpty()) recent.map { it.bufferLevel }.average().toLong() else 0L,
            bufferStability = calculateBufferStability(recent),
            underrunEvents = recent.count { it.bufferLevel < it.targetLevel * 0.3f },
            overrunEvents = recent.count { it.bufferLevel > it.targetLevel * 2.0f },
            stallEvents = recent.count { it.isStalled },
            averageRebufferTime = calculateAverageRebufferTime(recent),
            healthScore = getHealthScore(),
            strategy = bufferStrategy
        )
    }
    
    // Private implementation methods
    private fun startBufferMonitoring() {
        bufferScope.launch {
            while (isInitialized && currentCoroutineContext().isActive) {
                try {
                    // Record current buffer state
                    val snapshot = captureBufferSnapshot()
                    recordBufferSnapshot(snapshot)
                    
                    // Analyze buffer performance
                    val performance = analyzeBufferPerformance()
                    updatePerformanceMetrics(performance)
                    
                    // Emit buffer health update
                    _bufferEvents.emit(BufferEvent.HealthUpdate(getHealthScore(), performance))
                    
                    // Auto-adjust if needed
                    if (performance.requiresAdjustment) {
                        performAutoAdjustment(performance)
                    }
                    
                    delay(2000) // Monitor every 2 seconds
                } catch (e: Exception) {
                    // Log error but continue monitoring
                }
            }
        }
    }
    
    private fun createInitialBufferConfiguration(config: BufferConfig): BufferConfiguration {
        return BufferConfiguration(
            minBufferMs = config.minBufferMs,
            maxBufferMs = config.maxBufferMs,
            bufferForPlayback = config.bufferForPlayback,
            bufferForPlaybackAfterRebuffer = config.bufferForPlayback * 1.5f, // 50% more after rebuffer
            targetBufferBytes = calculateTargetBufferBytes(config),
            prioritizeTimeOverSizeThresholds = true,
            backBufferDurationMs = config.minBufferMs / 2,
            rebufferTimeoutMs = config.rebufferTimeout
        )
    }
    
    private fun calculateOptimalBufferConfig(
        strategy: BufferStrategy,
        networkInfo: NetworkInfo
    ): BufferConfiguration {
        val baseConfig = currentBufferConfig
        
        return when (strategy) {
            BufferStrategy.MINIMAL -> BufferConfiguration(
                minBufferMs = config.minBufferMs,
                maxBufferMs = config.minBufferMs * 2,
                bufferForPlayback = config.minBufferMs,
                bufferForPlaybackAfterRebuffer = config.minBufferMs * 1.2f,
                targetBufferBytes = calculateTargetBufferBytes(config) / 2,
                prioritizeTimeOverSizeThresholds = true,
                backBufferDurationMs = 0L,
                rebufferTimeoutMs = config.rebufferTimeout / 2
            )
            
            BufferStrategy.NORMAL -> baseConfig
            
            BufferStrategy.BALANCED -> BufferConfiguration(
                minBufferMs = config.minBufferMs,
                maxBufferMs = (config.maxBufferMs * 1.2f).toLong(),
                bufferForPlayback = config.bufferForPlayback,
                bufferForPlaybackAfterRebuffer = config.bufferForPlayback * 1.3f,
                targetBufferBytes = (calculateTargetBufferBytes(config) * 1.2f).toLong(),
                prioritizeTimeOverSizeThresholds = true,
                backBufferDurationMs = config.minBufferMs,
                rebufferTimeoutMs = config.rebufferTimeout
            )
            
            BufferStrategy.AGGRESSIVE -> BufferConfiguration(
                minBufferMs = config.minBufferMs * 2,
                maxBufferMs = config.maxBufferMs * 2,
                bufferForPlayback = config.bufferForPlayback * 1.5f,
                bufferForPlaybackAfterRebuffer = config.bufferForPlayback * 2.0f,
                targetBufferBytes = calculateTargetBufferBytes(config) * 2,
                prioritizeTimeOverSizeThresholds = false,
                backBufferDurationMs = config.minBufferMs,
                rebufferTimeoutMs = config.rebufferTimeout * 2
            )
        }.adjustForNetwork(networkInfo)
    }
    
    private fun BufferConfiguration.adjustForNetwork(networkInfo: NetworkInfo): BufferConfiguration {
        val multiplier = when (networkInfo.connectionQuality) {
            ConnectionQuality.POOR -> 1.5f
            ConnectionQuality.FAIR -> 1.2f
            ConnectionQuality.GOOD -> 1.0f
            ConnectionQuality.EXCELLENT -> 0.8f
        }
        
        return copy(
            minBufferMs = (minBufferMs * multiplier).toLong(),
            maxBufferMs = (maxBufferMs * multiplier).toLong(),
            bufferForPlayback = bufferForPlayback * multiplier,
            targetBufferBytes = (targetBufferBytes * multiplier).toLong()
        )
    }
    
    private fun shouldUpdateBufferConfig(newConfig: BufferConfiguration): Boolean {
        val significantChange = abs(newConfig.minBufferMs - currentBufferConfig.minBufferMs) > 1000L ||
                               abs(newConfig.maxBufferMs - currentBufferConfig.maxBufferMs) > 2000L ||
                               abs(newConfig.bufferForPlayback - currentBufferConfig.bufferForPlayback) > 500f
        
        return significantChange
    }
    
    private fun adjustForBufferUnderrun(networkMetrics: NetworkMetrics): BufferAdjustment {
        val newConfig = currentBufferConfig.copy(
            minBufferMs = (currentBufferConfig.minBufferMs * 1.5f).toLong(),
            bufferForPlayback = currentBufferConfig.bufferForPlayback * 1.3f,
            bufferForPlaybackAfterRebuffer = currentBufferConfig.bufferForPlaybackAfterRebuffer * 1.5f
        )
        
        return BufferAdjustment(
            newConfig = newConfig,
            shouldUpdate = true,
            reason = "Buffer underrun adjustment",
            description = "Increased buffer thresholds to prevent underruns"
        )
    }
    
    private fun adjustForExcessiveBuffering(networkMetrics: NetworkMetrics): BufferAdjustment {
        val newConfig = currentBufferConfig.copy(
            maxBufferMs = (currentBufferConfig.maxBufferMs * 0.8f).toLong(),
            targetBufferBytes = (currentBufferConfig.targetBufferBytes * 0.8f).toLong()
        )
        
        return BufferAdjustment(
            newConfig = newConfig,
            shouldUpdate = true,
            reason = "Excessive buffering adjustment",
            description = "Reduced buffer limits to optimize memory usage"
        )
    }
    
    private fun adjustForStallRecovery(networkMetrics: NetworkMetrics): BufferAdjustment {
        val newConfig = currentBufferConfig.copy(
            bufferForPlaybackAfterRebuffer = currentBufferConfig.bufferForPlaybackAfterRebuffer * 1.8f,
            rebufferTimeoutMs = (currentBufferConfig.rebufferTimeoutMs * 1.5f).toLong()
        )
        
        return BufferAdjustment(
            newConfig = newConfig,
            shouldUpdate = true,
            reason = "Stall recovery adjustment",
            description = "Enhanced post-rebuffer configuration for stability"
        )
    }
    
    private fun adjustForNetworkDegradation(networkMetrics: NetworkMetrics): BufferAdjustment {
        val degradationSeverity = 1.0f - (networkMetrics.bandwidth / networkMetrics.averageBandwidth)
        val adjustmentFactor = 1.0f + degradationSeverity
        
        val newConfig = currentBufferConfig.copy(
            minBufferMs = (currentBufferConfig.minBufferMs * adjustmentFactor).toLong(),
            bufferForPlayback = currentBufferConfig.bufferForPlayback * adjustmentFactor,
            targetBufferBytes = (currentBufferConfig.targetBufferBytes * adjustmentFactor).toLong()
        )
        
        return BufferAdjustment(
            newConfig = newConfig,
            shouldUpdate = true,
            reason = "Network degradation adjustment",
            description = "Increased buffering for network instability (${(degradationSeverity * 100).toInt()}% degradation)"
        )
    }
    
    private fun adjustForOptimalPerformance(networkMetrics: NetworkMetrics): BufferAdjustment {
        // Fine-tune based on recent performance
        val recentPerformance = analyzeRecentBufferPerformance()
        
        if (recentPerformance.isOptimal) {
            return BufferAdjustment(
                newConfig = currentBufferConfig,
                shouldUpdate = false,
                reason = "Optimal performance maintained",
                description = "Buffer configuration is performing optimally"
            )
        }
        
        val newConfig = currentBufferConfig.copy(
            bufferForPlayback = currentBufferConfig.bufferForPlayback * recentPerformance.recommendedAdjustment
        )
        
        return BufferAdjustment(
            newConfig = newConfig,
            shouldUpdate = true,
            reason = "Performance optimization",
            description = "Fine-tuned buffer for optimal performance"
        )
    }
    
    private fun updateBufferConfiguration(newConfig: BufferConfiguration, reason: String) {
        val previousConfig = currentBufferConfig
        currentBufferConfig = newConfig
        
        bufferScope.launch {
            recordBufferConfigChange(previousConfig, newConfig, reason)
            _bufferEvents.emit(BufferEvent.ConfigurationChanged(previousConfig, newConfig))
        }
    }
    
    // Buffer strategy configurations
    private fun createAggressiveBufferConfig() = BufferConfiguration(
        minBufferMs = config.minBufferMs * 3,
        maxBufferMs = config.maxBufferMs * 2,
        bufferForPlayback = config.bufferForPlayback * 2.0f,
        bufferForPlaybackAfterRebuffer = config.bufferForPlayback * 3.0f,
        targetBufferBytes = calculateTargetBufferBytes(config) * 3,
        prioritizeTimeOverSizeThresholds = false,
        backBufferDurationMs = config.minBufferMs * 2,
        rebufferTimeoutMs = config.rebufferTimeout * 2
    )
    
    private fun createNormalBufferConfig() = createInitialBufferConfiguration(config)
    
    private fun createMinimalBufferConfig() = BufferConfiguration(
        minBufferMs = config.minBufferMs / 2,
        maxBufferMs = config.minBufferMs,
        bufferForPlayback = config.bufferForPlayback * 0.5f,
        bufferForPlaybackAfterRebuffer = config.bufferForPlayback * 0.8f,
        targetBufferBytes = calculateTargetBufferBytes(config) / 3,
        prioritizeTimeOverSizeThresholds = true,
        backBufferDurationMs = 0L,
        rebufferTimeoutMs = config.rebufferTimeout / 2
    )
    
    private fun createLowResourceBufferConfig() = BufferConfiguration(
        minBufferMs = config.minBufferMs,
        maxBufferMs = config.minBufferMs * 2,
        bufferForPlayback = config.bufferForPlayback * 0.8f,
        bufferForPlaybackAfterRebuffer = config.bufferForPlayback * 1.0f,
        targetBufferBytes = calculateTargetBufferBytes(config) / 2,
        prioritizeTimeOverSizeThresholds = true,
        backBufferDurationMs = 0L,
        rebufferTimeoutMs = config.rebufferTimeout
    )
    
    private fun createHighPerformanceBufferConfig() = BufferConfiguration(
        minBufferMs = config.minBufferMs * 2,
        maxBufferMs = config.maxBufferMs * 1.5f,
        bufferForPlayback = config.bufferForPlayback * 1.5f,
        bufferForPlaybackAfterRebuffer = config.bufferForPlayback * 2.0f,
        targetBufferBytes = calculateTargetBufferBytes(config) * 2,
        prioritizeTimeOverSizeThresholds = false,
        backBufferDurationMs = config.minBufferMs,
        rebufferTimeoutMs = config.rebufferTimeout * 1.5f
    )
    
    private fun createThermalProtectionBufferConfig() = BufferConfiguration(
        minBufferMs = config.minBufferMs,
        maxBufferMs = config.minBufferMs * 1.5f,
        bufferForPlayback = config.bufferForPlayback * 0.7f,
        bufferForPlaybackAfterRebuffer = config.bufferForPlayback * 1.0f,
        targetBufferBytes = calculateTargetBufferBytes(config) / 2,
        prioritizeTimeOverSizeThresholds = true,
        backBufferDurationMs = 0L,
        rebufferTimeoutMs = config.rebufferTimeout / 2
    )
    
    // Load control configurations
    private fun createMinimalLoadControlConfig() = "MinimalLoadControl(buffer=${config.minBufferMs}ms, playback=${config.bufferForPlayback}ms)"
    private fun createNormalLoadControlConfig() = "NormalLoadControl(buffer=${config.minBufferMs}-${config.maxBufferMs}ms, playback=${config.bufferForPlayback}ms)"
    private fun createBalancedLoadControlConfig() = "BalancedLoadControl(buffer=${config.minBufferMs}-${config.maxBufferMs * 1.2f}ms, playback=${config.bufferForPlayback * 1.2f}ms)"
    private fun createAggressiveLoadControlConfig() = "AggressiveLoadControl(buffer=${config.minBufferMs * 2}-${config.maxBufferMs * 2}ms, playback=${config.bufferForPlayback * 2}ms)"
    
    // Utility methods
    private fun calculateTargetBufferBytes(config: BufferConfig): Long {
        // Estimate based on typical bitrate and buffer duration
        val estimatedBitrate = 2000000 // 2 Mbps
        val bytesPerSecond = estimatedBitrate / 8
        return (bytesPerSecond * (config.maxBufferMs / 1000.0)).toLong()
    }
    
    private fun captureBufferSnapshot(): BufferSnapshot {
        // In real implementation, this would get actual buffer metrics from player
        return BufferSnapshot(
            timestamp = System.currentTimeMillis(),
            bufferLevel = (currentBufferConfig.minBufferMs..currentBufferConfig.maxBufferMs).random(),
            targetLevel = currentBufferConfig.bufferForPlayback.toLong(),
            isStalled = false,
            rebufferCount = 0,
            configuration = currentBufferConfig
        )
    }
    
    private fun recordBufferSnapshot(snapshot: BufferSnapshot) {
        bufferHistory.add(snapshot)
        
        // Keep last 500 snapshots
        if (bufferHistory.size > 500) {
            bufferHistory.removeAt(0)
        }
    }
    
    private fun recordBufferConfigChange(
        previous: BufferConfiguration,
        new: BufferConfiguration,
        reason: String
    ) {
        // Record configuration change for analysis
    }
    
    private fun analyzeBufferPerformance(): BufferPerformanceAnalysis {
        val recent = bufferHistory.takeLast(30)
        if (recent.isEmpty()) return BufferPerformanceAnalysis.default()
        
        val underruns = recent.count { it.bufferLevel < it.targetLevel * 0.3f }
        val overruns = recent.count { it.bufferLevel > it.targetLevel * 2.0f }
        val stalls = recent.count { it.isStalled }
        
        return BufferPerformanceAnalysis(
            underrunRate = underruns.toFloat() / recent.size,
            overrunRate = overruns.toFloat() / recent.size,
            stallRate = stalls.toFloat() / recent.size,
            averageBufferLevel = recent.map { it.bufferLevel }.average().toLong(),
            bufferStability = calculateBufferStability(recent),
            requiresAdjustment = underruns > 2 || overruns > 5 || stalls > 0
        )
    }
    
    private fun analyzeRecentBufferPerformance(): RecentBufferPerformance {
        val recent = bufferHistory.takeLast(10)
        if (recent.isEmpty()) return RecentBufferPerformance.default()
        
        val stability = calculateBufferStability(recent)
        val efficiency = calculateBufferEfficiency(recent)
        
        return RecentBufferPerformance(
            isOptimal = stability > 0.8f && efficiency > 0.7f,
            recommendedAdjustment = when {
                stability < 0.6f -> 1.2f
                stability > 0.9f && efficiency > 0.8f -> 0.95f
                else -> 1.0f
            }
        )
    }
    
    private fun calculateBufferStability(snapshots: List<BufferSnapshot>): Float {
        if (snapshots.size < 2) return 1.0f
        
        val variations = snapshots.zipWithNext().map { (prev, curr) ->
            abs(curr.bufferLevel - prev.bufferLevel).toFloat() / maxOf(prev.bufferLevel, curr.bufferLevel)
        }
        
        val averageVariation = variations.average().toFloat()
        return 1.0f - minOf(1.0f, averageVariation)
    }
    
    private fun calculateBufferEfficiency(snapshots: List<BufferSnapshot>): Float {
        if (snapshots.isEmpty()) return 1.0f
        
        val optimalRangeHits = snapshots.count { snapshot ->
            val targetRange = snapshot.targetLevel * 0.8f..snapshot.targetLevel * 1.5f
            snapshot.bufferLevel in targetRange
        }
        
        return optimalRangeHits.toFloat() / snapshots.size
    }
    
    private fun calculateAverageRebufferTime(snapshots: List<BufferSnapshot>): Long {
        val rebufferEvents = snapshots.filter { it.isStalled }
        return if (rebufferEvents.isNotEmpty()) {
            rebufferEvents.map { it.rebufferCount * 1000L }.average().toLong()
        } else 0L
    }
    
    private fun updatePerformanceMetrics(performance: BufferPerformanceAnalysis) {
        performanceMetrics = performanceMetrics.copy(
            totalUnderruns = performanceMetrics.totalUnderruns + (performance.underrunRate * 30).toInt(),
            totalOverruns = performanceMetrics.totalOverruns + (performance.overrunRate * 30).toInt(),
            totalStalls = performanceMetrics.totalStalls + (performance.stallRate * 30).toInt(),
            averageStability = (performanceMetrics.averageStability + performance.bufferStability) / 2
        )
    }
    
    private fun performAutoAdjustment(performance: BufferPerformanceAnalysis) {
        bufferScope.launch {
            try {
                val adjustment = when {
                    performance.underrunRate > 0.2f -> "increase_buffer"
                    performance.overrunRate > 0.3f -> "decrease_buffer"
                    performance.stallRate > 0.1f -> "aggressive_buffer"
                    else -> "no_adjustment"
                }
                
                _bufferEvents.emit(BufferEvent.AutoAdjustment(adjustment, performance))
            } catch (e: Exception) {
                _bufferEvents.emit(BufferEvent.Error(e))
            }
        }
    }
    
    fun cleanup() {
        isInitialized = false
        bufferScope.cancel()
        bufferHistory.clear()
    }
}

// Data classes for buffer optimization
data class BufferConfiguration(
    val minBufferMs: Long,
    val maxBufferMs: Long,
    val bufferForPlayback: Float,
    val bufferForPlaybackAfterRebuffer: Float,
    val targetBufferBytes: Long,
    val prioritizeTimeOverSizeThresholds: Boolean,
    val backBufferDurationMs: Long,
    val rebufferTimeoutMs: Long
) {
    fun copy(
        minBufferMs: Long = this.minBufferMs,
        maxBufferMs: Long = this.maxBufferMs,
        bufferForPlayback: Float = this.bufferForPlayback,
        bufferForPlaybackAfterRebuffer: Float = this.bufferForPlaybackAfterRebuffer,
        targetBufferBytes: Long = this.targetBufferBytes,
        prioritizeTimeOverSizeThresholds: Boolean = this.prioritizeTimeOverSizeThresholds,
        backBufferDurationMs: Long = this.backBufferDurationMs,
        rebufferTimeoutMs: Long = this.rebufferTimeoutMs
    ) = BufferConfiguration(
        minBufferMs, maxBufferMs, bufferForPlayback, bufferForPlaybackAfterRebuffer,
        targetBufferBytes, prioritizeTimeOverSizeThresholds, backBufferDurationMs, rebufferTimeoutMs
    )
}

data class BufferSnapshot(
    val timestamp: Long,
    val bufferLevel: Long,
    val targetLevel: Long,
    val isStalled: Boolean,
    val rebufferCount: Int,
    val configuration: BufferConfiguration
)

data class BufferAdjustment(
    val newConfig: BufferConfiguration,
    val shouldUpdate: Boolean,
    val reason: String,
    val description: String
)

data class BufferPerformanceAnalysis(
    val underrunRate: Float,
    val overrunRate: Float,
    val stallRate: Float,
    val averageBufferLevel: Long,
    val bufferStability: Float,
    val requiresAdjustment: Boolean
) {
    companion object {
        fun default() = BufferPerformanceAnalysis(0f, 0f, 0f, 5000L, 1.0f, false)
    }
}

data class RecentBufferPerformance(
    val isOptimal: Boolean,
    val recommendedAdjustment: Float
) {
    companion object {
        fun default() = RecentBufferPerformance(true, 1.0f)
    }
}

data class BufferPerformanceMetrics(
    val totalUnderruns: Int = 0,
    val totalOverruns: Int = 0,
    val totalStalls: Int = 0,
    val averageStability: Float = 1.0f
) {
    fun copy(
        totalUnderruns: Int = this.totalUnderruns,
        totalOverruns: Int = this.totalOverruns,
        totalStalls: Int = this.totalStalls,
        averageStability: Float = this.averageStability
    ) = BufferPerformanceMetrics(totalUnderruns, totalOverruns, totalStalls, averageStability)
}

data class BufferStatistics(
    val currentConfiguration: BufferConfiguration,
    val averageBufferLevel: Long,
    val bufferStability: Float,
    val underrunEvents: Int,
    val overrunEvents: Int,
    val stallEvents: Int,
    val averageRebufferTime: Long,
    val healthScore: Float,
    val strategy: BufferStrategy
)

sealed class BufferEvent {
    data class Initialized(val configuration: BufferConfiguration) : BufferEvent()
    data class ConfigurationChanged(val previous: BufferConfiguration, val new: BufferConfiguration) : BufferEvent()
    data class HealthUpdate(val healthScore: Float, val performance: BufferPerformanceAnalysis) : BufferEvent()
    data class AutoAdjustment(val adjustment: String, val performance: BufferPerformanceAnalysis) : BufferEvent()
    data class Error(val exception: Exception) : BufferEvent()
}