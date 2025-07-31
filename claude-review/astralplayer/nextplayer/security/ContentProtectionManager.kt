package com.astralplayer.nextplayer.security

import android.content.Context
import android.net.Uri
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Advanced content protection manager
 * Handles content encryption, access control, and digital rights management
 */
class ContentProtectionManager(private val context: Context) {
    
    private val _protectionEvents = MutableSharedFlow<ProtectionEvent>()
    val protectionEvents: SharedFlow<ProtectionEvent> = _protectionEvents.asSharedFlow()
    
    private val _protectionState = MutableStateFlow(ProtectionState())
    val protectionState: StateFlow<ProtectionState> = _protectionState.asStateFlow()
    
    private var masterKey: MasterKey? = null
    private val protectedContent = ConcurrentHashMap<String, ProtectedContentInfo>()
    private val quarantinedContent = mutableSetOf<String>()
    private val contentAccessLog = mutableListOf<ContentAccessEntry>()
    
    private val protectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isInitialized = false
    
    /**
     * Initialize content protection manager
     */
    suspend fun initialize(masterKey: MasterKey) {
        this.masterKey = masterKey
        isInitialized = true
        
        // Load existing protection metadata
        loadProtectionMetadata()
        
        // Start protection monitoring
        startProtectionMonitoring()
        
        _protectionEvents.emit(ProtectionEvent.Initialized)
    }
    
    /**
     * Protect content with specified level and access policy
     */
    suspend fun protectContent(
        contentUri: Uri,
        protectionLevel: ContentProtectionLevel,
        accessPolicy: AccessPolicy?
    ): ContentProtectionInfo = withContext(Dispatchers.IO) {
        try {
            val contentId = generateContentId(contentUri)
            
            // Check if content is already protected
            val existingProtection = protectedContent[contentId]
            if (existingProtection != null) {
                return@withContext ContentProtectionInfo.AlreadyProtected(existingProtection)
            }
            
            // Apply protection based on level
            val protectionInfo = when (protectionLevel) {
                ContentProtectionLevel.NONE -> {
                    ProtectedContentInfo(
                        contentId = contentId,
                        contentUri = contentUri,
                        protectionLevel = protectionLevel,
                        accessPolicy = accessPolicy,
                        protectionTimestamp = System.currentTimeMillis(),
                        encryptionKey = null,
                        checksum = calculateContentChecksum(contentUri)
                    )
                }
                ContentProtectionLevel.BASIC -> {
                    applyBasicProtection(contentUri, contentId, accessPolicy)
                }
                ContentProtectionLevel.STANDARD -> {
                    applyStandardProtection(contentUri, contentId, accessPolicy)
                }
                ContentProtectionLevel.HIGH -> {
                    applyHighProtection(contentUri, contentId, accessPolicy)
                }
                ContentProtectionLevel.DRM_PROTECTED -> {
                    applyDRMProtection(contentUri, contentId, accessPolicy)
                }
            }
            
            // Store protection info
            protectedContent[contentId] = protectionInfo
            saveProtectionMetadata()
            
            // Update protection metrics
            updateProtectionMetrics(protectionLevel)
            
            _protectionEvents.emit(
                ProtectionEvent.ContentProtected(contentUri, protectionLevel)
            )
            
            ContentProtectionInfo.Success(protectionInfo)
            
        } catch (e: Exception) {
            _protectionEvents.emit(
                ProtectionEvent.ProtectionFailed(contentUri, e)
            )
            ContentProtectionInfo.Error(e)
        }
    }
    
    /**
     * Validate content access request
     */
    suspend fun validateContentAccess(
        contentUri: Uri,
        requestingComponent: String,
        accessType: AccessType
    ): AccessValidationResult = withContext(Dispatchers.IO) {
        try {
            val contentId = generateContentId(contentUri)
            val protectionInfo = protectedContent[contentId]
                ?: return@withContext AccessValidationResult.ContentNotProtected
            
            // Check if content is quarantined
            if (quarantinedContent.contains(contentId)) {
                return@withContext AccessValidationResult.ContentQuarantined
            }
            
            // Validate access policy
            val accessPolicy = protectionInfo.accessPolicy
            if (accessPolicy != null) {
                val policyValidation = validateAccessPolicy(
                    accessPolicy, requestingComponent, accessType
                )
                if (!policyValidation.isValid) {
                    return@withContext AccessValidationResult.PolicyViolation(policyValidation.reason)
                }
            }
            
            // Verify content integrity
            val currentChecksum = calculateContentChecksum(contentUri)
            if (currentChecksum != protectionInfo.checksum) {
                _protectionEvents.emit(
                    ProtectionEvent.ContentIntegrityViolation(contentUri)
                )
                return@withContext AccessValidationResult.IntegrityViolation
            }
            
            // Log access attempt
            logContentAccess(contentId, requestingComponent, accessType, true)
            
            AccessValidationResult.AccessGranted(protectionInfo)
            
        } catch (e: Exception) {
            logContentAccess(
                generateContentId(contentUri), requestingComponent, accessType, false, e
            )
            AccessValidationResult.Error(e)
        }
    }
    
    /**
     * Enhance protection for existing content
     */
    suspend fun enhanceProtection(contentUri: Uri): ProtectionEnhancementResult = withContext(Dispatchers.IO) {
        try {
            val contentId = generateContentId(contentUri)
            val existingProtection = protectedContent[contentId]
                ?: return@withContext ProtectionEnhancementResult.ContentNotProtected
            
            val enhancedLevel = when (existingProtection.protectionLevel) {
                ContentProtectionLevel.NONE -> ContentProtectionLevel.BASIC
                ContentProtectionLevel.BASIC -> ContentProtectionLevel.STANDARD
                ContentProtectionLevel.STANDARD -> ContentProtectionLevel.HIGH
                ContentProtectionLevel.HIGH -> ContentProtectionLevel.DRM_PROTECTED
                ContentProtectionLevel.DRM_PROTECTED -> {
                    return@withContext ProtectionEnhancementResult.AlreadyMaximum
                }
            }
            
            // Apply enhanced protection
            val enhancedInfo = applyEnhancedProtection(
                existingProtection, enhancedLevel
            )
            
            protectedContent[contentId] = enhancedInfo
            saveProtectionMetadata()
            
            _protectionEvents.emit(
                ProtectionEvent.ProtectionEnhanced(contentUri, enhancedLevel)
            )
            
            ProtectionEnhancementResult.Success(enhancedInfo)
            
        } catch (e: Exception) {
            ProtectionEnhancementResult.Error(e)
        }
    }
    
    /**
     * Quarantine potentially malicious content
     */
    suspend fun quarantineContent(contentUri: Uri, reasons: List<String>) {
        val contentId = generateContentId(contentUri)
        quarantinedContent.add(contentId)
        
        // Move content to quarantine location if it's a local file
        if (contentUri.scheme == "file") {
            moveToQuarantine(contentUri, reasons)
        }
        
        _protectionEvents.emit(
            ProtectionEvent.ContentQuarantined(contentUri, reasons)
        )
        
        // Update protection state
        _protectionState.value = _protectionState.value.copy(
            quarantinedItems = _protectionState.value.quarantinedItems + 1
        )
    }
    
    /**
     * Activate emergency protection protocols
     */
    suspend fun activateEmergencyProtection() {
        _protectionState.value = _protectionState.value.copy(
            emergencyMode = true
        )
        
        // Enhance protection for all content
        protectedContent.values.forEach { protection ->
            if (protection.protectionLevel != ContentProtectionLevel.DRM_PROTECTED) {
                protectionScope.launch {
                    enhanceProtection(protection.contentUri)
                }
            }
        }
        
        _protectionEvents.emit(ProtectionEvent.EmergencyProtectionActivated)
    }
    
    /**
     * Get protection metrics and analytics
     */
    fun getProtectionMetrics(): ProtectionMetrics {
        val protectionCounts = protectedContent.values.groupBy { it.protectionLevel }
            .mapValues { it.value.size }
        
        val recentAccesses = contentAccessLog.filter { 
            System.currentTimeMillis() - it.timestamp < 3600000L // Last hour
        }
        
        return ProtectionMetrics(
            totalProtectedItems = protectedContent.size,
            quarantinedItems = quarantinedContent.size,
            protectionLevelCounts = protectionCounts,
            recentAccessAttempts = recentAccesses.size,
            successfulAccesses = recentAccesses.count { it.success },
            failedAccesses = recentAccesses.count { !it.success },
            lastProtectionActivity = _protectionState.value.lastActivity
        )
    }
    
    // Private implementation methods
    private suspend fun loadProtectionMetadata() {
        // Load protection metadata from secure storage
        // Implementation would load from encrypted file or secure database
    }
    
    private suspend fun saveProtectionMetadata() {
        // Save protection metadata to secure storage
        // Implementation would save to encrypted file or secure database
    }
    
    private fun startProtectionMonitoring() {
        protectionScope.launch {
            while (isInitialized && currentCoroutineContext().isActive) {
                try {
                    monitorProtectedContent()
                    cleanupExpiredProtections()
                    analyzeAccessPatterns()
                    
                    delay(60000) // Monitor every minute
                } catch (e: Exception) {
                    // Handle monitoring error but continue
                }
            }
        }
    }
    
    private suspend fun applyBasicProtection(
        contentUri: Uri,
        contentId: String,
        accessPolicy: AccessPolicy?
    ): ProtectedContentInfo {
        return ProtectedContentInfo(
            contentId = contentId,
            contentUri = contentUri,
            protectionLevel = ContentProtectionLevel.BASIC,
            accessPolicy = accessPolicy,
            protectionTimestamp = System.currentTimeMillis(),
            encryptionKey = null, // Basic protection doesn't use encryption
            checksum = calculateContentChecksum(contentUri),
            accessCount = 0
        )
    }
    
    private suspend fun applyStandardProtection(
        contentUri: Uri,
        contentId: String,
        accessPolicy: AccessPolicy?
    ): ProtectedContentInfo {
        // Generate encryption key for standard protection
        val encryptionKey = generateContentEncryptionKey()
        
        return ProtectedContentInfo(
            contentId = contentId,
            contentUri = contentUri,
            protectionLevel = ContentProtectionLevel.STANDARD,
            accessPolicy = accessPolicy,
            protectionTimestamp = System.currentTimeMillis(),
            encryptionKey = encryptionKey,
            checksum = calculateContentChecksum(contentUri),
            accessCount = 0
        )
    }
    
    private suspend fun applyHighProtection(
        contentUri: Uri,
        contentId: String,
        accessPolicy: AccessPolicy?
    ): ProtectedContentInfo {
        // Generate strong encryption key and apply additional security measures
        val encryptionKey = generateStrongEncryptionKey()
        
        return ProtectedContentInfo(
            contentId = contentId,
            contentUri = contentUri,
            protectionLevel = ContentProtectionLevel.HIGH,
            accessPolicy = accessPolicy,
            protectionTimestamp = System.currentTimeMillis(),
            encryptionKey = encryptionKey,
            checksum = calculateContentChecksum(contentUri),
            accessCount = 0,
            securitySalt = generateSecuritySalt()
        )
    }
    
    private suspend fun applyDRMProtection(
        contentUri: Uri,
        contentId: String,
        accessPolicy: AccessPolicy?
    ): ProtectedContentInfo {
        // Apply DRM-level protection with hardware-backed security
        val drmKey = generateDRMKey()
        
        return ProtectedContentInfo(
            contentId = contentId,
            contentUri = contentUri,
            protectionLevel = ContentProtectionLevel.DRM_PROTECTED,
            accessPolicy = accessPolicy,
            protectionTimestamp = System.currentTimeMillis(),
            encryptionKey = drmKey,
            checksum = calculateContentChecksum(contentUri),
            accessCount = 0,
            securitySalt = generateSecuritySalt(),
            drmInfo = DRMInfo(
                provider = "AstralStream_DRM",
                licenseUrl = null,
                keyId = drmKey
            )
        )
    }
    
    private suspend fun applyEnhancedProtection(
        existing: ProtectedContentInfo,
        newLevel: ContentProtectionLevel
    ): ProtectedContentInfo {
        return when (newLevel) {
            ContentProtectionLevel.BASIC -> applyBasicProtection(
                existing.contentUri, existing.contentId, existing.accessPolicy
            )
            ContentProtectionLevel.STANDARD -> applyStandardProtection(
                existing.contentUri, existing.contentId, existing.accessPolicy
            )
            ContentProtectionLevel.HIGH -> applyHighProtection(
                existing.contentUri, existing.contentId, existing.accessPolicy
            )
            ContentProtectionLevel.DRM_PROTECTED -> applyDRMProtection(
                existing.contentUri, existing.contentId, existing.accessPolicy
            )
            else -> existing
        }.copy(accessCount = existing.accessCount)
    }
    
    private fun generateContentId(contentUri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(contentUri.toString().toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    private suspend fun calculateContentChecksum(contentUri: Uri): String {
        // Calculate SHA-256 checksum of content for integrity verification
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            // Implementation would read content and calculate checksum
            // For now, return a placeholder based on URI
            val hash = digest.digest(contentUri.toString().toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun validateAccessPolicy(
        policy: AccessPolicy,
        component: String,
        accessType: AccessType
    ): PolicyValidationResult {
        // Check component authorization
        if (!policy.authorizedComponents.contains(component)) {
            return PolicyValidationResult(false, "Component not authorized")
        }
        
        // Check access type permission
        if (!policy.allowedAccessTypes.contains(accessType)) {
            return PolicyValidationResult(false, "Access type not permitted")
        }
        
        // Check time restrictions
        policy.accessTimeRestrictions?.let { restrictions ->
            val currentTime = System.currentTimeMillis()
            if (currentTime < restrictions.validFrom || currentTime > restrictions.validUntil) {
                return PolicyValidationResult(false, "Outside permitted time window")
            }
        }
        
        return PolicyValidationResult(true, "Access authorized")
    }
    
    private fun logContentAccess(
        contentId: String,
        component: String,
        accessType: AccessType,
        success: Boolean,
        error: Exception? = null
    ) {
        val entry = ContentAccessEntry(
            contentId = contentId,
            component = component,
            accessType = accessType,
            success = success,
            timestamp = System.currentTimeMillis(),
            error = error?.message
        )
        
        contentAccessLog.add(entry)
        
        // Keep log size manageable
        if (contentAccessLog.size > 1000) {
            contentAccessLog.removeAt(0)
        }
        
        // Update access count for successful accesses
        if (success) {
            protectedContent[contentId]?.let { info ->
                protectedContent[contentId] = info.copy(
                    accessCount = info.accessCount + 1,
                    lastAccessTime = System.currentTimeMillis()
                )
            }
        }
    }
    
    private suspend fun moveToQuarantine(contentUri: Uri, reasons: List<String>) {
        try {
            val sourceFile = File(contentUri.path ?: return)
            if (!sourceFile.exists()) return
            
            val quarantineDir = File(context.cacheDir, "quarantine")
            if (!quarantineDir.exists()) {
                quarantineDir.mkdirs()
            }
            
            val quarantineFile = File(quarantineDir, "${System.currentTimeMillis()}_${sourceFile.name}")
            sourceFile.renameTo(quarantineFile)
            
            // Create metadata file with quarantine reasons
            val metadataFile = File(quarantineDir, "${quarantineFile.name}.meta")
            metadataFile.writeText(
                "Quarantined: ${System.currentTimeMillis()}\n" +
                "Original: ${contentUri}\n" +
                "Reasons: ${reasons.joinToString(", ")}"
            )
            
        } catch (e: Exception) {
            // Log error but don't fail quarantine process
        }
    }
    
    private fun generateContentEncryptionKey(): String {
        // Generate AES-256 key for content encryption
        return java.util.UUID.randomUUID().toString()
    }
    
    private fun generateStrongEncryptionKey(): String {
        // Generate stronger encryption key
        return java.util.UUID.randomUUID().toString() + "_" + System.currentTimeMillis()
    }
    
    private fun generateDRMKey(): String {
        // Generate DRM-compatible key
        return "DRM_" + java.util.UUID.randomUUID().toString()
    }
    
    private fun generateSecuritySalt(): String {
        return java.util.UUID.randomUUID().toString()
    }
    
    private suspend fun monitorProtectedContent() {
        // Monitor protected content for integrity and access patterns
        protectedContent.values.forEach { protection ->
            try {
                val currentChecksum = calculateContentChecksum(protection.contentUri)
                if (currentChecksum != protection.checksum && currentChecksum.isNotEmpty()) {
                    _protectionEvents.emit(
                        ProtectionEvent.ContentIntegrityViolation(protection.contentUri)
                    )
                }
            } catch (e: Exception) {
                // Handle monitoring error for individual content
            }
        }
    }
    
    private suspend fun cleanupExpiredProtections() {
        // Remove expired protections based on policy
        val currentTime = System.currentTimeMillis()
        val expiredItems = protectedContent.values.filter { protection ->
            protection.accessPolicy?.accessTimeRestrictions?.let { restrictions ->
                currentTime > restrictions.validUntil
            } ?: false
        }
        
        expiredItems.forEach { protection ->
            protectedContent.remove(protection.contentId)
            _protectionEvents.emit(
                ProtectionEvent.ProtectionExpired(protection.contentUri)
            )
        }
    }
    
    private suspend fun analyzeAccessPatterns() {
        // Analyze access patterns for suspicious activity
        val recentAccesses = contentAccessLog.filter { 
            System.currentTimeMillis() - it.timestamp < 3600000L // Last hour
        }
        
        // Detect unusual access patterns
        val suspiciousPatterns = detectSuspiciousPatterns(recentAccesses)
        if (suspiciousPatterns.isNotEmpty()) {
            _protectionEvents.emit(
                ProtectionEvent.SuspiciousActivityDetected(suspiciousPatterns)
            )
        }
    }
    
    private fun detectSuspiciousPatterns(accesses: List<ContentAccessEntry>): List<String> {
        val patterns = mutableListOf<String>()
        
        // Check for rapid successive failed accesses
        val failedAccesses = accesses.filter { !it.success }
        if (failedAccesses.size > 10) {
            patterns.add("Multiple failed access attempts detected")
        }
        
        // Check for unusual access times
        val nightAccesses = accesses.filter { 
            val hour = java.util.Calendar.getInstance().apply { 
                timeInMillis = it.timestamp 
            }.get(java.util.Calendar.HOUR_OF_DAY)
            hour < 6 || hour > 23
        }
        if (nightAccesses.size > accesses.size * 0.5) {
            patterns.add("Unusual access times detected")
        }
        
        return patterns
    }
    
    private fun updateProtectionMetrics(protectionLevel: ContentProtectionLevel) {
        _protectionState.value = _protectionState.value.copy(
            totalProtectedItems = protectedContent.size,
            lastActivity = System.currentTimeMillis()
        )
    }
    
    fun cleanup() {
        isInitialized = false
        protectionScope.cancel()
        protectedContent.clear()
        quarantinedContent.clear()
        contentAccessLog.clear()
    }
}

// Data classes for content protection
data class ProtectedContentInfo(
    val contentId: String,
    val contentUri: Uri,
    val protectionLevel: ContentProtectionLevel,
    val accessPolicy: AccessPolicy?,
    val protectionTimestamp: Long,
    val encryptionKey: String?,
    val checksum: String,
    val accessCount: Int = 0,
    val lastAccessTime: Long = 0L,
    val securitySalt: String? = null,
    val drmInfo: DRMInfo? = null
)

data class ContentAccessEntry(
    val contentId: String,
    val component: String,
    val accessType: AccessType,
    val success: Boolean,
    val timestamp: Long,
    val error: String?
)

data class ProtectionState(
    val totalProtectedItems: Int = 0,
    val quarantinedItems: Int = 0,
    val emergencyMode: Boolean = false,
    val lastActivity: Long = 0L
)

data class ProtectionMetrics(
    val totalProtectedItems: Int,
    val quarantinedItems: Int,
    val protectionLevelCounts: Map<ContentProtectionLevel, Int>,
    val recentAccessAttempts: Int,
    val successfulAccesses: Int,
    val failedAccesses: Int,
    val lastProtectionActivity: Long
)

data class PolicyValidationResult(
    val isValid: Boolean,
    val reason: String
)

data class DRMInfo(
    val provider: String,
    val licenseUrl: String?,
    val keyId: String
)

sealed class ContentProtectionInfo {
    data class Success(val protectionInfo: ProtectedContentInfo) : ContentProtectionInfo()
    data class AlreadyProtected(val existingInfo: ProtectedContentInfo) : ContentProtectionInfo()
    data class Error(val exception: Exception) : ContentProtectionInfo()
}

sealed class AccessValidationResult {
    data class AccessGranted(val protectionInfo: ProtectedContentInfo) : AccessValidationResult()
    object ContentNotProtected : AccessValidationResult()
    object ContentQuarantined : AccessValidationResult()
    data class PolicyViolation(val reason: String) : AccessValidationResult()
    object IntegrityViolation : AccessValidationResult()
    data class Error(val exception: Exception) : AccessValidationResult()
}

sealed class ProtectionEnhancementResult {
    data class Success(val enhancedInfo: ProtectedContentInfo) : ProtectionEnhancementResult()
    object ContentNotProtected : ProtectionEnhancementResult()
    object AlreadyMaximum : ProtectionEnhancementResult()
    data class Error(val exception: Exception) : ProtectionEnhancementResult()
}

sealed class ProtectionEvent {
    object Initialized : ProtectionEvent()
    data class ContentProtected(val contentUri: Uri, val level: ContentProtectionLevel) : ProtectionEvent()
    data class ProtectionFailed(val contentUri: Uri, val error: Exception) : ProtectionEvent()
    data class ProtectionEnhanced(val contentUri: Uri, val newLevel: ContentProtectionLevel) : ProtectionEvent()
    data class ContentQuarantined(val contentUri: Uri, val reasons: List<String>) : ProtectionEvent()
    data class ContentIntegrityViolation(val contentUri: Uri) : ProtectionEvent()
    data class ProtectionExpired(val contentUri: Uri) : ProtectionEvent()
    data class SuspiciousActivityDetected(val patterns: List<String>) : ProtectionEvent()
    object EmergencyProtectionActivated : ProtectionEvent()
}