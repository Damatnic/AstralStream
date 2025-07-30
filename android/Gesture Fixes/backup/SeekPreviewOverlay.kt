package com.astralplayer.nextplayer.feature.player.gestures

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * Enhanced seek preview overlay with thumbnail support
 */
@Composable
fun SeekPreviewOverlay(
    state: SeekPreviewState,
    currentPosition: Long,
    totalDuration: Long,
    isForward: Boolean,
    settings: SeekingGestureSettings,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible || !settings.enableSeekPreview) return
    
    val density = LocalDensity.current
    
    // Animation states
    val scaleAnimation by animateFloatAsState(
        targetValue = if (state.isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "seekPreviewScale"
    )
    
    val slideAnimation by animateFloatAsState(
        targetValue = if (state.isVisible) 0f else 50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "seekPreviewSlide"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(scaleAnimation)
            .alpha(scaleAnimation),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .offset(y = (-slideAnimation).dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E).copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Thumbnail section
                ThumbnailSection(
                    thumbnail = state.currentThumbnail,
                    isGenerating = state.thumbnailGenerating,
                    thumbnailSize = settings.previewThumbnailSize,
                    isForward = isForward
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Time and position info
                TimeDisplaySection(
                    currentPosition = currentPosition,
                    targetPosition = state.targetPosition,
                    totalDuration = totalDuration,
                    seekDelta = state.seekDelta,
                    isForward = isForward
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Progress bar
                if (settings.showProgressBar) {
                    SeekProgressBar(
                        currentPosition = currentPosition,
                        targetPosition = state.targetPosition,
                        totalDuration = totalDuration,
                        isForward = isForward
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Seek direction indicator
                SeekDirectionIndicator(
                    isForward = isForward,
                    seekDelta = state.seekDelta
                )
            }
        }
        
        // Background blur effect
        SeekPreviewBackground(
            isVisible = state.isVisible,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ThumbnailSection(
    thumbnail: Bitmap?,
    isGenerating: Boolean,
    thumbnailSize: ThumbnailSize,
    isForward: Boolean
) {
    val size = when (thumbnailSize) {
        ThumbnailSize.SMALL -> 120.dp
        ThumbnailSize.MEDIUM -> 160.dp
        ThumbnailSize.LARGE -> 200.dp
    }
    
    Box(
        modifier = Modifier
            .size(width = size, height = size * 9 / 16) // 16:9 aspect ratio
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        when {
            thumbnail != null -> {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = "Seek preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Overlay gradient for better text visibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            }
            isGenerating -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFF00BCD4),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Loading preview...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Preview unavailable",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        // Corner indicator for seek direction
        Box(
            modifier = Modifier
                .align(if (isForward) Alignment.TopEnd else Alignment.TopStart)
                .padding(8.dp)
                .background(
                    color = if (isForward) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = if (isForward) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun TimeDisplaySection(
    currentPosition: Long,
    targetPosition: Long,
    totalDuration: Long,
    seekDelta: Long,
    isForward: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Target time (large display)
        Text(
            text = formatTime(targetPosition),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        // Duration context
        Text(
            text = "/ ${formatTime(totalDuration)}",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Seek delta information
        if (abs(seekDelta) > 1000) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isForward) Icons.Default.Add else Icons.Default.Remove,
                    contentDescription = null,
                    tint = if (isForward) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = formatTime(abs(seekDelta)),
                    color = if (isForward) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Current position (smaller, for reference)
        Text(
            text = "From ${formatTime(currentPosition)}",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SeekProgressBar(
    currentPosition: Long,
    targetPosition: Long,
    totalDuration: Long,
    isForward: Boolean
) {
    if (totalDuration <= 0) return
    
    val currentProgress = currentPosition.toFloat() / totalDuration
    val targetProgress = targetPosition.toFloat() / totalDuration
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.2f))
    ) {
        // Background progress (current position)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(currentProgress)
                .background(Color.White.copy(alpha = 0.4f))
        )
        
        // Target progress indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(targetProgress)
                .background(
                    if (isForward) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
        )
        
        // Animated seek range
        val animatedProgress by animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "seekProgress"
        )
        
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            (if (isForward) Color(0xFF4CAF50) else Color(0xFFFF9800)).copy(alpha = 0.8f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun SeekDirectionIndicator(
    isForward: Boolean,
    seekDelta: Long
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
            contentDescription = null,
            tint = if (isForward) Color(0xFF4CAF50) else Color(0xFFFF9800),
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = if (isForward) "Fast Forward" else "Rewind",
            color = if (isForward) Color(0xFF4CAF50) else Color(0xFFFF9800),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        // Speed indicator based on seek delta
        val speed = calculateSeekSpeed(seekDelta)
        if (speed > 1f) {
            Text(
                text = "${speed.roundToInt()}x",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SeekPreviewBackground(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 0.3f else 0f,
        animationSpec = tween(300),
        label = "backgroundAlpha"
    )
    
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = alpha))
    )
}

/**
 * Compact seek preview for minimal UI mode
 */
@Composable
fun CompactSeekPreview(
    targetPosition: Long,
    totalDuration: Long,
    seekDelta: Long,
    isForward: Boolean,
    modifier: Modifier = Modifier
) {
    val scaleAnimation by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "compactScale"
    )
    
    Card(
        modifier = modifier
            .scale(scaleAnimation)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = null,
                tint = if (isForward) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = formatTime(targetPosition),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (abs(seekDelta) > 1000) {
                Text(
                    text = "(${if (seekDelta > 0) "+" else ""}${formatTime(abs(seekDelta))})",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Thumbnail generation and caching system
 */
class ThumbnailManager(
    private val onThumbnailReady: (position: Long, bitmap: Bitmap?) -> Unit
) {
    private val thumbnailCache = mutableMapOf<Long, Bitmap?>()
    private val generationJobs = mutableMapOf<Long, Job>()
    private val maxCacheSize = 100
    
    fun requestThumbnail(position: Long, priority: ThumbnailPriority = ThumbnailPriority.NORMAL) {
        // Check cache first
        thumbnailCache[position]?.let { cachedBitmap ->
            onThumbnailReady(position, cachedBitmap)
            return
        }
        
        // Cancel existing job for this position if any
        generationJobs[position]?.cancel()
        
        // Start new generation job
        generationJobs[position] = CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = generateThumbnail(position)
                
                // Cache the result
                cacheThumbnail(position, bitmap)
                
                withContext(Dispatchers.Main) {
                    onThumbnailReady(position, bitmap)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onThumbnailReady(position, null)
                }
            } finally {
                generationJobs.remove(position)
            }
        }
    }
    
    private suspend fun generateThumbnail(position: Long): Bitmap? {
        // This would integrate with ExoPlayer's MediaMetadataRetriever or similar
        // For now, simulate thumbnail generation
        delay(100) // Simulate generation time
        return null // Placeholder - would return actual bitmap
    }
    
    private fun cacheThumbnail(position: Long, bitmap: Bitmap?) {
        // Implement LRU cache behavior
        if (thumbnailCache.size >= maxCacheSize) {
            val oldestKey = thumbnailCache.keys.first()
            thumbnailCache.remove(oldestKey)
        }
        
        thumbnailCache[position] = bitmap
    }
    
    fun preloadThumbnails(startPosition: Long, endPosition: Long, interval: Long = 10000L) {
        var currentPos = startPosition
        while (currentPos <= endPosition) {
            requestThumbnail(currentPos, ThumbnailPriority.LOW)
            currentPos += interval
        }
    }
    
    fun clearCache() {
        generationJobs.values.forEach { it.cancel() }
        generationJobs.clear()
        thumbnailCache.clear()
    }
    
    enum class ThumbnailPriority {
        LOW, NORMAL, HIGH
    }
}

/**
 * Seek preview analytics for performance monitoring
 */
class SeekPreviewAnalytics {
    private val previewData = mutableListOf<SeekPreviewData>()
    
    data class SeekPreviewData(
        val timestamp: Long,
        val position: Long,
        val thumbnailLoadTime: Long,
        val thumbnailAvailable: Boolean,
        val userAccuracy: Float // How close the final seek was to the preview
    )
    
    fun recordPreviewUsage(
        position: Long,
        thumbnailLoadTime: Long,
        thumbnailAvailable: Boolean,
        finalSeekPosition: Long
    ) {
        val accuracy = 1f - abs(finalSeekPosition - position).toFloat() / max(position, 1L)
        
        val data = SeekPreviewData(
            timestamp = System.currentTimeMillis(),
            position = position,
            thumbnailLoadTime = thumbnailLoadTime,
            thumbnailAvailable = thumbnailAvailable,
            userAccuracy = accuracy
        )
        
        previewData.add(data)
        
        // Limit data size
        if (previewData.size > 1000) {
            previewData.removeAt(0)
        }
    }
    
    fun getAverageThumbnailLoadTime(): Long {
        return if (previewData.isNotEmpty()) {
            previewData.map { it.thumbnailLoadTime }.average().toLong()
        } else {
            100L
        }
    }
    
    fun getThumbnailAvailabilityRate(): Float {
        return if (previewData.isNotEmpty()) {
            previewData.count { it.thumbnailAvailable }.toFloat() / previewData.size
        } else {
            1f
        }
    }
    
    fun getAverageUserAccuracy(): Float {
        return if (previewData.isNotEmpty()) {
            previewData.map { it.userAccuracy }.average().toFloat()
        } else {
            1f
        }
    }
}

// Helper functions
private fun calculateSeekSpeed(seekDelta: Long): Float {
    return when {
        abs(seekDelta) < 5000L -> 1f
        abs(seekDelta) < 15000L -> 2f
        abs(seekDelta) < 30000L -> 4f
        abs(seekDelta) < 60000L -> 8f
        else -> 16f
    }
}

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