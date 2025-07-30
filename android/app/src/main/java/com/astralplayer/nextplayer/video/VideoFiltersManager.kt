package com.astralplayer.nextplayer.video

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class VideoFilterPreset(val displayName: String) {
    NORMAL("Normal"),
    BRIGHT("Bright"),
    DARK("Dark"),
    VIVID("Vivid"),
    SEPIA("Sepia")
}

class VideoFiltersManager {
    private var currentPreset = VideoFilterPreset.NORMAL

    fun setPreset(preset: VideoFilterPreset) {
        currentPreset = preset
    }

    fun getCurrentPreset(): VideoFilterPreset = currentPreset

    fun getAvailablePresets(): List<VideoFilterPreset> = VideoFilterPreset.values().toList()

    val isEnabled = MutableStateFlow(false)
    val filters = MutableStateFlow(VideoFilters())

    fun setEnabled(enabled: Boolean) {
        isEnabled.value = enabled
    }

    fun applyPreset(preset: VideoFilterPreset) {
        currentPreset = preset
    }

    fun setBrightness(brightness: Float) {
        val current = filters.value
        filters.value = current.copy(brightness = brightness)
    }

    fun setContrast(contrast: Float) {
        val current = filters.value
        filters.value = current.copy(contrast = contrast)
    }

    fun setSaturation(saturation: Float) {
        val current = filters.value
        filters.value = current.copy(saturation = saturation)
    }

    fun setHue(hue: Float) {
        val current = filters.value
        filters.value = current.copy(hue = hue)
    }

    fun resetToDefaults() {
        filters.value = VideoFilters()
    }
}

data class VideoFilters(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val hue: Float = 0f
)