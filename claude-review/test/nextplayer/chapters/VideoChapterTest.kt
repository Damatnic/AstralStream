package com.astralplayer.nextplayer.chapters

import org.junit.Test
import org.junit.Assert.*

class VideoChapterTest {

    @Test
    fun `test chapter creation with valid data`() {
        val chapter = VideoChapter(
            title = "Introduction",
            startTimeMs = 0L,
            endTimeMs = 30000L,
            description = "Video introduction"
        )

        assertEquals("Introduction", chapter.title)
        assertEquals(0L, chapter.startTimeMs)
        assertEquals(30000L, chapter.endTimeMs)
        assertEquals("Video introduction", chapter.description)
        assertEquals(30000L, chapter.durationMs)
    }

    @Test
    fun `test containsPosition returns true for position within chapter`() {
        val chapter = VideoChapter(
            title = "Chapter 1",
            startTimeMs = 10000L,
            endTimeMs = 50000L
        )

        assertTrue(chapter.containsPosition(10000L)) // start boundary
        assertTrue(chapter.containsPosition(30000L)) // middle
        assertTrue(chapter.containsPosition(50000L)) // end boundary
    }

    @Test
    fun `test containsPosition returns false for position outside chapter`() {
        val chapter = VideoChapter(
            title = "Chapter 1",
            startTimeMs = 10000L,
            endTimeMs = 50000L
        )

        assertFalse(chapter.containsPosition(9999L)) // before start
        assertFalse(chapter.containsPosition(50001L)) // after end
    }

    @Test
    fun `test getFormattedStartTime returns correct format for minutes only`() {
        val chapter = VideoChapter(
            title = "Chapter 1",
            startTimeMs = 90000L, // 1 minute 30 seconds
            endTimeMs = 120000L
        )

        assertEquals("01:30", chapter.getFormattedStartTime())
    }

    @Test
    fun `test getFormattedDuration returns correct format`() {
        val chapter = VideoChapter(
            title = "Chapter 1",
            startTimeMs = 60000L, // 1 minute
            endTimeMs = 150000L   // 2 minutes 30 seconds
        )

        assertEquals("01:30", chapter.getFormattedDuration()) // Duration is 1:30
    }

    @Test
    fun `test formatTime with hours minutes seconds`() {
        val timeMs = 3723000L // 1 hour, 2 minutes, 3 seconds

        assertEquals("01:02:03", VideoChapter.formatTime(timeMs))
    }

    @Test
    fun `test formatTime with minutes seconds only`() {
        val timeMs = 123000L // 2 minutes, 3 seconds

        assertEquals("02:03", VideoChapter.formatTime(timeMs))
    }

    @Test
    fun `test formatTime with zero time`() {
        assertEquals("00:00", VideoChapter.formatTime(0L))
    }

    @Test
    fun `test formatTime with exactly one hour`() {
        val timeMs = 3600000L // 1 hour exactly

        assertEquals("01:00:00", VideoChapter.formatTime(timeMs))
    }

    @Test
    fun `test formatTime rounds down partial seconds`() {
        val timeMs = 1500L // 1.5 seconds

        assertEquals("00:01", VideoChapter.formatTime(timeMs))
    }
}