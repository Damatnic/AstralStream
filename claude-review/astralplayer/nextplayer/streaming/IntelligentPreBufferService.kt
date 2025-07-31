package com.astralplayer.nextplayer.streaming

import android.content.Context
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class IntelligentPreBufferService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _bufferStatus = MutableStateFlow(BufferStatus())
    val bufferStatus: StateFlow<BufferStatus> = _bufferStatus.asStateFlow()
    
    private var currentSession: AdvancedStreamingEngine.StreamingSession? = null
    private var bufferJob: Job? = null
    private var isAggressiveMode = false
    private var targetBufferSize = DEFAULT_BUFFER_SIZE
    
    companion object {
        private const val DEFAULT_BUFFER_SIZE = 30_000L // 30 seconds
        private const val AGGRESSIVE_BUFFER_SIZE = 60_000L // 60 seconds
        private const val MIN_BUFFER_SIZE = 10_000L // 10 seconds
        private const val MAX_BUFFER_SIZE = 120_000L // 2 minutes
    }
    
    fun startPreBuffering(session: AdvancedStreamingEngine.StreamingSession) {
        currentSession = session
        
        bufferJob?.cancel()
        bufferJob = scope.launch {
            while (isActive) {
                updateBufferStatus()
                optimizeBufferSize()
                delay(5000) // Check every 5 seconds
            }
        }
    }
    
    fun setAggressiveMode(aggressive: Boolean) {
        isAggressiveMode = aggressive
        targetBufferSize = if (aggressive) AGGRESSIVE_BUFFER_SIZE else DEFAULT_BUFFER_SIZE
        
        _bufferStatus.value = _bufferStatus.value.copy(
            targetBufferSize = targetBufferSize,
            isAggressiveMode = aggressive
        )
    }
    
    fun increaseBufferSize() {
        targetBufferSize = (targetBufferSize * 1.5).toLong().coerceAtMost(MAX_BUFFER_SIZE)
        _bufferStatus.value = _bufferStatus.value.copy(targetBufferSize = targetBufferSize)
    }
    
    fun decreaseBufferSize() {
        targetBufferSize = (targetBufferSize * 0.8).toLong().coerceAtLeast(MIN_BUFFER_SIZE)
        _bufferStatus.value = _bufferStatus.value.copy(targetBufferSize = targetBufferSize)
    }
    
    private suspend fun updateBufferStatus() {
        currentSession?.let { session ->
            // Simulate buffer status update
            val currentBufferLevel = estimateCurrentBufferLevel(session)
            val bufferHealth = calculateBufferHealth(currentBufferLevel)
            val predictedUnderrun = predictBufferUnderrun(session)
            
            _bufferStatus.value = BufferStatus(
                currentBufferLevel = currentBufferLevel,
                targetBufferSize = targetBufferSize,
                bufferHealth = bufferHealth,
                isAggressiveMode = isAggressiveMode,
                predictedUnderrunTime = predictedUnderrun,
                bufferingStrategy = determineBufferingStrategy(bufferHealth)
            )
        }
    }
    
    private fun estimateCurrentBufferLevel(session: AdvancedStreamingEngine.StreamingSession): Long {
        // This would integrate with the actual media player to get real buffer levels
        // For now, simulate based on recent buffering events
        val recentEvents = session.bufferingHistory.takeLast(5)
        val averageBufferPercentage = recentEvents.map { it.bufferedPercentage }.average()
        
        return ((averageBufferPercentage / 100.0) * targetBufferSize).toLong()
    }
    
    private fun calculateBufferHealth(currentLevel: Long): Float {
        return (currentLevel.toFloat() / targetBufferSize).coerceIn(0f, 1f)
    }
    
    private fun predictBufferUnderrun(session: AdvancedStreamingEngine.StreamingSession): Long? {
        val recentBufferingEvents = session.bufferingHistory.takeLast(10)
        
        if (recentBufferingEvents.size < 3) return null
        
        val bufferingTrend = calculateBufferingTrend(recentBufferingEvents)
        
        return if (bufferingTrend < -0.1) { // Decreasing buffer health
            // Predict time until buffer underrun
            val currentLevel = estimateCurrentBufferLevel(session)
            val drainRate = -bufferingTrend * targetBufferSize // Estimated drain rate
            
            if (drainRate > 0) {
                (currentLevel / drainRate * 1000).toLong() // Time in milliseconds
            } else null
        } else null
    }
    
    private fun calculateBufferingTrend(events: List<AdvancedStreamingEngine.BufferingEvent>): Float {
        if (events.size < 2) return 0f
        
        val timeSpan = events.last().timestamp - events.first().timestamp
        if (timeSpan <= 0) return 0f
        
        val bufferChange = events.last().bufferedPercentage - events.first().bufferedPercentage
        return bufferChange.toFloat() / timeSpan * 1000 // Change per second
    }
    
    private fun determineBufferingStrategy(bufferHealth: Float): BufferingStrategy {
        return when {
            bufferHealth < 0.2f -> BufferingStrategy.EMERGENCY_PREBUFFER
            bufferHealth < 0.5f -> BufferingStrategy.AGGRESSIVE_PREBUFFER
            bufferHealth < 0.8f -> BufferingStrategy.NORMAL_PREBUFFER
            else -> BufferingStrategy.MINIMAL_PREBUFFER
        }
    }
    
    private suspend fun optimizeBufferSize() {
        currentSession?.let { session ->
            val networkCondition = getNetworkCondition(session)
            val recentBufferingEvents = session.bufferingHistory.takeLast(10)
            
            // Adjust buffer size based on network stability
            when (networkCondition.quality) {
                AdvancedStreamingEngine.NetworkQuality.POOR -> {
                    if (recentBufferingEvents.count { it.isBuffering } > 3) {
                        increaseBufferSize()
                    }
                }
                AdvancedStreamingEngine.NetworkQuality.EXCELLENT -> {
                    if (recentBufferingEvents.none { it.isBuffering }) {
                        decreaseBufferSize()
                    }
                }
                else -> {
                    // Maintain current buffer size for GOOD network
                }
            }
        }
    }
    
    private fun getNetworkCondition(session: AdvancedStreamingEngine.StreamingSession): NetworkCondition {
        return session.bufferingHistory.lastOrNull()?.networkCondition 
            ?: NetworkCondition(
                quality = AdvancedStreamingEngine.NetworkQuality.GOOD,
                bandwidth = 2_000_000L,
                latency = 100
            )
    }
    
    /**
     * Get prebuffer recommendations
     */
    fun getBufferRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val status = _bufferStatus.value
        
        when (status.bufferingStrategy) {
            BufferingStrategy.EMERGENCY_PREBUFFER -> {
                recommendations.add("Critical buffer level - increasing prebuffer aggressively")
                recommendations.add("Consider lowering video quality temporarily")
            }
            BufferingStrategy.AGGRESSIVE_PREBUFFER -> {
                recommendations.add("Low buffer detected - increasing prebuffer size")
            }
            BufferingStrategy.NORMAL_PREBUFFER -> {
                recommendations.add("Buffer levels optimal")
            }
            BufferingStrategy.MINIMAL_PREBUFFER -> {
                recommendations.add("Buffer healthy - using minimal prebuffering for efficiency")
            }
        }
        
        status.predictedUnderrunTime?.let { time ->
            recommendations.add("Predicted buffer underrun in ${time / 1000} seconds")
        }
        
        return recommendations
    }
    
    fun stop() {
        bufferJob?.cancel()
        bufferJob = null
        currentSession = null
    }
    
    fun release() {
        scope.cancel()
        stop()
    }
    
    // Data classes
    data class BufferStatus(
        val currentBufferLevel: Long = 0,
        val targetBufferSize: Long = DEFAULT_BUFFER_SIZE,
        val bufferHealth: Float = 0f,
        val isAggressiveMode: Boolean = false,
        val predictedUnderrunTime: Long? = null,
        val bufferingStrategy: BufferingStrategy = BufferingStrategy.NORMAL_PREBUFFER
    )
    
    enum class BufferingStrategy {
        MINIMAL_PREBUFFER,
        NORMAL_PREBUFFER,
        AGGRESSIVE_PREBUFFER,
        EMERGENCY_PREBUFFER
    }
}