package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlin.math.*

/**
 * Enhanced direction change handler for long press gestures
 * Implements requirements 3.2: Direction change capabilities during long press
 */
@Composable
fun DirectionChangeHandler(
    viewModel: PlayerViewModel,
    gestureSettings: GestureSettings = GestureSettings(),
    onDirectionChange: (SeekDirection, Float) -> Unit = { _, _ -> },
    onSpeedChange: (Float, SeekDirection) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Direction change state
    var isLongPressing by remember { mutableStateOf(false) }
    var gestureStartPosition by remember { mutableStateOf(Offset.Zero) }
    var currentDirection by remember { mutableStateOf(SeekDirection.FORWARD) }
    var directionConfidence by remember { mutableFloatStateOf(0f) }
    var smoothTransitionProgress by remember { mutableFloatStateOf(0f) }
    
    // Direction change configuration
    val directionConfig = remember {
        DirectionChangeConfiguration(
            detectionThreshold = 50f, // pixels
            confidenceThreshold = 0.7f,
            smoothTransitionDuration = 300, // ms
            hapticFeedbackIntensity = 40L,
            directionLockTimeout = 200L // ms
        )
    }
    
    // Smooth transition animation
    val transitionAnimatable = remember { Animatable(0f) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        if (!gestureSettings.longPress.enabled) return@detectTapGestures
                        
                        isLongPressing = true
                        gestureStartPosition = offset
                        currentDirection = SeekDirection.FORWARD
                        directionConfidence = 1f
                        
                        // Start long press seek
                        viewModel.startLongPressSeek(offset)
                        
                        // Provide initial haptic feedback
                        scope.launch {
                            viewModel.provideHapticFeedback(directionConfig.hapticFeedbackIntensity)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (isLongPressing) {
                            gestureStartPosition = offset
                        }
                    },
                    onDragEnd = {
                        if (isLongPressing) {
                            isLongPressing = false
                            viewModel.endLongPressSeek()
                            
                            // Reset state
                            currentDirection = SeekDirection.FORWARD
                            directionConfidence = 0f
                            smoothTransitionProgress = 0f
                        }
                    }
                ) { change, _ ->
                    if (isLongPressing) {
                        val currentPosition = change.position
                        val horizontalDelta = currentPosition.x - gestureStartPosition.x
                        
                        // Detect direction change
                        val detectedDirection = detectDirection(
                            horizontalDelta,
                            directionConfig.detectionThreshold
                        )
                        
                        // Calculate confidence based on movement distance
                        val newConfidence = calculateDirectionConfidence(
                            horizontalDelta,
                            directionConfig.detectionThreshold
                        )
                        
                        // Handle direction change with smooth transition
                        if (detectedDirection != currentDirection && 
                            newConfidence >= directionConfig.confidenceThreshold) {
                            
                            handleDirectionChange(
                                newDirection = detectedDirection,
                                confidence = newConfidence,
                                transitionAnimatable = transitionAnimatable,
                                directionConfig = directionConfig,
                                onDirectionChange = onDirectionChange,
                                onHapticFeedback = {
                                    scope.launch {
                                        viewModel.provideHapticFeedback(it)
                                    }
                                }
                            ) { direction ->
                                currentDirection = direction
                                directionConfidence = newConfidence
                            }
                        }
                        
                        // Update seek with current direction and speed
                        val currentSpeed = calculateSpeedFromMovement(
                            horizontalDelta,
                            directionConfig.detectionThreshold
                        )
                        
                        onSpeedChange(currentSpeed, currentDirection)
                        viewModel.updateLongPressSeek(
                            speed = currentSpeed,
                            direction = currentDirection,
                            position = currentPosition
                        )
                    }
                }
            }
    )
}

/**
 * Configuration for direction change detection and transitions
 */
data class DirectionChangeConfiguration(
    val detectionThreshold: Float = 50f,
    val confidenceThreshold: Float = 0.7f,
    val smoothTransitionDuration: Int = 300,
    val hapticFeedbackIntensity: Long = 40L,
    val directionLockTimeout: Long = 200L
)

/**
 * Detects seek direction based on horizontal movement
 */
private fun detectDirection(horizontalDelta: Float, threshold: Float): SeekDirection {
    return when {
        horizontalDelta > threshold -> SeekDirection.FORWARD
        horizontalDelta < -threshold -> SeekDirection.BACKWARD
        else -> SeekDirection.NONE
    }
}

/**
 * Calculates confidence level for direction detection
 */
private fun calculateDirectionConfidence(horizontalDelta: Float, threshold: Float): Float {
    val absDistance = abs(horizontalDelta)
    return if (absDistance >= threshold) {
        min(1f, absDistance / (threshold * 2f))
    } else {
        absDistance / threshold
    }
}

/**
 * Handles smooth direction changes with animation and haptic feedback
 */
private fun handleDirectionChange(
    newDirection: SeekDirection,
    confidence: Float,
    transitionAnimatable: Animatable<Float, AnimationVector1D>,
    directionConfig: DirectionChangeConfiguration,
    onDirectionChange: (SeekDirection, Float) -> Unit,
    onHapticFeedback: (Long) -> Unit,
    onDirectionUpdate: (SeekDirection) -> Unit
) {
    // Provide haptic feedback for direction change
    val hapticIntensity = (directionConfig.hapticFeedbackIntensity * confidence).toLong()
    onHapticFeedback(hapticIntensity)
    
    // Trigger smooth transition animation
    CoroutineScope(Dispatchers.Main).launch {
        transitionAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = directionConfig.smoothTransitionDuration,
                easing = FastOutSlowInEasing
            )
        )
        
        // Update direction after animation
        onDirectionUpdate(newDirection)
        onDirectionChange(newDirection, confidence)
        
        // Reset animation
        transitionAnimatable.snapTo(0f)
    }
}

/**
 * Calculates speed multiplier based on movement intensity
 */
private fun calculateSpeedFromMovement(horizontalDelta: Float, threshold: Float): Float {
    val absDistance = abs(horizontalDelta)
    return when {
        absDistance < threshold -> 1f
        absDistance < threshold * 2 -> 1.5f
        absDistance < threshold * 3 -> 2f
        absDistance < threshold * 4 -> 3f
        else -> 4f
    }
}

/**
 * Enhanced direction change detector with visual feedback
 */
@Composable
fun EnhancedDirectionChangeDetector(
    viewModel: PlayerViewModel,
    gestureSettings: GestureSettings = GestureSettings(),
    onDirectionChange: (SeekDirection) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onVisualFeedback: (DirectionChangeVisualState) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Enhanced state management
    var detectorState by remember { mutableStateOf(DirectionChangeDetectorState()) }
    var visualState by remember { mutableStateOf(DirectionChangeVisualState()) }
    
    // Movement tracking
    val movementTracker = remember { MovementTracker() }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        detectorState = detectorState.copy(
                            isActive = true,
                            startPosition = offset,
                            currentPosition = offset
                        )
                        movementTracker.reset(offset)
                    },
                    onDragEnd = {
                        detectorState = detectorState.copy(isActive = false)
                        visualState = visualState.copy(isVisible = false)
                        onVisualFeedback(visualState)
                    }
                ) { change, _ ->
                    if (detectorState.isActive) {
                        val currentPosition = change.position
                        detectorState = detectorState.copy(currentPosition = currentPosition)
                        
                        // Track movement
                        movementTracker.addPoint(currentPosition)
                        
                        // Analyze movement pattern
                        val analysis = movementTracker.analyzeMovement()
                        
                        // Detect direction change
                        if (analysis.hasDirectionChange) {
                            val newDirection = analysis.dominantDirection
                            val confidence = analysis.confidence
                            
                            if (newDirection != detectorState.currentDirection && 
                                confidence > 0.8f) {
                                
                                // Update state
                                detectorState = detectorState.copy(
                                    currentDirection = newDirection,
                                    confidence = confidence,
                                    lastDirectionChangeTime = System.currentTimeMillis()
                                )
                                
                                // Update visual feedback
                                visualState = visualState.copy(
                                    isVisible = true,
                                    direction = newDirection,
                                    confidence = confidence,
                                    transitionProgress = 0f
                                )
                                
                                // Callbacks
                                onDirectionChange(newDirection)
                                onSpeedChange(analysis.speed)
                                onVisualFeedback(visualState)
                                
                                // Haptic feedback
                                scope.launch {
                                    viewModel.provideHapticFeedback(
                                        (50L * confidence).toLong()
                                    )
                                }
                            }
                        }
                    }
                }
            }
    )
}

/**
 * State for direction change detector
 */
data class DirectionChangeDetectorState(
    val isActive: Boolean = false,
    val startPosition: Offset = Offset.Zero,
    val currentPosition: Offset = Offset.Zero,
    val currentDirection: SeekDirection = SeekDirection.NONE,
    val confidence: Float = 0f,
    val lastDirectionChangeTime: Long = 0L
)

/**
 * Visual state for direction change feedback
 */
data class DirectionChangeVisualState(
    val isVisible: Boolean = false,
    val direction: SeekDirection = SeekDirection.NONE,
    val confidence: Float = 0f,
    val transitionProgress: Float = 0f,
    val showSpeedIndicator: Boolean = false,
    val currentSpeed: Float = 1f
)

/**
 * Movement tracker for analyzing gesture patterns
 */
class MovementTracker {
    private val points = mutableListOf<MovementPoint>()
    private val maxPoints = 20
    
    fun reset(startPosition: Offset) {
        points.clear()
        addPoint(startPosition)
    }
    
    fun addPoint(position: Offset) {
        val timestamp = System.currentTimeMillis()
        points.add(MovementPoint(position, timestamp))
        
        // Keep only recent points
        if (points.size > maxPoints) {
            points.removeAt(0)
        }
    }
    
    fun analyzeMovement(): MovementAnalysis {
        if (points.size < 3) {
            return MovementAnalysis()
        }
        
        val recentPoints = points.takeLast(5)
        val totalDeltaX = recentPoints.last().position.x - recentPoints.first().position.x
        val totalDeltaY = recentPoints.last().position.y - recentPoints.first().position.y
        
        // Determine dominant direction
        val dominantDirection = when {
            abs(totalDeltaX) > abs(totalDeltaY) && abs(totalDeltaX) > 30f -> {
                if (totalDeltaX > 0) SeekDirection.FORWARD else SeekDirection.BACKWARD
            }
            else -> SeekDirection.NONE
        }
        
        // Calculate confidence based on consistency
        val confidence = calculateMovementConfidence(recentPoints)
        
        // Calculate speed based on movement velocity
        val speed = calculateMovementSpeed(recentPoints)
        
        // Detect direction changes
        val hasDirectionChange = detectDirectionChange(points)
        
        return MovementAnalysis(
            dominantDirection = dominantDirection,
            confidence = confidence,
            speed = speed,
            hasDirectionChange = hasDirectionChange
        )
    }
    
    private fun calculateMovementConfidence(points: List<MovementPoint>): Float {
        if (points.size < 2) return 0f
        
        val directions = mutableListOf<Float>()
        for (i in 1 until points.size) {
            val deltaX = points[i].position.x - points[i-1].position.x
            directions.add(deltaX)
        }
        
        // Calculate consistency (lower variance = higher confidence)
        val mean = directions.average().toFloat()
        val variance = directions.map { (it - mean) * (it - mean) }.average().toFloat()
        
        return max(0f, min(1f, 1f - (variance / 1000f)))
    }
    
    private fun calculateMovementSpeed(points: List<MovementPoint>): Float {
        if (points.size < 2) return 1f
        
        val totalDistance = points.zipWithNext { a, b ->
            sqrt((b.position.x - a.position.x).pow(2) + (b.position.y - a.position.y).pow(2))
        }.sum()
        
        val totalTime = points.last().timestamp - points.first().timestamp
        val velocity = if (totalTime > 0) totalDistance / totalTime else 0f
        
        return max(1f, min(4f, 1f + velocity * 0.01f))
    }
    
    private fun detectDirectionChange(points: List<MovementPoint>): Boolean {
        if (points.size < 4) return false
        
        val midPoint = points.size / 2
        val firstHalf = points.subList(0, midPoint)
        val secondHalf = points.subList(midPoint, points.size)
        
        val firstDirection = if (firstHalf.size >= 2) {
            firstHalf.last().position.x - firstHalf.first().position.x
        } else 0f
        
        val secondDirection = if (secondHalf.size >= 2) {
            secondHalf.last().position.x - secondHalf.first().position.x
        } else 0f
        
        // Direction change if signs are different and magnitudes are significant
        return abs(firstDirection) > 20f && abs(secondDirection) > 20f && 
               (firstDirection * secondDirection) < 0
    }
}

/**
 * Data class for movement points
 */
data class MovementPoint(
    val position: Offset,
    val timestamp: Long
)

/**
 * Analysis result for movement patterns
 */
data class MovementAnalysis(
    val dominantDirection: SeekDirection = SeekDirection.NONE,
    val confidence: Float = 0f,
    val speed: Float = 1f,
    val hasDirectionChange: Boolean = false
)