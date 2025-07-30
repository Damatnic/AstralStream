# 🧪 AstralStream Testing Guide

## 🚀 Quick Start Testing

### 1. Installation Test
- [ ] APK installs without errors
- [ ] App launches successfully
- [ ] Permissions dialog appears
- [ ] Grant storage permissions

### 2. Basic Playback Test
- [ ] Open app and browse local videos
- [ ] Play a video file
- [ ] Test pause/play controls
- [ ] Test seek bar functionality
- [ ] Verify audio works

## 📱 Core Features Testing

### Gesture Controls
- [ ] **Swipe Up/Down (Right)**: Volume control
- [ ] **Swipe Up/Down (Left)**: Brightness control
- [ ] **Swipe Left/Right**: Seek backward/forward
- [ ] **Double Tap Left**: Skip backward
- [ ] **Double Tap Right**: Skip forward
- [ ] **Long Press**: Speed control options

### Video Formats
Test each format if available:
- [ ] MP4 file
- [ ] MKV file
- [ ] AVI file
- [ ] WEBM file
- [ ] MOV file

### Streaming Tests
- [ ] **HLS Stream**: 
  ```
  https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8
  ```
- [ ] **MP4 Stream**:
  ```
  https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
  ```

## 🌐 Browser Integration Testing

### Chrome Browser Test
1. Open Chrome and navigate to a video URL
2. Long-press on the video
3. Select "Open with" → "AstralStream"
4. Verify video opens in app

### Test URLs
- **YouTube**: Open any YouTube video
- **Vimeo**: https://vimeo.com/
- **Direct MP4**: Any .mp4 URL

### Adult Content Test (Optional)
1. Navigate to supported site
2. Play video
3. Use "Open with" option
4. Verify enhanced codec support

## ⚙️ Advanced Features

### Picture-in-Picture
- [ ] Start video playback
- [ ] Press home button
- [ ] Verify PiP window appears
- [ ] Test PiP controls

### Cast/Chromecast
- [ ] Connect to same WiFi as Chromecast
- [ ] Look for cast icon
- [ ] Select device
- [ ] Verify playback on TV

### Cloud Storage
- [ ] Open Settings → Cloud Storage
- [ ] Try connecting Google Drive
- [ ] Browse cloud files
- [ ] Stream video from cloud

### Playlist Management
- [ ] Create new playlist
- [ ] Add videos to playlist
- [ ] Play playlist
- [ ] Test shuffle/repeat modes

## 🔧 Settings Testing

### Video Settings
- [ ] Change decoder priority
- [ ] Modify default playback speed
- [ ] Test resume on startup
- [ ] Change video zoom mode

### Gesture Settings
- [ ] Enable/disable gestures
- [ ] Adjust sensitivity
- [ ] Test haptic feedback
- [ ] Customize gesture zones

### UI Settings
- [ ] Toggle dark mode
- [ ] Change control positions
- [ ] Test fullscreen options
- [ ] Verify theme changes

## 🐛 Common Issues & Solutions

### Video Won't Play
1. Check codec support in settings
2. Try switching decoder (hardware/software)
3. Verify file isn't corrupted
4. Check storage permissions

### Browser Integration Not Working
1. Clear browser defaults
2. Ensure AstralStream is set as video handler
3. Try different browser
4. Check intent filters in settings

### Streaming Issues
1. Check internet connection
2. Try lower quality stream
3. Enable adaptive streaming
4. Clear app cache

### Performance Problems
1. Close background apps
2. Enable hardware acceleration
3. Lower video quality
4. Disable advanced features

## 📊 Performance Benchmarks

### Expected Performance
- **App Launch**: < 2 seconds
- **Video Load**: < 3 seconds (local)
- **Seek Response**: < 500ms
- **Gesture Response**: Immediate
- **Memory Usage**: < 200MB typical

### Battery Usage
- **Local Playback**: ~5% per hour
- **Streaming**: ~8% per hour
- **Background Audio**: ~3% per hour

## ✅ Feature Checklist

### Must Test
- [x] Basic video playback
- [x] Gesture controls
- [x] Browser integration
- [x] Streaming support
- [ ] Your specific use case

### Nice to Have
- [ ] Cloud storage
- [ ] Chromecast
- [ ] Playlists
- [ ] Advanced settings

## 📝 Reporting Issues

When reporting issues, include:
1. Android version
2. Device model
3. Video format/URL
4. Steps to reproduce
5. Error messages (if any)
6. Screenshots/recordings

## 🎉 Success Criteria

The app is working correctly when:
- ✅ Videos play smoothly
- ✅ Gestures respond accurately
- ✅ Browser integration works
- ✅ No crashes during normal use
- ✅ Settings persist between launches

---

Happy Testing! 🚀 If you encounter any issues, check the troubleshooting section or report them for fixes.