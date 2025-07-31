package com.astralplayer.nextplayer.di

import android.content.Context
import com.astralplayer.nextplayer.gesture.*
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Named

/**
 * Hilt module for providing gesture-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object GestureModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
    
    @Provides
    @Singleton
    fun provideMultiTouchGestureDetector(
        @ApplicationContext context: Context
    ): MultiTouchGestureDetector {
        return MultiTouchGestureDetector(context)
    }
    
    @Provides
    @Singleton
    @Named("GestureHaptic")
    fun provideGestureHapticFeedbackManager(
        @ApplicationContext context: Context
    ): com.astralplayer.nextplayer.data.HapticFeedbackManager {
        return com.astralplayer.nextplayer.data.HapticFeedbackManager(context)
    }
    
    @Provides
    @Singleton
    fun provideGestureRecorder(): GestureRecorder {
        return GestureRecorder()
    }
    
    @Provides
    @Singleton
    fun provideVoiceCommandHandler(
        @ApplicationContext context: Context
    ): VoiceCommandHandler {
        return VoiceCommandHandler(context)
    }
    
    @Provides
    @Singleton
    fun provideGestureCustomizationRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): GestureCustomizationRepository {
        return GestureCustomizationRepository(context, gson)
    }
}