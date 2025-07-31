package com.astralplayer.nextplayer.streaming

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkCondition = MutableStateFlow(NetworkCondition(
        quality = AdvancedStreamingEngine.NetworkQuality.GOOD,
        bandwidth = 0L,
        latency = 0
    ))
    val networkCondition: StateFlow<NetworkCondition> = _networkCondition.asStateFlow()
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            measureNetworkCondition()
        }
        
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            measureNetworkCondition()
        }
        
        override fun onLost(network: Network) {
            super.onLost(network)
            _networkCondition.value = NetworkCondition(
                quality = AdvancedStreamingEngine.NetworkQuality.POOR,
                bandwidth = 0L,
                latency = Int.MAX_VALUE
            )
        }
    }
    
    init {
        startMonitoring()
    }
    
    private fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Initial measurement
        measureNetworkCondition()
        
        // Periodic measurements
        scope.launch {
            while (isActive) {
                delay(30000) // Every 30 seconds
                measureNetworkCondition()
            }
        }
    }
    
    private fun measureNetworkCondition() {
        scope.launch {
            try {
                val bandwidth = estimateBandwidth()
                val latency = measureLatency()
                
                val quality = when {
                    bandwidth > 5_000_000 && latency < 100 -> AdvancedStreamingEngine.NetworkQuality.EXCELLENT
                    bandwidth > 1_000_000 && latency < 300 -> AdvancedStreamingEngine.NetworkQuality.GOOD
                    else -> AdvancedStreamingEngine.NetworkQuality.POOR
                }
                
                _networkCondition.value = NetworkCondition(
                    quality = quality,
                    bandwidth = bandwidth,
                    latency = latency
                )
                
            } catch (e: Exception) {
                _networkCondition.value = NetworkCondition(
                    quality = AdvancedStreamingEngine.NetworkQuality.POOR,
                    bandwidth = 0L,
                    latency = Int.MAX_VALUE
                )
            }
        }
    }
    
    private suspend fun estimateBandwidth(): Long {
        return withContext(Dispatchers.IO) {
            try {
                // Simple bandwidth estimation by downloading a small test file
                val startTime = System.currentTimeMillis()
                val url = java.net.URL("https://httpbin.org/bytes/1024") // 1KB test
                val connection = url.openConnection()
                connection.connect()
                
                val inputStream = connection.getInputStream()
                val buffer = ByteArray(1024)
                var totalBytes = 0
                
                while (inputStream.read(buffer) != -1) {
                    totalBytes += buffer.size
                }
                
                val endTime = System.currentTimeMillis()
                val durationMs = endTime - startTime
                
                inputStream.close()
                
                // Calculate bandwidth in bytes per second
                if (durationMs > 0) {
                    (totalBytes * 1000L) / durationMs
                } else {
                    1_000_000L // Default to 1 Mbps
                }
            } catch (e: Exception) {
                500_000L // Default to 500 Kbps on error
            }
        }
    }
    
    private suspend fun measureLatency(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val url = java.net.URL("https://httpbin.org/status/200")
                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.connect()
                
                val endTime = System.currentTimeMillis()
                (endTime - startTime).toInt()
            } catch (e: Exception) {
                1000 // Default to 1 second latency on error
            }
        }
    }
    
    fun getCurrentCondition(): NetworkCondition {
        return _networkCondition.value
    }
    
    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    fun getConnectionType(): ConnectionType {
        val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.OTHER
        }
    }
    
    fun release() {
        scope.cancel()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
    
    enum class ConnectionType {
        NONE, WIFI, CELLULAR, ETHERNET, OTHER
    }
}

typealias NetworkCondition = AdvancedStreamingEngine.NetworkCondition