package com.astralplayer.nextplayer.streaming

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * Advanced adaptive bitrate management for optimal streaming quality
 */
class AdaptiveBitrateManager {
    
    private val _bitrateEvents = MutableSharedFlow<BitrateEvent>()
    val bitrateEvents: SharedFlow<BitrateEvent> = _bitrateEvents.asSharedFlow()
    
    private var currentBitrate = 1000000 // 1 Mbps default
    private var targetBitrate = 1000000
    private var availableBitrates = listOf(200000, 500000, 1000000, 2000000, 4000000, 8000000)
    
    private var bitrateHistory = mutableListOf<BitrateHistoryEntry>()
    private var networkHistory = mutableListOf<NetworkSample>()
    private var bufferHistory = mutableListOf<BufferSample>()
    
    private var config = BitrateConfig()
    private var isInitialized = false
    private var adaptationMode = AdaptationMode.AGGRESSIVE
    
    private val bitrateScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Initialize adaptive bitrate management
     */
    suspend fun initialize(bitrateConfig: BitrateConfig) {
        config = bitrateConfig
        currentBitrate = config.initialBitrate
        targetBitrate = config.initialBitrate
        availableBitrates = generateBitrateLadder(config)
        isInitialized = true
        
        startAdaptationMonitoring()
    }
    
    /**
     * Optimize bitrate based on current conditions
     */
    suspend fun optimizeBitrate(targetBitrate: Int, networkInfo: NetworkInfo): String {
        if (!isInitialized) return "Not initialized"
        
        try {
            // Record network sample
            recordNetworkSample(networkInfo)
            
            // Calculate optimal bitrate
            val optimalBitrate = calculateOptimalBitrate(networkInfo)
            
            // Apply adaptation algorithm
            val newBitrate = applyAdaptationAlgorithm(optimalBitrate, networkInfo)
            
            // Validate bitrate change
            if (shouldChangeBitrate(newBitrate, networkInfo)) {
                val previousBitrate = currentBitrate
                currentBitrate = newBitrate
                this.targetBitrate = newBitrate
                
                // Record bitrate change
                recordBitrateChange(previousBitrate, newBitrate, "Network optimization")
                
                _bitrateEvents.emit(BitrateEvent.BitrateChanged(previousBitrate, newBitrate, "optimization"))
                
                return "Bitrate optimized: ${previousBitrate / 1000}kbps → ${newBitrate / 1000}kbps"
            }
            
            return "Bitrate maintained: ${currentBitrate / 1000}kbps"
            
        } catch (e: Exception) {
            _bitrateEvents.emit(BitrateEvent.Error(e))
            return "Optimization failed: ${e.message}"
        }
    }
    
    /**
     * Adjust bitrate based on real-time metrics
     */
    suspend fun adjustBitrate(
        networkMetrics: NetworkMetrics,
        performanceAnalysis: PerformanceAnalysis
    ): Int {
        if (!isInitialized) return currentBitrate
        
        val adjustmentReason = performanceAnalysis.reason
        val newBitrate = when {
            performanceAnalysis.requiresBitrateAdjustment -> {
                calculateEmergencyBitrateAdjustment(networkMetrics, adjustmentReason)
            }
            else -> {
                calculateGradualBitrateAdjustment(networkMetrics)
            }
        }
        
        if (newBitrate != currentBitrate) {
            val previousBitrate = currentBitrate
            currentBitrate = newBitrate
            
            recordBitrateChange(previousBitrate, newBitrate, adjustmentReason)
            _bitrateEvents.emit(BitrateEvent.BitrateChanged(previousBitrate, newBitrate, adjustmentReason))
        }
        
        return currentBitrate
    }
    
    /**
     * Set minimum bitrate for poor network conditions
     */
    suspend fun setMinimumBitrate() {
        val minBitrate = availableBitrates.minOrNull() ?: config.minBitrate
        if (currentBitrate != minBitrate) {
            val previousBitrate = currentBitrate
            currentBitrate = minBitrate
            
            recordBitrateChange(previousBitrate, minBitrate, "Minimum bitrate for poor network")
            _bitrateEvents.emit(BitrateEvent.BitrateChanged(previousBitrate, minBitrate, "poor_network"))
        }
    }
    
    /**
     * Set balanced bitrate for normal conditions
     */
    suspend fun setBalancedBitrate(networkInfo: NetworkInfo) {
        val balancedBitrate = calculateBalancedBitrate(networkInfo)
        if (currentBitrate != balancedBitrate) {
            val previousBitrate = currentBitrate
            currentBitrate = balancedBitrate
            
            recordBitrateChange(previousBitrate, balancedBitrate, "Balanced bitrate for stable network")
            _bitrateEvents.emit(BitrateEvent.BitrateChanged(previousBitrate, balancedBitrate, "balanced"))
        }
    }
    
    /**
     * Set maximum bitrate for excellent conditions
     */
    suspend fun setMaximumBitrate(networkInfo: NetworkInfo) {
        val maxBitrate = calculateMaxSafeBitrate(networkInfo)
        if (currentBitrate != maxBitrate) {
            val previousBitrate = currentBitrate
            currentBitrate = maxBitrate
            
            recordBitrateChange(previousBitrate, maxBitrate, "Maximum bitrate for excellent network")
            _bitrateEvents.emit(BitrateEvent.BitrateChanged(previousBitrate, maxBitrate, "excellent_network"))
        }
    }
    
    /**
     * Prepare bitrate options for content
     */
    fun prepareBitrateOptions(contentInfo: ContentInfo, networkInfo: NetworkInfo): List<Int> {
        val contentMaxBitrate = contentInfo.bitrate
        val networkCapacity = (networkInfo.bandwidth * 0.8f).toInt() // 80% utilization
        
        return availableBitrates.filter { bitrate ->
            bitrate <= minOf(contentMaxBitrate, networkCapacity, config.maxBitrate)
        }.sorted()
    }
    
    /**
     * Enable thermal protection mode
     */
    suspend fun enableThermalProtection() {
        adaptationMode = AdaptationMode.CONSERVATIVE
        
        // Reduce bitrate by 30% to reduce heat generation
        val thermalBitrate = (currentBitrate * 0.7f).toInt()
        val safeBitrate = availableBitrates.filter { it <= thermalBitrate }.maxOrNull() ?: config.minBitrate
        
        if (currentBitrate != safeBitrate) {
            val previousBitrate = currentBitrate
            currentBitrate = safeBitrate
            
            recordBitrateChange(previousBitrate, safeBitrate, "Thermal protection enabled")
            _bitrateEvents.emit(BitrateEvent.BitrateChanged(previousBitrate, safeBitrate, "thermal_protection"))
        }
    }
    
    /**
     * Set performance modes
     */
    suspend fun setLowResourceMode() {
        adaptationMode = AdaptationMode.CONSERVATIVE
        val lowResourceBitrate = availableBitrates.filter { it <= config.initialBitrate }.maxOrNull() ?: config.minBitrate
        
        if (currentBitrate != lowResourceBitrate) {
            val previousBitrate = currentBitrate
            currentBitrate = lowResourceBitrate
            
            recordBitrateChange(previousBitrate, lowResourceBitrate, "Low resource mode")
            _bitrateEvents.emit(BitrateEvent.BitrateChanged(previousBitrate, lowResourceBitrate, "low_resource"))
        }
    }
    
    suspend fun setHighPerformanceMode() {
        adaptationMode = AdaptationMode.AGGRESSIVE
        _bitrateEvents.emit(BitrateEvent.ModeChanged(AdaptationMode.AGGRESSIVE))
    }
    
    /**
     * Get current bitrate statistics
     */
    fun getCurrentBitrate(): Int = currentBitrate
    
    fun getAverageBitrate(): Int {
        return if (bitrateHistory.isNotEmpty()) {
            bitrateHistory.map { it.bitrate }.average().toInt()
        } else currentBitrate
    }
    
    fun getStabilityScore(): Float {
        if (bitrateHistory.size < 10) return 1.0f
        
        val recentChanges = bitrateHistory.takeLast(20)
        val changes = recentChanges.zipWithNext().count { (prev, curr) -> prev.bitrate != curr.bitrate }
        
        return 1.0f - (changes.toFloat() / recentChanges.size)
    }
    
    fun getBitrateStatistics(): BitrateStatistics {
        return BitrateStatistics(
            currentBitrate = currentBitrate,
            averageBitrate = getAverageBitrate(),
            stabilityScore = getStabilityScore(),
            totalChanges = bitrateHistory.size,
            upwardChanges = bitrateHistory.count { it.reason.contains("upgrade") || it.reason.contains("increase") },
            downwardChanges = bitrateHistory.count { it.reason.contains("downgrade") || it.reason.contains("decrease") },
            availableOptions = availableBitrates,
            adaptationMode = adaptationMode
        )
    }
    
    // Private implementation methods
    private fun startAdaptationMonitoring() {
        bitrateScope.launch {
            while (isInitialized && currentCoroutineContext().isActive) {
                try {
                    // Analyze recent performance
                    val recentPerformance = analyzeRecentPerformance()
                    
                    // Adjust adaptation sensitivity based on network stability
                    adjustAdaptationSensitivity(recentPerformance)
                    
                    // Clean old history data
                    cleanOldHistoryData()
                    
                    delay(10000) // Monitor every 10 seconds
                } catch (e: Exception) {
                    // Log error but continue
                }
            }
        }
    }
    
    private fun generateBitrateLadder(config: BitrateConfig): List<Int> {
        val ladder = mutableListOf<Int>()
        var currentBitrate = config.minBitrate
        
        while (currentBitrate <= config.maxBitrate) {
            ladder.add(currentBitrate)
            currentBitrate = (currentBitrate * 1.5f).toInt() // 50% increase each step
        }
        
        // Ensure initial and max bitrates are included
        if (config.initialBitrate !in ladder) {
            ladder.add(config.initialBitrate)
        }
        if (config.maxBitrate !in ladder) {
            ladder.add(config.maxBitrate)
        }
        
        return ladder.sorted()
    }
    
    private fun recordNetworkSample(networkInfo: NetworkInfo) {
        networkHistory.add(
            NetworkSample(
                timestamp = System.currentTimeMillis(),
                bandwidth = networkInfo.bandwidth,
                latency = networkInfo.latency,
                quality = networkInfo.connectionQuality
            )
        )
        
        // Keep last 100 samples
        if (networkHistory.size > 100) {
            networkHistory.removeAt(0)
        }
    }
    
    private fun recordBitrateChange(previousBitrate: Int, newBitrate: Int, reason: String) {
        bitrateHistory.add(
            BitrateHistoryEntry(
                timestamp = System.currentTimeMillis(),
                previousBitrate = previousBitrate,
                bitrate = newBitrate,
                reason = reason
            )
        )
        
        // Keep last 200 changes
        if (bitrateHistory.size > 200) {
            bitrateHistory.removeAt(0)
        }
    }
    
    private fun calculateOptimalBitrate(networkInfo: NetworkInfo): Int {
        val networkCapacity = networkInfo.bandwidth * 0.8f // 80% utilization
        val latencyPenalty = calculateLatencyPenalty(networkInfo.latency)
        val adjustedCapacity = networkCapacity * latencyPenalty
        
        return availableBitrates.filter { it <= adjustedCapacity }.maxOrNull() ?: config.minBitrate
    }
    
    private fun calculateLatencyPenalty(latency: Long): Float {
        return when {
            latency < 50 -> 1.0f      // No penalty
            latency < 100 -> 0.9f     // 10% penalty
            latency < 200 -> 0.8f     // 20% penalty
            latency < 500 -> 0.7f     // 30% penalty
            else -> 0.6f              // 40% penalty
        }
    }
    
    private fun applyAdaptationAlgorithm(optimalBitrate: Int, networkInfo: NetworkInfo): Int {
        val currentIndex = availableBitrates.indexOf(currentBitrate)
        val optimalIndex = availableBitrates.indexOf(optimalBitrate)
        
        if (currentIndex == -1 || optimalIndex == -1) return optimalBitrate
        
        return when (adaptationMode) {
            AdaptationMode.CONSERVATIVE -> {
                // Change by at most one step
                when {
                    optimalIndex > currentIndex -> availableBitrates[currentIndex + 1]
                    optimalIndex < currentIndex -> availableBitrates[currentIndex - 1]
                    else -> currentBitrate
                }
            }
            
            AdaptationMode.BALANCED -> {
                // Change by at most two steps
                val maxSteps = 2
                val targetIndex = when {
                    optimalIndex > currentIndex -> minOf(currentIndex + maxSteps, optimalIndex)
                    optimalIndex < currentIndex -> maxOf(currentIndex - maxSteps, optimalIndex)
                    else -> currentIndex
                }
                availableBitrates[targetIndex]
            }
            
            AdaptationMode.AGGRESSIVE -> {
                // Go directly to optimal
                optimalBitrate
            }
        }
    }
    
    private fun shouldChangeBitrate(newBitrate: Int, networkInfo: NetworkInfo): Boolean {
        if (newBitrate == currentBitrate) return false
        
        // Don't change too frequently
        val lastChange = bitrateHistory.lastOrNull()
        if (lastChange != null && System.currentTimeMillis() - lastChange.timestamp < 5000) {
            return false
        }
        
        // Don't oscillate
        if (bitrateHistory.size >= 3) {
            val lastThree = bitrateHistory.takeLast(3)
            if (lastThree.all { it.bitrate == newBitrate || it.bitrate == currentBitrate }) {
                return false // Avoid oscillation
            }
        }
        
        // Ensure significant improvement for upgrades
        if (newBitrate > currentBitrate) {
            val improvementRatio = newBitrate.toFloat() / currentBitrate
            return improvementRatio >= 1.25f // At least 25% improvement
        }
        
        return true
    }
    
    private fun calculateEmergencyBitrateAdjustment(
        networkMetrics: NetworkMetrics,
        reason: String
    ): Int {
        return when {
            reason.contains("stall") || reason.contains("underrun") -> {
                // Aggressive downgrade for stalls
                val currentIndex = availableBitrates.indexOf(currentBitrate)
                val targetIndex = maxOf(0, currentIndex - 2) // Down by 2 steps
                availableBitrates[targetIndex]
            }
            
            reason.contains("network degraded") -> {
                // Conservative downgrade for network issues
                val targetCapacity = networkMetrics.bandwidth * 0.6f // 60% utilization for safety
                availableBitrates.filter { it <= targetCapacity }.maxOrNull() ?: config.minBitrate
            }
            
            else -> calculateGradualBitrateAdjustment(networkMetrics)
        }
    }
    
    private fun calculateGradualBitrateAdjustment(networkMetrics: NetworkMetrics): Int {
        val networkCapacity = networkMetrics.bandwidth * 0.75f // 75% utilization
        val optimalBitrate = availableBitrates.filter { it <= networkCapacity }.maxOrNull() ?: config.minBitrate
        
        return applyAdaptationAlgorithm(optimalBitrate, NetworkInfo(
            NetworkType.UNKNOWN, networkMetrics.bandwidth, networkMetrics.latency,
            ConnectionQuality.GOOD, false, networkMetrics.averageBandwidth
        ))
    }
    
    private fun calculateBalancedBitrate(networkInfo: NetworkInfo): Int {
        val networkCapacity = networkInfo.bandwidth * 0.7f // Conservative utilization
        return availableBitrates.filter { it <= networkCapacity }.maxOrNull() ?: config.minBitrate
    }
    
    private fun calculateMaxSafeBitrate(networkInfo: NetworkInfo): Int {
        val networkCapacity = networkInfo.bandwidth * 0.9f // Aggressive utilization
        return availableBitrates.filter { it <= networkCapacity }.maxOrNull() ?: config.maxBitrate
    }
    
    private fun analyzeRecentPerformance(): PerformanceAnalysis {
        // Simplified performance analysis
        val recentNetworkSamples = networkHistory.takeLast(10)
        val networkStability = if (recentNetworkSamples.isNotEmpty()) {
            val bandwidthVariance = calculateVariance(recentNetworkSamples.map { it.bandwidth })
            1.0f / (1.0f + bandwidthVariance / 1000000f) // Normalize variance
        } else 1.0f
        
        return PerformanceAnalysis(
            requiresBitrateAdjustment = networkStability < 0.7f,
            requiresBufferAdjustment = false,
            requiresCacheAdjustment = false,
            reason = if (networkStability < 0.7f) "Network instability detected" else "Stable performance"
        )
    }
    
    private fun adjustAdaptationSensitivity(performance: PerformanceAnalysis) {
        adaptationMode = when {
            performance.reason.contains("instability") -> AdaptationMode.CONSERVATIVE
            performance.reason.contains("stable") -> AdaptationMode.BALANCED
            else -> adaptationMode
        }
    }
    
    private fun cleanOldHistoryData() {
        val cutoffTime = System.currentTimeMillis() - 300000 // 5 minutes
        
        networkHistory.removeAll { it.timestamp < cutoffTime }
        bufferHistory.removeAll { it.timestamp < cutoffTime }
        
        // Keep at least some history
        if (networkHistory.size > 50) {
            networkHistory = networkHistory.takeLast(50).toMutableList()
        }
    }
    
    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean).pow(2) }.average().toFloat()
        return variance
    }
    
    fun cleanup() {
        isInitialized = false
        bitrateScope.cancel()
        bitrateHistory.clear()
        networkHistory.clear()
        bufferHistory.clear()
    }
}

// Data classes and enums for adaptive bitrate management
enum class AdaptationMode { CONSERVATIVE, BALANCED, AGGRESSIVE }

data class BitrateHistoryEntry(
    val timestamp: Long,
    val previousBitrate: Int,
    val bitrate: Int,
    val reason: String
)

data class NetworkSample(
    val timestamp: Long,
    val bandwidth: Float,
    val latency: Long,
    val quality: ConnectionQuality
)

data class BufferSample(
    val timestamp: Long,
    val bufferLevel: Long,
    val targetBuffer: Long
)

data class BitrateStatistics(
    val currentBitrate: Int,
    val averageBitrate: Int,
    val stabilityScore: Float,
    val totalChanges: Int,
    val upwardChanges: Int,
    val downwardChanges: Int,
    val availableOptions: List<Int>,
    val adaptationMode: AdaptationMode
)

sealed class BitrateEvent {
    data class BitrateChanged(val previousBitrate: Int, val newBitrate: Int, val reason: String) : BitrateEvent()
    data class ModeChanged(val newMode: AdaptationMode) : BitrateEvent()
    data class Error(val exception: Exception) : BitrateEvent()
}