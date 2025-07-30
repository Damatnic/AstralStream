package com.astralplayer.nextplayer.ui.dialogs

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.astralplayer.nextplayer.data.ai.GoogleAISubtitleGenerator
import com.astralplayer.nextplayer.data.ai.SubtitleEntry
import kotlinx.coroutines.launch

/**
 * Dialog for AI subtitle generation and management
 */
@Composable
fun AISubtitleGeneratorDialog(
    aiGenerator: GoogleAISubtitleGenerator,
    videoUri: Uri? = null,
    onDismiss: () -> Unit,
    onSubtitlesGenerated: (List<SubtitleEntry>) -> Unit = {}
) {
    val state by aiGenerator.state.collectAsState()
    var selectedLanguage by remember { mutableStateOf("en-US") }
    var showLanguageSelector by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Subtitle Generator",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                // Language selection
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Language",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Button(
                            onClick = { showLanguageSelector = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("$selectedLanguage ▼")
                        }
                    }
                }
                
                // Generation controls
                if (!state.isGenerating) {
                    Button(
                        onClick = {
                            videoUri?.let { uri ->
                                lifecycleOwner.lifecycleScope.launch {
                                    aiGenerator.generateSubtitles(
                                        videoUri = uri,
                                        language = selectedLanguage
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate AI Subtitles")
                    }
                } else {
                    // Progress indicator
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Generating subtitles...",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            LinearProgressIndicator(
                                progress = state.generationProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                text = "${(state.generationProgress * 100).toInt()}%",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                // Translation controls
                if (state.generatedSubtitles.isNotEmpty() && !state.isTranslating) {
                    Button(
                        onClick = {
                            lifecycleOwner.lifecycleScope.launch {
                                aiGenerator.translateSubtitles(
                                    subtitles = state.generatedSubtitles,
                                    targetLanguage = selectedLanguage
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Blue
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translate Subtitles")
                    }
                }
                
                // Error display
                state.error?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Generated subtitles display
                if (state.generatedSubtitles.isNotEmpty()) {
                    Text(
                        text = "Generated Subtitles (${state.generatedSubtitles.size})",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.generatedSubtitles.take(10)) { subtitle ->
                            SubtitleDisplayItem(subtitle = subtitle)
                        }
                        
                        if (state.generatedSubtitles.size > 10) {
                            item {
                                Text(
                                    text = "... and ${state.generatedSubtitles.size - 10} more",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                    
                    // Use subtitles button
                    Button(
                        onClick = {
                            onSubtitlesGenerated(state.generatedSubtitles)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Use These Subtitles")
                    }
                }
            }
        }
    }
    
    // Language selector dialog
    if (showLanguageSelector) {
        LanguageSelectorDialog(
            languages = aiGenerator.getSupportedLanguages(),
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { language ->
                selectedLanguage = language
                showLanguageSelector = false
            },
            onDismiss = { showLanguageSelector = false }
        )
    }
}

@Composable
private fun SubtitleDisplayItem(subtitle: SubtitleEntry) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(subtitle.startTime),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "${(subtitle.confidence * 100).toInt()}%",
                    color = if (subtitle.confidence > 0.8f) Color.Green else Color.Yellow,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            Text(
                text = subtitle.text,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LanguageSelectorDialog(
    languages: List<String>,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Language",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn {
                    items(languages) { language ->
                        TextButton(
                            onClick = { onLanguageSelected(language) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = language,
                                color = if (language == selectedLanguage) 
                                    MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}