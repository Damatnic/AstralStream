package com.astralplayer.nextplayer.security

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Advanced encryption manager for secure data handling
 * Supports multiple encryption levels and algorithms
 */
class EncryptionManager(private val context: Context) {
    
    private val _encryptionEvents = MutableSharedFlow<EncryptionEvent>()
    val encryptionEvents: SharedFlow<EncryptionEvent> = _encryptionEvents.asSharedFlow()
    
    private val _encryptionState = MutableStateFlow(EncryptionState())
    val encryptionState: StateFlow<EncryptionState> = _encryptionState.asStateFlow()
    
    private var masterKey: MasterKey? = null
    private val keyStore = KeyStore.getInstance("AndroidKeyStore")
    private val secureRandom = SecureRandom()
    
    private val encryptionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false
    
    // Key aliases for different encryption levels
    private val keyAliases = mapOf(
        EncryptionLevel.BASIC to "astral_basic_key",
        EncryptionLevel.STANDARD to "astral_standard_key", 
        EncryptionLevel.HIGH to "astral_high_key",
        EncryptionLevel.MAXIMUM to "astral_maximum_key"
    )
    
    /**
     * Initialize encryption manager
     */
    suspend fun initialize(masterKey: MasterKey) {
        this.masterKey = masterKey
        keyStore.load(null)
        isInitialized = true
        
        // Generate encryption keys for all levels
        generateEncryptionKeys()
        
        // Start key health monitoring
        startKeyHealthMonitoring()
        
        _encryptionState.value = _encryptionState.value.copy(
            isInitialized = true,
            keyHealthScore = 1.0f
        )
        
        _encryptionEvents.emit(EncryptionEvent.Initialized)
    }
    
    /**
     * Encrypt sensitive data
     */
    suspend fun encryptData(
        data: ByteArray,
        encryptionLevel: EncryptionLevel
    ): EncryptionResult = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            // Compress data if beneficial
            val dataToEncrypt = if (data.size > 1024) {
                compressData(data)
            } else {
                data
            }
            
            val encryptedData = when (encryptionLevel) {
                EncryptionLevel.BASIC -> encryptWithBasicLevel(dataToEncrypt)
                EncryptionLevel.STANDARD -> encryptWithStandardLevel(dataToEncrypt)
                EncryptionLevel.HIGH -> encryptWithHighLevel(dataToEncrypt)
                EncryptionLevel.MAXIMUM -> encryptWithMaximumLevel(dataToEncrypt)
            }
            
            val encryptionTime = System.currentTimeMillis() - startTime
            
            val encryptionInfo = EncryptionInfo(
                algorithm = getAlgorithmForLevel(encryptionLevel),
                encryptionLevel = encryptionLevel,
                keyVersion = getCurrentKeyVersion(encryptionLevel),
                timestamp = System.currentTimeMillis(),
                dataType = SensitiveDataType.CACHE_DATA, // Default, should be specified
                isCompressed = data.size > 1024,
                originalSize = data.size,
                encryptedSize = encryptedData.size
            )
            
            // Update encryption metrics
            updateEncryptionMetrics(encryptionLevel, encryptionTime, data.size)
            
            _encryptionEvents.emit(
                EncryptionEvent.DataEncrypted(encryptionLevel, data.size, encryptionTime)
            )
            
            EncryptionResult.Success(encryptedData, encryptionInfo)
            
        } catch (e: Exception) {
            _encryptionEvents.emit(
                EncryptionEvent.EncryptionFailed(encryptionLevel, e)
            )
            EncryptionResult.Error(e)
        }
    }
    
    /**
     * Decrypt sensitive data
     */
    suspend fun decryptData(
        encryptedData: ByteArray,
        encryptionInfo: EncryptionInfo
    ): DecryptionResult = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            // Validate encryption info
            if (!isEncryptionInfoValid(encryptionInfo)) {
                return@withContext DecryptionResult.InvalidEncryptionInfo
            }
            
            val decryptedData = when (encryptionInfo.encryptionLevel) {
                EncryptionLevel.BASIC -> decryptWithBasicLevel(encryptedData, encryptionInfo)
                EncryptionLevel.STANDARD -> decryptWithStandardLevel(encryptedData, encryptionInfo)
                EncryptionLevel.HIGH -> decryptWithHighLevel(encryptedData, encryptionInfo)
                EncryptionLevel.MAXIMUM -> decryptWithMaximumLevel(encryptedData, encryptionInfo)
            }
            
            // Decompress if needed
            val finalData = if (encryptionInfo.isCompressed) {
                decompressData(decryptedData)
            } else {
                decryptedData
            }
            
            val decryptionTime = System.currentTimeMillis() - startTime
            
            // Update decryption metrics
            updateDecryptionMetrics(encryptionInfo.encryptionLevel, decryptionTime, finalData.size)
            
            _encryptionEvents.emit(
                EncryptionEvent.DataDecrypted(encryptionInfo.encryptionLevel, finalData.size, decryptionTime)
            )
            
            DecryptionResult.Success(finalData)
            
        } catch (e: Exception) {
            _encryptionEvents.emit(
                EncryptionEvent.DecryptionFailed(encryptionInfo.encryptionLevel, e)
            )
            DecryptionResult.Error(e)
        }
    }
    
    /**
     * Encrypt content file
     */
    suspend fun encryptContent(
        contentUri: Uri,
        protectionLevel: ContentProtectionLevel
    ): EncryptionResult = withContext(Dispatchers.IO) {
        try {
            val encryptionLevel = mapProtectionToEncryptionLevel(protectionLevel)
            
            // For content encryption, we typically encrypt metadata rather than the entire file
            val contentMetadata = generateContentMetadata(contentUri, protectionLevel)
            val metadataBytes = contentMetadata.toByteArray()
            
            encryptData(metadataBytes, encryptionLevel)
            
        } catch (e: Exception) {
            EncryptionResult.Error(e)
        }
    }
    
    /**
     * Encrypt access token data
     */
    suspend fun encryptTokenData(tokenData: AccessTokenData): String = withContext(Dispatchers.IO) {
        try {
            val tokenJson = serializeTokenData(tokenData)
            val encryptionResult = encryptData(tokenJson.toByteArray(), EncryptionLevel.HIGH)
            
            when (encryptionResult) {
                is EncryptionResult.Success -> {
                    Base64.encodeToString(encryptionResult.encryptedData, Base64.NO_WRAP)
                }
                is EncryptionResult.Error -> throw encryptionResult.exception
            }
            
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Rotate encryption keys for enhanced security
     */
    suspend fun rotateKeys(): KeyRotationResult = withContext(Dispatchers.IO) {
        try {
            val rotationResults = mutableMapOf<EncryptionLevel, Boolean>()
            
            keyAliases.forEach { (level, alias) ->
                try {
                    // Delete old key
                    keyStore.deleteEntry(alias)
                    
                    // Generate new key
                    generateKeyForLevel(level)
                    
                    rotationResults[level] = true
                    
                    _encryptionEvents.emit(
                        EncryptionEvent.KeyRotated(level)
                    )
                    
                } catch (e: Exception) {
                    rotationResults[level] = false
                }
            }
            
            // Update key health after rotation
            updateKeyHealth()
            
            val successfulRotations = rotationResults.values.count { it }
            val totalKeys = rotationResults.size
            
            if (successfulRotations == totalKeys) {
                KeyRotationResult.Success(totalKeys)
            } else {
                KeyRotationResult.PartialSuccess(successfulRotations, totalKeys)
            }
            
        } catch (e: Exception) {
            KeyRotationResult.Error(e)
        }
    }
    
    /**
     * Check encryption key health
     */
    fun checkKeyHealth(): Float {
        return try {
            var healthScore = 1.0f
            var totalKeys = 0
            var healthyKeys = 0
            
            keyAliases.values.forEach { alias ->
                totalKeys++
                if (keyStore.containsAlias(alias)) {
                    try {
                        val key = keyStore.getKey(alias, null)
                        if (key != null) {
                            healthyKeys++
                        }
                    } catch (e: Exception) {
                        // Key exists but is corrupted or inaccessible
                    }
                }
            }
            
            healthScore = if (totalKeys > 0) healthyKeys.toFloat() / totalKeys else 0f
            
            // Consider key age in health calculation
            val keyAge = getAverageKeyAge()
            val ageScore = maxOf(0f, 1f - (keyAge / (365 * 24 * 60 * 60 * 1000L))) // 1 year max age
            
            (healthScore + ageScore) / 2f
            
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * Get encryption metrics
     */
    fun getEncryptionMetrics(): EncryptionMetrics {
        val state = _encryptionState.value
        return EncryptionMetrics(
            totalEncryptions = state.totalEncryptions,
            totalDecryptions = state.totalDecryptions,
            averageEncryptionTime = state.averageEncryptionTime,
            averageDecryptionTime = state.averageDecryptionTime,
            encryptionsByLevel = state.encryptionsByLevel,
            keyHealthScore = state.keyHealthScore,
            lastKeyRotation = state.lastKeyRotation,
            encryptionEfficiency = calculateEncryptionEfficiency()
        )
    }
    
    // Private implementation methods
    private suspend fun generateEncryptionKeys() {
        EncryptionLevel.values().forEach { level ->
            generateKeyForLevel(level)
        }
    }
    
    private fun generateKeyForLevel(level: EncryptionLevel) {
        val alias = keyAliases[level] ?: return
        
        if (keyStore.containsAlias(alias)) {
            return // Key already exists
        }
        
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        
        val keyGenParameterSpec = when (level) {
            EncryptionLevel.BASIC -> KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(128) // AES-128 for basic level
                .build()
                
            EncryptionLevel.STANDARD -> KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256) // AES-256 for standard level
                .build()
                
            EncryptionLevel.HIGH -> KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
                
            EncryptionLevel.MAXIMUM -> KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false) // Can be set to true for additional security
                .build()
        }
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    
    private fun encryptWithBasicLevel(data: ByteArray): ByteArray {
        val key = getKeyForLevel(EncryptionLevel.BASIC)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        
        // Combine IV and encrypted data
        return iv + encryptedData
    }
    
    private fun encryptWithStandardLevel(data: ByteArray): ByteArray {
        val key = getKeyForLevel(EncryptionLevel.STANDARD)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        
        return iv + encryptedData
    }
    
    private fun encryptWithHighLevel(data: ByteArray): ByteArray {
        // Add additional security layer for high level
        val key = getKeyForLevel(EncryptionLevel.HIGH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        // Add salt for additional security
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(salt + data)
        
        return iv + encryptedData
    }
    
    private fun encryptWithMaximumLevel(data: ByteArray): ByteArray {
        // Double encryption for maximum security
        val key = getKeyForLevel(EncryptionLevel.MAXIMUM)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        
        // First encryption round
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv1 = cipher.iv
        val firstEncryption = cipher.doFinal(data)
        
        // Second encryption round with new IV
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv2 = cipher.iv
        val secondEncryption = cipher.doFinal(firstEncryption)
        
        // Combine both IVs and final encrypted data
        return iv1 + iv2 + secondEncryption
    }
    
    private fun decryptWithBasicLevel(encryptedData: ByteArray, encryptionInfo: EncryptionInfo): ByteArray {
        val key = getKeyForLevel(EncryptionLevel.BASIC)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        
        val iv = encryptedData.sliceArray(0..11) // GCM IV is 12 bytes
        val cipherText = encryptedData.sliceArray(12 until encryptedData.size)
        
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        return cipher.doFinal(cipherText)
    }
    
    private fun decryptWithStandardLevel(encryptedData: ByteArray, encryptionInfo: EncryptionInfo): ByteArray {
        val key = getKeyForLevel(EncryptionLevel.STANDARD)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        
        val iv = encryptedData.sliceArray(0..11)
        val cipherText = encryptedData.sliceArray(12 until encryptedData.size)
        
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        return cipher.doFinal(cipherText)
    }
    
    private fun decryptWithHighLevel(encryptedData: ByteArray, encryptionInfo: EncryptionInfo): ByteArray {
        val key = getKeyForLevel(EncryptionLevel.HIGH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        
        val iv = encryptedData.sliceArray(0..11)
        val cipherText = encryptedData.sliceArray(12 until encryptedData.size)
        
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        val decryptedWithSalt = cipher.doFinal(cipherText)
        
        // Remove salt (first 16 bytes)
        return decryptedWithSalt.sliceArray(16 until decryptedWithSalt.size)
    }
    
    private fun decryptWithMaximumLevel(encryptedData: ByteArray, encryptionInfo: EncryptionInfo): ByteArray {
        val key = getKeyForLevel(EncryptionLevel.MAXIMUM)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        
        // Extract both IVs
        val iv1 = encryptedData.sliceArray(0..11)
        val iv2 = encryptedData.sliceArray(12..23)
        val cipherText = encryptedData.sliceArray(24 until encryptedData.size)
        
        // First decryption round
        val spec2 = GCMParameterSpec(128, iv2)
        cipher.init(Cipher.DECRYPT_MODE, key, spec2)
        val firstDecryption = cipher.doFinal(cipherText)
        
        // Second decryption round
        val spec1 = GCMParameterSpec(128, iv1)
        cipher.init(Cipher.DECRYPT_MODE, key, spec1)
        
        return cipher.doFinal(firstDecryption)
    }
    
    private fun getKeyForLevel(level: EncryptionLevel): SecretKey {
        val alias = keyAliases[level] ?: throw IllegalArgumentException("Invalid encryption level")
        return keyStore.getKey(alias, null) as SecretKey
    }
    
    private fun compressData(data: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val gzipStream = GZIPOutputStream(outputStream)
        gzipStream.write(data)
        gzipStream.close()
        return outputStream.toByteArray()
    }
    
    private fun decompressData(compressedData: ByteArray): ByteArray {
        val inputStream = ByteArrayInputStream(compressedData)
        val gzipStream = GZIPInputStream(inputStream)
        return gzipStream.readBytes()
    }
    
    private fun mapProtectionToEncryptionLevel(protectionLevel: ContentProtectionLevel): EncryptionLevel {
        return when (protectionLevel) {
            ContentProtectionLevel.NONE -> EncryptionLevel.BASIC
            ContentProtectionLevel.BASIC -> EncryptionLevel.BASIC
            ContentProtectionLevel.STANDARD -> EncryptionLevel.STANDARD
            ContentProtectionLevel.HIGH -> EncryptionLevel.HIGH
            ContentProtectionLevel.DRM_PROTECTED -> EncryptionLevel.MAXIMUM
        }
    }
    
    private fun generateContentMetadata(contentUri: Uri, protectionLevel: ContentProtectionLevel): String {
        return "URI:${contentUri}|PROTECTION:${protectionLevel}|TIMESTAMP:${System.currentTimeMillis()}"
    }
    
    private fun serializeTokenData(tokenData: AccessTokenData): String {
        return "${tokenData.contentUri}|${tokenData.component}|${tokenData.accessType}|${tokenData.issuedAt}|${tokenData.expiresAt}|${tokenData.nonce}"
    }
    
    private fun getAlgorithmForLevel(level: EncryptionLevel): String {
        return when (level) {
            EncryptionLevel.BASIC -> "AES-128-GCM"
            EncryptionLevel.STANDARD -> "AES-256-GCM"
            EncryptionLevel.HIGH -> "AES-256-GCM-Salt"
            EncryptionLevel.MAXIMUM -> "AES-256-GCM-Double"
        }
    }
    
    private fun getCurrentKeyVersion(level: EncryptionLevel): Int {
        // Track key versions for rotation management
        return 1 // Simplified version tracking
    }
    
    private fun isEncryptionInfoValid(encryptionInfo: EncryptionInfo): Boolean {
        return encryptionInfo.timestamp > 0 &&
               encryptionInfo.encryptionLevel != null &&
               encryptionInfo.algorithm.isNotEmpty()
    }
    
    private fun startKeyHealthMonitoring() {
        encryptionScope.launch {
            while (isInitialized && currentCoroutineContext().isActive) {
                try {
                    updateKeyHealth()
                    delay(300000) // Check every 5 minutes
                } catch (e: Exception) {
                    // Handle monitoring error but continue
                }
            }
        }
    }
    
    private fun updateKeyHealth() {
        val healthScore = checkKeyHealth()
        _encryptionState.value = _encryptionState.value.copy(
            keyHealthScore = healthScore,
            lastHealthCheck = System.currentTimeMillis()
        )
    }
    
    private fun updateEncryptionMetrics(level: EncryptionLevel, time: Long, dataSize: Int) {
        val state = _encryptionState.value
        val newTotal = state.totalEncryptions + 1
        val newAverage = ((state.averageEncryptionTime * state.totalEncryptions) + time) / newTotal
        
        val levelCounts = state.encryptionsByLevel.toMutableMap()
        levelCounts[level] = (levelCounts[level] ?: 0) + 1
        
        _encryptionState.value = state.copy(
            totalEncryptions = newTotal,
            averageEncryptionTime = newAverage,
            encryptionsByLevel = levelCounts,
            totalBytesEncrypted = state.totalBytesEncrypted + dataSize
        )
    }
    
    private fun updateDecryptionMetrics(level: EncryptionLevel, time: Long, dataSize: Int) {
        val state = _encryptionState.value
        val newTotal = state.totalDecryptions + 1
        val newAverage = ((state.averageDecryptionTime * state.totalDecryptions) + time) / newTotal
        
        _encryptionState.value = state.copy(
            totalDecryptions = newTotal,
            averageDecryptionTime = newAverage,
            totalBytesDecrypted = state.totalBytesDecrypted + dataSize
        )
    }
    
    private fun getAverageKeyAge(): Long {
        // Simplified key age calculation
        return System.currentTimeMillis() - (_encryptionState.value.lastKeyRotation ?: 0L)
    }
    
    private fun calculateEncryptionEfficiency(): Float {
        val state = _encryptionState.value
        if (state.totalEncryptions == 0L) return 1.0f
        
        // Calculate efficiency based on time and throughput
        val avgTimePerByte = if (state.totalBytesEncrypted > 0) {
            state.averageEncryptionTime.toFloat() / state.totalBytesEncrypted
        } else 1.0f
        
        // Lower time per byte = higher efficiency
        return maxOf(0.1f, 1.0f - (avgTimePerByte / 1000f)) // Normalize to reasonable range
    }
    
    fun cleanup() {
        isInitialized = false
        encryptionScope.cancel()
    }
}

// Data classes for encryption management
data class EncryptionState(
    val isInitialized: Boolean = false,
    val totalEncryptions: Long = 0L,
    val totalDecryptions: Long = 0L,
    val averageEncryptionTime: Long = 0L,
    val averageDecryptionTime: Long = 0L,
    val encryptionsByLevel: Map<EncryptionLevel, Int> = emptyMap(),
    val keyHealthScore: Float = 1.0f,
    val lastKeyRotation: Long? = null,
    val lastHealthCheck: Long = 0L,
    val totalBytesEncrypted: Long = 0L,
    val totalBytesDecrypted: Long = 0L
)

data class EncryptionInfo(
    val algorithm: String,
    val encryptionLevel: EncryptionLevel,
    val keyVersion: Int,
    val timestamp: Long,
    val dataType: SensitiveDataType,
    val isCompressed: Boolean,
    val originalSize: Int,
    val encryptedSize: Int
)

data class EncryptionMetrics(
    val totalEncryptions: Long,
    val totalDecryptions: Long,
    val averageEncryptionTime: Long,
    val averageDecryptionTime: Long,
    val encryptionsByLevel: Map<EncryptionLevel, Int>,
    val keyHealthScore: Float,
    val lastKeyRotation: Long?,
    val encryptionEfficiency: Float
)

data class AccessTokenData(
    val contentUri: String,
    val component: String,
    val accessType: AccessType,
    val issuedAt: Long,
    val expiresAt: Long,
    val nonce: String
)

sealed class EncryptionResult {
    data class Success(val encryptedData: ByteArray, val encryptionInfo: EncryptionInfo) : EncryptionResult()
    object NotRequired : EncryptionResult()
    data class Error(val exception: Exception) : EncryptionResult()
}

sealed class DecryptionResult {
    data class Success(val decryptedData: ByteArray) : DecryptionResult()
    object InvalidEncryptionInfo : DecryptionResult()
    data class Error(val exception: Exception) : DecryptionResult()
}

sealed class KeyRotationResult {
    data class Success(val keysRotated: Int) : KeyRotationResult()
    data class PartialSuccess(val successfulRotations: Int, val totalKeys: Int) : KeyRotationResult()
    data class Error(val exception: Exception) : KeyRotationResult()
}

sealed class EncryptionEvent {
    object Initialized : EncryptionEvent()
    data class DataEncrypted(val level: EncryptionLevel, val dataSize: Int, val time: Long) : EncryptionEvent()
    data class DataDecrypted(val level: EncryptionLevel, val dataSize: Int, val time: Long) : EncryptionEvent()
    data class EncryptionFailed(val level: EncryptionLevel, val error: Exception) : EncryptionEvent()
    data class DecryptionFailed(val level: EncryptionLevel, val error: Exception) : EncryptionEvent()
    data class KeyRotated(val level: EncryptionLevel) : EncryptionEvent()
}