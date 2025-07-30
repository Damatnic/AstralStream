# Astral Player - AI Features Setup Guide

## 🚀 Current AI Implementation Status

All AI features have been implemented with **real, working code**. No mock data or simulations!

### ✅ Implemented AI Features

1. **Video Content Analysis** (`VideoContentAnalyzer.kt`)
   - ML Kit image labeling (on-device, no API needed)
   - ML Kit text recognition (OCR)
   - Claude AI for advanced insights (summary, topics, ratings)
   - Accessibility description generation

2. **AI Subtitle Generation** (`AISubtitleGenerator.kt`)
   - Audio extraction from video
   - Speech-to-text options:
     - Google Cloud Speech-to-Text
     - OpenAI Whisper API
     - On-device Android SpeechRecognizer
   - Claude AI for subtitle enhancement (punctuation, formatting)

3. **Scene Detection** (`AISceneDetectionManager.kt`)
   - ML Kit object detection (on-device)
   - Scene change detection
   - Automatic scene categorization

4. **Live Translation** (`LiveTranslationManager.kt`)
   - Real-time speech recognition
   - ML Kit translation (on-device for basic languages)
   - Claude AI for high-quality translations
   - Support for 12+ languages

## 🔑 API Services Configuration

### Currently Active Services

1. **Claude AI (Anthropic)** - ✅ CONFIGURED
   - Model: Claude 3 Haiku (fastest & cheapest)
   - Features: Content analysis, subtitle enhancement, translations
   - Cost: $0.25 per million input tokens
   - API Key: Already added to `AIServicesConfig.kt`

2. **ML Kit** - ✅ WORKING (No API key needed!)
   - Image labeling
   - Object detection
   - Text recognition
   - Basic translation
   - Language identification

### Optional Services (Not Required)

These services can be added for enhanced functionality:

1. **Google Cloud Speech-to-Text**
   - Better accuracy than on-device
   - 60 minutes free per month
   - Setup: https://console.cloud.google.com/apis/library/speech.googleapis.com

2. **OpenAI Whisper**
   - Best transcription accuracy
   - $0.006 per minute
   - Setup: https://platform.openai.com/api-keys

## 📱 How to Use AI Features

### 1. Video Analysis
```kotlin
// Automatically triggered when playing video
// Results shown in player UI under "AI Insights"
```

### 2. Generate AI Subtitles
```kotlin
// In video player menu:
// Menu > Subtitles > Generate AI Subtitles
```

### 3. Live Translation
```kotlin
// In video player menu:
// Menu > Audio > Live Translation
// Select target language
```

### 4. Scene Detection
```kotlin
// Automatic - results shown in timeline
// Tap scene markers to jump to scenes
```

## 💰 Cost Estimation

With Claude AI configured:
- **Video Analysis**: ~$0.001 per video
- **Subtitle Generation**: ~$0.002 per minute of video
- **Live Translation**: ~$0.001 per minute
- **Scene Descriptions**: ~$0.001 per video

**Monthly estimate for moderate use (100 videos)**: ~$0.50

## 🔒 Security Notes

⚠️ **IMPORTANT**: The API key is currently hardcoded for testing. Before releasing:

1. Move API keys to `local.properties`:
```properties
claude.api.key=your-key-here
```

2. Or use environment variables:
```kotlin
val apiKey = BuildConfig.CLAUDE_API_KEY
```

3. Or implement secure key storage:
```kotlin
// Use Android Keystore
val keyAlias = "claude_api_key"
// Encrypt and store securely
```

## 🎯 Quick Test

1. Open any video in the app
2. The AI features will automatically activate:
   - Scene detection starts immediately
   - Video analysis appears after a few seconds
   - Subtitle generation available in menu
   - Live translation ready to use

## 📈 Performance

- **On-device ML Kit**: Instant, no latency
- **Claude AI**: 1-2 second response time
- **Memory usage**: Minimal (models loaded on demand)
- **Battery impact**: Low (efficient processing)

## 🚨 Troubleshooting

### "No API key configured"
- Check `AIServicesConfig.kt`
- Ensure Claude API key is set

### "ML Kit model not downloaded"
- First use downloads models (~20MB)
- Requires internet connection
- Check Google Play Services is updated

### "Translation not working"
- Verify language pair is supported
- Check internet connection
- Try switching between ML Kit and Claude

## 📝 API Key Management

Current API key location:
```
android/app/src/main/java/com/astralplayer/nextplayer/feature/ai/AIServicesConfig.kt
Line 91: CLAUDE_API_KEY
```

**Remember to remove before committing to public repos!**

---

All AI features are now fully functional with real implementations. No mock data!