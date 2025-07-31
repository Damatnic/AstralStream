package com.astralplayer.nextplayer.di

import android.content.Context
import androidx.room.Room
import com.astralplayer.nextplayer.data.database.*
import com.astralplayer.nextplayer.data.dao.VideoDao
import com.astralplayer.nextplayer.data.dao.HistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AstralVuDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AstralVuDatabase::class.java,
            AstralVuDatabase.DATABASE_NAME
        )
            .addMigrations(*AstralVuDatabase.getAllMigrations())
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAstralStreamDatabase(@ApplicationContext context: Context): AstralStreamDatabase {
        return AstralStreamDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideRecentFilesDao(database: AstralVuDatabase): RecentFilesDao {
        return database.recentFilesDao()
    }
    
    @Provides
    fun providePlaylistDao(database: AstralVuDatabase): PlaylistDao {
        return database.playlistDao()
    }
    
    @Provides
    fun provideSubtitleDao(database: AstralVuDatabase): SubtitleDao {
        return database.subtitleDao()
    }
    
    @Provides
    fun provideCloudFileDao(database: AstralVuDatabase): CloudFileDao {
        return database.cloudFileDao()
    }
    
    @Provides
    fun provideDownloadQueueDao(database: AstralVuDatabase): DownloadQueueDao {
        return database.downloadQueueDao()
    }
    
    @Provides
    fun provideUserPreferenceDao(database: AstralVuDatabase): UserPreferenceDao {
        return database.userPreferenceDao()
    }
    
    @Provides
    fun providePlaybackHistoryDao(database: AstralVuDatabase): PlaybackHistoryDao {
        return database.playbackHistoryDao()
    }
    
    @Provides
    fun provideVideoDao(database: AstralStreamDatabase): VideoDao {
        return database.videoDao()
    }
    
    @Provides
    fun provideHistoryDao(database: AstralStreamDatabase): HistoryDao {
        return database.historyDao()
    }
}