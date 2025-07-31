package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Picture-in-Picture overlay controls
 * Minimal, touch-friendly controls optimized for small PiP window
 */
@Composable
fun PictureInPictureOverlay(
    isInPiPMode: Boolean,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    currentPosition: Long,
    duration: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showExtendedControls: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    
    AnimatedVisibility(
        visible = isInPiPMode,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Auto-hide controls after 3 seconds
            var showControls by remember { mutableStateOf(true) }
            val coroutineScope = rememberCoroutineScope()
            
            LaunchedEffect(isInPiPMode) {
                if (isInPiPMode) {
                    delay(3000)
                    showControls = false
                }
            }
            
            // Tap to show controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        showControls = !showControls
                        if (showControls) {
                            // Auto-hide again after 3 seconds
                            coroutineScope.launch {
                                delay(3000)
                                showControls = false
                            }
                        }
                    }
            )
            
            // Main PiP controls
            AnimatedVisibility(
                visible = showControls,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (isLandscape) {
                    LandscapePiPControls(
                        isPlaying = isPlaying,
                        onPlayPause = onPlayPause,
                        onClose = onClose,
                        showExtendedControls = showExtendedControls
                    )
                } else {
                    PortraitPiPControls(
                        isPlaying = isPlaying,
                        onPlayPause = onPlayPause,
                        onClose = onClose,
                        showExtendedControls = showExtendedControls
                    )
                }
            }
            
            // Progress indicator (always visible)
            PiPProgressIndicator(
                currentPosition = currentPosition,
                duration = duration,
                onSeek = onSeek,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(4.dp)
            )
            
            // Close button (top-right corner)
            AnimatedVisibility(
                visible = showControls,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                PiPCloseButton(
                    onClick = onClose,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

/**
 * Landscape-oriented PiP controls
 */
@Composable
private fun LandscapePiPControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    showExtendedControls: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showExtendedControls) {
            PiPControlButton(
                icon = Icons.Default.Replay10,
                contentDescription = "Rewind 10s",
                onClick = { /* Handle rewind */ }
            )
        }
        
        // Main play/pause button
        PiPPlayPauseButton(
            isPlaying = isPlaying,
            onClick = onPlayPause
        )
        
        if (showExtendedControls) {
            PiPControlButton(
                icon = Icons.Default.Forward10,
                contentDescription = "Forward 10s", 
                onClick = { /* Handle forward */ }
            )
        }
    }
}

/**
 * Portrait-oriented PiP controls
 */
@Composable
private fun PortraitPiPControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    showExtendedControls: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showExtendedControls) {
            PiPControlButton(
                icon = Icons.Default.Forward10,
                contentDescription = "Forward 10s",
                onClick = { /* Handle forward */ }
            )
        }
        
        // Main play/pause button
        PiPPlayPauseButton(
            isPlaying = isPlaying,
            onClick = onPlayPause
        )
        
        if (showExtendedControls) {
            PiPControlButton(
                icon = Icons.Default.Replay10,
                contentDescription = "Rewind 10s",
                onClick = { /* Handle rewind */ }
            )
        }
    }
}

/**
 * Main play/pause button for PiP
 */
@Composable
private fun PiPPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 1.1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "playPauseScale"
    )
    
    Surface(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .clickable { onClick() },
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.7f),
        shadowElevation = 4.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                },
                label = "playPauseIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Generic PiP control button
 */
@Composable
private fun PiPControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(40.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.6f),
        shadowElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Close/exit PiP button
 */
@Composable
private fun PiPCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(32.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = Color.Red.copy(alpha = 0.8f),
        shadowElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close PiP",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Minimal progress indicator for PiP mode
 */
@Composable
private fun PiPProgressIndicator(
    currentPosition: Long,
    duration: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Time indicator (only show if space permits)
        if (duration > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * PiP mode indicator overlay
 */
@Composable
fun PiPModeIndicator(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureInPictureAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Picture-in-Picture",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Helper function for time formatting
private fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = milliseconds / (1000 * 60 * 60)
    
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}