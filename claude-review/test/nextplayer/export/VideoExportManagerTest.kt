package com.astralplayer.nextplayer.export

import android.content.Context
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class VideoExportManagerTest {

    private lateinit var mockContext: Context
    private lateinit var exportManager: VideoExportManager
    private lateinit var mockUri: Uri

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockUri = mockk(relaxed = true)
        exportManager = VideoExportManager(mockContext)
    }

    @Test
    fun `test initial export state is idle`() = runTest {
        assertEquals(VideoExportManager.ExportState.IDLE, exportManager.exportState.value)
        assertEquals(0f, exportManager.exportProgress.value, 0.01f)
    }

    @Test
    fun `test export options creation with default values`() {
        val options = VideoExportManager.ExportOptions(
            inputUri = mockUri,
            outputPath = "/storage/output.mp4",
            title = "Test Video"
        )

        assertEquals(mockUri, options.inputUri)
        assertEquals("/storage/output.mp4", options.outputPath)
        assertEquals("Test Video", options.title)
        assertEquals(VideoExportManager.OutputFormat.MP4, options.format)
        assertEquals(VideoExportManager.Quality.HIGH, options.quality)
        assertEquals(0L, options.startTimeMs)
        assertEquals(Long.MAX_VALUE, options.endTimeMs)
        assertTrue(options.includeAudio)
    }

    @Test
    fun `test export options creation with custom values`() {
        val options = VideoExportManager.ExportOptions(
            inputUri = mockUri,
            outputPath = "/storage/output.webm",
            title = "Custom Video",
            format = VideoExportManager.OutputFormat.WEBM,
            quality = VideoExportManager.Quality.MEDIUM,
            startTimeMs = 5000L,
            endTimeMs = 30000L,
            includeAudio = false
        )

        assertEquals(VideoExportManager.OutputFormat.WEBM, options.format)
        assertEquals(VideoExportManager.Quality.MEDIUM, options.quality)
        assertEquals(5000L, options.startTimeMs)
        assertEquals(30000L, options.endTimeMs)
        assertFalse(options.includeAudio)
    }

    @Test
    fun `test quality enum provides correct bitrate values`() {
        assertEquals(2000000, VideoExportManager.Quality.LOW.videoBitrate)
        assertEquals(5000000, VideoExportManager.Quality.MEDIUM.videoBitrate)
        assertEquals(8000000, VideoExportManager.Quality.HIGH.videoBitrate)
        assertEquals(15000000, VideoExportManager.Quality.ULTRA.videoBitrate)

        assertEquals(64000, VideoExportManager.Quality.LOW.audioBitrate)
        assertEquals(128000, VideoExportManager.Quality.MEDIUM.audioBitrate)
        assertEquals(192000, VideoExportManager.Quality.HIGH.audioBitrate)
        assertEquals(320000, VideoExportManager.Quality.ULTRA.audioBitrate)
    }

    @Test
    fun `test output format enum provides correct properties`() {
        assertEquals("mp4", VideoExportManager.OutputFormat.MP4.extension)
        assertEquals("video/mp4", VideoExportManager.OutputFormat.MP4.mimeType)
        assertEquals("video/avc", VideoExportManager.OutputFormat.MP4.videoCodec)
        assertEquals("audio/mp4a-latm", VideoExportManager.OutputFormat.MP4.audioCodec)

        assertEquals("webm", VideoExportManager.OutputFormat.WEBM.extension)
        assertEquals("video/webm", VideoExportManager.OutputFormat.WEBM.mimeType)
        assertEquals("video/x-vnd.on2.vp9", VideoExportManager.OutputFormat.WEBM.videoCodec)
        assertEquals("audio/opus", VideoExportManager.OutputFormat.WEBM.audioCodec)

        assertEquals("mkv", VideoExportManager.OutputFormat.MKV.extension)
        assertEquals("video/x-matroska", VideoExportManager.OutputFormat.MKV.mimeType)
        assertEquals("video/avc", VideoExportManager.OutputFormat.MKV.videoCodec)
        assertEquals("audio/mp4a-latm", VideoExportManager.OutputFormat.MKV.audioCodec)
    }

    @Test
    fun `test export state transitions`() {
        // Test all export states exist
        val states = VideoExportManager.ExportState.values()
        assertTrue(states.contains(VideoExportManager.ExportState.IDLE))
        assertTrue(states.contains(VideoExportManager.ExportState.PREPARING))
        assertTrue(states.contains(VideoExportManager.ExportState.EXPORTING))
        assertTrue(states.contains(VideoExportManager.ExportState.COMPLETED))
        assertTrue(states.contains(VideoExportManager.ExportState.ERROR))
        assertTrue(states.contains(VideoExportManager.ExportState.CANCELLED))
    }

    @Test
    fun `test export duration calculation`() {
        val options = VideoExportManager.ExportOptions(
            inputUri = mockUri,
            outputPath = "/storage/output.mp4",
            title = "Test Video",
            startTimeMs = 10000L,
            endTimeMs = 40000L
        )

        val duration = options.endTimeMs - options.startTimeMs
        assertEquals(30000L, duration) // 30 seconds
    }

    @Test
    fun `test export options with full video length`() {
        val options = VideoExportManager.ExportOptions(
            inputUri = mockUri,
            outputPath = "/storage/output.mp4",
            title = "Test Video",
            startTimeMs = 0L,
            endTimeMs = Long.MAX_VALUE
        )

        assertTrue("Should export full video when endTime is MAX_VALUE", 
            options.endTimeMs == Long.MAX_VALUE)
        assertEquals(0L, options.startTimeMs)
    }

    @Test
    fun `test generateOutputFilename creates unique filenames`() {
        val filename1 = VideoExportManager.generateUniqueFilename("test", "mp4")
        val filename2 = VideoExportManager.generateUniqueFilename("test", "mp4")
        
        assertNotEquals("Generated filenames should be unique", filename1, filename2)
        assertTrue("Filename should contain base name", filename1.contains("test"))
        assertTrue("Filename should have correct extension", filename1.endsWith(".mp4"))
    }

    @Test
    fun `test format validation`() {
        val validFormats = VideoExportManager.OutputFormat.values()
        
        // All formats should have non-empty properties
        validFormats.forEach { format ->
            assertFalse("Extension should not be empty", format.extension.isEmpty())
            assertFalse("MIME type should not be empty", format.mimeType.isEmpty())
            assertFalse("Video codec should not be empty", format.videoCodec.isEmpty())
            assertFalse("Audio codec should not be empty", format.audioCodec.isEmpty())
        }
    }

    @Test
    fun `test quality validation`() {
        val validQualities = VideoExportManager.Quality.values()
        
        // All qualities should have positive bitrates
        validQualities.forEach { quality ->
            assertTrue("Video bitrate should be positive", quality.videoBitrate > 0)
            assertTrue("Audio bitrate should be positive", quality.audioBitrate > 0)
        }
        
        // Qualities should be in ascending order
        assertTrue("LOW should have lowest video bitrate", 
            VideoExportManager.Quality.LOW.videoBitrate < VideoExportManager.Quality.MEDIUM.videoBitrate)
        assertTrue("MEDIUM should be less than HIGH", 
            VideoExportManager.Quality.MEDIUM.videoBitrate < VideoExportManager.Quality.HIGH.videoBitrate)
        assertTrue("HIGH should be less than ULTRA", 
            VideoExportManager.Quality.HIGH.videoBitrate < VideoExportManager.Quality.ULTRA.videoBitrate)
    }
}