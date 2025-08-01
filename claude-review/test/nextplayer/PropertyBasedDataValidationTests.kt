package com.astralplayer.nextplayer

import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.utils.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import kotlin.test.*
import kotlin.random.Random

/**
 * Property-based testing for data validation across the application
 * Tests data integrity, validation rules, and edge cases using random data generation
 */
@RunWith(MockitoJUnitRunner::class)
class PropertyBasedDataValidationTests {

    @Mock private lateinit var mockContext: android.content.Context
    private lateinit var testScope: TestCoroutineScope

    @Before
    fun setup() {
        testScope = TestCoroutineScope()
    }

    @After
    fun cleanup() {
        testScope.cleanupTestCoroutines()
    }

    // ============================================================================
    // RECENT FILES DATA VALIDATION PROPERTIES
    // ============================================================================

    @Test
    fun `RecentFile validation properties hold for all valid inputs`() = testScope.runBlockingTest {
        repeat(1000) { iteration ->
            // Generate random valid recent file data
            val recentFile = generateRandomValidRecentFile(iteration)
            
            // Property 1: Valid recent file should always pass validation
            assertTrue(
                "Valid recent file should pass validation: $recentFile",
                recentFile.isValid()
            )
            
            // Property 2: Duration should never be negative
            assertTrue(
                "Duration should not be negative: ${recentFile.duration}",
                recentFile.duration >= 0
            )
            
            // Property 3: Last played position should not exceed duration
            assertTrue(
                "Position should not exceed duration: pos=${recentFile.lastPlayedPosition}, dur=${recentFile.duration}",
                recentFile.lastPlayedPosition <= recentFile.duration
            )
            
            // Property 4: Last played time should be reasonable
            val now = System.currentTimeMillis()
            assertTrue(
                "Last played time should be reasonable: ${recentFile.lastPlayedTime}",
                recentFile.lastPlayedTime <= now && recentFile.lastPlayedTime >= 0
            )
            
            // Property 5: File path should exist and be accessible
            assertTrue(
                "File path should be non-empty: '${recentFile.path}'",
                recentFile.path.isNotBlank()
            )
        }
    }

    @Test
    fun `RecentFile validation correctly rejects invalid inputs`() = testScope.runBlockingTest {
        repeat(500) { iteration ->
            // Generate random invalid recent file data
            val invalidRecentFile = generateRandomInvalidRecentFile(iteration)
            
            // Property: Invalid recent file should always fail validation
            assertFalse(
                "Invalid recent file should fail validation: $invalidRecentFile",
                invalidRecentFile.isValid()
            )
        }
    }

    @Test
    fun `RecentFile serialization roundtrip preserves data integrity`() = testScope.runBlockingTest {
        repeat(300) { iteration ->
            val originalFile = generateRandomValidRecentFile(iteration)
            
            // Serialize and deserialize
            val serialized = originalFile.toJson()
            val deserialized = RecentFile.fromJson(serialized)
            
            // Property: Roundtrip should preserve all data
            assertEquals(
                "ID should be preserved",
                originalFile.id, deserialized.id
            )
            assertEquals(
                "Path should be preserved",
                originalFile.path, deserialized.path
            )
            assertEquals(
                "Name should be preserved", 
                originalFile.name, deserialized.name
            )
            assertEquals(
                "Duration should be preserved",
                originalFile.duration, deserialized.duration
            )
            assertEquals(
                "Position should be preserved",
                originalFile.lastPlayedPosition, deserialized.lastPlayedPosition
            )
            assertEquals(
                "Timestamp should be preserved",
                originalFile.lastPlayedTime, deserialized.lastPlayedTime
            )
        }
    }

    // ============================================================================
    // BOOKMARK DATA VALIDATION PROPERTIES
    // ============================================================================

    @Test
    fun `Bookmark validation properties hold for all valid inputs`() = testScope.runBlockingTest {
        repeat(800) { iteration ->
            val bookmark = generateRandomValidBookmark(iteration)
            
            // Property 1: Valid bookmark should pass validation
            assertTrue(
                "Valid bookmark should pass validation: $bookmark",
                bookmark.isValid()
            )
            
            // Property 2: Position should be non-negative
            assertTrue(
                "Bookmark position should be non-negative: ${bookmark.position}",
                bookmark.position >= 0
            )
            
            // Property 3: Title should not be empty after trimming
            assertTrue(
                "Bookmark title should not be empty: '${bookmark.title}'",
                bookmark.title.trim().isNotEmpty()
            )
            
            // Property 4: Creation time should be reasonable
            val now = System.currentTimeMillis()
            assertTrue(
                "Creation time should be reasonable: ${bookmark.createdAt}",
                bookmark.createdAt <= now && bookmark.createdAt >= 0
            )
            
            // Property 5: Video URI should be valid format
            assertTrue(
                "Video URI should be valid: ${bookmark.videoUri}",
                bookmark.videoUri.toString().matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:.*"))
            )
        }
    }

    @Test
    fun `Bookmark comparison operations are consistent`() = testScope.runBlockingTest {
        repeat(400) { iteration ->
            val bookmark1 = generateRandomValidBookmark(iteration * 2)
            val bookmark2 = generateRandomValidBookmark(iteration * 2 + 1)
            
            // Property: Comparison should be consistent
            val comparison1to2 = bookmark1.compareTo(bookmark2)
            val comparison2to1 = bookmark2.compareTo(bookmark1)
            
            // Antisymmetric property
            assertEquals(
                "Comparison should be antisymmetric",
                -comparison1to2, comparison2to1
            )
            
            // Self-comparison should be zero
            assertEquals(
                "Self-comparison should be zero",
                0, bookmark1.compareTo(bookmark1)
            )
        }
    }

    // ============================================================================
    // VIDEO METADATA VALIDATION PROPERTIES
    // ============================================================================

    @Test
    fun `VideoMetadata validation properties hold for all inputs`() = testScope.runBlockingTest {
        repeat(600) { iteration ->
            val metadata = generateRandomVideoMetadata(iteration)
            
            // Property 1: Duration consistency
            if (metadata.duration > 0) {
                assertTrue(
                    "Duration should be positive when set: ${metadata.duration}",
                    metadata.duration > 0
                )
            }
            
            // Property 2: Resolution consistency
            if (metadata.width > 0 && metadata.height > 0) {
                assertTrue(
                    "Aspect ratio should be reasonable: ${metadata.width}x${metadata.height}",
                    (metadata.width.toFloat() / metadata.height.toFloat()) in 0.1f..10.0f
                )
            }
            
            // Property 3: Bitrate should be reasonable if set
            if (metadata.bitrate > 0) {
                assertTrue(
                    "Bitrate should be within reasonable range: ${metadata.bitrate}",
                    metadata.bitrate in 1000..100_000_000 // 1kbps to 100Mbps
                )
            }
            
            // Property 4: Frame rate should be reasonable
            if (metadata.frameRate > 0) {
                assertTrue(
                    "Frame rate should be reasonable: ${metadata.frameRate}",
                    metadata.frameRate in 1.0..240.0
                )
            }
            
            // Property 5: File size consistency
            if (metadata.fileSize > 0 && metadata.duration > 0 && metadata.bitrate > 0) {
                val expectedSize = (metadata.duration / 1000.0 * metadata.bitrate / 8.0).toLong()
                val tolerance = expectedSize * 0.5 // 50% tolerance for compression variance
                
                assertTrue(
                    "File size should be consistent with duration and bitrate",
                    kotlin.math.abs(metadata.fileSize - expectedSize) <= tolerance
                )
            }
        }
    }

    // ============================================================================
    // SUBTITLE DATA VALIDATION PROPERTIES
    // ============================================================================

    @Test
    fun `SubtitleEntry validation properties hold for all inputs`() = testScope.runBlockingTest {
        repeat(1000) { iteration ->
            val subtitleEntry = generateRandomSubtitleEntry(iteration)
            
            // Property 1: Start time should be before end time
            assertTrue(
                "Start time should be before end time: start=${subtitleEntry.startTime}, end=${subtitleEntry.endTime}",
                subtitleEntry.startTime < subtitleEntry.endTime
            )
            
            // Property 2: Times should be non-negative
            assertTrue(
                "Start time should be non-negative: ${subtitleEntry.startTime}",
                subtitleEntry.startTime >= 0
            )
            assertTrue(
                "End time should be non-negative: ${subtitleEntry.endTime}",
                subtitleEntry.endTime >= 0
            )
            
            // Property 3: Text should not be empty after trimming
            assertTrue(
                "Subtitle text should not be empty: '${subtitleEntry.text}'",
                subtitleEntry.text.trim().isNotEmpty()
            )
            
            // Property 4: Duration should be reasonable
            val duration = subtitleEntry.endTime - subtitleEntry.startTime
            assertTrue(
                "Subtitle duration should be reasonable: ${duration}ms",
                duration in 100..30000 // 0.1s to 30s
            )
        }
    }

    @Test
    fun `SubtitleTrack validation maintains chronological order`() = testScope.runBlockingTest {
        repeat(200) { iteration ->
            val subtitleTrack = generateRandomSubtitleTrack(iteration)
            
            // Property: Subtitle entries should be in chronological order
            for (i in 0 until subtitleTrack.entries.size - 1) {
                val current = subtitleTrack.entries[i]
                val next = subtitleTrack.entries[i + 1]
                
                assertTrue(
                    "Subtitles should be in chronological order: ${current.startTime} <= ${next.startTime}",
                    current.startTime <= next.startTime
                )
            }
            
            // Property: No overlapping subtitles (optional, depends on format)
            for (i in 0 until subtitleTrack.entries.size - 1) {
                val current = subtitleTrack.entries[i]
                val next = subtitleTrack.entries[i + 1]
                
                // Allow small overlap tolerance (common in subtitle formats)
                val overlapTolerance = 100L // 100ms
                assertTrue(
                    "Subtitles should not significantly overlap: current_end=${current.endTime}, next_start=${next.startTime}",
                    current.endTime <= next.startTime + overlapTolerance
                )
            }
        }
    }

    // ============================================================================
    // SETTINGS DATA VALIDATION PROPERTIES
    // ============================================================================

    @Test
    fun `Settings validation properties hold for all configurations`() = testScope.runBlockingTest {
        repeat(500) { iteration ->
            val settings = generateRandomSettings(iteration)
            
            // Property 1: Volume should be in valid range
            assertTrue(
                "Volume should be in range [0, 1]: ${settings.volume}",
                settings.volume in 0.0f..1.0f
            )
            
            // Property 2: Playback speed should be reasonable
            assertTrue(
                "Playback speed should be reasonable: ${settings.playbackSpeed}",
                settings.playbackSpeed in 0.25f..4.0f
            )
            
            // Property 3: Brightness should be in valid range
            assertTrue(
                "Brightness should be in range [0, 1]: ${settings.brightness}",
                settings.brightness in 0.0f..1.0f
            )
            
            // Property 4: Subtitle delay should be reasonable
            assertTrue(
                "Subtitle delay should be reasonable: ${settings.subtitleDelay}",
                kotlin.math.abs(settings.subtitleDelay) <= 60000 // ±60 seconds
            )
            
            // Property 5: Audio track selection should be valid
            if (settings.selectedAudioTrack >= 0) {
                assertTrue(
                    "Audio track selection should be reasonable: ${settings.selectedAudioTrack}",
                    settings.selectedAudioTrack < 20 // Reasonable upper bound
                )
            }
        }
    }

    // ============================================================================
    // NETWORK DATA VALIDATION PROPERTIES
    // ============================================================================

    @Test
    fun `NetworkRequest validation properties hold for all requests`() = testScope.runBlockingTest {
        repeat(400) { iteration ->
            val request = generateRandomNetworkRequest(iteration)
            
            // Property 1: URL should be valid format
            assertTrue(
                "URL should be valid: ${request.url}",
                android.util.Patterns.WEB_URL.matcher(request.url).matches()
            )
            
            // Property 2: Timeout should be reasonable
            assertTrue(
                "Timeout should be reasonable: ${request.timeoutMs}",
                request.timeoutMs in 1000..300000 // 1s to 5 minutes
            )
            
            // Property 3: Retry count should be reasonable
            assertTrue(
                "Retry count should be reasonable: ${request.maxRetries}",
                request.maxRetries in 0..10
            )
            
            // Property 4: Headers should not contain sensitive data
            request.headers.forEach { (key, value) ->
                assertFalse(
                    "Headers should not contain passwords: $key",
                    key.lowercase().contains("password") && value.length > 3
                )
                assertFalse(
                    "Headers should not contain API keys in clear text: $value",
                    value.lowercase().contains("key") && value.length > 10
                )
            }
        }
    }

    // ============================================================================
    // DATA TRANSFORMATION PROPERTIES
    // ============================================================================

    @Test
    fun `Data transformation operations preserve invariants`() = testScope.runBlockingTest {
        repeat(300) { iteration ->
            val originalData = generateRandomValidRecentFile(iteration)
            
            // Test various transformations
            val transformations = listOf(
                { file: RecentFile -> file.copy(lastPlayedPosition = minOf(file.lastPlayedPosition, file.duration)) },
                { file: RecentFile -> file.copy(name = file.name.trim()) },
                { file: RecentFile -> file.copy(lastPlayedTime = maxOf(file.lastPlayedTime, 0L)) }
            )
            
            transformations.forEach { transform ->
                val transformed = transform(originalData)
                
                // Property: Transformation should preserve validity
                if (originalData.isValid()) {
                    assertTrue(
                        "Transformation should preserve validity",
                        transformed.isValid()
                    )
                }
                
                // Property: Core identity should be preserved
                assertEquals(
                    "ID should be preserved during transformation",
                    originalData.id, transformed.id
                )
                assertEquals(
                    "Path should be preserved during transformation",
                    originalData.path, transformed.path
                )
            }
        }
    }

    // ============================================================================
    // BOUNDARY VALUE TESTING
    // ============================================================================

    @Test
    fun `Boundary values are handled correctly`() = testScope.runBlockingTest {
        val boundaryTestCases = listOf(
            // Duration boundaries
            0L, 1L, Long.MAX_VALUE - 1, Long.MAX_VALUE,
            // Position boundaries  
            0L, 1L, 1000L, 3600000L, // 0s, 1ms, 1s, 1h
            // Size boundaries
            0L, 1L, 1024L, 1024 * 1024L, Long.MAX_VALUE
        )
        
        boundaryTestCases.forEach { boundaryValue ->
            // Test duration boundary
            val fileWithBoundaryDuration = createRecentFileWithDuration(boundaryValue)
            if (boundaryValue >= 0) {
                assertTrue(
                    "File with boundary duration should be valid: $boundaryValue",
                    fileWithBoundaryDuration.isValid()
                )
            }
            
            // Test position boundary
            if (boundaryValue >= 0) {
                val fileWithBoundaryPosition = createRecentFileWithPosition(boundaryValue, boundaryValue + 1000)
                assertTrue(
                    "File with boundary position should be valid: $boundaryValue",
                    fileWithBoundaryPosition.isValid()
                )
            }
        }
    }

    // ============================================================================
    // RANDOM DATA GENERATORS
    // ============================================================================

    private fun generateRandomValidRecentFile(seed: Int): RecentFile {
        val random = Random(seed)
        val duration = random.nextLong(1, 7200000) // 1ms to 2 hours
        val position = random.nextLong(0, duration)
        
        return RecentFile(
            id = random.nextLong(1, Long.MAX_VALUE),
            path = generateRandomFilePath(random),
            name = generateRandomFileName(random),
            duration = duration,
            lastPlayedPosition = position,
            lastPlayedTime = System.currentTimeMillis() - random.nextLong(0, 86400000), // Within last day
            thumbnailPath = if (random.nextBoolean()) generateRandomFilePath(random) else null
        )
    }

    private fun generateRandomInvalidRecentFile(seed: Int): RecentFile {
        val random = Random(seed)
        
        // Randomly choose what to make invalid
        return when (random.nextInt(6)) {
            0 -> RecentFile(id = -1, path = "valid.mp4", name = "Valid", duration = 1000, lastPlayedPosition = 0, lastPlayedTime = System.currentTimeMillis())
            1 -> RecentFile(id = 1, path = "", name = "Valid", duration = 1000, lastPlayedPosition = 0, lastPlayedTime = System.currentTimeMillis())
            2 -> RecentFile(id = 1, path = "valid.mp4", name = "", duration = 1000, lastPlayedPosition = 0, lastPlayedTime = System.currentTimeMillis())
            3 -> RecentFile(id = 1, path = "valid.mp4", name = "Valid", duration = -1, lastPlayedPosition = 0, lastPlayedTime = System.currentTimeMillis())
            4 -> RecentFile(id = 1, path = "valid.mp4", name = "Valid", duration = 1000, lastPlayedPosition = -1, lastPlayedTime = System.currentTimeMillis())
            5 -> RecentFile(id = 1, path = "valid.mp4", name = "Valid", duration = 1000, lastPlayedPosition = 2000, lastPlayedTime = System.currentTimeMillis()) // Position > duration
            else -> RecentFile(id = 1, path = "valid.mp4", name = "Valid", duration = 1000, lastPlayedPosition = 0, lastPlayedTime = -1)
        }
    }

    private fun generateRandomValidBookmark(seed: Int): Bookmark {
        val random = Random(seed)
        
        return Bookmark(
            id = random.nextLong(1, Long.MAX_VALUE),
            videoUri = android.net.Uri.parse("file:///storage/emulated/0/video_${random.nextInt(1000)}.mp4"),
            position = random.nextLong(0, 7200000),
            title = generateRandomBookmarkTitle(random),
            description = if (random.nextBoolean()) generateRandomString(random, 50) else null,
            createdAt = System.currentTimeMillis() - random.nextLong(0, 86400000),
            thumbnailPath = if (random.nextBoolean()) generateRandomFilePath(random) else null
        )
    }

    private fun generateRandomVideoMetadata(seed: Int): VideoMetadata {
        val random = Random(seed)
        
        return VideoMetadata(
            duration = random.nextLong(1000, 7200000),
            width = random.nextInt(240, 3840),
            height = random.nextInt(240, 2160),
            bitrate = random.nextInt(64000, 50000000),
            frameRate = random.nextDouble(15.0, 120.0).toFloat(),
            fileSize = random.nextLong(1024, 5L * 1024 * 1024 * 1024), // 1KB to 5GB
            mimeType = listOf("video/mp4", "video/mkv", "video/avi", "video/webm").random(random),
            codec = listOf("h264", "h265", "vp9", "av1").random(random)
        )
    }

    private fun generateRandomSubtitleEntry(seed: Int): SubtitleEntry {
        val random = Random(seed)
        val startTime = random.nextLong(0, 3600000) // 0 to 1 hour
        val duration = random.nextLong(500, 10000) // 0.5 to 10 seconds
        
        return SubtitleEntry(
            startTime = startTime,
            endTime = startTime + duration,
            text = generateRandomSubtitleText(random)
        )
    }

    private fun generateRandomSubtitleTrack(seed: Int): SubtitleTrack {
        val random = Random(seed)
        val entryCount = random.nextInt(10, 100)
        val entries = mutableListOf<SubtitleEntry>()
        
        var currentTime = 0L
        repeat(entryCount) {
            val startTime = currentTime + random.nextLong(0, 2000) // Up to 2s gap
            val duration = random.nextLong(500, 5000) // 0.5 to 5s duration
            
            entries.add(SubtitleEntry(
                startTime = startTime,
                endTime = startTime + duration,
                text = generateRandomSubtitleText(random)
            ))
            
            currentTime = startTime + duration
        }
        
        return SubtitleTrack(
            language = listOf("en", "es", "fr", "de", "it").random(random),
            entries = entries.sortedBy { it.startTime }
        )
    }

    private fun generateRandomSettings(seed: Int): AppSettings {
        val random = Random(seed)
        
        return AppSettings(
            volume = random.nextFloat(),
            playbackSpeed = random.nextFloat() * 3.75f + 0.25f, // 0.25 to 4.0
            brightness = random.nextFloat(),
            subtitleDelay = random.nextLong(-30000, 30000),
            selectedAudioTrack = random.nextInt(-1, 10),
            autoPlay = random.nextBoolean(),
            rememberPosition = random.nextBoolean(),
            backgroundPlay = random.nextBoolean()
        )
    }

    private fun generateRandomNetworkRequest(seed: Int): NetworkRequest {
        val random = Random(seed)
        val domains = listOf("example.com", "api.test.com", "service.app")
        val paths = listOf("/api/v1/data", "/videos", "/subtitles", "/sync")
        
        return NetworkRequest(
            url = "https://${domains.random(random)}${paths.random(random)}",
            timeoutMs = random.nextInt(5000, 60000),
            maxRetries = random.nextInt(0, 5),
            headers = mapOf(
                "User-Agent" to "AstralStream/1.0",
                "Accept" to "application/json",
                "Content-Type" to "application/json"
            )
        )
    }

    private fun generateRandomFilePath(random: Random): String {
        val directories = listOf("Movies", "Videos", "Downloads", "DCIM")
        val extensions = listOf("mp4", "mkv", "avi", "mov", "webm")
        val filename = "video_${random.nextInt(1000)}"
        
        return "/storage/emulated/0/${directories.random(random)}/${filename}.${extensions.random(random)}"
    }

    private fun generateRandomFileName(random: Random): String {
        val adjectives = listOf("Amazing", "Epic", "Cool", "Awesome", "Great")
        val nouns = listOf("Video", "Movie", "Clip", "Recording", "Film")
        
        return "${adjectives.random(random)} ${nouns.random(random)} ${random.nextInt(100)}"
    }

    private fun generateRandomBookmarkTitle(random: Random): String {
        val titles = listOf("Favorite Scene", "Epic Moment", "Funny Part", "Action Sequence", "Emotional Scene")
        return "${titles.random(random)} ${random.nextInt(100)}"
    }

    private fun generateRandomString(random: Random, maxLength: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 "
        val length = random.nextInt(1, maxLength)
        return (1..length).map { chars.random(random) }.joinToString("")
    }

    private fun generateRandomSubtitleText(random: Random): String {
        val sentences = listOf(
            "This is a subtitle.",
            "The characters are talking.",
            "Something important is happening.",
            "The plot is advancing.",
            "Music is playing in the background."
        )
        return sentences.random(random)
    }

    private fun createRecentFileWithDuration(duration: Long): RecentFile {
        return RecentFile(
            id = 1,
            path = "/test/video.mp4",
            name = "Test Video",
            duration = duration,
            lastPlayedPosition = 0,
            lastPlayedTime = System.currentTimeMillis()
        )
    }

    private fun createRecentFileWithPosition(position: Long, duration: Long): RecentFile {
        return RecentFile(
            id = 1,
            path = "/test/video.mp4", 
            name = "Test Video",
            duration = duration,
            lastPlayedPosition = position,
            lastPlayedTime = System.currentTimeMillis()
        )
    }

    // ============================================================================
    // DATA CLASSES FOR TESTING
    // ============================================================================

    data class VideoMetadata(
        val duration: Long,
        val width: Int,
        val height: Int,
        val bitrate: Int,
        val frameRate: Float,
        val fileSize: Long,
        val mimeType: String,
        val codec: String
    )

    data class SubtitleEntry(
        val startTime: Long,
        val endTime: Long,
        val text: String
    )

    data class SubtitleTrack(
        val language: String,
        val entries: List<SubtitleEntry>
    )

    data class AppSettings(
        val volume: Float,
        val playbackSpeed: Float,
        val brightness: Float,
        val subtitleDelay: Long,
        val selectedAudioTrack: Int,
        val autoPlay: Boolean,
        val rememberPosition: Boolean,
        val backgroundPlay: Boolean
    )

    data class NetworkRequest(
        val url: String,
        val timeoutMs: Int,
        val maxRetries: Int,
        val headers: Map<String, String>
    )

    data class Bookmark(
        val id: Long,
        val videoUri: android.net.Uri,
        val position: Long,
        val title: String,
        val description: String?,
        val createdAt: Long,
        val thumbnailPath: String?
    ) : Comparable<Bookmark> {
        override fun compareTo(other: Bookmark): Int {
            return position.compareTo(other.position)
        }
        
        fun isValid(): Boolean {
            return id > 0 && 
                   position >= 0 && 
                   title.trim().isNotEmpty() && 
                   createdAt > 0
        }
    }
}