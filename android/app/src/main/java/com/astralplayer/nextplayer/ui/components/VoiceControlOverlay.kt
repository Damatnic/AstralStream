package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.feature.voice.*
import com.astralplayer.nextplayer.ui.theme.glassmorphicSurface
import kotlinx.coroutines.delay

/**
 * Voice Control Overlay
 * Shows voice recognition UI and feedback
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VoiceControlOverlay(
    voiceManager: VoiceControlManager,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening by voiceManager.isListening.collectAsState()
    val voiceState by voiceManager.voiceState.collectAsState()
    val recognizedText by voiceManager.recognizedText.collectAsState()
    val commandResult by voiceManager.commandResult.collectAsState()
    
    var showHelp by remember { mutableStateOf(false) }
    var recentCommands by remember { mutableStateOf(listOf<String>()) }
    
    // Auto-dismiss on successful command
    LaunchedEffect(commandResult) {
        commandResult?.let { result ->
            if (result.success) {
                delay(1500)
                onDismiss()
            }
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() }
        )
        
        // Voice control card
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(animationSpec = tween(300)) + fadeIn(),
            exit = scaleOut(animationSpec = tween(300)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .glassmorphicSurface(
                        cornerRadius = 28.dp,
                        glassColor = MaterialTheme.colorScheme.surface,
                        glassAlpha = 0.95f,
                        blurRadius = 16.dp,
                        borderWidth = 1.dp,
                        borderBrush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                            )
                        )
                    )
                    .clickable { /* Prevent click through */ },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                VoiceControlHeader(
                    onClose = onDismiss,
                    onHelp = { showHelp = !showHelp }
                )
                
                // Main content
                AnimatedContent(
                    targetState = showHelp,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() with
                        slideOutHorizontally { width -> -width } + fadeOut()
                    }
                ) { helpVisible ->
                    if (helpVisible) {
                        VoiceCommandHelp()
                    } else {
                        VoiceControlMain(
                            voiceState = voiceState,
                            isListening = isListening,
                            recognizedText = recognizedText,
                            commandResult = commandResult,
                            onStartListening = { voiceManager.startListening() },
                            onStopListening = { voiceManager.stopListening() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceControlHeader(
    onClose: () -> Unit,
    onHelp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Voice Control",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onHelp) {
                Icon(
                    Icons.Default.Help,
                    contentDescription = "Help",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun VoiceControlMain(
    voiceState: VoiceState,
    isListening: Boolean,
    recognizedText: String,
    commandResult: CommandResult?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Voice animation
        VoiceAnimation(
            isListening = isListening,
            voiceState = voiceState
        )
        
        // Status text
        AnimatedContent(
            targetState = voiceState,
            transitionSpec = {
                fadeIn() with fadeOut()
            }
        ) { state ->
            Text(
                text = when (state) {
                    VoiceState.IDLE -> "Tap to speak"
                    VoiceState.READY -> "Listening..."
                    VoiceState.LISTENING -> "Listening..."
                    VoiceState.RECORDING -> "Speak now..."
                    VoiceState.PROCESSING -> "Processing..."
                    VoiceState.ERROR -> "Error occurred"
                },
                style = MaterialTheme.typography.titleMedium,
                color = when (state) {
                    VoiceState.ERROR -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
        
        // Recognized text
        AnimatedVisibility(
            visible = recognizedText.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            BubbleCard(
                elevation = 2,
                cornerRadius = 20,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    text = recognizedText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Command result
        AnimatedVisibility(
            visible = commandResult != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            commandResult?.let { result ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (result.success) {
                                Color(0xFF4CAF50).copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (result.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control button
        MicrophoneButton(
            isListening = isListening,
            voiceState = voiceState,
            onClick = {
                if (isListening) {
                    onStopListening()
                } else {
                    onStartListening()
                }
            }
        )
        
        // Quick commands
        Text(
            text = "Quick Commands",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickCommands = listOf(
                "Play", "Pause", "Next", "Volume up", "Fullscreen"
            )
            
            items(quickCommands) { command ->
                QuickCommandChip(
                    command = command,
                    onClick = {
                        // Simulate voice command
                    }
                )
            }
        }
    }
}

@Composable
private fun VoiceAnimation(
    isListening: Boolean,
    voiceState: VoiceState
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val scale = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        )
    )
    
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circles
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale.value * (1f - index * 0.1f))
                    .graphicsLayer {
                        rotationZ = if (index % 2 == 0) rotation.value else -rotation.value
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f * (3 - index)),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        // Center circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    when (voiceState) {
                        VoiceState.ERROR -> MaterialTheme.colorScheme.errorContainer
                        VoiceState.PROCESSING -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Waveform animation
            if (isListening && voiceState == VoiceState.RECORDING) {
                WaveformAnimation()
            } else {
                Icon(
                    when (voiceState) {
                        VoiceState.ERROR -> Icons.Default.MicOff
                        VoiceState.PROCESSING -> Icons.Default.Psychology
                        else -> Icons.Default.Mic
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = when (voiceState) {
                        VoiceState.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

@Composable
private fun WaveformAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    
    Row(
        modifier = Modifier.fillMaxWidth(0.6f),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val height = infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 100,
                        easing = EaseInOut
                    ),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(height.value)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun MicrophoneButton(
    isListening: Boolean,
    voiceState: VoiceState,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    
    FloatingActionButton(
        onClick = onClick,
        containerColor = when {
            isListening -> MaterialTheme.colorScheme.error
            voiceState == VoiceState.ERROR -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.primary
        },
        contentColor = when {
            isListening -> MaterialTheme.colorScheme.onError
            voiceState == VoiceState.ERROR -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onPrimary
        },
        modifier = Modifier.size(64.dp)
    ) {
        AnimatedContent(
            targetState = isListening,
            transitionSpec = {
                scaleIn() + fadeIn() with scaleOut() + fadeOut()
            }
        ) { listening ->
            Icon(
                if (listening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (listening) "Stop listening" else "Start listening",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun QuickCommandChip(
    command: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(command) },
        leadingIcon = {
            Icon(
                Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

@Composable
private fun VoiceCommandHelp() {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val commandCategories = listOf(
            CommandCategory(
                "Playback Control",
                Icons.Default.PlayArrow,
                listOf(
                    "Play / Pause",
                    "Stop",
                    "Next / Previous",
                    "Replay"
                )
            ),
            CommandCategory(
                "Navigation",
                Icons.Default.FastForward,
                listOf(
                    "Forward 10 seconds",
                    "Backward 30 seconds",
                    "Skip to 5 minutes",
                    "Go to 1 hour 20 minutes"
                )
            ),
            CommandCategory(
                "Speed Control",
                Icons.Default.Speed,
                listOf(
                    "Speed up / Slow down",
                    "Normal speed",
                    "Set speed to 1.5"
                )
            ),
            CommandCategory(
                "Volume",
                Icons.Default.VolumeUp,
                listOf(
                    "Volume up / down",
                    "Mute / Unmute",
                    "Set volume to 50"
                )
            ),
            CommandCategory(
                "Display",
                Icons.Default.Fullscreen,
                listOf(
                    "Fullscreen",
                    "Exit fullscreen",
                    "Rotate",
                    "Zoom in / out"
                )
            ),
            CommandCategory(
                "Search & Navigation",
                Icons.Default.Search,
                listOf(
                    "Search for action movies",
                    "Show recent videos",
                    "Find HD videos",
                    "Go home / back"
                )
            ),
            CommandCategory(
                "Subtitles",
                Icons.Default.Subtitles,
                listOf(
                    "Show / Hide subtitles",
                    "Next subtitle",
                    "Subtitle language English"
                )
            ),
            CommandCategory(
                "Special Features",
                Icons.Default.StarBorder,
                listOf(
                    "Bookmark",
                    "Sleep timer 30 minutes",
                    "What's playing",
                    "Add to playlist"
                )
            )
        )
        
        items(commandCategories) { category ->
            CommandCategoryCard(category)
        }
    }
}

@Composable
private fun CommandCategoryCard(
    category: CommandCategory
) {
    BubbleCard(
        elevation = 2,
        cornerRadius = 20
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            category.commands.forEach { command ->
                Row(
                    modifier = Modifier.padding(start = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = command,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Data classes

private data class CommandCategory(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val commands: List<String>
)