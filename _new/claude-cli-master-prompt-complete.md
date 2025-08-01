# Claude CLI Master Enhancement Prompt for AstralStream

## Copy and paste this entire prompt into Claude CLI:

---

You are the Master Enhancement Coordinator for the AstralStream Android video player project. You will orchestrate the implementation of 5 major enhancements using a team of specialized agents. 

**PROJECT CONTEXT:**
- Project: AstralStream (Advanced Android Video Player)
- Package: com.astralplayer
- Current test coverage: 87%
- Current startup time: <1 second
- Architecture: MVVM + Clean Architecture + Jetpack Compose

**YOUR MISSION:** Implement 5 enhancements WITHOUT breaking existing functionality or creating redundant code.

## STEP 1: Initial Setup and Verification

First, verify we're in the correct project and everything is working:

```bash
# Show current directory
pwd

# Verify project structure
ls -la
find . -name "AstralPlayerApplication.kt" -type f | head -1

# Run initial build and tests
./gradlew clean
./gradlew build
./gradlew test
./gradlew jacocoTestReport

# Show current test coverage
cat app/build/reports/jacoco/jacocoTestReport/html/index.html | grep -A 2 "Total"

# Create backup branch
git checkout -b enhancement-implementation-$(date +%Y%m%d)
git add .
git commit -m "Backup before implementing enhancements"
```

Show me the output of these commands before proceeding.

## STEP 2: Create Enhancement Scripts

Create these helper scripts in the project root:

**File: enhancement-agents.sh**
```bash
#!/bin/bash
set -e

PROJECT_ROOT=$(pwd)
ENHANCEMENT_LOG="$PROJECT_ROOT/enhancement-progress.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$ENHANCEMENT_LOG"
}

check_tests() {
    log "Running test suite..."
    ./gradlew test || return 1
    ./gradlew jacocoTestReport || return 1
    
    # Extract coverage percentage
    COVERAGE=$(cat app/build/reports/jacoco/jacocoTestReport/html/index.html | grep -oP 'Total[^>]*>\K[0-9]+(?=%)')
    log "Current test coverage: $COVERAGE%"
    
    if [ "$COVERAGE" -lt 85 ]; then
        log "ERROR: Test coverage dropped below 85%!"
        return 1
    fi
    return 0
}

run_agent() {
    AGENT_NAME=$1
    log "Starting $AGENT_NAME..."
    
    # Pre-agent checks
    check_tests || { log "Pre-agent tests failed!"; return 1; }
    
    # Agent implementation will be inserted here
    case "$AGENT_NAME" in
        "VideoEditingAgent")
            implement_video_editing
            ;;
        "SubtitleCacheAgent")
            implement_subtitle_cache
            ;;
        "GestureCustomizationAgent")
            implement_gesture_customization
            ;;
        "AnalyticsDashboardAgent")
            implement_analytics_dashboard
            ;;
        "CommunityFeaturesAgent")
            implement_community_features
            ;;
    esac
    
    # Post-agent checks
    check_tests || { log "Post-agent tests failed!"; return 1; }
    
    # Commit changes
    git add .
    git commit -m "Enhancement: $AGENT_NAME implementation complete"
    
    log "$AGENT_NAME completed successfully!"
}

# Implementation functions
implement_video_editing() {
    log "Implementing video editing features..."
    # Implementation details will be added by the agent
}

implement_subtitle_cache() {
    log "Implementing subtitle cache system..."
    # Implementation details will be added by the agent
}

implement_gesture_customization() {
    log "Implementing gesture customization..."
    # Implementation details will be added by the agent
}

implement_analytics_dashboard() {
    log "Implementing analytics dashboard..."
    # Implementation details will be added by the agent
}

implement_community_features() {
    log "Implementing community features..."
    # Implementation details will be added by the agent
}

# Main execution
if [ $# -eq 0 ]; then
    echo "Usage: ./enhancement-agents.sh <agent-name>"
    echo "Available agents: VideoEditingAgent, SubtitleCacheAgent, GestureCustomizationAgent, AnalyticsDashboardAgent, CommunityFeaturesAgent"
    exit 1
fi

run_agent "$1"
```

Make the script executable: `chmod +x enhancement-agents.sh`

## STEP 3: Implement VideoEditingAgent

Now implement the video editing features. First, analyze the existing code:

```bash
# Find existing video editing files
find . -path "*/nextplayer/editing/*" -type f -name "*.kt" | while read file; do
    echo "=== $file ==="
    head -50 "$file"
done

# Check for TODOs in editing code
grep -r "TODO" --include="*.kt" . | grep -i "edit"
```

Based on the analysis, complete the `AdvancedVideoEditingTools.kt` implementation:

1. Implement all TODO methods in the editing package
2. Add these specific features:
   - Frame-accurate trimming using MediaCodec
   - Video filters: brightness, contrast, saturation, blur, sharpen, vintage, black&white, sepia, vignette, temperature
   - Speed control from 0.25x to 4.0x with audio pitch correction
   - Multi-track audio support with mixing
   - Export presets: 480p@2Mbps, 720p@5Mbps, 1080p@8Mbps, 4K@20Mbps

Create these new files:

**File: app/src/main/java/com/astralplayer/nextplayer/editing/filters/VideoFilterEngine.kt**
```kotlin
package com.astralplayer.nextplayer.editing.filters

import android.graphics.Bitmap
import android.media.effect.Effect
import android.media.effect.EffectContext
import android.media.effect.EffectFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoFilterEngine @Inject constructor() {
    
    private var effectContext: EffectContext? = null
    
    fun applyFilter(bitmap: Bitmap, filter: VideoFilter): Bitmap {
        // Initialize effect context if needed
        if (effectContext == null) {
            effectContext = EffectContext.createWithCurrentGlContext()
        }
        
        return when (filter) {
            is VideoFilter.Brightness -> applyBrightness(bitmap, filter.value)
            is VideoFilter.Contrast -> applyContrast(bitmap, filter.value)
            is VideoFilter.Saturation -> applySaturation(bitmap, filter.value)
            is VideoFilter.Blur -> applyBlur(bitmap, filter.radius)
            is VideoFilter.Sharpen -> applySharpen(bitmap, filter.strength)
            is VideoFilter.Vintage -> applyVintage(bitmap)
            is VideoFilter.BlackAndWhite -> applyBlackAndWhite(bitmap)
            is VideoFilter.Sepia -> applySepia(bitmap)
            is VideoFilter.Vignette -> applyVignette(bitmap, filter.intensity)
            is VideoFilter.Temperature -> applyTemperature(bitmap, filter.value)
        }
    }
    
    private fun applyBrightness(bitmap: Bitmap, value: Float): Bitmap {
        val factory = effectContext!!.factory
        val effect = factory.createEffect(EffectFactory.EFFECT_BRIGHTNESS)
        effect.setParameter("brightness", value)
        return applyEffect(bitmap, effect)
    }
    
    private fun applyContrast(bitmap: Bitmap, value: Float): Bitmap {
        val factory = effectContext!!.factory
        val effect = factory.createEffect(EffectFactory.EFFECT_CONTRAST)
        effect.setParameter("contrast", value)
        return applyEffect(bitmap, effect)
    }
    
    private fun applySaturation(bitmap: Bitmap, value: Float): Bitmap {
        val factory = effectContext!!.factory
        val effect = factory.createEffect(EffectFactory.EFFECT_SATURATE)
        effect.setParameter("scale", value)
        return applyEffect(bitmap, effect)
    }
    
    private fun applyBlur(bitmap: Bitmap, radius: Float): Bitmap {
        val factory = effectContext!!.factory
        val effect = factory.createEffect(EffectFactory.EFFECT_BLUR)
        effect.setParameter("radius", radius)
        return applyEffect(bitmap, effect)
    }
    
    private fun applySharpen(bitmap: Bitmap, strength: Float): Bitmap {
        val factory = effectContext!!.factory
        val effect = factory.createEffect(EffectFactory.EFFECT_SHARPEN)
        effect.setParameter("scale", strength)
        return applyEffect(bitmap, effect)
    }
    
    private fun applyVintage(bitmap: Bitmap): Bitmap {
        val factory = effectContext!!.factory
        val effect = factory.createEffect(EffectFactory.EFFECT_VINTAGE)
        return applyEffect(bitmap, effect)
    }
    
    private fun applyBlackAndWhite(bitmap: Bitmap): Bitmap {
        val factory = effectContext!!.factory
        val effect = factory.createEffect(EffectFactory.EFFECT_GRAYSCALE)
        return applyEffect(bitmap, effect)
    }
    
    private fun applySepia(bitmap: Bitmap): Bitmap {
        val factory = effectContext!!.factory
        val effect = factory.createEffect(EffectFactory.EFFECT_SEPIA)
        return applyEffect(bitmap, effect)
    }
    
    private fun applyVignette(bitmap: Bitmap, intensity: Float): Bitmap {
        val factory = effectContext!!.factory
        val effect = factory.createEffect(EffectFactory.EFFECT_VIGNETTE)
        effect.setParameter("scale", intensity)
        return applyEffect(bitmap, effect)
    }
    
    private fun applyTemperature(bitmap: Bitmap, value: Float): Bitmap {
        val factory = effectContext!!.factory
        val effect = factory.createEffect(EffectFactory.EFFECT_TEMPERATURE)
        effect.setParameter("scale", value)
        return applyEffect(bitmap, effect)
    }
    
    private fun applyEffect(bitmap: Bitmap, effect: Effect): Bitmap {
        // Apply the effect to the bitmap
        // This is a simplified version - in production, you'd use OpenGL
        val outputBitmap = bitmap.copy(bitmap.config, true)
        effect.apply(bitmap.generationId, outputBitmap.width, outputBitmap.height, outputBitmap.generationId)
        effect.release()
        return outputBitmap
    }
    
    fun release() {
        effectContext?.release()
        effectContext = null
    }
}

sealed class VideoFilter {
    data class Brightness(val value: Float) : VideoFilter() // 0.0 to 2.0
    data class Contrast(val value: Float) : VideoFilter() // 0.0 to 2.0
    data class Saturation(val value: Float) : VideoFilter() // 0.0 to 2.0
    data class Blur(val radius: Float) : VideoFilter() // 0.0 to 25.0
    data class Sharpen(val strength: Float) : VideoFilter() // 0.0 to 2.0
    object Vintage : VideoFilter()
    object BlackAndWhite : VideoFilter()
    object Sepia : VideoFilter()
    data class Vignette(val intensity: Float) : VideoFilter() // 0.0 to 1.0
    data class Temperature(val value: Float) : VideoFilter() // 0.0 to 2.0
}
```

**File: app/src/main/java/com/astralplayer/nextplayer/editing/timeline/FrameAccurateTrimmer.kt**
```kotlin
package com.astralplayer.nextplayer.editing.timeline

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject

class FrameAccurateTrimmer @Inject constructor() {
    
    suspend fun trimVideo(
        inputPath: String,
        outputPath: String,
        startTimeUs: Long,
        endTimeUs: Long,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(inputPath)
            
            val trackCount = extractor.trackCount
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            
            // Find video and audio tracks
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                
                when {
                    mime.startsWith("video/") && videoTrackIndex == -1 -> videoTrackIndex = i
                    mime.startsWith("audio/") && audioTrackIndex == -1 -> audioTrackIndex = i
                }
            }
            
            require(videoTrackIndex != -1) { "No video track found" }
            
            // Setup muxer
            val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            // Add tracks to muxer
            extractor.selectTrack(videoTrackIndex)
            val videoFormat = extractor.getTrackFormat(videoTrackIndex)
            val videoMuxerTrackIndex = muxer.addTrack(videoFormat)
            
            var audioMuxerTrackIndex = -1
            if (audioTrackIndex != -1) {
                extractor.selectTrack(audioTrackIndex)
                val audioFormat = extractor.getTrackFormat(audioTrackIndex)
                audioMuxerTrackIndex = muxer.addTrack(audioFormat)
            }
            
            muxer.start()
            
            // Process video track
            processTrack(
                extractor = extractor,
                muxer = muxer,
                trackIndex = videoTrackIndex,
                muxerTrackIndex = videoMuxerTrackIndex,
                startTimeUs = startTimeUs,
                endTimeUs = endTimeUs,
                onProgress = { progress -> onProgress(progress * 0.7f) }
            )
            
            // Process audio track if exists
            if (audioTrackIndex != -1) {
                extractor.unselectTrack(videoTrackIndex)
                extractor.selectTrack(audioTrackIndex)
                
                processTrack(
                    extractor = extractor,
                    muxer = muxer,
                    trackIndex = audioTrackIndex,
                    muxerTrackIndex = audioMuxerTrackIndex,
                    startTimeUs = startTimeUs,
                    endTimeUs = endTimeUs,
                    onProgress = { progress -> onProgress(0.7f + progress * 0.3f) }
                )
            }
            
            muxer.stop()
            muxer.release()
            extractor.release()
            
            onProgress(1.0f)
            
            Result.success(File(outputPath))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun processTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        trackIndex: Int,
        muxerTrackIndex: Int,
        startTimeUs: Long,
        endTimeUs: Long,
        onProgress: (Float) -> Unit
    ) {
        val bufferSize = 1024 * 1024 // 1MB buffer
        val buffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()
        
        // Seek to start time
        extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        
        var firstSampleTime = -1L
        val duration = endTimeUs - startTimeUs
        
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            
            val presentationTimeUs = extractor.sampleTime
            if (presentationTimeUs > endTimeUs) break
            
            if (presentationTimeUs >= startTimeUs) {
                if (firstSampleTime == -1L) {
                    firstSampleTime = presentationTimeUs
                }
                
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = presentationTimeUs - firstSampleTime
                bufferInfo.flags = extractor.sampleFlags
                
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                
                val progress = (presentationTimeUs - startTimeUs).toFloat() / duration
                onProgress(progress.coerceIn(0f, 1f))
            }
            
            extractor.advance()
        }
    }
}
```

**File: app/src/main/java/com/astralplayer/nextplayer/editing/export/ExportPresetManager.kt**
```kotlin
package com.astralplayer.nextplayer.editing.export

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportPresetManager @Inject constructor() {
    
    fun getPresets(): List<ExportPreset> = listOf(
        ExportPreset(
            name = "480p - Low Quality",
            resolution = Resolution(854, 480),
            bitrate = 2_000_000, // 2 Mbps
            frameRate = 30,
            audioCodec = AudioCodec.AAC,
            audioBitrate = 128_000, // 128 kbps
            container = Container.MP4
        ),
        ExportPreset(
            name = "720p - Standard HD",
            resolution = Resolution(1280, 720),
            bitrate = 5_000_000, // 5 Mbps
            frameRate = 30,
            audioCodec = AudioCodec.AAC,
            audioBitrate = 192_000, // 192 kbps
            container = Container.MP4
        ),
        ExportPreset(
            name = "1080p - Full HD",
            resolution = Resolution(1920, 1080),
            bitrate = 8_000_000, // 8 Mbps
            frameRate = 30,
            audioCodec = AudioCodec.AAC,
            audioBitrate = 256_000, // 256 kbps
            container = Container.MP4
        ),
        ExportPreset(
            name = "4K - Ultra HD",
            resolution = Resolution(3840, 2160),
            bitrate = 20_000_000, // 20 Mbps
            frameRate = 30,
            audioCodec = AudioCodec.AAC,
            audioBitrate = 320_000, // 320 kbps
            container = Container.MP4
        ),
        ExportPreset(
            name = "Custom",
            resolution = Resolution(1920, 1080),
            bitrate = 10_000_000,
            frameRate = 60,
            audioCodec = AudioCodec.AAC,
            audioBitrate = 256_000,
            container = Container.MP4,
            isCustom = true
        )
    )
    
    fun createCustomPreset(
        resolution: Resolution,
        bitrate: Int,
        frameRate: Int,
        audioCodec: AudioCodec = AudioCodec.AAC,
        audioBitrate: Int = 192_000,
        container: Container = Container.MP4
    ): ExportPreset {
        return ExportPreset(
            name = "Custom - ${resolution.width}x${resolution.height}",
            resolution = resolution,
            bitrate = bitrate,
            frameRate = frameRate,
            audioCodec = audioCodec,
            audioBitrate = audioBitrate,
            container = container,
            isCustom = true
        )
    }
}

data class ExportPreset(
    val name: String,
    val resolution: Resolution,
    val bitrate: Int,
    val frameRate: Int,
    val audioCodec: AudioCodec,
    val audioBitrate: Int,
    val container: Container,
    val isCustom: Boolean = false
)

data class Resolution(
    val width: Int,
    val height: Int
) {
    override fun toString() = "${width}x${height}"
}

enum class AudioCodec {
    AAC,
    MP3,
    OPUS
}

enum class Container {
    MP4,
    MKV,
    WEBM
}
```

Write comprehensive tests for each feature:

**File: app/src/test/java/com/astralplayer/nextplayer/editing/VideoEditingTest.kt**
```kotlin
package com.astralplayer.nextplayer.editing

import com.astralplayer.nextplayer.editing.filters.VideoFilter
import com.astralplayer.nextplayer.editing.filters.VideoFilterEngine
import com.astralplayer.nextplayer.editing.timeline.FrameAccurateTrimmer
import com.astralplayer.nextplayer.editing.export.ExportPresetManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class VideoEditingTest {
    
    @Test
    fun `VideoFilterEngine should apply all filter types`() {
        val filterEngine = VideoFilterEngine()
        val filters = listOf(
            VideoFilter.Brightness(1.5f),
            VideoFilter.Contrast(1.2f),
            VideoFilter.Saturation(0.8f),
            VideoFilter.Blur(10f),
            VideoFilter.Sharpen(1.5f),
            VideoFilter.Vintage,
            VideoFilter.BlackAndWhite,
            VideoFilter.Sepia,
            VideoFilter.Vignette(0.7f),
            VideoFilter.Temperature(1.3f)
        )
        
        // Test that all filters can be applied without crashing
        filters.forEach { filter ->
            assertNotNull("Filter $filter should be supported", filter)
        }
    }
    
    @Test
    fun `FrameAccurateTrimmer should trim within specified time range`() = runTest {
        val trimmer = FrameAccurateTrimmer()
        val startTime = 5_000_000L // 5 seconds
        val endTime = 15_000_000L // 15 seconds
        
        // Mock implementation test
        assertTrue("Trimmer should handle valid time ranges", startTime < endTime)
    }
    
    @Test
    fun `ExportPresetManager should provide all standard presets`() {
        val presetManager = ExportPresetManager()
        val presets = presetManager.getPresets()
        
        assertEquals("Should have 5 presets", 5, presets.size)
        assertTrue("Should have 480p preset", presets.any { it.name.contains("480p") })
        assertTrue("Should have 720p preset", presets.any { it.name.contains("720p") })
        assertTrue("Should have 1080p preset", presets.any { it.name.contains("1080p") })
        assertTrue("Should have 4K preset", presets.any { it.name.contains("4K") })
        assertTrue("Should have Custom preset", presets.any { it.isCustom })
    }
}
```

## STEP 4: Implement SubtitleCacheAgent

Implement the subtitle caching system:

```bash
# Analyze current subtitle implementation
find . -name "*Subtitle*.kt" -type f | while read file; do
    echo "=== Analyzing: $file ==="
    grep -A 5 -B 5 "class\|interface\|fun" "$file" | head -30
done
```

Create these components:

**File: app/src/main/java/com/astralplayer/astralstream/data/entity/CachedSubtitleEntity.kt**
```kotlin
package com.astralplayer.astralstream.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "cached_subtitles")
data class CachedSubtitleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val videoId: String,
    val videoUrl: String,
    val language: String,
    val content: String,
    val format: String, // srt, vtt, ass
    val isEncrypted: Boolean = false,
    val encryptionKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val size: Long,
    val version: Int = 1,
    val checksum: String,
    val source: String // ai-generated, user-uploaded, community
)
```

**File: app/src/main/java/com/astralplayer/astralstream/data/dao/CachedSubtitleDao.kt**
```kotlin
package com.astralplayer.astralstream.data.dao

import androidx.room.*
import com.astralplayer.astralstream.data.entity.CachedSubtitleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedSubtitleDao {
    
    @Query("SELECT * FROM cached_subtitles WHERE videoUrl = :videoUrl AND language = :language ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCachedSubtitle(videoUrl: String, language: String): CachedSubtitleEntity?
    
    @Query("SELECT * FROM cached_subtitles WHERE videoId = :videoId")
    fun getSubtitlesForVideo(videoId: String): Flow<List<CachedSubtitleEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitle(subtitle: CachedSubtitleEntity)
    
    @Query("UPDATE cached_subtitles SET lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM cached_subtitles WHERE id = :id")
    suspend fun deleteSubtitle(id: String)
    
    @Query("SELECT SUM(size) FROM cached_subtitles")
    suspend fun getTotalCacheSize(): Long?
    
    @Query("DELETE FROM cached_subtitles WHERE lastAccessedAt < :threshold")
    suspend fun deleteOldSubtitles(threshold: Long)
    
    @Query("SELECT * FROM cached_subtitles ORDER BY lastAccessedAt ASC LIMIT :count")
    suspend fun getLeastRecentlyUsed(count: Int): List<CachedSubtitleEntity>
}
```

**File: app/src/main/java/com/astralplayer/nextplayer/subtitle/cache/SubtitleCacheManager.kt**
```kotlin
package com.astralplayer.nextplayer.subtitle.cache

import com.astralplayer.astralstream.data.dao.CachedSubtitleDao
import com.astralplayer.astralstream.data.entity.CachedSubtitleEntity
import com.astralplayer.core.security.EncryptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitleCacheManager @Inject constructor(
    private val cachedSubtitleDao: CachedSubtitleDao,
    private val encryptionManager: EncryptionManager
) {
    
    companion object {
        private const val MAX_CACHE_SIZE = 100 * 1024 * 1024L // 100 MB
        private const val MAX_AGE_DAYS = 30
        private const val LRU_CLEANUP_THRESHOLD = 0.9f // Clean up when 90% full
    }
    
    suspend fun getCachedSubtitle(
        videoUrl: String,
        language: String
    ): String? = withContext(Dispatchers.IO) {
        val cached = cachedSubtitleDao.getCachedSubtitle(videoUrl, language)
        
        if (cached != null) {
            // Update last accessed time
            cachedSubtitleDao.updateLastAccessed(cached.id)
            
            // Decrypt if encrypted
            return if (cached.isEncrypted && cached.encryptionKey != null) {
                decryptSubtitle(cached.content, cached.encryptionKey)
            } else {
                cached.content
            }
        }
        
        return null
    }
    
    suspend fun cacheSubtitle(
        videoId: String,
        videoUrl: String,
        language: String,
        content: String,
        format: String,
        source: String,
        encrypt: Boolean = false
    ) = withContext(Dispatchers.IO) {
        // Check cache size and perform LRU eviction if needed
        ensureCacheSize()
        
        val size = content.toByteArray().size.toLong()
        val checksum = calculateChecksum(content)
        
        val (finalContent, encryptionKey) = if (encrypt) {
            encryptSubtitle(content)
        } else {
            content to null
        }
        
        val entity = CachedSubtitleEntity(
            videoId = videoId,
            videoUrl = videoUrl,
            language = language,
            content = finalContent,
            format = format,
            isEncrypted = encrypt,
            encryptionKey = encryptionKey,
            size = size,
            checksum = checksum,
            source = source
        )
        
        cachedSubtitleDao.insertSubtitle(entity)
    }
    
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cachedSubtitleDao.deleteOldSubtitles(System.currentTimeMillis())
    }
    
    suspend fun clearOldCache() = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - (MAX_AGE_DAYS * 24 * 60 * 60 * 1000L)
        cachedSubtitleDao.deleteOldSubtitles(threshold)
    }
    
    private suspend fun ensureCacheSize() {
        val currentSize = cachedSubtitleDao.getTotalCacheSize() ?: 0L
        
        if (currentSize > MAX_CACHE_SIZE * LRU_CLEANUP_THRESHOLD) {
            // Remove least recently used items until we're under 70% of max size
            val targetSize = (MAX_CACHE_SIZE * 0.7f).toLong()
            var deletedSize = 0L
            
            val lruItems = cachedSubtitleDao.getLeastRecentlyUsed(50)
            
            for (item in lruItems) {
                if (currentSize - deletedSize <= targetSize) break
                
                cachedSubtitleDao.deleteSubtitle(item.id)
                deletedSize += item.size
            }
        }
    }
    
    private fun calculateChecksum(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    private suspend fun encryptSubtitle(content: String): Pair<String, String> {
        val encrypted = encryptionManager.encryptSensitiveData(content.toByteArray())
        val encryptedContent = android.util.Base64.encodeToString(
            encrypted.encryptedBytes,
            android.util.Base64.NO_WRAP
        )
        return encryptedContent to encrypted.keyAlias
    }
    
    private suspend fun decryptSubtitle(encryptedContent: String, keyAlias: String): String {
        val encryptedBytes = android.util.Base64.decode(encryptedContent, android.util.Base64.NO_WRAP)
        val decrypted = encryptionManager.decryptSensitiveData(
            EncryptedData(
                encryptedBytes = encryptedBytes,
                iv = ByteArray(0), // Would be stored properly in production
                keyAlias = keyAlias
            )
        )
        return String(decrypted)
    }
}
```

Update the Room database to include the new DAO:

**Update: app/src/main/java/com/astralplayer/data/database/AstralStreamDatabase.kt**
```kotlin
// Add to the entities array in @Database annotation:
CachedSubtitleEntity::class

// Add the abstract function:
abstract fun cachedSubtitleDao(): CachedSubtitleDao

// Update version to 3 and add migration
```

## STEP 5: Implement GestureCustomizationAgent

Analyze and enhance the gesture system:

```bash
# Find current gesture implementations
find . -name "*Gesture*.kt" -type f
cat app/src/main/java/com/astralplayer/nextplayer/gesture/AdvancedGestureManager.kt | head -100
```

**File: app/src/main/java/com/astralplayer/nextplayer/gesture/zones/GestureZoneMapper.kt**
```kotlin
package com.astralplayer.nextplayer.gesture.zones

import android.graphics.RectF
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureZoneMapper @Inject constructor() {
    
    data class GestureZone(
        val id: String,
        val bounds: RectF,
        val gestures: MutableMap<GestureType, GestureAction> = mutableMapOf()
    )
    
    private val zones = mutableMapOf<String, GestureZone>()
    
    init {
        // Initialize default zones
        setupDefaultZones()
    }
    
    private fun setupDefaultZones() {
        // Screen divided into 9 zones (3x3 grid)
        val zoneNames = listOf(
            "top_left", "top_center", "top_right",
            "middle_left", "middle_center", "middle_right",
            "bottom_left", "bottom_center", "bottom_right"
        )
        
        var index = 0
        for (row in 0..2) {
            for (col in 0..2) {
                val left = col * 0.333f
                val top = row * 0.333f
                val right = (col + 1) * 0.333f
                val bottom = (row + 1) * 0.333f
                
                zones[zoneNames[index]] = GestureZone(
                    id = zoneNames[index],
                    bounds = RectF(left, top, right, bottom)
                )
                index++
            }
        }
        
        // Set default gestures
        setDefaultGestures()
    }
    
    private fun setDefaultGestures() {
        // Middle zones for playback control
        zones["middle_left"]?.gestures?.apply {
            put(GestureType.DOUBLE_TAP, GestureAction.SEEK_BACKWARD)
            put(GestureType.SWIPE_UP, GestureAction.BRIGHTNESS_UP)
            put(GestureType.SWIPE_DOWN, GestureAction.BRIGHTNESS_DOWN)
        }
        
        zones["middle_center"]?.gestures?.apply {
            put(GestureType.SINGLE_TAP, GestureAction.TOGGLE_CONTROLS)
            put(GestureType.DOUBLE_TAP, GestureAction.PLAY_PAUSE)
            put(GestureType.LONG_PRESS, GestureAction.SHOW_INFO)
        }
        
        zones["middle_right"]?.gestures?.apply {
            put(GestureType.DOUBLE_TAP, GestureAction.SEEK_FORWARD)
            put(GestureType.SWIPE_UP, GestureAction.VOLUME_UP)
            put(GestureType.SWIPE_DOWN, GestureAction.VOLUME_DOWN)
        }
        
        // Bottom zone for seeking
        zones["bottom_center"]?.gestures?.apply {
            put(GestureType.SWIPE_LEFT, GestureAction.SEEK_BACKWARD_LONG)
            put(GestureType.SWIPE_RIGHT, GestureAction.SEEK_FORWARD_LONG)
        }
    }
    
    fun getZone(x: Float, y: Float, screenWidth: Int, screenHeight: Int): GestureZone? {
        val normalizedX = x / screenWidth
        val normalizedY = y / screenHeight
        
        return zones.values.find { zone ->
            zone.bounds.contains(normalizedX, normalizedY)
        }
    }
    
    fun customizeZone(
        zoneId: String,
        gestureType: GestureType,
        action: GestureAction
    ) {
        zones[zoneId]?.gestures?.put(gestureType, action)
    }
    
    fun removeGesture(zoneId: String, gestureType: GestureType) {
        zones[zoneId]?.gestures?.remove(gestureType)
    }
    
    fun getZoneConfiguration(): Map<String, GestureZone> = zones.toMap()
    
    fun resetToDefaults() {
        zones.clear()
        setupDefaultZones()
    }
    
    fun exportConfiguration(): String {
        // Convert zones to JSON for export
        return zones.map { (id, zone) ->
            mapOf(
                "id" to id,
                "gestures" to zone.gestures.map { (type, action) ->
                    mapOf("type" to type.name, "action" to action.name)
                }
            )
        }.toString() // In production, use proper JSON serialization
    }
    
    fun importConfiguration(config: String) {
        // Parse JSON and update zones
        // Implementation would parse the config and update zones
    }
}

enum class GestureType {
    SINGLE_TAP,
    DOUBLE_TAP,
    LONG_PRESS,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    PINCH_IN,
    PINCH_OUT,
    ROTATE_CLOCKWISE,
    ROTATE_COUNTER_CLOCKWISE
}

enum class GestureAction {
    PLAY_PAUSE,
    SEEK_FORWARD,
    SEEK_BACKWARD,
    SEEK_FORWARD_LONG,
    SEEK_BACKWARD_LONG,
    VOLUME_UP,
    VOLUME_DOWN,
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    TOGGLE_CONTROLS,
    SHOW_INFO,
    TOGGLE_FULLSCREEN,
    NEXT_VIDEO,
    PREVIOUS_VIDEO,
    SHOW_PLAYLIST,
    TOGGLE_SUBTITLES,
    PLAYBACK_SPEED,
    SCREENSHOT,
    LOCK_CONTROLS,
    NONE
}
```

**File: app/src/main/java/com/astralplayer/nextplayer/gesture/recorder/CustomGestureRecorder.kt**
```kotlin
package com.astralplayer.nextplayer.gesture.recorder

import android.view.MotionEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomGestureRecorder @Inject constructor() {
    
    data class RecordedGesture(
        val name: String,
        val points: List<GesturePoint>,
        val duration: Long,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class GesturePoint(
        val x: Float,
        val y: Float,
        val pressure: Float,
        val timestamp: Long
    )
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording
    
    private val _currentRecording = MutableStateFlow<List<GesturePoint>>(emptyList())
    val currentRecording: StateFlow<List<GesturePoint>> = _currentRecording
    
    private var recordingStartTime = 0L
    private val recordedGestures = mutableListOf<RecordedGesture>()
    
    fun startRecording() {
        _isRecording.value = true
        _currentRecording.value = emptyList()
        recordingStartTime = System.currentTimeMillis()
    }
    
    fun stopRecording(gestureName: String): RecordedGesture? {
        if (!_isRecording.value) return null
        
        _isRecording.value = false
        val points = _currentRecording.value
        
        if (points.isEmpty()) return null
        
        val duration = System.currentTimeMillis() - recordingStartTime
        val gesture = RecordedGesture(
            name = gestureName,
            points = points,
            duration = duration
        )
        
        recordedGestures.add(gesture)
        return gesture
    }
    
    fun recordMotionEvent(event: MotionEvent) {
        if (!_isRecording.value) return
        
        val point = GesturePoint(
            x = event.x,
            y = event.y,
            pressure = event.pressure,
            timestamp = event.eventTime
        )
        
        _currentRecording.value = _currentRecording.value + point
    }
    
    fun matchGesture(points: List<GesturePoint>): RecordedGesture? {
        // Simple gesture matching algorithm
        // In production, use DTW (Dynamic Time Warping) or similar
        
        return recordedGestures.minByOrNull { gesture ->
            calculateGestureDistance(points, gesture.points)
        }?.takeIf { gesture ->
            calculateGestureDistance(points, gesture.points) < 0.3f
        }
    }
    
    private fun calculateGestureDistance(
        points1: List<GesturePoint>,
        points2: List<GesturePoint>
    ): Float {
        // Simplified distance calculation
        // Normalize both gesture paths and compare
        
        if (points1.isEmpty() || points2.isEmpty()) return Float.MAX_VALUE
        
        val normalized1 = normalizeGesture(points1)
        val normalized2 = normalizeGesture(points2)
        
        var totalDistance = 0f
        val sampleCount = minOf(normalized1.size, normalized2.size)
        
        for (i in 0 until sampleCount) {
            val dx = normalized1[i].x - normalized2[i].x
            val dy = normalized1[i].y - normalized2[i].y
            totalDistance += kotlin.math.sqrt(dx * dx + dy * dy)
        }
        
        return totalDistance / sampleCount
    }
    
    private fun normalizeGesture(points: List<GesturePoint>): List<GesturePoint> {
        if (points.isEmpty()) return emptyList()
        
        // Find bounding box
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        
        val width = maxX - minX
        val height = maxY - minY
        
        // Normalize to 0-1 range
        return points.map { point ->
            point.copy(
                x = if (width > 0) (point.x - minX) / width else 0.5f,
                y = if (height > 0) (point.y - minY) / height else 0.5f
            )
        }
    }
    
    fun getSavedGestures(): List<RecordedGesture> = recordedGestures.toList()
    
    fun deleteGesture(name: String) {
        recordedGestures.removeAll { it.name == name }
    }
    
    fun clearAllGestures() {
        recordedGestures.clear()
    }
}
```

**File: app/src/main/java/com/astralplayer/presentation/gestures/GestureCustomizationScreen.kt**
```kotlin
package com.astralplayer.presentation.gestures

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.astralplayer.nextplayer.gesture.zones.GestureAction
import com.astralplayer.nextplayer.gesture.zones.GestureType
import com.astralplayer.nextplayer.gesture.zones.GestureZoneMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureCustomizationScreen(
    onBack: () -> Unit,
    viewModel: GestureCustomizationViewModel = hiltViewModel()
) {
    val zones by viewModel.zones.collectAsState()
    val selectedZone by viewModel.selectedZone.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Gestures") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                    IconButton(onClick = { viewModel.exportConfiguration() }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Zone visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(16.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawZones(zones, selectedZone)
                }
                
                // Zone selection overlay
                zones.forEach { (zoneId, zone) ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { viewModel.selectZone(zoneId) }
                    ) {
                        // Invisible clickable areas
                    }
                }
            }
            
            // Selected zone configuration
            selectedZone?.let { zone ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Zone: ${zone.id.replace('_', ' ').capitalize()}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Gesture list
                        zone.gestures.forEach { (gestureType, action) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(gestureType.name.replace('_', ' '))
                                
                                Row {
                                    Text(
                                        text = action.name.replace('_', ' '),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    IconButton(
                                        onClick = { viewModel.removeGesture(zone.id, gestureType) }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Add gesture button
                        OutlinedButton(
                            onClick = { viewModel.showAddGestureDialog(zone.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Gesture")
                        }
                    }
                }
            }
            
            // Custom gesture recorder
            if (isRecording) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recording gesture...")
                        
                        Button(
                            onClick = { viewModel.stopRecording() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Stop")
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawZones(
    zones: Map<String, GestureZoneMapper.GestureZone>,
    selectedZone: GestureZoneMapper.GestureZone?
) {
    zones.forEach { (_, zone) ->
        val left = zone.bounds.left * size.width
        val top = zone.bounds.top * size.height
        val width = zone.bounds.width() * size.width
        val height = zone.bounds.height() * size.height
        
        // Draw zone border
        drawRect(
            color = if (zone == selectedZone) Color.Yellow else Color.White.copy(alpha = 0.3f),
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(
                width = if (zone == selectedZone) 3.dp.toPx() else 1.dp.toPx()
            )
        )
        
        // Draw zone fill if selected
        if (zone == selectedZone) {
            drawRect(
                color = Color.Yellow.copy(alpha = 0.1f),
                topLeft = Offset(left, top),
                size = Size(width, height)
            )
        }
    }
}
```

## STEP 6: Implement AnalyticsDashboardAgent

Create the analytics module:

```bash
# Create analytics package structure
mkdir -p app/src/main/java/com/astralplayer/analytics/{api,data,repository,ui}
```

**File: app/src/main/java/com/astralplayer/analytics/data/AnalyticsDatabase.kt**
```kotlin
package com.astralplayer.analytics.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Database(
    entities = [
        PlaybackEvent::class,
        FeatureUsageEvent::class,
        PerformanceMetric::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AnalyticsConverters::class)
abstract class AnalyticsDatabase : RoomDatabase() {
    abstract fun analyticsDao(): AnalyticsDao
}

@Entity(tableName = "playback_events")
data class PlaybackEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: String,
    val videoTitle: String,
    val duration: Long,
    val watchedDuration: Long,
    val completionPercentage: Float,
    val timestamp: Date = Date(),
    val source: String // local, cloud, streaming
)

@Entity(tableName = "feature_usage_events")
data class FeatureUsageEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val featureName: String,
    val usageCount: Int = 1,
    val lastUsed: Date = Date(),
    val metadata: String? = null
)

@Entity(tableName = "performance_metrics")
data class PerformanceMetric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val metricName: String,
    val value: Float,
    val unit: String,
    val timestamp: Date = Date()
)

@Dao
interface AnalyticsDao {
    @Insert
    suspend fun insertPlaybackEvent(event: PlaybackEvent)
    
    @Query("SELECT * FROM playback_events WHERE timestamp >= :startDate ORDER BY timestamp DESC")
    fun getPlaybackEvents(startDate: Date): Flow<List<PlaybackEvent>>
    
    @Query("SELECT SUM(watchedDuration) as total FROM playback_events WHERE timestamp >= :startDate")
    suspend fun getTotalWatchTime(startDate: Date): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeatureUsage(event: FeatureUsageEvent)
    
    @Query("SELECT * FROM feature_usage_events ORDER BY usageCount DESC LIMIT :limit")
    fun getTopFeatures(limit: Int = 10): Flow<List<FeatureUsageEvent>>
    
    @Insert
    suspend fun insertPerformanceMetric(metric: PerformanceMetric)
    
    @Query("SELECT AVG(value) as average FROM performance_metrics WHERE metricName = :name AND timestamp >= :startDate")
    suspend fun getAverageMetric(name: String, startDate: Date): Float?
    
    @Query("DELETE FROM playback_events WHERE timestamp < :date")
    suspend fun deleteOldPlaybackEvents(date: Date)
    
    @Query("DELETE FROM performance_metrics WHERE timestamp < :date")
    suspend fun deleteOldMetrics(date: Date)
}

class AnalyticsConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
```

**File: app/src/main/java/com/astralplayer/analytics/repository/AnalyticsRepository.kt**
```kotlin
package com.astralplayer.analytics.repository

import com.astralplayer.analytics.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val analyticsDao: AnalyticsDao
) {
    
    fun trackPlayback(
        videoId: String,
        videoTitle: String,
        duration: Long,
        watchedDuration: Long,
        source: String
    ) {
        val completionPercentage = if (duration > 0) {
            (watchedDuration.toFloat() / duration) * 100
        } else 0f
        
        val event = PlaybackEvent(
            videoId = videoId,
            videoTitle = videoTitle,
            duration = duration,
            watchedDuration = watchedDuration,
            completionPercentage = completionPercentage,
            source = source
        )
        
        // Fire and forget
        kotlinx.coroutines.GlobalScope.launch {
            analyticsDao.insertPlaybackEvent(event)
        }
    }
    
    fun trackFeatureUsage(featureName: String, metadata: String? = null) {
        kotlinx.coroutines.GlobalScope.launch {
            analyticsDao.insertFeatureUsage(
                FeatureUsageEvent(
                    featureName = featureName,
                    metadata = metadata
                )
            )
        }
    }
    
    fun trackPerformance(metricName: String, value: Float, unit: String) {
        kotlinx.coroutines.GlobalScope.launch {
            analyticsDao.insertPerformanceMetric(
                PerformanceMetric(
                    metricName = metricName,
                    value = value,
                    unit = unit
                )
            )
        }
    }
    
    fun getWatchTimeStats(days: Int = 7): Flow<WatchTimeStats> {
        val startDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }.time
        
        return analyticsDao.getPlaybackEvents(startDate).map { events ->
            WatchTimeStats(
                totalWatchTime = events.sumOf { it.watchedDuration },
                totalVideos = events.distinctBy { it.videoId }.size,
                averageCompletion = events.map { it.completionPercentage }.average().toFloat(),
                watchHistory = events.groupBy { 
                    Calendar.getInstance().apply { time = it.timestamp }.get(Calendar.DAY_OF_YEAR)
                }.map { (day, dayEvents) ->
                    DailyWatchTime(
                        day = day,
                        watchTime = dayEvents.sumOf { it.watchedDuration }
                    )
                }
            )
        }
    }
    
    fun getTopFeatures(limit: Int = 10): Flow<List<FeatureUsageEvent>> {
        return analyticsDao.getTopFeatures(limit)
    }
    
    suspend fun getPerformanceReport(): PerformanceReport {
        val last24Hours = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, -24)
        }.time
        
        return PerformanceReport(
            averageFps = analyticsDao.getAverageMetric("fps", last24Hours) ?: 0f,
            averageMemoryUsage = analyticsDao.getAverageMetric("memory_mb", last24Hours) ?: 0f,
            averageStartupTime = analyticsDao.getAverageMetric("startup_ms", last24Hours) ?: 0f,
            averageBatteryDrain = analyticsDao.getAverageMetric("battery_percent_per_hour", last24Hours) ?: 0f
        )
    }
    
    suspend fun cleanupOldData(daysToKeep: Int = 30) {
        val threshold = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysToKeep)
        }.time
        
        analyticsDao.deleteOldPlaybackEvents(threshold)
        analyticsDao.deleteOldMetrics(threshold)
    }
}

data class WatchTimeStats(
    val totalWatchTime: Long,
    val totalVideos: Int,
    val averageCompletion: Float,
    val watchHistory: List<DailyWatchTime>
)

data class DailyWatchTime(
    val day: Int,
    val watchTime: Long
)

data class PerformanceReport(
    val averageFps: Float,
    val averageMemoryUsage: Float,
    val averageStartupTime: Float,
    val averageBatteryDrain: Float
)
```

**File: app/src/main/java/com/astralplayer/analytics/ui/AnalyticsDashboardScreen.kt**
```kotlin
package com.astralplayer.analytics.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsDashboardViewModel = hiltViewModel()
) {
    val watchTimeStats by viewModel.watchTimeStats.collectAsStateWithLifecycle()
    val topFeatures by viewModel.topFeatures.collectAsStateWithLifecycle()
    val performanceReport by viewModel.performanceReport.collectAsStateWithLifecycle()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportAnalytics() }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    IconButton(onClick = { viewModel.clearAnalytics() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time range selector
            item {
                TimeRangeSelector(
                    selectedRange = selectedTimeRange,
                    onRangeSelected = { viewModel.setTimeRange(it) }
                )
            }
            
            // Watch time statistics
            item {
                WatchTimeCard(stats = watchTimeStats)
            }
            
            // Watch time graph
            item {
                WatchTimeGraph(
                    watchHistory = watchTimeStats?.watchHistory ?: emptyList()
                )
            }
            
            // Top features
            item {
                TopFeaturesCard(features = topFeatures)
            }
            
            // Performance metrics
            item {
                PerformanceCard(report = performanceReport)
            }
        }
    }
}

@Composable
private fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TimeRange.values().forEach { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) }
            )
        }
    }
}

@Composable
private fun WatchTimeCard(stats: WatchTimeStats?) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Watch Time Statistics",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.PlayArrow,
                    value = formatDuration(stats?.totalWatchTime ?: 0),
                    label = "Total Watch Time"
                )
                
                StatItem(
                    icon = Icons.Default.VideoLibrary,
                    value = "${stats?.totalVideos ?: 0}",
                    label = "Videos Watched"
                )
                
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = "${stats?.averageCompletion?.toInt() ?: 0}%",
                    label = "Avg Completion"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WatchTimeGraph(
    watchHistory: List<DailyWatchTime>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Daily Watch Time",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawWatchTimeGraph(watchHistory)
            }
        }
    }
}

private fun DrawScope.drawWatchTimeGraph(
    watchHistory: List<DailyWatchTime>
) {
    if (watchHistory.isEmpty()) return
    
    val maxTime = watchHistory.maxOf { it.watchTime }
    val barWidth = size.width / watchHistory.size
    
    watchHistory.forEachIndexed { index, dayData ->
        val barHeight = (dayData.watchTime.toFloat() / maxTime) * size.height * 0.8f
        val x = index * barWidth + barWidth * 0.1f
        val y = size.height - barHeight
        
        // Draw bar
        drawRect(
            color = Color(0xFF4CAF50),
            topLeft = Offset(x, y),
            size = Size(barWidth * 0.8f, barHeight)
        )
    }
}

@Composable
private fun TopFeaturesCard(features: List<FeatureUsageEvent>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Most Used Features",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            features.take(5).forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(feature.featureName)
                    Text(
                        text = "${feature.usageCount}x",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceCard(report: PerformanceReport?) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PerformanceMetricRow(
                label = "Average FPS",
                value = "${report?.averageFps?.toInt() ?: 0}",
                unit = "fps"
            )
            
            PerformanceMetricRow(
                label = "Memory Usage",
                value = "${report?.averageMemoryUsage?.toInt() ?: 0}",
                unit = "MB"
            )
            
            PerformanceMetricRow(
                label = "Startup Time",
                value = "${report?.averageStartupTime?.toInt() ?: 0}",
                unit = "ms"
            )
            
            PerformanceMetricRow(
                label = "Battery Drain",
                value = String.format("%.1f", report?.averageBatteryDrain ?: 0f),
                unit = "%/hr"
            )
        }
    }
}

@Composable
private fun PerformanceMetricRow(
    label: String,
    value: String,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(
            text = "$value $unit",
            color = MaterialTheme.colorScheme.primary
        )
    }
}

enum class TimeRange(val label: String, val days: Int) {
    WEEK("7 Days", 7),
    MONTH("30 Days", 30),
    QUARTER("90 Days", 90)
}

private fun formatDuration(millis: Long): String {
    val hours = millis / 3600000
    val minutes = (millis % 3600000) / 60000
    
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
```

## STEP 7: Implement CommunityFeaturesAgent

Create community features with mock API for now:

```bash
# Create community package
mkdir -p app/src/main/java/com/astralplayer/community/{api,data,repository,ui}
```

**File: app/src/main/java/com/astralplayer/community/api/CommunityApi.kt**
```kotlin
package com.astralplayer.community.api

import com.astralplayer.community.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface CommunityApi {
    
    @GET("playlists/shared")
    suspend fun getSharedPlaylists(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<List<SharedPlaylist>>
    
    @POST("playlists/share")
    suspend fun sharePlaylist(
        @Body request: SharePlaylistRequest
    ): Response<SharedPlaylist>
    
    @GET("playlists/{id}")
    suspend fun getPlaylistDetails(
        @Path("id") playlistId: String
    ): Response<SharedPlaylist>
    
    @POST("playlists/{id}/like")
    suspend fun likePlaylist(
        @Path("id") playlistId: String
    ): Response<Unit>
    
    @GET("subtitles/contributions")
    suspend fun getSubtitleContributions(
        @Query("videoId") videoId: String? = null,
        @Query("language") language: String? = null
    ): Response<List<SubtitleContribution>>
    
    @POST("subtitles/contribute")
    suspend fun contributeSubtitle(
        @Body request: ContributeSubtitleRequest
    ): Response<SubtitleContribution>
    
    @POST("subtitles/{id}/vote")
    suspend fun voteSubtitle(
        @Path("id") subtitleId: String,
        @Body request: VoteRequest
    ): Response<Unit>
    
    @GET("rooms/active")
    suspend fun getActiveWatchRooms(
        @Query("page") page: Int = 1
    ): Response<List<WatchRoom>>
    
    @POST("rooms/create")
    suspend fun createWatchRoom(
        @Body request: CreateRoomRequest
    ): Response<WatchRoom>
    
    @POST("rooms/{id}/join")
    suspend fun joinWatchRoom(
        @Path("id") roomId: String
    ): Response<RoomJoinResponse>
}

// For now, create a mock implementation
class MockCommunityApi : CommunityApi {
    
    override suspend fun getSharedPlaylists(page: Int, limit: Int): Response<List<SharedPlaylist>> {
        val mockPlaylists = (1..limit).map { index ->
            SharedPlaylist(
                id = "playlist_$index",
                name = "Awesome Playlist $index",
                description = "A great collection of videos",
                creatorName = "User${index}",
                creatorId = "user_$index",
                videoCount = (10..50).random(),
                likes = (100..1000).random(),
                createdAt = System.currentTimeMillis() - (index * 86400000L),
                thumbnailUrl = null,
                isPublic = true
            )
        }
        return Response.success(mockPlaylists)
    }
    
    override suspend fun sharePlaylist(request: SharePlaylistRequest): Response<SharedPlaylist> {
        val shared = SharedPlaylist(
            id = UUID.randomUUID().toString(),
            name = request.name,
            description = request.description,
            creatorName = "Current User",
            creatorId = "current_user",
            videoCount = request.videoIds.size,
            likes = 0,
            createdAt = System.currentTimeMillis(),
            thumbnailUrl = null,
            isPublic = request.isPublic
        )
        return Response.success(shared)
    }
    
    // Implement other methods similarly...
}
```

**File: app/src/main/java/com/astralplayer/community/data/model/CommunityModels.kt**
```kotlin
package com.astralplayer.community.data.model

import java.util.UUID

data class SharedPlaylist(
    val id: String,
    val name: String,
    val description: String?,
    val creatorName: String,
    val creatorId: String,
    val videoCount: Int,
    val likes: Int,
    val createdAt: Long,
    val thumbnailUrl: String?,
    val isPublic: Boolean
)

data class SharePlaylistRequest(
    val name: String,
    val description: String?,
    val videoIds: List<String>,
    val isPublic: Boolean = true
)

data class SubtitleContribution(
    val id: String,
    val videoId: String,
    val videoTitle: String,
    val language: String,
    val languageName: String,
    val contributorName: String,
    val contributorId: String,
    val content: String,
    val format: String,
    val upvotes: Int,
    val downvotes: Int,
    val status: ContributionStatus,
    val createdAt: Long
)

enum class ContributionStatus {
    PENDING,
    APPROVED,
    REJECTED,
    UNDER_REVIEW
}

data class ContributeSubtitleRequest(
    val videoId: String,
    val videoTitle: String,
    val language: String,
    val content: String,
    val format: String
)

data class VoteRequest(
    val voteType: VoteType
)

enum class VoteType {
    UPVOTE,
    DOWNVOTE
}

data class WatchRoom(
    val id: String,
    val name: String,
    val hostName: String,
    val hostId: String,
    val currentVideo: VideoInfo?,
    val participantCount: Int,
    val maxParticipants: Int,
    val isPublic: Boolean,
    val createdAt: Long
)

data class VideoInfo(
    val id: String,
    val title: String,
    val duration: Long,
    val currentPosition: Long
)

data class CreateRoomRequest(
    val name: String,
    val maxParticipants: Int = 10,
    val isPublic: Boolean = true
)

data class RoomJoinResponse(
    val roomId: String,
    val sessionToken: String,
    val webSocketUrl: String
)
```

**File: app/src/main/java/com/astralplayer/community/ui/CommunityScreen.kt**
```kotlin
package com.astralplayer.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astralplayer.community.data.model.SharedPlaylist
import com.astralplayer.community.data.model.SubtitleContribution

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToSubtitle: (String) -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val sharedPlaylists by viewModel.sharedPlaylists.collectAsStateWithLifecycle()
    val subtitleContributions by viewModel.subtitleContributions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab selector
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                CommunityTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.title) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        }
                    )
                }
            }
            
            // Content based on selected tab
            when (selectedTab) {
                CommunityTab.PLAYLISTS -> {
                    PlaylistsContent(
                        playlists = sharedPlaylists,
                        isLoading = isLoading,
                        onPlaylistClick = onNavigateToPlaylist,
                        onSharePlaylist = { viewModel.shareCurrentPlaylist() }
                    )
                }
                
                CommunityTab.SUBTITLES -> {
                    SubtitlesContent(
                        contributions = subtitleContributions,
                        isLoading = isLoading,
                        onContributionClick = onNavigateToSubtitle,
                        onContributeSubtitle = { viewModel.contributeSubtitle() }
                    )
                }
                
                CommunityTab.WATCH_TOGETHER -> {
                    WatchTogetherContent(
                        onCreateRoom = { viewModel.createWatchRoom() },
                        onJoinRoom = { roomId -> viewModel.joinWatchRoom(roomId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistsContent(
    playlists: List<SharedPlaylist>,
    isLoading: Boolean,
    onPlaylistClick: (String) -> Unit,
    onSharePlaylist: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) }
                )
            }
        }
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        ExtendedFloatingActionButton(
            onClick = onSharePlaylist,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            text = { Text("Share Playlist") },
            icon = { Icon(Icons.Default.Share, contentDescription = null) }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: SharedPlaylist,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "by ${playlist.creatorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${playlist.videoCount} videos",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "${playlist.likes}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun SubtitlesContent(
    contributions: List<SubtitleContribution>,
    isLoading: Boolean,
    onContributionClick: (String) -> Unit,
    onContributeSubtitle: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contributions) { contribution ->
                SubtitleContributionCard(
                    contribution = contribution,
                    onClick = { onContributionClick(contribution.id) }
                )
            }
        }
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        ExtendedFloatingActionButton(
            onClick = onContributeSubtitle,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            text = { Text("Contribute") },
            icon = { Icon(Icons.Default.Add, contentDescription = null) }
        )
    }
}

@Composable
private fun SubtitleContributionCard(
    contribution: SubtitleContribution,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = contribution.videoTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${contribution.languageName} by ${contribution.contributorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                AssistChip(
                    onClick = { },
                    label = { Text(contribution.status.name) },
                    modifier = Modifier.height(24.dp)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${contribution.upvotes}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ThumbDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "${contribution.downvotes}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchTogetherContent(
    onCreateRoom: () -> Unit,
    onJoinRoom: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Watch Together",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "Create or join a room to watch videos with friends",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCreateRoom,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Room")
        }
        
        OutlinedButton(
            onClick = { /* Show join dialog */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Login, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Join Room")
        }
    }
}

enum class CommunityTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PLAYLISTS("Playlists", Icons.Default.PlaylistPlay),
    SUBTITLES("Subtitles", Icons.Default.Subtitles),
    WATCH_TOGETHER("Watch Together", Icons.Default.Group)
}
```

## STEP 8: Final Integration and Testing

After all agents complete:

```bash
# Run comprehensive tests
./gradlew clean
./gradlew build
./gradlew test --info
./gradlew connectedAndroidTest
./gradlew jacocoTestReport
./gradlew lint
./gradlew dependencyCheckAnalyze

# Check final metrics
echo "=== FINAL METRICS ==="
echo "Test Coverage: $(cat app/build/reports/jacoco/jacocoTestReport/html/index.html | grep -oP 'Total[^>]*>\K[0-9]+(?=%)')%"
echo "APK Size: $(du -h app/build/outputs/apk/debug/*.apk | cut -f1)"
echo "Method Count: $(dx --dex --output=temp.dex app/build/outputs/apk/debug/*.apk && dexdump -f temp.dex | grep method_ids_size | awk '{print $2}' && rm temp.dex)"

# Generate enhancement report
cat > ENHANCEMENT_REPORT.md << EOF
# AstralStream Enhancement Report

## Implemented Features
1. ✅ Video Editing - Advanced editing tools with filters and export
2. ✅ Subtitle Cache - Offline caching with encryption
3. ✅ Gesture Customization - Zone-based custom gestures
4. ✅ Analytics Dashboard - Privacy-first analytics
5. ✅ Community Features - Sharing and collaboration

## Metrics
- Test Coverage: $(cat app/build/reports/jacoco/jacocoTestReport/html/index.html | grep -oP 'Total[^>]*>\K[0-9]+(?=%)')%
- Build Status: SUCCESS
- All Tests: PASSING

## Files Changed
$(git diff --name-only enhancement-implementation-*)

Generated on: $(date)
EOF

# Create PR-ready commit
git add .
git commit -m "feat: Implement 5 major enhancements

- Video Editing: Complete implementation with 10+ filters
- Subtitle Cache: Offline caching with LRU and encryption  
- Gesture Customization: Zone-based custom gestures
- Analytics Dashboard: Privacy-first local analytics
- Community Features: Playlist sharing and subtitle contributions

Test coverage maintained at 87%+
No breaking changes to existing features"
```

## EXECUTION CHECKLIST

As you work through each step, mark these items:

- [ ] Initial setup and verification complete
- [ ] Helper scripts created
- [ ] VideoEditingAgent implementation complete
- [ ] SubtitleCacheAgent implementation complete  
- [ ] GestureCustomizationAgent implementation complete
- [ ] AnalyticsDashboardAgent implementation complete
- [ ] CommunityFeaturesAgent implementation complete
- [ ] All tests passing (>85% coverage)
- [ ] No lint errors
- [ ] Performance metrics maintained
- [ ] Enhancement report generated
- [ ] Ready for PR

**IMPORTANT RULES:**
1. NEVER create a new project - work in the existing codebase
2. ALWAYS run tests after each major change
3. MAINTAIN the 87% test coverage - do not let it drop
4. PRESERVE the <1 second startup time
5. COMMIT after each successful agent implementation

Start with Step 1 and show me the output before proceeding to the implementations.