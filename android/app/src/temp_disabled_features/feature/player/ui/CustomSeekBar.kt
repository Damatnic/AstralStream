package com.astralplayer.nextplayer.feature.player.ui

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomSeekBar(
    progress: Float,
    bufferedProgress: Float = 0f,
    onSeek: (Float) -> Unit,
    onSeekStart: () -> Unit = {},
    onSeekEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }
    var trackWidth by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(progress) {
        if (!isDragging) {
            dragProgress = progress
        }
    }
    
    Box(
        modifier = modifier
            .height(40.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        onSeekStart()
                        trackWidth = size.width.toFloat()
                        val newProgress = (offset.x / trackWidth).coerceIn(0f, 1f)
                        dragProgress = newProgress
                    },
                    onDragEnd = {
                        isDragging = false
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
            modifier = Modifier
                .fillMaxSize()
        ) { 
            val thumbRadius = 8.dp.toPx()
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