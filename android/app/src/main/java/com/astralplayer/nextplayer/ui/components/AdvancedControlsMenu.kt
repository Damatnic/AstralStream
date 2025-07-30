package com.astralplayer.nextplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.utils.FrameCaptureManager
import kotlinx.coroutines.launch

@Composable
fun AdvancedControlsMenu(
    videoUri: String,
    videoTitle: String,
    onPipMode: () -> Unit,
    onCastVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Advanced controls"
            )
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Picture-in-Picture") },
                onClick = {
                    onPipMode()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.PictureInPicture, contentDescription = null)
                }
            )
            
            DropdownMenuItem(
                text = { Text("Take Screenshot") },
                onClick = {
                    coroutineScope.launch {
                        FrameCaptureManager.captureAndSave(context, videoUri, videoTitle)
                    }
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                }
            )
            
            DropdownMenuItem(
                text = { Text("Cast to TV") },
                onClick = {
                    onCastVideo()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Cast, contentDescription = null)
                }
            )
        }
    }
}