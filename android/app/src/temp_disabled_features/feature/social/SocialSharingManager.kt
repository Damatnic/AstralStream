package com.astralplayer.nextplayer.feature.social

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SocialPlatform(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val isEnabled: Boolean = true
)

data class ShareContent(
    val videoUri: Uri,
    val title: String,
    val description: String = "",
    val thumbnailUri: Uri? = null,
    val duration: Long = 0L
)

data class SocialPost(
    val id: String,
    val platform: SocialPlatform,
    val content: ShareContent,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: PostStatus = PostStatus.PENDING
)

enum class PostStatus {
    PENDING, POSTED, FAILED
}

class SocialSharingManager(private val context: Context) {
    
    private val _availablePlatforms = MutableStateFlow<List<SocialPlatform>>(emptyList())
    val availablePlatforms: StateFlow<List<SocialPlatform>> = _availablePlatforms.asStateFlow()
    
    private val _shareHistory = MutableStateFlow<List<SocialPost>>(emptyList())
    val shareHistory: StateFlow<List<SocialPost>> = _shareHistory.asStateFlow()
    
    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()
    
    private val _shareError = MutableStateFlow<String?>(null)
    val shareError: StateFlow<String?> = _shareError.asStateFlow()

    init {
        loadSocialPlatforms()
    }

    private fun loadSocialPlatforms() {
        val platforms = listOf(
            SocialPlatform("twitter", "Twitter", Icons.Default.Share, Color(0xFF1DA1F2)),
            SocialPlatform("facebook", "Facebook", Icons.Default.Facebook, Color(0xFF4267B2)),
            SocialPlatform("instagram", "Instagram", Icons.Default.PhotoCamera, Color(0xFFE4405F)),
            SocialPlatform("youtube", "YouTube", Icons.Default.VideoLibrary, Color(0xFFFF0000)),
            SocialPlatform("tiktok", "TikTok", Icons.Default.MusicNote, Color(0xFF000000)),
            SocialPlatform("whatsapp", "WhatsApp", Icons.Default.Message, Color(0xFF25D366)),
            SocialPlatform("telegram", "Telegram", Icons.Default.Send, Color(0xFF0088CC)),
            SocialPlatform("email", "Email", Icons.Default.Email, Color(0xFF34495E))
        )
        _availablePlatforms.value = platforms
    }

    fun shareVideo(content: ShareContent, platform: SocialPlatform, message: String = "") {
        _isSharing.value = true
        
        try {
            val shareIntent = createShareIntent(content, platform, message)
            context.startActivity(Intent.createChooser(shareIntent, "Share via ${platform.name}"))
            
            val post = SocialPost(
                id = "post_${System.currentTimeMillis()}",
                platform = platform,
                content = content,
                message = message,
                status = PostStatus.POSTED
            )
            
            _shareHistory.value = listOf(post) + _shareHistory.value
            
        } catch (e: Exception) {
            _shareError.value = "Failed to share to ${platform.name}: ${e.message}"
        } finally {
            _isSharing.value = false
        }
    }

    private fun createShareIntent(content: ShareContent, platform: SocialPlatform, message: String): Intent {
        return when (platform.id) {
            "email" -> createEmailIntent(content, message)
            "whatsapp" -> createWhatsAppIntent(content, message)
            else -> createGenericShareIntent(content, message)
        }
    }

    private fun createEmailIntent(content: ShareContent, message: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_SUBJECT, content.title)
            putExtra(Intent.EXTRA_TEXT, "$message\n\n${content.description}")
            putExtra(Intent.EXTRA_STREAM, content.videoUri)
        }
    }

    private fun createWhatsAppIntent(content: ShareContent, message: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, "$message\n${content.title}")
            putExtra(Intent.EXTRA_STREAM, content.videoUri)
        }
    }

    private fun createGenericShareIntent(content: ShareContent, message: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_TEXT, "$message\n${content.title}\n${content.description}")
            putExtra(Intent.EXTRA_STREAM, content.videoUri)
        }
    }

    fun clearError() {
        _shareError.value = null
    }
}

class SocialSharingViewModel(private val socialManager: SocialSharingManager) : ViewModel() {
    
    val availablePlatforms = socialManager.availablePlatforms
    val shareHistory = socialManager.shareHistory
    val isSharing = socialManager.isSharing
    val shareError = socialManager.shareError

    fun shareVideo(content: ShareContent, platform: SocialPlatform, message: String = "") {
        viewModelScope.launch {
            socialManager.shareVideo(content, platform, message)
        }
    }

    fun clearError() {
        socialManager.clearError()
    }
}

@Composable
fun SocialSharingScreen(
    platforms: List<SocialPlatform>,
    shareHistory: List<SocialPost>,
    isSharing: Boolean,
    shareError: String?,
    onShareVideo: (ShareContent, SocialPlatform, String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Social Sharing",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        shareError?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(error, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClearError) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(
            text = "Available Platforms",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(platforms) { platform ->
                SocialPlatformCard(
                    platform = platform,
                    onClick = {
                        // Mock share content for demo
                        val mockContent = ShareContent(
                            videoUri = Uri.parse("content://mock/video"),
                            title = "Amazing Video",
                            description = "Check out this amazing video!"
                        )
                        onShareVideo(mockContent, platform, "Sharing from Astral Player!")
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (shareHistory.isNotEmpty()) {
            Text(
                text = "Share History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shareHistory.take(10)) { post ->
                    ShareHistoryItem(post = post)
                }
            }
        }
    }
}

@Composable
fun SocialPlatformCard(
    platform: SocialPlatform,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = platform.color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = platform.icon,
                contentDescription = platform.name,
                tint = platform.color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = platform.name,
                style = MaterialTheme.typography.labelSmall,
                color = platform.color
            )
        }
    }
}

@Composable
fun ShareHistoryItem(post: SocialPost) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = post.platform.icon,
                contentDescription = null,
                tint = post.platform.color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.content.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Shared to ${post.platform.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = when (post.status) {
                    PostStatus.POSTED -> Icons.Default.CheckCircle
                    PostStatus.FAILED -> Icons.Default.Error
                    PostStatus.PENDING -> Icons.Default.Schedule
                },
                contentDescription = null,
                tint = when (post.status) {
                    PostStatus.POSTED -> Color.Green
                    PostStatus.FAILED -> MaterialTheme.colorScheme.error
                    PostStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}