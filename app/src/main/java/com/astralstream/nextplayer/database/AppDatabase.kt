package com.astralstream.nextplayer.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.astralstream.nextplayer.database.entities.CachedSubtitleEntity
import com.astralstream.nextplayer.database.entities.PlaylistEntity
import com.astralstream.nextplayer.database.entities.PlaylistVideoEntity
import com.astralstream.nextplayer.database.entities.SharedPlaylistEntity
import com.astralstream.nextplayer.database.dao.SubtitleCacheDao
import com.astralstream.nextplayer.database.dao.PlaylistDao
import com.astralstream.nextplayer.database.dao.PlaylistVideoDao

@Database(
    entities = [
        CachedSubtitleEntity::class,
        PlaylistEntity::class,
        PlaylistVideoEntity::class,
        SharedPlaylistEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subtitleCacheDao(): SubtitleCacheDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistVideoDao(): PlaylistVideoDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "astralstream_database"
                )
                .addMigrations(MIGRATION_1_TO_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private val MIGRATION_1_TO_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_subtitles (
                        cacheKey TEXT PRIMARY KEY NOT NULL,
                        videoUri TEXT NOT NULL,
                        language TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        sourceType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        lastAccessed INTEGER NOT NULL,
                        accessCount INTEGER NOT NULL,
                        confidence REAL NOT NULL,
                        isUserContributed INTEGER NOT NULL DEFAULT 0,
                        contributorId TEXT,
                        checksum TEXT
                    )
                """)
                database.execSQL("CREATE INDEX index_cached_subtitles_videoUri ON cached_subtitles(videoUri)")
                database.execSQL("CREATE INDEX index_cached_subtitles_language ON cached_subtitles(language)")
                database.execSQL("CREATE INDEX index_cached_subtitles_lastAccessed ON cached_subtitles(lastAccessed)")
                database.execSQL("CREATE INDEX index_cached_subtitles_timestamp ON cached_subtitles(timestamp)")
            }
        }
    }
}