package com.astralvu.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class ErrorHandlingManager(private val context: Context) {
    
    private val errorQueue = ConcurrentLinkedQueue<ErrorEvent>()
    private val errorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val errorLogFile: File
    private val maxLogSize = 5 * 1024 * 1024 // 5MB
    private val maxErrorsInMemory = 100
    
    init {
        errorLogFile = File(context.filesDir, "error_logs.txt")
        startErrorProcessor()
    }
    
    data class ErrorEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val error: Throwable? = null,
        val message: String? = null,
        val severity: ErrorSeverity = ErrorSeverity.ERROR,
        val context: Map<String, Any?> = emptyMap()
    )
    
    enum class ErrorSeverity {
        DEBUG, INFO, WARNING, ERROR, CRITICAL
    }
    
    fun logError(throwable: Throwable, severity: ErrorSeverity = ErrorSeverity.ERROR) {
        val errorEvent = ErrorEvent(
            error = throwable,
            message = throwable.message,
            severity = severity
        )
        
        errorQueue.offer(errorEvent)
        
        // Log to Android Logcat
        when (severity) {
            ErrorSeverity.DEBUG -> Log.d(TAG, "Error logged", throwable)
            ErrorSeverity.INFO -> Log.i(TAG, "Error logged", throwable)
            ErrorSeverity.WARNING -> Log.w(TAG, "Error logged", throwable)
            ErrorSeverity.ERROR -> Log.e(TAG, "Error logged", throwable)
            ErrorSeverity.CRITICAL -> Log.wtf(TAG, "Critical error logged", throwable)
        }
        
        // Handle critical errors specially
        if (severity == ErrorSeverity.CRITICAL) {
            handleCriticalError(throwable)
        }
    }
    
    fun logError(event: Any) {
        when (event) {
            is String -> logMessage(event)
            is Map<*, *> -> logStructuredError(event)
            else -> logMessage(event.toString())
        }
    }
    
    fun logMessage(message: String, severity: ErrorSeverity = ErrorSeverity.INFO) {
        val errorEvent = ErrorEvent(
            message = message,
            severity = severity
        )
        errorQueue.offer(errorEvent)
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun logStructuredError(event: Map<*, *>) {
        val errorEvent = ErrorEvent(
            message = event["message"] as? String ?: "Structured error",
            severity = parseSeverity(event["severity"] as? String),
            context = event as Map<String, Any?>
        )
        errorQueue.offer(errorEvent)
    }
    
    private fun parseSeverity(severity: String?): ErrorSeverity {
        return try {
            severity?.let { ErrorSeverity.valueOf(it.uppercase()) } ?: ErrorSeverity.ERROR
        } catch (e: IllegalArgumentException) {
            ErrorSeverity.ERROR
        }
    }
    
    private fun startErrorProcessor() {
        errorScope.launch {
            while (isActive) {
                processErrorQueue()
                delay(1000) // Process every second
            }
        }
    }
    
    private suspend fun processErrorQueue() {
        val errors = mutableListOf<ErrorEvent>()
        
        // Drain queue up to maxErrorsInMemory
        while (errors.size < maxErrorsInMemory) {
            val error = errorQueue.poll() ?: break
            errors.add(error)
        }
        
        if (errors.isNotEmpty()) {
            writeErrorsToFile(errors)
            
            // Keep only recent errors in memory
            if (errorQueue.size > maxErrorsInMemory * 2) {
                val toRemove = errorQueue.size - maxErrorsInMemory
                repeat(toRemove) {
                    errorQueue.poll()
                }
            }
        }
    }
    
    private suspend fun writeErrorsToFile(errors: List<ErrorEvent>) = withContext(Dispatchers.IO) {
        try {
            // Check file size and rotate if necessary
            if (errorLogFile.exists() && errorLogFile.length() > maxLogSize) {
                rotateLogFile()
            }
            
            errorLogFile.appendText(buildString {
                errors.forEach { error ->
                    appendLine("=====================================")
                    appendLine("Timestamp: ${formatTimestamp(error.timestamp)}")
                    appendLine("Severity: ${error.severity}")
                    
                    error.message?.let {
                        appendLine("Message: $it")
                    }
                    
                    error.error?.let { throwable ->
                        appendLine("Exception: ${throwable.javaClass.simpleName}")
                        appendLine("Stack trace:")
                        appendLine(getStackTraceString(throwable))
                    }
                    
                    if (error.context.isNotEmpty()) {
                        appendLine("Context:")
                        error.context.forEach { (key, value) ->
                            appendLine("  $key: $value")
                        }
                    }
                    appendLine()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write errors to file", e)
        }
    }
    
    private fun rotateLogFile() {
        try {
            val backupFile = File(context.filesDir, "error_logs_${System.currentTimeMillis()}.txt")
            errorLogFile.renameTo(backupFile)
            
            // Keep only the last 3 backup files
            val logFiles = context.filesDir.listFiles { file ->
                file.name.startsWith("error_logs_") && file.name.endsWith(".txt")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
            
            if (logFiles.size > 3) {
                logFiles.drop(3).forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log file", e)
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    }
    
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
    
    private fun handleCriticalError(throwable: Throwable) {
        // Save crash info for next app launch
        val crashFile = File(context.filesDir, "last_crash.txt")
        try {
            crashFile.writeText(buildString {
                appendLine("Critical Error Report")
                appendLine("Time: ${formatTimestamp(System.currentTimeMillis())}")
                appendLine("Error: ${throwable.message}")
                appendLine("Stack trace:")
                appendLine(getStackTraceString(throwable))
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash info", e)
        }
    }
    
    fun getRecentErrors(count: Int = 50): List<ErrorEvent> {
        return errorQueue.toList().takeLast(count)
    }
    
    fun clearErrors() {
        errorQueue.clear()
        try {
            errorLogFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear error log file", e)
        }
    }
    
    fun exportErrorLog(): File? {
        return try {
            val exportFile = File(context.getExternalFilesDir(null), "astral_error_log_${System.currentTimeMillis()}.txt")
            errorLogFile.copyTo(exportFile, overwrite = true)
            exportFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export error log", e)
            null
        }
    }
    
    fun cleanup() {
        errorScope.cancel()
    }
    
    companion object {
        private const val TAG = "ErrorHandlingManager"
    }
}