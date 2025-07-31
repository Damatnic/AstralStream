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
}