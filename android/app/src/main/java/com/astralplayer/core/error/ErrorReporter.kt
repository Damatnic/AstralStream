package com.astralplayer.core.error

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Error reporting system for logging and analytics
 */
@Singleton
class ErrorReporter @Inject constructor(
    private val context: Context
) {
    
    private val errorLogFile = File(context.filesDir, "error_log.txt")
    private val maxLogSize = 1024 * 1024 // 1MB
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    private val reportScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Report non-fatal error
     */
    fun reportError(
        throwable: Throwable,
        context: ErrorContext,
        additionalInfo: Map<String, String> = emptyMap()
    ) {
        Timber.e(throwable, "Error reported in context: $context")
        
        reportScope.launch {
            try {
                val errorReport = buildErrorReport(throwable, context, additionalInfo)
                
                // Log to file
                logToFile(errorReport)
                
                // In production, would send to analytics service
                if (!BuildConfig.DEBUG) {
                    sendToAnalytics(errorReport)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to report error")
            }
        }
    }
    
    /**
     * Report custom event
     */
    fun reportEvent(
        event: String,
        parameters: Map<String, String> = emptyMap()
    ) {
        reportScope.launch {
            val eventReport = buildEventReport(event, parameters)
            logToFile(eventReport)
        }
    }
    
    /**
     * Build error report with device and app info
     */
    private fun buildErrorReport(
        throwable: Throwable,
        context: ErrorContext,
        additionalInfo: Map<String, String>
    ): ErrorReport {
        return ErrorReport(
            timestamp = System.currentTimeMillis(),
            errorType = throwable.javaClass.simpleName,
            message = throwable.message ?: "No message",
            stackTrace = throwable.stackTraceToString(),
            context = context.name,
            deviceInfo = getDeviceInfo(),
            appInfo = getAppInfo(),
            additionalInfo = additionalInfo
        )
    }
    
    /**
     * Build event report
     */
    private fun buildEventReport(
        event: String,
        parameters: Map<String, String>
    ): EventReport {
        return EventReport(
            timestamp = System.currentTimeMillis(),
            event = event,
            parameters = parameters,
            deviceInfo = getDeviceInfo()
        )
    }
    
    /**
     * Get device information
     */
    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            osVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            screenDensity = context.resources.displayMetrics.density,
            screenSize = "${context.resources.displayMetrics.widthPixels}x${context.resources.displayMetrics.heightPixels}"
        )
    }
    
    /**
     * Get app information
     */
    private fun getAppInfo(): AppInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return AppInfo(
            packageName = context.packageName,
            versionName = packageInfo.versionName,
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            },
            buildType = BuildConfig.BUILD_TYPE
        )
    }
    
    /**
     * Log error to file
     */
    private fun logToFile(report: Any) {
        try {
            // Rotate log if too large
            if (errorLogFile.length() > maxLogSize) {
                rotateLogFile()
            }
            
            errorLogFile.appendText(
                "${dateFormat.format(Date())}: ${report.toJson()}\n"
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to write to error log")
        }
    }
    
    /**
     * Rotate log file
     */
    private fun rotateLogFile() {
        val backupFile = File(context.filesDir, "error_log.old.txt")
        if (backupFile.exists()) {
            backupFile.delete()
        }
        errorLogFile.renameTo(backupFile)
    }
    
    /**
     * Send to analytics service (placeholder)
     */
    private fun sendToAnalytics(report: ErrorReport) {
        // In production, integrate with Crashlytics, Sentry, etc.
    }
    
    /**
     * Clear error logs
     */
    fun clearLogs() {
        reportScope.launch {
            try {
                errorLogFile.delete()
                File(context.filesDir, "error_log.old.txt").delete()
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear logs")
            }
        }
    }
    
    /**
     * Get error logs for debugging
     */
    fun getErrorLogs(): String {
        return try {
            errorLogFile.readText()
        } catch (e: Exception) {
            "No logs available"
        }
    }
}

/**
 * Error report data class
 */
data class ErrorReport(
    val timestamp: Long,
    val errorType: String,
    val message: String,
    val stackTrace: String,
    val context: String,
    val deviceInfo: DeviceInfo,
    val appInfo: AppInfo,
    val additionalInfo: Map<String, String>
) {
    fun toJson(): String = """
        {
            "timestamp": $timestamp,
            "errorType": "$errorType",
            "message": "$message",
            "context": "$context",
            "device": ${deviceInfo.toJson()},
            "app": ${appInfo.toJson()},
            "additional": ${additionalInfo.entries.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }}
        }
    """.trimIndent()
}

/**
 * Event report data class
 */
data class EventReport(
    val timestamp: Long,
    val event: String,
    val parameters: Map<String, String>,
    val deviceInfo: DeviceInfo
) {
    fun toJson(): String = """
        {
            "timestamp": $timestamp,
            "event": "$event",
            "parameters": ${parameters.entries.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }},
            "device": ${deviceInfo.toJson()}
        }
    """.trimIndent()
}

/**
 * Device information
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val sdkInt: Int,
    val screenDensity: Float,
    val screenSize: String
) {
    fun toJson(): String = """
        {
            "manufacturer": "$manufacturer",
            "model": "$model",
            "osVersion": "$osVersion",
            "sdkInt": $sdkInt,
            "screenDensity": $screenDensity,
            "screenSize": "$screenSize"
        }
    """.trimIndent()
}

/**
 * App information
 */
data class AppInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val buildType: String
) {
    fun toJson(): String = """
        {
            "packageName": "$packageName",
            "versionName": "$versionName",
            "versionCode": $versionCode,
            "buildType": "$buildType"
        }
    """.trimIndent()
}