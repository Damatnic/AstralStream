package com.astralplayer.nextplayer.data

/**
 * Network information and connection details
 */
data class NetworkInfo(
    val connectionType: ConnectionType,
    val isConnected: Boolean,
    val isMetered: Boolean,
    val bandwidth: Long, // in bps
    val latency: Long = 0L, // in ms
    val signalStrength: Int = 0, // 0-100%
    val networkName: String? = null
) {
    enum class ConnectionType {
        WIFI,
        CELLULAR,
        ETHERNET,
        VPN,
        BLUETOOTH,
        UNKNOWN
    }
    
    companion object {
        val DISCONNECTED = NetworkInfo(
            connectionType = ConnectionType.UNKNOWN,
            isConnected = false,
            isMetered = false,
            bandwidth = 0L
        )
        
        val DEFAULT_WIFI = NetworkInfo(
            connectionType = ConnectionType.WIFI,
            isConnected = true,
            isMetered = false,
            bandwidth = 100_000_000L, // 100 Mbps
            signalStrength = 80
        )
        
        val DEFAULT_CELLULAR = NetworkInfo(
            connectionType = ConnectionType.CELLULAR,
            isConnected = true,
            isMetered = true,
            bandwidth = 10_000_000L, // 10 Mbps
            signalStrength = 70
        )
    }
    
    /**
     * Get recommended video quality based on network
     */
    fun getRecommendedQuality(): VideoQuality {
        if (!isConnected) return VideoQuality.QUALITY_240P
        
        return when {
            bandwidth >= 15_000_000L -> VideoQuality.QUALITY_1080P
            bandwidth >= 5_000_000L -> VideoQuality.QUALITY_720P
            bandwidth >= 2_000_000L -> VideoQuality.QUALITY_480P
            bandwidth >= 1_000_000L -> VideoQuality.QUALITY_360P
            else -> VideoQuality.QUALITY_240P
        }
    }
    
    /**
     * Check if network supports high quality streaming
     */
    fun supportsHighQuality(): Boolean {
        return isConnected && bandwidth >= 5_000_000L && (!isMetered || connectionType == ConnectionType.WIFI)
    }
    
    /**
     * Get network speed category
     */
    fun getSpeedCategory(): SpeedCategory {
        return when {
            bandwidth >= 25_000_000L -> SpeedCategory.VERY_FAST
            bandwidth >= 10_000_000L -> SpeedCategory.FAST
            bandwidth >= 5_000_000L -> SpeedCategory.MEDIUM
            bandwidth >= 1_000_000L -> SpeedCategory.SLOW
            else -> SpeedCategory.VERY_SLOW
        }
    }
    
    enum class SpeedCategory {
        VERY_FAST,
        FAST,
        MEDIUM,
        SLOW,
        VERY_SLOW
    }
    
    /**
     * Get human readable connection description
     */
    fun getDisplayName(): String {
        if (!isConnected) return "No Connection"
        
        val speedMbps = bandwidth / 1_000_000L
        val meteredText = if (isMetered) " (Metered)" else ""
        
        return "${connectionType.name}${meteredText} - ${speedMbps}Mbps"
    }
}