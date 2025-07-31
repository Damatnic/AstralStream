package com.astralplayer.nextplayer.models

enum class VideoQuality {
    AUTO,
    SD_360P,
    SD_480P,
    HD_720P,
    HD_1080P,
    UHD_4K,
    UHD_8K
}

enum class PlaybackSpeed(val value: Float) {
    SPEED_0_25X(0.25f),
    SPEED_0_5X(0.5f),
    SPEED_0_75X(0.75f),
    SPEED_1X(1.0f),
    SPEED_1_25X(1.25f),
    SPEED_1_5X(1.5f),
    SPEED_2X(2.0f)
}

enum class AspectRatio {
    ASPECT_RATIO_DEFAULT,
    ASPECT_RATIO_16_9,
    ASPECT_RATIO_4_3,
    ASPECT_RATIO_SQUARE,
    ASPECT_RATIO_FILL,
    ASPECT_RATIO_STRETCH
}

enum class DecoderType {
    HARDWARE,
    SOFTWARE,
    AUTO
}