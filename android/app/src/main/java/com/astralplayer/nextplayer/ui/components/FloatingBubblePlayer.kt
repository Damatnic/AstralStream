package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.media3.ui.PlayerView
import com.astralplayer.nextplayer.viewmodel.EnhancedPlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Floating Bubble Video Player Component
 * Provides a draggable, resizable mini-player with bubble UI controls
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FloatingBubblePlayer(
    viewModel: EnhancedPlayerViewModel,
    videoTitle: String,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    initialPosition: Offset = Offset.Zero
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Player state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(false) }
    
    // UI state
    var playerSize by remember { mutableStateOf(BubblePlayerSize.NORMAL) }
    var position by remember { mutableStateOf(initialPosition) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }
    
    // Update playback state
    LaunchedEffect(Unit) {
        // Simple state simulation for now
        isPlaying = viewModel.getExoPlayer().isPlaying
        currentPosition = viewModel.getExoPlayer().currentPosition
        duration = viewModel.getExoPlayer().duration
    }
    
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(position.x.roundToInt(), position.y.roundToInt()),
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = modifier
                .size(playerSize.width, playerSize.height)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { 
                            isDragging = false
                            // Snap to edge if needed
                            position = snapToEdge(position, playerSize, configuration)
                        },
                        onDrag = { _, dragAmount ->
                            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
                            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
                            val playerWidthPx = with(density) { playerSize.width.toPx() }
                            val playerHeightPx = with(density) { playerSize.height.toPx() }
                            
                            val newX = (position.x + dragAmount.x).coerceIn(
                                0f,
                                screenWidthPx - playerWidthPx
                            )
                            val newY = (position.y + dragAmount.y).coerceIn(
                                0f,
                                screenHeightPx - playerHeightPx
                            )
                            position = Offset(newX, newY)
                        }
                    )
                }
        ) {
            AnimatedContent(
                targetState = playerSize,
                transitionSpec = {
                    scaleIn(animationSpec = tween(300)) with
                    scaleOut(animationSpec = tween(300))
                }
            ) { size ->
                when (size) {
                    BubblePlayerSize.MINI -> MiniFloatingPlayer(
                        isPlaying = isPlaying,
                        onPlayPause = {
                            isPlaying = !isPlaying
                            if (isPlaying) viewModel.play() else viewModel.pause()
                        },
                        onExpand = { playerSize = BubblePlayerSize.NORMAL },
                        isDragging = isDragging
                    )
                    
                    BubblePlayerSize.NORMAL -> NormalFloatingPlayer(
                        viewModel = viewModel,
                        videoTitle = videoTitle,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        showControls = showControls,
                        onToggleControls = { showControls = !showControls },
                        onPlayPause = {
                            isPlaying = !isPlaying
                            if (isPlaying) viewModel.play() else viewModel.pause()
                        },
                        onMinimize = { playerSize = BubblePlayerSize.MINI },
                        onExpand = onExpand,
                        onClose = onClose,
                        isDragging = isDragging
                    )
                    
                    BubblePlayerSize.EXPANDED -> ExpandedFloatingPlayer(
                        viewModel = viewModel,
                        videoTitle = videoTitle,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        onPlayPause = {
                            isPlaying = !isPlaying
                            if (isPlaying) viewModel.play() else viewModel.pause()
                        },
                        onShrink = { playerSize = BubblePlayerSize.NORMAL },
                        onExpand = onExpand,
                        onClose = onClose,
                        isDragging = isDragging
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MiniFloatingPlayer(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Box(
        modifier = modifier
            .size(64.dp)
            .scale(scale)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated play/pause icon
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                scaleIn(animationSpec = tween(200)) with
                scaleOut(animationSpec = tween(200))
            }
        ) { playing ->
            BubbleIconButton(
                onClick = onPlayPause,
                icon = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                size = 48,
                iconSize = 24,
                containerColor = Color.Transparent,
                contentColor = Color.White
            )
        }
        
        // Expand button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onExpand,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.OpenInFull,
                    contentDescription = "Expand",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun NormalFloatingPlayer(
    viewModel: EnhancedPlayerViewModel,
    videoTitle: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onPlayPause: () -> Unit,
    onMinimize: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    BubbleCard(
        modifier = modifier
            .scale(scale)
            .size(width = 240.dp, height = 180.dp),
        onClick = onToggleControls,
        elevation = 16,
        cornerRadius = 20,
        containerColor = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Video Surface
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        player = viewModel.getExoPlayer()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            
            // Top controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = videoTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    BubbleIconButton(
                        onClick = onMinimize,
                        icon = Icons.Default.RemoveCircleOutline,
                        size = 24,
                        iconSize = 14,
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                    
                    BubbleIconButton(
                        onClick = onClose,
                        icon = Icons.Default.Close,
                        size = 24,
                        iconSize = 14,
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                }
            }
            
            // Center controls
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BubbleIconButton(
                        onClick = { viewModel.seekTo(currentPosition - 10000) },
                        icon = Icons.Default.Replay10,
                        size = 36,
                        iconSize = 18,
                        containerColor = Color.Black.copy(alpha = 0.7f),
                        contentColor = Color.White
                    )
                    
                    BubbleIconButton(
                        onClick = onPlayPause,
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        size = 48,
                        iconSize = 24,
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        contentColor = Color.White
                    )
                    
                    BubbleIconButton(
                        onClick = { viewModel.seekTo(currentPosition + 10000) },
                        icon = Icons.Default.Forward10,
                        size = 36,
                        iconSize = 18,
                        containerColor = Color.Black.copy(alpha = 0.7f),
                        contentColor = Color.White
                    )
                }
            }
            
            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Progress bar
                LinearProgressIndicator(
                    progress = { if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                    
                    BubbleIconButton(
                        onClick = onExpand,
                        icon = Icons.Default.Fullscreen,
                        size = 20,
                        iconSize = 12,
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                    
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedFloatingPlayer(
    viewModel: EnhancedPlayerViewModel,
    videoTitle: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onShrink: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    // Similar to NormalFloatingPlayer but with size 320x240
    NormalFloatingPlayer(
        viewModel = viewModel,
        videoTitle = videoTitle,
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        showControls = true,
        onToggleControls = {},
        onPlayPause = onPlayPause,
        onMinimize = onShrink,
        onExpand = onExpand,
        onClose = onClose,
        isDragging = isDragging,
        modifier = modifier.size(width = 320.dp, height = 240.dp)
    )
}

// Bubble player sizes
private enum class BubblePlayerSize(val width: dp, val height: dp) {
    MINI(64.dp, 64.dp),
    NORMAL(240.dp, 180.dp),
    EXPANDED(320.dp, 240.dp)
}

// Utility functions
private fun snapToEdge(
    position: Offset,
    playerSize: BubblePlayerSize,
    configuration: android.content.res.Configuration
): Offset {
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val playerWidth = playerSize.width
    val playerHeight = playerSize.height
    
    // Determine which edge is closest
    val leftDistance = position.x
    val rightDistance = screenWidth.value - position.x - playerWidth.value
    val topDistance = position.y
    val bottomDistance = screenHeight.value - position.y - playerHeight.value
    
    val minHorizontal = minOf(leftDistance, rightDistance)
    val minVertical = minOf(topDistance, bottomDistance)
    
    return when {
        minHorizontal < minVertical -> {
            // Snap to left or right edge
            if (leftDistance < rightDistance) {
                Offset(0f, position.y)
            } else {
                Offset(screenWidth.value - playerWidth.value, position.y)
            }
        }
        else -> {
            // Snap to top or bottom edge
            if (topDistance < bottomDistance) {
                Offset(position.x, 0f)
            } else {
                Offset(position.x, screenHeight.value - playerHeight.value)
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}