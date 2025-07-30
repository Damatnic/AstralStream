package com.astralplayer.nextplayer.integration

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.nextplayer.cache.AdvancedCacheManager
import com.astralplayer.nextplayer.streaming.VideoStreamOptimizer
import com.astralplayer.nextplayer.streaming.NetworkAnalyzer
import com.astralplayer.nextplayer.quality.VideoQualityManager
import com.astralplayer.nextplayer.quality.AdaptiveQualityController
import com.astralplayer.nextplayer.accessibility.AccessibilityManager
import com.astralplayer.nextplayer.data.gesture.EnhancedGestureDetector
import com.astralplayer.nextplayer.error.ErrorHandlingService
import com.astralplayer.nextplayer.viewmodel.EnhancedPlayerViewModel
import kotlinx.coroutines.*
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
 * Integration tests for error handling across all system components
 * Ensures graceful degradation and recovery from various failure scenarios
 */
@RunWith(AndroidJUnit4::class)
class ErrorHandlingIntegrationTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    
    @Mock private lateinit var mockExoPlayer: ExoPlayer
    
    private lateinit var errorHandlingService: ErrorHandlingService
    private lateinit var cacheManager: AdvancedCacheManager
    private lateinit var streamOptimizer: VideoStreamOptimizer
    private lateinit var networkAnalyzer: NetworkAnalyzer
    private lateinit var qualityManager: VideoQualityManager
    private lateinit var adaptiveController: AdaptiveQualityController
    private lateinit var accessibilityManager: AccessibilityManager
    private lateinit var gestureDetector: EnhancedGestureDetector
    private lateinit var playerViewModel: EnhancedPlayerViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        
        errorHandlingService = ErrorHandlingService(context)
        networkAnalyzer = NetworkAnalyzer(context)
        cacheManager = AdvancedCacheManager(context)
        qualityManager = VideoQualityManager(context, mockExoPlayer)
        adaptiveController = AdaptiveQualityController(context, mockExoPlayer, networkAnalyzer)
        streamOptimizer = VideoStreamOptimizer(context, mockExoPlayer)
        accessibilityManager = AccessibilityManager(context)
        gestureDetector = EnhancedGestureDetector(context)
        playerViewModel = EnhancedPlayerViewModel(
            context, networkAnalyzer, cacheManager, qualityManager, accessibilityManager
        )
    }

    @After
    fun tearDown() {
        runTest {
            errorHandlingService.cleanup()
            cacheManager.cleanup()
            streamOptimizer.cleanup()
            networkAnalyzer.cleanup()
            qualityManager.cleanup()
            adaptiveController.cleanup()
            accessibilityManager.cleanup()
            gestureDetector.cleanup()
        }
    }

    @Test
    fun testNetworkErrorRecovery() = runTest {
        // Given - System initialized and running
        initializeAllComponents()
        val testUri = Uri.parse("https://example.com/test-video.mp4")
        
        // Start normal playback
        playerViewModel.startPlayback(testUri)
        advanceUntilIdle()
        
        // When - Network error occurs
        networkAnalyzer.simulateNetworkError("Connection timeout")
        advanceUntilIdle()
        
        // Then - System should handle error gracefully
        val errorState = errorHandlingService.getErrorState()
        assertTrue("Error should be detected and handled", errorState.hasActiveErrors)
        assertTrue("Should be network-related error", 
                  errorState.lastError?.category == com.astralplayer.nextplayer.error.ErrorCategory.NETWORK)
        
        // Verify components adapt to error
        val adaptationState = adaptiveController.currentAdaptationState.value
        assertTrue("Adaptive controller should remain functional", 
                  adaptationState.strategy != null)
        
        val cacheState = cacheManager.cacheState.value
        assertTrue("Cache should continue operating during network error",
                  cacheState.health.score > 0.2f)
        
        // When - Network recovers
        networkAnalyzer.simulateNetworkRecovery()
        advanceUntilIdle()
        
        // Then - System should recover
        val recoveredErrorState = errorHandlingService.getErrorState()
        assertFalse("Error state should clear after recovery", recoveredErrorState.hasActiveErrors)
        
        val playerState = playerViewModel.playerState.value
        assertTrue("Player should resume normal operation", 
                  playerState.playbackState != "error")
    }

    @Test
    fun testCacheErrorHandling() = runTest {
        // Given - Cache operations in progress
        initializeAllComponents()
        
        val testUris = (1..5).map { Uri.parse("https://example.com/cache-test$it.mp4") }
        
        // Start caching operations
        testUris.forEach { uri ->
            launch { cacheManager.cacheContent(uri) }
        }
        advanceUntilIdle()
        
        // When - Cache storage error occurs
        cacheManager.simulateStorageError("Disk full")
        advanceUntilIdle()
        
        // Then - System should handle cache errors gracefully
        val errorState = errorHandlingService.getErrorState()
        assertTrue("Cache error should be handled", 
                  errorState.errors.any { it.category == com.astralplayer.nextplayer.error.ErrorCategory.STORAGE })
        
        val cacheState = cacheManager.cacheState.value
        assertTrue("Cache should attempt recovery", cacheState.health.score > 0.1f)
        
        // Other components should continue functioning
        val gestureResult = gestureDetector.detectGesture(
            startX = 200f, startY = 300f, endX = 400f, endY = 300f, velocityX = 500f
        )
        assertTrue("Gesture detection should remain functional", gestureResult.isRecognized)
        
        val networkMetrics = networkAnalyzer.getRealtimeMetrics()
        assertTrue("Network monitoring should continue", networkMetrics.bandwidth > 0)
    }

    @Test
    fun testQualityAdaptationErrorHandling() = runTest {
        // Given - Quality adaptation in progress
        initializeAllComponents()
        
        val qualities = listOf(
            createQuality("480p", 480, 1_200_000),
            createQuality("720p", 720, 2_500_000),
            createQuality("1080p", 1080, 5_000_000)
        )
        qualityManager.setAvailableQualities(qualities)
        advanceUntilIdle()
        
        // When - Quality adaptation error occurs
        qualityManager.simulateAdaptationError("Invalid quality configuration")
        advanceUntilIdle()
        
        // Then - System should handle quality errors
        val errorState = errorHandlingService.getErrorState()
        assertTrue("Quality error should be tracked", 
                  errorState.errors.any { it.category == com.astralplayer.nextplayer.error.ErrorCategory.PLAYBACK })
        
        val currentQuality = qualityManager.currentQuality.value
        assertNotNull("Should maintain a valid quality", currentQuality)
        
        // Adaptive controller should handle gracefully
        val adaptationInsights = adaptiveController.getAdaptationInsights()
        assertTrue("Adaptation should remain stable", 
                  adaptationInsights.adaptationAccuracy > 0.3f)
        
        // Should fall back to safe quality
        assertTrue("Should use safe fallback quality", 
                  currentQuality!!.bitrate <= 2_500_000) // Medium quality fallback
    }

    @Test
    fun testStreamingErrorRecovery() = runTest {
        // Given - Streaming optimization active
        initializeAllComponents()
        val streamUri = Uri.parse("https://example.com/error-stream.m3u8")
        
        streamOptimizer.optimizeForStreaming(streamUri)
        advanceUntilIdle()
        
        // When - Streaming error occurs
        streamOptimizer.simulateStreamingError("Segment download failed")
        advanceUntilIdle()
        
        // Then - Should implement recovery strategies
        val errorState = errorHandlingService.getErrorState()
        assertTrue("Streaming error should be handled", 
                  errorState.errors.any { it.category == com.astralplayer.nextplayer.error.ErrorCategory.STREAMING })
        
        val streamingState = streamOptimizer.optimizationState.value
        assertTrue("Should attempt recovery", streamingState.recoveryAttempts > 0)
        
        // Should adjust strategy for robustness
        assertTrue("Should use more conservative strategy after error",
                  streamingState.currentStrategy.toString().contains("conservative", ignoreCase = true))
        
        // Other components should adapt
        val adaptationState = adaptiveController.currentAdaptationState.value
        assertTrue("Should trigger error-based adaptation",
                  adaptationState.lastAdaptationTime > 0L)
    }

    @Test
    fun testAccessibilityErrorHandling() = runTest {
        // Given - Accessibility features active
        initializeAllComponents()
        
        // Enable accessibility features
        accessibilityManager.configureHighContrastMode(true)
        accessibilityManager.enableAudioDescriptions(true)
        advanceUntilIdle()
        
        // When - Accessibility service error occurs
        accessibilityManager.simulateAccessibilityError("TTS service unavailable")
        advanceUntilIdle()
        
        // Then - Should degrade gracefully
        val errorState = errorHandlingService.getErrorState()
        assertTrue("Accessibility error should be tracked", 
                  errorState.errors.any { it.category == com.astralplayer.nextplayer.error.ErrorCategory.ACCESSIBILITY })
        
        val accessibilityState = accessibilityManager.accessibilityState.value
        assertTrue("Should maintain core accessibility features", 
                  accessibilityState.highContrastEnabled)
        
        // Should provide alternative feedback methods
        val hapticFeedback = accessibilityManager.provideHapticFeedback(
            com.astralplayer.nextplayer.accessibility.HapticFeedbackType.MEDIUM
        )
        // Haptic should work even if audio feedback fails
        
        // UI should remain accessible
        val accessibilityReport = accessibilityManager.generateAccessibilityReport()
        assertTrue("Should maintain minimum accessibility compliance",
                  accessibilityReport.wcagComplianceLevel.ordinal >= 1) // At least A level
    }

    @Test
    fun testGestureDetectionErrorHandling() = runTest {
        // Given - Gesture detection active
        initializeAllComponents()
        gestureDetector.enableHapticFeedback(true)
        
        // When - Gesture detection error occurs
        gestureDetector.simulateDetectionError("Touch sensor malfunction")
        advanceUntilIdle()
        
        // Then - Should handle gracefully
        val errorState = errorHandlingService.getErrorState()
        assertTrue("Gesture error should be tracked", 
                  errorState.errors.any { it.category == com.astralplayer.nextplayer.error.ErrorCategory.INPUT })
        
        // Should fall back to alternative input methods
        val gestureState = gestureDetector.detectionState.value
        assertTrue("Should enable fallback input methods", 
                  gestureState.fallbackInputEnabled)
        
        // Basic gestures should still work
        val simpleGesture = gestureDetector.detectSimpleGesture(
            startX = 300f, startY = 300f, endX = 400f, endY = 300f
        )
        assertTrue("Simple gestures should remain functional", simpleGesture.isRecognized)
    }

    @Test
    fun testCascadingErrorHandling() = runTest {
        // Given - Multiple systems active
        initializeAllComponents()
        val testUri = Uri.parse("https://example.com/cascade-test.mp4")
        
        playerViewModel.startPlayback(testUri)
        advanceUntilIdle()
        
        // When - Multiple errors occur in sequence
        // 1. Network error
        networkAnalyzer.simulateNetworkError("Connection lost")
        delay(100)
        
        // 2. Cache error due to network issues
        cacheManager.simulateStorageError("Cache corruption")
        delay(100)
        
        // 3. Quality adaptation error due to network unavailability
        qualityManager.simulateAdaptationError("No network for quality check")
        delay(100)
        
        advanceUntilIdle()
        
        // Then - Should handle cascading errors
        val errorState = errorHandlingService.getErrorState()
        assertTrue("Should track multiple error categories", errorState.errors.size >= 3)
        
        // Should prioritize critical errors
        val criticalErrors = errorState.errors.filter { 
            it.severity == com.astralplayer.nextplayer.error.ErrorSeverity.CRITICAL 
        }
        assertTrue("Should identify critical errors", criticalErrors.isNotEmpty())
        
        // System should maintain core functionality
        val playerState = playerViewModel.playerState.value
        assertTrue("Player should enter safe mode", 
                  playerState.playbackState == "safe_mode" || playerState.error != null)
        
        // Should attempt staged recovery
        assertTrue("Should initiate recovery procedures", 
                  errorState.recoveryAttempts > 0)
    }

    @Test
    fun testErrorRecoveryCoordination() = runTest {
        // Given - System in error state
        initializeAllComponents()
        
        // Simulate multiple errors
        networkAnalyzer.simulateNetworkError("Network unavailable")
        cacheManager.simulateStorageError("Storage full")
        advanceUntilIdle()
        
        val initialErrorState = errorHandlingService.getErrorState()
        assertTrue("Should have multiple errors", initialErrorState.errors.size >= 2)
        
        // When - Recovery begins
        errorHandlingService.initiateSystemRecovery()
        
        // Simulate partial recovery
        networkAnalyzer.simulateNetworkRecovery()
        delay(500)
        
        cacheManager.optimizeStorage(force = true)
        delay(500)
        
        advanceUntilIdle()
        
        // Then - Should coordinate recovery across components
        val recoveredErrorState = errorHandlingService.getErrorState()
        assertTrue("Should clear resolved errors", 
                  recoveredErrorState.errors.size < initialErrorState.errors.size)
        
        // Components should be restored
        val networkMetrics = networkAnalyzer.getRealtimeMetrics()
        assertTrue("Network should be functional after recovery", 
                  networkMetrics.bandwidth > 0)
        
        val cacheState = cacheManager.cacheState.value
        assertTrue("Cache should be healthier after recovery", 
                  cacheState.health.score > 0.5f)
        
        // Player should resume normal operation
        val playerState = playerViewModel.playerState.value
        assertFalse("Player should exit error state", 
                   playerState.playbackState == "error")
    }

    @Test
    fun testErrorReportingAndAnalytics() = runTest {
        // Given - Various errors occur during operation
        initializeAllComponents()
        
        // Generate different types of errors
        networkAnalyzer.simulateNetworkError("Intermittent connectivity")
        delay(100)
        
        qualityManager.simulateAdaptationError("Quality switch timeout")
        delay(100)
        
        gestureDetector.simulateDetectionError("Gesture timeout")
        delay(100)
        
        accessibilityManager.simulateAccessibilityError("Screen reader unavailable")
        advanceUntilIdle()
        
        // When - Generate error report
        val errorReport = errorHandlingService.generateErrorReport()
        
        // Then - Should provide comprehensive error analytics
        assertTrue("Should track error categories", errorReport.errorsByCategory.isNotEmpty())
        assertTrue("Should track error frequency", errorReport.totalErrors > 0)
        assertTrue("Should track recovery success rate", errorReport.recoverySuccessRate >= 0f)
        
        // Should identify patterns
        assertTrue("Should identify error patterns", errorReport.errorPatterns.isNotEmpty())
        
        // Should provide recommendations
        assertTrue("Should provide recovery recommendations", 
                  errorReport.recoveryRecommendations.isNotEmpty())
        
        // Error history should be maintained
        assertTrue("Should maintain error history", errorReport.recentErrors.isNotEmpty())
        
        // Should track system health impact
        assertTrue("Should track system health impact", 
                  errorReport.systemHealthImpact >= 0f)
    }

    // Helper methods
    private suspend fun initializeAllComponents() {
        errorHandlingService.initialize()
        networkAnalyzer.initialize()
        cacheManager.initialize()
        qualityManager.initialize(networkAnalyzer)
        adaptiveController.initialize()
        streamOptimizer.initialize()
        accessibilityManager.initialize()
        gestureDetector.initialize()
        
        // Register error handlers
        errorHandlingService.registerErrorHandler(networkAnalyzer)
        errorHandlingService.registerErrorHandler(cacheManager)
        errorHandlingService.registerErrorHandler(qualityManager)
        errorHandlingService.registerErrorHandler(streamOptimizer)
        errorHandlingService.registerErrorHandler(accessibilityManager)
        errorHandlingService.registerErrorHandler(gestureDetector)
        
        advanceUntilIdle()
    }

    private fun createQuality(name: String, height: Int, bitrate: Int) = 
        com.astralplayer.nextplayer.quality.VideoQuality(
            id = name.lowercase(),
            name = name,
            width = (height * 16) / 9,
            height = height,
            bitrate = bitrate,
            frameRate = 30f,
            codec = "h264"
        )
}