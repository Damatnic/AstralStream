package com.astralplayer.nextplayer

/**
 * Application configuration and constants
 */
object AppConfig {
    
    // Demo streams for testing
    val DEMO_STREAMS = listOf(
        StreamPreset("Big Buck Bunny", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
        StreamPreset("Elephant Dream", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
        StreamPreset("For Bigger Blazes", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
    )
}

data class StreamPreset(
    val name: String,
    val url: String
)