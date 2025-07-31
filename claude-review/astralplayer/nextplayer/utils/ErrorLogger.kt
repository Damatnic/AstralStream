package com.astralplayer.nextplayer.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

data class ErrorEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: ErrorLevel,
    val tag: String,
    val message: String,
    val exception: Throwable? = null,
    val context: Map<String, Any> = emptyMap()
)

enum class ErrorLevel(val priority: Int) {
    VERBOSE(Log.VERBOSE),
    DEBUG(Log.DEBUG),
    INFO(Log.INFO),
    WARN(Log.WARN),
    ERROR(Log.ERROR),
    ASSERT(Log.ASSERT)
}

class ErrorLogger private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: ErrorLogger? = null
        
        fun getInstance(context: Context): ErrorLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ErrorLogger(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val LOG_FILE_NAME = "astral_vu_logs.txt"
        private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5MB
        private const val MAX_LOG_ENTRIES = 1000
    }
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val logFile = File(context.filesDir, LOG_FILE_NAME)
    private val logEntries = mutableListOf<ErrorEntry>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    init {
        // Load existing logs if they exist
        loadExistingLogs()
    }
    
    fun v(tag: String, message: String, context: Map<String, Any> = emptyMap()) {
        log(ErrorLevel.VERBOSE, tag, message, null, context)
    }
    
    fun d(tag: String, message: String, context: Map<String, Any> = emptyMap()) {
        log(ErrorLevel.DEBUG, tag, message, null, context)
    }
    
    fun i(tag: String, message: String, context: Map<String, Any> = emptyMap()) {
        log(ErrorLevel.INFO, tag, message, null, context)
    }
    
    fun w(tag: String, message: String, context: Map<String, Any> = emptyMap()) {
        log(ErrorLevel.WARN, tag, message, null, context)
    }
    
    fun w(tag: String, message: String, exception: Throwable?, context: Map<String, Any> = emptyMap()) {
        log(ErrorLevel.WARN, tag, message, exception, context)
    }
    
    fun e(tag: String, message: String, context: Map<String, Any> = emptyMap()) {
        log(ErrorLevel.ERROR, tag, message, null, context)
    }
    
    fun e(tag: String, message: String, exception: Throwable?, context: Map<String, Any> = emptyMap()) {
        log(ErrorLevel.ERROR, tag, message, exception, context)
    }
    
    fun wtf(tag: String, message: String, context: Map<String, Any> = emptyMap()) {
        log(ErrorLevel.ASSERT, tag, message, null, context)
    }
    
    fun wtf(tag: String, message: String, exception: Throwable?, context: Map<String, Any> = emptyMap()) {
        log(ErrorLevel.ASSERT, tag, message, exception, context)
    }
    
    private fun log(level: ErrorLevel, tag: String, message: String, exception: Throwable?, context: Map<String, Any>) {
        val entry = ErrorEntry(
            level = level,
            tag = tag,
            message = message,
            exception = exception,
            context = context
        )
        
        // Log to Android system log
        when (level) {
            ErrorLevel.VERBOSE -> Log.v(tag, message, exception)
            ErrorLevel.DEBUG -> Log.d(tag, message, exception)
            ErrorLevel.INFO -> Log.i(tag, message, exception)
            ErrorLevel.WARN -> Log.w(tag, message, exception)
            ErrorLevel.ERROR -> Log.e(tag, message, exception)
            ErrorLevel.ASSERT -> Log.wtf(tag, message, exception)
        }
        
        // Add to internal log
        synchronized(logEntries) {
            logEntries.add(entry)
            
            // Maintain size limit
            if (logEntries.size > MAX_LOG_ENTRIES) {
                logEntries.removeFirst()
            }
        }
        
        // Write to file asynchronously
        coroutineScope.launch {
            writeToFile(entry)
        }
    }
    
    private fun writeToFile(entry: ErrorEntry) {
        try {
            // Check file size and rotate if necessary
            if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
                rotateLogFile()
            }
            
            val logLine = formatLogEntry(entry)
            
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(logLine)
                writer.flush()
            }
        } catch (e: IOException) {
            Log.e("ErrorLogger", "Failed to write log to file", e)
        }
    }
    
    private fun formatLogEntry(entry: ErrorEntry): String {
        val timestamp = dateFormatter.format(Date(entry.timestamp))
        val levelChar = when (entry.level) {
            ErrorLevel.VERBOSE -> "V"
            ErrorLevel.DEBUG -> "D"
            ErrorLevel.INFO -> "I"
            ErrorLevel.WARN -> "W"
            ErrorLevel.ERROR -> "E"
            ErrorLevel.ASSERT -> "A"
        }
        
        val contextStr = if (entry.context.isNotEmpty()) {
            " [${entry.context.entries.joinToString(", ") { "${it.key}=${it.value}" }}]"
        } else ""
        
        val exceptionStr = entry.exception?.let { exception ->
            "\n${exception.javaClass.simpleName}: ${exception.message}\n${
                exception.stackTrace.take(10).joinToString("\n") { "  at $it" }
            }"
        } ?: ""
        
        return "$timestamp $levelChar/${entry.tag}: ${entry.message}$contextStr$exceptionStr"
    }
    
    private fun rotateLogFile() {
        try {
            val backupFile = File(context.filesDir, "${LOG_FILE_NAME}.old")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            logFile.renameTo(backupFile)
        } catch (e: Exception) {
            Log.e("ErrorLogger", "Failed to rotate log file", e)
        }
    }
    
    private fun loadExistingLogs() {
        try {
            if (logFile.exists()) {
                val lines = logFile.readLines()
                // Parse and load recent entries (implement if needed for log analysis)
            }
        } catch (e: Exception) {
            Log.e("ErrorLogger", "Failed to load existing logs", e)
        }
    }
    
    fun getRecentLogs(count: Int = 100): List<ErrorEntry> {
        return synchronized(logEntries) {
            logEntries.takeLast(count).toList()
        }
    }
    
    fun getLogsByLevel(level: ErrorLevel, count: Int = 100): List<ErrorEntry> {
        return synchronized(logEntries) {
            logEntries.filter { it.level == level }.takeLast(count)
        }
    }
    
    fun getLogsByTag(tag: String, count: Int = 100): List<ErrorEntry> {
        return synchronized(logEntries) {
            logEntries.filter { it.tag.contains(tag, ignoreCase = true) }.takeLast(count)
        }
    }
    
    fun clearLogs() {
        synchronized(logEntries) {
            logEntries.clear()
        }
        try {
            if (logFile.exists()) {
                logFile.delete()
            }
        } catch (e: Exception) {
            Log.e("ErrorLogger", "Failed to clear log file", e)
        }
    }
    
    fun getLogFile(): File = logFile
    
    fun exportLogs(): String {
        return synchronized(logEntries) {
            logEntries.joinToString("\n") { formatLogEntry(it) }
        }
    }
    
    // Convenience methods for common error scenarios
    fun logNetworkError(tag: String, url: String, exception: Throwable) {
        e(tag, "Network error occurred", exception, mapOf("url" to url))
    }
    
    fun logPlayerError(tag: String, videoUri: String, exception: Throwable) {
        e(tag, "Player error occurred", exception, mapOf("videoUri" to videoUri))
    }
    
    fun logDatabaseError(tag: String, operation: String, exception: Throwable) {
        e(tag, "Database error occurred", exception, mapOf("operation" to operation))
    }
    
    fun logPerformanceWarning(tag: String, operation: String, durationMs: Long) {
        if (durationMs > 1000) { // Log operations taking more than 1 second
            w(tag, "Slow operation detected", mapOf(
                "operation" to operation,
                "duration_ms" to durationMs
            ))
        }
    }
}