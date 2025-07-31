package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.astralplayer.nextplayer.enhancement.SmartVideoEnhancementEngine
import kotlinx.coroutines.launch

@Composable
fun VideoEnhancementControls(
    enhancementEngine: SmartVideoEnhancementEngine,
    onDismiss: () -> Unit
) {
    val enhancementState by enhancementEngine.enhancementState.collectAsState()
    val performanceMetrics by enhancementEngine.performanceMetrics.collectAsState()
    val scope = rememberCoroutineScope()
    
    var currentSettings by remember {
        mutableStateOf(
            SmartVideoEnhancementEngine.EnhancementSettings()
        )
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video Enhancement",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Status display
                AnimatedContent(
                    targetState = enhancementState,
                    transitionSpec = {
                        fadeIn() + scaleIn() with fadeOut() + scaleOut()
                    }
                ) { state ->
                    when (state) {
                        is SmartVideoEnhancementEngine.EnhancementState.Idle -> {
                            IdleStateDisplay()
                        }
                        is SmartVideoEnhancementEngine.EnhancementState.Initializing -> {
                            InitializingStateDisplay()
                        }
                        is SmartVideoEnhancementEngine.EnhancementState.Ready -> {
                            ReadyStateDisplay()
                        }
                        is SmartVideoEnhancementEngine.EnhancementState.Processing -> {
                            ProcessingStateDisplay(performanceMetrics)
                        }
                        is SmartVideoEnhancementEngine.EnhancementState.Error -> {
                            ErrorStateDisplay(state.message)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Enhancement controls
                if (enhancementState is SmartVideoEnhancementEngine.EnhancementState.Ready ||
                    enhancementState is SmartVideoEnhancementEngine.EnhancementState.Processing) {
                    
                    EnhancementSettingsPanel(
                        settings = currentSettings,
                        onSettingsChanged = { newSettings ->
                            currentSettings = newSettings
                            enhancementEngine.updateSettings(newSettings)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Performance display
                    PerformanceMetricsDisplay(performanceMetrics)
                }
            }
        }
    }
}

@Composable
private fun IdleStateDisplay() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Icon(
            Icons.Default.AutoFixHigh,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AI Video Enhancement",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enhance video quality with AI upscaling, HDR, and denoising",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InitializingStateDisplay() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Initializing AI models...",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Loading GPU shaders and AI enhancement models",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadyStateDisplay() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Enhancement Ready",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "AI models loaded and ready for real-time enhancement",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProcessingStateDisplay(
    metrics: SmartVideoEnhancementEngine.PerformanceMetrics
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = metrics.gpuUsage / 100f,
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp,
                color = if (metrics.isWithinTarget) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
            Icon(
                Icons.Default.AutoFixHigh,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Enhancing Video",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${metrics.frameProcessingTime.toInt()}ms processing time",
            style = MaterialTheme.typography.bodySmall,
            color = if (metrics.isWithinTarget) 
                MaterialTheme.colorScheme.onSurfaceVariant 
            else 
                MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ErrorStateDisplay(errorMessage: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Enhancement Error",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EnhancementSettingsPanel(
    settings: SmartVideoEnhancementEngine.EnhancementSettings,
    onSettingsChanged: (SmartVideoEnhancementEngine.EnhancementSettings) -> Unit
) {
    Column {
        Text(
            text = "Enhancement Settings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Upscaling toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI Upscaling",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Enhance resolution using AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.upscalingEnabled,
                onCheckedChange = { 
                    onSettingsChanged(settings.copy(upscalingEnabled = it))
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // HDR toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HDR Enhancement",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Improve color and contrast",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.hdrEnabled,
                onCheckedChange = { 
                    onSettingsChanged(settings.copy(hdrEnabled = it))
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Denoise toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Noise Reduction",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Remove noise and artifacts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.denoiseEnabled,
                onCheckedChange = { 
                    onSettingsChanged(settings.copy(denoiseEnabled = it))
                }
            )
        }
        
        if (settings.hdrEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // HDR Exposure slider
            Column {
                Text(
                    text = "HDR Exposure: ${String.format("%.1f", settings.hdrExposure)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = settings.hdrExposure,
                    onValueChange = { 
                        onSettingsChanged(settings.copy(hdrExposure = it))
                    },
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        if (settings.denoiseEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Denoise strength slider
            Column {
                Text(
                    text = "Denoise Strength: ${String.format("%.1f", settings.denoiseStrength)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = settings.denoiseStrength,
                    onValueChange = { 
                        onSettingsChanged(settings.copy(denoiseStrength = it))
                    },
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PerformanceMetricsDisplay(
    metrics: SmartVideoEnhancementEngine.PerformanceMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Frame Time",
                    value = "${metrics.frameProcessingTime.toInt()}ms",
                    isGood = metrics.frameProcessingTime < 16.67f
                )
                MetricItem(
                    label = "GPU Usage",
                    value = "${metrics.gpuUsage.toInt()}%",
                    isGood = metrics.gpuUsage < 90f
                )
                MetricItem(
                    label = "CPU Usage",
                    value = "${metrics.cpuUsage.toInt()}%",
                    isGood = metrics.cpuUsage < 90f
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (metrics.isWithinTarget) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (metrics.isWithinTarget) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (metrics.isWithinTarget) "Performance optimal" else "Performance degraded",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (metrics.isWithinTarget) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    isGood: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isGood) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.error
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}