package com.astralplayer.nextplayer.feature.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import com.astralplayer.nextplayer.feature.player.viewmodel.PlayerViewModel

data class MoreOption(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val isEnabled: Boolean = true,
    val action: () -> Unit
)

@Composable
fun MoreOptionsDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()
    
    val options = remember(playerState) {
        listOf(
            MoreOption(
                id = "loop",
                title = "Loop Video",
                subtitle = if (playerState.isLooping) "On" else "Off",
                icon = Icons.Default.Loop,
                action = { viewModel.toggleLoop() }
            ),
            MoreOption(
                id = "pip",
                title = "Picture in Picture",
                subtitle = "Watch in floating window",
                icon = Icons.Default.PictureInPicture,
                action = { 
                    viewModel.enterPictureInPictureMode()
                    onDismiss()
                }
            ),
            MoreOption(
                id = "sleep_timer",
                title = "Sleep Timer",
                subtitle = "Stop playback after time",
                icon = Icons.Default.Timer,
                action = { viewModel.showSleepTimer() }
            ),
            MoreOption(
                id = "equalizer",
                title = "Equalizer",
                subtitle = "Adjust audio frequencies",
                icon = Icons.Default.Equalizer,
                action = { viewModel.openEqualizer() }
            ),
            MoreOption(
                id = "video_info",
                title = "Video Information",
                subtitle = "View codec and details",
                icon = Icons.Default.Info,
                action = { viewModel.showVideoInfo() }
            ),
            MoreOption(
                id = "share",
                title = "Share",
                subtitle = "Share video link",
                icon = Icons.Default.Share,
                action = { viewModel.shareVideo() }
            ),
            MoreOption(
                id = "settings",
                title = "Player Settings",
                subtitle = "Configure player options",
                icon = Icons.Default.Settings,
                action = { 
                    onOpenSettings()
                    onDismiss()
                }
            )
        )
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "More Options",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Options list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(options.size) { index ->
                        val option = options[index]
                        MoreOptionItem(
                            option = option,
                            onClick = {
                                option.action()
                                if (option.id != "pip" && option.id != "settings") {
                                    onDismiss()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreOptionItem(
    option: MoreOption,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (option.isEnabled) Color.White.copy(alpha = 0.05f)
                else Color.White.copy(alpha = 0.02f)
            )
            .clickable(enabled = option.isEnabled) { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = null,
            tint = if (option.isEnabled) Color(0xFF00BCD4) else Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                color = if (option.isEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            option.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}