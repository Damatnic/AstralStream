package com.astralplayer.nextplayer.vr

import android.content.Context
import android.hardware.SensorEvent
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
 * Comprehensive tests for advanced VR/AR video support
 * Tests VR/AR modes, spatial video, head tracking, interactions, and immersive effects
 */
@RunWith(AndroidJUnit4::class)
class AdvancedVRARVideoSupportTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var vrArSupport: AdvancedVRARVideoSupport
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        vrArSupport = AdvancedVRARVideoSupport(context)
    }

    @After
    fun tearDown() {
        vrArSupport.cleanup()
    }

    @Test
    fun testVRARSystemInitialization() = runTest {
        // When
        val result = vrArSupport.initialize()
        advanceUntilIdle()
        
        // Then
        assertNotNull("Initialization result should not be null", result)
        assertTrue("VR/AR system should initialize successfully", result.success)
        assertNotNull("Capabilities should be provided", result.capabilities)
        assertTrue("Initialization time should be set", result.initializationTime > 0)
        
        // Verify capabilities
        val capabilities = result.capabilities!!
        assertTrue("Should have supported modes", capabilities.supportedModes.isNotEmpty())
        assertTrue("Should have supported projections", capabilities.supportedProjections.isNotEmpty())
        assertTrue("Should support VR mode", capabilities.supportedModes.contains(VRARMode.VR))
        assertTrue("Max resolution should be reasonable", 
                  capabilities.maxResolution.first > 0 && capabilities.maxResolution.second > 0)
        assertTrue("Max frame rate should be positive", capabilities.maxFrameRate > 0f)
        
        // Verify state
        val state = vrArSupport.vrState.value
        assertTrue("System should be initialized", state.isInitialized)
        assertTrue("Should have supported modes", state.supportedModes.isNotEmpty())
        assertTrue("Should have supported projections", state.supportedProjections.isNotEmpty())
        assertEquals("Current mode should be NONE initially", VRARMode.NONE, state.currentMode)
    }

    @Test
    fun testVRModeEnable() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        val vrConfig = VRModeConfig(
            headTrackingConfig = HeadTrackingConfig(
                enabled = true,
                predictionTime = 18f,
                smoothingFactor = 0.8f,
                trackingMode = TrackingMode.ORIENTATION_ONLY
            ),
            stereoConfig = StereoRenderingConfig(
                enabled = true,
                renderingMode = StereoRenderingMode.SIDE_BY_SIDE,
                eyeSeparation = 65f,
                antiAliasingLevel = 4
            ),
            interactionConfig = VRInteractionConfig(
                gestureConfig = VRGestureConfig(
                    enabled = true,
                    supportedGestures = listOf(VRGesture.TAP, VRGesture.SWIPE)
                )
            ),
            renderingQuality = RenderingQuality.HIGH,
            fieldOfView = 90f,
            enableComfortSettings = true
        )
        
        // When
        val result = vrArSupport.enableVRMode(vrConfig)
        
        // Then
        assertNotNull("VR mode result should not be null", result)
        assertTrue("VR mode should be enabled successfully", result.success)
        assertTrue("Should have enabled features", result.enabledFeatures.isNotEmpty())
        assertNotNull("Rendering info should be provided", result.renderingInfo)
        assertTrue("Enable time should be set", result.enableTime > 0)
        
        // Verify enabled features
        assertTrue("Should have head tracking", result.enabledFeatures.contains("Head Tracking"))
        assertTrue("Should have stereoscopic rendering", result.enabledFeatures.contains("Stereoscopic Rendering"))
        assertTrue("Should have gesture control", result.enabledFeatures.contains("Gesture Control"))
        
        // Verify rendering info
        val renderingInfo = result.renderingInfo!!
        assertTrue("Resolution should be reasonable", 
                  renderingInfo.resolution.first > 0 && renderingInfo.resolution.second > 0)
        assertTrue("Refresh rate should be positive", renderingInfo.refreshRate > 0f)
        assertNotNull("Rendering API should be specified", renderingInfo.renderingAPI)
        
        // Verify state update
        val state = vrArSupport.vrState.value
        assertEquals("Current mode should be VR", VRARMode.VR, state.currentMode)
        assertTrue("VR mode should be enabled", state.vrModeEnabled)
        assertEquals("VR config should be stored", vrConfig, state.vrConfig)
        assertTrue("Head tracking should be enabled", state.headTrackingEnabled)
        assertTrue("Stereo rendering should be enabled", state.stereoRenderingEnabled)
    }

    @Test
    fun testARModeEnable() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        val arConfig = ARModeConfig(
            cameraConfig = CameraConfig(
                enabled = true,
                resolution = Pair(1920, 1080),
                frameRate = 30f,
                autoFocus = true,
                stabilization = true
            ),
            trackingConfig = ARTrackingConfig(
                environmentTracking = true,
                planeTracking = true,
                imageTracking = false,
                trackingAccuracy = TrackingAccuracy.HIGH
            ),
            interactionConfig = ARInteractionConfig(
                touchEnabled = true,
                gestureEnabled = true,
                raycastingEnabled = true
            ),
            renderingConfig = ARRenderingConfig(
                occlusionMasking = true,
                shadowCasting = true,
                lightEstimation = true
            ),
            lightEstimation = true,
            planeDetection = true
        )
        
        // When
        val result = vrArSupport.enableARMode(arConfig)
        
        // Then
        assertNotNull("AR mode result should not be null", result)
        assertTrue("AR mode should be enabled successfully", result.success)
        assertTrue("Should have enabled features", result.enabledFeatures.isNotEmpty())
        assertNotNull("Tracking info should be provided", result.trackingInfo)
        assertTrue("Enable time should be set", result.enableTime > 0)
        
        // Verify enabled features
        assertTrue("Should have camera feed", result.enabledFeatures.contains("Camera Feed"))
        assertTrue("Should have environment tracking", result.enabledFeatures.contains("Environment Tracking"))
        assertTrue("Should have touch interaction", result.enabledFeatures.contains("Touch Interaction"))
        
        // Verify tracking info
        val trackingInfo = result.trackingInfo!!
        assertNotNull("Tracking state should be provided", trackingInfo.trackingState)
        assertTrue("Tracked features should be non-negative", trackingInfo.trackedFeatures >= 0)
        assertTrue("Tracking accuracy should be valid", 
                  trackingInfo.trackingAccuracy >= 0f && trackingInfo.trackingAccuracy <= 1f)
        assertTrue("Environment lighting should be valid", 
                  trackingInfo.environmentLighting >= 0f && trackingInfo.environmentLighting <= 1f)
        
        // Verify state update
        val state = vrArSupport.vrState.value
        assertEquals("Current mode should be AR", VRARMode.AR, state.currentMode)
        assertTrue("AR mode should be enabled", state.arModeEnabled)
        assertEquals("AR config should be stored", arConfig, state.arConfig)
        assertTrue("Camera feed should be enabled", state.cameraFeedEnabled)
        assertTrue("Environment tracking should be enabled", state.environmentTrackingEnabled)
    }

    @Test
    fun testSpatialVideoLoading() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        val videoUri = Uri.parse("content://test/360_video.mp4")
        val spatialFormat = SpatialVideoFormat.EQUIRECTANGULAR_360
        
        // When
        val result = vrArSupport.loadSpatialVideo(videoUri, spatialFormat)
        
        // Then
        assertNotNull("Spatial video result should not be null", result)
        assertTrue("Spatial video should load successfully", result.success)
        assertNotNull("Video info should be provided", result.videoInfo)
        assertTrue("Should have texture IDs", result.textureIds.isNotEmpty())
        assertNotNull("Projection mapping should be provided", result.projectionMapping)
        assertTrue("Load time should be set", result.loadTime > 0)
        
        // Verify video info
        val videoInfo = result.videoInfo!!
        assertEquals("Video URI should match", videoUri, videoInfo.uri)
        assertEquals("Spatial format should match", spatialFormat, videoInfo.format)
        assertTrue("Resolution should be valid", 
                  videoInfo.resolution.first > 0 && videoInfo.resolution.second > 0)
        assertTrue("Frame rate should be positive", videoInfo.frameRate > 0f)
        assertTrue("Duration should be positive", videoInfo.duration > 0)
        assertTrue("Bitrate should be positive", videoInfo.bitrate > 0)
        
        // Verify projection mapping
        val projectionMapping = result.projectionMapping!!
        assertEquals("Projection format should match", spatialFormat, projectionMapping.format)
        assertTrue("Field of view should be reasonable", projectionMapping.fieldOfView > 0f)
        assertNotNull("UV mapping should be provided", projectionMapping.uvMapping)
        assertNotNull("Distortion correction should be provided", projectionMapping.distortionCorrection)
        
        // Verify state update
        val state = vrArSupport.vrState.value
        assertEquals("Current video URI should be set", videoUri, state.currentVideoUri)
        assertEquals("Spatial format should be set", spatialFormat, state.spatialFormat)
        assertTrue("Spatial video should be marked as loaded", state.spatialVideoLoaded)
    }

    @Test
    fun testProjectionConfiguration() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        val projectionTypes = listOf(
            ProjectionType.EQUIRECTANGULAR,
            ProjectionType.CUBEMAP,
            ProjectionType.FISHEYE,
            ProjectionType.STEREOSCOPIC
        )
        
        projectionTypes.forEach { projectionType ->
            // When
            val result = vrArSupport.configureProjection(projectionType)
            
            // Then
            assertTrue("Projection configuration should succeed for $projectionType", result.success)
            assertEquals("Projection type should match", projectionType, result.projectionType)
            assertTrue("Field of view should be positive", result.fieldOfView > 0f)
            assertTrue("Aspect ratio should be positive", result.aspectRatio > 0f)
            assertTrue("Configure time should be set", result.configureTime > 0)
            
            // Verify state update
            val state = vrArSupport.vrState.value
            assertEquals("Current projection should be updated", projectionType, state.currentProjection)
            assertNotNull("Projection config should be stored", state.projectionConfig)
        }
    }

    @Test
    fun testGazeInteractionEnable() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        val interactionZones = listOf(
            InteractionZone(
                id = "play_pause",
                bounds = BoundingBox(
                    center = Vector3(0f, 0f, -2f),
                    size = Vector3(0.5f, 0.2f, 0.1f)
                ),
                action = InteractionAction.PLAY_PAUSE,
                dwellTime = 1000L
            ),
            InteractionZone(
                id = "volume_control",
                bounds = BoundingBox(
                    center = Vector3(1f, 0f, -2f),
                    size = Vector3(0.3f, 0.8f, 0.1f)
                ),
                action = InteractionAction.VOLUME_UP,
                dwellTime = 500L
            )
        )
        
        val gazeConfig = GazeInteractionConfig(
            enabled = true,
            dwellTime = 1000L,
            cursorVisible = true,
            cursorSize = 0.02f,
            interactionZones = interactionZones,
            smoothingFactor = 0.7f,
            deadZone = 1.0f
        )
        
        // When
        val result = vrArSupport.enableGazeInteraction(gazeConfig)
        
        // Then
        assertNotNull("Gaze interaction result should not be null", result)
        assertTrue("Gaze interaction should be enabled successfully", result.success)
        assertTrue("Tracking accuracy should be reasonable", result.trackingAccuracy >= 0.5f)
        assertEquals("Interaction zones count should match", interactionZones.size, result.interactionZones)
        assertTrue("Enable time should be set", result.enableTime > 0)
        
        // Verify state update
        val state = vrArSupport.vrState.value
        assertTrue("Gaze interaction should be enabled", state.gazeInteractionEnabled)
        assertEquals("Gaze config should be stored", gazeConfig, state.gazeConfig)
    }

    @Test
    fun testHandTrackingEnable() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        val gestureConfig = GestureRecognitionConfig(
            enabled = true,
            supportedGestures = listOf(
                HandGesture.POINT,
                HandGesture.GRAB,
                HandGesture.PINCH,
                HandGesture.THUMBS_UP
            ),
            recognitionTimeout = 500L,
            confidenceThreshold = 0.8f
        )
        
        val handTrackingConfig = HandTrackingConfig(
            enabled = true,
            trackingMode = HandTrackingMode.SKELETON,
            gestureConfig = gestureConfig,
            handednessDetection = true,
            fingerTracking = true,
            confidenceThreshold = 0.7f
        )
        
        // When
        val result = vrArSupport.enableHandTracking(handTrackingConfig)
        
        // Then
        assertNotNull("Hand tracking result should not be null", result)
        assertTrue("Hand tracking should be enabled successfully", result.success)
        assertTrue("Tracking accuracy should be reasonable", result.trackingAccuracy >= 0.5f)
        assertEquals("Supported gestures should match", 
                    gestureConfig.supportedGestures, result.supportedGestures)
        assertTrue("Enable time should be set", result.enableTime > 0)
        
        // Verify state update
        val state = vrArSupport.vrState.value
        assertTrue("Hand tracking should be enabled", state.handTrackingEnabled)
        assertEquals("Hand tracking config should be stored", handTrackingConfig, state.handTrackingConfig)
    }

    @Test
    fun testImmersiveEffectsApplication() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        val immersiveConfig = ImmersiveEffectsConfig(
            spatialAudioEnabled = true,
            spatialAudioConfig = SpatialAudioConfig(
                enabled = true,
                roomSize = RoomSize.MEDIUM,
                reverbLevel = 0.3f,
                doppler = true,
                occlusionEnabled = true
            ),
            environmentalEffectsEnabled = true,
            environmentalEffectsConfig = EnvironmentalEffectsConfig(
                ambientSounds = true,
                weatherEffects = false,
                environmentType = EnvironmentType.FOREST
            ),
            hapticFeedbackEnabled = true,
            hapticConfig = HapticFeedbackConfig(
                enabled = true,
                intensity = 0.5f,
                audioDriven = true
            ),
            visualEffectsEnabled = true,
            visualEffectsConfig = VisualEffectsConfig(
                particleEffects = false,
                lightingEffects = true,
                postProcessing = true,
                bloom = false
            )
        )
        
        // When
        val result = vrArSupport.applyImmersiveEffects(immersiveConfig)
        
        // Then
        assertNotNull("Immersive effects result should not be null", result)
        assertTrue("Immersive effects should be applied successfully", result.success)
        assertTrue("Should have applied effects", result.appliedEffects.isNotEmpty())
        assertTrue("Performance impact should be reasonable", 
                  result.performanceImpact >= 0f && result.performanceImpact <= 1f)
        assertTrue("Apply time should be set", result.applyTime > 0)
        
        // Verify applied effects
        assertTrue("Should have spatial audio", result.appliedEffects.contains("Spatial Audio"))
        assertTrue("Should have environmental effects", result.appliedEffects.contains("Environmental Effects"))
        assertTrue("Should have haptic feedback", result.appliedEffects.contains("Haptic Feedback"))
        assertTrue("Should have visual effects", result.appliedEffects.contains("Visual Effects"))
        
        // Verify state update
        val state = vrArSupport.vrState.value
        assertTrue("Immersive effects should be enabled", state.immersiveEffectsEnabled)
        assertEquals("Immersive effects config should be stored", immersiveConfig, state.immersiveEffectsConfig)
    }

    @Test
    fun testHeadOrientationUpdate() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        val events = mutableListOf<VRAREvent>()
        val job = launch {
            vrArSupport.vrEvents.collect { event ->
                events.add(event)
            }
        }
        
        // Create test rotation matrix (45-degree rotation around Y-axis)
        val rotationMatrix = FloatArray(16)
        android.opengl.Matrix.setIdentityM(rotationMatrix, 0)
        android.opengl.Matrix.rotateM(rotationMatrix, 0, 45f, 0f, 1f, 0f)
        
        // When
        vrArSupport.updateHeadOrientation(rotationMatrix)
        advanceUntilIdle()
        
        job.cancel()
        
        // Then
        assertTrue("Should have emitted orientation update event", 
                  events.any { it is VRAREvent.HeadOrientationUpdated })
        
        val orientationEvent = events.filterIsInstance<VRAREvent.HeadOrientationUpdated>().first()
        assertTrue("Yaw should be reasonable", 
                  orientationEvent.yaw >= -kotlin.math.PI && orientationEvent.yaw <= kotlin.math.PI)
        assertTrue("Pitch should be reasonable", 
                  orientationEvent.pitch >= -kotlin.math.PI/2 && orientationEvent.pitch <= kotlin.math.PI/2)
        assertTrue("Roll should be reasonable", 
                  orientationEvent.roll >= -kotlin.math.PI && orientationEvent.roll <= kotlin.math.PI)
    }

    @Test
    fun testVRARMetricsRetrieval() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        // Enable VR mode to populate metrics
        val vrConfig = VRModeConfig(renderingQuality = RenderingQuality.HIGH)
        vrArSupport.enableVRMode(vrConfig)
        advanceUntilIdle()
        
        // When
        val metrics = vrArSupport.getVRARMetrics()
        
        // Then
        assertNotNull("Metrics should not be null", metrics)
        assertTrue("Frame rate should be positive", metrics.frameRate > 0f)
        assertTrue("Rendering latency should be reasonable", metrics.renderingLatency >= 0 && metrics.renderingLatency <= 100)
        assertTrue("Tracking accuracy should be valid", 
                  metrics.trackingAccuracy >= 0f && metrics.trackingAccuracy <= 1f)
        assertTrue("Head tracking latency should be reasonable", 
                  metrics.headTrackingLatency >= 0 && metrics.headTrackingLatency <= 50)
        assertTrue("Battery usage should be valid", 
                  metrics.batteryUsage >= 0f && metrics.batteryUsage <= 1f)
        assertNotNull("Thermal state should be provided", metrics.thermalState)
        assertTrue("Memory usage should be positive", metrics.memoryUsage > 0)
        assertTrue("CPU usage should be valid", 
                  metrics.cpuUsage >= 0f && metrics.cpuUsage <= 1f)
        assertTrue("GPU usage should be valid", 
                  metrics.gpuUsage >= 0f && metrics.gpuUsage <= 1f)
        assertTrue("Last update time should be recent", 
                  System.currentTimeMillis() - metrics.lastUpdateTime < 5000)
    }

    @Test
    fun testSpatialVideoFormats() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        val videoUri = Uri.parse("content://test/spatial_video.mp4")
        val spatialFormats = listOf(
            SpatialVideoFormat.EQUIRECTANGULAR_360,
            SpatialVideoFormat.EQUIRECTANGULAR_180,
            SpatialVideoFormat.CUBEMAP,
            SpatialVideoFormat.FISHEYE,
            SpatialVideoFormat.STEREOSCOPIC_SBS,
            SpatialVideoFormat.STEREOSCOPIC_TB
        )
        
        spatialFormats.forEach { format ->
            // When
            val result = vrArSupport.loadSpatialVideo(videoUri, format)
            
            // Then
            assertTrue("Spatial video should load successfully for $format", result.success)
            assertNotNull("Video info should be provided for $format", result.videoInfo)
            assertEquals("Format should match for $format", format, result.videoInfo!!.format)
            assertNotNull("Projection mapping should be provided for $format", result.projectionMapping)
            
            // Verify projection mapping is appropriate for format
            val projectionMapping = result.projectionMapping!!
            assertEquals("Projection format should match spatial format", format, projectionMapping.format)
            
            when (format) {
                SpatialVideoFormat.EQUIRECTANGULAR_360 -> {
                    assertEquals("360° video should have 360° field of view", 360f, projectionMapping.fieldOfView)
                }
                SpatialVideoFormat.EQUIRECTANGULAR_180 -> {
                    assertEquals("180° video should have 180° field of view", 180f, projectionMapping.fieldOfView)
                }
                SpatialVideoFormat.FISHEYE -> {
                    assertTrue("Fisheye should have wide field of view", projectionMapping.fieldOfView > 180f)
                }
                else -> {
                    assertTrue("Field of view should be positive", projectionMapping.fieldOfView > 0f)
                }
            }
        }
    }

    @Test
    fun testVRAREventEmission() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        val events = mutableListOf<VRAREvent>()
        val job = launch {
            vrArSupport.vrEvents.collect { event ->
                events.add(event)
            }
        }
        
        // When - Perform various VR/AR operations
        val vrConfig = VRModeConfig()
        vrArSupport.enableVRMode(vrConfig)
        
        val videoUri = Uri.parse("content://test/vr_video.mp4")
        vrArSupport.loadSpatialVideo(videoUri, SpatialVideoFormat.EQUIRECTANGULAR_360)
        
        vrArSupport.configureProjection(ProjectionType.EQUIRECTANGULAR)
        
        val gazeConfig = GazeInteractionConfig(enabled = true)
        vrArSupport.enableGazeInteraction(gazeConfig)
        
        advanceUntilIdle()
        job.cancel()
        
        // Then
        assertTrue("Should have emitted events", events.isNotEmpty())
        
        val hasSystemInitialized = events.any { it is VRAREvent.SystemInitialized }
        val hasVRModeEnabled = events.any { it is VRAREvent.VRModeEnabled }
        val hasSpatialVideoLoaded = events.any { it is VRAREvent.SpatialVideoLoaded }
        val hasProjectionChanged = events.any { it is VRAREvent.ProjectionChanged }
        val hasGazeInteractionEnabled = events.any { it is VRAREvent.GazeInteractionEnabled }
        
        assertTrue("Should have system initialized event", hasSystemInitialized)
        assertTrue("Should have VR mode enabled event", hasVRModeEnabled)
        assertTrue("Should have spatial video loaded event", hasSpatialVideoLoaded)
        assertTrue("Should have projection changed event", hasProjectionChanged)
        assertTrue("Should have gaze interaction enabled event", hasGazeInteractionEnabled)
    }

    @Test
    fun testVRARStateTracking() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        // Initial state
        var state = vrArSupport.vrState.value
        assertTrue("Should be initialized", state.isInitialized)
        assertEquals("Initial mode should be NONE", VRARMode.NONE, state.currentMode)
        assertFalse("VR mode should be disabled initially", state.vrModeEnabled)
        assertFalse("AR mode should be disabled initially", state.arModeEnabled)
        
        // Enable VR mode
        val vrConfig = VRModeConfig(
            headTrackingConfig = HeadTrackingConfig(enabled = true),
            stereoConfig = StereoRenderingConfig(enabled = true)
        )
        vrArSupport.enableVRMode(vrConfig)
        advanceUntilIdle()
        
        state = vrArSupport.vrState.value
        assertEquals("Mode should be VR", VRARMode.VR, state.currentMode)
        assertTrue("VR mode should be enabled", state.vrModeEnabled)
        assertEquals("VR config should be stored", vrConfig, state.vrConfig)
        assertTrue("Head tracking should be enabled", state.headTrackingEnabled)
        assertTrue("Stereo rendering should be enabled", state.stereoRenderingEnabled)
        
        // Load spatial video
        val videoUri = Uri.parse("content://test/360_video.mp4")
        val spatialFormat = SpatialVideoFormat.EQUIRECTANGULAR_360
        vrArSupport.loadSpatialVideo(videoUri, spatialFormat)
        advanceUntilIdle()
        
        state = vrArSupport.vrState.value
        assertEquals("Current video URI should be set", videoUri, state.currentVideoUri)
        assertEquals("Spatial format should be set", spatialFormat, state.spatialFormat)
        assertTrue("Spatial video should be loaded", state.spatialVideoLoaded)
        
        // Configure projection
        vrArSupport.configureProjection(ProjectionType.EQUIRECTANGULAR)
        advanceUntilIdle()
        
        state = vrArSupport.vrState.value
        assertEquals("Current projection should be set", ProjectionType.EQUIRECTANGULAR, state.currentProjection)
        assertNotNull("Projection config should be stored", state.projectionConfig)
    }

    @Test
    fun testErrorHandling() = runTest {
        // Test operations without initialization
        val vrConfig = VRModeConfig()
        val vrResult = vrArSupport.enableVRMode(vrConfig)
        assertFalse("VR mode should fail without initialization", vrResult.success)
        assertNotNull("Should have error message", vrResult.error)
        
        val arConfig = ARModeConfig()
        val arResult = vrArSupport.enableARMode(arConfig)
        assertFalse("AR mode should fail without initialization", arResult.success)
        assertNotNull("Should have error message", arResult.error)
        
        // Initialize for other tests
        vrArSupport.initialize()
        advanceUntilIdle()
        
        // Test invalid spatial video loading
        val invalidUri = Uri.parse("invalid://uri")
        val spatialResult = vrArSupport.loadSpatialVideo(invalidUri, SpatialVideoFormat.EQUIRECTANGULAR_360)
        // Result may succeed or fail based on implementation, but should not crash
        assertNotNull("Spatial video result should not be null", spatialResult)
        
        // Test invalid projection configuration
        // All projection types should be valid, so we can't easily test invalid ones
        
        // Test metrics with no active mode
        val metrics = vrArSupport.getVRARMetrics()
        assertNotNull("Metrics should always be available", metrics)
        assertTrue("Frame rate should be reasonable even without active mode", metrics.frameRate >= 0f)
    }

    @Test
    fun testSensorAccuracyHandling() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        val events = mutableListOf<VRAREvent>()
        val job = launch {
            vrArSupport.vrEvents.collect { event ->
                events.add(event)
            }
        }
        
        // Simulate sensor accuracy change
        vrArSupport.onAccuracyChanged(null, android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
        advanceUntilIdle()
        
        job.cancel()
        
        // Then
        val hasAccuracyChanged = events.any { it is VRAREvent.SensorAccuracyChanged }
        assertTrue("Should have emitted sensor accuracy changed event", hasAccuracyChanged)
        
        if (hasAccuracyChanged) {
            val accuracyEvent = events.filterIsInstance<VRAREvent.SensorAccuracyChanged>().first()
            assertEquals("Accuracy should match", android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH, accuracyEvent.accuracy)
        }
    }

    @Test
    fun testDataClassStructures() {
        // Test VRModeConfig construction and properties
        val vrConfig = VRModeConfig(
            headTrackingConfig = HeadTrackingConfig(
                enabled = true,
                predictionTime = 20f,
                trackingMode = TrackingMode.SIX_DOF
            ),
            stereoConfig = StereoRenderingConfig(
                enabled = true,
                renderingMode = StereoRenderingMode.SIDE_BY_SIDE,
                eyeSeparation = 65f
            ),
            renderingQuality = RenderingQuality.ULTRA,
            fieldOfView = 110f,
            ipd = 65f
        )
        
        assertTrue("Head tracking should be enabled", vrConfig.headTrackingConfig.enabled)
        assertEquals("Prediction time should match", 20f, vrConfig.headTrackingConfig.predictionTime)
        assertEquals("Tracking mode should match", TrackingMode.SIX_DOF, vrConfig.headTrackingConfig.trackingMode)
        assertEquals("Rendering quality should match", RenderingQuality.ULTRA, vrConfig.renderingQuality)
        assertEquals("Field of view should match", 110f, vrConfig.fieldOfView)
        
        // Test ARModeConfig construction
        val arConfig = ARModeConfig(
            cameraConfig = CameraConfig(
                enabled = true,
                resolution = Pair(3840, 2160),
                frameRate = 60f
            ),
            trackingConfig = ARTrackingConfig(
                environmentTracking = true,
                planeTracking = true,
                trackingAccuracy = TrackingAccuracy.MAXIMUM
            ),
            lightEstimation = true
        )
        
        assertTrue("Camera should be enabled", arConfig.cameraConfig.enabled)
        assertEquals("Camera resolution should match", Pair(3840, 2160), arConfig.cameraConfig.resolution)
        assertEquals("Camera frame rate should match", 60f, arConfig.cameraConfig.frameRate)
        assertTrue("Environment tracking should be enabled", arConfig.trackingConfig.environmentTracking)
        assertTrue("Light estimation should be enabled", arConfig.lightEstimation)
        
        // Test immersive effects config
        val effectsConfig = ImmersiveEffectsConfig(
            spatialAudioEnabled = true,
            spatialAudioConfig = SpatialAudioConfig(
                roomSize = RoomSize.LARGE,
                reverbLevel = 0.5f,
                doppler = true
            ),
            hapticFeedbackEnabled = true,
            hapticConfig = HapticFeedbackConfig(
                intensity = 0.8f,
                audioDriven = true
            )
        )
        
        assertTrue("Spatial audio should be enabled", effectsConfig.spatialAudioEnabled)
        assertEquals("Room size should match", RoomSize.LARGE, effectsConfig.spatialAudioConfig.roomSize)
        assertEquals("Reverb level should match", 0.5f, effectsConfig.spatialAudioConfig.reverbLevel)
        assertTrue("Haptic feedback should be enabled", effectsConfig.hapticFeedbackEnabled)
        assertEquals("Haptic intensity should match", 0.8f, effectsConfig.hapticConfig.intensity)
    }

    @Test
    fun testConcurrentVRAROperations() = runTest {
        vrArSupport.initialize()
        advanceUntilIdle()
        
        // When - Perform multiple operations concurrently
        val operations = listOf(
            async { vrArSupport.enableVRMode(VRModeConfig()) },
            async { vrArSupport.loadSpatialVideo(Uri.parse("content://test/video1.mp4"), SpatialVideoFormat.EQUIRECTANGULAR_360) },
            async { vrArSupport.configureProjection(ProjectionType.EQUIRECTANGULAR) },
            async { vrArSupport.enableGazeInteraction(GazeInteractionConfig(enabled = true)) },
            async { vrArSupport.applyImmersiveEffects(ImmersiveEffectsConfig(spatialAudioEnabled = true)) }
        )
        
        val results = operations.awaitAll()
        
        // Then
        assertEquals("All operations should complete", 5, results.size)
        results.forEach { result ->
            assertNotNull("Each result should not be null", result)
            // Individual results may succeed or fail based on implementation and timing
        }
    }
}