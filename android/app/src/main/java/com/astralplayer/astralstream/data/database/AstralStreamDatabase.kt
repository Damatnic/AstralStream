package com.astralplayer.astralstream.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.astralplayer.astralstream.data.entity.VideoEntity
import com.astralplayer.astralstream.data.entity.PlaylistEntity
import com.astralplayer.astralstream.data.entity.PlaybackStateEntity
import com.astralplayer.astralstream.data.entity.SettingsEntity
import com.astralplayer.astralstream.data.entity.SubtitleEntity
import com.astralplayer.astralstream.data.entity.CachedSubtitleEntity
import com.astralplayer.astralstream.data.dao.VideoDao
import com.astralplayer.astralstream.data.dao.PlaylistDao
import com.astralplayer.astralstream.data.dao.PlaybackStateDao
import com.astralplayer.astralstream.data.dao.SettingsDao
import com.astralplayer.astralstream.data.dao.SubtitleDao
import com.astralplayer.astralstream.data.dao.SubtitleCacheDao

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        PlaybackStateEntity::class,
        SettingsEntity::class,
        SubtitleEntity::class,
        CachedSubtitleEntity::class
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
    abstract fun subtitleCacheDao(): SubtitleCacheDao
    
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                MIGRATION_1_2
            )
        }
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create cached_subtitles table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_subtitles (
                        id TEXT PRIMARY KEY NOT NULL,
                        videoId TEXT NOT NULL,
                        videoUrl TEXT NOT NULL,
                        videoTitle TEXT NOT NULL,
                        language TEXT NOT NULL,
                        languageCode TEXT NOT NULL,
                        content TEXT NOT NULL,
                        format TEXT NOT NULL,
                        isEncrypted INTEGER NOT NULL DEFAULT 1,
                        encryptionIv TEXT,
                        createdTime INTEGER NOT NULL,
                        lastAccessTime INTEGER NOT NULL,
                        accessCount INTEGER NOT NULL DEFAULT 0,
                        fileSize INTEGER NOT NULL,
                        checksum TEXT NOT NULL,
                        source TEXT NOT NULL,
                        confidence REAL NOT NULL DEFAULT 1.0,
                        syncOffset INTEGER NOT NULL DEFAULT 0,
                        version INTEGER NOT NULL DEFAULT 1,
                        compressionType TEXT NOT NULL DEFAULT 'NONE',
                        originalSize INTEGER NOT NULL,
                        tags TEXT NOT NULL DEFAULT '',
                        providerName TEXT NOT NULL DEFAULT '',
                        processingTime INTEGER NOT NULL DEFAULT 0,
                        qualityScore REAL NOT NULL DEFAULT 1.0
                    )
                """.trimIndent())
                
                // Create indices for performance
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_cached_subtitles_videoId_language ON cached_subtitles(videoId, language)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cached_subtitles_lastAccessTime ON cached_subtitles(lastAccessTime)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cached_subtitles_fileSize ON cached_subtitles(fileSize)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cached_subtitles_source ON cached_subtitles(source)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cached_subtitles_createdTime ON cached_subtitles(createdTime)")
            }
        }
    }
}