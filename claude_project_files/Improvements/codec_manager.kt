package com.astralplayer.nextplayer.utils

import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Advanced codec manager for handling various video formats including adult content
 */
class CodecManager(private val context: Context) {
    
    companion object {
        private const val TAG = "CodecManager"
        
        // Common adult content codecs and formats
        val ADULT_CONTENT_MIME_TYPES = listOf(
            MimeTypes.VIDEO_H264,
            MimeTypes.VIDEO_H265,
            MimeTypes.VIDEO_VP8,
            MimeTypes.VIDEO_VP9,
            MimeTypes.VIDEO_AV1,
            MimeTypes.VIDEO_MPEG,
            MimeTypes.VIDEO_MPEG2,
            MimeTypes.VIDEO_MP4V,
            MimeTypes.VIDEO_DIVX,
            MimeTypes.VIDEO_VC1,
            MimeTypes.VIDEO_WMV,
            MimeTypes.VIDEO_FLV,
            "video/x-matroska", // MKV
            "video/x-msvideo", // AVI
            "video/quicktime", // MOV
            "video/x-ms-wmv", // WMV
            "video/x-flv", // FLV
            "video/x-ms-asf", // ASF
            "video/vnd.rn-realvideo", // RealVideo
            "video/x-pn-realvideo", // RealVideo
        )
        
        val ADVANCED_AUDIO_CODECS = listOf(
            MimeTypes.AUDIO_AAC,
            MimeTypes.AUDIO_AC3,
            MimeTypes.AUDIO_E_AC3,
            MimeTypes.AUDIO_DTS,
            MimeTypes.AUDIO_DTS_HD,
            MimeTypes.AUDIO_TRUEHD,
            MimeTypes.AUDIO_OPUS,
            MimeTypes.AUDIO_VORBIS,
            MimeTypes.AUDIO_FLAC,
            MimeTypes.AUDIO_ALAC,
            MimeTypes.AUDIO_MP3,
            MimeTypes.AUDIO_AMR_NB,
            MimeTypes.AUDIO_AMR_WB,
        )
    }
    
    private val _codecInfo = MutableStateFlow<CodecInfo>(CodecInfo())
    val codecInfo: StateFlow<CodecInfo> = _codecInfo.asStateFlow()
    
    private val _decoderInfo = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val decoderInfo: StateFlow<Map<String, List<String>>> = _decoderInfo.asStateFlow()
    
    data class CodecInfo(
        val hardwareDecodersAvailable: List<String> = emptyList(),
        val softwareDecodersAvailable: List<String> = emptyList(),
        val supportedVideoFormats: List<String> = emptyList(),
        val supportedAudioFormats: List<String> = emptyList(),
        val isDtsSupported: Boolean = false,
        val isAc3Supported: Boolean = false,
        val isHevcSupported: Boolean = false,
        val isAv1Supported: Boolean = false,
        val maxSupportedResolution: String = "Unknown"
    )
    
    fun initializeCodecs() {
        Log.d(TAG, "Initializing codec support...")
        
        val hardwareDecoders = mutableListOf<String>()
        val softwareDecoders = mutableListOf<String>()
        val supportedVideoFormats = mutableSetOf<String>()
        val supportedAudioFormats = mutableSetOf<String>()
        val decoderMap = mutableMapOf<String, MutableList<String>>()
        
        var isDtsSupported = false
        var isAc3Supported = false
        var isHevcSupported = false
        var isAv1Supported = false
        var maxResolution = "Unknown"
        
        try {
            // Check video codecs
            for (mimeType in ADULT_CONTENT_MIME_TYPES) {
                try {
                    val decoderInfos = MediaCodecUtil.getDecoderInfos(mimeType, false, false)
                    val decoderNames = mutableListOf<String>()
                    
                    for (decoderInfo in decoderInfos) {
                        val decoderName = decoderInfo.name
                        decoderNames.add(decoderName)
                        
                        if (decoderInfo.hardwareAccelerated) {
                            hardwareDecoders.add("$decoderName ($mimeType)")
                        } else {
                            softwareDecoders.add("$decoderName ($mimeType)")
                        }
                        
                        // Check specific codec support
                        when (mimeType) {
                            MimeTypes.VIDEO_H265 -> isHevcSupported = true
                            MimeTypes.VIDEO_AV1 -> isAv1Supported = true
                        }
                        
                        // Try to determine max resolution
                        try {
                            val capabilities = decoderInfo.getCapabilities(mimeType)
                            val videoCapabilities = capabilities.videoCapabilities
                            if (videoCapabilities != null) {
                                val maxWidth = videoCapabilities.supportedWidths.upper
                                val maxHeight = videoCapabilities.supportedHeights.upper
                                val resolution = "${maxWidth}x${maxHeight}"
                                if (maxWidth > 3840 || maxHeight > 2160) {
                                    maxResolution = "8K ($resolution)"
                                } else if (maxWidth > 1920 || maxHeight > 1080) {
                                    maxResolution = "4K ($resolution)"
                                } else if (maxWidth > 1280 || maxHeight > 720) {
                                    maxResolution = "1080p ($resolution)"
                                } else {
                                    maxResolution = "720p ($resolution)"
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not determine video capabilities for $mimeType", e)
                        }
                    }
                    
                    if (decoderInfos.isNotEmpty()) {
                        supportedVideoFormats.add(mimeType)
                        decoderMap[mimeType] = decoderNames
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking codec support for $mimeType", e)
                }
            }
            
            // Check audio codecs
            for (mimeType in ADVANCED_AUDIO_CODECS) {
                try {
                    val decoderInfos = MediaCodecUtil.getDecoderInfos(mimeType, false, false)
                    val decoderNames = mutableListOf<String>()
                    
                    for (decoderInfo in decoderInfos) {
                        val decoderName = decoderInfo.name
                        decoderNames.add(decoderName)
                        
                        // Check specific audio codec support
                        when (mimeType) {
                            MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD -> isDtsSupported = true
                            MimeTypes.AUDIO_AC3, MimeTypes.AUDIO_E_AC3 -> isAc3Supported = true
                        }
                    }
                    
                    if (decoderInfos.isNotEmpty()) {
                        supportedAudioFormats.add(mimeType)
                        decoderMap[mimeType] = decoderNames
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking audio codec support for $mimeType", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing codecs", e)
        }
        
        val codecInfo = CodecInfo(
            hardwareDecodersAvailable = hardwareDecoders.distinct(),
            softwareDecodersAvailable = softwareDecoders.distinct(),
            supportedVideoFormats = supportedVideoFormats.toList(),
            supportedAudioFormats = supportedAudioFormats.toList(),
            isDtsSupported = isDtsSupported,
            isAc3Supported = isAc3Supported,
            isHevcSupported = isHevcSupported,
            isAv1Supported = isAv1Supported,
            maxSupportedResolution = maxResolution
        )
        
        _codecInfo.value = codecInfo
        _decoderInfo.value = decoderMap.mapValues { it.value.toList() }
        
        Log.d(TAG, "Codec initialization complete:")
        Log.d(TAG, "Hardware decoders: ${hardwareDecoders.size}")
        Log.d(TAG, "Software decoders: ${softwareDecoders.size}")
        Log.d(TAG, "Video formats: ${supportedVideoFormats.size}")
        Log.d(TAG, "Audio formats: ${supportedAudioFormats.size}")
        Log.d(TAG, "HEVC support: $isHevcSupported")
        Log.d(TAG, "AV1 support: $isAv1Supported")
        Log.d(TAG, "DTS support: $isDtsSupported")
        Log.d(TAG, "AC3 support: $isAc3Supported")
        Log.d(TAG, "Max resolution: $maxResolution")
    }
    
    /**
     * Create optimized renderers factory for adult content playback
     */
    fun createRenderersFactory(): RenderersFactory {
        return object : DefaultRenderersFactory(context) {
            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: androidx.media3.exoplayer.mediacodec.MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: android.os.Handler,
                videoRendererEventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: ArrayList<androidx.media3.exoplayer.Renderer>
            ) {
                // Create optimized video renderer with custom settings
                val videoRenderer = androidx.media3.exoplayer.video.MediaCodecVideoRenderer(
                    context,
                    mediaCodecSelector,
                    allowedVideoJoiningTimeMs,
                    enableDecoderFallback,
                    eventHandler,
                    videoRendererEventListener,
                    50 // Max dropped frame count before fallback
                ).apply {
                    // Enable experimental features for better adult content support
                    setOutputSurfaceRenderer(null)
                }
                
                out.add(videoRenderer)
                
                super.buildVideoRenderers(
                    context,
                    extensionRendererMode,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    eventHandler,
                    videoRendererEventListener,
                    allowedVideoJoiningTimeMs,
                    out
                )
            }
            
            override fun buildAudioRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: androidx.media3.exoplayer.mediacodec.MediaCodecSelector,
                enableDecoderFallback: Boolean,
                audioSink: androidx.media3.exoplayer.audio.AudioSink,
                eventHandler: android.os.Handler,
                audioRendererEventListener: AudioRendererEventListener,
                out: ArrayList<androidx.media3.exoplayer.Renderer>
            ) {
                // Create optimized audio renderer
                val audioRenderer = androidx.media3.exoplayer.audio.MediaCodecAudioRenderer(
                    context,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    eventHandler,
                    audioRendererEventListener,
                    audioSink
                )
                
                out.add(audioRenderer)
                
                super.buildAudioRenderers(
                    context,
                    extensionRendererMode,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    audioSink,
                    eventHandler,
                    audioRendererEventListener,
                    out
                )
            }
        }
    }
    
    /**
     * Check if a specific format is supported
     */
    fun isFormatSupported(mimeType: String): Boolean {
        val currentInfo = _codecInfo.value
        return currentInfo.supportedVideoFormats.contains(mimeType) || 
               currentInfo.supportedAudioFormats.contains(mimeType)
    }
    
    /**
     * Get recommended decoder for a format
     */
    fun getRecommendedDecoder(mimeType: String): String? {
        val decoders = _decoderInfo.value[mimeType] ?: return null
        
        // Prefer hardware decoders for performance
        val hardwareDecoder = decoders.find { decoder ->
            _codecInfo.value.hardwareDecodersAvailable.any { it.contains(decoder) }
        }
        
        return hardwareDecoder ?: decoders.firstOrNull()
    }
    
    /**
     * Get codec information as formatted string for display
     */
    fun getCodecInfoString(): String {
        val info = _codecInfo.value
        return buildString {
            appendLine("=== Codec Information ===")
            appendLine("Hardware Decoders: ${info.hardwareDecodersAvailable.size}")
            appendLine("Software Decoders: ${info.softwareDecodersAvailable.size}")
            appendLine("Video Formats: ${info.supportedVideoFormats.size}")
            appendLine("Audio Formats: ${info.supportedAudioFormats.size}")
            appendLine("HEVC/H.265: ${if (info.isHevcSupported) "✓" else "✗"}")
            appendLine("AV1: ${if (info.isAv1Supported) "✓" else "✗"}")
            appendLine("DTS Audio: ${if (info.isDtsSupported) "✓" else "✗"}")
            appendLine("AC3 Audio: ${if (info.isAc3Supported) "✓" else "✗"}")
            appendLine("Max Resolution: ${info.maxSupportedResolution}")
            appendLine()
            appendLine("=== Supported Video Formats ===")
            info.supportedVideoFormats.forEach { format ->
                appendLine("• $format")
            }
            appendLine()
            appendLine("=== Supported Audio Formats ===")
            info.supportedAudioFormats.forEach { format ->
                appendLine("• $format")
            }
        }
    }
    
    /**
     * Apply optimized settings for adult content playback
     */
    fun applyAdultContentOptimizations(playerBuilder: ExoPlayer.Builder): ExoPlayer.Builder {
        return playerBuilder.apply {
            // Use our custom renderers factory
            setRenderersFactory(createRenderersFactory())
            
            // Set optimized seek parameters for adult content
            setSeekBackIncrementMs(10000) // 10 seconds
            setSeekForwardIncrementMs(30000) // 30 seconds
            
            // Enable bandwidth meter for adaptive streaming
            setBandwidthMeter(androidx.media3.exoplayer.upstream.DefaultBandwidthMeter.getSingletonInstance(context))
            
            // Set load control for better buffering
            setLoadControl(
                androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        50000, // Min buffer
                        300000, // Max buffer
                        2500, // Buffer for playback
                        5000 // Buffer for playback after rebuffer
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
        }
    }
}