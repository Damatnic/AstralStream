package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * No Search Results State
 * Animated empty state for search results
 */
@Composable
fun NoSearchResultsState(
    query: String,
    modifier: Modifier = Modifier,
    suggestions: List<String> = emptyList(),
    onSuggestionClick: ((String) -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val scale = infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val rotation = infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    rotationZ = rotation.value
                },
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Main message
        Text(
            text = "No results found",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Query display
        Text(
            text = "for \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Helpful message
        Text(
            text = "Try adjusting your search terms or filters",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        // Search suggestions
        if (suggestions.isNotEmpty() && onSuggestionClick != null) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Try searching for:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.take(3).forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = { Text(suggestion) }
                    )
                }
            }
        }
    }
}

/**
 * Alternative No Search Results with Animation
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedNoSearchResults(
    query: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + 
                scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Magnifying glass animation
            AnimatedMagnifyingGlass()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Nothing here!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "We couldn't find any videos matching\n\"$query\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                BubbleButton(
                    onClick = onRetry,
                    text = "Try Again",
                    icon = Icons.Default.Refresh,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * Animated magnifying glass icon
 */
@Composable
private fun AnimatedMagnifyingGlass() {
    val infiniteTransition = rememberInfiniteTransition()
    
    val offsetX = infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val offsetY = infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Surface(
            modifier = Modifier.size(140.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {}
        
        // Moving magnifying glass
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    translationX = offsetX.value
                    translationY = offsetY.value
                },
            tint = MaterialTheme.colorScheme.primary
        )
        
        // Question mark
        Text(
            text = "?",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.offset(x = 20.dp, y = (-20).dp)
        )
    }
}