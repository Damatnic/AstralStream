package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Modern volume overlay with multiple style options
 */
@Composable
fun VolumeOverlay(
    isVisible: Boolean,
    currentLevel: Float,
    maxLevel: Float = 1f,
    style: OverlayStyle = OverlayStyle.MODERN,
    side: TouchSide = TouchSide.RIGHT,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    val scaleAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "volumeScale"
    )
    
    val slideAnimation by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "volumeSlide"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(scaleAnimation)
            .alpha(scaleAnimation),
        contentAlignment = when (side) {
            TouchSide.RIGHT -> Alignment.CenterEnd
            TouchSide.LEFT -> Alignment.CenterStart
            TouchSide.CENTER -> Alignment.Center
        }
    ) {
        when (style) {
            OverlayStyle.MODERN -> ModernVolumeOverlay(
                currentLevel = currentLevel,
                maxLevel = maxLevel,
                slideOffset = slideAnimation,
                side = side
            )
            OverlayStyle.CLASSIC -> ClassicVolumeOverlay(
                currentLevel = currentLevel,
                maxLevel = maxLevel,
                slideOffset = slideAnimation
            )
            OverlayStyle.MINIMAL -> MinimalVolumeOverlay(
                currentLevel = currentLevel,
                maxLevel = maxLevel,
                slideOffset = slideAnimation
            )
            OverlayStyle.COSMIC -> CosmicVolumeOverlay(
                currentLevel = currentLevel,
                maxLevel = maxLevel,
                slideOffset = slideAnimation
            )
        }
    }
}

@Composable
private fun ModernVolumeOverlay(
    currentLevel: Float,
    maxLevel: Float,
    slideOffset: Float,
    side: TouchSide
) {
    Card(
        modifier = Modifier
            .padding(24.dp)
            .offset(x = if (side == TouchSide.RIGHT) slideOffset.dp else (-slideOffset).dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Volume icon with animation
            VolumeIcon(currentLevel, maxLevel)
            
            // Volume level text
            Text(
                text = "${(currentLevel * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Modern volume bars
            ModernVolumeBars(
                currentLevel = currentLevel,
                maxLevel = maxLevel,
                barCount = 10
            )
            
            // Volume level indicator
            Text(
                text = "Volume",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ClassicVolumeOverlay(
    currentLevel: Float,
    maxLevel: Float,
    slideOffset: Float
) {
    Box(
        modifier = Modifier
            .padding(32.dp)
            .offset(x = slideOffset.dp)
            .background(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VolumeIcon(currentLevel, maxLevel)
            
            // Classic horizontal progress bar
            LinearProgressIndicator(
                progress = currentLevel / maxLevel,
                modifier = Modifier
                    .width(120.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF00BCD4),
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            
            Text(
                text = "${(currentLevel * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MinimalVolumeOverlay(
    currentLevel: Float,
    maxLevel: Float,
    slideOffset: Float
) {
    Box(
        modifier = Modifier
            .padding(24.dp)
            .offset(x = slideOffset.dp)
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape
            )
            .size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            VolumeIcon(currentLevel, maxLevel, size = 24.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(currentLevel * 100).roundToInt()}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CosmicVolumeOverlay(
    currentLevel: Float,
    maxLevel: Float,
    slideOffset: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cosmicRotation"
    )
    
    Box(
        modifier = Modifier
            .padding(32.dp)
            .offset(x = slideOffset.dp)
            .size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Cosmic background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        ) {
            drawCosmicVolumeBackground(currentLevel, maxLevel)
        }
        
        // Center content
        Card(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E).copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                VolumeIcon(currentLevel, maxLevel, size = 24.dp)
                Text(
                    text = "${(currentLevel * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Modern brightness overlay with multiple style options
 */
@Composable
fun BrightnessOverlay(
    isVisible: Boolean,
    currentLevel: Float,
    maxLevel: Float = 1f,
    style: OverlayStyle = OverlayStyle.MODERN,
    side: TouchSide = TouchSide.LEFT,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    val scaleAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "brightnessScale"
    )
    
    val slideAnimation by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "brightnessSlide"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(scaleAnimation)
            .alpha(scaleAnimation),
        contentAlignment = when (side) {
            TouchSide.LEFT -> Alignment.CenterStart
            TouchSide.RIGHT -> Alignment.CenterEnd
            TouchSide.CENTER -> Alignment.Center
        }
    ) {
        when (style) {
            OverlayStyle.MODERN -> ModernBrightnessOverlay(
                currentLevel = currentLevel,
                maxLevel = maxLevel,
                slideOffset = slideAnimation,
                side = side
            )
            OverlayStyle.CLASSIC -> ClassicBrightnessOverlay(
                currentLevel = currentLevel,
                maxLevel = maxLevel,
                slideOffset = slideAnimation
            )
            OverlayStyle.MINIMAL -> MinimalBrightnessOverlay(
                currentLevel = currentLevel,
                maxLevel = maxLevel,
                slideOffset = slideAnimation
            )
            OverlayStyle.COSMIC -> CosmicBrightnessOverlay(
                currentLevel = currentLevel,
                maxLevel = maxLevel,
                slideOffset = slideAnimation
            )
        }
    }
}

@Composable
private fun ModernBrightnessOverlay(
    currentLevel: Float,
    maxLevel: Float,
    slideOffset: Float,
    side: TouchSide
) {
    Card(
        modifier = Modifier
            .padding(24.dp)
            .offset(x = if (side == TouchSide.LEFT) (-slideOffset).dp else slideOffset.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Brightness icon with glow effect
            BrightnessIcon(currentLevel, maxLevel)
            
            // Brightness level text
            Text(
                text = "${(currentLevel * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Modern brightness bars
            ModernBrightnessBars(
                currentLevel = currentLevel,
                maxLevel = maxLevel,
                barCount = 10
            )
            
            // Brightness level indicator
            Text(
                text = "Brightness",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ClassicBrightnessOverlay(
    currentLevel: Float,
    maxLevel: Float,
    slideOffset: Float
) {
    Box(
        modifier = Modifier
            .padding(32.dp)
            .offset(x = (-slideOffset).dp)
            .background(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrightnessIcon(currentLevel, maxLevel)
            
            // Classic horizontal progress bar
            LinearProgressIndicator(
                progress = currentLevel / maxLevel,
                modifier = Modifier
                    .width(120.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFFFC107),
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            
            Text(
                text = "${(currentLevel * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MinimalBrightnessOverlay(
    currentLevel: Float,
    maxLevel: Float,
    slideOffset: Float
) {
    Box(
        modifier = Modifier
            .padding(24.dp)
            .offset(x = (-slideOffset).dp)
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape
            )
            .size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrightnessIcon(currentLevel, maxLevel, size = 24.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(currentLevel * 100).roundToInt()}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CosmicBrightnessOverlay(
    currentLevel: Float,
    maxLevel: Float,
    slideOffset: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmicBrightness")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brightnessGlow"
    )
    
    Box(
        modifier = Modifier
            .padding(32.dp)
            .offset(x = (-slideOffset).dp)
            .size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Cosmic brightness background
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawCosmicBrightnessBackground(currentLevel, maxLevel, glow)
        }
        
        // Center content
        Card(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E).copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BrightnessIcon(currentLevel, maxLevel, size = 24.dp)
                Text(
                    text = "${(currentLevel * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper composables
@Composable
private fun VolumeIcon(
    currentLevel: Float,
    maxLevel: Float,
    size: androidx.compose.ui.unit.Dp = 32.dp
) {
    val icon = when {
        currentLevel <= 0f -> Icons.Default.VolumeOff
        currentLevel < maxLevel * 0.3f -> Icons.Default.VolumeDown
        currentLevel < maxLevel * 0.7f -> Icons.AutoMirrored.Filled.VolumeUp
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }
    
    val color = when {
        currentLevel <= 0f -> Color(0xFFFF5722)
        currentLevel >= maxLevel -> Color(0xFF4CAF50)
        else -> Color(0xFF00BCD4)
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "Volume",
        tint = color,
        modifier = Modifier.size(size)
    )
}

@Composable
private fun BrightnessIcon(
    currentLevel: Float,
    maxLevel: Float,
    size: androidx.compose.ui.unit.Dp = 32.dp
) {
    val icon = when {
        currentLevel < maxLevel * 0.3f -> Icons.Default.BrightnessLow
        currentLevel < maxLevel * 0.7f -> Icons.Default.BrightnessMedium
        else -> Icons.Default.BrightnessHigh
    }
    
    val color = Color(0xFFFFC107).copy(alpha = 0.5f + currentLevel * 0.5f)
    
    Icon(
        imageVector = icon,
        contentDescription = "Brightness",
        tint = color,
        modifier = Modifier.size(size)
    )
}

@Composable
private fun ModernVolumeBars(
    currentLevel: Float,
    maxLevel: Float,
    barCount: Int
) {
    val activeBarCount = ((currentLevel / maxLevel) * barCount).roundToInt()
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(barCount) { index ->
            val isActive = index < activeBarCount
            val barHeight = (8 + index * 2).dp
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .background(
                        color = if (isActive) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun ModernBrightnessBars(
    currentLevel: Float,
    maxLevel: Float,
    barCount: Int
) {
    val activeBarCount = ((currentLevel / maxLevel) * barCount).roundToInt()
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(barCount) { index ->
            val isActive = index < activeBarCount
            val barHeight = (8 + index * 2).dp
            val alpha = if (isActive) 0.5f + (currentLevel * 0.5f) else 0.3f
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .background(
                        color = if (isActive) Color(0xFFFFC107).copy(alpha = alpha) 
                               else Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

// Custom drawing functions
private fun DrawScope.drawCosmicVolumeBackground(currentLevel: Float, maxLevel: Float) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2
    val progress = currentLevel / maxLevel
    
    // Draw volume rings
    repeat(5) { ring ->
        val ringRadius = radius * (0.3f + ring * 0.15f)
        val ringAlpha = if (ring < progress * 5) 0.8f else 0.2f
        
        drawCircle(
            color = Color(0xFF00BCD4),
            radius = ringRadius,
            center = center,
            alpha = ringAlpha,
            style = Stroke(width = 2.dp.toPx())
        )
    }
    
    // Draw volume particles
    val particleCount = (progress * 20).toInt()
    repeat(particleCount) { particle ->
        val angle = (particle * 360f / 20f) * PI / 180f
        val particleRadius = radius * 0.8f
        val particleX = center.x + cos(angle).toFloat() * particleRadius
        val particleY = center.y + sin(angle).toFloat() * particleRadius
        
        drawCircle(
            color = Color(0xFF00BCD4),
            radius = 3.dp.toPx(),
            center = Offset(particleX, particleY),
            alpha = 0.6f
        )
    }
}

private fun DrawScope.drawCosmicBrightnessBackground(
    currentLevel: Float,
    maxLevel: Float,
    glow: Float
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2
    val progress = currentLevel / maxLevel
    
    // Draw brightness glow
    val glowRadius = radius * (0.5f + progress * 0.5f) * glow
    val gradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFC107).copy(alpha = progress * 0.3f),
            Color.Transparent
        ),
        radius = glowRadius,
        center = center
    )
    
    drawCircle(
        brush = gradient,
        radius = glowRadius,
        center = center
    )
    
    // Draw brightness rays
    val rayCount = (progress * 12).toInt()
    repeat(rayCount) { ray ->
        val angle = (ray * 360f / 12f) * PI / 180f
        val rayLength = radius * 0.7f * progress
        val startX = center.x + cos(angle).toFloat() * radius * 0.3f
        val startY = center.y + sin(angle).toFloat() * radius * 0.3f
        val endX = center.x + cos(angle).toFloat() * (radius * 0.3f + rayLength)
        val endY = center.y + sin(angle).toFloat() * (radius * 0.3f + rayLength)
        
        drawLine(
            color = Color(0xFFFFC107),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.dp.toPx(),
            alpha = 0.6f
        )
    }
}