package com.astralplayer.astralstream.data.model

/**
 * Enum representing different video formats and containers
 */
enum class VideoFormat {
    MP4, AVI, MKV, MOV, WMV, FLV, WEBM, OGV, M4V,
    HLS_M3U8, DASH_MPD, SMOOTH_STREAMING,
    THREEGP, ASF, RM, RMVB,
    H264, H265_HEVC, VP8, VP9, AV1,
    UNKNOWN
}