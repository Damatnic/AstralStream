package com.astralplayer.nextplayer.feature.display

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AspectRatioMode(val displayName: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    FILL("Fill", -1f),
    FIT("Fit", 0f),
    RATIO_4_3("4:3", 4f/3f),
    RATIO_16_9("16:9", 16f/9f),
    RATIO_18_9("18:9", 18f/9f),
    ZOOM("Zoom", -2f),
    CROP("Crop", -3f)
}

class AspectRatioManager {
    
    private val _currentMode = MutableStateFlow(AspectRatioMode.ORIGINAL)
    val currentMode: StateFlow<AspectRatioMode> = _currentMode.asStateFlow()
    
    private val _videoRotation = MutableStateFlow(0f)
    val videoRotation: StateFlow<Float> = _videoRotation.asStateFlow()
    
    private val _zoomLevel = MutableStateFlow(1f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()
    
    fun setAspectRatioMode(mode: AspectRatioMode) {
        _currentMode.value = mode
    }
    
    fun cycleAspectRatio() {
        val modes = AspectRatioMode.values()
        val currentIndex = modes.indexOf(_currentMode.value)
        val nextIndex = (currentIndex + 1) % modes.size
        setAspectRatioMode(modes[nextIndex])
    }
    
    fun rotateVideo(degrees: Float) {
        _videoRotation.value = (_videoRotation.value + degrees) % 360f
    }
    
    fun setZoom(level: Float) {
        _zoomLevel.value = level.coerceIn(0.5f, 5f)
    }
    
    fun resetAll() {
        _currentMode.value = AspectRatioMode.ORIGINAL
        _videoRotation.value = 0f
        _zoomLevel.value = 1f
    }
}