# 📱 Installing AstralStream on Your Phone

## 🚀 Quick Install Guide

### Method 1: Direct USB Transfer (Recommended)

1. **Enable Developer Options on your phone**:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back to Settings → Developer Options
   - Enable "USB Debugging"

2. **Connect your phone to PC via USB**:
   - Select "File Transfer" mode on your phone
   - Allow USB debugging if prompted

3. **Install via ADB** (if you have ADB installed):
   ```bash
   cd "C:\Astral Projects\Astral-Projects\_Repos\AstralStream\android"
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

### Method 2: Copy APK to Phone

1. **Copy the APK file**:
   - Location: `C:\Astral Projects\Astral-Projects\_Repos\AstralStream\android\app\build\outputs\apk\debug\app-debug.apk`
   - Copy to your phone's Downloads folder via USB

2. **Install from phone**:
   - Open Files/File Manager app
   - Navigate to Downloads
   - Tap on `app-debug.apk`
   - Allow "Install from unknown sources" if prompted
   - Tap "Install"

### Method 3: Upload to Cloud (Google Drive/Dropbox)

1. **Upload APK**:
   - Upload `app-debug.apk` to your Google Drive/Dropbox
   - Share the file or make it accessible

2. **Download on phone**:
   - Open Drive/Dropbox app on phone
   - Download the APK
   - Open and install

### Method 4: Local Network Transfer

1. **Use a file sharing app**:
   - Apps like ShareIt, Send Anywhere, or LocalSend
   - Or use Windows nearby sharing if supported

2. **Email to yourself**:
   - Attach APK to email (if under 25MB limit)
   - Open on phone and download

## ⚙️ Phone Settings Required

### Before Installation:
1. **Go to Settings → Security**
2. **Enable "Unknown sources"** or "Install unknown apps"
3. **For specific app** (like Chrome or Files):
   - Settings → Apps → Chrome → Install unknown apps → Allow

### For Android 8.0+:
- You'll be prompted per-app to allow installations
- Grant permission when asked

## 🎯 ADB Installation (Fastest Method)

If you have Android SDK/Platform Tools:

```bash
# Check if device is connected
adb devices

# Install the APK
adb install "C:\Astral Projects\Astral-Projects\_Repos\AstralStream\android\app\build\outputs\apk\debug\app-debug.apk"

# If app is already installed, use -r flag
adb install -r "C:\Astral Projects\Astral-Projects\_Repos\AstralStream\android\app\build\outputs\apk\debug\app-debug.apk"
```

## 📋 Quick ADB Setup (If Needed)

1. **Download Platform Tools**:
   - https://developer.android.com/studio/releases/platform-tools
   - Extract to C:\platform-tools

2. **Add to PATH** or navigate to folder:
   ```bash
   cd C:\platform-tools
   adb install "C:\Astral Projects\Astral-Projects\_Repos\AstralStream\android\app\build\outputs\apk\debug\app-debug.apk"
   ```

## ✅ After Installation

1. **Find the app**: Look for "AstralStream" icon
2. **First launch**:
   - Grant storage permissions
   - Allow notifications (optional)
   - Configure settings

3. **Test browser integration**:
   - Open Chrome
   - Go to any video
   - Long press → Open with → AstralStream

## 🔧 Troubleshooting

### "App not installed" error:
- Enable unknown sources
- Check available storage (need ~100MB)
- Uninstall previous version if exists
- Try ADB with -r flag

### Can't find APK on phone:
- Check Downloads folder
- Use file manager's search
- Check notification panel for download

### ADB not recognized:
- Install Android Platform Tools
- Enable USB debugging
- Try different USB port/cable
- Install phone drivers if needed

## 📱 Permissions Needed

The app will request:
- **Storage**: To play local videos
- **Network**: For streaming
- **Notifications**: For background playback (optional)

Grant all for full functionality!

---

🎉 **Enjoy AstralStream!** Open any video in your browser and use "Open with" for the enhanced experience!