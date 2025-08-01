package com.astralplayer.features.navigation

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astralplayer.features.analytics.ui.AnalyticsDashboardScreen
import com.astralplayer.features.analytics.ui.AnalyticsSettingsScreen
import com.astralplayer.features.community.ui.CommunityScreen
import com.astralplayer.features.editing.ui.VideoEditorScreen
import com.astralplayer.features.gestures.ui.GestureCustomizationScreen
import com.astralplayer.features.gestures.ui.GestureSettingsScreen
import com.astralplayer.features.gestures.ui.GestureTestScreen

sealed class AstralStreamDestination(val route: String) {
    object Home : AstralStreamDestination("home")
    object VideoPlayer : AstralStreamDestination("video_player/{videoUri}") {
        fun createRoute(videoUri: String) = "video_player/$videoUri"
    }
    object VideoEditor : AstralStreamDestination("video_editor/{videoUri}") {
        fun createRoute(videoUri: String) = "video_editor/$videoUri"
    }
    object Community : AstralStreamDestination("community")
    object Analytics : AstralStreamDestination("analytics")
    object AnalyticsSettings : AstralStreamDestination("analytics_settings")
    object GestureCustomization : AstralStreamDestination("gesture_customization")
    object GestureSettings : AstralStreamDestination("gesture_settings")
    object GestureTest : AstralStreamDestination("gesture_test")
    object Settings : AstralStreamDestination("settings")
    object About : AstralStreamDestination("about")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstralStreamApp(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    Scaffold(
        bottomBar = {
            AstralStreamBottomBar(navController = navController)
        },
        modifier = modifier
    ) { paddingValues ->
        AstralStreamNavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun AstralStreamNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AstralStreamDestination.Home.route,
        modifier = modifier
    ) {
        composable(AstralStreamDestination.Home.route) {
            HomeScreen(
                onNavigateToVideoPlayer = { uri ->
                    navController.navigate(
                        AstralStreamDestination.VideoPlayer.createRoute(uri.toString())
                    )
                },
                onNavigateToVideoEditor = { uri ->
                    navController.navigate(
                        AstralStreamDestination.VideoEditor.createRoute(uri.toString())
                    )
                },
                onNavigateToSettings = {
                    navController.navigate(AstralStreamDestination.Settings.route)
                }
            )
        }
        
        composable(AstralStreamDestination.VideoPlayer.route) { backStackEntry ->
            val videoUriString = backStackEntry.arguments?.getString("videoUri") ?: ""
            val videoUri = Uri.parse(videoUriString)
            
            VideoPlayerScreen(
                videoUri = videoUri,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = {
                    navController.navigate(
                        AstralStreamDestination.VideoEditor.createRoute(videoUriString)
                    )
                },
                onNavigateToAnalytics = {
                    navController.navigate(AstralStreamDestination.Analytics.route)
                }
            )
        }
        
        composable(AstralStreamDestination.VideoEditor.route) { backStackEntry ->
            val videoUriString = backStackEntry.arguments?.getString("videoUri") ?: ""
            val videoUri = Uri.parse(videoUriString)
            
            VideoEditorScreen(
                videoUri = videoUri,
                onNavigateBack = { navController.popBackStack() },
                onExportComplete = { exportedUri ->
                    // Navigate back to player with exported video
                    navController.navigate(
                        AstralStreamDestination.VideoPlayer.createRoute(exportedUri.toString())
                    ) {
                        popUpTo(AstralStreamDestination.Home.route)
                    }
                }
            )
        }
        
        composable(AstralStreamDestination.Community.route) {
            CommunityScreen(
                onNavigateBack = { navController.popBackStack() },
                onVideoSelected = { uri ->
                    navController.navigate(
                        AstralStreamDestination.VideoPlayer.createRoute(uri.toString())
                    )
                }
            )
        }
        
        composable(AstralStreamDestination.Analytics.route) {
            AnalyticsDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVideoDetails = { videoId ->
                    // Could navigate to a detailed video analytics screen
                }
            )
        }
        
        composable(AstralStreamDestination.AnalyticsSettings.route) {
            AnalyticsSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = {
                    navController.navigate(AstralStreamDestination.Analytics.route)
                }
            )
        }
        
        composable(AstralStreamDestination.GestureCustomization.route) {
            GestureCustomizationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTest = {
                    navController.navigate(AstralStreamDestination.GestureTest.route)
                }
            )
        }
        
        composable(AstralStreamDestination.GestureSettings.route) {
            GestureSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCustomization = {
                    navController.navigate(AstralStreamDestination.GestureCustomization.route)
                }
            )
        }
        
        composable(AstralStreamDestination.GestureTest.route) {
            GestureTestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(AstralStreamDestination.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGestureSettings = {
                    navController.navigate(AstralStreamDestination.GestureSettings.route)
                },
                onNavigateToAnalyticsSettings = {
                    navController.navigate(AstralStreamDestination.AnalyticsSettings.route)
                },
                onNavigateToAbout = {
                    navController.navigate(AstralStreamDestination.About.route)
                }
            )
        }
        
        composable(AstralStreamDestination.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun AstralStreamBottomBar(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Only show bottom bar on main screens
    val showBottomBar = when (currentRoute) {
        AstralStreamDestination.Home.route,
        AstralStreamDestination.Community.route,
        AstralStreamDestination.Analytics.route,
        AstralStreamDestination.Settings.route -> true
        else -> false
    }
    
    if (showBottomBar) {
        NavigationBar {
            NavigationBarItem(
                selected = currentRoute == AstralStreamDestination.Home.route,
                onClick = {
                    navController.navigate(AstralStreamDestination.Home.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") }
            )
            
            NavigationBarItem(
                selected = currentRoute == AstralStreamDestination.Community.route,
                onClick = {
                    navController.navigate(AstralStreamDestination.Community.route) {
                        launchSingleTop = true
                    }
                },
                icon = { Icon(Icons.Default.People, contentDescription = "Community") },
                label = { Text("Community") }
            )
            
            NavigationBarItem(
                selected = currentRoute == AstralStreamDestination.Analytics.route,
                onClick = {
                    navController.navigate(AstralStreamDestination.Analytics.route) {
                        launchSingleTop = true
                    }
                },
                icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                label = { Text("Analytics") }
            )
            
            NavigationBarItem(
                selected = currentRoute == AstralStreamDestination.Settings.route,
                onClick = {
                    navController.navigate(AstralStreamDestination.Settings.route) {
                        launchSingleTop = true
                    }
                },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") }
            )
        }
    }
}

// Placeholder screens - these would be implemented based on existing app structure

@Composable
fun HomeScreen(
    onNavigateToVideoPlayer: (Uri) -> Unit,
    onNavigateToVideoEditor: (Uri) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "AstralStream",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { 
                // Demo video URI - replace with actual file picker
                val demoUri = Uri.parse("android.resource://com.astralplayer/raw/demo_video")
                onNavigateToVideoPlayer(demoUri)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play Video")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                // Demo video URI - replace with actual file picker
                val demoUri = Uri.parse("android.resource://com.astralplayer/raw/demo_video")
                onNavigateToVideoEditor(demoUri)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Video")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Settings")
        }
    }
}

@Composable
fun VideoPlayerScreen(
    videoUri: Uri,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    // This would integrate with the existing video player
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Video Player Placeholder")
        Text("URI: $videoUri")
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onNavigateToEditor) {
                Text("Edit")
            }
            Button(onClick = onNavigateToAnalytics) {
                Text("Analytics")
            }
        }
        
        Button(onClick = onNavigateBack) {
            Text("Back")
        }
    }
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGestureSettings: () -> Unit,
    onNavigateToAnalyticsSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection(title = "Features") {
                SettingsItem(
                    title = "Gesture Settings",
                    description = "Customize gesture controls",
                    icon = Icons.Default.TouchApp,
                    onClick = onNavigateToGestureSettings
                )
                
                SettingsItem(
                    title = "Analytics Settings",
                    description = "Manage analytics and privacy",
                    icon = Icons.Default.Analytics,
                    onClick = onNavigateToAnalyticsSettings
                )
            }
        }
        
        item {
            SettingsSection(title = "About") {
                SettingsItem(
                    title = "About AstralStream",
                    description = "Version info and credits",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "About AstralStream",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Elite Video Player with AI-Powered Features",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Version 1.0.0",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onNavigateBack) {
            Text("Back")
        }
    }
}