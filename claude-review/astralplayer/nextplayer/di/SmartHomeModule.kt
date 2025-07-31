package com.astralplayer.nextplayer.di

import android.content.Context
import com.astralplayer.nextplayer.smarthome.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing smart home integration dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SmartHomeModule {
    
    @Provides
    @Singleton
    fun provideSmartHomeIntegrationEngine(
        @ApplicationContext context: Context,
        voiceAssistantManager: VoiceAssistantManager,
        smartTVCastingManager: SmartTVCastingManager,
        ioTDeviceManager: IoTDeviceManager,
        homeAutomationController: HomeAutomationController,
        ambientLightingSync: AmbientLightingSync
    ): SmartHomeIntegrationEngine {
        return SmartHomeIntegrationEngine(
            context = context,
            voiceAssistantManager = voiceAssistantManager,
            smartTVCastingManager = smartTVCastingManager,
            ioTDeviceManager = ioTDeviceManager,
            homeAutomationController = homeAutomationController,
            ambientLightingSync = ambientLightingSync
        )
    }
    
    @Provides
    @Singleton
    fun provideVoiceAssistantManager(
        @ApplicationContext context: Context
    ): VoiceAssistantManager {
        return VoiceAssistantManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSmartTVCastingManager(
        @ApplicationContext context: Context
    ): SmartTVCastingManager {
        return SmartTVCastingManager(context)
    }
    
    @Provides
    @Singleton
    fun provideIoTDeviceManager(
        @ApplicationContext context: Context
    ): IoTDeviceManager {
        return IoTDeviceManager(context)
    }
    
    @Provides
    @Singleton
    fun provideHomeAutomationController(
        @ApplicationContext context: Context
    ): HomeAutomationController {
        return HomeAutomationController(context)
    }
    
    @Provides
    @Singleton
    fun provideAmbientLightingSync(
        @ApplicationContext context: Context,
        ioTDeviceManager: IoTDeviceManager
    ): AmbientLightingSync {
        return AmbientLightingSync(context, ioTDeviceManager)
    }
}