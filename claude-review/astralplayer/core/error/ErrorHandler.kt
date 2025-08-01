package com.astralplayer.core.error

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling system for AstralStream
 * Provides consistent error processing, categorization, and user-friendly messaging
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val context: Context
) {
    
    private val _errorState = MutableStateFlow<ErrorState?>(null)
    val errorState: StateFlow<ErrorState?> = _errorState.asStateFlow()
    
    /**
     * Handle any throwable and convert to user-friendly error
     */
    fun handleError(throwable: Throwable, context: ErrorContext = ErrorContext.GENERAL) {
        Timber.e(throwable, "Error in context: $context")
        
        val errorState = when (throwable) {
            is UnknownHostException -> ErrorState.NetworkError(
                message = "No internet connection",
                retry = true,
                context = context
            )
            
            is SocketTimeoutException -> ErrorState.NetworkError(
                message = "Connection timeout. Please try again.",
                retry = true,
                context = context
            )
            
            is SecurityException -> ErrorState.PermissionError(
                message = "Permission denied. Please grant the required permissions.",
                retry = false,
                context = context
            )
            
            is OutOfMemoryError -> ErrorState.MemoryError(
                message = "Not enough memory. Please close other apps and try again.",
                retry = true,
                context = context
            )
            
            is IllegalArgumentException -> ErrorState.ValidationError(
                message = throwable.message ?: "Invalid input",
                retry = false,
                context = context
            )
            
            else -> ErrorState.UnknownError(
                message = "Something went wrong. Please try again.",
                retry = true,
                context = context,
                originalError = throwable
            )
        }
        
        _errorState.value = errorState
    }
    
    /**
     * Clear current error state
     */
    fun clearError() {
        _errorState.value = null
    }
    
    /**
     * Create a coroutine exception handler
     */
    fun createExceptionHandler(context: ErrorContext = ErrorContext.GENERAL): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            handleError(throwable, context)
        }
    }
    
    /**
     * Safely execute a block with error handling
     */
    suspend fun <T> runSafely(
        context: ErrorContext = ErrorContext.GENERAL,
        onError: ((Throwable) -> T?)? = null,
        block: suspend () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            handleError(e, context)
            onError?.invoke(e)
        }
    }
}

/**
 * Error states for different types of errors
 */
sealed class ErrorState {
    abstract val message: String
    abstract val retry: Boolean
    abstract val context: ErrorContext
    
    data class NetworkError(
        override val message: String,
        override val retry: Boolean,
        override val context: ErrorContext
    ) : ErrorState()
    
    data class PermissionError(
        override val message: String,
        override val retry: Boolean,
        override val context: ErrorContext
    ) : ErrorState()
    
    data class MemoryError(
        override val message: String,
        override val retry: Boolean,
        override val context: ErrorContext
    ) : ErrorState()
    
    data class ValidationError(
        override val message: String,
        override val retry: Boolean,
        override val context: ErrorContext
    ) : ErrorState()
    
    data class PlaybackError(
        override val message: String,
        override val retry: Boolean,
        override val context: ErrorContext,
        val videoUri: String? = null
    ) : ErrorState()
    
    data class StorageError(
        override val message: String,
        override val retry: Boolean,
        override val context: ErrorContext
    ) : ErrorState()
    
    data class UnknownError(
        override val message: String,
        override val retry: Boolean,
        override val context: ErrorContext,
        val originalError: Throwable
    ) : ErrorState()
}

/**
 * Context where error occurred
 */
enum class ErrorContext {
    GENERAL,
    VIDEO_PLAYBACK,
    NETWORK_REQUEST,
    FILE_OPERATION,
    CLOUD_STORAGE,
    SUBTITLE_GENERATION,
    DOWNLOAD,
    DATABASE,
    AUTHENTICATION,
    INITIALIZATION
}