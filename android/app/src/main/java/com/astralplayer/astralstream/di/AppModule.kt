package com.astralplayer.astralstream.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.astralplayer.astralstream.data.database.AstralStreamDatabase
import com.astralplayer.astralstream.data.repository.SettingsRepository
import com.astralplayer.astralstream.data.repository.SettingsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    @Provides
    @Singleton
    fun provideAstralStreamDatabase(
        @ApplicationContext context: Context
    ): AstralStreamDatabase {
        return Room.databaseBuilder(
            context,
            AstralStreamDatabase::class.java,
            AstralStreamDatabase.DATABASE_NAME
        )
        .addMigrations(*AstralStreamDatabase.getAllMigrations())
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
    }
    
    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideVideoDao(database: AstralStreamDatabase) = database.videoDao()
    
    @Provides
    @Singleton
    fun providePlaylistDao(database: AstralStreamDatabase) = database.playlistDao()
    
    @Provides
    @Singleton
    fun provideSubtitleDao(database: AstralStreamDatabase) = database.subtitleDao()
    
    @Provides
    @Singleton
    fun provideSettingsDao(database: AstralStreamDatabase) = database.settingsDao()
    
    @Provides
    @Singleton
    fun providePlaybackStateDao(database: AstralStreamDatabase) = database.playbackStateDao()
    
    @Provides
    @Singleton
    fun provideAISubtitleGenerator(
        @ApplicationContext context: Context
    ): com.astralplayer.astralstream.ai.AISubtitleGenerator {
        return com.astralplayer.astralstream.ai.AISubtitleGenerator(context)
    }
    
    @Provides
    @Singleton
    fun provideCloudStorageManager(
        @ApplicationContext context: Context
    ): com.astralplayer.astralstream.cloud.CloudStorageManager {
        return com.astralplayer.astralstream.cloud.CloudStorageManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAdvancedPlayerConfiguration(
        @ApplicationContext context: Context,
        database: AstralStreamDatabase,
        settingsRepository: SettingsRepository
    ): com.astralplayer.astralstream.player.AdvancedPlayerConfiguration {
        return com.astralplayer.astralstream.player.AdvancedPlayerConfiguration(
            context,
            database,
            settingsRepository
        )
    }
}