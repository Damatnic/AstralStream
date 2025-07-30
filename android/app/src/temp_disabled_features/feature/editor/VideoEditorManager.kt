package com.astralplayer.nextplayer.feature.editor

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class representing a video editing project
 */
data class EditingProject(
    val id: String,
    val name: String,
    val sourceVideoUri: String,
    val duration: Long,
    val clips: List<VideoClip> = emptyList(),
    val effects: List<VideoEffect> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Data class representing a video clip
 */
data class VideoClip(
    val id: String,
    val sourceUri: String,
    val startTime: Long,
    val endTime: Long,
    val trimStart: Long = 0L,
    val trimEnd: Long = endTime - startTime,
    val volume: Float = 1.0f,
    val speed: Float = 1.0f
)

/**
 * Data class representing video effects
 */
data class VideoEffect(
    val id: String,
    val type: EffectType,
    val startTime: Long,
    val duration: Long,
    val intensity: Float = 1.0f,
    val parameters: Map<String, Any> = emptyMap()
)

/**
 * Enum for effect types
 */
enum class EffectType {
    FADE_IN,
    FADE_OUT,
    BLUR,
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    SEPIA,
    GRAYSCALE,
    VIGNETTE
}

/**
 * Manager for video editing functionality
 */
class VideoEditorManager(private val context: Context) {
    
    private val videoProcessor = VideoProcessor(context)
    private val ffmpegProcessor = FFmpegVideoProcessor(context)
    
    private val _currentProject = MutableStateFlow<EditingProject?>(null)
    val currentProject: StateFlow<EditingProject?> = _currentProject.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()
    
    private val _editorError = MutableStateFlow<String?>(null)
    val editorError: StateFlow<String?> = _editorError.asStateFlow()
    
    /**
     * Create new editing project
     */
    fun createProject(name: String, videoUri: Uri, duration: Long): EditingProject {
        val project = EditingProject(
            id = "project_${System.currentTimeMillis()}",
            name = name,
            sourceVideoUri = videoUri.toString(),
            duration = duration,
            clips = listOf(
                VideoClip(
                    id = "clip_1",
                    sourceUri = videoUri.toString(),
                    startTime = 0L,
                    endTime = duration
                )
            )
        )
        
        _currentProject.value = project
        return project
    }
    
    /**
     * Trim video clip
     */
    fun trimClip(clipId: String, newStart: Long, newEnd: Long) {
        val project = _currentProject.value ?: return
        val updatedClips = project.clips.map { clip ->
            if (clip.id == clipId) {
                clip.copy(
                    trimStart = newStart,
                    trimEnd = newEnd
                )
            } else {
                clip
            }
        }
        
        _currentProject.value = project.copy(
            clips = updatedClips,
            lastModified = System.currentTimeMillis()
        )
    }
    
    /**
     * Split clip at specified time
     */
    fun splitClip(clipId: String, splitTime: Long) {
        val project = _currentProject.value ?: return
        val clipToSplit = project.clips.find { it.id == clipId } ?: return
        
        val firstClip = clipToSplit.copy(
            trimEnd = splitTime - clipToSplit.startTime
        )
        
        val secondClip = clipToSplit.copy(
            id = "clip_${System.currentTimeMillis()}",
            trimStart = splitTime - clipToSplit.startTime
        )
        
        val updatedClips = project.clips.map { clip ->
            if (clip.id == clipId) firstClip else clip
        } + secondClip
        
        _currentProject.value = project.copy(
            clips = updatedClips,
            lastModified = System.currentTimeMillis()
        )
    }
    
    /**
     * Add effect to project
     */
    fun addEffect(effect: VideoEffect) {
        val project = _currentProject.value ?: return
        val updatedEffects = project.effects + effect
        
        _currentProject.value = project.copy(
            effects = updatedEffects,
            lastModified = System.currentTimeMillis()
        )
    }
    
    /**
     * Remove effect from project
     */
    fun removeEffect(effectId: String) {
        val project = _currentProject.value ?: return
        val updatedEffects = project.effects.filter { it.id != effectId }
        
        _currentProject.value = project.copy(
            effects = updatedEffects,
            lastModified = System.currentTimeMillis()
        )
    }
    
    /**
     * Export edited video
     */
    suspend fun exportVideo(outputPath: String): Boolean {
        return try {
            _isProcessing.value = true
            _processingProgress.value = 0f
            
            val project = _currentProject.value ?: throw IllegalStateException("No project loaded")
            
            // If single clip with no effects, just trim
            if (project.clips.size == 1 && project.effects.isEmpty()) {
                val clip = project.clips.first()
                val result = videoProcessor.trimVideo(
                    inputUri = Uri.parse(clip.sourceUri),
                    outputPath = outputPath,
                    startTimeUs = clip.trimStart * 1000,
                    endTimeUs = clip.trimEnd * 1000,
                    onProgress = { progress ->
                        _processingProgress.value = progress
                    }
                )
                
                _isProcessing.value = false
                result.isSuccess
                
            } else if (project.clips.size > 1) {
                // Multiple clips - merge them
                val clipUris = project.clips.map { Uri.parse(it.sourceUri) }
                val result = videoProcessor.mergeVideos(
                    inputUris = clipUris,
                    outputPath = outputPath,
                    onProgress = { progress ->
                        _processingProgress.value = progress
                    }
                )
                
                _isProcessing.value = false
                result.isSuccess
                
            } else {
                // Apply effects using FFmpeg
                val clip = project.clips.first()
                var currentUri = Uri.parse(clip.sourceUri)
                var tempFile: String? = null
                
                // First trim the video if needed
                if (clip.trimStart > 0 || clip.trimEnd < (clip.endTime - clip.startTime)) {
                    tempFile = "${context.cacheDir}/temp_trimmed_${System.currentTimeMillis()}.mp4"
                    val trimResult = videoProcessor.trimVideo(
                        inputUri = currentUri,
                        outputPath = tempFile,
                        startTimeUs = clip.trimStart * 1000,
                        endTimeUs = clip.trimEnd * 1000,
                        onProgress = { progress ->
                            _processingProgress.value = progress * 0.5f // 50% for trimming
                        }
                    )
                    
                    if (!trimResult.isSuccess) {
                        _isProcessing.value = false
                        return@exportVideo false
                    }
                    currentUri = Uri.parse(tempFile)
                }
                
                // Apply effects one by one
                var effectsApplied = true
                for ((index, effect) in project.effects.withIndex()) {
                    val outputFile = if (index == project.effects.size - 1) {
                        outputPath
                    } else {
                        "${context.cacheDir}/temp_effect_${effect.id}.mp4"
                    }
                    
                    val effectResult = ffmpegProcessor.applyVideoEffect(
                        inputUri = currentUri,
                        outputPath = outputFile,
                        effect = effect,
                        onProgress = { progress ->
                            val baseProgress = 0.5f + (0.5f * index / project.effects.size)
                            val effectProgress = 0.5f / project.effects.size
                            _processingProgress.value = baseProgress + (progress * effectProgress)
                        }
                    )
                    
                    if (!effectResult.isSuccess) {
                        effectsApplied = false
                        break
                    }
                    
                    // Clean up temp file if not the original
                    if (currentUri.path != clip.sourceUri && currentUri.path != outputFile) {
                        java.io.File(currentUri.path ?: "").delete()
                    }
                    
                    currentUri = Uri.parse(outputFile)
                }
                
                // If no effects or trimming, just copy the file
                if (project.effects.isEmpty() && tempFile == null) {
                    val inputFile = java.io.File(clip.sourceUri)
                    val outputFile = java.io.File(outputPath)
                    inputFile.copyTo(outputFile, overwrite = true)
                }
                
                // Clean up remaining temp files
                tempFile?.let { java.io.File(it).delete() }
                
                _isProcessing.value = false
                effectsApplied
            }
            
        } catch (e: Exception) {
            _editorError.value = "Export failed: ${e.message}"
            _isProcessing.value = false
            false
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _editorError.value = null
    }
    
    /**
     * Apply a single effect to a video
     */
    suspend fun applyEffect(inputUri: Uri, outputPath: String, effect: VideoEffect): Boolean {
        return try {
            _isProcessing.value = true
            _processingProgress.value = 0f
            
            val result = ffmpegProcessor.applyVideoEffect(
                inputUri = inputUri,
                outputPath = outputPath,
                effect = effect,
                onProgress = { progress ->
                    _processingProgress.value = progress
                }
            )
            
            _isProcessing.value = false
            result.isSuccess
        } catch (e: Exception) {
            _editorError.value = "Failed to apply effect: ${e.message}"
            _isProcessing.value = false
            false
        }
    }
    
    /**
     * Add text overlay to video
     */
    suspend fun addTextOverlay(
        inputUri: Uri,
        outputPath: String,
        text: String,
        position: TextPosition = TextPosition.BOTTOM_CENTER,
        fontSize: Int = 24,
        fontColor: String = "white"
    ): Boolean {
        return try {
            _isProcessing.value = true
            _processingProgress.value = 0f
            
            val result = ffmpegProcessor.addTextOverlay(
                inputUri = inputUri,
                outputPath = outputPath,
                text = text,
                position = position,
                fontSize = fontSize,
                fontColor = fontColor,
                onProgress = { progress ->
                    _processingProgress.value = progress
                }
            )
            
            _isProcessing.value = false
            result.isSuccess
        } catch (e: Exception) {
            _editorError.value = "Failed to add text overlay: ${e.message}"
            _isProcessing.value = false
            false
        }
    }
    
    /**
     * Extract audio from video
     */
    suspend fun extractAudio(inputUri: Uri, outputPath: String): Boolean {
        return try {
            _isProcessing.value = true
            _processingProgress.value = 0f
            
            val result = ffmpegProcessor.extractAudio(
                inputUri = inputUri,
                outputPath = outputPath,
                onProgress = { progress ->
                    _processingProgress.value = progress
                }
            )
            
            _isProcessing.value = false
            result.isSuccess
        } catch (e: Exception) {
            _editorError.value = "Failed to extract audio: ${e.message}"
            _isProcessing.value = false
            false
        }
    }
}

/**
 * ViewModel for video editor
 */
class VideoEditorViewModel(private val editorManager: VideoEditorManager) : ViewModel() {
    
    val currentProject = editorManager.currentProject
    val isProcessing = editorManager.isProcessing
    val processingProgress = editorManager.processingProgress
    val editorError = editorManager.editorError
    
    fun createProject(name: String, videoUri: Uri, duration: Long) {
        editorManager.createProject(name, videoUri, duration)
    }
    
    fun trimClip(clipId: String, newStart: Long, newEnd: Long) {
        editorManager.trimClip(clipId, newStart, newEnd)
    }
    
    fun splitClip(clipId: String, splitTime: Long) {
        editorManager.splitClip(clipId, splitTime)
    }
    
    fun addEffect(effect: VideoEffect) {
        editorManager.addEffect(effect)
    }
    
    fun removeEffect(effectId: String) {
        editorManager.removeEffect(effectId)
    }
    
    fun exportVideo(outputPath: String) {
        viewModelScope.launch {
            editorManager.exportVideo(outputPath)
        }
    }
    
    fun clearError() {
        editorManager.clearError()
    }
}

/**
 * Composable for video editor screen
 */
@Composable
fun VideoEditorScreen(
    project: EditingProject?,
    isProcessing: Boolean,
    processingProgress: Float,
    editorError: String?,
    onTrimClip: (String, Long, Long) -> Unit,
    onSplitClip: (String, Long) -> Unit,
    onAddEffect: (VideoEffect) -> Unit,
    onRemoveEffect: (String) -> Unit,
    onExportVideo: (String) -> Unit,
    onClearError: () -> Unit,
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
                text = project?.name ?: "Video Editor",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (project != null) {
                Button(
                    onClick = { onExportVideo("output.mp4") },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00BCD4)
                    )
                ) {
                    Text("Export")
                }
            }
        }
        
        // Processing indicator
        if (isProcessing) {
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
                        text = "Processing video...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = processingProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF00BCD4)
                    )
                    
                    Text(
                        text = "${(processingProgress * 100).toInt()}%",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
        
        // Error display
        editorError?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Red.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color.Red
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = error,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = onClearError) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        
        // Editor content
        if (project != null) {
            VideoEditorContent(
                project = project,
                onTrimClip = onTrimClip,
                onSplitClip = onSplitClip,
                onAddEffect = onAddEffect,
                onRemoveEffect = onRemoveEffect
            )
        } else {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.VideoFile,
                        contentDescription = "No project",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "No project loaded",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Video editor content
 */
@Composable
private fun VideoEditorContent(
    project: EditingProject,
    onTrimClip: (String, Long, Long) -> Unit,
    onSplitClip: (String, Long) -> Unit,
    onAddEffect: (VideoEffect) -> Unit,
    onRemoveEffect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Timeline
        TimelineView(
            clips = project.clips,
            effects = project.effects,
            duration = project.duration,
            onTrimClip = onTrimClip,
            onSplitClip = onSplitClip
        )
        
        // Effects panel
        EffectsPanel(
            effects = project.effects,
            onAddEffect = onAddEffect,
            onRemoveEffect = onRemoveEffect
        )
    }
}

/**
 * Timeline view for video editing
 */
@Composable
private fun TimelineView(
    clips: List<VideoClip>,
    effects: List<VideoEffect>,
    duration: Long,
    onTrimClip: (String, Long, Long) -> Unit,
    onSplitClip: (String, Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Timeline",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Clips track
            clips.forEach { clip ->
                ClipItem(
                    clip = clip,
                    totalDuration = duration,
                    onTrim = { start, end -> onTrimClip(clip.id, start, end) },
                    onSplit = { time -> onSplitClip(clip.id, time) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Individual clip item in timeline
 */
@Composable
private fun ClipItem(
    clip: VideoClip,
    totalDuration: Long,
    onTrim: (Long, Long) -> Unit,
    onSplit: (Long) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF00BCD4).copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Clip ${clip.id.takeLast(1)}",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = { onSplit(clip.startTime + (clip.endTime - clip.startTime) / 2) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.ContentCut,
                    contentDescription = "Split",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Effects panel
 */
@Composable
private fun EffectsPanel(
    effects: List<VideoEffect>,
    onAddEffect: (VideoEffect) -> Unit,
    onRemoveEffect: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Effects",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = {
                        val effect = VideoEffect(
                            id = "effect_${System.currentTimeMillis()}",
                            type = EffectType.FADE_IN,
                            startTime = 0L,
                            duration = 1000L
                        )
                        onAddEffect(effect)
                    }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Effect",
                        tint = Color(0xFF00BCD4)
                    )
                }
            }
            
            // Effects list
            effects.forEach { effect ->
                EffectItem(
                    effect = effect,
                    onRemove = { onRemoveEffect(effect.id) }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            if (effects.isEmpty()) {
                Text(
                    text = "No effects applied",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Individual effect item
 */
@Composable
private fun EffectItem(
    effect: VideoEffect,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = effect.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove",
                tint = Color.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}