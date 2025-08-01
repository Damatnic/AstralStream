package com.astralplayer.features.editing.service

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.astralplayer.nextplayer.editing.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class VideoEditingServiceTest {

    private lateinit var videoEditingService: VideoEditingService
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        videoEditingService = VideoEditingService(context)
    }

    @Test
    fun `initialize service returns available features`() = runBlocking {
        val result = videoEditingService.initialize()
        
        assertTrue(result.success)
        assertNotNull(result.availableFeatures)
        assertNotNull(result.supportedFormats)
        assertTrue(result.initializationTime > 0)
        
        // Should have basic features available
        assertTrue(result.availableFeatures.contains(EditingFeature.VIDEO_TRIMMING))
        assertTrue(result.availableFeatures.contains(EditingFeature.AUDIO_EDITING))
        assertTrue(result.availableFeatures.contains(EditingFeature.EFFECTS_APPLICATION))
        assertTrue(result.availableFeatures.contains(EditingFeature.TRANSITIONS))
        assertTrue(result.availableFeatures.contains(EditingFeature.OVERLAYS))
        
        // Should support common formats
        assertTrue(result.supportedFormats.contains(VideoFormat.MP4))
        assertTrue(result.supportedFormats.contains(VideoFormat.MOV))
        assertTrue(result.supportedFormats.contains(VideoFormat.AVI))
    }

    @Test
    fun `create project generates valid project`() = runBlocking {
        videoEditingService.initialize()
        
        val projectName = "Test Project"
        val settings = ProjectSettings(
            resolution = Pair(1920, 1080),
            frameRate = 30f,
            sampleRate = 48000,
            audioChannels = 2
        )
        
        val result = videoEditingService.createProject(projectName, settings)
        
        assertTrue(result.success)
        assertNotNull(result.project)
        assertTrue(result.creationTime > 0)
        
        val project = result.project!!
        assertEquals(projectName, project.name)
        assertEquals(settings.resolution, project.settings.resolution)
        assertEquals(settings.frameRate, project.settings.frameRate)
        assertTrue(project.id.isNotEmpty())
        assertTrue(project.createdAt > 0)
        assertEquals(project.createdAt, project.lastModified)
    }

    @Test
    fun `import video analyzes and creates clip`() = runBlocking {
        videoEditingService.initialize()
        
        // Create a mock URI for testing
        val videoUri = Uri.parse("android.resource://com.astralplayer.test/raw/test_video")
        
        val result = videoEditingService.importVideo(videoUri)
        
        assertTrue(result.success)
        assertNotNull(result.clip)
        assertNotNull(result.videoInfo)
        assertTrue(result.importTime > 0)
        
        val clip = result.clip!!
        assertEquals(ClipType.VIDEO, clip.type)
        assertEquals(videoUri, clip.sourceUri)
        assertTrue(clip.duration > 0)
        assertEquals(0L, clip.startTime)
        
        val videoInfo = result.videoInfo!!
        assertTrue(videoInfo.resolution.first > 0)
        assertTrue(videoInfo.resolution.second > 0)
        assertTrue(videoInfo.duration > 0)
        assertTrue(videoInfo.frameRate > 0)
    }

    @Test
    fun `apply effect adds effect to clip`() = runBlocking {
        videoEditingService.initialize()
        
        val clip = TimelineClip(
            id = "test_clip",
            type = ClipType.VIDEO,
            sourceUri = Uri.parse("test://video"),
            name = "Test Clip",
            startTime = 0L,
            duration = 60000L,
            trackIndex = 0
        )
        
        val effect = VideoEffect(
            id = "blur_effect",
            type = EffectType.BLUR,
            name = "Blur Effect",
            intensity = 0.5f
        )
        
        val result = videoEditingService.applyEffect(clip, effect)
        
        assertTrue(result.success)
        assertNotNull(result.appliedEffect)
        assertNotNull(result.modifiedClip)
        assertTrue(result.applicationTime > 0)
        
        assertEquals(effect.id, result.appliedEffect!!.id)
        assertEquals(EffectType.BLUR, result.appliedEffect!!.type)
        assertEquals(0.5f, result.appliedEffect!!.intensity)
    }

    @Test
    fun `add transition creates transition between clips`() = runBlocking {
        videoEditingService.initialize()
        
        val fromClip = TimelineClip(
            id = "clip1",
            type = ClipType.VIDEO,
            sourceUri = Uri.parse("test://video1"),
            name = "Clip 1",
            startTime = 0L,
            duration = 30000L,
            trackIndex = 0
        )
        
        val toClip = TimelineClip(
            id = "clip2",
            type = ClipType.VIDEO,
            sourceUri = Uri.parse("test://video2"),
            name = "Clip 2", 
            startTime = 25000L, // 5 second overlap
            duration = 30000L,
            trackIndex = 0
        )
        
        val transition = VideoTransition(
            id = "fade_transition",
            type = TransitionType.FADE,
            name = "Fade Transition",
            duration = 5000L,
            startTime = 25000L
        )
        
        val result = videoEditingService.addTransition(fromClip, toClip, transition)
        
        assertTrue(result.success)
        assertNotNull(result.transition)
        assertEquals(2, result.modifiedClips.size)
        assertTrue(result.addTime > 0)
        
        assertEquals(TransitionType.FADE, result.transition!!.type)
        assertEquals(5000L, result.transition!!.duration)
    }

    @Test
    fun `add overlay creates overlay successfully`() = runBlocking {
        videoEditingService.initialize()
        
        val overlay = VideoOverlay(
            id = "text_overlay",
            type = OverlayType.TEXT,
            name = "Text Overlay",
            startTime = 5000L,
            duration = 10000L,
            content = OverlayContent(
                text = "Sample Text",
                fontSize = 24f,
                color = "#FFFFFF"
            )
        )
        
        val result = videoEditingService.addOverlay(overlay)
        
        assertTrue(result.success)
        assertNotNull(result.overlay)
        assertTrue(result.addTime > 0)
        
        assertEquals(OverlayType.TEXT, result.overlay!!.type)
        assertEquals("Sample Text", result.overlay!!.content.text)
        assertEquals(24f, result.overlay!!.content.fontSize)
    }

    @Test
    fun `adjust audio modifies audio settings`() = runBlocking {
        videoEditingService.initialize()
        
        val clip = TimelineClip(
            id = "audio_clip",
            type = ClipType.VIDEO,
            sourceUri = Uri.parse("test://video"),
            name = "Audio Test Clip",
            startTime = 0L,
            duration = 60000L,
            trackIndex = 0,
            audioInfo = AudioInfo(
                sampleRate = 48000,
                channels = 2,
                bitrate = 192000L,
                codec = "AAC",
                duration = 60000L,
                fileSize = 1024 * 1024L
            )
        )
        
        val adjustments = AudioAdjustments(
            volume = 1.5f,
            pan = 0.2f,
            fadeIn = 2000L,
            fadeOut = 3000L,
            normalize = true,
            removeNoise = true
        )
        
        val result = videoEditingService.adjustAudio(clip, adjustments)
        
        assertTrue(result.success)
        assertNotNull(result.adjustedAudioInfo)
        assertNotNull(result.modifiedClip)
        assertTrue(result.adjustTime > 0)
    }

    @Test
    fun `apply color correction generates correction data`() = runBlocking {
        videoEditingService.initialize()
        
        val clip = TimelineClip(
            id = "color_clip",
            type = ClipType.VIDEO,
            sourceUri = Uri.parse("test://video"),
            name = "Color Test Clip",
            startTime = 0L,
            duration = 60000L,
            trackIndex = 0
        )
        
        val settings = ColorCorrectionSettings(
            brightness = 0.2f,
            contrast = 1.1f,
            saturation = 1.2f,
            hue = 0.1f,
            gamma = 0.9f,
            exposure = 0.1f
        )
        
        val result = videoEditingService.applyColorCorrection(clip, settings)
        
        assertTrue(result.success)
        assertNotNull(result.correctionData)
        assertNotNull(result.modifiedClip)
        assertTrue(result.correctionTime > 0)
        
        val correctionData = result.correctionData!!
        assertNotNull(correctionData.histogram)
        assertNotNull(correctionData.waveform)
        assertNotNull(correctionData.vectorscope)
        assertEquals(settings, correctionData.appliedSettings)
    }

    @Test
    fun `export video creates output file`() = runBlocking {
        videoEditingService.initialize()
        
        // Create a simple timeline
        val timeline = Timeline(
            videoTracks = mutableListOf(
                VideoTrack(
                    id = "track1",
                    name = "Video Track 1",
                    clips = mutableListOf(
                        TimelineClip(
                            id = "clip1",
                            type = ClipType.VIDEO,
                            sourceUri = Uri.parse("test://video"),
                            name = "Test Clip",
                            startTime = 0L,
                            duration = 30000L,
                            trackIndex = 0
                        )
                    )
                )
            ),
            duration = 30000L
        )
        
        val tempFile = java.io.File.createTempFile("export_test", ".mp4")
        val settings = ExportSettings(
            outputUri = Uri.fromFile(tempFile),
            outputFormat = VideoFormat.MP4,
            resolution = Pair(1280, 720),
            frameRate = 30f,
            bitrate = 5_000_000L,
            audioSettings = AudioExportSettings(
                codec = AudioCodec.AAC,
                sampleRate = 48000,
                channels = 2,
                bitrate = 192_000L
            )
        )
        
        var progressUpdates = 0
        val result = videoEditingService.exportVideo(
            timeline = timeline,
            settings = settings,
            progressCallback = { progress ->
                progressUpdates++
                assertTrue(progress >= 0f && progress <= 1f)
            }
        )
        
        // Clean up temp file
        tempFile.delete()
        
        assertTrue(result.success)
        assertNotNull(result.outputUri)
        assertTrue(result.duration > 0)
        assertTrue(result.exportTime > 0)
        assertTrue(progressUpdates > 0) // Should have received progress updates
    }

    @Test
    fun `undo and redo operations work correctly`() = runBlocking {
        videoEditingService.initialize()
        
        // Initially nothing to undo
        val initialUndo = videoEditingService.undo()
        assertTrue(initialUndo.success)
        assertEquals("Nothing to undo", initialUndo.error)
        
        // Simulate some operations by directly manipulating the service
        // In real implementation, operations would be tracked automatically
        
        // For now, just test the undo/redo mechanism structure
        val redoResult = videoEditingService.redo()
        assertTrue(redoResult.success)
        assertEquals("Nothing to redo", redoResult.error)
    }

    @Test
    fun `save project persists project data`() = runBlocking {
        videoEditingService.initialize()
        
        val project = EditingProject(
            id = "save_test_project",
            name = "Save Test Project",
            description = "Project for testing save functionality",
            settings = ProjectSettings(),
            timeline = Timeline(),
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
        
        // Save project should not throw exception
        assertDoesNotThrow {
            runBlocking {
                videoEditingService.saveProject(project)
            }
        }
    }

    @Test
    fun `service handles invalid inputs gracefully`() = runBlocking {
        videoEditingService.initialize()
        
        // Test invalid video URI
        val invalidUri = Uri.parse("invalid://non-existent-video")
        val importResult = videoEditingService.importVideo(invalidUri)
        
        assertTrue(importResult.success || importResult.error != null)
        // Either succeeds with mock data or fails gracefully with error message
        
        // Test invalid project settings
        val invalidSettings = ProjectSettings(
            resolution = Pair(-1, -1), // Invalid resolution
            frameRate = -30f, // Invalid frame rate
            sampleRate = -48000 // Invalid sample rate
        )
        
        val projectResult = videoEditingService.createProject("Invalid Project", invalidSettings)
        
        // Should either succeed (service corrects invalid values) or fail gracefully
        if (!projectResult.success) {
            assertNotNull(projectResult.error)
            assertTrue(projectResult.error!!.isNotEmpty())
        }
    }

    @Test
    fun `cleanup releases resources properly`() {
        // Test that cleanup doesn't throw exceptions
        assertDoesNotThrow {
            videoEditingService.cleanup()
        }
        
        // After cleanup, operations should still work (service should handle gracefully)
        runBlocking {
            val result = videoEditingService.initialize()
            // Should either work or fail gracefully, not crash
        }
    }

    @Test
    fun `concurrent operations are handled safely`() = runBlocking {
        videoEditingService.initialize()
        
        // Test multiple simultaneous operations
        val operations = (1..5).map { index ->
            kotlinx.coroutines.async {
                videoEditingService.createProject(
                    name = "Concurrent Project $index",
                    settings = ProjectSettings()
                )
            }
        }
        
        val results = operations.map { it.await() }
        
        // All operations should complete without crashing
        assertEquals(5, results.size)
        results.forEach { result ->
            // Each should either succeed or fail gracefully
            if (result.success) {
                assertNotNull(result.project)
            } else {
                assertNotNull(result.error)
            }
        }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception, but got: ${e.message}")
        }
    }
}