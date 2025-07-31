package com.astralplayer.nextplayer.streaming

import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class AdaptiveBitrateController @Inject constructor() {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _currentBitrate = MutableStateFlow(0L)
    val currentBitrate: StateFlow<Long> = _currentBitrate.asStateFlow()
    
    private val _targetQuality = MutableStateFlow(AdvancedStreamingEngine.VideoQuality.HD_720P)
    val targetQuality: StateFlow<AdvancedStreamingEngine.VideoQuality> = _targetQuality.asStateFlow()
    
    private var currentSession: AdvancedStreamingEngine.StreamingSession? = null
    private val qualityHistory = mutableListOf<QualityChange>()
    
    fun initialize(
        session: AdvancedStreamingEngine.StreamingSession,
        networkCondition: NetworkCondition
    ) {
        currentSession = session
        adaptToNetworkCondition(networkCondition)
    }
    
    fun adaptToNetworkCondition(condition: NetworkCondition) {
        val optimalQuality = calculateOptimalQuality(condition)
        val optimalBitrate = getTargetBitrate(optimalQuality)
        
        if (optimalQuality != _targetQuality.value) {
            val qualityChange = QualityChange(
                timestamp = System.currentTimeMillis(),
                fromQuality = _targetQuality.value,
                toQuality = optimalQuality,
                reason = getAdaptationReason(condition),
                networkCondition = condition
            )
            
            qualityHistory.add(qualityChange)
            _targetQuality.value = optimalQuality
            _currentBitrate.value = optimalBitrate
            
            currentSession?.qualitySwitchCount = currentSession?.qualitySwitchCount?.plus(1) ?: 1
        }
    }
    
    private fun calculateOptimalQuality(condition: NetworkCondition): AdvancedStreamingEngine.VideoQuality {
        return when (condition.quality) {
            AdvancedStreamingEngine.NetworkQuality.EXCELLENT -> {
                when {
                    condition.bandwidth > 25_000_000 -> AdvancedStreamingEngine.VideoQuality.UHD_4K
                    condition.bandwidth > 8_000_000 -> AdvancedStreamingEngine.VideoQuality.FHD_1080P
                    else -> AdvancedStreamingEngine.VideoQuality.HD_720P
                }
            }
            AdvancedStreamingEngine.NetworkQuality.GOOD -> {
                when {
                    condition.bandwidth > 5_000_000 -> AdvancedStreamingEngine.VideoQuality.FHD_1080P
                    condition.bandwidth > 2_500_000 -> AdvancedStreamingEngine.VideoQuality.HD_720P
                    else -> AdvancedStreamingEngine.VideoQuality.SD_480P
                }
            }
            AdvancedStreamingEngine.NetworkQuality.POOR -> {
                when {
                    condition.bandwidth > 1_000_000 -> AdvancedStreamingEngine.VideoQuality.SD_480P
                    else -> AdvancedStreamingEngine.VideoQuality.SD_480P // Lowest quality
                }
            }
        }
    }
    
    private fun getTargetBitrate(quality: AdvancedStreamingEngine.VideoQuality): Long {
        return when (quality) {
            AdvancedStreamingEngine.VideoQuality.SD_480P -> 1_000_000L // 1 Mbps
            AdvancedStreamingEngine.VideoQuality.HD_720P -> 2_500_000L // 2.5 Mbps
            AdvancedStreamingEngine.VideoQuality.FHD_1080P -> 5_000_000L // 5 Mbps
            AdvancedStreamingEngine.VideoQuality.UHD_4K -> 15_000_000L // 15 Mbps
        }
    }
    
    private fun getAdaptationReason(condition: NetworkCondition): AdaptationReason {
        return when (condition.quality) {
            AdvancedStreamingEngine.NetworkQuality.POOR -> AdaptationReason.POOR_NETWORK
            AdvancedStreamingEngine.NetworkQuality.GOOD -> AdaptationReason.NETWORK_IMPROVEMENT
            AdvancedStreamingEngine.NetworkQuality.EXCELLENT -> AdaptationReason.OPTIMAL_NETWORK
        }
    }
    
    /**
     * Predict best quality for given network conditions
     */
    fun predictOptimalQuality(
        bandwidth: Long,
        latency: Int,
        bufferHealth: Float
    ): AdvancedStreamingEngine.VideoQuality {
        // Factor in buffer health for more intelligent decisions
        val adjustedBandwidth = when {
            bufferHealth < 0.3f -> bandwidth * 0.7 // Conservative approach when buffer is low
            bufferHealth > 0.8f -> bandwidth * 1.2 // Aggressive approach when buffer is healthy
            else -> bandwidth.toDouble()
        }.toLong()
        
        return when {
            adjustedBandwidth > 20_000_000 && latency < 50 -> AdvancedStreamingEngine.VideoQuality.UHD_4K
            adjustedBandwidth > 6_000_000 && latency < 100 -> AdvancedStreamingEngine.VideoQuality.FHD_1080P
            adjustedBandwidth > 3_000_000 && latency < 200 -> AdvancedStreamingEngine.VideoQuality.HD_720P
            else -> AdvancedStreamingEngine.VideoQuality.SD_480P
        }
    }
    
    /**
     * Get adaptation statistics
     */
    fun getAdaptationStats(): AdaptationStats {
        val totalChanges = qualityHistory.size
        val upscales = qualityHistory.count { isUpgrade(it.fromQuality, it.toQuality) }
        val downscales = totalChanges - upscales
        
        val averageQuality = qualityHistory
            .map { getQualityScore(it.toQuality) }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0
        
        return AdaptationStats(
            totalQualityChanges = totalChanges,
            upscales = upscales,
            downscales = downscales,
            averageQualityScore = averageQuality.toFloat(),
            stabilityScore = calculateStabilityScore()
        )
    }
    
    private fun isUpgrade(
        from: AdvancedStreamingEngine.VideoQuality,
        to: AdvancedStreamingEngine.VideoQuality
    ): Boolean {
        return getQualityScore(to) > getQualityScore(from)
    }
    
    private fun getQualityScore(quality: AdvancedStreamingEngine.VideoQuality): Int {
        return when (quality) {
            AdvancedStreamingEngine.VideoQuality.SD_480P -> 1
            AdvancedStreamingEngine.VideoQuality.HD_720P -> 2
            AdvancedStreamingEngine.VideoQuality.FHD_1080P -> 3
            AdvancedStreamingEngine.VideoQuality.UHD_4K -> 4
        }
    }
    
    private fun calculateStabilityScore(): Float {
        if (qualityHistory.isEmpty()) return 1.0f
        
        val recentChanges = qualityHistory.takeLast(10)
        val changeFrequency = recentChanges.size.toFloat() / 10f
        
        // Lower change frequency = higher stability
        return (1.0f - changeFrequency).coerceIn(0f, 1f)
    }
    
    fun release() {
        scope.cancel()
        currentSession = null
        qualityHistory.clear()
    }
    
    // Data classes
    data class QualityChange(
        val timestamp: Long,
        val fromQuality: AdvancedStreamingEngine.VideoQuality,
        val toQuality: AdvancedStreamingEngine.VideoQuality,
        val reason: AdaptationReason,
        val networkCondition: NetworkCondition
    )
    
    data class AdaptationStats(
        val totalQualityChanges: Int,
        val upscales: Int,
        val downscales: Int,
        val averageQualityScore: Float,
        val stabilityScore: Float
    )
    
    enum class AdaptationReason {
        POOR_NETWORK,
        NETWORK_IMPROVEMENT,
        OPTIMAL_NETWORK,
        BUFFER_UNDERRUN,
        BUFFER_HEALTHY
    }
}