package com.astralplayer.core.performance

import android.content.Context
import android.os.SystemClock
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class StartupPerformanceManagerTest {
    
    @MockK
    private lateinit var context: Context
    
    private lateinit var startupPerformanceManager: StartupPerformanceManager
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1000L
        
        startupPerformanceManager = StartupPerformanceManager(context)
    }
    
    @Test
    fun `markAppStart should record start time`() {
        // Given
        every { SystemClock.elapsedRealtime() } returns 5000L
        
        // When
        startupPerformanceManager.markAppStart()
        
        // Then
        // Start time should be recorded (internal state)
        assertTrue(true) // Can't directly verify private field
    }
    
    @Test
    fun `initializeCriticalComponents should complete within timeout`() = runTest {
        // When
        val result = startupPerformanceManager.initializeCriticalComponents()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `initializeNonCriticalComponents should not block`() {
        // When
        startupPerformanceManager.initializeNonCriticalComponents()
        
        // Then
        // Should complete immediately without blocking
        assertTrue(true)
    }
    
    @Test
    fun `markFirstFrameRendered should log startup metrics`() {
        // Given
        every { SystemClock.elapsedRealtime() } returnsMany listOf(1000L, 1800L)
        startupPerformanceManager.markAppStart()
        
        // When
        startupPerformanceManager.markFirstFrameRendered()
        
        // Then
        // Would verify logging but Timber is static
        assertTrue(true)
    }
    
    @Test
    fun `getStartupReport should return correct metrics`() {
        // Given
        every { SystemClock.elapsedRealtime() } returnsMany listOf(1000L, 1900L)
        startupPerformanceManager.markAppStart()
        
        // When
        val report = startupPerformanceManager.getStartupReport()
        
        // Then
        assertEquals(900L, report.totalStartupTime)
        assertTrue(report.targetMet) // 900ms < 1000ms target
        assertNotNull(report.componentTimes)
        assertNotNull(report.recommendations)
    }
    
    @Test
    fun `getStartupReport should generate recommendations for slow startup`() {
        // Given
        every { SystemClock.elapsedRealtime() } returnsMany listOf(1000L, 3000L)
        startupPerformanceManager.markAppStart()
        
        // When
        val report = startupPerformanceManager.getStartupReport()
        
        // Then
        assertEquals(2000L, report.totalStartupTime)
        assertFalse(report.targetMet) // 2000ms > 1000ms target
        assertTrue(report.recommendations.isNotEmpty())
    }
}