package com.astralplayer.nextplayer.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay

// Error Types
sealed class PlayerError : Exception() {
    data class VideoLoadError(val uri: android.net.Uri, override val cause: Throwable?) : PlayerError() {
        override val message: String = "Failed to load video: ${uri}"
    }
    
    data class NetworkError(override val message: String, override val cause: Throwable?) : PlayerError()
    
    data class CloudAuthenticationError(val provider: CloudProvider, override val cause: Throwable?) : PlayerError() {
        override val message: String = "Authentication failed for ${provider.name}"
    }
    
    data class AISubtitleError(override val message: String, override val cause: Throwable?) : PlayerError()
    
    data class CastError(override val message: String, override val cause: Throwable?) : PlayerError()
    
    data class BrowserIntegrationError(val url: String, override val cause: Throwable?) : PlayerError() {
        override val message: String = "Failed to process URL: $url"
    }
    
    data class SecurityError(override val message: String) : PlayerError()
    
    data class DatabaseError(override val cause: Throwable?) : PlayerError() {
        override val message: String = "Database operation failed"
    }
    
    data class StorageError(override val message: String, override val cause: Throwable?) : PlayerError()
    
    data class PermissionError(val permission: String) : PlayerError() {
        override val message: String = "Permission required: $permission"
    }
    
    data class UnknownError(override val message: String, override val cause: Throwable?) : PlayerError()
}

// Recovery Strategies
sealed class ErrorRecoveryResult {
    data class Retry(
        val message: String,
        val retryAction: suspend () -> Result<Unit>,
        val maxRetries: Int = 3
    ) : ErrorRecoveryResult()
    
    data class Fallback(
        val message: String,
        val fallbackAction: suspend () -> Result<Unit>
    ) : ErrorRecoveryResult()
    
    data class UserAction(
        val message: String,
        val actionText: String,
        val action: () -> Unit
    ) : ErrorRecoveryResult()
    
    data class Fatal(
        val message: String,
        val shouldRestart: Boolean = false
    ) : ErrorRecoveryResult()
    
    data class Ignore(
        val message: String
    ) : ErrorRecoveryResult()
}

// Error State
data class ErrorState(
    val currentError: PlayerError? = null,
    val errorHistory: List<PlayerError> = emptyList(),
    val recoveryInProgress: Boolean = false,
    val recoveryAttempts: Int = 0,
    val lastRecoveryTime: Long = 0L,
    val networkAvailable: Boolean = true,
    val criticalErrorCount: Int = 0
)

// Network Manager for connectivity checks
class NetworkConnectivityManager constructor(
    private val context: Context
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    fun isMobileConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
    
    fun getConnectionType(): String {
        return when {
            isWifiConnected() -> "WiFi"
            isMobileConnected() -> "Mobile"
            isConnected() -> "Other"
            else -> "None"
        }
    }
}

// Main Error Recovery Manager
interface ErrorRecoveryManager {
    val errorState: StateFlow<ErrorState>
    
    suspend fun handleError(error: PlayerError): ErrorRecoveryResult
    suspend fun retryLastOperation(): Result<Unit>
    fun clearError()
    fun getErrorHistory(): List<PlayerError>
    fun getRecoveryRecommendations(): List<String>
}

class ErrorRecoveryManagerImpl constructor(
    private val context: Context,
    private val networkManager: NetworkConnectivityManager
) : ErrorRecoveryManager {
    
    private val _errorState = MutableStateFlow(ErrorState())
    override val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()
    
    private var lastRetryAction: (suspend () -> Result<Unit>)? = null
    
    override suspend fun handleError(error: PlayerError): ErrorRecoveryResult {
        updateErrorState { 
            copy(
                currentError = error,
                errorHistory = (errorHistory + error).takeLast(50), // Keep last 50 errors
                networkAvailable = networkManager.isConnected()
            )
        }
        
        return when (error) {
            is PlayerError.VideoLoadError -> handleVideoLoadError(error)
            is PlayerError.NetworkError -> handleNetworkError(error)
            is PlayerError.CloudAuthenticationError -> handleCloudAuthError(error)
            is PlayerError.AISubtitleError -> handleAISubtitleError(error)
            is PlayerError.CastError -> handleCastError(error)
            is PlayerError.BrowserIntegrationError -> handleBrowserError(error)
            is PlayerError.SecurityError -> handleSecurityError(error)
            is PlayerError.DatabaseError -> handleDatabaseError(error)
            is PlayerError.StorageError -> handleStorageError(error)
            is PlayerError.PermissionError -> handlePermissionError(error)
            is PlayerError.UnknownError -> handleUnknownError(error)
        }
    }
    
    override suspend fun retryLastOperation(): Result<Unit> {
        return try {
            val retryAction = lastRetryAction ?: return Result.failure(Exception("No retry action available"))
            
            updateErrorState { copy(recoveryInProgress = true, recoveryAttempts = recoveryAttempts + 1) }
            
            val result = retryAction()
            
            updateErrorState { 
                copy(
                    recoveryInProgress = false,
                    lastRecoveryTime = System.currentTimeMillis(),
                    currentError = if (result.isSuccess) null else currentError
                )
            }
            
            result
        } catch (e: Exception) {
            updateErrorState { copy(recoveryInProgress = false) }
            Result.failure(e)
        }
    }
    
    override fun clearError() {
        updateErrorState { copy(currentError = null, recoveryAttempts = 0) }
    }
    
    override fun getErrorHistory(): List<PlayerError> {
        return _errorState.value.errorHistory
    }
    
    override fun getRecoveryRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val state = _errorState.value
        
        if (!state.networkAvailable) {
            recommendations.add("Check your internet connection")
        }
        
        if (state.criticalErrorCount > 3) {
            recommendations.add("Consider restarting the app")
        }
        
        if (state.errorHistory.count { it is PlayerError.VideoLoadError } > 2) {
            recommendations.add("Try playing a different video format")
        }
        
        if (state.errorHistory.count { it is PlayerError.CloudAuthenticationError } > 1) {
            recommendations.add("Re-authenticate with cloud services")
        }
        
        return recommendations
    }
    
    private suspend fun handleVideoLoadError(error: PlayerError.VideoLoadError): ErrorRecoveryResult {
        return if (networkManager.isConnected()) {
            // Try alternative formats or sources
            ErrorRecoveryResult.Retry(
                message = "Failed to load video. Trying alternative format...",
                retryAction = {
                    // In a real implementation, this would try different video formats
                    delay(1000)
                    Result.success(Unit)
                }
            ).also { 
                lastRetryAction = { delay(1000); Result.success(Unit) }
            }
        } else {
            ErrorRecoveryResult.UserAction(
                message = "No internet connection. Please check your network settings.",
                actionText = "Open Settings",
                action = { /* Open network settings */ }
            )
        }
    }
    
    private suspend fun handleNetworkError(error: PlayerError.NetworkError): ErrorRecoveryResult {
        return if (networkManager.isConnected()) {
            ErrorRecoveryResult.Retry(
                message = "Network error occurred. Retrying...",
                retryAction = {
                    delay(2000)
                    if (networkManager.isConnected()) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Still no connection"))
                    }
                }
            ).also {
                lastRetryAction = { 
                    delay(2000)
                    if (networkManager.isConnected()) Result.success(Unit) 
                    else Result.failure(Exception("Still no connection"))
                }
            }
        } else {
            ErrorRecoveryResult.Fallback(
                message = "No network connection. Switching to offline mode.",
                fallbackAction = {
                    // Switch to offline mode
                    Result.success(Unit)
                }
            )
        }
    }
    
    private suspend fun handleCloudAuthError(error: PlayerError.CloudAuthenticationError): ErrorRecoveryResult {
        return ErrorRecoveryResult.UserAction(
            message = "Authentication failed for ${error.provider.name}. Please sign in again.",
            actionText = "Sign In",
            action = { /* Trigger re-authentication */ }
        )
    }
    
    private suspend fun handleAISubtitleError(error: PlayerError.AISubtitleError): ErrorRecoveryResult {
        return if (networkManager.isConnected()) {
            ErrorRecoveryResult.Fallback(
                message = "AI subtitle generation failed. Using offline mode.",
                fallbackAction = {
                    // Switch to offline subtitle generation
                    Result.success(Unit)
                }
            )
        } else {
            ErrorRecoveryResult.Ignore(
                message = "Subtitle generation unavailable offline. Continuing without subtitles."
            )
        }
    }
    
    private suspend fun handleCastError(error: PlayerError.CastError): ErrorRecoveryResult {
        return ErrorRecoveryResult.Retry(
            message = "Cast connection failed. Retrying...",
            retryAction = {
                delay(3000)
                // In a real implementation, this would retry cast connection
                Result.success(Unit)
            },
            maxRetries = 2
        ).also {
            lastRetryAction = { delay(3000); Result.success(Unit) }
        }
    }
    
    private suspend fun handleBrowserError(error: PlayerError.BrowserIntegrationError): ErrorRecoveryResult {
        return ErrorRecoveryResult.UserAction(
            message = "Unable to process this URL. The site may not be supported.",
            actionText = "Try Different URL",
            action = { /* Open URL input dialog */ }
        )
    }
    
    private suspend fun handleSecurityError(error: PlayerError.SecurityError): ErrorRecoveryResult {
        return ErrorRecoveryResult.Fatal(
            message = "Security error: ${error.message}. Please restart the app.",
            shouldRestart = true
        )
    }
    
    private suspend fun handleDatabaseError(error: PlayerError.DatabaseError): ErrorRecoveryResult {
        return ErrorRecoveryResult.Retry(
            message = "Database error occurred. Retrying...",
            retryAction = {
                delay(1000)
                // In a real implementation, this would retry the database operation
                Result.success(Unit)
            }
        ).also {
            lastRetryAction = { delay(1000); Result.success(Unit) }
        }
    }
    
    private suspend fun handleStorageError(error: PlayerError.StorageError): ErrorRecoveryResult {
        return ErrorRecoveryResult.UserAction(
            message = "Storage error: ${error.message}. Please free up space or check permissions.",
            actionText = "Open Settings",
            action = { /* Open storage settings */ }
        )
    }
    
    private suspend fun handlePermissionError(error: PlayerError.PermissionError): ErrorRecoveryResult {
        return ErrorRecoveryResult.UserAction(
            message = "Permission required: ${error.permission}. Please grant the permission to continue.",
            actionText = "Grant Permission",
            action = { /* Request permission */ }
        )
    }
    
    private suspend fun handleUnknownError(error: PlayerError.UnknownError): ErrorRecoveryResult {
        updateErrorState { copy(criticalErrorCount = criticalErrorCount + 1) }
        
        return if (_errorState.value.criticalErrorCount > 5) {
            ErrorRecoveryResult.Fatal(
                message = "Multiple critical errors occurred. Please restart the app.",
                shouldRestart = true
            )
        } else {
            ErrorRecoveryResult.Retry(
                message = "An unexpected error occurred. Retrying...",
                retryAction = {
                    delay(2000)
                    Result.success(Unit)
                }
            ).also {
                lastRetryAction = { delay(2000); Result.success(Unit) }
            }
        }
    }
    
    private fun updateErrorState(update: ErrorState.() -> ErrorState) {
        _errorState.value = _errorState.value.update()
    }
}

// Error Logger for debugging and analytics
class ErrorLogger constructor(
    private val context: Context
) {
    
    fun logError(error: PlayerError, additionalInfo: Map<String, String> = emptyMap()) {
        val errorInfo = buildMap {
            put("error_type", error::class.simpleName ?: "Unknown")
            put("error_message", error.message ?: "No message")
            put("timestamp", System.currentTimeMillis().toString())
            put("device_model", android.os.Build.MODEL)
            put("android_version", android.os.Build.VERSION.RELEASE)
            putAll(additionalInfo)
        }
        
        // In a real implementation, this would send to analytics service
        android.util.Log.e("AstralVu_Error", "Error logged: $errorInfo")
    }
    
    fun logRecoveryAttempt(error: PlayerError, recoveryResult: ErrorRecoveryResult) {
        val recoveryInfo = mapOf(
            "error_type" to (error::class.simpleName ?: "Unknown"),
            "recovery_type" to (recoveryResult::class.simpleName ?: "Unknown"),
            "timestamp" to System.currentTimeMillis().toString()
        )
        
        android.util.Log.i("AstralVu_Recovery", "Recovery attempted: $recoveryInfo")
    }
}

// Crash Handler for unhandled exceptions
class CrashHandler constructor(
    private val context: Context,
    private val errorLogger: ErrorLogger
) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            val crashError = PlayerError.UnknownError(
                message = "Unhandled exception: ${exception.message}",
                cause = exception
            )
            
            errorLogger.logError(
                crashError,
                mapOf(
                    "thread_name" to thread.name,
                    "stack_trace" to exception.stackTraceToString()
                )
            )
            
            // Save crash report to local storage for later upload
            saveCrashReport(exception)
            
        } catch (e: Exception) {
            // Don't let crash handler crash
        } finally {
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    private fun saveCrashReport(exception: Throwable) {
        try {
            val crashReport = buildString {
                appendLine("Crash Report - ${java.util.Date()}")
                appendLine("Device: ${android.os.Build.MODEL}")
                appendLine("Android: ${android.os.Build.VERSION.RELEASE}")
                appendLine("App Version: 1.0.0") // Would get from BuildConfig
                appendLine("Exception: ${exception.message}")
                appendLine("Stack Trace:")
                appendLine(exception.stackTraceToString())
            }
            
            val file = java.io.File(context.filesDir, "crash_reports")
            if (!file.exists()) {
                file.mkdirs()
            }
            
            val crashFile = java.io.File(file, "crash_${System.currentTimeMillis()}.txt")
            crashFile.writeText(crashReport)
            
        } catch (e: Exception) {
            // Don't let crash report saving crash
        }
    }
}