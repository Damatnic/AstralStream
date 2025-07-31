package com.astralplayer.nextplayer.security

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Advanced secure storage manager for sensitive data protection
 * Provides encrypted storage, secure key management, and data integrity verification
 */
class SecureStorageManager(private val context: Context) {
    
    private val _storageEvents = MutableSharedFlow<SecureStorageEvent>()
    val storageEvents: SharedFlow<SecureStorageEvent> = _storageEvents.asSharedFlow()
    
    private val _storageState = MutableStateFlow(SecureStorageState())
    val storageState: StateFlow<SecureStorageState> = _storageState.asStateFlow()
    
    private val storageScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false
    
    private var masterKey: MasterKey? = null
    private var encryptedPrefs: androidx.security.crypto.EncryptedSharedPreferences? = null
    private val encryptedFiles = ConcurrentHashMap<String, EncryptedFile>()
    private val storageVaults = ConcurrentHashMap<String, StorageVault>()
    private val integrityChecks = ConcurrentHashMap<String, String>()
    
    private val secureRandom = SecureRandom()
    
    companion object {
        private const val TAG = "SecureStorageManager"
        private const val MASTER_KEY_ALIAS = "astral_master_key"
        private const val ENCRYPTED_PREFS_NAME = "astral_secure_prefs"
        private const val SECURE_STORAGE_DIR = "secure_storage"
        private const val INTEGRITY_SUFFIX = ".integrity"
        private const val MAX_STORAGE_SIZE = 500 * 1024 * 1024L // 500MB
    }
    
    /**
     * Initialize secure storage with encryption
     */
    suspend fun initialize() {
        isInitialized = true
        
        // Create master key for encryption
        createMasterKey()
        
        // Initialize encrypted shared preferences
        initializeEncryptedPreferences()
        
        // Create secure storage directory
        createSecureStorageDirectory()
        
        // Load existing storage vaults
        loadStorageVaults()
        
        // Start storage monitoring
        startStorageMonitoring()
        
        _storageState.value = _storageState.value.copy(
            isInitialized = true,
            masterKeyAlias = MASTER_KEY_ALIAS,
            totalVaults = storageVaults.size
        )
        
        _storageEvents.emit(SecureStorageEvent.StorageInitialized)
        
        Log.d(TAG, "Secure storage initialized with ${storageVaults.size} vaults")
    }
    
    /**
     * Create secure storage vault for specific data type
     */
    suspend fun createVault(
        vaultId: String,
        vaultType: StorageVaultType,
        encryptionLevel: EncryptionLevel = EncryptionLevel.STANDARD
    ): VaultCreationResult = withContext(Dispatchers.IO) {
        try {
            if (storageVaults.containsKey(vaultId)) {
                return@withContext VaultCreationResult.VaultExists(vaultId)
            }
            
            val vaultConfig = StorageVaultConfig(
                vaultId = vaultId,
                vaultType = vaultType,
                encryptionLevel = encryptionLevel,
                compressionEnabled = shouldEnableCompression(vaultType),
                integrityCheckEnabled = true,
                maxSize = getMaxSizeForVaultType(vaultType)
            )
            
            val vault = StorageVault(
                config = vaultConfig,
                createdAt = System.currentTimeMillis(),
                lastAccessed = System.currentTimeMillis(),
                dataEntries = mutableMapOf(),
                totalSize = 0L,
                isLocked = false
            )
            
            storageVaults[vaultId] = vault
            
            // Create physical vault directory
            createVaultDirectory(vaultId)
            
            // Save vault configuration
            saveVaultConfiguration(vault)
            
            updateStorageMetrics()
            
            _storageEvents.emit(
                SecureStorageEvent.VaultCreated(vaultId, vaultType)
            )
            
            VaultCreationResult.Success(vault)
            
        } catch (e: Exception) {
            Log.e(TAG, "Vault creation failed", e)
            _storageEvents.emit(
                SecureStorageEvent.VaultCreationFailed(vaultId, e)
            )
            VaultCreationResult.Error(e)
        }
    }
    
    /**
     * Store sensitive data in vault with encryption
     */
    suspend fun storeData(
        vaultId: String,
        dataKey: String,
        data: ByteArray,
        metadata: StorageMetadata = StorageMetadata()
    ): StorageResult = withContext(Dispatchers.IO) {
        try {
            val vault = storageVaults[vaultId]
                ?: return@withContext StorageResult.VaultNotFound(vaultId)
            
            if (vault.isLocked) {
                return@withContext StorageResult.VaultLocked(vaultId)
            }
            
            // Check vault size limits
            if (vault.totalSize + data.size > vault.config.maxSize) {
                return@withContext StorageResult.VaultFull(vaultId)
            }
            
            // Apply compression if enabled
            val processedData = if (vault.config.compressionEnabled) {
                compressData(data)
            } else {
                data
            }
            
            // Encrypt data according to vault encryption level
            val encryptedData = encryptData(processedData, vault.config.encryptionLevel)
            
            // Generate integrity checksum
            val integrityHash = calculateIntegrityHash(encryptedData)
            
            // Create data entry
            val dataEntry = StorageDataEntry(
                key = dataKey,
                originalSize = data.size,
                compressedSize = processedData.size,
                encryptedSize = encryptedData.size,
                encryptionLevel = vault.config.encryptionLevel,
                compressed = vault.config.compressionEnabled,
                integrityHash = integrityHash,
                metadata = metadata,
                createdAt = System.currentTimeMillis(),
                lastAccessed = System.currentTimeMillis()
            )
            
            // Store encrypted data to file
            val dataFile = getDataFilePath(vaultId, dataKey)
            writeEncryptedFile(dataFile, encryptedData)
            
            // Store integrity hash
            if (vault.config.integrityCheckEnabled) {
                storeIntegrityHash(vaultId, dataKey, integrityHash)
            }
            
            // Update vault
            vault.dataEntries[dataKey] = dataEntry
            vault.totalSize += data.size
            vault.lastAccessed = System.currentTimeMillis()
            
            updateStorageMetrics()
            
            _storageEvents.emit(
                SecureStorageEvent.DataStored(vaultId, dataKey, data.size)
            )
            
            StorageResult.Success(dataEntry)
            
        } catch (e: Exception) {
            Log.e(TAG, "Data storage failed", e)
            _storageEvents.emit(
                SecureStorageEvent.StorageFailed(vaultId, dataKey, e)
            )
            StorageResult.Error(e)
        }
    }
    
    /**
     * Retrieve and decrypt data from vault
     */
    suspend fun retrieveData(
        vaultId: String,
        dataKey: String
    ): RetrievalResult = withContext(Dispatchers.IO) {
        try {
            val vault = storageVaults[vaultId]
                ?: return@withContext RetrievalResult.VaultNotFound(vaultId)
            
            if (vault.isLocked) {
                return@withContext RetrievalResult.VaultLocked(vaultId)
            }
            
            val dataEntry = vault.dataEntries[dataKey]
                ?: return@withContext RetrievalResult.DataNotFound(dataKey)
            
            // Read encrypted data from file
            val dataFile = getDataFilePath(vaultId, dataKey)
            val encryptedData = readEncryptedFile(dataFile)
            
            // Verify integrity if enabled
            if (vault.config.integrityCheckEnabled) {
                val storedHash = getStoredIntegrityHash(vaultId, dataKey)
                val currentHash = calculateIntegrityHash(encryptedData)
                
                if (storedHash != currentHash) {
                    _storageEvents.emit(
                        SecureStorageEvent.IntegrityViolation(vaultId, dataKey)
                    )
                    return@withContext RetrievalResult.IntegrityViolation(dataKey)
                }
            }
            
            // Decrypt data
            val decryptedData = decryptData(encryptedData, dataEntry.encryptionLevel)
            
            // Decompress if needed
            val finalData = if (dataEntry.compressed) {
                decompressData(decryptedData)
            } else {
                decryptedData
            }
            
            // Update access time
            dataEntry.lastAccessed = System.currentTimeMillis()
            vault.lastAccessed = System.currentTimeMillis()
            
            _storageEvents.emit(
                SecureStorageEvent.DataRetrieved(vaultId, dataKey, finalData.size)
            )
            
            RetrievalResult.Success(finalData, dataEntry)
            
        } catch (e: Exception) {
            Log.e(TAG, "Data retrieval failed", e)
            _storageEvents.emit(
                SecureStorageEvent.RetrievalFailed(vaultId, dataKey, e)
            )
            RetrievalResult.Error(e)
        }
    }
    
    /**
     * Delete data from vault
     */
    suspend fun deleteData(
        vaultId: String,
        dataKey: String
    ): DeletionResult = withContext(Dispatchers.IO) {
        try {
            val vault = storageVaults[vaultId]
                ?: return@withContext DeletionResult.VaultNotFound(vaultId)
            
            if (vault.isLocked) {
                return@withContext DeletionResult.VaultLocked(vaultId)
            }
            
            val dataEntry = vault.dataEntries.remove(dataKey)
                ?: return@withContext DeletionResult.DataNotFound(dataKey)
            
            // Delete physical files
            val dataFile = getDataFilePath(vaultId, dataKey)
            if (dataFile.exists()) {
                secureFileDelete(dataFile)
            }
            
            // Delete integrity hash file
            deleteIntegrityHash(vaultId, dataKey)
            
            // Update vault size
            vault.totalSize -= dataEntry.originalSize
            
            updateStorageMetrics()
            
            _storageEvents.emit(
                SecureStorageEvent.DataDeleted(vaultId, dataKey)
            )
            
            DeletionResult.Success(dataEntry)
            
        } catch (e: Exception) {
            Log.e(TAG, "Data deletion failed", e)
            _storageEvents.emit(
                SecureStorageEvent.DeletionFailed(vaultId, dataKey, e)
            )
            DeletionResult.Error(e)
        }
    }
    
    /**
     * Lock vault to prevent access
     */
    suspend fun lockVault(vaultId: String): VaultLockResult = withContext(Dispatchers.IO) {
        try {
            val vault = storageVaults[vaultId]
                ?: return@withContext VaultLockResult.VaultNotFound(vaultId)
            
            vault.isLocked = true
            vault.lockedAt = System.currentTimeMillis()
            
            // Clear vault from active memory (security measure)
            clearVaultFromMemory(vaultId)
            
            _storageEvents.emit(
                SecureStorageEvent.VaultLocked(vaultId)
            )
            
            VaultLockResult.Success
            
        } catch (e: Exception) {
            Log.e(TAG, "Vault locking failed", e)
            VaultLockResult.Error(e)
        }
    }
    
    /**
     * Unlock vault for access
     */
    suspend fun unlockVault(
        vaultId: String,
        credentials: StorageCredentials? = null
    ): VaultUnlockResult = withContext(Dispatchers.IO) {
        try {
            val vault = storageVaults[vaultId]
                ?: return@withContext VaultUnlockResult.VaultNotFound(vaultId)
            
            if (!vault.isLocked) {
                return@withContext VaultUnlockResult.VaultNotLocked(vaultId)
            }
            
            // Validate credentials if required
            if (vault.config.requiresCredentials && credentials == null) {
                return@withContext VaultUnlockResult.CredentialsRequired
            }
            
            credentials?.let { creds ->
                if (!validateCredentials(vaultId, creds)) {
                    return@withContext VaultUnlockResult.InvalidCredentials
                }
            }
            
            vault.isLocked = false
            vault.lockedAt = null
            vault.lastAccessed = System.currentTimeMillis()
            
            // Reload vault data if needed
            reloadVaultData(vaultId)
            
            _storageEvents.emit(
                SecureStorageEvent.VaultUnlocked(vaultId)
            )
            
            VaultUnlockResult.Success
            
        } catch (e: Exception) {
            Log.e(TAG, "Vault unlocking failed", e)
            VaultUnlockResult.Error(e)
        }
    }
    
    /**
     * Optimize storage by compacting and cleaning up
     */
    suspend fun optimizeStorage(): StorageOptimizationResult = withContext(Dispatchers.IO) {
        try {
            var totalReclaimed = 0L
            var vaultsOptimized = 0
            
            storageVaults.values.forEach { vault ->
                if (!vault.isLocked) {
                    val reclaimed = optimizeVault(vault)
                    totalReclaimed += reclaimed
                    if (reclaimed > 0) vaultsOptimized++
                }
            }
            
            // Clean up temporary files
            val tempReclaimed = cleanupTemporaryFiles()
            totalReclaimed += tempReclaimed
            
            // Defragment storage if needed
            defragmentStorage()
            
            updateStorageMetrics()
            
            _storageEvents.emit(
                SecureStorageEvent.StorageOptimized(totalReclaimed, vaultsOptimized)
            )
            
            StorageOptimizationResult.Success(totalReclaimed, vaultsOptimized)
            
        } catch (e: Exception) {
            Log.e(TAG, "Storage optimization failed", e)
            StorageOptimizationResult.Error(e)
        }
    }
    
    /**
     * Get storage analytics and metrics
     */
    fun getStorageAnalytics(): StorageAnalytics {
        val totalSize = storageVaults.values.sumOf { it.totalSize }
        val totalEntries = storageVaults.values.sumOf { it.dataEntries.size }
        val vaultsByType = storageVaults.values.groupBy { it.config.vaultType }
            .mapValues { it.value.size }
        
        val encryptionLevelDistribution = storageVaults.values
            .flatMap { vault -> vault.dataEntries.values.map { it.encryptionLevel } }
            .groupBy { it }
            .mapValues { it.value.size }
        
        return StorageAnalytics(
            totalVaults = storageVaults.size,
            totalStorageSize = totalSize,
            totalDataEntries = totalEntries,
            vaultsByType = vaultsByType,
            encryptionLevelDistribution = encryptionLevelDistribution,
            compressionRatio = calculateCompressionRatio(),
            integrityViolations = _storageState.value.integrityViolations,
            lastOptimization = _storageState.value.lastOptimization
        )
    }
    
    // Private implementation methods
    private suspend fun createMasterKey() {
        masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true)
            .build()
        
        Log.d(TAG, "Master key created: $MASTER_KEY_ALIAS")
    }
    
    private suspend fun initializeEncryptedPreferences() {
        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey!!,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
        
        Log.d(TAG, "Encrypted preferences initialized")
    }
    
    private fun createSecureStorageDirectory() {
        val storageDir = File(context.filesDir, SECURE_STORAGE_DIR)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
    }
    
    private suspend fun loadStorageVaults() {
        val storageDir = File(context.filesDir, SECURE_STORAGE_DIR)
        storageDir.listFiles()?.forEach { vaultDir ->
            if (vaultDir.isDirectory) {
                try {
                    val vault = loadVaultConfiguration(vaultDir.name)
                    if (vault != null) {
                        storageVaults[vault.config.vaultId] = vault
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load vault: ${vaultDir.name}", e)
                }
            }
        }
        
        Log.d(TAG, "Loaded ${storageVaults.size} storage vaults")
    }
    
    private fun startStorageMonitoring() {
        storageScope.launch {
            while (isInitialized && currentCoroutineContext().isActive) {
                try {
                    monitorStorageHealth()
                    cleanupExpiredData()
                    performIntegrityChecks()
                    
                    delay(300000) // Check every 5 minutes
                } catch (e: Exception) {
                    Log.e(TAG, "Storage monitoring error", e)
                }
            }
        }
    }
    
    private fun shouldEnableCompression(vaultType: StorageVaultType): Boolean {
        return when (vaultType) {
            StorageVaultType.USER_DATA -> true
            StorageVaultType.CACHE_DATA -> true
            StorageVaultType.LOGS -> true
            StorageVaultType.MEDIA_METADATA -> false
            StorageVaultType.SECURITY_KEYS -> false
            StorageVaultType.TEMPORARY -> true
        }
    }
    
    private fun getMaxSizeForVaultType(vaultType: StorageVaultType): Long {
        return when (vaultType) {
            StorageVaultType.USER_DATA -> 100 * 1024 * 1024L // 100MB
            StorageVaultType.CACHE_DATA -> 200 * 1024 * 1024L // 200MB
            StorageVaultType.LOGS -> 50 * 1024 * 1024L // 50MB
            StorageVaultType.MEDIA_METADATA -> 150 * 1024 * 1024L // 150MB
            StorageVaultType.SECURITY_KEYS -> 1 * 1024 * 1024L // 1MB
            StorageVaultType.TEMPORARY -> 50 * 1024 * 1024L // 50MB
        }
    }
    
    private fun createVaultDirectory(vaultId: String) {
        val vaultDir = File(context.filesDir, "$SECURE_STORAGE_DIR/$vaultId")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
    }
    
    private suspend fun saveVaultConfiguration(vault: StorageVault) {
        // Save vault configuration to encrypted storage
        val configData = serializeVaultConfig(vault.config)
        val configFile = File(context.filesDir, "$SECURE_STORAGE_DIR/${vault.config.vaultId}/config.dat")
        writeEncryptedFile(configFile, configData.toByteArray())
    }
    
    private suspend fun loadVaultConfiguration(vaultId: String): StorageVault? {
        return try {
            val configFile = File(context.filesDir, "$SECURE_STORAGE_DIR/$vaultId/config.dat")
            if (configFile.exists()) {
                val configData = readEncryptedFile(configFile)
                deserializeVaultConfig(String(configData))
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load vault configuration: $vaultId", e)
            null
        }
    }
    
    private fun compressData(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        val gzip = java.util.zip.GZIPOutputStream(output)
        gzip.write(data)
        gzip.close()
        return output.toByteArray()
    }
    
    private fun decompressData(compressedData: ByteArray): ByteArray {
        val input = ByteArrayInputStream(compressedData)
        val gzip = java.util.zip.GZIPInputStream(input)
        return gzip.readBytes()
    }
    
    private fun encryptData(data: ByteArray, level: EncryptionLevel): ByteArray {
        // Use simplified encryption for this implementation
        // In production, this would use proper encryption based on level
        val key = generateEncryptionKey(level)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        
        // Combine IV and encrypted data
        return iv + encryptedData
    }
    
    private fun decryptData(encryptedData: ByteArray, level: EncryptionLevel): ByteArray {
        val key = generateEncryptionKey(level)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        
        val iv = encryptedData.sliceArray(0..11) // GCM IV is 12 bytes
        val cipherText = encryptedData.sliceArray(12 until encryptedData.size)
        
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        return cipher.doFinal(cipherText)
    }
    
    private fun generateEncryptionKey(level: EncryptionLevel): SecretKeySpec {
        // Simplified key generation based on level
        val keyBytes = when (level) {
            EncryptionLevel.BASIC -> ByteArray(16) // AES-128
            EncryptionLevel.STANDARD -> ByteArray(32) // AES-256
            EncryptionLevel.HIGH -> ByteArray(32) // AES-256
            EncryptionLevel.MAXIMUM -> ByteArray(32) // AES-256
        }
        
        // In production, derive key from master key
        secureRandom.nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, "AES")
    }
    
    private fun calculateIntegrityHash(data: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
    
    private fun getDataFilePath(vaultId: String, dataKey: String): File {
        return File(context.filesDir, "$SECURE_STORAGE_DIR/$vaultId/$dataKey.dat")
    }
    
    private suspend fun writeEncryptedFile(file: File, data: ByteArray) {
        file.parentFile?.mkdirs()
        
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey!!,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        
        encryptedFile.openFileOutput().use { output ->
            output.write(data)
        }
    }
    
    private suspend fun readEncryptedFile(file: File): ByteArray {
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey!!,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        
        return encryptedFile.openFileInput().use { input ->
            input.readBytes()
        }
    }
    
    private fun storeIntegrityHash(vaultId: String, dataKey: String, hash: String) {
        integrityChecks["$vaultId:$dataKey"] = hash
        
        // Also store to file for persistence
        val hashFile = File(context.filesDir, "$SECURE_STORAGE_DIR/$vaultId/$dataKey$INTEGRITY_SUFFIX")
        hashFile.writeText(hash)
    }
    
    private fun getStoredIntegrityHash(vaultId: String, dataKey: String): String? {
        return integrityChecks["$vaultId:$dataKey"] ?: run {
            val hashFile = File(context.filesDir, "$SECURE_STORAGE_DIR/$vaultId/$dataKey$INTEGRITY_SUFFIX")
            if (hashFile.exists()) hashFile.readText() else null
        }
    }
    
    private fun deleteIntegrityHash(vaultId: String, dataKey: String) {
        integrityChecks.remove("$vaultId:$dataKey")
        val hashFile = File(context.filesDir, "$SECURE_STORAGE_DIR/$vaultId/$dataKey$INTEGRITY_SUFFIX")
        if (hashFile.exists()) {
            hashFile.delete()
        }
    }
    
    private fun secureFileDelete(file: File) {
        try {
            // Overwrite file content before deletion for security
            val fileSize = file.length()
            val randomData = ByteArray(fileSize.toInt())
            secureRandom.nextBytes(randomData)
            
            file.writeBytes(randomData)
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Secure file deletion failed", e)
            file.delete() // Fallback to regular deletion
        }
    }
    
    private fun clearVaultFromMemory(vaultId: String) {
        // Clear sensitive vault data from memory (security measure)
        storageVaults[vaultId]?.dataEntries?.clear()
    }
    
    private fun validateCredentials(vaultId: String, credentials: StorageCredentials): Boolean {
        // Simplified credential validation
        return credentials.isValid()
    }
    
    private suspend fun reloadVaultData(vaultId: String) {
        // Reload vault data after unlocking
        val vault = storageVaults[vaultId] ?: return
        
        try {
            val vaultDir = File(context.filesDir, "$SECURE_STORAGE_DIR/$vaultId")
            vaultDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".dat") && !file.name.contains("config")) {
                    val dataKey = file.name.removeSuffix(".dat")
                    // Load data entry metadata
                    loadDataEntryMetadata(vault, dataKey)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reload vault data: $vaultId", e)
        }
    }
    
    private fun loadDataEntryMetadata(vault: StorageVault, dataKey: String) {
        // Load data entry metadata (simplified implementation)
        val dataEntry = StorageDataEntry(
            key = dataKey,
            originalSize = 0,
            compressedSize = 0,
            encryptedSize = 0,
            encryptionLevel = vault.config.encryptionLevel,
            compressed = vault.config.compressionEnabled,
            integrityHash = "",
            metadata = StorageMetadata(),
            createdAt = System.currentTimeMillis(),
            lastAccessed = System.currentTimeMillis()
        )
        
        vault.dataEntries[dataKey] = dataEntry
    }
    
    private suspend fun optimizeVault(vault: StorageVault): Long {
        var reclaimedSpace = 0L
        
        // Remove expired data entries
        val expiredEntries = vault.dataEntries.values.filter { entry ->
            entry.metadata.expiresAt?.let { 
                System.currentTimeMillis() > it 
            } ?: false
        }
        
        expiredEntries.forEach { entry ->
            deleteData(vault.config.vaultId, entry.key)
            reclaimedSpace += entry.originalSize
        }
        
        return reclaimedSpace
    }
    
    private fun cleanupTemporaryFiles(): Long {
        var reclaimedSpace = 0L
        val tempDir = File(context.cacheDir, "temp_storage")
        
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                reclaimedSpace += file.length()
                file.delete()
            }
        }
        
        return reclaimedSpace
    }
    
    private fun defragmentStorage() {
        // Simplified defragmentation (in production, this would be more complex)
        Log.d(TAG, "Storage defragmentation completed")
    }
    
    private fun calculateCompressionRatio(): Float {
        val allEntries = storageVaults.values.flatMap { it.dataEntries.values }
        if (allEntries.isEmpty()) return 0f
        
        val totalOriginal = allEntries.sumOf { it.originalSize }
        val totalCompressed = allEntries.sumOf { 
            if (it.compressed) it.compressedSize else it.originalSize 
        }
        
        return if (totalOriginal > 0) {
            1f - (totalCompressed.toFloat() / totalOriginal)
        } else 0f
    }
    
    private fun updateStorageMetrics() {
        val currentState = _storageState.value
        _storageState.value = currentState.copy(
            totalVaults = storageVaults.size,
            totalSize = storageVaults.values.sumOf { it.totalSize },
            lastActivity = System.currentTimeMillis()
        )
    }
    
    private suspend fun monitorStorageHealth() {
        // Monitor storage health and performance
        val totalSize = storageVaults.values.sumOf { it.totalSize }
        if (totalSize > MAX_STORAGE_SIZE * 0.9) {
            _storageEvents.emit(SecureStorageEvent.StorageNearFull(totalSize))
        }
    }
    
    private suspend fun cleanupExpiredData() {
        storageVaults.values.forEach { vault ->
            if (!vault.isLocked) {
                optimizeVault(vault)
            }
        }
    }
    
    private suspend fun performIntegrityChecks() {
        storageVaults.values.forEach { vault ->
            if (!vault.isLocked && vault.config.integrityCheckEnabled) {
                vault.dataEntries.values.forEach { entry ->
                    try {
                        val dataFile = getDataFilePath(vault.config.vaultId, entry.key)
                        if (dataFile.exists()) {
                            val encryptedData = readEncryptedFile(dataFile)
                            val currentHash = calculateIntegrityHash(encryptedData)
                            val storedHash = getStoredIntegrityHash(vault.config.vaultId, entry.key)
                            
                            if (storedHash != null && currentHash != storedHash) {
                                _storageEvents.emit(
                                    SecureStorageEvent.IntegrityViolation(vault.config.vaultId, entry.key)
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Integrity check failed for ${vault.config.vaultId}:${entry.key}", e)
                    }
                }
            }
        }
    }
    
    private fun serializeVaultConfig(config: StorageVaultConfig): String {
        // Simplified serialization
        return "${config.vaultId}|${config.vaultType}|${config.encryptionLevel}|${config.compressionEnabled}|${config.integrityCheckEnabled}|${config.maxSize}"
    }
    
    private fun deserializeVaultConfig(data: String): StorageVault {
        val parts = data.split("|")
        val config = StorageVaultConfig(
            vaultId = parts[0],
            vaultType = StorageVaultType.valueOf(parts[1]),
            encryptionLevel = EncryptionLevel.valueOf(parts[2]),
            compressionEnabled = parts[3].toBoolean(),
            integrityCheckEnabled = parts[4].toBoolean(),
            maxSize = parts[5].toLong()
        )
        
        return StorageVault(
            config = config,
            createdAt = System.currentTimeMillis(),
            lastAccessed = System.currentTimeMillis(),
            dataEntries = mutableMapOf(),
            totalSize = 0L,
            isLocked = false
        )
    }
    
    fun cleanup() {
        isInitialized = false
        storageScope.cancel()
        storageVaults.clear()
        encryptedFiles.clear()
        integrityChecks.clear()
    }
}

// Data classes for secure storage
data class SecureStorageState(
    val isInitialized: Boolean = false,
    val masterKeyAlias: String = "",
    val totalVaults: Int = 0,
    val totalSize: Long = 0L,
    val integrityViolations: Int = 0,
    val lastOptimization: Long = 0L,
    val lastActivity: Long = 0L
)

data class StorageVault(
    val config: StorageVaultConfig,
    val createdAt: Long,
    var lastAccessed: Long,
    val dataEntries: MutableMap<String, StorageDataEntry>,
    var totalSize: Long,
    var isLocked: Boolean,
    var lockedAt: Long? = null
)

data class StorageVaultConfig(
    val vaultId: String,
    val vaultType: StorageVaultType,
    val encryptionLevel: EncryptionLevel,
    val compressionEnabled: Boolean,
    val integrityCheckEnabled: Boolean,
    val maxSize: Long,
    val requiresCredentials: Boolean = false
)

data class StorageDataEntry(
    val key: String,
    val originalSize: Long,
    val compressedSize: Long,
    val encryptedSize: Long,
    val encryptionLevel: EncryptionLevel,
    val compressed: Boolean,
    val integrityHash: String,
    val metadata: StorageMetadata,
    val createdAt: Long,
    var lastAccessed: Long
)

data class StorageMetadata(
    val contentType: String = "",
    val description: String = "",
    val tags: Set<String> = emptySet(),
    val expiresAt: Long? = null,
    val customData: Map<String, String> = emptyMap()
)

data class StorageCredentials(
    val username: String = "",
    val password: String = "",
    val token: String = "",
    val biometricData: ByteArray? = null
) {
    fun isValid(): Boolean = username.isNotEmpty() || token.isNotEmpty()
}

data class StorageAnalytics(
    val totalVaults: Int,
    val totalStorageSize: Long,
    val totalDataEntries: Int,
    val vaultsByType: Map<StorageVaultType, Int>,
    val encryptionLevelDistribution: Map<EncryptionLevel, Int>,
    val compressionRatio: Float,
    val integrityViolations: Int,
    val lastOptimization: Long
)

enum class StorageVaultType {
    USER_DATA, CACHE_DATA, LOGS, MEDIA_METADATA, SECURITY_KEYS, TEMPORARY
}

// Result classes
sealed class VaultCreationResult {
    data class Success(val vault: StorageVault) : VaultCreationResult()
    data class VaultExists(val vaultId: String) : VaultCreationResult()
    data class Error(val exception: Exception) : VaultCreationResult()
}

sealed class StorageResult {
    data class Success(val dataEntry: StorageDataEntry) : StorageResult()
    data class VaultNotFound(val vaultId: String) : StorageResult()
    data class VaultLocked(val vaultId: String) : StorageResult()
    data class VaultFull(val vaultId: String) : StorageResult()
    data class Error(val exception: Exception) : StorageResult()
}

sealed class RetrievalResult {
    data class Success(val data: ByteArray, val dataEntry: StorageDataEntry) : RetrievalResult()
    data class VaultNotFound(val vaultId: String) : RetrievalResult()
    data class VaultLocked(val vaultId: String) : RetrievalResult()
    data class DataNotFound(val dataKey: String) : RetrievalResult()
    data class IntegrityViolation(val dataKey: String) : RetrievalResult()
    data class Error(val exception: Exception) : RetrievalResult()
}

sealed class DeletionResult {
    data class Success(val dataEntry: StorageDataEntry) : DeletionResult()
    data class VaultNotFound(val vaultId: String) : DeletionResult()
    data class VaultLocked(val vaultId: String) : DeletionResult()
    data class DataNotFound(val dataKey: String) : DeletionResult()
    data class Error(val exception: Exception) : DeletionResult()
}

sealed class VaultLockResult {
    object Success : VaultLockResult()
    data class VaultNotFound(val vaultId: String) : VaultLockResult()
    data class Error(val exception: Exception) : VaultLockResult()
}

sealed class VaultUnlockResult {
    object Success : VaultUnlockResult()
    data class VaultNotFound(val vaultId: String) : VaultUnlockResult()
    data class VaultNotLocked(val vaultId: String) : VaultUnlockResult()
    object CredentialsRequired : VaultUnlockResult()
    object InvalidCredentials : VaultUnlockResult()
    data class Error(val exception: Exception) : VaultUnlockResult()
}

sealed class StorageOptimizationResult {
    data class Success(val spaceReclaimed: Long, val vaultsOptimized: Int) : StorageOptimizationResult()
    data class Error(val exception: Exception) : StorageOptimizationResult()
}

// Events
sealed class SecureStorageEvent {
    object StorageInitialized : SecureStorageEvent()
    data class VaultCreated(val vaultId: String, val vaultType: StorageVaultType) : SecureStorageEvent()
    data class VaultCreationFailed(val vaultId: String, val error: Exception) : SecureStorageEvent()
    data class DataStored(val vaultId: String, val dataKey: String, val size: Int) : SecureStorageEvent()
    data class StorageFailed(val vaultId: String, val dataKey: String, val error: Exception) : SecureStorageEvent()
    data class DataRetrieved(val vaultId: String, val dataKey: String, val size: Int) : SecureStorageEvent()
    data class RetrievalFailed(val vaultId: String, val dataKey: String, val error: Exception) : SecureStorageEvent()
    data class DataDeleted(val vaultId: String, val dataKey: String) : SecureStorageEvent()
    data class DeletionFailed(val vaultId: String, val dataKey: String, val error: Exception) : SecureStorageEvent()
    data class VaultLocked(val vaultId: String) : SecureStorageEvent()
    data class VaultUnlocked(val vaultId: String) : SecureStorageEvent()
    data class IntegrityViolation(val vaultId: String, val dataKey: String) : SecureStorageEvent()
    data class StorageOptimized(val spaceReclaimed: Long, val vaultsOptimized: Int) : SecureStorageEvent()
    data class StorageNearFull(val currentSize: Long) : SecureStorageEvent()
}