// CodecOptimizer.kt
package com.astralplayer.core.codec

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.upstream.DefaultAllocator
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class CodecOptimizer @Inject constructor() {
    
    fun getOptimizedLoadControl(isAdultContent: Boolean): LoadControl {
        return if (isAdultContent) {
            // Optimized for adult content streaming
            DefaultLoadControl.Builder()
                .setAllocator(DefaultAllocator(true, 16))
                .setBufferDurationsMs(
                    15000,  // Min buffer
                    30000,  // Max buffer
                    2500,   // Buffer for playback
                    5000    // Buffer for rebuffer
                )
                .setTargetBufferBytes(10 * 1024 * 1024) // 10MB
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        } else {
            DefaultLoadControl.Builder().build()
        }
    }
    
    fun getPreferredCodecs(): List<String> {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val preferredCodecs = mutableListOf<String>()
        
        // Prioritize hardware decoders
        val videoMimeTypes = listOf(
            MediaFormat.MIMETYPE_VIDEO_AVC,      // H.264
            MediaFormat.MIMETYPE_VIDEO_HEVC,     // H.265
            MediaFormat.MIMETYPE_VIDEO_VP9,      // VP9
            MediaFormat.MIMETYPE_VIDEO_AV1       // AV1
        )
        
        for (mimeType in videoMimeTypes) {
            codecList.codecInfos
                .filter { !it.isEncoder && isHardwareAccelerated(it) }
                .filter { it.supportedTypes.contains(mimeType) }
                .forEach { preferredCodecs.add(it.name) }
        }
        
        return preferredCodecs
    }
    
    private fun isHardwareAccelerated(codecInfo: MediaCodecInfo): Boolean {
        return !codecInfo.name.startsWith("OMX.google.") &&
               !codecInfo.name.startsWith("c2.android.")
    }
    
    fun getBestCodecForMimeType(mimeType: String): String? {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        
        return codecList.codecInfos
            .filter { !it.isEncoder }
            .filter { it.supportedTypes.contains(mimeType) }
            .sortedWith(compareBy<MediaCodecInfo> { !isHardwareAccelerated(it) }
                .thenBy { it.name })
            .firstOrNull()?.name
    }
    
    fun getOptimizedBufferSize(isAdultContent: Boolean, isHighQuality: Boolean): Int {
        return when {
            isAdultContent && isHighQuality -> 20 * 1024 * 1024 // 20MB
            isAdultContent -> 15 * 1024 * 1024 // 15MB
            isHighQuality -> 12 * 1024 * 1024 // 12MB
            else -> 8 * 1024 * 1024 // 8MB
        }
    }
}