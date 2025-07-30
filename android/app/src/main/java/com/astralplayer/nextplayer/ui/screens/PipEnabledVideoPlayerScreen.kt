package com.astralplayer.nextplayer.ui.screens

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.astralplayer.nextplayer.feature.pip.BubblePipManager
import com.astralplayer.nextplayer.ui.components.*
import com.astralplayer.nextplayer.viewmodel.EnhancedPlayerViewModel

/**
 * PiP-Enabled Video Player Screen
 * Seamlessly transitions between fullscreen and Picture-in-Picture mode
 */
@Composable
fun PipEnabledVideoPlayerScreen(
    viewModel: EnhancedPlayerViewModel,
    videoUri: String,
    videoTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // PiP Manager
    val pipManager = remember {
        activity?.let { BubblePipManager(it, viewModel.getExoPlayer()) }
    }
    
    // State
    var isInPipMode by remember { mutableStateOf(false) }
    var showPipHint by remember { mutableStateOf(false) }
    var pipAspectRatio by remember { mutableStateOf(BubblePipManager.RATIO_16_9) }
    
    // Lifecycle observer for PiP mode changes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Check if entering PiP mode
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        isInPipMode = activity?.isInPictureInPictureMode ?: false
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Update PiP state
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        isInPipMode = activity?.isInPictureInPictureMode ?: false
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            pipManager?.release()
        }
    }
    
    // Handle configuration changes
    LaunchedEffect(configuration) {
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            pipAspectRatio = BubblePipManager.RATIO_16_9
        } else {
            pipAspectRatio = BubblePipManager.RATIO_4_3
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!isInPipMode) {
            // Full screen player
            AdvancedVideoPlayerScreen(
                viewModel = viewModel,
                videoUri = videoUri,
                videoTitle = videoTitle,
                playlistRepository = TODO(), // Pass actual repository
                settingsRepository = TODO(), // Pass actual repository
                onBack = onBack,
                isInPipMode = false,
                onEnterPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        pipManager?.enterPipMode(
                            aspectRatio = pipAspectRatio,
                            autoEnterEnabled = true
                        )
                        showPipHint = false
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        @Suppress("DEPRECATION")
                        activity?.enterPictureInPictureMode()
                    } else {
                        // Show floating player for older devices
                        showPipHint = true
                    }
                }
            )
            
            // PiP hint overlay
            AnimatedVisibility(
                visible = showPipHint,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                PipHintCard(
                    onDismiss = { showPipHint = false },
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            // Simplified PiP mode UI
            PipModePlayer(
                viewModel = viewModel,
                videoTitle = videoTitle,
                pipManager = pipManager,
                onExitPip = {
                    // User will tap to return to fullscreen
                }
            )
        }
    }
}

@Composable
private fun PipModePlayer(
    viewModel: EnhancedPlayerViewModel,
    videoTitle: String,
    pipManager: BubblePipManager?,
    onExitPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOverlay by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video player view (simplified for PiP)
        AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    useController = false
                    player = viewModel.getExoPlayer()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Tap to show overlay hint
        AnimatedVisibility(
            visible = showOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            BubbleCard(
                onClick = onExitPip,
                elevation = 8,
                cornerRadius = 16,
                containerColor = Color.Black.copy(alpha = 0.8f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Tap to return to fullscreen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
        
        // Show overlay briefly on enter
        LaunchedEffect(Unit) {
            showOverlay = true
            kotlinx.coroutines.delay(2000)
            showOverlay = false
        }
    }
}

@Composable
private fun PipHintCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BubbleCard(
        modifier = modifier,
        elevation = 12,
        cornerRadius = 20,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PictureInPicture,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Picture-in-Picture",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Your device doesn't support PiP. Use the home button to minimize.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Advanced PiP Controls for Android 12+
 */
@Composable
fun AdvancedPipControls(
    pipManager: BubblePipManager,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Column(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Picture-in-Picture Settings",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Aspect ratio options
                BubbleChip(
                    text = "16:9",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            pipManager.updatePipParams(aspectRatio = Rational(16, 9))
                        }
                    },
                    selected = false
                )
                
                BubbleChip(
                    text = "4:3",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            pipManager.updatePipParams(aspectRatio = Rational(4, 3))
                        }
                    },
                    selected = false
                )
                
                BubbleChip(
                    text = "1:1",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            pipManager.updatePipParams(aspectRatio = Rational(1, 1))
                        }
                    },
                    selected = false
                )
                
                BubbleChip(
                    text = "21:9",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            pipManager.updatePipParams(aspectRatio = Rational(21, 9))
                        }
                    },
                    selected = false
                )
            }
            
            // Auto-PiP toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-enter PiP on back",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Switch(
                    checked = true,
                    onCheckedChange = { /* Handle auto-PiP toggle */ }
                )
            }
        }
    }
}