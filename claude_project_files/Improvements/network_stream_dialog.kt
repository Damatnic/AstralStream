package com.astralplayer.nextplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkStreamDialog(
    onStreamSelected: (Uri, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlText by remember { mutableStateOf(TextFieldValue("")) }
    var titleText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<StreamPreset?>(null) }
    var isValidating by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Network Stream",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Text(
                    text = "Enter a video URL to stream over the network. Supports HTTP, HTTPS, HLS, DASH, and more.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Divider()
                
                // URL Input
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { 
                        urlText = it
                        validationMessage = null
                        selectedPreset = null
                    },
                    label = { Text("Video URL") },
                    placeholder = { Text("https://example.com/video.mp4") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null)
                    },
                    trailingIcon = {
                        Row {
                            // Paste button
                            IconButton(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        urlText = TextFieldValue(clipText)
                                        validationMessage = null
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                            }
                            
                            // Validation button
                            IconButton(
                                onClick = {
                                    if (urlText.text.isNotBlank()) {
                                        validateUrl(urlText.text) { message ->
                                            validationMessage = message
                                        }
                                    }
                                }
                            ) {
                                if (isValidating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Validate")
                                }
                            }
                        }
                    },
                    isError = validationMessage?.startsWith("Error") == true,
                    supportingText = validationMessage?.let { { Text(it) } }
                )
                
                // Title Input
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Title (Optional)") },
                    placeholder = { Text("My Stream") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Title, contentDescription = null)
                    }
                )
                
                // Preset URLs
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(streamPresets.size) { index ->
                        val preset = streamPresets[index]
                        FilterChip(
                            selected = selectedPreset == preset,
                            onClick = {
                                selectedPreset = preset
                                urlText = TextFieldValue(preset.url)
                                titleText = preset.title
                                validationMessage = null
                            },
                            label = { Text(preset.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (preset.type) {
                                        StreamType.HLS -> Icons.Default.Stream
                                        StreamType.DASH -> Icons.Default.DashboardCustomize
                                        StreamType.MP4 -> Icons.Default.Movie
                                        StreamType.LIVE -> Icons.Default.LiveTv
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
                
                // Stream type info
                selectedPreset?.let { preset ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            SelectionContainer {
                                Text(
                                    text = preset.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
                
                // Supported formats info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportedActions,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Supported Formats",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        val supportedFormats = listOf(
                            "MP4, MKV, AVI, MOV, WMV",
                            "HLS (.m3u8) - Adaptive streaming",
                            "DASH (.mpd) - Dynamic streaming",
                            "RTMP, RTSP - Live streams",
                            "WebM, FLV, 3GP formats"
                        )
                        
                        supportedFormats.forEach { format ->
                            Text(
                                text = "• $format",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val url = urlText.text.trim()
                            val title = titleText.ifBlank { 
                                selectedPreset?.title ?: extractTitleFromUrl(url)
                            }
                            
                            if (url.isNotBlank()) {
                                onStreamSelected(Uri.parse(url), title)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = urlText.text.isNotBlank() && validationMessage?.startsWith("Error") != true
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stream")
                    }
                }
            }
        }
    }
}

private fun validateUrl(url: String, onResult: (String) -> Unit) {
    try {
        val parsedUrl = URL(url)
        when (parsedUrl.protocol.lowercase()) {
            "http", "https" -> {
                onResult("✓ Valid URL - Ready to stream")
            }
            "rtmp", "rtsp" -> {
                onResult("✓ Live stream URL detected")
            }
            else -> {
                onResult("⚠ Unusual protocol - May not be supported")
            }
        }
    } catch (e: Exception) {
        onResult("Error: Invalid URL format")
    }
}

private fun extractTitleFromUrl(url: String): String {
    return try {
        val uri = Uri.parse(url)
        val path = uri.path ?: ""
        val fileName = path.substringAfterLast('/')
        
        if (fileName.isNotEmpty()) {
            fileName.substringBeforeLast('.')
                .replace(Regex("[._-]"), " ")
                .trim()
                .takeIf { it.isNotEmpty() } ?: "Network Stream"
        } else {
            uri.host ?: "Network Stream"
        }
    } catch (e: Exception) {
        "Network Stream"
    }
}

enum class StreamType {
    HLS, DASH, MP4, LIVE
}

data class StreamPreset(
    val name: String,
    val title: String,
    val url: String,
    val description: String,
    val type: StreamType
)

private val streamPresets = listOf(
    StreamPreset(
        name = "Big Buck Bunny",
        title = "Big Buck Bunny (Test)",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        description = "Sample MP4 video for testing direct HTTP streaming",
        type = StreamType.MP4
    ),
    StreamPreset(
        name = "Apple HLS",
        title = "Apple HLS Test Stream",
        url = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
        description = "Apple's official HLS test stream with multiple quality levels",
        type = StreamType.HLS
    ),
    StreamPreset(
        name = "Sintel 4K",
        title = "Sintel 4K",
        url = "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd",
        description = "DASH stream with high quality video content",
        type = StreamType.DASH
    ),
    StreamPreset(
        name = "Elephant Dream",
        title = "Elephant Dream",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        description = "Classic open-source animated movie in MP4 format",
        type = StreamType.MP4
    )
)

@Composable
private fun LazyRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyRowScope.() -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        contentPadding = contentPadding,
        content = content
    )
}