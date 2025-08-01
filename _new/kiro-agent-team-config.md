# KIRO Claude CLI Agent Team Configuration for AstralStream Enhancement

## 🎯 Project Context
**Project**: AstralStream Android Video Player  
**Location**: `/path/to/AstralStream`  
**Package**: `com.astralplayer`  
**Objective**: Complete 5 enhancement features without breaking existing functionality

## 🚨 CRITICAL INSTRUCTIONS FOR ALL AGENTS

### Pre-Execution Checklist
```bash
# 1. ALWAYS run these commands FIRST before any work:
cd /path/to/AstralStream
./gradlew clean
./gradlew build
./gradlew test
./gradlew connectedAndroidTest

# 2. Verify project structure:
ls -la app/src/main/java/com/astralplayer/

# 3. Check current test coverage:
./gradlew jacocoTestReport

# 4. Backup current state:
git checkout -b enhancement-backup
git add .
git commit -m "Backup before enhancements"
```

### Working Rules
1. **NEVER** create new projects or duplicate existing features
2. **ALWAYS** work within the existing `com.astralplayer` package structure
3. **SCAN** existing code before implementing any feature
4. **TEST** every change with unit and integration tests
5. **MAINTAIN** the current 87% test coverage (don't let it drop)

---

## 🤖 Agent Team Configuration

### Agent 1: VideoEditingAgent

**Role**: Complete the partially implemented video editing features

**Skills**:
- Android video processing (FFmpeg, MediaCodec)
- Kotlin coroutines for async processing
- UI/UX with Jetpack Compose
- Memory optimization for video processing

**Tasks**:
1. Scan `/astralplayer/nextplayer/editing/` directory
2. Complete `AdvancedVideoEditingTools.kt` implementation
3. Implement missing video filters and effects
4. Add timeline editing with frame-accurate cuts
5. Create export functionality with quality presets
6. Write comprehensive tests for all editing features

**Implementation Plan**:
```kotlin
// Complete these files in order:
// 1. /astralplayer/nextplayer/editing/AdvancedVideoEditingTools.kt
// 2. /astralplayer/nextplayer/editing/filters/VideoFilterEngine.kt
// 3. /astralplayer/nextplayer/editing/timeline/TimelineEditor.kt
// 4. /astralplayer/nextplayer/editing/export/VideoExporter.kt
// 5. /astralplayer/presentation/editing/VideoEditingScreen.kt
```

**Quality Checks**:
- [ ] All editing operations work without crashes
- [ ] Memory usage stays under 200MB during editing
- [ ] Export maintains original quality
- [ ] UI responds within 16ms (60fps)
- [ ] Test coverage for editing module >85%

---

### Agent 2: SubtitleCacheAgent

**Role**: Implement offline subtitle caching system

**Skills**:
- Room database management
- File system operations
- Cache invalidation strategies
- Encryption for cached content

**Tasks**:
1. Extend Room database with subtitle cache entities
2. Implement cache manager with LRU eviction
3. Add encrypted storage for premium subtitles
4. Create background sync for updated subtitles
5. Add cache management UI in settings

**Implementation Files**:
```kotlin
// Create these new files:
// 1. /astralplayer/astralstream/data/entity/CachedSubtitleEntity.kt
// 2. /astralplayer/astralstream/data/dao/SubtitleCacheDao.kt
// 3. /astralplayer/core/cache/SubtitleCacheManager.kt
// 4. /astralplayer/core/cache/SubtitleEncryption.kt
// 5. /astralplayer/astralstream/ui/settings/CacheSettingsScreen.kt

// Modify these existing files:
// 1. /astralplayer/features/ai/EnhancedAISubtitleGenerator.kt
// 2. /astralplayer/astralstream/data/database/AstralStreamDatabase.kt
```

**Quality Checks**:
- [ ] Subtitles load from cache in <100ms
- [ ] Cache size limits are enforced
- [ ] Encrypted subtitles are unreadable on disk
- [ ] Cache survives app updates
- [ ] 90% test coverage for cache system

---

### Agent 3: GestureCustomizationAgent

**Role**: Implement advanced gesture customization system

**Skills**:
- Android touch event handling
- Custom view development
- Preference management
- Haptic feedback implementation

**Tasks**:
1. Create gesture zone mapper
2. Build gesture recording system
3. Implement custom gesture creator
4. Add gesture sensitivity controls
5. Create gesture preview/tutorial system

**Implementation Plan**:
```kotlin
// Enhance these existing files:
// 1. /astralplayer/nextplayer/gesture/AdvancedGestureManager.kt
// 2. /astralplayer/astralstream/gesture/EnhancedGestureManager.kt

// Create new files:
// 1. /astralplayer/core/gesture/GestureZoneMapper.kt
// 2. /astralplayer/core/gesture/CustomGestureRecorder.kt
// 3. /astralplayer/presentation/settings/GestureCustomizationScreen.kt
// 4. /astralplayer/data/entity/CustomGestureEntity.kt
// 5. /astralplayer/data/repository/GestureRepository.kt
```

**Quality Checks**:
- [ ] Gestures respond in <50ms
- [ ] Custom gestures don't conflict with system gestures
- [ ] Gesture zones are visually clear
- [ ] Settings persist across app restarts
- [ ] Accessibility compliance maintained

---

### Agent 4: AnalyticsDashboardAgent

**Role**: Build developer analytics dashboard

**Skills**:
- Analytics implementation (Firebase/Custom)
- Data visualization
- Performance metrics collection
- Privacy-compliant data handling

**Tasks**:
1. Implement analytics collection service
2. Create local analytics database
3. Build dashboard UI with charts
4. Add performance metric tracking
5. Implement privacy controls

**Implementation Plan**:
```kotlin
// Create analytics module:
// 1. /astralplayer/analytics/AnalyticsService.kt
// 2. /astralplayer/analytics/PerformanceCollector.kt
// 3. /astralplayer/analytics/data/AnalyticsDatabase.kt
// 4. /astralplayer/analytics/data/MetricsEntity.kt
// 5. /astralplayer/presentation/dashboard/AnalyticsDashboardActivity.kt
// 6. /astralplayer/presentation/dashboard/DashboardScreen.kt
```

**Quality Checks**:
- [ ] Analytics don't impact app performance (>1% overhead)
- [ ] User privacy is protected (opt-in required)
- [ ] Dashboard loads in <2 seconds
- [ ] Data retention policies enforced
- [ ] GDPR compliance implemented

---

### Agent 5: CommunityFeaturesAgent

**Role**: Implement community features for shared content

**Skills**:
- REST API integration
- Real-time synchronization
- Social features implementation
- Content moderation systems

**Tasks**:
1. Build playlist sharing system
2. Implement subtitle contribution feature
3. Create community subtitle voting
4. Add playlist collaboration
5. Implement content reporting system

**Implementation Plan**:
```kotlin
// Create community module:
// 1. /astralplayer/community/api/CommunityApiService.kt
// 2. /astralplayer/community/repository/PlaylistSharingRepository.kt
// 3. /astralplayer/community/repository/SubtitleContributionRepository.kt
// 4. /astralplayer/community/data/SharedPlaylistEntity.kt
// 5. /astralplayer/presentation/community/CommunityScreen.kt
// 6. /astralplayer/presentation/community/SharedPlaylistsScreen.kt
```

**Quality Checks**:
- [ ] API calls have proper error handling
- [ ] Offline mode works gracefully
- [ ] Content moderation prevents abuse
- [ ] Sync conflicts are resolved properly
- [ ] Community features are opt-in

---

## 🧪 Testing Coordinator Agent

**Role**: Ensure all enhancements maintain quality standards

**Continuous Tasks**:
1. Run test suite after each agent's changes
2. Monitor test coverage (must stay >85%)
3. Perform integration testing
4. Check for memory leaks
5. Validate performance metrics

**Test Commands**:
```bash
# After each feature implementation:
./gradlew test
./gradlew connectedAndroidTest
./gradlew jacocoTestReport

# Performance testing:
./gradlew benchmark

# Memory leak detection:
./gradlew leakCanary

# Security scan:
./gradlew dependencyCheckAnalyze
```

---

## 📋 Execution Order

1. **VideoEditingAgent** - Complete existing partial implementation
2. **SubtitleCacheAgent** - Enhance existing subtitle system
3. **GestureCustomizationAgent** - Extend current gesture system
4. **AnalyticsDashboardAgent** - New module (isolated)
5. **CommunityFeaturesAgent** - New module (requires backend)

## 🎯 Success Criteria

### Overall Project Metrics
- [ ] Test coverage remains >85%
- [ ] App startup time stays <1 second
- [ ] APK size increase <10MB
- [ ] No new crashes in 1000 sessions
- [ ] All existing features still work

### Feature Completion
- [ ] Video editing exports 1080p in <2min
- [ ] Subtitle cache hit rate >80%
- [ ] Gesture customization used by >30% users
- [ ] Analytics dashboard shows 10+ metrics
- [ ] Community features have <200ms latency

## 🔄 Continuous Integration

```yaml
# Add to CI/CD pipeline:
steps:
  - name: Run Enhancement Tests
    run: |
      ./gradlew test
      ./gradlew connectedAndroidTest
      ./gradlew jacocoTestReport
      ./gradlew benchmark
      ./gradlew lint
```

## 📝 Final Checklist for Claude CLI

Before marking complete:
- [ ] All tests pass
- [ ] No lint warnings
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Version bumped in build.gradle
- [ ] Git history is clean
- [ ] PR ready for review

---

**Remember**: Each agent must scan existing code first, work within the current structure, and maintain backward compatibility. The existing app is already high-quality - we're enhancing, not replacing!