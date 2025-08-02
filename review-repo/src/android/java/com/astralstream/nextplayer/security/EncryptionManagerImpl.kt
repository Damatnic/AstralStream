package com.astralstream.nextplayer.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionManagerImpl @Inject constructor() : EncryptionManager {
    
    private val keyAlias = "AstralStreamKey"
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private val ivSize = 12
    private val tagSize = 128
    
    init {
        generateKey()
    }
    
    private fun generateKey() {
        val keyStore = KeyStore.getInstance(androidKeyStore)
        keyStore.load(null)
        
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
    
    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeyStore)
        keyStore.load(null)
        return keyStore.getKey(keyAlias, null) as SecretKey
    }
    
    override fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        
        // Combine IV and encrypted data
        return iv + encryptedData
    }
    
    override fun decrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        
        // Extract IV from data
        val iv = data.sliceArray(0 until ivSize)
        val encryptedData = data.sliceArray(ivSize until data.size)
        
        val spec = GCMParameterSpec(tagSize, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        
        return cipher.doFinal(encryptedData)
    }
    
    override fun encryptString(data: String): String {
        val encrypted = encrypt(data.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }
    
    override fun decryptString(data: String): String {
        val decoded = Base64.decode(data, Base64.DEFAULT)
        val decrypted = decrypt(decoded)
        return String(decrypted)
    }
}