package com.astralstream.nextplayer.feature.player.enhancedplayer

import android.content.Context
import android.net.Uri
import androidx.media3.common.*
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.*
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.ui.PlayerView
import com.astralstream.nextplayer.ai.SubtitleGenerator
import com.astralstream.nextplayer.cache.SubtitleCacheManager
import com.astralstream.nextplayer.models.SubtitleEntry
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class EnhancedVideoPlayer @Inject constructor(
    private val context: Context,
    private val subtitleGenerator: SubtitleGenerator,
    private val subtitleCacheManager: SubtitleCacheManager,
    private val videoCache: SimpleCache
) {
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var subtitleJob: Job? = null
    
    fun initializePlayer(playerView: PlayerView) {
        exoPlayer = ExoPlayer.Builder(context)
            .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                playerView.player = this
                playerView.useController = true
                playerView.controllerShowTimeoutMs = 3000
                playerView.controllerHideOnTouch = true
            }
    }
    
    fun playVideo(
        uri: Uri, 
        title: String,
        headers: Map<String, String> = emptyMap()
    ) {
        val mediaSource = buildMediaSource(uri, headers)
        
        exoPlayer?.apply {
            setMediaSource(mediaSource)
            prepare()
            play()
        }
        
        // Cancel previous subtitle generation
        subtitleJob?.cancel()
        
        // Start new subtitle generation
        subtitleJob = scope.launch {
            delay(500) // Small delay to ensure playback starts
            generateSubtitlesInBackground(uri.toString(), title)
        }
    }
    
    private fun buildMediaSource(uri: Uri, headers: Map<String, String>): MediaSource {
        val dataSourceFactory = buildDataSourceFactory(headers)
        val cacheDataSourceFactory = buildCacheDataSourceFactory(dataSourceFactory)
        
        return when {
            uri.toString().contains(".m3u8") -> {
                HlsMediaSource.Factory(cacheDataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
            uri.toString().contains(".mpd") -> {
                DashMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
            else -> {
                ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
        }
    }
    
    private fun buildDataSourceFactory(headers: Map<String, String>): DataSource.Factory {
        return DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(headers)
            setAllowCrossProtocolRedirects(true)
            setConnectTimeoutMs(30000)
            setReadTimeoutMs(30000)
            setUserAgent("AstralStream/1.0")
        }
    }
    
    private fun buildCacheDataSourceFactory(
        upstreamFactory: DataSource.Factory
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(videoCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
    
    private suspend fun generateSubtitlesInBackground(videoUri: String, title: String) {
        try {
            // Check cache first
            val cachedSubtitle = subtitleCacheManager.getCachedSubtitle(videoUri, "en")
            if (cachedSubtitle != null) {
                withContext(Dispatchers.Main) {
                    applySubtitlesToPlayer(cachedSubtitle)
                }
                return
            }
            
            // Generate new subtitles
            val subtitle = subtitleGenerator.generateSubtitles(
                videoUri = videoUri,
                videoTitle = title,
                language = "en"
            )
            
            // Cache for future use
            subtitleCacheManager.cacheSubtitle(
                videoUri = videoUri,
                language = "en",
                subtitle = subtitle,
                sourceType = SubtitleCacheManager.SourceType.AI_GENERATED
            )
            
            // Apply to player
            withContext(Dispatchers.Main) {
                applySubtitlesToPlayer(subtitle)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate subtitles")
            // Don't show error to user - subtitles are optional
        }
    }
    
    private fun applySubtitlesToPlayer(subtitle: SubtitleEntry) {
        // Convert subtitle to SRT format
        val srtContent = subtitle.toSrtFormat()
        val subtitleFile = File(context.cacheDir, "current_subtitle.srt")
        subtitleFile.writeText(srtContent)
        
        // Add subtitle track to player
        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subtitleFile))
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage("en")
            .setSelectionFlags(C.SELECTION_FLAG_FORCED)
            .build()
        
        exoPlayer?.currentMediaItem?.let { mediaItem ->
            val newMediaItem = mediaItem.buildUpon()
                .setSubtitleConfigurations(listOf(subtitleConfig))
                .build()
            
            exoPlayer?.setMediaItem(newMediaItem, false)
        }
    }
    
    fun release() {
        subtitleJob?.cancel()
        scope.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}

// Extension function to convert SubtitleEntry to SRT
private fun SubtitleEntry.toSrtFormat(): String {
    return entries.mapIndexed { index, line ->
        """
        ${index + 1}
        ${formatTime(line.startTime)} --> ${formatTime(line.endTime)}
        ${line.text}
        
        """.trimIndent()
    }.joinToString("\n")
}

private fun formatTime(millis: Long): String {
    val hours = millis / 3600000
    val minutes = (millis % 3600000) / 60000
    val seconds = (millis % 60000) / 1000
    val milliseconds = millis % 1000
    return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
}