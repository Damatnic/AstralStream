package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Enhanced double tap gesture handler with configurable seek amounts and visual feedback
 */
class DoubleTapGestureHandler(
    private val onDoubleTapSeek: (seekAmount: Long, isForward: Boolean, position: Offset) -> Unit,
    private val onHapticFeedback: (intensity: HapticIntensity) -> Unit,
    private val getCurrentPosition: () -> Long,
    private val getDuration: () -> Long
) {
    
    private var firstTapTime = 0L
    private var firstTapPosition = Offset.Zero
    private var isWaitingForSecondTap = false
    private var doubleTapJob: Job? = null
    
    suspend fun PointerInputScope.detectDoubleTapGesture(
        settings: DoubleTapGestureSettings
    ) = coroutineScope {
        awaitEachGesture {
            if (!settings.isEnabled) return@awaitEachGesture
            
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            val tapTime = System.currentTimeMillis()
            val tapPosition = firstDown.position
            
            // Check if this could be the second tap of a double tap
            if (isWaitingForSecondTap && 
                tapTime - firstTapTime <= settings.tapTimeout &&
                (tapPosition - firstTapPosition).getDistance() <= 50f) {
                
                // This is a double tap!
                handleDoubleTap(tapPosition, settings)
                isWaitingForSecondTap = false
                doubleTapJob?.cancel()
                
            } else {
                // This is potentially the first tap of a double tap
                firstTapTime = tapTime
                firstTapPosition = tapPosition
                isWaitingForSecondTap = true
                
                // Start timeout for second tap
                doubleTapJob?.cancel()
                doubleTapJob = launch {
                    delay(settings.tapTimeout)
                    isWaitingForSecondTap = false
                    // Could handle single tap here if needed
                }
            }
            
            // Wait for tap to complete
            val up = waitForUpOrCancellation()
            if (up == null) {
                // Gesture was cancelled
                isWaitingForSecondTap = false
                doubleTapJob?.cancel()
            }
        }
    }
    
    private fun handleDoubleTap(position: Offset, settings: DoubleTapGestureSettings) {
        val screenWidth = size.width.toFloat()
        val side = determineTouchSide(position, screenWidth, settings.centerDeadZone)
        
        when (side) {
            TouchSide.LEFT -> {
                if (settings.enableLeftSide) {
                    val seekAmount = -settings.leftSideSeekAmount // Negative for backward
                    performSeek(seekAmount, false, position, settings)
                }
            }
            TouchSide.RIGHT -> {
                if (settings.enableRightSide) {
                    val seekAmount = settings.rightSideSeekAmount // Positive for forward
                    performSeek(seekAmount, true, position, settings)
                }
            }
            TouchSide.CENTER -> {
                // In center dead zone - no action
            }
        }
    }
    
    private fun performSeek(seekAmount: Long, isForward: Boolean, position: Offset, settings: DoubleTapGestureSettings) {
        val currentPos = getCurrentPosition()
        val duration = getDuration()
        val targetPosition = (currentPos + seekAmount).coerceIn(0L, duration)
        
        // Provide haptic feedback
        onHapticFeedback(HapticIntensity.MEDIUM)
        
        // Execute the seek
        onDoubleTapSeek(abs(seekAmount), isForward, position)
    }
    
    private fun determineTouchSide(position: Offset, screenWidth: Float, deadZone: Float): TouchSide {
        val deadZoneWidth = screenWidth * deadZone
        val leftBoundary = screenWidth / 2 - deadZoneWidth / 2
        val rightBoundary = screenWidth / 2 + deadZoneWidth / 2
        
        return when {
            position.x < leftBoundary -> TouchSide.LEFT
            position.x > rightBoundary -> TouchSide.RIGHT
            else -> TouchSide.CENTER
        }
    }
    
    fun reset() {
        isWaitingForSecondTap = false
        doubleTapJob?.cancel()
        doubleTapJob = null
    }
}

/**
 * Double tap visual feedback overlay
 */
@Composable
fun DoubleTapFeedbackOverlay(
    isVisible: Boolean,
    seekAmount: Long,
    isForward: Boolean,
    position: Offset,
    screenSize: androidx.compose.ui.unit.IntSize,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    val density = LocalDensity.current
    
    // Animation states
    val scaleAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "doubleTapScale"
    )
    
    val fadeAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "doubleTapFade"
    )
    
    val rippleAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "doubleTapRipple"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(scaleAnimation)
            .alpha(fadeAnimation)
    ) {
        // Position the feedback at the tap location
        val offsetX = with(density) { position.x.toDp() }
        val offsetY = with(density) { position.y.toDp() }
        
        Box(
            modifier = Modifier
                .offset(x = offsetX - 60.dp, y = offsetY - 60.dp)
                .size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ripple effect
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawDoubleTapRipple(
                    center = Offset(size.width / 2, size.height / 2),
                    progress = rippleAnimation,
                    isForward = isForward
                )
            }
            
            // Main feedback card
            Card(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isForward) Color(0xFF4CAF50).copy(alpha = 0.9f) 
                                   else Color(0xFFFF9800).copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Direction icon
                    Icon(
                        imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    // Seek amount
                    Text(
                        text = "${seekAmount / 1000}s",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Side indicator (shows which side was tapped)
        DoubleTapSideIndicator(
            isVisible = isVisible,
            isForward = isForward,
            seekAmount = seekAmount,
            modifier = Modifier.align(
                if (isForward) Alignment.CenterEnd else Alignment.CenterStart
            )
        )
    }
}

@Composable
private fun DoubleTapSideIndicator(
    isVisible: Boolean,
    isForward: Boolean,
    seekAmount: Long,
    modifier: Modifier = Modifier
) {
    val slideAnimation by animateFloatAsState(
        targetValue = if (isVisible) 0f else if (isForward) 100f else -100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sideIndicatorSlide"
    )
    
    Card(
        modifier = modifier
            .padding(24.dp)
            .offset(x = slideAnimation.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E).copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isForward) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${seekAmount / 1000}s",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isForward) "Forward" else "Backward",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
            
            if (isForward) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Compact double tap feedback for minimal UI
 */
@Composable
fun CompactDoubleTapFeedback(
    isVisible: Boolean,
    seekAmount: Long,
    isForward: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    val scaleAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "compactDoubleTapScale"
    )
    
    Box(
        modifier = modifier
            .scale(scaleAnimation)
            .alpha(scaleAnimation),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                    contentDescription = null,
                    tint = if (isForward) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                
                Text(
                    text = "${seekAmount / 1000}s",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Double tap gesture analytics
 */
class DoubleTapGestureAnalytics {
    private val tapData = mutableListOf<DoubleTapData>()
    
    data class DoubleTapData(
        val timestamp: Long,
        val side: TouchSide,
        val seekAmount: Long,
        val isForward: Boolean,
        val tapInterval: Long, // Time between first and second tap
        val accuracy: Float, // How close taps were to each other
        val success: Boolean
    )
    
    fun recordDoubleTap(
        side: TouchSide,
        seekAmount: Long,
        isForward: Boolean,
        tapInterval: Long,
        accuracy: Float,
        success: Boolean
    ) {
        val data = DoubleTapData(
            timestamp = System.currentTimeMillis(),
            side = side,
            seekAmount = seekAmount,
            isForward = isForward,
            tapInterval = tapInterval,
            accuracy = accuracy,
            success = success
        )
        
        tapData.add(data)
        
        // Limit data size
        if (tapData.size > 500) {
            tapData.removeAt(0)
        }
    }
    
    fun getAverageAccuracy(): Float {
        return if (tapData.isNotEmpty()) {
            tapData.map { it.accuracy }.average().toFloat()
        } else {
            1f
        }
    }
    
    fun getSuccessRate(): Float {
        return if (tapData.isNotEmpty()) {
            tapData.count { it.success }.toFloat() / tapData.size
        } else {
            1f
        }
    }
    
    fun getPreferredSide(): TouchSide {
        if (tapData.isEmpty()) return TouchSide.RIGHT
        
        val sideUsage = tapData.groupBy { it.side }
        return sideUsage.maxByOrNull { it.value.size }?.key ?: TouchSide.RIGHT
    }
    
    fun getAverageTapInterval(): Long {
        return if (tapData.isNotEmpty()) {
            tapData.map { it.tapInterval }.average().toLong()
        } else {
            200L
        }
    }
    
    fun getPreferredSeekAmount(isForward: Boolean): Long {
        val relevantData = tapData.filter { it.isForward == isForward }
        return if (relevantData.isNotEmpty()) {
            relevantData.groupBy { it.seekAmount }
                .maxByOrNull { it.value.size }?.key ?: 10000L
        } else {
            10000L
        }
    }
    
    fun getSuggestedSettings(currentSettings: DoubleTapGestureSettings): DoubleTapGestureSettings {
        if (tapData.isEmpty()) return currentSettings
        
        val averageInterval = getAverageTapInterval()
        val preferredForwardAmount = getPreferredSeekAmount(true)
        val preferredBackwardAmount = getPreferredSeekAmount(false)
        val successRate = getSuccessRate()
        
        return currentSettings.copy(
            tapTimeout = if (successRate < 0.7f) {
                // Increase timeout if users are struggling
                (averageInterval * 1.5f).toLong().coerceAtMost(500L)
            } else {
                currentSettings.tapTimeout
            },
            rightSideSeekAmount = preferredForwardAmount,
            leftSideSeekAmount = preferredBackwardAmount
        )
    }
}

/**
 * Double tap training mode for helping users learn the gesture
 */
class DoubleTapTrainingMode {
    private var isTrainingActive = false
    private var trainingStep = 0
    private val maxTrainingSteps = 5
    
    fun startTraining(): Boolean {
        if (isTrainingActive) return false
        
        isTrainingActive = true
        trainingStep = 0
        return true
    }
    
    fun getTrainingInstruction(): String {
        return when (trainingStep) {
            0 -> "Double tap on the right side of the screen to seek forward"
            1 -> "Double tap on the left side of the screen to seek backward"
            2 -> "Try double tapping faster - within 300ms"
            3 -> "Practice double tapping in the center - it should do nothing"
            4 -> "Great! You've mastered double tap gestures"
            else -> "Training complete"
        }
    }
    
    fun processTrainingTap(side: TouchSide, interval: Long): TrainingResult {
        if (!isTrainingActive) return TrainingResult.NOT_IN_TRAINING
        
        val expectedSide = when (trainingStep) {
            0 -> TouchSide.RIGHT
            1 -> TouchSide.LEFT
            2 -> TouchSide.RIGHT // Focus on timing
            3 -> TouchSide.CENTER
            else -> TouchSide.RIGHT
        }
        
        val success = when (trainingStep) {
            0, 1 -> side == expectedSide
            2 -> side == expectedSide && interval < 300L
            3 -> side == expectedSide
            else -> true
        }
        
        if (success) {
            trainingStep++
            if (trainingStep >= maxTrainingSteps) {
                isTrainingActive = false
                return TrainingResult.TRAINING_COMPLETE
            }
            return TrainingResult.STEP_SUCCESS
        } else {
            return TrainingResult.STEP_FAILED
        }
    }
    
    fun isTraining(): Boolean = isTrainingActive
    
    fun getCurrentStep(): Int = trainingStep
    
    fun getProgress(): Float = trainingStep.toFloat() / maxTrainingSteps
    
    enum class TrainingResult {
        NOT_IN_TRAINING, STEP_SUCCESS, STEP_FAILED, TRAINING_COMPLETE
    }
}

// Custom drawing functions
private fun DrawScope.drawDoubleTapRipple(
    center: Offset,
    progress: Float,
    isForward: Boolean
) {
    val maxRadius = size.minDimension / 2
    val rippleRadius = maxRadius * progress
    val rippleAlpha = (1f - progress) * 0.6f
    
    val color = if (isForward) Color(0xFF4CAF50) else Color(0xFFFF9800)
    
    // Draw multiple ripple rings
    repeat(3) { ring ->
        val ringProgress = (progress - ring * 0.2f).coerceIn(0f, 1f)
        val ringRadius = maxRadius * ringProgress
        val ringAlpha = rippleAlpha * (1f - ring * 0.3f)
        
        if (ringRadius > 0f && ringAlpha > 0f) {
            drawCircle(
                color = color,
                radius = ringRadius,
                center = center,
                alpha = ringAlpha
            )
        }
    }
    
    // Draw directional indicators
    if (progress > 0.3f) {
        val indicatorAlpha = ((progress - 0.3f) / 0.7f) * 0.8f
        val arrowSize = 20f
        val arrowOffset = 30f
        
        if (isForward) {
            // Draw forward arrows
            repeat(3) { arrow ->
                val arrowX = center.x + arrowOffset + arrow * 15f
                val arrowY = center.y
                
                drawPath(
                    path = Path().apply {
                        moveTo(arrowX - arrowSize / 2, arrowY - arrowSize / 2)
                        lineTo(arrowX + arrowSize / 2, arrowY)
                        lineTo(arrowX - arrowSize / 2, arrowY + arrowSize / 2)
                    },
                    color = color,
                    alpha = indicatorAlpha * (1f - arrow * 0.3f)
                )
            }
        } else {
            // Draw backward arrows
            repeat(3) { arrow ->
                val arrowX = center.x - arrowOffset - arrow * 15f
                val arrowY = center.y
                
                drawPath(
                    path = Path().apply {
                        moveTo(arrowX + arrowSize / 2, arrowY - arrowSize / 2)
                        lineTo(arrowX - arrowSize / 2, arrowY)
                        lineTo(arrowX + arrowSize / 2, arrowY + arrowSize / 2)
                    },
                    color = color,
                    alpha = indicatorAlpha * (1f - arrow * 0.3f)
                )
            }
        }
    }
}