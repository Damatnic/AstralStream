package com.astralplayer.nextplayer.feature.audio

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

data class EqualizerBand(
    val frequency: Int,
    val frequencyLabel: String,
    val level: Int,
    val minLevel: Int,
    val maxLevel: Int
)

data class EqualizerPreset(
    val id: String,
    val name: String,
    val description: String,
    val bandLevels: List<Int>,
    val isCustom: Boolean = false
)

data class EqualizerState(
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val currentPreset: EqualizerPreset? = null,
    val presets: List<EqualizerPreset> = emptyList(),
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudnessEnhancer: Int = 0,
    val isVisualizerEnabled: Boolean = true,
    val visualizerData: List<Float> = emptyList()
)

class AdvancedEqualizerViewModel : ViewModel() {
    
    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()
    
    private var equalizer: Equalizer? = null
    
    private val defaultPresets = listOf(
        EqualizerPreset(
            id = "normal",
            name = "Normal",
            description = "Balanced sound for all genres",
            bandLevels = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        ),
        EqualizerPreset(
            id = "rock",
            name = "Rock",
            description = "Enhanced bass and treble for rock music",
            bandLevels = listOf(800, 400, -550, -800, -300, 400, 850, 1100, 1100, 1100)
        ),
        EqualizerPreset(
            id = "pop",
            name = "Pop",
            description = "Crisp vocals and punchy bass",
            bandLevels = listOf(-150, 450, 700, 800, 550, 0, -200, -200, -150, -150)
        ),
        EqualizerPreset(
            id = "jazz",
            name = "Jazz",
            description = "Warm mids and smooth highs",
            bandLevels = listOf(400, 200, 0, 200, -150, -150, 0, 200, 400, 500)
        ),
        EqualizerPreset(
            id = "classical",
            name = "Classical",
            description = "Natural sound for orchestral music",
            bandLevels = listOf(500, 300, -200, -200, -200, -200, -700, -700, -700, -950)
        ),
        EqualizerPreset(
            id = "electronic",
            name = "Electronic",
            description = "Enhanced bass and crisp highs",
            bandLevels = listOf(450, 375, 100, 0, -200, 200, 100, 100, 375, 450)
        ),
        EqualizerPreset(
            id = "vocal",
            name = "Vocal",
            description = "Optimized for speech and vocals",
            bandLevels = listOf(-200, -300, -300, 100, 400, 400, 400, 200, 0, -300)
        ),
        EqualizerPreset(
            id = "bass_boost",
            name = "Bass Boost",
            description = "Heavy bass enhancement",
            bandLevels = listOf(700, 700, 400, 200, 0, -300, -400, -400, -400, -400)
        )
    )
    
    fun initializeEqualizer(audioSessionId: Int) {
        viewModelScope.launch {
            try {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = false
                }
                
                val bands = createEqualizerBands()
                _equalizerState.value = _equalizerState.value.copy(
                    bands = bands,
                    presets = defaultPresets
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun createEqualizerBands(): List<EqualizerBand> {
        val eq = equalizer ?: return emptyList()
        val numberOfBands = eq.numberOfBands.toInt()
        val bands = mutableListOf<EqualizerBand>()
        
        for (i in 0 until numberOfBands) {
            val frequency = eq.getCenterFreq(i.toShort())
            val frequencyLabel = formatFrequency(frequency)
            val minLevel = eq.bandLevelRange[0].toInt()
            val maxLevel = eq.bandLevelRange[1].toInt()
            val currentLevel = eq.getBandLevel(i.toShort()).toInt()
            
            bands.add(
                EqualizerBand(
                    frequency = frequency,
                    frequencyLabel = frequencyLabel,
                    level = currentLevel,
                    minLevel = minLevel,
                    maxLevel = maxLevel
                )
            )
        }
        
        return bands
    }
    
    fun toggleEqualizer() {
        val newEnabled = !_equalizerState.value.isEnabled
        equalizer?.enabled = newEnabled
        _equalizerState.value = _equalizerState.value.copy(isEnabled = newEnabled)
    }
    
    fun setBandLevel(bandIndex: Int, level: Int) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), level.toShort())
            
            val updatedBands = _equalizerState.value.bands.toMutableList()
            if (bandIndex < updatedBands.size) {
                updatedBands[bandIndex] = updatedBands[bandIndex].copy(level = level)
                _equalizerState.value = _equalizerState.value.copy(
                    bands = updatedBands,
                    currentPreset = null // Clear preset when manually adjusting
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun applyPreset(preset: EqualizerPreset) {
        try {
            val bands = _equalizerState.value.bands.toMutableList()
            
            preset.bandLevels.forEachIndexed { index, level ->
                if (index < bands.size) {
                    equalizer?.setBandLevel(index.toShort(), level.toShort())
                    bands[index] = bands[index].copy(level = level)
                }
            }
            
            _equalizerState.value = _equalizerState.value.copy(
                bands = bands,
                currentPreset = preset
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun resetEqualizer() {
        try {
            val bands = _equalizerState.value.bands.toMutableList()
            
            bands.forEachIndexed { index, _ ->
                equalizer?.setBandLevel(index.toShort(), 0)
                bands[index] = bands[index].copy(level = 0)
            }
            
            _equalizerState.value = _equalizerState.value.copy(
                bands = bands,
                currentPreset = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun saveCustomPreset(name: String, description: String) {
        val currentLevels = _equalizerState.value.bands.map { it.level }
        val customPreset = EqualizerPreset(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            description = description,
            bandLevels = currentLevels,
            isCustom = true
        )
        
        val updatedPresets = _equalizerState.value.presets + customPreset
        _equalizerState.value = _equalizerState.value.copy(
            presets = updatedPresets,
            currentPreset = customPreset
        )
    }
    
    fun updateVisualizerData(data: List<Float>) {
        _equalizerState.value = _equalizerState.value.copy(visualizerData = data)
    }
    
    private fun formatFrequency(frequency: Int): String {
        return when {
            frequency < 1000 -> "${frequency}Hz"
            frequency < 1000000 -> "${frequency / 1000}kHz"
            else -> "${frequency / 1000000}MHz"
        }
    }
    
    fun release() {
        equalizer?.release()
        equalizer = null
    }
}

@Composable
fun AdvancedEqualizerScreen(
    viewModel: AdvancedEqualizerViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val equalizerState by viewModel.equalizerState.collectAsState()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        ),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            EqualizerHeader(
                isEnabled = equalizerState.isEnabled,
                onToggle = { viewModel.toggleEqualizer() },
                onDismiss = onDismiss,
                onReset = { viewModel.resetEqualizer() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Visualizer
            if (equalizerState.isVisualizerEnabled) {
                AudioVisualizer(
                    data = equalizerState.visualizerData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Presets
            EqualizerPresets(
                presets = equalizerState.presets,
                currentPreset = equalizerState.currentPreset,
                onPresetSelected = { viewModel.applyPreset(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Equalizer Bands
            EqualizerBands(
                bands = equalizerState.bands,
                isEnabled = equalizerState.isEnabled,
                onBandLevelChanged = { bandIndex, level ->
                    viewModel.setBandLevel(bandIndex, level)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Custom Preset Button
            if (equalizerState.currentPreset == null && equalizerState.bands.any { it.level != 0 }) {
                Button(
                    onClick = {
                        viewModel.saveCustomPreset("Custom ${System.currentTimeMillis()}", "User created preset")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00BCD4)
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save as Custom Preset")
                }
            }
        }
    }
}

@Composable
private fun EqualizerHeader(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Equalizer",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00BCD4)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onReset) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color.White
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00BCD4),
                    checkedTrackColor = Color(0xFF00BCD4).copy(alpha = 0.5f)
                )
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun EqualizerPresets(
    presets: List<EqualizerPreset>,
    currentPreset: EqualizerPreset?,
    onPresetSelected: (EqualizerPreset) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(presets) { preset ->
            PresetChip(
                preset = preset,
                isSelected = currentPreset?.id == preset.id,
                onClick = { onPresetSelected(preset) }
            )
        }
    }
}

@Composable
private fun PresetChip(
    preset: EqualizerPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = preset.name,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (preset.isCustom) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Custom",
                    tint = if (isSelected) Color.Black else Color(0xFFFFC107),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun EqualizerBands(
    bands: List<EqualizerBand>,
    isEnabled: Boolean,
    onBandLevelChanged: (Int, Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        bands.forEachIndexed { index, band ->
            EqualizerBandSlider(
                band = band,
                isEnabled = isEnabled,
                onLevelChanged = { level ->
                    onBandLevelChanged(index, level)
                }
            )
        }
    }
}

@Composable
private fun EqualizerBandSlider(
    band: EqualizerBand,
    isEnabled: Boolean,
    onLevelChanged: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(40.dp)
    ) {
        // Level display
        Text(
            text = "${band.level / 100}",
            color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        
        // Vertical slider
        Box(
            modifier = Modifier
                .height(200.dp)
                .width(20.dp)
        ) {
            VerticalSlider(
                value = band.level.toFloat(),
                onValueChange = { onLevelChanged(it.toInt()) },
                valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                enabled = isEnabled,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Frequency label
        Text(
            text = band.frequencyLabel,
            color = if (isEnabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f),
            fontSize = 8.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    
    Canvas(
        modifier = modifier
            .pointerInput(enabled) {
                if (enabled) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false }
                    ) { _, dragAmount ->
                        val heightPx = size.height
                        val rangeSize = valueRange.endInclusive - valueRange.start
                        val deltaValue = -dragAmount.y / heightPx * rangeSize
                        val newValue = (value + deltaValue).coerceIn(valueRange.start, valueRange.endInclusive)
                        onValueChange(newValue)
                    }
                }
            }
    ) {
        val trackWidth = 4.dp.toPx()
        val thumbRadius = if (isDragging) 8.dp.toPx() else 6.dp.toPx()
        
        val trackColor = if (enabled) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)
        val activeColor = if (enabled) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.3f)
        
        // Track background
        drawRect(
            color = trackColor,
            topLeft = Offset((size.width - trackWidth) / 2, 0f),
            size = androidx.compose.ui.geometry.Size(trackWidth, size.height)
        )
        
        // Active track
        val normalizedValue = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        val thumbY = size.height * (1f - normalizedValue)
        val centerY = size.height / 2
        
        if (value > 0) {
            // Positive values (above center)
            drawRect(
                color = activeColor,
                topLeft = Offset((size.width - trackWidth) / 2, thumbY),
                size = androidx.compose.ui.geometry.Size(trackWidth, centerY - thumbY)
            )
        } else if (value < 0) {
            // Negative values (below center)
            drawRect(
                color = activeColor,
                topLeft = Offset((size.width - trackWidth) / 2, centerY),
                size = androidx.compose.ui.geometry.Size(trackWidth, thumbY - centerY)
            )
        }
        
        // Center line
        drawRect(
            color = Color.White.copy(alpha = 0.5f),
            topLeft = Offset((size.width - trackWidth) / 2 - 2.dp.toPx(), centerY - 1.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(trackWidth + 4.dp.toPx(), 2.dp.toPx())
        )
        
        // Thumb
        drawCircle(
            color = activeColor,
            radius = thumbRadius,
            center = Offset(size.width / 2, thumbY)
        )
    }
}

@Composable
private fun AudioVisualizer(
    data: List<Float>,
    modifier: Modifier = Modifier
) {
    val animatedData by animateFloatAsState(
        targetValue = if (data.isNotEmpty()) data.average().toFloat() else 0f,
        animationSpec = tween(100)
    )
    
    Canvas(modifier = modifier) {
        val barCount = 32
        val barWidth = size.width / barCount
        val maxHeight = size.height
        
        repeat(barCount) { index ->
            val height: Float = if (data.isNotEmpty()) {
                val dataIndex = (index * data.size / barCount).coerceIn(0, data.size - 1)
                data[dataIndex] * maxHeight
            } else {
                ((sin(index * 0.5 + animatedData) * 0.3 + 0.7) * maxHeight * 0.3).toFloat()
            }
            
            val x = index * barWidth
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF00BCD4),
                    Color(0xFF00BCD4).copy(alpha = 0.7f),
                    Color(0xFF00BCD4).copy(alpha = 0.3f)
                )
            )
            
            drawRect(
                brush = gradient,
                topLeft = Offset(x, maxHeight - height),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, height)
            )
        }
    }
}