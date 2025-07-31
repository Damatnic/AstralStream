package com.astralplayer.nextplayer.models

sealed class PlayerEvent {
    object PlaybackStarted : PlayerEvent()
    object PlaybackPaused : PlayerEvent()
    object PlaybackResumed : PlayerEvent()
    object PlaybackCompleted : PlayerEvent()
    data class PlaybackError(val error: String) : PlayerEvent()
    data class PlaybackProgress(val position: Long, val duration: Long) : PlayerEvent()
    data class BufferingUpdate(val percent: Int) : PlayerEvent()
}

sealed class UIEvent {
    object ShowControls : UIEvent()
    object HideControls : UIEvent()
    object ToggleFullscreen : UIEvent()
    object ShowSubtitleOptions : UIEvent()
    object ShowQualityOptions : UIEvent()
    object ShowSpeedOptions : UIEvent()
    data class ShowError(val message: String) : UIEvent()
}