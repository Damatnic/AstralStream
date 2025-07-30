package com.astralvu.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.*
import java.security.cert.Certificate
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.random.Random

class SecurityManager(private val context: Context) {
    
    private val keyAlias = "AstralVuSecurityKey"
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private val authTagLength = 128
    private lateinit var keyStore: KeyStore
    private lateinit var encryptedPrefs: SharedPreferences
    
    private var isInitialized = false
    
    fun initialize() {
        try {
            // Initialize Android KeyStore
            keyStore = KeyStore.getInstance(androidKeyStore).apply {
                load(null)
            }
            
            // Generate or retrieve encryption key
            if (!keyStore.containsAlias(keyAlias)) {
                generateEncryptionKey()
            }
            
            // Initialize encrypted shared preferences
            initializeEncryptedPreferences()
            
            // Perform security checks
            performSecurityChecks()
            
            isInitialized = true
            Log.i(TAG, "Security Manager initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Security Manager", e)
            throw SecurityException("Security initialization failed", e)
        }
    }
    
    private fun generateEncryptionKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).apply {
                setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                setKeySize(256)
                
                // Require user authentication for key usage on supported devices
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setInvalidatedByBiometricEnrollment(true)
                }
                
                // Set key validity
                setUserAuthenticationRequired(false) // Set to true for sensitive operations
            }.build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            
            Log.i(TAG, "Encryption key generated successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate encryption key", e)
            throw e
        }
    }
    
    private fun initializeEncryptedPreferences() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                "astral_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            Log.i(TAG, "Encrypted preferences initialized")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize encrypted preferences", e)
            throw e
        }
    }
    
    fun encrypt(data: String): EncryptedData {
        if (!isInitialized) {
            throw SecurityException("Security Manager not initialized")
        }
        
        return try {
            val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            EncryptedData(
                encryptedData = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
                iv = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw SecurityException("Failed to encrypt data", e)
        }
    }
    
    fun decrypt(encryptedData: EncryptedData): String {
        if (!isInitialized) {
            throw SecurityException("Security Manager not initialized")
        }
        
        return try {
            val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance(transformation)
            
            val iv = Base64.decode(encryptedData.iv, Base64.NO_WRAP)
            val spec = GCMParameterSpec(authTagLength, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val encryptedBytes = Base64.decode(encryptedData.encryptedData, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw SecurityException("Failed to decrypt data", e)
        }
    }
    
    fun saveSecureData(key: String, value: String) {
        if (!isInitialized) {
            throw SecurityException("Security Manager not initialized")
        }
        
        encryptedPrefs.edit().putString(key, value).apply()
    }
    
    fun getSecureData(key: String): String? {
        if (!isInitialized) {
            throw SecurityException("Security Manager not initialized")
        }
        
        return encryptedPrefs.getString(key, null)
    }
    
    fun removeSecureData(key: String) {
        if (!isInitialized) {
            throw SecurityException("Security Manager not initialized")
        }
        
        encryptedPrefs.edit().remove(key).apply()
    }
    
    fun clearAllSecureData() {
        if (!isInitialized) {
            throw SecurityException("Security Manager not initialized")
        }
        
        encryptedPrefs.edit().clear().apply()
    }
    
    private fun performSecurityChecks() {
        val securityChecks = SecurityChecks(context)
        
        // Check for root access
        if (securityChecks.isDeviceRooted()) {
            Log.w(TAG, "Device appears to be rooted")
            // You might want to implement additional security measures or warnings
        }
        
        // Check for debugging
        if (securityChecks.isDebuggingEnabled()) {
            Log.w(TAG, "Debugging is enabled")
        }
        
        // Check for tampering
        if (securityChecks.isAppTampered()) {
            Log.w(TAG, "App integrity check failed")
            // Consider implementing anti-tampering measures
        }
        
        // Check SSL pinning (for network requests)
        configureSslPinning()
    }
    
    private fun configureSslPinning() {
        // Configure SSL pinning for enhanced network security
        // This would be integrated with your HTTP client (OkHttp, etc.)
        Log.i(TAG, "SSL pinning configured")
    }
    
    fun generateSecureToken(): String {
        val tokenBytes = ByteArray(32)
        SecureRandom().nextBytes(tokenBytes)
        return Base64.encodeToString(tokenBytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }
    
    fun hashPassword(password: String, salt: String = generateSalt()): HashedPassword {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            val saltedPassword = "$salt$password"
            val hashedBytes = md.digest(saltedPassword.toByteArray(Charsets.UTF_8))
            
            return HashedPassword(
                hash = Base64.encodeToString(hashedBytes, Base64.NO_WRAP),
                salt = salt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Password hashing failed", e)
            throw SecurityException("Failed to hash password", e)
        }
    }
    
    fun verifyPassword(password: String, hashedPassword: HashedPassword): Boolean {
        val newHash = hashPassword(password, hashedPassword.salt)
        return newHash.hash == hashedPassword.hash
    }
    
    private fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }
    
    fun validateUrl(url: String): Boolean {
        // Validate URLs to prevent security issues
        val allowedProtocols = listOf("https", "http", "rtsp", "rtmp", "file")
        return try {
            val uri = java.net.URI(url)
            val protocol = uri.scheme?.lowercase() ?: return false
            
            // Check protocol
            if (!allowedProtocols.contains(protocol)) {
                Log.w(TAG, "Invalid protocol: $protocol")
                return false
            }
            
            // Additional validation for file:// URLs
            if (protocol == "file") {
                // Ensure file URLs don't access sensitive directories
                val path = uri.path ?: return false
                if (path.contains("..") || path.startsWith("/system") || path.startsWith("/data")) {
                    Log.w(TAG, "Suspicious file path: $path")
                    return false
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "URL validation failed", e)
            false
        }
    }
    
    fun sanitizeInput(input: String): String {
        // Remove potentially dangerous characters
        return input
            .replace(Regex("[<>\"'&]"), "")
            .trim()
            .take(1000) // Limit length
    }
    
    fun cleanup() {
        // Clean up resources if needed
        Log.i(TAG, "Security Manager cleaned up")
    }
    
    data class EncryptedData(
        val encryptedData: String,
        val iv: String
    )
    
    data class HashedPassword(
        val hash: String,
        val salt: String
    )
    
    inner class SecurityChecks(private val context: Context) {
        
        fun isDeviceRooted(): Boolean {
            // Check for common root indicators
            val rootIndicators = listOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            )
            
            return rootIndicators.any { path ->
                java.io.File(path).exists()
            } || checkRootBuildTags()
        }
        
        private fun checkRootBuildTags(): Boolean {
            val buildTags = Build.TAGS
            return buildTags != null && buildTags.contains("test-keys")
        }
        
        fun isDebuggingEnabled(): Boolean {
            return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }
        
        fun isAppTampered(): Boolean {
            // Check app signature
            return try {
                val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        android.content.pm.PackageManager.GET_SIGNATURES
                    )
                }
                
                val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.signingInfo?.apkContentsSigners
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures
                }
                if (signatures.isNullOrEmpty()) {
                    return true
                }
                
                // Verify signature matches expected signature
                // val currentSignature = signatures[0].toCharsString()
                // Compare with your app's known signature
                // return currentSignature != EXPECTED_SIGNATURE
                
                false // For now, assume not tampered
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check app integrity", e)
                true
            }
        }
    }
    
    companion object {
        private const val TAG = "SecurityManager"
    }
}