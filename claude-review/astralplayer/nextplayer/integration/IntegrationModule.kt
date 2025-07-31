package com.astralplayer.nextplayer.integration

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.astralplayer.nextplayer.gesture.AdvancedGestureManager
import com.astralplayer.nextplayer.subtitle.AdvancedAISubtitleGenerator
import com.astralplayer.nextplayer.enhancement.SmartVideoEnhancementEngine
import com.astralplayer.nextplayer.streaming.AdvancedStreamingEngine
import com.astralplayer.nextplayer.professional.ProfessionalVideoToolsEngine
import javax.inject.Singleton

/**
 * Hilt module for integration-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object IntegrationModule {
    
    @Provides
    @Singleton
    fun provideIntegratedPerformanceManager(
        @ApplicationContext context: Context
    ): IntegratedPerformanceManager {
        return IntegratedPerformanceManager(context)
    }
    
    @Provides
    @Singleton
    fun provideResourceOptimizationManager(
        @ApplicationContext context: Context
    ): ResourceOptimizationManager {
        return ResourceOptimizationManager(context)
    }
    
    @Provides
    @Singleton
    fun provideMasterIntegrationCoordinator(
        @ApplicationContext context: Context,
        gestureManager: AdvancedGestureManager,
        subtitleGenerator: AdvancedAISubtitleGenerator,
        enhancementEngine: SmartVideoEnhancementEngine,
        streamingEngine: AdvancedStreamingEngine,
        professionalTools: ProfessionalVideoToolsEngine,
        performanceManager: IntegratedPerformanceManager,
        resourceManager: ResourceOptimizationManager
    ): MasterIntegrationCoordinator {
        return MasterIntegrationCoordinator(
            context,
            gestureManager,
            subtitleGenerator,
            enhancementEngine,
            streamingEngine,
            professionalTools,
            performanceManager,
            resourceManager
        )
    }
}