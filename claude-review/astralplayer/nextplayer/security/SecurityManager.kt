package com.astralplayer.nextplayer.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Comprehensive security manager for AstralStream
 * Handles biometric authentication, encrypted storage, and adult content privacy
 */
class SecurityManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SecurityManager"
        private const val KEYSTORE_ALIAS = "AstralStreamSecureKey"
        private const val ENCRYPTED_PREFS_NAME = "astral_secure_prefs"
        private const val ADULT_CONTENT_HISTORY_KEY = "adult_content_history"
        private const val PRIVATE_MODE_ENABLED_KEY = "private_mode_enabled"
        private const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"
        private const val INCOGNITO_SESSION_KEY = "incognito_session_active"
        private const val CONTENT_LOCK_TIMEOUT_KEY = "content_lock_timeout"
        
        // Security levels
        const val SECURITY_LEVEL_NONE = 0
        const val SECURITY_LEVEL_PIN = 1
        const val SECURITY_LEVEL_BIOMETRIC = 2
        const val SECURITY_LEVEL_HIGH = 3
    }
    
    private val keyStore = KeyStore.getInstance("AndroidKeyStore")
    private var encryptedPrefs: SharedPreferences? = null
    private var biometricPrompt: BiometricPrompt? = null
    
    init {
        keyStore.load(null)
        initializeEncryptedStorage()
    }
    
    /**
     * Initialize encrypted shared preferences for sensitive data
     */
    private fun initializeEncryptedStorage() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            Log.d(TAG, "Encrypted storage initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize encrypted storage", e)
            // Fallback to regular SharedPreferences with warning
            encryptedPrefs = context.getSharedPreferences("${ENCRYPTED_PREFS_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Check if biometric authentication is available and enabled
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * Enable/disable biometric authentication for adult content
     */
    fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs?.edit()?.putBoolean(BIOMETRIC_ENABLED_KEY, enabled)?.apply()
        Log.d(TAG, "Biometric authentication ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Check if biometric authentication is enabled
     */
    fun isBiometricEnabled(): Boolean {
        return encryptedPrefs?.getBoolean(BIOMETRIC_ENABLED_KEY, false) ?: false
    }
    
    /**
     * Authenticate user with biometrics for adult content access
     */
    fun authenticateForAdultContent(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onUserCancel: () -> Unit
    ) {
        if (!isBiometricAvailable()) {
            onError("Biometric authentication not available")
            return
        }
        
        val executor = ContextCompat.getMainExecutor(context)
        biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Biometric authentication succeeded")
                onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.w(TAG, "Biometric authentication error: $errString")
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onUserCancel()
                    else -> onError(errString.toString())
                }
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Biometric authentication failed")
                onError("Authentication failed. Please try again.")
            }
        })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Adult Content Access")
            .setSubtitle("Authenticate to access private content")
            .setDescription("Use your fingerprint or face recognition to securely access adult content")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        
        biometricPrompt?.authenticate(promptInfo)
    }
    
    /**
     * Enable/disable private mode
     */
    fun setPrivateModeEnabled(enabled: Boolean) {
        encryptedPrefs?.edit()?.putBoolean(PRIVATE_MODE_ENABLED_KEY, enabled)?.apply()
        
        if (!enabled) {
            // Clear incognito session when private mode is disabled
            setIncognitoSessionActive(false)
        }
        
        Log.d(TAG, "Private mode ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Check if private mode is enabled
     */
    fun isPrivateModeEnabled(): Boolean {
        return encryptedPrefs?.getBoolean(PRIVATE_MODE_ENABLED_KEY, false) ?: false
    }
    
    /**
     * Set incognito session status
     */
    fun setIncognitoSessionActive(active: Boolean) {
        encryptedPrefs?.edit()?.putBoolean(INCOGNITO_SESSION_KEY, active)?.apply()
        Log.d(TAG, "Incognito session ${if (active) "started" else "ended"}")
    }
    
    /**
     * Check if incognito session is active
     */
    fun isIncognitoSessionActive(): Boolean {
        return encryptedPrefs?.getBoolean(INCOGNITO_SESSION_KEY, false) ?: false
    }
    
    /**
     * Securely store adult content history entry
     */
    suspend fun storeAdultContentHistory(url: String, title: String, timestamp: Long) {
        if (!isPrivateModeEnabled() && !isIncognitoSessionActive()) {
            withContext(Dispatchers.IO) {
                try {
                    val encryptedData = encryptSensitiveData("$url|$title|$timestamp")
                    val existingHistory = getEncryptedAdultContentHistory().toMutableList()
                    existingHistory.add(encryptedData)
                    
                    // Keep only last 100 entries for performance
                    if (existingHistory.size > 100) {
                        existingHistory.removeAt(0)
                    }
                    
                    val historyString = existingHistory.joinToString(";")
                    encryptedPrefs?.edit()?.putString(ADULT_CONTENT_HISTORY_KEY, historyString)?.apply()
                    
                    Log.d(TAG, "Adult content history entry stored securely")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to store adult content history", e)
                }
            }
        }
    }
    
    /**
     * Retrieve and decrypt adult content history
     */
    suspend fun getAdultContentHistory(): List<ContentHistoryEntry> {
        return withContext(Dispatchers.IO) {
            try {
                val encryptedEntries = getEncryptedAdultContentHistory()
                encryptedEntries.mapNotNull { encryptedEntry ->
                    try {
                        val decryptedData = decryptSensitiveData(encryptedEntry)
                        val parts = decryptedData.split("|")
                        if (parts.size == 3) {
                            ContentHistoryEntry(
                                url = parts[0],
                                title = parts[1],
                                timestamp = parts[2].toLong()
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to decrypt history entry", e)
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retrieve adult content history", e)
                emptyList()
            }
        }
    }
    
    /**
     * Clear all adult content history
     */
    fun clearAdultContentHistory() {
        encryptedPrefs?.edit()?.remove(ADULT_CONTENT_HISTORY_KEY)?.apply()
        Log.d(TAG, "Adult content history cleared")
    }
    
    /**
     * Set content lock timeout (in minutes)
     */
    fun setContentLockTimeout(timeoutMinutes: Int) {
        encryptedPrefs?.edit()?.putInt(CONTENT_LOCK_TIMEOUT_KEY, timeoutMinutes)?.apply()
        Log.d(TAG, "Content lock timeout set to $timeoutMinutes minutes")
    }
    
    /**
     * Get content lock timeout
     */
    fun getContentLockTimeout(): Int {
        return encryptedPrefs?.getInt(CONTENT_LOCK_TIMEOUT_KEY, 15) ?: 15 // Default 15 minutes
    }
    
    /**
     * Check if content should be locked based on timeout
     */
    fun shouldLockContent(): Boolean {
        val lastAccess = encryptedPrefs?.getLong("last_adult_content_access", 0) ?: 0
        val timeout = getContentLockTimeout() * 60 * 1000L // Convert to milliseconds
        val currentTime = System.currentTimeMillis()
        
        return (currentTime - lastAccess) > timeout
    }
    
    /**
     * Update last adult content access timestamp
     */
    fun updateLastContentAccess() {
        encryptedPrefs?.edit()?.putLong("last_adult_content_access", System.currentTimeMillis())?.apply()
    }
    
    /**
     * Get current security level
     */
    fun getCurrentSecurityLevel(): Int {
        return when {
            isBiometricEnabled() && isBiometricAvailable() -> SECURITY_LEVEL_BIOMETRIC
            isPrivateModeEnabled() -> SECURITY_LEVEL_PIN
            else -> SECURITY_LEVEL_NONE
        }
    }
    
    /**
     * Generate security recommendations
     */
    fun getSecurityRecommendations(): List<SecurityRecommendation> {
        val recommendations = mutableListOf<SecurityRecommendation>()
        
        if (!isPrivateModeEnabled()) {
            recommendations.add(
                SecurityRecommendation(
                    title = "Enable Private Mode",
                    description = "Protect your adult content browsing with private mode",
                    priority = SecurityPriority.HIGH
                )
            )
        }
        
        if (!isBiometricEnabled() && isBiometricAvailable()) {
            recommendations.add(
                SecurityRecommendation(
                    title = "Enable Biometric Lock",
                    description = "Use fingerprint or face recognition for secure access",
                    priority = SecurityPriority.MEDIUM
                )
            )
        }
        
        if (getContentLockTimeout() > 30) {
            recommendations.add(
                SecurityRecommendation(
                    title = "Reduce Lock Timeout",
                    description = "Consider reducing content lock timeout for better security",
                    priority = SecurityPriority.LOW
                )
            )
        }
        
        return recommendations
    }
    
    private fun getEncryptedAdultContentHistory(): List<String> {
        val historyString = encryptedPrefs?.getString(ADULT_CONTENT_HISTORY_KEY, "") ?: ""
        return if (historyString.isNotEmpty()) {
            historyString.split(";")
        } else {
            emptyList()
        }
    }
    
    private fun encryptSensitiveData(data: String): String {
        return try {
            generateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = iv + encryptedData
            android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            data // Fallback to plain text with warning
        }
    }
    
    private fun decryptSensitiveData(encryptedData: String): String {
        return try {
            val combined = android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT)
            val iv = combined.sliceArray(0..11) // GCM IV is 12 bytes
            val cipherText = combined.sliceArray(12 until combined.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            encryptedData // Return encrypted data as fallback
        }
    }
    
    private fun generateSecretKey() {
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
    
    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
    }
    
    /**
     * Perform security cleanup (call on app pause/destroy)
     */
    fun performSecurityCleanup() {
        if (isIncognitoSessionActive()) {
            // Clear any temporary data
            clearIncognitoData()
        }
        
        // Update last access time if adult content was accessed
        if (isPrivateModeEnabled()) {
            updateLastContentAccess()
        }
        
        Log.d(TAG, "Security cleanup performed")
    }
    
    private fun clearIncognitoData() {
        // Clear any cached adult content data
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.contains("adult_content") || file.name.contains("incognito")) {
                try {
                    file.delete()
                    Log.d(TAG, "Cleared incognito file: ${file.name}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear incognito file: ${file.name}", e)
                }
            }
        }
    }
}

/**
 * Data classes for security management
 */
data class ContentHistoryEntry(
    val url: String,
    val title: String,
    val timestamp: Long
) {
    val formattedTimestamp: String
        get() = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
}

data class SecurityRecommendation(
    val title: String,
    val description: String,
    val priority: SecurityPriority
)

enum class SecurityPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}