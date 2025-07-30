package com.astralplayer.nextplayer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.astralplayer.nextplayer.integration.FeatureIntegrationManager
import com.astralplayer.nextplayer.integration.IntegrationResult
import com.astralplayer.nextplayer.optimization.PerformanceOptimizationManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Comprehensive integration test for the complete AstralStream system
 * Tests the entire flow from initialization to adult content processing
 */
@RunWith(AndroidJUnit4::class)
class ComprehensiveIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var integrationManager: FeatureIntegrationManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        integrationManager = FeatureIntegrationManager(context)
    }
    
    @After
    fun cleanup() {
        integrationManager.cleanup()
    }
    
    @Test
    fun testFullSystemInitialization() = runBlocking {
        // Test complete system initialization
        val result = withTimeout(30000) { // 30 second timeout
            integrationManager.initialize()
        }
        
        assertTrue("System initialization should succeed", result is IntegrationResult.Success)
        assertTrue("System should be fully initialized", integrationManager.isFullyInitialized.value)
        
        // Verify all major components are ready
        assertTrue("Security should be ready", integrationManager.isSecurityReady())
        assertTrue("Network should be ready", integrationManager.isNetworkReady())
        assertTrue("Media should be ready", integrationManager.isMediaReady())
        assertTrue("Optimization should be ready", integrationManager.isOptimizationReady())
        
        if (result is IntegrationResult.Success) {
            val summary = result.summary
            assertTrue("Core services should be ready", summary.coreServicesReady)
            assertTrue("Security layer should be ready", summary.securityLayerReady)
            assertTrue("Network layer should be ready", summary.networkLayerReady)
            assertTrue("Media layer should be ready", summary.mediaLayerReady)
            assertTrue("Feature count should be positive", summary.featureCount > 0)
        }
    }
    
    @Test
    fun testMxPlayerStylePerformanceOptimization() = runBlocking {
        // Initialize system
        val initResult = integrationManager.initialize()
        assertTrue("Initialization should succeed", initResult is IntegrationResult.Success)
        
        val performanceManager = integrationManager.getPerformanceManager()
        assertNotNull("Performance manager should be available", performanceManager)
        
        // Test different optimization levels
        val optimizationLevels = listOf(
            PerformanceOptimizationManager.Companion.OptimizationLevel.BATTERY_SAVER,
            PerformanceOptimizationManager.Companion.OptimizationLevel.BALANCED,
            PerformanceOptimizationManager.Companion.OptimizationLevel.PERFORMANCE,
            PerformanceOptimizationManager.Companion.OptimizationLevel.GAMING
        )
        
        optimizationLevels.forEach { level ->
            performanceManager?.setOptimizationLevel(level)
            assertEquals("Optimization level should be set correctly", 
                level, performanceManager?.optimizationLevel?.value)
            
            // Test optimized configurations
            val bufferConfig = performanceManager?.getOptimizedBufferConfig()
            assertNotNull("Buffer config should not be null", bufferConfig)
            assertTrue("Min buffer should be positive", bufferConfig!!.minBufferMs > 0)
            assertTrue("Max buffer should be greater than min", 
                bufferConfig.maxBufferMs > bufferConfig.minBufferMs)
            
            val qualityConfig = performanceManager?.getOptimizedQualitySettings()
            assertNotNull("Quality config should not be null", qualityConfig)
            assertTrue("Max resolution should be positive", qualityConfig!!.maxResolution > 0)
            assertTrue("Max bitrate should be positive", qualityConfig.maxBitrate > 0)
        }
        
        // Test MX Player style recommendations
        val recommendations = performanceManager?.getMxPlayerStyleRecommendations()
        assertNotNull("Recommendations should not be null", recommendations)
        
        // Performance counters should work
        performanceManager?.recordFrameDrop()
        performanceManager?.recordNetworkRequest()
        performanceManager?.recordCacheHit()
        performanceManager?.recordCacheMiss()
        
        val metrics = performanceManager?.performanceMetrics?.value
        assertNotNull("Performance metrics should be available", metrics)
    }
    
    @Test
    fun testAdultContentSystemIntegration() = runBlocking {
        // Initialize system
        val initResult = integrationManager.initialize()
        assertTrue("Initialization should succeed", initResult is IntegrationResult.Success)
        
        // Enable security for testing
        val securityManager = integrationManager.getSecurityManager()
        assertNotNull("Security manager should be available", securityManager)
        
        securityManager!!.setPrivateModeEnabled(true)
        securityManager.setIncognitoSessionActive(true)
        
        try {
            // Test adult content processing flow
            val testUrl = "https://www.pornhub.com/view_video.php?viewkey=test123"
            
            val result = integrationManager.processAdultContent(testUrl)
            assertNotNull("Adult content processing should not return null", result)
            
            // Test content filter integration
            val contentFilter = integrationManager.getContentFilter()
            assertNotNull("Content filter should be available", contentFilter)
            
            val analysisResult = contentFilter!!.analyzeContent(testUrl, "Test Adult Video")
            assertTrue("Should detect as adult content", analysisResult.isAdultContent)
            assertTrue("Should allow access with private mode", 
                contentFilter.canAccessContent(analysisResult.rating))
            
            // Test network manager integration
            val networkManager = integrationManager.getNetworkManager()
            assertNotNull("Network manager should be available", networkManager)
            
            val siteConfig = networkManager!!.getSiteConfiguration("pornhub.com")
            assertNotNull("Site configuration should exist", siteConfig)
            assertTrue("Should require cookies for adult site", siteConfig.requiresCookies)
            
        } finally {
            // Cleanup security settings
            securityManager.setPrivateModeEnabled(false)
            securityManager.setIncognitoSessionActive(false)
        }
    }
    
    @Test
    fun testSystemHealthMonitoring() = runBlocking {
        // Initialize system
        val initResult = integrationManager.initialize()
        assertTrue("Initialization should succeed", initResult is IntegrationResult.Success)
        
        // Test system health monitoring
        val healthStatus = integrationManager.getSystemHealth()
        assertNotNull("Health status should not be null", healthStatus)
        
        // Health levels should be valid
        assertNotNull("Overall health should not be null", healthStatus.overallHealth)
        assertNotNull("Memory health should not be null", healthStatus.memoryHealth)
        assertNotNull("Network health should not be null", healthStatus.networkHealth)
        assertNotNull("Security health should not be null", healthStatus.securityHealth)
        
        // Recommendations should be available
        assertNotNull("Recommendations should not be null", healthStatus.recommendations)
        
        // If system is healthy, most components should be good
        if (healthStatus.overallHealth == com.astralplayer.nextplayer.integration.HealthLevel.GOOD) {
            assertEquals("Network health should be good when overall is good",
                com.astralplayer.nextplayer.integration.HealthLevel.GOOD, healthStatus.networkHealth)
        }
    }
    
    @Test
    fun testConcurrentOperations() = runBlocking {
        // Initialize system
        val initResult = integrationManager.initialize()
        assertTrue("Initialization should succeed", initResult is IntegrationResult.Success)
        
        // Test concurrent operations (simulating real-world usage)
        val jobs = (1..5).map { index ->
            kotlinx.coroutines.async {
                val testUrl = "https://www.pornhub.com/test$index"
                
                // Multiple concurrent operations
                val securityManager = integrationManager.getSecurityManager()
                val networkManager = integrationManager.getNetworkManager()
                val contentFilter = integrationManager.getContentFilter()
                val performanceManager = integrationManager.getPerformanceManager()
                
                // Simulate concurrent access
                val securityLevel = securityManager?.getCurrentSecurityLevel()
                val siteConfig = networkManager?.getSiteConfiguration("pornhub.com")
                val healthStatus = integrationManager.getSystemHealth()
                
                // Record some performance metrics
                performanceManager?.recordNetworkRequest()
                performanceManager?.recordCacheHit()
                
                Triple(securityLevel, siteConfig, healthStatus)
            }
        }
        
        // Wait for all operations to complete
        val results = jobs.map { it.await() }
        
        // Verify all operations completed successfully
        results.forEach { (securityLevel, siteConfig, healthStatus) ->
            assertNotNull("Security level should not be null", securityLevel)
            assertNotNull("Site config should not be null", siteConfig)
            assertNotNull("Health status should not be null", healthStatus)
        }
    }
    
    @Test
    fun testErrorRecovery() = runBlocking {
        // Initialize system normally
        val initResult = integrationManager.initialize()
        assertTrue("Initial initialization should succeed", initResult is IntegrationResult.Success)
        
        // Simulate error condition by cleanup and reinit
        integrationManager.cleanup()
        assertFalse("System should not be initialized after cleanup", 
            integrationManager.isFullyInitialized.value)
        
        // Test emergency restart
        val restartResult = integrationManager.emergencyRestart()
        assertTrue("Emergency restart should succeed", restartResult is IntegrationResult.Success)
        assertTrue("System should be fully initialized after restart", 
            integrationManager.isFullyInitialized.value)
        
        // Verify system still works after restart
        val healthStatus = integrationManager.getSystemHealth()
        assertNotNull("Health status should be available after restart", healthStatus)
    }
    
    @Test
    fun testMemoryEfficiency() = runBlocking {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Initialize system
        val initResult = integrationManager.initialize()
        assertTrue("Initialization should succeed", initResult is IntegrationResult.Success)
        
        // Perform memory-intensive operations
        repeat(50) { index ->
            val testUrl = "https://www.pornhub.com/test$index"
            
            integrationManager.getContentFilter()?.analyzeContent(testUrl, "Test Video $index")
            integrationManager.getNetworkManager()?.getSiteConfiguration("pornhub.com")
            integrationManager.getSystemHealth()
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(500)
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Memory increase should be reasonable (less than 50MB for all operations)
        assertTrue("Memory usage should be reasonable: ${memoryIncrease / 1024 / 1024}MB", 
            memoryIncrease < 50 * 1024 * 1024)
        
        // Performance manager should report memory metrics
        val performanceManager = integrationManager.getPerformanceManager()
        val metrics = performanceManager?.performanceMetrics?.value
        assertNotNull("Performance metrics should be available", metrics)
        assertTrue("Memory usage should be tracked", metrics!!.memoryUsageMB > 0)
    }
    
    @Test
    fun testFeatureDiscovery() = runBlocking {
        // Initialize system
        val initResult = integrationManager.initialize()
        assertTrue("Initialization should succeed", initResult is IntegrationResult.Success)
        
        // Test that all expected features are discoverable
        val features = mapOf(
            "PermissionsManager" to integrationManager.getPermissionsManager(),
            "SecurityManager" to integrationManager.getSecurityManager(),
            "PerformanceManager" to integrationManager.getPerformanceManager(),
            "NetworkManager" to integrationManager.getNetworkManager(),
            "StreamProcessor" to integrationManager.getStreamProcessor(),
            "ContentFilter" to integrationManager.getContentFilter(),
            "AdultSiteManager" to integrationManager.getAdultSiteManager(),
            "MediaScanner" to integrationManager.getMediaScanner()
        )
        
        features.forEach { (featureName, feature) ->
            assertNotNull("$featureName should be available", feature)
        }
        
        // Test feature readiness checks
        assertTrue("Security should be ready", integrationManager.isSecurityReady())
        assertTrue("Network should be ready", integrationManager.isNetworkReady())
        assertTrue("Media should be ready", integrationManager.isMediaReady())
        assertTrue("Optimization should be ready", integrationManager.isOptimizationReady())
    }
    
    @Test
    fun testInitializationPhases() = runBlocking {
        // Monitor initialization phases
        val phases = mutableListOf<FeatureIntegrationManager.Companion.InitializationPhase>()
        val progressValues = mutableListOf<Float>()
        
        val phaseJob = kotlinx.coroutines.launch {
            integrationManager.initializationPhase.collect { phase ->
                phases.add(phase)
            }
        }
        
        val progressJob = kotlinx.coroutines.launch {
            integrationManager.initializationProgress.collect { progress ->
                progressValues.add(progress)
            }
        }
        
        // Initialize system
        val initResult = integrationManager.initialize()
        assertTrue("Initialization should succeed", initResult is IntegrationResult.Success)
        
        // Give time for state collection
        kotlinx.coroutines.delay(100)
        
        phaseJob.cancel()
        progressJob.cancel()
        
        // Verify phases were tracked
        assertTrue("Should have recorded initialization phases", phases.isNotEmpty())
        assertTrue("Should have recorded progress values", progressValues.isNotEmpty())
        
        // Should end with COMPLETED phase
        assertEquals("Should end with COMPLETED phase", 
            FeatureIntegrationManager.Companion.InitializationPhase.COMPLETED, phases.last())
        
        // Progress should reach 1.0
        assertTrue("Progress should reach 1.0", progressValues.any { it >= 1.0f })
    }
}