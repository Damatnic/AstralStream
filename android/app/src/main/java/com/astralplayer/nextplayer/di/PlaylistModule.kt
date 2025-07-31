package com.astralplayer.nextplayer.di

import android.content.Context
import com.astralplayer.nextplayer.playlist.SmartPlaylistEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing smart playlist management dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object PlaylistModule {
    
    @Provides
    @Singleton
    fun provideSmartPlaylistEngine(
        @ApplicationContext context: Context
    ): SmartPlaylistEngine {
        return SmartPlaylistEngine(context)
    }
}