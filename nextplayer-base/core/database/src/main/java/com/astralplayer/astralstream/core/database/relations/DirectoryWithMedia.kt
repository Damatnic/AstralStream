package com.astralplayer.astralstream.core.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.astralplayer.astralstream.core.database.entities.DirectoryEntity
import com.astralplayer.astralstream.core.database.entities.MediumEntity

data class DirectoryWithMedia(
    @Embedded val directory: DirectoryEntity,
    @Relation(
        entity = MediumEntity::class,
        parentColumn = "path",
        entityColumn = "parent_path",
    )
    val media: List<MediumWithInfo>,
)
