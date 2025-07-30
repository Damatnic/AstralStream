package com.astralplayer.nextplayer.feature.ai

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

/**
 * Enhanced AI Scene Detection Manager using both ML Kit and Google AI Studio
 */
class EnhancedAISceneDetectionManager(
    private val context: Context,
    private val googleAIService: GoogleAIStudioService
) {
    
    companion object {
        private const val TAG = "EnhancedAISceneDetection"
        private const val FRAME_INTERVAL_MS = 2000L // Analyze every 2 seconds for better AI analysis
        private const val MIN_CONFIDENCE = 0.6f
        private const val SCENE_CHANGE_THRESHOLD = 0.4f
        private const val MAX_SCENES_PER_ANALYSIS = 50
    }
    
    private val _detectedScenes = MutableStateFlow<List<DetectedScene>>(emptyList())
    val detectedScenes: StateFlow<List<DetectedScene>> = _detectedScenes.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private val _analysisProgress = MutableStateFlow(0f)
    val analysisProgress: StateFlow<Float> = _analysisProgress.asStateFlow()
    
    private val _aiInsights = MutableStateFlow<String?>(null)
    val aiInsights: StateFlow<String?> = _aiInsights.asStateFlow()
    
    // ML Kit components
    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(MIN_CONFIDENCE)
            .build()
    )
    
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Enhanced video analysis combining ML Kit and Google AI Studio
     */
    suspend fun analyzeVideoEnhanced(videoUri: Uri, duration: Long, videoTitle: String = "Unknown Video"): List<DetectedScene> = withContext(Dispatchers.IO) {
        _isAnalyzing.value = true
        _analysisProgress.value = 0f
        
        val scenes = mutableListOf<DetectedScene>()
        val retriever = MediaMetadataRetriever()
        
        try {
            Log.d(TAG, "Starting enhanced video analysis for: $videoTitle")
            
            retriever.setDataSource(context, videoUri)
            val frameCount = minOf((duration / FRAME_INTERVAL_MS).toInt(), MAX_SCENES_PER_ANALYSIS)
            
            Log.d(TAG, "Analyzing $frameCount frames over ${duration}ms")
            
            var previousAIAnalysis: FrameAnalysisResult? = null
            var sceneStartTime = 0L
            var currentSceneId = 0
            
            // Phase 1: ML Kit + AI Studio frame analysis (70% of progress)
            for (i in 0 until frameCount) {
                val timeUs = i * FRAME_INTERVAL_MS * 1000
                val frame = retriever.getFrameAtTime(timeUs) ?: continue
                
                // Combine ML Kit and AI Studio analysis
                val combinedAnalysis = analyzeCombinedFrame(frame, timeUs / 1000)
                
                // Detect scene changes using AI-enhanced detection
                if (previousAIAnalysis != null) {
                    val sceneChangeScore = calculateAISceneChangeScore(previousAIAnalysis, combinedAnalysis)
                    
                    if (sceneChangeScore > SCENE_CHANGE_THRESHOLD) {
                        // Scene change detected
                        val scene = DetectedScene(
                            id = "ai_scene_${currentSceneId++}",
                            startTime = sceneStartTime,
                            endTime = timeUs / 1000,
                            confidence = combinedAnalysis.confidence,
                            sceneType = combinedAnalysis.sceneType,
                            description = combinedAnalysis.description,
                            thumbnail = frame,
                            keyframes = listOf(sceneStartTime, (sceneStartTime + timeUs / 1000) / 2)
                        )
                        scenes.add(scene)
                        sceneStartTime = timeUs / 1000
                        
                        Log.d(TAG, "AI detected scene change at ${timeUs / 1000}ms: ${combinedAnalysis.sceneType}")
                    }
                }
                
                previousAIAnalysis = combinedAnalysis
                
                val progress = (i + 1).toFloat() / frameCount * 0.7f
                _analysisProgress.value = progress
            }
            
            // Add final scene
            if (previousAIAnalysis != null && sceneStartTime < duration) {
                val finalScene = DetectedScene(
                    id = "ai_scene_${currentSceneId}",
                    startTime = sceneStartTime,
                    endTime = duration,
                    confidence = previousAIAnalysis.confidence,
                    sceneType = previousAIAnalysis.sceneType,
                    description = previousAIAnalysis.description,
                    keyframes = listOf(sceneStartTime, (sceneStartTime + duration) / 2)
                )
                scenes.add(finalScene)
            }
            
            _analysisProgress.value = 0.8f
            
            // Phase 2: Generate AI insights (20% of progress)
            if (scenes.isNotEmpty()) {
                Log.d(TAG, "Generating AI insights for ${scenes.size} detected scenes")
                
                launch {
                    googleAIService.generateVideoInsights(videoTitle, scenes).collect { result ->
                        when (result) {
                            is VideoInsightsResult.Success -> {
                                _aiInsights.value = result.insights
                                Log.d(TAG, "AI insights generated successfully")
                            }
                            is VideoInsightsResult.Error -> {
                                Log.e(TAG, "Failed to generate AI insights: ${result.message}")
                            }
                            is VideoInsightsResult.Progress -> {
                                Log.d(TAG, "AI insights progress: ${result.message}")
                            }
                        }
                    }
                }
            }
            
            _analysisProgress.value = 1.0f
            
            Log.d(TAG, "Enhanced analysis completed: ${scenes.size} scenes detected")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced video analysis", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
            }
            _isAnalyzing.value = false
        }
        
        _detectedScenes.value = scenes
        scenes
    }
    
    /**
     * Combine ML Kit and Google AI Studio analysis for a single frame
     */
    private suspend fun analyzeCombinedFrame(bitmap: Bitmap, timestamp: Long): FrameAnalysisResult {
        return try {
            // ML Kit analysis (fast, local)
            val mlKitAnalysis = analyzeFrameWithMLKit(bitmap)
            
            // Google AI Studio analysis (slower, more accurate)
            val aiAnalysis = googleAIService.analyzeVideoFrame(bitmap, timestamp)
            
            // Combine results with AI analysis taking precedence
            FrameAnalysisResult(
                timestamp = timestamp,
                sceneType = aiAnalysis.sceneType,
                objects = (mlKitAnalysis.objects + aiAnalysis.objects).distinct(),
                mood = aiAnalysis.mood,
                confidence = maxOf(mlKitAnalysis.confidence, aiAnalysis.confidence),
                description = if (aiAnalysis.description.isNotBlank()) aiAnalysis.description else mlKitAnalysis.description
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in combined frame analysis", e)
            // Fallback to ML Kit only
            analyzeFrameWithMLKit(bitmap)
        }
    }
    
    /**
     * ML Kit-only frame analysis for fallback
     */
    private suspend fun analyzeFrameWithMLKit(bitmap: Bitmap): FrameAnalysisResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            
            // Get image labels
            val labels = imageLabeler.process(image).await()
            val labelTexts = labels.map { it.text }
            val avgConfidence = if (labels.isNotEmpty()) labels.map { it.confidence }.average().toFloat() else 0.5f
            
            // Get detected objects
            val objects = objectDetector.process(image).await()
            val objectLabels = objects.mapNotNull { it.labels.firstOrNull()?.text }
            
            // Determine scene type from ML Kit results
            val allLabels = labelTexts + objectLabels
            val sceneType = determineSceneTypeFromLabels(allLabels)
            
            FrameAnalysisResult(
                timestamp = 0L, // Will be set by calling function
                sceneType = sceneType,
                objects = allLabels,
                mood = "neutral",
                confidence = avgConfidence,
                description = "Scene with ${allLabels.take(3).joinToString(", ")}"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit analysis failed", e)
            FrameAnalysisResult(
                timestamp = 0L,
                sceneType = SceneType.TRANSITION,
                objects = emptyList(),
                mood = "unknown",
                confidence = 0.3f,
                description = "Analysis unavailable"
            )
        }
    }
    
    /**
     * Calculate scene change score using AI-enhanced analysis
     */
    private fun calculateAISceneChangeScore(
        previous: FrameAnalysisResult,
        current: FrameAnalysisResult
    ): Float {
        // Scene type change (high weight)
        val sceneTypeChange = if (previous.sceneType != current.sceneType) 0.8f else 0.0f
        
        // Mood change (medium weight)
        val moodChange = if (previous.mood != current.mood) 0.4f else 0.0f
        
        // Object similarity (low weight)
        val objectSimilarity = calculateObjectSimilarity(previous.objects, current.objects)
        val objectChange = (1.0f - objectSimilarity) * 0.3f
        
        // Confidence difference (very low weight)
        val confidenceChange = abs(previous.confidence - current.confidence) * 0.1f
        
        return sceneTypeChange + moodChange + objectChange + confidenceChange
    }
    
    private fun calculateObjectSimilarity(objects1: List<String>, objects2: List<String>): Float {
        if (objects1.isEmpty() && objects2.isEmpty()) return 1.0f
        if (objects1.isEmpty() || objects2.isEmpty()) return 0.0f
        
        val intersection = objects1.intersect(objects2.toSet()).size
        val union = (objects1 + objects2).toSet().size
        
        return intersection.toFloat() / union
    }
    
    private fun determineSceneTypeFromLabels(labels: List<String>): SceneType {
        val lowerLabels = labels.map { it.lowercase() }
        
        return when {
            lowerLabels.any { it.contains("person") || it.contains("face") } -> {
                if (lowerLabels.any { it.contains("crowd") || it.contains("people") }) SceneType.CROWD
                else SceneType.CLOSE_UP
            }
            lowerLabels.any { it.contains("landscape") || it.contains("mountain") || it.contains("ocean") || it.contains("nature") } -> SceneType.LANDSCAPE
            lowerLabels.any { it.contains("action") || it.contains("sport") || it.contains("motion") || it.contains("vehicle") } -> SceneType.ACTION
            lowerLabels.any { it.contains("indoor") || it.contains("room") || it.contains("furniture") || it.contains("building") } -> SceneType.INDOOR
            lowerLabels.any { it.contains("outdoor") || it.contains("sky") || it.contains("tree") || it.contains("street") } -> SceneType.OUTDOOR
            lowerLabels.any { it.contains("night") || it.contains("dark") } -> SceneType.NIGHT
            lowerLabels.any { it.contains("day") || it.contains("sun") || it.contains("bright") } -> SceneType.DAY
            lowerLabels.any { it.contains("text") || it.contains("dialogue") || it.contains("conversation") } -> SceneType.DIALOGUE
            else -> SceneType.TRANSITION
        }
    }
    
    /**
     * Get AI-generated video recommendations
     */
    suspend fun getVideoRecommendations(
        currentVideoTitle: String,
        watchHistory: List<String>
    ): List<VideoRecommendation> {
        return try {
            googleAIService.generateRecommendations(currentVideoTitle, watchHistory)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video recommendations", e)
            emptyList()
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        try {
            imageLabeler.close()
            objectDetector.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up ML Kit resources", e)
        }
    }
}