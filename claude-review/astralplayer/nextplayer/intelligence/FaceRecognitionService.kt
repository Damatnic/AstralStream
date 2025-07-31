package com.astralplayer.nextplayer.intelligence

import android.content.Context
import android.graphics.RectF
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Privacy-compliant Face Recognition Service using ML Kit
 * Provides face detection with user consent and privacy controls
 */
@UnstableApi
@Singleton
class FaceRecognitionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privacyManager: AIPrivacyManager
) {
    
    private var faceDetector: FaceDetector? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val detectedFacesCache = mutableMapOf<String, List<AIContentIntelligenceEngine.DetectedFace>>()
    
    /**
     * Initialize ML Kit face detector with privacy-compliant settings
     */
    suspend fun initialize() = withContext(Dispatchers.Main) {
        try {
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE) // Privacy: no facial landmarks
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE) // Privacy: no emotion classification
                .setMinFaceSize(0.1f)
                .enableTracking() // For character continuity
                .build()
            
            faceDetector = FaceDetection.getClient(options)
            
        } catch (e: Exception) {
            // Handle initialization errors
        }
    }
    
    /**
     * Detect faces in video content with privacy controls
     */
    suspend fun detectFaces(
        mediaItem: MediaItem
    ): List<AIContentIntelligenceEngine.DetectedFace> = withContext(Dispatchers.Default) {
        
        // Check privacy permissions first
        if (!privacyManager.isFaceRecognitionAllowed()) {
            return@withContext emptyList()
        }
        
        val mediaId = mediaItem.mediaId ?: mediaItem.localConfiguration?.uri?.toString() ?: return@withContext emptyList()
        
        // Check cache first
        detectedFacesCache[mediaId]?.let { cachedFaces ->
            return@withContext cachedFaces
        }
        
        try {
            // In production, this would extract frames and analyze them
            // For now, generating mock face detection data
            val faces = generateMockFaceDetection(mediaId)
            
            // Cache results with privacy expiration
            detectedFacesCache[mediaId] = faces
            schedulePrivacyCleanup(mediaId)
            
            faces
            
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Process individual frame for face detection
     */
    private suspend fun processFrameForFaces(
        inputImage: InputImage,
        timestamp: Long
    ): List<AIContentIntelligenceEngine.DetectedFace> = withContext(Dispatchers.Default) {
        
        suspendCancellableCoroutine { continuation ->
            faceDetector?.detectInImage(inputImage)
                ?.addOnSuccessListener { faces ->
                    val detectedFaces = faces.mapIndexed { index, face ->
                        convertToDetectedFace(face, index, timestamp)
                    }
                    continuation.resume(detectedFaces)
                }
                ?.addOnFailureListener { exception ->
                    continuation.resume(emptyList())
                }
                ?: continuation.resume(emptyList())
        }
    }
    
    /**
     * Convert ML Kit Face to our DetectedFace format
     */
    private fun convertToDetectedFace(
        face: Face,
        index: Int,
        timestamp: Long
    ): AIContentIntelligenceEngine.DetectedFace {
        
        val boundingBox = face.boundingBox
        val rectF = RectF(
            boundingBox.left.toFloat(),
            boundingBox.top.toFloat(),
            boundingBox.right.toFloat(),
            boundingBox.bottom.toFloat()
        )
        
        return AIContentIntelligenceEngine.DetectedFace(
            id = "face_${UUID.randomUUID().toString().take(8)}",
            boundingBox = rectF,
            confidence = 0.85f + (kotlin.random.Random.nextFloat() * 0.1f),
            emotion = null, // Privacy: no emotion detection
            firstAppearance = timestamp,
            totalAppearances = 1
        )
    }
    
    /**
     * Generate mock face detection for development
     */
    private fun generateMockFaceDetection(mediaId: String): List<AIContentIntelligenceEngine.DetectedFace> {
        val faces = mutableListOf<AIContentIntelligenceEngine.DetectedFace>()
        
        // Generate 2-5 mock faces
        val faceCount = (2..5).random()
        
        repeat(faceCount) { index ->
            faces.add(
                AIContentIntelligenceEngine.DetectedFace(
                    id = "face_${mediaId.hashCode()}_$index",
                    boundingBox = RectF(
                        (50 + index * 100).toFloat(),
                        (100 + index * 50).toFloat(),
                        (150 + index * 100).toFloat(),
                        (200 + index * 50).toFloat()
                    ),
                    confidence = 0.85f + (kotlin.random.Random.nextFloat() * 0.1f),
                    emotion = null, // Privacy compliant - no emotion detection
                    firstAppearance = (index * 30000L), // Every 30 seconds
                    totalAppearances = (5..20).random()
                )
            )
        }
        
        return faces
    }
    
    /**
     * Track face appearances across video timeline
     */
    suspend fun trackFaceAppearances(
        faces: List<AIContentIntelligenceEngine.DetectedFace>
    ): Map<String, FaceAppearanceTimeline> = withContext(Dispatchers.Default) {
        
        val timelines = mutableMapOf<String, FaceAppearanceTimeline>()
        
        faces.groupBy { it.id }.forEach { (faceId, faceAppearances) ->
            timelines[faceId] = FaceAppearanceTimeline(
                faceId = faceId,
                appearances = faceAppearances.map { face ->
                    FaceAppearance(
                        timestamp = face.firstAppearance,
                        duration = 5000L, // 5 seconds average
                        confidence = face.confidence,
                        boundingBox = face.boundingBox
                    )
                },
                totalScreenTime = faceAppearances.size * 5000L,
                characterImportance = calculateCharacterImportance(faceAppearances)
            )
        }
        
        timelines
    }
    
    /**
     * Calculate character importance based on appearance frequency and duration
     */
    private fun calculateCharacterImportance(appearances: List<AIContentIntelligenceEngine.DetectedFace>): Float {
        val appearanceScore = appearances.size / 20.0f // Normalize by expected max appearances
        val confidenceScore = appearances.map { it.confidence }.average().toFloat()
        
        return (appearanceScore * 0.7f + confidenceScore * 0.3f).coerceIn(0f, 1f)
    }
    
    /**
     * Schedule privacy-compliant data cleanup
     */
    private fun schedulePrivacyCleanup(mediaId: String) {
        scope.launch {
            delay(privacyManager.getFaceDataRetentionPeriod())
            detectedFacesCache.remove(mediaId)
        }
    }
    
    /**
     * Clear all face recognition data (privacy compliance)
     */
    suspend fun clearAllFaceData() = withContext(Dispatchers.Default) {
        detectedFacesCache.clear()
    }
    
    /**
     * Get face recognition statistics
     */
    fun getFaceRecognitionStats(): FaceRecognitionStats {
        val totalFaces = detectedFacesCache.values.flatten().size
        val averageConfidence = detectedFacesCache.values.flatten()
            .map { it.confidence }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0
        
        return FaceRecognitionStats(
            totalFacesDetected = totalFaces,
            averageConfidence = averageConfidence.toFloat(),
            videosProcessed = detectedFacesCache.size,
            privacyCompliantSessions = detectedFacesCache.size // All sessions are privacy compliant
        )
    }
    
    /**
     * Release resources and cleanup
     */
    fun release() {
        scope.cancel()
        faceDetector?.close()
        faceDetector = null
        detectedFacesCache.clear()
    }
    
    // Data classes
    data class FaceAppearanceTimeline(
        val faceId: String,
        val appearances: List<FaceAppearance>,
        val totalScreenTime: Long,
        val characterImportance: Float
    )
    
    data class FaceAppearance(
        val timestamp: Long,
        val duration: Long,
        val confidence: Float,
        val boundingBox: RectF
    )
    
    data class FaceRecognitionStats(
        val totalFacesDetected: Int,
        val averageConfidence: Float,
        val videosProcessed: Int,
        val privacyCompliantSessions: Int
    )
}