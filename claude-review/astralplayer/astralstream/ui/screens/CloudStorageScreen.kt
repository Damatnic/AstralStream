package com.astralplayer.astralstream.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.astralplayer.astralstream.cloud.CloudStorageManager

@Composable
fun CloudStorageScreen(
    onVideoClick: (CloudStorageManager.CloudFile) -> Unit,
    onBackPressed: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Cloud Storage Screen")
    }
}