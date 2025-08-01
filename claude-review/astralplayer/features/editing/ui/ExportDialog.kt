package com.astralplayer.features.editing.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.editing.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    exportSettings: ExportSettings,
    onSettingsChanged: (ExportSettings) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit
) {
    var showAdvancedSettings by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Export Video",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
                
                // Settings
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quality Presets
                    QualityPresetSection(
                        currentQuality = exportSettings.quality,
                        onQualitySelected = { quality ->
                            val (resolution, bitrate) = getPresetValues(quality)
                            onSettingsChanged(
                                exportSettings.copy(
                                    quality = quality,
                                    resolution = resolution,
                                    bitrate = bitrate
                                )
                            )
                        }
                    )
                    
                    Divider()
                    
                    // Format Selection
                    FormatSection(
                        currentFormat = exportSettings.outputFormat,
                        onFormatSelected = { format ->
                            onSettingsChanged(exportSettings.copy(outputFormat = format))
                        }
                    )
                    
                    Divider()
                    
                    // Resolution Settings
                    ResolutionSection(
                        currentResolution = exportSettings.resolution,
                        onResolutionChanged = { resolution ->
                            onSettingsChanged(exportSettings.copy(resolution = resolution))
                        }
                    )
                    
                    Divider()
                    
                    // Frame Rate
                    FrameRateSection(
                        currentFrameRate = exportSettings.frameRate,
                        onFrameRateChanged = { frameRate ->
                            onSettingsChanged(exportSettings.copy(frameRate = frameRate))
                        }
                    )
                    
                    // Advanced Settings Toggle
                    TextButton(
                        onClick = { showAdvancedSettings = !showAdvancedSettings },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showAdvancedSettings) "Hide Advanced Settings" else "Show Advanced Settings")
                    }
                    
                    // Advanced Settings
                    if (showAdvancedSettings) {
                        AdvancedSettingsSection(
                            exportSettings = exportSettings,
                            onSettingsChanged = onSettingsChanged
                        )
                    }
                }
                
                // Footer
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        
                        Button(onClick = onExport) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityPresetSection(
    currentQuality: ExportQuality,
    onQualitySelected: (ExportQuality) -> Unit
) {
    Column {
        Text(
            "Quality Preset",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            ExportQuality.values().filter { it != ExportQuality.CUSTOM }.forEachIndexed { index, quality ->
                SegmentedButton(
                    selected = currentQuality == quality,
                    onClick = { onQualitySelected(quality) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ExportQuality.values().size - 1
                    )
                ) {
                    Text(quality.name.capitalize())
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Quality description
        Text(
            text = getQualityDescription(currentQuality),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FormatSection(
    currentFormat: VideoFormat,
    onFormatSelected: (VideoFormat) -> Unit
) {
    Column {
        Text(
            "Output Format",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val commonFormats = listOf(VideoFormat.MP4, VideoFormat.MOV, VideoFormat.MKV, VideoFormat.WEBM)
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonFormats.forEach { format ->
                FilterChip(
                    selected = currentFormat == format,
                    onClick = { onFormatSelected(format) },
                    label = { Text(format.name) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolutionSection(
    currentResolution: Pair<Int, Int>,
    onResolutionChanged: (Pair<Int, Int>) -> Unit
) {
    Column {
        Text(
            "Resolution",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val commonResolutions = listOf(
            "4K" to Pair(3840, 2160),
            "1080p" to Pair(1920, 1080),
            "720p" to Pair(1280, 720),
            "480p" to Pair(854, 480)
        )
        
        var showCustom by remember { mutableStateOf(false) }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonResolutions.forEach { (name, resolution) ->
                FilterChip(
                    selected = currentResolution == resolution,
                    onClick = { 
                        onResolutionChanged(resolution)
                        showCustom = false
                    },
                    label = { Text(name) }
                )
            }
            
            FilterChip(
                selected = showCustom,
                onClick = { showCustom = true },
                label = { Text("Custom") }
            )
        }
        
        if (showCustom) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentResolution.first.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { width ->
                            onResolutionChanged(width to currentResolution.second)
                        }
                    },
                    label = { Text("Width") },
                    modifier = Modifier.weight(1f)
                )
                
                Text("×")
                
                OutlinedTextField(
                    value = currentResolution.second.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { height ->
                            onResolutionChanged(currentResolution.first to height)
                        }
                    },
                    label = { Text("Height") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FrameRateSection(
    currentFrameRate: Float,
    onFrameRateChanged: (Float) -> Unit
) {
    Column {
        Text(
            "Frame Rate",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val commonFrameRates = listOf(24f, 25f, 30f, 50f, 60f)
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonFrameRates.forEach { fps ->
                FilterChip(
                    selected = currentFrameRate == fps,
                    onClick = { onFrameRateChanged(fps) },
                    label = { Text("${fps.toInt()} fps") }
                )
            }
        }
    }
}

@Composable
private fun AdvancedSettingsSection(
    exportSettings: ExportSettings,
    onSettingsChanged: (ExportSettings) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bitrate
        BitrateSettings(
            currentBitrate = exportSettings.bitrate,
            onBitrateChanged = { bitrate ->
                onSettingsChanged(exportSettings.copy(bitrate = bitrate))
            }
        )
        
        // Audio Settings
        AudioExportSettings(
            audioSettings = exportSettings.audioSettings,
            onAudioSettingsChanged = { audioSettings ->
                onSettingsChanged(exportSettings.copy(audioSettings = audioSettings))
            }
        )
        
        // Hardware Acceleration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("GPU Acceleration")
                Text(
                    "Use hardware encoding for faster export",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = exportSettings.enableGPUAcceleration,
                onCheckedChange = { enabled ->
                    onSettingsChanged(exportSettings.copy(enableGPUAcceleration = enabled))
                }
            )
        }
        
        // Multi-threading
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Multi-threading")
                Text(
                    "Use multiple CPU cores for encoding",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = exportSettings.enableMultiThreading,
                onCheckedChange = { enabled ->
                    onSettingsChanged(exportSettings.copy(enableMultiThreading = enabled))
                }
            )
        }
    }
}

@Composable
private fun BitrateSettings(
    currentBitrate: Long,
    onBitrateChanged: (Long) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Video Bitrate")
            Text("${currentBitrate / 1_000_000} Mbps")
        }
        
        Slider(
            value = currentBitrate.toFloat(),
            onValueChange = { onBitrateChanged(it.toLong()) },
            valueRange = 1_000_000f..50_000_000f,
            steps = 49,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AudioExportSettings(
    audioSettings: AudioExportSettings,
    onAudioSettingsChanged: (AudioExportSettings) -> Unit
) {
    Column {
        Text(
            "Audio Settings",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Audio Codec
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioCodec.values().take(3).forEach { codec ->
                FilterChip(
                    selected = audioSettings.codec == codec,
                    onClick = { 
                        onAudioSettingsChanged(audioSettings.copy(codec = codec))
                    },
                    label = { Text(codec.name) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Audio Bitrate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Audio Bitrate")
            Text("${audioSettings.bitrate / 1000} kbps")
        }
        
        Slider(
            value = audioSettings.bitrate.toFloat(),
            onValueChange = { 
                onAudioSettingsChanged(audioSettings.copy(bitrate = it.toLong()))
            },
            valueRange = 64_000f..320_000f,
            steps = 7,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun getPresetValues(quality: ExportQuality): Pair<Pair<Int, Int>, Long> {
    return when (quality) {
        ExportQuality.LOW -> Pair(854, 480) to 2_000_000L
        ExportQuality.MEDIUM -> Pair(1280, 720) to 5_000_000L
        ExportQuality.HIGH -> Pair(1920, 1080) to 10_000_000L
        ExportQuality.ULTRA -> Pair(3840, 2160) to 25_000_000L
        ExportQuality.LOSSLESS -> Pair(3840, 2160) to 50_000_000L
        ExportQuality.CUSTOM -> Pair(1920, 1080) to 10_000_000L
    }
}

private fun getQualityDescription(quality: ExportQuality): String {
    return when (quality) {
        ExportQuality.LOW -> "Fast export, smaller file size, suitable for sharing"
        ExportQuality.MEDIUM -> "Good balance between quality and file size"
        ExportQuality.HIGH -> "High quality, larger file size"
        ExportQuality.ULTRA -> "Ultra high quality, very large file size"
        ExportQuality.LOSSLESS -> "No quality loss, extremely large file size"
        ExportQuality.CUSTOM -> "Custom settings"
    }
}