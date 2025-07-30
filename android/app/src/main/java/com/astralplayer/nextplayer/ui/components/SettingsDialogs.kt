package com.astralplayer.nextplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

// Aspect Ratio Selection Dialog
@Composable
fun AspectRatioSelectorDialog(
    currentAspectRatio: String,
    onAspectRatioSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aspectRatios = listOf(
        AspectRatioOption("fit", "Fit to screen", "Scales video to fit screen without cropping"),
        AspectRatioOption("fill", "Fill screen", "Fills entire screen, may crop video"),
        AspectRatioOption("16:9", "16:9", "Standard widescreen format"),
        AspectRatioOption("4:3", "4:3", "Traditional TV format"),
        AspectRatioOption("21:9", "21:9", "Ultra-wide cinematic format"),
        AspectRatioOption("1:1", "1:1", "Square format"),
        AspectRatioOption("auto", "Auto", "Use video's original aspect ratio")
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Aspect Ratio") },
        text = {
            LazyColumn {
                items(aspectRatios) { option ->
                    AspectRatioItem(
                        option = option,
                        isSelected = option.value == currentAspectRatio,
                        onClick = {
                            onAspectRatioSelected(option.value)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun AspectRatioItem(
    option: AspectRatioOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

data class AspectRatioOption(
    val value: String,
    val displayName: String,
    val description: String
)

// Language Selection Dialog
@Composable
fun LanguageSelectorDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = listOf(
        LanguageOption("en", "English", "English"),
        LanguageOption("es", "Spanish", "Español"),
        LanguageOption("fr", "French", "Français"),
        LanguageOption("de", "German", "Deutsch"),
        LanguageOption("it", "Italian", "Italiano"),
        LanguageOption("pt", "Portuguese", "Português"),
        LanguageOption("ru", "Russian", "Русский"),
        LanguageOption("ja", "Japanese", "日本語"),
        LanguageOption("ko", "Korean", "한국어"),
        LanguageOption("zh", "Chinese", "中文"),
        LanguageOption("ar", "Arabic", "العربية"),
        LanguageOption("hi", "Hindi", "हिन्दी"),
        LanguageOption("auto", "Auto-detect", "Detect from video")
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language") },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp)
            ) {
                items(languages) { language ->
                    LanguageItem(
                        language = language,
                        isSelected = language.code == currentLanguage,
                        onClick = {
                            onLanguageSelected(language.code)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun LanguageItem(
    language: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = language.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

data class LanguageOption(
    val code: String,
    val displayName: String,
    val nativeName: String
)

// Quality Selection Dialog
@Composable
fun QualitySelectorDialog(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qualities = listOf(
        QualityOption("auto", "Auto", "Automatic based on connection", Icons.Default.AutoMode),
        QualityOption("2160p", "2160p (4K)", "Ultra High Definition", Icons.Default.HighQuality),
        QualityOption("1440p", "1440p (2K)", "Quad High Definition", Icons.Default.HighQuality),
        QualityOption("1080p", "1080p (HD)", "Full High Definition", Icons.Default.Hd),
        QualityOption("720p", "720p", "High Definition", Icons.Default.Hd),
        QualityOption("480p", "480p", "Standard Definition", Icons.Default.Sd),
        QualityOption("360p", "360p", "Low Definition", Icons.Default.Sd),
        QualityOption("240p", "240p", "Very Low (data saver)", Icons.Default.DataSaverOff)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Video Quality") },
        text = {
            LazyColumn {
                items(qualities) { quality ->
                    QualityItem(
                        quality = quality,
                        isSelected = quality.value == currentQuality,
                        onClick = {
                            onQualitySelected(quality.value)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun QualityItem(
    quality: QualityOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = quality.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = quality.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = quality.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

data class QualityOption(
    val value: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector
)

// Clear Data Confirmation Dialog
@Composable
fun ClearDataConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Clear All Data")
        },
        text = {
            Column {
                Text("This will permanently delete:")
                Spacer(modifier = Modifier.height(8.dp))
                Text("• All playlists and recent files")
                Text("• Video watching history")
                Text("• Downloaded subtitles")
                Text("• App preferences and settings")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This action cannot be undone.",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

// Subtitle Appearance Settings Dialog  
@Composable
fun SubtitleAppearanceDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepository = remember { 
        com.astralplayer.nextplayer.data.repository.SettingsRepositoryImpl(context)
    }
    val coroutineScope = rememberCoroutineScope()
    
    var subtitleFontSize by remember { mutableStateOf(16f) }
    var subtitleBackground by remember { mutableStateOf(true) }
    var subtitlePosition by remember { mutableStateOf(0.8f) }
    
    LaunchedEffect(Unit) {
        settingsRepository.getSubtitleFontSize().collect { size ->
            subtitleFontSize = size
        }
    }
    
    LaunchedEffect(Unit) {
        settingsRepository.getSubtitleBackground().collect { enabled ->
            subtitleBackground = enabled
        }
    }
    
    LaunchedEffect(Unit) {
        settingsRepository.getSubtitlePosition().collect { position ->
            subtitlePosition = when(position) {
                "top" -> 0.2f
                "middle" -> 0.5f
                "bottom" -> 0.8f
                else -> 0.8f
            }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Subtitle Appearance",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Font Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Font Size")
                    Row {
                        FilledTonalButton(onClick = { 
                            if (subtitleFontSize > 8f) {
                                subtitleFontSize -= 2f
                                coroutineScope.launch {
                                    settingsRepository.setSubtitleFontSize(subtitleFontSize)
                                }
                            }
                        }) {
                            Text("A-")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${subtitleFontSize.toInt()}sp",
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        FilledTonalButton(onClick = { 
                            if (subtitleFontSize < 32f) {
                                subtitleFontSize += 2f
                                coroutineScope.launch {
                                    settingsRepository.setSubtitleFontSize(subtitleFontSize)
                                }
                            }
                        }) {
                            Text("A+")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Background
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Background")
                    Switch(
                        checked = subtitleBackground,
                        onCheckedChange = { enabled ->
                            subtitleBackground = enabled
                            coroutineScope.launch {
                                settingsRepository.setSubtitleBackground(enabled)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Position
                Text("Position")
                Slider(
                    value = subtitlePosition,
                    onValueChange = { value ->
                        subtitlePosition = value
                        coroutineScope.launch {
                            val position = when {
                                value < 0.33f -> "top"
                                value < 0.66f -> "middle"
                                else -> "bottom"
                            }
                            settingsRepository.setSubtitlePosition(position)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    steps = 2
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}