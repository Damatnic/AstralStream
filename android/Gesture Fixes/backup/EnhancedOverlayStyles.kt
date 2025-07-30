package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Enhanced volume and brightness overlays with configurable styles
 * Implements requirements 4.2: Volume and brightness overlays
 */

/**
 * Overlay style configurations
 */
enum class OverlayStyle {
    CLASSIC,    // Traditional simple design
    MODERN,     // Material 3 design with gradients
    MINIMAL,    // Clean, minimal design
    COSMIC      // Futuristic design with animations
}

/**
 * Enhanced volume overlay with configurable styles
 */
@Composable
fun EnhancedVolumeOverlay(
    volume: Float,
    isVisible: Boolean,
    style: OverlayStyle = OverlayStyle.MODERN,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = when (style) {
            OverlayStyle.CLASSIC -> fadeIn() + slideInHorizontally(initialOffsetX = { it })
            OverlayStyle.MODERN -> fadeIn() + scaleIn() + slideInHorizontally(initialOffsetX = { it / 2 })
            OverlayStyle.MINIMAL -> fadeIn()
            OverlayStyle.COSMIC -> fadeIn() + scaleIn() + slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        },
        exit = when (style) {
            OverlayStyle.CLASSIC -> fadeOut() + slideOutHorizontally(targetOffsetX = { it })
            OverlayStyle.MODERN -> fadeOut() + scaleOut() + slideOutHorizontally(targetOffsetX = { it / 2 })
            OverlayStyle.MINIMAL -> fadeOut()
            OverlayStyle.COSMIC -> fadeOut() + scaleOut() + slideOutHorizontally(targetOffsetX = { it })
        },
        modifier = modifier
    ) {
        when (style) {
            OverlayStyle.CLASSIC -> ClassicVolumeOverlay(volume)
            OverlayStyle.MODERN -> ModernVolumeOverlay(volume)
            OverlayStyle.MINIMAL -> MinimalVolumeOverlay(volume)
            OverlayStyle.COSMIC -> CosmicVolumeOverlay(volume)
        }
    }
}

/**
 * Enhanced brightness overlay with configurable styles
 */
@Composable
fun EnhancedBrightnessOverlay(
    brightness: Float,
    isVisible: Boolean,
    style: OverlayStyle = OverlayStyle.MODERN,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = when (style) {
            OverlayStyle.CLASSIC -> fadeIn() + slideInHorizontally(initialOffsetX = { -it })
            OverlayStyle.MODERN -> fadeIn() + scaleIn() + slideInHorizontally(initialOffsetX = { -it / 2 })
            OverlayStyle.MINIMAL -> fadeIn()
            OverlayStyle.COSMIC -> fadeIn() + scaleIn() + slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        },
        exit = when (style) {
            OverlayStyle.CLASSIC -> fadeOut() + slideOutHorizontally(targetOffsetX = { -it })
            OverlayStyle.MODERN -> fadeOut() + scaleOut() + slideOutHorizontally(targetOffsetX = { -it / 2 })
            OverlayStyle.MINIMAL -> fadeOut()
            OverlayStyle.COSMIC -> fadeOut() + scaleOut() + slideOutHorizontally(targetOffsetX = { -it })
        },
        modifier = modifier
    ) {
        when (style) {
            OverlayStyle.CLASSIC -> ClassicBrightnessOverlay(brightness)
            OverlayStyle.MODERN -> ModernBrightnessOverlay(brightness)
            OverlayStyle.MINIMAL -> MinimalBrightnessOverlay(brightness)
            OverlayStyle.COSMIC -> CosmicBrightnessOverlay(brightness)
        }
    }
}

/**
 * Classic volume overlay - traditional simple design
 */
@Composable
private fun ClassicVolumeOverlay(volume: Float) {
    Card(
        modifier = Modifier
            .width(60.dp)
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getVolumeIcon(volume),
                contentDescription = "Volume",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            
            // Simple volume bar
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(volume)
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .align(Alignment.BottomCenter)
                )
            }
            
            Text(
                text = "${(volume * 100).toInt()}%",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Modern volume overlay - Material 3 design with gradients
 */
@Composable
private fun ModernVolumeOverlay(volume: Float) {
    val animatedVolume by animateFloatAsState(
        targetValue = volume,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "volume"
    )
    
    Card(
        modifier = Modifier
            .width(70.dp)
            .height(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated icon with scale effect
            val iconScale by animateFloatAsState(
                targetValue = 1f + (volume * 0.2f),
                animationSpec = spring(),
                label = "icon_scale"
            )
            
            Icon(
                imageVector = getVolumeIcon(volume),
                contentDescription = "Volume",
                tint = Color(0xFF00BCD4),
                modifier = Modifier
                    .size(28.dp)
                    .scale(iconScale)
            )
            
            // Modern gradient volume bar
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .weight(1f)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(6.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedVolume)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF00BCD4),
                                    Color(0xFF0097A7)
                                )
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .align(Alignment.BottomCenter)
                )
            }
            
            Text(
                text = "${(animatedVolume * 100).toInt()}%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Minimal volume overlay - clean, minimal design
 */
@Composable
private fun MinimalVolumeOverlay(volume: Float) {
    Box(
        modifier = Modifier
            .width(50.dp)
            .height(180.dp)
            .background(
                Color.Black.copy(alpha = 0.6f),
                RoundedCornerShape(25.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getVolumeIcon(volume),
                contentDescription = "Volume",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            
            // Minimal volume indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(120.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(volume)
                        .background(Color.White, CircleShape)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * Cosmic volume overlay - futuristic design with animations
 */
@Composable
private fun CosmicVolumeOverlay(volume: Float) {
    val animatedVolume by animateFloatAsState(
        targetValue = volume,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "volume"
    )
    
    // Pulsing animation based on volume
    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1f + (volume * 0.1f),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(240.dp)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00BCD4).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(40.dp)
                )
        )
        
        // Main container
        Card(
            modifier = Modifier
                .width(70.dp)
                .height(220.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(35.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rotating icon
                val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing)
                    ),
                    label = "rotation"
                )
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00BCD4).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .rotate(rotation),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getVolumeIcon(volume),
                        contentDescription = "Volume",
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Cosmic volume bar with particles
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .weight(1f)
                ) {
                    // Background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                    )
                    
                    // Volume fill with gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedVolume)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF00BCD4),
                                        Color(0xFF4FC3F7),
                                        Color(0xFF81C784)
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(Alignment.BottomCenter)
                    )
                    
                    // Floating particles
                    repeat(5) { index ->
                        val particleOffset by rememberInfiniteTransition(label = "particle_$index").animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 2000 + (index * 200),
                                    easing = LinearEasing
                                ),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "particle_offset_$index"
                        )
                        
                        if (animatedVolume > index * 0.2f) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .offset(
                                        x = (sin(particleOffset * 2 * PI + index) * 8).dp,
                                        y = (150 * (1 - particleOffset) - index * 30).dp
                                    )
                                    .background(
                                        Color.White.copy(alpha = 1f - particleOffset),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
                
                // Glowing percentage text
                Text(
                    text = "${(animatedVolume * 100).toInt()}%",
                    color = Color(0xFF00BCD4),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Classic brightness overlay
 */
@Composable
private fun ClassicBrightnessOverlay(brightness: Float) {
    Card(
        modifier = Modifier
            .width(60.dp)
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getBrightnessIcon(brightness),
                contentDescription = "Brightness",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(brightness)
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .align(Alignment.BottomCenter)
                )
            }
            
            Text(
                text = "${(brightness * 100).toInt()}%",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Modern brightness overlay
 */
@Composable
private fun ModernBrightnessOverlay(brightness: Float) {
    val animatedBrightness by animateFloatAsState(
        targetValue = brightness,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "brightness"
    )
    
    Card(
        modifier = Modifier
            .width(70.dp)
            .height(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val iconScale by animateFloatAsState(
                targetValue = 1f + (brightness * 0.2f),
                animationSpec = spring(),
                label = "icon_scale"
            )
            
            Icon(
                imageVector = getBrightnessIcon(brightness),
                contentDescription = "Brightness",
                tint = Color(0xFFFF9800),
                modifier = Modifier
                    .size(28.dp)
                    .scale(iconScale)
            )
            
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .weight(1f)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(6.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedBrightness)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF9800),
                                    Color(0xFFFFC107)
                                )
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .align(Alignment.BottomCenter)
                )
            }
            
            Text(
                text = "${(animatedBrightness * 100).toInt()}%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Minimal brightness overlay
 */
@Composable
private fun MinimalBrightnessOverlay(brightness: Float) {
    Box(
        modifier = Modifier
            .width(50.dp)
            .height(180.dp)
            .background(
                Color.Black.copy(alpha = 0.6f),
                RoundedCornerShape(25.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = getBrightnessIcon(brightness),
                contentDescription = "Brightness",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(120.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(brightness)
                        .background(Color.White, CircleShape)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * Cosmic brightness overlay
 */
@Composable
private fun CosmicBrightnessOverlay(brightness: Float) {
    val animatedBrightness by animateFloatAsState(
        targetValue = brightness,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "brightness"
    )
    
    val glowIntensity by animateFloatAsState(
        targetValue = brightness,
        animationSpec = tween(500),
        label = "glow"
    )
    
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF9800).copy(alpha = glowIntensity * 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(40.dp)
                )
        )
        
        Card(
            modifier = Modifier
                .width(70.dp)
                .height(220.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(35.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pulsing sun icon
                val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_scale"
                )
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(pulseScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF9800).copy(alpha = glowIntensity * 0.5f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getBrightnessIcon(brightness),
                        contentDescription = "Brightness",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Cosmic brightness bar with light rays
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedBrightness)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFF9800),
                                        Color(0xFFFFC107),
                                        Color(0xFFFFEB3B)
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .align(Alignment.BottomCenter)
                    )
                    
                    // Light rays
                    repeat(3) { index ->
                        val rayAlpha by rememberInfiniteTransition(label = "ray_$index").animateFloat(
                            initialValue = 0.2f,
                            targetValue = 0.8f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 1500 + (index * 300),
                                    easing = FastOutSlowInEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "ray_alpha_$index"
                        )
                        
                        if (animatedBrightness > index * 0.33f) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(20.dp)
                                    .offset(
                                        x = (index * 5 - 5).dp,
                                        y = (150 * (1 - animatedBrightness) + index * 20).dp
                                    )
                                    .background(
                                        Color(0xFFFFEB3B).copy(alpha = rayAlpha * glowIntensity),
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
                
                Text(
                    text = "${(animatedBrightness * 100).toInt()}%",
                    color = Color(0xFFFF9800),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Utility functions for icons
 */
private fun getVolumeIcon(volume: Float): ImageVector {
    return when {
        volume == 0f -> Icons.Default.VolumeOff
        volume < 0.3f -> Icons.Default.VolumeMute
        volume < 0.7f -> Icons.Default.VolumeDown
        else -> Icons.Default.VolumeUp
    }
}

private fun getBrightnessIcon(brightness: Float): ImageVector {
    return when {
        brightness < 0.3f -> Icons.Default.BrightnessLow
        brightness < 0.7f -> Icons.Default.BrightnessMedium
        else -> Icons.Default.BrightnessHigh
    }
}