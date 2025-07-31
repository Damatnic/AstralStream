package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speedOptions = listOf(
        0.25f to "0.25x",
        0.5f to "0.5x",
        0.75f to "0.75x",
        1.0f to "Normal",
        1.25f to "1.25x",
        1.5f to "1.5x",
        1.75f to "1.75x",
        2.0f to "2x"
    )
    
    var selectedSpeed by remember { mutableStateOf(currentSpeed) }
    var customSpeed by remember { mutableStateOf(currentSpeed.toString()) }
    var showCustomSpeed by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Speed options
                LazyColumn {
                    items(speedOptions) { (speed, label) ->
                        SpeedOption(
                            speed = speed,
                            label = label,
                            isSelected = selectedSpeed == speed,
                            onClick = {
                                selectedSpeed = speed
                                onSpeedSelected(speed)
                            }
                        )
                    }
                    
                    // Custom speed option
                    item {
                        SpeedOption(
                            speed = -1f,
                            label = "Custom",
                            isSelected = showCustomSpeed,
                            onClick = {
                                showCustomSpeed = !showCustomSpeed
                            }
                        )
                    }
                }
                
                // Custom speed input
                if (showCustomSpeed) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customSpeed,
                            onValueChange = { customSpeed = it },
                            label = { Text("Speed (0.25 - 4.0)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                val speed = customSpeed.toFloatOrNull()
                                if (speed != null && speed in 0.25f..4.0f) {
                                    selectedSpeed = speed
                                    onSpeedSelected(speed)
                                }
                            }
                        ) {
                            Text("Apply")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Current speed indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Current: ${String.format("%.2fx", selectedSpeed)}",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedOption(
    speed: Float,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        
        if (isSelected && speed != -1f) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}