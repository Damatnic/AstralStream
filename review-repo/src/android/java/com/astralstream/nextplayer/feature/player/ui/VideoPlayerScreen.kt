package com.astralstream.nextplayer.feature.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import com.astralstream.nextplayer.feature.player.gestures.AdvancedGestureManager
import com.astralstream.nextplayer.viewmodels.VideoPlayerViewModel

@Composable
fun VideoPlayerScreen(
    videoUri: String,
    videoTitle: String,
    onNavigateBack: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    
    DisposableEffect(videoUri) {
        viewModel.initializePlayer(videoUri, videoTitle)
        
        onDispose {
            viewModel.releasePlayer()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    viewModel.setupPlayerView(this)
                    playerView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Gesture overlay
        playerView?.let { pv ->
            GestureOverlay(
                playerView = pv,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Back button overlay
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun GestureOverlay(
    playerView: PlayerView,
    modifier: Modifier = Modifier,
    gestureManager: AdvancedGestureManager = hiltViewModel()
) {
    BoxWithConstraints(modifier = modifier) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        gestureManager.handleGesture(
                            x = offset.x,
                            y = offset.y,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            onAction = { action ->
                                when (action) {
                                    AdvancedGestureManager.GestureAction.PLAY_PAUSE -> {
                                        playerView.player?.let { player ->
                                            if (player.isPlaying) {
                                                player.pause()
                                            } else {
                                                player.play()
                                            }
                                        }
                                    }
                                    AdvancedGestureManager.GestureAction.SEEK_FORWARD -> {
                                        playerView.player?.seekToDefaultPosition()
                                    }
                                    AdvancedGestureManager.GestureAction.SEEK_BACKWARD -> {
                                        playerView.player?.let { player ->
                                            val currentPosition = player.currentPosition
                                            player.seekTo(maxOf(0, currentPosition - 10000))
                                        }
                                    }
                                    AdvancedGestureManager.GestureAction.FULLSCREEN -> {
                                        // Toggle fullscreen mode
                                        playerView.useController = !playerView.useController
                                    }
                                    else -> {
                                        // Handle other gesture actions
                                    }
                                }
                            }
                        )
                    }
                }
        )
    }
}