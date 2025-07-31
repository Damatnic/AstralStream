package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Control lock overlay component for video player
 * Prevents accidental touches during playback
 */
@Composable
fun ControlLockOverlay(
    isLocked: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLocked,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            // Unlock button in top-left corner
            FloatingActionButton(
                onClick = onUnlock,
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.TopStart)
                    .size(48.dp),
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Unlock controls",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Lock button component to add to player controls
 */
@Composable
fun LockControlButton(
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onLock,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.LockOpen,
            contentDescription = "Lock controls",
            tint = Color.White
        )
    }
}