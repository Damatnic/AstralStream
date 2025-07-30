package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * MX Player-style speed progression handler with configurable progression levels
 * Implements requirements 3.1: MX Player-style speed progression
 */
@Composable
fun MxPlayerSpeedProgressionHandler(
    viewModel: PlayerViewModel,
    gestureSettings: GestureSettings = GestureSettings(),
    onGestureStart: () -> Unit = {},
    onGestureEnd: () -> Unit = {},
    onSpeedChange: (Float, SeekDirection) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    // Speed progression state
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressStartTime by remember { mutableLongStateOf(0L) }
    var currentSpeedLevel by remember { mutableIntStateOf(0) }
    var currentDirection by remember { mutableStateOf(SeekDirection.FORWARD) }
    var gestureStartPosition by remember { mutableStateOf(Offset.Zero) }
    var speedProgressionJob by remember { mutableStateOf<Job?>(null) }
    var swipeBasedSpeedJob by remember { mutableStateOf<Job?>(null) }
    
    // MX Player speed progression configuration
    val speedProgression = remember {
        MxPlayerSpeedProgression(
            levels = listOf(1f, 2f, 4f, 8f, 16f, 32f),
            progressionIntervals = listOf(0L, 1000L, 2000L, 3500L, 5000L, 7000L), // ms
            swipeSpeedMultiplier = 2f,
            directionChangeThreshold = 50f // pixels
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        if (!gestureSettings.longPress.enabled) return@detectTapGestures
                        
                        // Start long press seek with speed progression
                        isLongPressing = true
                        longPressStartTime = System.currentTimeMillis()
                        gestureStartPosition = offset
                        currentSpeedLevel = 0
                        currentDirection = SeekDirection.FORWARD
                        
                        onGestureStart()
                        viewModel.startLongPressSeek(offset)
                        
                        // Start automatic speed progression
                        speedProgressionJob = scope.launch {
                            startAutomaticSpeedProgression(
                                speedProgression = speedProgression,
                                onSpeedLevelChange = { level, speed ->
                                    currentSpeedLevel = level
                                    onSpeedChange(speed, currentDirection)
                                    viewModel.updateLongPressSeek(
                                        speed = speed,
                                        direction = currentDirection,
                                        position = gestureStartPosition
                                    )
                                    
                                    // Provide haptic feedback for speed changes
                                    viewModel.provideHapticFeedback(
                                        intensity = when (level) {
                                            0, 1 -> 25L
                                            2, 3 -> 50L
                                            else -> 75L
                                        }
                                    )
                                }
                            )
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (isLongPressing) {
                            // Enable swipe-based speed control during long press
                            swipeBasedSpeedJob = scope.launch {
                                handleSwipeBasedSpeedControl(
                                    startPosition = gestureStartPosition,
                                    speedProgression = speedProgression,
                                    onDirectionChange = { newDirection ->
                                        if (newDirection != currentDirection) {
                                            currentDirection = newDirection
                                            onSpeedChange(
                                                speedProgression.levels[currentSpeedLevel],
                                                currentDirection
                                            )
                                            
                                            // Haptic feedback for direction change
                                            scope.launch {
                                                viewModel.provideHapticFeedback(40L)
                                            }
                                        }
                                    },
                                    onSpeedMultiplierChange = { multiplier ->
                                        val adjustedSpeed = speedProgression.levels[currentSpeedLevel] * multiplier
                                        onSpeedChange(adjustedSpeed, currentDirection)
                                        viewModel.updateLongPressSeek(
                                            speed = adjustedSpeed,
                                            direction = currentDirection,
                                            position = gestureStartPosition
                                        )
                                    }
                                )
                            }
                        }
                    },
                    onDragEnd = {
                        if (isLongPressing) {
                            // End long press seek
                            isLongPressing = false
                            speedProgressionJob?.cancel()
                            swipeBasedSpeedJob?.cancel()
                            
                            viewModel.endLongPressSeek()
                            onGestureEnd()
                            
                            // Reset state
                            currentSpeedLevel = 0
                            currentDirection = SeekDirection.FORWARD
                        }
                    }
                ) { change, _ ->
                    if (isLongPressing) {
                        val currentPosition = change.position
                        val deltaX = currentPosition.x - gestureStartPosition.x
                        
                        // Handle direction changes during long press
                        val newDirection = when {
                            deltaX > speedProgression.directionChangeThreshold -> SeekDirection.FORWARD
                            deltaX < -speedProgression.directionChangeThreshold -> SeekDirection.BACKWARD
                            else -> currentDirection
                        }
                        
                        if (newDirection != currentDirection) {
                            currentDirection = newDirection
                            onSpeedChange(
                                speedProgression.levels[currentSpeedLevel],
                                currentDirection
                            )
                            
                            // Update long press seek with new direction
                            viewModel.updateLongPressSeek(
                                speed = speedProgression.levels[currentSpeedLevel],
                                direction = currentDirection,
                                position = currentPosition
                            )
                        }
                    }
                }
            }
    )
}

/**
 * MX Player speed progression configuration
 */
data class MxPlayerSpeedProgression(
    val levels: List<Float> = listOf(1f, 2f, 4f, 8f, 16f, 32f),
    val progressionIntervals: List<Long> = listOf(0L, 1000L, 2000L, 3500L, 5000L, 7000L),
    val swipeSpeedMultiplier: Float = 2f,
    val directionChangeThreshold: Float = 50f
)

/**
 * Starts automatic speed progression based on hold duration
 */
private suspend fun startAutomaticSpeedProgression(
    speedProgression: MxPlayerSpeedProgression,
    onSpeedLevelChange: (level: Int, speed: Float) -> Unit
) {
    val startTime = System.currentTimeMillis()
    
    for (level in speedProgression.levels.indices) {
        val targetTime = startTime + speedProgression.progressionIntervals[level]
        val currentTime = System.currentTimeMillis()
        
        if (currentTime < targetTime) {
            delay(targetTime - currentTime)
        }
        
        // Check if coroutine is still active
        if (!isActive) break
        
        onSpeedLevelChange(level, speedProgression.levels[level])
    }
}

/**
 * Handles swipe-based speed control during long press
 */
private suspend fun handleSwipeBasedSpeedControl(
    startPosition: Offset,
    speedProgression: MxPlayerSpeedProgression,
    onDirectionChange: (SeekDirection) -> Unit,
    onSpeedMultiplierChange: (Float) -> Unit
) {
    // This function would handle real-time swipe detection
    // For now, it's a placeholder for the swipe-based control logic
    
    while (isActive) {
        delay(16) // ~60fps updates
        
        // In a real implementation, this would:
        // 1. Track current touch position
        // 2. Calculate swipe velocity and direction
        // 3. Apply speed multipliers based on swipe intensity
        // 4. Handle direction changes smoothly
    }
}

/**
 * Enhanced long press seek handler with MX Player features
 */
@Composable
fun EnhancedLongPressSeekHandler(
    viewModel: PlayerViewModel,
    gestureSettings: GestureSettings = GestureSettings(),
    onSeekStart: (Offset, SeekDirection) -> Unit = { _, _ -> },
    onSeekUpdate: (Long, Float, SeekDirection, Int) -> Unit = { _, _, _, _ -> },
    onSeekEnd: () -> Unit = {},
    onDirectionChange: (SeekDirection) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var seekState by remember { mutableStateOf(EnhancedLongPressSeekState()) }
    val scope = rememberCoroutineScope()
    
    // Enhanced seek configuration
    val seekConfig = remember {
        EnhancedSeekConfiguration(
            speedLevels = listOf(0.5f, 1f, 2f, 4f, 8f, 16f, 32f),
            accelerationThresholds = listOf(0L, 500L, 1500L, 3000L, 5000L, 8000L, 12000L),
            directionSmoothingFactor = 0.7f,
            hapticFeedbackEnabled = gestureSettings.longPress.enabled
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        if (!gestureSettings.longPress.enabled) return@detectTapGestures
                        
                        seekState = seekState.copy(
                            isActive = true,
                            startPosition = offset,
                            currentPosition = offset,
                            startTime = System.currentTimeMillis(),
                            direction = SeekDirection.FORWARD,
                            speedLevel = 0
                        )
                        
                        onSeekStart(offset, SeekDirection.FORWARD)
                        
                        // Start continuous seeking with speed progression
                        scope.launch {
                            startContinuousSeeking(
                                seekState = seekState,
                                seekConfig = seekConfig,
                                onUpdate = { position, speed, direction, level ->
                                    onSeekUpdate(position, speed, direction, level)
                                },
                                onDirectionChange = onDirectionChange,
                                viewModel = viewModel
                            )
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (seekState.isActive) {
                            seekState = seekState.copy(isActive = false)
                            onSeekEnd()
                        }
                    }
                ) { change, _ ->
                    if (seekState.isActive) {
                        val newDirection = determineSeekDirection(
                            change.position,
                            seekState.startPosition,
                            seekConfig.directionSmoothingFactor
                        )
                        
                        if (newDirection != seekState.direction) {
                            seekState = seekState.copy(direction = newDirection)
                            onDirectionChange(newDirection)
                        }
                        
                        seekState = seekState.copy(currentPosition = change.position)
                    }
                }
            }
    )
}

/**
 * Enhanced long press seek state
 */
data class EnhancedLongPressSeekState(
    val isActive: Boolean = false,
    val startPosition: Offset = Offset.Zero,
    val currentPosition: Offset = Offset.Zero,
    val startTime: Long = 0L,
    val direction: SeekDirection = SeekDirection.FORWARD,
    val speedLevel: Int = 0,
    val currentSpeed: Float = 1f
)

/**
 * Enhanced seek configuration
 */
data class EnhancedSeekConfiguration(
    val speedLevels: List<Float> = listOf(0.5f, 1f, 2f, 4f, 8f, 16f, 32f),
    val accelerationThresholds: List<Long> = listOf(0L, 500L, 1500L, 3000L, 5000L, 8000L, 12000L),
    val directionSmoothingFactor: Float = 0.7f,
    val hapticFeedbackEnabled: Boolean = true
)

/**
 * Starts continuous seeking with automatic speed progression
 */
private suspend fun startContinuousSeeking(
    seekState: EnhancedLongPressSeekState,
    seekConfig: EnhancedSeekConfiguration,
    onUpdate: (Long, Float, SeekDirection, Int) -> Unit,
    onDirectionChange: (SeekDirection) -> Unit,
    viewModel: PlayerViewModel
) {
    while (seekState.isActive && isActive) {
        val elapsedTime = System.currentTimeMillis() - seekState.startTime
        val speedLevel = determineSpeedLevel(elapsedTime, seekConfig.accelerationThresholds)
        val currentSpeed = seekConfig.speedLevels.getOrElse(speedLevel) { seekConfig.speedLevels.last() }
        
        // Calculate seek amount based on speed and direction
        val seekAmount = calculateSeekAmount(currentSpeed, seekState.direction)
        
        onUpdate(seekAmount, currentSpeed, seekState.direction, speedLevel)
        
        // Provide haptic feedback for speed level changes
        if (seekConfig.hapticFeedbackEnabled && speedLevel != seekState.speedLevel) {
            viewModel.provideHapticFeedback(
                intensity = when (speedLevel) {
                    0, 1 -> 20L
                    2, 3 -> 40L
                    4, 5 -> 60L
                    else -> 80L
                }
            )
        }
        
        delay(100) // Update every 100ms
    }
}

/**
 * Determines seek direction based on touch movement
 */
private fun determineSeekDirection(
    currentPosition: Offset,
    startPosition: Offset,
    smoothingFactor: Float
): SeekDirection {
    val deltaX = currentPosition.x - startPosition.x
    val threshold = 30f * smoothingFactor
    
    return when {
        deltaX > threshold -> SeekDirection.FORWARD
        deltaX < -threshold -> SeekDirection.BACKWARD
        else -> SeekDirection.NONE
    }
}

/**
 * Determines speed level based on elapsed time
 */
private fun determineSpeedLevel(elapsedTime: Long, thresholds: List<Long>): Int {
    for (i in thresholds.indices.reversed()) {
        if (elapsedTime >= thresholds[i]) {
            return i
        }
    }
    return 0
}

/**
 * Calculates seek amount based on speed and direction
 */
private fun calculateSeekAmount(speed: Float, direction: SeekDirection): Long {
    val baseSeekAmount = 1000L // 1 second base
    val seekAmount = (baseSeekAmount * speed).toLong()
    
    return when (direction) {
        SeekDirection.FORWARD -> seekAmount
        SeekDirection.BACKWARD -> -seekAmount
        SeekDirection.NONE -> 0L
    }
}