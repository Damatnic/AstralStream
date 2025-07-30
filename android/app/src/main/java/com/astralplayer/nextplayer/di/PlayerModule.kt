package com.astralplayer.nextplayer.di

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.*
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.*
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import okhttp3.OkHttpClient
import java.io.File

// Enhanced PlayerModule with comprehensive format support
object PlayerModule {
    
    private const val CACHE_SIZE = 100 * 1024 * 1024L // 100MB cache
    private const val USER_AGENT = "Astral-Vu/1.0 (Android)"
    
    @Volatile
    private var simpleCache: SimpleCache? = null
    
    @UnstableApi
    fun createExoPlayer(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setRenderersFactory(createRenderersFactory(context))
            .setTrackSelector(createTrackSelector(context))
            .setLoadControl(createLoadControl())
            .setMediaSourceFactory(createMediaSourceFactory(context))
            .setBandwidthMeter(createBandwidthMeter(context))
            .setSeekBackIncrementMs(10000) // 10 seconds
            .setSeekForwardIncrementMs(30000) // 30 seconds
            .build()
    }
    
    @UnstableApi
    private fun createRenderersFactory(context: Context): RenderersFactory {
        return object : DefaultRenderersFactory(context) {
            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: android.os.Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: java.util.ArrayList<Renderer>
            ) {
                // Add MediaCodec video renderer with enhanced configuration
                out.add(
                    MediaCodecVideoRenderer(
                        context,
                        mediaCodecSelector,
                        allowedVideoJoiningTimeMs,
                        enableDecoderFallback,
                        eventHandler,
                        eventListener,
                        MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY
                    )
                )
                
                // Add other video renderers
                super.buildVideoRenderers(
                    context,
                    extensionRendererMode,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    allowedVideoJoiningTimeMs,
                    out
                )
            }
            
            override fun buildAudioRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                audioSink: androidx.media3.exoplayer.audio.AudioSink,
                eventHandler: android.os.Handler,
                eventListener: AudioRendererEventListener,
                out: java.util.ArrayList<Renderer>
            ) {
                // Add MediaCodec audio renderer with enhanced audio sink
                out.add(
                    MediaCodecAudioRenderer(
                        context,
                        mediaCodecSelector,
                        enableDecoderFallback,
                        eventHandler,
                        eventListener,
                        DefaultAudioSink.Builder()
                            .setEnableFloatOutput(false)
                            .setEnableAudioTrackPlaybackParams(false)
                            .build()
                    )
                )
                
                // Add other audio renderers
                super.buildAudioRenderers(
                    context,
                    extensionRendererMode,
                    mediaCodecSelector,
                    enableDecoderFallback,
                    audioSink,
                    eventHandler,
                    eventListener,
                    out
                )
            }
            
            override fun buildTextRenderers(
                context: Context,
                output: androidx.media3.exoplayer.text.TextOutput,
                outputLooper: android.os.Looper,
                extensionRendererMode: Int,
                out: java.util.ArrayList<Renderer>
            ) {
                // Add text renderer for subtitles
                out.add(TextRenderer(output, outputLooper))
                
                super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, out)
            }
        }
    }
    
    @UnstableApi
    private fun createTrackSelector(context: Context): DefaultTrackSelector {
        return DefaultTrackSelector(context, AdaptiveTrackSelection.Factory()).apply {
            setParameters(
                buildUponParameters()
                    // Video quality settings
                    .setMaxVideoSizeSd() // Start with SD, can be upgraded
                    .setMaxVideoBitrate(2000000) // 2 Mbps max initially
                    .setMinVideoSize(480, 360) // Minimum resolution
                    .setViewportSizeToPhysicalDisplaySize(context, true)
                    
                    // Audio settings
                    .setPreferredAudioLanguages("en", "und") // English preferred, undefined as fallback
                    .setMaxAudioBitrate(128000) // 128 kbps max
                    .setMaxAudioChannelCount(2) // Stereo max initially
                    
                    // Text/subtitle settings
                    .setPreferredTextLanguages("en", "und")
                    .setSelectUndeterminedTextLanguage(true)
                    
                    // Performance settings
                    .setForceLowestBitrate(false)
                    .setForceHighestSupportedBitrate(false)
                    .setTunnelingEnabled(false) // Disable tunneling for compatibility
                    
                    .build()
            )
        }
    }
    
    @UnstableApi
    private fun createLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            // Buffer settings for smooth playback
            .setBufferDurationsMs(
                15000, // Min buffer (15 seconds)
                50000, // Max buffer (50 seconds)
                2500,  // Buffer for playback (2.5 seconds)
                5000   // Buffer for playback after rebuffer (5 seconds)
            )
            .setTargetBufferBytes(C.LENGTH_UNSET) // Use default
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(20000, true) // 20 second back buffer, retain after rebuffer
            .build()
    }
    
    @UnstableApi
    private fun createMediaSourceFactory(context: Context): DefaultMediaSourceFactory {
        val dataSourceFactory = createDataSourceFactory(context)
        
        return DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
    }
    
    @UnstableApi
    private fun createDataSourceFactory(context: Context): DataSource.Factory {
        // Create OkHttp client with comprehensive configuration
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        
        // Create HTTP data source factory
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(USER_AGENT)
        
        // Create default data source factory (handles file://, content://, etc.)
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        
        // Add caching layer
        val cache = getCache(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setCacheWriteDataSinkFactory(null) // Use default
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
    
    private fun createBandwidthMeter(context: Context): DefaultBandwidthMeter {
        return DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(1000000L) // 1 Mbps initial estimate
            .build()
    }
    
    @Synchronized
    private fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "exoplayer_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            simpleCache = SimpleCache(cacheDir, evictor)
        }
        return simpleCache!!
    }
    
    fun createPlayerRepository(exoPlayer: ExoPlayer, context: Context): com.astralplayer.nextplayer.data.PlayerRepository {
        return com.astralplayer.nextplayer.data.PlayerRepositoryImpl(exoPlayer, context)
    }
    
    fun releaseCache() {
        simpleCache?.release()
        simpleCache = null
    }
    
    // Configuration constants
    private const val MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY = 5
}