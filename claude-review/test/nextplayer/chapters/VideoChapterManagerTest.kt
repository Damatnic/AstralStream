package com.astralplayer.nextplayer.chapters

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class VideoChapterManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockExoPlayer: ExoPlayer
    private lateinit var chapterManager: VideoChapterManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockExoPlayer = mockk(relaxed = true)
        chapterManager = VideoChapterManager(mockContext, mockExoPlayer)
    }

    @Test
    fun `test detectChapters generates auto chapters for long videos`() = runTest {
        val duration = 900000L // 15 minutes
        val mediaItem = mockk<MediaItem> {
            every { mediaMetadata } returns mockk {
                every { extras } returns null
            }
            every { localConfiguration } returns mockk {
                every { uri } returns mockk {
                    every { lastPathSegment } returns "video.mp4"
                    every { path } returns "/storage/video.mp4"
                }
            }
        }

        chapterManager.detectChapters(mediaItem, duration)

        // Should generate auto chapters for videos > 5 minutes
        assertTrue("Should generate chapters for long videos", 
            chapterManager.chapters.value.isNotEmpty())
    }

    @Test
    fun `test detectChapters skips auto generation for short videos`() = runTest {
        val duration = 180000L // 3 minutes
        val mediaItem = mockk<MediaItem> {
            every { mediaMetadata } returns mockk {
                every { extras } returns null
            }
            every { localConfiguration } returns mockk {
                every { uri } returns mockk {
                    every { lastPathSegment } returns "short_video.mp4"
                    every { path } returns "/storage/short_video.mp4"
                }
            }
        }

        chapterManager.detectChapters(mediaItem, duration)

        // Should not generate auto chapters for videos <= 5 minutes
        assertTrue("Should not generate chapters for short videos", 
            chapterManager.chapters.value.isEmpty())
    }

    @Test
    fun `test updateCurrentChapter updates index correctly`() = runTest {
        // Setup chapters
        val chapters = listOf(
            VideoChapter("Chapter 1", 0L, 30000L),
            VideoChapter("Chapter 2", 30000L, 60000L),
            VideoChapter("Chapter 3", 60000L, 90000L)
        )
        
        // Manually set chapters for testing
        val field = chapterManager::class.java.getDeclaredField("_chapters")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val chaptersFlow = field.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<List<VideoChapter>>
        chaptersFlow.value = chapters

        // Test position in first chapter
        chapterManager.updateCurrentChapter(15000L)
        assertEquals(0, chapterManager.currentChapterIndex.value)

        // Test position in second chapter
        chapterManager.updateCurrentChapter(45000L)
        assertEquals(1, chapterManager.currentChapterIndex.value)

        // Test position in third chapter
        chapterManager.updateCurrentChapter(75000L)
        assertEquals(2, chapterManager.currentChapterIndex.value)

        // Test position outside chapters
        chapterManager.updateCurrentChapter(100000L)
        assertEquals(-1, chapterManager.currentChapterIndex.value)
    }

    @Test
    fun `test jumpToChapter seeks to correct position`() = runTest {
        // Setup chapters
        val chapters = listOf(
            VideoChapter("Chapter 1", 0L, 30000L),
            VideoChapter("Chapter 2", 30000L, 60000L)
        )
        
        val field = chapterManager::class.java.getDeclaredField("_chapters")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val chaptersFlow = field.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<List<VideoChapter>>
        chaptersFlow.value = chapters

        chapterManager.jumpToChapter(1)

        verify { mockExoPlayer.seekTo(30000L) }
    }

    @Test
    fun `test jumpToChapter ignores invalid index`() = runTest {
        val chapters = listOf(
            VideoChapter("Chapter 1", 0L, 30000L)
        )
        
        val field = chapterManager::class.java.getDeclaredField("_chapters")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val chaptersFlow = field.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<List<VideoChapter>>
        chaptersFlow.value = chapters

        chapterManager.jumpToChapter(-1)
        chapterManager.jumpToChapter(5)

        verify(exactly = 0) { mockExoPlayer.seekTo(any()) }
    }

    @Test
    fun `test nextChapter advances to next chapter`() = runTest {
        val chapters = listOf(
            VideoChapter("Chapter 1", 0L, 30000L),
            VideoChapter("Chapter 2", 30000L, 60000L)
        )
        
        val field1 = chapterManager::class.java.getDeclaredField("_chapters")
        field1.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val chaptersFlow = field1.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<List<VideoChapter>>
        chaptersFlow.value = chapters

        val field2 = chapterManager::class.java.getDeclaredField("_currentChapterIndex")
        field2.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val currentIndexFlow = field2.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<Int>
        currentIndexFlow.value = 0

        chapterManager.nextChapter()

        verify { mockExoPlayer.seekTo(30000L) }
    }

    @Test
    fun `test nextChapter does nothing at last chapter`() = runTest {
        val chapters = listOf(
            VideoChapter("Chapter 1", 0L, 30000L)
        )
        
        val field1 = chapterManager::class.java.getDeclaredField("_chapters")
        field1.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val chaptersFlow = field1.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<List<VideoChapter>>
        chaptersFlow.value = chapters

        val field2 = chapterManager::class.java.getDeclaredField("_currentChapterIndex")
        field2.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val currentIndexFlow = field2.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<Int>
        currentIndexFlow.value = 0

        chapterManager.nextChapter()

        verify(exactly = 0) { mockExoPlayer.seekTo(any()) }
    }

    @Test
    fun `test previousChapter goes to previous chapter`() = runTest {
        val chapters = listOf(
            VideoChapter("Chapter 1", 0L, 30000L),
            VideoChapter("Chapter 2", 30000L, 60000L)
        )
        
        val field1 = chapterManager::class.java.getDeclaredField("_chapters")
        field1.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val chaptersFlow = field1.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<List<VideoChapter>>
        chaptersFlow.value = chapters

        val field2 = chapterManager::class.java.getDeclaredField("_currentChapterIndex")
        field2.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val currentIndexFlow = field2.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<Int>
        currentIndexFlow.value = 1

        every { mockExoPlayer.currentPosition } returns 45000L

        chapterManager.previousChapter()

        verify { mockExoPlayer.seekTo(0L) }
    }

    @Test
    fun `test addCustomChapter adds and sorts chapters`() = runTest {
        val existingChapters = listOf(
            VideoChapter("Chapter 1", 0L, 30000L),
            VideoChapter("Chapter 3", 60000L, 90000L)
        )
        
        val field = chapterManager::class.java.getDeclaredField("_chapters")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val chaptersFlow = field.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<List<VideoChapter>>
        chaptersFlow.value = existingChapters

        chapterManager.addCustomChapter("Chapter 2", 30000L, 60000L)

        val updatedChapters = chapterManager.chapters.value
        assertEquals(3, updatedChapters.size)
        assertEquals("Chapter 1", updatedChapters[0].title)
        assertEquals("Chapter 2", updatedChapters[1].title)
        assertEquals("Chapter 3", updatedChapters[2].title)
    }

    @Test
    fun `test removeChapter removes correct chapter`() = runTest {
        val chapters = listOf(
            VideoChapter("Chapter 1", 0L, 30000L),
            VideoChapter("Chapter 2", 30000L, 60000L),
            VideoChapter("Chapter 3", 60000L, 90000L)
        )
        
        val field = chapterManager::class.java.getDeclaredField("_chapters")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val chaptersFlow = field.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<List<VideoChapter>>
        chaptersFlow.value = chapters

        chapterManager.removeChapter(1)

        val updatedChapters = chapterManager.chapters.value
        assertEquals(2, updatedChapters.size)
        assertEquals("Chapter 1", updatedChapters[0].title)
        assertEquals("Chapter 3", updatedChapters[1].title)
    }

    @Test
    fun `test removeChapter ignores invalid index`() = runTest {
        val chapters = listOf(
            VideoChapter("Chapter 1", 0L, 30000L)
        )
        
        val field = chapterManager::class.java.getDeclaredField("_chapters")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val chaptersFlow = field.get(chapterManager) as kotlinx.coroutines.flow.MutableStateFlow<List<VideoChapter>>
        chaptersFlow.value = chapters

        chapterManager.removeChapter(-1)
        chapterManager.removeChapter(5)

        assertEquals(1, chapterManager.chapters.value.size)
    }
}