package com.astralstream.nextplayer.di

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VideoCacheModule {
    
    @Provides
    @Singleton
    fun provideVideoCache(@ApplicationContext context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "video_cache")
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024) // 500MB
        val databaseProvider = StandaloneDatabaseProvider(context)
        
        return SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }
}