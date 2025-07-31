package com.astralplayer.nextplayer.di

import android.content.Context
import androidx.room.Room
import com.astralplayer.nextplayer.data.database.AstralVuDatabase
import com.astralplayer.nextplayer.data.repository.*
import com.astralplayer.nextplayer.utils.CodecManager
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
    fun provideDatabase(@ApplicationContext context: Context): AstralVuDatabase {
        return Room.databaseBuilder(
            context,
            AstralVuDatabase::class.java,
            AstralVuDatabase.DATABASE_NAME
        )
        .addMigrations(*AstralVuDatabase.getAllMigrations())
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    @Singleton
    fun provideCodecManager(@ApplicationContext context: Context): CodecManager {
        return CodecManager(context).apply {
            initializeCodecs()
        }
    }
    
    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }
    
    @Provides
    @Singleton
    fun provideRecentFilesRepository(database: AstralVuDatabase): RecentFilesRepository {
        return RecentFilesRepositoryImpl(database.recentFileDao())
    }
    
    @Provides
    @Singleton
    fun providePlaylistRepository(database: AstralVuDatabase): PlaylistRepository {
        return PlaylistRepositoryImpl(database.playlistDao())
    }
    
    @Provides
    @Singleton
    fun provideVideoRepository(database: AstralVuDatabase): VideoRepository {
        return VideoRepositoryImpl(database.videoDao())
    }

}