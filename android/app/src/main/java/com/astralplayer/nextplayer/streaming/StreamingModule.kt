package com.astralplayer.nextplayer.streaming

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for streaming-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object StreamingModule {
    
    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideAdaptiveBitrateController(): AdaptiveBitrateController {
        return AdaptiveBitrateController()
    }
    
    @Provides
    @Singleton
    fun provideIntelligentPreBufferService(
        @ApplicationContext context: Context
    ): IntelligentPreBufferService {
        return IntelligentPreBufferService(context)
    }
    
    @Provides
    @Singleton
    fun provideOfflineDownloadManager(
        @ApplicationContext context: Context
    ): OfflineDownloadManager {
        return OfflineDownloadManager(context)
    }
    
    @Provides
    @Singleton
    fun provideP2PStreamingService(
        @ApplicationContext context: Context
    ): P2PStreamingService {
        return P2PStreamingService(context)
    }
    
    @Provides
    @Singleton
    fun provideAdvancedStreamingEngine(
        @ApplicationContext context: Context,
        networkMonitor: NetworkMonitor,
        adaptiveBitrateController: AdaptiveBitrateController,
        intelligentPreBuffer: IntelligentPreBufferService,
        downloadManager: OfflineDownloadManager,
        p2pStreamingService: P2PStreamingService
    ): AdvancedStreamingEngine {
        return AdvancedStreamingEngine(
            context,
            networkMonitor,
            adaptiveBitrateController,
            intelligentPreBuffer,
            downloadManager,
            p2pStreamingService
        )
    }
}