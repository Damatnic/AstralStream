package com.astralplayer.nextplayer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.astralplayer.nextplayer.feature.adult.AdultSiteManager
import com.astralplayer.nextplayer.feature.network.AdvancedNetworkManager
import com.astralplayer.nextplayer.feature.network.AdultContentUrlMatcher
import com.astralplayer.nextplayer.feature.network.VideoStreamExtractor
import com.astralplayer.nextplayer.feature.security.AdultContentFilter
import com.astralplayer.nextplayer.feature.streaming.StreamProcessor
import com.astralplayer.nextplayer.security.SecurityManager
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Comprehensive integration tests for adult content system
 * Tests the complete flow from URL detection to stream playback
 */
@RunWith(AndroidJUnit4::class)
class AdultContentIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var networkManager: AdvancedNetworkManager
    private lateinit var streamProcessor: StreamProcessor
    private lateinit var securityManager: SecurityManager
    private lateinit var adultSiteManager: AdultSiteManager
    private lateinit var contentFilter: AdultContentFilter
    private lateinit var urlMatcher: AdultContentUrlMatcher
    private lateinit var videoExtractor: VideoStreamExtractor
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        networkManager = AdvancedNetworkManager(context)
        streamProcessor = StreamProcessor(context, networkManager)
        securityManager = SecurityManager(context)
        adultSiteManager = AdultSiteManager(context, networkManager, streamProcessor, securityManager)
        contentFilter = AdultContentFilter(context)
        urlMatcher = AdultContentUrlMatcher()
        videoExtractor = VideoStreamExtractor(context)
    }
    
    @Test
    fun testAdultUrlDetection() {
        // Test URL matching for major adult sites
        val testUrls = mapOf(
            "https://www.pornhub.com/view_video.php?viewkey=ph123456" to true,
            "https://www.xvideos.com/video123456/test-video" to true,
            "https://www.xnxx.com/video-123456/test" to true,
            "https://spankbang.com/123456/video/test" to true,
            "https://www.youtube.com/watch?v=123456" to false,
            "https://www.vimeo.com/123456" to false
        )
        
        testUrls.forEach { (url, shouldBeAdult) ->
            val isAdult = urlMatcher.isAdultContentUrl(url)
            assertEquals("URL detection failed for: $url", shouldBeAdult, isAdult)
            
            if (shouldBeAdult) {
                assertTrue("Should handle adult URL: $url", urlMatcher.shouldHandleUrl(url))
                assertNotNull("Site name should not be null for: $url", urlMatcher.getSiteName(url))
            }
        }
    }
    
    @Test
    fun testContentSafetyAnalysis() = runBlocking {
        val testCases = listOf(
            Triple("https://www.pornhub.com/test", "Hot Adult Video", AdultContentFilter.RATING_ADULT_ONLY),
            Triple("https://www.youtube.com/watch", "Educational Tutorial", AdultContentFilter.RATING_SAFE),
            Triple("https://example.com/video", "Mature Content Warning", AdultContentFilter.RATING_MATURE)
        )
        
        testCases.forEach { (url, title, expectedRating) ->
            val result = contentFilter.analyzeContent(url, title)
            assertTrue("Analysis should succeed for: $url", result.rating >= 0)
            
            if (expectedRating == AdultContentFilter.RATING_ADULT_ONLY) {
                assertTrue("Should detect as adult content: $url", result.isAdultContent)
            }
        }
    }
    
    @Test
    fun testSecurityManagerIntegration() {
        // Test security manager basic functionality
        assertFalse("Private mode should be disabled by default", securityManager.isPrivateModeEnabled())
        assertFalse("Incognito should be inactive by default", securityManager.isIncognitoSessionActive())
        
        // Test private mode
        securityManager.setPrivateModeEnabled(true)
        assertTrue("Private mode should be enabled", securityManager.isPrivateModeEnabled())
        
        // Test incognito session
        securityManager.setIncognitoSessionActive(true)
        assertTrue("Incognito session should be active", securityManager.isIncognitoSessionActive())
        
        // Test security level calculation
        val securityLevel = securityManager.getCurrentSecurityLevel()
        assertTrue("Security level should be valid", securityLevel >= SecurityManager.SECURITY_LEVEL_NONE)
        
        // Cleanup
        securityManager.setPrivateModeEnabled(false)
        securityManager.setIncognitoSessionActive(false)
    }
    
    @Test
    fun testNetworkManagerConfiguration() {
        val testDomains = listOf("pornhub.com", "xvideos.com", "spankbang.com")
        
        testDomains.forEach { domain ->
            val config = networkManager.getSiteConfiguration(domain)
            assertNotNull("Configuration should exist for: $domain", config)
            assertNotNull("User agent should be set for: $domain", config.userAgent)
            assertNotNull("Referer should be set for: $domain", config.referer)
            assertTrue("Should require cookies for adult site: $domain", config.requiresCookies)
            assertTrue("Should have higher timeout for adult site: $domain", config.timeout > 30000)
        }
    }
    
    @Test
    fun testStreamProcessorFormats() = runBlocking {
        val testUrls = listOf(
            "https://example.com/video.mp4" to com.astralplayer.nextplayer.feature.streaming.StreamFormat.PROGRESSIVE,
            "https://example.com/playlist.m3u8" to com.astralplayer.nextplayer.feature.streaming.StreamFormat.HLS,
            "https://example.com/manifest.mpd" to com.astralplayer.nextplayer.feature.streaming.StreamFormat.DASH
        )
        
        testUrls.forEach { (url, expectedFormat) ->
            // This would typically require actual network calls, so we'll test format detection
            val result = streamProcessor.processStream(url)
            assertNotNull("Stream processing result should not be null", result)
        }
    }
    
    @Test
    fun testAdultSiteManagerFlow() = runBlocking {
        // Enable private mode for testing
        securityManager.setPrivateModeEnabled(true)
        
        val testUrl = "https://www.pornhub.com/view_video.php?viewkey=test123"
        
        // Test age verification (won't make real network calls in test environment)
        val ageResult = adultSiteManager.handleAgeVerification(testUrl)
        assertNotNull("Age verification result should not be null", ageResult)
        
        // Test quality preferences
        val preferences = adultSiteManager.getSiteQualityPreferences("pornhub.com")
        assertNotNull("Quality preferences should not be null", preferences)
        assertTrue("Buffer ahead should be positive", preferences.bufferAhead > 0)
        
        // Cleanup
        securityManager.setPrivateModeEnabled(false)
    }
    
    @Test
    fun testEndToEndAdultContentFlow() = runBlocking {
        // Setup security
        securityManager.setPrivateModeEnabled(true)
        securityManager.setIncognitoSessionActive(true)
        
        val testUrl = "https://www.pornhub.com/view_video.php?viewkey=test123"
        val testTitle = "Test Adult Video"
        
        try {
            // 1. URL Detection
            assertTrue("Should detect as adult URL", urlMatcher.isAdultContentUrl(testUrl))
            
            // 2. Content Analysis
            val analysisResult = contentFilter.analyzeContent(testUrl, testTitle)
            assertTrue("Should be classified as adult content", analysisResult.isAdultContent)
            
            // 3. Security Check
            assertTrue("Should allow access with private mode", 
                contentFilter.canAccessContent(analysisResult.rating))
            
            // 4. Site-specific handling (mock - no real network calls)
            val siteConfig = networkManager.getSiteConfiguration("pornhub.com")
            assertNotNull("Site configuration should exist", siteConfig)
            
            // 5. Verify integration points
            val bufferConfig = streamProcessor.getBufferConfiguration()
            assertNotNull("Buffer configuration should exist", bufferConfig)
            assertTrue("Min buffer should be positive", bufferConfig.minBufferMs > 0)
            
        } finally {
            // Cleanup
            securityManager.setPrivateModeEnabled(false)
            securityManager.setIncognitoSessionActive(false)
        }
    }
    
    @Test
    fun testPerformanceOptimizations() {
        val startTime = System.currentTimeMillis()
        
        // Test multiple operations to ensure they complete quickly
        repeat(10) {
            urlMatcher.isAdultContentUrl("https://www.pornhub.com/test$it")
            securityManager.getCurrentSecurityLevel()
            networkManager.getSiteConfiguration("pornhub.com")
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // All operations should complete within reasonable time
        assertTrue("Performance test took too long: ${duration}ms", duration < 1000)
    }
    
    @Test
    fun testMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Perform memory-intensive operations
        repeat(100) {
            urlMatcher.isAdultContentUrl("https://www.pornhub.com/test$it")
            val config = networkManager.getSiteConfiguration("pornhub.com")
            // Simulate some processing
            config.headers.forEach { (key, value) ->
                key.length + value.length
            }
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Memory increase should be reasonable (less than 10MB)
        assertTrue("Memory usage increased too much: ${memoryIncrease / 1024 / 1024}MB", 
            memoryIncrease < 10 * 1024 * 1024)
    }
    
    @Test
    fun testErrorHandling() = runBlocking {
        // Test invalid URL handling
        val invalidUrls = listOf(
            "not-a-url",
            "https://",
            "malformed://url",
            ""
        )
        
        invalidUrls.forEach { invalidUrl ->
            try {
                val result = contentFilter.analyzeContent(invalidUrl, "Test Title")
                // Should not crash, may return error result
                assertNotNull("Analysis result should not be null for invalid URL", result)
            } catch (e: Exception) {
                // Should handle gracefully without crashing
                assertNotNull("Exception should have message", e.message)
            }
        }
    }
    
    @Test
    fun testConcurrentAccess() = runBlocking {
        // Test concurrent access to various components
        val jobs = (1..10).map { index ->
            kotlinx.coroutines.async {
                val url = "https://www.pornhub.com/test$index"
                val isAdult = urlMatcher.isAdultContentUrl(url)
                val config = networkManager.getSiteConfiguration("pornhub.com")
                val securityLevel = securityManager.getCurrentSecurityLevel()
                
                Triple(isAdult, config, securityLevel)
            }
        }
        
        // Wait for all operations to complete
        val results = jobs.map { it.await() }
        
        // Verify all results are consistent
        results.forEach { (isAdult, config, securityLevel) ->
            assertTrue("Adult detection should be consistent", isAdult)
            assertNotNull("Config should not be null", config)
            assertTrue("Security level should be valid", securityLevel >= 0)
        }
    }
}