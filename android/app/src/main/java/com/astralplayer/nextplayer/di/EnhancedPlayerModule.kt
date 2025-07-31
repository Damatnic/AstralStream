// PlayerModule.kt
package com.astralplayer.nextplayer.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.astralplayer.nextplayer.data.PlayerRepository
import com.astralplayer.nextplayer.data.PlayerRepositoryImpl
import com.astralplayer.nextplayer.utils.CodecManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.File

@UnstableApi
@Module
@InstallIn(ViewModelComponent::class)
object PlayerModule {
    
    @Provides
    @ViewModelScoped
    fun provideSimpleCache(@ApplicationContext context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100MB
        return SimpleCache(cacheDir, cacheEvictor)
    }
    
    @Provides
    @ViewModelScoped
    fun provideDataSourceFactory(
        @ApplicationContext context: Context,
        cache: SimpleCache
    ): DefaultDataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("AstralStream/1.0")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)
        
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(dataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
    
    @Provides
    @ViewModelScoped
    fun provideTrackSelector(@ApplicationContext context: Context): DefaultTrackSelector {
        return DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSizeSd()
                    .setPreferredAudioLanguage("en")
                    .setPreferredTextLanguage("en")
                    .setSelectUndeterminedTextLanguage(true)
                    .setForceLowestBitrate(false)
                    .build()
            )
        }
    }
    
    @Provides
    @ViewModelScoped
    fun provideLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50000,  // 50 seconds minimum buffer
                300000, // 5 minutes maximum buffer
                2500,   // 2.5 seconds to start playback
                5000    // 5 seconds after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    @Provides
    @ViewModelScoped
    fun provideAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
    }
    
    @Provides
    @ViewModelScoped
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        codecManager: CodecManager,
        trackSelector: DefaultTrackSelector,
        loadControl: DefaultLoadControl,
        audioAttributes: AudioAttributes
    ): ExoPlayer {
        return codecManager.applyAdultContentOptimizations(
            ExoPlayer.Builder(context)
                .setRenderersFactory(codecManager.createRenderersFactory())
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
        )
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()
    }
    
    @Provides
    @ViewModelScoped
    fun providePlayerRepository(
        exoPlayer: ExoPlayer,
        @ApplicationContext context: Context
    ): PlayerRepository {
        return PlayerRepositoryImpl(exoPlayer, context)
    }
}