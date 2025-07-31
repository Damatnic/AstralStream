package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.data.GestureAction
import com.astralplayer.nextplayer.data.gesture.accessibility.AccessibleGestureInput

/**
 * Accessible gesture control overlay for users who cannot use gestures
 */
@Composable
fun AccessibleGestureControls(
    visible: Boolean,
    accessibleControls: AccessibleGestureInput.AccessibleControls,
    onAction: (GestureAction) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.Black.copy(alpha = 0.8f),
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(16.dp)
                .semantics {
                    contentDescription = "Accessible gesture controls"
                }
        ) {
            // Seek controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AccessibleButton(
                    control = accessibleControls.seekBackward,
                    icon = Icons.Default.FastRewind,
                    onClick = { onAction(accessibleControls.seekBackward.action()) }
                )
                
                AccessibleButton(
                    control = accessibleControls.playPause,
                    icon = Icons.Default.PlayArrow,
                    onClick = { onAction(accessibleControls.playPause.action()) },
                    modifier = Modifier.size(80.dp)
                )
                
                AccessibleButton(
                    control = accessibleControls.seekForward,
                    icon = Icons.Default.FastForward,
                    onClick = { onAction(accessibleControls.seekForward.action()) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Volume and brightness controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Volume controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Volume",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Row {
                        AccessibleButton(
                            control = accessibleControls.volumeDown,
                            icon = Icons.Default.VolumeDown,
                            onClick = { onAction(accessibleControls.volumeDown.action()) },
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AccessibleButton(
                            control = accessibleControls.volumeUp,
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            onClick = { onAction(accessibleControls.volumeUp.action()) },
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                // Brightness controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Brightness",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Row {
                        AccessibleButton(
                            control = accessibleControls.brightnessDown,
                            icon = Icons.Default.BrightnessLow,
                            onClick = { onAction(accessibleControls.brightnessDown.action()) },
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AccessibleButton(
                            control = accessibleControls.brightnessUp,
                            icon = Icons.Default.BrightnessHigh,
                            onClick = { onAction(accessibleControls.brightnessUp.action()) },
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual accessible button
 */
@Composable
private fun AccessibleButton(
    control: AccessibleGestureInput.AccessibleControl,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                CircleShape
            )
            .semantics {
                contentDescription = control.contentDescription
                role = Role.Button
                stateDescription = control.label
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Description in semantics
            tint = Color.White,
            modifier = Modifier.fillMaxSize(0.6f)
        )
    }
}

/**
 * Gesture hints for accessibility
 */
@Composable
fun GestureHintsOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .semantics {
                    contentDescription = "Gesture hints and instructions"
                    liveRegion = LiveRegionMode.Polite
                },
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Gesture Controls",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                GestureHintItem(
                    icon = Icons.Default.SwipeRight,
                    text = "Swipe horizontally to seek"
                )
                
                GestureHintItem(
                    icon = Icons.Default.SwipeVertical,
                    text = "Swipe up/down on right for volume"
                )
                
                GestureHintItem(
                    icon = Icons.Default.SwipeVertical,
                    text = "Swipe up/down on left for brightness"
                )
                
                GestureHintItem(
                    icon = Icons.Default.TouchApp,
                    text = "Double tap to skip 10 seconds"
                )
                
                GestureHintItem(
                    icon = Icons.Default.TouchApp,
                    text = "Long press for fast seek"
                )
            }
        }
    }
}

@Composable
private fun GestureHintItem(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * High contrast mode for gesture overlays
 */
@Composable
fun HighContrastGestureOverlay(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = if (enabled) {
            Modifier
                .background(Color.Black)
                .semantics {
                    contentDescription = "High contrast mode enabled"
                }
        } else {
            Modifier
        }
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) Color.White else LocalContentColor.current
        ) {
            content()
        }
    }
}

/**
 * Keyboard navigation support for gestures
 */
@Composable
fun KeyboardGestureSupport(
    onAction: (GestureAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // This would typically be implemented with key event handlers
    // For demonstration, showing the structure
    Box(
        modifier = modifier
            .semantics {
                contentDescription = "Keyboard shortcuts available: " +
                    "Arrow keys for seek, " +
                    "Plus/Minus for volume, " +
                    "B for brightness, " +
                    "Space for play/pause"
            }
    )
}

/**
 * Focus indicators for gesture zones
 */
@Composable
fun GestureZoneFocusIndicators(
    showZones: Boolean,
    modifier: Modifier = Modifier
) {
    if (!showZones) return
    
    Box(modifier = modifier.fillMaxSize()) {
        // Left zone (brightness)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .align(Alignment.CenterStart)
                .background(Color.Blue.copy(alpha = 0.1f))
                .semantics {
                    contentDescription = "Brightness control zone - swipe up or down"
                }
        )
        
        // Center zone (seek and tap)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.4f)
                .align(Alignment.Center)
                .background(Color.Green.copy(alpha = 0.1f))
                .semantics {
                    contentDescription = "Seek and tap control zone - swipe horizontally or tap"
                }
        )
        
        // Right zone (volume)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .align(Alignment.CenterEnd)
                .background(Color.Red.copy(alpha = 0.1f))
                .semantics {
                    contentDescription = "Volume control zone - swipe up or down"
                }
        )
    }
}