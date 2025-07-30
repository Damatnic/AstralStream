# AstralStream 

**Next-Generation Video Player with Advanced MX Player-Style Gesture Controls**

AstralStream is a powerful Android video player that combines the familiar functionality of MX Player with modern enhancements and advanced gesture controls. Built with Kotlin and Jetpack Compose, it offers a seamless and intuitive video playback experience.

## 🚀 Key Features

### Core Video Player Features
- **Universal format support** - Play virtually any video format
- **Smooth hardware-accelerated playback** using ExoPlayer/Media3
- **Subtitle support** with multiple formats and customization
- **Audio track selection** and audio/video synchronization
- **Picture-in-Picture (PiP)** mode support
- **Background playback** capabilities

### Advanced Gesture Controls (MX Player Style)
- **Long Press Speed Control** - Press and hold center screen for 2x speed (configurable)
- **Progressive Speed Control** - While holding, swipe up/down to adjust speed dynamically
- **Swipe to Seek** - Left/right swipes for fast seeking
- **Double Tap to Seek** - Quick 10-second jumps
- **Pinch to Zoom** - Gesture-based video scaling
- **Brightness & Volume Control** - Edge swipes for quick adjustments

### Smart Speed Memory System
- **Per-Video Speed Memory** - Automatically remembers playback speed for each video
- **Persistent Settings** - All preferences saved using DataStore
- **Clear All Memory** - Option to reset all saved speeds
- **Smart Restoration** - Speeds restored when reopening videos

### Enhanced User Interface
- **Bubble Quick Settings Menu** - MX Player-style floating settings bubble
- **Comprehensive Settings Screen** - Full control over all features
- **Dark Theme Optimized** - Modern Material 3 design
- **Responsive Layout** - Adapts to different screen sizes
- **Accessibility Features** - Screen reader and keyboard navigation support

### Performance & Optimization
- **Smooth Animations** - Spring-based transitions and micro-interactions
- **Haptic Feedback** - Enhanced tactile responses for gestures
- **Memory Efficient** - Optimized for smooth playback on all devices
- **Battery Optimized** - Intelligent power management

## 🛠 Technical Architecture

### Built With
- **Kotlin** - Modern Android development language
- **Jetpack Compose** - Declarative UI framework
- **ExoPlayer/Media3** - Google's robust media playback library
- **DataStore** - Modern preferences and data storage
- **Coroutines & Flow** - Reactive programming patterns
- **Material 3** - Latest Material Design components

### Key Components
- **MxStyleGestureDetector** - Advanced gesture recognition system
- **SimpleEnhancedPlayerViewModel** - State management and business logic
- **SettingsRepository** - Comprehensive settings persistence
- **BubbleQuickSettingsMenu** - Floating settings interface
- **LongPressSpeedOverlay** - Visual feedback for speed control

## 📱 Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 21+ (Android 5.0+)
- Kotlin 1.9.0+
- Gradle 8.0+

### Building from Source
```bash
git clone https://github.com/yourusername/AstralStream.git
cd AstralStream/android
./gradlew assembleDebug
```

### Running Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## 🎯 Phase Implementation Plan

### ✅ Phase 1A: Core Speed Memory System (COMPLETED)
- [x] Clear All Speed Memory functionality
- [x] Settings integration (main + bubble menu)
- [x] Auto-test compilation verification
- [x] All compilation errors fixed

### 🔄 Phase 1B: User Experience Enhancements (NEXT)
- [ ] Toast notifications for speed restoration
- [ ] Visual indicators for speed memory status
- [ ] Enhanced feedback systems

### 📊 Phase 2: Statistics & Analytics (PLANNED)
- [ ] Speed usage statistics
- [ ] Video watching analytics
- [ ] Usage pattern insights
- [ ] Performance metrics

### 🔧 Phase 3: Data Management & Portability (PLANNED)
- [ ] Import/Export speed memory data
- [ ] Backup and restore functionality
- [ ] Cross-device synchronization
- [ ] Data migration tools

### 🤖 Phase 4: AI Features & Smart Enhancements (FUTURE)
- [ ] AI-powered speed recommendations
- [ ] Content-aware speed adjustment
- [ ] Smart gesture learning
- [ ] Personalized optimization

## 🎮 Usage Guide

### Basic Controls
1. **Play/Pause** - Tap center of screen
2. **Seek Forward/Back** - Swipe left or right
3. **Volume Control** - Swipe up/down on right edge
4. **Brightness Control** - Swipe up/down on left edge

### Long Press Speed Control
1. **Activate** - Long press and hold center of screen
2. **Adjust Speed** - While holding, swipe up (faster) or down (slower)
3. **Release** - Let go to return to normal speed
4. **Automatic Memory** - Speed is automatically saved for this video

### Quick Settings
1. **Open Menu** - Tap the floating settings button (top-right)
2. **Navigate** - Tap any setting to access detailed controls
3. **Customize** - Adjust speeds, sensitivity, and preferences
4. **Clear Memory** - Use "Clear All Speed Memory" to reset all saved speeds

## 🔧 Configuration Options

### Long Press Speed Settings
- **Enable/Disable** - Toggle long press speed control
- **Initial Speed** - Set starting speed (0.25x to 8.0x)
- **Progressive Control** - Enable swipe-to-adjust functionality
- **Swipe Sensitivity** - Adjust gesture responsiveness
- **Timeout Duration** - Customize long press detection time

### Speed Memory Settings
- **Per-Video Memory** - Enable/disable automatic speed saving
- **Clear All Data** - Reset all saved video speeds
- **Statistics View** - Monitor usage patterns (Phase 2)

### Gesture Customization
- **Seek Sensitivity** - Adjust swipe-to-seek responsiveness
- **Double Tap Intervals** - Customize seek jump duration
- **Zone Sizes** - Adjust gesture detection areas
- **Haptic Feedback** - Control vibration intensity

## 🔒 Privacy & Security

- **Local Storage Only** - All data stored locally on device
- **No Telemetry** - No usage data sent to external servers
- **Minimal Permissions** - Only essential Android permissions required
- **Open Source** - Full transparency in code and functionality

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **MX Player** - Inspiration for gesture controls and UI design
- **ExoPlayer Team** - Robust media playback foundation
- **Android Jetpack** - Modern development framework
- **Material Design** - Beautiful and functional UI components

## 📞 Support

- **Issues** - [GitHub Issues](https://github.com/yourusername/AstralStream/issues)
- **Discussions** - [GitHub Discussions](https://github.com/yourusername/AstralStream/discussions)
- **Documentation** - [Wiki](https://github.com/yourusername/AstralStream/wiki)

---

**AstralStream** - *Elevating your video experience to the next level* ⭐