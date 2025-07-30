package com.astralplayer.nextplayer.feature.timer

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingTimeMs: Long = 0L,
    val totalTimeMs: Long = 0L,
    val fadeOutEnabled: Boolean = true,
    val fadeOutDurationMs: Long = 30000L, // 30 seconds
    val isFadingOut: Boolean = false,
    val currentVolume: Float = 1.0f,
    val originalVolume: Float = 1.0f,
    val pauseOnComplete: Boolean = true,
    val showNotification: Boolean = true
)

data class TimerPreset(
    val id: String,
    val name: String,
    val durationMs: Long,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

class SleepTimerManager : ViewModel() {
    
    private val _timerState = MutableStateFlow(SleepTimerState())
    val timerState: StateFlow<SleepTimerState> = _timerState.asStateFlow()
    
    private var timerJob: Job? = null
    private var fadeJob: Job? = null
    
    private val timerPresets = listOf(
        TimerPreset("5min", "5 minutes", 5 * 60 * 1000L, Icons.Default.Timer),
        TimerPreset("10min", "10 minutes", 10 * 60 * 1000L, Icons.Default.Timer),
        TimerPreset("15min", "15 minutes", 15 * 60 * 1000L, Icons.Default.Timer),
        TimerPreset("30min", "30 minutes", 30 * 60 * 1000L, Icons.Default.Timer),
        TimerPreset("45min", "45 minutes", 45 * 60 * 1000L, Icons.Default.Timer),
        TimerPreset("1hour", "1 hour", 60 * 60 * 1000L, Icons.Default.Schedule),
        TimerPreset("1.5hour", "1.5 hours", 90 * 60 * 1000L, Icons.Default.Schedule),
        TimerPreset("2hour", "2 hours", 120 * 60 * 1000L, Icons.Default.Schedule)
    )
    
    fun startTimer(
        durationMs: Long,
        fadeOutEnabled: Boolean = true,
        fadeOutDurationMs: Long = 30000L,
        pauseOnComplete: Boolean = true,
        onVolumeChange: (Float) -> Unit = {},
        onTimerComplete: () -> Unit = {}
    ) {
        cancelTimer()
        
        _timerState.value = _timerState.value.copy(
            isActive = true,
            remainingTimeMs = durationMs,
            totalTimeMs = durationMs,
            fadeOutEnabled = fadeOutEnabled,
            fadeOutDurationMs = fadeOutDurationMs,
            pauseOnComplete = pauseOnComplete,
            originalVolume = _timerState.value.currentVolume
        )
        
        timerJob = viewModelScope.launch {
            var remaining = durationMs
            
            while (remaining > 0 && isActive) {
                delay(1000) // Update every second
                remaining -= 1000
                
                _timerState.value = _timerState.value.copy(
                    remainingTimeMs = max(0, remaining)
                )
                
                // Start fade out if enabled and within fade duration
                if (fadeOutEnabled && remaining <= fadeOutDurationMs && !_timerState.value.isFadingOut) {
                    startFadeOut(remaining, onVolumeChange)
                }
            }
            
            // Timer completed
            if (remaining <= 0) {
                _timerState.value = _timerState.value.copy(
                    isActive = false,
                    remainingTimeMs = 0L,
                    isFadingOut = false
                )
                
                onTimerComplete()
            }
        }
    }
    
    private fun startFadeOut(remainingMs: Long, onVolumeChange: (Float) -> Unit) {
        _timerState.value = _timerState.value.copy(isFadingOut = true)
        
        fadeJob = viewModelScope.launch {
            val fadeStartVolume = _timerState.value.currentVolume
            val fadeDuration = _timerState.value.fadeOutDurationMs
            val updateInterval = 100L // Update every 100ms for smooth fade
            
            var elapsed = 0L
            while (elapsed < fadeDuration && _timerState.value.isActive) {
                delay(updateInterval)
                elapsed += updateInterval
                
                val progress = elapsed.toFloat() / fadeDuration.toFloat()
                val newVolume = fadeStartVolume * (1f - progress)
                
                _timerState.value = _timerState.value.copy(currentVolume = newVolume)
                onVolumeChange(newVolume)
            }
        }
    }
    
    fun cancelTimer() {
        timerJob?.cancel()
        fadeJob?.cancel()
        
        _timerState.value = _timerState.value.copy(
            isActive = false,
            remainingTimeMs = 0L,
            isFadingOut = false,
            currentVolume = _timerState.value.originalVolume
        )
    }
    
    fun extendTimer(additionalMs: Long) {
        if (_timerState.value.isActive) {
            val newRemaining = _timerState.value.remainingTimeMs + additionalMs
            val newTotal = _timerState.value.totalTimeMs + additionalMs
            
            _timerState.value = _timerState.value.copy(
                remainingTimeMs = newRemaining,
                totalTimeMs = newTotal
            )
        }
    }
    
    fun getTimerPresets(): List<TimerPreset> = timerPresets
    
    fun getFormattedTimeRemaining(): String {
        val totalSeconds = _timerState.value.remainingTimeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
    
    fun getProgressPercentage(): Float {
        val total = _timerState.value.totalTimeMs
        val remaining = _timerState.value.remainingTimeMs
        
        return if (total > 0) {
            ((total - remaining).toFloat() / total.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }
    
    override fun onCleared() {
        super.onCleared()
        cancelTimer()
    }
}

@Composable
fun SleepTimerDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onTimerStart: (Long, Boolean, Long, Boolean) -> Unit,
    viewModel: SleepTimerManager = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val timerState by viewModel.timerState.collectAsState()
    
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A2E)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sleep Timer",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00BCD4)
                        )
                        
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (timerState.isActive) {
                        // Active timer display
                        ActiveTimerDisplay(
                            timeRemaining = viewModel.getFormattedTimeRemaining(),
                            progress = viewModel.getProgressPercentage(),
                            isFadingOut = timerState.isFadingOut,
                            currentVolume = timerState.currentVolume,
                            onExtend = { viewModel.extendTimer(15 * 60 * 1000L) }, // Extend by 15 minutes
                            onCancel = { viewModel.cancelTimer() }
                        )
                    } else {
                        // Timer setup
                        TimerSetup(
                            presets = viewModel.getTimerPresets(),
                            onTimerStart = onTimerStart,
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveTimerDisplay(
    timeRemaining: String,
    progress: Float,
    isFadingOut: Boolean,
    currentVolume: Float,
    onExtend: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Circular progress indicator
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(200.dp),
                color = if (isFadingOut) Color(0xFFFF6B35) else Color(0xFF00BCD4),
                strokeWidth = 8.dp,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeRemaining,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                if (isFadingOut) {
                    Text(
                        text = "Fading out...",
                        fontSize = 14.sp,
                        color = Color(0xFFFF6B35)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Volume indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeDown,
                            contentDescription = "Volume",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        
                        LinearProgressIndicator(
                            progress = currentVolume,
                            modifier = Modifier.width(80.dp),
                            color = Color(0xFFFF6B35),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
        
        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Extend button
            Button(
                onClick = onExtend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00BCD4).copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Extend",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("15 min")
            }
            
            // Cancel button
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4444).copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = "Cancel",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun TimerSetup(
    presets: List<TimerPreset>,
    onTimerStart: (Long, Boolean, Long, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf<TimerPreset?>(null) }
    var fadeOutEnabled by remember { mutableStateOf(true) }
    var fadeOutDuration by remember { mutableStateOf(30) } // seconds
    var pauseOnComplete by remember { mutableStateOf(true) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Timer presets
        Text(
            text = "Select Duration",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(presets) { preset ->
                TimerPresetCard(
                    preset = preset,
                    isSelected = selectedPreset?.id == preset.id,
                    onClick = { selectedPreset = preset }
                )
            }
        }
        
        if (selectedPreset != null) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Options
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Fade out option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Fade Out",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Gradually reduce volume",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    
                    Switch(
                        checked = fadeOutEnabled,
                        onCheckedChange = { fadeOutEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00BCD4),
                            checkedTrackColor = Color(0xFF00BCD4).copy(alpha = 0.5f)
                        )
                    )
                }
                
                // Fade duration (if enabled)
                if (fadeOutEnabled) {
                    Column {
                        Text(
                            text = "Fade Duration: ${fadeOutDuration}s",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        
                        Slider(
                            value = fadeOutDuration.toFloat(),
                            onValueChange = { fadeOutDuration = it.toInt() },
                            valueRange = 10f..120f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00BCD4),
                                activeTrackColor = Color(0xFF00BCD4),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
                
                // Pause on complete option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pause on Complete",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Pause playback when timer ends",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    
                    Switch(
                        checked = pauseOnComplete,
                        onCheckedChange = { pauseOnComplete = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00BCD4),
                            checkedTrackColor = Color(0xFF00BCD4).copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Start button
            Button(
                onClick = {
                    selectedPreset?.let { preset ->
                        onTimerStart(
                            preset.durationMs,
                            fadeOutEnabled,
                            fadeOutDuration * 1000L,
                            pauseOnComplete
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00BCD4)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Timer",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TimerPresetCard(
    preset: TimerPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .width(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = preset.icon,
                contentDescription = preset.name,
                tint = if (isSelected) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = preset.name,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SleepTimerIndicator(
    isActive: Boolean,
    timeRemaining: String,
    progress: Float,
    isFadingOut: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isActive,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (isFadingOut) Color(0xFFFF6B35).copy(alpha = 0.9f) else Color(0xFF00BCD4).copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp)
                ) {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(32.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = "Sleep Timer",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Column {
                    Text(
                        text = timeRemaining,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isFadingOut) {
                        Text(
                            text = "Fading out",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}