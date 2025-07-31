package com.astralplayer.nextplayer.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astralplayer.nextplayer.data.ABRepeatManager

@Composable
fun ABRepeatDialog(
    abRepeatState: ABRepeatManager.ABRepeatState,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClear: () -> Unit,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    formattedPointA: String?,
    formattedPointB: String?
) {
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
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "A-B Repeat",
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Status indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (abRepeatState.isEnabled) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = null,
                            tint = if (abRepeatState.isEnabled)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (abRepeatState.isEnabled) "A-B Repeat Active" else "A-B Repeat Inactive",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Point A and Point B display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Point A
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Point A",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (abRepeatState.currentPoint == ABRepeatManager.ABPoint.A)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Text(
                                text = formattedPointA ?: "--:--",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    // Arrow
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .align(Alignment.CenterVertically),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Point B
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Point B",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (abRepeatState.currentPoint == ABRepeatManager.ABPoint.B)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Text(
                                text = formattedPointB ?: "--:--",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Instructions
                if (abRepeatState.isSettingPoint) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = when (abRepeatState.currentPoint) {
                                ABRepeatManager.ABPoint.A -> "Navigate to start position and click 'Set Point A'"
                                ABRepeatManager.ABPoint.B -> "Navigate to end position and click 'Set Point B'"
                                ABRepeatManager.ABPoint.NONE -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (abRepeatState.isEnabled && abRepeatState.isSettingPoint) {
                        when (abRepeatState.currentPoint) {
                            ABRepeatManager.ABPoint.A -> {
                                Button(
                                    onClick = onSetPointA,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Set Point A")
                                }
                            }
                            ABRepeatManager.ABPoint.B -> {
                                Button(
                                    onClick = onSetPointB,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Stop,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Set Point B")
                                }
                            }
                            ABRepeatManager.ABPoint.NONE -> {}
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle button
                    OutlinedButton(
                        onClick = onToggle,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (abRepeatState.isEnabled) 
                                Icons.Filled.RepeatOn else Icons.Filled.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (abRepeatState.isEnabled) "Disable" else "Enable")
                    }
                    
                    // Clear button
                    if (abRepeatState.pointA != null || abRepeatState.pointB != null) {
                        OutlinedButton(
                            onClick = onClear,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }
}