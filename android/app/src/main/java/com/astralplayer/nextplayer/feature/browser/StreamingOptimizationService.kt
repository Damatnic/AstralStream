package com.astralplayer.nextplayer.feature.browser

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import android.net.ConnectivityManager
import android.content.Context
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Background service for optimizing streaming video playback
 * Monitors network conditions and adjusts streaming parameters in real-time
 */
class StreamingOptimizationService : Service() {
    
    companion object {
        private const val TAG = "StreamingOptimizationService"
        private const val OPTIMIZATION_INTERVAL = 5000L // 5 seconds
        
        // Intent actions
        const val ACTION_REGISTER_STREAM = "com.astralplayer.action.REGISTER_STREAM"
        const val ACTION_UNREGISTER_STREAM = "com.astralplayer.action.UNREGISTER_STREAM"
        const val ACTION_UPDATE_METRICS = "com.astralplayer.action.UPDATE_METRICS"
        const val ACTION_APPLY_OPTIMIZATION = "com.astralplayer.action.APPLY_OPTIMIZATION"
        
        // Intent extras
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_PROTOCOL = "protocol"
        const val EXTRA_BUFFER_LEVEL = "buffer_level"
        const val EXTRA_CURRENT_BITRATE = "current_bitrate"
        const val EXTRA_DROPPED_FRAMES = "dropped_frames"
        const val EXTRA_TARGET_BITRATE = "target_bitrate"
        const val EXTRA_BUFFER_DURATION = "buffer_duration"
        const val EXTRA_REBUFFER_THRESHOLD = "rebuffer_threshold"
        const val EXTRA_MAX_RETRY_ATTEMPTS = "max_retry_attempts"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeStreams = ConcurrentHashMap<String, StreamingSession>()
    private var optimizationJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "StreamingOptimizationService created")
        startOptimizationMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_REGISTER_STREAM -> registerStream(it)
                ACTION_UNREGISTER_STREAM -> unregisterStream(it)
                ACTION_UPDATE_METRICS -> updateStreamMetrics(it)
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "StreamingOptimizationService destroyed")
    }
    
    /**
     * Start monitoring and optimizing active streams
     */
    private fun startOptimizationMonitoring() {
        optimizationJob = serviceScope.launch {
            while (isActive) {
                try {
                    optimizeActiveStreams()
                    delay(OPTIMIZATION_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during optimization monitoring", e)
                }
            }
        }
    }
    
    /**
     * Register a new streaming session for optimization
     */
    private fun registerStream(intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: return
        val protocol = intent.getStringExtra(EXTRA_PROTOCOL) ?: "HTTP"
        
        val session = StreamingSession(
            sessionId = sessionId,
            streamUrl = streamUrl,
            protocol = protocol,
            startTime = System.currentTimeMillis(),
            lastOptimizationTime = System.currentTimeMillis()
        )
        
        activeStreams[sessionId] = session
        Log.d(TAG, "Registered streaming session: $sessionId")
        
        // Perform initial optimization
        serviceScope.launch {
            optimizeStream(session)
        }
    }
    
    /**
     * Unregister a streaming session
     */
    private fun unregisterStream(intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        activeStreams.remove(sessionId)
        Log.d(TAG, "Unregistered streaming session: $sessionId")
    }
    
    /**
     * Update streaming metrics for a session
     */
    private fun updateStreamMetrics(intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val bufferLevel = intent.getIntExtra(EXTRA_BUFFER_LEVEL, -1)
        val bitrate = intent.getIntExtra(EXTRA_CURRENT_BITRATE, -1)
        val droppedFrames = intent.getIntExtra(EXTRA_DROPPED_FRAMES, 0)
        
        activeStreams[sessionId]?.let { session ->
            session.currentBufferLevel = bufferLevel
            session.currentBitrate = bitrate
            session.droppedFrames = droppedFrames
            session.lastUpdateTime = System.currentTimeMillis()
        }
    }
    
    /**
     * Optimize all active streaming sessions
     */
    private suspend fun optimizeActiveStreams() = withContext(Dispatchers.IO) {
        val currentNetwork = getCurrentNetworkState()
        
        activeStreams.values.forEach { session ->
            try {
                // Skip if recently optimized
                if (System.currentTimeMillis() - session.lastOptimizationTime < OPTIMIZATION_INTERVAL) {
                    return@forEach
                }
                
                // Apply network-based optimizations
                val optimization = calculateOptimization(session, currentNetwork)
                applyOptimization(session, optimization)
                
                session.lastOptimizationTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(TAG, "Error optimizing session ${session.sessionId}", e)
            }
        }
    }
    
    /**
     * Optimize a specific streaming session
     */
    private suspend fun optimizeStream(session: StreamingSession) = withContext(Dispatchers.IO) {
        val currentNetwork = getCurrentNetworkState()
        val optimization = calculateOptimization(session, currentNetwork)
        applyOptimization(session, optimization)
    }
    
    /**
     * Calculate optimization parameters based on current conditions
     */
    private fun calculateOptimization(
        session: StreamingSession,
        networkState: NetworkState
    ): StreamingOptimization {
        
        // Base optimization on network type and quality
        val baseOptimization = when (networkState.type) {
            NetworkType.WIFI -> StreamingOptimization(
                targetBitrate = if (networkState.bandwidth > 20_000_000) Int.MAX_VALUE else networkState.bandwidth.toInt(),
                bufferDuration = 30_000, // 30 seconds
                rebufferThreshold = 5_000, // 5 seconds
                maxRetryAttempts = 5
            )
            NetworkType.MOBILE_5G -> StreamingOptimization(
                targetBitrate = minOf(networkState.bandwidth.toInt(), 15_000_000), // Max 15 Mbps
                bufferDuration = 20_000, // 20 seconds
                rebufferThreshold = 3_000, // 3 seconds
                maxRetryAttempts = 3
            )
            NetworkType.MOBILE_4G -> StreamingOptimization(
                targetBitrate = minOf(networkState.bandwidth.toInt(), 8_000_000), // Max 8 Mbps
                bufferDuration = 15_000, // 15 seconds
                rebufferThreshold = 2_000, // 2 seconds
                maxRetryAttempts = 3
            )
            NetworkType.MOBILE_3G -> StreamingOptimization(
                targetBitrate = minOf(networkState.bandwidth.toInt(), 2_000_000), // Max 2 Mbps
                bufferDuration = 10_000, // 10 seconds
                rebufferThreshold = 1_000, // 1 second
                maxRetryAttempts = 2
            )
            else -> StreamingOptimization(
                targetBitrate = 4_000_000, // Default 4 Mbps
                bufferDuration = 15_000,
                rebufferThreshold = 2_000,
                maxRetryAttempts = 3
            )
        }
        
        // Adjust based on current performance
        return if (session.droppedFrames > 100) {
            // Many dropped frames, reduce quality
            baseOptimization.copy(
                targetBitrate = (baseOptimization.targetBitrate * 0.7).toInt()
            )
        } else if (session.currentBufferLevel < 2000) {
            // Low buffer, be more conservative
            baseOptimization.copy(
                targetBitrate = (baseOptimization.targetBitrate * 0.8).toInt(),
                bufferDuration = baseOptimization.bufferDuration + 5000
            )
        } else {
            baseOptimization
        }
    }
    
    /**
     * Apply optimization to the streaming session
     */
    private fun applyOptimization(session: StreamingSession, optimization: StreamingOptimization) {
        // Send optimization broadcast to the player
        val intent = Intent(ACTION_APPLY_OPTIMIZATION).apply {
            putExtra(EXTRA_SESSION_ID, session.sessionId)
            putExtra(EXTRA_TARGET_BITRATE, optimization.targetBitrate)
            putExtra(EXTRA_BUFFER_DURATION, optimization.bufferDuration)
            putExtra(EXTRA_REBUFFER_THRESHOLD, optimization.rebufferThreshold)
            putExtra(EXTRA_MAX_RETRY_ATTEMPTS, optimization.maxRetryAttempts)
        }
        
        sendBroadcast(intent)
        
        Log.d(TAG, "Applied optimization to session ${session.sessionId}: " +
                "bitrate=${optimization.targetBitrate}, buffer=${optimization.bufferDuration}ms")
    }
    
    /**
     * Get current network state and capabilities
     */
    private fun getCurrentNetworkState(): NetworkState {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            val type = when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> NetworkType.WIFI
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> {
                    // Determine cellular type based on downstream bandwidth
                    val bandwidth = capabilities.linkDownstreamBandwidthKbps * 1000L
                    when {
                        bandwidth > 10_000_000 -> NetworkType.MOBILE_5G
                        bandwidth > 3_000_000 -> NetworkType.MOBILE_4G
                        else -> NetworkType.MOBILE_3G
                    }
                }
                else -> NetworkType.UNKNOWN
            }
            
            val bandwidth = capabilities?.linkDownstreamBandwidthKbps?.times(1000L) ?: 0L
            val isMetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
            
            NetworkState(type, bandwidth, isMetered)
        } else {
            // Fallback for older Android versions
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            val type = when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.MOBILE_4G
                else -> NetworkType.UNKNOWN
            }
            NetworkState(type, 5_000_000L, type != NetworkType.WIFI) // Default 5 Mbps
        }
    }
    
    // Data classes
    data class StreamingSession(
        val sessionId: String,
        val streamUrl: String,
        val protocol: String,
        val startTime: Long,
        var lastOptimizationTime: Long,
        var lastUpdateTime: Long = System.currentTimeMillis(),
        var currentBufferLevel: Int = -1,
        var currentBitrate: Int = -1,
        var droppedFrames: Int = 0
    )
    
    data class StreamingOptimization(
        val targetBitrate: Int,
        val bufferDuration: Int,
        val rebufferThreshold: Int,
        val maxRetryAttempts: Int
    )
    
    data class NetworkState(
        val type: NetworkType,
        val bandwidth: Long, // in bps
        val isMetered: Boolean
    )
    
    enum class NetworkType {
        WIFI, MOBILE_5G, MOBILE_4G, MOBILE_3G, UNKNOWN
    }
    
}