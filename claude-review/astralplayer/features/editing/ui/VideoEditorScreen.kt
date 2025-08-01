package com.astralplayer.features.editing.ui

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.astralplayer.features.editing.viewmodel.VideoEditorViewModel
import com.astralplayer.features.editing.viewmodel.EditorTab
import com.astralplayer.nextplayer.editing.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    videoUri: Uri,
    onNavigateBack: () -> Unit,
    onExportComplete: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideoEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    DisposableEffect(videoUri) {
        viewModel.loadVideo(videoUri)
        onDispose { }
    }
    
    Scaffold(
        topBar = {
            VideoEditorTopBar(
                projectName = uiState.currentProject?.name ?: "Untitled Project",
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onSave = viewModel::saveProject,
                onExport = { viewModel.showExportDialog() },
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Video Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = viewModel.player
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Playback Controls Overlay
                PlaybackControlsOverlay(
                    isPlaying = uiState.isPlaying,
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                    onPlayPause = viewModel::togglePlayback,
                    onSeek = viewModel::seekTo,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            
            // Timeline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                TimelineView(
                    timeline = uiState.timeline,
                    currentPosition = uiState.currentPosition,
                    duration = uiState.duration,
                    zoomLevel = uiState.zoomLevel,
                    selectedClip = uiState.selectedClip,
                    onClipSelected = viewModel::selectClip,
                    onClipMoved = viewModel::moveClip,
                    onTimelineSeek = viewModel::seekTo,
                    onZoomChange = viewModel::setZoomLevel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Editor Tools
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
            ) {
                when (uiState.currentTab) {
                    EditorTab.TRIM -> TrimTools(
                        selectedClip = uiState.selectedClip,
                        onTrim = viewModel::trimClip,
                        onSplit = viewModel::splitClip
                    )
                    EditorTab.EFFECTS -> EffectsPanel(
                        availableEffects = uiState.availableEffects,
                        selectedClip = uiState.selectedClip,
                        onEffectSelected = viewModel::applyEffect
                    )
                    EditorTab.TRANSITIONS -> TransitionsPanel(
                        availableTransitions = uiState.availableTransitions,
                        onTransitionSelected = viewModel::addTransition
                    )
                    EditorTab.AUDIO -> AudioTools(
                        selectedClip = uiState.selectedClip,
                        audioAdjustments = uiState.audioAdjustments,
                        onAdjustmentsChanged = viewModel::updateAudioAdjustments
                    )
                    EditorTab.COLOR -> ColorCorrectionTools(
                        selectedClip = uiState.selectedClip,
                        colorSettings = uiState.colorCorrectionSettings,
                        onSettingsChanged = viewModel::updateColorCorrection
                    )
                    EditorTab.TEXT -> TextOverlayTools(
                        onAddText = viewModel::addTextOverlay
                    )
                }
            }
            
            // Bottom Navigation
            EditorBottomBar(
                currentTab = uiState.currentTab,
                onTabSelected = viewModel::selectTab
            )
        }
        
        // Export Dialog
        if (uiState.showExportDialog) {
            ExportDialog(
                exportSettings = uiState.exportSettings,
                onSettingsChanged = viewModel::updateExportSettings,
                onExport = { 
                    viewModel.exportVideo { result ->
                        if (result.success) {
                            result.outputUri?.let { onExportComplete(it) }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Export failed: ${result.error}"
                                )
                            }
                        }
                    }
                },
                onDismiss = viewModel::hideExportDialog
            )
        }
        
        // Processing Indicator
        if (uiState.isProcessing || uiState.isExporting) {
            ProcessingOverlay(
                message = if (uiState.isExporting) "Exporting video..." else "Processing...",
                progress = uiState.exportProgress
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoEditorTopBar(
    projectName: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = { 
            Text(
                text = projectName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(
                onClick = onUndo,
                enabled = canUndo
            ) {
                Icon(Icons.Default.Undo, contentDescription = "Undo")
            }
            IconButton(
                onClick = onRedo,
                enabled = canRedo
            ) {
                Icon(Icons.Default.Redo, contentDescription = "Redo")
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }
            Button(
                onClick = onExport,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Export")
            }
        }
    )
}

@Composable
private fun PlaybackControlsOverlay(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    fontSize = 12.sp
                )
                
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { value ->
                        onSeek((value * duration).toLong())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
            
            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSeek(maxOf(0, currentPosition - 10000)) }) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White
                    )
                }
                
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(onClick = { onSeek(minOf(duration, currentPosition + 10000)) }) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorBottomBar(
    currentTab: EditorTab,
    onTabSelected: (EditorTab) -> Unit
) {
    NavigationBar {
        EditorTab.values().forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.title) },
                label = { Text(tab.title) }
            )
        }
    }
}

@Composable
private fun TrimTools(
    selectedClip: TimelineClip?,
    onTrim: (TimelineClip, Long, Long) -> Unit,
    onSplit: (TimelineClip, Long) -> Unit
) {
    if (selectedClip == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Select a clip to trim",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Trim Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Trim handles visualization would go here
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Text(
                        "Trim handles (to be implemented)",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { onSplit(selectedClip, selectedClip.startTime + selectedClip.duration / 2) }
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Split")
                }
                
                Button(
                    onClick = { /* Delete clip */ }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun EffectsPanel(
    availableEffects: List<EffectType>,
    selectedClip: TimelineClip?,
    onEffectSelected: (EffectType) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableEffects) { effect ->
            EffectCard(
                effect = effect,
                isEnabled = selectedClip != null,
                onClick = { onEffectSelected(effect) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EffectCard(
    effect: EffectType,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                getEffectIcon(effect),
                contentDescription = effect.name,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatEffectName(effect),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TransitionsPanel(
    availableTransitions: List<TransitionType>,
    onTransitionSelected: (TransitionType) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableTransitions) { transition ->
            TransitionCard(
                transition = transition,
                onClick = { onTransitionSelected(transition) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransitionCard(
    transition: TransitionType,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatTransitionName(transition),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AudioTools(
    selectedClip: TimelineClip?,
    audioAdjustments: AudioAdjustments,
    onAdjustmentsChanged: (AudioAdjustments) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Audio Adjustments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            VolumeControl(
                volume = audioAdjustments.volume,
                onVolumeChange = { volume ->
                    onAdjustmentsChanged(audioAdjustments.copy(volume = volume))
                }
            )
        }
        
        item {
            FadeControls(
                fadeIn = audioAdjustments.fadeIn,
                fadeOut = audioAdjustments.fadeOut,
                onFadeInChange = { fadeIn ->
                    onAdjustmentsChanged(audioAdjustments.copy(fadeIn = fadeIn))
                },
                onFadeOutChange = { fadeOut ->
                    onAdjustmentsChanged(audioAdjustments.copy(fadeOut = fadeOut))
                }
            )
        }
        
        item {
            AudioEffectsSection(
                adjustments = audioAdjustments,
                onAdjustmentsChanged = onAdjustmentsChanged
            )
        }
    }
}

@Composable
private fun VolumeControl(
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Volume")
                Text("${(volume * 100).toInt()}%")
            }
            
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..2f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FadeControls(
    fadeIn: Long,
    fadeOut: Long,
    onFadeInChange: (Long) -> Unit,
    onFadeOutChange: (Long) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Fade Effects",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Fade In")
                Text("${fadeIn / 1000.0}s")
            }
            
            Slider(
                value = fadeIn.toFloat(),
                onValueChange = { onFadeInChange(it.toLong()) },
                valueRange = 0f..5000f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Fade Out")
                Text("${fadeOut / 1000.0}s")
            }
            
            Slider(
                value = fadeOut.toFloat(),
                onValueChange = { onFadeOutChange(it.toLong()) },
                valueRange = 0f..5000f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AudioEffectsSection(
    adjustments: AudioAdjustments,
    onAdjustmentsChanged: (AudioAdjustments) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Audio Effects",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Normalize")
                Switch(
                    checked = adjustments.normalize,
                    onCheckedChange = { 
                        onAdjustmentsChanged(adjustments.copy(normalize = it))
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Remove Noise")
                Switch(
                    checked = adjustments.removeNoise,
                    onCheckedChange = { 
                        onAdjustmentsChanged(adjustments.copy(removeNoise = it))
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorCorrectionTools(
    selectedClip: TimelineClip?,
    colorSettings: ColorCorrectionSettings,
    onSettingsChanged: (ColorCorrectionSettings) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Color Correction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            ColorSlider(
                label = "Brightness",
                value = colorSettings.brightness,
                onValueChange = { onSettingsChanged(colorSettings.copy(brightness = it)) },
                valueRange = -1f..1f
            )
        }
        
        item {
            ColorSlider(
                label = "Contrast",
                value = colorSettings.contrast,
                onValueChange = { onSettingsChanged(colorSettings.copy(contrast = it)) },
                valueRange = 0f..2f
            )
        }
        
        item {
            ColorSlider(
                label = "Saturation",
                value = colorSettings.saturation,
                onValueChange = { onSettingsChanged(colorSettings.copy(saturation = it)) },
                valueRange = 0f..2f
            )
        }
        
        item {
            ColorSlider(
                label = "Temperature",
                value = colorSettings.temperature,
                onValueChange = { onSettingsChanged(colorSettings.copy(temperature = it)) },
                valueRange = -1f..1f
            )
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label)
                Text(String.format("%.2f", value))
            }
            
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TextOverlayTools(
    onAddText: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Text Overlay",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Enter text") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                if (text.isNotBlank()) {
                    onAddText(text)
                    text = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = text.isNotBlank()
        ) {
            Text("Add Text Overlay")
        }
    }
}

@Composable
private fun ProcessingOverlay(
    message: String,
    progress: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = if (progress > 0) progress else null,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (progress > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

// Helper functions
private fun formatTime(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
    } else {
        String.format("%d:%02d", minutes, seconds % 60)
    }
}

private fun getEffectIcon(effect: EffectType): ImageVector {
    return when (effect) {
        EffectType.BLUR -> Icons.Default.BlurOn
        EffectType.BRIGHTNESS_CONTRAST -> Icons.Default.WbSunny
        EffectType.COLOR_CORRECTION -> Icons.Default.ColorLens
        else -> Icons.Default.AutoAwesome
    }
}

private fun formatEffectName(effect: EffectType): String {
    return effect.name.replace('_', ' ').lowercase()
        .split(' ').joinToString(" ") { it.capitalize() }
}

private fun formatTransitionName(transition: TransitionType): String {
    return transition.name.replace('_', ' ').lowercase()
        .split(' ').joinToString(" ") { it.capitalize() }
}