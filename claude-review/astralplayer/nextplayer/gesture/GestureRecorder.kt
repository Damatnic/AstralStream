package com.astralplayer.nextplayer.gesture

import android.graphics.PointF
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records and manages custom gesture patterns
 */
@Singleton
class GestureRecorder @Inject constructor() {
    
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState
    
    private val currentPath = mutableListOf<GesturePoint>()
    private var startTime = 0L
    
    fun startRecording() {
        _recordingState.value = RecordingState.RECORDING
        currentPath.clear()
        startTime = System.currentTimeMillis()
    }
    
    fun recordGesturePoint(point: PointF) {
        if (_recordingState.value != RecordingState.RECORDING) return
        
        currentPath.add(
            GesturePoint(
                x = point.x,
                y = point.y,
                timestamp = System.currentTimeMillis() - startTime
            )
        )
    }
    
    fun stopRecording(): RecordedGesture? {
        if (_recordingState.value != RecordingState.RECORDING) return null
        
        _recordingState.value = RecordingState.IDLE
        
        if (currentPath.size < MIN_POINTS_FOR_GESTURE) {
            return null
        }
        
        val gesture = RecordedGesture(
            id = System.currentTimeMillis().toString(),
            path = normalizeGesturePath(currentPath),
            duration = currentPath.lastOrNull()?.timestamp ?: 0L,
            fingerCount = 1 // Can be extended for multi-finger recording
        )
        
        currentPath.clear()
        return gesture
    }
    
    fun cancelRecording() {
        _recordingState.value = RecordingState.IDLE
        currentPath.clear()
    }
    
    /**
     * Normalizes the gesture path to be scale and position independent
     */
    private fun normalizeGesturePath(path: List<GesturePoint>): List<GesturePoint> {
        if (path.isEmpty()) return emptyList()
        
        // Find bounding box
        val minX = path.minOf { it.x }
        val maxX = path.maxOf { it.x }
        val minY = path.minOf { it.y }
        val maxY = path.maxOf { it.y }
        
        val width = maxX - minX
        val height = maxY - minY
        val scale = maxOf(width, height)
        
        if (scale == 0f) return path
        
        // Normalize to 0-1 range
        return path.map { point ->
            GesturePoint(
                x = (point.x - minX) / scale,
                y = (point.y - minY) / scale,
                timestamp = point.timestamp
            )
        }
    }
    
    /**
     * Matches a recorded gesture against a template
     */
    fun matchGesture(
        recordedPath: List<GesturePoint>,
        templatePath: List<GesturePoint>,
        threshold: Float = 0.8f
    ): Boolean {
        if (recordedPath.isEmpty() || templatePath.isEmpty()) return false
        
        val normalizedRecorded = normalizeGesturePath(recordedPath)
        val normalizedTemplate = normalizeGesturePath(templatePath)
        
        // Simple DTW (Dynamic Time Warping) for gesture matching
        val similarity = calculateDTWSimilarity(normalizedRecorded, normalizedTemplate)
        
        return similarity >= threshold
    }
    
    private fun calculateDTWSimilarity(
        path1: List<GesturePoint>,
        path2: List<GesturePoint>
    ): Float {
        val n = path1.size
        val m = path2.size
        
        // Create DTW matrix
        val dtw = Array(n + 1) { FloatArray(m + 1) { Float.MAX_VALUE } }
        dtw[0][0] = 0f
        
        // Fill DTW matrix
        for (i in 1..n) {
            for (j in 1..m) {
                val cost = calculateDistance(path1[i - 1], path2[j - 1])
                dtw[i][j] = cost + minOf(
                    dtw[i - 1][j],    // insertion
                    dtw[i][j - 1],    // deletion
                    dtw[i - 1][j - 1] // match
                )
            }
        }
        
        // Convert distance to similarity (0-1)
        val maxDistance = kotlin.math.sqrt(2f) * maxOf(n, m)
        val distance = dtw[n][m]
        
        return 1f - (distance / maxDistance).coerceIn(0f, 1f)
    }
    
    private fun calculateDistance(p1: GesturePoint, p2: GesturePoint): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    enum class RecordingState {
        IDLE,
        RECORDING,
        PROCESSING
    }
    
    data class GesturePoint(
        val x: Float,
        val y: Float,
        val timestamp: Long
    )
    
    data class RecordedGesture(
        val id: String,
        val path: List<GesturePoint>,
        val duration: Long,
        val fingerCount: Int
    )
    
    companion object {
        private const val MIN_POINTS_FOR_GESTURE = 5
    }
}