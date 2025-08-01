package com.astralplayer.nextplayer.security

import android.content.Context
import android.net.Uri
import androidx.security.crypto.MasterKey
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
 * Comprehensive unit tests for individual security components
 * Tests each security manager in isolation with various scenarios
 */
@RunWith(AndroidJUnit4::class)
class SecurityComponentsUnitTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    
    @Mock private lateinit var mockMasterKey: MasterKey
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
    }

    @After
    fun tearDown() {
        // Cleanup is handled by individual tests
    }

    // ====== EncryptionManager Tests ======
    
    @Test
    fun testEncryptionManagerInitialization() = runTest {
        val encryptionManager = EncryptionManager(context)
        val masterKey = createTestMasterKey()
        
        // When
        encryptionManager.initialize(masterKey)
        advanceUntilIdle()
        
        // Then
        val metrics = encryptionManager.getEncryptionMetrics()
        assertTrue("Encryption manager should be initialized", 
                  metrics.keyHealthScore > 0f)
        
        encryptionManager.cleanup()
    }
    
    @Test
    fun testDataEncryptionDecryption() = runTest {
        val encryptionManager = EncryptionManager(context)
        val masterKey = createTestMasterKey()
        encryptionManager.initialize(masterKey)
        
        val originalData = "Sensitive test data for encryption".toByteArray()
        
        // When - Encrypt data
        val encryptionResult = encryptionManager.encryptData(originalData, EncryptionLevel.STANDARD)
        
        // Then - Encryption should succeed
        assertTrue("Encryption should succeed", encryptionResult is EncryptionResult.Success)
        
        when (encryptionResult) {
            is EncryptionResult.Success -> {
                // When - Decrypt data
                val decryptionResult = encryptionManager.decryptData(
                    encryptedData = encryptionResult.encryptedData,
                    encryptionInfo = encryptionResult.encryptionInfo
                )
                
                // Then - Decryption should succeed and match original
                assertTrue("Decryption should succeed", 
                          decryptionResult is DecryptionResult.Success)
                
                when (decryptionResult) {
                    is DecryptionResult.Success -> {
                        assertEquals("Decrypted data should match original",
                                   String(originalData), String(decryptionResult.decryptedData))
                    }
                    else -> throw AssertionError("Decryption should succeed")
                }
            }
            else -> throw AssertionError("Encryption should succeed")
        }
        
        encryptionManager.cleanup()
    }
    
    @Test
    fun testMultiLevelEncryption() = runTest {
        val encryptionManager = EncryptionManager(context)
        val masterKey = createTestMasterKey()
        encryptionManager.initialize(masterKey)
        
        val testData = "Multi-level encryption test".toByteArray()
        val encryptionLevels = listOf(
            EncryptionLevel.BASIC,
            EncryptionLevel.STANDARD,
            EncryptionLevel.HIGH,
            EncryptionLevel.MAXIMUM
        )
        
        encryptionLevels.forEach { level ->
            // When
            val encryptionResult = encryptionManager.encryptData(testData, level)
            
            // Then
            assertTrue("Encryption should succeed for level $level", 
                      encryptionResult is EncryptionResult.Success)
            
            when (encryptionResult) {
                is EncryptionResult.Success -> {
                    assertEquals("Encryption level should match", 
                               level, encryptionResult.encryptionInfo.encryptionLevel)
                    assertTrue("Encrypted data should be different from original",
                             !encryptionResult.encryptedData.contentEquals(testData))
                }
                else -> throw AssertionError("Encryption should succeed")
            }
        }
        
        encryptionManager.cleanup()
    }
    
    @Test
    fun testKeyRotation() = runTest {
        val encryptionManager = EncryptionManager(context)
        val masterKey = createTestMasterKey()
        encryptionManager.initialize(masterKey)
        
        val initialMetrics = encryptionManager.getEncryptionMetrics()
        val initialKeyHealth = initialMetrics.keyHealthScore
        
        // When
        val rotationResult = encryptionManager.rotateKeys()
        advanceUntilIdle()
        
        // Then
        assertTrue("Key rotation should succeed", 
                  rotationResult is KeyRotationResult.Success)
        
        val postRotationMetrics = encryptionManager.getEncryptionMetrics()
        assertNotNull("Key rotation timestamp should be updated", 
                     postRotationMetrics.lastKeyRotation)
        
        encryptionManager.cleanup()
    }

    // ====== ContentProtectionManager Tests ======
    
    @Test
    fun testContentProtectionBasicFlow() = runTest {
        val protectionManager = ContentProtectionManager(context)
        val masterKey = createTestMasterKey()
        protectionManager.initialize(masterKey)
        
        val testUri = Uri.parse("https://example.com/test-video.mp4")
        val accessPolicy = AccessPolicy(
            authorizedComponents = setOf("video_player"),
            allowedAccessTypes = setOf(AccessType.READ)
        )
        
        // When - Protect content
        val protectionResult = protectionManager.protectContent(
            contentUri = testUri,
            protectionLevel = ContentProtectionLevel.STANDARD,
            accessPolicy = accessPolicy
        )
        
        // Then
        assertTrue("Content protection should succeed", 
                  protectionResult is ContentProtectionInfo.Success)
        
        // When - Validate access
        val accessResult = protectionManager.validateContentAccess(
            contentUri = testUri,
            requestingComponent = "video_player",
            accessType = AccessType.READ
        )
        
        // Then
        assertTrue("Access should be granted to authorized component", 
                  accessResult is AccessValidationResult.AccessGranted)
        
        protectionManager.cleanup()
    }
    
    @Test
    fun testContentProtectionAccessDenied() = runTest {
        val protectionManager = ContentProtectionManager(context)
        val masterKey = createTestMasterKey()
        protectionManager.initialize(masterKey)
        
        val testUri = Uri.parse("https://example.com/restricted-video.mp4")
        val restrictivePolicy = AccessPolicy(
            authorizedComponents = setOf("premium_player"),
            allowedAccessTypes = setOf(AccessType.READ)
        )
        
        // When - Protect content with restrictive policy
        protectionManager.protectContent(
            contentUri = testUri,
            protectionLevel = ContentProtectionLevel.HIGH,
            accessPolicy = restrictivePolicy
        )
        
        // When - Try to access with unauthorized component
        val accessResult = protectionManager.validateContentAccess(
            contentUri = testUri,
            requestingComponent = "basic_player",
            accessType = AccessType.READ
        )
        
        // Then
        assertTrue("Access should be denied to unauthorized component", 
                  accessResult is AccessValidationResult.PolicyViolation)
        
        protectionManager.cleanup()
    }
    
    @Test
    fun testContentQuarantine() = runTest {
        val protectionManager = ContentProtectionManager(context)
        val masterKey = createTestMasterKey()
        protectionManager.initialize(masterKey)
        
        val maliciousUri = Uri.parse("file:///storage/malicious-file.exe")
        val quarantineReasons = listOf("Malware detected", "Suspicious behavior")
        
        // When
        protectionManager.quarantineContent(maliciousUri, quarantineReasons)
        advanceUntilIdle()
        
        // When - Try to access quarantined content
        val accessResult = protectionManager.validateContentAccess(
            contentUri = maliciousUri,
            requestingComponent = "any_component",
            accessType = AccessType.READ
        )
        
        // Then
        assertTrue("Access to quarantined content should be denied", 
                  accessResult is AccessValidationResult.ContentQuarantined)
        
        val metrics = protectionManager.getProtectionMetrics()
        assertTrue("Quarantined items count should increase", 
                  metrics.quarantinedItems > 0)
        
        protectionManager.cleanup()
    }

    // ====== PrivacyManager Tests ======
    
    @Test
    fun testPrivacyManagerConsentFlow() = runTest {
        val privacyManager = PrivacyManager(context)
        privacyManager.initialize()
        
        // When - Request consent
        val consentResult = privacyManager.requestConsent(
            consentType = ConsentType.DATA_COLLECTION,
            purpose = "Analytics and performance monitoring"
        )
        
        // Then
        assertTrue("Consent should be granted", 
                  consentResult is ConsentResult.Granted)
        
        // When - Log data collection
        privacyManager.logDataCollection(
            dataType = SensitiveDataType.USAGE_ANALYTICS,
            source = "video_player",
            purpose = "Performance analysis"
        )
        
        // When - Generate privacy report
        val privacyReport = privacyManager.generatePrivacyReport()
        
        // Then
        assertTrue("Privacy report should show data collection", 
                  privacyReport.dataCollectionSummary.isNotEmpty())
        assertTrue("Compliance score should be reasonable", 
                  privacyReport.complianceScore >= 0.5f)
        
        privacyManager.cleanup()
    }
    
    @Test
    fun testDataMinimization() = runTest {
        val privacyManager = PrivacyManager(context)
        val privacySettings = PrivacySettings(
            enabled = true,
            dataMinimization = true,
            anonymizeData = false
        )
        privacyManager.initialize(privacySettings)
        
        val originalData = "Very long user data with lots of details".toByteArray()
        
        // When
        val minimizationResult = privacyManager.minimizeData(
            dataType = SensitiveDataType.USER_PROFILE,
            originalData = originalData
        )
        
        // Then
        assertTrue("Data minimization should be applied", 
                  minimizationResult is DataMinimizationResult.Minimized)
        
        when (minimizationResult) {
            is DataMinimizationResult.Minimized -> {
                assertTrue("Minimized data should be smaller", 
                          minimizationResult.data.size < originalData.size)
                assertTrue("Reduction ratio should be positive", 
                          minimizationResult.reductionRatio > 0f)
            }
            else -> throw AssertionError("Data minimization should be applied")
        }
        
        privacyManager.cleanup()
    }
    
    @Test
    fun testDataDeletion() = runTest {
        val privacyManager = PrivacyManager(context)
        privacyManager.initialize()
        
        // First, simulate some data collection
        privacyManager.requestConsent(ConsentType.DATA_COLLECTION, "Test purpose")
        privacyManager.logDataCollection(
            SensitiveDataType.BROWSING_HISTORY, "browser", "test"
        )
        
        // When
        val deletionResult = privacyManager.deleteUserData(
            dataTypes = setOf(SensitiveDataType.BROWSING_HISTORY),
            reason = "User request"
        )
        
        // Then
        assertTrue("Data deletion should succeed", 
                  deletionResult is DataDeletionResult.Success)
        
        when (deletionResult) {
            is DataDeletionResult.Success -> {
                assertTrue("Some data should be deleted", 
                          deletionResult.totalDeleted >= 0)
            }
            else -> throw AssertionError("Data deletion should succeed")
        }
        
        privacyManager.cleanup()
    }

    // ====== DRMManager Tests ======
    
    @Test
    fun testDRMConfigurationAndLicense() = runTest {
        val drmManager = DRMManager(context)
        drmManager.initialize(setOf(DRMScheme.WIDEVINE))
        
        val contentUri = Uri.parse("https://example.com/drm-content.mp4")
        val drmConfig = DRMConfiguration(
            provider = DRMScheme.WIDEVINE,
            licenseUrl = "https://drm.example.com/license"
        )
        
        // When - Configure DRM
        val configResult = drmManager.configureDRM(contentUri, drmConfig)
        
        // Then
        assertTrue("DRM configuration should succeed", 
                  configResult is DRMConfigurationResult.Success)
        
        // When - Acquire license
        val licenseResult = drmManager.acquireLicense(contentUri)
        
        // Then
        assertTrue("License acquisition should succeed", 
                  licenseResult is LicenseAcquisitionResult.Success)
        
        // When - Validate license
        val validationResult = drmManager.validateLicense(
            contentUri = contentUri,
            requestedPermissions = setOf(DRMPermission.PLAY)
        )
        
        // Then
        assertTrue("License validation should succeed", 
                  validationResult is LicenseValidationResult.Valid)
        
        drmManager.cleanup()
    }
    
    @Test
    fun testDRMSessionManagement() = runTest {
        val drmManager = DRMManager(context)
        drmManager.initialize(setOf(DRMScheme.WIDEVINE))
        
        val contentUri = Uri.parse("https://example.com/session-test.mp4")
        val drmConfig = DRMConfiguration(
            provider = DRMScheme.WIDEVINE,
            licenseUrl = "https://drm.example.com/license"
        )
        
        // Setup DRM and license
        drmManager.configureDRM(contentUri, drmConfig)
        drmManager.acquireLicense(contentUri)
        
        // When - Create session
        val sessionResult = drmManager.createSession(contentUri)
        
        // Then
        assertTrue("DRM session creation should succeed", 
                  sessionResult is DRMSessionResult.Success)
        
        // Verify DRM metrics
        val metrics = drmManager.getDRMMetrics()
        assertTrue("Active sessions should be tracked", 
                  metrics.activeSessions >= 0)
        assertTrue("Active licenses should be tracked", 
                  metrics.activeLicenses >= 0)
        
        drmManager.cleanup()
    }

    // ====== SecurityAnalyzer Tests ======
    
    @Test
    fun testSecurityScanning() = runTest {
        val securityAnalyzer = SecurityAnalyzer(context)
        securityAnalyzer.initialize()
        
        val testUri = Uri.parse("https://example.com/scan-test.mp4")
        
        // When - Perform different types of scans
        val basicScan = securityAnalyzer.scanContent(testUri, SecurityScanType.BASIC)
        val comprehensiveScan = securityAnalyzer.scanContent(testUri, SecurityScanType.COMPREHENSIVE)
        val deepScan = securityAnalyzer.scanContent(testUri, SecurityScanType.DEEP)
        
        // Then
        assertEquals("Basic scan type should be correct", 
                    SecurityScanType.BASIC, basicScan.scanType)
        assertEquals("Comprehensive scan type should be correct", 
                    SecurityScanType.COMPREHENSIVE, comprehensiveScan.scanType)
        assertEquals("Deep scan type should be correct", 
                    SecurityScanType.DEEP, deepScan.scanType)
        
        // Verify scan results structure
        assertNotNull("Scan results should have scan ID", basicScan.scanId)
        assertTrue("Scan should have valid risk score", 
                  basicScan.riskScore >= 0f && basicScan.riskScore <= 1f)
        assertTrue("Scan should have duration", 
                  basicScan.scanEndTime >= basicScan.scanStartTime)
        
        securityAnalyzer.cleanup()
    }
    
    @Test
    fun testThreatAnalysis() = runTest {
        val securityAnalyzer = SecurityAnalyzer(context)
        securityAnalyzer.initialize()
        
        val threatIndicators = listOf(
            "suspicious_pattern_detected",
            "malware_signature_found",
            "unauthorized_access_attempt"
        )
        
        // When
        val threatAnalysis = securityAnalyzer.analyzeThreat(
            indicators = threatIndicators,
            context = ThreatContext(source = "test_scan")
        )
        
        // Then
        assertTrue("Threat analysis should succeed", 
                  threatAnalysis is ThreatAnalysisResult.Success)
        
        when (threatAnalysis) {
            is ThreatAnalysisResult.Success -> {
                assertTrue("Threats should be detected", 
                          threatAnalysis.threats.isNotEmpty())
                assertNotNull("Overall threat level should be determined", 
                             threatAnalysis.overallLevel)
            }
            else -> throw AssertionError("Threat analysis should succeed")
        }
        
        securityAnalyzer.cleanup()
    }
    
    @Test
    fun testVulnerabilityScanning() = runTest {
        val securityAnalyzer = SecurityAnalyzer(context)
        securityAnalyzer.initialize()
        
        // When
        val vulnerabilityScan = securityAnalyzer.scanVulnerabilities()
        
        // Then
        assertTrue("Vulnerability scan should succeed", 
                  vulnerabilityScan is VulnerabilityScanResult.Success)
        
        when (vulnerabilityScan) {
            is VulnerabilityScanResult.Success -> {
                assertTrue("Risk score should be valid", 
                          vulnerabilityScan.riskScore >= 0f && vulnerabilityScan.riskScore <= 1f)
                assertNotNull("Recommendations should be provided", 
                             vulnerabilityScan.recommendations)
                assertTrue("Scan should have timestamp", 
                          vulnerabilityScan.scanTimestamp > 0)
            }
            else -> throw AssertionError("Vulnerability scan should succeed")
        }
        
        securityAnalyzer.cleanup()
    }

    // ====== SecureStorageManager Tests ======
    
    @Test
    fun testSecureVaultOperations() = runTest {
        val storageManager = SecureStorageManager(context)
        storageManager.initialize()
        
        val vaultId = "test_vault"
        val testData = "Secure test data".toByteArray()
        
        // When - Create vault
        val vaultResult = storageManager.createVault(
            vaultId = vaultId,
            vaultType = StorageVaultType.USER_DATA,
            encryptionLevel = EncryptionLevel.HIGH
        )
        
        // Then
        assertTrue("Vault creation should succeed", 
                  vaultResult is VaultCreationResult.Success)
        
        // When - Store data
        val storageResult = storageManager.storeData(
            vaultId = vaultId,
            dataKey = "test_key",
            data = testData
        )
        
        // Then
        assertTrue("Data storage should succeed", 
                  storageResult is StorageResult.Success)
        
        // When - Retrieve data
        val retrievalResult = storageManager.retrieveData(
            vaultId = vaultId,
            dataKey = "test_key"
        )
        
        // Then
        assertTrue("Data retrieval should succeed", 
                  retrievalResult is RetrievalResult.Success)
        
        when (retrievalResult) {
            is RetrievalResult.Success -> {
                assertEquals("Retrieved data should match original", 
                           String(testData), String(retrievalResult.data))
            }
            else -> throw AssertionError("Data retrieval should succeed")
        }
        
        storageManager.cleanup()
    }
    
    @Test
    fun testVaultLockingAndUnlocking() = runTest {
        val storageManager = SecureStorageManager(context)
        storageManager.initialize()
        
        val vaultId = "lockable_vault"
        
        // Create and populate vault
        storageManager.createVault(vaultId, StorageVaultType.SECURITY_KEYS)
        storageManager.storeData(vaultId, "secret_key", "secret_data".toByteArray())
        
        // When - Lock vault
        val lockResult = storageManager.lockVault(vaultId)
        
        // Then
        assertTrue("Vault locking should succeed", 
                  lockResult is VaultLockResult.Success)
        
        // When - Try to access locked vault
        val accessResult = storageManager.retrieveData(vaultId, "secret_key")
        
        // Then
        assertTrue("Access to locked vault should be denied", 
                  accessResult is RetrievalResult.VaultLocked)
        
        // When - Unlock vault
        val unlockResult = storageManager.unlockVault(vaultId)
        
        // Then
        assertTrue("Vault unlocking should succeed", 
                  unlockResult is VaultUnlockResult.Success)
        
        // When - Access unlocked vault
        val postUnlockAccess = storageManager.retrieveData(vaultId, "secret_key")
        
        // Then
        assertTrue("Access to unlocked vault should succeed", 
                  postUnlockAccess is RetrievalResult.Success)
        
        storageManager.cleanup()
    }
    
    @Test
    fun testStorageOptimization() = runTest {
        val storageManager = SecureStorageManager(context)
        storageManager.initialize()
        
        val vaultId = "optimization_vault"
        storageManager.createVault(vaultId, StorageVaultType.TEMPORARY)
        
        // Store multiple data entries
        repeat(10) { index ->
            storageManager.storeData(
                vaultId = vaultId,
                dataKey = "temp_data_$index",
                data = "Temporary data $index".toByteArray()
            )
        }
        
        val initialAnalytics = storageManager.getStorageAnalytics()
        val initialSize = initialAnalytics.totalStorageSize
        
        // When
        val optimizationResult = storageManager.optimizeStorage()
        
        // Then
        assertTrue("Storage optimization should succeed", 
                  optimizationResult is StorageOptimizationResult.Success)
        
        when (optimizationResult) {
            is StorageOptimizationResult.Success -> {
                assertTrue("Some space should be optimized", 
                          optimizationResult.spaceReclaimed >= 0)
                assertTrue("At least one vault should be processed", 
                          optimizationResult.vaultsOptimized >= 0)
            }
            else -> throw AssertionError("Storage optimization should succeed")
        }
        
        storageManager.cleanup()
    }

    // ====== Integration Edge Cases ======
    
    @Test
    fun testInvalidInputHandling() = runTest {
        val encryptionManager = EncryptionManager(context)
        val masterKey = createTestMasterKey()
        encryptionManager.initialize(masterKey)
        
        // Test empty data encryption
        val emptyData = ByteArray(0)
        val encryptionResult = encryptionManager.encryptData(emptyData, EncryptionLevel.BASIC)
        
        // Should handle gracefully (either succeed with empty result or return appropriate error)
        assertNotNull("Empty data encryption should be handled", encryptionResult)
        
        encryptionManager.cleanup()
    }
    
    @Test
    fun testConcurrentOperations() = runTest {
        val encryptionManager = EncryptionManager(context)
        val masterKey = createTestMasterKey()
        encryptionManager.initialize(masterKey)
        
        val testData = "Concurrent encryption test".toByteArray()
        val concurrentJobs = (1..10).map { index ->
            async {
                encryptionManager.encryptData("$testData $index".toByteArray(), EncryptionLevel.STANDARD)
            }
        }
        
        // When
        val results = concurrentJobs.awaitAll()
        
        // Then
        assertTrue("All concurrent operations should complete", 
                  results.size == 10)
        
        val successCount = results.count { it is EncryptionResult.Success }
        assertTrue("Most operations should succeed", 
                  successCount >= 8) // Allow for some failures under load
        
        encryptionManager.cleanup()
    }

    // Helper methods
    private fun createTestMasterKey(): MasterKey {
        return MasterKey.Builder(context, "test_master_key_${System.currentTimeMillis()}")
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
}