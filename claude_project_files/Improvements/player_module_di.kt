package com.astralplayer.nextplayer.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultDataSource
import androidx.media3.exoplayer.upstream.DefaultHttpDataSource
import androidx.media3.exoplayer.upstream.cache.CacheDataSource
import androidx.media3.exoplayer.upstream.cache.SimpleCache
import androidx.media3.exoplayer.upstream.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.database.StandaloneDatabaseProvider
import com.astralplayer.nextplayer.data.PlayerRepository
import com.astralplayer.nextplayer.data.PlayerRepositoryImpl
import com.astralplayer.nextplayer.utils.CodecManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    
    @Provides
    @Singleton
    fun provideSimpleCache(@ApplicationContext context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "video_cache")
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024) // 500MB cache
        val databaseProvider = StandaloneDatabaseProvider(context)
        
        return SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }
    
    @Provides
    @Singleton
    fun provideBandwidthMeter(@ApplicationContext context: Context): DefaultBandwidthMeter {
        return DefaultBandwidthMeter.getSingletonInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideDataSourceFactory(
        @ApplicationContext context: Context,
        cache: SimpleCache,
        bandwidthMeter: DefaultBandwidthMeter
    ): DefaultDataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("AstralPlayer/1.0.0")
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)
            .setAllowCrossProtocolRedirects(true)
            .setTransferListener(bandwidthMeter)
        
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(dataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .let { cacheFactory ->
                object : DefaultDataSource.Factory(context) {
                    override fun createDataSource(): DefaultDataSource {
                        return DefaultDataSource(
                            context,
                            /* baseDataSourceFactory = */ cacheFactory.createDataSource(),
                            /* transferListener = */ bandwidthMeter
                        )
                    }
                }
            }
    }
    
    @Provides
    @Singleton
    fun provideTrackSelector(@ApplicationContext context: Context): DefaultTrackSelector {
        return DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setForceLowestBitrate(false)
                    .setAllowVideoMixedMimeTypeAdaptation(true)
                    .setAllowAudioMixedMimeTypeAdaptation(true)
                    .setExceedVideoConstraintsIfNecessary(true)
                    .setExceedAudioConstraintsIfNecessary(true)
                    .setTunnelingEnabled(true)
                    .build()
            )
        }
    }
    
    @Provides
    @Singleton
    fun provideLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 50000,  // 50 seconds minimum buffer
                /* maxBufferMs = */ 300000, // 5 minutes maximum buffer
                /* bufferForPlaybackMs = */ 2500,  // 2.5 seconds to start playback
                /* bufferForPlaybackAfterRebufferMs = */ 5000  // 5 seconds after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        codecManager: CodecManager,
        trackSelector: DefaultTrackSelector,
        loadControl: DefaultLoadControl,
        audioAttributes: AudioAttributes,
        bandwidthMeter: DefaultBandwidthMeter
    ): ExoPlayer {
        return codecManager.applyAdultContentOptimizations(
            ExoPlayer.Builder(context)
                .setRenderersFactory(codecManager.createRenderersFactory())
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setBandwidthMeter(bandwidthMeter)
        )
        .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setPauseAtEndOfMediaItems(false)
        .setSkipSilenceEnabled(false)
        .build()
    }
    
    @Provides
    @Singleton
    fun providePlayerRepository(
        exoPlayer: ExoPlayer,
        @ApplicationContext context: Context
    ): PlayerRepository {
        return PlayerRepositoryImpl(exoPlayer, context)
    }
    
    // Static methods for compatibility with existing code
    companion object {
        fun createExoPlayer(context: Context, codecManager: CodecManager? = null): ExoPlayer {
            val manager = codecManager ?: CodecManager(context).apply { initializeCodecs() }
            
            val trackSelector = DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setForceLowestBitrate(false)
                        .setAllowVideoMixedMimeTypeAdaptation(true)
                        .setAllowAudioMixedMimeTypeAdaptation(true)
                        .setExceedVideoConstraintsIfNecessary(true)
                        .setExceedAudioConstraintsIfNecessary(true)
                        .setTunnelingEnabled(true)
                        .build()
                )
            }
            
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    50000,  // 50 seconds minimum buffer
                    300000, // 5 minutes maximum buffer
                    2500,   // 2.5 seconds to start playback
                    5000    // 5 seconds after rebuffer
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build()
            
            val bandwidthMeter = DefaultBandwidthMeter.getSingletonInstance(context)
            
            return manager.applyAdultContentOptimizations(
                ExoPlayer.Builder(context)
                    .setRenderersFactory(manager.createRenderersFactory())
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadControl)
                    .setBandwidthMeter(bandwidthMeter)
            )
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setPauseAtEndOfMediaItems(false)
            .setSkipSilenceEnabled(false)
            .build()
        }
        
        fun createPlayerRepository(exoPlayer: ExoPlayer, context: Context): PlayerRepository {
            return PlayerRepositoryImpl(exoPlayer, context)
        }
    }
}