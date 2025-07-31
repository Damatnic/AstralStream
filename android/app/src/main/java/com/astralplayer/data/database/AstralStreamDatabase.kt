package com.astralplayer.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.astralplayer.data.entity.OfflineVideoEntity
import com.astralplayer.data.dao.OfflineVideoDao
import com.astralplayer.astralstream.data.entity.*
import com.astralplayer.astralstream.data.dao.*
import com.astralplayer.astralstream.data.database.Converters

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        PlaybackStateEntity::class,
        SettingsEntity::class,
        SubtitleEntity::class,
        OfflineVideoEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AstralStreamDatabase : RoomDatabase() {
    
    abstract fun videoDao(): VideoDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackStateDao(): PlaybackStateDao
    abstract fun settingsDao(): SettingsDao
    abstract fun subtitleDao(): SubtitleDao
    abstract fun offlineVideoDao(): OfflineVideoDao
    
    companion object {
        @Volatile
        private var INSTANCE: AstralStreamDatabase? = null
        
        const val DATABASE_NAME = "astralstream_database"
        
        fun getDatabase(context: Context): AstralStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AstralStreamDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `offline_videos` (
                        `videoId` TEXT NOT NULL,
                        `originalUri` TEXT NOT NULL,
                        `localPath` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `duration` INTEGER NOT NULL,
                        `thumbnailPath` TEXT,
                        `fileSize` INTEGER NOT NULL,
                        `downloadedAt` INTEGER NOT NULL,
                        `lastPlayedAt` INTEGER,
                        `watchProgress` INTEGER NOT NULL,
                        PRIMARY KEY(`videoId`)
                    )
                """)
            }
        }
    }
}