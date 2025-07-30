package com.astralplayer.nextplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Dialog for testing intent handling functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentHandlerDialog(
    onIntentTest: (Uri, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTestType by remember { mutableStateOf(TestType.LOCAL_FILE) }
    var customUrl by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
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
                Text(
                    text = "Test Intent Handler",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Test how the app handles various video intents from browsers and other apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Divider()
                
                // Test type selection
                Text(
                    text = "Select Test Type:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TestType.values().forEach { testType ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTestType == testType,
                                onClick = { selectedTestType = testType }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = testType.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = testType.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Custom URL input for streaming tests
                if (selectedTestType == TestType.CUSTOM_STREAM) {
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("Video URL") },
                        placeholder = { Text("https://example.com/video.mp4") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Video Title") },
                        placeholder = { Text("My Custom Video") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Test scenarios section
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "What this tests:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        val testScenarios = when (selectedTestType) {
                            TestType.LOCAL_FILE -> listOf(
                                "• Content URI handling",
                                "• Local file playback",
                                "• File provider integration",
                                "• Metadata extraction"
                            )
                            TestType.HTTP_STREAM -> listOf(
                                "• HTTP streaming support",
                                "• Progressive download",
                                "• Network error handling",
                                "• Buffering optimization"
                            )
                            TestType.HLS_STREAM -> listOf(
                                "• HLS adaptive streaming",
                                "• M3U8 playlist parsing",
                                "• Quality adaptation",
                                "• Live stream support"
                            )
                            TestType.ADULT_CONTENT -> listOf(
                                "• Enhanced codec support",
                                "• Adult content optimizations",
                                "• Advanced format handling",
                                "• Performance settings"
                            )
                            TestType.CUSTOM_STREAM -> listOf(
                                "• Custom URL handling",
                                "• Browser integration",
                                "• Intent filter matching",
                                "• External app support"
                            )
                        }
                        
                        testScenarios.forEach { scenario ->
                            Text(
                                text = scenario,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            val (testUri, testTitle) = getTestUriAndTitle(selectedTestType, customUrl, customTitle)
                            onIntentTest(testUri, testTitle)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = if (selectedTestType == TestType.CUSTOM_STREAM) {
                            customUrl.isNotBlank() && customTitle.isNotBlank()
                        } else {
                            true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Intent")
                    }
                }
            }
        }
    }
}

enum class TestType(
    val displayName: String,
    val description: String
) {
    LOCAL_FILE(
        "Local Video File",
        "Test with a sample local video file"
    ),
    HTTP_STREAM(
        "HTTP Video Stream",
        "Test with a direct HTTP video URL"
    ),
    HLS_STREAM(
        "HLS Stream",
        "Test with an HLS adaptive stream"
    ),
    ADULT_CONTENT(
        "Adult Content",
        "Test with adult content optimizations"
    ),
    CUSTOM_STREAM(
        "Custom URL",
        "Test with your own video URL"
    )
}

private fun getTestUriAndTitle(testType: TestType, customUrl: String, customTitle: String): Pair<Uri, String> {
    return when (testType) {
        TestType.LOCAL_FILE -> {
            // Create a dummy content URI for testing
            val testUri = Uri.parse("content://media/external/video/media/1")
            Pair(testUri, "Sample Local Video")
        }
        TestType.HTTP_STREAM -> {
            val testUri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
            Pair(testUri, "Big Buck Bunny (Test)")
        }
        TestType.HLS_STREAM -> {
            val testUri = Uri.parse("https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8")
            Pair(testUri, "Apple HLS Test Stream")
        }
        TestType.ADULT_CONTENT -> {
            // Use a generic MP4 but mark as adult content
            val testUri = Uri.parse("https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4")
            Pair(testUri, "Adult Content Test Video")
        }
        TestType.CUSTOM_STREAM -> {
            val testUri = Uri.parse(customUrl)
            Pair(testUri, customTitle)
        }
    }
}