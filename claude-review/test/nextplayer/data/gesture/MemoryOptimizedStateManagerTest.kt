package com.astralplayer.nextplayer.data.gesture

import android.app.ActivityManager
import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.astralplayer.nextplayer.data.GestureType
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryOptimizedStateManagerTest {

    private lateinit var context: Context
    private lateinit var activityManager: ActivityManager
    private lateinit var testScope: TestScope
    private lateinit var memoryManager: MemoryOptimizedStateManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        activityManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        
        testScope = TestScope()
        memoryManager = MemoryOptimizedStateManager(context, testScope)
    }

    @After
    fun tearDown() {
        memoryManager.cleanup()
    }

    @Test
    fun `object pool obtains and recycles objects correctly`() {
        // Test MotionEventWrapper pooling
        val wrapper1 = memoryManager.obtainFromPool(MotionEventWrapper::class)
        val wrapper2 = memoryManager.obtainFromPool(MotionEventWrapper::class)
        
        assertNotNull(wrapper1)
        assertNotNull(wrapper2)
        assertNotSame(wrapper1, wrapper2)
        
        // Return to pool
        memoryManager.returnToPool(wrapper1)
        memoryManager.returnToPool(wrapper2)
        
        // Verify reuse
        val wrapper3 = memoryManager.obtainFromPool(MotionEventWrapper::class)
        assertTrue(wrapper3 === wrapper1 || wrapper3 === wrapper2)
    }

    @Test
    fun `flyweight factory returns same instance for identical configurations`() {
        val config1 = memoryManager.getGestureConfig(
            GestureType.HORIZONTAL_SEEK,
            0.5f,
            10f
        )
        
        val config2 = memoryManager.getGestureConfig(
            GestureType.HORIZONTAL_SEEK,
            0.5f,
            10f
        )
        
        assertSame(config1, config2)
    }

    @Test
    fun `flyweight factory returns different instances for different configurations`() {
        val config1 = memoryManager.getGestureConfig(
            GestureType.HORIZONTAL_SEEK,
            0.5f,
            10f
        )
        
        val config2 = memoryManager.getGestureConfig(
            GestureType.VERTICAL_VOLUME,
            0.5f,
            10f
        )
        
        assertNotSame(config1, config2)
    }

    @Test
    fun `memory pressure handling trims object pools correctly`() {
        // Fill pools with objects
        val wrappers = mutableListOf<MotionEventWrapper>()
        repeat(20) {
            wrappers.add(memoryManager.obtainFromPool(MotionEventWrapper::class))
        }
        
        // Return all to pool
        wrappers.forEach { memoryManager.returnToPool(it) }
        
        val initialStats = memoryManager.getMemoryUsage()
        val initialPooledCount = initialStats.pooledObjectsCount
        
        // Trigger memory pressure
        memoryManager.handleMemoryPressure(MemoryPressureLevel.MODERATE)
        
        val afterPressureStats = memoryManager.getMemoryUsage()
        val afterPooledCount = afterPressureStats.pooledObjectsCount
        
        assertTrue("Pool should be trimmed", afterPooledCount < initialPooledCount)
    }

    @Test
    fun `gesture config flyweight calculates trigger correctly`() = runTest {
        val horizontalConfig = memoryManager.getGestureConfig(
            GestureType.HORIZONTAL_SEEK,
            1.0f,
            20f
        )
        
        assertTrue(horizontalConfig.shouldTrigger(25f, 5f))
        assertFalse(horizontalConfig.shouldTrigger(15f, 5f))
        
        val verticalConfig = memoryManager.getGestureConfig(
            GestureType.VERTICAL_VOLUME,
            1.0f,
            20f
        )
        
        assertTrue(verticalConfig.shouldTrigger(5f, 25f))
        assertFalse(verticalConfig.shouldTrigger(5f, 15f))
    }

    @Test
    fun `gesture config flyweight calculates intensity correctly`() {
        val config = memoryManager.getGestureConfig(
            GestureType.HORIZONTAL_SEEK,
            0.5f,
            10f
        )
        
        val intensity = config.calculateIntensity(40f, 0f)
        assertEquals(1.0f, intensity, 0.001f) // Should be clamped to 1.0
        
        val lowIntensity = config.calculateIntensity(10f, 0f)
        assertEquals(0.5f, lowIntensity, 0.001f)
    }

    @Test
    fun `memory usage stats are calculated correctly`() {
        val stats = memoryManager.getMemoryUsage()
        
        assertTrue(stats.totalMemoryBytes > 0)
        assertTrue(stats.maxMemoryBytes > 0)
        assertTrue(stats.memoryUsagePercentage >= 0f)
        assertTrue(stats.memoryUsagePercentage <= 100f)
        assertNotNull(stats.formattedUsedMemory)
        assertNotNull(stats.formattedTotalMemory)
    }

    @Test
    fun `object pool wrapper classes reset correctly`() {
        val motionWrapper = MotionEventWrapper()
        motionWrapper.timestamp = 12345L
        motionWrapper.reset()
        assertEquals(0L, motionWrapper.timestamp)
        assertNull(motionWrapper.motionEvent)
        
        val gestureWrapper = GestureStateWrapper()
        gestureWrapper.initialize(
            GestureType.DOUBLE_TAP,
            Offset(10f, 20f),
            Offset(30f, 40f),
            12345L
        )
        assertTrue(gestureWrapper.isActive)
        
        gestureWrapper.reset()
        assertFalse(gestureWrapper.isActive)
        assertEquals(GestureType.SINGLE_TAP, gestureWrapper.gestureType)
        assertEquals(Offset.Zero, gestureWrapper.startPosition)
    }

    @Test
    fun `object pool limits size correctly`() {
        val pool = ObjectPool<String>(
            factory = { "test" },
            reset = { },
            maxSize = 2
        )
        
        // Get objects
        val obj1 = pool.obtain()
        val obj2 = pool.obtain()
        val obj3 = pool.obtain()
        
        // Return all to pool
        pool.recycle(obj1)
        pool.recycle(obj2)
        pool.recycle(obj3) // This should be ignored due to max size
        
        assertEquals(2, pool.size())
    }

    @Test
    fun `object pool trim works correctly`() {
        val pool = ObjectPool<String>(
            factory = { "test" },
            reset = { },
            maxSize = 10
        )
        
        // Fill pool
        repeat(5) {
            val obj = pool.obtain()
            pool.recycle(obj)
        }
        
        assertEquals(5, pool.size())
        
        // Trim 50%
        pool.trim(0.5f)
        assertEquals(2, pool.size()) // 5 * 0.5 = 2.5, rounded down to 2
    }

    @Test
    fun `memory pressure monitor detects correct levels`() = runTest {
        val memoryInfo = ActivityManager.MemoryInfo().apply {
            totalMem = 1000L
            availMem = 200L
            lowMemory = false
        }
        
        every { activityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.totalMem = memoryInfo.totalMem
            info.availMem = memoryInfo.availMem
            info.lowMemory = memoryInfo.lowMemory
        }
        
        val monitor = MemoryPressureMonitor(context, testScope)
        
        // Wait for initial check
        testScope.testScheduler.advanceTimeBy(5000)
        
        assertEquals(MemoryPressureLevel.MODERATE, monitor.memoryPressure.value)
    }
}