package com.astralplayer.nextplayer.feature.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Advanced network manager for AstralStream
 * Handles complex networking scenarios, bandwidth adaptation, and adult site compatibility
 */
class AdvancedNetworkManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AdvancedNetworkManager"
        
        // Network quality thresholds (in Mbps)
        private const val QUALITY_EXCELLENT_THRESHOLD = 25.0
        private const val QUALITY_GOOD_THRESHOLD = 10.0
        private const val QUALITY_FAIR_THRESHOLD = 5.0
        private const val QUALITY_POOR_THRESHOLD = 1.0
        
        
        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // Network state flows
    private val _networkQuality = MutableStateFlow(NetworkQuality.UNKNOWN)
    val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()
    
    private val _connectionType = MutableStateFlow(ConnectionType.UNKNOWN)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()
    
    private val _bandwidth = MutableStateFlow(0.0)
    val bandwidth: StateFlow<Double> = _bandwidth.asStateFlow()
    
    // Cookie and session management
    private val cookieJar = PersistentCookieJar()
    private val sessionManager = SessionManager()
    
    // Advanced OkHttp client with custom interceptors
    val httpClient: OkHttpClient by lazy {
        createAdvancedHttpClient()
    }
    
    init {
        startNetworkMonitoring()
    }
    
    /**
     * Create advanced HTTP client with all interceptors and configurations
     */
    private fun createAdvancedHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("$TAG-HTTP", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(cookieJar)
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(RefererInterceptor())
            .addInterceptor(CorsHandlerInterceptor())
            .addInterceptor(RetryInterceptor())
            .addInterceptor(BandwidthMonitorInterceptor())
            .addInterceptor(SessionInterceptor(sessionManager))
            .addNetworkInterceptor(CacheInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .build()
    }
    
    /**
     * Start monitoring network conditions
     */
    private fun startNetworkMonitoring() {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                updateNetworkInfo()
            }
            
            override fun onLost(network: android.net.Network) {
                _connectionType.value = ConnectionType.NONE
                _networkQuality.value = NetworkQuality.NO_CONNECTION
            }
            
            override fun onCapabilitiesChanged(
                network: android.net.Network, 
                networkCapabilities: NetworkCapabilities
            ) {
                updateNetworkInfo(networkCapabilities)
            }
        }
        
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        updateNetworkInfo()
    }
    
    /**
     * Update network information and quality assessment
     */
    private fun updateNetworkInfo(capabilities: NetworkCapabilities? = null) {
        val networkCapabilities = capabilities ?: connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork
        )
        
        if (networkCapabilities == null) {
            _connectionType.value = ConnectionType.NONE
            _networkQuality.value = NetworkQuality.NO_CONNECTION
            return
        }
        
        // Determine connection type
        _connectionType.value = when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.OTHER
        }
        
        // Estimate bandwidth and quality
        val downstreamBandwidth = networkCapabilities.linkDownstreamBandwidthKbps / 1000.0 // Convert to Mbps
        _bandwidth.value = downstreamBandwidth
        
        _networkQuality.value = when {
            downstreamBandwidth >= QUALITY_EXCELLENT_THRESHOLD -> NetworkQuality.EXCELLENT
            downstreamBandwidth >= QUALITY_GOOD_THRESHOLD -> NetworkQuality.GOOD
            downstreamBandwidth >= QUALITY_FAIR_THRESHOLD -> NetworkQuality.FAIR
            downstreamBandwidth >= QUALITY_POOR_THRESHOLD -> NetworkQuality.POOR
            else -> NetworkQuality.VERY_POOR
        }
        
        Log.d(TAG, "Network updated - Type: ${_connectionType.value}, Quality: ${_networkQuality.value}, Bandwidth: ${downstreamBandwidth}Mbps")
    }
    
    /**
     * Get recommended video quality based on network conditions
     */
    fun getRecommendedVideoQuality(): VideoQuality {
        return when (_networkQuality.value) {
            NetworkQuality.EXCELLENT -> VideoQuality.ULTRA_HD_4K
            NetworkQuality.GOOD -> VideoQuality.FULL_HD_1080P
            NetworkQuality.FAIR -> VideoQuality.HD_720P
            NetworkQuality.POOR -> VideoQuality.SD_480P
            NetworkQuality.VERY_POOR -> VideoQuality.SD_360P
            NetworkQuality.NO_CONNECTION -> VideoQuality.OFFLINE
            NetworkQuality.UNKNOWN -> VideoQuality.AUTO
        }
    }
    
    /**
     * Perform network speed test
     */
    suspend fun performSpeedTest(): NetworkSpeedResult {
        return withContext(Dispatchers.IO) {
            try {
                val testUrl = "https://httpbin.org/bytes/1048576" // 1MB test file
                val startTime = System.currentTimeMillis()
                
                val request = Request.Builder()
                    .url(testUrl)
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val bytes = response.body?.bytes()?.size ?: 0
                val endTime = System.currentTimeMillis()
                
                val durationSeconds = (endTime - startTime) / 1000.0
                val speedMbps = (bytes * 8) / (durationSeconds * 1_000_000) // Convert to Mbps
                
                NetworkSpeedResult(
                    downloadSpeedMbps = speedMbps,
                    latencyMs = endTime - startTime,
                    testSizeBytes = bytes,
                    success = true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Speed test failed", e)
                NetworkSpeedResult(
                    downloadSpeedMbps = 0.0,
                    latencyMs = -1,
                    testSizeBytes = 0,
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Check if streaming is advisable based on current network conditions
     */
    fun isStreamingAdvisable(requiredQuality: VideoQuality): Boolean {
        val requiredBandwidth = when (requiredQuality) {
            VideoQuality.ULTRA_HD_4K -> 25.0
            VideoQuality.FULL_HD_1080P -> 8.0
            VideoQuality.HD_720P -> 5.0
            VideoQuality.SD_480P -> 2.5
            VideoQuality.SD_360P -> 1.0
            VideoQuality.SD_240P -> 0.5
            else -> 1.0
        }
        
        return _bandwidth.value >= requiredBandwidth
    }
    
    /**
     * Get site-specific configuration for adult content sites
     */
    fun getSiteConfiguration(domain: String): SiteConfiguration {
        val cleanDomain = domain.removePrefix("www.").lowercase()
        
        return SiteConfiguration(
            domain = cleanDomain,
            userAgent = UserAgentInterceptor.ADULT_SITE_USER_AGENTS[cleanDomain] ?: getDefaultUserAgent(),
            referer = "https://$cleanDomain/",
            headers = getSiteSpecificHeaders(cleanDomain),
            requiresCookies = isAdultSite(cleanDomain),
            maxRetries = if (isAdultSite(cleanDomain)) MAX_RETRIES else 1,
            timeout = if (isAdultSite(cleanDomain)) 60000 else 30000
        )
    }
    
    private fun getSiteSpecificHeaders(domain: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        when (domain) {
            "pornhub.com" -> {
                headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                headers["Accept-Language"] = "en-US,en;q=0.5"
                headers["Accept-Encoding"] = "gzip, deflate, br"
                headers["DNT"] = "1"
                headers["Connection"] = "keep-alive"
                headers["Upgrade-Insecure-Requests"] = "1"
            }
            
            "xvideos.com", "xnxx.com" -> {
                headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                headers["Accept-Language"] = "en-US,en;q=0.5"
                headers["Accept-Encoding"] = "gzip, deflate"
                headers["Connection"] = "keep-alive"
            }
            
            "spankbang.com" -> {
                headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
                headers["Accept-Language"] = "en-US,en;q=0.9"
                headers["Cache-Control"] = "no-cache"
                headers["Pragma"] = "no-cache"
            }
        }
        
        return headers
    }
    
    private fun isAdultSite(domain: String): Boolean {
        return UserAgentInterceptor.ADULT_SITE_USER_AGENTS.containsKey(domain)
    }
    
    private fun getDefaultUserAgent(): String {
        return "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
    
    /**
     * Enhanced request with automatic retries and error handling
     */
    suspend fun executeRequest(
        request: Request,
        maxRetries: Int = MAX_RETRIES
    ): NetworkResult {
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            
            repeat(maxRetries + 1) { attempt ->
                try {
                    val response = httpClient.newCall(request).execute()
                    
                    return@withContext if (response.isSuccessful) {
                        NetworkResult.Success(response)
                    } else {
                        NetworkResult.HttpError(response.code, response.message)
                    }
                    
                } catch (e: IOException) {
                    lastException = e
                    if (attempt < maxRetries) {
                        Log.w(TAG, "Request failed (attempt ${attempt + 1}/$maxRetries), retrying...", e)
                        kotlinx.coroutines.delay(RETRY_DELAY_MS * (attempt + 1))
                    }
                } catch (e: Exception) {
                    return@withContext NetworkResult.Error(e)
                }
            }
            
            NetworkResult.Error(lastException ?: IOException("Request failed after $maxRetries retries"))
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        cookieJar.clear()
        sessionManager.clearAll()
    }
}

/**
 * Custom interceptors for advanced networking
 */
private class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val host = originalRequest.url.host
        
        val userAgent = ADULT_SITE_USER_AGENTS[host] ?: 
            "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .build()
        
        return chain.proceed(requestWithUserAgent)
    }
    
    companion object {
        val ADULT_SITE_USER_AGENTS = mapOf(
            "pornhub.com" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "xvideos.com" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "xnxx.com" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "spankbang.com" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "redtube.com" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/120.0.0.0",
            "youporn.com" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
        )
    }
}

private class RefererInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val host = originalRequest.url.host
        
        val requestWithReferer = originalRequest.newBuilder()
            .header("Referer", "https://$host/")
            .build()
        
        return chain.proceed(requestWithReferer)
    }
}

private class CorsHandlerInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val requestWithCors = originalRequest.newBuilder()
            .header("Origin", "https://${originalRequest.url.host}")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Dest", "empty")
            .build()
        
        return chain.proceed(requestWithCors)
    }
}

private class RetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        
        var tryCount = 0
        while (!response.isSuccessful && tryCount < 2) {
            tryCount++
            response.close()
            Thread.sleep(1000L * tryCount) // Progressive delay
            response = chain.proceed(request)
        }
        
        return response
    }
}

private class BandwidthMonitorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(chain.request())
        val endTime = System.currentTimeMillis()
        
        val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
        val duration = (endTime - startTime) / 1000.0
        
        if (duration > 0 && contentLength > 0) {
            val speedKbps = (contentLength / duration) / 1024.0
            Log.d("BandwidthMonitor", "Transfer speed: ${"%.2f".format(speedKbps)} KB/s")
        }
        
        return response
    }
}

private class SessionInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val host = originalRequest.url.host
        
        val sessionId = sessionManager.getSessionId(host)
        val requestWithSession = if (sessionId != null) {
            originalRequest.newBuilder()
                .header("X-Session-ID", sessionId)
                .build()
        } else {
            originalRequest
        }
        
        val response = chain.proceed(requestWithSession)
        
        // Extract and store new session ID if present
        response.header("Set-Session-ID")?.let { newSessionId ->
            sessionManager.setSessionId(host, newSessionId)
        }
        
        return response
    }
}

private class CacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        
        return response.newBuilder()
            .header("Cache-Control", "public, max-age=300") // 5 minutes cache
            .build()
    }
}

/**
 * Cookie management for persistent sessions
 */
private class PersistentCookieJar : CookieJar {
    private val cookies = mutableMapOf<String, MutableList<Cookie>>()
    
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        this.cookies[host] = cookies.toMutableList()
    }
    
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        return cookies[host] ?: emptyList()
    }
    
    fun clear() {
        cookies.clear()
    }
}

/**
 * Session management for adult sites
 */
private class SessionManager {
    private val sessions = mutableMapOf<String, String>()
    
    fun getSessionId(host: String): String? = sessions[host]
    
    fun setSessionId(host: String, sessionId: String) {
        sessions[host] = sessionId
    }
    
    fun clearAll() {
        sessions.clear()
    }
}

/**
 * Data classes for network management
 */
data class SiteConfiguration(
    val domain: String,
    val userAgent: String,
    val referer: String,
    val headers: Map<String, String>,
    val requiresCookies: Boolean,
    val maxRetries: Int,
    val timeout: Int
)

data class NetworkSpeedResult(
    val downloadSpeedMbps: Double,
    val latencyMs: Long,
    val testSizeBytes: Int,
    val success: Boolean,
    val error: String? = null
)

sealed class NetworkResult {
    data class Success(val response: Response) : NetworkResult()
    data class HttpError(val code: Int, val message: String) : NetworkResult()
    data class Error(val exception: Exception) : NetworkResult()
}

enum class NetworkQuality {
    NO_CONNECTION, VERY_POOR, POOR, FAIR, GOOD, EXCELLENT, UNKNOWN
}

enum class ConnectionType {
    NONE, WIFI, CELLULAR, ETHERNET, OTHER, UNKNOWN
}

enum class VideoQuality {
    OFFLINE, AUTO, SD_240P, SD_360P, SD_480P, HD_720P, FULL_HD_1080P, ULTRA_HD_4K
}