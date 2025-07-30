plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
    id 'kotlin-parcelize'
    id 'androidx.room'
    id 'com.google.gms.google-services'
}

android {
    namespace 'com.astralplayer.nextplayer'
    compileSdk 34

    defaultConfig {
        applicationId "com.astralplayer.nextplayer"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary true
        }
        
        // Enable multidex for large APK
        multiDexEnabled true
        
        // Optimization for video playback
        ndk {
            abiFilters 'arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'
        }
        
        // ProGuard configurations
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }

    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            
            // Optimization flags
            zipAlignEnabled true
            debuggable false
            jniDebuggable false
            renderscriptDebuggable false
            
            // Signing config (configure as needed)
            // signingConfig signingConfigs.release
        }
        debug {
            minifyEnabled false
            applicationIdSuffix ".debug"
            versionNameSuffix "-debug"
            debuggable true
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
        coreLibraryDesugaringEnabled true
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
        
        // Enable experimental features
        freeCompilerArgs += [
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.media3.common.util.UnstableApi"
        ]
    }
    
    buildFeatures {
        compose true
        buildConfig true
        viewBinding true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion '1.5.8'
    }
    
    packagingOptions {
        resources {
            excludes += '/META-INF/{AL2.0,LGPL2.1}'
            excludes += '/META-INF/gradle/incremental.annotation.processors'
        }
    }
    
    // Enable Hilt incremental annotation processing
    hilt {
        enableAggregatingTask = true
    }
    
    room {
        schemaLocation "$projectDir/schemas"
    }
    
    // Configure for large video files
    dexOptions {
        javaMaxHeapSize "4g"
    }
    
    // Enhanced build optimizations
    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}

dependencies {
    // Core Android dependencies
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-compose:2.7.0'
    implementation 'androidx.activity:activity-compose:1.8.2'
    implementation 'androidx.fragment:fragment-ktx:1.6.2'
    implementation 'androidx.multidex:multidex:2.0.1'
    
    // Compose BOM and core libraries
    implementation platform('androidx.compose:compose-bom:2024.02.00')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-graphics'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.compose.material3:material3:1.2.0'
    implementation 'androidx.compose.material:material-icons-extended'
    implementation 'androidx.compose.animation:animation'
    implementation 'androidx.compose.foundation:foundation'
    implementation 'androidx.compose.ui:ui-util'
    
    // Navigation
    implementation 'androidx.navigation:navigation-compose:2.7.6'
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.6'
    
    // ExoPlayer/Media3 with all extensions
    implementation 'androidx.media3:media3-exoplayer:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-dash:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-hls:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-smoothstreaming:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-rtsp:1.2.1'
    implementation 'androidx.media3:media3-ui:1.2.1'
    implementation 'androidx.media3:media3-session:1.2.1'
    implementation 'androidx.media3:media3-datasource-okhttp:1.2.1'
    implementation 'androidx.media3:media3-decoder-ffmpeg:1.2.1' // For additional codec support
    implementation 'androidx.media3:media3-effect:1.2.1'
    implementation 'androidx.media3:media3-transformer:1.2.1'
    implementation 'androidx.media3:media3-extractor:1.2.1'
    implementation 'androidx.media3:media3-datasource-cronet:1.2.1'
    
    // Hilt Dependency Injection
    implementation 'com.google.dagger:hilt-android:2.48'
    implementation 'androidx.hilt:hilt-navigation-compose:1.1.0'
    implementation 'androidx.hilt:hilt-work:1.1.0'
    kapt 'com.google.dagger:hilt-compiler:2.48'
    kapt 'androidx.hilt:hilt-compiler:1.1.0'
    
    // Room Database
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
    
    // DataStore for preferences
    implementation 'androidx.datastore:datastore-preferences:1.0.0'
    implementation 'androidx.datastore:datastore-core:1.0.0'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3'
    
    // Networking
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.retrofit2:converter-moshi:2.9.0'
    
    // JSON parsing
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'com.squareup.moshi:moshi:1.15.0'
    implementation 'com.squareup.moshi:moshi-kotlin:1.15.0'
    
    // Image loading
    implementation 'io.coil-kt:coil-compose:2.5.0'
    implementation 'io.coil-kt:coil-video:2.5.0'
    implementation 'io.coil-kt:coil-gif:2.5.0'
    
    // Permissions handling
    implementation 'com.google.accompanist:accompanist-permissions:0.32.0'
    implementation 'com.google.accompanist:accompanist-systemuicontroller:0.32.0'
    implementation 'com.google.accompanist:accompanist-insets:0.32.0'
    
    // Cloud Storage APIs
    implementation 'com.google.android.gms:play-services-auth:20.7.0'
    implementation 'com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0'
    implementation 'com.google.api-client:google-api-client-android:2.2.0'
    implementation 'com.google.oauth-client:google-oauth-client-jetty:1.34.1'
    implementation 'com.google.http-client:google-http-client-gson:1.43.3'
    
    // Dropbox API
    implementation 'com.dropbox.core:dropbox-core-sdk:5.4.5'
    
    // OneDrive API
    implementation 'com.microsoft.graph:microsoft-graph:5.74.0'
    implementation 'com.microsoft.graph:microsoft-graph-auth:0.4.0'
    
    // Firebase (for analytics and crash reporting)
    implementation platform('com.google.firebase:firebase-bom:32.7.1')
    implementation 'com.google.firebase:firebase-analytics-ktx'
    implementation 'com.google.firebase:firebase-crashlytics-ktx'
    implementation 'com.google.firebase:firebase-performance-ktx'
    
    // AI/ML capabilities
    implementation 'com.google.ai.client.generativeai:generativeai:0.2.2'
    implementation 'com.google.mlkit:translate:17.0.2'
    implementation 'com.google.mlkit:language-id:17.0.4'
    
    // Biometric authentication
    implementation 'androidx.biometric:biometric:1.1.0'
    
    // File picker and document handling
    implementation 'androidx.documentfile:documentfile:1.0.1'
    
    // Advanced video processing
    implementation 'org.bytedeco:javacv-platform:1.5.9'
    implementation 'org.bytedeco:ffmpeg-platform:6.0-1.5.9'
    
    // Subtitle parsing
    implementation 'org.mozilla:rhino:1.7.14'
    
    // Advanced gesture detection
    implementation 'androidx.compose.foundation:foundation:1.6.0'
    
    // WorkManager for background tasks
    implementation 'androidx.work:work-runtime-ktx:2.9.0'
    implementation 'androidx.work:work-hilt:1.1.0'
    
    // Palette for dynamic theming
    implementation 'androidx.palette:palette-ktx:1.0.0'
    
    // SplashScreen API
    implementation 'androidx.core:core-splashscreen:1.0.1'
    
    // Advanced animation
    implementation 'androidx.compose.animation:animation-graphics:1.6.0'
    
    // Custom components
    implementation 'com.google.accompanist:accompanist-flowlayout:0.32.0'
    implementation 'com.google.accompanist:accompanist-pager:0.32.0'
    implementation 'com.google.accompanist:accompanist-pager-indicators:0.32.0'
    implementation 'com.google.accompanist:accompanist-swiperefresh:0.32.0'
    
    // Advanced material components
    implementation 'com.google.android.material:material:1.11.0'
    
    // Logging
    implementation 'com.jakewharton.timber:timber:5.0.1'
    
    // Desugaring for API compatibility
    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.0.4'
    
    // Testing dependencies
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
    testImplementation 'androidx.room:room-testing:2.6.1'
    testImplementation 'com.google.truth:truth:1.1.4'
    testImplementation 'org.mockito:mockito-core:5.8.0'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:5.2.1'
    testImplementation 'androidx.arch.core:core-testing:2.2.0'
    
    // Android testing
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation platform('androidx.compose:compose-bom:2024.02.00')
    androidTestImplementation 'androidx.compose.ui:ui-test-junit4'
    androidTestImplementation 'androidx.navigation:navigation-testing:2.7.6'
    androidTestImplementation 'com.google.dagger:hilt-android-testing:2.48'
    androidTestImplementation 'androidx.work:work-testing:2.9.0'
    kaptAndroidTest 'com.google.dagger:hilt-android-compiler:2.48'
    
    // Debug tools
    debugImplementation 'androidx.compose.ui:ui-tooling'
    debugImplementation 'androidx.compose.ui:ui-test-manifest'
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
}