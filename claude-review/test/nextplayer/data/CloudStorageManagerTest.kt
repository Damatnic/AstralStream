package com.astralplayer.nextplayer.data

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class CloudStorageManagerTest {

    private lateinit var mockContext: Context
    private lateinit var cloudStorageManager: CloudStorageManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        cloudStorageManager = CloudStorageManager(mockContext)
    }

    @Test
    fun `initial sync status is idle`() {
        assertEquals(SyncStatus.IDLE, cloudStorageManager.syncStatus.value)
    }

    @Test
    fun `sync settings updates status correctly`() = runTest {
        assertEquals(SyncStatus.IDLE, cloudStorageManager.syncStatus.value)
        
        cloudStorageManager.syncSettings()
        
        assertEquals(SyncStatus.SUCCESS, cloudStorageManager.syncStatus.value)
    }

    @Test
    fun `upload file returns success`() = runTest {
        val result = cloudStorageManager.uploadFile("local.txt", "cloud.txt")
        assertTrue("Upload should succeed", result)
    }

    @Test
    fun `download file returns success`() = runTest {
        val result = cloudStorageManager.downloadFile("cloud.txt", "local.txt")
        assertTrue("Download should succeed", result)
    }

    @Test
    fun `list cloud files returns mock data`() = runTest {
        val files = cloudStorageManager.listCloudFiles()
        
        assertTrue("Should have files", files.isNotEmpty())
        assertTrue("Should have settings backup", files.any { it.name == "settings_backup.json" })
        assertTrue("Should have bookmarks backup", files.any { it.name == "bookmarks_backup.json" })
    }

    @Test
    fun `backup all data returns success`() = runTest {
        val result = cloudStorageManager.backupAllData()
        assertTrue("Backup should succeed", result)
        assertEquals(SyncStatus.SUCCESS, cloudStorageManager.syncStatus.value)
    }

    @Test
    fun `restore all data returns success`() = runTest {
        val result = cloudStorageManager.restoreAllData()
        assertTrue("Restore should succeed", result)
        assertEquals(SyncStatus.SUCCESS, cloudStorageManager.syncStatus.value)
    }

    @Test
    fun `cloud file data class works correctly`() {
        val file = CloudFile("test.json", 1024L, System.currentTimeMillis())
        
        assertEquals("test.json", file.name)
        assertEquals(1024L, file.size)
        assertTrue("Should have timestamp", file.lastModified > 0)
    }

    @Test
    fun `sync status enum has all values`() {
        val values = SyncStatus.values()
        
        assertTrue("Should have IDLE", values.contains(SyncStatus.IDLE))
        assertTrue("Should have SYNCING", values.contains(SyncStatus.SYNCING))
        assertTrue("Should have SUCCESS", values.contains(SyncStatus.SUCCESS))
        assertTrue("Should have ERROR", values.contains(SyncStatus.ERROR))
    }
}