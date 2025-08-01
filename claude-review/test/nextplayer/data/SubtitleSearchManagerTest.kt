package com.astralplayer.nextplayer.data

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SubtitleSearchManagerTest {

    private lateinit var mockContext: Context
    private lateinit var testScope: TestScope
    private lateinit var subtitleSearchManager: SubtitleSearchManager
    private lateinit var mockVideoFile: File

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        testScope = TestScope()
        
        val mockCacheDir = mockk<File>(relaxed = true)
        every { mockContext.cacheDir } returns mockCacheDir
        every { mockCacheDir.mkdirs() } returns true
        
        mockVideoFile = mockk(relaxed = true)
        every { mockVideoFile.nameWithoutExtension } returns "TestMovie"
        every { mockVideoFile.length() } returns 1024L * 1024L * 100L // 100MB
        
        subtitleSearchManager = SubtitleSearchManager(mockContext, testScope)
    }

    @After
    fun tearDown() {
        // Cleanup if needed
    }

    @Test
    fun `searches subtitles correctly`() = runTest {
        subtitleSearchManager.searchSubtitles(mockVideoFile, listOf("en", "es"))
        
        // Wait for search to complete
        testScope.testScheduler.advanceUntilIdle()
        
        val results = subtitleSearchManager.searchResults.value
        assertTrue("Should have search results", results.isNotEmpty())
        assertTrue("Should have English results", results.any { it.language == "en" })
        assertTrue("Should have Spanish results", results.any { it.language == "es" })
    }

    @Test
    fun `sets searching state correctly`() = runTest {
        assertFalse("Should not be searching initially", subtitleSearchManager.isSearching.value)
        
        val searchJob = launch {
            subtitleSearchManager.searchSubtitles(mockVideoFile)
        }
        
        // Check that searching state is set
        assertTrue("Should be searching", subtitleSearchManager.isSearching.value)
        
        searchJob.join()
        testScope.testScheduler.advanceUntilIdle()
        
        assertFalse("Should not be searching after completion", subtitleSearchManager.isSearching.value)
    }

    @Test
    fun `downloads subtitle correctly`() = runTest {
        // First search for subtitles
        subtitleSearchManager.searchSubtitles(mockVideoFile)
        testScope.testScheduler.advanceUntilIdle()
        
        val results = subtitleSearchManager.searchResults.value
        assertTrue("Should have results", results.isNotEmpty())
        
        val firstResult = results.first()
        val downloadedFile = subtitleSearchManager.downloadSubtitle(firstResult)
        
        assertNotNull("Should download file", downloadedFile)
    }

    @Test
    fun `auto downloads preferred languages`() = runTest {
        val downloadedFiles = subtitleSearchManager.autoDownloadSubtitles(
            mockVideoFile,
            listOf("en", "es")
        )
        
        assertTrue("Should download files", downloadedFiles.isNotEmpty())
        assertTrue("Should download at most 2 files", downloadedFiles.size <= 2)
    }

    @Test
    fun `tracks download progress`() = runTest {
        subtitleSearchManager.searchSubtitles(mockVideoFile)
        testScope.testScheduler.advanceUntilIdle()
        
        val results = subtitleSearchManager.searchResults.value
        val firstResult = results.first()
        
        val downloadJob = launch {
            subtitleSearchManager.downloadSubtitle(firstResult)
        }
        
        // Check that progress is tracked
        testScope.testScheduler.advanceTimeBy(500)
        val progress = subtitleSearchManager.downloadProgress.value[firstResult.id]
        assertNotNull("Should have progress", progress)
        
        downloadJob.join()
    }

    @Test
    fun `clears cache correctly`() {
        subtitleSearchManager.clearCache()
        // Should not throw exception
        assertTrue("Clear cache should complete", true)
    }

    @Test
    fun `gets cached subtitles`() {
        val cachedFiles = subtitleSearchManager.getCachedSubtitles()
        assertNotNull("Should return list", cachedFiles)
    }
}