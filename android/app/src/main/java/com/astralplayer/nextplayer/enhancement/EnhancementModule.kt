package com.astralplayer.nextplayer.enhancement

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for video enhancement dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object EnhancementModule {
    
    @Provides
    @Singleton
    fun provideVideoShaderManager(): VideoShaderManager {
        return VideoShaderManager()
    }
    
    @Provides
    @Singleton
    fun provideAIModelManager(
        @ApplicationContext context: Context
    ): AIModelManager {
        return AIModelManager(context)
    }
    
    @Provides
    @Singleton
    fun provideEnhancementPerformanceMonitor(
        @ApplicationContext context: Context
    ): EnhancementPerformanceMonitor {
        return EnhancementPerformanceMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideSmartVideoEnhancementEngine(
        @ApplicationContext context: Context,
        shaderManager: VideoShaderManager,
        aiModelManager: AIModelManager,
        performanceMonitor: EnhancementPerformanceMonitor
    ): SmartVideoEnhancementEngine {
        return SmartVideoEnhancementEngine(
            context,
            shaderManager,
            aiModelManager,
            performanceMonitor
        )
    }
}