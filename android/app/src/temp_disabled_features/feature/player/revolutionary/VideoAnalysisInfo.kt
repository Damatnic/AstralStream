package com.astralplayer.nextplayer.feature.player.revolutionary

/**
 * Video analysis information for AI-powered features
 */
data class VideoAnalysisInfo(
    val videoId: String = "",
    val duration: Long = 0L,
    val resolution: String = "",
    val frameRate: Float = 0f,
    val bitrate: Long = 0L,
    val codec: String = "",
    val audioTracks: Int = 0,
    val subtitleTracks: Int = 0,
    val chapters: List<VideoChapter> = emptyList(),
    val keyMoments: List<KeyMoment> = emptyList(),
    val analysisComplete: Boolean = false,
    val analysisProgress: Float = 0f,
    // AI Analysis properties
    val contentType: String = "Unknown",
    val facesDetected: Int = 0,
    val dominantColors: List<String> = emptyList(),
    val averageBrightness: Float = 0.5f,
    val motionLevel: Float = 0.5f,
    val objects: List<String> = emptyList(),
    val scenes: List<Scene> = emptyList(),
    // Additional analysis properties
    val currentTime: Long = 0L,
    val detectedObjects: List<DetectedObject> = emptyList(),
    val audioAnalysis: AudioAnalysis? = null,
    val contentRating: ContentRating = ContentRating.UNKNOWN,
    val suggestedActions: List<SuggestedAction> = emptyList()
)

/**
 * Video chapter information
 */
data class VideoChapter(
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val thumbnail: String? = null
)

/**
 * Key moment in video for quick navigation
 */
data class KeyMoment(
    val timestamp: Long,
    val description: String,
    val importance: Float = 1f,
    val type: KeyMomentType = KeyMomentType.GENERAL
)

/**
 * Types of key moments
 */
enum class KeyMomentType {
    GENERAL,
    ACTION,
    DIALOGUE,
    MUSIC,
    SCENE_CHANGE,
    HIGHLIGHT
}

/**
 * Detected object in video frame
 */
data class DetectedObject(
    val id: String,
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,
    val timestamp: Long
)

/**
 * Audio analysis information
 */
data class AudioAnalysis(
    val speechSegments: List<SpeechSegment> = emptyList(),
    val musicSegments: List<MusicSegment> = emptyList(),
    val averageVolume: Float = 0f,
    val peakVolume: Float = 0f,
    val silencePercentage: Float = 0f,
    val dominantFrequencies: List<Float> = emptyList()
)

/**
 * Speech segment in audio
 */
data class SpeechSegment(
    val startTime: Long,
    val endTime: Long,
    val text: String = "",
    val confidence: Float = 0f,
    val speaker: String = "Unknown"
)

/**
 * Music segment in audio
 */
data class MusicSegment(
    val startTime: Long,
    val endTime: Long,
    val genre: String = "Unknown",
    val tempo: Float = 0f,
    val key: String = "Unknown"
)

/**
 * Content rating information
 */
enum class ContentRating {
    UNKNOWN,
    G,
    PG,
    PG13,
    R,
    NC17,
    UNRATED
}

/**
 * Suggested action for user
 */
data class SuggestedAction(
    val id: String,
    val title: String,
    val description: String,
    val actionType: String,
    val timestamp: Long = 0L,
    val priority: Int = 0
)

/**
 * Scene information
 */
data class Scene(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val type: SceneType,
    val description: String = "",
    val thumbnail: String? = null
)

/**
 * Types of scenes
 */
enum class SceneType {
    INDOOR,
    OUTDOOR,
    DIALOGUE,
    ACTION,
    MUSIC,
    CREDITS,
    LANDSCAPE,
    MONTAGE,
    TRANSITION
}