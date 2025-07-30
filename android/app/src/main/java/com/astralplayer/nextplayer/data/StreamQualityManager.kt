package com.astralplayer.nextplayer.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.util.Log
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages video quality selection and adaptive streaming
 */
class StreamQualityManager(
    private val context: Context,
    private val exoPlayer: ExoPlayer
) {
    companion object {
        private const val TAG = "StreamQualityManager"
    }
    
    private val _availableQualities = MutableStateFlow<List<VideoQuality>>(emptyList())
    val availableQualities: StateFlow<List<VideoQuality>> = _availableQualities.asStateFlow()
    
    private val _currentQuality = MutableStateFlow<VideoQuality?>(null)
    val currentQuality: StateFlow<VideoQuality?> = _currentQuality.asStateFlow()
    
    private val _qualitySettings = MutableStateFlow(QualitySettings())
    val qualitySettings: StateFlow<QualitySettings> = _qualitySettings.asStateFlow()
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Update available qualities from ExoPlayer tracks
     */
    fun updateAvailableQualities(tracks: Tracks) {
        val qualities = mutableListOf<VideoQuality>()
        
        // Add auto quality option
        qualities.add(VideoQuality.AUTO)
        
        // Extract video qualities from tracks
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val quality = createVideoQualityFromFormat(format)
                    if (!qualities.any { it.height == quality.height }) {
                        qualities.add(quality)
                    }
                }
            }
        }
        
        // Sort by quality (highest first)
        val sortedQualities = qualities.sortedWith { a, b ->
            when {
                a.isAdaptive && !b.isAdaptive -> -1
                !a.isAdaptive && b.isAdaptive -> 1
                else -> b.height.compareTo(a.height)
            }
        }
        
        _availableQualities.value = sortedQualities
        Log.d(TAG, "Updated available qualities: ${sortedQualities.map { it.label }}")
    }
    
    /**
     * Select a specific video quality
     */
    fun selectQuality(quality: VideoQuality) {
        try {
            val trackSelector = exoPlayer.trackSelector as? DefaultTrackSelector
            if (trackSelector != null) {
                val parametersBuilder = trackSelector.buildUponParameters()
                
                if (quality.isAdaptive) {
                    // Enable adaptive streaming
                    parametersBuilder
                        .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                        .setMaxVideoBitrate(Int.MAX_VALUE)
                        .clearVideoSizeConstraints()
                } else {
                    // Set specific quality constraints
                    parametersBuilder
                        .setMaxVideoSize(quality.width, quality.height)
                        .setMaxVideoBitrate(quality.bitrate)
                        .setMinVideoSize(quality.width, quality.height)
                }
                
                trackSelector.setParameters(parametersBuilder)
                _currentQuality.value = quality
                
                Log.d(TAG, "Selected quality: ${quality.label}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting quality: ${quality.label}", e)
        }
    }
    
    /**
     * Enable or disable data saver mode
     */
    fun enableDataSaverMode(enabled: Boolean) {
        try {
            val trackSelector = exoPlayer.trackSelector as? DefaultTrackSelector
            if (trackSelector != null) {
                val parametersBuilder = trackSelector.buildUponParameters()
                
                if (enabled) {
                    // Limit to lower quality for data saving
                    parametersBuilder
                        .setMaxVideoSize(854, 480) // 480p max
                        .setMaxVideoBitrate(1_200_000) // 1.2 Mbps max
                        .setForceLowestBitrate(true)
                } else {
                    // Remove data saver restrictions
                    parametersBuilder
                        .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                        .setMaxVideoBitrate(Int.MAX_VALUE)
                        .setForceLowestBitrate(false)
                }
                
                trackSelector.setParameters(parametersBuilder)
                
                val settings = _qualitySettings.value.copy(dataSaverEnabled = enabled)
                _qualitySettings.value = settings
                
                Log.d(TAG, "Data saver mode ${if (enabled) "enabled" else "disabled"}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting data saver mode", e)
        }
    }
    
    /**
     * Get current network information
     */
    fun getNetworkInfo(): com.astralplayer.nextplayer.data.NetworkInfo {
        try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (capabilities == null) {
                return com.astralplayer.nextplayer.data.NetworkInfo.DISCONNECTED
            }
            
            val connectionType = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 
                    com.astralplayer.nextplayer.data.NetworkInfo.ConnectionType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 
                    com.astralplayer.nextplayer.data.NetworkInfo.ConnectionType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 
                    com.astralplayer.nextplayer.data.NetworkInfo.ConnectionType.ETHERNET
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> 
                    com.astralplayer.nextplayer.data.NetworkInfo.ConnectionType.VPN
                else -> com.astralplayer.nextplayer.data.NetworkInfo.ConnectionType.UNKNOWN
            }
            
            val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            val bandwidth = capabilities.linkDownstreamBandwidthKbps * 1000L // Convert to bps
            
            return com.astralplayer.nextplayer.data.NetworkInfo(
                connectionType = connectionType,
                isConnected = true,
                isMetered = isMetered,
                bandwidth = bandwidth
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network info", e)
            return com.astralplayer.nextplayer.data.NetworkInfo.DISCONNECTED
        }
    }
    
    /**
     * Get recommended quality based on network conditions
     */
    fun getRecommendedQuality(): VideoQuality? {
        val networkInfo = getNetworkInfo()
        val availableQualitiesList = _availableQualities.value
        
        if (!networkInfo.isConnected || availableQualitiesList.isEmpty()) {
            return null
        }
        
        val recommendedByNetwork = networkInfo.getRecommendedQuality()
        
        // Find the best available quality that matches network recommendation
        return availableQualitiesList
            .filter { !it.isAdaptive }
            .minByOrNull { 
                kotlin.math.abs(it.height - recommendedByNetwork.height)
            } ?: availableQualitiesList.firstOrNull()
    }
    
    /**
     * Auto-select quality based on network conditions
     */
    fun autoSelectQuality() {
        val recommendedQuality = getRecommendedQuality()
        if (recommendedQuality != null) {
            selectQuality(recommendedQuality)
            Log.d(TAG, "Auto-selected quality: ${recommendedQuality.label}")
        }
    }
    
    private fun createVideoQualityFromFormat(format: Format): VideoQuality {
        val height = format.height
        val width = format.width
        val bitrate = format.bitrate
        val fps = format.frameRate.toInt()
        
        val label = when {
            height >= 2160 -> "2160p (4K)"
            height >= 1440 -> "1440p"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            height >= 360 -> "360p"
            height >= 240 -> "240p"
            else -> "${height}p"
        }
        
        return VideoQuality(
            id = "${height}p_${bitrate}",
            label = label,
            height = height,
            width = width,
            bitrate = bitrate,
            fps = fps,
            codec = format.codecs ?: "Unknown"
        )
    }
}

/**
 * Quality settings and preferences
 */
data class QualitySettings(
    val autoQualityEnabled: Boolean = true,
    val dataSaverEnabled: Boolean = false,
    val preferredQuality: VideoQuality? = null,
    val maxQualityOnCellular: VideoQuality = VideoQuality.QUALITY_720P,
    val maxQualityOnWifi: VideoQuality = VideoQuality.QUALITY_1080P
)