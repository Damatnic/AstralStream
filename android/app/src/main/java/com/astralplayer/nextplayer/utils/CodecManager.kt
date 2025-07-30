package com.astralplayer.nextplayer.utils

import android.content.Context
import android.util.Log
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simplified codec manager for handling various video formats
 */
class CodecManager(private val context: Context) {
    
    companion object {
        private const val TAG = "CodecManager"
        
        // Supported video formats
        val SUPPORTED_VIDEO_FORMATS = listOf(
            MimeTypes.VIDEO_H264,
            MimeTypes.VIDEO_H265,
            MimeTypes.VIDEO_VP8,
            MimeTypes.VIDEO_VP9,
            MimeTypes.VIDEO_AV1,
            MimeTypes.VIDEO_MP4V
        )
        
        // Supported audio formats
        val SUPPORTED_AUDIO_FORMATS = listOf(
            MimeTypes.AUDIO_AAC,
            MimeTypes.AUDIO_MPEG,
            MimeTypes.AUDIO_OPUS,
            MimeTypes.AUDIO_VORBIS
        )
    }
    
    private val _codecState = MutableStateFlow(CodecState())
    val codecState: StateFlow<CodecState> = _codecState.asStateFlow()
    
    /**
     * Initialize codec support detection
     */
    fun initializeCodecs() {
        try {
            Log.i(TAG, "Initializing codec support detection")
            
            val supportedVideoCodecs = SUPPORTED_VIDEO_FORMATS.filter { mimeType ->
                try {
                    // Basic codec availability check would go here
                    true // For now, assume all are supported
                } catch (e: Exception) {
                    Log.w(TAG, "Codec not supported: $mimeType", e)
                    false
                }
            }
            
            val supportedAudioCodecs = SUPPORTED_AUDIO_FORMATS.filter { mimeType ->
                try {
                    // Basic codec availability check would go here
                    true // For now, assume all are supported
                } catch (e: Exception) {
                    Log.w(TAG, "Codec not supported: $mimeType", e)
                    false
                }
            }
            
            _codecState.value = CodecState(
                isInitialized = true,
                supportedVideoCodecs = supportedVideoCodecs,
                supportedAudioCodecs = supportedAudioCodecs
            )
            
            Log.i(TAG, "Codec initialization complete. Video: ${supportedVideoCodecs.size}, Audio: ${supportedAudioCodecs.size}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize codecs", e)
            _codecState.value = CodecState(isInitialized = false, error = e.message)
        }
    }
    
    /**
     * Create a renderers factory with codec optimizations
     */
    fun createRenderersFactory(): RenderersFactory {
        return DefaultRenderersFactory(context).apply {
            // Basic renderer configuration
            setEnableDecoderFallback(true)
        }
    }
    
    /**
     * Apply basic optimizations to ExoPlayer builder
     */
    fun applyOptimizations(playerBuilder: ExoPlayer.Builder): ExoPlayer.Builder {
        return playerBuilder.apply {
            setRenderersFactory(createRenderersFactory())
            setSeekBackIncrementMs(10000) // 10 seconds
            setSeekForwardIncrementMs(30000) // 30 seconds
        }
    }
    
    /**
     * Check if a specific mime type is supported
     */
    fun isFormatSupported(mimeType: String): Boolean {
        val state = _codecState.value
        return state.supportedVideoCodecs.contains(mimeType) || 
               state.supportedAudioCodecs.contains(mimeType)
    }
    
    /**
     * Get the best available codec for a mime type
     */
    fun getBestCodec(mimeType: String): String? {
        return if (isFormatSupported(mimeType)) mimeType else null
    }
}

/**
 * Codec manager state
 */
data class CodecState(
    val isInitialized: Boolean = false,
    val supportedVideoCodecs: List<String> = emptyList(),
    val supportedAudioCodecs: List<String> = emptyList(),
    val error: String? = null
)