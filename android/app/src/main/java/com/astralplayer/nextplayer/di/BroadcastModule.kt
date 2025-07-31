package com.astralplayer.nextplayer.di

import android.content.Context
import com.astralplayer.nextplayer.broadcast.ProfessionalBroadcastingEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing professional broadcasting dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object BroadcastModule {
    
    @Provides
    @Singleton
    fun provideProfessionalBroadcastingEngine(
        @ApplicationContext context: Context
    ): ProfessionalBroadcastingEngine {
        return ProfessionalBroadcastingEngine(context)
    }
}