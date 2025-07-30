# 🚀 AstralStream - Advanced Video Player

## ✨ Overview
AstralStream is a next-generation Android video player that combines powerful streaming capabilities with an intuitive user interface. Built with modern Android technologies and optimized for both local and streaming content.

## 🎯 Key Features

### 📱 Core Video Playback
- **Multi-format Support**: MP4, MKV, AVI, MOV, WEBM, FLV, 3GP, and more
- **Advanced Codec Support**: Hardware-accelerated decoding with fallback options
- **Gesture Controls**: Swipe for volume, brightness, and seek
- **Picture-in-Picture**: Continue watching while using other apps
- **Playback Speed Control**: 0.25x to 4.0x with fine-tuning
- **A-B Repeat**: Loop specific sections
- **Subtitle Support**: Multiple formats with customizable styling

### 🌐 Streaming Capabilities
- **HLS Support**: Adaptive bitrate streaming for .m3u8 files
- **DASH Support**: Dynamic streaming for .mpd files
- **RTMP/RTSP**: Live streaming protocols
- **Network Optimization**: Automatic quality adjustment based on connection
- **Browser Integration**: "Open with" support from Chrome, Firefox, etc.
- **Adult Content Optimization**: Enhanced codec support for streaming sites

### ☁️ Cloud Storage Integration
- **Google Drive**: Browse and stream videos from Drive
- **Dropbox**: Direct streaming from Dropbox storage
- **OneDrive**: Microsoft cloud integration (framework ready)
- **Sync Management**: Track sync status and manage files
- **Offline Caching**: Download for offline viewing

### 🎨 User Interface
- **Material You Design**: Dynamic theming based on system colors
- **Dark Mode**: OLED-friendly pure black theme
- **Gesture Overlays**: Visual feedback for all gestures
- **Customizable Controls**: Position controls where you want
- **Quick Settings**: Bubble-style quick access menu
- **Haptic Feedback**: Tactile response for interactions

### 🤖 Smart Features
- **AI Subtitle Generation**: Framework for automatic subtitle creation
- **Scene Detection**: Identify key moments in videos
- **Smart Resume**: Remember position across app restarts
- **Playlist Management**: Create and organize video collections
- **Watch History**: Track viewing progress
- **Advanced Search**: Find videos by name, date, or metadata

### 📡 Casting & Sharing
- **Chromecast Support**: Cast to TV with full controls
- **Network Streaming**: Stream to other devices
- **Social Sharing**: Share video links or clips
- **Export Options**: Save frames or create GIFs

### 🔒 Privacy & Security
- **Biometric Lock**: Fingerprint/face protection for sensitive content
- **Private Mode**: Hide specific videos or folders
- **Secure Storage**: Encrypted preferences
- **No Analytics**: Privacy-focused, no tracking

### ⚡ Performance
- **Hardware Acceleration**: GPU-powered decoding
- **Adaptive Buffering**: Smart buffer management
- **Memory Optimization**: Efficient resource usage
- **Battery Friendly**: Optimized power consumption
- **Background Playback**: Audio-only mode for music videos

### 🎛️ Advanced Settings
- **Decoder Priority**: Choose hardware/software decoders
- **Audio Passthrough**: Bitstream for surround sound
- **Screen Orientation**: Per-video orientation lock
- **Gesture Sensitivity**: Customize swipe responsiveness
- **Network Limits**: Set streaming quality caps

## 🛠️ Technical Implementation

### Architecture
- **MVVM Pattern**: Clean architecture with ViewModels
- **Hilt DI**: Dependency injection for modularity
- **Kotlin Coroutines**: Async operations and flows
- **Room Database**: Local data persistence
- **Media3/ExoPlayer**: Core playback engine

### Key Technologies
- **Jetpack Compose**: Modern declarative UI
- **Material Design 3**: Latest design guidelines
- **Firebase**: Crashlytics and analytics (optional)
- **OkHttp**: Network operations
- **Coil**: Image loading and caching

### Browser Integration
The app registers as a video handler for:
- Direct video URLs (.mp4, .mkv, .m3u8, etc.)
- Streaming sites (YouTube, Vimeo, etc.)
- Adult content platforms (optimized codecs)
- Custom URL schemes for deep linking

### Streaming Optimization
- **Adaptive Bitrate**: Automatic quality switching
- **Preloading**: Smart buffering based on network
- **Error Recovery**: Automatic retry mechanisms
- **Bandwidth Monitoring**: Real-time network analysis

## 📦 APK Information
- **Size**: ~31 MB
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: Universal (arm64-v8a, armeabi-v7a, x86, x86_64)

## 🚀 Getting Started

### Installation
1. Enable "Unknown sources" in Android settings
2. Download the APK
3. Install and grant necessary permissions
4. Start streaming!

### First Launch
- Grant storage permission for local videos
- Optional: Connect cloud storage accounts
- Customize gesture controls in settings
- Enable "Open with" in your browser

### Browser Streaming
1. Open any video URL in Chrome/Firefox
2. Tap the video or menu
3. Select "Open with AstralStream"
4. Enjoy enhanced playback!

## 🎯 Use Cases

### Local Video Library
- Organize personal video collection
- Create themed playlists
- Track watching progress
- Quick resume from where you left off

### Streaming Enhancement
- Better codec support than browsers
- No ads or popups
- Background playback
- Download for offline viewing

### Educational Content
- Variable speed for lectures
- A-B repeat for language learning
- Subtitle support for accessibility
- Note-taking with bookmarks

### Entertainment
- Binge-watch with auto-play
- Cast to TV for group viewing
- Picture-in-picture multitasking
- Immersive fullscreen experience

## 🔮 Future Enhancements
- AI-powered video enhancement
- Real-time translation
- Social viewing parties
- Advanced video editing
- Cloud transcoding
- VR/360° video support

## 🤝 Contributing
AstralStream is built with modern Android development practices and welcomes contributions. The codebase is organized for easy navigation and extension.

## 📄 License
This project enhances the video playback experience on Android while respecting content rights and user privacy.

---

**AstralStream** - Your gateway to an enhanced video experience on Android! 🌟