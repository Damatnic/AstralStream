package com.astralplayer.features.editing.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.editing.*
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun TimelineView(
    timeline: Timeline,
    currentPosition: Long,
    duration: Long,
    zoomLevel: Float,
    selectedClip: TimelineClip?,
    onClipSelected: (TimelineClip?) -> Unit,
    onClipMoved: (TimelineClip, Long) -> Unit,
    onTimelineSeek: (Long) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    var timelineOffset by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    Box(modifier = modifier) {
        // Timeline Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Handle zoom
                        val newZoom = (zoomLevel * zoom).coerceIn(0.5f, 5f)
                        onZoomChange(newZoom)
                        
                        // Handle pan
                        timelineOffset = (timelineOffset + pan.x).coerceIn(
                            -(duration * zoomLevel - size.width).coerceAtLeast(0f),
                            0f
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Convert tap position to timeline position
                        val timelinePos = ((offset.x - timelineOffset) / zoomLevel).toLong()
                        onTimelineSeek(timelinePos.coerceIn(0, duration))
                    }
                }
        ) {
            drawTimeline(
                timeline = timeline,
                currentPosition = currentPosition,
                duration = duration,
                zoomLevel = zoomLevel,
                timelineOffset = timelineOffset,
                selectedClip = selectedClip,
                textMeasurer = textMeasurer
            )
        }
        
        // Track Headers
        Column(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Time ruler header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Time",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            // Video tracks
            timeline.videoTracks.forEach { track ->
                TrackHeader(
                    track = track,
                    height = 60.dp
                )
            }
            
            // Audio tracks
            timeline.audioTracks.forEach { track ->
                AudioTrackHeader(
                    track = track,
                    height = 40.dp
                )
            }
        }
        
        // Zoom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            IconButton(
                onClick = { onZoomChange((zoomLevel * 0.8f).coerceIn(0.5f, 5f)) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("-", fontSize = 20.sp)
            }
            
            Text(
                "${(zoomLevel * 100).toInt()}%",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            
            IconButton(
                onClick = { onZoomChange((zoomLevel * 1.2f).coerceIn(0.5f, 5f)) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", fontSize = 20.sp)
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawTimeline(
    timeline: Timeline,
    currentPosition: Long,
    duration: Long,
    zoomLevel: Float,
    timelineOffset: Float,
    selectedClip: TimelineClip?,
    textMeasurer: TextMeasurer
) {
    val timelineWidth = size.width - 80.dp.toPx() // Account for track headers
    val timelineStart = 80.dp.toPx()
    
    // Draw background
    drawRect(
        color = Color.Black.copy(alpha = 0.05f),
        topLeft = Offset(timelineStart, 0f),
        size = androidx.compose.ui.geometry.Size(timelineWidth, size.height)
    )
    
    // Draw time ruler
    drawTimeRuler(
        duration = duration,
        zoomLevel = zoomLevel,
        timelineOffset = timelineOffset,
        timelineStart = timelineStart,
        textMeasurer = textMeasurer
    )
    
    var trackY = 30.dp.toPx()
    
    // Draw video tracks
    timeline.videoTracks.forEach { track ->
        drawVideoTrack(
            track = track,
            trackY = trackY,
            trackHeight = 60.dp.toPx(),
            zoomLevel = zoomLevel,
            timelineOffset = timelineOffset,
            timelineStart = timelineStart,
            selectedClip = selectedClip,
            textMeasurer = textMeasurer
        )
        trackY += 60.dp.toPx()
    }
    
    // Draw audio tracks
    timeline.audioTracks.forEach { track ->
        drawAudioTrack(
            track = track,
            trackY = trackY,
            trackHeight = 40.dp.toPx(),
            zoomLevel = zoomLevel,
            timelineOffset = timelineOffset,
            timelineStart = timelineStart,
            textMeasurer = textMeasurer
        )
        trackY += 40.dp.toPx()
    }
    
    // Draw playhead
    val playheadX = timelineStart + (currentPosition * zoomLevel) + timelineOffset
    if (playheadX >= timelineStart && playheadX <= size.width) {
        drawLine(
            color = Color.Red,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, size.height),
            strokeWidth = 2.dp.toPx()
        )
        
        // Draw playhead handle
        drawCircle(
            color = Color.Red,
            radius = 8.dp.toPx(),
            center = Offset(playheadX, 15.dp.toPx())
        )
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawTimeRuler(
    duration: Long,
    zoomLevel: Float,
    timelineOffset: Float,
    timelineStart: Float,
    textMeasurer: TextMeasurer
) {
    val rulerHeight = 30.dp.toPx()
    
    // Draw ruler background
    drawRect(
        color = Color.Gray.copy(alpha = 0.1f),
        topLeft = Offset(timelineStart, 0f),
        size = androidx.compose.ui.geometry.Size(size.width - timelineStart, rulerHeight)
    )
    
    // Calculate time intervals based on zoom level
    val pixelsPerSecond = zoomLevel * 1000
    val intervalSeconds = when {
        pixelsPerSecond > 100 -> 1L
        pixelsPerSecond > 50 -> 5L
        pixelsPerSecond > 10 -> 10L
        pixelsPerSecond > 5 -> 30L
        else -> 60L
    }
    
    val intervalMillis = intervalSeconds * 1000
    var time = 0L
    
    while (time <= duration) {
        val x = timelineStart + (time * zoomLevel) + timelineOffset
        
        if (x >= timelineStart && x <= size.width) {
            // Draw tick mark
            val tickHeight = if (time % (intervalMillis * 5) == 0L) 15.dp.toPx() else 8.dp.toPx()
            drawLine(
                color = Color.Gray,
                start = Offset(x, rulerHeight - tickHeight),
                end = Offset(x, rulerHeight),
                strokeWidth = 1.dp.toPx()
            )
            
            // Draw time label for major ticks
            if (time % (intervalMillis * 5) == 0L) {
                val timeText = formatTimeLabel(time)
                val textResult = textMeasurer.measure(
                    text = AnnotatedString(timeText),
                    style = TextStyle(fontSize = 10.sp)
                )
                
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(
                        x - textResult.size.width / 2,
                        rulerHeight - tickHeight - textResult.size.height - 2.dp.toPx()
                    )
                )
            }
        }
        
        time += intervalMillis
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawVideoTrack(
    track: VideoTrack,
    trackY: Float,
    trackHeight: Float,
    zoomLevel: Float,
    timelineOffset: Float,
    timelineStart: Float,
    selectedClip: TimelineClip?,
    textMeasurer: TextMeasurer
) {
    // Draw track background
    drawRect(
        color = Color.Gray.copy(alpha = 0.05f),
        topLeft = Offset(timelineStart, trackY),
        size = androidx.compose.ui.geometry.Size(size.width - timelineStart, trackHeight)
    )
    
    // Draw track separator
    drawLine(
        color = Color.Gray.copy(alpha = 0.3f),
        start = Offset(timelineStart, trackY + trackHeight),
        end = Offset(size.width, trackY + trackHeight),
        strokeWidth = 1.dp.toPx()
    )
    
    // Draw clips
    track.clips.forEach { clip ->
        drawClip(
            clip = clip,
            trackY = trackY,
            trackHeight = trackHeight,
            zoomLevel = zoomLevel,
            timelineOffset = timelineOffset,
            timelineStart = timelineStart,
            isSelected = clip == selectedClip,
            textMeasurer = textMeasurer
        )
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawClip(
    clip: TimelineClip,
    trackY: Float,
    trackHeight: Float,
    zoomLevel: Float,
    timelineOffset: Float,
    timelineStart: Float,
    isSelected: Boolean,
    textMeasurer: TextMeasurer
) {
    val clipX = timelineStart + (clip.startTime * zoomLevel) + timelineOffset
    val clipWidth = clip.duration * zoomLevel
    val clipPadding = 2.dp.toPx()
    
    // Only draw if clip is visible
    if (clipX + clipWidth >= timelineStart && clipX <= size.width) {
        // Clip bounds
        val actualX = maxOf(clipX, timelineStart)
        val actualWidth = minOf(clipX + clipWidth, size.width) - actualX
        
        clipRect(
            left = actualX,
            right = actualX + actualWidth,
            top = trackY + clipPadding,
            bottom = trackY + trackHeight - clipPadding
        ) {
            // Draw clip background
            drawRoundRect(
                color = when (clip.type) {
                    ClipType.VIDEO -> Color(0xFF2196F3)
                    ClipType.AUDIO -> Color(0xFF4CAF50)
                    ClipType.IMAGE -> Color(0xFFFF9800)
                    ClipType.TEXT -> Color(0xFF9C27B0)
                    else -> Color.Gray
                },
                topLeft = Offset(actualX, trackY + clipPadding),
                size = androidx.compose.ui.geometry.Size(actualWidth, trackHeight - clipPadding * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            
            // Draw selection border
            if (isSelected) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(actualX, trackY + clipPadding),
                    size = androidx.compose.ui.geometry.Size(actualWidth, trackHeight - clipPadding * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            // Draw clip name
            if (actualWidth > 40.dp.toPx()) {
                val clipName = clip.name.ifEmpty { "Clip ${clip.id.take(4)}" }
                val textResult = textMeasurer.measure(
                    text = AnnotatedString(clipName),
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color.White
                    ),
                    maxLines = 1
                )
                
                if (textResult.size.width < actualWidth - 8.dp.toPx()) {
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset(
                            actualX + 4.dp.toPx(),
                            trackY + (trackHeight - textResult.size.height) / 2
                        )
                    )
                }
            }
            
            // Draw thumbnails for video clips
            if (clip.type == ClipType.VIDEO && actualWidth > 60.dp.toPx()) {
                drawVideoThumbnails(
                    clipX = actualX,
                    clipY = trackY + clipPadding,
                    clipWidth = actualWidth,
                    clipHeight = trackHeight - clipPadding * 2
                )
            }
        }
    }
}

private fun DrawScope.drawVideoThumbnails(
    clipX: Float,
    clipY: Float,
    clipWidth: Float,
    clipHeight: Float
) {
    // Draw placeholder thumbnails
    val thumbnailWidth = 40.dp.toPx()
    val thumbnailCount = (clipWidth / thumbnailWidth).toInt()
    
    for (i in 0 until thumbnailCount) {
        val x = clipX + i * thumbnailWidth + 4.dp.toPx()
        if (x + thumbnailWidth <= clipX + clipWidth) {
            drawRect(
                color = Color.Black.copy(alpha = 0.3f),
                topLeft = Offset(x, clipY + 4.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(
                    thumbnailWidth - 8.dp.toPx(),
                    clipHeight - 8.dp.toPx()
                )
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawAudioTrack(
    track: AudioTrack,
    trackY: Float,
    trackHeight: Float,
    zoomLevel: Float,
    timelineOffset: Float,
    timelineStart: Float,
    textMeasurer: TextMeasurer
) {
    // Draw track background
    drawRect(
        color = Color.Green.copy(alpha = 0.05f),
        topLeft = Offset(timelineStart, trackY),
        size = androidx.compose.ui.geometry.Size(size.width - timelineStart, trackHeight)
    )
    
    // Draw track separator
    drawLine(
        color = Color.Gray.copy(alpha = 0.3f),
        start = Offset(timelineStart, trackY + trackHeight),
        end = Offset(size.width, trackY + trackHeight),
        strokeWidth = 1.dp.toPx()
    )
    
    // Draw audio clips
    track.clips.forEach { clip ->
        drawAudioClip(
            clip = clip,
            trackY = trackY,
            trackHeight = trackHeight,
            zoomLevel = zoomLevel,
            timelineOffset = timelineOffset,
            timelineStart = timelineStart,
            textMeasurer = textMeasurer
        )
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawAudioClip(
    clip: AudioClip,
    trackY: Float,
    trackHeight: Float,
    zoomLevel: Float,
    timelineOffset: Float,
    timelineStart: Float,
    textMeasurer: TextMeasurer
) {
    val clipX = timelineStart + (clip.startTime * zoomLevel) + timelineOffset
    val clipWidth = clip.duration * zoomLevel
    val clipPadding = 2.dp.toPx()
    
    // Only draw if clip is visible
    if (clipX + clipWidth >= timelineStart && clipX <= size.width) {
        val actualX = maxOf(clipX, timelineStart)
        val actualWidth = minOf(clipX + clipWidth, size.width) - actualX
        
        // Draw clip background
        drawRoundRect(
            color = Color(0xFF4CAF50),
            topLeft = Offset(actualX, trackY + clipPadding),
            size = androidx.compose.ui.geometry.Size(actualWidth, trackHeight - clipPadding * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )
        
        // Draw waveform
        if (actualWidth > 20.dp.toPx()) {
            drawAudioWaveform(
                clipX = actualX,
                clipY = trackY + clipPadding,
                clipWidth = actualWidth,
                clipHeight = trackHeight - clipPadding * 2,
                waveformData = clip.audioInfo.waveformData
            )
        }
    }
}

private fun DrawScope.drawAudioWaveform(
    clipX: Float,
    clipY: Float,
    clipWidth: Float,
    clipHeight: Float,
    waveformData: List<Float>
) {
    if (waveformData.isEmpty()) {
        // Draw placeholder waveform
        val path = Path()
        val centerY = clipY + clipHeight / 2
        path.moveTo(clipX, centerY)
        
        for (i in 0..clipWidth.toInt() step 2) {
            val amplitude = sin(i * 0.05f) * clipHeight * 0.3f
            path.lineTo(clipX + i, centerY + amplitude)
        }
        
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.5f),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun TrackHeader(
    track: VideoTrack,
    height: androidx.compose.ui.unit.Dp
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            
            if (track.isLocked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioTrackHeader(
    track: AudioTrack,
    height: androidx.compose.ui.unit.Dp
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(
            green = MaterialTheme.colorScheme.surfaceVariant.green + 0.1f
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = track.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatTimeLabel(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    return if (minutes > 0) {
        String.format("%d:%02d", minutes, seconds % 60)
    } else {
        String.format("0:%02d", seconds)
    }
}