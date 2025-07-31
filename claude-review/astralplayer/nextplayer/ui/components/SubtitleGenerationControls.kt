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
import com.astralplayer.nextplayer.subtitle.AdvancedAISubtitleGenerator
import kotlinx.coroutines.launch

@Composable
fun SubtitleGenerationControls(
    subtitleGenerator: AdvancedAISubtitleGenerator,
    onDismiss: () -> Unit,
    onSubtitlesGenerated: (List<AdvancedAISubtitleGenerator.SubtitleEntry>) -> Unit
) {
    val generationState by subtitleGenerator.generationState.collectAsState()
    val progress by subtitleGenerator.progress.collectAsState()
    val scope = rememberCoroutineScope()
    
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Subtitle Generation",
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
                    targetState = generationState,
                    transitionSpec = {
                        fadeIn() + scaleIn() with fadeOut() + scaleOut()
                    }
                ) { state ->
                    when (state) {
                        is AdvancedAISubtitleGenerator.GenerationState.Idle -> {
                            IdleStateDisplay()
                        }
                        is AdvancedAISubtitleGenerator.GenerationState.Preparing -> {
                            PreparingStateDisplay()
                        }
                        is AdvancedAISubtitleGenerator.GenerationState.Generating -> {
                            GeneratingStateDisplay(progress)
                        }
                        is AdvancedAISubtitleGenerator.GenerationState.Complete -> {
                            CompleteStateDisplay()
                        }
                        is AdvancedAISubtitleGenerator.GenerationState.Error -> {
                            ErrorStateDisplay(state.message)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Progress bar
                if (generationState !is AdvancedAISubtitleGenerator.GenerationState.Idle &&
                    generationState !is AdvancedAISubtitleGenerator.GenerationState.Complete &&
                    generationState !is AdvancedAISubtitleGenerator.GenerationState.Error) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (generationState) {
                        is AdvancedAISubtitleGenerator.GenerationState.Idle -> {
                            Button(
                                onClick = {
                                    // Start generation would be triggered from parent
                                    // This is just UI
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Subtitles")
                            }
                        }
                        
                        is AdvancedAISubtitleGenerator.GenerationState.Preparing,
                        is AdvancedAISubtitleGenerator.GenerationState.Generating -> {
                            OutlinedButton(
                                onClick = {
                                    subtitleGenerator.cancelGeneration()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel")
                            }
                        }
                        
                        is AdvancedAISubtitleGenerator.GenerationState.Complete -> {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Done")
                            }
                        }
                        
                        is AdvancedAISubtitleGenerator.GenerationState.Error -> {
                            OutlinedButton(
                                onClick = {
                                    // Retry would be triggered from parent
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
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
            Icons.Default.Subtitles,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Generate AI-powered subtitles",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Subtitles will be generated in 3-5 seconds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PreparingStateDisplay() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Preparing audio...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun GeneratingStateDisplay(progress: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Generating subtitles...",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Using AI to transcribe and time subtitles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompleteStateDisplay() {
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
            text = "Subtitles generated!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "AI-generated subtitles are now available",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            text = "Generation failed",
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