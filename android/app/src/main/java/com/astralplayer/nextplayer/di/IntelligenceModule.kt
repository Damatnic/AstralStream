package com.astralplayer.nextplayer.di

import android.content.Context
import com.astralplayer.nextplayer.intelligence.AIContentIntelligenceEngine
import com.astralplayer.nextplayer.intelligence.AISceneDetectionService
import com.astralplayer.nextplayer.intelligence.ContentCategorizationService
import com.astralplayer.nextplayer.intelligence.ContentIntelligenceRepository
import com.astralplayer.nextplayer.intelligence.FaceRecognitionService
import com.astralplayer.nextplayer.intelligence.ObjectDetectionService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing AI content intelligence dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object IntelligenceModule {
    
    @Provides
    @Singleton
    fun provideAIContentIntelligenceEngine(
        @ApplicationContext context: Context
    ): AIContentIntelligenceEngine {
        return AIContentIntelligenceEngine(context)
    }
    
    @Provides
    @Singleton
    fun provideAISceneDetectionService(
        @ApplicationContext context: Context
    ): AISceneDetectionService {
        return AISceneDetectionService(context)
    }
    
    @Provides
    @Singleton
    fun provideContentCategorizationService(
        @ApplicationContext context: Context
    ): ContentCategorizationService {
        return ContentCategorizationService(context)
    }
    
    @Provides
    @Singleton
    fun provideContentIntelligenceRepository(
        @ApplicationContext context: Context
    ): ContentIntelligenceRepository {
        return ContentIntelligenceRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideFaceRecognitionService(
        @ApplicationContext context: Context
    ): FaceRecognitionService {
        return FaceRecognitionService(context)
    }
    
    @Provides
    @Singleton
    fun provideObjectDetectionService(
        @ApplicationContext context: Context
    ): ObjectDetectionService {
        return ObjectDetectionService(context)
    }
}