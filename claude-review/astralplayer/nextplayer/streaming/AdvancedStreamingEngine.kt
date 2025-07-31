package com.astralplayer.nextplayer.streaming

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class AdvancedStreamingEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val adaptiveBitrateController: AdaptiveBitrateController,
    private val intelligentPreBuffer: IntelligentPreBufferService,
    private val downloadManager: OfflineDownloadManager,
    private val p2pStreamingService: P2PStreamingService
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _streamingState = MutableStateFlow<StreamingState>(StreamingState.Idle)
    val streamingState: StateFlow<StreamingState> = _streamingState.asStateFlow()
    
    private val _bufferHealth = MutableStateFlow(BufferHealth())
    val bufferHealth: StateFlow<BufferHealth> = _bufferHealth.asStateFlow()
    
    private var currentStreamingSession: StreamingSession? = null
    private var streamingCallbacks: StreamingCallbacks? = null
    
    suspend fun initializeStreaming(
        mediaItem: MediaItem,
        callbacks: StreamingCallbacks
    ): StreamingSession {
        return withContext(Dispatchers.Main) {
            try {
                _streamingState.value = StreamingState.Initializing
                streamingCallbacks = callbacks
                
                val session = StreamingSession(
                    id = UUID.randomUUID().toString(),
                    mediaItem = mediaItem,
                    networkCondition = networkMonitor.getCurrentCondition(),
                    startTime = System.currentTimeMillis()
                )
                
                currentStreamingSession = session
                
                // Initialize adaptive bitrate based on network
                adaptiveBitrateController.initialize(session, networkMonitor.getCurrentCondition())
                
                // Start intelligent pre-buffering
                intelligentPreBuffer.startPreBuffering(session)
                
                // Check for P2P opportunities
                if (p2pStreamingService.isAvailable()) {
                    p2pStreamingService.checkForLocalPeers(mediaItem.localConfiguration?.uri.toString())
                }
                
                _streamingState.value = StreamingState.Active(session)
                session
                
            } catch (e: Exception) {
                _streamingState.value = StreamingState.Error(e.message ?: "Streaming initialization failed")
                throw e
            }
        }
    }
    
    fun optimizeForNetworkCondition(condition: NetworkCondition) {
        currentStreamingSession?.let { session ->
            adaptiveBitrateController.adaptToNetworkCondition(condition)
            
            when (condition.quality) {
                NetworkQuality.POOR -> {
                    // Switch to aggressive buffering and lower quality
                    intelligentPreBuffer.setAggressiveMode(true)
                    streamingCallbacks?.onQualityRecommendation(VideoQuality.SD_480P)
                }
                NetworkQuality.GOOD -> {
                    // Balanced approach
                    intelligentPreBuffer.setAggressiveMode(false)
                    streamingCallbacks?.onQualityRecommendation(VideoQuality.HD_720P)
                }
                NetworkQuality.EXCELLENT -> {
                    // Maximum quality and minimal buffering
                    intelligentPreBuffer.setAggressiveMode(false)
                    streamingCallbacks?.onQualityRecommendation(VideoQuality.FHD_1080P)
                }
            }
        }
    }
    
    fun handleBufferingEvent(bufferedPercentage: Int, isBuffering: Boolean) {
        currentStreamingSession?.let { session ->
            val bufferingEvent = BufferingEvent(
                timestamp = System.currentTimeMillis(),
                bufferedPercentage = bufferedPercentage,
                isBuffering = isBuffering,
                networkCondition = networkMonitor.getCurrentCondition()
            )
            
            session.bufferingHistory.add(bufferingEvent)
            
            // Update buffer health
            _bufferHealth.value = BufferHealth(
                currentBufferPercentage = bufferedPercentage,
                isBuffering = isBuffering,
                bufferingFrequency = calculateBufferingFrequency(session),
                averageBufferLevel = calculateAverageBufferLevel(session)
            )
            
            // Analyze buffering patterns for future optimization
            if (isBuffering) {
                analyzeBufferingCause(session)
            }
        }
    }
    
    private fun analyzeBufferingCause(session: StreamingSession) {
        val recentBufferingEvents = session.bufferingHistory.takeLast(5)
        
        // Check if buffering is due to network issues
        if (recentBufferingEvents.all { it.networkCondition.quality == NetworkQuality.POOR }) {
            streamingCallbacks?.onBufferingAnalysis(
                BufferingCause.POOR_NETWORK,
                "Consider lowering video quality for smoother playback"
            )
        }
        
        // Check for frequent buffering patterns
        val bufferingCount = recentBufferingEvents.count { it.isBuffering }
        if (bufferingCount > 3) {
            streamingCallbacks?.onBufferingAnalysis(
                BufferingCause.INSUFFICIENT_PREBUFFER,
                "Increasing buffer size for more stable playback"
            )
            intelligentPreBuffer.increaseBufferSize()
        }
    }
    
    private fun calculateBufferingFrequency(session: StreamingSession): Float {
        val totalEvents = session.bufferingHistory.size
        val bufferingEvents = session.bufferingHistory.count { it.isBuffering }
        return if (totalEvents > 0) bufferingEvents.toFloat() / totalEvents else 0f
    }
    
    private fun calculateAverageBufferLevel(session: StreamingSession): Float {
        return session.bufferingHistory.map { it.bufferedPercentage }.average().toFloat()
    }
    
    /**
     * Get streaming statistics
     */
    fun getStreamingStats(): StreamingStats? {
        return currentStreamingSession?.let { session ->
            StreamingStats(
                sessionId = session.id,
                duration = System.currentTimeMillis() - session.startTime,
                totalBufferingEvents = session.bufferingHistory.count { it.isBuffering },
                averageBufferLevel = calculateAverageBufferLevel(session),
                networkSwitches = session.networkSwitchCount,
                qualitySwitches = session.qualitySwitchCount,
                bytesTransferred = session.bytesTransferred,
                p2pBytesReceived = session.p2pBytesReceived
            )
        }
    }
    
    /**
     * Enable/disable P2P streaming
     */
    fun setP2PEnabled(enabled: Boolean) {
        if (enabled) {
            p2pStreamingService.enable()
        } else {
            p2pStreamingService.disable()
        }
    }
    
    /**
     * Start offline download
     */
    suspend fun startDownload(
        mediaItem: MediaItem,
        quality: VideoQuality,
        onProgress: (Float) -> Unit
    ): DownloadResult {
        return downloadManager.startDownload(mediaItem, quality, onProgress)
    }
    
    /**
     * Release resources
     */
    fun release() {
        scope.cancel()
        currentStreamingSession = null
        streamingCallbacks = null
        intelligentPreBuffer.stop()
        p2pStreamingService.disconnect()
    }
    
    // Data classes
    sealed class StreamingState {
        object Idle : StreamingState()
        object Initializing : StreamingState()
        data class Active(val session: StreamingSession) : StreamingState()
        data class Error(val message: String) : StreamingState()
    }
    
    data class StreamingSession(
        val id: String,
        val mediaItem: MediaItem,
        val networkCondition: NetworkCondition,
        val startTime: Long,
        val bufferingHistory: MutableList<BufferingEvent> = mutableListOf(),
        var networkSwitchCount: Int = 0,
        var qualitySwitchCount: Int = 0,
        var bytesTransferred: Long = 0,
        var p2pBytesReceived: Long = 0
    )
    
    data class BufferingEvent(
        val timestamp: Long,
        val bufferedPercentage: Int,
        val isBuffering: Boolean,
        val networkCondition: NetworkCondition
    )
    
    data class BufferHealth(
        val currentBufferPercentage: Int = 0,
        val isBuffering: Boolean = false,
        val bufferingFrequency: Float = 0f,
        val averageBufferLevel: Float = 0f
    ) {
        val isHealthy: Boolean
            get() = !isBuffering && bufferingFrequency < 0.1f && averageBufferLevel > 50f
    }
    
    data class StreamingStats(
        val sessionId: String,
        val duration: Long,
        val totalBufferingEvents: Int,
        val averageBufferLevel: Float,
        val networkSwitches: Int,
        val qualitySwitches: Int,
        val bytesTransferred: Long,
        val p2pBytesReceived: Long
    )
    
    enum class BufferingCause {
        POOR_NETWORK,
        INSUFFICIENT_PREBUFFER,
        HIGH_QUALITY_DEMAND,
        SERVER_ISSUES
    }
    
    enum class NetworkQuality {
        POOR, GOOD, EXCELLENT
    }
    
    enum class VideoQuality {
        SD_480P, HD_720P, FHD_1080P, UHD_4K
    }
    
    data class NetworkCondition(
        val quality: NetworkQuality,
        val bandwidth: Long,
        val latency: Int
    )
    
    interface StreamingCallbacks {
        fun onQualityRecommendation(quality: VideoQuality)
        fun onBufferingAnalysis(cause: BufferingCause, recommendation: String)
        fun onNetworkConditionChanged(condition: NetworkCondition)
        fun onP2PConnected(peerId: String)
    }
    
    data class DownloadResult(
        val success: Boolean,
        val filePath: String? = null,
        val error: String? = null
    )
}