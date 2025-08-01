package com.astralplayer.features.editing.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.astralplayer.features.editing.service.VideoEditingService
import com.astralplayer.nextplayer.editing.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class EditorTab(val title: String, val icon: ImageVector) {
    TRIM("Trim", Icons.Default.ContentCut),
    EFFECTS("Effects", Icons.Default.AutoAwesome),
    TRANSITIONS("Transitions", Icons.Default.ViewCarousel),
    AUDIO("Audio", Icons.Default.VolumeUp),
    COLOR("Color", Icons.Default.ColorLens),
    TEXT("Text", Icons.Default.TextFields)
}

data class VideoEditorUiState(
    val isLoading: Boolean = true,
    val currentProject: EditingProject? = null,
    val timeline: Timeline = Timeline(),
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isPlaying: Boolean = false,
    val zoomLevel: Float = 1.0f,
    val currentTab: EditorTab = EditorTab.TRIM,
    val selectedClip: TimelineClip? = null,
    val availableEffects: List<EffectType> = emptyList(),
    val availableTransitions: List<TransitionType> = emptyList(),
    val audioAdjustments: AudioAdjustments = AudioAdjustments(),
    val colorCorrectionSettings: ColorCorrectionSettings = ColorCorrectionSettings(),
    val exportSettings: ExportSettings = ExportSettings(
        outputUri = Uri.EMPTY,
        outputFormat = VideoFormat.MP4,
        resolution = Pair(1920, 1080),
        frameRate = 30f,
        bitrate = 10_000_000L,
        audioSettings = AudioExportSettings()
    ),
    val showExportDialog: Boolean = false,
    val isProcessing: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class VideoEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoEditingService: VideoEditingService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(VideoEditorUiState())
    val uiState: StateFlow<VideoEditorUiState> = _uiState.asStateFlow()
    
    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        })
    }
    
    private val undoStack = mutableListOf<EditingAction>()
    private val redoStack = mutableListOf<EditingAction>()
    
    init {
        // Initialize video editing service
        viewModelScope.launch {
            val result = videoEditingService.initialize()
            if (result.success) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        availableEffects = result.availableFeatures
                            .filter { it.name.contains("EFFECT") }
                            .mapNotNull { 
                                try { 
                                    EffectType.valueOf(it.name.replace("EFFECT_", ""))
                                } catch (e: Exception) { null }
                            },
                        availableTransitions = TransitionType.values().toList()
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = result.error
                    )
                }
            }
        }
        
        // Update playback position
        viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    _uiState.update { 
                        it.copy(
                            currentPosition = player.currentPosition,
                            duration = player.duration.coerceAtLeast(0)
                        )
                    }
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }
    
    fun loadVideo(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Create or load project
            val projectName = "Project_${System.currentTimeMillis()}"
            val projectResult = videoEditingService.createProject(
                name = projectName,
                settings = ProjectSettings()
            )
            
            if (projectResult.success && projectResult.project != null) {
                // Import video
                val importResult = videoEditingService.importVideo(uri)
                
                if (importResult.success && importResult.clip != null) {
                    // Add clip to timeline
                    val timeline = projectResult.project.timeline
                    if (timeline.videoTracks.isEmpty()) {
                        timeline.videoTracks.add(
                            VideoTrack(
                                id = UUID.randomUUID().toString(),
                                name = "Video 1"
                            )
                        )
                    }
                    timeline.videoTracks.first().clips.add(importResult.clip)
                    
                    // Load video in player
                    player.setMediaItem(MediaItem.fromUri(uri))
                    player.prepare()
                    
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            currentProject = projectResult.project,
                            timeline = timeline,
                            duration = importResult.videoInfo?.duration ?: 0L
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = importResult.error
                        )
                    }
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = projectResult.error
                    )
                }
            }
        }
    }
    
    fun togglePlayback() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }
    
    fun seekTo(position: Long) {
        player.seekTo(position)
        _uiState.update { it.copy(currentPosition = position) }
    }
    
    fun selectTab(tab: EditorTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }
    
    fun selectClip(clip: TimelineClip?) {
        _uiState.update { it.copy(selectedClip = clip) }
    }
    
    fun moveClip(clip: TimelineClip, newPosition: Long) {
        viewModelScope.launch {
            val oldPosition = clip.startTime
            clip.startTime = newPosition
            
            addToUndoStack(
                EditingAction.MoveClip(clip, oldPosition, newPosition)
            )
            
            _uiState.update { it.copy(timeline = it.timeline) }
        }
    }
    
    fun trimClip(clip: TimelineClip, newStart: Long, newEnd: Long) {
        viewModelScope.launch {
            val originalClip = clip.copy()
            val newDuration = newEnd - newStart
            
            clip.startTime = newStart
            clip.duration = newDuration
            
            addToUndoStack(
                EditingAction.TrimClip(originalClip, clip)
            )
            
            _uiState.update { it.copy(timeline = it.timeline) }
        }
    }
    
    fun splitClip(clip: TimelineClip, splitPoint: Long) {
        viewModelScope.launch {
            val track = _uiState.value.timeline.videoTracks
                .firstOrNull { it.clips.contains(clip) } ?: return@launch
            
            val relativePoint = splitPoint - clip.startTime
            if (relativePoint <= 0 || relativePoint >= clip.duration) return@launch
            
            // Create two new clips
            val firstClip = clip.copy(
                id = UUID.randomUUID().toString(),
                duration = relativePoint
            )
            
            val secondClip = clip.copy(
                id = UUID.randomUUID().toString(),
                startTime = clip.startTime + relativePoint,
                duration = clip.duration - relativePoint
            )
            
            // Replace original clip
            val clipIndex = track.clips.indexOf(clip)
            track.clips.removeAt(clipIndex)
            track.clips.add(clipIndex, firstClip)
            track.clips.add(clipIndex + 1, secondClip)
            
            _uiState.update { it.copy(timeline = it.timeline) }
        }
    }
    
    fun applyEffect(effectType: EffectType) {
        val selectedClip = _uiState.value.selectedClip ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            
            val effect = VideoEffect(
                id = UUID.randomUUID().toString(),
                type = effectType,
                name = effectType.name
            )
            
            val result = videoEditingService.applyEffect(selectedClip, effect)
            
            if (result.success) {
                selectedClip.effects.add(effect)
                addToUndoStack(
                    EditingAction.ApplyEffect(selectedClip.copy(), selectedClip, effect)
                )
            }
            
            _uiState.update { 
                it.copy(
                    isProcessing = false,
                    timeline = it.timeline,
                    error = result.error
                )
            }
        }
    }
    
    fun addTransition(transitionType: TransitionType) {
        // Find adjacent clips
        val timeline = _uiState.value.timeline
        val selectedClip = _uiState.value.selectedClip ?: return
        
        viewModelScope.launch {
            val track = timeline.videoTracks
                .firstOrNull { it.clips.contains(selectedClip) } ?: return@launch
            
            val clipIndex = track.clips.indexOf(selectedClip)
            if (clipIndex < track.clips.size - 1) {
                val nextClip = track.clips[clipIndex + 1]
                
                val transition = VideoTransition(
                    id = UUID.randomUUID().toString(),
                    type = transitionType,
                    duration = 1000L, // 1 second default
                    startTime = selectedClip.startTime + selectedClip.duration - 500L
                )
                
                val result = videoEditingService.addTransition(
                    selectedClip, nextClip, transition
                )
                
                if (result.success) {
                    selectedClip.transitions.add(transition)
                    addToUndoStack(
                        EditingAction.AddTransition(selectedClip, nextClip, transition)
                    )
                }
                
                _uiState.update { 
                    it.copy(
                        timeline = it.timeline,
                        error = result.error
                    )
                }
            }
        }
    }
    
    fun updateAudioAdjustments(adjustments: AudioAdjustments) {
        _uiState.update { it.copy(audioAdjustments = adjustments) }
        
        val selectedClip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            val result = videoEditingService.adjustAudio(selectedClip, adjustments)
            if (!result.success) {
                _uiState.update { it.copy(error = result.error) }
            }
        }
    }
    
    fun updateColorCorrection(settings: ColorCorrectionSettings) {
        _uiState.update { it.copy(colorCorrectionSettings = settings) }
        
        val selectedClip = _uiState.value.selectedClip ?: return
        viewModelScope.launch {
            val result = videoEditingService.applyColorCorrection(selectedClip, settings)
            if (!result.success) {
                _uiState.update { it.copy(error = result.error) }
            }
        }
    }
    
    fun addTextOverlay(text: String) {
        viewModelScope.launch {
            val overlay = VideoOverlay(
                id = UUID.randomUUID().toString(),
                type = OverlayType.TEXT,
                name = "Text",
                startTime = _uiState.value.currentPosition,
                duration = 5000L, // 5 seconds default
                content = OverlayContent(text = text)
            )
            
            val result = videoEditingService.addOverlay(overlay)
            
            if (result.success) {
                if (_uiState.value.timeline.overlayTracks.isEmpty()) {
                    _uiState.value.timeline.overlayTracks.add(
                        OverlayTrack(
                            id = UUID.randomUUID().toString(),
                            name = "Overlays"
                        )
                    )
                }
                _uiState.value.timeline.overlayTracks.first().overlays.add(overlay)
                
                addToUndoStack(EditingAction.AddOverlay(overlay))
            }
            
            _uiState.update { 
                it.copy(
                    timeline = it.timeline,
                    error = result.error
                )
            }
        }
    }
    
    fun setZoomLevel(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom) }
    }
    
    fun saveProject() {
        viewModelScope.launch {
            val project = _uiState.value.currentProject ?: return@launch
            videoEditingService.saveProject(project)
        }
    }
    
    fun showExportDialog() {
        _uiState.update { it.copy(showExportDialog = true) }
    }
    
    fun hideExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }
    
    fun updateExportSettings(settings: ExportSettings) {
        _uiState.update { it.copy(exportSettings = settings) }
    }
    
    fun exportVideo(onComplete: (VideoExportResult) -> Unit) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    showExportDialog = false,
                    isExporting = true,
                    exportProgress = 0f
                )
            }
            
            val timeline = _uiState.value.timeline
            val settings = _uiState.value.exportSettings.copy(
                outputUri = generateOutputUri()
            )
            
            val result = videoEditingService.exportVideo(
                timeline = timeline,
                settings = settings,
                progressCallback = { progress ->
                    _uiState.update { it.copy(exportProgress = progress) }
                }
            )
            
            _uiState.update { 
                it.copy(
                    isExporting = false,
                    exportProgress = 0f
                )
            }
            
            onComplete(result)
        }
    }
    
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val action = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(action)
            
            // Apply undo logic based on action type
            viewModelScope.launch {
                videoEditingService.undo()
                updateUndoRedoState()
            }
        }
    }
    
    fun redo() {
        if (redoStack.isNotEmpty()) {
            val action = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(action)
            
            // Apply redo logic based on action type
            viewModelScope.launch {
                videoEditingService.redo()
                updateUndoRedoState()
            }
        }
    }
    
    private fun addToUndoStack(action: EditingAction) {
        undoStack.add(action)
        redoStack.clear()
        updateUndoRedoState()
    }
    
    private fun updateUndoRedoState() {
        _uiState.update { 
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }
    
    private fun generateOutputUri(): Uri {
        val fileName = "export_${System.currentTimeMillis()}.mp4"
        val file = java.io.File(context.getExternalFilesDir("exports"), fileName)
        return Uri.fromFile(file)
    }
    
    override fun onCleared() {
        super.onCleared()
        player.release()
        videoEditingService.cleanup()
    }
}