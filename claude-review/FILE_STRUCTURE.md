# AstralStream File Structure Overview

## Root Configuration Files
- `AndroidManifest.xml` - App manifest with intent filters and permissions
- `app-build.gradle` - App-level build configuration and dependencies
- `project-build.gradle` - Project-level build configuration
- `settings.gradle` - Project settings
- `gradle.properties` - Gradle configuration properties

## Source Code Structure (`/astralplayer/`)

### Main Application
- `AstralPlayerApplication.kt` - Main application class with Hilt setup

### Core Components (`/core/`)
- **audio/**
  - `AudioExtractorEngine.kt` - Audio extraction for subtitle generation
- **browser/**
  - `BrowserIntentHandler.kt` - Browser-specific data extraction
- **codec/**
  - `CodecManager.kt` - Codec management
  - `CodecOptimizer.kt` - Video codec optimization
- **config/**
  - `ApiKeyManager.kt` - Secure API key storage
- **extractor/**
  - `StreamExtractor.kt` - Video URL extraction from web pages
- **intent/**
  - `VideoIntentHandler.kt` - Video intent processing
- **performance/**
  - `PerformanceValidator.kt` - Performance monitoring
- **system/**
  - `DefaultPlayerManager.kt` - Default video player registration

### Features (`/features/`)
- **ai/**
  - `EnhancedAISubtitleGenerator.kt` - Multi-provider subtitle generation
  - `SubtitleFallbackEngine.kt` - Fallback subtitle system

### Presentation Layer (`/presentation/`)
- **player/**
  - `EnhancedVideoPlayerActivity.kt` - Main video player activity
  - `VideoPlayerScreen.kt` - Compose UI for video player

### AstralStream Package (`/astralstream/`)
- **ai/**
  - `AISubtitleGenerator.kt` - Original AI subtitle generator
- **cloud/**
  - `CloudStorageManager.kt` - Cloud storage integration
- **data/**
  - **dao/** - Room DAOs for database operations
  - **database/** - Database configuration
  - **entity/** - Database entities
  - **repository/** - Repository pattern implementations
- **di/**
  - `AppModule.kt` - Hilt dependency injection module
- **gesture/**
  - `EnhancedGestureManager.kt` - Gesture controls
- **navigation/**
  - `AstralStreamNavigation.kt` - Navigation setup
- **player/**
  - `AdvancedPlayerConfiguration.kt` - Player configuration
- **ui/**
  - **components/** - Reusable UI components
  - **screens/** - App screens (Home, Settings, etc.)
  - **theme/** - Material3 theme configuration
- **viewmodel/**
  - `MainViewModel.kt` - Main screen view model
  - `VideoPlayerViewModel.kt` - Video player view model

### NextPlayer Package (`/nextplayer/`)
Contains additional features from NextPlayer integration:
- Accessibility features
- Audio/Video selection
- Brightness/Volume controls
- Chapter support
- Chromecast integration
- Download functionality
- Gesture controls
- Lock screen features
- Media3 extensions
- Picture-in-Picture support
- Playlist management
- Settings and preferences
- Subtitle rendering
- UI components

## Total Statistics
- ~382 Kotlin files
- ~5MB total size
- Comprehensive video player implementation with advanced features