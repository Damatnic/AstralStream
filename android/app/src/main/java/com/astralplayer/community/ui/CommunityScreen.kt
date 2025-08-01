package com.astralplayer.community.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astralplayer.community.viewmodel.CommunityViewModel
import com.astralplayer.community.viewmodel.CommunityTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToSharedPlaylists: () -> Unit,
    onNavigateToSubtitleContribution: () -> Unit,
    onNavigateToMyContributions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { CommunityTab.values().size })
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            CommunityTopBar(
                currentTab = CommunityTab.values()[pagerState.currentPage],
                onSettingsClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (CommunityTab.values()[pagerState.currentPage]) {
                        CommunityTab.PLAYLISTS -> onNavigateToSharedPlaylists()
                        CommunityTab.SUBTITLES -> onNavigateToSubtitleContribution()
                        CommunityTab.ACTIVITY -> {} // No FAB action for activity
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = when (CommunityTab.values()[pagerState.currentPage]) {
                        CommunityTab.PLAYLISTS -> Icons.Default.PlaylistAdd
                        CommunityTab.SUBTITLES -> Icons.Default.Subtitles
                        CommunityTab.ACTIVITY -> Icons.Default.Add
                    },
                    contentDescription = "Add"
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth()
            ) {
                CommunityTab.values().forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(tab.title) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        }
                    )
                }
            }
            
            // Content Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (CommunityTab.values()[page]) {
                    CommunityTab.PLAYLISTS -> PlaylistsTabContent(
                        playlists = uiState.trendingPlaylists,
                        onPlaylistClick = { /* Navigate to playlist details */ },
                        onRefresh = { viewModel.refreshPlaylists() }
                    )
                    CommunityTab.SUBTITLES -> SubtitlesTabContent(
                        contributors = uiState.topContributors,
                        recentSubtitles = uiState.recentSubtitles,
                        onContributorClick = { /* Navigate to contributor profile */ },
                        onSubtitleClick = { /* Navigate to subtitle details */ },
                        onMyContributionsClick = onNavigateToMyContributions
                    )
                    CommunityTab.ACTIVITY -> ActivityTabContent(
                        activities = uiState.recentActivities,
                        onActivityClick = { /* Navigate to activity details */ }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityTopBar(
    currentTab: CommunityTab,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = { 
            Text(
                text = "Community",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
private fun PlaylistsTabContent(
    playlists: List<com.astralplayer.community.repository.TrendingPlaylist>,
    onPlaylistClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    if (playlists.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.PlaylistPlay,
            title = "No Trending Playlists",
            subtitle = "Be the first to share a playlist!",
            actionText = "Refresh",
            onAction = onRefresh
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.shareCode) }
                )
            }
        }
    }
}

@Composable
private fun SubtitlesTabContent(
    contributors: List<com.astralplayer.community.repository.Contributor>,
    recentSubtitles: List<com.astralplayer.community.repository.CommunitySubtitle>,
    onContributorClick: (String) -> Unit,
    onSubtitleClick: (String) -> Unit,
    onMyContributionsClick: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // My Contributions Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMyContributionsClick() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Contributions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "View and manage your subtitle contributions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Go to contributions"
                    )
                }
            }
        }
        
        // Top Contributors Section
        if (contributors.isNotEmpty()) {
            item {
                Text(
                    text = "Top Contributors",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(contributors.take(5)) { contributor ->
                ContributorCard(
                    contributor = contributor,
                    onClick = { onContributorClick(contributor.id) }
                )
            }
        }
        
        // Recent Subtitles Section
        if (recentSubtitles.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Subtitles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            items(recentSubtitles) { subtitle ->
                SubtitleCard(
                    subtitle = subtitle,
                    onClick = { onSubtitleClick(subtitle.id) }
                )
            }
        }
    }
}

@Composable
private fun ActivityTabContent(
    activities: List<com.astralplayer.community.api.ActivityResponse>,
    onActivityClick: (String) -> Unit
) {
    if (activities.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Timeline,
            title = "No Recent Activity",
            subtitle = "Community activity will appear here"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activities) { activity ->
                ActivityCard(
                    activity = activity,
                    onClick = { onActivityClick(activity.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistCard(
    playlist: com.astralplayer.community.repository.TrendingPlaylist,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Playlist thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistPlay,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "by ${playlist.creatorName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LabelWithIcon(
                        icon = Icons.Default.VideoLibrary,
                        text = "${playlist.videoCount} videos"
                    )
                    LabelWithIcon(
                        icon = Icons.Default.Star,
                        text = String.format("%.1f", playlist.avgRating)
                    )
                    LabelWithIcon(
                        icon = Icons.Default.Download,
                        text = "${playlist.downloadCount}"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContributorCard(
    contributor: com.astralplayer.community.repository.Contributor,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contributor.name.first().toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = contributor.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (contributor.isVerified) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "${contributor.contributionCount} contributions • ${contributor.reputation} reputation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFFC107)
                )
                Text(
                    text = String.format("%.1f", contributor.averageRating),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtitleCard(
    subtitle: com.astralplayer.community.repository.CommunitySubtitle,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = subtitle.language,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                VerificationBadge(status = subtitle.verificationStatus)
            }
            
            Text(
                text = "for ${subtitle.videoHash.take(8)}...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LabelWithIcon(
                    icon = Icons.Default.ThumbUp,
                    text = "${subtitle.upvotes}"
                )
                LabelWithIcon(
                    icon = Icons.Default.ThumbDown,
                    text = "${subtitle.downvotes}"
                )
                LabelWithIcon(
                    icon = Icons.Default.Download,
                    text = "${subtitle.downloadCount}"
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activity: com.astralplayer.community.api.ActivityResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (activity.type) {
                    "playlist_shared" -> Icons.Default.PlaylistAdd
                    "subtitle_contributed" -> Icons.Default.Subtitles
                    "vote_cast" -> Icons.Default.ThumbUp
                    else -> Icons.Default.Notifications
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VerificationBadge(
    status: com.astralplayer.community.data.SubtitleVerificationStatus
) {
    val (text, color) = when (status) {
        com.astralplayer.community.data.SubtitleVerificationStatus.MODERATOR_VERIFIED -> "Verified" to MaterialTheme.colorScheme.primary
        com.astralplayer.community.data.SubtitleVerificationStatus.COMMUNITY_VERIFIED -> "Community" to MaterialTheme.colorScheme.secondary
        com.astralplayer.community.data.SubtitleVerificationStatus.AUTO_VERIFIED -> "Auto" to MaterialTheme.colorScheme.tertiary
        com.astralplayer.community.data.SubtitleVerificationStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> return
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LabelWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}