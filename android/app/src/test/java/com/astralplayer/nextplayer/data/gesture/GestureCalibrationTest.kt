package com.astralplayer.nextplayer.data.gesture

import android.content.Context
import android.util.DisplayMetrics
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class GestureCalibrationTest {

    private lateinit var mockContext: Context
    private lateinit var calibration: GestureCalibration

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        
        val mockResources = mockk<android.content.res.Resources>(relaxed = true)
        val mockDisplayMetrics = DisplayMetrics().apply {
            density = 2.0f
            widthPixels = 1080
        }
        
        every { mockContext.resources } returns mockResources
        every { mockResources.displayMetrics } returns mockDisplayMetrics
        
        calibration = GestureCalibration(mockContext)
    }

    @Test
    fun `calibrates for standard device correctly`() = runTest {
        val settings = calibration.calibrateForDevice()
        
        assertEquals(1.0f, settings.seekSensitivity, 0.01f)
        assertEquals(1.0f, settings.touchSensitivity, 0.01f)
        assertEquals(0.8f, settings.hapticIntensity, 0.01f)
    }

    @Test
    fun `adjusts for large screen device`() = runTest {
        val mockDisplayMetrics = DisplayMetrics().apply {
            density = 3.5f
            widthPixels = 1440
        }
        every { mockContext.resources.displayMetrics } returns mockDisplayMetrics
        
        val settings = calibration.calibrateForDevice()
        
        assertEquals(1.2f, settings.seekSensitivity, 0.01f)
        assertEquals(1.1f, settings.touchSensitivity, 0.01f)
        assertEquals(0.9f, settings.hapticIntensity, 0.01f)
    }

    @Test
    fun `adjusts for small screen device`() = runTest {
        val mockDisplayMetrics = DisplayMetrics().apply {
            density = 1.5f
            widthPixels = 720
        }
        every { mockContext.resources.displayMetrics } returns mockDisplayMetrics
        
        val settings = calibration.calibrateForDevice()
        
        assertEquals(0.8f, settings.seekSensitivity, 0.01f)
        assertEquals(0.9f, settings.touchSensitivity, 0.01f)
        assertEquals(0.8f, settings.hapticIntensity, 0.01f)
    }
}