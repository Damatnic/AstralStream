# Firebase Setup Guide for AstralStream

## What You Need from Firebase

Based on the project configuration, AstralStream uses the following Firebase services:
- **Firebase Analytics** - For tracking app usage and events
- **Firebase Crashlytics** - For crash reporting and stability monitoring

## How to Get Firebase Configuration

### Step 1: Create a Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" or "Add project"
3. Enter project name: "AstralStream" (or your preferred name)
4. Follow the setup wizard (you can disable Google Analytics if not needed)

### Step 2: Add Android App to Firebase
1. In your Firebase project, click the Android icon or "Add app" > "Android"
2. Register your app with these details:
   - **Android package name**: `com.astralplayer.nextplayer`
   - **App nickname**: AstralStream (optional)
   - **Debug signing certificate SHA-1**: (optional for now, required for some features)

### Step 3: Download google-services.json
1. After registering, download the `google-services.json` file
2. Place it in: `android/app/google-services.json`

### Step 4: Enable Required Services
1. **For Analytics** (if needed):
   - Go to Analytics in Firebase Console
   - It's usually enabled by default
   
2. **For Crashlytics** (if needed):
   - Go to Crashlytics in Firebase Console
   - Click "Enable Crashlytics"

## Current API Keys Setup

Your API keys are now stored in:
`android/app/src/main/assets/api_config.properties`

This file contains:
- **GEMINI_API_KEY**: For Google AI Studio (Gemini) integration
- **ANTHROPIC_API_KEY**: For Claude AI integration

## How to Use the API Keys in Code

```kotlin
// Initialize in Application class (already done)
ApiKeyManager.initialize(context)

// Get API keys when needed
val geminiKey = ApiKeyManager.getGeminiApiKey()
val anthropicKey = ApiKeyManager.getAnthropicApiKey()

// Check if keys are available
if (ApiKeyManager.hasRequiredApiKeys()) {
    // Proceed with AI features
}
```

## Security Best Practices

1. **Add to .gitignore**: Make sure to add the API config file to .gitignore:
   ```
   android/app/src/main/assets/api_config.properties
   android/app/google-services.json
   ```

2. **Environment Variables**: For production, consider using:
   - Build config fields
   - Environment variables
   - Secure key management services

3. **API Key Restrictions**:
   - Restrict Gemini API key to your app's package name in Google Cloud Console
   - Set up API quotas and monitoring

## Next Steps

1. If you want to use Firebase features, download and add `google-services.json`
2. If you don't need Firebase, you can remove the Firebase dependencies from `build.gradle`
3. The AI features are ready to use with your API keys through `ApiKeyManager`

## Disabling Firebase (Optional)

If you don't want to use Firebase:

1. Remove from `android/app/build.gradle`:
   ```gradle
   id 'com.google.gms.google-services'
   id 'com.google.firebase.crashlytics'
   ```

2. Remove Firebase initialization from `AstralVuApplication.kt`:
   ```kotlin
   // Remove this line
   FirebaseApp.initializeApp(this)
   ```

3. Remove Firebase dependencies from the dependencies section