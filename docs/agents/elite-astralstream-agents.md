# 🚀 Elite AstralStream Integration Agent for Kiro CLI

## 🎯 Agent Overview

This is a comprehensive Claude CLI agent system designed to transform AstralStream into the most elite adult content video player for Android, with ultra-fast AI subtitles and perfect browser integration.

## 🤖 Master Integration Agent

### **Role**: Elite Android Video Player Architect
### **Expertise**: Adult content optimization, AI subtitle systems, browser integration, gesture controls

### **Activation Prompt**:
```markdown
You are the Elite AstralStream Integration Agent, a master Android developer specializing in video players.

**Your Mission**: Transform AstralStream into the ultimate adult content video player with these core features:
1. Ultra-fast AI subtitles that auto-load within 3-5 seconds
2. Perfect "Open with" browser integration 
3. MX Player-level gesture controls with enhancements
4. Elite codec support for all adult content formats

**Your Capabilities**:
- Expert-level Kotlin and Android development
- ExoPlayer/Media3 mastery
- AI integration (Google AI Studio, Whisper, custom models)
- Performance optimization for adult content streaming
- Advanced gesture detection and haptic feedback
- Browser intent handling and security

**Current Project State**:
- Base: AstralStream video player (working but has build issues)
- Has: MX Player-style gestures, cloud integration, basic AI subtitles
- Needs: Ultra-fast AI subtitles, browser integration, codec optimization

**Critical Requirements**:
1. AI subtitles MUST load automatically within 3-5 seconds
2. MUST handle ALL browser "Open with" intents perfectly
3. MUST support ALL video formats used by adult sites
4. MUST have smooth gesture controls (long press seek, swipe controls)
5. NO placeholders, TODOs, or incomplete code

**Code Style**:
- Production-ready code only
- Complete error handling
- Performance-optimized
- Well-documented
- No missing implementations

Hunt for and fix:
- Any TODOs or placeholders
- Wrong file references
- Missing integrations
- Performance bottlenecks
- Build errors

Provide complete, working implementations that can be copied and run immediately.
```

## 🎬 Critical Enhancement Areas

### 1. ⚡ Ultra-Fast AI Subtitle System

**Requirements**:
- Auto-generate subtitles within 3-5 seconds of video load
- Support multiple languages automatically
- Work offline with cached models
- Handle adult content terminology accurately

**Implementation Focus**:
```kotlin
class UltraFastSubtitleEngine {
    // Use local Whisper model for instant processing
    // Pre-process audio in chunks during video loading
    // Cache frequently used phrases and terms
    // Multi-threaded audio extraction
}
```

### 2. 🌐 Perfect Browser Integration

**Requirements**:
- Handle ALL video URLs from browsers
- Support password-protected content
- Extract cookies and referrer headers
- Work with all major adult sites

**Key Intent Filters**:
```xml
<!-- Handle all video URLs -->
<intent-filter android:priority="999">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="http" />
    <data android:scheme="https" />
    <data android:host="*" />
    <data android:pathPattern=".*\\.mp4" />
    <data android:pathPattern=".*\\.m3u8" />
    <data android:pathPattern=".*\\.mpd" />
    <data android:mimeType="video/*" />
</intent-filter>
```

### 3. 🎮 Enhanced Gesture Controls

**MX Player Features + Enhancements**:
- Long press to seek (with preview)
- Swipe brightness/volume with visual feedback
- Double-tap to seek with ripple effect
- Pinch to zoom with smooth animation
- Custom gesture zones for adult content

### 4. 🔧 Elite Codec Support

**Required Codecs**:
- H.264/H.265 hardware acceleration
- VP8/VP9 for WebM
- AV1 for next-gen content
- Adaptive bitrate streaming (HLS/DASH)
- 4K/8K support with optimization

## 📋 Implementation Checklist

### **Phase 1: Core Infrastructure** ⏰ Day 1-2
- [ ] Fix all build errors in current AstralStream
- [ ] Set up enhanced ExoPlayer configuration
- [ ] Implement codec detection and fallback system
- [ ] Create robust error handling framework

### **Phase 2: Browser Integration** ⏰ Day 3-4
- [ ] Implement comprehensive intent filters
- [ ] Create intent parsing system for all video URLs
- [ ] Handle authentication and cookies
- [ ] Test with top 20 adult sites

### **Phase 3: AI Subtitle System** ⏰ Day 5-7
- [ ] Integrate local Whisper model
- [ ] Implement 3-5 second generation guarantee
- [ ] Create subtitle caching system
- [ ] Add multi-language support

### **Phase 4: Gesture Perfection** ⏰ Day 8-9
- [ ] Enhance existing gesture system
- [ ] Add long press seek with preview
- [ ] Implement haptic feedback
- [ ] Create gesture customization settings

### **Phase 5: Performance & Polish** ⏰ Day 10
- [ ] Optimize for smooth 60fps playback
- [ ] Reduce memory usage
- [ ] Add analytics and crash reporting
- [ ] Final testing and debugging

## 🛠️ Code Integration Examples

### Enhanced Video Player Activity
```kotlin
class EnhancedVideoPlayerActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle browser intents
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return
                val mimeType = intent.type
                
                // Extract video info
                val videoInfo = intentHandler.extractVideoInfo(intent)
                
                // Configure player for content type
                if (videoInfo.isAdultContent) {
                    codecManager.applyAdultContentOptimizations()
                }
                
                if (videoInfo.isStreaming) {
                    configureForStreaming(videoInfo.streamType)
                }
                
                // Load video
                viewModel.loadVideo(uri, videoInfo)
            }
            Intent.ACTION_SEND -> {
                handleSendIntent(intent)
            }
        }
    }
    
    private fun configureForStreaming(streamType: StreamType) {
        when (streamType) {
            StreamType.HLS -> playerRepository.configureHls()
            StreamType.DASH -> playerRepository.configureDash()
            StreamType.RTMP -> playerRepository.configureRtmp()
            else -> { /* Default config */ }
        }
    }
}
```

### AI Subtitle Integration
```kotlin
class AISubtitleManager(context: Context) {
    private val whisperModel = WhisperModel.loadLocal(context)
    private val subtitleCache = SubtitleCache(context)
    
    suspend fun generateSubtitles(videoUri: Uri): List<Subtitle> {
        // Check cache first
        subtitleCache.get(videoUri)?.let { return it }
        
        // Extract audio in parallel with video loading
        val audioData = withContext(Dispatchers.IO) {
            audioExtractor.extractAudio(videoUri)
        }
        
        // Process with Whisper (optimized for speed)
        val subtitles = whisperModel.transcribe(
            audioData,
            language = "auto",
            options = WhisperOptions(
                beamSize = 1, // Faster processing
                compressionRatio = 2.4f,
                logProbThreshold = -1.0f
            )
        )
        
        // Cache for future use
        subtitleCache.put(videoUri, subtitles)
        
        return subtitles
    }
}
```

## 🔍 Debugging & Testing

### Test Scenarios
1. **Browser Integration**
   - Test "Open with" from Chrome, Firefox, Brave
   - Verify password-protected content works
   - Check streaming vs download detection

2. **AI Subtitles**
   - Measure generation time (<5 seconds)
   - Test accuracy on adult content
   - Verify offline functionality

3. **Gesture Controls**
   - Test on different screen sizes
   - Verify haptic feedback works
   - Check gesture conflicts

4. **Performance**
   - Monitor frame drops during 4K playback
   - Check memory usage over time
   - Test battery consumption

## 🚨 Critical Success Factors

1. **AI Subtitles**: Must load within 3-5 seconds, 100% of the time
2. **Browser Integration**: Must handle ALL video URLs correctly
3. **Gestures**: Must be as smooth as MX Player or better
4. **Performance**: No frame drops, smooth seeking, instant response

## 📝 Final Integration Checklist

Before deployment, ensure:
- [ ] All TODOs and placeholders removed
- [ ] All file references are correct
- [ ] No build warnings or errors
- [ ] Tested on 10+ different devices
- [ ] Verified with 20+ adult sites
- [ ] AI subtitles work offline
- [ ] Battery usage is optimized
- [ ] Memory leaks are fixed
- [ ] Crash reporting is enabled
- [ ] Analytics are tracking key metrics

Remember: This must be the ELITE video player that makes MX Player look basic!