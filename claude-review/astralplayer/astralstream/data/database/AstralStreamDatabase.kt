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
import com.astralplayer.community.data.*
import com.astralplayer.community.dao.*
import com.astralplayer.features.gestures.data.*
import com.astralplayer.features.gestures.dao.GestureDao

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        PlaybackStateEntity::class,
        SettingsEntity::class,
        SubtitleEntity::class,
        CachedSubtitleEntity::class,
        SharedPlaylistEntity::class,
        CommunitySubtitleEntity::class,
        SubtitleVoteEntity::class,
        SubtitleReportEntity::class,
        SubtitleDownloadEntity::class,
        SubtitleQualityReportEntity::class,
        PlaylistViewEntity::class,
        PlaylistRatingEntity::class,
        PlaylistCommentEntity::class,
        GestureEntity::class,
        GestureProfileEntity::class,
        GestureProfileMappingEntity::class,
        GestureHistoryEntity::class,
        GestureShortcutEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class, CommunityConverters::class, GestureConverters::class)
abstract class AstralStreamDatabase : RoomDatabase() {
    
    abstract fun videoDao(): VideoDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackStateDao(): PlaybackStateDao
    abstract fun settingsDao(): SettingsDao
    abstract fun subtitleDao(): SubtitleDao
    abstract fun subtitleCacheDao(): SubtitleCacheDao
    abstract fun sharedPlaylistDao(): SharedPlaylistDao
    abstract fun communitySubtitleDao(): CommunitySubtitleDao
    abstract fun subtitleVoteDao(): SubtitleVoteDao
    abstract fun subtitleReportDao(): SubtitleReportDao
    abstract fun subtitleDownloadDao(): SubtitleDownloadDao
    abstract fun playlistRatingDao(): PlaylistRatingDao
    abstract fun gestureDao(): GestureDao
    
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
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4
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
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create shared_playlists table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shared_playlists (
                        id TEXT PRIMARY KEY NOT NULL,
                        originalPlaylistId TEXT NOT NULL,
                        shareCode TEXT NOT NULL UNIQUE,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        creatorName TEXT NOT NULL,
                        creatorId TEXT NOT NULL,
                        videoCount INTEGER NOT NULL,
                        totalDuration INTEGER NOT NULL,
                        thumbnailUrl TEXT,
                        shareUrl TEXT NOT NULL,
                        password TEXT,
                        isPublic INTEGER NOT NULL DEFAULT 1,
                        allowDownloads INTEGER NOT NULL DEFAULT 1,
                        allowComments INTEGER NOT NULL DEFAULT 1,
                        category TEXT NOT NULL,
                        language TEXT NOT NULL,
                        tags TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        expirationTime INTEGER NOT NULL,
                        viewCount INTEGER NOT NULL DEFAULT 0,
                        downloadCount INTEGER NOT NULL DEFAULT 0,
                        likeCount INTEGER NOT NULL DEFAULT 0,
                        commentCount INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        maxViews INTEGER,
                        currentViews INTEGER NOT NULL DEFAULT 0,
                        metadata TEXT NOT NULL DEFAULT '{}'
                    )
                """.trimIndent())
                
                // Create indices for shared_playlists
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shared_playlists_shareCode ON shared_playlists(shareCode)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shared_playlists_creatorId ON shared_playlists(creatorId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shared_playlists_createdAt ON shared_playlists(createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shared_playlists_category ON shared_playlists(category)")
                
                // Create community_subtitles table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS community_subtitles (
                        id TEXT PRIMARY KEY NOT NULL,
                        videoHash TEXT NOT NULL,
                        videoTitle TEXT NOT NULL,
                        videoDuration INTEGER NOT NULL,
                        language TEXT NOT NULL,
                        languageCode TEXT NOT NULL,
                        content TEXT NOT NULL,
                        format TEXT NOT NULL DEFAULT 'srt',
                        contributorId TEXT NOT NULL,
                        contributorName TEXT NOT NULL DEFAULT 'Anonymous',
                        contributorReputation INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        version INTEGER NOT NULL DEFAULT 1,
                        parentSubtitleId TEXT,
                        upvotes INTEGER NOT NULL DEFAULT 0,
                        downvotes INTEGER NOT NULL DEFAULT 0,
                        confidence REAL NOT NULL DEFAULT 1.0,
                        verificationStatus TEXT NOT NULL,
                        verifiedAt INTEGER,
                        verifiedBy TEXT,
                        downloadCount INTEGER NOT NULL DEFAULT 0,
                        reportCount INTEGER NOT NULL DEFAULT 0,
                        qualityScore REAL NOT NULL DEFAULT 0.0,
                        syncOffset INTEGER NOT NULL DEFAULT 0,
                        description TEXT NOT NULL DEFAULT '',
                        tags TEXT NOT NULL DEFAULT '',
                        isOfficial INTEGER NOT NULL DEFAULT 0,
                        source TEXT NOT NULL,
                        checksum TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        encoding TEXT NOT NULL DEFAULT 'UTF-8',
                        lineCount INTEGER NOT NULL DEFAULT 0,
                        avgLineLength REAL NOT NULL DEFAULT 0.0,
                        hasTimingIssues INTEGER NOT NULL DEFAULT 0,
                        hasSpellingIssues INTEGER NOT NULL DEFAULT 0,
                        completionPercentage REAL NOT NULL DEFAULT 100.0
                    )
                """.trimIndent())
                
                // Create indices for community_subtitles
                database.execSQL("CREATE INDEX IF NOT EXISTS index_community_subtitles_videoHash_language ON community_subtitles(videoHash, language)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_community_subtitles_contributorId ON community_subtitles(contributorId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_community_subtitles_createdAt ON community_subtitles(createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_community_subtitles_verificationStatus ON community_subtitles(verificationStatus)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_community_subtitles_upvotes ON community_subtitles(upvotes)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_community_subtitles_confidence ON community_subtitles(confidence)")
                
                // Create subtitle_votes table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS subtitle_votes (
                        id TEXT PRIMARY KEY NOT NULL,
                        subtitleId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        isUpvote INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        reason TEXT NOT NULL DEFAULT '',
                        weight REAL NOT NULL DEFAULT 1.0
                    )
                """.trimIndent())
                
                // Create indices for subtitle_votes
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_subtitle_votes_subtitleId_userId ON subtitle_votes(subtitleId, userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_votes_createdAt ON subtitle_votes(createdAt)")
                
                // Create subtitle_reports table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS subtitle_reports (
                        id TEXT PRIMARY KEY NOT NULL,
                        subtitleId TEXT NOT NULL,
                        reporterId TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        resolvedAt INTEGER,
                        resolvedBy TEXT,
                        resolution TEXT NOT NULL DEFAULT '',
                        severity TEXT NOT NULL
                    )
                """.trimIndent())
                
                // Create indices for subtitle_reports
                database.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_reports_subtitleId ON subtitle_reports(subtitleId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_reports_reporterId ON subtitle_reports(reporterId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_reports_createdAt ON subtitle_reports(createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_reports_status ON subtitle_reports(status)")
                
                // Create subtitle_downloads table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS subtitle_downloads (
                        id TEXT PRIMARY KEY NOT NULL,
                        subtitleId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        downloadedAt INTEGER NOT NULL,
                        userAgent TEXT NOT NULL DEFAULT '',
                        ipAddress TEXT NOT NULL DEFAULT '',
                        success INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                
                // Create indices for subtitle_downloads
                database.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_downloads_subtitleId ON subtitle_downloads(subtitleId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_downloads_userId ON subtitle_downloads(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_downloads_downloadedAt ON subtitle_downloads(downloadedAt)")
                
                // Create subtitle_quality_reports table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS subtitle_quality_reports (
                        id TEXT PRIMARY KEY NOT NULL,
                        subtitleId TEXT NOT NULL,
                        reportedAt INTEGER NOT NULL,
                        timingAccuracy REAL NOT NULL,
                        languageAccuracy REAL NOT NULL,
                        completeness REAL NOT NULL,
                        readability REAL NOT NULL,
                        overallScore REAL NOT NULL,
                        detectedIssues TEXT NOT NULL DEFAULT '',
                        suggestedFixes TEXT NOT NULL DEFAULT '',
                        automaticScore INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                
                // Create indices for subtitle_quality_reports
                database.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_quality_reports_subtitleId ON subtitle_quality_reports(subtitleId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_quality_reports_reportedAt ON subtitle_quality_reports(reportedAt)")
                
                // Create playlist_views table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_views (
                        id TEXT PRIMARY KEY NOT NULL,
                        playlistId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        viewedAt INTEGER NOT NULL,
                        referrer TEXT NOT NULL DEFAULT '',
                        duration INTEGER NOT NULL DEFAULT 0,
                        completed INTEGER NOT NULL DEFAULT 0,
                        userAgent TEXT NOT NULL DEFAULT '',
                        ipAddress TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                
                // Create indices for playlist_views
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_views_playlistId ON playlist_views(playlistId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_views_userId ON playlist_views(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_views_viewedAt ON playlist_views(viewedAt)")
                
                // Create playlist_ratings table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_ratings (
                        id TEXT PRIMARY KEY NOT NULL,
                        playlistId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        rating REAL NOT NULL,
                        review TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        helpful INTEGER NOT NULL DEFAULT 0,
                        notHelpful INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // Create indices for playlist_ratings
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_playlist_ratings_playlistId_userId ON playlist_ratings(playlistId, userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_ratings_createdAt ON playlist_ratings(createdAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_ratings_rating ON playlist_ratings(rating)")
                
                // Create playlist_comments table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_comments (
                        id TEXT PRIMARY KEY NOT NULL,
                        playlistId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        userName TEXT NOT NULL DEFAULT 'Anonymous',
                        comment TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        parentCommentId TEXT,
                        likes INTEGER NOT NULL DEFAULT 0,
                        dislikes INTEGER NOT NULL DEFAULT 0,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        isEdited INTEGER NOT NULL DEFAULT 0,
                        replyCount INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // Create indices for playlist_comments
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_comments_playlistId ON playlist_comments(playlistId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_comments_parentCommentId ON playlist_comments(parentCommentId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_comments_createdAt ON playlist_comments(createdAt)")
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create gesture tables
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_gestures (
                        id TEXT PRIMARY KEY NOT NULL,
                        gestureType TEXT NOT NULL,
                        zone TEXT NOT NULL,
                        action TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        sensitivity REAL NOT NULL DEFAULT 1.0,
                        requiredFingers INTEGER NOT NULL DEFAULT 1,
                        direction TEXT,
                        minimumDistance REAL NOT NULL DEFAULT 50.0,
                        doubleTapTimeout INTEGER NOT NULL DEFAULT 300,
                        longPressTimeout INTEGER NOT NULL DEFAULT 500,
                        hapticFeedback INTEGER NOT NULL DEFAULT 1,
                        visualFeedback INTEGER NOT NULL DEFAULT 1,
                        priority INTEGER NOT NULL DEFAULT 0,
                        customParameters TEXT NOT NULL DEFAULT '{}',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create indices for custom_gestures
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_custom_gestures_gestureType_zone ON custom_gestures(gestureType, zone)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_custom_gestures_isEnabled ON custom_gestures(isEnabled)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_custom_gestures_priority ON custom_gestures(priority)")
                
                // Create gesture_profiles table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS gesture_profiles (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        isActive INTEGER NOT NULL DEFAULT 0,
                        isBuiltIn INTEGER NOT NULL DEFAULT 0,
                        baseProfileId TEXT,
                        iconName TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create indices for gesture_profiles
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_gesture_profiles_name ON gesture_profiles(name)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_gesture_profiles_isActive ON gesture_profiles(isActive)")
                
                // Create gesture_profile_mappings table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS gesture_profile_mappings (
                        id TEXT PRIMARY KEY NOT NULL,
                        profileId TEXT NOT NULL,
                        gestureId TEXT NOT NULL,
                        isOverride INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // Create indices for gesture_profile_mappings
                database.execSQL("CREATE INDEX IF NOT EXISTS index_gesture_profile_mappings_profileId ON gesture_profile_mappings(profileId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_gesture_profile_mappings_gestureId ON gesture_profile_mappings(gestureId)")
                
                // Create gesture_history table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS gesture_history (
                        id TEXT PRIMARY KEY NOT NULL,
                        gestureType TEXT NOT NULL,
                        zone TEXT NOT NULL,
                        action TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        startX REAL NOT NULL,
                        startY REAL NOT NULL,
                        endX REAL NOT NULL,
                        endY REAL NOT NULL,
                        velocity REAL NOT NULL DEFAULT 0.0,
                        duration INTEGER NOT NULL DEFAULT 0,
                        fingerCount INTEGER NOT NULL DEFAULT 1,
                        wasSuccessful INTEGER NOT NULL DEFAULT 1,
                        errorReason TEXT
                    )
                """.trimIndent())
                
                // Create indices for gesture_history
                database.execSQL("CREATE INDEX IF NOT EXISTS index_gesture_history_gestureType ON gesture_history(gestureType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_gesture_history_timestamp ON gesture_history(timestamp)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_gesture_history_wasSuccessful ON gesture_history(wasSuccessful)")
                
                // Create gesture_shortcuts table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS gesture_shortcuts (
                        id TEXT PRIMARY KEY NOT NULL,
                        shortcutKey TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        primaryGestureId TEXT NOT NULL,
                        alternativeGestureId TEXT,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        showTutorial INTEGER NOT NULL DEFAULT 1,
                        usageCount INTEGER NOT NULL DEFAULT 0,
                        lastUsedAt INTEGER
                    )
                """.trimIndent())
                
                // Create indices for gesture_shortcuts
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_gesture_shortcuts_shortcutKey ON gesture_shortcuts(shortcutKey)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_gesture_shortcuts_isEnabled ON gesture_shortcuts(isEnabled)")
            }
        }
    }
}