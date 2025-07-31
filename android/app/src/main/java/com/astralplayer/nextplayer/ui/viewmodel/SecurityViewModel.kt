package com.astralplayer.nextplayer.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astralplayer.nextplayer.security.AppLockManager
import com.astralplayer.nextplayer.security.BiometricManager
import com.astralplayer.nextplayer.security.HiddenFolderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    private val biometricManager: BiometricManager,
    private val hiddenFolderManager: HiddenFolderManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _securityState = MutableStateFlow(SecurityState())
    val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()
    
    init {
        loadSecurityState()
    }
    
    private fun loadSecurityState() {
        viewModelScope.launch {
            val hiddenVideos = hiddenFolderManager.getHiddenVideos()
            val hiddenStorageUsed = hiddenVideos.sumOf { File(it.encryptedPath).length() }
            
            _securityState.value = SecurityState(
                isAppLockEnabled = appLockManager.isLockEnabled(),
                isBiometricAvailable = biometricManager.isBiometricAvailable(),
                hiddenVideoCount = hiddenVideos.size,
                hiddenStorageUsed = hiddenStorageUsed,
                totalStorage = context.filesDir.totalSpace
            )
        }
    }
    
    fun toggleAppLock() {
        val newState = !_securityState.value.isAppLockEnabled
        appLockManager.setLockEnabled(newState)
        _securityState.value = _securityState.value.copy(isAppLockEnabled = newState)
    }
    
    fun lockNow() = appLockManager.lockApp()
    
    data class SecurityState(
        val isAppLockEnabled: Boolean = false,
        val isBiometricAvailable: Boolean = false,
        val hiddenVideoCount: Int = 0,
        val hiddenStorageUsed: Long = 0,
        val totalStorage: Long = 0
    )
}