package com.astralplayer.core.error

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Handles network error recovery with smart retry strategies
 */
@Singleton
class NetworkErrorRecovery @Inject constructor(
    private val context: Context
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkState = MutableStateFlow(NetworkState.UNKNOWN)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val _isOnline = MutableStateFlow(checkNetworkAvailability())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    init {
        registerNetworkCallback()
    }
    
    /**
     * Execute network operation with automatic retry
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 30000,
        factor: Double = 2.0,
        onRetry: ((attempt: Int, delay: Long) -> Unit)? = null,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                // Check network before attempting
                if (!isOnline.value) {
                    throw NetworkUnavailableException("No network connection")
                }
                
                return block()
            } catch (e: Exception) {
                lastException = e
                
                if (!isRetryableError(e) || attempt == maxRetries - 1) {
                    throw e
                }
                
                // Calculate exponential backoff delay
                val delay = min(currentDelay, maxDelayMs)
                onRetry?.invoke(attempt + 1, delay)
                
                Timber.d("Retrying network operation (attempt ${attempt + 1}/$maxRetries) after ${delay}ms")
                
                delay(delay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
        
        throw lastException ?: IOException("Network operation failed")
    }
    
    /**
     * Wait for network to become available
     */
    suspend fun waitForNetwork(timeoutMs: Long = 30000): Boolean {
        if (isOnline.value) return true
        
        return withTimeoutOrNull(timeoutMs) {
            while (!isOnline.value) {
                delay(500)
            }
            true
        } ?: false
    }
    
    /**
     * Check if error is retryable
     */
    private fun isRetryableError(error: Exception): Boolean {
        return when (error) {
            is IOException,
            is NetworkUnavailableException,
            is java.net.SocketTimeoutException,
            is java.net.UnknownHostException,
            is javax.net.ssl.SSLException -> true
            else -> false
        }
    }
    
    /**
     * Check current network availability
     */
    private fun checkNetworkAvailability(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Register network state callback
     */
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(
            networkRequest,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    _networkState.value = NetworkState.CONNECTED
                    Timber.d("Network available")
                }
                
                override fun onLost(network: Network) {
                    _isOnline.value = false
                    _networkState.value = NetworkState.DISCONNECTED
                    Timber.d("Network lost")
                }
                
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val isValidated = networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )
                    
                    _networkState.value = when {
                        !isValidated -> NetworkState.CONNECTING
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkState.WIFI
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkState.CELLULAR
                        else -> NetworkState.CONNECTED
                    }
                }
            }
        )
    }
    
    /**
     * Calculate retry delay with jitter
     */
    fun calculateRetryDelay(
        attempt: Int,
        baseDelayMs: Long = 1000,
        maxDelayMs: Long = 30000
    ): Long {
        val exponentialDelay = baseDelayMs * 2.0.pow(attempt - 1).toLong()
        val jitter = (Math.random() * 0.3 * exponentialDelay).toLong()
        return min(exponentialDelay + jitter, maxDelayMs)
    }
}

/**
 * Network connection states
 */
enum class NetworkState {
    UNKNOWN,
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    WIFI,
    CELLULAR
}

/**
 * Exception for network unavailable
 */
class NetworkUnavailableException(message: String) : IOException(message)