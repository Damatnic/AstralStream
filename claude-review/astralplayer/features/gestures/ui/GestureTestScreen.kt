package com.astralplayer.features.gestures.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astralplayer.features.gestures.data.*
import com.astralplayer.features.gestures.viewmodel.GestureTestViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureTestScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GestureTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val touchPoints = remember { mutableStateListOf<TouchPoint>() }
    val gestureTrail = remember { mutableStateListOf<Offset>() }
    var showZoneOverlay by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Gestures") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showZoneOverlay = !showZoneOverlay }) {
                        Icon(
                            if (showZoneOverlay) Icons.Default.GridOff else Icons.Default.GridOn,
                            contentDescription = "Toggle zones"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Gesture detection area
            GestureDetectionArea(
                onGestureDetected = viewModel::onGestureDetected,
                onTouchEvent = { points ->
                    touchPoints.clear()
                    touchPoints.addAll(points)
                },
                onGestureTrail = { trail ->
                    gestureTrail.clear()
                    gestureTrail.addAll(trail)
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Zone overlay
            if (showZoneOverlay) {
                ZoneOverlay(
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Touch visualization
            TouchVisualization(
                touchPoints = touchPoints,
                gestureTrail = gestureTrail,
                modifier = Modifier.fillMaxSize()
            )
            
            // Detected gesture display
            AnimatedVisibility(
                visible = uiState.lastDetectedGesture != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                DetectedGestureCard(
                    gesture = uiState.lastDetectedGesture,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Stats panel
            StatsPanel(
                detectedGestures = uiState.detectedGestures,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
            
            // Instructions
            if (uiState.detectedGestures.isEmpty()) {
                InstructionsOverlay(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun GestureDetectionArea(
    onGestureDetected: (GestureType, GestureZone, Offset, Offset) -> Unit,
    onTouchEvent: (List<TouchPoint>) -> Unit,
    onGestureTrail: (List<Offset>) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }
    var longPressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var gestureStartPosition by remember { mutableStateOf(Offset.Zero) }
    val currentTouchPoints = remember { mutableStateListOf<TouchPoint>() }
    val trailPoints = remember { mutableStateListOf<Offset>() }
    
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    val zone = getZoneFromPosition(centroid, size)
                    
                    when {
                        zoom > 1.1f -> onGestureDetected(
                            GestureType.PINCH_OUT,
                            zone,
                            centroid,
                            centroid
                        )
                        zoom < 0.9f -> onGestureDetected(
                            GestureType.PINCH_IN,
                            zone,
                            centroid,
                            centroid
                        )
                        abs(rotation) > 15f -> onGestureDetected(
                            if (rotation > 0) GestureType.ROTATION_CLOCKWISE
                            else GestureType.ROTATION_COUNTER_CLOCKWISE,
                            zone,
                            centroid,
                            centroid
                        )
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    gestureStartPosition = down.position
                    currentTouchPoints.clear()
                    currentTouchPoints.add(TouchPoint(down.position, down.id.value))
                    onTouchEvent(currentTouchPoints.toList())
                    
                    // Track all pointers
                    val touches = mutableMapOf(down.id to down)
                    
                    longPressJob?.cancel()
                    longPressJob = scope.launch {
                        delay(500)
                        val zone = getZoneFromPosition(down.position, size)
                        onGestureDetected(
                            GestureType.LONG_PRESS,
                            zone,
                            down.position,
                            down.position
                        )
                    }
                    
                    do {
                        val event = awaitPointerEvent()
                        
                        // Update touch points
                        currentTouchPoints.clear()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                currentTouchPoints.add(
                                    TouchPoint(change.position, change.id.value)
                                )
                                touches[change.id] = change
                            } else {
                                touches.remove(change.id)
                            }
                        }
                        onTouchEvent(currentTouchPoints.toList())
                        
                        // Track gesture trail
                        if (event.changes.size == 1 && event.changes[0].pressed) {
                            trailPoints.add(event.changes[0].position)
                            if (trailPoints.size > 50) {
                                trailPoints.removeAt(0)
                            }
                            onGestureTrail(trailPoints.toList())
                        }
                        
                        // Detect multi-finger tap
                        if (touches.size >= 2 && event.changes.all { !it.pressed }) {
                            longPressJob?.cancel()
                            val center = Offset(
                                touches.values.map { it.position.x }.average().toFloat(),
                                touches.values.map { it.position.y }.average().toFloat()
                            )
                            val zone = getZoneFromPosition(center, size)
                            
                            when (touches.size) {
                                2 -> onGestureDetected(
                                    GestureType.TWO_FINGER_TAP,
                                    zone,
                                    center,
                                    center
                                )
                                3 -> onGestureDetected(
                                    GestureType.THREE_FINGER_TAP,
                                    zone,
                                    center,
                                    center
                                )
                            }
                        }
                        
                    } while (event.changes.any { it.pressed })
                    
                    longPressJob?.cancel()
                    trailPoints.clear()
                    onGestureTrail(emptyList())
                    currentTouchPoints.clear()
                    onTouchEvent(emptyList())
                    
                    // Single finger gestures
                    if (touches.size == 1) {
                        val up = touches.values.first()
                        val distance = (up.position - gestureStartPosition).getDistance()
                        val zone = getZoneFromPosition(gestureStartPosition, size)
                        
                        when {
                            distance < 20f -> {
                                // Tap or double tap
                                val currentTime = System.currentTimeMillis()
                                val timeDiff = currentTime - lastTapTime
                                val positionDiff = (up.position - lastTapPosition).getDistance()
                                
                                if (timeDiff < 300 && positionDiff < 50) {
                                    onGestureDetected(
                                        GestureType.DOUBLE_TAP,
                                        zone,
                                        up.position,
                                        up.position
                                    )
                                } else {
                                    onGestureDetected(
                                        GestureType.TAP,
                                        zone,
                                        up.position,
                                        up.position
                                    )
                                }
                                
                                lastTapTime = currentTime
                                lastTapPosition = up.position
                            }
                            distance > 50f -> {
                                // Swipe
                                val angle = atan2(
                                    up.position.y - gestureStartPosition.y,
                                    up.position.x - gestureStartPosition.x
                                ) * 180 / PI
                                
                                val gestureType = when (angle) {
                                    in -45..45 -> GestureType.SWIPE_RIGHT
                                    in 45..135 -> GestureType.SWIPE_DOWN
                                    in -135..-45 -> GestureType.SWIPE_UP
                                    else -> GestureType.SWIPE_LEFT
                                }
                                
                                onGestureDetected(
                                    gestureType,
                                    zone,
                                    gestureStartPosition,
                                    up.position
                                )
                            }
                        }
                    }
                }
            }
    )
}

@Composable
private fun ZoneOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val dashLength = 10.dp.toPx()
        val dashGap = 5.dp.toPx()
        
        // Draw zone boundaries
        drawDashedLine(
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = strokeWidth,
            dashLength = dashLength,
            dashGap = dashGap
        )
        
        drawDashedLine(
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = strokeWidth,
            dashLength = dashLength,
            dashGap = dashGap
        )
        
        // Draw quarter lines
        drawDashedLine(
            start = Offset(size.width / 4, 0f),
            end = Offset(size.width / 4, size.height),
            strokeWidth = strokeWidth / 2,
            dashLength = dashLength,
            dashGap = dashGap,
            alpha = 0.3f
        )
        
        drawDashedLine(
            start = Offset(size.width * 3 / 4, 0f),
            end = Offset(size.width * 3 / 4, size.height),
            strokeWidth = strokeWidth / 2,
            dashLength = dashLength,
            dashGap = dashGap,
            alpha = 0.3f
        )
    }
}

@Composable
private fun TouchVisualization(
    touchPoints: List<TouchPoint>,
    gestureTrail: List<Offset>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Draw gesture trail
        if (gestureTrail.size > 1) {
            val path = Path().apply {
                moveTo(gestureTrail[0].x, gestureTrail[0].y)
                for (i in 1 until gestureTrail.size) {
                    lineTo(gestureTrail[i].x, gestureTrail[i].y)
                }
            }
            
            drawPath(
                path = path,
                color = Color.Blue.copy(alpha = 0.5f),
                style = Stroke(width = 4.dp.toPx())
            )
        }
        
        // Draw touch points
        touchPoints.forEach { point ->
            drawCircle(
                color = Color.Red.copy(alpha = 0.3f),
                radius = 40.dp.toPx(),
                center = point.position
            )
            drawCircle(
                color = Color.Red,
                radius = 8.dp.toPx(),
                center = point.position
            )
        }
    }
}

@Composable
private fun DetectedGestureCard(
    gesture: DetectedGesture?,
    modifier: Modifier = Modifier
) {
    gesture?.let {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GestureIcon(
                    gestureType = it.type,
                    modifier = Modifier.size(40.dp)
                )
                Column {
                    Text(
                        text = formatGestureType(it.type),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zone: ${formatZone(it.zone)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    it.matchedAction?.let { action ->
                        Text(
                            text = "→ ${formatAction(action)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsPanel(
    detectedGestures: List<DetectedGesture>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Gesture Stats",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            
            val gestureCount = detectedGestures.groupBy { it.type }
                .mapValues { it.value.size }
                .entries
                .sortedByDescending { it.value }
                .take(5)
            
            if (gestureCount.isEmpty()) {
                Text(
                    text = "No gestures yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                gestureCount.forEach { (type, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatGestureType(type),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionsOverlay(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Try Some Gestures!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tap, swipe, pinch, or use multiple fingers to test gesture detection",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// Helper functions
private fun getZoneFromPosition(position: Offset, size: androidx.compose.ui.unit.IntSize): GestureZone {
    val x = position.x / size.width
    val y = position.y / size.height
    
    return when {
        x < 0.25f && y < 0.5f -> GestureZone.TOP_LEFT_QUARTER
        x > 0.75f && y < 0.5f -> GestureZone.TOP_RIGHT_QUARTER
        x < 0.25f && y > 0.5f -> GestureZone.BOTTOM_LEFT_QUARTER
        x > 0.75f && y > 0.5f -> GestureZone.BOTTOM_RIGHT_QUARTER
        x < 0.5f && y in 0.25f..0.75f -> GestureZone.LEFT_HALF
        x > 0.5f && y in 0.25f..0.75f -> GestureZone.RIGHT_HALF
        y < 0.5f && x in 0.25f..0.75f -> GestureZone.TOP_HALF
        y > 0.5f && x in 0.25f..0.75f -> GestureZone.BOTTOM_HALF
        x in 0.25f..0.75f && y in 0.25f..0.75f -> GestureZone.CENTER
        else -> GestureZone.FULL_SCREEN
    }
}

private fun DrawScope.drawDashedLine(
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    dashLength: Float,
    dashGap: Float,
    color: Color = Color.Gray,
    alpha: Float = 0.5f
) {
    val distance = (end - start).getDistance()
    val direction = (end - start) / distance
    var currentDistance = 0f
    
    while (currentDistance < distance) {
        val segmentStart = start + direction * currentDistance
        val segmentEnd = start + direction * minOf(currentDistance + dashLength, distance)
        
        drawLine(
            color = color.copy(alpha = alpha),
            start = segmentStart,
            end = segmentEnd,
            strokeWidth = strokeWidth
        )
        
        currentDistance += dashLength + dashGap
    }
}

data class TouchPoint(
    val position: Offset,
    val id: Long
)