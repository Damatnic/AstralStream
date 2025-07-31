package com.astralplayer.nextplayer.di

import android.content.Context
import com.astralplayer.nextplayer.audio.AdvancedAudioProcessingEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing advanced audio processing dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioProcessingModule {
    
    @Provides
    @Singleton
    fun provideAdvancedAudioProcessingEngine(
        @ApplicationContext context: Context
    ): AdvancedAudioProcessingEngine {
        return AdvancedAudioProcessingEngine(context)
    }
}