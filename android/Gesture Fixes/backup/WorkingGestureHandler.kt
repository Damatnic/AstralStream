package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel

// Minimal gesture handler for build compatibility
@Composable
fun WorkingGestureHandler(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel,
    gestureSettings: GestureSettings = GestureSettings(),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    if (gestureSettings.tapToToggleControls) {
                        viewModel.toggleControlsVisibility()
                    }
                }
            )
        }
    ) {
        content()
    }
}