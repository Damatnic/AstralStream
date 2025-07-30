package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.ui.components.*

/**
 * Sleep Timer Dialog
 * Allows users to set a timer to stop playback after a certain duration
 */
@Composable
fun SleepTimerDialog(
    onTimerSet: (Int) -> Unit, // minutes
    onDismiss: () -> Unit
) {
    val presetMinutes = listOf(
        5 to "5 min",
        10 to "10 min",
        15 to "15 min",
        20 to "20 min",
        30 to "30 min",
        45 to "45 min",
        60 to "1 hour",
        90 to "1.5 hours",
        120 to "2 hours"
    )
    
    var customMinutes by remember { mutableStateOf(30) }
    var showCustomTimer by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        BubbleCard(
            elevation = 16,
            cornerRadius = 24,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        
                        Text(
                            text = "Sleep Timer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    BubbleIconButton(
                        onClick = onDismiss,
                        icon = Icons.Default.Close,
                        size = 32,
                        iconSize = 18
                    )
                }
                
                // Preset times grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(presetMinutes) { (minutes, label) ->
                        TimerOption(
                            label = label,
                            onClick = {
                                onTimerSet(minutes)
                                onDismiss()
                            }
                        )
                    }
                }
                
                // Custom timer option
                BubbleCard(
                    onClick = { showCustomTimer = !showCustomTimer },
                    elevation = if (showCustomTimer) 4 else 2,
                    cornerRadius = 16,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Custom Timer",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            
                            Icon(
                                imageVector = if (showCustomTimer) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        AnimatedVisibility(visible = showCustomTimer) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Custom time picker
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BubbleIconButton(
                                        onClick = { 
                                            if (customMinutes > 5) customMinutes -= 5 
                                        },
                                        icon = Icons.Default.Remove,
                                        size = 40,
                                        iconSize = 20,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    
                                    BubbleCard(
                                        elevation = 2,
                                        cornerRadius = 12,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.width(100.dp)
                                    ) {
                                        Text(
                                            text = "$customMinutes min",
                                            style = MaterialTheme.typography.headlineSmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                    
                                    BubbleIconButton(
                                        onClick = { 
                                            if (customMinutes < 180) customMinutes += 5 
                                        },
                                        icon = Icons.Default.Add,
                                        size = 40,
                                        iconSize = 20,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                                
                                Slider(
                                    value = customMinutes.toFloat(),
                                    onValueChange = { customMinutes = it.toInt() },
                                    valueRange = 5f..180f,
                                    steps = 35,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                BubbleButton(
                                    onClick = {
                                        onTimerSet(customMinutes)
                                        onDismiss()
                                    },
                                    text = "Set Timer",
                                    icon = Icons.Default.Timer,
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
                
                // Cancel timer option
                BubbleButton(
                    onClick = {
                        onTimerSet(0) // 0 means cancel timer
                        onDismiss()
                    },
                    text = "Cancel Timer",
                    icon = Icons.Default.TimerOff,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun TimerOption(
    label: String,
    onClick: () -> Unit
) {
    BubbleCard(
        onClick = onClick,
        elevation = 2,
        cornerRadius = 16,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}