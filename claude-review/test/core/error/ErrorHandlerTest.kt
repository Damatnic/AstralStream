package com.astralplayer.core.error

import android.content.Context
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.net.UnknownHostException
import java.net.SocketTimeoutException

@ExperimentalCoroutinesApi
class ErrorHandlerTest {
    
    @MockK
    private lateinit var context: Context
    
    private lateinit var errorHandler: ErrorHandler
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        errorHandler = ErrorHandler(context)
    }
    
    @Test
    fun `handleError should convert UnknownHostException to NetworkError`() {
        // Given
        val exception = UnknownHostException("No internet")
        
        // When
        errorHandler.handleError(exception, ErrorContext.NETWORK_REQUEST)
        
        // Then
        val errorState = errorHandler.errorState.value
        assertTrue(errorState is ErrorState.NetworkError)
        assertEquals("No internet connection", errorState?.message)
        assertTrue(errorState?.retry ?: false)
    }
    
    @Test
    fun `handleError should convert SocketTimeoutException to NetworkError with retry`() {
        // Given
        val exception = SocketTimeoutException("Timeout")
        
        // When
        errorHandler.handleError(exception, ErrorContext.VIDEO_PLAYBACK)
        
        // Then
        val errorState = errorHandler.errorState.value
        assertTrue(errorState is ErrorState.NetworkError)
        assertEquals("Connection timeout. Please try again.", errorState?.message)
        assertTrue(errorState?.retry ?: false)
    }
    
    @Test
    fun `handleError should convert SecurityException to PermissionError`() {
        // Given
        val exception = SecurityException("Permission denied")
        
        // When
        errorHandler.handleError(exception, ErrorContext.FILE_OPERATION)
        
        // Then
        val errorState = errorHandler.errorState.value
        assertTrue(errorState is ErrorState.PermissionError)
        assertEquals("Permission denied. Please grant the required permissions.", errorState?.message)
        assertFalse(errorState?.retry ?: true)
    }
    
    @Test
    fun `handleError should convert OutOfMemoryError to MemoryError`() {
        // Given
        val exception = OutOfMemoryError()
        
        // When
        errorHandler.handleError(exception, ErrorContext.VIDEO_PLAYBACK)
        
        // Then
        val errorState = errorHandler.errorState.value
        assertTrue(errorState is ErrorState.MemoryError)
        assertEquals("Not enough memory. Please close other apps and try again.", errorState?.message)
        assertTrue(errorState?.retry ?: false)
    }
    
    @Test
    fun `clearError should set error state to null`() {
        // Given
        errorHandler.handleError(Exception("Test"), ErrorContext.GENERAL)
        assertNotNull(errorHandler.errorState.value)
        
        // When
        errorHandler.clearError()
        
        // Then
        assertNull(errorHandler.errorState.value)
    }
    
    @Test
    fun `runSafely should execute block successfully`() = runTest {
        // Given
        val expectedResult = "Success"
        
        // When
        val result = errorHandler.runSafely {
            expectedResult
        }
        
        // Then
        assertEquals(expectedResult, result)
        assertNull(errorHandler.errorState.value)
    }
    
    @Test
    fun `runSafely should handle exceptions and return null`() = runTest {
        // Given
        val exception = RuntimeException("Test error")
        
        // When
        val result = errorHandler.runSafely {
            throw exception
        }
        
        // Then
        assertNull(result)
        assertNotNull(errorHandler.errorState.value)
        assertTrue(errorHandler.errorState.value is ErrorState.UnknownError)
    }
    
    @Test
    fun `runSafely should call onError callback when exception occurs`() = runTest {
        // Given
        val exception = RuntimeException("Test error")
        var callbackException: Throwable? = null
        
        // When
        errorHandler.runSafely(
            onError = { e ->
                callbackException = e
                "Error handled"
            }
        ) {
            throw exception
        }
        
        // Then
        assertEquals(exception, callbackException)
    }
}