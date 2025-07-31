package com.astralplayer.nextplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme
import com.astralplayer.nextplayer.ui.screens.SettingsScreen
import com.astralplayer.nextplayer.data.repository.SettingsRepository
import com.astralplayer.nextplayer.data.repository.SettingsRepositoryImpl

class SettingsActivity : ComponentActivity() {
    
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
                    SettingsScreen(
                        settingsRepository = settingsRepository,
                        onNavigateBack = { finish() },
                        onNavigateToGestureSettings = {
                            val intent = Intent(this@SettingsActivity, GestureSettingsActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}