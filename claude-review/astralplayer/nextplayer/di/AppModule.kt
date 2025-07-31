package com.astralplayer.nextplayer.di

import android.app.Application
import android.content.Context
import com.astralplayer.nextplayer.data.*
import com.astralplayer.nextplayer.data.database.*
import com.astralplayer.nextplayer.feature.codec.CodecPackManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NextPlayerAppModule {
    
    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }
    
    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
    
    @Provides
    @Singleton
    fun provideCodecPackManager(@ApplicationContext context: Context): CodecPackManager {
        return CodecPackManager(context)
    }
}