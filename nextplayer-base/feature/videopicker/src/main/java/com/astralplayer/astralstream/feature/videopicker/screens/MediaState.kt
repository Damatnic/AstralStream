package com.astralplayer.astralstream.feature.videopicker.screens

import com.astralplayer.astralstream.core.model.Folder

sealed interface MediaState {
    data object Loading : MediaState
    data class Success(val data: Folder?) : MediaState
}
