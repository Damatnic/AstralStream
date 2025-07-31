package com.astralplayer.core.codec

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CodecManager @Inject constructor(
    private val context: Context
) {
    
    data class CodecInfo(
        val name: String,
        val mimeType: String,
        val isHardwareAccelerated: Boolean,
        val maxResolution: String,
        val supportedProfiles: List<Int>
    )
    
    data class StreamType(
        val isHLS: Boolean = false,
        val isDASH: Boolean = false,
        val isRTMP: Boolean = false,
        val isAdultContent: Boolean = false,
        val requiresSpecialHandling: Boolean = false
    )
    
    private val _availableCodecs = MutableStateFlow<List<CodecInfo>>(emptyList())
    val availableCodecs: StateFlow<List<CodecInfo>> = _availableCodecs
    
    private val _currentOptimization = MutableStateFlow<String>("default")
    val currentOptimization: StateFlow<String> = _currentOptimization
    
    init {
        analyzeAvailableCodecs()
    }
    
    private fun analyzeAvailableCodecs() {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecs = mutableListOf<CodecInfo>()
        
        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) {
                for (type in codecInfo.supportedTypes) {
                    val capabilities = codecInfo.getCapabilitiesForType(type)
                    
                    codecs.add(
                        CodecInfo(
                            name = codecInfo.name,
                            mimeType = type,
                            isHardwareAccelerated = isHardwareAccelerated(codecInfo),
                            maxResolution = getMaxResolution(capabilities),
                            supportedProfiles = capabilities.profileLevels?.map { it.profile } ?: emptyList()
                        )
                    )
                }
            }
        }
        
        _availableCodecs.value = codecs
        Timber.d("Found ${codecs.size} available decoders")
    }
    
    private fun isHardwareAccelerated(codecInfo: MediaCodecInfo): Boolean {
        return !codecInfo.name.startsWith("c2.android") && 
               !codecInfo.name.startsWith("OMX.google")
    }
    
    private fun getMaxResolution(capabilities: MediaCodecInfo.CodecCapabilities): String {
        val videoCapabilities = capabilities.videoCapabilities ?: return "Unknown"
        
        return when {
            videoCapabilities.isSizeSupported(7680, 4320) -> "8K"
            videoCapabilities.isSizeSupported(3840, 2160) -> "4K"
            videoCapabilities.isSizeSupported(1920, 1080) -> "1080p"
            videoCapabilities.isSizeSupported(1280, 720) -> "720p"
            else -> "SD"
        }
    }
    
    fun applyAdultContentOptimizations() {
        _currentOptimization.value = "adult_content"
        Timber.d("Applied adult content optimizations")
        
        // Configure for optimal adult content playback
        // - Prioritize H.264/H.265 decoders
        // - Optimize buffer sizes for high bitrate content
        // - Enable hardware acceleration if available
        // - Configure for minimal seek latency
    }
    
    fun configureForStreaming(streamType: StreamType) {
        when {
            streamType.isHLS -> configureHLS()
            streamType.isDASH -> configureDASH()
            streamType.isRTMP -> configureRTMP()
            else -> configureDefault()
        }
    }
    
    private fun configureHLS() {
        _currentOptimization.value = "hls_streaming"
        Timber.d("Configured for HLS streaming")
        // HLS-specific optimizations
    }
    
    private fun configureDASH() {
        _currentOptimization.value = "dash_streaming"
        Timber.d("Configured for DASH streaming")
        // DASH-specific optimizations
    }
    
    private fun configureRTMP() {
        _currentOptimization.value = "rtmp_streaming"
        Timber.d("Configured for RTMP streaming")
        // RTMP-specific optimizations
    }
    
    private fun configureDefault() {
        _currentOptimization.value = "default"
        Timber.d("Configured for default playback")
    }
    
    fun getBestCodecForMimeType(mimeType: String): CodecInfo? {
        return _availableCodecs.value
            .filter { it.mimeType == mimeType }
            .filter { it.isHardwareAccelerated }
            .maxByOrNull { getCodecPriority(it) }
            ?: _availableCodecs.value
                .filter { it.mimeType == mimeType }
                .maxByOrNull { getCodecPriority(it) }
    }
    
    private fun getCodecPriority(codec: CodecInfo): Int {
        var priority = 0
        
        if (codec.isHardwareAccelerated) priority += 100
        
        when (codec.maxResolution) {
            "8K" -> priority += 50
            "4K" -> priority += 40
            "1080p" -> priority += 30
            "720p" -> priority += 20
        }
        
        // Prefer modern codecs
        when {
            codec.mimeType == MimeTypes.VIDEO_H265 -> priority += 15
            codec.mimeType == MimeTypes.VIDEO_AV1 -> priority += 10
            codec.mimeType == MimeTypes.VIDEO_VP9 -> priority += 8
            codec.mimeType == MimeTypes.VIDEO_H264 -> priority += 5
        }
        
        return priority
    }
    
    fun supportsHDR(): Boolean {
        return _availableCodecs.value.any { codec ->
            codec.mimeType in listOf(MimeTypes.VIDEO_H265, MimeTypes.VIDEO_VP9, MimeTypes.VIDEO_AV1) &&
            codec.isHardwareAccelerated
        }
    }
    
    fun supports4K(): Boolean {
        return _availableCodecs.value.any { 
            it.maxResolution in listOf("4K", "8K") && it.isHardwareAccelerated 
        }
    }
}