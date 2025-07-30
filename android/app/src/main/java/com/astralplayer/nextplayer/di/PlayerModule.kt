package com.astralplayer.nextplayer.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    
    @Provides
    @Singleton
    fun provideBandwidthMeter(@ApplicationContext context: Context): DefaultBandwidthMeter {
        return DefaultBandwidthMeter.getSingletonInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideTrackSelector(@ApplicationContext context: Context): DefaultTrackSelector {
        return DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setForceLowestBitrate(false)
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
            .build()
    }
    
    @Provides
    @Singleton
    fun provideCodecManager(@ApplicationContext context: Context): CodecManager {
        return CodecManager(context).apply {
            initializeCodecs()
        }
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
        return codecManager.applyOptimizations(
            ExoPlayer.Builder(context)
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
        fun createExoPlayer(context: Context, codecManager: CodecManager? = null): ExoPlayer {
            val manager = codecManager ?: CodecManager(context).apply { initializeCodecs() }
            
            val trackSelector = DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setForceLowestBitrate(false)
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
                .build()
            
            val bandwidthMeter = DefaultBandwidthMeter.getSingletonInstance(context)
            
            return manager.applyOptimizations(
                ExoPlayer.Builder(context)
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