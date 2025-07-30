package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.feature.playback.ABRepeatManager
import com.astralplayer.nextplayer.ui.components.*
import kotlinx.coroutines.flow.collectAsState

/**
 * A-B Repeat Dialog
 * Allows users to set loop points for repeating a section of video
 */
@Composable
fun ABRepeatDialog(
    abRepeatManager: ABRepeatManager,
    currentPosition: Long,
    onDismiss: () -> Unit
) {
    val isEnabled by abRepeatManager.isEnabled.collectAsState()
    val pointA by abRepeatManager.pointA.collectAsState()
    val pointB by abRepeatManager.pointB.collectAsState()
    
    var tempPointA by remember { mutableStateOf(pointA) }
    var tempPointB by remember { mutableStateOf(pointB) }
    
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
            containerColor = MaterialTheme.colorScheme.surface
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
                            Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        
                        Column {
                            Text(
                                text = "A-B Repeat",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (isEnabled) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    BubbleIconButton(
                        onClick = onDismiss,
                        icon = Icons.Default.Close,
                        size = 32,
                        iconSize = 18
                    )
                }
                
                // A-B Points
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Point A
                    PointSelector(
                        label = "Point A",
                        time = tempPointA,
                        currentPosition = currentPosition,
                        onSetPoint = { 
                            tempPointA = currentPosition
                            abRepeatManager.setPointA(currentPosition)
                        },
                        onClearPoint = {
                            tempPointA = null
                            abRepeatManager.clearPointA()
                        },
                        isStart = true
                    )
                    
                    // Visual indicator
                    if (tempPointA != null && tempPointB != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Point B
                    PointSelector(
                        label = "Point B",
                        time = tempPointB,
                        currentPosition = currentPosition,
                        onSetPoint = { 
                            tempPointB = currentPosition
                            abRepeatManager.setPointB(currentPosition)
                        },
                        onClearPoint = {
                            tempPointB = null
                            abRepeatManager.clearPointB()
                        },
                        isStart = false
                    )
                }
                
                // Info text
                if (tempPointA != null && tempPointB != null && tempPointB!! > tempPointA!!) {
                    val duration = tempPointB!! - tempPointA!!
                    Text(
                        text = "Loop duration: ${formatTime(duration)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Enable/Disable button
                BubbleButton(
                    onClick = {
                        if (isEnabled) {
                            abRepeatManager.disable()
                        } else {
                            if (tempPointA != null && tempPointB != null && tempPointB!! > tempPointA!!) {
                                abRepeatManager.enable()
                            }
                        }
                    },
                    text = if (isEnabled) "Disable A-B Repeat" else "Enable A-B Repeat",
                    icon = if (isEnabled) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = if (isEnabled) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isEnabled) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    enabled = !isEnabled || (tempPointA != null && tempPointB != null && tempPointB!! > tempPointA!!)
                )
                
                // Clear all button
                if (tempPointA != null || tempPointB != null) {
                    BubbleButton(
                        onClick = {
                            abRepeatManager.clear()
                            tempPointA = null
                            tempPointB = null
                        },
                        text = "Clear All",
                        icon = Icons.Default.Clear,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PointSelector(
    label: String,
    time: Long?,
    currentPosition: Long,
    onSetPoint: () -> Unit,
    onClearPoint: () -> Unit,
    isStart: Boolean
) {
    BubbleCard(
        elevation = 2,
        cornerRadius = 16,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isStart) Icons.Default.Flag else Icons.Default.FlagCircle,
                        contentDescription = null,
                        tint = if (time != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (time != null) {
                    Text(
                        text = formatTime(time),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Not set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (time != null) {
                    BubbleIconButton(
                        onClick = onClearPoint,
                        icon = Icons.Default.Clear,
                        size = 36,
                        iconSize = 18,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                }
                
                BubbleButton(
                    onClick = onSetPoint,
                    text = if (time != null) "Update" else "Set",
                    cornerRadius = 12,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}