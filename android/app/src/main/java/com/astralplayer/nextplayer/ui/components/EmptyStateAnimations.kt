package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AnimatedEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300) // Small delay for better UX
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(800, easing = EaseOutCubic)
        ) + slideInVertically(
            animationSpec = tween(800, easing = EaseOutCubic),
            initialOffsetY = { it / 4 }
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PulsingIcon(
                icon = icon,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedText(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                delay = 200
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AnimatedText(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                delay = 400
            )
            
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(32.dp))
                
                AnimatedButton(
                    text = actionText,
                    onClick = onActionClick,
                    delay = 600
                )
            }
        }
    }
}

@Composable
private fun PulsingIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier
            .scale(scale)
            .alpha(alpha),
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    )
}

@Composable
private fun AnimatedText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    textAlign: TextAlign = TextAlign.Start,
    delay: Long = 0,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(delay)
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(600, easing = EaseOutCubic)
        ) + slideInVertically(
            animationSpec = tween(600, easing = EaseOutCubic),
            initialOffsetY = { it / 3 }
        )
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            textAlign = textAlign,
            modifier = modifier
        )
    }
}

@Composable
private fun AnimatedButton(
    text: String,
    onClick: () -> Unit,
    delay: Long = 0,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(delay)
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(600, easing = EaseOutCubic)
        ) + scaleIn(
            animationSpec = tween(600, easing = EaseOutCubic),
            initialScale = 0.8f
        )
    ) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text)
        }
    }
}

// Specialized empty states for different screens

@Composable
fun NoVideosFoundState(
    onOpenFileManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedEmptyState(
        icon = Icons.Default.VideoLibrary,
        title = "No videos found",
        description = "We couldn't find any videos on your device. Make sure you have granted storage permissions and have video files in your gallery.",
        actionText = "Browse Files",
        onActionClick = onOpenFileManager,
        modifier = modifier
    )
}

@Composable
fun NoPlaylistsState(
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedEmptyState(
        icon = Icons.Default.PlaylistAdd,
        title = "No playlists yet",
        description = "Create your first playlist to organize your favorite videos. You can add videos from your library or recent files.",
        actionText = "Create Playlist",
        onActionClick = onCreatePlaylist,
        modifier = modifier
    )
}

@Composable
fun NoRecentFilesState(
    onPlayTestVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedEmptyState(
        icon = Icons.Default.History,
        title = "No recent videos",
        description = "Videos you play will appear here for quick access. Start watching videos to build your recent files list.",
        actionText = "Play Test Video",
        onActionClick = onPlayTestVideo,
        modifier = modifier
    )
}

@Composable
fun NoSearchResultsState(
    query: String,
    modifier: Modifier = Modifier
) {
    AnimatedEmptyState(
        icon = Icons.Default.SearchOff,
        title = "No results found",
        description = "We couldn't find any videos, playlists, or files matching \"$query\". Try a different search term or check your spelling.",
        modifier = modifier
    )
}

@Composable
fun NoSubtitlesState(
    modifier: Modifier = Modifier
) {
    AnimatedEmptyState(
        icon = Icons.Default.Subtitles,
        title = "No subtitles available",
        description = "No subtitle tracks were found for this video. You can manually add subtitle files or enable auto-generated captions if available.",
        modifier = modifier
    )
}

@Composable
fun LoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400))
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PulsingProgressIndicator()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PulsingProgressIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    CircularProgressIndicator(
        modifier = Modifier.scale(scale),
        color = MaterialTheme.colorScheme.primary
    )
}

// Connection/Network error states

@Composable
fun NetworkErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedEmptyState(
        icon = Icons.Default.WifiOff,
        title = "Connection problem",
        description = "Check your internet connection and try again. Make sure you're connected to Wi-Fi or mobile data.",
        actionText = "Retry",
        onActionClick = onRetry,
        modifier = modifier
    )
}

@Composable
fun PermissionDeniedState(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedEmptyState(
        icon = Icons.Default.Lock,
        title = "Permission required",
        description = "We need storage permission to access your videos. Please grant permission to continue using the app.",
        actionText = "Grant Permission",
        onActionClick = onGrantPermission,
        modifier = modifier
    )
}

// Floating action button with animation

@Composable
fun AnimatedFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(800) // Delay to show after content loads
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialOffsetY = { it * 2 }
        ) + fadeIn(
            animationSpec = tween(300)
        ),
        exit = slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { it * 2 }
        ) + fadeOut(
            animationSpec = tween(200)
        )
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = containerColor
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
        }
    }
}

// Content transition animations

@Composable
fun ContentTransition(
    targetState: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = targetState,
        enter = fadeIn(
            animationSpec = tween(400, easing = EaseOutCubic)
        ) + slideInVertically(
            animationSpec = tween(400, easing = EaseOutCubic),
            initialOffsetY = { it / 10 }
        ),
        exit = fadeOut(
            animationSpec = tween(200)
        ) + slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { -it / 10 }
        ),
        content = content
    )
}