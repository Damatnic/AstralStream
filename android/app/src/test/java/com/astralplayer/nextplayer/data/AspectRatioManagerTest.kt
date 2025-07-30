package com.astralplayer.nextplayer.data

import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AspectRatioManagerTest {

    private lateinit var aspectRatioManager: AspectRatioManager

    @Before
    fun setup() {
        aspectRatioManager = AspectRatioManager()
    }

    @Test
    fun `sets and gets aspect ratio correctly`() = runTest {
        aspectRatioManager.setAspectRatio(AspectRatio.RATIO_16_9)
        assertEquals(AspectRatio.RATIO_16_9, aspectRatioManager.currentRatio.value)
    }

    @Test
    fun `cycles through aspect ratios`() {
        aspectRatioManager.setAspectRatio(AspectRatio.FIT)
        val nextRatio = aspectRatioManager.getNextAspectRatio()
        assertEquals(AspectRatio.FILL, nextRatio)
    }

    @Test
    fun `calculates fit transform correctly`() {
        aspectRatioManager.setVideoSize(1920f, 1080f) // 16:9 video
        aspectRatioManager.setScreenSize(1080f, 1920f) // 9:16 screen
        aspectRatioManager.setAspectRatio(AspectRatio.FIT)
        
        val transform = aspectRatioManager.calculateTransform()
        assertTrue("Scale should be positive", transform.scaleX > 0 && transform.scaleY > 0)
    }

    @Test
    fun `calculates stretch transform correctly`() {
        aspectRatioManager.setVideoSize(1920f, 1080f)
        aspectRatioManager.setScreenSize(1080f, 1920f)
        aspectRatioManager.setAspectRatio(AspectRatio.STRETCH)
        
        val transform = aspectRatioManager.calculateTransform()
        assertEquals(1f, transform.scaleX, 0.01f)
        assertEquals(1f, transform.scaleY, 0.01f)
    }

    @Test
    fun `handles zero video size gracefully`() {
        aspectRatioManager.setVideoSize(0f, 0f)
        aspectRatioManager.setScreenSize(1080f, 1920f)
        
        val transform = aspectRatioManager.calculateTransform()
        assertEquals(VideoTransform(), transform)
    }

    @Test
    fun `calculates 16:9 ratio correctly`() {
        aspectRatioManager.setVideoSize(1920f, 1080f)
        aspectRatioManager.setScreenSize(1080f, 1920f)
        aspectRatioManager.setAspectRatio(AspectRatio.RATIO_16_9)
        
        val transform = aspectRatioManager.calculateTransform()
        assertNotNull(transform)
    }
}