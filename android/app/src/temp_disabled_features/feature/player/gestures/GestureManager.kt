package com.astralplayer.nextplayer.feature.player.gestures

import android.content.Context
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

/**
 * Comprehensive gesture manager that coordinates all gesture handlers
 */
class GestureManager(
    private val context: Context,
    private val gestureCallbacks: GestureCallbacks
) {
    private val verticalGestureHandler = VerticalGestureHandler(
        context = context,
        onVolumeChange = { level, delta, side -> gestureCallbacks.onVolumeChange(level, delta, side) },
        onBrightnessChange = { level, delta, side -> gestureCallbacks.onBrightnessChange(level, delta, side) },
        onGestureStart = { type, side -> gestureCallbacks.onGestureStart(type, side) },
        onGestureEnd = { type, success -> gestureCallbacks.onGestureEnd(type, success) }
    )
    
    private val horizontalSwipeHandler = HorizontalSwipeHandler(
        onSeekStart = { gestureCallbacks.onSeekStart() },
        onSeek = { deltaMs -> gestureCallbacks.onSeek(deltaMs) },
        onSeekEnd = { totalDeltaMs -> gestureCallbacks.onSeekEnd(totalDeltaMs) },
        onGestureStart = { type -> gestureCallbacks.onGestureStart(type) },
        onGestureEnd = { type, success -> gestureCallbacks.onGestureEnd(type, success) }
    )
    
    private val doubleTapHandler = DoubleTapHandler(
        onSingleTap = { gestureCallbacks.onSingleTap() },
        onDoubleTapLeft = { gestureCallbacks.onDoubleTapLeft() },
        onDoubleTapCenter = { gestureCallbacks.onDoubleTapCenter() },
        onDoubleTapRight = { gestureCallbacks.onDoubleTapRight() },
        onGestureStart = { type -> gestureCallbacks.onGestureStart(type) },
        onGestureEnd = { type, success -> gestureCallbacks.onGestureEnd(type, success) }
    )
    
    private val pinchZoomHandler = PinchZoomHandler(
        onZoomStart = { gestureCallbacks.onZoomStart() },
        onZoom = { scaleFactor -> gestureCallbacks.onZoom(scaleFactor) },
        onZoomEnd = { gestureCallbacks.onZoomEnd() },
        onGestureStart = { type -> gestureCallbacks.onGestureStart(type) },
        onGestureEnd = { type, success -> gestureCallbacks.onGestureEnd(type, success) }
    )
    
    private val longPressHandler = LongPressHandler(
        onLongPress = { position -> gestureCallbacks.onLongPress(position) },
        onGestureStart = { type -> gestureCallbacks.onGestureStart(type) },
        onGestureEnd = { type, success -> gestureCallbacks.onGestureEnd(type, success) }
    )
    
    @Composable
    fun Modifier.applyGestures(
        settings: GestureSettings,
        size: IntSize
    ): Modifier {
        return this.pointerInput(settings, size) {
            coroutineScope {
                // Launch all gesture detectors concurrently
                launch {
                    detectTransformGestures(
                        panZoomLock = true,
                        onGesture = { centroid, pan, zoom, rotation ->
                            if (zoom != 1f && settings.zoom.enabled) {
                                gestureCallbacks.onZoom(zoom)
                            }
                        }
                    )
                }
                
                launch {
                    with(verticalGestureHandler) {
                        detectVerticalGestures(
                            volumeSettings = VolumeGestureSettings(
                                isEnabled = settings.vertical.volumeGestureEnabled,
                                sensitivity = settings.vertical.volumeSensitivity,
                                rightSideOnly = settings.vertical.rightSideForVolume
                            ),
                            brightnessSettings = BrightnessGestureSettings(
                                isEnabled = settings.vertical.brightnessGestureEnabled,
                                sensitivity = settings.vertical.brightnessSensitivity,
                                leftSideOnly = settings.vertical.leftSideForBrightness
                            )
                        )
                    }
                }
                
                launch {
                    with(horizontalSwipeHandler) {
                        detectHorizontalSwipes(
                            settings = SeekGestureSettings(
                                isEnabled = settings.horizontal.seekGestureEnabled,
                                sensitivity = settings.horizontal.sensitivity,
                                showPreview = settings.horizontal.showSeekPreview
                            )
                        )
                    }
                }
                
                launch {
                    with(doubleTapHandler) {
                        detectDoubleTaps(
                            settings = DoubleTapSettings(
                                isEnabled = settings.doubleTap.enabled,
                                doubleTapTimeout = settings.doubleTap.maxTapInterval.toInt(),
                                seekAmount = (settings.doubleTap.seekAmount / 1000).toInt()
                            )
                        )
                    }
                }
                
                launch {
                    detectTapGestures(
                        onLongPress = { offset ->
                            if (settings.longPress.enabled) {
                                gestureCallbacks.onLongPress(offset)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Callbacks for all gesture events
 */
interface GestureCallbacks {
    // Volume and brightness
    fun onVolumeChange(level: Float, delta: Float, side: TouchSide)
    fun onBrightnessChange(level: Float, delta: Float, side: TouchSide)
    
    // Seeking
    fun onSeekStart()
    fun onSeek(deltaMs: Long)
    fun onSeekEnd(totalDeltaMs: Long)
    
    // Taps
    fun onSingleTap()
    fun onDoubleTapLeft()
    fun onDoubleTapCenter()
    fun onDoubleTapRight()
    
    // Zoom
    fun onZoomStart()
    fun onZoom(scaleFactor: Float)
    fun onZoomEnd()
    
    // Long press
    fun onLongPress(position: Offset)
    
    // General
    fun onGestureStart(type: GestureType, side: TouchSide = TouchSide.CENTER)
    fun onGestureEnd(type: GestureType, success: Boolean)
}

/**
 * Pinch zoom handler
 */
class PinchZoomHandler(
    private val onZoomStart: () -> Unit,
    private val onZoom: (scaleFactor: Float) -> Unit,
    private val onZoomEnd: () -> Unit,
    private val onGestureStart: (type: GestureType) -> Unit,
    private val onGestureEnd: (type: GestureType, success: Boolean) -> Unit
) {
    suspend fun PointerInputScope.detectPinchZoom(
        settings: GestureSettings
    ) {
        if (!settings.zoom.enabled) return
        
        detectTransformGestures { centroid, pan, zoom, rotation ->
            if (zoom != 1f) {
                onGestureStart(GestureType.ZOOM)
                onZoom(zoom)
                onGestureEnd(GestureType.ZOOM, true)
            }
        }
    }
}

/**
 * Long press handler
 */
class LongPressHandler(
    private val onLongPress: (position: Offset) -> Unit,
    private val onGestureStart: (type: GestureType) -> Unit,
    private val onGestureEnd: (type: GestureType, success: Boolean) -> Unit
) {
    suspend fun PointerInputScope.detectLongPress(
        settings: GestureSettings
    ) {
        if (!settings.longPress.enabled) return
        
        detectTapGestures(
            onLongPress = { offset ->
                onGestureStart(GestureType.LONG_PRESS)
                onLongPress(offset)
                onGestureEnd(GestureType.LONG_PRESS, true)
            }
        )
    }
}