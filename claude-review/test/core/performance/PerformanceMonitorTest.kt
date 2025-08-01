package com.astralplayer.core.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class PerformanceMonitorTest {
    
    @MockK
    private lateinit var context: Context
    
    @MockK
    private lateinit var activityManager: ActivityManager
    
    @MockK
    private lateinit var memoryInfo: ActivityManager.MemoryInfo
    
    @MockK
    private lateinit var choreographer: Choreographer
    
    @MockK
    private lateinit var handler: Handler
    
    private lateinit var performanceMonitor: PerformanceMonitor
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        mockkStatic(Choreographer::class)
        mockkConstructor(Handler::class)
        
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.availMem = 1000000000L // 1GB
            info.lowMemory = false
        }
        
        every { Choreographer.getInstance() } returns choreographer
        every { choreographer.postFrameCallback(any()) } just Runs
        every { choreographer.removeFrameCallback(any()) } just Runs
        
        every { anyConstructed<Handler>().postDelayed(any(), any()) } returns true
        every { anyConstructed<Handler>().removeCallbacksAndMessages(null) } just Runs
        
        performanceMonitor = PerformanceMonitor(context)
    }
    
    @Test
    fun `startMonitoring should initialize all monitoring systems`() {
        // When
        performanceMonitor.startMonitoring()
        
        // Then
        verify { choreographer.postFrameCallback(any()) }
        verify { anyConstructed<Handler>().postDelayed(any(), any()) }
    }
    
    @Test
    fun `stopMonitoring should cleanup all callbacks`() {
        // Given
        performanceMonitor.startMonitoring()
        
        // When
        performanceMonitor.stopMonitoring()
        
        // Then
        verify { choreographer.removeFrameCallback(any()) }
        verify { anyConstructed<Handler>().removeCallbacksAndMessages(null) }
    }
    
    @Test
    fun `getMemoryInfo should return correct memory metrics`() = runTest {
        // Given
        val runtime = mockk<Runtime>()
        mockkStatic(Runtime::class)
        every { Runtime.getRuntime() } returns runtime
        every { runtime.totalMemory() } returns 200 * 1048576L // 200MB
        every { runtime.freeMemory() } returns 50 * 1048576L // 50MB
        every { runtime.maxMemory() } returns 400 * 1048576L // 400MB
        
        // When
        performanceMonitor.startMonitoring()
        Thread.sleep(100) // Let monitoring update
        
        val state = performanceMonitor.performanceState.first()
        
        // Then
        assertEquals(150, state.memoryUsageMB) // 200 - 50 = 150MB used
        assertEquals(400, state.totalMemoryMB)
    }
    
    @Test
    fun `performance report should calculate correct score`() {
        // Given
        performanceMonitor.startMonitoring()
        
        // When
        val report = performanceMonitor.getPerformanceReport()
        
        // Then
        assertTrue(report.performanceScore in 0..100)
        assertNotNull(report.recommendations)
    }
    
    @Test
    fun `should detect high memory usage and trigger optimization`() = runTest {
        // Given
        val runtime = mockk<Runtime>()
        mockkStatic(Runtime::class)
        every { Runtime.getRuntime() } returns runtime
        every { runtime.totalMemory() } returns 250 * 1048576L // 250MB
        every { runtime.freeMemory() } returns 50 * 1048576L // 50MB
        every { runtime.maxMemory() } returns 300 * 1048576L // 300MB
        
        mockkStatic(System::class)
        every { System.gc() } just Runs
        
        // When
        performanceMonitor.startMonitoring()
        Thread.sleep(6000) // Wait for memory check interval
        
        val state = performanceMonitor.performanceState.first()
        
        // Then
        assertEquals(200, state.memoryUsageMB) // Over threshold
        assertTrue(state.needsMemoryOptimization)
        verify { System.gc() }
    }
    
    @Test
    fun `should generate performance recommendations based on metrics`() {
        // Given
        performanceMonitor.startMonitoring()
        
        // When
        val report = performanceMonitor.getPerformanceReport()
        
        // Then
        assertFalse(report.recommendations.isEmpty())
        if (report.averageFps < 50) {
            assertTrue(report.recommendations.any { it.contains("hardware acceleration") })
        }
    }
}