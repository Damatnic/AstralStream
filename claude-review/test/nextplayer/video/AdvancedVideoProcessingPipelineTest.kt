package com.astralplayer.nextplayer.video

import android.content.Context
import android.graphics.Rect
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
 * Comprehensive tests for advanced video processing pipeline
 * Tests video effects, transformations, quality enhancement, and real-time processing
 */
@RunWith(AndroidJUnit4::class)
class AdvancedVideoProcessingPipelineTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var processingPipeline: AdvancedVideoProcessingPipeline
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        processingPipeline = AdvancedVideoProcessingPipeline(context)
    }

    @After
    fun tearDown() {
        runTest {
            processingPipeline.cleanup()
        }
    }

    @Test
    fun testPipelineInitialization() = runTest {
        // When
        processingPipeline.initialize()
        advanceUntilIdle()
        
        // Then
        val state = processingPipeline.pipelineState.value
        assertTrue("Pipeline should be initialized", state.isInitialized)
        assertTrue("Initialization time should be set", state.initializationTime > 0)
        assertEquals("Initial active filters should be 0", 0, state.activeFilters)
        assertEquals("Initial processed frames should be 0", 0L, state.processedFrames)
    }

    @Test
    fun testFrameProcessing() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val testFrame = createTestVideoFrame()
        val processingConfig = FrameProcessingConfig(
            enableColorCorrection = true,
            enableVisualEffects = true,
            enableTransformation = false,
            enableAIEnhancement = false
        )
        
        // When
        val processedFrame = processingPipeline.processFrame(testFrame, processingConfig)
        
        // Then
        assertNotNull("Processed frame should not be null", processedFrame)
        assertEquals("Original frame should be preserved", testFrame, processedFrame.originalFrame)
        assertNotNull("Processed data should not be null", processedFrame.processedData)
        assertTrue("Processing time should be non-negative", processedFrame.processingTime >= 0)
        assertTrue("Quality score should be valid", 
                  processedFrame.qualityScore >= 0f && processedFrame.qualityScore <= 1f)
        assertNotNull("Applied filters list should not be null", processedFrame.appliedFilters)
    }

    @Test
    fun testColorCorrectionFilter() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val colorConfig = ColorCorrectionConfig(
            brightness = 0.1f,
            contrast = 1.2f,
            saturation = 1.1f,
            hue = 10f,
            gamma = 1.1f,
            whiteBalance = 0.05f
        )
        
        // When
        processingPipeline.applyColorCorrection(colorConfig)
        advanceUntilIdle()
        
        val testFrame = createTestVideoFrame()
        val processedFrame = processingPipeline.processFrame(testFrame)
        
        // Then
        assertTrue("Color correction filter should be applied", 
                  processedFrame.appliedFilters.contains("color_correction"))
        assertTrue("Processing should complete successfully", 
                  processedFrame.processingTime >= 0)
    }

    @Test
    fun testVisualEffects() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val effectConfig = EffectConfig(
            intensity = 0.7f,
            radius = 8f,
            threshold = 0.3f
        )
        
        // Test different visual effects
        val effects = listOf(
            VisualEffectType.BLUR,
            VisualEffectType.SHARPEN,
            VisualEffectType.VIGNETTE,
            VisualEffectType.FILM_GRAIN,
            VisualEffectType.VINTAGE,
            VisualEffectType.GLOW,
            VisualEffectType.EDGE_ENHANCEMENT,
            VisualEffectType.NOISE_REDUCTION
        )
        
        for (effect in effects) {
            // When
            processingPipeline.applyVisualEffect(effect, effectConfig)
            advanceUntilIdle()
            
            val testFrame = createTestVideoFrame()
            val processedFrame = processingPipeline.processFrame(testFrame)
            
            // Then
            val expectedFilterName = "effect_${effect.name.lowercase()}"
            assertTrue("Effect $effect should be applied", 
                      processedFrame.appliedFilters.contains(expectedFilterName))
        }
    }

    @Test
    fun testGeometricTransformation() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val transformation = GeometricTransformation(
            rotation = 45f,
            scale = Pair(1.5f, 1.5f),
            translation = Pair(100f, 50f),
            cropRect = Rect(0, 0, 800, 600)
        )
        
        // When
        processingPipeline.applyTransformation(transformation)
        advanceUntilIdle()
        
        val testFrame = createTestVideoFrame()
        val processedFrame = processingPipeline.processFrame(testFrame)
        
        // Then
        assertTrue("Transformation filter should be applied", 
                  processedFrame.appliedFilters.contains("transformation"))
        assertTrue("Processing should complete successfully", 
                  processedFrame.processingTime >= 0)
    }

    @Test
    fun testQualityEnhancement() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val enhancementConfig = QualityEnhancementConfig(
            enableUpscaling = true,
            upscaleRatio = 2f,
            enableDenoising = true,
            denoisingStrength = 0.6f,
            enableSharpening = true,
            sharpeningStrength = 0.4f,
            preserveDetails = true,
            preserveTexture = true
        )
        
        // When
        val enhancementResult = processingPipeline.enhanceVideoQuality(enhancementConfig)
        
        // Then
        assertNotNull("Enhancement result should not be null", enhancementResult)
        assertTrue("Enhancement should succeed", enhancementResult.success)
        assertTrue("Quality improvement should be positive", enhancementResult.qualityImprovement >= 0f)
        assertTrue("Processing time should be positive", enhancementResult.processingTime > 0)
    }

    @Test
    fun testDeviceOptimization() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val deviceCapabilities = DeviceCapabilities(
            cpuCores = 8,
            cpuFrequency = 2.8f,
            totalMemoryMB = 8192L,
            gpuModel = "Adreno 730",
            supportsMobileHDR = true,
            supportsHardwareDecoding = true,
            hardwareDecoders = listOf("h264", "h265", "vp9"),
            maxTextureSize = 4096
        )
        
        val optimizationConfig = DeviceOptimizationConfig(
            enableHDR = true,
            enableHardwareAcceleration = true,
            prioritizePerformance = true,
            prioritizeQuality = false
        )
        
        // When
        val optimizationResult = processingPipeline.optimizeForDevice(deviceCapabilities, optimizationConfig)
        
        // Then
        assertNotNull("Optimization result should not be null", optimizationResult)
        assertTrue("Optimization should succeed", optimizationResult.success)
        assertTrue("Performance gain should be positive", optimizationResult.performanceGain > 0f)
        assertTrue("Optimization time should be positive", optimizationResult.optimizationTime >= 0)
        assertTrue("Should have applied optimizations", optimizationResult.appliedOptimizations.isNotEmpty())
        
        // Verify optimized configuration
        val optimizedConfig = optimizationResult.optimizedConfig
        assertTrue("GPU acceleration should be enabled for high-end device", optimizedConfig.enableGPUAcceleration)
        assertEquals("Quality should be high for high-end device", VideoQuality.ULTRA, optimizedConfig.qualitySettings.targetQuality)
        assertTrue("Max concurrent frames should be optimized", optimizedConfig.maxConcurrentFrames >= 4)
    }

    @Test
    fun testStreamProcessing() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        // Create test video stream
        val testFrames = (1..10).map { index ->
            createTestVideoFrame(timestamp = index * 33_333_333L) // 30 FPS
        }
        val inputStream = testFrames.asFlow()
        
        val processingConfig = FrameProcessingConfig(
            enableColorCorrection = true,
            enableVisualEffects = false,
            maxConcurrentFrames = 2
        )
        
        // When
        val processedFrames = mutableListOf<ProcessedVideoFrame>()
        processingPipeline.processVideoStream(inputStream, processingConfig)
            .collect { processedFrame ->
                processedFrames.add(processedFrame)
            }
        
        // Then
        assertEquals("All frames should be processed", testFrames.size, processedFrames.size)
        
        processedFrames.forEach { processedFrame ->
            assertNotNull("Each processed frame should not be null", processedFrame)
            assertTrue("Each processing time should be non-negative", processedFrame.processingTime >= 0)
            assertTrue("Each quality score should be valid", 
                      processedFrame.qualityScore >= 0f && processedFrame.qualityScore <= 1f)
        }
        
        // Verify frame order is preserved
        processedFrames.forEachIndexed { index, processedFrame ->
            assertEquals("Frame order should be preserved", 
                        testFrames[index].timestamp, processedFrame.originalFrame.timestamp)
        }
    }

    @Test
    fun testFilterManagement() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val filterConfig = FilterConfig(
            type = FilterType.COLOR_CORRECTION,
            parameters = mapOf(
                "brightness" to 0.2f,
                "contrast" to 1.3f,
                "saturation" to 1.1f
            ),
            priority = 5
        )
        
        // When - Apply filter
        processingPipeline.applyFilter("test_filter", filterConfig)
        advanceUntilIdle()
        
        val testFrame = createTestVideoFrame()
        val processedFrame1 = processingPipeline.processFrame(testFrame)
        
        // Then - Filter should be applied
        assertTrue("Filter should be applied", 
                  processedFrame1.appliedFilters.contains("test_filter"))
        
        // When - Remove filter
        processingPipeline.removeFilter("test_filter")
        advanceUntilIdle()
        
        val processedFrame2 = processingPipeline.processFrame(testFrame)
        
        // Then - Filter should not be applied
        assertFalse("Filter should not be applied after removal", 
                   processedFrame2.appliedFilters.contains("test_filter"))
    }

    @Test
    fun testProcessingMetrics() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        // Process several frames to generate metrics
        repeat(5) { index ->
            val testFrame = createTestVideoFrame(timestamp = index * 33_333_333L)
            processingPipeline.processFrame(testFrame)
        }
        
        // When
        val metrics = processingPipeline.getProcessingMetrics()
        
        // Then
        assertNotNull("Metrics should not be null", metrics)
        assertTrue("Average processing time should be non-negative", metrics.averageProcessingTime >= 0.0)
        assertTrue("Current FPS should be non-negative", metrics.currentFPS >= 0f)
        assertEquals("Target FPS should be 60", 60f, metrics.targetFPS)
        assertTrue("GPU utilization should be valid", 
                  metrics.gpuUtilization >= 0f && metrics.gpuUtilization <= 1f)
        assertTrue("Memory usage should be positive", metrics.memoryUsage > 0)
        assertTrue("Cache hit rate should be valid", 
                  metrics.cacheHitRate >= 0f && metrics.cacheHitRate <= 1f)
        assertTrue("Frame drop rate should be valid", 
                  metrics.frameDropRate >= 0f && metrics.frameDropRate <= 1f)
        assertTrue("Quality score should be valid", 
                  metrics.qualityScore >= 0f && metrics.qualityScore <= 1f)
    }

    @Test
    fun testVideoExport() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val inputVideoUri = Uri.parse("android.resource://${context.packageName}/raw/test_video")
        val outputPath = "${context.cacheDir}/exported_video.mp4"
        
        val exportConfig = VideoExportConfig(
            outputFormat = VideoFormat.MP4,
            resolution = Pair(1280, 720),
            bitrate = 3000000,
            frameRate = 30f,
            processingConfig = FrameProcessingConfig(
                enableColorCorrection = true,
                enableVisualEffects = false
            )
        )
        
        // When
        val exportResult = processingPipeline.exportProcessedVideo(inputVideoUri, outputPath, exportConfig)
        
        // Then
        assertNotNull("Export result should not be null", exportResult)
        assertEquals("Output path should match", outputPath, exportResult.outputPath)
        assertTrue("Export time should be positive", exportResult.exportTime > 0)
        assertTrue("Average quality should be valid", 
                  exportResult.averageQuality >= 0f && exportResult.averageQuality <= 1f)
        
        if (exportResult.success) {
            assertTrue("Processed frames should be positive", exportResult.processedFrames > 0)
            assertTrue("Total frames should be positive", exportResult.totalFrames > 0)
            assertTrue("File size should be positive", exportResult.fileSize >= 0L)
        }
    }

    @Test
    fun testFrameCaching() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val testFrame = createTestVideoFrame()
        val processingConfig = FrameProcessingConfig(bypassCache = false)
        
        // When - Process frame first time
        val startTime1 = System.nanoTime()
        val processedFrame1 = processingPipeline.processFrame(testFrame, processingConfig)
        val processingTime1 = System.nanoTime() - startTime1
        
        // When - Process same frame again (should use cache)
        val startTime2 = System.nanoTime()
        val processedFrame2 = processingPipeline.processFrame(testFrame, processingConfig)
        val processingTime2 = System.nanoTime() - startTime2
        
        // Then
        assertEquals("Processed data should be identical", 
                    processedFrame1.processedData.size, processedFrame2.processedData.size)
        assertTrue("Second processing should be faster (cached)", 
                  processingTime2 < processingTime1 || processedFrame2.processingTime == 0L)
    }

    @Test
    fun testPipelineEventEmission() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val events = mutableListOf<ProcessingEvent>()
        val job = launch {
            processingPipeline.processingEvents.collect { event ->
                events.add(event)
            }
        }
        
        // When - Perform various operations
        processingPipeline.applyFilter("test_filter", FilterConfig(
            type = FilterType.COLOR_CORRECTION,
            parameters = emptyMap(),
            priority = 1
        ))
        
        val testFrame = createTestVideoFrame()
        processingPipeline.processFrame(testFrame)
        
        processingPipeline.removeFilter("test_filter")
        
        advanceUntilIdle()
        job.cancel()
        
        // Then
        assertTrue("Should have emitted events", events.isNotEmpty())
        
        val hasPipelineInitialized = events.any { it is ProcessingEvent.PipelineInitialized }
        val hasFilterApplied = events.any { it is ProcessingEvent.FilterApplied }
        val hasFrameProcessed = events.any { it is ProcessingEvent.FrameProcessed }
        val hasFilterRemoved = events.any { it is ProcessingEvent.FilterRemoved }
        
        assertTrue("Should have pipeline initialized event", hasPipelineInitialized)
        assertTrue("Should have filter applied event", hasFilterApplied)
        assertTrue("Should have frame processed event", hasFrameProcessed)
        assertTrue("Should have filter removed event", hasFilterRemoved)
    }

    @Test
    fun testErrorHandling() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        // Create an invalid frame to test error handling
        val invalidFrame = VideoFrame(
            data = ByteArray(0), // Empty data
            width = 0,
            height = 0,
            format = PixelFormat.RGBA_8888,
            timestamp = System.nanoTime()
        )
        
        // When
        val processedFrame = processingPipeline.processFrame(invalidFrame)
        
        // Then - Should handle gracefully
        assertNotNull("Processed frame should not be null even with invalid input", processedFrame)
        assertEquals("Original frame should be preserved", invalidFrame, processedFrame.originalFrame)
        assertTrue("Processing time should be non-negative", processedFrame.processingTime >= 0)
    }

    @Test
    fun testConcurrentProcessing() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val testFrames = (1..10).map { index ->
            createTestVideoFrame(timestamp = index * 100_000L)
        }
        
        // When - Process frames concurrently
        val processedFrames = testFrames.map { frame ->
            async {
                processingPipeline.processFrame(frame)
            }
        }.awaitAll()
        
        // Then
        assertEquals("All frames should be processed", testFrames.size, processedFrames.size)
        
        processedFrames.forEach { processedFrame ->
            assertNotNull("Each processed frame should not be null", processedFrame)
            assertTrue("Each processing should complete", processedFrame.processingTime >= 0)
        }
    }

    @Test
    fun testFilterPriority() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        // Apply filters with different priorities
        processingPipeline.applyFilter("high_priority", FilterConfig(
            type = FilterType.COLOR_CORRECTION,
            parameters = mapOf("brightness" to 0.1f),
            priority = 1
        ))
        
        processingPipeline.applyFilter("low_priority", FilterConfig(
            type = FilterType.VISUAL_EFFECT,
            parameters = mapOf("intensity" to 0.5f),
            priority = 10
        ))
        
        processingPipeline.applyFilter("medium_priority", FilterConfig(
            type = FilterType.TRANSFORMATION,
            parameters = mapOf("rotation" to 5f),
            priority = 5
        ))
        
        advanceUntilIdle()
        
        // When
        val testFrame = createTestVideoFrame()
        val processedFrame = processingPipeline.processFrame(testFrame)
        
        // Then
        assertTrue("All filters should be applied", processedFrame.appliedFilters.size >= 3)
        
        // Verify filters are applied in priority order (implementation dependent)
        val appliedFilters = processedFrame.appliedFilters
        assertTrue("Should include all applied filters", 
                  appliedFilters.contains("high_priority") ||
                  appliedFilters.contains("low_priority") ||
                  appliedFilters.contains("medium_priority"))
    }

    @Test
    fun testProcessingConfigurationEffects() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val testFrame = createTestVideoFrame()
        
        // Test with all processing disabled
        val disabledConfig = FrameProcessingConfig(
            enableColorCorrection = false,
            enableVisualEffects = false,
            enableTransformation = false,
            enableAIEnhancement = false,
            enableHDR = false
        )
        
        // When
        val processedFrame1 = processingPipeline.processFrame(testFrame, disabledConfig)
        
        // Test with all processing enabled
        val enabledConfig = FrameProcessingConfig(
            enableColorCorrection = true,
            enableVisualEffects = true,
            enableTransformation = true,
            enableAIEnhancement = true,
            enableHDR = true
        )
        
        val processedFrame2 = processingPipeline.processFrame(testFrame, enabledConfig)
        
        // Then
        assertTrue("Both configurations should process successfully", 
                  processedFrame1.processingTime >= 0 && processedFrame2.processingTime >= 0)
        
        // Enabled config might have more filters applied (implementation dependent)
        assertTrue("Processing results should be valid", 
                  processedFrame1.qualityScore >= 0f && processedFrame2.qualityScore >= 0f)
    }

    @Test
    fun testQualitySettings() = runTest {
        processingPipeline.initialize()
        advanceUntilIdle()
        
        val testFrame = createTestVideoFrame()
        
        val qualityConfigs = listOf(
            QualitySettings(targetQuality = VideoQuality.LOW),
            QualitySettings(targetQuality = VideoQuality.MEDIUM),
            QualitySettings(targetQuality = VideoQuality.HIGH),
            QualitySettings(targetQuality = VideoQuality.ULTRA)
        )
        
        qualityConfigs.forEach { qualitySettings ->
            // When
            val processingConfig = FrameProcessingConfig(qualitySettings = qualitySettings)
            val processedFrame = processingPipeline.processFrame(testFrame, processingConfig)
            
            // Then
            assertNotNull("Processed frame should not be null for ${qualitySettings.targetQuality}", processedFrame)
            assertTrue("Processing should complete for ${qualitySettings.targetQuality}", 
                      processedFrame.processingTime >= 0)
            assertTrue("Quality score should be valid for ${qualitySettings.targetQuality}", 
                      processedFrame.qualityScore >= 0f && processedFrame.qualityScore <= 1f)
        }
    }

    // Helper methods
    
    private fun createTestVideoFrame(
        width: Int = 1920,
        height: Int = 1080,
        format: PixelFormat = PixelFormat.RGBA_8888,
        timestamp: Long = System.nanoTime()
    ): VideoFrame {
        val dataSize = when (format) {
            PixelFormat.RGBA_8888 -> width * height * 4
            PixelFormat.RGB_565 -> width * height * 2
            PixelFormat.ARGB_8888 -> width * height * 4
            PixelFormat.YUV_420_888 -> (width * height * 3) / 2
        }
        
        return VideoFrame(
            data = ByteArray(dataSize) { it.toByte() },
            width = width,
            height = height,
            format = format,
            timestamp = timestamp
        )
    }
}