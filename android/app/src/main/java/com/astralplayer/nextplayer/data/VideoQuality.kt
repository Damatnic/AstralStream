package com.astralplayer.nextplayer.data

/**
 * Represents video quality options and parameters
 */
data class VideoQuality(
    val id: String,
    val label: String,
    val height: Int,
    val width: Int,
    val bitrate: Int,
    val fps: Int = 30,
    val isAdaptive: Boolean = false,
    val codec: String = "H.264"
) {
    companion object {
        // Standard quality presets
        val AUTO = VideoQuality("auto", "Auto", 0, 0, 0, isAdaptive = true)
        val QUALITY_240P = VideoQuality("240p", "240p", 240, 426, 300000)
        val QUALITY_360P = VideoQuality("360p", "360p", 360, 640, 700000)
        val QUALITY_480P = VideoQuality("480p", "480p", 480, 854, 1200000)
        val QUALITY_720P = VideoQuality("720p", "720p", 720, 1280, 2500000)
        val QUALITY_1080P = VideoQuality("1080p", "1080p", 1080, 1920, 5000000)
        val QUALITY_1440P = VideoQuality("1440p", "1440p", 1440, 2560, 9000000)
        val QUALITY_2160P = VideoQuality("2160p (4K)", "2160p", 2160, 3840, 18000000)
        
        val DEFAULT_QUALITIES = listOf(
            AUTO,
            QUALITY_2160P,
            QUALITY_1440P,
            QUALITY_1080P,
            QUALITY_720P,
            QUALITY_480P,
            QUALITY_360P,
            QUALITY_240P
        )
        
        fun fromHeight(height: Int): VideoQuality {
            return when {
                height >= 2160 -> QUALITY_2160P
                height >= 1440 -> QUALITY_1440P  
                height >= 1080 -> QUALITY_1080P
                height >= 720 -> QUALITY_720P
                height >= 480 -> QUALITY_480P
                height >= 360 -> QUALITY_360P
                height >= 240 -> QUALITY_240P
                else -> QUALITY_240P
            }
        }
    }
    
    /**
     * Get display name for UI
     */
    fun getDisplayName(): String {
        return if (isAdaptive) {
            "Auto (Adaptive)"
        } else {
            "$label (${bitrate / 1000}kbps)"
        }
    }
    
    /**
     * Check if this quality is better than another
     */
    fun isBetterThan(other: VideoQuality): Boolean {
        if (isAdaptive) return true
        if (other.isAdaptive) return false
        return height > other.height
    }
    
    /**
     * Get memory usage estimation in MB
     */
    fun getMemoryUsageEstimate(): Int {
        return when {
            height >= 2160 -> 100
            height >= 1440 -> 70
            height >= 1080 -> 50
            height >= 720 -> 30
            height >= 480 -> 20
            else -> 10
        }
    }
}