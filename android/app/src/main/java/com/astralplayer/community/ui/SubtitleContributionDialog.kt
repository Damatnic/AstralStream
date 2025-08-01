package com.astralplayer.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astralplayer.community.viewmodel.SubtitleContributionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleContributionDialog(
    videoHash: String,
    videoTitle: String,
    videoDuration: Long,
    onDismiss: () -> Unit,
    onSuccess: (subtitleId: String) -> Unit,
    viewModel: SubtitleContributionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    LaunchedEffect(videoHash) {
        viewModel.initializeForVideo(videoHash, videoTitle, videoDuration)
    }
    
    LaunchedEffect(uiState.contributionResult) {
        uiState.contributionResult?.let { result ->
            if (result.isSuccess) {
                onSuccess(result.getOrNull()?.subtitleId ?: "")
                onDismiss()
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contribute Subtitles",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Divider()
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Video info
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Video: $videoTitle",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Duration: ${formatDuration(videoDuration)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Language selection
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedLanguage,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Language") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            supportedLanguages.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(language.name) },
                                    onClick = {
                                        viewModel.selectLanguage(language.name, language.code)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Format selection
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.format == "srt",
                            onClick = { viewModel.selectFormat("srt") },
                            label = { Text("SRT") }
                        )
                        FilterChip(
                            selected = uiState.format == "vtt",
                            onClick = { viewModel.selectFormat("vtt") },
                            label = { Text("WebVTT") }
                        )
                        FilterChip(
                            selected = uiState.format == "ass",
                            onClick = { viewModel.selectFormat("ass") },
                            label = { Text("ASS/SSA") }
                        )
                    }
                    
                    // Subtitle content
                    OutlinedTextField(
                        value = uiState.content,
                        onValueChange = viewModel::updateContent,
                        label = { Text("Subtitle Content") },
                        placeholder = { Text("Paste or type subtitle content here...") },
                        minLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Description
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text("Description (optional)") },
                        placeholder = { Text("Describe your subtitle contribution...") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Confidence slider
                    Column {
                        Text(
                            text = "Confidence Level: ${(uiState.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = uiState.confidence,
                            onValueChange = viewModel::updateConfidence,
                            valueRange = 0.5f..1f,
                            steps = 9
                        )
                        Text(
                            text = "How confident are you in the accuracy of these subtitles?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Tags
                    OutlinedTextField(
                        value = uiState.tagsInput,
                        onValueChange = viewModel::updateTagsInput,
                        label = { Text("Tags (comma separated)") },
                        placeholder = { Text("e.g., corrected, professional, cleaned") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Quality preview
                    if (uiState.qualityMetrics != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Quality Preview",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                QualityMetricRow("Line Count", "${uiState.qualityMetrics.lineCount}")
                                QualityMetricRow("Avg Line Length", "${uiState.qualityMetrics.avgLineLength.toInt()} chars")
                                QualityMetricRow("Completion", "${uiState.qualityMetrics.completionPercentage.toInt()}%")
                                QualityMetricRow("Quality Score", "${(uiState.qualityMetrics.overallScore * 100).toInt()}%")
                            }
                        }
                    }
                    
                    // Validation errors
                    uiState.validationErrors.forEach { error ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                Divider()
                
                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = viewModel::contribute,
                        enabled = uiState.canContribute && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Contribute")
                        }
                    }
                }
            }
        }
    }
    
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun QualityMetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else -> String.format("%d:%02d", minutes, seconds % 60)
    }
}

private data class Language(val name: String, val code: String)

private val supportedLanguages = listOf(
    Language("English", "en"),
    Language("Spanish", "es"),
    Language("French", "fr"),
    Language("German", "de"),
    Language("Italian", "it"),
    Language("Portuguese", "pt"),
    Language("Russian", "ru"),
    Language("Japanese", "ja"),
    Language("Korean", "ko"),
    Language("Chinese (Simplified)", "zh-CN"),
    Language("Chinese (Traditional)", "zh-TW"),
    Language("Arabic", "ar"),
    Language("Hindi", "hi"),
    Language("Dutch", "nl"),
    Language("Swedish", "sv"),
    Language("Polish", "pl"),
    Language("Turkish", "tr"),
    Language("Greek", "el"),
    Language("Hebrew", "he"),
    Language("Thai", "th")
)