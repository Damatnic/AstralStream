package com.astralplayer.nextplayer.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VideoQuality(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val isAdaptive: Boolean = false
) {
    val displayName: String
        get() = when {
            isAdaptive -> "Auto"
            height >= 2160 -> "4K (${height}p)"
            height >= 1440 -> "1440p"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            height >= 360 -> "360p"
            else -> "${height}p"
        }
}

enum class NetworkType {
    WIFI, MOBILE, ETHERNET, NONE
}

enum class QualityPreference {
    AUTO, MANUAL, DATA_SAVER, HIGH_QUALITY
}

data class QualitySettings(
    val preference: QualityPreference = QualityPreference.AUTO,
    val maxQualityOnWifi: VideoQuality? = null,
    val maxQualityOnMobile: VideoQuality? = null,
    val dataSaverMode: Boolean = false,
    val adaptiveBitrate: Boolean = true
)

class NetworkMonitor(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _networkType = MutableStateFlow(NetworkType.NONE)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()
    
    fun getCurrentNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.NONE
        }
    }
    
    fun updateNetworkType() {
        _networkType.value = getCurrentNetworkType()
    }
    
    fun isMeteredConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        return connectivityManager.isActiveNetworkMetered
    }
    
    fun getNetworkSpeed(): String {
        val networkType = getCurrentNetworkType()
        return when (networkType) {
            NetworkType.WIFI -> "High Speed"
            NetworkType.ETHERNET -> "Very High Speed"
            NetworkType.MOBILE -> {
                if (isMeteredConnection()) "Mobile Data" else "High Speed Mobile"
            }
            NetworkType.NONE -> "No Connection"
        }
    }
}

class StreamQualityManager(
    private val context: Context,
    private val exoPlayer: ExoPlayer
) {
    private val networkMonitor = NetworkMonitor(context)
    private val trackSelector = exoPlayer.trackSelector as? DefaultTrackSelector
    
    private val _availableQualities = MutableStateFlow<List<VideoQuality>>(emptyList())
    val availableQualities: StateFlow<List<VideoQuality>> = _availableQualities.asStateFlow()
    
    private val _currentQuality = MutableStateFlow<VideoQuality?>(null)
    val currentQuality: StateFlow<VideoQuality?> = _currentQuality.asStateFlow()
    
    private val _qualitySettings = MutableStateFlow(QualitySettings())
    val qualitySettings: StateFlow<QualitySettings> = _qualitySettings.asStateFlow()
    
    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()
    
    init {
        setupAdaptiveStreaming()
        networkMonitor.updateNetworkType()
    }
    
    private fun setupAdaptiveStreaming() {
        trackSelector?.let { selector ->
            val parameters = selector.buildUponParameters()
                .setMaxVideoSize(1920, 1080) // Default max quality
                .setMaxVideoBitrate(5000000) // 5 Mbps default
                .build()
            
            selector.setParameters(parameters)
        }
    }
    
    fun updateAvailableQualities(tracks: Tracks) {
        val qualities = mutableListOf<VideoQuality>()
        
        // Add auto quality option
        qualities.add(
            VideoQuality(
                id = "auto",
                name = "Auto",
                width = 0,
                height = 0,
                bitrate = 0,
                isAdaptive = true
            )
        )
        
        // Extract video tracks
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until trackGroup.mediaTrackGroup.length) {
                    val format = trackGroup.mediaTrackGroup.getFormat(i)
                    
                    if (format.width > 0 && format.height > 0) {
                        val quality = VideoQuality(
                            id = "${format.width}x${format.height}_${format.bitrate}",
                            name = "${format.height}p",
                            width = format.width,
                            height = format.height,
                            bitrate = format.bitrate
                        )
                        
                        if (!qualities.any { it.height == quality.height && it.width == quality.width }) {
                            qualities.add(quality)
                        }
                    }
                }
            }
        }
        
        // Sort by quality (highest first)
        val sortedQualities = qualities.filter { !it.isAdaptive }
            .sortedByDescending { it.height }
            .toMutableList()
        
        // Add auto option at the beginning
        sortedQualities.add(0, qualities.first { it.isAdaptive })
        
        _availableQualities.value = sortedQualities
    }
    
    fun selectQuality(quality: VideoQuality) {
        trackSelector?.let { selector ->
            val parameters = if (quality.isAdaptive) {
                // Enable adaptive streaming
                adaptToNetworkConditions()
            } else {
                // Set specific quality
                selector.buildUponParameters()
                    .setMaxVideoSize(quality.width, quality.height)
                    .setMaxVideoBitrate(quality.bitrate)
                    .build()
            }
            
            selector.setParameters(parameters)
            _currentQuality.value = quality
        }
    }
    
    private fun adaptToNetworkConditions(): TrackSelectionParameters {
        val networkType = networkMonitor.getCurrentNetworkType()
        val isMetered = networkMonitor.isMeteredConnection()
        val settings = _qualitySettings.value
        
        return trackSelector?.buildUponParameters()?.let { builder ->
            when (networkType) {
                NetworkType.WIFI -> {
                    if (settings.dataSaverMode) {
                        builder.setMaxVideoSize(1280, 720) // 720p max
                            .setMaxVideoBitrate(2000000) // 2 Mbps
                    } else {
                        builder.setMaxVideoSize(1920, 1080) // 1080p max
                            .setMaxVideoBitrate(5000000) // 5 Mbps
                    }
                }
                NetworkType.MOBILE -> {
                    if (isMetered || settings.dataSaverMode) {
                        builder.setMaxVideoSize(854, 480) // 480p max
                            .setMaxVideoBitrate(1000000) // 1 Mbps
                    } else {
                        builder.setMaxVideoSize(1280, 720) // 720p max
                            .setMaxVideoBitrate(2000000) // 2 Mbps
                    }
                }
                NetworkType.ETHERNET -> {
                    builder.setMaxVideoSize(3840, 2160) // 4K max
                        .setMaxVideoBitrate(15000000) // 15 Mbps
                }
                NetworkType.NONE -> {
                    builder.setMaxVideoSize(640, 360) // 360p max
                        .setMaxVideoBitrate(500000) // 500 Kbps
                }
            }
            builder.build()
        } ?: TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
    }
    
    fun enableDataSaverMode(enabled: Boolean) {
        _qualitySettings.value = _qualitySettings.value.copy(dataSaverMode = enabled)
        
        if (enabled) {
            // Force lower quality settings
            trackSelector?.setParameters(
                trackSelector.buildUponParameters()
                    .setMaxVideoSize(854, 480) // 480p max
                    .setMaxVideoBitrate(800000) // 800 Kbps
                    .build()
            )
        } else {
            // Re-adapt to current network conditions
            selectQuality(_currentQuality.value ?: _availableQualities.value.firstOrNull { it.isAdaptive } ?: return)
        }
    }
    
    fun updateQualitySettings(settings: QualitySettings) {
        _qualitySettings.value = settings
        
        when (settings.preference) {
            QualityPreference.AUTO -> {
                selectQuality(_availableQualities.value.firstOrNull { it.isAdaptive } ?: return)
            }
            QualityPreference.DATA_SAVER -> {
                enableDataSaverMode(true)
            }
            QualityPreference.HIGH_QUALITY -> {
                val highestQuality = _availableQualities.value
                    .filter { !it.isAdaptive }
                    .maxByOrNull { it.height }
                if (highestQuality != null) {
                    selectQuality(highestQuality)
                }
            }
            QualityPreference.MANUAL -> {
                // Manual selection handled by user
            }
        }
    }
    
    fun onNetworkChanged() {
        networkMonitor.updateNetworkType()
        
        // Re-adapt quality if in auto mode
        val currentQuality = _currentQuality.value
        if (currentQuality?.isAdaptive == true) {
            trackSelector?.setParameters(adaptToNetworkConditions())
        }
    }
    
    fun setBuffering(isBuffering: Boolean) {
        _isBuffering.value = isBuffering
        
        if (isBuffering) {
            // If buffering frequently, consider lowering quality
            val networkType = networkMonitor.getCurrentNetworkType()
            if (networkType == NetworkType.MOBILE && networkMonitor.isMeteredConnection()) {
                // Auto-reduce quality on mobile data if buffering
                val lowerQuality = _availableQualities.value
                    .filter { !it.isAdaptive && it.height <= 480 }
                    .maxByOrNull { it.height }
                
                if (lowerQuality != null && _currentQuality.value?.height ?: 0 > lowerQuality.height) {
                    selectQuality(lowerQuality)
                }
            }
        }
    }
    
    fun getRecommendedQuality(): VideoQuality? {
        val networkType = networkMonitor.getCurrentNetworkType()
        val isMetered = networkMonitor.isMeteredConnection()
        val availableQualities = _availableQualities.value.filter { !it.isAdaptive }
        
        return when (networkType) {
            NetworkType.WIFI -> {
                if (isMetered) {
                    availableQualities.find { it.height == 720 } ?: availableQualities.maxByOrNull { it.height }
                } else {
                    availableQualities.find { it.height == 1080 } ?: availableQualities.maxByOrNull { it.height }
                }
            }
            NetworkType.MOBILE -> {
                if (isMetered) {
                    availableQualities.find { it.height == 480 } ?: availableQualities.minByOrNull { it.height }
                } else {
                    availableQualities.find { it.height == 720 } ?: availableQualities.maxByOrNull { it.height }
                }
            }
            NetworkType.ETHERNET -> {
                availableQualities.maxByOrNull { it.height }
            }
            NetworkType.NONE -> {
                availableQualities.find { it.height == 360 } ?: availableQualities.minByOrNull { it.height }
            }
        }
    }
    
    fun getNetworkInfo(): NetworkInfo {
        return NetworkInfo(
            type = networkMonitor.getCurrentNetworkType(),
            isMetered = networkMonitor.isMeteredConnection(),
            speed = networkMonitor.getNetworkSpeed()
        )
    }
}

data class NetworkInfo(
    val type: NetworkType,
    val isMetered: Boolean,
    val speed: String
)