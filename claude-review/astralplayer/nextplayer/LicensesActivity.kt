package com.astralplayer.nextplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astralplayer.nextplayer.ui.theme.AstralPlayerTheme

class LicensesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstralPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LicensesScreen(onBack = { finish() })
                }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "This application uses the following open source libraries:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LicenseItem(
                name = "Android Jetpack Compose",
                description = "Android's modern toolkit for building native UI",
                license = "Apache License 2.0",
                copyright = "Copyright © 2020 The Android Open Source Project"
            )
            
            LicenseItem(
                name = "ExoPlayer",
                description = "An application level media player for Android",
                license = "Apache License 2.0",
                copyright = "Copyright © 2014 The Android Open Source Project"
            )
            
            LicenseItem(
                name = "Room Database",
                description = "Persistence library providing an abstraction layer over SQLite",
                license = "Apache License 2.0",
                copyright = "Copyright © 2017 The Android Open Source Project"
            )
            
            LicenseItem(
                name = "Kotlin",
                description = "A modern programming language for multiplatform applications",
                license = "Apache License 2.0",
                copyright = "Copyright © 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors"
            )
            
            LicenseItem(
                name = "kotlinx.coroutines",
                description = "Library support for Kotlin coroutines",
                license = "Apache License 2.0",
                copyright = "Copyright © 2016-2023 JetBrains s.r.o."
            )
            
            LicenseItem(
                name = "Material Design Components",
                description = "Material Design components for Android",
                license = "Apache License 2.0",
                copyright = "Copyright © 2016 The Android Open Source Project"
            )
            
            LicenseItem(
                name = "AndroidX Libraries",
                description = "Jetpack libraries for Android development",
                license = "Apache License 2.0",
                copyright = "Copyright © 2018 The Android Open Source Project"
            )
            
            LicenseItem(
                name = "Firebase",
                description = "Google's mobile and web application development platform",
                license = "Apache License 2.0",
                copyright = "Copyright © 2016 Google Inc."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Apache License 2.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = apache2License,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseItem(
    name: String,
    description: String,
    license: String,
    copyright: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = copyright,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val apache2License = """
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
""".trimIndent()