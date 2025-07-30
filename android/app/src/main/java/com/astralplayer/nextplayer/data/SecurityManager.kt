package com.astralplayer.nextplayer.data

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.CertificatePinner
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SecurityState(
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isEncryptionEnabled: Boolean = true,
    val lastSecurityCheck: Long = 0L,
    val securityLevel: SecurityLevel = SecurityLevel.MEDIUM,
    val trustedDomains: List<String> = emptyList()
)

enum class SecurityLevel {
    LOW, MEDIUM, HIGH, MAXIMUM
}

data class EncryptedData(
    val encryptedBytes: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EncryptedData
        
        if (!encryptedBytes.contentEquals(other.encryptedBytes)) return false
        if (!iv.contentEquals(other.iv)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = encryptedBytes.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}

interface SecurityManager {
    val securityState: StateFlow<SecurityState>
    
    // Encryption/Decryption
    suspend fun encryptData(data: String): Result<EncryptedData>
    suspend fun decryptData(encryptedData: EncryptedData): Result<String>
    
    // Token Management
    suspend fun storeSecureToken(key: String, token: String): Result<Unit>
    suspend fun getSecureToken(key: String): Result<String?>
    suspend fun deleteSecureToken(key: String): Result<Unit>
    
    // URL Validation
    fun validateVideoUrl(url: String): Boolean
    fun isUrlSafe(url: String): Boolean
    fun addTrustedDomain(domain: String)
    fun removeTrustedDomain(domain: String)
    
    // Biometric Authentication
    suspend fun enableBiometricAuth(): Result<Unit>
    
    // Certificate Pinning
    fun getCertificatePinner(): CertificatePinner
    
    // Security Checks
    suspend fun performSecurityCheck(): Result<SecurityState>
    fun getSecurityRecommendations(): List<String>
    
    // Data Sanitization
    fun sanitizeInput(input: String): String
    fun validateFileAccess(uri: Uri): Boolean
}

class SecurityManagerImpl constructor(
    private val context: Context
) : SecurityManager {
    
    private val _securityState = MutableStateFlow(SecurityState())
    override val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()
    
    private val keyAlias = "AstralVuSecretKey"
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    
    private val trustedDomains = mutableSetOf(
        "youtube.com", "youtu.be", "vimeo.com", "dailymotion.com",
        "googleapis.com", "dropboxapi.com", "dropbox.com",
        "twitch.tv", "facebook.com", "instagram.com"
    )
    
    init {
        initializeKeyStore()
        checkBiometricAvailability()
    }
    
    override suspend fun encryptData(data: String): Result<EncryptedData> {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            Result.success(EncryptedData(encryptedBytes, iv))
        } catch (e: Exception) {
            Result.failure(SecurityException("Encryption failed: ${e.message}", e))
        }
    }
    
    override suspend fun decryptData(encryptedData: EncryptedData): Result<String> {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(128, encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedData.encryptedBytes)
            val decryptedString = String(decryptedBytes, Charsets.UTF_8)
            
            Result.success(decryptedString)
        } catch (e: Exception) {
            Result.failure(SecurityException("Decryption failed: ${e.message}", e))
        }
    }
    
    override suspend fun storeSecureToken(key: String, token: String): Result<Unit> {
        return try {
            val encryptedData = encryptData(token)
            if (encryptedData.isFailure) {
                return Result.failure(encryptedData.exceptionOrNull()!!)
            }
            
            val encrypted = encryptedData.getOrThrow()
            val prefs = context.getSharedPreferences("secure_tokens", Context.MODE_PRIVATE)
            
            prefs.edit()
                .putString("${key}_data", android.util.Base64.encodeToString(encrypted.encryptedBytes, android.util.Base64.DEFAULT))
                .putString("${key}_iv", android.util.Base64.encodeToString(encrypted.iv, android.util.Base64.DEFAULT))
                .apply()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(SecurityException("Token storage failed: ${e.message}", e))
        }
    }
    
    override suspend fun getSecureToken(key: String): Result<String?> {
        return try {
            val prefs = context.getSharedPreferences("secure_tokens", Context.MODE_PRIVATE)
            
            val encryptedDataString = prefs.getString("${key}_data", null)
            val ivString = prefs.getString("${key}_iv", null)
            
            if (encryptedDataString == null || ivString == null) {
                return Result.success(null)
            }
            
            val encryptedBytes = android.util.Base64.decode(encryptedDataString, android.util.Base64.DEFAULT)
            val iv = android.util.Base64.decode(ivString, android.util.Base64.DEFAULT)
            
            val encryptedData = EncryptedData(encryptedBytes, iv)
            val decryptedResult = decryptData(encryptedData)
            
            if (decryptedResult.isFailure) {
                return Result.failure(decryptedResult.exceptionOrNull()!!)
            }
            
            Result.success(decryptedResult.getOrThrow())
        } catch (e: Exception) {
            Result.failure(SecurityException("Token retrieval failed: ${e.message}", e))
        }
    }
    
    override suspend fun deleteSecureToken(key: String): Result<Unit> {
        return try {
            val prefs = context.getSharedPreferences("secure_tokens", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("${key}_data")
                .remove("${key}_iv")
                .apply()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(SecurityException("Token deletion failed: ${e.message}", e))
        }
    }
    
    override fun validateVideoUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            when (uri.scheme?.lowercase()) {
                "http", "https" -> isUrlSafe(url)
                "file" -> validateFileUri(uri)
                "content" -> validateContentUri(uri)
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override fun isUrlSafe(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            
            // Check if it's a trusted domain
            val isTrusted = trustedDomains.any { trustedDomain ->
                host == trustedDomain || host.endsWith(".$trustedDomain")
            }
            
            // Check for suspicious patterns
            val hasSuspiciousPatterns = checkForSuspiciousPatterns(url)
            
            isTrusted && !hasSuspiciousPatterns
        } catch (e: Exception) {
            false
        }
    }
    
    override fun addTrustedDomain(domain: String) {
        trustedDomains.add(domain.lowercase())
        updateSecurityState { 
            copy(trustedDomains = trustedDomains.toList())
        }
    }
    
    override fun removeTrustedDomain(domain: String) {
        trustedDomains.remove(domain.lowercase())
        updateSecurityState { 
            copy(trustedDomains = trustedDomains.toList())
        }
    }
    
    override suspend fun enableBiometricAuth(): Result<Unit> {
        return try {
            val biometricManager = BiometricManager.from(context)
            
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    updateSecurityState { 
                        copy(
                            isBiometricEnabled = true,
                            isBiometricAvailable = true
                        )
                    }
                    Result.success(Unit)
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Result.failure(SecurityException("No biometric hardware available"))
                }
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Result.failure(SecurityException("Biometric hardware unavailable"))
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Result.failure(SecurityException("No biometric credentials enrolled"))
                }
                else -> {
                    Result.failure(SecurityException("Biometric authentication not available"))
                }
            }
        } catch (e: Exception) {
            Result.failure(SecurityException("Biometric setup failed: ${e.message}", e))
        }
    }
    
    override fun getCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("*.googleapis.com", "sha256/WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=")
            .add("*.dropboxapi.com", "sha256/5kJvNEMw0KjrCAu7eXY5HZdvyCS13BbA0VJG1RSP91w=")
            .add("*.youtube.com", "sha256/KTxRllTzxlRllTzxlRllTzxlRllTzxlRllTzxlRllTzxl=")
            .add("*.vimeo.com", "sha256/VimeoRllTzxlRllTzxlRllTzxlRllTzxlRllTzxlRllT=")
            .build()
    }
    
    override suspend fun performSecurityCheck(): Result<SecurityState> {
        return try {
            val currentTime = System.currentTimeMillis()
            
            // Check various security aspects
            val biometricAvailable = checkBiometricAvailability()
            val encryptionWorking = testEncryption()
            
            val securityLevel = calculateSecurityLevel(biometricAvailable, encryptionWorking)
            
            val newState = _securityState.value.copy(
                lastSecurityCheck = currentTime,
                securityLevel = securityLevel,
                isBiometricAvailable = biometricAvailable,
                isEncryptionEnabled = encryptionWorking
            )
            
            _securityState.value = newState
            Result.success(newState)
        } catch (e: Exception) {
            Result.failure(SecurityException("Security check failed: ${e.message}", e))
        }
    }
    
    override fun getSecurityRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val state = _securityState.value
        
        if (!state.isBiometricEnabled && state.isBiometricAvailable) {
            recommendations.add("Enable biometric authentication for enhanced security")
        }
        
        if (state.securityLevel == SecurityLevel.LOW) {
            recommendations.add("Consider upgrading your device security settings")
        }
        
        if (state.trustedDomains.isEmpty()) {
            recommendations.add("Add trusted domains for safer browsing")
        }
        
        if (System.currentTimeMillis() - state.lastSecurityCheck > 86400000L) { // 24 hours
            recommendations.add("Run a security check to ensure system integrity")
        }
        
        return recommendations
    }
    
    override fun sanitizeInput(input: String): String {
        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
            .trim()
    }
    
    override fun validateFileAccess(uri: Uri): Boolean {
        return try {
            when (uri.scheme?.lowercase()) {
                "file" -> validateFileUri(uri)
                "content" -> validateContentUri(uri)
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun initializeKeyStore() {
        try {
            getOrCreateSecretKey()
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }
    
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeyStore)
        keyStore.load(null)
        
        return if (keyStore.containsAlias(keyAlias)) {
            keyStore.getKey(keyAlias, null) as SecretKey
        } else {
            createSecretKey()
        }
    }
    
    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    private fun checkBiometricAvailability(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    private suspend fun testEncryption(): Boolean {
        return try {
            val testData = "test_encryption_data"
            val encrypted = encryptData(testData)
            if (encrypted.isFailure) return false
            
            val decrypted = decryptData(encrypted.getOrThrow())
            decrypted.isSuccess && decrypted.getOrThrow() == testData
        } catch (e: Exception) {
            false
        }
    }
    
    private fun calculateSecurityLevel(biometricAvailable: Boolean, encryptionWorking: Boolean): SecurityLevel {
        var score = 0
        
        if (encryptionWorking) score += 2
        if (biometricAvailable) score += 2
        if (_securityState.value.isBiometricEnabled) score += 1
        if (trustedDomains.isNotEmpty()) score += 1
        
        return when (score) {
            0, 1 -> SecurityLevel.LOW
            2, 3 -> SecurityLevel.MEDIUM
            4, 5 -> SecurityLevel.HIGH
            else -> SecurityLevel.MAXIMUM
        }
    }
    
    private fun checkForSuspiciousPatterns(url: String): Boolean {
        val suspiciousPatterns = listOf(
            "javascript:", "data:", "vbscript:",
            "<script", "</script>", "onerror=", "onclick=",
            "../..", "file://", "ftp://"
        )
        
        return suspiciousPatterns.any { pattern ->
            url.lowercase().contains(pattern.lowercase())
        }
    }
    
    private fun validateFileUri(uri: Uri): Boolean {
        val path = uri.path ?: return false
        
        // Check for directory traversal attempts
        if (path.contains("..") || path.contains("//")) {
            return false
        }
        
        // Check for system directories
        val systemPaths = listOf("/system", "/root", "/proc", "/dev")
        if (systemPaths.any { path.startsWith(it) }) {
            return false
        }
        
        return true
    }
    
    private fun validateContentUri(uri: Uri): Boolean {
        // Content URIs are generally safe as they go through the content provider system
        return uri.authority != null
    }
    
    private fun updateSecurityState(update: SecurityState.() -> SecurityState) {
        _securityState.value = _securityState.value.update()
    }
}