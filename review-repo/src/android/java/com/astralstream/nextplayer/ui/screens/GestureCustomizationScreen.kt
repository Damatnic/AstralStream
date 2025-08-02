// GestureCustomizationScreen.kt
package com.astralstream.nextplayer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astralstream.nextplayer.viewmodels.GestureCustomizationViewModel
import com.astralstream.nextplayer.ui.components.GestureZoneVisualizer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureCustomizationScreen(
    onNavigateBack: () -> Unit,
    viewModel: GestureCustomizationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gesture Customization") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.resetToDefaults()
                            }
                        }
                    ) {
                        Text("Reset")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Visual Zone Mapper
            GestureZoneVisualizer(
                selectedZone = state.selectedZone,
                onZoneSelected = { zone ->
                    viewModel.selectZone(zone)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp)
            )
            
            // Selected Zone Configuration
            state.selectedZone?.let { zone ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Configure ${zone.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Gesture Type Selection
                        Text(
                            text = "Gesture Type",
                            style = MaterialTheme.typography.labelLarge
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GestureType.values().forEach { type ->
                                FilterChip(
                                    selected = state.zoneGestures[zone] == type,
                                    onClick = {
                                        viewModel.setZoneGesture(zone, type)
                                    },
                                    label = { Text(type.displayName) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Action Selection
                        Text(
                            text = "Action",
                            style = MaterialTheme.typography.labelLarge
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(GestureAction.values()) { action ->
                                FilterChip(
                                    selected = state.gestureActions[
                                        state.zoneGestures[zone] ?: GestureType.TAP
                                    ] == action,
                                    onClick = {
                                        viewModel.setGestureAction(
                                            state.zoneGestures[zone] ?: GestureType.TAP,
                                            action
                                        )
                                    },
                                    label = { Text(action.displayName) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Sensitivity Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Sensitivity Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Swipe Sensitivity
                    SensitivitySlider(
                        label = "Swipe Sensitivity",
                        value = state.swipeSensitivity,
                        onValueChange = { viewModel.setSwipeSensitivity(it) }
                    )
                    
                    // Long Press Duration
                    SensitivitySlider(
                        label = "Long Press Duration",
                        value = state.longPressDuration,
                        onValueChange = { viewModel.setLongPressDuration(it) },
                        valueRange = 200f..2000f,
                        unit = "ms"
                    )
                    
                    // Double Tap Speed
                    SensitivitySlider(
                        label = "Double Tap Speed",
                        value = state.doubleTapSpeed,
                        onValueChange = { viewModel.setDoubleTapSpeed(it) },
                        valueRange = 100f..500f,
                        unit = "ms"
                    )
                }
            }
            
            // Gesture Recording
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Custom Gesture Recording",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (state.isRecording) {
                        CircularProgressIndicator()
                        Text(
                            text = "Recording gesture...",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startRecording() },
                            enabled = !state.isRecording
                        ) {
                            Icon(Icons.Default.FiberManualRecord, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Record")
                        }
                        
                        Button(
                            onClick = { viewModel.stopRecording() },
                            enabled = state.isRecording
                        ) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop")
                        }
                    }
                }
            }
            
            // Import/Export
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { viewModel.exportProfile() }
                ) {
                    Icon(Icons.Default.Upload, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Profile")
                }
                
                OutlinedButton(
                    onClick = { viewModel.importProfile() }
                ) {
                    Icon(Icons.Default.Download, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Profile")
                }
            }
        }
    }
}

@Composable
fun GestureZoneVisualizer(
    selectedZone: GestureZone?,
    onZoneSelected: (GestureZone) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    ) {
        // Create 9-zone grid
        val zones = listOf(
            GestureZone.TOP_LEFT to Alignment.TopStart,
            GestureZone.TOP_CENTER to Alignment.TopCenter,
            GestureZone.TOP_RIGHT to Alignment.TopEnd,
            GestureZone.MIDDLE_LEFT to Alignment.CenterStart,
            GestureZone.MIDDLE_CENTER to Alignment.Center,
            GestureZone.MIDDLE_RIGHT to Alignment.CenterEnd,
            GestureZone.BOTTOM_LEFT to Alignment.BottomStart,
            GestureZone.BOTTOM_CENTER to Alignment.BottomCenter,
            GestureZone.BOTTOM_RIGHT to Alignment.BottomEnd
        )
        
        zones.forEach { (zone, alignment) ->
            Box(
                modifier = Modifier
                    .fillMaxSize(0.33f)
                    .align(alignment)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selectedZone == zone) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        } else {
                            Color.White.copy(alpha = 0.1f)
                        }
                    )
                    .border(
                        width = if (selectedZone == zone) 2.dp else 1.dp,
                        color = if (selectedZone == zone) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onZoneSelected(zone) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = zone.shortName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (selectedZone == zone) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun SensitivitySlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0.1f..2.0f,
    unit: String = "x"
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${String.format("%.1f", value)}$unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

enum class GestureZone(val displayName: String, val shortName: String) {
    TOP_LEFT("Top Left", "TL"),
    TOP_CENTER("Top Center", "TC"),
    TOP_RIGHT("Top Right", "TR"),
    MIDDLE_LEFT("Middle Left", "ML"),
    MIDDLE_CENTER("Middle Center", "MC"),
    MIDDLE_RIGHT("Middle Right", "MR"),
    BOTTOM_LEFT("Bottom Left", "BL"),
    BOTTOM_CENTER("Bottom Center", "BC"),
    BOTTOM_RIGHT("Bottom Right", "BR")
}

enum class GestureType(val displayName: String) {
    TAP("Tap"),
    DOUBLE_TAP("Double Tap"),
    LONG_PRESS("Long Press"),
    SWIPE_UP("Swipe Up"),
    SWIPE_DOWN("Swipe Down"),
    SWIPE_LEFT("Swipe Left"),
    SWIPE_RIGHT("Swipe Right")
}

enum class GestureAction(val displayName: String) {
    PLAY_PAUSE("Play/Pause"),
    SEEK_FORWARD("Seek Forward"),
    SEEK_BACKWARD("Seek Backward"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
    BRIGHTNESS_UP("Brightness Up"),
    BRIGHTNESS_DOWN("Brightness Down"),
    NEXT_VIDEO("Next Video"),
    PREVIOUS_VIDEO("Previous Video"),
    TOGGLE_FULLSCREEN("Toggle Fullscreen"),
    SHOW_INFO("Show Info"),
    TOGGLE_SUBTITLES("Toggle Subtitles"),
    CHANGE_SPEED("Change Speed"),
    SCREENSHOT("Screenshot"),
    LOCK_CONTROLS("Lock Controls"),
    NONE("None")
}