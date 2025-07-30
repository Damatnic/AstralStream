package com.astralplayer.nextplayer.data.gesture

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class GestureAnalyticsTest {

    private lateinit var analytics: GestureAnalytics

    @Before
    fun setup() {
        analytics = GestureAnalytics()
    }

    @Test
    fun `records gesture usage correctly`() = runTest {
        analytics.recordGestureUsage("tap")
        analytics.recordGestureUsage("seek")
        analytics.recordGestureUsage("tap")
        
        val data = analytics.analytics.value
        assertEquals(3, data.totalGestures)
        assertEquals("tap", data.mostUsedGesture)
        assertEquals(2, data.gestureBreakdown["tap"])
        assertEquals(1, data.gestureBreakdown["seek"])
    }

    @Test
    fun `tracks performance metrics`() = runTest {
        analytics.recordPerformanceMetric(60f)
        analytics.recordPerformanceMetric(55f)
        analytics.recordPerformanceMetric(58f)
        
        val data = analytics.analytics.value
        assertEquals(57.67f, data.averagePerformance, 0.1f)
    }

    @Test
    fun `maintains performance history limit`() = runTest {
        repeat(150) { i ->
            analytics.recordPerformanceMetric(i.toFloat())
        }
        
        // Should only keep last 100 entries
        val data = analytics.analytics.value
        assertTrue(data.averagePerformance > 100f) // Should be average of last 100 entries
    }
}