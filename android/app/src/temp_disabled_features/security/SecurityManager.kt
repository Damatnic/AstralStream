package com.astralplayer.nextplayer.security

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import okhttp3.CertificatePinner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    
    private val encryptedSharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    enum class CloudProvider { GOOGLE_DRIVE, DROPBOX }
    
    fun storeCloudToken(provider: CloudProvider, token: String) {
        encryptedSharedPreferences.edit()
            .putString("${provider.name}_token", token)
            .apply()
    }
    
    fun getCloudToken(provider: CloudProvider): String? {
        return encryptedSharedPreferences.getString("${provider.name}_token", null)
    }
    
    fun getDropboxToken(): String? = getCloudToken(CloudProvider.DROPBOX)
    fun getGoogleDriveToken(): String? = getCloudToken(CloudProvider.GOOGLE_DRIVE)
    
    fun getCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("*.googleapis.com", "sha256/WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=")
            .add("*.dropboxapi.com", "sha256/5kJvNEMw0KjrCAu7eXY5HZdvyCS13BbA0VJG1RSP91w=")
            .build()
    }
    
    fun validateVideoUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            when (uri.scheme?.lowercase()) {
                "http", "https" -> isUrlSafe(url)
                "file", "content" -> true
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isUrlSafe(url: String): Boolean {
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: return false
        
        // Check against whitelist of known video sites
        val safeHosts = listOf(
            "youtube.com", "youtu.be", "vimeo.com", "dailymotion.com",
            "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com",
            "redtube.com", "youporn.com", "spankbang.com"
        )
        
        return safeHosts.any { safeHost ->
            host == safeHost || host.endsWith(".$safeHost")
        }
    }
    
    fun clearAllTokens() {
        encryptedSharedPreferences.edit().clear().apply()
    }
    
    fun hasValidToken(provider: CloudProvider): Boolean {
        return getCloudToken(provider)?.isNotEmpty() == true
    }
}