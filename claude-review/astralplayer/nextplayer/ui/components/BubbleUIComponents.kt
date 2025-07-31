package com.astralplayer.nextplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Comprehensive Bubble UI Components Library for AstralStream
 * Modern glassmorphism design with smooth animations
 */

// ============= CARDS AND CONTAINERS =============

@Composable
fun BubbleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Int = 8,
    cornerRadius: Int = 16,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    glowColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Card(
        modifier = modifier
            .shadow(elevation.dp, RoundedCornerShape(cornerRadius.dp), spotColor = glowColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Int = 20,
    glassStrength: Float = 0.15f,
    borderWidth: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = glassStrength),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = glassStrength * 0.8f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius.dp)
            )
            .border(
                width = borderWidth.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius.dp)
            )
            .clip(RoundedCornerShape(cornerRadius.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else Modifier
            ),
        content = content
    )
}

// ============= BUTTONS =============

@Composable
fun BubbleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    icon: ImageVector? = null,
    cornerRadius: Int = 24,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (enabled) 8.dp else 2.dp,
        animationSpec = tween(200),
        label = "button_elevation"
    )
    
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .shadow(animatedElevation, RoundedCornerShape(cornerRadius.dp))
            .clip(RoundedCornerShape(cornerRadius.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(cornerRadius.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun BubbleIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Int = 48,
    iconSize: Int = 24,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    contentDescription: String? = null
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "icon_button_scale"
    )
    
    Box(
        modifier = modifier
            .size(size.dp)
            .shadow(6.dp, CircleShape)
            .background(containerColor, CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled) { onClick() }
            .scale(animatedScale),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize.dp)
        )
    }
}

// ============= NAVIGATION =============

@Composable
fun BubbleTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
) {
    BubbleCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24,
        containerColor = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                BubbleTab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    text = title,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BubbleTab(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(300),
        label = "tab_color"
    )
    
    val animatedTextColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "tab_text_color"
    )
    
    Box(
        modifier = modifier
            .background(animatedColor, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = animatedTextColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ============= INPUT FIELDS =============

@Composable
fun BubbleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    cornerRadius: Int = 16
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder) },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingIcon = trailingIcon?.let { icon ->
            {
                if (onTrailingIconClick != null) {
                    IconButton(onClick = onTrailingIconClick) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    )
}

// ============= LIST ITEMS =============

@Composable
fun BubbleListItem(
    headlineText: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
) {
    BubbleCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = containerColor,
        elevation = 4,
        cornerRadius = 12
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leadingContent?.invoke()
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                supportingText?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            trailingContent?.invoke()
        }
    }
}

// ============= FLOATING ACTION BUTTON =============

@Composable
fun BubbleFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentDescription: String? = null
) {
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_scale",
    )
    
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .shadow(12.dp, CircleShape)
            .scale(animatedScale),
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ============= TOP APP BAR =============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubbleTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                BubbleIconButton(
                    onClick = onNavigationClick,
                    icon = navigationIcon,
                    size = 40,
                    iconSize = 20
                )
            }
        },
        actions = {
            actions?.invoke(this)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

// ============= SEARCH BAR =============

@Composable
fun BubbleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    onSearch: (String) -> Unit = {},
    onClear: () -> Unit = { onQueryChange("") },
    active: Boolean = false,
    onActiveChange: (Boolean) -> Unit = {}
) {
    GlassmorphismCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 28
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            Box(
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = @Composable { innerTextField ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
            
            if (query.isNotEmpty()) {
                BubbleIconButton(
                    onClick = onClear,
                    icon = Icons.Default.Clear,
                    size = 32,
                    iconSize = 16,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============= LOADING AND EMPTY STATES =============

@Composable
fun BubbleLoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        GlassmorphismCard(
            cornerRadius = 20,
            glassStrength = 0.1f
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BubbleEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionButton: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            actionButton?.invoke()
        }
    }
}

// ============= HELPER FUNCTIONS =============

@Composable
fun BubbleLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
fun BubbleLazyRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    content: LazyListScope.() -> Unit
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}

// ============= DROPDOWN MENUS =============

@Composable
fun BubbleDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(16.dp)
            )
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        content = content
    )
}

@Composable
fun BubbleDropdownMenuItem(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                icon?.let { iconVector ->
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (destructive) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (destructive) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
        },
        onClick = onClick,
        modifier = modifier
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        enabled = enabled
    )
}

// ============= BUTTONS =============

@Composable
fun BubbleButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    cornerRadius: Int = 16,
    elevation: Int = 4
) {
    BubbleCard(
        modifier = modifier,
        onClick = if (enabled) onClick else null,
        elevation = elevation,
        cornerRadius = cornerRadius,
        containerColor = containerColor.copy(alpha = if (enabled) 1f else 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon?.let { iconVector ->
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor.copy(alpha = if (enabled) 1f else 0.5f)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = if (icon == null) Modifier.fillMaxWidth() else Modifier
            )
        }
    }
}

// ============= CHIPS =============

@Composable
fun BubbleChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    containerColor: Color = if (selected) 
        MaterialTheme.colorScheme.primaryContainer 
    else 
        MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = if (selected) 
        MaterialTheme.colorScheme.onPrimaryContainer 
    else 
        MaterialTheme.colorScheme.onSurfaceVariant
) {
    BubbleCard(
        modifier = modifier,
        onClick = if (enabled) onClick else null,
        elevation = if (selected) 6 else 3,
        cornerRadius = 16,
        containerColor = containerColor.copy(alpha = if (enabled) 1f else 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.let { iconVector ->
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor.copy(alpha = if (enabled) 1f else 0.5f)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}