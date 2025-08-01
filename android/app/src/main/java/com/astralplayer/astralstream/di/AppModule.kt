package com.astralplayer.astralstream.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.astralplayer.astralstream.data.database.AstralStreamDatabase
import com.astralplayer.astralstream.data.repository.SettingsRepository
import com.astralplayer.astralstream.data.repository.SettingsRepositoryImpl
import com.astralplayer.core.intent.VideoIntentHandler
import com.astralplayer.core.extractor.StreamExtractor
import com.astralplayer.core.codec.CodecOptimizer
import com.astralplayer.core.config.ApiKeyManager
import com.astralplayer.core.audio.AudioExtractorEngine
import com.astralplayer.core.browser.BrowserIntentHandler
import com.astralplayer.core.system.DefaultPlayerManager
import com.astralplayer.features.ai.EnhancedAISubtitleGenerator
import com.astralplayer.features.ai.SubtitleFallbackEngine
import com.astralplayer.community.api.*
import com.astralplayer.community.dao.*
import com.astralplayer.community.repository.*
import com.astralplayer.astralstream.data.dao.SubtitleCacheDao
import com.astralplayer.features.subtitle.SubtitleCacheManager
import com.astralplayer.features.subtitle.EncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    @Provides
    @Singleton
    fun provideAstralStreamDatabase(
        @ApplicationContext context: Context
    ): AstralStreamDatabase {
        return Room.databaseBuilder(
            context,
            AstralStreamDatabase::class.java,
            AstralStreamDatabase.DATABASE_NAME
        )
        .addMigrations(*AstralStreamDatabase.getAllMigrations())
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
    }
    
    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideVideoDao(database: AstralStreamDatabase) = database.videoDao()
    
    @Provides
    @Singleton
    fun providePlaylistDao(database: AstralStreamDatabase) = database.playlistDao()
    
    @Provides
    @Singleton
    fun provideSubtitleDao(database: AstralStreamDatabase) = database.subtitleDao()
    
    @Provides
    @Singleton
    fun provideSettingsDao(database: AstralStreamDatabase) = database.settingsDao()
    
    @Provides
    @Singleton
    fun providePlaybackStateDao(database: AstralStreamDatabase) = database.playbackStateDao()
    
    @Provides
    @Singleton
    fun provideAISubtitleGenerator(
        @ApplicationContext context: Context
    ): com.astralplayer.astralstream.ai.AISubtitleGenerator {
        return com.astralplayer.astralstream.ai.AISubtitleGenerator(context)
    }
    
    @Provides
    @Singleton
    fun provideCloudStorageManager(
        @ApplicationContext context: Context
    ): com.astralplayer.astralstream.cloud.CloudStorageManager {
        return com.astralplayer.astralstream.cloud.CloudStorageManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAdvancedPlayerConfiguration(
        @ApplicationContext context: Context,
        database: AstralStreamDatabase,
        settingsRepository: SettingsRepository
    ): com.astralplayer.astralstream.player.AdvancedPlayerConfiguration {
        return com.astralplayer.astralstream.player.AdvancedPlayerConfiguration(
            context,
            database,
            settingsRepository
        )
    }
    
    // Enhanced Video Player Components
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideVideoIntentHandler(): VideoIntentHandler {
        return VideoIntentHandler()
    }
    
    @Provides
    @Singleton
    fun provideStreamExtractor(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): StreamExtractor {
        return StreamExtractor(context, okHttpClient)
    }
    
    @Provides
    @Singleton
    fun provideCodecOptimizer(): CodecOptimizer {
        return CodecOptimizer()
    }
    
    @Provides
    @Singleton
    fun provideApiKeyManager(
        @ApplicationContext context: Context
    ): ApiKeyManager {
        return ApiKeyManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAudioExtractorEngine(
        @ApplicationContext context: Context
    ): AudioExtractorEngine {
        return AudioExtractorEngine(context)
    }
    
    @Provides
    @Singleton
    fun provideSubtitleFallbackEngine(
        @ApplicationContext context: Context
    ): SubtitleFallbackEngine {
        return SubtitleFallbackEngine(context)
    }
    
    @Provides
    @Singleton
    fun provideBrowserIntentHandler(): BrowserIntentHandler {
        return BrowserIntentHandler()
    }
    
    @Provides
    @Singleton
    fun provideDefaultPlayerManager(
        @ApplicationContext context: Context
    ): DefaultPlayerManager {
        return DefaultPlayerManager(context)
    }
    
    @Provides
    @Singleton
    fun provideEnhancedAISubtitleGenerator(
        @ApplicationContext context: Context,
        apiKeyManager: ApiKeyManager,
        audioExtractor: AudioExtractorEngine,
        fallbackEngine: SubtitleFallbackEngine,
        subtitleCacheManager: SubtitleCacheManager
    ): EnhancedAISubtitleGenerator {
        return EnhancedAISubtitleGenerator(
            context, 
            apiKeyManager, 
            audioExtractor, 
            fallbackEngine, 
            subtitleCacheManager
        )
    }
    
    // Subtitle Cache System
    @Provides
    @Singleton
    fun provideSubtitleCacheDao(database: AstralStreamDatabase): SubtitleCacheDao {
        return database.subtitleCacheDao()
    }
    
    @Provides
    @Singleton
    fun provideEncryptionManager(@ApplicationContext context: Context): EncryptionManager {
        return EncryptionManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSubtitleCacheManager(
        @ApplicationContext context: Context,
        subtitleCacheDao: SubtitleCacheDao,
        encryptionManager: EncryptionManager
    ): SubtitleCacheManager {
        return SubtitleCacheManager(context, subtitleCacheDao, encryptionManager)
    }
    
    // Community Features
    @Provides
    @Singleton
    fun provideMockCommunityApiService(): MockCommunityApiService {
        return MockCommunityApiService()
    }
    
    @Provides
    @Singleton
    fun provideCommunityApiClient(): CommunityApiClient {
        return CommunityApiClient()
    }
    
    @Provides
    @Singleton
    fun provideCommunityApiManager(
        mockApi: MockCommunityApiService,
        apiClient: CommunityApiClient
    ): CommunityApiManager {
        return CommunityApiManager(mockApi, apiClient)
    }
    
    @Provides
    @Singleton
    fun provideSharedPlaylistDao(database: AstralStreamDatabase): SharedPlaylistDao {
        return database.sharedPlaylistDao()
    }
    
    @Provides
    @Singleton
    fun provideCommunitySubtitleDao(database: AstralStreamDatabase): CommunitySubtitleDao {
        return database.communitySubtitleDao()
    }
    
    @Provides
    @Singleton
    fun provideSubtitleVoteDao(database: AstralStreamDatabase): SubtitleVoteDao {
        return database.subtitleVoteDao()
    }
    
    @Provides
    @Singleton
    fun provideSubtitleReportDao(database: AstralStreamDatabase): SubtitleReportDao {
        return database.subtitleReportDao()
    }
    
    @Provides
    @Singleton
    fun provideSubtitleDownloadDao(database: AstralStreamDatabase): SubtitleDownloadDao {
        return database.subtitleDownloadDao()
    }
    
    @Provides
    @Singleton
    fun providePlaylistRatingDao(database: AstralStreamDatabase): PlaylistRatingDao {
        return database.playlistRatingDao()
    }
    
    @Provides
    @Singleton
    fun providePlaylistSharingRepository(
        apiManager: CommunityApiManager,
        database: AstralStreamDatabase,
        @ApplicationContext context: Context
    ): PlaylistSharingRepository {
        return PlaylistSharingRepository(apiManager, database, context)
    }
    
    @Provides
    @Singleton
    fun provideSubtitleContributionRepository(
        apiManager: CommunityApiManager,
        database: AstralStreamDatabase,
        @ApplicationContext context: Context
    ): SubtitleContributionRepository {
        return SubtitleContributionRepository(apiManager, database, context)
    }
    
    // Gesture System
    @Provides
    @Singleton
    fun provideGestureDao(database: AstralStreamDatabase): com.astralplayer.features.gestures.dao.GestureDao {
        return database.gestureDao()
    }
    
    @Provides
    @Singleton
    fun provideGestureRepository(
        gestureDao: com.astralplayer.features.gestures.dao.GestureDao,
        @ApplicationContext context: Context
    ): com.astralplayer.features.gestures.repository.GestureRepository {
        return com.astralplayer.features.gestures.repository.GestureRepository(gestureDao, context)
    }
    
    // Analytics System
    @Provides
    @Singleton
    fun provideAnalyticsDao(database: AstralStreamDatabase): com.astralplayer.features.analytics.dao.AnalyticsDao {
        return database.analyticsDao()
    }
    
    @Provides
    @Singleton
    fun provideAnalyticsRepository(
        analyticsDao: com.astralplayer.features.analytics.dao.AnalyticsDao,
        @ApplicationContext context: Context
    ): com.astralplayer.features.analytics.repository.AnalyticsRepository {
        return com.astralplayer.features.analytics.repository.AnalyticsRepository(analyticsDao, context)
    }
    
    // Video Editing System
    @Provides
    @Singleton
    fun provideVideoEditingService(
        @ApplicationContext context: Context
    ): com.astralplayer.features.editing.service.VideoEditingService {
        return com.astralplayer.features.editing.service.VideoEditingService(context)
    }
}