# Astral Player - Android Video Player

A modern, feature-rich video player for Android with advanced gesture controls, cloud storage integration, and AI-powered features.

## Features

### Core Features
- **Universal Video Format Support**: Supports all major video formats including MP4, MKV, AVI, MOV, WebM, FLV, and more
- **Advanced Gesture Controls**: Customizable swipe gestures for volume, brightness, and seeking
- **Picture-in-Picture (PiP)**: Continue watching while using other apps
- **Playlist Management**: Create and manage video playlists
- **Recent Files**: Quick access to recently played videos
- **Search Functionality**: Search videos across device and playlists

### Cloud Storage Integration
- **Google Drive**: Stream videos directly from Google Drive
- **Dropbox** (Coming Soon): Access videos from Dropbox
- **OneDrive** (Coming Soon): Stream from Microsoft OneDrive

### Advanced Features
- **AI Subtitle Generation**: Generate subtitles using AI (experimental)
- **Chromecast Support**: Cast videos to TV
- **Sleep Timer**: Auto-stop playback after set duration
- **Playback Speed Control**: Adjust video playback speed
- **Audio Track Selection**: Choose between multiple audio tracks
- **Subtitle Support**: Load external subtitles, adjust appearance

### UI/UX Features
- **Material Design 3**: Modern, clean interface following latest design guidelines
- **Dark/Light Theme**: Automatic theme based on system settings
- **Adaptive Icons**: Launcher icon that adapts to device theme
- **Haptic Feedback**: Tactile response for gestures

## Project Structure

```
android/
├── app/
│   └── src/
│       └── main/
│           └── java/com/astralplayer/nextplayer/
│               ├── data/           # Data layer (repositories, models)
│               ├── feature/        # Feature modules
│               │   ├── cloud/      # Cloud storage integration
│               │   ├── pip/        # Picture-in-Picture
│               │   └── player/     # Video player components
│               ├── ui/             # UI components and screens
│               │   ├── components/ # Reusable UI components
│               │   ├── screens/    # Full screen composables
│               │   └── theme/      # Theming and styling
│               ├── utils/          # Utility classes
│               └── viewmodel/      # ViewModels
```

## Key Components

### Activities
- **MainActivity**: Entry point, displays video list
- **VideoPlayerActivity**: Main video playback screen
- **SettingsActivity**: App settings and preferences
- **SearchActivity**: Search functionality
- **CloudStorageActivity**: Cloud storage management
- **PlaylistActivity**: Playlist management

### Core Components
- **PlayerRepository**: Manages video playback state
- **GestureManager**: Handles gesture recognition and processing
- **CloudStorageManager**: Manages cloud storage connections
- **ErrorHandler**: Centralized error handling

## Build Instructions

### Requirements
- Android Studio Arctic Fox or newer
- JDK 17
- Android SDK 34
- Gradle 8.14

### Building
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Dependencies

### Media Playback
- AndroidX Media3 (ExoPlayer) - Video playback engine
- Support for HLS, DASH, SmoothStreaming, RTSP

### UI
- Jetpack Compose - Modern declarative UI
- Material Design 3 - Latest Material components
- Coil - Image loading

### Cloud Storage
- Google Drive API - Google Drive integration
- Play Services Auth - Google authentication

### Other
- Room Database - Local data persistence
- Kotlin Coroutines - Asynchronous programming
- Firebase - Analytics and crash reporting

## Configuration

### Google Drive Setup
1. Create a project in Google Cloud Console
2. Enable Google Drive API
3. Configure OAuth 2.0 credentials
4. Add configuration to the app

### Permissions
The app requires the following permissions:
- `READ_MEDIA_VIDEO` - Access video files
- `INTERNET` - Stream videos and cloud access
- `WAKE_LOCK` - Keep screen on during playback
- `VIBRATE` - Haptic feedback

## Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.