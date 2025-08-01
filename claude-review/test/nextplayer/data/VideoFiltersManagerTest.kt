package com.astralplayer.nextplayer.data

import androidx.media3.exoplayer.ExoPlayer
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class VideoFiltersManagerTest {

    private lateinit var mockExoPlayer: ExoPlayer
    private lateinit var filtersManager: VideoFiltersManager

    @Before
    fun setup() {
        mockExoPlayer = mockk(relaxed = true)
        filtersManager = VideoFiltersManager(mockExoPlayer)
    }

    @Test
    fun `test initial filter state has default values`() = runTest {
        val initialState = filtersManager.filterState.value

        assertEquals(0f, initialState.brightness, 0.01f)
        assertEquals(1f, initialState.contrast, 0.01f)
        assertEquals(1f, initialState.saturation, 0.01f)
        assertEquals(0f, initialState.hue, 0.01f)
        assertEquals(0f, initialState.rotation, 0.01f)
        assertEquals(1f, initialState.zoom, 0.01f)
        assertFalse(initialState.isGrayscale)
        assertFalse(initialState.isInverted)
    }

    @Test
    fun `test setBrightness updates brightness value`() = runTest {
        filtersManager.setBrightness(0.5f)

        val state = filtersManager.filterState.value
        assertEquals(0.5f, state.brightness, 0.01f)
    }

    @Test
    fun `test setBrightness clamps values to valid range`() = runTest {
        // Test upper bound
        filtersManager.setBrightness(2f)
        assertEquals(1f, filtersManager.filterState.value.brightness, 0.01f)

        // Test lower bound
        filtersManager.setBrightness(-2f)
        assertEquals(-1f, filtersManager.filterState.value.brightness, 0.01f)
    }

    @Test
    fun `test setContrast updates contrast value`() = runTest {
        filtersManager.setContrast(1.5f)

        val state = filtersManager.filterState.value
        assertEquals(1.5f, state.contrast, 0.01f)
    }

    @Test
    fun `test setContrast clamps values to valid range`() = runTest {
        // Test upper bound
        filtersManager.setContrast(5f)
        assertEquals(3f, filtersManager.filterState.value.contrast, 0.01f)

        // Test lower bound
        filtersManager.setContrast(-1f)
        assertEquals(0f, filtersManager.filterState.value.contrast, 0.01f)
    }

    @Test
    fun `test setSaturation updates saturation value`() = runTest {
        filtersManager.setSaturation(0.8f)

        val state = filtersManager.filterState.value
        assertEquals(0.8f, state.saturation, 0.01f)
    }

    @Test
    fun `test setSaturation clamps values to valid range`() = runTest {
        // Test upper bound
        filtersManager.setSaturation(5f)
        assertEquals(3f, filtersManager.filterState.value.saturation, 0.01f)

        // Test lower bound
        filtersManager.setSaturation(-1f)
        assertEquals(0f, filtersManager.filterState.value.saturation, 0.01f)
    }

    @Test
    fun `test setHue updates hue value`() = runTest {
        filtersManager.setHue(45f)

        val state = filtersManager.filterState.value
        assertEquals(45f, state.hue, 0.01f)
    }

    @Test
    fun `test setHue normalizes angle values`() = runTest {
        // Test wrapping around 360 degrees
        filtersManager.setHue(450f)
        assertEquals(90f, filtersManager.filterState.value.hue, 0.01f)

        // Test negative values
        filtersManager.setHue(-90f)
        assertEquals(270f, filtersManager.filterState.value.hue, 0.01f)
    }

    @Test
    fun `test setRotation updates rotation value`() = runTest {
        filtersManager.setRotation(90f)

        val state = filtersManager.filterState.value
        assertEquals(90f, state.rotation, 0.01f)
    }

    @Test
    fun `test setRotation normalizes angle values`() = runTest {
        // Test wrapping around 360 degrees
        filtersManager.setRotation(450f)
        assertEquals(90f, filtersManager.filterState.value.rotation, 0.01f)

        // Test negative values
        filtersManager.setRotation(-90f)
        assertEquals(270f, filtersManager.filterState.value.rotation, 0.01f)
    }

    @Test
    fun `test setZoom updates zoom value`() = runTest {
        filtersManager.setZoom(2f)

        val state = filtersManager.filterState.value
        assertEquals(2f, state.zoom, 0.01f)
    }

    @Test
    fun `test setZoom clamps values to valid range`() = runTest {
        // Test upper bound
        filtersManager.setZoom(15f)
        assertEquals(10f, filtersManager.filterState.value.zoom, 0.01f)

        // Test lower bound
        filtersManager.setZoom(0.5f)
        assertEquals(0.5f, filtersManager.filterState.value.zoom, 0.01f)

        // Test below minimum
        filtersManager.setZoom(0f)
        assertEquals(0.5f, filtersManager.filterState.value.zoom, 0.01f)
    }

    @Test
    fun `test toggleGrayscale changes grayscale state`() = runTest {
        assertFalse(filtersManager.filterState.value.isGrayscale)

        filtersManager.toggleGrayscale()
        assertTrue(filtersManager.filterState.value.isGrayscale)

        filtersManager.toggleGrayscale()
        assertFalse(filtersManager.filterState.value.isGrayscale)
    }

    @Test
    fun `test toggleInverted changes inverted state`() = runTest {
        assertFalse(filtersManager.filterState.value.isInverted)

        filtersManager.toggleInverted()
        assertTrue(filtersManager.filterState.value.isInverted)

        filtersManager.toggleInverted()
        assertFalse(filtersManager.filterState.value.isInverted)
    }

    @Test
    fun `test resetFilters resets all values to default`() = runTest {
        // Apply various filters
        filtersManager.setBrightness(0.5f)
        filtersManager.setContrast(2f)
        filtersManager.setSaturation(0.5f)
        filtersManager.setHue(180f)
        filtersManager.setRotation(90f)
        filtersManager.setZoom(3f)
        filtersManager.toggleGrayscale()
        filtersManager.toggleInverted()

        // Reset all filters
        filtersManager.resetFilters()

        val state = filtersManager.filterState.value
        assertEquals(0f, state.brightness, 0.01f)
        assertEquals(1f, state.contrast, 0.01f)
        assertEquals(1f, state.saturation, 0.01f)
        assertEquals(0f, state.hue, 0.01f)
        assertEquals(0f, state.rotation, 0.01f)
        assertEquals(1f, state.zoom, 0.01f)
        assertFalse(state.isGrayscale)
        assertFalse(state.isInverted)
    }

    @Test
    fun `test multiple filter changes maintain independent values`() = runTest {
        filtersManager.setBrightness(0.3f)
        filtersManager.setContrast(1.8f)
        filtersManager.setSaturation(0.6f)
        filtersManager.setHue(120f)
        filtersManager.toggleGrayscale()

        val state = filtersManager.filterState.value
        assertEquals(0.3f, state.brightness, 0.01f)
        assertEquals(1.8f, state.contrast, 0.01f)
        assertEquals(0.6f, state.saturation, 0.01f)
        assertEquals(120f, state.hue, 0.01f)
        assertTrue(state.isGrayscale)
        assertEquals(0f, state.rotation, 0.01f) // Unchanged
        assertEquals(1f, state.zoom, 0.01f) // Unchanged
    }
}