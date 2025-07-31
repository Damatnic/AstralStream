// AppModule.kt
package com.astralplayer.nextplayer.di

import android.content.Context
import androidx.room.Room
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.data.repository.*
import com.astralplayer.nextplayer.utils.CodecManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AstralVuDatabase {
        return Room.databaseBuilder(
            context,
            AstralVuDatabase::class.java,
            AstralVuDatabase.DATABASE_NAME
        )
        .addMigrations(*AstralVuDatabase.getAllMigrations())
        .fallbackToDestructiveMigration()
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
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideRecentFilesRepository(database: AstralVuDatabase): RecentFilesRepository {
        return RecentFilesRepositoryImpl(database.recentFileDao())
    }
    
    @Provides
    @Singleton
    fun providePlaylistRepository(database: AstralVuDatabase): PlaylistRepository {
        return PlaylistRepositoryImpl(database.playlistDao())
    }
    
    @Provides
    @Singleton
    fun provideVideoRepository(database: AstralVuDatabase): VideoRepository {
        return VideoRepositoryImpl(database.videoDao())
    }
}

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
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvicto
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

// NetworkModule.kt
package com.astralplayer.nextplayer.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/") // Replace with actual API base URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}