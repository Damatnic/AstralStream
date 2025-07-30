package com.astralplayer.nextplayer.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralplayer.nextplayer.utils.ThumbnailGenerator
import com.astralplayer.nextplayer.data.ThumbnailService
import kotlinx.coroutines.launch

/**
 * Composable for displaying video thumbnails with loading and error states
 */
@Composable
fun VideoThumbnail(
    videoUri: Uri,
    duration: Long = 0L,
    modifier: Modifier = Modifier,
    showDuration: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop,
    useThumbnailService: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    // Choose between ThumbnailService and direct ThumbnailGenerator
    val thumbnailService = remember { if (useThumbnailService) ThumbnailService(context) else null }
    val thumbnailGenerator = remember { if (!useThumbnailService) ThumbnailGenerator(context) else null }
    
    // Load thumbnail
    LaunchedEffect(videoUri) {
        isLoading = true
        hasError = false
        try {
            if (useThumbnailService && thumbnailService != null) {
                // Use the correct ThumbnailService from utils package
                val utilsThumbnailService = com.astralplayer.nextplayer.utils.ThumbnailService.getInstance(context)
                utilsThumbnailService.generateVideoThumbnail(videoUri) { thumbnailUrl ->
                    // ThumbnailService returns a file URL, we need to load the bitmap from it
                    thumbnail = if (thumbnailUrl != null) {
                        try {
                            android.graphics.BitmapFactory.decodeFile(thumbnailUrl.removePrefix("file://"))
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                    hasError = thumbnail == null
                    isLoading = false
                }
            } else {
                // Use direct thumbnail generator
                scope.launch {
                    try {
                        thumbnail = thumbnailGenerator?.getThumbnail(videoUri)
                        hasError = thumbnail == null
                    } catch (e: Exception) {
                        hasError = true
                    } finally {
                        isLoading = false
                    }
                }
            }
        } catch (e: Exception) {
            hasError = true
            isLoading = false
        }
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            hasError || thumbnail == null -> {
                // Error or placeholder state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasError) Icons.Default.BrokenImage else Icons.Default.VideoFile,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            else -> {
                // Display thumbnail
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = "Video thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }
        
        // Duration badge
        if (showDuration && duration > 0 && !isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Video thumbnail for seek preview
 */
@Composable
fun SeekPreviewThumbnail(
    videoUri: Uri,
    positionMs: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Remember the thumbnail generator
    val thumbnailGenerator = remember { ThumbnailGenerator(context) }
    
    // Load thumbnail at specific position
    LaunchedEffect(videoUri, positionMs) {
        if (positionMs >= 0) {
            isLoading = true
            scope.launch {
                try {
                    // Convert milliseconds to microseconds
                    val positionUs = positionMs * 1000
                    thumbnail = thumbnailGenerator.getThumbnailAtPosition(videoUri, positionUs)
                } catch (e: Exception) {
                    thumbnail = null
                } finally {
                    isLoading = false
                }
            }
        }
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null && !isLoading) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = "Seek preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        
        // Time indicator
        if (positionMs >= 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(4.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatTime(positionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

private fun formatDuration(totalMillis: Long): String {
    val totalSeconds = totalMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatTime(positionMs: Long): String {
    val totalSeconds = positionMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * Background thumbnail generation progress indicator
 */
@Composable
fun ThumbnailGenerationProgress(
    thumbnailService: ThumbnailService,
    modifier: Modifier = Modifier
) {
    val isGenerating by thumbnailService.isGenerating.collectAsState()
    val progress by thumbnailService.generationProgress.collectAsState()
    
    if (isGenerating) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Generating thumbnails...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Grid of seek preview thumbnails
 */
@Composable
fun SeekThumbnailGrid(
    videoUri: Uri,
    thumbnails: List<Pair<Long, Bitmap?>>,
    currentPosition: Long,
    onPositionSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(thumbnails) { (position, bitmap) ->
            val isSelected = kotlin.math.abs(currentPosition - position) < 1000L // Within 1 second
            
            Card(
                modifier = Modifier
                    .size(width = 80.dp, height = 45.dp)
                    .clickable { onPositionSelected(position) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else 
                        MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) 
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else null
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Seek preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    
                    // Time overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatTime(position),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}