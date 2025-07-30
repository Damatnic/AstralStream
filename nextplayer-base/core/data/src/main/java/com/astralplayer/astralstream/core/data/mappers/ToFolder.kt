package com.astralplayer.astralstream.core.data.mappers

import com.astralplayer.astralstream.core.common.Utils
import com.astralplayer.astralstream.core.database.relations.DirectoryWithMedia
import com.astralplayer.astralstream.core.database.relations.MediumWithInfo
import com.astralplayer.astralstream.core.model.Folder

fun DirectoryWithMedia.toFolder() = Folder(
    name = directory.name,
    path = directory.path,
    dateModified = directory.modified,
    parentPath = directory.parentPath,
    formattedMediaSize = Utils.formatFileSize(media.sumOf { it.mediumEntity.size }),
    mediaList = media.map(MediumWithInfo::toVideo),
)
