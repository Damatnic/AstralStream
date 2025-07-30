package com.astralplayer.nextplayer.feature.player.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TestViewModel @Inject constructor() : ViewModel() {
    fun getTestString(): String = "Test ViewModel is working"
}