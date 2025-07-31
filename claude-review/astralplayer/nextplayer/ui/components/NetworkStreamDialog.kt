package com.astralplayer.nextplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkStreamDialog(
    onStreamSelected: (Uri, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var isValidUrl by remember { mutableStateOf(true) }
    var recentStreams by remember { mutableStateOf(listOf<StreamHistoryItem>()) }
    val focusRequester = remember { FocusRequester() }
    
    // Common streaming URLs for quick access
    val sampleStreams = remember {
        listOf(
            StreamHistoryItem(
                url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                title = "Big Buck Bunny (HLS)",
                format = "HLS"
            ),
            StreamHistoryItem(
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                title = "Big Buck Bunny (MP4)",
                format = "MP4"
            ),
            StreamHistoryItem(
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                title = "Elephants Dream",
                format = "MP4"
            ),
            StreamHistoryItem(
                url = "https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8",
                title = "Bitdash Demo (HLS)",
                format = "HLS"
            )
        )
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    fun validateUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            uri.scheme in listOf("http", "https", "rtsp", "rtmp") && !uri.host.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    fun playStream() {
        if (validateUrl(urlText)) {
            val uri = Uri.parse(urlText)
            val title = titleText.ifEmpty { "Network Stream" }
            onStreamSelected(uri, title)
            onDismiss()
        } else {
            isValidUrl = false
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open Network Stream",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // URL Input
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { 
                        urlText = it
                        isValidUrl = true // Reset validation on change
                    },
                    label = { Text("Stream URL") },
                    placeholder = { Text("https://example.com/video.mp4") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null
                        )
                    },
                    isError = !isValidUrl,
                    supportingText = {
                        if (!isValidUrl) {
                            Text(
                                text = "Please enter a valid URL",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Title Input (Optional)
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Title (Optional)") },
                    placeholder = { Text("My Stream") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Title,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { playStream() }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Supported formats info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Supported formats",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "MP4, MKV, WebM, HLS (m3u8), DASH, RTSP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sample streams
                Text(
                    text = "Sample Streams",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                sampleStreams.forEach { stream ->
                    StreamItem(
                        stream = stream,
                        onClick = {
                            urlText = stream.url
                            titleText = stream.title
                            isValidUrl = true
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { playStream() },
                        enabled = urlText.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamItem(
    stream: StreamHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (stream.format) {
                    "HLS" -> Icons.Default.Stream
                    "DASH" -> Icons.Default.Speed
                    else -> Icons.Default.VideoFile
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stream.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stream.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = stream.format,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

data class StreamHistoryItem(
    val url: String,
    val title: String,
    val format: String,
    val lastPlayed: Long = System.currentTimeMillis()
)