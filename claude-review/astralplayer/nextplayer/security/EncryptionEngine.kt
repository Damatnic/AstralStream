package com.astralplayer.nextplayer.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyAlias = "AstralStreamKey"
    private val transformation = "AES/GCM/NoPadding"
    private val androidKeyStore = "AndroidKeyStore"
    
    init {
        generateKey()
    }
    
    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false) // We handle auth separately
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    
    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeyStore)
        keyStore.load(null)
        return keyStore.getKey(keyAlias, null) as SecretKey
    }
    
    suspend fun encryptFile(inputFile: File, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())
            
            val iv = cipher.iv
            val buffer = ByteArray(8192)
            
            FileInputStream(inputFile).use { input ->
                FileOutputStream(outputFile).use { output ->
                    // Write IV first
                    output.write(iv.size)
                    output.write(iv)
                    
                    // Encrypt file in chunks
                    val cipherOutputStream = CipherOutputStream(output, cipher)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        cipherOutputStream.write(buffer, 0, bytesRead)
                    }
                    cipherOutputStream.close()
                }
            }
            true
        } catch (e: Exception) {
            Log.e("EncryptionEngine", "Failed to encrypt file", e)
            false
        }
    }
    
    suspend fun decryptFile(inputFile: File, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            FileInputStream(inputFile).use { input ->
                // Read IV
                val ivSize = input.read()
                val iv = ByteArray(ivSize)
                input.read(iv)
                
                val cipher = Cipher.getInstance(transformation)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
                
                FileOutputStream(outputFile).use { output ->
                    val cipherInputStream = CipherInputStream(input, cipher)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (cipherInputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("EncryptionEngine", "Failed to decrypt file", e)
            false
        }
    }
    
    // Fast in-memory encryption for metadata
    fun encryptString(plainText: String): String {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray())
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
    
    fun decryptString(encryptedText: String): String {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        val iv = combined.sliceArray(0..11)
        val encrypted = combined.sliceArray(12 until combined.size)
        
        val cipher = Cipher.getInstance(transformation)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        
        return String(cipher.doFinal(encrypted))
    }
}