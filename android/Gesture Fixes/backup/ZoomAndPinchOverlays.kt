package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Zoom overlay with multiple visual styles
 */
@Composable
fun ZoomOverlay(
    isVisible: Boolean,
    currentZoom: Float,
    minZoom: Float = 0.5f,
    maxZoom: Float = 4.0f,
    style: OverlayStyle = OverlayStyle.MODERN,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    val scaleAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "zoomScale"
    )
    
    val fadeAnimation by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "zoomFade"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(scaleAnimation)
            .alpha(fadeAnimation),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            OverlayStyle.MODERN -> ModernZoomOverlay(
                currentZoom = currentZoom,
                minZoom = minZoom,
                maxZoom = maxZoom
            )
            OverlayStyle.CLASSIC -> ClassicZoomOverlay(
                currentZoom = currentZoom,
                minZoom = minZoom,
                maxZoom = maxZoom
            )
            OverlayStyle.MINIMAL -> MinimalZoomOverlay(
                currentZoom = currentZoom,
                minZoom = minZoom,
                maxZoom = maxZoom
            )
            OverlayStyle.COSMIC -> CosmicZoomOverlay(
                currentZoom = currentZoom,
                minZoom = minZoom,
                maxZoom = maxZoom
            )
        }
    }
}

@Composable
private fun ModernZoomOverlay(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float
) {
    Card(
        modifier = Modifier.padding(24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Zoom icon with scale animation
            ZoomIcon(currentZoom, minZoom, maxZoom)
            
            // Zoom level text
            Text(
                text = "${(currentZoom * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Modern zoom indicator
            ModernZoomIndicator(
                currentZoom = currentZoom,
                minZoom = minZoom,
                maxZoom = maxZoom
            )
            
            // Zoom controls hint
            Text(
                text = "Pinch to zoom",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ClassicZoomOverlay(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float
) {
    Box(
        modifier = Modifier
            .padding(32.dp)
            .background(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZoomIcon(currentZoom, minZoom, maxZoom, size = 28.dp)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(currentZoom * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Classic progress bar
                LinearProgressIndicator(
                    progress = (currentZoom - minZoom) / (maxZoom - minZoom),
                    modifier = Modifier
                        .width(100.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF00BCD4),
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun MinimalZoomOverlay(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float
) {
    Box(
        modifier = Modifier
            .padding(24.dp)
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = CircleShape
            )
            .size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ZoomIcon(currentZoom, minZoom, maxZoom, size = 32.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(currentZoom * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CosmicZoomOverlay(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmicZoom")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "zoomRotation"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoomPulse"
    )
    
    Box(
        modifier = Modifier
            .padding(32.dp)
            .size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Cosmic zoom background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        ) {
            drawCosmicZoomBackground(currentZoom, minZoom, maxZoom, pulse)
        }
        
        // Center content
        Card(
            modifier = Modifier.size(90.dp),
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
                ZoomIcon(currentZoom, minZoom, maxZoom, size = 28.dp)
                Text(
                    text = "${(currentZoom * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Pinch gesture detector with zoom overlay integration
 */
@Composable
fun PinchZoomGestureDetector(
    settings: PinchZoomGestureSettings,
    onZoomStart: (center: Offset) -> Unit,
    onZoomChange: (scale: Float, center: Offset) -> Unit,
    onZoomEnd: (finalScale: Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isZooming by remember { mutableStateOf(false) }
    var zoomCenter by remember { mutableStateOf(Offset.Zero) }
    var currentScale by remember { mutableStateOf(1f) }
    
    Box(
        modifier = modifier
            .pointerInput(settings) {
                if (!settings.isEnabled) return@pointerInput
                
                detectTransformGestures(
                    panZoomLock = true
                ) { centroid, pan, zoom, rotation ->
                    if (!isZooming && abs(zoom - 1f) > 0.05f) {
                        isZooming = true
                        zoomCenter = centroid
                        onZoomStart(centroid)
                    }
                    
                    if (isZooming) {
                        val newScale = (currentScale * zoom).coerceIn(settings.minZoom, settings.maxZoom)
                        if (newScale != currentScale) {
                            currentScale = newScale
                            onZoomChange(newScale, centroid)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.isEmpty() && isZooming) {
                            isZooming = false
                            onZoomEnd(currentScale)
                        }
                    } while (event.changes.isNotEmpty())
                }
            }
    ) {
        content()
        
        // Show zoom overlay when zooming
        if (isZooming && settings.showZoomOverlay) {
            ZoomOverlay(
                isVisible = true,
                currentZoom = currentScale,
                minZoom = settings.minZoom,
                maxZoom = settings.maxZoom,
                style = OverlayStyle.MODERN
            )
        }
    }
}

/**
 * Advanced zoom controls with gesture integration
 */
@Composable
fun ZoomControlsOverlay(
    isVisible: Boolean,
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    val slideAnimation by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "zoomControlsSlide"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .offset(y = slideAnimation.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E).copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom in button
                IconButton(
                    onClick = onZoomIn,
                    enabled = currentZoom < maxZoom,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (currentZoom < maxZoom) Color(0xFF00BCD4).copy(alpha = 0.2f) 
                                   else Color.Gray.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom in",
                        tint = if (currentZoom < maxZoom) Color(0xFF00BCD4) else Color.Gray
                    )
                }
                
                // Current zoom level
                Text(
                    text = "${(currentZoom * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // Zoom out button
                IconButton(
                    onClick = onZoomOut,
                    enabled = currentZoom > minZoom,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (currentZoom > minZoom) Color(0xFF00BCD4).copy(alpha = 0.2f) 
                                   else Color.Gray.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom out",
                        tint = if (currentZoom > minZoom) Color(0xFF00BCD4) else Color.Gray
                    )
                }
                
                // Reset zoom button
                if (currentZoom != 1f) {
                    Divider(
                        modifier = Modifier.width(24.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    
                    IconButton(
                        onClick = onZoomReset,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color(0xFFFF9800).copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Reset zoom",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper composables
@Composable
private fun ZoomIcon(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    size: androidx.compose.ui.unit.Dp = 32.dp
) {
    val icon = when {
        currentZoom < 1f -> Icons.Default.ZoomOut
        currentZoom > 1f -> Icons.Default.ZoomIn
        else -> Icons.Default.CenterFocusStrong
    }
    
    val color = when {
        currentZoom < 1f -> Color(0xFFFF9800)
        currentZoom > 1f -> Color(0xFF4CAF50)
        else -> Color(0xFF00BCD4)
    }
    
    val scale by animateFloatAsState(
        targetValue = 1f + (currentZoom - 1f) * 0.2f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "zoomIconScale"
    )
    
    Icon(
        imageVector = icon,
        contentDescription = "Zoom",
        tint = color,
        modifier = Modifier
            .size(size)
            .scale(scale)
    )
}

@Composable
private fun ModernZoomIndicator(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float
) {
    val progress = (currentZoom - minZoom) / (maxZoom - minZoom)
    
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF9800),
                            Color(0xFF00BCD4),
                            Color(0xFF4CAF50)
                        )
                    )
                )
        )
        
        // Zoom level markers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(5) { index ->
                val markerProgress = index / 4f
                val isActive = progress >= markerProgress
                
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

/**
 * Zoom gesture analytics for adaptive learning
 */
class ZoomGestureAnalytics {
    private val zoomData = mutableListOf<ZoomGestureData>()
    
    data class ZoomGestureData(
        val timestamp: Long,
        val startZoom: Float,
        val endZoom: Float,
        val duration: Long,
        val gestureType: ZoomGestureType,
        val accuracy: Float // How precisely the user reached their intended zoom
    )
    
    enum class ZoomGestureType {
        PINCH_IN, PINCH_OUT, BUTTON_ZOOM, DOUBLE_TAP_RESET
    }
    
    fun recordZoomGesture(
        startZoom: Float,
        endZoom: Float,
        duration: Long,
        gestureType: ZoomGestureType
    ) {
        val accuracy = calculateZoomAccuracy(startZoom, endZoom, duration)
        
        val data = ZoomGestureData(
            timestamp = System.currentTimeMillis(),
            startZoom = startZoom,
            endZoom = endZoom,
            duration = duration,
            gestureType = gestureType,
            accuracy = accuracy
        )
        
        zoomData.add(data)
        
        // Limit data size
        if (zoomData.size > 500) {
            zoomData.removeAt(0)
        }
    }
    
    private fun calculateZoomAccuracy(startZoom: Float, endZoom: Float, duration: Long): Float {
        // Simple accuracy calculation based on zoom smoothness
        val zoomChange = abs(endZoom - startZoom)
        val expectedDuration = zoomChange * 1000f // Expected 1 second per 1x zoom change
        return (1f - abs(duration - expectedDuration) / expectedDuration).coerceIn(0f, 1f)
    }
    
    fun getAverageZoomAccuracy(): Float {
        return if (zoomData.isNotEmpty()) {
            zoomData.map { it.accuracy }.average().toFloat()
        } else {
            1f
        }
    }
    
    fun getPreferredZoomRange(): Pair<Float, Float> {
        if (zoomData.isEmpty()) return Pair(0.5f, 4f)
        
        val zoomLevels = zoomData.flatMap { listOf(it.startZoom, it.endZoom) }
        val minUsed = zoomLevels.minOrNull() ?: 0.5f
        val maxUsed = zoomLevels.maxOrNull() ?: 4f
        
        return Pair(minUsed, maxUsed)
    }
    
    fun getMostUsedGestureType(): ZoomGestureType {
        return if (zoomData.isNotEmpty()) {
            zoomData.groupBy { it.gestureType }
                .maxByOrNull { it.value.size }?.key ?: ZoomGestureType.PINCH_IN
        } else {
            ZoomGestureType.PINCH_IN
        }
    }
}

// Custom drawing functions
private fun DrawScope.drawCosmicZoomBackground(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    pulse: Float
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2
    val progress = (currentZoom - minZoom) / (maxZoom - minZoom)
    
    // Draw zoom rings
    repeat(6) { ring ->
        val ringRadius = radius * (0.2f + ring * 0.12f) * pulse
        val ringAlpha = if (ring < progress * 6) 0.6f else 0.2f
        val ringColor = when {
            progress < 0.33f -> Color(0xFFFF9800) // Orange for zoom out
            progress < 0.66f -> Color(0xFF00BCD4) // Cyan for normal
            else -> Color(0xFF4CAF50) // Green for zoom in
        }
        
        drawCircle(
            color = ringColor,
            radius = ringRadius,
            center = center,
            alpha = ringAlpha,
            style = Stroke(width = 2.dp.toPx())
        )
    }
    
    // Draw zoom particles
    val particleCount = (progress * 24).toInt()
    repeat(particleCount) { particle ->
        val angle = (particle * 360f / 24f) * PI / 180f
        val particleRadius = radius * (0.6f + sin(particle * 0.5f) * 0.2f)
        val particleX = center.x + cos(angle).toFloat() * particleRadius
        val particleY = center.y + sin(angle).toFloat() * particleRadius
        
        val particleColor = when {
            progress < 0.33f -> Color(0xFFFF9800)
            progress < 0.66f -> Color(0xFF00BCD4)
            else -> Color(0xFF4CAF50)
        }
        
        drawCircle(
            color = particleColor,
            radius = (2 + progress * 3).dp.toPx(),
            center = Offset(particleX, particleY),
            alpha = 0.7f
        )
    }
    
    // Draw zoom level arcs
    val arcCount = 4
    repeat(arcCount) { arc ->
        val arcRadius = radius * (0.4f + arc * 0.15f)
        val sweepAngle = 60f * progress
        val startAngle = arc * 90f
        
        rotate(degrees = arc * 45f, pivot = center) {
            drawArc(
                color = Color.White,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2),
                alpha = 0.4f,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}