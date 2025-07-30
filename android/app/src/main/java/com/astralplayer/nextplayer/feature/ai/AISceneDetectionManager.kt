package com.astralplayer.nextplayer.feature.ai

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.io.IOException
import kotlin.math.abs

/**
 * Data class representing a detected scene
 */
data class DetectedScene(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val confidence: Float,
    val sceneType: SceneType,
    val description: String,
    val thumbnail: Bitmap? = null,
    val keyframes: List<Long> = emptyList()
)

/**
 * Enum for scene types
 */
enum class SceneType {
    ACTION,
    DIALOGUE,
    LANDSCAPE,
    CLOSE_UP,
    CROWD,
    INDOOR,
    OUTDOOR,
    NIGHT,
    DAY,
    TRANSITION
}

/**
 * AI Scene Detection Manager using ML Kit
 */
class AISceneDetectionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AISceneDetection"
        private const val FRAME_INTERVAL_MS = 1000L // Analyze every 1 second
        private const val MIN_CONFIDENCE = 0.7f
        private const val SCENE_CHANGE_THRESHOLD = 0.5f
    }
    
    private val _detectedScenes = MutableStateFlow<List<DetectedScene>>(emptyList())
    val detectedScenes: StateFlow<List<DetectedScene>> = _detectedScenes.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private val _analysisProgress = MutableStateFlow(0f)
    val analysisProgress: StateFlow<Float> = _analysisProgress.asStateFlow()
    
    // ML Kit image labeler
    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(MIN_CONFIDENCE)
            .build()
    )
    
    // ML Kit object detector
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Analyze video for scene detection using ML Kit
     */
    suspend fun analyzeVideo(videoUri: Uri, duration: Long): List<DetectedScene> = withContext(Dispatchers.IO) {
        _isAnalyzing.value = true
        _analysisProgress.value = 0f
        
        val scenes = mutableListOf<DetectedScene>()
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(context, videoUri)
            val frameCount = (duration / FRAME_INTERVAL_MS).toInt()
            var previousLabels = emptyList<String>()
            var sceneStartTime = 0L
            var currentSceneId = 0
            
            for (i in 0 until frameCount) {
                val timeUs = i * FRAME_INTERVAL_MS * 1000
                val frame = retriever.getFrameAtTime(timeUs) ?: continue
                
                // Analyze frame with ML Kit
                val frameAnalysis = analyzeFrame(frame, timeUs / 1000)
                
                // Detect scene changes
                if (frameAnalysis.labels.isNotEmpty()) {
                    val similarity = calculateSimilarity(previousLabels, frameAnalysis.labels)
                    
                    if (similarity < SCENE_CHANGE_THRESHOLD && previousLabels.isNotEmpty()) {
                        // Scene change detected
                        val scene = DetectedScene(
                            id = "scene_${currentSceneId++}",
                            startTime = sceneStartTime,
                            endTime = timeUs / 1000,
                            confidence = frameAnalysis.confidence,
                            sceneType = determineSceneType(previousLabels),
                            description = generateSceneDescription(previousLabels),
                            thumbnail = frame,
                            keyframes = listOf(sceneStartTime, (sceneStartTime + timeUs / 1000) / 2)
                        )
                        scenes.add(scene)
                        sceneStartTime = timeUs / 1000
                    }
                    
                    previousLabels = frameAnalysis.labels
                }
                
                _analysisProgress.value = (i + 1).toFloat() / frameCount
            }
            
            // Add final scene
            if (previousLabels.isNotEmpty()) {
                val scene = DetectedScene(
                    id = "scene_${currentSceneId}",
                    startTime = sceneStartTime,
                    endTime = duration,
                    confidence = 0.8f,
                    sceneType = determineSceneType(previousLabels),
                    description = generateSceneDescription(previousLabels),
                    keyframes = listOf(sceneStartTime, (sceneStartTime + duration) / 2)
                )
                scenes.add(scene)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing video", e)
        } finally {
            retriever.release()
            _isAnalyzing.value = false
        }
        
        _detectedScenes.value = scenes
        scenes
    }
    
    /**
     * Analyze a single frame using ML Kit
     */
    private suspend fun analyzeFrame(bitmap: Bitmap, timeMs: Long): FrameAnalysis {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            
            // Get image labels
            val labels = imageLabeler.process(image).await()
            val labelTexts = labels.map { it.text }
            val avgConfidence = labels.map { it.confidence }.average().toFloat()
            
            // Get detected objects
            val objects = objectDetector.process(image).await()
            val objectLabels = objects.mapNotNull { it.labels.firstOrNull()?.text }
            
            FrameAnalysis(
                timeMs = timeMs,
                labels = labelTexts + objectLabels,
                confidence = avgConfidence,
                objectCount = objects.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing frame at $timeMs", e)
            FrameAnalysis(timeMs, emptyList(), 0f, 0)
        }
    }
    
    /**
     * Calculate similarity between two label sets
     */
    private fun calculateSimilarity(labels1: List<String>, labels2: List<String>): Float {
        if (labels1.isEmpty() || labels2.isEmpty()) return 0f
        
        val intersection = labels1.intersect(labels2.toSet()).size
        val union = (labels1 + labels2).toSet().size
        
        return intersection.toFloat() / union
    }
    
    /**
     * Determine scene type based on detected labels
     */
    private fun determineSceneType(labels: List<String>): SceneType {
        val lowerLabels = labels.map { it.lowercase() }
        
        return when {
            lowerLabels.any { it.contains("person") || it.contains("face") } -> {
                if (lowerLabels.any { it.contains("crowd") }) SceneType.CROWD
                else SceneType.CLOSE_UP
            }
            lowerLabels.any { it.contains("landscape") || it.contains("mountain") || it.contains("ocean") } -> SceneType.LANDSCAPE
            lowerLabels.any { it.contains("action") || it.contains("sport") || it.contains("motion") } -> SceneType.ACTION
            lowerLabels.any { it.contains("indoor") || it.contains("room") || it.contains("furniture") } -> SceneType.INDOOR
            lowerLabels.any { it.contains("outdoor") || it.contains("sky") || it.contains("tree") } -> SceneType.OUTDOOR
            lowerLabels.any { it.contains("night") || it.contains("dark") } -> SceneType.NIGHT
            lowerLabels.any { it.contains("day") || it.contains("sun") || it.contains("bright") } -> SceneType.DAY
            else -> SceneType.TRANSITION
        }
    }
    
    /**
     * Generate scene description from labels
     */
    private fun generateSceneDescription(labels: List<String>): String {
        if (labels.isEmpty()) return "Unknown scene"
        
        val topLabels = labels.take(3)
        return "Scene with ${topLabels.joinToString(", ")}"
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        imageLabeler.close()
        objectDetector.close()
    }
    
    /**
     * Data class for frame analysis results
     */
    private data class FrameAnalysis(
        val timeMs: Long,
        val labels: List<String>,
        val confidence: Float,
        val objectCount: Int
    )
}

/**
 * ViewModel for AI Scene Detection
 */
class AISceneDetectionViewModel(private val aiManager: AISceneDetectionManager) : ViewModel() {
    
    val detectedScenes = aiManager.detectedScenes
    val isAnalyzing = aiManager.isAnalyzing
    val analysisProgress = aiManager.analysisProgress
    
    fun analyzeVideo(videoUri: Uri, duration: Long) {
        viewModelScope.launch {
            aiManager.analyzeVideo(videoUri, duration)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        aiManager.cleanup()
    }
}

/**
 * Composable for AI Scene Detection UI
 */
@Composable
fun AISceneDetectionScreen(
    scenes: List<DetectedScene>,
    isAnalyzing: Boolean,
    progress: Float,
    onAnalyzeVideo: () -> Unit,
    onSceneClick: (DetectedScene) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Scene Detection",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = onAnalyzeVideo,
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00BCD4)
                )
            ) {
                Text("Analyze")
            }
        }
        
        // Analysis progress
        if (isAnalyzing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF00BCD4).copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Analyzing video with AI...",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF00BCD4)
                    )
                }
            }
        }
        
        // Detected scenes
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(scenes) { scene ->
                SceneItem(
                    scene = scene,
                    onClick = { onSceneClick(scene) }
                )
            }
        }
    }
}

/**
 * Scene item composable
 */
@Composable
private fun SceneItem(
    scene: DetectedScene,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scene type icon
            Icon(
                imageVector = when (scene.sceneType) {
                    SceneType.ACTION -> Icons.Default.FlashOn
                    SceneType.DIALOGUE -> Icons.Default.RecordVoiceOver
                    SceneType.LANDSCAPE -> Icons.Default.Landscape
                    else -> Icons.Default.Movie
                },
                contentDescription = scene.sceneType.name,
                tint = Color(0xFF00BCD4),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scene.description,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${formatTime(scene.startTime)} - ${formatTime(scene.endTime)}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            
            // Confidence indicator
            Text(
                text = "${(scene.confidence * 100).toInt()}%",
                color = Color(0xFF4CAF50),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}