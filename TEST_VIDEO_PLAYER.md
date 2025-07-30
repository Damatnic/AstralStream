# Video Player Testing Guide

## 🎯 Testing the Astral Video Player

This guide explains how to test the video player functionality using the TestVideoPlayerActivity.

## ✅ Current Status
- ✅ Project builds successfully
- ✅ PlayerViewModel integrated with ExoPlayer
- ✅ SimpleVideoPlayerScreen UI implemented
- ✅ Basic gesture detection system in place
- ✅ TestVideoPlayerActivity created for testing

## 🧪 Testing Methods

### Method 1: Test UI Without Video
The TestVideoPlayerActivity can be launched without a video to test the UI components:

```bash
# Launch the test activity (UI only)
adb shell am start -n com.astralplayer.nextplayer/.TestVideoPlayerActivity
```

### Method 2: Test with Video File
To test with an actual video file, provide a video URI:

```bash
# Test with a local video file
adb shell am start -n com.astralplayer.nextplayer/.TestVideoPlayerActivity \
  --es video_uri "file:///storage/emulated/0/Movies/sample.mp4"

# Test with a network video
adb shell am start -n com.astralplayer.nextplayer/.TestVideoPlayerActivity \
  --es video_uri "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
```

## 🎮 Available Features to Test

### Basic Playback Controls
- ✅ Play/Pause button
- ✅ Seek bar (progress slider)
- ✅ Time display (current/total)
- ✅ Loading indicator
- ✅ Error handling display

### Gesture Controls
- ✅ Single tap - Toggle controls visibility
- ✅ Double tap - Seek forward/backward (left/right side)
- ✅ Long press - Variable speed seeking (implemented)

### Advanced Features (Implemented but need testing)
- 🔄 Volume control gestures
- 🔄 Brightness control gestures
- 🔄 Zoom gestures
- 🔄 Screen lock functionality
- 🔄 Picture-in-picture mode
- 🔄 Subtitle support
- 🔄 Audio track selection
- 🔄 Playback speed control
- 🔄 A-B repeat functionality

## 🐛 Known Issues & Next Steps

### Immediate Testing Priorities
1. **Verify ExoPlayer Integration** - Ensure videos actually play
2. **Test Gesture Accuracy** - Verify gesture detection works on real devices
3. **Test Error Handling** - Try invalid video URIs
4. **Test Performance** - Check memory usage and smooth playback

### Next Implementation Steps
1. Complete gesture system integration
2. Add proper error handling and loading states
3. Implement settings persistence
4. Add video information overlay
5. Test on multiple devices and screen sizes

## 📱 Device Testing Checklist

- [ ] Test on phone (portrait/landscape)
- [ ] Test on tablet
- [ ] Test with different video formats (MP4, MKV, AVI)
- [ ] Test with different resolutions (720p, 1080p, 4K)
- [ ] Test gesture sensitivity
- [ ] Test performance with long videos
- [ ] Test network video streaming
- [ ] Test offline video playback

## 🔧 Development Notes

The TestVideoPlayerActivity uses:
- `PlayerViewModel` for state management
- `SimpleVideoPlayerScreen` for UI
- ExoPlayer for video playback
- Compose UI for modern Android UI

To modify or extend testing:
1. Edit `TestVideoPlayerActivity.kt` for different test scenarios
2. Modify `SimpleVideoPlayerScreen.kt` for UI changes
3. Update `PlayerViewModel.kt` for functionality changes

---

*Last Updated: 2025-07-25*
*Status: Ready for testing*