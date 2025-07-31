package com.astralplayer.nextplayer.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionEngine: EncryptionEngine
) {
    private val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
    
    fun putEncryptedString(key: String, value: String) {
        val encrypted = encryptionEngine.encryptString(value)
        prefs.edit().putString(key, encrypted).apply()
    }
    
    fun getEncryptedString(key: String): String? {
        val encrypted = prefs.getString(key, null) ?: return null
        return try {
            encryptionEngine.decryptString(encrypted)
        } catch (e: Exception) {
            null
        }
    }
    
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
    
    fun getBoolean(key: String, default: Boolean): Boolean {
        return prefs.getBoolean(key, default)
    }
    
    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }
    
    fun getLong(key: String, default: Long): Long {
        return prefs.getLong(key, default)
    }
}