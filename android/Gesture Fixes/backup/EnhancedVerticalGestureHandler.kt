package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Enhanced vertical gesture handler with configurable dead zones and smooth adjustment curves
 * Implements requirements 2.3: Enhanced vertical gestures for volume and brightness
 */
@Composable
fun EnhancedVerticalGestureHandler(
    viewModel: PlayerViewModel,
    gestureSettings: GestureSettings = GestureSettings(),
    onGestureStart: () -> Unit = {},
    onGestureEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Enhanced gesture state
    var gestureStartPosition by remember { mutableStateOf(Offset.Zero) }
    var currentGestureType by remember { mutableStateOf(VerticalGestureType.NONE) }
    var accumulatedDelta by remember { mutableFloatStateOf(0f) }
    var lastAdjustmentTime by remember { mutableLongStateOf(0L) }
    
    // Enhanced settings with dead zones
    val enhancedSettings = remember(gestureSettings) {
        EnhancedVerticalGestureSettings(
            deadZoneWidth = 80f, // Configurable dead zone in pixels
            edgeZoneWidth = 120f, // Width of edge zones for gesture detection
            smoothingFactor = 0.3f, // Smoothing for adjustment curves
            minimumDelta = 10f, // Minimum movement to trigger gesture
            accelerationCurve = AccelerationCurve.SMOOTH,
            volumeSensitivity = gestureSettings.vertical.volumeSensitivity,
            brightnessSensitivity = gestureSettings.vertical.brightnessSensitivity
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        gestureStartPosition = offset
                        currentGestureType = determineGestureType(offset, size, enhancedSettings)
                        accumulatedDelta = 0f
                        
                        if (currentGestureType != VerticalGestureType.NONE) {
                            onGestureStart()
                            
                            // Show appropriate overlay
                            when (currentGestureType) {
                                VerticalGestureType.BRIGHTNESS -> viewModel.showBrightnessOverlay()
                                VerticalGestureType.VOLUME -> viewModel.showVolumeOverlay()
                                VerticalGestureType.NONE -> {}
                            }
                        }
                    },
                    onDragEnd = {
                        if (currentGestureType != VerticalGestureType.NONE) {
                            onGestureEnd()
                            
                            // Provide haptic feedback for gesture completion
                            scope.launch {
                                viewModel.provideHapticFeedback(30L)
                            }
                        }
                        
                        currentGestureType = VerticalGestureType.NONE
                        accumulatedDelta = 0f
                    }
                ) { change, _ ->
                    if (currentGestureType == VerticalGestureType.NONE) return@detectDragGestures
                    
                    val currentPosition = change.position
                    val deltaY = currentPosition.y - gestureStartPosition.y
                    
                    // Apply smooth adjustment curves
                    val adjustedDelta = applySmoothAdjustmentCurve(
                        deltaY, 
                        size.height.toFloat(),
                        enhancedSettings
                    )
                    
                    accumulatedDelta += adjustedDelta
                    
                    // Throttle adjustments to prevent excessive updates
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastAdjustmentTime >= 16) { // ~60fps
                        lastAdjustmentTime = currentTime
                        
                        when (currentGestureType) {
                            VerticalGestureType.BRIGHTNESS -> {
                                if (gestureSettings.vertical.brightnessGestureEnabled) {
                                    val brightnessAdjustment = calculateBrightnessAdjustment(
                                        accumulatedDelta,
                                        enhancedSettings
                                    )
                                    viewModel.adjustBrightness(brightnessAdjustment)
                                }
                            }
                            VerticalGestureType.VOLUME -> {
                                if (gestureSettings.vertical.volumeGestureEnabled) {
                                    val volumeAdjustment = calculateVolumeAdjustment(
                                        accumulatedDelta,
                                        enhancedSettings
                                    )
                                    viewModel.adjustVolume(volumeAdjustment)
                                }
                            }
                            VerticalGestureType.NONE -> {}
                        }
                        
                        // Reset accumulated delta after applying adjustment
                        accumulatedDelta = 0f
                    }
                }
            }
    )
}

/**
 * Enhanced vertical gesture settings with configurable dead zones
 */
data class EnhancedVerticalGestureSettings(
    val deadZoneWidth: Float = 80f,
    val edgeZoneWidth: Float = 120f,
    val smoothingFactor: Float = 0.3f,
    val minimumDelta: Float = 10f,
    val accelerationCurve: AccelerationCurve = AccelerationCurve.SMOOTH,
    val volumeSensitivity: Float = 1f,
    val brightnessSensitivity: Float = 1f
)

/**
 * Types of vertical gestures
 */
enum class VerticalGestureType {
    BRIGHTNESS, // Left side
    VOLUME,     // Right side
    NONE        // Dead zone or invalid area
}

/**
 * Acceleration curve types for smooth adjustments
 */
enum class AccelerationCurve {
    LINEAR,     // Constant adjustment rate
    SMOOTH,     // Ease-in-out curve
    EXPONENTIAL // Accelerating curve
}

/**
 * Determines the gesture type based on touch position and dead zones
 */
private fun determineGestureType(
    position: Offset,
    screenSize: IntSize,
    settings: EnhancedVerticalGestureSettings
): VerticalGestureType {
    val screenWidth = screenSize.width.toFloat()
    val x = position.x
    
    return when {
        // Left edge zone for brightness
        x <= settings.edgeZoneWidth -> VerticalGestureType.BRIGHTNESS
        
        // Right edge zone for volume
        x >= screenWidth - settings.edgeZoneWidth -> VerticalGestureType.VOLUME
        
        // Center dead zone
        x >= (screenWidth / 2) - (settings.deadZoneWidth / 2) &&
        x <= (screenWidth / 2) + (settings.deadZoneWidth / 2) -> VerticalGestureType.NONE
        
        // Left side (outside dead zone)
        x < screenWidth / 2 -> VerticalGestureType.BRIGHTNESS
        
        // Right side (outside dead zone)
        else -> VerticalGestureType.VOLUME
    }
}

/**
 * Applies smooth adjustment curves to raw delta values
 */
private fun applySmoothAdjustmentCurve(
    deltaY: Float,
    screenHeight: Float,
    settings: EnhancedVerticalGestureSettings
): Float {
    // Normalize delta to screen height
    val normalizedDelta = deltaY / screenHeight
    
    return when (settings.accelerationCurve) {
        AccelerationCurve.LINEAR -> normalizedDelta * settings.smoothingFactor
        
        AccelerationCurve.SMOOTH -> {
            // Ease-in-out curve using sine function
            val smoothed = sin(normalizedDelta * PI / 2).toFloat()
            smoothed * settings.smoothingFactor
        }
        
        AccelerationCurve.EXPONENTIAL -> {
            // Exponential curve for faster acceleration
            val exponential = if (normalizedDelta >= 0) {
                (normalizedDelta * normalizedDelta)
            } else {
                -((-normalizedDelta) * (-normalizedDelta))
            }
            exponential * settings.smoothingFactor
        }
    }
}

/**
 * Calculates brightness adjustment with system integration
 */
private fun calculateBrightnessAdjustment(
    accumulatedDelta: Float,
    settings: EnhancedVerticalGestureSettings
): Float {
    // Apply sensitivity and ensure reasonable bounds
    val adjustment = -accumulatedDelta * settings.brightnessSensitivity
    
    // Clamp adjustment to prevent extreme changes
    return adjustment.coerceIn(-0.1f, 0.1f)
}

/**
 * Calculates volume adjustment with system integration
 */
private fun calculateVolumeAdjustment(
    accumulatedDelta: Float,
    settings: EnhancedVerticalGestureSettings
): Float {
    // Apply sensitivity and ensure reasonable bounds
    val adjustment = -accumulatedDelta * settings.volumeSensitivity
    
    // Clamp adjustment to prevent extreme changes
    return adjustment.coerceIn(-0.15f, 0.15f)
}