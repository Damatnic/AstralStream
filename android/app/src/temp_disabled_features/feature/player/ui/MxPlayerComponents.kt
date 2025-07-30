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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun UltraSmoothSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    bufferedPercentage: Float,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "seek_progress"
    )
    
    val animatedBuffer by animateFloatAsState(
        targetValue = bufferedPercentage,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "buffer_progress"
    )
    
    Box(modifier = modifier.height(32.dp)) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .align(Alignment.Center)
                .background(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(3.dp)
                )
        )
        
        // Buffered track with smooth animation
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedBuffer.coerceIn(0f, 1f))
                .height(6.dp)
                .align(Alignment.CenterStart)
                .background(
                    color = Color.White.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(3.dp)
                )
        )
        
        // Progress track with MX Player cyan color
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedValue.coerceIn(0f, 1f))
                .height(6.dp)
                .align(Alignment.CenterStart)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00BCD4),
                            Color(0xFF00E5FF)
                        )
                    ),
                    shape = RoundedCornerShape(3.dp)
                )
        )
        
        // Invisible slider for interaction
        Slider(
            value = animatedValue.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0f),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SmoothControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        animationSpec = tween(100),
        label = "button_alpha"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun CosmicLoadingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_loading")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale)
        ) {
            rotate(rotation) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.minDimension / 4
                
                // Draw rotating cosmic elements
                for (i in 0 until 8) {
                    val angle = (i * 45f) * (kotlin.math.PI / 180f)
                    val x = centerX + kotlin.math.cos(angle).toFloat() * radius
                    val y = centerY + kotlin.math.sin(angle).toFloat() * radius
                    
                    drawCircle(
                        color = Color(0xFF00BCD4).copy(alpha = 0.8f - i * 0.1f),
                        radius = 8f - i * 0.5f,
                        center = Offset(x, y)
                    )
                }
            }
        }
        
        // Center cosmic glow
        Canvas(modifier = Modifier.size(40.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.6f),
                        Color(0xFF00BCD4).copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2
            )
        }
    }
}

@Composable
fun AIFeaturesPanel(
    onFeatureToggle: (AIFeature) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val features = remember {
        listOf(
            AIFeature("ai_upscale", "AI Upscaling", "Enhance video quality", Icons.Default.HighQuality),
            AIFeature("ai_subtitle", "AI Subtitles", "Generate subtitles", Icons.Default.Subtitles),
            AIFeature("ai_translate", "Live Translation", "Translate subtitles", Icons.Default.Translate),
            AIFeature("ai_summary", "Video Summary", "Generate summary", Icons.Default.Summarize)
        )
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Features",
                    color = Color(0xFF00D4FF),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            features.forEach { feature ->
                AIFeatureItem(
                    feature = feature,
                    onClick = { onFeatureToggle(feature) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AIFeatureItem(
    feature: AIFeature,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.name,
                tint = Color(0xFF00D4FF),
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = feature.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

data class AIFeature(
    val id: String,
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)