package com.astralplayer.nextplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch

data class AudioTrack(
    val id: String,
    val label: String,
    val language: String?,
    val mimeType: String?,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val channelCount: Int = 0,
    val codecName: String? = null,
    val isDefault: Boolean = false
) {
    val displayName: String
        get() = when {
            label.isNotBlank() && label != "und" -> label
            !language.isNullOrBlank() && language != "und" -> {
                getLanguageDisplayName(language)
            }
            else -> "Audio Track ${id.takeLast(2)}"
        }
    
    val detailsText: String
        get() = buildString {
            if (!language.isNullOrBlank() && language != "und" && language != label) {
                append(language.uppercase())
            }
            if (channelCount > 0) {
                if (isNotEmpty()) append(" • ")
                append(getChannelDescription(channelCount))
            }
            if (bitrate > 0) {
                if (isNotEmpty()) append(" • ")
                append("${bitrate / 1000}kbps")
            }
            if (!codecName.isNullOrBlank()) {
                if (isNotEmpty()) append(" • ")
                append(codecName)
            }
        }
}

@Composable
fun AudioTrackSelectionDialog(
    availableAudioTracks: List<AudioTrack>,
    currentTrackId: String?,
    onTrackSelected: (AudioTrack?) -> Unit,
    onDismiss: () -> Unit,
    onShowAudioSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepository = remember { 
        com.astralplayer.nextplayer.data.repository.SettingsRepositoryImpl(context)
    }
    val coroutineScope = rememberCoroutineScope()
    var audioNormalizationEnabled by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        settingsRepository.getAudioNormalizationEnabled().collect { enabled ->
            audioNormalizationEnabled = enabled
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Audio Track")
                IconButton(onClick = { 
                    onShowAudioSettings()
                }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Audio settings"
                    )
                }
            }
        },
        text = {
            if (availableAudioTracks.isEmpty()) {
                EmptyAudioTracksMessage()
            } else {
                LazyColumn {
                    // Auto/Default option
                    item {
                        AudioTrackItem(
                            track = null,
                            isSelected = currentTrackId == null,
                            onClick = { 
                                onTrackSelected(null)
                                onDismiss()
                            }
                        )
                    }
                    
                    // Available audio tracks
                    items(availableAudioTracks) { track ->
                        AudioTrackItem(
                            track = track,
                            isSelected = track.id == currentTrackId,
                            onClick = { 
                                onTrackSelected(track)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun AudioTrackItem(
    track: AudioTrack?,
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
                    text = track?.displayName ?: "Auto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                val detailsText = track?.detailsText ?: "Automatic selection"
                if (detailsText.isNotEmpty()) {
                    Text(
                        text = detailsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (track?.isDefault == true) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAudioTracksMessage(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VolumeOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Audio Tracks",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "This video does not contain multiple audio tracks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun AudioTrackSettingsSheet(
    onDismiss: () -> Unit,
    isAudioBoostEnabled: Boolean = false,
    onAudioBoostToggle: (Boolean) -> Unit = {},
    audioDelayMs: Int = 0,
    onAudioDelayChange: (Int) -> Unit = {},
    settingsRepository: com.astralplayer.nextplayer.data.repository.SettingsRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var audioNormalizationEnabled by remember { mutableStateOf(false) }
    
    // Load initial normalization state
    LaunchedEffect(Unit) {
        audioNormalizationEnabled = settingsRepository.getAudioNormalizationEnabled().first()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Audio Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Audio boost setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Audio Boost")
                Switch(
                    checked = isAudioBoostEnabled,
                    onCheckedChange = onAudioBoostToggle
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Audio delay setting
            Text(
                text = "Audio Delay",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("−500ms")
                Slider(
                    value = (audioDelayMs + 500f) / 1000f, // Map -500..500 to 0..1
                    onValueChange = { value ->
                        val delayMs = ((value * 1000f) - 500f).toInt()
                        onAudioDelayChange(delayMs)
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                )
                Text("+500ms")
            }
            
            Text(
                text = "${audioDelayMs}ms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Normalize audio setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Normalize Audio")
                    Text(
                        text = "Automatically adjust volume levels",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = audioNormalizationEnabled,
                    onCheckedChange = { enabled ->
                        audioNormalizationEnabled = enabled
                        coroutineScope.launch {
                            settingsRepository.setAudioNormalizationEnabled(enabled)
                        }
                    }
                )
            }
        }
    }
}

private fun getLanguageDisplayName(languageCode: String): String {
    return when (languageCode.lowercase()) {
        "en", "eng" -> "English"
        "es", "spa" -> "Spanish"
        "fr", "fra", "fre" -> "French"
        "de", "deu", "ger" -> "German"
        "it", "ita" -> "Italian"
        "pt", "por" -> "Portuguese"
        "ru", "rus" -> "Russian"
        "ja", "jpn" -> "Japanese"
        "ko", "kor" -> "Korean"
        "zh", "chi", "zho" -> "Chinese"
        "ar", "ara" -> "Arabic"
        "hi", "hin" -> "Hindi"
        "th", "tha" -> "Thai"
        "vi", "vie" -> "Vietnamese"
        "nl", "nld", "dut" -> "Dutch"
        "sv", "swe" -> "Swedish"
        "no", "nor" -> "Norwegian"
        "da", "dan" -> "Danish"
        "fi", "fin" -> "Finnish"
        "pl", "pol" -> "Polish"
        "cs", "ces", "cze" -> "Czech"
        "hu", "hun" -> "Hungarian"
        "ro", "ron", "rum" -> "Romanian"
        "el", "ell", "gre" -> "Greek"
        "he", "heb" -> "Hebrew"
        "tr", "tur" -> "Turkish"
        "uk", "ukr" -> "Ukrainian"
        "bg", "bul" -> "Bulgarian"
        "hr", "hrv" -> "Croatian"
        "sk", "slk", "slo" -> "Slovak"
        "sl", "slv" -> "Slovenian"
        "et", "est" -> "Estonian"
        "lv", "lav" -> "Latvian"
        "lt", "lit" -> "Lithuanian"
        else -> languageCode.uppercase()
    }
}

private fun getChannelDescription(channelCount: Int): String {
    return when (channelCount) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        7 -> "6.1"
        8 -> "7.1"
        else -> "${channelCount}ch"
    }
}