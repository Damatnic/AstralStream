// DefaultPlayerManager.kt
package com.astralplayer.core.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPlayerManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        const val REQUEST_DEFAULT_VIDEO_PLAYER = 1001
        const val REQUEST_DEFAULT_AUDIO_PLAYER = 1002
    }
    
    fun isDefaultVideoPlayer(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                roleManager.isRoleHeld(RoleManager.ROLE_BROWSER) // Check if we handle video URLs
            } else {
                // For older versions, check if our app is preferred for video intents
                isPreferredVideoHandler()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check default video player status")
            false
        }
    }
    
    private fun isPreferredVideoHandler(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(android.net.Uri.parse("http://example.com/video.mp4"), "video/mp4")
            }
            
            val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            Timber.e(e, "Failed to check preferred video handler")
            false
        }
    }
    
    fun requestDefaultVideoPlayer(): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                
                // Request to be the default handler for browser/video activities
                if (roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                } else {
                    // Fallback to app settings
                    createAppSettingsIntent()
                }
            } else {
                // For older versions, show app selection settings
                createAppSettingsIntent()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create default player request intent")
            createAppSettingsIntent()
        }
    }
    
    private fun createAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    fun showAppSelectionDialog(): Intent {
        // Create an intent that will force the "Open with" dialog
        return Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("http://example.com/test.mp4")
            type = "video/mp4"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // This will force the chooser dialog
            putExtra(Intent.EXTRA_CHOOSER_TARGETS, arrayOf<Intent>())
        }
    }
    
    fun createChooserIntent(originalIntent: Intent, title: String = "Open video with"): Intent {
        return Intent.createChooser(originalIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    fun registerForVideoMimeTypes() {
        try {
            // This method would programmatically register our app for video MIME types
            // Note: This requires system-level permissions and is mainly for reference
            Timber.d("Video MIME type registration would be handled by the manifest intent filters")
        } catch (e: Exception) {
            Timber.e(e, "Failed to register for video MIME types")
        }
    }
    
    fun checkVideoAppCompatibility(): VideoAppCompatibility {
        val videoSupport = checkVideoSupport()
        val browserIntegration = checkBrowserIntegration()
        val systemIntegration = checkSystemIntegration()
        
        return VideoAppCompatibility(
            hasVideoSupport = videoSupport,
            hasBrowserIntegration = browserIntegration,
            hasSystemIntegration = systemIntegration,
            isDefaultPlayer = isDefaultVideoPlayer(),
            androidVersion = Build.VERSION.SDK_INT
        )
    }
    
    private fun checkVideoSupport(): Boolean {
        return try {
            // Check if our app can handle various video formats
            val testIntents = listOf(
                createVideoIntent("video/mp4"),
                createVideoIntent("video/webm"),
                createVideoIntent("application/x-mpegURL"),
                createVideoIntent("application/dash+xml")
            )
            
            testIntents.all { intent ->
                context.packageManager.resolveActivity(intent, 0) != null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check video support")
            false
        }
    }
    
    private fun checkBrowserIntegration(): Boolean {
        return try {
            // Check if browsers can launch our app
            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://example.com/video.mp4")
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            
            val resolveInfos = context.packageManager.queryIntentActivities(browserIntent, 0)
            resolveInfos.any { it.activityInfo.packageName == context.packageName }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check browser integration")
            false
        }
    }
    
    private fun checkSystemIntegration(): Boolean {
        return try {
            // Check various system integration points
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://example.com/video.mp4")
            }
            
            val resolveInfos = context.packageManager.queryIntentActivities(shareIntent, 0)
            resolveInfos.any { it.activityInfo.packageName == context.packageName }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check system integration")
            false
        }
    }
    
    private fun createVideoIntent(mimeType: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(android.net.Uri.parse("content://example/video"), mimeType)
        }
    }
    
    data class VideoAppCompatibility(
        val hasVideoSupport: Boolean,
        val hasBrowserIntegration: Boolean,
        val hasSystemIntegration: Boolean,
        val isDefaultPlayer: Boolean,
        val androidVersion: Int
    ) {
        fun getCompatibilityScore(): Float {
            var score = 0f
            if (hasVideoSupport) score += 0.3f
            if (hasBrowserIntegration) score += 0.3f
            if (hasSystemIntegration) score += 0.2f
            if (isDefaultPlayer) score += 0.2f
            return score
        }
        
        fun getRecommendations(): List<String> {
            val recommendations = mutableListOf<String>()
            
            if (!hasVideoSupport) {
                recommendations.add("Video format support needs improvement")
            }
            if (!hasBrowserIntegration) {
                recommendations.add("Browser integration not working properly")
            }
            if (!hasSystemIntegration) {
                recommendations.add("System integration needs configuration")
            }
            if (!isDefaultPlayer) {
                recommendations.add("Set as default video player for better integration")
            }
            
            return recommendations
        }
    }
}