package com.astralplayer.nextplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import com.astralplayer.nextplayer.ui.screens.GestureSettingsScreen
import com.astralplayer.nextplayer.data.repository.SettingsRepository
import com.astralplayer.nextplayer.data.repository.SettingsRepositoryImpl

class GestureSettingsActivity : ComponentActivity() {
    
    private lateinit var settingsRepository: SettingsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize settings repository
        settingsRepository = SettingsRepositoryImpl(this)
        
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GestureSettingsScreen(
                        settingsRepository = settingsRepository,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}