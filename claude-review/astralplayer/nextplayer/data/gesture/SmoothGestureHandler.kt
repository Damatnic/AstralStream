package com.astralplayer.nextplayer.data.gesture

import android.view.VelocityTracker
import android.view.MotionEvent
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import com.astralplayer.nextplayer.data.GestureType

/**
 * Smooth gesture handler that matches MX Player's fluid gesture feel
 * with proper acceleration curves and momentum
 */
class SmoothGestureHandler(
    private val onSeekUpdate: (Float) -> Unit,
    private val onVolumeUpdate: (Float) -> Unit,
    private val onBrightnessUpdate: (Float) -> Unit
) {
    
    // Smoothing parameters for MX Player-like feel
    private val GESTURE_SMOOTHING_FACTOR = 0.15f
    private val MOMENTUM_FRICTION = 0.95f
    private val VELOCITY_THRESHOLD = 50f
    private val ACCELERATION_CURVE = 2.2f // Power curve for natural acceleration
    
    // State tracking
    private var lastGestureTime = 0L
    private var gestureVelocity = Offset.Zero
    private var smoothedPosition = Offset.Zero
    private var momentumJob: Job? = null
    
    // Manual velocity tracking (VelocityTracker requires MotionEvent)
    private var velocityHistory = mutableListOf<Pair<Long, Offset>>()
    
    // Gesture state
    data class GestureState(
        val startPosition: Offset = Offset.Zero,
        val currentPosition: Offset = Offset.Zero,
        val velocity: Offset = Offset.Zero,
        val isActive: Boolean = false,
        val gestureType: GestureType? = null
    )
    
    private var gestureState = GestureState()
    
    /**
     * Start gesture tracking with smooth initialization
     */
    fun startGesture(position: Offset, gestureType: GestureType) {
        // Cancel any ongoing momentum
        momentumJob?.cancel()
        
        // Reset velocity tracking
        velocityHistory.clear()
        
        // Initialize state
        gestureState = GestureState(
            startPosition = position,
            currentPosition = position,
            velocity = Offset.Zero,
            isActive = true,
            gestureType = gestureType
        )
        
        smoothedPosition = position
        lastGestureTime = System.currentTimeMillis()
    }
    
    /**
     * Update gesture with smooth interpolation
     */
    fun updateGesture(position: Offset, timestamp: Long) {
        if (!gestureState.isActive) return
        
        val deltaTime = (timestamp - lastGestureTime).coerceAtLeast(1)
        val delta = position - gestureState.currentPosition
        
        // Track velocity manually
        velocityHistory.add(timestamp to position)
        // Keep only recent history (last 100ms)
        velocityHistory.removeAll { timestamp - it.first > 100 }
        
        // Apply smoothing with acceleration curve
        val smoothingFactor = calculateSmoothingFactor(delta, deltaTime)
        smoothedPosition = Offset(
            smoothedPosition.x + (position.x - smoothedPosition.x) * smoothingFactor,
            smoothedPosition.y + (position.y - smoothedPosition.y) * smoothingFactor
        )
        
        // Calculate velocity with smoothing
        val instantVelocity = Offset(
            (smoothedPosition.x - gestureState.currentPosition.x) / deltaTime * 1000,
            (smoothedPosition.y - gestureState.currentPosition.y) / deltaTime * 1000
        )
        
        gestureVelocity = Offset(
            gestureVelocity.x * 0.7f + instantVelocity.x * 0.3f,
            gestureVelocity.y * 0.7f + instantVelocity.y * 0.3f
        )
        
        // Update state
        gestureState = gestureState.copy(
            currentPosition = smoothedPosition,
            velocity = gestureVelocity
        )
        
        // Apply gesture based on type
        applyGesture(smoothedPosition - gestureState.startPosition)
        
        lastGestureTime = timestamp
    }
    
    /**
     * End gesture with momentum
     */
    fun endGesture() {
        if (!gestureState.isActive) return
        
        gestureState = gestureState.copy(isActive = false)
        
        // Calculate final velocity from history
        val currentTime = System.currentTimeMillis()
        val recentHistory = velocityHistory.filter { currentTime - it.first < 50 }
        
        var velocityX = 0f
        var velocityY = 0f
        
        if (recentHistory.size >= 2) {
            val oldest = recentHistory.first()
            val newest = recentHistory.last()
            val timeDelta = (newest.first - oldest.first).toFloat()
            if (timeDelta > 0) {
                velocityX = (newest.second.x - oldest.second.x) / timeDelta * 1000
                velocityY = (newest.second.y - oldest.second.y) / timeDelta * 1000
            }
        }
        
        // Start momentum animation if velocity is significant
        if (abs(velocityX) > VELOCITY_THRESHOLD || abs(velocityY) > VELOCITY_THRESHOLD) {
            startMomentumAnimation(Offset(velocityX, velocityY))
        }
    }
    
    /**
     * Calculate dynamic smoothing factor based on gesture speed
     */
    private fun calculateSmoothingFactor(delta: Offset, deltaTime: Long): Float {
        val speed = delta.getDistance() / deltaTime
        
        // Apply acceleration curve for natural feel
        val normalizedSpeed = (speed / 10f).coerceIn(0f, 1f)
        val acceleratedSpeed = normalizedSpeed.pow(1f / ACCELERATION_CURVE)
        
        // Interpolate between min and max smoothing
        val minSmoothing = 0.08f
        val maxSmoothing = 0.25f
        
        return minSmoothing + (maxSmoothing - minSmoothing) * acceleratedSpeed
    }
    
    /**
     * Start momentum animation for smooth deceleration
     */
    private fun startMomentumAnimation(initialVelocity: Offset) {
        momentumJob?.cancel()
        
        momentumJob = CoroutineScope(Dispatchers.Main).launch {
            var velocity = initialVelocity
            var position = gestureState.currentPosition
            val startTime = System.currentTimeMillis()
            
            while (velocity.getDistance() > 10f) {
                delay(16) // 60 FPS
                
                val deltaTime = (System.currentTimeMillis() - startTime) / 1000f
                
                // Apply friction with easing
                velocity = Offset(
                    velocity.x * MOMENTUM_FRICTION,
                    velocity.y * MOMENTUM_FRICTION
                )
                
                // Update position
                position = Offset(
                    position.x + velocity.x * 0.016f,
                    position.y + velocity.y * 0.016f
                )
                
                // Apply gesture with decreasing intensity
                val intensity = (velocity.getDistance() / initialVelocity.getDistance()).coerceIn(0f, 1f)
                applyGestureWithIntensity(position - gestureState.startPosition, intensity)
            }
        }
    }
    
    /**
     * Apply gesture based on type with smoothing
     */
    private fun applyGesture(delta: Offset) {
        when (gestureState.gestureType) {
            GestureType.HORIZONTAL_SEEK -> {
                // Smooth seeking with non-linear response curve
                val seekDelta = delta.x.pow(1.3f) * sign(delta.x) * 0.001f
                onSeekUpdate(seekDelta)
            }
            GestureType.VERTICAL_VOLUME -> {
                // Smooth volume adjustment
                val volumeDelta = -delta.y * 0.002f
                onVolumeUpdate(volumeDelta)
            }
            GestureType.VERTICAL_BRIGHTNESS -> {
                // Smooth brightness adjustment
                val brightnessDelta = -delta.y * 0.002f
                onBrightnessUpdate(brightnessDelta)
            }
            else -> {}
        }
    }
    
    /**
     * Apply gesture with intensity for momentum
     */
    private fun applyGestureWithIntensity(delta: Offset, intensity: Float) {
        when (gestureState.gestureType) {
            GestureType.HORIZONTAL_SEEK -> {
                val seekDelta = delta.x * 0.0005f * intensity
                onSeekUpdate(seekDelta)
            }
            else -> {}
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        momentumJob?.cancel()
        velocityHistory.clear()
    }
}

/**
 * Gesture smoothing configuration for MX Player-like experience
 */
data class GestureSmoothingConfig(
    val enableSmoothing: Boolean = true,
    val enableMomentum: Boolean = true,
    val smoothingFactor: Float = 0.15f,
    val momentumFriction: Float = 0.95f,
    val accelerationCurve: Float = 2.2f,
    val velocityThreshold: Float = 50f,
    val hapticFeedbackIntensity: Float = 0.7f
)

/**
 * Enhanced gesture detector with MX Player-style smoothness
 */
@Composable
fun rememberSmoothGestureHandler(
    config: GestureSmoothingConfig = GestureSmoothingConfig(),
    onSeekUpdate: (Float) -> Unit,
    onVolumeUpdate: (Float) -> Unit,
    onBrightnessUpdate: (Float) -> Unit
): SmoothGestureHandler {
    return remember {
        SmoothGestureHandler(
            onSeekUpdate = onSeekUpdate,
            onVolumeUpdate = onVolumeUpdate,
            onBrightnessUpdate = onBrightnessUpdate
        )
    }
}