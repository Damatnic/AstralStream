package com.astralplayer.astralstream.feature.player.extensions

import com.astralplayer.astralstream.core.model.FastSeek
import com.astralplayer.astralstream.core.model.PlayerPreferences

fun PlayerPreferences.shouldFastSeek(duration: Long): Boolean {
    return when (fastSeek) {
        FastSeek.ENABLE -> true
        FastSeek.DISABLE -> false
        FastSeek.AUTO -> duration >= minDurationForFastSeek
    }
}
