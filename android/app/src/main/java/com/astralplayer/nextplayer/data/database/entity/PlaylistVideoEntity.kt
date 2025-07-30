package com.astralplayer.nextplayer.data.database.entity

import androidx.room.*

@Entity(
    tableName = "playlist_videos",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlist_id"]),
        Index(value = ["video_path"])
    ]
)
data class PlaylistVideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,
    
    @ColumnInfo(name = "video_path")
    val videoPath: String,
    
    @ColumnInfo(name = "video_name")
    val videoName: String,
    
    @ColumnInfo(name = "duration")
    val duration: Long = 0L, // in milliseconds
    
    @ColumnInfo(name = "position")
    val position: Int, // Order in playlist
    
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null,
    
    @ColumnInfo(name = "last_played_position")
    val lastPlayedPosition: Long = 0L, // Resume position
    
    @ColumnInfo(name = "is_watched")
    val isWatched: Boolean = false,
    
    @ColumnInfo(name = "added_date")
    val addedDate: Long = System.currentTimeMillis()
)