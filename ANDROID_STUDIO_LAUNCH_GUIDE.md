# Android Studio Launch Guide for AstralStream

## Step 1: Open Project in Android Studio

1. **Launch Android Studio**
2. **Open Project**:
   - Click "Open" (not "Import")
   - Navigate to: `C:\Astral Projects\Astral-Projects\_Repos\AstralStream\android`
   - Select the `android` folder (not the root AstralStream folder)
   - Click "OK"

## Step 2: Initial Setup & Sync

1. **Wait for Initial Indexing** (may take 2-5 minutes)
2. **Gradle Sync**:
   - Android Studio should automatically start syncing
   - If not, click "Sync Project with Gradle Files" button (elephant icon in toolbar)
   - Or go to: File → Sync Project with Gradle Files

## Step 3: Fix Common Issues

### If you see "SDK not found" error:
1. File → Project Structure (Ctrl+Alt+Shift+S)
2. SDK Location → Android SDK location
3. Set to your Android SDK path (usually `C:\Users\[YourName]\AppData\Local\Android\Sdk`)

### If you see Gradle sync failures:
1. File → Invalidate Caches and Restart
2. After restart, let it re-index
3. Clean project: Build → Clean Project

## Step 4: Configure Run Configuration

1. **Check/Create Run Configuration**:
   - Look at top toolbar for "app" configuration
   - If missing, click "Add Configuration" → "+" → "Android App"
   - Module: app
   - Deploy: Default APK

2. **Select Device**:
   - Physical device: Enable USB debugging and connect
   - Emulator: Click AVD Manager and create/start an emulator

## Step 5: Build and Run

1. **First Build** (Important!):
   ```
   Build → Clean Project
   Build → Rebuild Project
   ```

2. **Run the App**:
   - Click green "Run" button (Shift+F10)
   - Or: Run → Run 'app'

## Step 6: Troubleshooting

### If build fails with Hilt/Kapt errors:
```gradle
// In Android Studio Terminal:
./gradlew clean
./gradlew --stop
./gradlew assembleDebug --info
```

### If you see "Could not load module" error:
1. Delete these folders:
   - `.gradle` (in project root)
   - `build` (in project root and app folder)
2. Restart Android Studio
3. Sync again

### Memory issues:
1. Help → Change Memory Settings
2. Set to at least 4096 MB
3. Restart Android Studio

## Quick Checklist:
- [ ] Android Studio opened the `android` folder
- [ ] Gradle sync completed successfully
- [ ] No red errors in the IDE
- [ ] Run configuration shows "app"
- [ ] Device/Emulator is connected
- [ ] Build completed without errors

## Keyboard Shortcuts:
- **Sync Project**: Ctrl+Shift+O
- **Build Project**: Ctrl+F9
- **Run App**: Shift+F10
- **Clean Project**: No default (use menu)
- **Invalidate Caches**: File menu only

## Expected First Launch:
When the app launches successfully, you should see:
1. Splash screen with AstralStream logo
2. Main activity with video library
3. Permission requests for storage access
4. Empty library (add videos to test)

Good luck! 🚀