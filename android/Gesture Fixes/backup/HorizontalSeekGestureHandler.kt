package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Velocity
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Enhanced horizontal seek gesture handler with precise control and preview
 */
class HorizontalSeekGestureHandler(
    private val onSeekStart: (startPosition: Long) -> Unit,
    private val onSeekUpdate: (delta: Long, targetPosition: Long, velocity: Float) -> Unit,
    private val onSeekEnd: (finalPosition: Long, success: Boolean) -> Unit,
    private val getCurrentPosition: () -> Long,
    private val getDuration: () -> Long,
    private val onRequestThumbnail: (position: Long) -> Unit
) {
    
    private var isActive = false
    private var startPosition = Offset.Zero
    private var startTime = 0L
    private var startVideoPosition = 0L
    private var accumulatedDelta = 0f
    private var lastUpdateTime = 0L
    private var velocityTracker = VelocityTracker()
    
    suspend fun PointerInputScope.detectHorizontalSeekGesture(
        settings: SeekingGestureSettings
    ) = coroutineScope {
        awaitEachGesture {
            if (!settings.isEnabled) return@awaitEachGesture
            
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            startGesture(firstDown, settings)
            
            var currentPointer = firstDown
            do {
                val event = awaitPointerEvent()
                currentPointer = event.changes.firstOrNull { !it.isConsumed } ?: currentPointer
                
                if (isActive) {
                    updateGesture(currentPointer, settings)
                    currentPointer.consume()
                }
                
            } while (event.changes.any { !it.isConsumed })
            
            endGesture(settings)
        }
    }
    
    private fun startGesture(firstDown: PointerInputChange, settings: SeekingGestureSettings) {
        startPosition = firstDown.position
        startTime = System.currentTimeMillis()
        startVideoPosition = getCurrentPosition()
        accumulatedDelta = 0f
        lastUpdateTime = startTime
        isActive = false // Will be activated when minimum distance is reached
        velocityTracker.reset()
        velocityTracker.addPosition(startTime, firstDown.position)
        
        onSeekStart(startVideoPosition)
    }
    
    private fun updateGesture(pointer: PointerInputChange, settings: SeekingGestureSettings) {
        val currentTime = System.currentTimeMillis()
        val currentPosition = pointer.position
        val deltaX = currentPosition.x - startPosition.x
        val deltaY = currentPosition.y - startPosition.y
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
        
        velocityTracker.addPosition(currentTime, currentPosition)
        
        // Check if we should activate the gesture
        if (!isActive && distance >= settings.minimumSwipeDistance) {
            // Ensure this is primarily a horizontal gesture
            val horizontalRatio = abs(deltaX) / (abs(deltaY) + 1f) // +1 to avoid division by zero
            if (horizontalRatio >= 1.5f) { // Must be more horizontal than vertical
                isActive = true
            }
        }
        
        if (!isActive) return
        
        // Calculate seek delta based on horizontal movement
        val rawDelta = deltaX * settings.sensitivity
        val timeDelta = currentTime - lastUpdateTime
        
        // Apply velocity-based scaling for more responsive seeking
        val velocity = velocityTracker.calculateVelocity()
        val velocityMultiplier = 1f + (abs(velocity.x) / 1000f) * settings.velocityMultiplier
        val adjustedDelta = rawDelta * velocityMultiplier
        
        // Convert pixel delta to time delta
        val screenWidth = size.width.toFloat()
        val videoDuration = getDuration()
        val normalizedDelta = adjustedDelta / screenWidth
        val timeDeltaMs = (normalizedDelta * videoDuration).toLong()
        
        // Apply fine seek mode if enabled
        val finalTimeDelta = if (settings.enableFineSeek && abs(normalizedDelta) < settings.fineSeekThreshold) {
            // Fine seek mode: smaller movements for precise control
            (timeDeltaMs * 0.3f).toLong()
        } else {
            timeDeltaMs
        }
        
        // Calculate target position
        val targetPosition = (startVideoPosition + finalTimeDelta).coerceIn(0L, videoDuration)
        
        // Update seek preview if enabled
        if (settings.enableSeekPreview && timeDelta > 100) { // Throttle thumbnail requests
            onRequestThumbnail(targetPosition)
        }
        
        // Send update
        onSeekUpdate(finalTimeDelta, targetPosition, velocity.x)
        
        lastUpdateTime = currentTime
        accumulatedDelta = adjustedDelta
    }
    
    private fun endGesture(settings: SeekingGestureSettings) {
        if (!isActive) {
            onSeekEnd(startVideoPosition, false)
            return
        }
        
        val finalVelocity = velocityTracker.calculateVelocity()
        val videoDuration = getDuration()
        
        // Calculate final position with momentum if velocity is high
        var finalDelta = accumulatedDelta
        if (abs(finalVelocity.x) > 500f) { // High velocity threshold
            val momentumDelta = finalVelocity.x * 0.1f // 100ms of momentum
            finalDelta += momentumDelta
        }
        
        // Convert to time delta
        val screenWidth = size.width.toFloat()
        val normalizedDelta = finalDelta / screenWidth
        val timeDeltaMs = (normalizedDelta * videoDuration).toLong()
        val finalPosition = (startVideoPosition + timeDeltaMs).coerceIn(0L, videoDuration)
        
        onSeekEnd(finalPosition, true)
        
        // Reset state
        isActive = false
        velocityTracker.reset()
    }
}

/**
 * Velocity tracker for smooth gesture recognition
 */
private class VelocityTracker {
    private val positions = mutableListOf<TimedPosition>()
    private val maxHistorySize = 10
    
    data class TimedPosition(val time: Long, val position: Offset)
    
    fun addPosition(time: Long, position: Offset) {
        positions.add(TimedPosition(time, position))
        if (positions.size > maxHistorySize) {
            positions.removeAt(0)
        }
    }
    
    fun calculateVelocity(): Velocity {
        if (positions.size < 2) return Velocity.Zero
        
        val recent = positions.takeLast(5) // Use last 5 positions for velocity calculation
        if (recent.size < 2) return Velocity.Zero
        
        val first = recent.first()
        val last = recent.last()
        val timeDelta = (last.time - first.time).toFloat()
        
        if (timeDelta <= 0) return Velocity.Zero
        
        val positionDelta = last.position - first.position
        return Velocity(
            x = (positionDelta.x / timeDelta) * 1000f, // Convert to pixels per second
            y = (positionDelta.y / timeDelta) * 1000f
        )
    }
    
    fun reset() {
        positions.clear()
    }
}

/**
 * Seek preview manager for thumbnail generation and display
 */
class SeekPreviewManager(
    private val onThumbnailGenerated: (position: Long, thumbnail: android.graphics.Bitmap?) -> Unit,
    private val onPreviewUpdate: (position: Long, timeText: String, progress: Float) -> Unit
) {
    private var thumbnailGenerationJob: Job? = null
    private val thumbnailCache = mutableMapOf<Long, android.graphics.Bitmap?>()
    private val maxCacheSize = 50
    
    fun requestThumbnail(position: Long, videoDuration: Long) {
        // Cancel previous thumbnail generation
        thumbnailGenerationJob?.cancel()
        
        // Check cache first
        val cachedThumbnail = thumbnailCache[position]
        if (cachedThumbnail != null) {
            onThumbnailGenerated(position, cachedThumbnail)
            updatePreview(position, videoDuration)
            return
        }
        
        // Generate new thumbnail
        thumbnailGenerationJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val thumbnail = generateThumbnail(position)
                
                // Cache the thumbnail
                if (thumbnailCache.size >= maxCacheSize) {
                    // Remove oldest entry
                    val oldestKey = thumbnailCache.keys.first()
                    thumbnailCache.remove(oldestKey)
                }
                thumbnailCache[position] = thumbnail
                
                withContext(Dispatchers.Main) {
                    onThumbnailGenerated(position, thumbnail)
                    updatePreview(position, videoDuration)
                }
            } catch (e: Exception) {
                // Handle thumbnail generation failure
                withContext(Dispatchers.Main) {
                    onThumbnailGenerated(position, null)
                    updatePreview(position, videoDuration)
                }
            }
        }
    }
    
    private fun updatePreview(position: Long, videoDuration: Long) {
        val timeText = formatTime(position)
        val progress = if (videoDuration > 0) position.toFloat() / videoDuration else 0f
        onPreviewUpdate(position, timeText, progress)
    }
    
    private suspend fun generateThumbnail(position: Long): android.graphics.Bitmap? {
        // This would integrate with ExoPlayer's thumbnail generation
        // For now, return null as a placeholder
        delay(50) // Simulate thumbnail generation time
        return null
    }
    
    fun clearCache() {
        thumbnailCache.clear()
        thumbnailGenerationJob?.cancel()
    }
    
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}

/**
 * Seek gesture analytics for adaptive learning
 */
class SeekGestureAnalytics {
    private val gestureData = mutableListOf<SeekGestureData>()
    
    data class SeekGestureData(
        val startTime: Long,
        val endTime: Long,
        val startPosition: Long,
        val endPosition: Long,
        val distance: Float,
        val velocity: Float,
        val accuracy: Float, // How close to intended position
        val success: Boolean
    )
    
    fun recordGesture(
        startTime: Long,
        endTime: Long,
        startPosition: Long,
        endPosition: Long,
        distance: Float,
        velocity: Float,
        success: Boolean
    ) {
        val accuracy = calculateAccuracy(startPosition, endPosition, distance)
        val data = SeekGestureData(
            startTime, endTime, startPosition, endPosition,
            distance, velocity, accuracy, success
        )
        
        gestureData.add(data)
        
        // Keep only recent data to prevent memory issues
        if (gestureData.size > 1000) {
            gestureData.removeAt(0)
        }
    }
    
    private fun calculateAccuracy(startPos: Long, endPos: Long, distance: Float): Float {
        // Simple accuracy calculation - could be more sophisticated
        val timeDelta = abs(endPos - startPos)
        val expectedDistance = timeDelta / 1000f // Rough estimate
        return if (expectedDistance > 0) {
            1f - abs(distance - expectedDistance) / expectedDistance
        } else {
            1f
        }
    }
    
    fun getAverageAccuracy(): Float {
        return if (gestureData.isNotEmpty()) {
            gestureData.map { it.accuracy }.average().toFloat()
        } else {
            1f
        }
    }
    
    fun getAverageVelocity(): Float {
        return if (gestureData.isNotEmpty()) {
            gestureData.map { it.velocity }.average().toFloat()
        } else {
            0f
        }
    }
    
    fun getSuccessRate(): Float {
        return if (gestureData.isNotEmpty()) {
            gestureData.count { it.success }.toFloat() / gestureData.size
        } else {
            1f
        }
    }
    
    fun getSuggestedSensitivity(currentSensitivity: Float): Float {
        val accuracy = getAverageAccuracy()
        val velocity = getAverageVelocity()
        
        return when {
            accuracy < 0.7f && velocity > 1000f -> currentSensitivity * 0.9f // Too fast, reduce sensitivity
            accuracy < 0.7f && velocity < 500f -> currentSensitivity * 1.1f // Too slow, increase sensitivity
            else -> currentSensitivity
        }.coerceIn(0.1f, 3.0f)
    }
}