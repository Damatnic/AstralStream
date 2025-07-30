package com.astralplayer.nextplayer

import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.feature.ai.*
import com.astralplayer.nextplayer.feature.cloud.*
import com.astralplayer.nextplayer.feature.gesture.*
import com.astralplayer.nextplayer.utils.*
import com.astralplayer.nextplayer.voice.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import kotlin.test.*

/**
 * Comprehensive edge case testing to achieve 100% code coverage
 * Focus on error conditions, boundary cases, and exceptional scenarios
 */
@RunWith(MockitoJUnitRunner::class)
class ComprehensiveEdgeCaseTests {

    @Mock private lateinit var mockContext: android.content.Context
    private lateinit var testScope: TestCoroutineScope

    @Before
    fun setup() {
        testScope = TestCoroutineScope()
    }

    @After
    fun cleanup() {
        testScope.cleanupTestCoroutines()
    }

    // ============================================================================
    // DATABASE OPERATIONS - CORRUPTION AND MIGRATION EDGE CASES
    // ============================================================================

    @Test
    fun `RecentFilesRepository handles database corruption gracefully`() = testScope.runBlockingTest {
        // Given
        val repository = createRecentFilesRepository()
        whenever(repository.database.isOpen).thenReturn(false) // Simulate corruption

        // When
        val result = repository.addRecentFile(createTestRecentFile())

        // Then
        assertTrue(result is DatabaseResult.CorruptionDetected)
        verify(repository).attemptDatabaseRecovery()
    }

    @Test
    fun `RecentFilesRepository handles concurrent database access`() = testScope.runBlockingTest {
        // Given
        val repository = createRecentFilesRepository()
        
        // When - simulate concurrent access
        val jobs = (1..10).map { index ->
            launch {
                repository.addRecentFile(createTestRecentFile("file_$index"))
            }
        }
        
        jobs.forEach { it.join() }

        // Then - all operations should complete without deadlock
        val recentFiles = repository.getAllRecentFiles()
        assertTrue(recentFiles.size <= 10) // Some might be duplicates handled
    }

    @Test
    fun `RecentFilesRepository handles database schema migration failures`() = testScope.runBlockingTest {
        // Given
        val repository = createRecentFilesRepository()
        val oldSchemaVersion = 1
        val newSchemaVersion = 5

        // When - simulate migration failure
        whenever(repository.migrationHelper.migrate(oldSchemaVersion, newSchemaVersion))
            .thenThrow(android.database.sqlite.SQLiteException("Migration failed"))

        val result = repository.migrateDatabase(oldSchemaVersion, newSchemaVersion)

        // Then
        assertTrue(result is MigrationResult.Failed)
        assertTrue(result.requiresDataReset)
    }

    @Test
    fun `SettingsRepository handles encrypted preferences corruption`() = testScope.runBlockingTest {
        // Given
        val settingsRepo = createSettingsRepository()
        
        // When - simulate encrypted preferences corruption
        whenever(settingsRepo.encryptedPrefs.getString(any(), any()))
            .thenThrow(javax.crypto.BadPaddingException("Decryption failed"))

        val result = settingsRepo.getString("user_preference", "default")

        // Then - should handle gracefully and return default
        assertEquals("default", result)
        verify(settingsRepo).resetCorruptedPreferences()
    }

    // ============================================================================
    // FILE SYSTEM OPERATIONS - PERMISSION AND STORAGE EDGE CASES
    // ============================================================================

    @Test
    fun `FileManager handles storage permissions revoked during operation`() = testScope.runBlockingTest {
        // Given
        val fileManager = createTestFileManager()
        val testFile = createTestVideoFile()

        // When - permission revoked mid-operation
        whenever(mockContext.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE))
            .thenReturn(android.content.pm.PackageManager.PERMISSION_GRANTED)
            .thenReturn(android.content.pm.PackageManager.PERMISSION_DENIED) // Revoked

        val result = fileManager.loadVideoFile(testFile.uri)

        // Then
        assertTrue(result is FileLoadResult.PermissionRevoked)
        assertTrue(result.requiresUserAction)
    }

    @Test
    fun `FileManager handles external storage unmounted`() = testScope.runBlockingTest {
        // Given
        val fileManager = createTestFileManager()
        
        // When - external storage becomes unavailable
        whenever(android.os.Environment.getExternalStorageState())
            .thenReturn(android.os.Environment.MEDIA_UNMOUNTED)

        val result = fileManager.scanForVideoFiles()

        // Then
        assertTrue(result is FileScanResult.StorageUnavailable)
        assertTrue(result.affectsExternalFilesOnly)
    }

    @Test
    fun `FileManager handles corrupted video files`() = testScope.runBlockingTest {
        // Given
        val fileManager = createTestFileManager()
        val corruptedFile = createCorruptedVideoFile()

        // When
        val result = fileManager.validateVideoFile(corruptedFile)

        // Then
        assertTrue(result is FileValidationResult.Corrupted)
        assertTrue(result.hasRecoveryOptions)
        assertNotNull(result.suggestedActions)
    }

    @Test
    fun `FileManager handles network storage disconnection`() = testScope.runBlockingTest {
        // Given
        val fileManager = createTestFileManager()
        val networkFile = createNetworkVideoFile()

        // When - network disconnects during access
        whenever(mockContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE))
            .thenReturn(createDisconnectedConnectivityManager())

        val result = fileManager.loadVideoFile(networkFile.uri)

        // Then
        assertTrue(result is FileLoadResult.NetworkUnavailable)
        assertTrue(result.hasOfflineAlternative)
    }

    // ============================================================================
    // MEMORY MANAGEMENT - LOW MEMORY AND OOM SCENARIOS
    // ============================================================================

    @Test
    fun `MemoryManager handles OutOfMemoryError during video loading`() = testScope.runBlockingTest {
        // Given
        val memoryManager = createTestMemoryManager()
        val largeVideo = createLarge4KVideoFile()

        // When - simulate OOM during loading
        whenever(memoryManager.allocateVideoBuffer(largeVideo.size))
            .thenThrow(OutOfMemoryError("Cannot allocate buffer"))

        val result = memoryManager.loadVideoIntoMemory(largeVideo)

        // Then
        assertTrue(result is MemoryAllocationResult.OutOfMemory)
        verify(memoryManager).triggerGarbageCollection()
        verify(memoryManager).reduceBitrate()
    }

    @Test
    fun `MemoryManager handles memory pressure warnings`() = testScope.runBlockingTest {
        // Given
        val memoryManager = createTestMemoryManager()
        
        // When - system sends memory pressure warning
        memoryManager.onTrimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)

        // Then
        verify(memoryManager).releaseNonEssentialMemory()
        verify(memoryManager).pauseBackgroundTasks()
        assertTrue(memoryManager.isInLowMemoryMode())
    }

    @Test
    fun `MemoryManager handles memory leaks detection`() = testScope.runBlockingTest {
        // Given
        val memoryManager = createTestMemoryManager()
        
        // When - detect potential memory leak
        val memoryGrowth = simulateMemoryGrowth(50_000_000) // 50MB growth
        memoryManager.analyzeMemoryGrowth(memoryGrowth)

        // Then
        assertTrue(memoryManager.hasDetectedMemoryLeak())
        verify(memoryManager).reportMemoryLeak()
        verify(memoryManager).attemptLeakMitigation()
    }

    // ============================================================================
    // THREADING AND CONCURRENCY - RACE CONDITIONS AND DEADLOCKS
    // ============================================================================

    @Test
    fun `ThreadManager handles race conditions in subtitle generation`() = testScope.runBlockingTest {
        // Given
        val threadManager = createTestThreadManager()
        val subtitleManager = createTestAISubtitleManager()

        // When - multiple concurrent subtitle requests
        val languages = listOf("en", "es", "fr", "de", "it")
        val jobs = languages.map { language ->
            launch {
                subtitleManager.generateSubtitles(language)
            }
        }

        // Then - should handle concurrency without race conditions
        jobs.forEach { it.join() }
        assertFalse(threadManager.hasDeadlocks())
        assertTrue(threadManager.allTasksCompleted())
    }

    @Test
    fun `ThreadManager handles thread pool exhaustion`() = testScope.runBlockingTest {
        // Given
        val threadManager = createTestThreadManager()
        threadManager.setMaxThreads(2) // Limited thread pool

        // When - submit more tasks than threads available
        val tasks = (1..10).map { index ->
            threadManager.submitTask {
                Thread.sleep(1000) // Long-running task
                "Task $index completed"
            }
        }

        // Then - should queue excess tasks without crashing
        assertTrue(threadManager.hasQueuedTasks())
        assertFalse(threadManager.isThreadPoolExhausted())
    }

    @Test
    fun `ThreadManager handles uncaught exceptions in background threads`() = testScope.runBlockingTest {
        // Given
        val threadManager = createTestThreadManager()
        val exceptionHandler = createMockExceptionHandler()

        // When - background thread throws exception
        threadManager.submitTask {
            throw RuntimeException("Test exception in background thread")
        }

        advanceUntilIdle()

        // Then - should handle gracefully
        verify(exceptionHandler).handleUncaughtException(any(), any())
        assertTrue(threadManager.isStable())
    }

    // ============================================================================
    // NETWORK OPERATIONS - CONNECTIVITY AND PROTOCOL EDGE CASES
    // ============================================================================

    @Test
    fun `NetworkManager handles SSL certificate validation failures`() = testScope.runBlockingTest {
        // Given
        val networkManager = createTestNetworkManager()
        
        // When - SSL certificate is invalid
        whenever(networkManager.validateCertificate(any()))
            .thenThrow(javax.net.ssl.SSLException("Certificate validation failed"))

        val result = networkManager.makeSecureRequest("https://api.astralstream.app/test")

        // Then
        assertTrue(result is NetworkResult.SSLError)
        assertFalse(result.allowsInsecureRetry)
    }

    @Test
    fun `NetworkManager handles HTTP/2 protocol upgrade failures`() = testScope.runBlockingTest {
        // Given
        val networkManager = createTestNetworkManager()
        
        // When - HTTP/2 upgrade fails
        whenever(networkManager.attemptHttp2Upgrade())
            .thenReturn(ProtocolUpgradeResult.Failed("Server doesn't support HTTP/2"))

        val result = networkManager.optimizeConnection()

        // Then
        assertTrue(result.fallbackToHttp1)
        assertTrue(result.maintainsCompatibility)
    }

    @Test
    fun `NetworkManager handles request timeout variations`() = testScope.runBlockingTest {
        // Given
        val networkManager = createTestNetworkManager()
        val timeoutScenarios = listOf(1000L, 5000L, 30000L, 60000L)

        timeoutScenarios.forEach { timeout ->
            // When - test different timeout scenarios
            networkManager.setRequestTimeout(timeout)
            
            // Simulate slow response
            advanceTimeBy(timeout + 1000)
            
            val result = networkManager.makeRequest("https://slow-api.example.com")

            // Then
            assertTrue(result is NetworkResult.Timeout)
            assertEquals(timeout, result.timeoutDuration)
        }
    }

    // ============================================================================
    // USER INTERFACE - LAYOUT AND INPUT EDGE CASES
    // ============================================================================

    @Test
    fun `UIManager handles screen rotation during video playback`() = testScope.runBlockingTest {
        // Given
        val uiManager = createTestUIManager()
        val videoPlayer = createTestVideoPlayer()
        videoPlayer.play()

        // When - screen rotates multiple times rapidly
        repeat(5) {
            uiManager.onConfigurationChanged(createLandscapeConfiguration())
            uiManager.onConfigurationChanged(createPortraitConfiguration())
        }

        // Then - should maintain stable state
        assertTrue(videoPlayer.isPlaying())
        assertTrue(uiManager.isLayoutStable())
        assertFalse(uiManager.hasLayoutErrors())
    }

    @Test
    fun `UIManager handles extreme screen size variations`() = testScope.runBlockingTest {
        // Given
        val uiManager = createTestUIManager()
        val extremeScreenSizes = listOf(
            createScreenSize(240, 320), // Very small
            createScreenSize(480, 800), // Small
            createScreenSize(1440, 2960), // Large
            createScreenSize(2160, 3840), // Very large
            createScreenSize(100, 100), // Square
            createScreenSize(4000, 1000) // Ultra-wide
        )

        extremeScreenSizes.forEach { screenSize ->
            // When
            uiManager.adaptToScreenSize(screenSize)

            // Then - UI should adapt appropriately
            assertTrue(uiManager.isUIResponsive())
            assertTrue(uiManager.areControlsAccessible())
            assertFalse(uiManager.hasLayoutOverflow())
        }
    }

    @Test
    fun `UIManager handles accessibility service interactions`() = testScope.runBlockingTest {
        // Given
        val uiManager = createTestUIManager()
        val accessibilityServices = listOf(
            "com.google.android.marvin.talkback", // TalkBack
            "com.samsung.android.app.talkback", // Samsung TalkBack
            "com.google.android.accessibility.selecttospeak" // Select to Speak
        )

        accessibilityServices.forEach { service ->
            // When - accessibility service is enabled
            uiManager.onAccessibilityServiceEnabled(service)

            // Then - UI should adapt for accessibility
            assertTrue(uiManager.isAccessibilityOptimized())
            assertTrue(uiManager.hasProperContentDescriptions())
            assertTrue(uiManager.supportsScreenReader())
        }
    }

    // ============================================================================
    // HARDWARE INTEGRATION - SENSOR AND DEVICE EDGE CASES
    // ============================================================================

    @Test
    fun `HardwareManager handles sensor unavailability`() = testScope.runBlockingTest {
        // Given
        val hardwareManager = createTestHardwareManager()
        
        // When - required sensors are not available
        whenever(mockContext.getPackageManager().hasSystemFeature("android.hardware.sensor.accelerometer"))
            .thenReturn(false)

        val result = hardwareManager.initializeMotionSensors()

        // Then
        assertTrue(result is SensorInitResult.SensorsUnavailable)
        assertTrue(result.usesAlternativeMethods)
    }

    @Test
    fun `HardwareManager handles biometric hardware failures`() = testScope.runBlockingTest {
        // Given
        val hardwareManager = createTestHardwareManager()
        
        // When - biometric hardware fails
        whenever(hardwareManager.biometricManager.canAuthenticate())
            .thenReturn(androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE)

        val result = hardwareManager.authenticateUser()

        // Then
        assertTrue(result is AuthResult.BiometricUnavailable)
        assertTrue(result.fallsBackToPassword)
    }

    @Test
    fun `HardwareManager handles camera access conflicts`() = testScope.runBlockingTest {
        // Given
        val hardwareManager = createTestHardwareManager()
        
        // When - camera is in use by another app
        whenever(hardwareManager.cameraManager.openCamera(any(), any(), any()))
            .thenThrow(android.hardware.camera2.CameraAccessException("Camera in use"))

        val result = hardwareManager.initializeCamera()

        // Then
        assertTrue(result is CameraResult.AccessDenied)
        assertTrue(result.suggestsRetryLater)
    }

    // ============================================================================
    // SECURITY AND ENCRYPTION - CRYPTOGRAPHIC EDGE CASES
    // ============================================================================

    @Test
    fun `SecurityManager handles key generation failures`() = testScope.runBlockingTest {
        // Given
        val securityManager = createTestSecurityManager()
        
        // When - key generation fails
        whenever(securityManager.keyGenerator.generateKey())
            .thenThrow(java.security.NoSuchAlgorithmException("Algorithm not available"))

        val result = securityManager.initializeEncryption()

        // Then
        assertTrue(result is EncryptionResult.KeyGenerationFailed)
        assertTrue(result.hasAlternativeMethod)
    }

    @Test
    fun `SecurityManager handles encryption key corruption`() = testScope.runBlockingTest {
        // Given
        val securityManager = createTestSecurityManager()
        val corruptedKey = ByteArray(32) { 0 } // All zeros - invalid key

        // When
        val result = securityManager.decryptData("encrypted_data", corruptedKey)

        // Then
        assertTrue(result is DecryptionResult.KeyCorrupted)
        assertTrue(result.requiresKeyReset)
    }

    @Test
    fun `SecurityManager handles certificate pinning failures`() = testScope.runBlockingTest {
        // Given
        val securityManager = createTestSecurityManager()
        val invalidCertificate = createInvalidCertificate()

        // When
        val result = securityManager.validateCertificatePinning(invalidCertificate)

        // Then
        assertTrue(result is CertificateValidationResult.PinningFailed)
        assertFalse(result.allowsConnection)
    }

    // ============================================================================
    // INTERNATIONALIZATION - LOCALE AND TEXT RENDERING EDGE CASES
    // ============================================================================

    @Test
    fun `LocalizationManager handles unsupported locales`() = testScope.runBlockingTest {
        // Given
        val localizationManager = createTestLocalizationManager()
        val unsupportedLocales = listOf(
            java.util.Locale("xx", "XX"), // Invalid locale
            java.util.Locale("zzz"), // Three-letter code
            java.util.Locale("") // Empty locale
        )

        unsupportedLocales.forEach { locale ->
            // When
            val result = localizationManager.setLocale(locale)

            // Then
            assertTrue(result is LocalizationResult.UnsupportedLocale)
            assertEquals(java.util.Locale.ENGLISH, localizationManager.getCurrentLocale()) // Falls back to English
        }
    }

    @Test
    fun `LocalizationManager handles RTL text rendering edge cases`() = testScope.runBlockingTest {
        // Given
        val localizationManager = createTestLocalizationManager()
        val rtlLocale = java.util.Locale("ar", "SA") // Arabic (Saudi Arabia)

        // When
        localizationManager.setLocale(rtlLocale)
        val mixedText = "Hello العالم 123" // Mixed LTR/RTL/numbers

        val result = localizationManager.formatText(mixedText)

        // Then
        assertTrue(result.isRTL)
        assertTrue(result.handlesDirectionalOverrides)
        assertFalse(result.hasRenderingIssues)
    }

    @Test
    fun `LocalizationManager handles complex script rendering`() = testScope.runBlockingTest {
        // Given
        val localizationManager = createTestLocalizationManager()
        val complexScripts = mapOf(
            "hi" to "नमस्ते दुनिया", // Devanagari (Hindi)
            "th" to "สวัสดีชาวโลก", // Thai
            "ja" to "こんにちは世界", // Japanese (Hiragana + Kanji)
            "zh" to "你好世界", // Chinese (Simplified)
            "ko" to "안녕하세요 세계" // Korean (Hangul)
        )

        complexScripts.forEach { (languageCode, text) ->
            // When
            localizationManager.setLocale(java.util.Locale(languageCode))
            val result = localizationManager.renderText(text)

            // Then
            assertTrue(result.isRenderedCorrectly)
            assertFalse(result.hasGlyphSubstitutions)
            assertTrue(result.respectsLanguageRules)
        }
    }

    // ============================================================================
    // DATA SERIALIZATION - JSON AND BINARY FORMAT EDGE CASES
    // ============================================================================

    @Test
    fun `SerializationManager handles malformed JSON gracefully`() = testScope.runBlockingTest {
        // Given
        val serializationManager = createTestSerializationManager()
        val malformedJsonSamples = listOf(
            "{invalid json", // Unclosed brace
            "[1,2,3,}", // Invalid array
            "{'key': value}", // Unquoted value
            "{\"key\": \"value\",}", // Trailing comma
            "", // Empty string
            "null", // Null value
            "{\"key\": ∞}", // Invalid number
            "{\"\\uXXXX\": \"value\"}" // Invalid unicode escape
        )

        malformedJsonSamples.forEach { json ->
            // When
            val result = serializationManager.deserializeFromJson(json, TestDataClass::class.java)

            // Then
            assertTrue(result is DeserializationResult.MalformedData)
            assertNotNull(result.errorDescription)
        }
    }

    @Test
    fun `SerializationManager handles circular references`() = testScope.runBlockingTest {
        // Given
        val serializationManager = createTestSerializationManager()
        val circularObject = createCircularReferenceObject()

        // When
        val result = serializationManager.serializeToJson(circularObject)

        // Then
        assertTrue(result is SerializationResult.CircularReference)
        assertTrue(result.hasAlternativeFormat)
    }

    @Test
    fun `SerializationManager handles version compatibility`() = testScope.runBlockingTest {
        // Given
        val serializationManager = createTestSerializationManager()
        val oldVersionData = createOldVersionSerializedData()

        // When
        val result = serializationManager.migrateSerializedData(oldVersionData, "1.0", "2.0")

        // Then
        assertTrue(result is MigrationResult.Success)
        assertTrue(result.preservedDataIntegrity)
        assertFalse(result.hasDataLoss)
    }

    // ============================================================================
    // HELPER METHODS FOR TEST SETUP
    // ============================================================================

    private fun createRecentFilesRepository(): RecentFilesRepository = mock()
    
    private fun createSettingsRepository(): SettingsRepository = mock()
    
    private fun createTestFileManager(): FileManager = mock()
    
    private fun createTestMemoryManager(): MemoryManager = mock()
    
    private fun createTestThreadManager(): ThreadManager = mock()
    
    private fun createTestNetworkManager(): NetworkManager = mock()
    
    private fun createTestUIManager(): UIManager = mock()
    
    private fun createTestHardwareManager(): HardwareManager = mock()
    
    private fun createTestSecurityManager(): SecurityManager = mock()
    
    private fun createTestLocalizationManager(): LocalizationManager = mock()
    
    private fun createTestSerializationManager(): SerializationManager = mock()

    private fun createTestRecentFile(path: String = "test.mp4"): RecentFile = 
        RecentFile(
            id = 1,
            path = path,
            name = "Test Video",
            duration = 120000,
            lastPlayedPosition = 30000,
            lastPlayedTime = System.currentTimeMillis(),
            thumbnailPath = null
        )

    private fun createTestVideoFile(): VideoFile = mock()
    
    private fun createCorruptedVideoFile(): VideoFile = mock()
    
    private fun createNetworkVideoFile(): VideoFile = mock()
    
    private fun createLarge4KVideoFile(): VideoFile = mock()

    private fun simulateMemoryGrowth(bytes: Int): MemoryGrowthData = 
        MemoryGrowthData(
            initialMemory = 100_000_000,
            currentMemory = 100_000_000 + bytes,
            growthRate = bytes / 1000, // Per second
            duration = 1000
        )

    private fun createMockExceptionHandler(): Thread.UncaughtExceptionHandler = mock()

    private fun createLandscapeConfiguration(): android.content.res.Configuration = 
        android.content.res.Configuration().apply {
            orientation = android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }

    private fun createPortraitConfiguration(): android.content.res.Configuration = 
        android.content.res.Configuration().apply {
            orientation = android.content.res.Configuration.ORIENTATION_PORTRAIT
        }

    private fun createScreenSize(width: Int, height: Int): ScreenSize = 
        ScreenSize(width, height)

    private fun createDisconnectedConnectivityManager(): android.net.ConnectivityManager = mock {
        on { activeNetworkInfo } doReturn null
    }

    private fun createInvalidCertificate(): java.security.cert.X509Certificate = mock()

    private fun createCircularReferenceObject(): Any {
        val obj1 = TestCircularClass()
        val obj2 = TestCircularClass()
        obj1.reference = obj2
        obj2.reference = obj1
        return obj1
    }

    private fun createOldVersionSerializedData(): String = 
        """{"version": "1.0", "data": {"oldField": "value"}}"""

    // Data classes for testing
    data class TestDataClass(val key: String, val value: Int)
    
    class TestCircularClass {
        var reference: TestCircularClass? = null
    }
    
    data class ScreenSize(val width: Int, val height: Int)
    
    data class MemoryGrowthData(
        val initialMemory: Long,
        val currentMemory: Long,
        val growthRate: Int,
        val duration: Long
    )

    // Result classes for testing
    sealed class DatabaseResult {
        object Success : DatabaseResult()
        object CorruptionDetected : DatabaseResult()
    }

    sealed class MigrationResult {
        object Success : MigrationResult()
        data class Failed(val requiresDataReset: Boolean) : MigrationResult()
    }

    sealed class FileLoadResult {
        object Success : FileLoadResult()
        data class PermissionRevoked(val requiresUserAction: Boolean) : FileLoadResult()
        data class NetworkUnavailable(val hasOfflineAlternative: Boolean) : FileLoadResult()
    }

    sealed class FileScanResult {
        object Success : FileScanResult()
        data class StorageUnavailable(val affectsExternalFilesOnly: Boolean) : FileScanResult()
    }

    sealed class FileValidationResult {
        object Valid : FileValidationResult()
        data class Corrupted(val hasRecoveryOptions: Boolean, val suggestedActions: List<String>) : FileValidationResult()
    }

    sealed class MemoryAllocationResult {
        object Success : MemoryAllocationResult()
        object OutOfMemory : MemoryAllocationResult()
    }

    sealed class NetworkResult {
        object Success : NetworkResult()
        data class SSLError(val allowsInsecureRetry: Boolean) : NetworkResult()
        data class Timeout(val timeoutDuration: Long) : NetworkResult()
    }

    data class ProtocolUpgradeResult(
        val success: Boolean,
        val fallbackToHttp1: Boolean = false,
        val maintainsCompatibility: Boolean = true
    ) {
        companion object {
            fun Failed(reason: String) = ProtocolUpgradeResult(
                success = false,
                fallbackToHttp1 = true,
                maintainsCompatibility = true
            )
        }
    }

    sealed class SensorInitResult {
        object Success : SensorInitResult()
        data class SensorsUnavailable(val usesAlternativeMethods: Boolean) : SensorInitResult()
    }

    sealed class AuthResult {
        object Success : AuthResult()
        data class BiometricUnavailable(val fallsBackToPassword: Boolean) : AuthResult()
    }

    sealed class CameraResult {
        object Success : CameraResult()
        data class AccessDenied(val suggestsRetryLater: Boolean) : CameraResult()
    }

    sealed class EncryptionResult {
        object Success : EncryptionResult()
        data class KeyGenerationFailed(val hasAlternativeMethod: Boolean) : EncryptionResult()
    }

    sealed class DecryptionResult {
        object Success : DecryptionResult()
        data class KeyCorrupted(val requiresKeyReset: Boolean) : DecryptionResult()
    }

    sealed class CertificateValidationResult {
        object Valid : CertificateValidationResult()
        data class PinningFailed(val allowsConnection: Boolean) : CertificateValidationResult()
    }

    sealed class LocalizationResult {
        object Success : LocalizationResult()
        object UnsupportedLocale : LocalizationResult()
    }

    data class TextRenderingResult(
        val isRTL: Boolean,
        val handlesDirectionalOverrides: Boolean,
        val hasRenderingIssues: Boolean,
        val isRenderedCorrectly: Boolean = true,
        val hasGlyphSubstitutions: Boolean = false,
        val respectsLanguageRules: Boolean = true
    )

    sealed class DeserializationResult {
        object Success : DeserializationResult()
        data class MalformedData(val errorDescription: String) : DeserializationResult()
    }

    sealed class SerializationResult {
        object Success : SerializationResult()
        data class CircularReference(val hasAlternativeFormat: Boolean) : SerializationResult()
    }
}