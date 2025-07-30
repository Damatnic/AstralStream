package com.astralplayer.nextplayer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data models for video-related functionality
 */

/**
 * Represents a video file with metadata
 */
@Parcelize
data class VideoMetadata(
    val id: Long = 0,
    val title: String = "",
    val path: String = "",
    val mimeType: String = "",
    val size: Long = 0,
    val duration: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val thumbnailPath: String? = null,
    val lastPlayedPosition: Long = 0,
    val lastPlayedDate: Long = 0,
    val isFavorite: Boolean = false
) : Parcelable

/**
 * Represents a video playlist
 */
@Parcelize
data class VideoPlaylist(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val dateCreated: Long = 0,
    val dateModified: Long = 0,
    val videoIds: List<Long> = emptyList()
) : Parcelable

/**
 * Represents playback settings for a video
 */
@Parcelize
data class PlaybackSettings(
    val videoId: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val brightness: Float = 0.5f,
    val subtitleEnabled: Boolean = true,
    val subtitleLanguage: String = "en",
    val audioTrack: Int = 0,
    val aspectRatio: AspectRatio = AspectRatio.FIT
) : Parcelable

/**
 * Enum representing different aspect ratios for video playback
 */
enum class AspectRatio {
    FIT,
    FILL,
    FIXED_WIDTH,
    FIXED_HEIGHT,
    STRETCH,
    CUSTOM
}

/**
 * Represents a subtitle track
 */
@Parcelize
data class SubtitleTrack(
    val id: Int = 0,
    val language: String = "",
    val name: String = "",
    val path: String = "",
    val format: SubtitleFormat = SubtitleFormat.SRT,
    val isExternal: Boolean = false,
    val isEnabled: Boolean = false
) : Parcelable

/**
 * Enum representing different subtitle formats
 */
enum class SubtitleFormat {
    SRT,
    VTT,
    ASS,
    SSA,
    TTML,
    UNKNOWN
}

/**
 * Represents a video folder
 */
@Parcelize
data class VideoFolder(
    val id: Long = 0,
    val name: String = "",
    val path: String = "",
    val videoCount: Int = 0,
    val thumbnailPath: String? = null
) : Parcelable