package com.astralplayer.nextplayer.di

import android.content.Context
import androidx.room.Room
import com.astralplayer.nextplayer.subtitle.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for subtitle-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SubtitleModule {
    
    @Provides
    @Singleton
    fun provideSubtitleDatabase(
        @ApplicationContext context: Context
    ): SubtitleDatabase {
        return Room.databaseBuilder(
            context,
            SubtitleDatabase::class.java,
            "subtitle_database"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideSubtitleDao(database: SubtitleDatabase): SubtitleDao {
        return database.subtitleDao()
    }
    
    @Provides
    @Singleton
    fun provideAudioExtractor(
        @ApplicationContext context: Context
    ): AudioExtractor {
        return AudioExtractor(context)
    }
    
    @Provides
    @Singleton
    fun provideLanguageDetector(
        @ApplicationContext context: Context
    ): LanguageDetector {
        return LanguageDetector(context)
    }
    
    @Provides
    @Singleton
    fun provideSubtitleRepository(
        @ApplicationContext context: Context,
        subtitleDao: SubtitleDao
    ): SubtitleRepository {
        return SubtitleRepository(context, subtitleDao)
    }
    
    @Provides
    @Singleton
    fun provideAdvancedAISubtitleGenerator(
        @ApplicationContext context: Context,
        audioExtractor: AudioExtractor,
        subtitleRepository: SubtitleRepository,
        languageDetector: LanguageDetector
    ): AdvancedAISubtitleGenerator {
        return AdvancedAISubtitleGenerator(
            context,
            audioExtractor,
            subtitleRepository,
            languageDetector
        )
    }
}