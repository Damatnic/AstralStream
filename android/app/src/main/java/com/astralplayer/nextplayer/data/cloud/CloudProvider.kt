package com.astralplayer.nextplayer.data.cloud

enum class CloudProvider(
    val displayName: String,
    val supportedFeatures: Set<CloudFeature>,
    val maxFileSize: Long = Long.MAX_VALUE,
    val supportedFormats: Set<String> = emptySet()
) {
    GOOGLE_DRIVE(
        displayName = "Google Drive",
        supportedFeatures = setOf(
            CloudFeature.FILE_LISTING,
            CloudFeature.DIRECT_STREAMING,
            CloudFeature.DOWNLOAD,
            CloudFeature.UPLOAD,
            CloudFeature.SHARING,
            CloudFeature.FOLDER_SUPPORT
        ),
        maxFileSize = 5L * 1024 * 1024 * 1024, // 5GB
        supportedFormats = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v")
    ),
    
    DROPBOX(
        displayName = "Dropbox",
        supportedFeatures = setOf(
            CloudFeature.FILE_LISTING,
            CloudFeature.DIRECT_STREAMING,
            CloudFeature.DOWNLOAD,
            CloudFeature.UPLOAD,
            CloudFeature.SHARING
        ),
        maxFileSize = 50L * 1024 * 1024 * 1024, // 50GB
        supportedFormats = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp")
    ),
    
    ONEDRIVE(
        displayName = "OneDrive",
        supportedFeatures = setOf(
            CloudFeature.FILE_LISTING,
            CloudFeature.DIRECT_STREAMING,
            CloudFeature.DOWNLOAD,
            CloudFeature.UPLOAD,
            CloudFeature.SHARING,
            CloudFeature.FOLDER_SUPPORT
        ),
        maxFileSize = 100L * 1024 * 1024 * 1024, // 100GB
        supportedFormats = setOf("mp4", "avi", "mkv", "mov", "wmv", "webm", "m4v")
    ),
    
    ICLOUD(
        displayName = "iCloud Drive",
        supportedFeatures = setOf(
            CloudFeature.FILE_LISTING,
            CloudFeature.DOWNLOAD
        ),
        maxFileSize = 50L * 1024 * 1024 * 1024, // 50GB
        supportedFormats = setOf("mp4", "mov", "m4v")
    ),
    
    MEGA(
        displayName = "MEGA",
        supportedFeatures = setOf(
            CloudFeature.FILE_LISTING,
            CloudFeature.DIRECT_STREAMING,
            CloudFeature.DOWNLOAD,
            CloudFeature.UPLOAD,
            CloudFeature.SHARING,
            CloudFeature.FOLDER_SUPPORT
        ),
        maxFileSize = Long.MAX_VALUE, // No specific limit
        supportedFormats = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ogv")
    ),
    
    PCLOUD(
        displayName = "pCloud",
        supportedFeatures = setOf(
            CloudFeature.FILE_LISTING,
            CloudFeature.DIRECT_STREAMING,
            CloudFeature.DOWNLOAD,
            CloudFeature.UPLOAD,
            CloudFeature.SHARING,
            CloudFeature.FOLDER_SUPPORT
        ),
        maxFileSize = Long.MAX_VALUE,
        supportedFormats = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v")
    ),
    
    NEXTCLOUD(
        displayName = "Nextcloud",
        supportedFeatures = setOf(
            CloudFeature.FILE_LISTING,
            CloudFeature.DIRECT_STREAMING,
            CloudFeature.DOWNLOAD,
            CloudFeature.UPLOAD,
            CloudFeature.SHARING,
            CloudFeature.FOLDER_SUPPORT,
            CloudFeature.WEBDAV_SUPPORT
        ),
        maxFileSize = Long.MAX_VALUE,
        supportedFormats = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "ogv", "3gp")
    ),
    
    WEBDAV(
        displayName = "WebDAV",
        supportedFeatures = setOf(
            CloudFeature.FILE_LISTING,
            CloudFeature.DIRECT_STREAMING,
            CloudFeature.DOWNLOAD,
            CloudFeature.UPLOAD,
            CloudFeature.FOLDER_SUPPORT,
            CloudFeature.WEBDAV_SUPPORT
        ),
        maxFileSize = Long.MAX_VALUE,
        supportedFormats = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "ogv", "3gp", "asf")
    ),
    
    SMB(
        displayName = "SMB/CIFS",
        supportedFeatures = setOf(
            CloudFeature.FILE_LISTING,
            CloudFeature.DIRECT_STREAMING,
            CloudFeature.DOWNLOAD,
            CloudFeature.FOLDER_SUPPORT,
            CloudFeature.NETWORK_SHARE
        ),
        maxFileSize = Long.MAX_VALUE,
        supportedFormats = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "ogv", "3gp", "asf", "ts", "m2ts")
    ),
    
    FTP(
        displayName = "FTP/SFTP",
        supportedFeatures = setOf(
            CloudFeature.FILE_LISTING,
            CloudFeature.DIRECT_STREAMING,
            CloudFeature.DOWNLOAD,
            CloudFeature.UPLOAD,
            CloudFeature.FOLDER_SUPPORT,
            CloudFeature.NETWORK_SHARE
        ),
        maxFileSize = Long.MAX_VALUE,
        supportedFormats = setOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "ogv", "3gp", "asf", "ts", "m2ts")
    );
    
    fun supportsFeature(feature: CloudFeature): Boolean {
        return feature in supportedFeatures
    }
    
    fun supportsFormat(format: String): Boolean {
        return supportedFormats.isEmpty() || format.lowercase() in supportedFormats
    }
    
    fun canStreamDirectly(): Boolean {
        return supportsFeature(CloudFeature.DIRECT_STREAMING)
    }
    
    fun requiresDownload(): Boolean {
        return !canStreamDirectly()
    }
    
    fun isNetworkShare(): Boolean {
        return supportsFeature(CloudFeature.NETWORK_SHARE)
    }
}

enum class CloudFeature {
    FILE_LISTING,
    DIRECT_STREAMING,
    DOWNLOAD,
    UPLOAD,
    SHARING,
    FOLDER_SUPPORT,
    WEBDAV_SUPPORT,
    NETWORK_SHARE,
    OFFLINE_SYNC,
    THUMBNAILS,
    SEARCH,
    VERSIONING
}

data class CloudProviderConfig(
    val provider: CloudProvider,
    val isEnabled: Boolean = false,
    val authToken: String? = null,
    val refreshToken: String? = null,
    val serverUrl: String? = null, // For self-hosted solutions like Nextcloud, WebDAV
    val username: String? = null,
    val lastSyncTime: Long = 0L,
    val syncIntervalMs: Long = 30 * 60 * 1000L, // 30 minutes default
    val autoSync: Boolean = false,
    val downloadQuality: VideoQuality = VideoQuality.AUTO,
    val maxCacheSize: Long = 1024 * 1024 * 1024L, // 1GB default
    val settings: Map<String, Any> = emptyMap()
)

enum class VideoQuality {
    AUTO,
    LOW,
    MEDIUM,
    HIGH,
    ORIGINAL
}

data class CloudFileMetadata(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val mimeType: String?,
    val modifiedTime: Long,
    val isFolder: Boolean = false,
    val parentId: String? = null,
    val downloadUrl: String? = null,
    val streamingUrl: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Long? = null, // For video files
    val resolution: String? = null, // For video files
    val bitrate: Int? = null, // For video files
    val codec: String? = null, // For video files
    val checksumMd5: String? = null,
    val sharedLink: String? = null,
    val permissions: Set<CloudPermission> = emptySet(),
    val tags: List<String> = emptyList(),
    val customMetadata: Map<String, String> = emptyMap()
)

enum class CloudPermission {
    READ,
    WRITE,
    DELETE,
    SHARE,
    ADMIN
}

data class CloudSyncStatus(
    val provider: CloudProvider,
    val status: SyncStatus,
    val lastSyncTime: Long,
    val totalFiles: Int = 0,
    val syncedFiles: Int = 0,
    val failedFiles: Int = 0,
    val bytesTotal: Long = 0L,
    val bytesSynced: Long = 0L,
    val currentFile: String? = null,
    val errorMessage: String? = null,
    val estimatedTimeRemaining: Long? = null
)

enum class SyncStatus {
    IDLE,
    SYNCING,
    COMPLETED,
    FAILED,
    PAUSED,
    CANCELLED
}

data class CloudStorageInfo(
    val provider: CloudProvider,
    val totalSpace: Long,
    val usedSpace: Long,
    val availableSpace: Long,
    val quotaInfo: QuotaInfo? = null
) {
    val usedPercentage: Float
        get() = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace) * 100f else 0f
}

data class QuotaInfo(
    val type: QuotaType,
    val limit: Long,
    val used: Long,
    val resetTime: Long? = null
)

enum class QuotaType {
    STORAGE,
    BANDWIDTH,
    REQUESTS
}