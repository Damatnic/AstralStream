package com.astralplayer.nextplayer.professional

import android.content.Context
import android.graphics.*
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@UnstableApi
@Singleton
class ProfessionalVideoToolsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val frameExtractor: VideoFrameExtractor,
    private val measurementCalculator: MeasurementCalculator,
    private val annotationManager: AnnotationManager,
    private val analysisExporter: AnalysisExporter
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _toolState = MutableStateFlow<ToolState>(ToolState.Idle)
    val toolState: StateFlow<ToolState> = _toolState.asStateFlow()
    
    private val _currentFrame = MutableStateFlow<VideoFrame?>(null)
    val currentFrame: StateFlow<VideoFrame?> = _currentFrame.asStateFlow()
    
    private val _measurements = MutableStateFlow<List<Measurement>>(emptyList())
    val measurements: StateFlow<List<Measurement>> = _measurements.asStateFlow()
    
    private val _annotations = MutableStateFlow<List<Annotation>>(emptyList())
    val annotations: StateFlow<List<Annotation>> = _annotations.asStateFlow()
    
    private var currentPlayer: ExoPlayer? = null
    private var currentMediaItem: MediaItem? = null
    private var playbackSpeed = 1.0f
    
    /**
     * Initialize professional tools with player
     */
    suspend fun initialize(player: ExoPlayer, mediaItem: MediaItem) {
        withContext(Dispatchers.Main) {
            try {
                _toolState.value = ToolState.Initializing
                
                currentPlayer = player
                currentMediaItem = mediaItem
                
                // Initialize frame extraction
                frameExtractor.initialize(mediaItem)
                
                _toolState.value = ToolState.Ready
                
            } catch (e: Exception) {
                _toolState.value = ToolState.Error(e.message ?: "Initialization failed")
            }
        }
    }
    
    /**
     * Navigate frame by frame
     */
    suspend fun navigateFrame(direction: FrameDirection): VideoFrame? {
        return withContext(Dispatchers.Default) {
            try {
                val currentPosition = currentPlayer?.currentPosition ?: 0L
                val frameRate = getVideoFrameRate()
                val frameDuration = (1000.0 / frameRate).toLong()
                
                val newPosition = when (direction) {
                    FrameDirection.NEXT -> currentPosition + frameDuration
                    FrameDirection.PREVIOUS -> (currentPosition - frameDuration).coerceAtLeast(0)
                }
                
                // Seek to new position
                withContext(Dispatchers.Main) {
                    currentPlayer?.seekTo(newPosition)
                }
                
                // Extract frame at new position
                val frame = frameExtractor.extractFrameAt(newPosition)
                _currentFrame.value = frame
                
                frame
                
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Set precise playback speed for slow-motion analysis
     */
    fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.01f, 2.0f)
        playbackSpeed = clampedSpeed
        
        currentPlayer?.setPlaybackSpeed(clampedSpeed)
    }
    
    /**
     * Add measurement between two points
     */
    fun addDistanceMeasurement(point1: PointF, point2: PointF, realWorldScale: Float = 1.0f): Measurement {
        val pixelDistance = sqrt(
            (point2.x - point1.x).pow(2) + (point2.y - point1.y).pow(2)
        )
        
        val realWorldDistance = pixelDistance * realWorldScale
        
        val measurement = Measurement(
            id = generateMeasurementId(),
            type = MeasurementType.DISTANCE,
            points = listOf(point1, point2),
            pixelValue = pixelDistance,
            realWorldValue = realWorldDistance,
            unit = "mm", // Default unit
            timestamp = System.currentTimeMillis(),
            framePosition = currentPlayer?.currentPosition ?: 0L
        )
        
        val currentMeasurements = _measurements.value.toMutableList()
        currentMeasurements.add(measurement)
        _measurements.value = currentMeasurements
        
        return measurement
    }
    
    /**
     * Add angle measurement between three points
     */
    fun addAngleMeasurement(point1: PointF, vertex: PointF, point2: PointF): Measurement {
        val angle = measurementCalculator.calculateAngle(point1, vertex, point2)
        
        val measurement = Measurement(
            id = generateMeasurementId(),
            type = MeasurementType.ANGLE,
            points = listOf(point1, vertex, point2),
            pixelValue = 0f, // Not applicable for angles
            realWorldValue = angle,
            unit = "degrees",
            timestamp = System.currentTimeMillis(),
            framePosition = currentPlayer?.currentPosition ?: 0L
        )
        
        val currentMeasurements = _measurements.value.toMutableList()
        currentMeasurements.add(measurement)
        _measurements.value = currentMeasurements
        
        return measurement
    }
    
    /**
     * Add area measurement for polygon
     */
    fun addAreaMeasurement(points: List<PointF>, realWorldScale: Float = 1.0f): Measurement {
        val pixelArea = measurementCalculator.calculatePolygonArea(points)
        val realWorldArea = pixelArea * realWorldScale * realWorldScale
        
        val measurement = Measurement(
            id = generateMeasurementId(),
            type = MeasurementType.AREA,
            points = points,
            pixelValue = pixelArea,
            realWorldValue = realWorldArea,
            unit = "mm²",
            timestamp = System.currentTimeMillis(),
            framePosition = currentPlayer?.currentPosition ?: 0L
        )
        
        val currentMeasurements = _measurements.value.toMutableList()
        currentMeasurements.add(measurement)
        _measurements.value = currentMeasurements
        
        return measurement
    }
    
    /**
     * Add annotation to current frame
     */
    fun addAnnotation(
        position: PointF,
        text: String,
        type: AnnotationType = AnnotationType.TEXT
    ): Annotation {
        val annotation = Annotation(
            id = generateAnnotationId(),
            type = type,
            position = position,
            text = text,
            timestamp = System.currentTimeMillis(),
            framePosition = currentPlayer?.currentPosition ?: 0L
        )
        
        val currentAnnotations = _annotations.value.toMutableList()
        currentAnnotations.add(annotation)
        _annotations.value = currentAnnotations
        
        return annotation
    }
    
    /**
     * Add drawing annotation
     */
    fun addDrawingAnnotation(path: Path, paint: Paint): Annotation {
        val annotation = Annotation(
            id = generateAnnotationId(),
            type = AnnotationType.DRAWING,
            position = PointF(0f, 0f), // Not used for drawings
            text = "",
            drawingPath = path,
            drawingPaint = paint,
            timestamp = System.currentTimeMillis(),
            framePosition = currentPlayer?.currentPosition ?: 0L
        )
        
        val currentAnnotations = _annotations.value.toMutableList()
        currentAnnotations.add(annotation)
        _annotations.value = currentAnnotations
        
        return annotation
    }
    
    /**
     * Remove measurement
     */
    fun removeMeasurement(measurementId: String) {
        val currentMeasurements = _measurements.value.toMutableList()
        currentMeasurements.removeAll { it.id == measurementId }
        _measurements.value = currentMeasurements
    }
    
    /**
     * Remove annotation
     */
    fun removeAnnotation(annotationId: String) {
        val currentAnnotations = _annotations.value.toMutableList()
        currentAnnotations.removeAll { it.id == annotationId }
        _annotations.value = currentAnnotations
    }
    
    /**
     * Export analysis report
     */
    suspend fun exportAnalysis(
        format: ExportFormat,
        includeFrames: Boolean = true
    ): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val analysisData = AnalysisData(
                    mediaItem = currentMediaItem!!,
                    measurements = _measurements.value,
                    annotations = _annotations.value,
                    exportTimestamp = System.currentTimeMillis(),
                    playbackSpeed = playbackSpeed
                )
                
                val result = when (format) {
                    ExportFormat.PDF -> analysisExporter.exportToPDF(analysisData, includeFrames)
                    ExportFormat.CSV -> analysisExporter.exportToCSV(analysisData)
                    ExportFormat.JSON -> analysisExporter.exportToJSON(analysisData)
                }
                
                result
                
            } catch (e: Exception) {
                ExportResult(
                    success = false,
                    filePath = null,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Capture current frame with annotations
     */
    suspend fun captureAnnotatedFrame(): Bitmap? {
        return withContext(Dispatchers.Default) {
            try {
                val currentPosition = currentPlayer?.currentPosition ?: 0L
                val frame = frameExtractor.extractFrameAt(currentPosition)
                
                frame?.bitmap?.let { bitmap ->
                    val annotatedBitmap = annotationManager.renderAnnotations(
                        bitmap,
                        _annotations.value.filter { it.framePosition == currentPosition },
                        _measurements.value.filter { it.framePosition == currentPosition }
                    )
                    annotatedBitmap
                }
                
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Get analysis statistics
     */
    fun getAnalysisStats(): AnalysisStats {
        val measurements = _measurements.value
        val annotations = _annotations.value
        
        val distanceMeasurements = measurements.filter { it.type == MeasurementType.DISTANCE }
        val angleMeasurements = measurements.filter { it.type == MeasurementType.ANGLE }
        val areaMeasurements = measurements.filter { it.type == MeasurementType.AREA }
        
        return AnalysisStats(
            totalMeasurements = measurements.size,
            distanceMeasurements = distanceMeasurements.size,
            angleMeasurements = angleMeasurements.size,
            areaMeasurements = areaMeasurements.size,
            totalAnnotations = annotations.size,
            averageDistance = distanceMeasurements.map { it.realWorldValue }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f,
            averageAngle = angleMeasurements.map { it.realWorldValue }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f,
            totalArea = areaMeasurements.sumOf { it.realWorldValue.toDouble() }.toFloat()
        )
    }
    
    private fun getVideoFrameRate(): Double {
        // This would normally get the actual frame rate from the video
        // For now, assume 30 FPS
        return 30.0
    }
    
    private fun generateMeasurementId(): String {
        return "measurement_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    private fun generateAnnotationId(): String {
        return "annotation_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    /**
     * Release resources
     */
    fun release() {
        scope.cancel()
        currentPlayer = null
        currentMediaItem = null
        frameExtractor.release()
    }
    
    // Data classes and enums
    sealed class ToolState {
        object Idle : ToolState()
        object Initializing : ToolState()
        object Ready : ToolState()
        data class Error(val message: String) : ToolState()
    }
    
    enum class FrameDirection {
        NEXT, PREVIOUS
    }
    
    enum class MeasurementType {
        DISTANCE, ANGLE, AREA
    }
    
    enum class AnnotationType {
        TEXT, ARROW, CIRCLE, RECTANGLE, DRAWING
    }
    
    enum class ExportFormat {
        PDF, CSV, JSON
    }
    
    data class VideoFrame(
        val bitmap: Bitmap,
        val timestamp: Long,
        val frameNumber: Long
    )
    
    data class Measurement(
        val id: String,
        val type: MeasurementType,
        val points: List<PointF>,
        val pixelValue: Float,
        val realWorldValue: Float,
        val unit: String,
        val timestamp: Long,
        val framePosition: Long
    )
    
    data class Annotation(
        val id: String,
        val type: AnnotationType,
        val position: PointF,
        val text: String,
        val drawingPath: Path? = null,
        val drawingPaint: Paint? = null,
        val timestamp: Long,
        val framePosition: Long
    )
    
    data class AnalysisData(
        val mediaItem: MediaItem,
        val measurements: List<Measurement>,
        val annotations: List<Annotation>,
        val exportTimestamp: Long,
        val playbackSpeed: Float
    )
    
    data class ExportResult(
        val success: Boolean,
        val filePath: String?,
        val error: String?
    )
    
    data class AnalysisStats(
        val totalMeasurements: Int,
        val distanceMeasurements: Int,
        val angleMeasurements: Int,
        val areaMeasurements: Int,
        val totalAnnotations: Int,
        val averageDistance: Float,
        val averageAngle: Float,
        val totalArea: Float
    )
}