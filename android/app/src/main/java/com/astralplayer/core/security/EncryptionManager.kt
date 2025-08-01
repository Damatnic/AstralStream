package com.astralplayer.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "EncryptionManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "AstralStreamSubtitleCache"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }
    
    init {
        generateKeyIfNeeded()
    }
    
    /**
     * Data class for encrypted content with IV
     */
    data class EncryptionResult(
        val encryptedData: String,
        val iv: String
    )
    
    /**
     * Encrypt content with generated IV
     */
    fun encryptWithIV(plainText: String): EncryptionResult {
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            return EncryptionResult(
                encryptedData = Base64.getEncoder().encodeToString(encryptedBytes),
                iv = Base64.getEncoder().encodeToString(iv)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw SecurityException("Failed to encrypt data", e)
        }
    }
    
    /**
     * Decrypt content with provided IV
     */
    fun decryptWithIV(encryptedData: String, ivString: String): String {
        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            val iv = Base64.getDecoder().decode(ivString)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            val encryptedBytes = Base64.getDecoder().decode(encryptedData)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw SecurityException("Failed to decrypt data", e)
        }
    }
    
    /**
     * Legacy encrypt method for backward compatibility
     */
    fun encrypt(plainText: String): String {
        val result = encryptWithIV(plainText)
        // Combine IV and encrypted data for legacy format
        return "${result.iv}:${result.encryptedData}"
    }
    
    /**
     * Legacy decrypt method for backward compatibility
     */
    fun decrypt(encryptedData: String): String {
        return if (encryptedData.contains(":")) {
            // Legacy format with IV
            val parts = encryptedData.split(":")
            if (parts.size == 2) {
                decryptWithIV(parts[1], parts[0])
            } else {
                throw SecurityException("Invalid encrypted data format")
            }
        } else {
            // New format, assume IV is separate
            throw SecurityException("IV required for decryption")
        }
    }
    
    /**
     * Generate a hash for data integrity checking
     */
    fun hash(input: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(hashBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Hashing failed", e)
            throw SecurityException("Failed to hash data", e)
        }
    }
    
    /**
     * Verify hash
     */
    fun verify(input: String, hash: String): Boolean {
        return try {
            val computedHash = hash(input)
            computedHash == hash
        } catch (e: Exception) {
            Log.e(TAG, "Hash verification failed", e)
            false
        }
    }
    
    /**
     * Get current key alias
     */
    fun getCurrentKeyAlias(): String = KEY_ALIAS
    
    private fun generateKeyIfNeeded() {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateSecretKey()
                Log.d(TAG, "Generated new encryption key")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check/generate key", e)
        }
    }
    
    private fun generateSecretKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    
    private fun getOrCreateSecretKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            generateSecretKey()
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }
    }
}