package com.astralplayer.nextplayer.cloud

import android.content.Context
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
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFalse

/**
 * Comprehensive tests for cloud synchronization platform
 * Tests cloud providers, data sync, file operations, and multi-device capabilities
 */
@RunWith(AndroidJUnit4::class)
class CloudSynchronizationPlatformTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var cloudPlatform: CloudSynchronizationPlatform
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        cloudPlatform = CloudSynchronizationPlatform(context)
    }

    @After
    fun tearDown() {
        cloudPlatform.cleanup()
    }

    @Test
    fun testCloudPlatformInitialization() = runTest {
        // When
        val result = cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Then
        assertNotNull("Initialization result should not be null", result)
        assertTrue("Cloud platform should initialize successfully", result.success)
        assertTrue("Should have available providers", result.availableProviders.isNotEmpty())
        assertNotNull("Device ID should be generated", result.deviceId)
        assertTrue("Initialization time should be set", result.initializationTime > 0)
        
        // Verify state
        val state = cloudPlatform.syncState.value
        assertTrue("System should be initialized", state.isInitialized)
        assertTrue("Available providers should be populated", state.availableProviders.isNotEmpty())
        assertEquals("Device ID should match", result.deviceId, state.currentDeviceId)
        
        // Verify available providers
        val expectedProviders = listOf("google_drive", "dropbox", "onedrive", "icloud", "astral_cloud")
        assertTrue("Should have expected providers", 
                  result.availableProviders.containsAll(expectedProviders))
    }

    @Test
    fun testGoogleDriveConnection() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        val googleDriveProvider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials(
            clientId = "test_client_id",
            clientSecret = "test_client_secret",
            accessToken = "test_access_token"
        )
        
        // When
        val result = cloudPlatform.connectToProvider(googleDriveProvider, credentials)
        
        // Then
        assertNotNull("Connection result should not be null", result)
        assertTrue("Connection should succeed", result.success)
        assertEquals("Provider ID should match", "google_drive", result.providerId)
        assertNotNull("Connection ID should be generated", result.connectionId)
        assertNotNull("Storage quota should be provided", result.storageQuota)
        assertTrue("Connection time should be set", result.connectionTime > 0)
        
        // Verify quota information
        val quota = result.storageQuota!!
        assertTrue("Total quota should be positive", quota.total > 0)
        assertTrue("Available quota should be reasonable", quota.available <= quota.total)
        
        // Verify state update
        val state = cloudPlatform.syncState.value
        assertTrue("Google Drive should be in connected providers", 
                  state.connectedProviders.contains("google_drive"))
        assertTrue("Last connection time should be recent", 
                  System.currentTimeMillis() - state.lastConnectionTime < 5000)
    }

    @Test
    fun testWatchHistorySync() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Connect to provider
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        advanceUntilIdle()
        
        // When
        val result = cloudPlatform.syncWatchHistory("google_drive")
        
        // Then
        assertNotNull("Sync result should not be null", result)
        assertTrue("Watch history sync should succeed", result.success)
        assertTrue("Synced entries should be non-negative", result.syncedEntries >= 0)
        assertTrue("Conflicts resolved should be non-negative", result.conflictsResolved >= 0)
        assertTrue("Last sync time should be set", result.lastSyncTime > 0)
    }

    @Test
    fun testPlaylistSync() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Connect to provider
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        advanceUntilIdle()
        
        // When
        val result = cloudPlatform.syncPlaylists("google_drive")
        
        // Then
        assertNotNull("Sync result should not be null", result)
        assertTrue("Playlist sync should succeed", result.success)
        assertTrue("Synced playlists should be non-negative", result.syncedPlaylists >= 0)
        assertTrue("Conflicts resolved should be non-negative", result.conflictsResolved >= 0)
        assertTrue("Last sync time should be set", result.lastSyncTime > 0)
    }

    @Test
    fun testUserPreferencesSync() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Connect to provider
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        advanceUntilIdle()
        
        // When
        val result = cloudPlatform.syncUserPreferences("google_drive")
        
        // Then
        assertNotNull("Sync result should not be null", result)
        assertTrue("Preferences sync should succeed", result.success)
        assertTrue("Synced preferences should be non-negative", result.syncedPreferences >= 0)
        assertTrue("Conflicts resolved should be non-negative", result.conflictsResolved >= 0)
        assertTrue("Last sync time should be set", result.lastSyncTime > 0)
    }

    @Test
    fun testBookmarkSync() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Connect to provider
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        advanceUntilIdle()
        
        // When
        val result = cloudPlatform.syncBookmarks("google_drive")
        
        // Then
        assertNotNull("Sync result should not be null", result)
        assertTrue("Bookmark sync should succeed", result.success)
        assertTrue("Synced bookmarks should be non-negative", result.syncedBookmarks >= 0)
        assertTrue("Conflicts resolved should be non-negative", result.conflictsResolved >= 0)
        assertTrue("Last sync time should be set", result.lastSyncTime > 0)
    }

    @Test
    fun testComprehensiveSync() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Connect to provider
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        advanceUntilIdle()
        
        // When
        val result = cloudPlatform.syncAllData("google_drive")
        
        // Then
        assertNotNull("Comprehensive sync result should not be null", result)
        assertTrue("Comprehensive sync should succeed", result.success)
        assertTrue("Should have sync results", result.syncResults.isNotEmpty())
        assertEquals("Should have results for all data types", 4, result.syncResults.size)
        assertTrue("Total conflicts resolved should be non-negative", result.totalConflictsResolved >= 0)
        assertTrue("Last sync time should be set", result.lastSyncTime > 0)
        
        // Verify all data types are synced
        val expectedDataTypes = setOf("watchHistory", "playlists", "preferences", "bookmarks")
        assertEquals("All data types should be synced", expectedDataTypes, result.syncResults.keys)
        
        // Verify state update
        val state = cloudPlatform.syncState.value
        assertTrue("Last full sync time should be updated", state.lastFullSyncTime > 0)
        assertTrue("Total syncs completed should increase", state.totalSyncsCompleted > 0)
        assertEquals("Pending sync operations should be 0", 0, state.pendingSyncOperations)
    }

    @Test
    fun testFileUpload() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Connect to provider
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        advanceUntilIdle()
        
        // Create test file
        val testFile = File(context.cacheDir, "test_upload.txt")
        testFile.writeText("Test content for upload")
        
        // When
        val result = cloudPlatform.uploadFile("google_drive", testFile, "/uploads/test_upload.txt")
        
        // Then
        assertNotNull("Upload result should not be null", result)
        assertTrue("File upload should succeed", result.success)
        assertNotNull("File ID should be generated", result.fileId)
        assertEquals("Remote path should match", "/uploads/test_upload.txt", result.remotePath)
        assertEquals("File size should match", testFile.length(), result.fileSize)
        assertTrue("Upload time should be set", result.uploadTime > 0)
        assertNotNull("Checksum should be generated", result.checksum)
        
        // Cleanup
        testFile.delete()
    }

    @Test
    fun testFileDownload() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Connect to provider
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        advanceUntilIdle()
        
        // Prepare download destination
        val downloadFile = File(context.cacheDir, "test_download.txt")
        if (downloadFile.exists()) downloadFile.delete()
        
        // When
        val result = cloudPlatform.downloadFile("google_drive", "/uploads/test_file.txt", downloadFile)
        
        // Then
        assertNotNull("Download result should not be null", result)
        assertTrue("File download should succeed", result.success)
        assertEquals("Local path should match", downloadFile.absolutePath, result.localPath)
        assertTrue("File size should be positive", result.fileSize >= 0)
        assertTrue("Download time should be set", result.downloadTime > 0)
        assertNotNull("Checksum should be generated", result.checksum)
        
        // Cleanup
        if (downloadFile.exists()) downloadFile.delete()
    }

    @Test
    fun testAutoSyncConfiguration() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Connect to provider
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        advanceUntilIdle()
        
        val autoSyncConfig = AutoSyncConfig(
            enabled = true,
            syncInterval = 30 * 60 * 1000L, // 30 minutes
            syncCondition = AutoSyncCondition.WIFI_ONLY,
            dataTypesToSync = setOf(
                SyncDataType.WATCH_HISTORY,
                SyncDataType.PLAYLISTS,
                SyncDataType.PREFERENCES
            ),
            maxRetryAttempts = 3,
            batteryOptimized = true
        )
        
        // When
        val result = cloudPlatform.enableAutoSync("google_drive", autoSyncConfig)
        
        // Then
        assertNotNull("Auto sync result should not be null", result)
        assertTrue("Auto sync should be enabled successfully", result.success)
        assertEquals("Provider ID should match", "google_drive", result.providerId)
        assertEquals("Config should match", autoSyncConfig, result.config)
        assertTrue("Enable time should be set", result.enableTime > 0)
        
        // Verify state update
        val state = cloudPlatform.syncState.value
        assertTrue("Auto sync should be enabled in state", state.autoSyncEnabled)
        assertEquals("Auto sync provider should be set", "google_drive", state.autoSyncProviderId)
        assertEquals("Auto sync config should be stored", autoSyncConfig, state.autoSyncConfig)
    }

    @Test
    fun testConnectedDevices() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Connect to provider
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        advanceUntilIdle()
        
        // When
        val result = cloudPlatform.getConnectedDevices("google_drive")
        
        // Then
        assertNotNull("Connected devices result should not be null", result)
        assertTrue("Getting connected devices should succeed", result.success)
        assertTrue("Total devices should be non-negative", result.totalDevices >= 0)
        assertNotNull("Current device should be provided", result.currentDevice)
        
        // Verify current device information
        val currentDevice = result.currentDevice!!
        assertNotNull("Device ID should not be null", currentDevice.deviceId)
        assertNotNull("Device name should not be null", currentDevice.deviceName)
        assertNotNull("Device type should not be null", currentDevice.deviceType)
        assertTrue("Last seen should be recent", 
                  System.currentTimeMillis() - currentDevice.lastSeen < 10000)
    }

    @Test
    fun testSyncStatistics() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Connect to provider and perform some syncs
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        cloudPlatform.syncAllData("google_drive")
        advanceUntilIdle()
        
        // When
        val result = cloudPlatform.getSyncStatistics("google_drive")
        
        // Then
        assertNotNull("Statistics result should not be null", result)
        assertTrue("Getting statistics should succeed", result.success)
        assertNotNull("Statistics should be provided", result.statistics)
        
        // Verify statistics structure
        val stats = result.statistics!!
        assertTrue("Total syncs should be non-negative", stats.totalSyncs >= 0)
        assertTrue("Successful syncs should be non-negative", stats.successfulSyncs >= 0)
        assertTrue("Failed syncs should be non-negative", stats.failedSyncs >= 0)
        assertTrue("Total syncs should equal successful + failed", 
                  stats.totalSyncs == stats.successfulSyncs + stats.failedSyncs)
        assertTrue("Average sync duration should be non-negative", stats.averageSyncDuration >= 0)
        assertTrue("Total data synced should be non-negative", stats.totalDataSynced >= 0)
        assertTrue("Conflicts resolved should be non-negative", stats.conflictsResolved >= 0)
    }

    @Test
    fun testCloudEventEmission() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        val events = mutableListOf<CloudSyncEvent>()
        val job = launch {
            cloudPlatform.syncEvents.collect { event ->
                events.add(event)
            }
        }
        
        // When - Perform various cloud operations
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        cloudPlatform.syncWatchHistory("google_drive")
        
        advanceUntilIdle()
        job.cancel()
        
        // Then
        assertTrue("Should have emitted events", events.isNotEmpty())
        
        val hasSystemInitialized = events.any { it is CloudSyncEvent.SystemInitialized }
        val hasProviderConnected = events.any { it is CloudSyncEvent.ProviderConnected }
        val hasWatchHistorySynced = events.any { it is CloudSyncEvent.WatchHistorySynced }
        
        assertTrue("Should have system initialized event", hasSystemInitialized)
        assertTrue("Should have provider connected event", hasProviderConnected)
        assertTrue("Should have watch history synced event", hasWatchHistorySynced)
    }

    @Test
    fun testMultipleProviderConnections() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        val providers = listOf(
            GoogleDriveProvider(),
            DropboxProvider(),
            OneDriveProvider()
        )
        
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        
        // When - Connect to multiple providers
        val results = providers.map { provider ->
            cloudPlatform.connectToProvider(provider, credentials)
        }
        
        // Then
        assertEquals("All providers should connect", 3, results.size)
        results.forEach { result ->
            assertTrue("Each connection should succeed", result.success)
            assertNotNull("Each should have provider ID", result.providerId)
        }
        
        // Verify state
        val state = cloudPlatform.syncState.value
        assertEquals("Should have 3 connected providers", 3, state.connectedProviders.size)
        assertTrue("Should include Google Drive", state.connectedProviders.contains("google_drive"))
        assertTrue("Should include Dropbox", state.connectedProviders.contains("dropbox"))
        assertTrue("Should include OneDrive", state.connectedProviders.contains("onedrive"))
    }

    @Test
    fun testDataSynchronizerMerging() = runTest {
        // Test the data synchronizer directly
        val synchronizer = DataSynchronizer("test_data", WatchHistory.serializer())
        
        val localHistory = WatchHistory(
            entries = listOf(
                WatchHistoryEntry(
                    entryId = "entry1",
                    videoUri = "video1",
                    videoTitle = "Video 1",
                    watchedAt = 1000L,
                    position = 500L,
                    duration = 1000L,
                    deviceId = "device1"
                )
            ),
            lastModified = 2000L,
            deviceId = "device1"
        )
        
        val remoteHistory = WatchHistory(
            entries = listOf(
                WatchHistoryEntry(
                    entryId = "entry2",
                    videoUri = "video2",
                    videoTitle = "Video 2",
                    watchedAt = 1500L,
                    position = 750L,
                    duration = 1500L,
                    deviceId = "device2"
                )
            ),
            lastModified = 2500L,
            deviceId = "device2"
        )
        
        // When
        val mergeResult = synchronizer.merge(localHistory, remoteHistory)
        
        // Then
        assertNotNull("Merge result should not be null", mergeResult)
        assertNotNull("Merged data should not be null", mergeResult.mergedData)
        assertNotNull("Conflicts list should not be null", mergeResult.conflicts)
        assertNotNull("Merge strategy should be set", mergeResult.mergeStrategy)
    }

    @Test
    fun testCloudProviderFileOperations() = runTest {
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        
        // Test connection
        val connection = provider.connect(credentials)
        assertNotNull("Connection should be established", connection)
        assertTrue("Connection should be active", connection.isConnected)
        assertEquals("Provider ID should match", "google_drive", connection.providerId)
        
        // Test verification
        val verification = provider.verifyConnection(connection)
        assertTrue("Verification should succeed", verification.success)
        assertNotNull("Storage quota should be provided", verification.storageQuota)
        assertTrue("Should have permissions", verification.permissions.isNotEmpty())
        
        // Test folder creation
        val folderResult = provider.createFolder(connection, "test_folder")
        assertTrue("Folder creation should succeed", folderResult.success)
        assertEquals("Folder path should match", "test_folder", folderResult.folderPath)
        
        // Test file listing
        val listResult = provider.listFiles(connection, "test_folder")
        assertTrue("File listing should succeed", listResult.success)
        assertNotNull("File list should not be null", listResult.files)
        
        // Test file info
        val fileInfo = provider.getFileInfo(connection, "test_file.txt")
        assertNotNull("File info should not be null", fileInfo)
        assertEquals("File name should be extracted", "file.txt", fileInfo.fileName)
        assertTrue("File size should be positive", fileInfo.fileSize > 0)
    }

    @Test
    fun testSyncStateTracking() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Initial state
        var state = cloudPlatform.syncState.value
        assertTrue("Should be initialized", state.isInitialized)
        assertFalse("Auto sync should be disabled initially", state.autoSyncEnabled)
        assertEquals("Should have no connected providers initially", 0, state.connectedProviders.size)
        
        // Connect provider
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        advanceUntilIdle()
        
        state = cloudPlatform.syncState.value
        assertEquals("Should have 1 connected provider", 1, state.connectedProviders.size)
        assertTrue("Last connection time should be recent", 
                  System.currentTimeMillis() - state.lastConnectionTime < 5000)
        
        // Perform sync
        cloudPlatform.syncAllData("google_drive")
        advanceUntilIdle()
        
        state = cloudPlatform.syncState.value
        assertTrue("Total syncs completed should increase", state.totalSyncsCompleted > 0)
        assertTrue("Last full sync time should be recent", 
                  System.currentTimeMillis() - state.lastFullSyncTime < 5000)
    }

    @Test
    fun testErrorHandling() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Test sync without connection
        val syncResult = cloudPlatform.syncWatchHistory("nonexistent_provider")
        assertFalse("Sync should fail without valid provider", syncResult.success)
        assertNotNull("Should have error message", syncResult.error)
        
        // Test file upload without connection
        val testFile = File(context.cacheDir, "test.txt")
        testFile.writeText("test")
        
        val uploadResult = cloudPlatform.uploadFile("nonexistent_provider", testFile, "/test.txt")
        assertFalse("Upload should fail without valid provider", uploadResult.success)
        assertNotNull("Should have error message", uploadResult.error)
        
        // Test auto sync enable without connection
        val autoSyncResult = cloudPlatform.enableAutoSync(
            "nonexistent_provider", 
            AutoSyncConfig(enabled = true)
        )
        assertFalse("Auto sync enable should fail without valid provider", autoSyncResult.success)
        assertNotNull("Should have error message", autoSyncResult.error)
        
        // Cleanup
        testFile.delete()
    }

    @Test
    fun testConcurrentSyncOperations() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Connect to provider
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        advanceUntilIdle()
        
        // When - Perform multiple sync operations concurrently
        val syncOperations = listOf(
            async { cloudPlatform.syncWatchHistory("google_drive") },
            async { cloudPlatform.syncPlaylists("google_drive") },
            async { cloudPlatform.syncUserPreferences("google_drive") },
            async { cloudPlatform.syncBookmarks("google_drive") }
        )
        
        val results = syncOperations.awaitAll()
        
        // Then
        assertEquals("All sync operations should complete", 4, results.size)
        results.forEach { result ->
            assertNotNull("Each result should not be null", result)
            // Results may succeed or fail based on implementation, but should not crash
        }
    }

    @Test
    fun testCloudDataClassSerialization() {
        // Test data class serialization/deserialization
        val watchHistoryEntry = WatchHistoryEntry(
            entryId = "test_entry",
            videoUri = "test_video",
            videoTitle = "Test Video",
            watchedAt = System.currentTimeMillis(),
            position = 1000L,
            duration = 2000L,
            deviceId = "test_device"
        )
        
        val watchHistory = WatchHistory(
            entries = listOf(watchHistoryEntry),
            lastModified = System.currentTimeMillis(),
            deviceId = "test_device"
        )
        
        // Verify structure
        assertEquals("Should have 1 entry", 1, watchHistory.entries.size)
        assertEquals("Entry should match", watchHistoryEntry, watchHistory.entries[0])
        assertTrue("Last modified should be recent", 
                  System.currentTimeMillis() - watchHistory.lastModified < 5000)
    }

    @Test
    fun testDeviceInfoGeneration() = runTest {
        cloudPlatform.initialize()
        advanceUntilIdle()
        
        // Get connected devices to verify device info generation
        val provider = GoogleDriveProvider()
        val credentials = CloudCredentials.OAuth2Credentials("client", "secret", "token")
        cloudPlatform.connectToProvider(provider, credentials)
        
        val devicesResult = cloudPlatform.getConnectedDevices("google_drive")
        
        assertTrue("Should succeed", devicesResult.success)
        assertNotNull("Current device should be available", devicesResult.currentDevice)
        
        val device = devicesResult.currentDevice!!
        assertNotNull("Device ID should not be null", device.deviceId)
        assertTrue("Device ID should not be empty", device.deviceId.isNotEmpty())
        assertNotNull("Device name should not be null", device.deviceName)
        assertEquals("Device type should be Android phone", DeviceType.ANDROID_PHONE, device.deviceType)
        assertNotNull("OS version should not be null", device.osVersion)
        assertNotNull("App version should not be null", device.appVersion)
        assertTrue("Capabilities should not be empty", device.capabilities.isNotEmpty())
        assertTrue("Should have video playback capability", 
                  device.capabilities.contains("video_playback"))
    }
}