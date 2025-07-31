package com.astralplayer.nextplayer

import android.content.Context
import com.astralplayer.nextplayer.performance.BatteryOptimizer
import com.astralplayer.nextplayer.performance.PerformanceMonitor
import com.astralplayer.nextplayer.performance.VideoCache
import com.astralplayer.nextplayer.security.EncryptionEngine
import com.astralplayer.nextplayer.security.SecurePreferences
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class Week1ComponentTest {
    
    private lateinit var context: Context
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var batteryOptimizer: BatteryOptimizer
    private lateinit var videoCache: VideoCache
    private lateinit var encryptionEngine: EncryptionEngine
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        performanceMonitor = PerformanceMonitor(context)
        batteryOptimizer = BatteryOptimizer(context)
        videoCache = VideoCache(context)
        encryptionEngine = EncryptionEngine(context)
    }
    
    @Test
    fun `performance monitor measures operations correctly`() {
        val startTime = performanceMonitor.startMeasure("test_operation")
        assertTrue("Start time should be positive", startTime > 0)
        
        val duration = performanceMonitor.endMeasure("test_operation", startTime)
        assertTrue("Duration should be non-negative", duration >= 0)
    }
    
    @Test
    fun `battery optimizer provides valid video quality`() {
        val quality = batteryOptimizer.getOptimalVideoQuality()
        assertNotNull("Quality should not be null", quality)
        assertTrue("Quality should be valid enum", 
            quality in BatteryOptimizer.VideoQuality.values())
    }
    
    @Test
    fun `video cache operations work correctly`() = runTest {
        val testUri = mockk<android.net.Uri>(relaxed = true)
        val testData = byteArrayOf(1, 2, 3, 4, 5)
        
        val cached = videoCache.cacheVideoSegment(testUri, testData)
        assertTrue("Caching should succeed", cached)
    }
    
    @Test
    fun `encryption engine encrypts and decrypts strings`() {
        val plainText = "test_string"
        val encrypted = encryptionEngine.encryptString(plainText)
        assertNotEquals("Encrypted should differ from plain", plainText, encrypted)
        
        val decrypted = encryptionEngine.decryptString(encrypted)
        assertEquals("Decrypted should match original", plainText, decrypted)
    }
    
    @Test
    fun `secure preferences store and retrieve values`() {
        val securePrefs = SecurePreferences(context, encryptionEngine)
        
        securePrefs.putBoolean("test_key", true)
        val retrieved = securePrefs.getBoolean("test_key", false)
        assertTrue("Retrieved value should match stored", retrieved)
    }
}