package com.astralplayer.nextplayer.feature.browser

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val isVideoFile: Boolean = false,
    val extension: String = ""
)

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC,
    SIZE_ASC,
    SIZE_DESC
}

data class FolderBrowserUiState(
    val currentPath: String = "",
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isGridView: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val selectedFile: FileItem? = null,
    val searchQuery: String = "",
    val showHiddenFiles: Boolean = false
)

class FolderBrowserViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(FolderBrowserUiState())
    val uiState: StateFlow<FolderBrowserUiState> = _uiState.asStateFlow()
    
    private val videoExtensions = setOf(
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp", "3g2",
        "asf", "divx", "f4v", "m2ts", "mts", "ogv", "rm", "rmvb", "ts", "vob"
    )
    
    fun loadInitialDirectory() {
        val initialPath = Environment.getExternalStorageDirectory()?.absolutePath ?: ""
        navigateToPath(initialPath)
    }
    
    fun navigateToPath(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                currentPath = path
            )
            
            try {
                val files = loadFilesFromPath(path)
                _uiState.value = _uiState.value.copy(
                    files = files,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load directory",
                    isLoading = false
                )
            }
        }
    }
    
    fun navigateToDirectory(path: String) {
        navigateToPath(path)
    }
    
    fun refreshCurrentDirectory() {
        navigateToPath(_uiState.value.currentPath)
    }
    
    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(
            isGridView = !_uiState.value.isGridView
        )
    }
    
    fun toggleSortOrder() {
        val currentSort = _uiState.value.sortOrder
        val nextSort = when (currentSort) {
            SortOrder.NAME_ASC -> SortOrder.NAME_DESC
            SortOrder.NAME_DESC -> SortOrder.DATE_ASC
            SortOrder.DATE_ASC -> SortOrder.DATE_DESC
            SortOrder.DATE_DESC -> SortOrder.SIZE_ASC
            SortOrder.SIZE_ASC -> SortOrder.SIZE_DESC
            SortOrder.SIZE_DESC -> SortOrder.NAME_ASC
        }
        
        _uiState.value = _uiState.value.copy(sortOrder = nextSort)
        
        // Re-sort current files
        val sortedFiles = sortFiles(_uiState.value.files, nextSort)
        _uiState.value = _uiState.value.copy(files = sortedFiles)
    }
    
    fun showFileOptions(file: FileItem) {
        _uiState.value = _uiState.value.copy(selectedFile = file)
    }
    
    fun hideFileOptions() {
        _uiState.value = _uiState.value.copy(selectedFile = null)
    }
    
    fun deleteFile(file: FileItem) {
        viewModelScope.launch {
            try {
                val fileToDelete = File(file.path)
                if (fileToDelete.exists()) {
                    val deleted = if (file.isDirectory) {
                        fileToDelete.deleteRecursively()
                    } else {
                        fileToDelete.delete()
                    }
                    
                    if (deleted) {
                        // Refresh the current directory
                        refreshCurrentDirectory()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to delete ${file.name}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error deleting file: ${e.message}"
                )
            }
        }
    }
    
    fun searchFiles(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        if (query.isEmpty()) {
            refreshCurrentDirectory()
        } else {
            viewModelScope.launch {
                val allFiles = loadFilesFromPath(_uiState.value.currentPath)
                val filteredFiles = allFiles.filter { file ->
                    file.name.contains(query, ignoreCase = true)
                }
                _uiState.value = _uiState.value.copy(files = filteredFiles)
            }
        }
    }
    
    fun toggleHiddenFiles() {
        _uiState.value = _uiState.value.copy(
            showHiddenFiles = !_uiState.value.showHiddenFiles
        )
        refreshCurrentDirectory()
    }
    
    fun navigateUp() {
        val currentPath = _uiState.value.currentPath
        val parentFile = File(currentPath).parentFile
        if (parentFile != null && parentFile.exists()) {
            navigateToPath(parentFile.absolutePath)
        }
    }
    
    fun getStorageLocations(): List<StorageLocation> {
        return listOf(
            StorageLocation(
                name = "Internal Storage",
                path = Environment.getExternalStorageDirectory()?.absolutePath ?: "",
                icon = "internal_storage",
                isAvailable = true
            ),
            StorageLocation(
                name = "Downloads",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath ?: "",
                icon = "download",
                isAvailable = true
            ),
            StorageLocation(
                name = "Movies",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)?.absolutePath ?: "",
                icon = "movie",
                isAvailable = true
            ),
            StorageLocation(
                name = "DCIM",
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.absolutePath ?: "",
                icon = "camera",
                isAvailable = true
            )
        )
    }
    
    private suspend fun loadFilesFromPath(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) {
            throw IllegalArgumentException("Invalid directory: $path")
        }
        
        val files = directory.listFiles()?.toList() ?: emptyList()
        val showHidden = _uiState.value.showHiddenFiles
        
        val fileItems = files
            .filter { file ->
                // Filter hidden files if not showing them
                showHidden || !file.name.startsWith(".")
            }
            .map { file ->
                val extension = if (file.isFile) {
                    file.extension.lowercase()
                } else ""
                
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                    isDirectory = file.isDirectory,
                    isVideoFile = extension in videoExtensions,
                    extension = extension
                )
            }
        
        sortFiles(fileItems, _uiState.value.sortOrder)
    }
    
    private fun sortFiles(files: List<FileItem>, sortOrder: SortOrder): List<FileItem> {
        return when (sortOrder) {
            SortOrder.NAME_ASC -> files.sortedWith(
                compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
            )
            SortOrder.NAME_DESC -> files.sortedWith(
                compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase() }
            )
            SortOrder.DATE_ASC -> files.sortedWith(
                compareBy<FileItem> { !it.isDirectory }.thenBy { it.lastModified }
            )
            SortOrder.DATE_DESC -> files.sortedWith(
                compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified }
            )
            SortOrder.SIZE_ASC -> files.sortedWith(
                compareBy<FileItem> { !it.isDirectory }.thenBy { it.size }
            )
            SortOrder.SIZE_DESC -> files.sortedWith(
                compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size }
            )
        }
    }
    
    fun getVideoFiles(): List<FileItem> {
        return _uiState.value.files.filter { it.isVideoFile }
    }
    
    fun getDirectories(): List<FileItem> {
        return _uiState.value.files.filter { it.isDirectory }
    }
    
    fun getFileCount(): FileCount {
        val files = _uiState.value.files
        return FileCount(
            total = files.size,
            directories = files.count { it.isDirectory },
            videos = files.count { it.isVideoFile },
            others = files.count { !it.isDirectory && !it.isVideoFile }
        )
    }
    
    fun getTotalSize(): Long {
        return _uiState.value.files
            .filter { !it.isDirectory }
            .sumOf { it.size }
    }
}

data class StorageLocation(
    val name: String,
    val path: String,
    val icon: String,
    val isAvailable: Boolean
)

data class FileCount(
    val total: Int,
    val directories: Int,
    val videos: Int,
    val others: Int
)

// Extension functions for file operations
fun FileItem.isVideo(): Boolean = isVideoFile

fun FileItem.getFormattedSize(): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun FileItem.getFormattedDate(): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(lastModified))
}

fun FileItem.canPlay(): Boolean = isVideoFile

fun FileItem.canDelete(): Boolean = true // Can be customized based on permissions

fun FileItem.canShare(): Boolean = !isDirectory