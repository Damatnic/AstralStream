package com.astralplayer.astralstream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.astralplayer.astralstream.navigation.AstralStreamNavigation
import com.astralplayer.astralstream.ui.theme.AstralStreamTheme
import com.astralplayer.astralstream.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val mainViewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Keep splash screen visible while loading
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        
        enableEdgeToEdge()
        
        setContent {
            val isLoading by mainViewModel.isLoading.collectAsState()
            val isDarkTheme by mainViewModel.isDarkTheme.collectAsState()
            
            // Control splash screen visibility
            LaunchedEffect(isLoading) {
                if (!isLoading) {
                    delay(300) // Small delay for smooth transition
                    keepSplashOnScreen = false
                }
            }
            
            AstralStreamTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { paddingValues ->
                        AstralStreamNavigation(
                            navController = navController,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        mainViewModel.checkForUpdates()
    }
}