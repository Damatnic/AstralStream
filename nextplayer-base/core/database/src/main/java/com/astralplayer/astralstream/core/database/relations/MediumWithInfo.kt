package com.astralplayer.astralstream.core.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.astralplayer.astralstream.core.database.entities.AudioStreamInfoEntity
import com.astralplayer.astralstream.core.database.entities.MediumEntity
import com.astralplayer.astralstream.core.database.entities.SubtitleStreamInfoEntity
import com.astralplayer.astralstream.core.database.entities.VideoStreamInfoEntity

data class MediumWithInfo(
    @Embedded val mediumEntity: MediumEntity,
    @Relation(
        parentColumn = "uri",
        entityColumn = "medium_uri",
    )
    val videoStreamInfo: VideoStreamInfoEntity?,
    @Relation(
        parentColumn = "uri",
        entityColumn = "medium_uri",
    )
    val audioStreamsInfo: List<AudioStreamInfoEntity>,
    @Relation(
        parentColumn = "uri",
        entityColumn = "medium_uri",
    )
    val subtitleStreamsInfo: List<SubtitleStreamInfoEntity>,
)
