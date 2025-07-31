// EnhancedVideoPlayerActivity.kt
package com.astralplayer.presentation.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.*
import androidx.media3.datasource.*
import androidx.media3.exoplayer.*
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.source.*
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.SubtitleView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.astralplayer.core.intent.VideoIntentHandler
import com.astralplayer.core.extractor.StreamExtractor
import com.astralplayer.core.browser.BrowserIntentHandler
import com.astralplayer.core.system.DefaultPlayerManager
import com.astralplayer.features.ai.EnhancedAISubtitleGenerator
import timber.log.Timber

@AndroidEntryPoint
class EnhancedVideoPlayerActivity : ComponentActivity() {
    
    @Inject lateinit var intentHandler: VideoIntentHandler
    @Inject lateinit var streamExtractor: StreamExtractor
    @Inject lateinit var browserHandler: BrowserIntentHandler
    @Inject lateinit var defaultPlayerManager: DefaultPlayerManager
    @Inject lateinit var subtitleGenerator: EnhancedAISubtitleGenerator
    
    private lateinit var player: ExoPlayer
    private val dynamicSubtitles = mutableListOf<Cue>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if we should be the default video player
        checkDefaultPlayerStatus()
        
        // Initialize player with subtitle support
        initializePlayer()
        
        // Handle intent with browser-specific processing
        handleIntent(intent)
        
        setContent {
            VideoPlayerScreen(
                player = player,
                subtitleGenerator = subtitleGenerator
            )
        }
    }
    
    private fun initializePlayer() {
        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        
        player = ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(DefaultTrackSelector(this).apply {
                setParameters(
                    buildUponParameters()
                        .setMaxVideoSizeSd()
                        .setPreferredAudioLanguage("en")
                        .setPreferredTextLanguage("en")
                )
            })
            .build()
        
        // Add text output listener for dynamic subtitles
        player.addListener(object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                // Handle subtitle display
                val cues = cueGroup.cues
                Timber.d("Received ${cues.size} subtitle cues")
            }
            
            override fun onPlayerError(error: PlaybackException) {
                Timber.e("Player error: ${error.message}")
            }
        })
    }
    
    private fun checkDefaultPlayerStatus() {
        if (!defaultPlayerManager.isDefaultVideoPlayer()) {
            Timber.d("App is not the default video player")
            // Could show a prompt to set as default, but for now just log
        }
        
        // Check compatibility
        val compatibility = defaultPlayerManager.checkVideoAppCompatibility()
        Timber.d("Video app compatibility score: ${compatibility.getCompatibilityScore()}")
        
        if (compatibility.getCompatibilityScore() < 0.8f) {
            Timber.w("Low compatibility score, recommendations: ${compatibility.getRecommendations()}")
        }
    }
    
    private fun handleIntent(intent: android.content.Intent) {
        lifecycleScope.launch {
            try {
                // First, extract browser-specific data
                val browserData = browserHandler.extractBrowserData(intent)
                Timber.d("Browser data extracted: ${browserData.sourceApp} - ${browserData.originalUrl}")
                
                // Check if this is a video URL
                if (!browserData.isVideoUrl && !browserHandler.isVideoUrl(browserData.originalUrl)) {
                    Timber.w("URL may not be a video: ${browserData.originalUrl}")
                    // Continue anyway, might be an embedded video
                }
                
                // Parse intent with enhanced video detection
                val videoInfo = intentHandler.extractVideoInfo(intent)
                Timber.d("Video info extracted: $videoInfo")
                
                // Extract actual video URL if needed
                val videoUrl = if (videoInfo.requiresExtraction) {
                    streamExtractor.extractVideoUrl(
                        videoInfo.uri.toString(),
                        videoInfo.cookies,
                        videoInfo.headers
                    ) ?: videoInfo.uri.toString()
                } else {
                    videoInfo.uri.toString()
                }
                
                Timber.d("Final video URL: $videoUrl")
                
                // Create data source with headers and cookies
                val dataSourceFactory = createDataSourceFactory(videoInfo)
                
                // Create media source
                val mediaSource = createMediaSource(videoUrl, dataSourceFactory, videoInfo.streamType)
                
                // Prepare and play
                player.setMediaSource(mediaSource)
                player.prepare()
                player.play()
                
                // START AUTOMATIC SUBTITLE GENERATION
                startAutomaticSubtitleGeneration(videoUrl)
                
            } catch (e: Exception) {
                Timber.e(e, "Error handling intent")
            }
        }
    }
    
    private fun createDataSourceFactory(videoInfo: VideoIntentHandler.VideoInfo): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)
            .setAllowCrossProtocolRedirects(true)
        
        // Add headers
        val requestProperties = videoInfo.headers.toMutableMap()
        
        // Add cookies
        if (videoInfo.cookies.isNotEmpty()) {
            val cookieHeader = videoInfo.cookies.entries
                .joinToString("; ") { "${it.key}=${it.value}" }
            requestProperties["Cookie"] = cookieHeader
        }
        
        httpDataSourceFactory.setDefaultRequestProperties(requestProperties)
        
        return DefaultDataSource.Factory(this, httpDataSourceFactory)
    }
    
    private fun createMediaSource(
        uri: String,
        dataSourceFactory: DataSource.Factory,
        streamType: VideoIntentHandler.StreamType
    ): MediaSource {
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .build()
        
        return when (streamType) {
            VideoIntentHandler.StreamType.HLS -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }
            VideoIntentHandler.StreamType.DASH -> {
                DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            VideoIntentHandler.StreamType.SMOOTH -> {
                // For SmoothStreaming, you'd need to add the dependency
                // For now, fallback to progressive
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }
    
    private fun startAutomaticSubtitleGeneration(videoUrl: String) {
        // Auto-generate subtitles
        subtitleGenerator.autoGenerateSubtitles(
            videoUri = videoUrl,
            targetLanguage = "en",
            onSubtitleReady = { subtitle ->
                // Add subtitle to player in real-time
                lifecycleScope.launch {
                    addDynamicSubtitle(subtitle)
                }
            }
        )
        
        // Observe subtitle state
        lifecycleScope.launch {
            subtitleGenerator.state.collectLatest { state ->
                when {
                    state.error != null -> {
                        Timber.w("Subtitle generation error: ${state.error}")
                    }
                    state.isComplete -> {
                        Timber.d("All subtitles generated")
                    }
                }
            }
        }
    }
    
    private fun addDynamicSubtitle(subtitle: EnhancedAISubtitleGenerator.SubtitleEntry) {
        val cue = Cue.Builder()
            .setText(subtitle.text)
            .setTextAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
            .setLine(-1f, Cue.LINE_TYPE_NUMBER)
            .setLineAnchor(Cue.ANCHOR_TYPE_END)
            .setPosition(0.5f)
            .setPositionAnchor(Cue.ANCHOR_TYPE_MIDDLE)
            .build()
        
        dynamicSubtitles.add(cue)
        
        // Update subtitle track
        updateSubtitleTrack()
    }
    
    private fun updateSubtitleTrack() {
        // This would integrate with ExoPlayer's subtitle system
        // For production, you'd create a custom SubtitleDecoder
        Timber.d("Updated subtitle track with ${dynamicSubtitles.size} cues")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player.release()
        subtitleGenerator.cleanup()
    }
}