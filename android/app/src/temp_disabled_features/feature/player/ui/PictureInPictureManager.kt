package com.astralplayer.nextplayer.feature.player.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Manager for Picture-in-Picture functionality
 */
class PictureInPictureManager(private val context: Context) {
    
    /**
     * Check if PiP is supported on this device
     */
    fun isPiPSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        } else {
            false
        }
    }
    
    /**
     * Enter Picture-in-Picture mode
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun enterPiPMode(
        activity: Activity,
        aspectRatio: Rational = Rational(16, 9),
        sourceRectHint: Rect? = null
    ): Boolean {
        return try {
            val pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .apply {
                    sourceRectHint?.let { setSourceRectHint(it) }
                    
                    // Add custom actions for PiP mode
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Actions will be added here
                    }
                }
                .build()
            
            activity.enterPictureInPictureMode(pipParams)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Update PiP parameters
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePiPParams(
        activity: Activity,
        aspectRatio: Rational,
        sourceRectHint: Rect? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .apply {
                    sourceRectHint?.let { setSourceRectHint(it) }
                }
                .build()
            
            activity.setPictureInPictureParams(pipParams)
        }
    }
    
    /**
     * Check if currently in PiP mode
     */
    fun isInPiPMode(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activity.isInPictureInPictureMode
        } else {
            false
        }
    }
}

/**
 * Composable for PiP controls overlay
 */
@Composable
fun PiPControlsOverlay(
    isInPiPMode: Boolean,
    onEnterPiP: () -> Unit,
    onExitPiP: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pipManager = remember { PictureInPictureManager(context) }
    
    if (pipManager.isPiPSupported()) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isInPiPMode) Icons.Default.Fullscreen else Icons.Default.PictureInPictureAlt,
                    contentDescription = if (isInPiPMode) "Exit PiP" else "Enter PiP",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                
                Text(
                    text = if (isInPiPMode) "Exit PiP" else "Picture in Picture",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                IconButton(
                    onClick = if (isInPiPMode) onExitPiP else onEnterPiP,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isInPiPMode) Icons.Default.Close else Icons.Default.Launch,
                        contentDescription = if (isInPiPMode) "Exit" else "Enter",
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * PiP-aware video player composable
 */
@Composable
fun PiPAwareVideoPlayer(
    isInPiPMode: Boolean,
    onPiPModeChanged: (Boolean) -> Unit,
    content: @Composable (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Monitor PiP mode changes
    LaunchedEffect(isInPiPMode) {
        onPiPModeChanged(isInPiPMode)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Main video content
        content(isInPiPMode)
        
        // PiP mode indicator
        if (isInPiPMode) {
            PiPModeIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
        }
    }
}

/**
 * Indicator shown when in PiP mode
 */
@Composable
private fun PiPModeIndicator(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF00BCD4).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PictureInPictureAlt,
                contentDescription = "PiP Mode",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            
            Text(
                text = "PiP",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Extension functions for Activity
 */
fun Activity.enterPiPModeIfSupported(
    aspectRatio: Rational = Rational(16, 9),
    sourceRectHint: Rect? = null
): Boolean {
    val pipManager = PictureInPictureManager(this)
    return if (pipManager.isPiPSupported() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        pipManager.enterPiPMode(this, aspectRatio, sourceRectHint)
    } else {
        false
    }
}

fun Activity.updatePiPParamsIfSupported(
    aspectRatio: Rational,
    sourceRectHint: Rect? = null
) {
    val pipManager = PictureInPictureManager(this)
    if (pipManager.isPiPSupported() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        pipManager.updatePiPParams(this, aspectRatio, sourceRectHint)
    }
}

/**
 * Data class for PiP configuration
 */
data class PiPConfiguration(
    val aspectRatio: Rational = Rational(16, 9),
    val sourceRectHint: Rect? = null,
    val autoEnterOnBackground: Boolean = false,
    val showControls: Boolean = true,
    val enableCustomActions: Boolean = true
)

/**
 * PiP state management
 */
@Composable
fun rememberPiPState(): MutableState<Boolean> {
    val context = LocalContext.current
    val activity = context as? Activity
    
    return remember {
        mutableStateOf(
            activity?.let { 
                PictureInPictureManager(context).isInPiPMode(it) 
            } ?: false
        )
    }
}

/**
 * Hook for PiP lifecycle events
 */
@Composable
fun PiPLifecycleEffect(
    isInPiPMode: Boolean,
    onEnterPiP: () -> Unit = {},
    onExitPiP: () -> Unit = {}
) {
    LaunchedEffect(isInPiPMode) {
        if (isInPiPMode) {
            onEnterPiP()
        } else {
            onExitPiP()
        }
    }
}