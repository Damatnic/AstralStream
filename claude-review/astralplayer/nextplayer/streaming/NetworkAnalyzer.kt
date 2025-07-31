package com.astralplayer.nextplayer.streaming

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * Advanced network analysis for streaming optimization
 */
class NetworkAnalyzer(
    private val connectivityManager: ConnectivityManager
) {
    
    private val _networkEvents = MutableSharedFlow<NetworkEvent>()
    val networkEvents: SharedFlow<NetworkEvent> = _networkEvents.asSharedFlow()
    
    private var currentNetworkInfo = NetworkInfo(
        NetworkType.UNKNOWN, 0f, 0L, ConnectionQuality.POOR, false, 0f
    )
    
    private var networkHistory = mutableListOf<NetworkMeasurement>()
    private var bandwidthHistory = mutableListOf<BandwidthSample>()
    private var latencyHistory = mutableListOf<LatencySample>()
    
    private var isInitialized = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val analysisScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Initialize network analysis
     */
    suspend fun initialize() {
        isInitialized = true
        
        // Register network callback
        registerNetworkCallback()
        
        // Start continuous monitoring
        startNetworkMonitoring()
        
        // Initial network analysis
        updateCurrentNetworkInfo()
        
        _networkEvents.emit(NetworkEvent.Initialized(currentNetworkInfo))
    }
    
    /**
     * Get current network information
     */
    fun getCurrentNetworkInfo(): NetworkInfo {
        return currentNetworkInfo
    }
    
    /**
     * Get real-time network metrics
     */
    suspend fun getRealtimeMetrics(): NetworkMetrics {
        val recentBandwidth = bandwidthHistory.takeLast(10)
        val recentLatency = latencyHistory.takeLast(10)
        
        return NetworkMetrics(
            bandwidth = recentBandwidth.lastOrNull()?.bandwidth ?: currentNetworkInfo.bandwidth,
            latency = recentLatency.lastOrNull()?.latency ?: currentNetworkInfo.latency,
            packetLoss = calculatePacketLoss(),
            jitter = calculateJitter(recentLatency),
            averageBandwidth = calculateAverageBandwidth()
        )
    }
    
    /**
     * Analyze network change
     */
    suspend fun analyzeNetworkChange(
        networkType: NetworkType, 
        capabilities: NetworkCapabilities?
    ): NetworkInfo {
        val newNetworkInfo = analyzeNetworkCapabilities(networkType, capabilities)
        
        if (newNetworkInfo != currentNetworkInfo) {
            val previousInfo = currentNetworkInfo
            currentNetworkInfo = newNetworkInfo
            
            recordNetworkChange(previousInfo, newNetworkInfo)
            _networkEvents.emit(NetworkEvent.NetworkChanged(previousInfo, newNetworkInfo))
        }
        
        return newNetworkInfo
    }
    
    /**
     * Measure network bandwidth
     */
    suspend fun measureBandwidth(): Float = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            // Simulate bandwidth measurement (in real implementation, this would download test data)
            val testDataSize = 1024 * 100 // 100KB
            val downloadTime = simulateDataDownload(testDataSize)
            
            val bandwidth = (testDataSize * 8f) / (downloadTime / 1000f) // bits per second
            
            recordBandwidthMeasurement(bandwidth)
            bandwidth
            
        } catch (e: Exception) {
            currentNetworkInfo.bandwidth
        }
    }
    
    /**
     * Measure network latency
     */
    suspend fun measureLatency(): Long = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            // Simulate ping measurement
            val latency = simulateNetworkPing()
            
            recordLatencyMeasurement(latency)
            latency
            
        } catch (e: Exception) {
            currentNetworkInfo.latency
        }
    }
    
    /**
     * Get network efficiency score
     */
    fun getEfficiencyScore(): Float {
        if (networkHistory.isEmpty()) return 0.8f
        
        val recentMeasurements = networkHistory.takeLast(20)
        val consistencyScore = calculateNetworkConsistency(recentMeasurements)
        val qualityScore = calculateQualityScore(currentNetworkInfo.connectionQuality)
        
        return (consistencyScore + qualityScore) / 2f
    }
    
    /**
     * Predict network conditions
     */
    fun predictNetworkConditions(durationMs: Long): NetworkPrediction {
        val trend = analyzeBandwidthTrend()
        val stability = calculateNetworkStability()
        
        val predictedBandwidth = currentNetworkInfo.bandwidth * (1f + trend * (durationMs / 60000f))
        val confidence = stability * 0.8f // Reduce confidence based on prediction duration
        
        return NetworkPrediction(
            predictedBandwidth = predictedBandwidth.coerceAtLeast(0f),
            predictedLatency = currentNetworkInfo.latency,
            confidence = confidence.coerceIn(0f, 1f),
            recommendation = generateNetworkRecommendation(predictedBandwidth, stability)
        )
    }
    
    /**
     * Check if network is suitable for quality
     */
    fun isNetworkSuitableForQuality(requiredBitrate: Int, bufferMs: Long): Boolean {
        val availableBandwidth = currentNetworkInfo.bandwidth * 0.8f // 80% utilization
        val hasEnoughBandwidth = availableBandwidth >= requiredBitrate
        
        val isStable = getEfficiencyScore() > 0.7f
        val hasReasonableLatency = currentNetworkInfo.latency < 200L
        
        return hasEnoughBandwidth && isStable && hasReasonableLatency
    }
    
    // Private implementation methods
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                analysisScope.launch {
                    updateCurrentNetworkInfo()
                    _networkEvents.emit(NetworkEvent.Available(network))
                }
            }
            
            override fun onLost(network: Network) {
                analysisScope.launch {
                    _networkEvents.emit(NetworkEvent.Lost(network))
                }
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                analysisScope.launch {
                    val newInfo = analyzeNetworkCapabilities(getNetworkType(capabilities), capabilities)
                    analyzeNetworkChange(newInfo.type, capabilities)
                }
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }
    
    private fun startNetworkMonitoring() {
        analysisScope.launch {
            while (isInitialized && currentCoroutineContext().isActive) {
                try {
                    // Periodic bandwidth measurement
                    val bandwidth = measureBandwidth()
                    val latency = measureLatency()
                    
                    // Update current info
                    currentNetworkInfo = currentNetworkInfo.copy(
                        bandwidth = bandwidth,
                        latency = latency,
                        connectionQuality = determineConnectionQuality(bandwidth, latency)
                    )
                    
                    _networkEvents.emit(NetworkEvent.MetricsUpdate(getRealtimeMetrics()))
                    
                    delay(30000) // Monitor every 30 seconds
                } catch (e: Exception) {
                    // Continue monitoring despite errors
                }
            }
        }
    }
    
    private suspend fun updateCurrentNetworkInfo() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        
        currentNetworkInfo = if (capabilities != null) {
            analyzeNetworkCapabilities(getNetworkType(capabilities), capabilities)
        } else {
            NetworkInfo(NetworkType.UNKNOWN, 0f, 0L, ConnectionQuality.POOR, false, 0f)
        }
    }
    
    private fun analyzeNetworkCapabilities(
        networkType: NetworkType,
        capabilities: NetworkCapabilities?
    ): NetworkInfo {
        if (capabilities == null) {
            return NetworkInfo(NetworkType.UNKNOWN, 0f, Long.MAX_VALUE, ConnectionQuality.POOR, false, 0f)
        }
        
        val bandwidth = estimateBandwidth(networkType, capabilities)
        val latency = estimateLatency(networkType)
        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val quality = determineConnectionQuality(bandwidth, latency)
        val averageBandwidth = calculateAverageBandwidth()
        
        return NetworkInfo(networkType, bandwidth, latency, quality, isMetered, averageBandwidth)
    }
    
    private fun getNetworkType(capabilities: NetworkCapabilities): NetworkType {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.UNKNOWN
        }
    }
    
    private fun estimateBandwidth(networkType: NetworkType, capabilities: NetworkCapabilities): Float {
        // Get downstream bandwidth if available
        val downstreamBandwidth = if (capabilities.linkDownstreamBandwidthKbps != -1) {
            capabilities.linkDownstreamBandwidthKbps * 1000f // Convert to bps
        } else {
            // Fallback estimates based on network type
            when (networkType) {
                NetworkType.WIFI -> 20_000_000f // 20 Mbps
                NetworkType.ETHERNET -> 100_000_000f // 100 Mbps
                NetworkType.CELLULAR -> 5_000_000f // 5 Mbps
                NetworkType.UNKNOWN -> 1_000_000f // 1 Mbps
            }
        }
        
        return downstreamBandwidth
    }
    
    private fun estimateLatency(networkType: NetworkType): Long {
        return when (networkType) {
            NetworkType.WIFI -> 20L
            NetworkType.ETHERNET -> 5L
            NetworkType.CELLULAR -> 100L
            NetworkType.UNKNOWN -> 200L
        }
    }
    
    private fun determineConnectionQuality(bandwidth: Float, latency: Long): ConnectionQuality {
        val bandwidthScore = when {
            bandwidth >= 10_000_000f -> 3 // >= 10 Mbps
            bandwidth >= 5_000_000f -> 2  // >= 5 Mbps
            bandwidth >= 1_000_000f -> 1  // >= 1 Mbps
            else -> 0
        }
        
        val latencyScore = when {
            latency <= 50L -> 3
            latency <= 100L -> 2
            latency <= 200L -> 1
            else -> 0
        }
        
        val totalScore = bandwidthScore + latencyScore
        
        return when {
            totalScore >= 5 -> ConnectionQuality.EXCELLENT
            totalScore >= 3 -> ConnectionQuality.GOOD
            totalScore >= 1 -> ConnectionQuality.FAIR
            else -> ConnectionQuality.POOR
        }
    }
    
    private fun recordNetworkChange(previous: NetworkInfo, new: NetworkInfo) {
        networkHistory.add(
            NetworkMeasurement(
                timestamp = System.currentTimeMillis(),
                networkType = new.type,
                bandwidth = new.bandwidth,
                latency = new.latency,
                quality = new.connectionQuality,
                changeReason = "network_switch"
            )
        )
        
        // Keep history manageable
        if (networkHistory.size > 100) {
            networkHistory.removeAt(0)
        }
    }
    
    private fun recordBandwidthMeasurement(bandwidth: Float) {
        bandwidthHistory.add(
            BandwidthSample(
                timestamp = System.currentTimeMillis(),
                bandwidth = bandwidth
            )
        )
        
        if (bandwidthHistory.size > 50) {
            bandwidthHistory.removeAt(0)
        }
    }
    
    private fun recordLatencyMeasurement(latency: Long) {
        latencyHistory.add(
            LatencySample(
                timestamp = System.currentTimeMillis(),
                latency = latency
            )
        )
        
        if (latencyHistory.size > 50) {
            latencyHistory.removeAt(0)
        }
    }
    
    private fun calculateAverageBandwidth(): Float {
        return if (bandwidthHistory.isNotEmpty()) {
            bandwidthHistory.map { it.bandwidth }.average().toFloat()
        } else {
            currentNetworkInfo.bandwidth
        }
    }
    
    private fun calculatePacketLoss(): Float {
        // Simplified packet loss calculation
        val recentMeasurements = networkHistory.takeLast(10)
        if (recentMeasurements.isEmpty()) return 0f
        
        val qualityVariation = recentMeasurements.map { measurement ->
            when (measurement.quality) {
                ConnectionQuality.EXCELLENT -> 0f
                ConnectionQuality.GOOD -> 0.01f
                ConnectionQuality.FAIR -> 0.03f
                ConnectionQuality.POOR -> 0.10f
            }
        }.average().toFloat()
        
        return qualityVariation
    }
    
    private fun calculateJitter(latencyHistory: List<LatencySample>): Long {
        if (latencyHistory.size < 2) return 0L
        
        val variations = latencyHistory.zipWithNext().map { (prev, curr) ->
            abs(curr.latency - prev.latency)
        }
        
        return variations.average().toLong()
    }
    
    private fun calculateNetworkConsistency(measurements: List<NetworkMeasurement>): Float {
        if (measurements.size < 2) return 1.0f
        
        val bandwidthVariations = measurements.zipWithNext().map { (prev, curr) ->
            abs(curr.bandwidth - prev.bandwidth) / maxOf(prev.bandwidth, curr.bandwidth)
        }
        
        val averageVariation = bandwidthVariations.average().toFloat()
        return 1.0f - minOf(1.0f, averageVariation)
    }
    
    private fun calculateQualityScore(quality: ConnectionQuality): Float {
        return when (quality) {
            ConnectionQuality.EXCELLENT -> 1.0f
            ConnectionQuality.GOOD -> 0.8f
            ConnectionQuality.FAIR -> 0.6f
            ConnectionQuality.POOR -> 0.3f
        }
    }
    
    private fun analyzeBandwidthTrend(): Float {
        if (bandwidthHistory.size < 10) return 0f
        
        val recent = bandwidthHistory.takeLast(10)
        val older = bandwidthHistory.takeLast(20).take(10)
        
        if (older.isEmpty()) return 0f
        
        val recentAverage = recent.map { it.bandwidth }.average()
        val olderAverage = older.map { it.bandwidth }.average()
        
        return ((recentAverage - olderAverage) / olderAverage).toFloat()
    }
    
    private fun calculateNetworkStability(): Float {
        val recentMeasurements = networkHistory.takeLast(20)
        if (recentMeasurements.isEmpty()) return 0.5f
        
        val qualityConsistency = calculateQualityConsistency(recentMeasurements)
        val bandwidthConsistency = calculateNetworkConsistency(recentMeasurements)
        
        return (qualityConsistency + bandwidthConsistency) / 2f
    }
    
    private fun calculateQualityConsistency(measurements: List<NetworkMeasurement>): Float {
        if (measurements.isEmpty()) return 1.0f
        
        val qualityChanges = measurements.zipWithNext().count { (prev, curr) ->
            prev.quality != curr.quality
        }
        
        return 1.0f - (qualityChanges.toFloat() / measurements.size)
    }
    
    private fun generateNetworkRecommendation(bandwidth: Float, stability: Float): String {
        return when {
            bandwidth > 10_000_000f && stability > 0.8f -> "excellent_for_hd"
            bandwidth > 5_000_000f && stability > 0.7f -> "good_for_standard"
            bandwidth > 1_000_000f && stability > 0.6f -> "adequate_for_low"
            else -> "consider_offline_mode"
        }
    }
    
    // Simulation methods (replace with real implementations)
    private suspend fun simulateDataDownload(sizeBytes: Int): Long {
        delay(100) // Simulate download time
        return 100L + (0..50L).random() // 100-150ms
    }
    
    private suspend fun simulateNetworkPing(): Long {
        delay(10) // Simulate ping time
        return when (currentNetworkInfo.type) {
            NetworkType.WIFI -> 20L + (0..30L).random()
            NetworkType.ETHERNET -> 5L + (0..10L).random()
            NetworkType.CELLULAR -> 80L + (0..100L).random()
            NetworkType.UNKNOWN -> 150L + (0..100L).random()
        }
    }
    
    fun cleanup() {
        isInitialized = false
        analysisScope.cancel()
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        networkHistory.clear()
        bandwidthHistory.clear()
        latencyHistory.clear()
    }
}

// Data classes for network analysis
data class NetworkMeasurement(
    val timestamp: Long,
    val networkType: NetworkType,
    val bandwidth: Float,
    val latency: Long,
    val quality: ConnectionQuality,
    val changeReason: String
)

data class BandwidthSample(
    val timestamp: Long,
    val bandwidth: Float
)

data class LatencySample(
    val timestamp: Long,
    val latency: Long
)

data class NetworkPrediction(
    val predictedBandwidth: Float,
    val predictedLatency: Long,
    val confidence: Float,
    val recommendation: String
)

sealed class NetworkEvent {
    data class Initialized(val networkInfo: NetworkInfo) : NetworkEvent()
    data class Available(val network: Network) : NetworkEvent()
    data class Lost(val network: Network) : NetworkEvent()
    data class NetworkChanged(val previous: NetworkInfo, val new: NetworkInfo) : NetworkEvent()
    data class MetricsUpdate(val metrics: NetworkMetrics) : NetworkEvent()
    data class QualityChanged(val newQuality: ConnectionQuality) : NetworkEvent()
    data class Error(val exception: Exception) : NetworkEvent()
}