package com.astralplayer.nextplayer.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Centralized error handling utility for the application
 */
object ErrorHandler {
    private const val TAG = "AstralVu"
    
    /**
     * Severity levels for errors
     */
    enum class Severity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
    
    /**
     * Error types for categorization
     */
    enum class ErrorType {
        NETWORK,
        FILE_ACCESS,
        DATABASE,
        PLAYBACK,
        PERMISSION,
        UNKNOWN
    }
    
    /**
     * Log and handle an error
     */
    fun handleError(
        context: Context,
        throwable: Throwable,
        userMessage: String? = null,
        severity: Severity = Severity.ERROR,
        errorType: ErrorType = ErrorType.UNKNOWN,
        showToast: Boolean = true
    ) {
        // Log the error
        logError(throwable, severity, errorType)
        
        // Show user-friendly message
        if (showToast) {
            val message = userMessage ?: getDefaultErrorMessage(throwable, errorType)
            showErrorToast(context, message)
        }
        
        // Report to analytics/crash reporting if integrated
        reportError(throwable, errorType)
    }
    
    /**
     * Handle error with coroutine scope for snackbar
     */
    fun handleError(
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
        throwable: Throwable,
        userMessage: String? = null,
        severity: Severity = Severity.ERROR,
        errorType: ErrorType = ErrorType.UNKNOWN
    ) {
        // Log the error
        logError(throwable, severity, errorType)
        
        // Show snackbar
        scope.launch {
            val message = userMessage ?: getDefaultErrorMessage(throwable, errorType)
            snackbarHostState.showSnackbar(message)
        }
        
        // Report to analytics/crash reporting if integrated
        reportError(throwable, errorType)
    }
    
    /**
     * Log error with details
     */
    private fun logError(throwable: Throwable, severity: Severity, errorType: ErrorType) {
        val stackTrace = getStackTraceString(throwable)
        val logMessage = """
            |Error occurred:
            |Type: $errorType
            |Severity: $severity
            |Message: ${throwable.message}
            |Stack trace:
            |$stackTrace
        """.trimMargin()
        
        when (severity) {
            Severity.INFO -> Log.i(TAG, logMessage)
            Severity.WARNING -> Log.w(TAG, logMessage)
            Severity.ERROR -> Log.e(TAG, logMessage)
            Severity.CRITICAL -> Log.wtf(TAG, logMessage)
        }
    }
    
    /**
     * Get stack trace as string
     */
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
    
    /**
     * Show error toast
     */
    private fun showErrorToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * Get user-friendly error message
     */
    private fun getDefaultErrorMessage(throwable: Throwable, errorType: ErrorType): String {
        return when (errorType) {
            ErrorType.NETWORK -> "Network connection error. Please check your internet connection."
            ErrorType.FILE_ACCESS -> "Unable to access the file. Please check permissions."
            ErrorType.DATABASE -> "Database error occurred. Please try again."
            ErrorType.PLAYBACK -> "Playback error: ${throwable.message ?: "Unknown error"}"
            ErrorType.PERMISSION -> "Permission denied. Please grant the required permissions."
            ErrorType.UNKNOWN -> "An unexpected error occurred: ${throwable.message ?: "Unknown error"}"
        }
    }
    
    /**
     * Report error to analytics/crash reporting service
     */
    private fun reportError(throwable: Throwable, errorType: ErrorType) {
        // Firebase Crashlytics integration placeholder
        try {
            // com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(throwable)
            // com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCustomKey("error_type", errorType.name)
            Log.d(TAG, "Error reported to crashlytics: $errorType - ${throwable.message}")
        } catch (e: Exception) {
            // Fallback if crashlytics is not available
            Log.e(TAG, "Failed to report to crashlytics", e)
        }
        
        // Additional custom crash reporting can be added here
        // Example: Sentry, Bugsnag, etc.
    }
    
    /**
     * Safe execution wrapper
     */
    inline fun <T> safeExecute(
        context: Context,
        errorType: ErrorType = ErrorType.UNKNOWN,
        showError: Boolean = true,
        crossinline block: () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            handleError(
                context = context,
                throwable = e,
                errorType = errorType,
                showToast = showError
            )
            null
        }
    }
    
    /**
     * Safe suspend execution wrapper
     */
    suspend inline fun <T> safeSuspendExecute(
        context: Context,
        errorType: ErrorType = ErrorType.UNKNOWN,
        showError: Boolean = true,
        crossinline block: suspend () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            handleError(
                context = context,
                throwable = e,
                errorType = errorType,
                showToast = showError
            )
            null
        }
    }
}

/**
 * Composable error handler state
 */
@Composable
fun rememberErrorHandler(): ErrorHandlerState {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    return remember {
        ErrorHandlerState(context, snackbarHostState)
    }
}

/**
 * Error handler state for Compose
 */
class ErrorHandlerState(
    private val context: Context,
    val snackbarHostState: SnackbarHostState
) {
    fun handleError(
        throwable: Throwable,
        userMessage: String? = null,
        severity: ErrorHandler.Severity = ErrorHandler.Severity.ERROR,
        errorType: ErrorHandler.ErrorType = ErrorHandler.ErrorType.UNKNOWN
    ) {
        ErrorHandler.handleError(
            context = context,
            throwable = throwable,
            userMessage = userMessage,
            severity = severity,
            errorType = errorType,
            showToast = true
        )
    }
}