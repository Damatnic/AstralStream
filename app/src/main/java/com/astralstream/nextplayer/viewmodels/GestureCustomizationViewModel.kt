package com.astralstream.nextplayer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralstream.nextplayer.feature.player.gestures.AdvancedGestureManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestureCustomizationViewModel @Inject constructor(
    private val gestureManager: AdvancedGestureManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(GestureCustomizationState())
    val state: StateFlow<GestureCustomizationState> = _state.asStateFlow()
    
    data class GestureCustomizationState(
        val selectedZone: GestureZone? = null,
        val zones: List<GestureZone> = GestureZone.values().toList(),
        val gestureTypes: List<GestureType> = GestureType.values().toList(),
        val actions: List<GestureAction> = GestureAction.values().toList(),
        val currentMappings: Map<GestureZone, Map<GestureType, GestureAction>> = emptyMap(),
        val isDirty: Boolean = false
    )
    
    enum class GestureZone(val displayName: String) {
        TOP_LEFT("Top Left"),
        TOP_CENTER("Top Center"),
        TOP_RIGHT("Top Right"),
        MIDDLE_LEFT("Middle Left"),
        MIDDLE_CENTER("Middle Center"),
        MIDDLE_RIGHT("Middle Right"),
        BOTTOM_LEFT("Bottom Left"),
        BOTTOM_CENTER("Bottom Center"),
        BOTTOM_RIGHT("Bottom Right")
    }
    
    enum class GestureType(val displayName: String) {
        TAP("Tap"),
        DOUBLE_TAP("Double Tap"),
        LONG_PRESS("Long Press"),
        SWIPE_UP("Swipe Up"),
        SWIPE_DOWN("Swipe Down"),
        SWIPE_LEFT("Swipe Left"),
        SWIPE_RIGHT("Swipe Right")
    }
    
    enum class GestureAction(val displayName: String) {
        PLAY_PAUSE("Play/Pause"),
        SEEK_FORWARD("Seek Forward"),
        SEEK_BACKWARD("Seek Backward"),
        VOLUME_UP("Volume Up"),
        VOLUME_DOWN("Volume Down"),
        BRIGHTNESS_UP("Brightness Up"),
        BRIGHTNESS_DOWN("Brightness Down"),
        NEXT_VIDEO("Next Video"),
        PREVIOUS_VIDEO("Previous Video"),
        TOGGLE_FULLSCREEN("Toggle Fullscreen"),
        SHOW_CONTROLS("Show Controls"),
        SHOW_INFO("Show Info"),
        NONE("No Action")
    }
    
    init {
        loadCurrentMappings()
    }
    
    private fun loadCurrentMappings() {
        viewModelScope.launch {
            val mappings = gestureManager.getGestureMappings()
            _state.value = _state.value.copy(currentMappings = mappings)
        }
    }
    
    fun selectZone(zone: GestureZone) {
        _state.value = _state.value.copy(selectedZone = zone)
    }
    
    fun updateGestureMapping(zone: GestureZone, gestureType: GestureType, action: GestureAction) {
        viewModelScope.launch {
            val currentMappings = _state.value.currentMappings.toMutableMap()
            val zoneMappings = currentMappings[zone]?.toMutableMap() ?: mutableMapOf()
            zoneMappings[gestureType] = action
            currentMappings[zone] = zoneMappings
            
            _state.value = _state.value.copy(
                currentMappings = currentMappings,
                isDirty = true
            )
            
            gestureManager.updateGestureMapping(zone, gestureType, action)
        }
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            gestureManager.saveGestureMappings(_state.value.currentMappings)
            _state.value = _state.value.copy(isDirty = false)
        }
    }
    
    fun resetToDefaults() {
        viewModelScope.launch {
            gestureManager.resetToDefaults()
            loadCurrentMappings()
            _state.value = _state.value.copy(isDirty = false)
        }
    }
}