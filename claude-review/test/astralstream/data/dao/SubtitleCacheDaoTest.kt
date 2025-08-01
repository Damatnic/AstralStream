package com.astralplayer.astralstream.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astralplayer.astralstream.data.database.AstralStreamDatabase
import com.astralplayer.astralstream.data.entity.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class SubtitleCacheDaoTest {

    private lateinit var database: AstralStreamDatabase
    private lateinit var cacheDao: SubtitleCacheDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AstralStreamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cacheDao = database.subtitleCacheDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert should add subtitle to database`() = runTest {
        // Given
        val subtitle = createTestSubtitle()

        // When
        val id = cacheDao.insert(subtitle)

        // Then
        assertTrue(id > 0)
        val retrieved = cacheDao.getById(subtitle.id)
        assertNotNull(retrieved)
        assertEquals(subtitle.videoId, retrieved?.videoId)
        assertEquals(subtitle.language, retrieved?.language)
    }

    @Test
    fun `insert with OnConflictStrategy REPLACE should update existing entry`() = runTest {
        // Given
        val originalSubtitle = createTestSubtitle()
        val updatedSubtitle = originalSubtitle.copy(
            content = "Updated content",
            confidence = 0.95f
        )

        // When
        cacheDao.insert(originalSubtitle)
        cacheDao.insert(updatedSubtitle) // Should replace due to REPLACE strategy

        // Then
        val retrieved = cacheDao.getById(originalSubtitle.id)
        assertEquals("Updated content", retrieved?.content)
        assertEquals(0.95f, retrieved?.confidence)
    }

    @Test
    fun `getSubtitle should retrieve by videoId and language`() = runTest {
        // Given
        val subtitle1 = createTestSubtitle(videoId = "video1", language = "English")
        val subtitle2 = createTestSubtitle(videoId = "video1", language = "Spanish")
        val subtitle3 = createTestSubtitle(videoId = "video2", language = "English")

        cacheDao.insertAll(listOf(subtitle1, subtitle2, subtitle3))

        // When
        val result = cacheDao.getSubtitle("video1", "English")

        // Then
        assertNotNull(result)
        assertEquals("video1", result?.videoId)
        assertEquals("English", result?.language)
    }

    @Test
    fun `getSubtitle should return null for non-existent combination`() = runTest {
        // Given
        val subtitle = createTestSubtitle(videoId = "video1", language = "English")
        cacheDao.insert(subtitle)

        // When
        val result = cacheDao.getSubtitle("video1", "French")

        // Then
        assertNull(result)
    }

    @Test
    fun `getSubtitleByCode should retrieve by videoId and languageCode`() = runTest {
        // Given
        val subtitle = createTestSubtitle(
            videoId = "video1", 
            language = "German",
            languageCode = "de"
        )
        cacheDao.insert(subtitle)

        // When
        val result = cacheDao.getSubtitleByCode("video1", "de")

        // Then
        assertNotNull(result)
        assertEquals("video1", result?.videoId)
        assertEquals("de", result?.languageCode)
    }

    @Test
    fun `updateAccess should increment access count and update timestamp`() = runTest {
        // Given
        val subtitle = createTestSubtitle()
        cacheDao.insert(subtitle)
        val newTimestamp = System.currentTimeMillis()

        // When
        cacheDao.updateAccess(subtitle.id, newTimestamp)

        // Then
        val updated = cacheDao.getById(subtitle.id)
        assertEquals(1, updated?.accessCount) // Should be incremented from 0
        assertEquals(newTimestamp, updated?.lastAccessTime)
    }

    @Test
    fun `getAvailableLanguages should return distinct languages for video`() = runTest {
        // Given
        val videoId = "multilang_video"
        val subtitles = listOf(
            createTestSubtitle(videoId = videoId, language = "English"),
            createTestSubtitle(videoId = videoId, language = "Spanish"),
            createTestSubtitle(videoId = videoId, language = "French"),
            createTestSubtitle(videoId = "other_video", language = "German") // Different video
        )
        cacheDao.insertAll(subtitles)

        // When
        val languages = cacheDao.getAvailableLanguages(videoId).first()

        // Then
        assertEquals(3, languages.size)
        assertTrue(languages.contains("English"))
        assertTrue(languages.contains("Spanish"))
        assertTrue(languages.contains("French"))
        assertFalse(languages.contains("German")) // Should not include other video's languages
    }

    @Test
    fun `getAvailableLanguageCodes should return distinct language codes for video`() = runTest {
        // Given
        val videoId = "multilang_video"
        val subtitles = listOf(
            createTestSubtitle(videoId = videoId, languageCode = "en"),
            createTestSubtitle(videoId = videoId, languageCode = "es"),
            createTestSubtitle(videoId = videoId, languageCode = "fr")
        )
        cacheDao.insertAll(subtitles)

        // When
        val languageCodes = cacheDao.getAvailableLanguageCodes(videoId).first()

        // Then
        assertEquals(3, languageCodes.size)
        assertTrue(languageCodes.containsAll(listOf("en", "es", "fr")))
    }

    @Test
    fun `getTotalCacheSize should sum all file sizes`() = runTest {
        // Given
        val subtitles = listOf(
            createTestSubtitle(fileSize = 1000L),
            createTestSubtitle(fileSize = 2000L),
            createTestSubtitle(fileSize = 3000L)
        )
        cacheDao.insertAll(subtitles)

        // When
        val totalSize = cacheDao.getTotalCacheSize()

        // Then
        assertEquals(6000L, totalSize)
    }

    @Test
    fun `getCacheCount should return correct number of entries`() = runTest {
        // Given
        val subtitles = (1..5).map { createTestSubtitle(videoId = "video_$it") }
        cacheDao.insertAll(subtitles)

        // When
        val count = cacheDao.getCacheCount()

        // Then
        assertEquals(5, count)
    }

    @Test
    fun `getLeastRecentlyUsed should return entries ordered by access time`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val subtitles = listOf(
            createTestSubtitle(id = "newest", lastAccessTime = now),
            createTestSubtitle(id = "oldest", lastAccessTime = now - 10000),
            createTestSubtitle(id = "middle", lastAccessTime = now - 5000)
        )
        cacheDao.insertAll(subtitles)

        // When
        val lruEntries = cacheDao.getLeastRecentlyUsed(2)

        // Then
        assertEquals(2, lruEntries.size)
        assertEquals("oldest", lruEntries[0].id)
        assertEquals("middle", lruEntries[1].id)
    }

    @Test
    fun `deleteOlderThan should remove entries created before cutoff time`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val cutoffTime = now - 5000
        val subtitles = listOf(
            createTestSubtitle(id = "old1", createdTime = now - 10000),
            createTestSubtitle(id = "old2", createdTime = now - 8000),
            createTestSubtitle(id = "new1", createdTime = now - 2000),
            createTestSubtitle(id = "new2", createdTime = now)
        )
        cacheDao.insertAll(subtitles)

        // When
        val deletedCount = cacheDao.deleteOlderThan(cutoffTime)

        // Then
        assertEquals(2, deletedCount) // Should delete old1 and old2
        assertEquals(2, cacheDao.getCacheCount()) // Should have 2 remaining

        // Verify the correct entries remain
        assertNotNull(cacheDao.getById("new1"))
        assertNotNull(cacheDao.getById("new2"))
        assertNull(cacheDao.getById("old1"))
        assertNull(cacheDao.getById("old2"))
    }

    @Test
    fun `deleteNotAccessedSince should remove entries not accessed since cutoff`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val cutoffTime = now - 5000
        val subtitles = listOf(
            createTestSubtitle(id = "stale1", lastAccessTime = now - 10000),
            createTestSubtitle(id = "stale2", lastAccessTime = now - 8000),
            createTestSubtitle(id = "recent1", lastAccessTime = now - 2000),
            createTestSubtitle(id = "recent2", lastAccessTime = now)
        )
        cacheDao.insertAll(subtitles)

        // When
        val deletedCount = cacheDao.deleteNotAccessedSince(cutoffTime)

        // Then
        assertEquals(2, deletedCount)
        assertEquals(2, cacheDao.getCacheCount())
    }

    @Test
    fun `deleteLeastRecentlyUsed should remove specified number of LRU entries`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val subtitles = (1..5).map { index ->
            createTestSubtitle(
                id = "entry_$index",
                lastAccessTime = now - (index * 1000L) // Oldest has highest index
            )
        }
        cacheDao.insertAll(subtitles)

        // When
        val deletedCount = cacheDao.deleteLeastRecentlyUsed(2)

        // Then
        assertEquals(2, deletedCount)
        assertEquals(3, cacheDao.getCacheCount()) // 5 - 2 = 3 remaining

        // Verify the oldest entries were deleted
        assertNull(cacheDao.getById("entry_5")) // Oldest
        assertNull(cacheDao.getById("entry_4")) // Second oldest
        assertNotNull(cacheDao.getById("entry_1")) // Most recent should remain
    }

    @Test
    fun `search should find entries by video title and language`() = runTest {
        // Given
        val subtitles = listOf(
            createTestSubtitle(
                videoTitle = "Action Movie Thriller",
                language = "English"
            ),
            createTestSubtitle(
                videoTitle = "Romantic Comedy",
                language = "Spanish"
            ),
            createTestSubtitle(
                videoTitle = "Horror Film",
                language = "English Action" // Contains "Action" in language
            )
        )
        cacheDao.insertAll(subtitles)

        // When
        val actionResults = cacheDao.search("Action")
        val englishResults = cacheDao.search("English")

        // Then
        assertEquals(2, actionResults.size) // Should find both entries containing "Action"
        assertEquals(2, englishResults.size) // Should find both entries containing "English"
    }

    @Test
    fun `getBySource should filter entries by source type`() = runTest {
        // Given
        val subtitles = listOf(
            createTestSubtitle(source = SubtitleSource.AI_GENERATED),
            createTestSubtitle(source = SubtitleSource.AI_GENERATED),
            createTestSubtitle(source = SubtitleSource.USER_UPLOADED),
            createTestSubtitle(source = SubtitleSource.COMMUNITY_CONTRIBUTED)
        )
        cacheDao.insertAll(subtitles)

        // When
        val aiGenerated = cacheDao.getBySource(SubtitleSource.AI_GENERATED)
        val userUploaded = cacheDao.getBySource(SubtitleSource.USER_UPLOADED)

        // Then
        assertEquals(2, aiGenerated.size)
        assertEquals(1, userUploaded.size)
        assertTrue(aiGenerated.all { it.source == SubtitleSource.AI_GENERATED })
        assertTrue(userUploaded.all { it.source == SubtitleSource.USER_UPLOADED })
    }

    @Test
    fun `getByFormat should filter entries by format type`() = runTest {
        // Given
        val subtitles = listOf(
            createTestSubtitle(format = SubtitleFormat.SRT),
            createTestSubtitle(format = SubtitleFormat.VTT),
            createTestSubtitle(format = SubtitleFormat.SRT),
            createTestSubtitle(format = SubtitleFormat.ASS)
        )
        cacheDao.insertAll(subtitles)

        // When
        val srtSubtitles = cacheDao.getByFormat(SubtitleFormat.SRT)
        val vttSubtitles = cacheDao.getByFormat(SubtitleFormat.VTT)

        // Then
        assertEquals(2, srtSubtitles.size)
        assertEquals(1, vttSubtitles.size)
        assertTrue(srtSubtitles.all { it.format == SubtitleFormat.SRT })
        assertTrue(vttSubtitles.all { it.format == SubtitleFormat.VTT })
    }

    @Test
    fun `getByMinimumConfidence should filter by confidence threshold`() = runTest {
        // Given
        val subtitles = listOf(
            createTestSubtitle(confidence = 0.95f),
            createTestSubtitle(confidence = 0.85f),
            createTestSubtitle(confidence = 0.75f),
            createTestSubtitle(confidence = 0.65f)
        )
        cacheDao.insertAll(subtitles)

        // When
        val highConfidence = cacheDao.getByMinimumConfidence(0.8f)

        // Then
        assertEquals(2, highConfidence.size) // Should include 0.95 and 0.85
        assertTrue(highConfidence.all { it.confidence >= 0.8f })
        // Should be ordered by confidence DESC
        assertEquals(0.95f, highConfidence[0].confidence)
        assertEquals(0.85f, highConfidence[1].confidence)
    }

    @Test
    fun `getLanguageStatistics should return language usage stats`() = runTest {
        // Given
        val subtitles = listOf(
            createTestSubtitle(language = "English"),
            createTestSubtitle(language = "English"),
            createTestSubtitle(language = "Spanish"),
            createTestSubtitle(language = "French"),
            createTestSubtitle(language = "English") // 3 English total
        )
        cacheDao.insertAll(subtitles)

        // When
        val stats = cacheDao.getLanguageStatistics()

        // Then
        assertEquals(3, stats.size) // 3 distinct languages
        
        val englishStats = stats.find { it.language == "English" }
        assertNotNull(englishStats)
        assertEquals(3, englishStats?.count) // Should be ordered by count DESC
        
        val spanishStats = stats.find { it.language == "Spanish" }
        assertEquals(1, spanishStats?.count)
    }

    @Test
    fun `getSourceStatistics should return source usage and confidence stats`() = runTest {
        // Given
        val subtitles = listOf(
            createTestSubtitle(source = SubtitleSource.AI_GENERATED, confidence = 0.9f),
            createTestSubtitle(source = SubtitleSource.AI_GENERATED, confidence = 0.8f),
            createTestSubtitle(source = SubtitleSource.USER_UPLOADED, confidence = 1.0f)
        )
        cacheDao.insertAll(subtitles)

        // When
        val stats = cacheDao.getSourceStatistics()

        // Then
        assertEquals(2, stats.size) // 2 distinct sources
        
        val aiStats = stats.find { it.source == SubtitleSource.AI_GENERATED }
        assertNotNull(aiStats)
        assertEquals(2, aiStats?.count)
        assertEquals(0.85f, aiStats?.avgConfidence, 0.01f) // (0.9 + 0.8) / 2
        
        val userStats = stats.find { it.source == SubtitleSource.USER_UPLOADED }
        assertEquals(1, userStats?.count)
        assertEquals(1.0f, userStats?.avgConfidence, 0.01f)
    }

    @Test
    fun `findDuplicates should return entries with same checksum`() = runTest {
        // Given
        val duplicateChecksum = "duplicate_checksum_123"
        val subtitles = listOf(
            createTestSubtitle(id = "original", checksum = duplicateChecksum),
            createTestSubtitle(id = "duplicate1", checksum = duplicateChecksum),
            createTestSubtitle(id = "unique", checksum = "unique_checksum")
        )
        cacheDao.insertAll(subtitles)

        // When
        val duplicates = cacheDao.findDuplicates(duplicateChecksum)

        // Then
        assertEquals(2, duplicates.size)
        val ids = duplicates.map { it.id }
        assertTrue(ids.contains("original"))
        assertTrue(ids.contains("duplicate1"))
        assertFalse(ids.contains("unique"))
    }

    @Test
    fun `updateQualityScore should update specific entry's quality score`() = runTest {
        // Given
        val subtitle = createTestSubtitle(qualityScore = 0.5f)
        cacheDao.insert(subtitle)

        // When
        cacheDao.updateQualityScore(subtitle.id, 0.9f)

        // Then
        val updated = cacheDao.getById(subtitle.id)
        assertEquals(0.9f, updated?.qualityScore)
    }

    @Test
    fun `getByDateRange should return entries within specified time range`() = runTest {
        // Given
        val baseTime = System.currentTimeMillis()
        val subtitles = listOf(
            createTestSubtitle(id = "before", createdTime = baseTime - 10000),
            createTestSubtitle(id = "during1", createdTime = baseTime),
            createTestSubtitle(id = "during2", createdTime = baseTime + 5000),
            createTestSubtitle(id = "after", createdTime = baseTime + 15000)
        )
        cacheDao.insertAll(subtitles)

        // When
        val rangeResults = cacheDao.getByDateRange(
            startTime = baseTime - 1000,
            endTime = baseTime + 10000
        )

        // Then
        assertEquals(2, rangeResults.size)
        val ids = rangeResults.map { it.id }
        assertTrue(ids.contains("during1"))
        assertTrue(ids.contains("during2"))
        assertFalse(ids.contains("before"))
        assertFalse(ids.contains("after"))
    }

    @Test
    fun `getAllForExport should return all entries ordered by creation time`() = runTest {
        // Given
        val baseTime = System.currentTimeMillis()
        val subtitles = listOf(
            createTestSubtitle(id = "third", createdTime = baseTime + 2000),
            createTestSubtitle(id = "first", createdTime = baseTime),
            createTestSubtitle(id = "second", createdTime = baseTime + 1000)
        )
        cacheDao.insertAll(subtitles)

        // When
        val allEntries = cacheDao.getAllForExport()

        // Then
        assertEquals(3, allEntries.size)
        // Should be ordered by createdTime DESC (newest first)
        assertEquals("third", allEntries[0].id)
        assertEquals("second", allEntries[1].id)
        assertEquals("first", allEntries[2].id)
    }

    @Test
    fun `delete should remove specific entry`() = runTest {
        // Given
        val subtitle = createTestSubtitle()
        cacheDao.insert(subtitle)
        
        // Verify it exists
        assertNotNull(cacheDao.getById(subtitle.id))

        // When
        cacheDao.delete(subtitle)

        // Then
        assertNull(cacheDao.getById(subtitle.id))
        assertEquals(0, cacheDao.getCacheCount())
    }

    @Test
    fun `deleteById should remove entry by ID`() = runTest {
        // Given
        val subtitle = createTestSubtitle()
        cacheDao.insert(subtitle)

        // When
        cacheDao.deleteById(subtitle.id)

        // Then
        assertNull(cacheDao.getById(subtitle.id))
    }

    @Test
    fun `deleteAll should remove all entries`() = runTest {
        // Given
        val subtitles = (1..5).map { createTestSubtitle(videoId = "video_$it") }
        cacheDao.insertAll(subtitles)
        assertEquals(5, cacheDao.getCacheCount())

        // When
        cacheDao.deleteAll()

        // Then
        assertEquals(0, cacheDao.getCacheCount())
    }

    // Helper function to create test subtitle entities
    private fun createTestSubtitle(
        id: String = "test_id_${System.currentTimeMillis()}_${Math.random()}",
        videoId: String = "test_video",
        videoUrl: String = "file:///test.mp4",
        videoTitle: String = "Test Video",
        language: String = "English",
        languageCode: String = "en",
        content: String = "Test subtitle content",
        format: SubtitleFormat = SubtitleFormat.SRT,
        source: SubtitleSource = SubtitleSource.AI_GENERATED,
        confidence: Float = 0.9f,
        fileSize: Long = 100L,
        checksum: String = "test_checksum_${Math.random()}",
        createdTime: Long = System.currentTimeMillis(),
        lastAccessTime: Long = System.currentTimeMillis(),
        qualityScore: Float = 0.8f
    ): CachedSubtitleEntity {
        return CachedSubtitleEntity(
            id = id,
            videoId = videoId,
            videoUrl = videoUrl,
            videoTitle = videoTitle,
            language = language,
            languageCode = languageCode,
            content = content,
            format = format,
            source = source,
            confidence = confidence,
            fileSize = fileSize,
            checksum = checksum,
            createdTime = createdTime,
            lastAccessTime = lastAccessTime,
            qualityScore = qualityScore
        )
    }
}