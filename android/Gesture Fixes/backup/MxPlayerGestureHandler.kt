package com.astralplayer.nextplayer.feature.player.gestures

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MxPlayerGestureHandler(
    viewModel: PlayerViewModel,
    onGestureStart: () -> Unit = {},
    onGestureEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var tapCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val currentTime = System.currentTimeMillis()
                        val screenWidth = size.width
                        
                        // Reset tap count if too much time has passed
                        if (currentTime - lastTapTime > 500) {
                            tapCount = 0
                        }
                        
                        tapCount++
                        lastTapTime = currentTime
                        
                        when (tapCount) {
                            1 -> {
                                // Single tap - toggle controls
                                onGestureStart()
                                viewModel.toggleControlsVisibility()
                                
                                // Reset tap count after delay if no more taps
                                scope.launch {
                                    delay(500)
                                    if (currentTime == lastTapTime) {
                                        tapCount = 0
                                    }
                                }
                            }
                            2 -> {
                                // Double tap - seek based on screen position
                                onGestureStart()
                                val isLeftSide = offset.x < screenWidth / 2
                                if (isLeftSide) {
                                    viewModel.seekBackward()
                                } else {
                                    viewModel.seekForward()
                                }
                                
                                scope.launch {
                                    delay(300)
                                    onGestureEnd()
                                    tapCount = 0
                                }
                            }
                            3 -> {
                                // Triple tap - toggle screen lock
                                onGestureStart()
                                viewModel.toggleScreenLock()
                                tapCount = 0
                                
                                scope.launch {
                                    delay(300)
                                    onGestureEnd()
                                }
                            }
                        }
                    }
                )
            }
    )
}