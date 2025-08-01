package com.astralplayer.features.gestures.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.features.gestures.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureEditDialog(
    gesture: GestureEntity,
    onDismiss: () -> Unit,
    onSave: (GestureEntity) -> Unit
) {
    var selectedAction by remember { mutableStateOf(gesture.action) }
    var sensitivity by remember { mutableStateOf(gesture.sensitivity) }
    var hapticFeedback by remember { mutableStateOf(gesture.hapticFeedback) }
    var visualFeedback by remember { mutableStateOf(gesture.visualFeedback) }
    var minimumDistance by remember { mutableStateOf(gesture.minimumDistance) }
    var longPressTimeout by remember { mutableStateOf(gesture.longPressTimeout) }
    var doubleTapTimeout by remember { mutableStateOf(gesture.doubleTapTimeout) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Edit Gesture",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${formatGestureType(gesture.gestureType)} in ${formatZone(gesture.zone)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Divider()
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Gesture visualization
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GestureIcon(
                                gestureType = gesture.gestureType,
                                modifier = Modifier.size(64.dp)
                            )
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Zone Preview",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                ZonePreview(
                                    zone = gesture.zone,
                                    modifier = Modifier.size(80.dp, 60.dp)
                                )
                            }
                        }
                    }
                    
                    // Action selection
                    var actionExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = actionExpanded,
                        onExpandedChange = { actionExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = formatAction(selectedAction),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Action") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = actionExpanded,
                            onDismissRequest = { actionExpanded = false }
                        ) {
                            GestureAction.values()
                                .filter { it != GestureAction.NONE }
                                .groupBy { getActionCategory(it) }
                                .forEach { (category, actions) ->
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                    actions.forEach { action ->
                                        DropdownMenuItem(
                                            text = { Text(formatAction(action)) },
                                            onClick = {
                                                selectedAction = action
                                                actionExpanded = false
                                            }
                                        )
                                    }
                                }
                        }
                    }
                    
                    // Sensitivity slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Sensitivity",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${(sensitivity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = sensitivity,
                            onValueChange = { sensitivity = it },
                            valueRange = 0.5f..2.0f,
                            steps = 15
                        )
                        Text(
                            text = "Adjust how responsive the gesture is",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Gesture-specific settings
                    when (gesture.gestureType) {
                        GestureType.SWIPE_UP, GestureType.SWIPE_DOWN,
                        GestureType.SWIPE_LEFT, GestureType.SWIPE_RIGHT -> {
                            Column {
                                Text(
                                    text = "Minimum Distance: ${minimumDistance.toInt()} pixels",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Slider(
                                    value = minimumDistance,
                                    onValueChange = { minimumDistance = it },
                                    valueRange = 20f..200f,
                                    steps = 17
                                )
                            }
                        }
                        
                        GestureType.LONG_PRESS -> {
                            Column {
                                Text(
                                    text = "Long Press Duration: ${longPressTimeout.toInt()} ms",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Slider(
                                    value = longPressTimeout.toFloat(),
                                    onValueChange = { longPressTimeout = it.toLong() },
                                    valueRange = 200f..2000f,
                                    steps = 17
                                )
                            }
                        }
                        
                        GestureType.DOUBLE_TAP -> {
                            Column {
                                Text(
                                    text = "Double Tap Speed: ${doubleTapTimeout.toInt()} ms",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Slider(
                                    value = doubleTapTimeout.toFloat(),
                                    onValueChange = { doubleTapTimeout = it.toLong() },
                                    valueRange = 100f..500f,
                                    steps = 7
                                )
                            }
                        }
                        
                        else -> { /* No specific settings */ }
                    }
                    
                    // Feedback settings
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Feedback",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Haptic Feedback")
                                Switch(
                                    checked = hapticFeedback,
                                    onCheckedChange = { hapticFeedback = it }
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Visual Feedback")
                                Switch(
                                    checked = visualFeedback,
                                    onCheckedChange = { visualFeedback = it }
                                )
                            }
                        }
                    }
                    
                    // Tips
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = getGestureTip(gesture.gestureType),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                
                Divider()
                
                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(
                                gesture.copy(
                                    action = selectedAction,
                                    sensitivity = sensitivity,
                                    hapticFeedback = hapticFeedback,
                                    visualFeedback = visualFeedback,
                                    minimumDistance = minimumDistance,
                                    longPressTimeout = longPressTimeout,
                                    doubleTapTimeout = doubleTapTimeout,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ZonePreview(
    zone: GestureZone,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Draw zone highlight
            val zoneModifier = when (zone) {
                GestureZone.FULL_SCREEN -> Modifier.fillMaxSize()
                GestureZone.LEFT_HALF -> Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterStart)
                GestureZone.RIGHT_HALF -> Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterEnd)
                GestureZone.TOP_HALF -> Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.TopCenter)
                GestureZone.BOTTOM_HALF -> Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.BottomCenter)
                GestureZone.CENTER -> Modifier.fillMaxSize(0.6f).align(Alignment.Center)
                GestureZone.TOP_LEFT_QUARTER -> Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.5f).align(Alignment.TopStart)
                GestureZone.TOP_RIGHT_QUARTER -> Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.5f).align(Alignment.TopEnd)
                GestureZone.BOTTOM_LEFT_QUARTER -> Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.5f).align(Alignment.BottomStart)
                GestureZone.BOTTOM_RIGHT_QUARTER -> Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.5f).align(Alignment.BottomEnd)
                else -> Modifier.fillMaxSize()
            }
            
            Surface(
                modifier = zoneModifier,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ) {}
        }
    }
}

private fun getActionCategory(action: GestureAction): String {
    return when (action) {
        GestureAction.PLAY_PAUSE, GestureAction.SEEK_FORWARD, 
        GestureAction.SEEK_BACKWARD, GestureAction.FAST_FORWARD,
        GestureAction.REWIND, GestureAction.NEXT_VIDEO,
        GestureAction.PREVIOUS_VIDEO, GestureAction.JUMP_TO_BEGINNING,
        GestureAction.JUMP_TO_END -> "Playback"
        
        GestureAction.VOLUME_UP, GestureAction.VOLUME_DOWN,
        GestureAction.MUTE_UNMUTE -> "Volume"
        
        GestureAction.BRIGHTNESS_UP, GestureAction.BRIGHTNESS_DOWN,
        GestureAction.AUTO_BRIGHTNESS -> "Brightness"
        
        GestureAction.SPEED_UP, GestureAction.SPEED_DOWN,
        GestureAction.RESET_SPEED, GestureAction.CYCLE_SPEED_PRESETS -> "Speed"
        
        GestureAction.TOGGLE_SUBTITLES, GestureAction.NEXT_SUBTITLE_TRACK,
        GestureAction.PREVIOUS_SUBTITLE_TRACK, GestureAction.SUBTITLE_DELAY_INCREASE,
        GestureAction.SUBTITLE_DELAY_DECREASE, GestureAction.SUBTITLE_SIZE_INCREASE,
        GestureAction.SUBTITLE_SIZE_DECREASE -> "Subtitles"
        
        GestureAction.NEXT_AUDIO_TRACK, GestureAction.PREVIOUS_AUDIO_TRACK,
        GestureAction.AUDIO_DELAY_INCREASE, GestureAction.AUDIO_DELAY_DECREASE -> "Audio"
        
        GestureAction.SHOW_HIDE_CONTROLS, GestureAction.TOGGLE_FULLSCREEN,
        GestureAction.TOGGLE_PIP, GestureAction.OPEN_SETTINGS,
        GestureAction.OPEN_PLAYLIST, GestureAction.TOGGLE_LOCK,
        GestureAction.SCREENSHOT -> "UI"
        
        GestureAction.ZOOM_IN, GestureAction.ZOOM_OUT,
        GestureAction.RESET_ZOOM, GestureAction.FIT_TO_SCREEN,
        GestureAction.FILL_SCREEN -> "Zoom"
        
        GestureAction.NEXT_CHAPTER, GestureAction.PREVIOUS_CHAPTER,
        GestureAction.SHOW_CHAPTERS -> "Chapters"
        
        GestureAction.CUSTOM_ACTION_1, GestureAction.CUSTOM_ACTION_2,
        GestureAction.CUSTOM_ACTION_3 -> "Custom"
        
        GestureAction.NONE -> "None"
    }
}

private fun getGestureTip(gestureType: GestureType): String {
    return when (gestureType) {
        GestureType.TAP -> "Single tap is best for frequently used actions like play/pause"
        GestureType.DOUBLE_TAP -> "Double tap works well for seeking or skipping content"
        GestureType.LONG_PRESS -> "Long press is ideal for actions that need confirmation"
        GestureType.SWIPE_UP, GestureType.SWIPE_DOWN -> "Vertical swipes are perfect for volume and brightness"
        GestureType.SWIPE_LEFT, GestureType.SWIPE_RIGHT -> "Horizontal swipes work best for seeking through content"
        GestureType.PINCH_IN, GestureType.PINCH_OUT -> "Pinch gestures are intuitive for zoom controls"
        GestureType.TWO_FINGER_TAP -> "Multi-finger taps can trigger advanced features"
        else -> "Customize this gesture to fit your viewing preferences"
    }
}