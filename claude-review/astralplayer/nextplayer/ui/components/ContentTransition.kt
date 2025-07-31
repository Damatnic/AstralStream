package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ContentTransition(
    targetState: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = targetState,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(),
        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(),
        content = content
    )
}