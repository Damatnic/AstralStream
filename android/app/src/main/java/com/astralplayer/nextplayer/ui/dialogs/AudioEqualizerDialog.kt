package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.audio.*
import com.astralplayer.nextplayer.ui.components.*
import kotlinx.coroutines.launch

/**
 * Comprehensive Audio Equalizer Dialog
 * Advanced audio controls with bubble UI design
 */
@Composable
fun AudioEqualizerDialog(
    equalizerManager: AudioEqualizerManager,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // State
    val isEnabled by equalizerManager.isEnabled.collectAsState()
    val currentPreset by equalizerManager.currentPreset.collectAsState()
    val bandLevels by equalizerManager.bandLevels.collectAsState()
    val bassLevel by equalizerManager.bassLevel.collectAsState()
    val virtualizerLevel by equalizerManager.virtualizerLevel.collectAsState()
    val reverbLevel by equalizerManager.reverbLevel.collectAsState()
    val loudnessLevel by equalizerManager.loudnessLevel.collectAsState()
    
    var selectedTab by remember { mutableStateOf(EqualizerTab.EQUALIZER) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showCustomPresetsDialog by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        BubbleCard(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            elevation = 24,
            cornerRadius = 32,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                EqualizerHeader(
                    isEnabled = isEnabled,
                    onToggleEnabled = { equalizerManager.setEnabled(it) },
                    onClose = onDismiss
                )
                
                // Tabs
                EqualizerTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                
                // Content
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        slideInHorizontally { it } with slideOutHorizontally { -it }
                    }
                ) { tab ->
                    when (tab) {
                        EqualizerTab.EQUALIZER -> {
                            EqualizerContent(
                                currentPreset = currentPreset,
                                bandLevels = bandLevels,
                                onPresetSelected = { equalizerManager.applyPreset(it) },
                                onBandLevelChanged = { band, level ->
                                    equalizerManager.setBandLevel(band, level)
                                },
                                onSavePreset = { showSavePresetDialog = true },
                                onLoadCustomPresets = { showCustomPresetsDialog = true }
                            )
                        }
                        
                        EqualizerTab.EFFECTS -> {
                            EffectsContent(
                                bassLevel = bassLevel,
                                virtualizerLevel = virtualizerLevel,
                                reverbLevel = reverbLevel,
                                loudnessLevel = loudnessLevel,
                                onBassLevelChanged = { equalizerManager.setBassBoostLevel(it) },
                                onVirtualizerLevelChanged = { equalizerManager.setVirtualizerLevel(it) },
                                onReverbPresetChanged = { preset ->
                                    equalizerManager.setReverbPreset(preset)
                                },
                                onLoudnessLevelChanged = { equalizerManager.setLoudnessGain(it) }
                            )
                        }
                        
                        EqualizerTab.VISUALIZER -> {
                            VisualizerContent()
                        }
                    }
                }
            }
        }
    }
    
    // Save Preset Dialog
    if (showSavePresetDialog) {
        SavePresetDialog(
            onSave = { name ->
                equalizerManager.saveCustomPreset(name)
                showSavePresetDialog = false
            },
            onDismiss = { showSavePresetDialog = false }
        )
    }
    
    // Custom Presets Dialog
    if (showCustomPresetsDialog) {
        CustomPresetsDialog(
            customPresets = equalizerManager.getCustomPresets(),
            onPresetSelected = { preset ->
                equalizerManager.loadCustomPreset(preset)
                showCustomPresetsDialog = false
            },
            onPresetDeleted = { preset ->
                equalizerManager.deleteCustomPreset(preset)
            },
            onDismiss = { showCustomPresetsDialog = false }
        )
    }
}

@Composable
private fun EqualizerHeader(
    isEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Equalizer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Column {
                Text(
                    text = "Audio Equalizer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = if (isEnabled) "Active" else "Inactive",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggleEnabled
            )
            
            BubbleIconButton(
                onClick = onClose,
                icon = Icons.Default.Close,
                size = 36,
                iconSize = 20
            )
        }
    }
}

@Composable
private fun EqualizerTabs(
    selectedTab: EqualizerTab,
    onTabSelected: (EqualizerTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EqualizerTab.values().forEach { tab ->
            BubbleChip(
                text = tab.title,
                onClick = { onTabSelected(tab) },
                selected = selectedTab == tab,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EqualizerContent(
    currentPreset: EqualizerPreset,
    bandLevels: List<BandLevel>,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onBandLevelChanged: (Short, Int) -> Unit,
    onSavePreset: () -> Unit,
    onLoadCustomPresets: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Preset selector
        item {
            PresetSelector(
                currentPreset = currentPreset,
                onPresetSelected = onPresetSelected,
                onCustomPresetsClick = onLoadCustomPresets
            )
        }
        
        // Frequency bands
        item {
            FrequencyBands(
                bandLevels = bandLevels,
                onBandLevelChanged = onBandLevelChanged
            )
        }
        
        // Save preset button
        if (currentPreset == EqualizerPreset.CUSTOM) {
            item {
                BubbleButton(
                    onClick = onSavePreset,
                    text = "Save as Custom Preset",
                    icon = Icons.Default.Save,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }
}

@Composable
private fun PresetSelector(
    currentPreset: EqualizerPreset,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onCustomPresetsClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (currentPreset == EqualizerPreset.CUSTOM) {
                BubbleButton(
                    onClick = onCustomPresetsClick,
                    text = "Custom Presets",
                    icon = Icons.Default.FolderOpen,
                    cornerRadius = 16,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(EqualizerPreset.values().filter { it != EqualizerPreset.CUSTOM }) { preset ->
                PresetChip(
                    preset = preset,
                    isSelected = currentPreset == preset,
                    onSelect = { onPresetSelected(preset) }
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    preset: EqualizerPreset,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    BubbleCard(
        onClick = onSelect,
        modifier = Modifier
            .scale(animatedScale)
            .width(120.dp),
        elevation = if (isSelected) 8 else 4,
        cornerRadius = 20,
        containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getPresetIcon(preset),
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = preset.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FrequencyBands(
    bandLevels: List<BandLevel>,
    onBandLevelChanged: (Short, Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Frequency Bands",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        BubbleCard(
            elevation = 4,
            cornerRadius = 24,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bandLevels.forEach { band ->
                    FrequencyBandSlider(
                        band = band,
                        onLevelChanged = { level ->
                            onBandLevelChanged(band.band, level)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyBandSlider(
    band: BandLevel,
    onLevelChanged: (Int) -> Unit
) {
    var sliderValue by remember(band.currentLevel) { 
        mutableStateOf(band.currentLevel.toFloat()) 
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Frequency label
        Text(
            text = if (band.frequency >= 1000) {
                "${band.frequency / 1000}k"
            } else {
                "${band.frequency}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Vertical slider container
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
            
            // Active track
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(
                        (sliderValue - band.minLevel) / 
                        (band.maxLevel - band.minLevel).toFloat()
                    )
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            
            // Slider (rotated)
            Slider(
                value = sliderValue,
                onValueChange = { 
                    sliderValue = it
                    onLevelChanged(it.toInt())
                },
                valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = -90f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    }
                    .width(200.dp)
                    .offset(x = 100.dp, y = (-100).dp)
            )
        }
        
        // Level label
        Text(
            text = "${(sliderValue / 100).toInt()}dB",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EffectsContent(
    bassLevel: Int,
    virtualizerLevel: Int,
    reverbLevel: Int,
    loudnessLevel: Int,
    onBassLevelChanged: (Int) -> Unit,
    onVirtualizerLevelChanged: (Int) -> Unit,
    onReverbPresetChanged: (ReverbPreset) -> Unit,
    onLoudnessLevelChanged: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bass Boost
        item {
            EffectControl(
                title = "Bass Boost",
                icon = Icons.Default.GraphicEq,
                value = bassLevel,
                maxValue = 1000,
                unit = "%",
                onValueChange = onBassLevelChanged
            )
        }
        
        // Virtualizer
        item {
            EffectControl(
                title = "3D Surround",
                icon = Icons.Default.Surround,
                value = virtualizerLevel,
                maxValue = 1000,
                unit = "%",
                onValueChange = onVirtualizerLevelChanged
            )
        }
        
        // Reverb
        item {
            ReverbControl(
                currentReverbLevel = reverbLevel,
                onReverbPresetChanged = onReverbPresetChanged
            )
        }
        
        // Loudness Enhancer
        item {
            EffectControl(
                title = "Loudness",
                icon = Icons.Default.VolumeUp,
                value = loudnessLevel,
                maxValue = 5000,
                unit = "mB",
                onValueChange = onLoudnessLevelChanged
            )
        }
    }
}

@Composable
private fun EffectControl(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    maxValue: Int,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    BubbleCard(
        elevation = 4,
        cornerRadius = 20,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = if (unit == "%") {
                        "${(value * 100 / maxValue)}$unit"
                    } else {
                        "$value$unit"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..maxValue.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReverbControl(
    currentReverbLevel: Int,
    onReverbPresetChanged: (ReverbPreset) -> Unit
) {
    BubbleCard(
        elevation = 4,
        cornerRadius = 20,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Waves,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "Reverb",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ReverbPreset.values()) { preset ->
                    BubbleChip(
                        text = preset.displayName,
                        selected = currentReverbLevel == preset.value,
                        onClick = { onReverbPresetChanged(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VisualizerContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated visualizer bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(200.dp)
            ) {
                repeat(20) { index ->
                    VisualizerBar(index)
                }
            }
            
            Text(
                text = "Audio Visualizer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VisualizerBar(index: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val height by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500 + index * 50,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier
            .width(8.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
    )
}

// Helper dialogs

@Composable
private fun SavePresetDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        BubbleCard(
            elevation = 16,
            cornerRadius = 24
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Save Custom Preset",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BubbleButton(
                        onClick = onDismiss,
                        text = "Cancel",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    BubbleButton(
                        onClick = { 
                            if (presetName.isNotBlank()) {
                                onSave(presetName)
                            }
                        },
                        text = "Save",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomPresetsDialog(
    customPresets: List<CustomEqualizerPreset>,
    onPresetSelected: (CustomEqualizerPreset) -> Unit,
    onPresetDeleted: (CustomEqualizerPreset) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        BubbleCard(
            elevation = 16,
            cornerRadius = 24,
            modifier = Modifier.fillMaxHeight(0.6f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Custom Presets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (customPresets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No custom presets saved",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(customPresets) { preset ->
                            CustomPresetItem(
                                preset = preset,
                                onSelect = { onPresetSelected(preset) },
                                onDelete = { onPresetDeleted(preset) }
                            )
                        }
                    }
                }
                
                BubbleButton(
                    onClick = onDismiss,
                    text = "Close",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomPresetItem(
    preset: CustomEqualizerPreset,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    BubbleCard(
        onClick = onSelect,
        elevation = 2,
        cornerRadius = 16,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyLarge
            )
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Utility functions

private fun getPresetIcon(preset: EqualizerPreset) = when (preset) {
    EqualizerPreset.NORMAL -> Icons.Default.Tune
    EqualizerPreset.CLASSICAL -> Icons.Default.MusicNote
    EqualizerPreset.DANCE -> Icons.Default.MusicNote
    EqualizerPreset.FLAT -> Icons.Default.HorizontalRule
    EqualizerPreset.FOLK -> Icons.Default.Grass
    EqualizerPreset.HEAVY_METAL -> Icons.Default.Bolt
    EqualizerPreset.HIP_HOP -> Icons.Default.Headphones
    EqualizerPreset.JAZZ -> Icons.Default.MusicNote
    EqualizerPreset.POP -> Icons.Default.Star
    EqualizerPreset.ROCK -> Icons.Default.VolumeUp
    EqualizerPreset.MOVIE -> Icons.Default.Movie
    EqualizerPreset.MUSIC -> Icons.Default.LibraryMusic
    EqualizerPreset.VOICE -> Icons.Default.Mic
    EqualizerPreset.CUSTOM -> Icons.Default.Settings
}

enum class EqualizerTab(val title: String) {
    EQUALIZER("Equalizer"),
    EFFECTS("Effects"),
    VISUALIZER("Visualizer")
}