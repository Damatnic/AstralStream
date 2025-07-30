package com.astralplayer.nextplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.ui.theme.AstralTheme

class LicensesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AstralTheme {
                LicensesScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Source Licenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(licenses) { license ->
                LicenseCard(license = license)
            }
        }
    }
}

@Composable
fun LicenseCard(license: License) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = license.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = license.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = license.license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (license.url.isNotEmpty()) {
                Text(
                    text = license.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

data class License(
    val name: String,
    val author: String,
    val license: String,
    val url: String = ""
)

val licenses = listOf(
    License(
        name = "AndroidX Media3",
        author = "The Android Open Source Project",
        license = "Apache License 2.0",
        url = "https://github.com/androidx/media"
    ),
    License(
        name = "Jetpack Compose",
        author = "The Android Open Source Project",
        license = "Apache License 2.0",
        url = "https://developer.android.com/jetpack/compose"
    ),
    License(
        name = "Hilt",
        author = "Google",
        license = "Apache License 2.0",
        url = "https://dagger.dev/hilt/"
    ),
    License(
        name = "Kotlin Coroutines",
        author = "JetBrains",
        license = "Apache License 2.0",
        url = "https://github.com/Kotlin/kotlinx.coroutines"
    ),
    License(
        name = "Coil",
        author = "Coil Contributors",
        license = "Apache License 2.0",
        url = "https://github.com/coil-kt/coil"
    ),
    License(
        name = "DataStore",
        author = "The Android Open Source Project",
        license = "Apache License 2.0",
        url = "https://developer.android.com/topic/libraries/architecture/datastore"
    ),
    License(
        name = "Material Components",
        author = "Google",
        license = "Apache License 2.0",
        url = "https://github.com/material-components/material-components-android"
    ),
    License(
        name = "OkHttp",
        author = "Square, Inc.",
        license = "Apache License 2.0",
        url = "https://github.com/square/okhttp"
    ),
    License(
        name = "Firebase",
        author = "Google",
        license = "Apache License 2.0",
        url = "https://firebase.google.com"
    ),
    License(
        name = "Compose Reorderable",
        author = "burnoutcrew",
        license = "Apache License 2.0",
        url = "https://github.com/aclassen/ComposeReorderable"
    )
)