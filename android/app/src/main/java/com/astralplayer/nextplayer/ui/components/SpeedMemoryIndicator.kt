package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpeedMemoryIndicator(
    hasSpeedMemory: Boolean,
    currentSpeed: Float,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible && hasSpeedMemory) 0.9f else 0f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "indicator_alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (hasSpeedMemory) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "indicator_scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !hasSpeedMemory -> Color.Transparent
            currentSpeed > 1f -> Color(0xFF4CAF50).copy(alpha = 0.8f) // Green for faster speeds
            currentSpeed < 1f -> Color(0xFF2196F3).copy(alpha = 0.8f) // Blue for slower speeds
            else -> Color(0xFF9E9E9E).copy(alpha = 0.8f) // Gray for normal speed
        },
        animationSpec = tween(300),
        label = "background_color"
    )
    
    if (alpha > 0f) {
        Card(
            modifier = modifier
                .alpha(alpha)
                .scale(scale),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Speed Memory",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                
                Text(
                    text = "${currentSpeed}x",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun FloatingSpeedMemoryIndicator(
    hasSpeedMemory: Boolean,
    currentSpeed: Float,
    modifier: Modifier = Modifier
) {
    var showIndicator by remember { mutableStateOf(false) }
    var previousSpeed by remember { mutableStateOf(currentSpeed) }
    
    // Show indicator when speed memory exists or when speed changes
    LaunchedEffect(hasSpeedMemory, currentSpeed) {
        val speedChanged = currentSpeed != previousSpeed && currentSpeed != 1.0f
        
        if (hasSpeedMemory || speedChanged) {
            showIndicator = true
            kotlinx.coroutines.delay(if (speedChanged) 2000 else 3000) // Show for 2-3 seconds
            showIndicator = false
        }
        
        previousSpeed = currentSpeed
    }
    
    SpeedMemoryIndicator(
        hasSpeedMemory = hasSpeedMemory,
        currentSpeed = currentSpeed,
        isVisible = showIndicator,
        modifier = modifier
    )
}

@Composable
fun EnhancedSpeedMemoryFeedback(
    hasSpeedMemory: Boolean,
    currentSpeed: Float,
    isSpeedChanging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pulseAnimation by animateFloatAsState(
        targetValue = if (isSpeedChanging) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 800f
        ),
        label = "pulse_animation"
    )
    
    val glowAnimation by animateFloatAsState(
        targetValue = if (hasSpeedMemory && isSpeedChanging) 0.8f else 0f,
        animationSpec = tween(300),
        label = "glow_animation"
    )
    
    if (hasSpeedMemory) {
        Box(
            modifier = modifier
                .scale(pulseAnimation)
                .alpha(0.9f)
        ) {
            // Glow effect when speed is changing
            if (glowAnimation > 0f) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.White.copy(alpha = glowAnimation * 0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .align(Alignment.Center)
                )
            }
            
            SpeedMemoryIndicator(
                hasSpeedMemory = hasSpeedMemory,
                currentSpeed = currentSpeed,
                isVisible = true,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}