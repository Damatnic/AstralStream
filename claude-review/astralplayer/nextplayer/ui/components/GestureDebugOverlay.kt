package com.astralplayer.nextplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.data.*

/**
 * Debug overlay for gesture system
 */
@Composable
fun GestureDebugOverlay(
    gestureState: GestureState,
    settings: EnhancedGestureSettings,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Gesture Debug",
                color = Color.Yellow,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Text(
                text = "Active: ${gestureState.activeGesture?.name ?: "None"}",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            
            if (gestureState.isGestureActive) {
                Text(
                    text = "Pos: (${gestureState.currentGesturePosition.x.toInt()}, ${gestureState.currentGesturePosition.y.toInt()})",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Text(
                    text = "Velocity: ${gestureState.gestureVelocity.x.toInt()}, ${gestureState.gestureVelocity.y.toInt()}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            if (gestureState.conflictingGestures.isNotEmpty()) {
                Text(
                    text = "Conflicts: ${gestureState.conflictingGestures.joinToString()}",
                    color = Color.Red,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}