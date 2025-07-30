package com.astralplayer.nextplayer.feature.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ABRepeatState(
    val isEnabled: Boolean = false,
    val pointA: Long? = null,
    val pointB: Long? = null,
    val isRepeating: Boolean = false,
    val currentLoop: Int = 0,
    val maxLoops: Int = -1
)

class ABRepeatManager {
    private val _state = MutableStateFlow(ABRepeatState())
    val state: StateFlow<ABRepeatState> = _state.asStateFlow()
    
    var onSeekToPosition: ((Long) -> Unit)? = null
    
    fun setPointA(position: Long) {
        _state.value = _state.value.copy(
            pointA = position,
            pointB = if (_state.value.pointB != null && _state.value.pointB!! <= position) null else _state.value.pointB
        )
    }
    
    fun setPointB(position: Long) {
        val pointA = _state.value.pointA
        if (pointA != null && position > pointA) {
            _state.value = _state.value.copy(
                pointB = position,
                isEnabled = true
            )
        }
    }
    
    fun toggleRepeat() {
        val currentState = _state.value
        if (currentState.pointA != null && currentState.pointB != null) {
            _state.value = currentState.copy(
                isRepeating = !currentState.isRepeating,
                currentLoop = 0
            )
        }
    }
    
    fun checkPosition(currentPosition: Long): Boolean {
        val currentState = _state.value
        
        if (!currentState.isRepeating || currentState.pointA == null || currentState.pointB == null) {
            return false
        }
        
        if (currentPosition >= currentState.pointB!!) {
            val newLoop = currentState.currentLoop + 1
            
            if (currentState.maxLoops == -1 || newLoop <= currentState.maxLoops) {
                _state.value = currentState.copy(currentLoop = newLoop)
                onSeekToPosition?.invoke(currentState.pointA!!)
                return true
            } else {
                _state.value = currentState.copy(isRepeating = false, currentLoop = 0)
            }
        }
        
        return false
    }
    
    fun clearPoints() {
        _state.value = ABRepeatState()
    }
}