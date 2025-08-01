package com.astralplayer.features.gestures.viewmodel

import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.features.gestures.data.*
import com.astralplayer.features.gestures.repository.GestureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestureTestViewModel @Inject constructor(
    private val repository: GestureRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    
    private val _uiState = MutableStateFlow(GestureTestUiState())
    val uiState: StateFlow<GestureTestUiState> = _uiState.asStateFlow()
    
    private val _detectedGestures = MutableStateFlow<List<DetectedGesture>>(emptyList())
    
    init {
        loadGestures()
    }
    
    private fun loadGestures() {
        viewModelScope.launch {
            repository.getEnabledGestures()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { gestures ->
                    _uiState.update { it.copy(availableGestures = gestures) }
                }
        }
    }
    
    fun onGestureDetected(
        type: GestureType,
        zone: GestureZone,
        startPosition: Offset,
        endPosition: Offset
    ) {
        viewModelScope.launch {
            // Find matching gesture configuration
            val matchingGesture = _uiState.value.availableGestures
                .find { it.gestureType == type && (it.zone == zone || it.zone == GestureZone.FULL_SCREEN) }
            
            val detectedGesture = DetectedGesture(
                type = type,
                zone = zone,
                startPosition = startPosition,
                endPosition = endPosition,
                timestamp = System.currentTimeMillis(),
                matchedAction = matchingGesture?.action
            )
            
            // Add to history
            val updatedGestures = (_detectedGestures.value + detectedGesture).takeLast(50)
            _detectedGestures.value = updatedGestures
            
            // Update UI state
            _uiState.update {
                it.copy(
                    lastDetectedGesture = detectedGesture,
                    detectedGestures = updatedGestures
                )
            }
            
            // Provide haptic feedback if enabled
            if (matchingGesture?.hapticFeedback == true) {
                provideHapticFeedback()
            }
            
            // Record gesture usage
            repository.recordGestureUsage(
                type = type,
                zone = zone,
                action = matchingGesture?.action ?: GestureAction.NONE,
                startX = startPosition.x,
                startY = startPosition.y,
                endX = endPosition.x,
                endY = endPosition.y,
                wasSuccessful = matchingGesture != null
            )
            
            // Clear the detected gesture after a delay
            delay(2000)
            _uiState.update {
                if (it.lastDetectedGesture == detectedGesture) {
                    it.copy(lastDetectedGesture = null)
                } else it
            }
        }
    }
    
    private fun provideHapticFeedback() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }
    
    fun clearHistory() {
        _detectedGestures.value = emptyList()
        _uiState.update {
            it.copy(
                detectedGestures = emptyList(),
                lastDetectedGesture = null
            )
        }
    }
}

data class GestureTestUiState(
    val availableGestures: List<GestureEntity> = emptyList(),
    val lastDetectedGesture: DetectedGesture? = null,
    val detectedGestures: List<DetectedGesture> = emptyList(),
    val error: String? = null
)

data class DetectedGesture(
    val type: GestureType,
    val zone: GestureZone,
    val startPosition: Offset,
    val endPosition: Offset,
    val timestamp: Long,
    val matchedAction: GestureAction? = null
)