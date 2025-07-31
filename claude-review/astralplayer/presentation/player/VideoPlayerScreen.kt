// VideoPlayerScreen.kt
package com.astralplayer.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.astralplayer.features.ai.EnhancedAISubtitleGenerator
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun VideoPlayerScreen(
    player: ExoPlayer,
    subtitleGenerator: EnhancedAISubtitleGenerator
) {
    val context = LocalContext.current
    val subtitleState by subtitleGenerator.state.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ExoPlayer PlayerView
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Enhanced subtitle generation progress indicator
        if (subtitleState.isGenerating) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Service indicator
                    subtitleState.aiService?.let { service ->
                        Text(
                            text = if (subtitleState.fallbackActive) "Fallback Mode" else service.uppercase(),
                            color = if (subtitleState.fallbackActive) Color.Yellow else Color.Green,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    Text(
                        text = if (subtitleState.fallbackActive) "Generating Basic Subtitles..." else "AI Generating Subtitles...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = subtitleState.progress,
                        modifier = Modifier.width(120.dp),
                        color = if (subtitleState.fallbackActive) Color.Yellow else Color.Green
                    )
                    Text(
                        text = "${(subtitleState.progress * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                    
                    // Cost estimate
                    subtitleState.costEstimate?.let { cost ->
                        Text(
                            text = "Est. cost: $cost",
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        
        // Subtitle count indicator
        if (subtitleState.subtitles.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "${subtitleState.subtitles.size} subtitles",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        
        // Error display
        subtitleState.error?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}