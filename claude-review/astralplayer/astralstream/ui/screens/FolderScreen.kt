package com.astralplayer.astralstream.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.astralplayer.astralstream.data.entity.VideoEntity

@Composable
fun FolderScreen(
    folderId: Long,
    onVideoClick: (VideoEntity) -> Unit,
    onBackPressed: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Folder Screen - ID: $folderId")
    }
}