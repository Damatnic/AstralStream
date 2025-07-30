package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.feature.player.gestures.SeekDirection
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun VolumeOverlay(
    visible: Boolean,
    volume: Float,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Card(
                modifier = Modifier
                    .width(60.dp)
                    .height(200.dp),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Background track
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight(0.8f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    
                    // Volume indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(4.dp)
                            .fillMaxHeight(0.8f * volume)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                    
                    // Volume icon
                    Icon(
                        imageVector = when {
                            volume == 0f -> Icons.AutoMirrored.Filled.VolumeOff
                            volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = "Volume",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .size(28.dp)
                    )
                    
                    // Volume percentage
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BrightnessOverlay(
    visible: Boolean,
    brightness: Float,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Card(
                modifier = Modifier
                    .width(60.dp)
                    .height(200.dp),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Background track
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight(0.8f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    
                    // Brightness indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(4.dp)
                            .fillMaxHeight(0.8f * brightness)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFFFD700)) // Gold color for brightness
                    )
                    
                    // Brightness icon
                    Icon(
                        imageVector = if (brightness < 0.3f) Icons.Filled.BrightnessLow else Icons.Filled.BrightnessHigh,
                        contentDescription = "Brightness",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .size(28.dp)
                    )
                    
                    // Brightness percentage
                    Text(
                        text = "${(brightness * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SeekOverlay(
    visible: Boolean,
    seekDirection: SeekDirection,
    seekAmount: Long,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(150)) + expandIn(expandFrom = Alignment.Center),
        exit = fadeOut(animationSpec = tween(300)) + shrinkOut(shrinkTowards = Alignment.Center),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Animated arrow icons
                    val arrowCount = minOf(3, (abs(seekAmount) / 10000).toInt() + 1)
                    for (i in 0 until arrowCount) {
                        AnimatedArrowIcon(
                            direction = seekDirection,
                            delay = i * 100
                        )
                    }
                    
                    // Seek amount text
                    Text(
                        text = formatSeekTime(seekAmount),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedArrowIcon(
    direction: SeekDirection,
    delay: Int
) {
    var alpha by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(0.8f) }
    
    LaunchedEffect(key1 = true) {
        delay(delay.toLong())
        alpha = 1f
        scale = 1f
    }
    
    Icon(
        imageVector = when (direction) {
            SeekDirection.FORWARD -> Icons.AutoMirrored.Filled.KeyboardArrowRight
            SeekDirection.BACKWARD -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
            SeekDirection.NONE -> Icons.Filled.Circle
        },
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier
            .size(32.dp)
            .alpha(animateFloatAsState(targetValue = alpha, animationSpec = tween(300)).value)
            .scale(animateFloatAsState(targetValue = scale, animationSpec = spring()).value)
    )
}

@Composable
fun DoubleTapSeekOverlay(
    visible: Boolean,
    position: Offset,
    seekAmount: Int,
    isForward: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(500)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRippleEffect(
                    center = position,
                    progress = 1f,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (!isForward) 48.dp else 0.dp,
                        end = if (isForward) 48.dp else 0.dp
                    ),
                contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isForward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "$seekAmount seconds",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ZoomIndicator(
    visible: Boolean,
    zoomLevel: Float,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier.padding(top = 32.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ZoomIn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${(zoomLevel * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun PlaybackSpeedIndicator(
    visible: Boolean,
    speed: Float,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier.padding(bottom = 100.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = when {
                            speed < 1f -> Color(0xFF4CAF50) // Green for slow
                            speed > 1f -> Color(0xFFFF9800) // Orange for fast
                            else -> Color.White
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "${speed}x",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawRippleEffect(
    center: Offset,
    progress: Float,
    color: Color
) {
    val radius = 100.dp.toPx() * progress
    val alpha = 1f - progress
    
    drawCircle(
        color = color.copy(alpha = alpha * 0.3f),
        radius = radius,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )
    
    drawCircle(
        color = color.copy(alpha = alpha * 0.1f),
        radius = radius,
        center = center
    )
}

private fun formatSeekTime(milliseconds: Long): String {
    val seconds = abs(milliseconds) / 1000
    val sign = if (milliseconds < 0) "-" else "+"
    return "$sign${seconds}s"
}