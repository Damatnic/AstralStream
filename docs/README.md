# 🚀 Astral Player (Aplay) - Enhanced Version 2.0
# AstralVu Player

A next-generation media player with advanced features, AI capabilities, and a modern UI.

## 🚀 Features

### Core Functionality
- **Multi-format Playback**: Support for MP4, WebM, HLS, DASH and more
- **Adaptive Streaming**: Automatic quality adjustment based on network conditions
- **Responsive Design**: Works perfectly on mobile, tablet, and desktop
- **Cache Management**: Smart caching for offline playback and reduced bandwidth usage

### User Interface
- **Modern Design**: Clean, intuitive controls with customizable themes
- **Gesture Controls**: Swipe, pinch, and tap gestures for natural interaction
- **Picture-in-Picture**: Continue watching while using other apps
- **Theater Mode**: Distraction-free viewing experience

### Advanced Features
- **AI-Generated Subtitles**: Automatic speech recognition in multiple languages
- **Smart Chapters**: Automatic scene detection and chapter creation
- **Video Effects**: Real-time filters and adjustments (brightness, contrast, etc.)
- **Accessibility**: High contrast mode, keyboard shortcuts, screen reader support

### Customization
- **Theme System**: Multiple themes with customization options
- **Keyboard Shortcuts**: Configurable shortcuts for power users
- **Settings Management**: Persistent user preferences

## 📋 Requirements

### Web Version
- Modern browser with HTML5 and JavaScript support
- Chrome, Firefox, Safari, or Edge (latest 2 versions)

### Android Version
- Android 6.0 (Marshmallow) or higher
- 2GB RAM minimum

## 🔧 Installation

### Web

```bash
# Clone the repository
git clone https://github.com/yourusername/astral-player.git

# Navigate to project directory
cd astral-player

# Install dependencies
npm install

# Build the project
npm run build

# Start development server
npm run dev
```

### Android

```bash
# Build Android app
npm run build:android

# Run on connected device
npx cap run android
```

## 🎮 Usage

### Basic Implementation

```html
<div id="player-container" class="astralvu-player-container"></div>

<script>
  const player = new AstralVuPlayer('player-container', {
    sources: [{
      src: 'https://example.com/video.mp4',
      type: 'video/mp4'
    }],
    autoplay: false,
    muted: false,
    poster: 'path/to/poster.jpg'
  });
</script>
```

### Advanced Configuration

```javascript
const player = new AstralVuPlayer('player-container', {
  sources: [{
    src: 'https://example.com/video.mp4',
    type: 'video/mp4',
    label: '720p'
  }, {
    src: 'https://example.com/video-hd.mp4',
    type: 'video/mp4',
    label: '1080p'
  }],
  subtitles: [{
    src: 'path/to/subtitles-en.vtt',
    language: 'en',
    label: 'English'
  }, {
    src: 'path/to/subtitles-es.vtt',
    language: 'es',
    label: 'Spanish'
  }],
  autoplay: false,
  muted: false,
  loop: false,
  poster: 'path/to/poster.jpg',
  preload: 'auto',
  volume: 0.8,
  speed: 1.0,
  defaultSubtitleLanguage: 'en',
  theme: 'cosmic'
});

// Control methods
player.play();
player.pause();
player.seek(30); // seek to 30 seconds
player.setVolume(0.5); // 50% volume
player.setPlaybackRate(1.5); // 1.5x speed
```

## 🧩 API Reference

### Constructor

```javascript
const player = new AstralVuPlayer(elementId, options);
```

| Parameter | Type | Description |
|-----------|------|-------------|
| elementId | string | ID of container element |
| options | object | Configuration options |

### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| sources | array | [] | Array of source objects |
| subtitles | array | [] | Array of subtitle objects |
| autoplay | boolean | false | Auto-start playback |
| muted | boolean | false | Start with audio muted |
| loop | boolean | false | Loop playback |
| poster | string | '' | URL to poster image |
| preload | string | 'metadata' | Preload behavior ('none', 'metadata', 'auto') |
| volume | number | 1.0 | Initial volume (0-1) |
| speed | number | 1.0 | Initial playback rate |
| theme | string | 'cosmic' | UI theme name |

### Methods

| Method | Parameters | Description |
|--------|------------|-------------|
| play() | | Start playback |
| pause() | | Pause playback |
| seek(time) | time: number | Seek to specified time (seconds) |
| setVolume(volume) | volume: number | Set volume (0-1) |
| setPlaybackRate(rate) | rate: number | Set playback speed |
| toggleFullscreen() | | Toggle fullscreen mode |
| toggleMute() | | Toggle audio mute |
| loadSource(source) | source: object | Load new media source |

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Coding Standards
- Follow ESLint configuration
- Write unit tests for new functionality

### Pull Request Guidelines
- Describe changes clearly
- Include screenshots for UI changes
- Ensure all tests pass
- Update documentation if needed

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **ExoPlayer Team**: For the robust media playback framework
- **Jetpack Compose Team**: For the modern UI toolkit
- **Material Design**: For the design system and components
- **Android Community**: For continuous support and feedback

## 📞 Support

If you encounter any issues or have questions, please [open an issue](https://github.com/yourusername/astral-player/issues) on GitHub.

---

Developed with ❤️ by the AstralVu Team
A sophisticated multi-platform video player application with AI-powered backend, featuring both web (AstralVu) and Android components with advanced streaming capabilities and intelligent model routing.

## 🌟 Features

### 📱 Multi-Platform Support
- **Web Player (AstralVu)**: Advanced HTML5 video player with HLS/DASH streaming
- **Android App**: Native Kotlin/Compose application with ExoPlayer integration  
- **AI Backend**: Python-based intelligent model router with escalation system
- **Capacitor Bridge**: Seamless web-to-native communication

### 🎥 Enhanced Video Player Features
- **Universal Format Support**: MP4, AVI, MKV, WebM, MOV, 3GP, FLV, WMV, M4V, TS
- **Advanced Streaming**: HLS (.m3u8), DASH (.mpd), HTTP/HTTPS with adaptive bitrate
- **Quality Control**: Automatic and manual quality switching (360p-4K)
- **Playback Speed**: Variable speed control (0.25x to 2x) with smooth transitions
- **Picture-in-Picture**: Modern PiP support for multitasking
- **Theater Mode**: Immersive full-width viewing experience
- **Subtitle Support**: Closed captioning and multiple subtitle tracks
- **Audio Tracks**: Multiple audio track selection and switching

### 🎨 Enhanced UI/UX
- **Futuristic Design**: Holographic interface with cosmic theme and animations
- **Responsive Layout**: Optimized for mobile, tablet, and desktop viewing
- **Smooth Animations**: Fluid transitions with spring physics and easing
- **Dark Theme**: Eye-friendly dark interface with customizable accent colors
- **Advanced Gestures**: Enhanced touch controls with haptic feedback
- **Smart Controls**: Auto-hide controls with intelligent timing

### 🤖 AI Backend Improvements
- **Enhanced CLI Interface**: Rich command-line experience with colors and formatting
- **Web API Dashboard**: RESTful API with built-in monitoring interface
- **Advanced Configuration**: Comprehensive settings with real-time validation
- **Statistics Tracking**: Real-time performance monitoring and analytics
- **Intelligent Caching**: Smart response caching with TTL management
- **Model Escalation**: Automatic quality-based model switching and fallback

### Advanced Controls
- **Smart Gesture Detection**: 
  - Left side swipe: Brightness control
  - Right side swipe: Volume control  
  - Center horizontal swipe: Video seeking
  - Double tap: Play/pause toggle
  - Single tap: Show/hide controls

- **Playback Controls**:
  - Variable speed playback (0.5x to 2.0x)
  - Precise seeking with visual feedback
  - Auto-hide controls with customizable timeout
  - Fullscreen mode support

### User Interface
- **Modern Design**: Material Design 3 components
- **Responsive Layout**: Adapts to different screen sizes
- **Accessibility**: Full screen reader support
- **Visual Feedback**: Smooth animations and transitions

## 🏗️ Architecture

### Technology Stack
- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose
- **Media Player**: ExoPlayer (Media3)
- **Architecture**: MVVM with Compose
- **Dependency Injection**: Hilt
- **Build System**: Gradle with Kotlin DSL

### Project Structure
```
app/src/main/java/com/astralplayer/nextplayer/
├── MainActivity.kt                    # Main entry point
├── feature/player/
│   ├── VideoPlayerActivity.kt         # Video player activity
│   ├── PlayerHelpers.kt              # Utility functions
│   ├── ui/
│   │   ├── CompleteVideoPlayer.kt    # Main player composable
│   │   ├── BasicControlsOverlay.kt   # Player controls UI
│   │   ├── GestureOverlay.kt         # Gesture detection
│   │   └── GestureSettingsScreen.kt  # Settings UI
│   └── gestures/
│       ├── GestureTypes.kt           # Gesture definitions
│       └── EnhancedGestureDetection.kt # Advanced gestures
├── ui/theme/                         # App theming
└── utils/                           # Utilities and helpers
    ├── Logger.kt                    # Centralized logging
    └── ErrorHandler.kt              # Error management
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9.22+
- Gradle 8.13+

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/astral-player.git
   cd astral-player
   ```

2. Open in Android Studio:
   - File → Open → Select the project folder
   - Wait for Gradle sync to complete

3. Build and run:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

### Building APK
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

## 📱 Usage

### Opening Videos
1. **File Browser**: Tap "Open File" to browse local videos
2. **URL Input**: Tap "Open URL" to play streaming content
3. **Intent Handling**: Open videos from other apps via "Open With"

### Gesture Controls
- **Volume**: Swipe up/down on the right side of the screen
- **Brightness**: Swipe up/down on the left side of the screen
- **Seeking**: Swipe left/right in the center area
- **Play/Pause**: Double-tap anywhere on the screen
- **Controls**: Single tap to show/hide player controls

### Settings
- **Playback Speed**: Choose from 0.5x to 2.0x speed
- **Video Quality**: Select resolution (when available)
- **Subtitles**: Enable/disable and configure subtitles
- **Audio Tracks**: Switch between available audio tracks

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Test Coverage
- **PlayerHelpers**: Time formatting, seek calculations, gesture detection
- **UI Components**: MainActivity, player controls, gesture overlays
- **Error Handling**: Network errors, file access, playback issues

## 🔧 Configuration

### Supported Formats
- **Video**: MP4, MKV, WebM, MOV, AVI, FLV, WMV, 3GP, M4V
- **Streaming**: HLS (m3u8), DASH, Progressive HTTP
- **Codecs**: H.264, H.265, VP8, VP9, AV1 (device dependent)

### Permissions
- `INTERNET`: For streaming content
- `READ_EXTERNAL_STORAGE`: For local file access
- `WRITE_SETTINGS`: For brightness control
- `WAKE_LOCK`: To keep screen on during playback

## 🐛 Troubleshooting

### Common Issues
1. **Video won't play**: Check format support and file integrity
2. **Gestures not working**: Ensure proper touch area and sensitivity
3. **Brightness control fails**: Grant system settings permission
4. **Network streaming issues**: Check internet connection and URL validity

### Debug Logging
Enable detailed logging by setting log level in `Logger.kt`:
```kotlin
Logger.logPlayerEvent("Event", "Details")
Logger.logGestureEvent("Gesture", "Value")
```

## 🤝 Contributing
# Next Player - Advanced Video Player with AI Features

## 🔍 Overview

Next Player is a state-of-the-art Android video player built with Jetpack Compose and ExoPlayer, featuring advanced AI capabilities for an enhanced viewing experience. The player combines cutting-edge user interface design with powerful features like gesture controls, AI-generated subtitles, and adaptive playback settings.

## ✨ Features

### 🎮 Advanced Controls
- Intuitive gesture controls for volume, brightness, and seeking
- Double-tap to seek forward/backward
- Pinch to zoom
- Customizable gestures

### 🤖 AI Features
- **AI-generated subtitles in multiple languages**
  - Uses free tier of various speech-to-text models
  - Supports 20+ languages
  - Real-time generation with progress tracking
- Adaptive playback behavior that learns from user preferences
- Smart contrast and brightness adjustment

### 📺 Playback Features
- Supports all major video formats
- HLS and DASH streaming support
- Picture-in-picture mode
- Background playback
- A-B repeat
- Playback speed control

### 🌈 UI Features
- Modern Material 3 design
- Dark and light themes
- Customizable UI elements
- Smooth animations

## 🔧 AI Subtitle Generation

Next Player can automatically generate subtitles for any video using free AI speech-to-text models. 

### How It Works
1. Audio is extracted from the video file
2. The audio is sent to one of the supported free AI models
3. The resulting transcription is formatted into proper SRT subtitles
4. Subtitles are saved and can be reused without regenerating

### Supported AI Models
- **Whisper Tiny**: Fast but less accurate (via HuggingFace)
- **Whisper Small**: Good balance of speed and accuracy (via Cloudflare AI)
- **Whisper Medium**: More accurate but slower (via AssemblyAI)

### Language Support
The subtitle generator supports over 20 languages including English, Spanish, French, German, Italian, Japanese, Korean, Chinese, Russian, and more.

## 📋 Requirements

- Android 9.0+ (API level 28+)
- Java SDK 21
- Gradle 8.0+

## 🛠 Installation

1. Clone the repository
2. Open the project in Android Studio
3. Build and run on your device or emulator

## 🚀 Getting Started

1. Open the app and select a video to play
2. Use gestures to control playback:
   - Swipe left/right to seek
   - Swipe up/down on right side to adjust volume
   - Swipe up/down on left side to adjust brightness
   - Double-tap left/right to seek backward/forward
   - Pinch to zoom
3. Tap the subtitle button to generate AI subtitles
4. Configure subtitle appearance in settings

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **ExoPlayer Team**: For the robust media playback framework
- **Jetpack Compose Team**: For the modern UI toolkit
- **Material Design**: For the design system and components
- **Android Community**: For continuous support and feedback
### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make changes and test thoroughly
4. Commit changes: `git commit -m 'Add amazing feature'`
5. Push to branch: `git push origin feature/amazing-feature`
6. Open a Pull Request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add documentation for public APIs
- Write unit tests for new functionality

### Pull Request Guidelines
- Describe changes clearly
- Include screenshots for UI changes
- Ensure all tests pass
- Update documentation if needed

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
# AstralVu Player

A modern, feature-rich media player with advanced AI capabilities and smooth user experience.

## 🚀 Features

- **Adaptive Streaming** - Automatically adjusts quality based on network conditions
- **Smart Gestures** - Intuitive gesture controls for playback, volume, and brightness
- **Advanced Video Effects** - Apply real-time filters to enhance the viewing experience
- **AI-Powered Subtitles** - Generate and customize subtitles with advanced AI
- **Smart Chapters** - Automatically detect scenes and navigate through content
- **Multiple Themes** - Choose from various visual themes with futuristic designs
- **Accessibility Focus** - Designed with inclusive features for all users

## 📋 Requirements

- Node.js 16+ and npm
- For Android: Android Studio, JDK 11+, Gradle

## 🛠️ Setup & Installation

### Web Version

1. Clone the repository
2. Install dependencies:
   ```
   npm install
   ```
3. Fix package versions (if needed):
   ```
   package-update.bat
   ```
4. Start the development server:
   ```
   npm run dev
   ```

### Android Version

1. Complete the web setup steps first
2. Run the Android build:
   ```
   cd android
   gradlew clean assembleDebug
   ```

## 🧪 Testing

Run the test build script to build and test both versions:

```
test_build.bat
```

This will:
1. Install dependencies
2. Build web assets
3. Launch a test server for the web version
4. Build the Android debug APK

## 📦 Build for Production

### Web

```
npm run build
```

The built files will be in the `dist` folder.

### Android

```
cd android
gradlew assembleRelease
```

The APK will be in `android/app/build/outputs/apk/release/`.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- **ExoPlayer Team**: For the robust media playback framework
- **Jetpack Compose Team**: For the modern UI toolkit
- **Material Design**: For the design system and components
- **Android Community**: For continuous support and feedback
## 🙏 Acknowledgments

- **ExoPlayer Team**: For the robust media playback framework
- **Jetpack Compose Team**: For the modern UI toolkit
- **Material Design**: For the design system and components
- **Android Community**: For continuous support and feedback

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/astral-player/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/astral-player/discussions)
- **Email**: support@astralplayer.com

---

**Astral Player** - Elevating your video viewing experience with advanced controls and seamless playback.