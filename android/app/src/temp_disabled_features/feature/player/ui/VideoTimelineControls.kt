package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToLong

@Composable
fun VideoTimelineControls(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(if (isDragging) (dragPosition * duration).roundToLong() else currentPosition),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = formatTime(duration),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Timeline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val progress = (offset.x / size.width).coerceIn(0f, 1f)
                            dragPosition = progress
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek((dragPosition * duration).roundToLong())
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            val progress = ((change.position.x + dragAmount) / size.width).coerceIn(0f, 1f)
                            dragPosition = progress
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                val progress = if (isDragging) dragPosition else (currentPosition.toFloat() / duration)
                val bufferedProgress = bufferedPosition.toFloat() / duration
                
                drawTimeline(
                    progress = progress,
                    bufferedProgress = bufferedProgress,
                    isDragging = isDragging
                )
            }
        }
    }
}

private fun DrawScope.drawTimeline(
    progress: Float,
    bufferedProgress: Float,
    isDragging: Boolean
) {
    val height = size.height
    val width = size.width
    
    // Background track
    drawLine(
        color = Color.White.copy(alpha = 0.2f),
        start = Offset(0f, height / 2),
        end = Offset(width, height / 2),
        strokeWidth = height,
        cap = StrokeCap.Round
    )
    
    // Buffered progress
    if (bufferedProgress > 0f) {
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, height / 2),
            end = Offset(width * bufferedProgress, height / 2),
            strokeWidth = height,
            cap = StrokeCap.Round
        )
    }
    
    // Progress
    if (progress > 0f) {
        drawLine(
            color = if (isDragging) Color(0xFFFF6B6B) else Color.White,
            start = Offset(0f, height / 2),
            end = Offset(width * progress, height / 2),
            strokeWidth = height,
            cap = StrokeCap.Round
        )
    }
    
    // Scrubber
    val scrubberRadius = if (isDragging) height * 2 else height * 1.5f
    drawCircle(
        color = Color.White,
        radius = scrubberRadius,
        center = Offset(width * progress, height / 2)
    )
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}