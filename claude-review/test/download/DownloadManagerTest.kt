package com.astralplayer.download

import android.content.Context
import androidx.work.WorkManager
import com.astralplayer.domain.model.VideoMetadata
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

@ExperimentalCoroutinesApi
class DownloadManagerTest {
    
    @MockK
    private lateinit var context: Context
    
    @MockK
    private lateinit var offlineVideoRepository: OfflineVideoRepository
    
    @MockK
    private lateinit var workManager: WorkManager
    
    private lateinit var downloadManager: DownloadManager
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns workManager
        
        downloadManager = DownloadManager(context, offlineVideoRepository)
    }
    
    @Test
    fun `queueDownload should add task to queue and schedule work`() = runTest {
        // Given
        val videoUri = "https://example.com/video.mp4"
        val metadata = VideoMetadata(
            uri = videoUri,
            title = "Test Video",
            duration = 120000L,
            width = 1920,
            height = 1080,
            frameRate = 30f,
            bitrate = 5000000L,
            codec = "h264",
            hasAudio = true,
            audioChannels = 2
        )
        
        coEvery { offlineVideoRepository.insertDownloadTask(any()) } just Runs
        every { workManager.enqueueUniqueWork(any(), any(), any()) } returns mockk()
        
        // When
        val downloadId = downloadManager.queueDownload(videoUri, metadata)
        
        // Then
        assertNotNull(downloadId)
        
        val queue = downloadManager.downloadQueue.first()
        assertEquals(1, queue.size)
        assertEquals(videoUri, queue[0].videoUri)
        assertEquals(DownloadStatus.QUEUED, queue[0].status)
        
        coVerify { offlineVideoRepository.insertDownloadTask(any()) }
        verify { workManager.enqueueUniqueWork(any(), any(), any()) }
    }
    
    @Test
    fun `pauseDownload should cancel work and update status`() = runTest {
        // Given
        val downloadId = UUID.randomUUID().toString()
        val task = DownloadTask(
            id = downloadId,
            videoUri = "https://example.com/video.mp4",
            metadata = createTestMetadata(),
            quality = VideoQuality.HIGH,
            status = DownloadStatus.DOWNLOADING,
            createdAt = System.currentTimeMillis()
        )
        
        // Add task to queue
        downloadManager.queueDownload(task.videoUri, task.metadata)
        
        every { workManager.cancelWorkById(any()) } returns mockk()
        
        // When
        downloadManager.pauseDownload(downloadId)
        
        // Then
        verify { workManager.cancelWorkById(UUID.fromString(downloadId)) }
    }
    
    @Test
    fun `cancelDownload should remove from queue and delete task`() = runTest {
        // Given
        val videoUri = "https://example.com/video.mp4"
        val metadata = createTestMetadata()
        
        coEvery { offlineVideoRepository.insertDownloadTask(any()) } just Runs
        coEvery { offlineVideoRepository.deleteDownloadTask(any()) } just Runs
        every { workManager.enqueueUniqueWork(any(), any(), any()) } returns mockk()
        every { workManager.cancelWorkById(any()) } returns mockk()
        every { context.getExternalFilesDir(null) } returns mockk {
            every { path } returns "/storage/emulated/0/Android/data/com.test/files"
        }
        
        val downloadId = downloadManager.queueDownload(videoUri, metadata)
        
        // When
        downloadManager.cancelDownload(downloadId)
        
        // Then
        val queue = downloadManager.downloadQueue.first()
        assertEquals(0, queue.size)
        
        verify { workManager.cancelWorkById(UUID.fromString(downloadId)) }
        coVerify { offlineVideoRepository.deleteDownloadTask(downloadId) }
    }
    
    @Test
    fun `getStorageInfo should return correct storage information`() = runTest {
        // Given
        every { context.getExternalFilesDir(null) } returns mockk {
            every { totalSpace } returns 1000000000L // 1GB
            every { freeSpace } returns 500000000L // 500MB
        }
        
        // When
        val storageInfo = downloadManager.getStorageInfo()
        
        // Then
        assertEquals(1000000000L, storageInfo.totalSpace)
        assertEquals(500000000L, storageInfo.usedSpace)
        assertEquals(500000000L, storageInfo.freeSpace)
    }
    
    private fun createTestMetadata() = VideoMetadata(
        uri = "test.mp4",
        title = "Test Video",
        duration = 60000L,
        width = 1280,
        height = 720,
        frameRate = 30f,
        bitrate = 2500000L,
        codec = "h264",
        hasAudio = true,
        audioChannels = 2
    )
}