package com.astralplayer.astralstream.data.model

/**
 * Enum representing different video sources
 */
enum class VideoSource {
    LOCAL_FILE,
    HTTP_URL,
    HTTPS_URL,
    STREAMING_HLS,
    STREAMING_DASH,
    STREAMING_SMOOTH,
    STREAMING_RTSP,
    BROWSER_INTENT,
    CONTENT_URI,
    ASSETS,
    RAW_RESOURCE,
    YOUTUBE,
    VIMEO,
    TWITCH,
    OTHER_STREAMING,
    UNKNOWN
}