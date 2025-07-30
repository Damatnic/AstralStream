package com.astralplayer.astralstream.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.astralplayer.astralstream.core.database.dao.DirectoryDao
import com.astralplayer.astralstream.core.database.dao.MediumDao
import com.astralplayer.astralstream.core.database.entities.AudioStreamInfoEntity
import com.astralplayer.astralstream.core.database.entities.DirectoryEntity
import com.astralplayer.astralstream.core.database.entities.MediumEntity
import com.astralplayer.astralstream.core.database.entities.SubtitleStreamInfoEntity
import com.astralplayer.astralstream.core.database.entities.VideoStreamInfoEntity

@Database(
    entities = [
        DirectoryEntity::class,
        MediumEntity::class,
        VideoStreamInfoEntity::class,
        AudioStreamInfoEntity::class,
        SubtitleStreamInfoEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MediaDatabase : RoomDatabase() {

    abstract fun mediumDao(): MediumDao

    abstract fun directoryDao(): DirectoryDao

    companion object {
        const val DATABASE_NAME = "media_db"
    }
}
