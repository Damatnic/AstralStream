package com.astralplayer.nextplayer.data.cloud

sealed class CloudResult<out T> {
    data class Success<T>(val data: T) : CloudResult<T>()
    data class Error(val exception: CloudException) : CloudResult<Nothing>()
    data class Loading(val progress: Float = 0f, val message: String? = null) : CloudResult<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Cannot get data from loading state")
    }
    
    inline fun onSuccess(action: (T) -> Unit): CloudResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (CloudException) -> Unit): CloudResult<T> {
        if (this is Error) action(exception)
        return this
    }
    
    inline fun onLoading(action: (Float, String?) -> Unit): CloudResult<T> {
        if (this is Loading) action(progress, message)
        return this
    }
    
    inline fun <R> map(transform: (T) -> R): CloudResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }
    
    inline fun <R> flatMap(transform: (T) -> CloudResult<R>): CloudResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Loading -> this
    }
}

sealed class CloudException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    class AuthenticationException(
        message: String = "Authentication failed",
        cause: Throwable? = null
    ) : CloudException(message, cause)
    
    class AuthorizationException(
        message: String = "Access denied",
        cause: Throwable? = null
    ) : CloudException(message, cause)
    
    class NetworkException(
        message: String = "Network error",
        cause: Throwable? = null
    ) : CloudException(message, cause)
    
    class FileNotFoundException(
        val filePath: String,
        message: String = "File not found: $filePath",
        cause: Throwable? = null
    ) : CloudException(message, cause)
    
    class FileAccessException(
        val filePath: String,
        message: String = "Cannot access file: $filePath",
        cause: Throwable? = null
    ) : CloudException(message, cause)
    
    class QuotaExceededException(
        val quotaType: QuotaType,
        message: String = "Quota exceeded: $quotaType",
        cause: Throwable? = null
    ) : CloudException(message, cause)
    
    class UnsupportedOperationException(
        val operation: String,
        val provider: CloudProvider,
        message: String = "Operation '$operation' not supported by ${provider.displayName}",
        cause: Throwable? = null
    ) : CloudException(message, cause)
    
    class ServerException(
        val statusCode: Int,
        message: String = "Server error: $statusCode",
        cause: Throwable? = null
    ) : CloudException(message, cause)
    
    class ParseException(
        message: String = "Failed to parse response",
        cause: Throwable? = null
    ) : CloudException(message, cause)
    
    class TimeoutException(
        message: String = "Operation timed out",
        cause: Throwable? = null
    ) : CloudException(message, cause)
    
    class ConfigurationException(
        message: String = "Invalid configuration",
        cause: Throwable? = null
    ) : CloudException(message, cause)
    
    class UnknownException(
        message: String = "Unknown error occurred",
        cause: Throwable? = null
    ) : CloudException(message, cause)
}

data class CloudOperation(
    val id: String,
    val type: CloudOperationType,
    val provider: CloudProvider,
    val filePath: String,
    val status: CloudOperationStatus,
    val progress: Float = 0f,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val bytesTotal: Long = 0L,
    val bytesTransferred: Long = 0L,
    val errorMessage: String? = null
) {
    val isCompleted: Boolean get() = status in setOf(
        CloudOperationStatus.COMPLETED,
        CloudOperationStatus.FAILED,
        CloudOperationStatus.CANCELLED
    )
    
    val isActive: Boolean get() = status in setOf(
        CloudOperationStatus.QUEUED,
        CloudOperationStatus.RUNNING,
        CloudOperationStatus.PAUSED
    )
    
    val durationMs: Long get() = (endTime ?: System.currentTimeMillis()) - startTime
    
    val transferRate: Float get() = if (durationMs > 0) {
        (bytesTransferred.toFloat() / durationMs) * 1000f // bytes per second
    } else 0f
    
    val estimatedTimeRemaining: Long? get() = if (transferRate > 0 && bytesTotal > 0) {
        val remainingBytes = bytesTotal - bytesTransferred
        (remainingBytes / transferRate * 1000).toLong() // milliseconds
    } else null
}

enum class CloudOperationType {
    UPLOAD,
    DOWNLOAD,
    DELETE,
    MOVE,
    COPY,
    SHARE,
    SYNC,
    LIST,
    STREAM
}

enum class CloudOperationStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class CloudAuthState(
    val provider: CloudProvider,
    val isAuthenticated: Boolean,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long? = null,
    val scope: Set<String> = emptySet(),
    val userInfo: CloudUserInfo? = null
) {
    val isExpired: Boolean get() = expiresAt?.let { it < System.currentTimeMillis() } ?: false
    val needsRefresh: Boolean get() = isExpired && refreshToken != null
}

data class CloudUserInfo(
    val id: String,
    val email: String?,
    val name: String?,
    val avatarUrl: String?,
    val accountType: String? = null,
    val storageQuota: CloudStorageInfo? = null
)