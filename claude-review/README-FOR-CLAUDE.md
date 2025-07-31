# AstralStream Code Review Package

This folder contains all necessary files for conducting a comprehensive code review of the AstralStream video player project.

## Project Overview

AstralStream is an advanced Android video player application with the following key features:

1. **Enhanced Video Player**: ExoPlayer-based with HLS/DASH support
2. **Browser Integration**: "Open With" functionality for video URLs from any browser
3. **AI-Powered Subtitles**: Multi-provider subtitle generation with fallback systems
4. **Cloud Storage**: Integration with Google Drive, Dropbox, OneDrive
5. **Advanced Playback**: 4K support, HDR, codec optimization
6. **Gesture Controls**: Swipe gestures for volume, brightness, seek

## Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **DI**: Hilt/Dagger
- **Media Player**: ExoPlayer
- **Database**: Room
- **Min SDK**: 21
- **Target SDK**: 34

## Key Components to Review

### 1. Core Functionality
- `/com/astralplayer/AstralPlayerApplication.kt` - Main application class
- `/com/astralplayer/presentation/player/EnhancedVideoPlayerActivity.kt` - Main video player
- `/com/astralplayer/presentation/player/VideoPlayerScreen.kt` - Compose UI

### 2. Browser Integration
- `/com/astralplayer/core/browser/BrowserIntentHandler.kt` - Browser data extraction
- `/com/astralplayer/core/intent/VideoIntentHandler.kt` - Video intent processing
- `/com/astralplayer/core/extractor/StreamExtractor.kt` - Stream URL extraction
- `/com/astralplayer/core/system/DefaultPlayerManager.kt` - Default player registration

### 3. AI Subtitle System
- `/com/astralplayer/features/ai/EnhancedAISubtitleGenerator.kt` - Main subtitle generator
- `/com/astralplayer/core/config/ApiKeyManager.kt` - Secure API key storage
- `/com/astralplayer/core/audio/AudioExtractorEngine.kt` - Audio extraction
- `/com/astralplayer/features/ai/SubtitleFallbackEngine.kt` - Fallback subtitle generation

### 4. Configuration
- `AndroidManifest.xml` - Intent filters and permissions
- `app-build.gradle` - Dependencies and build configuration
- `/com/astralplayer/astralstream/di/AppModule.kt` - Dependency injection setup

## Critical Areas for Review

1. **Browser Intent Filters**: Check if priority=999 and intent-filter categories are properly configured
2. **Package Name Consistency**: Ensure all references use `com.astralplayer` consistently
3. **AI Integration**: Review API key management and fallback systems
4. **Security**: Check for any exposed API keys or security vulnerabilities
5. **Performance**: Look for memory leaks, especially in video playback
6. **Error Handling**: Ensure proper error handling in network operations

## Known Issues to Verify

1. Browser "Open With" menu appearance on all devices
2. Video URL extraction from JavaScript-rendered content
3. Subtitle generation without API keys (fallback system)
4. Default player registration on Android 10+

## Testing Recommendations

1. Test browser integration with Chrome, Firefox, Edge, Samsung Browser
2. Verify video playback with various formats (MP4, MKV, HLS, DASH)
3. Test subtitle generation with and without API keys
4. Check memory usage during long video playback sessions
5. Verify gesture controls work properly

## Dependencies to Review

Key dependencies in app-build.gradle:
- ExoPlayer: 2.19.1
- Hilt: 2.48
- Compose BOM: 2023.10.01
- Room: 2.6.1
- OkHttp: 4.12.0
- Retrofit: 2.9.0

## Security Considerations

- API keys are stored encrypted using Android Keystore
- No hardcoded credentials in source code
- Network requests use HTTPS
- Cookies and headers are properly handled

Please conduct a thorough review focusing on code quality, security, performance, and architectural decisions.