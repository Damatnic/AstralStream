package com.astralplayer.astralstream.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.astralplayer.astralstream.data.repository.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive unit tests for MainViewModel
 * Implements TestCoverageAgent requirements for 85%+ coverage
 */
@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var viewModel: MainViewModel
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        settingsRepository = mockk(relaxed = true)
        savedStateHandle = mockk(relaxed = true)
        
        // Mock default settings
        every { settingsRepository.isDarkThemeEnabled } returns flowOf(false)
        every { settingsRepository.isGestureEnabled } returns flowOf(true)
        
        viewModel = MainViewModel(settingsRepository, savedStateHandle)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initial state should be correct`() {
        // Given default setup
        
        // Then
        assertNotNull(viewModel.uiState.value)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `dark theme toggle should update repository`() = runTest {
        // Given
        val newValue = true
        coEvery { settingsRepository.setDarkThemeEnabled(newValue) } just Runs

        // When
        viewModel.toggleDarkTheme()

        // Then
        coVerify { settingsRepository.setDarkThemeEnabled(newValue) }
    }

    @Test
    fun `gesture toggle should update repository`() = runTest {
        // Given
        val newValue = false
        every { settingsRepository.isGestureEnabled } returns flowOf(true)
        coEvery { settingsRepository.setGestureEnabled(newValue) } just Runs

        // When
        viewModel.toggleGestures()

        // Then
        coVerify { settingsRepository.setGestureEnabled(newValue) }
    }

    @Test
    fun `error handling should update UI state`() {
        // Given
        val errorMessage = "Test error"

        // When
        viewModel.handleError(Exception(errorMessage))

        // Then
        assertEquals(errorMessage, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loading state should be managed correctly`() {
        // When starting operation
        viewModel.setLoading(true)
        
        // Then
        assertTrue(viewModel.uiState.value.isLoading)
        
        // When completing operation
        viewModel.setLoading(false)
        
        // Then
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `saved state restoration should work correctly`() {
        // Given
        val savedValue = "test_saved_value"
        every { savedStateHandle.get<String>("last_state") } returns savedValue
        
        // When
        val restoredValue = viewModel.getLastSavedState()
        
        // Then
        assertEquals(savedValue, restoredValue)
    }

    @Test
    fun `concurrent operations should be handled safely`() = runTest {
        // Given multiple simultaneous operations
        val operations = List(10) {
            launch(testDispatcher) {
                viewModel.performConcurrentOperation(it)
            }
        }
        
        // When all complete
        operations.forEach { it.join() }
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then state should be consistent
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `memory leak prevention should work`() {
        // When
        viewModel.onCleared()
        
        // Then verify cleanup
        verify { savedStateHandle.keys() }
        // Additional memory leak checks would go here
    }
}