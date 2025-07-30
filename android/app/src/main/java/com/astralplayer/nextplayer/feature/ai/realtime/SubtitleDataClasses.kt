package com.astralplayer.nextplayer.feature.ai.realtime

// Data classes and supporting types
data class SubtitleEntry(
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val language: String,
    val confidence: Float
)

data class AudioData(
    val sampleRate: Int,
    val channels: Int,
    val data: ByteArray,
    val durationMs: Long
) {
    fun extractChunk(startMs: Long, endMs: Long): ByteArray {
        // Extract audio chunk for the specified time range
        val startSample = (startMs * sampleRate / 1000).toInt()
        val endSample = (endMs * sampleRate / 1000).toInt()
        val sampleSize = endSample - startSample
        
        return if (startSample < data.size && endSample <= data.size) {
            data.copyOfRange(startSample, endSample)
        } else {
            ByteArray(0)
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as AudioData
        
        if (sampleRate != other.sampleRate) return false
        if (channels != other.channels) return false
        if (!data.contentEquals(other.data)) return false
        if (durationMs != other.durationMs) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = sampleRate
        result = 31 * result + channels
        result = 31 * result + data.contentHashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }
}

data class AudioChunk(
    val data: ByteArray,
    val startTime: Long,
    val endTime: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as AudioChunk
        
        if (!data.contentEquals(other.data)) return false
        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        return result
    }
}

data class CachedSubtitles(
    val subtitles: List<SubtitleEntry>,
    val timestamp: Long,
    val ttlMs: Long = 24 * 60 * 60 * 1000L // 24 hours
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > ttlMs
}

data class PerformanceMetrics(
    val totalRequests: Long,
    val successRate: Double,
    val averageGenerationTime: Double,
    val cacheHitRate: Double,
    val under3SecondsRate: Double,
    val under5SecondsRate: Double
)

sealed class SubtitleGenerationResult {
    object Idle : SubtitleGenerationResult()
    data class Processing(val progress: Float, val message: String) : SubtitleGenerationResult()
    data class Success(val subtitles: List<SubtitleEntry>) : SubtitleGenerationResult()
    data class Error(val message: String, val exception: Throwable? = null) : SubtitleGenerationResult()
}

class SubtitleGenerationException(message: String, cause: Throwable? = null) : Exception(message, cause)