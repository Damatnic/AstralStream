package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class SpeedMemoryToastType {
    RESTORED,
    SAVED,
    CLEARED,
    ERROR
}

@Composable
fun SpeedMemoryToast(
    message: String,
    type: SpeedMemoryToastType,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 400f
        ),
        label = "toast_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "toast_alpha"
    )
    
    val backgroundColor = when (type) {
        SpeedMemoryToastType.RESTORED -> Color(0xFF4CAF50) // Green
        SpeedMemoryToastType.SAVED -> Color(0xFF2196F3) // Blue
        SpeedMemoryToastType.CLEARED -> Color(0xFFFF9800) // Orange
        SpeedMemoryToastType.ERROR -> Color(0xFFF44336) // Red
    }
    
    val icon = when (type) {
        SpeedMemoryToastType.RESTORED -> Icons.Default.Restore
        SpeedMemoryToastType.SAVED -> Icons.Default.Save
        SpeedMemoryToastType.CLEARED -> Icons.Default.Delete
        SpeedMemoryToastType.ERROR -> Icons.Default.Error
    }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(2500) // Auto-dismiss after 2.5 seconds
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.8f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(250)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .scale(scale)
                .alpha(alpha),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SpeedMemoryToastContainer(
    currentToast: SpeedMemoryToastState?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        currentToast?.let { toast ->
            SpeedMemoryToast(
                message = toast.message,
                type = toast.type,
                isVisible = toast.isVisible,
                onDismiss = toast.onDismiss,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

data class SpeedMemoryToastState(
    val message: String,
    val type: SpeedMemoryToastType,
    val isVisible: Boolean = true,
    val onDismiss: () -> Unit = {}
)