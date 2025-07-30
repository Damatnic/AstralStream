package com.astralplayer.nextplayer.feature.player.revolutionary

import kotlinx.serialization.Serializable

@Serializable
data class RectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)