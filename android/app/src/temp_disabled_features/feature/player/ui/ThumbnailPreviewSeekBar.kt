package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThumbnailPreviewSeekBar(
    progress: Float,
    bufferedProgress: Float = 0f,
    duration: Long,
    onSeek: (Float) -> Unit,
    onSeekStart: () -> Unit = {},
    onSeekEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }
    var trackWidth by remember { mutableFloatStateOf(0f) }
    var showPreview by remember { mutableStateOf(false) }
    
    LaunchedEffect(progress) {
        if (!isDragging) {
            dragProgress = progress
        }
    }
    
    Box(
        modifier = modifier
            .height(60.dp) // Increased height for thumbnail preview
            .fillMaxWidth()
    ) {
        // Thumbnail preview overlay
        AnimatedVisibility(
            visible = showPreview && isDragging,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (dragProgress * trackWidth - 60).dp) // Center above thumb
        ) {
            ThumbnailPreview(
                progress = dragProgress,
                duration = duration
            )
        }
        
        // Seek bar container
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(40.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            showPreview = true
                            onSeekStart()
                            trackWidth = size.width.toFloat()
                            val newProgress = (offset.x / trackWidth).coerceIn(0f, 1f)
                            dragProgress = newProgress
                        },
                        onDragEnd = {
                            isDragging = false
                            showPreview = false
                            onSeek(dragProgress)
                            onSeekEnd()
                        }
                    ) { change, _ ->
                        val newProgress = (change.position.x / trackWidth).coerceIn(0f, 1f)
                        dragProgress = newProgress
                    }
                }
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Color.White.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp)
                    )
            )
            
            // Buffered progress
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(bufferedProgress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(
                        Color.White.copy(alpha = 0.5f),
                        RoundedCornerShape(2.dp)
                    )
            )
            
            // Active progress
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(if (isDragging) dragProgress else progress)
                    .height(4.dp)
                    .background(
                        Color(0xFF00BCD4),
                        RoundedCornerShape(2.dp)
                    )
            )
            
            // Thumb
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) { 
                val thumbRadius = if (isDragging) 10.dp.toPx() else 8.dp.toPx()
                val currentProgress = if (isDragging) dragProgress else progress
                val thumbX = currentProgress * size.width
                val thumbY = size.height / 2
                
                drawCircle(
                    color = Color(0xFF00BCD4),
                    radius = thumbRadius,
                    center = Offset(thumbX, thumbY)
                )
                
                // Outer ring when dragging
                if (isDragging) {
                    drawCircle(
                        color = Color(0xFF00BCD4).copy(alpha = 0.3f),
                        radius = thumbRadius * 1.5f,
                        center = Offset(thumbX, thumbY)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThumbnailPreview(
    progress: Float,
    duration: Long,
    modifier: Modifier = Modifier
) {
    val previewTime = (progress * duration).toLong()
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Thumbnail placeholder (in real implementation, this would be an actual video frame)
            Box(
                modifier = Modifier
                    .size(120.dp, 68.dp) // 16:9 aspect ratio
                    .background(
                        Color.Gray.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
            ) {
                Text(
                    text = "Preview",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Time display
            Text(
                text = formatTime(previewTime),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Helper function for time formatting
private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}