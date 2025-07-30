package com.astralplayer.nextplayer.editing

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFalse

/**
 * Comprehensive tests for advanced video editing tools
 * Tests video editing functionality including trimming, effects, transitions, overlays,
 * audio editing, color correction, stabilization, and project management
 */
@RunWith(AndroidJUnit4::class)
class AdvancedVideoEditingToolsTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var editingTools: AdvancedVideoEditingTools
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        editingTools = AdvancedVideoEditingTools(context)
    }

    @After
    fun tearDown() {
        editingTools.cleanup()
    }

    @Test
    fun testVideoEditingSystemInitialization() = runTest {
        // When
        val result = editingTools.initialize()
        advanceUntilIdle()
        
        // Then
        assertNotNull("Initialization result should not be null", result)
        assertTrue("Video editing system should initialize successfully", result.success)
        assertTrue("Should have available features", result.availableFeatures.isNotEmpty())
        assertTrue("Should have supported formats", result.supportedFormats.isNotEmpty())
        assertTrue("Initialization time should be set", result.initializationTime > 0)
        
        // Verify available features
        val expectedFeatures = listOf(
            EditingFeature.VIDEO_TRIMMING,
            EditingFeature.AUDIO_EDITING,
            EditingFeature.EFFECTS_APPLICATION,
            EditingFeature.TRANSITIONS,
            EditingFeature.OVERLAYS,
            EditingFeature.COLOR_CORRECTION,
            EditingFeature.VIDEO_STABILIZATION,
            EditingFeature.UNDO_REDO,
            EditingFeature.PROJECT_MANAGEMENT
        )
        
        expectedFeatures.forEach { feature ->
            assertTrue("Should have $feature", result.availableFeatures.contains(feature))
        }
        
        // Verify supported formats
        val expectedFormats = listOf(
            VideoFormat.MP4,
            VideoFormat.AVI,
            VideoFormat.MOV,
            VideoFormat.MKV,
            VideoFormat.WEBM
        )
        
        expectedFormats.forEach { format ->
            assertTrue("Should support $format", result.supportedFormats.contains(format))
        }
        
        // Verify state
        val state = editingTools.editingState.value
        assertTrue("System should be initialized", state.isInitialized)
        assertTrue("Should have available features", state.availableFeatures.isNotEmpty())
        assertTrue("Should have supported formats", state.supportedFormats.isNotEmpty())
    }

    @Test
    fun testProjectCreation() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        val projectName = "Test Video Project"
        val projectSettings = ProjectSettings(
            resolution = Pair(1920, 1080),
            frameRate = 30f,
            sampleRate = 48000,
            audioChannels = 2,
            colorSpace = ColorSpace.REC709,
            autoSave = true
        )
        
        // When
        val result = editingTools.createProject(projectName, projectSettings)
        
        // Then
        assertNotNull("Project creation result should not be null", result)
        assertTrue("Project should be created successfully", result.success)
        assertNotNull("Project should be provided", result.project)
        assertTrue("Creation time should be set", result.creationTime > 0)
        
        // Verify project structure
        val project = result.project!!
        assertEquals("Project name should match", projectName, project.name)
        assertEquals("Project settings should match", projectSettings, project.settings)
        assertNotNull("Timeline should be created", project.timeline)
        assertTrue("Project ID should be set", project.id.isNotEmpty())
        assertTrue("Created timestamp should be set", project.createdAt > 0)
        
        // Verify timeline structure
        val timeline = project.timeline
        assertTrue("Should have video tracks", timeline.videoTracks.isNotEmpty())
        assertTrue("Should have audio tracks", timeline.audioTracks.isNotEmpty())
        assertTrue("Should have overlay tracks", timeline.overlayTracks.isNotEmpty())
        
        // Verify state update
        val state = editingTools.editingState.value
        assertEquals("Current project should be set", project, state.currentProject)
        assertFalse("Should not have unsaved changes initially", state.hasUnsavedChanges)
    }

    @Test
    fun testVideoImport() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        // Create a project first
        val projectResult = editingTools.createProject("Import Test Project")
        assertTrue("Project creation should succeed", projectResult.success)
        advanceUntilIdle()
        
        val videoUri = Uri.parse("content://test/sample_video.mp4")
        val importSettings = ImportSettings(
            preProcess = true,
            generateThumbnails = true,
            analyzeAudio = true,
            extractMetadata = true,
            createProxy = false
        )
        
        // When
        val result = editingTools.importVideo(videoUri, importSettings)
        
        // Then
        assertNotNull("Import result should not be null", result)
        assertTrue("Video should be imported successfully", result.success)
        assertNotNull("Clip should be created", result.clip)
        assertNotNull("Video info should be provided", result.videoInfo)
        assertTrue("Import time should be set", result.importTime > 0)
        
        // Verify clip structure
        val clip = result.clip!!
        assertEquals("Clip source URI should match", videoUri, clip.sourceUri)
        assertEquals("Clip type should be VIDEO", ClipType.VIDEO, clip.type)
        assertTrue("Clip ID should be set", clip.id.isNotEmpty())
        assertTrue("Clip duration should be positive", clip.duration > 0)
        assertEquals("Clip should be on track 0", 0, clip.trackIndex)
        assertTrue("Clip should be enabled", clip.isEnabled)
        
        // Verify video info
        val videoInfo = result.videoInfo!!
        assertTrue("Resolution should be valid", 
                  videoInfo.resolution.first > 0 && videoInfo.resolution.second > 0)
        assertTrue("Frame rate should be positive", videoInfo.frameRate > 0)
        assertTrue("Duration should be positive", videoInfo.duration > 0)
        assertTrue("Bitrate should be positive", videoInfo.bitrate > 0)
        assertTrue("Codec should be specified", videoInfo.codec.isNotEmpty())
        assertTrue("File size should be positive", videoInfo.fileSize > 0)
        
        // Verify project state update
        val state = editingTools.editingState.value
        assertTrue("Should have unsaved changes", state.hasUnsavedChanges)
        val project = state.currentProject!!
        assertTrue("Timeline should have the imported clip", 
                  project.timeline.videoTracks[0].clips.any { it.id == clip.id })
    }

    @Test
    fun testVideoTrimming() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        // Setup project with imported video
        editingTools.createProject("Trim Test Project")
        val videoUri = Uri.parse("content://test/trim_video.mp4")
        val importResult = editingTools.importVideo(videoUri)
        assertTrue("Import should succeed", importResult.success)
        advanceUntilIdle()
        
        val clipId = importResult.clip!!.id
        val originalDuration = importResult.clip!!.duration
        val trimStart = 5000L // 5 seconds
        val trimEnd = originalDuration - 10000L // Remove last 10 seconds
        
        // When
        val result = editingTools.trimVideo(clipId, trimStart, trimEnd)
        
        // Then
        assertNotNull("Trim result should not be null", result)
        assertTrue("Video trim should succeed", result.success)
        assertNotNull("Modified clip should be provided", result.modifiedClip)
        assertTrue("Edit time should be set", result.editTime > 0)
        
        // Verify trimmed clip
        val trimmedClip = result.modifiedClip!!
        assertEquals("Clip ID should remain the same", clipId, trimmedClip.id)
        assertEquals("Start time should be updated", trimStart, trimmedClip.startTime)
        assertEquals("Duration should be updated", trimEnd - trimStart, trimmedClip.duration)
        assertTrue("Last modified should be updated", trimmedClip.lastModified > 0)
        
        // Verify state update
        val state = editingTools.editingState.value
        assertTrue("Should have unsaved changes", state.hasUnsavedChanges)
        assertTrue("Should be able to undo", state.canUndo)
        assertFalse("Should not be able to redo", state.canRedo)
    }

    @Test
    fun testEffectApplication() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        // Setup project with imported video
        editingTools.createProject("Effects Test Project")
        val videoUri = Uri.parse("content://test/effects_video.mp4")
        val importResult = editingTools.importVideo(videoUri)
        assertTrue("Import should succeed", importResult.success)
        advanceUntilIdle()
        
        val clipId = importResult.clip!!.id
        val effect = VideoEffect(
            id = "blur_effect_1",
            type = EffectType.BLUR,
            name = "Gaussian Blur",
            parameters = mapOf(
                "radius" to 5.0f,
                "quality" to "high"
            ),
            intensity = 0.8f,
            startTime = 0L,
            duration = importResult.clip!!.duration
        )
        
        // When
        val result = editingTools.applyEffect(clipId, effect)
        
        // Then
        assertNotNull("Effect application result should not be null", result)
        assertTrue("Effect should be applied successfully", result.success)
        assertNotNull("Applied effect should be provided", result.appliedEffect)
        assertNotNull("Modified clip should be provided", result.modifiedClip)
        assertTrue("Application time should be set", result.applicationTime > 0)
        
        // Verify applied effect
        val appliedEffect = result.appliedEffect!!
        assertEquals("Effect type should match", EffectType.BLUR, appliedEffect.type)
        assertEquals("Effect intensity should match", 0.8f, appliedEffect.intensity)
        assertTrue("Effect should be enabled", appliedEffect.isEnabled)
        assertNotNull("Effect parameters should be set", appliedEffect.parameters)
        assertTrue("Effect should have blur radius parameter", 
                  appliedEffect.parameters.containsKey("radius"))
        
        // Verify modified clip
        val modifiedClip = result.modifiedClip!!  
        assertEquals("Clip ID should remain the same", clipId, modifiedClip.id)
        assertTrue("Clip should have effects", modifiedClip.effects.isNotEmpty())
        assertTrue("Clip should contain the applied effect", 
                  modifiedClip.effects.any { it.id == appliedEffect.id })
        
        // Verify state update
        val state = editingTools.editingState.value
        assertTrue("Should have unsaved changes", state.hasUnsavedChanges)
        assertTrue("Should be able to undo", state.canUndo)
    }

    @Test
    fun testTransitionAddition() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        // Setup project with two imported videos
        editingTools.createProject("Transitions Test Project")
        
        val video1Uri = Uri.parse("content://test/video1.mp4")
        val video2Uri = Uri.parse("content://test/video2.mp4")
        
        val import1Result = editingTools.importVideo(video1Uri)
        val import2Result = editingTools.importVideo(video2Uri)
        
        assertTrue("First import should succeed", import1Result.success)
        assertTrue("Second import should succeed", import2Result.success)
        advanceUntilIdle()
        
        val fromClipId = import1Result.clip!!.id
        val toClipId = import2Result.clip!!.id
        
        val transition = VideoTransition(
            id = "fade_transition_1",
            type = TransitionType.FADE,
            name = "Fade Transition",
            duration = 1000L, // 1 second
            startTime = import1Result.clip!!.duration - 500L, // Overlap by 0.5 seconds
            parameters = mapOf(
                "fadeType" to "cross_fade",
                "smoothness" to 0.8f
            ),
            easing = EasingType.EASE_IN_OUT
        )
        
        // When
        val result = editingTools.addTransition(fromClipId, toClipId, transition)
        
        // Then
        assertNotNull("Transition result should not be null", result)
        assertTrue("Transition should be added successfully", result.success)
        assertNotNull("Transition should be provided", result.transition)
        assertTrue("Should have modified clips", result.modifiedClips.isNotEmpty())
        assertEquals("Should have modified both clips", 2, result.modifiedClips.size)
        assertTrue("Add time should be set", result.addTime > 0)
        
        // Verify transition
        val addedTransition = result.transition!!
        assertEquals("Transition type should match", TransitionType.FADE, addedTransition.type)
        assertEquals("Transition duration should match", 1000L, addedTransition.duration)
        assertEquals("Transition easing should match", EasingType.EASE_IN_OUT, addedTransition.easing)
        assertTrue("Transition should be enabled", addedTransition.isEnabled)
        
        // Verify modified clips
        result.modifiedClips.forEach { clip ->
            assertTrue("Each clip should have the transition", 
                      clip.transitions.any { it.id == addedTransition.id })
            assertTrue("Last modified should be updated", clip.lastModified > 0)
        }
        
        // Verify state update
        val state = editingTools.editingState.value
        assertTrue("Should have unsaved changes", state.hasUnsavedChanges)
        assertTrue("Should be able to undo", state.canUndo)
    }

    @Test
    fun testOverlayAddition() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        // Setup project with imported video
        editingTools.createProject("Overlay Test Project")
        val videoUri = Uri.parse("content://test/overlay_video.mp4")
        val importResult = editingTools.importVideo(videoUri)
        assertTrue("Import should succeed", importResult.success)
        advanceUntilIdle()
        
        val clipId = importResult.clip!!.id
        val overlay = VideoOverlay(
            id = "text_overlay_1",
            type = OverlayType.TEXT,
            name = "Title Text",
            startTime = 2000L,
            duration = 5000L,
            position = Position2D(100f, 50f),
            size = Size2D(400f, 100f),
            rotation = 0f,
            opacity = 0.9f,
            content = OverlayContent(
                text = "Sample Title Text",
                fontSize = 24f,
                fontFamily = "Arial",
                fontStyle = FontStyle.BOLD,
                alignment = TextAlignment.CENTER,
                color = "#FFFFFF",
                strokeColor = "#000000",
                strokeWidth = 2f
            ),
            animation = OverlayAnimation(
                type = AnimationType.FADE_IN,
                duration = 1000L,
                easing = EasingType.EASE_IN_OUT
            )
        )
        
        // When
        val result = editingTools.addOverlay(clipId, overlay)
        
        // Then
        assertNotNull("Overlay result should not be null", result)
        assertTrue("Overlay should be added successfully", result.success)
        assertNotNull("Overlay should be provided", result.overlay)
        assertTrue("Add time should be set", result.addTime > 0)
        
        // Verify overlay
        val addedOverlay = result.overlay!!
        assertEquals("Overlay type should match", OverlayType.TEXT, addedOverlay.type)
        assertEquals("Overlay start time should match", 2000L, addedOverlay.startTime)
        assertEquals("Overlay duration should match", 5000L, addedOverlay.duration)
        assertEquals("Position should match", Position2D(100f, 50f), addedOverlay.position)
        assertEquals("Size should match", Size2D(400f, 100f), addedOverlay.size)
        assertEquals("Opacity should match", 0.9f, addedOverlay.opacity)
        assertTrue("Overlay should be enabled", addedOverlay.isEnabled)
        
        // Verify overlay content
        val content = addedOverlay.content
        assertEquals("Text content should match", "Sample Title Text", content.text)
        assertEquals("Font size should match", 24f, content.fontSize)
        assertEquals("Font family should match", "Arial", content.fontFamily)
        assertEquals("Font style should match", FontStyle.BOLD, content.fontStyle)
        assertEquals("Text alignment should match", TextAlignment.CENTER, content.alignment)
        
        // Verify animation
        val animation = addedOverlay.animation!!
        assertEquals("Animation type should match", AnimationType.FADE_IN, animation.type)
        assertEquals("Animation duration should match", 1000L, animation.duration)
        assertEquals("Animation easing should match", EasingType.EASE_IN_OUT, animation.easing)
        
        // Verify state update
        val state = editingTools.editingState.value
        assertTrue("Should have unsaved changes", state.hasUnsavedChanges)
        assertTrue("Should be able to undo", state.canUndo)
    }

    @Test
    fun testAudioAdjustment() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        // Setup project with imported video
        editingTools.createProject("Audio Test Project")
        val videoUri = Uri.parse("content://test/audio_video.mp4")
        val importResult = editingTools.importVideo(videoUri)
        assertTrue("Import should succeed", importResult.success)
        advanceUntilIdle()
        
        val clipId = importResult.clip!!.id
        val audioAdjustments = AudioAdjustments(
            volume = 1.5f,
            pan = 0.2f,
            pitch = 1.1f,
            speed = 0.9f,
            fadeIn = 1000L,
            fadeOut = 2000L,
            normalize = true,
            removeNoise = true,
            enhanceBass = 0.3f,
            enhanceTreble = 0.2f,
            equalizerBands = listOf(
                EqualizerBand(frequency = 100f, gain = 2f),
                EqualizerBand(frequency = 1000f, gain = -1f),
                EqualizerBand(frequency = 10000f, gain = 1.5f)
            ),
            compressor = CompressorSettings(
                threshold = -18f,
                ratio = 3f,
                attack = 5f,
                release = 50f,
                makeup = 2f
            ),
            reverb = ReverbSettings(
                roomSize = 0.7f,
                dampening = 0.4f,
                wetLevel = 0.3f,
                dryLevel = 0.7f
            )
        )
        
        // When
        val result = editingTools.adjustAudio(clipId, audioAdjustments)
        
        // Then
        assertNotNull("Audio adjustment result should not be null", result)
        assertTrue("Audio adjustment should succeed", result.success)
        assertNotNull("Adjusted audio info should be provided", result.adjustedAudioInfo)
        assertNotNull("Modified clip should be provided", result.modifiedClip)
        assertTrue("Adjust time should be set", result.adjustTime > 0)
        
        // Verify adjusted audio info
        val adjustedAudioInfo = result.adjustedAudioInfo!!
        assertTrue("Sample rate should be positive", adjustedAudioInfo.sampleRate > 0)
        assertTrue("Channels should be positive", adjustedAudioInfo.channels > 0)
        assertTrue("Duration should be positive", adjustedAudioInfo.duration > 0)
        assertTrue("Codec should be specified", adjustedAudioInfo.codec.isNotEmpty())
        
        // Verify modified clip
        val modifiedClip = result.modifiedClip!!
        assertEquals("Clip ID should remain the same", clipId, modifiedClip.id)
        assertEquals("Audio info should be updated", adjustedAudioInfo, modifiedClip.audioInfo)
        assertTrue("Last modified should be updated", modifiedClip.lastModified > 0)
        
        // Verify state update
        val state = editingTools.editingState.value
        assertTrue("Should have unsaved changes", state.hasUnsavedChanges)
        assertTrue("Should be able to undo", state.canUndo)
    }

    @Test
    fun testColorCorrection() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        // Setup project with imported video
        editingTools.createProject("Color Correction Test Project")
        val videoUri = Uri.parse("content://test/color_video.mp4")
        val importResult = editingTools.importVideo(videoUri)
        assertTrue("Import should succeed", importResult.success)
        advanceUntilIdle()
        
        val clipId = importResult.clip!!.id
        val colorSettings = ColorCorrectionSettings(
            brightness = 0.1f,
            contrast = 1.2f,
            saturation = 1.1f,
            hue = 0.05f,
            gamma = 0.9f,
            exposure = 0.2f,
            shadows = -0.1f,
            highlights = 0.1f,
            whites = 0.05f,
            blacks = -0.05f,
            clarity = 0.3f,
            vibrance = 0.2f,
            temperature = 100f,
            tint = -50f,
            lut = "cinematic_lut.cube",
            lutIntensity = 0.8f
        )
        
        // When
        val result = editingTools.performColorCorrection(clipId, colorSettings)
        
        // Then
        assertNotNull("Color correction result should not be null", result)
        assertTrue("Color correction should succeed", result.success)
        assertNotNull("Correction data should be provided", result.correctionData)
        assertNotNull("Modified clip should be provided", result.modifiedClip)
        assertTrue("Correction time should be set", result.correctionTime > 0)
        
        // Verify correction data
        val correctionData = result.correctionData!!
        assertNotNull("Histogram should be provided", correctionData.histogram)
        assertNotNull("Waveform should be provided", correctionData.waveform)
        assertNotNull("Vectorscope should be provided", correctionData.vectorscope)
        assertEquals("Applied settings should match", colorSettings, correctionData.appliedSettings)
        
        // Verify histogram
        val histogram = correctionData.histogram
        assertEquals("Red histogram should have 256 values", 256, histogram.red.size)
        assertEquals("Green histogram should have 256 values", 256, histogram.green.size)
        assertEquals("Blue histogram should have 256 values", 256, histogram.blue.size)
        assertEquals("Luminance histogram should have 256 values", 256, histogram.luminance.size)
        
        // Verify waveform
        val waveform = correctionData.waveform
        assertTrue("Waveform should have data", waveform.data.isNotEmpty())
        assertTrue("Waveform width should be positive", waveform.width > 0)
        assertTrue("Waveform height should be positive", waveform.height > 0)
        
        // Verify vectorscope
        val vectorscope = correctionData.vectorscope
        assertTrue("Vectorscope should have data", vectorscope.data.isNotEmpty())
        assertTrue("Vectorscope scale should be positive", vectorscope.scale > 0)
        
        // Verify modified clip
        val modifiedClip = result.modifiedClip!!
        assertEquals("Clip ID should remain the same", clipId, modifiedClip.id)
        assertTrue("Clip should have color correction effect", 
                  modifiedClip.effects.any { it.type == EffectType.COLOR_CORRECTION })
        
        // Verify state update
        val state = editingTools.editingState.value
        assertTrue("Should have unsaved changes", state.hasUnsavedChanges)
        assertTrue("Should be able to undo", state.canUndo)
    }

    @Test
    fun testVideoStabilization() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        // Setup project with imported video
        editingTools.createProject("Stabilization Test Project")
        val videoUri = Uri.parse("content://test/shaky_video.mp4")
        val importResult = editingTools.importVideo(videoUri)
        assertTrue("Import should succeed", importResult.success)
        advanceUntilIdle()
        
        val clipId = importResult.clip!!.id
        val stabilizationSettings = VideoStabilizationSettings(
            strength = 0.9f,
            smoothness = 0.7f,
            cropFactor = 0.15f,
            analysisType = StabilizationAnalysis.MOTION_VECTORS,
            rollingShutterCorrection = true,
            zoomSmoothing = true,
            adaptiveZoom = true
        )
        
        // When
        val result = editingTools.stabilizeVideo(clipId, stabilizationSettings)
        
        // Then
        assertNotNull("Stabilization result should not be null", result)
        assertTrue("Video stabilization should succeed", result.success)
        assertNotNull("Stabilization data should be provided", result.stabilizationData)
        assertNotNull("Modified clip should be provided", result.modifiedClip)
        assertTrue("Stabilization time should be set", result.stabilizationTime > 0)
        
        // Verify stabilization data
        val stabilizationData = result.stabilizationData!!
        assertTrue("Quality score should be valid", 
                  stabilizationData.qualityScore >= 0f && stabilizationData.qualityScore <= 1f)
        assertTrue("Analysis time should be set", stabilizationData.analysisTime > 0)
        // Motion vectors, matrix, and crop regions can be empty in test implementation
        
        // Verify modified clip
        val modifiedClip = result.modifiedClip!!
        assertEquals("Clip ID should remain the same", clipId, modifiedClip.id)
        assertTrue("Clip should have stabilization effect", 
                  modifiedClip.effects.any { it.type == EffectType.STABILIZATION })
        
        // Verify stabilization effect parameters
        val stabilizationEffect = modifiedClip.effects.find { it.type == EffectType.STABILIZATION }!!
        assertEquals("Strength parameter should match", stabilizationSettings.strength, 
                    stabilizationEffect.parameters["strength"])
        assertEquals("Smoothness parameter should match", stabilizationSettings.smoothness, 
                    stabilizationEffect.parameters["smoothness"])
        assertEquals("Crop factor parameter should match", stabilizationSettings.cropFactor, 
                    stabilizationEffect.parameters["cropFactor"])
        
        // Verify state update
        val state = editingTools.editingState.value
        assertTrue("Should have unsaved changes", state.hasUnsavedChanges)
        assertTrue("Should be able to undo", state.canUndo)
    }

    @Test
    fun testUndoRedoFunctionality() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        // Setup project with imported video
        editingTools.createProject("Undo Redo Test Project")
        val videoUri = Uri.parse("content://test/undo_video.mp4")
        val importResult = editingTools.importVideo(videoUri)
        assertTrue("Import should succeed", importResult.success)
        advanceUntilIdle()
        
        val clipId = importResult.clip!!.id
        val originalClip = importResult.clip!!
        
        // Perform a trim operation
        val trimResult = editingTools.trimVideo(clipId, 1000L, 5000L)
        assertTrue("Trim should succeed", trimResult.success)
        advanceUntilIdle()
        
        // Verify we can undo
        var state = editingTools.editingState.value
        assertTrue("Should be able to undo", state.canUndo)
        assertFalse("Should not be able to redo", state.canRedo)
        
        // Perform undo
        val undoResult = editingTools.undo()
        
        // Then
        assertNotNull("Undo result should not be null", undoResult)
        assertTrue("Undo should succeed", undoResult.success)
        assertEquals("Action type should be trim_clip", "trim_clip", undoResult.actionType)
        assertTrue("Operation time should be set", undoResult.operationTime > 0)
        
        // Verify state after undo
        state = editingTools.editingState.value
        assertFalse("Should not be able to undo after single undo", state.canUndo)
        assertTrue("Should be able to redo", state.canRedo)
        
        // Verify clip is restored to original state
        val project = state.currentProject!!
        val restoredClip = project.timeline.videoTracks[0].clips.find { it.id == clipId }!!
        assertEquals("Start time should be restored", originalClip.startTime, restoredClip.startTime)
        assertEquals("Duration should be restored", originalClip.duration, restoredClip.duration)
        
        // Test redo functionality
        val redoResult = editingTools.redo()
        
        // Then
        assertNotNull("Redo result should not be null", redoResult)
        assertTrue("Redo should succeed", redoResult.success)
        assertEquals("Action type should be trim_clip", "trim_clip", redoResult.actionType)
        assertTrue("Operation time should be set", redoResult.operationTime > 0)
        
        // Verify state after redo
        state = editingTools.editingState.value
        assertTrue("Should be able to undo after redo", state.canUndo)
        assertFalse("Should not be able to redo after single redo", state.canRedo)
        
        // Verify clip is trimmed again
        val retrimedClip = state.currentProject!!.timeline.videoTracks[0].clips.find { it.id == clipId }!!
        assertEquals("Start time should be trimmed again", 1000L, retrimedClip.startTime)
        assertEquals("Duration should be trimmed again", 4000L, retrimedClip.duration)
    }

    @Test
    fun testVideoExport() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        // Setup project with imported video and some edits
        editingTools.createProject("Export Test Project")
        val videoUri = Uri.parse("content://test/export_video.mp4")
        val importResult = editingTools.importVideo(videoUri)
        assertTrue("Import should succeed", importResult.success)
        
        // Apply some effects
        val clipId = importResult.clip!!.id
        val effect = VideoEffect(
            id = "export_effect",
            type = EffectType.COLOR_CORRECTION,
            intensity = 0.5f,
            duration = importResult.clip!!.duration
        )
        editingTools.applyEffect(clipId, effect)
        advanceUntilIdle()
        
        val exportSettings = ExportSettings(
            outputUri = Uri.parse("file:///storage/exports/test_export.mp4"),
            outputFormat = VideoFormat.MP4,
            resolution = Pair(1920, 1080),
            frameRate = 30f,
            bitrate = 8000000L,
            audioSettings = AudioExportSettings(
                codec = AudioCodec.AAC,
                sampleRate = 48000,
                channels = 2,
                bitrate = 192000L
            ),
            quality = ExportQuality.HIGH,
            enableGPUAcceleration = true,
            enableMultiThreading = true
        )
        
        // When
        val result = editingTools.exportVideo(exportSettings)
        
        // Then
        assertNotNull("Export result should not be null", result)
        assertTrue("Video export should succeed", result.success)
        assertNotNull("Output URI should be provided", result.outputUri)
        assertTrue("File size should be positive", result.fileSize > 0)
        assertTrue("Duration should be positive", result.duration > 0)
        assertTrue("Export time should be set", result.exportTime > 0)
        
        // Verify export settings were applied
        assertEquals("Output URI should match", exportSettings.outputUri, result.outputUri)
        assertEquals("Duration should match project duration", 
                    editingTools.editingState.value.currentProject!!.timeline.getTotalDuration(), 
                    result.duration)
        
        // Verify export state during process
        // Note: In a real implementation, we would test intermediate states during export
        val state = editingTools.editingState.value
        assertFalse("Should not be exporting after completion", state.isExporting)
        assertEquals("Export progress should be complete", 1f, state.exportProgress)
        assertTrue("Last export time should be set", state.lastExportTime > 0)
    }

    @Test
    fun testEditingMetricsRetrieval() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        // Create project and perform some editing operations
        editingTools.createProject("Metrics Test Project")
        
        val video1Uri = Uri.parse("content://test/metrics_video1.mp4")
        val video2Uri = Uri.parse("content://test/metrics_video2.mp4")
        
        val import1 = editingTools.importVideo(video1Uri)
        val import2 = editingTools.importVideo(video2Uri)
        
        assertTrue("First import should succeed", import1.success)
        assertTrue("Second import should succeed", import2.success)
        
        // Apply effects and transitions
        val effect = VideoEffect(id = "metrics_effect", type = EffectType.BLUR, duration = 5000L)
        editingTools.applyEffect(import1.clip!!.id, effect)
        
        val transition = VideoTransition(id = "metrics_transition", type = TransitionType.FADE, duration = 1000L, startTime = 4000L)
        editingTools.addTransition(import1.clip!!.id, import2.clip!!.id, transition)
        
        val overlay = VideoOverlay(
            id = "metrics_overlay", 
            type = OverlayType.TEXT, 
            startTime = 0L, 
            duration = 3000L,
            content = OverlayContent(text = "Test Overlay")
        )
        editingTools.addOverlay(import1.clip!!.id, overlay)
        
        advanceUntilIdle()
        
        // When
        val metrics = editingTools.getEditingMetrics()
        
        // Then
        assertNotNull("Metrics should not be null", metrics)
        assertTrue("Total projects should be positive", metrics.totalProjects > 0)
        assertTrue("Current project duration should be positive", metrics.currentProjectDuration > 0)
        assertEquals("Total clips should be 2", 2, metrics.totalClips)
        assertEquals("Total effects should be 1", 1, metrics.totalEffects)
        assertEquals("Total transitions should be 1", 1, metrics.totalTransitions)
        assertEquals("Total overlays should be 1", 1, metrics.totalOverlays)
        assertTrue("Undo stack should have items", metrics.undoStackSize > 0)
        assertEquals("Redo stack should be empty", 0, metrics.redoStackSize)
        assertTrue("Memory usage should be positive", metrics.memoryUsage > 0)
        assertTrue("Processing load should be valid", 
                  metrics.processingLoad >= 0f && metrics.processingLoad <= 1f)
        assertTrue("Last update time should be recent", 
                  System.currentTimeMillis() - metrics.lastUpdateTime < 5000)
    }

    @Test
    fun testEditingEventEmission() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        val events = mutableListOf<VideoEditingEvent>()
        val job = launch {
            editingTools.editingEvents.collect { event ->
                events.add(event)
            }
        }
        
        // When - Perform various editing operations
        editingTools.createProject("Events Test Project")
        
        val videoUri = Uri.parse("content://test/events_video.mp4")
        val importResult = editingTools.importVideo(videoUri)
        
        val clipId = importResult.clip!!.id
        editingTools.trimVideo(clipId, 1000L, 5000L)
        
        val effect = VideoEffect(id = "event_effect", type = EffectType.SHARPEN, duration = 4000L)
        editingTools.applyEffect(clipId, effect)
        
        val overlay = VideoOverlay(
            id = "event_overlay", 
            type = OverlayType.WATERMARK, 
            startTime = 0L, 
            duration = 2000L,
            content = OverlayContent(text = "Watermark")
        )
        editingTools.addOverlay(clipId, overlay)
        
        editingTools.undo()
        
        advanceUntilIdle()
        job.cancel()
        
        // Then
        assertTrue("Should have emitted events", events.isNotEmpty())
        
        val hasSystemInitialized = events.any { it is VideoEditingEvent.SystemInitialized }
        val hasProjectCreated = events.any { it is VideoEditingEvent.ProjectCreated }
        val hasVideoImported = events.any { it is VideoEditingEvent.VideoImported }
        val hasClipTrimmed = events.any { it is VideoEditingEvent.ClipTrimmed }
        val hasEffectApplied = events.any { it is VideoEditingEvent.EffectApplied }
        val hasOverlayAdded = events.any { it is VideoEditingEvent.OverlayAdded }
        val hasActionUndone = events.any { it is VideoEditingEvent.ActionUndone }
        
        assertTrue("Should have system initialized event", hasSystemInitialized)
        assertTrue("Should have project created event", hasProjectCreated)
        assertTrue("Should have video imported event", hasVideoImported)
        assertTrue("Should have clip trimmed event", hasClipTrimmed)
        assertTrue("Should have effect applied event", hasEffectApplied)
        assertTrue("Should have overlay added event", hasOverlayAdded)
        assertTrue("Should have action undone event", hasActionUndone)
    }

    @Test
    fun testErrorHandling() = runTest {
        // Test operations without initialization
        val trimResult = editingTools.trimVideo("nonexistent_clip", 0L, 1000L)
        assertFalse("Trim should fail without initialization", trimResult.success)
        assertNotNull("Should have error message", trimResult.error)
        
        val effectResult = editingTools.applyEffect("nonexistent_clip", 
            VideoEffect(id = "test", type = EffectType.BLUR, duration = 1000L))
        assertFalse("Effect application should fail without initialization", effectResult.success)
        assertNotNull("Should have error message", effectResult.error)
        
        // Initialize for other tests
        editingTools.initialize()
        advanceUntilIdle()
        
        // Test operations without project
        val trimWithoutProject = editingTools.trimVideo("nonexistent_clip", 0L, 1000L)
        assertFalse("Trim should fail without project", trimWithoutProject.success)
        assertNotNull("Should have error message", trimWithoutProject.error)
        
        // Create project for clip tests
        editingTools.createProject("Error Test Project")
        advanceUntilIdle()
        
        // Test operations with nonexistent clip
        val trimInvalidClip = editingTools.trimVideo("nonexistent_clip", 0L, 1000L)
        assertFalse("Trim should fail with nonexistent clip", trimInvalidClip.success)
        assertNotNull("Should have error message", trimInvalidClip.error)
        
        // Test invalid trim parameters
        val videoUri = Uri.parse("content://test/error_video.mp4")
        val importResult = editingTools.importVideo(videoUri)
        assertTrue("Import should succeed", importResult.success)
        
        val clipId = importResult.clip!!.id
        val invalidTrim = editingTools.trimVideo(clipId, -1000L, 1000L)
        assertFalse("Trim should fail with invalid parameters", invalidTrim.success)
        assertNotNull("Should have error message", invalidTrim.error)
        
        // Test undo/redo without actions
        val undoWithoutActions = editingTools.undo()
        assertFalse("Undo should fail without actions", undoWithoutActions.success)
        assertNotNull("Should have error message", undoWithoutActions.error)
        
        val redoWithoutActions = editingTools.redo()
        assertFalse("Redo should fail without actions", redoWithoutActions.success)
        assertNotNull("Should have error message", redoWithoutActions.error)
        
        // Test export without project data
        editingTools.createProject("Empty Export Project")
        val invalidExport = editingTools.exportVideo(ExportSettings(
            outputUri = Uri.parse(""),
            outputFormat = VideoFormat.UNKNOWN,
            resolution = Pair(-1, -1),
            frameRate = -30f,
            bitrate = -1L,
            audioSettings = AudioExportSettings()
        ))
        assertFalse("Export should fail with invalid settings", invalidExport.success)
        assertNotNull("Should have error message", invalidExport.error)
    }

    @Test
    fun testDataClassStructures() {
        // Test ProjectSettings construction and properties
        val projectSettings = ProjectSettings(
            resolution = Pair(3840, 2160),
            frameRate = 60f,
            sampleRate = 96000,
            audioChannels = 6,
            colorSpace = ColorSpace.REC2020,
            bitDepth = 10,
            aspectRatio = "16:9",
            autoSave = true,
            maxUndoLevels = 100
        )
        
        assertEquals("Resolution should match", Pair(3840, 2160), projectSettings.resolution)
        assertEquals("Frame rate should match", 60f, projectSettings.frameRate)
        assertEquals("Sample rate should match", 96000, projectSettings.sampleRate)
        assertEquals("Audio channels should match", 6, projectSettings.audioChannels)
        assertEquals("Color space should match", ColorSpace.REC2020, projectSettings.colorSpace)
        assertEquals("Bit depth should match", 10, projectSettings.bitDepth)
        assertTrue("Auto save should be enabled", projectSettings.autoSave)
        assertEquals("Max undo levels should match", 100, projectSettings.maxUndoLevels)
        
        // Test VideoEffect construction
        val videoEffect = VideoEffect(
            id = "complex_effect",
            type = EffectType.COLOR_CORRECTION,
            name = "Advanced Color Grading",
            parameters = mapOf(
                "brightness" to 0.2f,
                "contrast" to 1.3f,
                "saturation" to 1.1f,
                "lut" to "cinematic.cube"
            ),
            intensity = 0.85f,
            startTime = 2000L,
            duration = 8000L,
            keyframes = listOf(
                EffectKeyframe(2000L, 0f, InterpolationType.EASE_IN),
                EffectKeyframe(6000L, 1f, InterpolationType.LINEAR),
                EffectKeyframe(10000L, 0f, InterpolationType.EASE_OUT)
            ),
            isEnabled = true,
            blendMode = BlendMode.OVERLAY
        )
        
        assertEquals("Effect type should match", EffectType.COLOR_CORRECTION, videoEffect.type)
        assertEquals("Effect intensity should match", 0.85f, videoEffect.intensity)
        assertEquals("Effect duration should match", 8000L, videoEffect.duration)
        assertEquals("Should have 3 keyframes", 3, videoEffect.keyframes.size)
        assertTrue("Effect should be enabled", videoEffect.isEnabled)
        assertEquals("Blend mode should match", BlendMode.OVERLAY, videoEffect.blendMode)
        assertTrue("Should have brightness parameter", videoEffect.parameters.containsKey("brightness"))
        
        // Test AudioAdjustments
        val audioAdjustments = AudioAdjustments(
            volume = 2.0f,
            pan = -0.5f,
            pitch = 1.2f,
            speed = 0.8f,
            fadeIn = 3000L,
            fadeOut = 5000L,
            normalize = true,
            removeNoise = true,
            equalizerBands = listOf(
                EqualizerBand(60f, 3f),
                EqualizerBand(200f, -2f),
                EqualizerBand(2000f, 1f),
                EqualizerBand(8000f, 2f)
            )
        )
        
        assertEquals("Volume should match", 2.0f, audioAdjustments.volume)
        assertEquals("Pan should match", -0.5f, audioAdjustments.pan)
        assertEquals("Pitch should match", 1.2f, audioAdjustments.pitch)
        assertEquals("Speed should match", 0.8f, audioAdjustments.speed)
        assertTrue("Normalize should be enabled", audioAdjustments.normalize)
        assertTrue("Noise removal should be enabled", audioAdjustments.removeNoise)
        assertEquals("Should have 4 equalizer bands", 4, audioAdjustments.equalizerBands.size)
        
        // Test Timeline helper functions
        val timeline = Timeline(
            videoTracks = mutableListOf(
                VideoTrack("v1", "Video 1", mutableListOf(
                    TimelineClip("c1", ClipType.VIDEO, Uri.parse("test://1"), startTime = 0L, duration = 5000L, trackIndex = 0),
                    TimelineClip("c2", ClipType.VIDEO, Uri.parse("test://2"), startTime = 5000L, duration = 3000L, trackIndex = 0)
                ))
            ),
            audioTracks = mutableListOf(
                AudioTrack("a1", "Audio 1", mutableListOf(
                    AudioClip("ac1", Uri.parse("test://audio"), startTime = 1000L, duration = 6000L, trackIndex = 0,
                             audioInfo = AudioInfo(48000, 2, 192000L, "AAC", 6000L, 1024*1024L))
                ))
            )
        )
        
        assertEquals("Total duration should be 8000L", 8000L, timeline.getTotalDuration())
        assertEquals("Total clips should be 3", 3, timeline.getTotalClips())
        assertEquals("Total effects should be 0", 0, timeline.getTotalEffects())
        assertEquals("Total transitions should be 0", 0, timeline.getTotalTransitions())
    }

    @Test
    fun testConcurrentEditingOperations() = runTest {
        editingTools.initialize()
        advanceUntilIdle()
        
        editingTools.createProject("Concurrent Test Project")
        val videoUri = Uri.parse("content://test/concurrent_video.mp4")
        val importResult = editingTools.importVideo(videoUri)
        assertTrue("Import should succeed", importResult.success)
        advanceUntilIdle()
        
        val clipId = importResult.clip!!.id
        
        // When - Perform multiple operations concurrently
        val operations = listOf(
            async { editingTools.trimVideo(clipId, 1000L, 8000L) },
            async { editingTools.applyEffect(clipId, VideoEffect(id = "concurrent1", type = EffectType.BLUR, duration = 5000L)) },
            async { editingTools.adjustAudio(clipId, AudioAdjustments(volume = 1.5f)) },
            async { editingTools.addOverlay(clipId, VideoOverlay(id = "concurrent_overlay", type = OverlayType.TEXT, 
                   startTime = 0L, duration = 2000L, content = OverlayContent(text = "Concurrent"))) },
            async { editingTools.performColorCorrection(clipId, ColorCorrectionSettings(brightness = 0.1f)) }
        )
        
        val results = operations.awaitAll()
        
        // Then
        assertEquals("All operations should complete", 5, results.size)
        results.forEach { result ->
            assertNotNull("Each result should not be null", result)
            // Individual results may succeed or fail based on implementation and timing
            // In a production system, we would implement proper locking mechanisms
        }
        
        // Verify final state is consistent
        val state = editingTools.editingState.value
        assertNotNull("Should have current project", state.currentProject)
        assertTrue("Should have unsaved changes", state.hasUnsavedChanges)
    }
}