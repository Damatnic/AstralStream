package com.astralplayer.features.subtitle

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astralplayer.astralstream.data.database.AstralStreamDatabase
import com.astralplayer.astralstream.data.dao.SubtitleCacheDao
import com.astralplayer.astralstream.data.entity.CachedSubtitleEntity
import com.astralplayer.astralstream.data.entity.CompressionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SubtitleCacheManagerTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockEncryptionManager: EncryptionManager
    
    private lateinit var database: AstralStreamDatabase
    private lateinit var subtitleCacheDao: SubtitleCacheDao
    private lateinit var subtitleCacheManager: SubtitleCacheManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AstralStreamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        subtitleCacheDao = database.subtitleCacheDao()
        
        // Mock encryption manager
        whenever(mockEncryptionManager.encrypt(any())).thenReturn("encrypted_content")
        whenever(mockEncryptionManager.decrypt(any())).thenReturn("original_content")
        
        subtitleCacheManager = SubtitleCacheManager(
            context = context,
            subtitleCacheDao = subtitleCacheDao,
            encryptionManager = mockEncryptionManager
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `cache subtitle stores encrypted content correctly`() = runBlocking {
        val videoId = "test_video_1"
        val language = "en"
        val content = "Test subtitle content"
        
        val result = subtitleCacheManager.cacheSubtitle(
            videoId = videoId,
            language = language,
            content = content,
            format = "srt",
            quality = 95.5f
        )
        
        assertTrue(result.isSuccess)
        val cachedId = result.getOrNull()
        assertNotNull(cachedId)
        
        // Verify content was encrypted
        verify(mockEncryptionManager).encrypt(content)
        
        // Verify stored in database
        val cachedEntity = subtitleCacheDao.getCachedSubtitle(videoId, language)
        assertNotNull(cachedEntity)
        assertEquals(videoId, cachedEntity.videoId)
        assertEquals(language, cachedEntity.language)
        assertEquals("encrypted_content", cachedEntity.content)
        assertTrue(cachedEntity.isEncrypted)
    }

    @Test
    fun `retrieve cached subtitle decrypts content correctly`() = runBlocking {
        // First cache a subtitle
        val videoId = "test_video_2"
        val language = "es"
        val originalContent = "Original subtitle content"
        
        subtitleCacheManager.cacheSubtitle(
            videoId = videoId,
            language = language,
            content = originalContent,
            format = "srt"
        )
        
        // Now retrieve it
        val result = subtitleCacheManager.getCachedSubtitle(videoId, language)
        
        assertTrue(result.isSuccess)
        val retrievedContent = result.getOrNull()
        assertEquals("original_content", retrievedContent)
        
        // Verify content was decrypted
        verify(mockEncryptionManager).decrypt("encrypted_content")
    }

    @Test
    fun `cache with compression reduces file size`() = runBlocking {
        val videoId = "test_video_3"
        val language = "en"
        val content = "A".repeat(1000) // Large content for compression
        
        whenever(mockEncryptionManager.encrypt(any())).thenReturn("compressed_encrypted_content")
        
        val result = subtitleCacheManager.cacheSubtitle(
            videoId = videoId,
            language = language,
            content = content,
            format = "srt",
            enableCompression = true
        )
        
        assertTrue(result.isSuccess)
        
        val cachedEntity = subtitleCacheDao.getCachedSubtitle(videoId, language)
        assertNotNull(cachedEntity)
        assertEquals(CompressionType.GZIP, cachedEntity.compressionType)
        assertTrue(cachedEntity.compressedSize < cachedEntity.originalSize)
    }

    @Test
    fun `LRU eviction removes oldest entries when cache limit exceeded`() = runBlocking {
        // Mock cache size check to simulate full cache
        val cacheManager = spy(subtitleCacheManager)
        doReturn(95L * 1024 * 1024).whenever(cacheManager).getCurrentCacheSize() // 95MB
        
        // Cache multiple subtitles
        repeat(5) { index ->
            cacheManager.cacheSubtitle(
                videoId = "video_$index",
                language = "en",
                content = "Content $index",
                format = "srt"
            )
            Thread.sleep(10) // Ensure different timestamps
        }
        
        // Cache one more to trigger eviction
        cacheManager.cacheSubtitle(
            videoId = "video_new",
            language = "en", 
            content = "New content",
            format = "srt"
        )
        
        // Should have evicted oldest entries
        val remainingEntries = subtitleCacheDao.getAllCachedSubtitles()
        assertTrue(remainingEntries.size <= 5)
        
        // Newest entry should still exist
        val newestEntry = subtitleCacheDao.getCachedSubtitle("video_new", "en")
        assertNotNull(newestEntry)
    }

    @Test
    fun `cache statistics are tracked correctly`() = runBlocking {
        val videoId = "stats_test_video"
        val language = "en"
        
        // Cache a subtitle
        subtitleCacheManager.cacheSubtitle(
            videoId = videoId,
            language = language,
            content = "Test content",
            format = "srt"
        )
        
        // Access it multiple times
        repeat(3) {
            subtitleCacheManager.getCachedSubtitle(videoId, language)
        }
        
        val stats = subtitleCacheManager.getCacheStatistics()
        assertTrue(stats.totalEntries > 0)
        assertTrue(stats.totalSize > 0)
        assertTrue(stats.hitRate >= 0.0)
        assertTrue(stats.compressionRatio >= 0.0)
    }

    @Test
    fun `cleanup removes expired entries`() = runBlocking {
        // Cache subtitle with short TTL
        val videoId = "expire_test_video"
        val language = "en"
        
        subtitleCacheManager.cacheSubtitle(
            videoId = videoId,
            language = language,
            content = "Expiring content",
            format = "srt",
            ttlHours = 0 // Immediate expiry
        )
        
        // Wait a moment and run cleanup
        Thread.sleep(100)
        subtitleCacheManager.cleanupExpiredEntries()
        
        // Entry should be removed
        val result = subtitleCacheManager.getCachedSubtitle(videoId, language)
        assertTrue(result.isFailure)
    }

    @Test
    fun `cache handles invalid video ID gracefully`() = runBlocking {
        val result = subtitleCacheManager.cacheSubtitle(
            videoId = "", // Invalid empty ID
            language = "en",
            content = "Test content",
            format = "srt"
        )
        
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("Invalid") == true)
    }

    @Test
    fun `cache supports multiple languages for same video`() = runBlocking {
        val videoId = "multilang_video"
        val languages = listOf("en", "es", "fr", "de")
        
        // Cache subtitles in different languages
        languages.forEach { language ->
            val result = subtitleCacheManager.cacheSubtitle(
                videoId = videoId,
                language = language,
                content = "Content in $language",
                format = "srt"
            )
            assertTrue(result.isSuccess)
        }
        
        // Verify all languages are cached
        languages.forEach { language ->
            val result = subtitleCacheManager.getCachedSubtitle(videoId, language)
            assertTrue(result.isSuccess)
            assertEquals("original_content", result.getOrNull())
        }
        
        val cachedLanguages = subtitleCacheManager.getCachedLanguages(videoId)
        assertEquals(languages.size, cachedLanguages.size)
        assertTrue(cachedLanguages.containsAll(languages))
    }

    @Test
    fun `performance metrics are tracked during operations`() = runBlocking {
        val videoId = "perf_test_video"
        val language = "en"
        val content = "Performance test content"
        
        // Cache subtitle and measure performance
        val cacheStartTime = System.currentTimeMillis()
        val cacheResult = subtitleCacheManager.cacheSubtitle(
            videoId = videoId,
            language = language,
            content = content,
            format = "srt"
        )
        val cacheEndTime = System.currentTimeMillis()
        
        assertTrue(cacheResult.isSuccess)
        assertTrue((cacheEndTime - cacheStartTime) < 1000) // Should be fast
        
        // Retrieve subtitle and measure performance
        val retrieveStartTime = System.currentTimeMillis()
        val retrieveResult = subtitleCacheManager.getCachedSubtitle(videoId, language)
        val retrieveEndTime = System.currentTimeMillis()
        
        assertTrue(retrieveResult.isSuccess)
        assertTrue((retrieveEndTime - retrieveStartTime) < 500) // Should be very fast
        
        // Verify performance stats
        val cachedEntity = subtitleCacheDao.getCachedSubtitle(videoId, language)
        assertNotNull(cachedEntity)
        assertTrue(cachedEntity.cacheTime > 0)
        assertTrue(cachedEntity.accessCount > 0)
    }
}