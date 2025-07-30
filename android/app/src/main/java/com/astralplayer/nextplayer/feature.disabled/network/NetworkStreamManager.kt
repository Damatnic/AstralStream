package com.astralplayer.nextplayer.feature.network

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

data class StreamQuality(
    val name: String,
    val width: Int,
    val height: Int,
    val bitrate: Int
)

class NetworkStreamManager(private val context: Context) {
    
    private val _bufferPercentage = MutableStateFlow(0)
    val bufferPercentage: StateFlow<Int> = _bufferPercentage.asStateFlow()
    
    private val _currentQuality = MutableStateFlow<StreamQuality?>(null)
    val currentQuality: StateFlow<StreamQuality?> = _currentQuality.asStateFlow()
    
    fun startStreaming(url: String) {
        Log.d("NetworkStreamManager", "Starting stream: $url")
        
        try {
            val uri = Uri.parse(url)
            Log.d("NetworkStreamManager", "Parsed URI: $uri")
            
            // Analyze URL to determine stream type and qualities
            val availableQualities = analyzeStreamUrl(url)
            
            // Set initial quality based on stream analysis
            val defaultQuality = when {
                availableQualities.any { it.name == "720p" } -> availableQualities.first { it.name == "720p" }
                availableQualities.isNotEmpty() -> availableQualities.first()
                else -> StreamQuality("720p", 1280, 720, 2500000)
            }
            
            _currentQuality.value = defaultQuality
            Log.d("NetworkStreamManager", "Stream started with quality: ${defaultQuality.name}")
            
            // Start buffer monitoring
            startBufferMonitoring()
            
        } catch (e: Exception) {
            Log.e("NetworkStreamManager", "Failed to start streaming", e)
            _currentQuality.value = StreamQuality("720p", 1280, 720, 2500000)
        }
    }
    
    private fun analyzeStreamUrl(url: String): List<StreamQuality> {
        return when {
            url.contains("m3u8", ignoreCase = true) -> {
                // HLS stream - typically has multiple qualities
                listOf(
                    StreamQuality("1080p", 1920, 1080, 5000000),
                    StreamQuality("720p", 1280, 720, 2500000),
                    StreamQuality("480p", 854, 480, 1000000),
                    StreamQuality("360p", 640, 360, 500000)
                )
            }
            url.contains("mpd", ignoreCase = true) -> {
                // DASH stream
                listOf(
                    StreamQuality("1080p", 1920, 1080, 5000000),
                    StreamQuality("720p", 1280, 720, 2500000),
                    StreamQuality("480p", 854, 480, 1000000)
                )
            }
            url.contains("youtube", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true) -> {
                // YouTube-like platform
                listOf(
                    StreamQuality("1080p60", 1920, 1080, 6000000),
                    StreamQuality("1080p", 1920, 1080, 4500000),
                    StreamQuality("720p60", 1280, 720, 3500000),
                    StreamQuality("720p", 1280, 720, 2500000),
                    StreamQuality("480p", 854, 480, 1000000),
                    StreamQuality("360p", 640, 360, 500000)
                )
            }
            else -> {
                // Direct video file or unknown format
                listOf(
                    StreamQuality("720p", 1280, 720, 2500000),
                    StreamQuality("480p", 854, 480, 1000000)
                )
            }
        }
    }
    
    private fun startBufferMonitoring() {
        GlobalScope.launch {
            // Simulate buffer monitoring
            var bufferLevel = 0
            while (bufferLevel < 100) {
                delay(200)
                bufferLevel = minOf(100, bufferLevel + 5)
                _bufferPercentage.value = bufferLevel
            }
        }
    }
    
    fun setQuality(quality: StreamQuality) {
        _currentQuality.value = quality
    }
    
    fun updateBufferPercentage(percentage: Int) {
        _bufferPercentage.value = percentage
    }
    
    fun getAvailableQualities(): List<StreamQuality> {
        return listOf(
            StreamQuality("1080p", 1920, 1080, 5000000),
            StreamQuality("720p", 1280, 720, 2500000),
            StreamQuality("480p", 854, 480, 1000000)
        )
    }
}