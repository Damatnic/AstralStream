package com.astralplayer.nextplayer.feature.codec

import android.content.Context
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.decoder.DecoderException
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages additional codec packs for extended format support
 */
@Singleton
class CodecPackManager @Inject constructor(
    private val context: Context
) {
    data class CodecPack(
        val id: String,
        val name: String,
        val description: String,
        val version: String,
        val size: Long,
        val formats: List<String>,
        val downloadUrl: String,
        val isInstalled: Boolean = false,
        val isDownloading: Boolean = false,
        val downloadProgress: Float = 0f
    )

    data class CodecSupport(
        val format: String,
        val isSupported: Boolean,
        val codecName: String?,
        val requiresCodecPack: String? = null
    )

    private val _availableCodecPacks = MutableStateFlow<List<CodecPack>>(emptyList())
    val availableCodecPacks: StateFlow<List<CodecPack>> = _availableCodecPacks.asStateFlow()

    private val _installedCodecs = MutableStateFlow<Set<String>>(emptySet())
    val installedCodecs: StateFlow<Set<String>> = _installedCodecs.asStateFlow()

    private val codecsDir = File(context.filesDir, "codecs")

    init {
        codecsDir.mkdirs()
        loadAvailableCodecPacks()
        scanInstalledCodecs()
    }

    /**
     * Available codec packs that can be downloaded
     */
    private fun loadAvailableCodecPacks() {
        _availableCodecPacks.value = listOf(
            CodecPack(
                id = "ac3_dts_pack",
                name = "AC3/DTS Audio Pack",
                description = "Adds support for AC3, E-AC3, DTS, and DTS-HD audio formats",
                version = "2.0.1",
                size = 2 * 1024 * 1024, // 2MB
                formats = listOf("AC3", "E-AC3", "DTS", "DTS-HD"),
                downloadUrl = "https://astral-vu.com/codecs/ac3_dts_pack_v2.0.1.zip",
                isInstalled = checkCodecPackInstalled("ac3_dts_pack")
            ),
            CodecPack(
                id = "hevc_pack",
                name = "HEVC/H.265 Pack",
                description = "Hardware accelerated HEVC/H.265 video codec support",
                version = "1.5.0",
                size = 5 * 1024 * 1024, // 5MB
                formats = listOf("HEVC", "H.265", "HEVC Main10"),
                downloadUrl = "https://astral-vu.com/codecs/hevc_pack_v1.5.0.zip",
                isInstalled = checkCodecPackInstalled("hevc_pack")
            ),
            CodecPack(
                id = "av1_pack",
                name = "AV1 Codec Pack",
                description = "Next-generation AV1 video codec for efficient streaming",
                version = "1.2.0",
                size = 4 * 1024 * 1024, // 4MB
                formats = listOf("AV1", "AV1 Main"),
                downloadUrl = "https://astral-vu.com/codecs/av1_pack_v1.2.0.zip",
                isInstalled = checkCodecPackInstalled("av1_pack")
            ),
            CodecPack(
                id = "dolby_pack",
                name = "Dolby Audio Pack",
                description = "Dolby Digital Plus, Dolby Atmos, and TrueHD support",
                version = "1.8.0",
                size = 3 * 1024 * 1024, // 3MB
                formats = listOf("Dolby Digital Plus", "Dolby Atmos", "Dolby TrueHD"),
                downloadUrl = "https://astral-vu.com/codecs/dolby_pack_v1.8.0.zip",
                isInstalled = checkCodecPackInstalled("dolby_pack")
            ),
            CodecPack(
                id = "legacy_pack",
                name = "Legacy Formats Pack",
                description = "Support for older video formats: DivX, Xvid, WMV, RealVideo",
                version = "1.0.5",
                size = 8 * 1024 * 1024, // 8MB
                formats = listOf("DivX", "Xvid", "WMV", "RealVideo", "MPEG-2"),
                downloadUrl = "https://astral-vu.com/codecs/legacy_pack_v1.0.5.zip",
                isInstalled = checkCodecPackInstalled("legacy_pack")
            ),
            CodecPack(
                id = "opus_flac_pack",
                name = "High Quality Audio Pack",
                description = "OPUS, FLAC, ALAC, and DSD audio format support",
                version = "1.3.0",
                size = 2 * 1024 * 1024, // 2MB
                formats = listOf("OPUS", "FLAC", "ALAC", "DSD"),
                downloadUrl = "https://astral-vu.com/codecs/hq_audio_pack_v1.3.0.zip",
                isInstalled = checkCodecPackInstalled("opus_flac_pack")
            )
        )
    }

    /**
     * Check if a codec pack is installed
     */
    private fun checkCodecPackInstalled(packId: String): Boolean {
        val packDir = File(codecsDir, packId)
        return packDir.exists() && packDir.isDirectory && 
               File(packDir, "manifest.json").exists()
    }

    /**
     * Scan for installed codec libraries
     */
    private fun scanInstalledCodecs() {
        val installedSet = mutableSetOf<String>()
        
        // Scan system codecs
        try {
            // Check for common video codecs
            val videoCodecs = listOf(
                MimeTypes.VIDEO_H264,
                MimeTypes.VIDEO_H265,
                MimeTypes.VIDEO_VP8,
                MimeTypes.VIDEO_VP9,
                MimeTypes.VIDEO_AV1,
                MimeTypes.VIDEO_MPEG2,
                MimeTypes.VIDEO_MPEG
            )
            
            videoCodecs.forEach { mimeType ->
                try {
                    val decoderInfos = MediaCodecUtil.getDecoderInfos(mimeType, false, false)
                    if (decoderInfos.isNotEmpty()) {
                        installedSet.add(mimeType)
                    }
                } catch (e: DecoderException) {
                    // Codec not available
                }
            }
            
            // Check for audio codecs
            val audioCodecs = listOf(
                MimeTypes.AUDIO_AAC,
                MimeTypes.AUDIO_MPEG,
                MimeTypes.AUDIO_OPUS,
                MimeTypes.AUDIO_FLAC,
                MimeTypes.AUDIO_VORBIS,
                MimeTypes.AUDIO_AC3,
                MimeTypes.AUDIO_E_AC3,
                MimeTypes.AUDIO_DTS,
                MimeTypes.AUDIO_DTS_HD,
                MimeTypes.AUDIO_TRUEHD
            )
            
            audioCodecs.forEach { mimeType ->
                try {
                    val decoderInfos = MediaCodecUtil.getDecoderInfos(mimeType, false, false)
                    if (decoderInfos.isNotEmpty()) {
                        installedSet.add(mimeType)
                    }
                } catch (e: DecoderException) {
                    // Codec not available
                }
            }
            
            // Add custom installed codecs
            codecsDir.listFiles()?.forEach { packDir ->
                if (packDir.isDirectory) {
                    val manifestFile = File(packDir, "manifest.json")
                    if (manifestFile.exists()) {
                        // Parse manifest to get supported formats
                        installedSet.add("custom_${packDir.name}")
                    }
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        _installedCodecs.value = installedSet
    }

    /**
     * Check codec support for a given format
     */
    suspend fun checkCodecSupport(format: Format): CodecSupport = withContext(Dispatchers.IO) {
        val mimeType = format.sampleMimeType ?: return@withContext CodecSupport(
            format = "Unknown",
            isSupported = false,
            codecName = null
        )
        
        try {
            val decoderInfos = MediaCodecUtil.getDecoderInfos(mimeType, false, false)
            if (decoderInfos.isNotEmpty()) {
                return@withContext CodecSupport(
                    format = getFormatName(mimeType),
                    isSupported = true,
                    codecName = decoderInfos.first().name
                )
            }
        } catch (e: DecoderException) {
            // Check if a codec pack could provide support
            val requiredPack = getRequiredCodecPack(mimeType)
            return@withContext CodecSupport(
                format = getFormatName(mimeType),
                isSupported = false,
                codecName = null,
                requiresCodecPack = requiredPack
            )
        }
        
        CodecSupport(
            format = getFormatName(mimeType),
            isSupported = false,
            codecName = null
        )
    }

    /**
     * Get the codec pack required for a mime type
     */
    private fun getRequiredCodecPack(mimeType: String): String? {
        return when (mimeType) {
            MimeTypes.AUDIO_AC3, MimeTypes.AUDIO_E_AC3, 
            MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD -> "ac3_dts_pack"
            MimeTypes.VIDEO_H265 -> "hevc_pack"
            MimeTypes.VIDEO_AV1 -> "av1_pack"
            MimeTypes.AUDIO_TRUEHD -> "dolby_pack"
            MimeTypes.AUDIO_OPUS, MimeTypes.AUDIO_FLAC -> "opus_flac_pack"
            else -> null
        }
    }

    /**
     * Get human-readable format name
     */
    private fun getFormatName(mimeType: String): String {
        return when (mimeType) {
            MimeTypes.VIDEO_H264 -> "H.264/AVC"
            MimeTypes.VIDEO_H265 -> "H.265/HEVC"
            MimeTypes.VIDEO_VP8 -> "VP8"
            MimeTypes.VIDEO_VP9 -> "VP9"
            MimeTypes.VIDEO_AV1 -> "AV1"
            MimeTypes.AUDIO_AAC -> "AAC"
            MimeTypes.AUDIO_MPEG -> "MP3"
            MimeTypes.AUDIO_OPUS -> "OPUS"
            MimeTypes.AUDIO_FLAC -> "FLAC"
            MimeTypes.AUDIO_AC3 -> "AC3"
            MimeTypes.AUDIO_E_AC3 -> "E-AC3"
            MimeTypes.AUDIO_DTS -> "DTS"
            MimeTypes.AUDIO_DTS_HD -> "DTS-HD"
            MimeTypes.AUDIO_TRUEHD -> "TrueHD"
            else -> mimeType.substringAfterLast("/").uppercase()
        }
    }

    /**
     * Download and install a codec pack
     */
    suspend fun downloadCodecPack(packId: String, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val pack = _availableCodecPacks.value.find { it.id == packId } ?: return@withContext
        
        try {
            // Update state to downloading
            updatePackState(packId) { it.copy(isDownloading = true, downloadProgress = 0f) }
            
            // Simulate download (in production, implement actual download)
            for (i in 0..100 step 10) {
                onProgress(i / 100f)
                updatePackState(packId) { it.copy(downloadProgress = i / 100f) }
                kotlinx.coroutines.delay(100) // Simulate download time
            }
            
            // Extract and install
            val packDir = File(codecsDir, packId)
            packDir.mkdirs()
            
            // Create manifest file
            File(packDir, "manifest.json").writeText("""
                {
                    "id": "$packId",
                    "version": "${pack.version}",
                    "formats": ${pack.formats.map { "\"$it\"" }}
                }
            """.trimIndent())
            
            // Update state to installed
            updatePackState(packId) { 
                it.copy(isInstalled = true, isDownloading = false, downloadProgress = 1f) 
            }
            
            // Rescan codecs
            scanInstalledCodecs()
            
        } catch (e: Exception) {
            updatePackState(packId) { 
                it.copy(isDownloading = false, downloadProgress = 0f) 
            }
            throw e
        }
    }

    /**
     * Uninstall a codec pack
     */
    suspend fun uninstallCodecPack(packId: String) = withContext(Dispatchers.IO) {
        val packDir = File(codecsDir, packId)
        if (packDir.exists()) {
            packDir.deleteRecursively()
            updatePackState(packId) { it.copy(isInstalled = false) }
            scanInstalledCodecs()
        }
    }

    /**
     * Update codec pack state
     */
    private fun updatePackState(packId: String, update: (CodecPack) -> CodecPack) {
        _availableCodecPacks.value = _availableCodecPacks.value.map { pack ->
            if (pack.id == packId) update(pack) else pack
        }
    }

    /**
     * Get codec information for debugging
     */
    fun getCodecInfo(): String {
        val sb = StringBuilder()
        sb.appendLine("=== System Codec Information ===")
        sb.appendLine("Android API Level: ${Build.VERSION.SDK_INT}")
        sb.appendLine()
        
        // List all available decoders
        val allCodecs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.media.MediaCodecList(android.media.MediaCodecList.ALL_CODECS).codecInfos
        } else {
            @Suppress("DEPRECATION")
            (0 until android.media.MediaCodecList.getCodecCount()).map {
                android.media.MediaCodecList.getCodecInfoAt(it)
            }.toTypedArray()
        }
        
        sb.appendLine("Video Decoders:")
        allCodecs.filter { !it.isEncoder && it.supportedTypes.any { type -> 
            type.startsWith("video/") 
        }}.forEach { codec ->
            sb.appendLine("  ${codec.name}")
            codec.supportedTypes.filter { it.startsWith("video/") }.forEach { type ->
                sb.appendLine("    - $type")
            }
        }
        
        sb.appendLine()
        sb.appendLine("Audio Decoders:")
        allCodecs.filter { !it.isEncoder && it.supportedTypes.any { type -> 
            type.startsWith("audio/") 
        }}.forEach { codec ->
            sb.appendLine("  ${codec.name}")
            codec.supportedTypes.filter { it.startsWith("audio/") }.forEach { type ->
                sb.appendLine("    - $type")
            }
        }
        
        return sb.toString()
    }
}