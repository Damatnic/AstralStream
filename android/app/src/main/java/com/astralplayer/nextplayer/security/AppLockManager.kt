package com.astralplayer.nextplayer.security

import android.content.Context
import android.os.CountDownTimer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferences: SecurePreferences
) {
    private var lockTimer: CountDownTimer? = null
    private var isLocked = false
    private val lockDelay = 5000L // 5 seconds after backgrounding
    
    fun isAppLocked(): Boolean = isLocked
    
    fun lockApp() {
        isLocked = true
        securePreferences.putBoolean("app_locked", true)
    }
    
    fun unlockApp() {
        isLocked = false
        securePreferences.putBoolean("app_locked", false)
        cancelLockTimer()
    }
    
    fun onAppBackgrounded() {
        if (isLockEnabled()) {
            startLockTimer()
        }
    }
    
    fun onAppForegrounded() {
        if (isLocked || (lockTimer != null && isLockEnabled())) {
            lockApp()
        }
        cancelLockTimer()
    }
    
    private fun startLockTimer() {
        cancelLockTimer()
        lockTimer = object : CountDownTimer(lockDelay, lockDelay) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                lockApp()
            }
        }.start()
    }
    
    private fun cancelLockTimer() {
        lockTimer?.cancel()
        lockTimer = null
    }
    
    fun isLockEnabled(): Boolean = securePreferences.getBoolean("lock_enabled", true)
    fun setLockEnabled(enabled: Boolean) = securePreferences.putBoolean("lock_enabled", enabled)
    
    fun isHiddenFolderLocked(): Boolean = securePreferences.getBoolean("hidden_folder_locked", true)
    fun setHiddenFolderLocked(locked: Boolean) = securePreferences.putBoolean("hidden_folder_locked", locked)
}