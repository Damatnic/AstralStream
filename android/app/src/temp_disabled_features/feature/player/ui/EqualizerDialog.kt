package com.astralplayer.nextplayer.feature.player.ui

import android.media.audiofx.Equalizer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import com.astralplayer.nextplayer.feature.player.viewmodel.EqualizerBand
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val equalizerState by viewModel.equalizerState.collectAsState()
    val presets by viewModel.equalizerPresets.collectAsState()
    val currentPreset by viewModel.currentEqualizerPreset.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 600.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = null,
                    tint = Color(0xFF00D4FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Equalizer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Enable/Disable switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Equalizer",
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = equalizerState.enabled,
                        onCheckedChange = { viewModel.toggleEqualizer() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00D4FF),
                            checkedTrackColor = Color(0xFF00D4FF).copy(alpha = 0.5f)
                        )
                    )
                }
                
                if (equalizerState.enabled) {
                    // Preset selector
                    if (presets.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.05f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Presets",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 150.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(presets) { preset ->
                                        PresetItem(
                                            preset = preset,
                                            isSelected = preset == currentPreset,
                                            onClick = { viewModel.selectEqualizerPreset(preset) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Frequency bands
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Manual Adjustment",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(
                                    onClick = { viewModel.resetEqualizer() },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color(0xFF00D4FF)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", fontSize = 12.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Frequency response graph
                            FrequencyResponseGraph(
                                bandLevels = equalizerState.bandLevels,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Band sliders
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                equalizerState.bands.forEachIndexed { index, band ->
                                    BandSlider(
                                        band = band,
                                        level = equalizerState.bandLevels[index],
                                        onLevelChange = { level ->
                                            viewModel.setEqualizerBandLevel(index, level)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Info text
                    Text(
                        text = "Adjust frequency bands to customize audio output",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun PresetItem(
    preset: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFF00D4FF).copy(alpha = 0.2f)
            } else {
                Color.Transparent
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00D4FF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = preset,
                fontSize = 14.sp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun BandSlider(
    band: EqualizerBand,
    level: Int,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "+${band.maxLevel / 100}",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            )
            
            // Slider
            Slider(
                value = level.toFloat(),
                onValueChange = { onLevelChange(it.roundToInt()) },
                valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(150.dp)
                    .graphicsLayer {
                        rotationZ = -90f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    },
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00D4FF),
                    activeTrackColor = Color(0xFF00D4FF),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }
        
        Text(
            text = "-${Math.abs(band.minLevel) / 100}",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = band.frequencyLabel,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FrequencyResponseGraph(
    bandLevels: List<Int>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        // Draw grid lines
        for (i in 0..4) {
            val y = height * i / 4
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Draw frequency response curve
        if (bandLevels.isNotEmpty()) {
            val path = Path()
            val maxLevel = 1500f // Typical equalizer range
            
            bandLevels.forEachIndexed { index, level ->
                val x = width * index / (bandLevels.size - 1)
                val normalizedLevel = level / maxLevel
                val y = centerY - (normalizedLevel * centerY * 0.8f)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    val prevX = width * (index - 1) / (bandLevels.size - 1)
                    val prevLevel = bandLevels[index - 1] / maxLevel
                    val prevY = centerY - (prevLevel * centerY * 0.8f)
                    
                    val controlX1 = prevX + (x - prevX) * 0.5f
                    val controlX2 = prevX + (x - prevX) * 0.5f
                    
                    path.cubicTo(
                        controlX1, prevY,
                        controlX2, y,
                        x, y
                    )
                }
            }
            
            drawPath(
                path = path,
                color = Color(0xFF00D4FF),
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Draw points
            bandLevels.forEachIndexed { index, level ->
                val x = width * index / (bandLevels.size - 1)
                val normalizedLevel = level / maxLevel
                val y = centerY - (normalizedLevel * centerY * 0.8f)
                
                drawCircle(
                    color = Color(0xFF00D4FF),
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
        
        // Draw center line
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.dp.toPx()
        )
    }
}