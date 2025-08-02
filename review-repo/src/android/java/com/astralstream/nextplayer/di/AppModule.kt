package com.astralstream.nextplayer.di

import android.content.Context
import com.astralstream.nextplayer.ai.SpeechRecognitionEngine
import com.astralstream.nextplayer.ai.SubtitleGenerator
import com.astralstream.nextplayer.analytics.AnalyticsDashboardEngine
import com.astralstream.nextplayer.cache.SubtitleCacheManager
import com.astralstream.nextplayer.community.CommunityRepository
import com.astralstream.nextplayer.community.PlaylistSharingService
import com.astralstream.nextplayer.database.AppDatabase
import com.astralstream.nextplayer.feature.player.enhancedplayer.EnhancedVideoPlayer
import com.astralstream.nextplayer.feature.player.gestures.AdvancedGestureManager
import com.astralstream.nextplayer.network.CommunityApiService
import com.astralstream.nextplayer.security.EncryptionManager
import com.astralstream.nextplayer.security.EncryptionManagerImpl
import com.astralstream.nextplayer.utils.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideEncryptionManager(): EncryptionManager {
        return EncryptionManagerImpl()
    }
    
    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }
    
    @Provides
    @Singleton
    fun provideSubtitleCacheManager(
        @ApplicationContext context: Context,
        database: AppDatabase,
        encryptionManager: EncryptionManager
    ): SubtitleCacheManager {
        return SubtitleCacheManager(context, database, encryptionManager)
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.astralstream.com/") // Replace with actual API URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideCommunityApiService(retrofit: Retrofit): CommunityApiService {
        return retrofit.create(CommunityApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideCommunityRepository(
        apiService: CommunityApiService
    ): CommunityRepository {
        return CommunityRepository(apiService)
    }
    
    @Provides
    @Singleton
    fun providePlaylistSharingService(
        @ApplicationContext context: Context,
        database: AppDatabase,
        communityApi: CommunityApiService,
        encryptionManager: EncryptionManager,
        networkUtils: NetworkUtils
    ): PlaylistSharingService {
        return PlaylistSharingService(context, database, communityApi, encryptionManager, networkUtils)
    }
    
    @Provides
    @Singleton
    fun provideAdvancedGestureManager(
        @ApplicationContext context: Context
    ): AdvancedGestureManager {
        return AdvancedGestureManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAnalyticsDashboardEngine(): AnalyticsDashboardEngine {
        return AnalyticsDashboardEngine()
    }
    
    @Provides
    @Singleton
    fun provideSpeechRecognitionEngine(
        @ApplicationContext context: Context
    ): SpeechRecognitionEngine {
        return SpeechRecognitionEngine(context)
    }
    
    @Provides
    @Singleton
    fun provideSubtitleGenerator(
        @ApplicationContext context: Context,
        speechRecognizer: SpeechRecognitionEngine
    ): SubtitleGenerator {
        return SubtitleGenerator(context, speechRecognizer)
    }
    
    @Provides
    @Singleton
    fun provideEnhancedVideoPlayer(
        @ApplicationContext context: Context,
        subtitleGenerator: SubtitleGenerator,
        subtitleCacheManager: SubtitleCacheManager,
        videoCache: androidx.media3.datasource.cache.SimpleCache
    ): EnhancedVideoPlayer {
        return EnhancedVideoPlayer(context, subtitleGenerator, subtitleCacheManager, videoCache)
    }
}