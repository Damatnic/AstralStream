package com.astralplayer.nextplayer.production

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Comprehensive tests for production readiness validation system
 * Tests validation modules, benchmarks, stress tests, security audits, and deployment readiness
 */
@RunWith(AndroidJUnit4::class)
class ProductionReadinessValidatorTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var productionValidator: ProductionReadinessValidator
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        productionValidator = ProductionReadinessValidator(context)
    }

    @After
    fun tearDown() {
        runTest {
            productionValidator.cleanup()
        }
    }

    @Test
    fun testProductionValidatorInitialization() = runTest {
        // When
        productionValidator.initialize()
        advanceUntilIdle()
        
        // Then
        val state = productionValidator.validationState.value
        assertTrue("Validator should be initialized", state.isInitialized)
        assertTrue("Initialization time should be set", state.initializationTime > 0)
        assertFalse("Should not be validating initially", state.isValidating)
        assertEquals("Initial readiness score should be 0", 0f, state.overallReadinessScore)
    }

    @Test
    fun testComprehensiveProductionReadinessValidation() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        val validationConfig = ValidationConfig(
            performance = PerformanceValidationConfig(enableBenchmarks = true, timeoutSeconds = 60),
            security = SecurityValidationConfig(enablePenetrationTesting = true, timeoutSeconds = 120),
            stability = StabilityValidationConfig(enableCrashTesting = true, timeoutSeconds = 180),
            compatibility = CompatibilityValidationConfig(enableDeviceTesting = true, timeoutSeconds = 90),
            deployment = DeploymentValidationConfig(enableBuildValidation = true, timeoutSeconds = 30),
            quality = QualityValidationConfig(enableUXValidation = true, timeoutSeconds = 60)
        )
        
        // When
        val report = productionValidator.validateProductionReadiness(validationConfig)
        
        // Then
        assertNotNull("Production readiness report should not be null", report)
        assertTrue("Overall readiness score should be valid", 
                  report.overallReadinessScore >= 0f && report.overallReadinessScore <= 1f)
        assertEquals("Should have results for all modules", 6, report.moduleResults.size)
        assertTrue("Validation duration should be positive", report.validationDuration > 0)
        assertNotNull("Should have device info", report.deviceInfo)
        assertNotNull("Should have build info", report.buildInfo)
        assertTrue("Generated timestamp should be recent", 
                  System.currentTimeMillis() - report.generatedAt < 10000)
        
        // Verify all modules are covered
        val moduleNames = report.moduleResults.map { it.moduleName }.toSet()
        val expectedModules = setOf("Performance", "Security", "Stability", "Compatibility", "Deployment", "Quality")
        assertEquals("All validation modules should be present", expectedModules, moduleNames)
    }

    @Test
    fun testPerformanceBenchmarks() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        val benchmarkConfig = BenchmarkConfig(
            videoPlayback = VideoPlaybackBenchmarkConfig(duration = 1000L),
            audioDecoding = AudioDecodingBenchmarkConfig(duration = 500L),
            networkStreaming = NetworkStreamingBenchmarkConfig(duration = 2000L),
            uiResponsiveness = UIResponsivenessBenchmarkConfig(duration = 300L),
            memoryEfficiency = MemoryEfficiencyBenchmarkConfig(duration = 1500L),
            batteryConsumption = BatteryConsumptionBenchmarkConfig(duration = 3000L)
        )
        
        // When
        val benchmarkReport = productionValidator.runPerformanceBenchmarks(benchmarkConfig)
        
        // Then
        assertNotNull("Benchmark report should not be null", benchmarkReport)
        assertEquals("Should have 6 benchmark results", 6, benchmarkReport.benchmarks.size)
        assertTrue("Overall performance score should be valid", 
                  benchmarkReport.overallPerformanceScore >= 0f && benchmarkReport.overallPerformanceScore <= 1f)
        assertNotNull("Should have device info", benchmarkReport.deviceInfo)
        assertTrue("Should have recommendations", benchmarkReport.recommendations.isNotEmpty())
        
        // Verify individual benchmarks
        val benchmarkNames = benchmarkReport.benchmarks.map { it.name }.toSet()
        val expectedBenchmarks = setOf(
            "Video Playback", "Audio Decoding", "Network Streaming", 
            "UI Responsiveness", "Memory Efficiency", "Battery Consumption"
        )
        assertEquals("All benchmarks should be present", expectedBenchmarks, benchmarkNames)
        
        // Verify benchmark results structure
        benchmarkReport.benchmarks.forEach { benchmark ->
            assertTrue("Benchmark score should be valid", 
                      benchmark.score >= 0f && benchmark.score <= 1f)
            assertTrue("Execution time should be positive", benchmark.executionTime > 0)
            assertTrue("Should have metrics", benchmark.metrics.isNotEmpty())
            assertNotNull("Should have pass/fail status", benchmark.passed)
        }
    }

    @Test
    fun testStressTests() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        val stressTestConfig = StressTestConfig(
            concurrentPlayback = ConcurrentPlaybackStressConfig(maxStreams = 3),
            memoryPressure = MemoryPressureStressConfig(pressureLevel = 0.8f),
            networkInstability = NetworkInstabilityStressConfig(instabilityLevel = 0.7f),
            longRunningSessions = LongRunningSessionStressConfig(sessionDuration = 3600000L), // 1 hour
            rapidInteractions = RapidInteractionStressConfig(interactionsPerSecond = 5)
        )
        
        // When
        val stressTestReport = productionValidator.runStressTests(stressTestConfig)
        
        // Then
        assertNotNull("Stress test report should not be null", stressTestReport)
        assertEquals("Should have 5 stress test results", 5, stressTestReport.tests.size)
        assertTrue("Overall stability score should be valid", 
                  stressTestReport.overallStabilityScore >= 0f && stressTestReport.overallStabilityScore <= 1f)
        assertTrue("Should have recommendations", stressTestReport.recommendations.isNotEmpty())
        assertTrue("Execution time should be positive", stressTestReport.executionTime > 0)
        
        // Verify stress test names
        val testNames = stressTestReport.tests.map { it.name }.toSet()
        assertTrue("Should have concurrent playback test", 
                  testNames.any { it.contains("Concurrent Playback") })
        assertTrue("Should have memory pressure test", 
                  testNames.any { it.contains("Memory Pressure") })
        assertTrue("Should have network instability test", 
                  testNames.any { it.contains("Network Instability") })
        
        // Verify test results structure
        stressTestReport.tests.forEach { test ->
            assertTrue("Test score should be valid", 
                      test.score >= 0f && test.score <= 1f)
            assertNotNull("Should have pass/fail status", test.passed)
            assertTrue("Should have metrics", test.metrics.isNotEmpty())
        }
    }

    @Test
    fun testSecurityAudit() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        val securityConfig = SecurityAuditConfig(
            encryption = EncryptionAuditConfig(enablePenetrationTesting = true),
            accessControl = AccessControlAuditConfig(enablePrivilegeEscalation = true),
            dataProtection = DataProtectionAuditConfig(enableDataLeakTesting = true),
            networkSecurity = NetworkSecurityAuditConfig(enableMITMTesting = true),
            privacyCompliance = PrivacyComplianceAuditConfig(enableGDPRCheck = true)
        )
        
        // When
        val securityReport = productionValidator.runSecurityAudit(securityConfig)
        
        // Then
        assertNotNull("Security audit report should not be null", securityReport)
        assertEquals("Should have 5 audit results", 5, securityReport.auditResults.size)
        assertTrue("Overall security score should be valid", 
                  securityReport.overallSecurityScore >= 0f && securityReport.overallSecurityScore <= 1f)
        assertNotNull("Should have compliance status", securityReport.complianceStatus)
        assertTrue("Should have recommendations", securityReport.recommendations.isNotEmpty())
        assertTrue("Execution time should be positive", securityReport.executionTime > 0)
        
        // Verify audit areas
        val auditNames = securityReport.auditResults.map { it.name }.toSet()
        val expectedAudits = setOf(
            "Encryption Audit", "Access Control Audit", "Data Protection Audit",
            "Network Security Audit", "Privacy Compliance Audit"
        )
        assertEquals("All security audits should be present", expectedAudits, auditNames)
        
        // Verify audit results structure
        securityReport.auditResults.forEach { audit ->
            assertTrue("Audit score should be valid", 
                      audit.score >= 0f && audit.score <= 1f)
            assertNotNull("Should have pass/fail status", audit.passed)
            assertNotNull("Should have compliance status", audit.complianceStatus)
        }
    }

    @Test
    fun testCompatibilityTests() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        val compatibilityConfig = CompatibilityTestConfig(
            androidVersions = listOf(21, 23, 26, 28, 29, 30, 31),
            devices = listOf("phone", "tablet", "tv"),
            screenSizes = listOf("small", "medium", "large"),
            performanceTiers = listOf("low", "medium", "high"),
            networkConditions = listOf("wifi", "cellular", "offline")
        )
        
        // When
        val compatibilityReport = productionValidator.runCompatibilityTests(compatibilityConfig)
        
        // Then
        assertNotNull("Compatibility report should not be null", compatibilityReport)
        assertEquals("Should have 5 compatibility test results", 5, compatibilityReport.tests.size)
        assertTrue("Overall compatibility score should be valid", 
                  compatibilityReport.overallCompatibilityScore >= 0f && compatibilityReport.overallCompatibilityScore <= 1f)
        assertTrue("Should have supported devices", compatibilityReport.supportedDevices.isNotEmpty())
        assertTrue("Should have recommendations", compatibilityReport.recommendations.isNotEmpty())
        assertTrue("Execution time should be positive", compatibilityReport.executionTime > 0)
        
        // Verify compatibility test areas
        val testNames = compatibilityReport.tests.map { it.name }.toSet()
        assertTrue("Should have Android version compatibility test", 
                  testNames.any { it.contains("Android Version") })
        assertTrue("Should have device compatibility test", 
                  testNames.any { it.contains("Device Compatibility") })
        assertTrue("Should have screen size compatibility test", 
                  testNames.any { it.contains("Screen Size") })
        
        // Verify test results structure
        compatibilityReport.tests.forEach { test ->
            assertTrue("Test score should be valid", 
                      test.score >= 0f && test.score <= 1f)
            assertNotNull("Should have pass/fail status", test.passed)
        }
    }

    @Test
    fun testDeploymentChecklist() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        // When
        val checklist = productionValidator.generateDeploymentChecklist()
        
        // Then
        assertNotNull("Deployment checklist should not be null", checklist)
        assertTrue("Should have checklist items", checklist.items.isNotEmpty())
        assertTrue("Total items should match items list", checklist.totalItems == checklist.items.size)
        assertTrue("Completed items should be non-negative", checklist.completedItems >= 0)
        assertTrue("Completion percentage should be valid", 
                  checklist.completionPercentage >= 0f && checklist.completionPercentage <= 100f)
        assertTrue("Generated timestamp should be recent", 
                  System.currentTimeMillis() - checklist.generatedAt < 5000)
        
        // Verify checklist categories are covered
        val categories = checklist.items.map { it.category }.toSet()
        val expectedCategories = setOf(
            ChecklistCategory.CODE_QUALITY, ChecklistCategory.BUILD_PACKAGING,
            ChecklistCategory.CONFIGURATION, ChecklistCategory.DOCUMENTATION,
            ChecklistCategory.TESTING, ChecklistCategory.PERFORMANCE,
            ChecklistCategory.SECURITY, ChecklistCategory.RELEASE
        )
        assertEquals("All checklist categories should be present", expectedCategories, categories)
        
        // Verify checklist items structure
        checklist.items.forEach { item ->
            assertNotNull("Item description should not be null", item.description)
            assertNotNull("Item should have completion status", item.isCompleted)
            assertNotNull("Item should have category", item.category)
            assertNotNull("Item should have priority", item.priority)
        }
        
        // Verify completion percentage calculation
        val actualCompletedItems = checklist.items.count { it.isCompleted }
        assertEquals("Completed items count should match", actualCompletedItems, checklist.completedItems)
        
        val expectedPercentage = if (checklist.totalItems > 0) {
            (actualCompletedItems.toFloat() / checklist.totalItems) * 100f
        } else {
            0f
        }
        assertEquals("Completion percentage should be calculated correctly", 
                    expectedPercentage, checklist.completionPercentage, 0.1f)
    }

    @Test
    fun testQualityAssurance() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        val qaConfig = QualityAssuranceConfig(
            userExperience = UserExperienceValidationConfig(enableUsabilityTesting = true),
            accessibility = AccessibilityValidationConfig(enableScreenReaderTesting = true),
            contentQuality = ContentQualityValidationConfig(enableContentValidation = true),
            localization = LocalizationValidationConfig(enableL10nTesting = true),
            errorHandling = ErrorHandlingValidationConfig(enableErrorScenarios = true)
        )
        
        // When
        val qaReport = productionValidator.runQualityAssurance(qaConfig)
        
        // Then
        assertNotNull("QA report should not be null", qaReport)
        assertEquals("Should have 5 QA results", 5, qaReport.results.size)
        assertTrue("Overall quality score should be valid", 
                  qaReport.overallQualityScore >= 0f && qaReport.overallQualityScore <= 1f)
        assertTrue("Should have recommendations", qaReport.recommendations.isNotEmpty())
        assertTrue("Execution time should be positive", qaReport.executionTime > 0)
        
        // Verify QA areas
        val qaNames = qaReport.results.map { it.name }.toSet()
        val expectedQA = setOf(
            "User Experience Validation", "Accessibility Validation", 
            "Content Quality Validation", "Localization Validation", 
            "Error Handling Validation"
        )
        assertEquals("All QA validations should be present", expectedQA, qaNames)
        
        // Verify QA results structure
        qaReport.results.forEach { result ->
            assertTrue("QA score should be valid", 
                      result.score >= 0f && result.score <= 1f)
            assertNotNull("Should have pass/fail status", result.passed)
        }
    }

    @Test
    fun testProductionReadinessStatus() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        // Run a validation to populate status
        productionValidator.validateProductionReadiness()
        advanceUntilIdle()
        
        // When
        val status = productionValidator.getProductionReadinessStatus()
        
        // Then
        assertNotNull("Status should not be null", status)
        assertTrue("Should be initialized", status.isInitialized)
        assertFalse("Should not be validating after completion", status.isValidating)
        assertTrue("Last validation time should be set", status.lastValidationTime > 0)
        assertTrue("Overall readiness score should be valid", 
                  status.overallReadinessScore >= 0f && status.overallReadinessScore <= 1f)
        assertTrue("Next recommended validation should be in the future", 
                  status.nextRecommendedValidation > System.currentTimeMillis())
    }

    @Test
    fun testValidationEventEmission() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        val events = mutableListOf<ValidationEvent>()
        val job = launch {
            productionValidator.validationEvents.collect { event ->
                events.add(event)
            }
        }
        
        // When - Run validation
        productionValidator.validateProductionReadiness()
        advanceUntilIdle()
        
        job.cancel()
        
        // Then
        assertTrue("Should have emitted events", events.isNotEmpty())
        
        val hasSystemInitialized = events.any { it is ValidationEvent.SystemInitialized }
        val hasValidationStarted = events.any { it is ValidationEvent.ValidationStarted }
        val hasValidationCompleted = events.any { it is ValidationEvent.ValidationCompleted }
        
        assertTrue("Should have system initialized event", hasSystemInitialized)
        assertTrue("Should have validation started event", hasValidationStarted)
        assertTrue("Should have validation completed event", hasValidationCompleted)
    }

    @Test
    fun testValidationStateTracking() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        // Initial state
        var state = productionValidator.validationState.value
        assertTrue("Should be initialized", state.isInitialized)
        assertFalse("Should not be validating initially", state.isValidating)
        
        // Start validation
        val validationJob = launch {
            productionValidator.validateProductionReadiness()
        }
        
        // Check state during validation
        yield() // Allow validation to start
        state = productionValidator.validationState.value
        // Note: Validation might complete too quickly to catch intermediate state
        
        // Wait for completion
        validationJob.join()
        
        // Final state
        state = productionValidator.validationState.value
        assertFalse("Should not be validating after completion", state.isValidating)
        assertTrue("Last validation time should be set", state.lastValidationTime > 0)
        assertTrue("Overall readiness score should be set", state.overallReadinessScore > 0)
    }

    @Test
    fun testPerformanceBenchmarkIndividualTests() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        val quickBenchmarkConfig = BenchmarkConfig(
            videoPlayback = VideoPlaybackBenchmarkConfig(duration = 100L),
            audioDecoding = AudioDecodingBenchmarkConfig(duration = 100L),
            networkStreaming = NetworkStreamingBenchmarkConfig(duration = 100L),
            uiResponsiveness = UIResponsivenessBenchmarkConfig(duration = 100L),
            memoryEfficiency = MemoryEfficiencyBenchmarkConfig(duration = 100L),
            batteryConsumption = BatteryConsumptionBenchmarkConfig(duration = 100L)
        )
        
        // When
        val report = productionValidator.runPerformanceBenchmarks(quickBenchmarkConfig)
        
        // Then - Verify each benchmark individually
        val videoPlaybackBenchmark = report.benchmarks.find { it.name == "Video Playback" }
        assertNotNull("Video playback benchmark should exist", videoPlaybackBenchmark)
        assertTrue("Video playback should have frame rate metric", 
                  videoPlaybackBenchmark!!.metrics.containsKey("frame_rate"))
        
        val audioDecodingBenchmark = report.benchmarks.find { it.name == "Audio Decoding" }
        assertNotNull("Audio decoding benchmark should exist", audioDecodingBenchmark)
        assertTrue("Audio decoding should have latency metric", 
                  audioDecodingBenchmark!!.metrics.containsKey("latency"))
        
        val networkBenchmark = report.benchmarks.find { it.name == "Network Streaming" }
        assertNotNull("Network streaming benchmark should exist", networkBenchmark)
        assertTrue("Network should have bandwidth metric", 
                  networkBenchmark!!.metrics.containsKey("bandwidth_utilization"))
        
        val uiBenchmark = report.benchmarks.find { it.name == "UI Responsiveness" }
        assertNotNull("UI responsiveness benchmark should exist", uiBenchmark)
        assertTrue("UI should have latency metric", 
                  uiBenchmark!!.metrics.containsKey("input_latency"))
        
        val memoryBenchmark = report.benchmarks.find { it.name == "Memory Efficiency" }
        assertNotNull("Memory efficiency benchmark should exist", memoryBenchmark)
        assertTrue("Memory should have usage metric", 
                  memoryBenchmark!!.metrics.containsKey("memory_usage"))
        
        val batteryBenchmark = report.benchmarks.find { it.name == "Battery Consumption" }
        assertNotNull("Battery consumption benchmark should exist", batteryBenchmark)
        assertTrue("Battery should have consumption metric", 
                  batteryBenchmark!!.metrics.containsKey("power_consumption"))
    }

    @Test
    fun testValidationTimeout() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        val timeoutConfig = ValidationConfig(
            performance = PerformanceValidationConfig(timeoutSeconds = 1), // Very short timeout
            security = SecurityValidationConfig(timeoutSeconds = 1),
            stability = StabilityValidationConfig(timeoutSeconds = 1),
            compatibility = CompatibilityValidationConfig(timeoutSeconds = 1),
            deployment = DeploymentValidationConfig(timeoutSeconds = 1),
            quality = QualityValidationConfig(timeoutSeconds = 1)
        )
        
        // When - Run validation with very short timeouts
        val startTime = System.currentTimeMillis()
        val report = productionValidator.validateProductionReadiness(timeoutConfig)
        val endTime = System.currentTimeMillis()
        
        // Then
        assertNotNull("Report should be generated even with short timeouts", report)
        assertTrue("Validation should complete quickly", endTime - startTime < 30000) // 30 seconds max
        assertEquals("All modules should still report results", 6, report.moduleResults.size)
    }

    @Test
    fun testProductionReadinessThresholds() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        // When
        val report = productionValidator.validateProductionReadiness()
        
        // Then - Verify production readiness determination
        val highScoringModules = report.moduleResults.count { it.score >= 0.8f }
        val allModulesPassed = report.moduleResults.all { it.passed }
        
        // Production readiness should be based on both overall score and individual module performance
        if (report.overallReadinessScore >= 0.85f && allModulesPassed) {
            assertTrue("Should be production ready with high scores", report.isProductionReady)
        } else {
            assertFalse("Should not be production ready with low scores", report.isProductionReady)
        }
        
        // Verify score consistency
        val calculatedOverallScore = report.moduleResults.map { it.score }.average().toFloat()
        assertEquals("Overall score should match module average", 
                    calculatedOverallScore, report.overallReadinessScore, 0.01f)
    }

    @Test
    fun testConcurrentValidationRequests() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        // When - Run multiple validations concurrently
        val validationJobs = (1..3).map { index ->
            async {
                productionValidator.validateProductionReadiness(
                    ValidationConfig(
                        performance = PerformanceValidationConfig(timeoutSeconds = 30)
                    )
                )
            }
        }
        
        val reports = validationJobs.awaitAll()
        
        // Then
        assertEquals("All validation requests should complete", 3, reports.size)
        reports.forEach { report ->
            assertNotNull("Each report should not be null", report)
            assertTrue("Each report should have valid scores", 
                      report.overallReadinessScore >= 0f && report.overallReadinessScore <= 1f)
            assertEquals("Each report should have all modules", 6, report.moduleResults.size)
        }
    }

    @Test
    fun testValidationErrorHandling() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        // Test that validation handles potential errors gracefully
        // Since our implementation is simplified, we'll test the general error handling structure
        
        try {
            val report = productionValidator.validateProductionReadiness()
            
            // If no exception is thrown, verify the report is well-formed
            assertNotNull("Report should be generated", report)
            assertTrue("Report should have reasonable scores", 
                      report.overallReadinessScore >= 0f)
            
        } catch (e: Exception) {
            // If an exception occurs, verify it's handled properly
            val state = productionValidator.validationState.value
            assertNotNull("Error should be captured in state", state.lastValidationError)
            assertFalse("Should not be validating after error", state.isValidating)
        }
    }

    @Test
    fun testDeviceAndBuildInfoInclusion() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        // When
        val report = productionValidator.validateProductionReadiness()
        
        // Then - Verify device info
        assertNotNull("Device info should be included", report.deviceInfo)
        assertNotNull("Device manufacturer should be set", report.deviceInfo.manufacturer)
        assertNotNull("Device model should be set", report.deviceInfo.model)
        assertNotNull("Android version should be set", report.deviceInfo.androidVersion)
        assertTrue("API level should be positive", report.deviceInfo.apiLevel > 0)
        assertNotNull("Architecture should be set", report.deviceInfo.architecture)
        
        // Verify build info
        assertNotNull("Build info should be included", report.buildInfo)
        assertNotNull("Build version should be set", report.buildInfo.version)
        assertNotNull("Build number should be set", report.buildInfo.buildNumber)
        assertNotNull("Build type should be set", report.buildInfo.buildType)
        assertTrue("Build time should be positive", report.buildInfo.buildTime > 0)
    }

    @Test
    fun testRecommendationsGeneration() = runTest {
        productionValidator.initialize()
        advanceUntilIdle()
        
        // When
        val report = productionValidator.validateProductionReadiness()
        
        // Then
        assertTrue("Should have recommendations", report.recommendations.isNotEmpty())
        
        report.recommendations.forEach { recommendation ->
            assertNotNull("Recommendation should not be null", recommendation)
            assertTrue("Recommendation should not be empty", recommendation.isNotEmpty())
        }
        
        // Verify module-specific recommendations
        report.moduleResults.forEach { moduleResult ->
            assertTrue("Each module should have recommendations", 
                      moduleResult.recommendations.isNotEmpty())
        }
        
        // Verify recommendations are actionable (contain verbs or action words)
        val actionWords = listOf("optimize", "improve", "enhance", "update", "implement", "review", "configure")
        val hasActionableRecommendations = report.recommendations.any { recommendation ->
            actionWords.any { actionWord -> 
                recommendation.lowercase().contains(actionWord) 
            }
        }
        assertTrue("Should have actionable recommendations", hasActionableRecommendations)
    }
}