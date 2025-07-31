package com.astralplayer.nextplayer.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Comprehensive settings backup and restore manager
 */
class SettingsBackupManager(private val context: Context) {
    
    private val _backupProgress = MutableSharedFlow<BackupProgress>()
    val backupProgress: SharedFlow<BackupProgress> = _backupProgress.asSharedFlow()
    
    private val _restoreProgress = MutableSharedFlow<RestoreProgress>()
    val restoreProgress: SharedFlow<RestoreProgress> = _restoreProgress.asSharedFlow()
    
    private val backupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Create complete backup of all settings and data
     */
    suspend fun createBackup(
        backupUri: Uri,
        config: BackupConfig = BackupConfig()
    ): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            _backupProgress.emit(BackupProgress.Started())
            
            val backupData = BackupData(
                metadata = BackupMetadata(
                    appVersion = getAppVersion(),
                    createdAt = System.currentTimeMillis(),
                    deviceInfo = getDeviceInfo(),
                    backupVersion = BACKUP_VERSION
                ),
                userPreferences = if (config.includeUserPreferences) getUserPreferences() else null,
                playerSettings = if (config.includePlayerSettings) getPlayerSettings() else null,
                gestureSettings = if (config.includeGestureSettings) getGestureSettings() else null,
                uiSettings = if (config.includeUiSettings) getUiSettings() else null,
                playlists = if (config.includePlaylists) getPlaylists() else null,
                favorites = if (config.includeFavorites) getFavorites() else null,
                history = if (config.includeHistory) getHistory() else null,
                customizations = if (config.includeCustomizations) getCustomizations() else null
            )
            
            _backupProgress.emit(BackupProgress.DataCollected())
            
            // Create backup file
            val backupInfo = writeBackupFile(backupUri, backupData)
            
            _backupProgress.emit(BackupProgress.Completed(backupInfo))
            Result.success(backupInfo)
            
        } catch (e: Exception) {
            _backupProgress.emit(BackupProgress.Error(e))
            Result.failure(e)
        }
    }
    
    /**
     * Restore settings from backup
     */
    suspend fun restoreBackup(
        backupUri: Uri,
        config: RestoreConfig = RestoreConfig()
    ): Result<RestoreInfo> = withContext(Dispatchers.IO) {
        try {
            _restoreProgress.emit(RestoreProgress.Started())
            
            // Read and validate backup
            val backupData = readBackupFile(backupUri)
            validateBackup(backupData)
            
            _restoreProgress.emit(RestoreProgress.ValidationCompleted())
            
            var restoredItems = 0
            val totalItems = countRestorableItems(backupData, config)
            
            // Restore components based on config
            if (config.restoreUserPreferences && backupData.userPreferences != null) {
                restoreUserPreferences(backupData.userPreferences)
                restoredItems++
                _restoreProgress.emit(RestoreProgress.Progress("User Preferences", restoredItems, totalItems))
            }
            
            if (config.restorePlayerSettings && backupData.playerSettings != null) {
                restorePlayerSettings(backupData.playerSettings)
                restoredItems++
                _restoreProgress.emit(RestoreProgress.Progress("Player Settings", restoredItems, totalItems))
            }
            
            if (config.restoreGestureSettings && backupData.gestureSettings != null) {
                restoreGestureSettings(backupData.gestureSettings)
                restoredItems++
                _restoreProgress.emit(RestoreProgress.Progress("Gesture Settings", restoredItems, totalItems))
            }
            
            if (config.restoreUiSettings && backupData.uiSettings != null) {
                restoreUiSettings(backupData.uiSettings)
                restoredItems++
                _restoreProgress.emit(RestoreProgress.Progress("UI Settings", restoredItems, totalItems))
            }
            
            if (config.restorePlaylists && backupData.playlists != null) {
                restorePlaylists(backupData.playlists)
                restoredItems++
                _restoreProgress.emit(RestoreProgress.Progress("Playlists", restoredItems, totalItems))
            }
            
            if (config.restoreFavorites && backupData.favorites != null) {
                restoreFavorites(backupData.favorites)
                restoredItems++
                _restoreProgress.emit(RestoreProgress.Progress("Favorites", restoredItems, totalItems))
            }
            
            if (config.restoreHistory && backupData.history != null) {
                restoreHistory(backupData.history)
                restoredItems++
                _restoreProgress.emit(RestoreProgress.Progress("History", restoredItems, totalItems))
            }
            
            if (config.restoreCustomizations && backupData.customizations != null) {
                restoreCustomizations(backupData.customizations)
                restoredItems++
                _restoreProgress.emit(RestoreProgress.Progress("Customizations", restoredItems, totalItems))
            }
            
            val restoreInfo = RestoreInfo(
                backupMetadata = backupData.metadata,
                restoredComponents = restoredItems,
                totalComponents = totalItems,
                restoredAt = System.currentTimeMillis(),
                requiresRestart = config.restorePlayerSettings || config.restoreGestureSettings
            )
            
            _restoreProgress.emit(RestoreProgress.Completed(restoreInfo))
            Result.success(restoreInfo)
            
        } catch (e: Exception) {
            _restoreProgress.emit(RestoreProgress.Error(e))
            Result.failure(e)
        }
    }
    
    /**
     * Get backup information without restoring
     */
    suspend fun getBackupInfo(backupUri: Uri): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val backupData = readBackupFile(backupUri)
            Result.success(backupData.metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export settings to JSON format
     */
    suspend fun exportSettingsAsJson(outputUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backupData = BackupData(
                metadata = BackupMetadata(
                    appVersion = getAppVersion(),
                    createdAt = System.currentTimeMillis(),
                    deviceInfo = getDeviceInfo(),
                    backupVersion = BACKUP_VERSION
                ),
                userPreferences = getUserPreferences(),
                playerSettings = getPlayerSettings(),
                gestureSettings = getGestureSettings(),
                uiSettings = getUiSettings(),
                playlists = getPlaylists(),
                favorites = getFavorites(),
                history = null, // Don't include history in JSON export for privacy
                customizations = getCustomizations()
            )
            
            val json = Json.encodeToString(backupData)
            
            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                output.write(json.toByteArray())
            }
            
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Import settings from JSON format
     */
    suspend fun importSettingsFromJson(inputUri: Uri): Result<RestoreInfo> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(inputUri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: throw IOException("Cannot read backup file")
            
            val backupData = Json.decodeFromString<BackupData>(json)
            
            // Restore all available components
            var restoredItems = 0
            
            backupData.userPreferences?.let {
                restoreUserPreferences(it)
                restoredItems++
            }
            
            backupData.playerSettings?.let {
                restorePlayerSettings(it)
                restoredItems++
            }
            
            backupData.gestureSettings?.let {
                restoreGestureSettings(it)
                restoredItems++
            }
            
            backupData.uiSettings?.let {
                restoreUiSettings(it)
                restoredItems++
            }
            
            backupData.playlists?.let {
                restorePlaylists(it)
                restoredItems++
            }
            
            backupData.favorites?.let {
                restoreFavorites(it)
                restoredItems++
            }
            
            backupData.customizations?.let {
                restoreCustomizations(it)
                restoredItems++
            }
            
            val restoreInfo = RestoreInfo(
                backupMetadata = backupData.metadata,
                restoredComponents = restoredItems,
                totalComponents = restoredItems,
                restoredAt = System.currentTimeMillis(),
                requiresRestart = true
            )
            
            Result.success(restoreInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create automatic backup
     */
    suspend fun createAutoBackup(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "auto_backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // Keep only last 5 auto backups
            cleanOldAutoBackups(backupDir, 5)
            
            val backupFile = File(backupDir, "auto_backup_${System.currentTimeMillis()}.asb")
            val backupUri = Uri.fromFile(backupFile)
            
            val backupResult = createBackup(backupUri, BackupConfig.createQuickBackup())
            
            if (backupResult.isSuccess) {
                Result.success(backupFile)
            } else {
                Result.failure(backupResult.exceptionOrNull() ?: Exception("Auto backup failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun writeBackupFile(uri: Uri, backupData: BackupData): BackupInfo {
        val json = Json.encodeToString(backupData)
        
        context.contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                // Add metadata
                zip.putNextEntry(ZipEntry("metadata.json"))
                zip.write(Json.encodeToString(backupData.metadata).toByteArray())
                zip.closeEntry()
                
                // Add main data
                zip.putNextEntry(ZipEntry("backup_data.json"))
                zip.write(json.toByteArray())
                zip.closeEntry()
                
                // Add version info
                zip.putNextEntry(ZipEntry("version.txt"))
                zip.write(BACKUP_VERSION.toString().toByteArray())
                zip.closeEntry()
            }
        }
        
        return BackupInfo(
            metadata = backupData.metadata,
            fileSizeBytes = getFileSize(uri),
            createdAt = System.currentTimeMillis()
        )
    }
    
    private suspend fun readBackupFile(uri: Uri): BackupData {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var backupData: BackupData? = null
                
                while (true) {
                    val entry = zip.nextEntry ?: break
                    
                    when (entry.name) {
                        "backup_data.json" -> {
                            val jsonData = zip.readBytes().toString(Charsets.UTF_8)
                            backupData = Json.decodeFromString(jsonData)
                        }
                    }
                    
                    zip.closeEntry()
                }
                
                return backupData ?: throw IOException("Invalid backup file format")
            }
        } ?: throw IOException("Cannot read backup file")
    }
    
    private fun validateBackup(backupData: BackupData) {
        if (backupData.metadata.backupVersion > BACKUP_VERSION) {
            throw UnsupportedOperationException("Backup was created with a newer version of the app")
        }
    }
    
    private fun countRestorableItems(backupData: BackupData, config: RestoreConfig): Int {
        var count = 0
        if (config.restoreUserPreferences && backupData.userPreferences != null) count++
        if (config.restorePlayerSettings && backupData.playerSettings != null) count++
        if (config.restoreGestureSettings && backupData.gestureSettings != null) count++
        if (config.restoreUiSettings && backupData.uiSettings != null) count++
        if (config.restorePlaylists && backupData.playlists != null) count++
        if (config.restoreFavorites && backupData.favorites != null) count++
        if (config.restoreHistory && backupData.history != null) count++
        if (config.restoreCustomizations && backupData.customizations != null) count++
        return count
    }
    
    private fun getUserPreferences(): Map<String, Any> {
        val prefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        return prefs.all.mapValues { it.value ?: "" }
    }
    
    private fun getPlayerSettings(): Map<String, Any> {
        val prefs = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        return prefs.all.mapValues { it.value ?: "" }
    }
    
    private fun getGestureSettings(): Map<String, Any> {
        val prefs = context.getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)
        return prefs.all.mapValues { it.value ?: "" }
    }
    
    private fun getUiSettings(): Map<String, Any> {
        val prefs = context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE)
        return prefs.all.mapValues { it.value ?: "" }
    }
    
    private fun getPlaylists(): List<PlaylistBackup> {
        // In a real implementation, this would read from your playlist database
        return emptyList()
    }
    
    private fun getFavorites(): List<String> {
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        return prefs.getStringSet("favorite_videos", emptySet())?.toList() ?: emptyList()
    }
    
    private fun getHistory(): List<HistoryEntry> {
        // In a real implementation, this would read from your history database
        return emptyList()
    }
    
    private fun getCustomizations(): Map<String, Any> {
        val prefs = context.getSharedPreferences("customizations", Context.MODE_PRIVATE)
        return prefs.all.mapValues { it.value ?: "" }
    }
    
    private fun restoreUserPreferences(preferences: Map<String, Any>) {
        val prefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        preferences.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        
        editor.apply()
    }
    
    private fun restorePlayerSettings(settings: Map<String, Any>) {
        val prefs = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        settings.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        
        editor.apply()
    }
    
    private fun restoreGestureSettings(settings: Map<String, Any>) {
        val prefs = context.getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        settings.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        
        editor.apply()
    }
    
    private fun restoreUiSettings(settings: Map<String, Any>) {
        val prefs = context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        settings.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        
        editor.apply()
    }
    
    private fun restorePlaylists(playlists: List<PlaylistBackup>) {
        // In a real implementation, this would write to your playlist database
    }
    
    private fun restoreFavorites(favorites: List<String>) {
        val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("favorite_videos", favorites.toSet()).apply()
    }
    
    private fun restoreHistory(history: List<HistoryEntry>) {
        // In a real implementation, this would write to your history database
    }
    
    private fun restoreCustomizations(customizations: Map<String, Any>) {
        val prefs = context.getSharedPreferences("customizations", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        customizations.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        
        editor.apply()
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = android.os.Build.MANUFACTURER,
            model = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            apiLevel = android.os.Build.VERSION.SDK_INT
        )
    }
    
    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun cleanOldAutoBackups(backupDir: File, keepCount: Int) {
        val backupFiles = backupDir.listFiles { file ->
            file.name.startsWith("auto_backup_") && file.name.endsWith(".asb")
        } ?: return
        
        if (backupFiles.size > keepCount) {
            backupFiles.sortBy { it.lastModified() }
            backupFiles.take(backupFiles.size - keepCount).forEach { it.delete() }
        }
    }
    
    fun cleanup() {
        backupScope.cancel()
    }
    
    companion object {
        private const val BACKUP_VERSION = 1
    }
}

// Data classes for backup system
@Serializable
data class BackupData(
    val metadata: BackupMetadata,
    val userPreferences: Map<String, String>? = null,
    val playerSettings: Map<String, String>? = null,
    val gestureSettings: Map<String, String>? = null,
    val uiSettings: Map<String, String>? = null,
    val playlists: List<PlaylistBackup>? = null,
    val favorites: List<String>? = null,
    val history: List<HistoryEntry>? = null,
    val customizations: Map<String, String>? = null
)

@Serializable
data class BackupMetadata(
    val appVersion: String,
    val createdAt: Long,
    val deviceInfo: DeviceInfo,
    val backupVersion: Int
)

@Serializable
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int
)

@Serializable
data class PlaylistBackup(
    val name: String,
    val videos: List<String>,
    val createdAt: Long
)

@Serializable
data class HistoryEntry(
    val videoUri: String,
    val title: String,
    val lastPosition: Long,
    val watchCount: Int,
    val lastWatched: Long
)

data class BackupConfig(
    val includeUserPreferences: Boolean = true,
    val includePlayerSettings: Boolean = true,
    val includeGestureSettings: Boolean = true,
    val includeUiSettings: Boolean = true,
    val includePlaylists: Boolean = true,
    val includeFavorites: Boolean = true,
    val includeHistory: Boolean = false, // Privacy sensitive
    val includeCustomizations: Boolean = true
) {
    companion object {
        fun createFullBackup() = BackupConfig(includeHistory = true)
        fun createQuickBackup() = BackupConfig(includeHistory = false)
        fun createSettingsOnly() = BackupConfig(
            includePlaylists = false,
            includeFavorites = false,
            includeHistory = false
        )
    }
}

data class RestoreConfig(
    val restoreUserPreferences: Boolean = true,
    val restorePlayerSettings: Boolean = true,
    val restoreGestureSettings: Boolean = true,
    val restoreUiSettings: Boolean = true,
    val restorePlaylists: Boolean = true,
    val restoreFavorites: Boolean = true,
    val restoreHistory: Boolean = false,
    val restoreCustomizations: Boolean = true
)

data class BackupInfo(
    val metadata: BackupMetadata,
    val fileSizeBytes: Long,
    val createdAt: Long
)

data class RestoreInfo(
    val backupMetadata: BackupMetadata,
    val restoredComponents: Int,
    val totalComponents: Int,
    val restoredAt: Long,
    val requiresRestart: Boolean
)

sealed class BackupProgress {
    object Started : BackupProgress()
    object DataCollected : BackupProgress()
    data class Completed(val backupInfo: BackupInfo) : BackupProgress()
    data class Error(val error: Throwable) : BackupProgress()
}

sealed class RestoreProgress {
    object Started : RestoreProgress()
    object ValidationCompleted : RestoreProgress()
    data class Progress(val component: String, val current: Int, val total: Int) : RestoreProgress()
    data class Completed(val restoreInfo: RestoreInfo) : RestoreProgress()
    data class Error(val error: Throwable) : RestoreProgress()
}