package com.astralplayer.nextplayer.data

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages video filters and adjustments for playback
 */
@UnstableApi
class VideoFiltersManager(
    private val exoPlayer: ExoPlayer
) {
    private val _filterState = MutableStateFlow(VideoFilterState())
    val filterState: StateFlow<VideoFilterState> = _filterState.asStateFlow()
    
    // Store filter values for potential future use
    
    data class VideoFilterState(
        val brightness: Float = 0f,      // -1.0 to 1.0
        val contrast: Float = 1f,        // 0.0 to 2.0
        val saturation: Float = 1f,      // 0.0 to 2.0
        val hue: Float = 0f,            // -180 to 180
        val sharpness: Float = 0f,      // 0.0 to 1.0
        val gamma: Float = 1f,          // 0.1 to 4.0
        val rotation: Float = 0f,        // 0, 90, 180, 270
        val isGrayscale: Boolean = false,
        val isInverted: Boolean = false,
        val zoom: Float = 1f            // 0.5 to 3.0
    )
    
    /**
     * Apply brightness adjustment
     * @param value -1.0 (darkest) to 1.0 (brightest), 0 is normal
     */
    fun setBrightness(value: Float) {
        val clampedValue = value.coerceIn(-1f, 1f)
        _filterState.value = _filterState.value.copy(brightness = clampedValue)
        applyFilters()
    }
    
    /**
     * Apply contrast adjustment
     * @param value 0.0 (no contrast) to 2.0 (high contrast), 1.0 is normal
     */
    fun setContrast(value: Float) {
        val clampedValue = value.coerceIn(0f, 2f)
        _filterState.value = _filterState.value.copy(contrast = clampedValue)
        applyFilters()
    }
    
    /**
     * Apply saturation adjustment
     * @param value 0.0 (grayscale) to 2.0 (vivid colors), 1.0 is normal
     */
    fun setSaturation(value: Float) {
        val clampedValue = value.coerceIn(0f, 2f)
        _filterState.value = _filterState.value.copy(saturation = clampedValue)
        applyFilters()
    }
    
    /**
     * Apply hue adjustment
     * @param value -180 to 180 degrees
     */
    fun setHue(value: Float) {
        val clampedValue = value.coerceIn(-180f, 180f)
        _filterState.value = _filterState.value.copy(hue = clampedValue)
        applyFilters()
    }
    
    /**
     * Apply sharpness adjustment
     * @param value 0.0 (no sharpening) to 1.0 (maximum sharpness)
     */
    fun setSharpness(value: Float) {
        val clampedValue = value.coerceIn(0f, 1f)
        _filterState.value = _filterState.value.copy(sharpness = clampedValue)
        applyFilters()
    }
    
    /**
     * Apply gamma adjustment
     * @param value 0.1 to 4.0, 1.0 is normal
     */
    fun setGamma(value: Float) {
        val clampedValue = value.coerceIn(0.1f, 4f)
        _filterState.value = _filterState.value.copy(gamma = clampedValue)
        applyFilters()
    }
    
    /**
     * Rotate video
     * @param degrees 0, 90, 180, or 270
     */
    fun setRotation(degrees: Float) {
        val validRotations = listOf(0f, 90f, 180f, 270f)
        val nearestRotation = validRotations.minByOrNull { kotlin.math.abs(it - degrees) } ?: 0f
        _filterState.value = _filterState.value.copy(rotation = nearestRotation)
        applyFilters()
    }
    
    /**
     * Toggle grayscale filter
     */
    fun toggleGrayscale() {
        _filterState.value = _filterState.value.copy(isGrayscale = !_filterState.value.isGrayscale)
        applyFilters()
    }
    
    /**
     * Toggle color inversion
     */
    fun toggleInverted() {
        _filterState.value = _filterState.value.copy(isInverted = !_filterState.value.isInverted)
        applyFilters()
    }
    
    /**
     * Set zoom level
     * @param value 0.5 (zoomed out) to 3.0 (zoomed in), 1.0 is normal
     */
    fun setZoom(value: Float) {
        val clampedValue = value.coerceIn(0.5f, 3f)
        _filterState.value = _filterState.value.copy(zoom = clampedValue)
        applyFilters()
    }
    
    /**
     * Reset all filters to default values
     */
    fun resetFilters() {
        _filterState.value = VideoFilterState()
        applyFilters()
    }
    
    /**
     * Apply all active filters to the video
     */
    private fun applyFilters() {
        // Note: Media3 effect APIs are not available in the current version
        // Storing filter values for potential future implementation
        // or for use with custom video processing
        
        // For now, we can only apply basic transformations through player settings
        val state = _filterState.value
        
        // Apply zoom using video scaling mode
        if (state.zoom != 1f) {
            // This is a simplified approach - actual zoom would require video effects API
            when {
                state.zoom > 1.5f -> exoPlayer.videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                else -> exoPlayer.videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
        }
    }
    
    /**
     * Create color matrix for brightness, contrast, saturation, and hue adjustments
     */
    private fun createColorMatrix(state: VideoFilterState): FloatArray {
        // Base matrix (identity)
        val matrix = floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        )
        
        // Apply contrast
        if (state.contrast != 1f) {
            for (i in matrix.indices) {
                matrix[i] *= state.contrast
            }
        }
        
        // Apply saturation
        if (state.saturation != 1f) {
            val gray = 0.299f * (1 - state.saturation)
            val green = 0.587f * (1 - state.saturation)
            val blue = 0.114f * (1 - state.saturation)
            
            matrix[0] = gray + state.saturation
            matrix[1] = green
            matrix[2] = blue
            
            matrix[3] = gray
            matrix[4] = green + state.saturation
            matrix[5] = blue
            
            matrix[6] = gray
            matrix[7] = green
            matrix[8] = blue + state.saturation
        }
        
        // Apply hue rotation
        if (state.hue != 0f) {
            val angle = Math.toRadians(state.hue.toDouble())
            val cos = kotlin.math.cos(angle).toFloat()
            val sin = kotlin.math.sin(angle).toFloat()
            
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f
            
            val hueMatrix = floatArrayOf(
                lumR + cos * (1 - lumR) + sin * -lumR,
                lumG + cos * -lumG + sin * -lumG,
                lumB + cos * -lumB + sin * (1 - lumB),
                
                lumR + cos * -lumR + sin * 0.143f,
                lumG + cos * (1 - lumG) + sin * 0.140f,
                lumB + cos * -lumB + sin * -0.283f,
                
                lumR + cos * -lumR + sin * -(1 - lumR),
                lumG + cos * -lumG + sin * lumG,
                lumB + cos * (1 - lumB) + sin * lumB
            )
            
            // Multiply matrices
            val result = FloatArray(9)
            for (i in 0..2) {
                for (j in 0..2) {
                    result[i * 3 + j] = 0f
                    for (k in 0..2) {
                        result[i * 3 + j] += matrix[i * 3 + k] * hueMatrix[k * 3 + j]
                    }
                }
            }
            return result
        }
        
        return matrix
    }
    
    /**
     * Check if any filters are active
     */
    fun hasActiveFilters(): Boolean {
        val state = _filterState.value
        return state.brightness != 0f ||
                state.contrast != 1f ||
                state.saturation != 1f ||
                state.hue != 0f ||
                state.sharpness != 0f ||
                state.gamma != 1f ||
                state.rotation != 0f ||
                state.isGrayscale ||
                state.isInverted ||
                state.zoom != 1f
    }
    
    /**
     * Get current filter values as a map for saving/restoring
     */
    fun getFilterValues(): Map<String, Float> {
        val state = _filterState.value
        return mapOf(
            "brightness" to state.brightness,
            "contrast" to state.contrast,
            "saturation" to state.saturation,
            "hue" to state.hue,
            "sharpness" to state.sharpness,
            "gamma" to state.gamma,
            "rotation" to state.rotation,
            "grayscale" to if (state.isGrayscale) 1f else 0f,
            "inverted" to if (state.isInverted) 1f else 0f,
            "zoom" to state.zoom
        )
    }
    
    /**
     * Restore filter values from a map
     */
    fun restoreFilterValues(values: Map<String, Float>) {
        _filterState.value = VideoFilterState(
            brightness = values["brightness"] ?: 0f,
            contrast = values["contrast"] ?: 1f,
            saturation = values["saturation"] ?: 1f,
            hue = values["hue"] ?: 0f,
            sharpness = values["sharpness"] ?: 0f,
            gamma = values["gamma"] ?: 1f,
            rotation = values["rotation"] ?: 0f,
            isGrayscale = (values["grayscale"] ?: 0f) > 0.5f,
            isInverted = (values["inverted"] ?: 0f) > 0.5f,
            zoom = values["zoom"] ?: 1f
        )
        applyFilters()
    }
}