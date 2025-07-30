# 🚀 AstralStream Integration Guide for VS Code + Claude AI

## 📋 Prerequisites

### Required Extensions
1. **Claude Dev** - AI assistant for VS Code
2. **Kotlin Language** - Kotlin support
3. **Android IDE Support** - Android development tools
4. **Git Lens** - Enhanced Git capabilities
5. **Error Lens** - Inline error display
6. **Code Spell Checker** - Code quality
7. **Bracket Pair Colorizer** - Better code readability

### Install Extensions
```bash
# Using VS Code command palette (Ctrl+Shift+P)
ext install claude-ai.claude-dev
ext install mathiasfrohlich.kotlin
ext install google.android-studio-support
ext install eamodio.gitlens
ext install usernamehw.errorlens
ext install streetsidesoftware.code-spell-checker
```

## 🏗️ Project Setup

### 1. Backup Your Current Project
```bash
# Create a backup branch
git checkout -b backup-before-astralstream-integration
git add .
git commit -m "Backup before AstralStream integration"
git push origin backup-before-astralstream-integration

# Return to main branch
git checkout main
```

### 2. Create Integration Branch
```bash
git checkout -b feature/astralstream-integration
```

## 🤖 Claude AI Agents Setup

### Agent 1: File Integration Assistant
Create this prompt template in VS Code:

```markdown
## File Integration Agent

You are a specialized Android development assistant for integrating AstralStream video player components. Your tasks:

1. **Analyze existing code** and identify conflicts
2. **Merge new files** with existing codebase
3. **Update dependencies** in build.gradle files
4. **Fix compilation errors** and resolve conflicts
5. **Verify integration** completeness

**Current Task**: [Specify what you need help with]
**Files to integrate**: [List the files]
**Existing conflicts**: [Describe any conflicts]

Please provide step-by-step instructions and code changes needed.
```

### Agent 2: Dependency Resolution Agent
```markdown
## Dependency Resolution Agent

You are an expert in Android dependency management. Help me:

1. **Analyze build.gradle** for conflicts
2. **Resolve version conflicts** between dependencies
3. **Add missing dependencies** for AstralStream
4. **Optimize build configuration** 
5. **Handle ProGuard rules** for release builds

**Current build.gradle content**: [Paste your current build.gradle]
**Target features**: Enhanced codecs, PiP, cloud storage, AI features

Provide the complete updated build.gradle with explanations.
```

### Agent 3: Manifest Integration Agent
```markdown
## Manifest Integration Agent

You are an Android manifest specialist. Help me:

1. **Merge AndroidManifest.xml** files safely
2. **Add required permissions** without breaking existing ones
3. **Set up intent filters** for "Open with" functionality
4. **Configure activities** and services properly
5. **Handle manifest conflicts** with existing declarations

**My current AndroidManifest.xml**: [Paste current manifest]
**Required features**: Video intent handling, PiP, cloud storage, biometric auth

Provide step-by-step integration instructions.
```

## 📁 File Integration Steps

### Step 1: Enhanced Dependencies
Create this VS Code task (`.vscode/tasks.json`):

```json
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "Backup build.gradle",
            "type": "shell",
            "command": "cp app/build.gradle app/build.gradle.backup",
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always"
            }
        },
        {
            "label": "Validate dependencies",
            "type": "shell",
            "command": "./gradlew dependencies",
            "group": "build",
            "dependsOn": "Backup build.gradle"
        }
    ]
}
```

**Claude AI Prompt for Step 1**:
```
Help me integrate the enhanced build.gradle dependencies into my existing project. 

My current build.gradle (app level):
[PASTE YOUR CURRENT BUILD.GRADLE HERE]

I need to add:
- Media3/ExoPlayer advanced features
- Hilt dependency injection  
- Cloud storage APIs
- AI/ML capabilities
- Enhanced codec support
- Biometric authentication

Please provide:
1. Merged build.gradle file
2. Any conflicts you notice
3. Gradle sync commands to run
4. ProGuard rules needed
```

### Step 2: Application Class Integration

**VS Code Snippet** (Create in `.vscode/snippets.json`):
```json
{
    "AstralVu Application Integration": {
        "prefix": "astral-app",
        "body": [
            "// TODO: Integrate AstralVuApplication class",
            "// 1. Enable Hilt: @HiltAndroidApp", 
            "// 2. Add codec manager initialization",
            "// 3. Setup intent handlers",
            "// 4. Initialize Firebase if needed",
            "$1"
        ],
        "description": "Template for AstralVu app integration"
    }
}
```

**Claude AI Prompt for Step 2**:
```
I need to integrate the enhanced AstralVuApplication class with my existing Application class. 

My current Application class:
[PASTE YOUR CURRENT APPLICATION CLASS]

The new AstralVuApplication has:
- Hilt integration (@HiltAndroidApp)
- Codec manager setup
- Intent handlers for "Open with"
- Firebase initialization
- Advanced settings repository

Please help me:
1. Merge the two Application classes
2. Identify any conflicts
3. Provide migration steps
4. Update any references in other files
```

### Step 3: Manifest Integration

**Claude AI Prompt for Step 3**:
```
I need to merge the comprehensive AndroidManifest.xml with my existing manifest.

My current AndroidManifest.xml:
[PASTE YOUR CURRENT MANIFEST]

The new manifest includes:
- Comprehensive video intent filters
- Adult content codec support  
- Network streaming protocols
- PiP support configuration
- Enhanced permissions
- Multiple activity declarations

Please provide:
1. Safely merged AndroidManifest.xml
2. Explanation of new permissions
3. Intent filter priorities
4. Potential conflicts with existing apps
5. Testing checklist for "Open with" functionality
```

### Step 4: Activity Integration

**VS Code Workspace Settings** (`.vscode/settings.json`):
```json
{
    "kotlin.languageServer.enabled": true,
    "android.gradle.syncOnStart": true,
    "files.associations": {
        "*.kt": "kotlin"
    },
    "editor.codeActionsOnSave": {
        "source.organizeImports": true,
        "source.fixAll": true
    },
    "claude.contextLength": 4000,
    "claude.autoComplete": true
}
```

**Claude AI Prompt for Step 4**:
```
Help me integrate the enhanced VideoPlayerActivity and MainActivity with my existing activities.

My current MainActivity:
[PASTE CURRENT MAINACTIVITY]

My current VideoPlayerActivity (if exists):
[PASTE CURRENT VIDEOPLAYERACTIVITY OR SAY "NONE"]

The enhanced activities include:
- Advanced intent handling
- Codec manager integration
- PiP support
- Adult content optimizations
- Network streaming support
- Error handling improvements

Please provide:
1. Step-by-step integration plan
2. Code merge strategies
3. Potential breaking changes
4. Testing procedures
5. Migration checklist
```

## 🔧 Integration Workflow

### Phase 1: Preparation (Day 1)
```bash
# 1. Create workspace
mkdir astralstream-integration
cd astralstream-integration
code .

# 2. Setup Git workflow
git flow init

# 3. Create feature branch
git flow feature start enhanced-video-player
```

**Claude AI Prompt for Phase 1**:
```
I'm starting the AstralStream integration. Help me create a comprehensive integration plan.

My current project structure:
[PASTE YOUR PROJECT STRUCTURE]

My current features:
[LIST YOUR CURRENT VIDEO PLAYER FEATURES]

Target enhancements:
- Enhanced codec support
- "Open with" browser integration  
- Adult content optimizations
- Cloud storage support
- AI subtitle generation
- Advanced gesture controls

Please provide:
1. Integration timeline (realistic estimates)
2. Risk assessment
3. Rollback strategy
4. Testing milestones
5. Deployment checklist
```

### Phase 2: Core Integration (Day 2-3)

**VS Code Tasks for Phase 2**:
```json
{
    "label": "Integration Phase 2",
    "dependsOrder": "sequence",
    "dependsOn": [
        "Backup current code",
        "Update build.gradle", 
        "Integrate Application class",
        "Update AndroidManifest",
        "Sync Gradle",
        "Resolve conflicts"
    ]
}
```

**Claude AI Prompts for Phase 2**:

**Build Integration**:
```
I'm ready to integrate the enhanced build.gradle. Here's my current situation:

Current build.gradle: [PASTE]
Current gradle.properties: [PASTE]
Current proguard-rules.pro: [PASTE]

Please help me:
1. Merge dependencies safely
2. Update minimum SDK if needed
3. Add ProGuard rules for new libraries
4. Configure build variants properly
5. Handle any version conflicts
```

**Codec Manager Integration**:
```
Help me integrate the CodecManager into my existing codebase.

My current video player setup:
[DESCRIBE YOUR CURRENT PLAYER SETUP]

I need to:
1. Add CodecManager to dependency injection
2. Initialize advanced codec support
3. Update ExoPlayer configuration
4. Handle adult content optimizations
5. Test codec detection

Provide step-by-step integration with code examples.
```

### Phase 3: UI Integration (Day 4-5)

**Claude AI Prompt for UI Integration**:
```
Help me integrate the enhanced UI components:

My current UI components:
[LIST YOUR CURRENT COMPOSE COMPONENTS]

New components to integrate:
- NetworkStreamDialog
- IntentHandlerDialog  
- Enhanced video player controls
- Codec information display
- Advanced settings screens

Please provide:
1. Component integration strategy
2. Theme compatibility updates
3. Navigation graph updates
4. State management changes
5. UI testing approach
```

### Phase 4: Testing & Validation (Day 6-7)

**VS Code Testing Configuration**:
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "name": "Android Attach Debugger",
            "type": "android",
            "request": "attach"
        },
        {
            "name": "Test Intent Handling",
            "type": "android",
            "request": "launch",
            "intentAction": "android.intent.action.VIEW",
            "intentData": "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
            "intentType": "video/mp4"
        }
    ]
}
```

**Claude AI Prompt for Testing**:
```
Help me create a comprehensive testing plan for the AstralStream integration.

I need test cases for:
1. "Open with" functionality from browsers
2. Adult content codec support
3. Network streaming capabilities
4. PiP mode transitions
5. Advanced gesture controls
6. Error handling scenarios

Please provide:
1. Manual testing checklist
2. Automated test suggestions
3. Device compatibility testing
4. Performance testing approach
5. User acceptance criteria
```

## 🚨 Common Issues & Solutions

### Issue 1: Hilt Integration Conflicts
**Claude AI Prompt**:
```
I'm getting Hilt compilation errors during integration:

Error messages:
[PASTE YOUR ERROR MESSAGES]

My current Hilt setup:
[DESCRIBE CURRENT HILT USAGE]

Please help me resolve these Hilt conflicts and properly integrate the enhanced dependency injection.
```

### Issue 2: Manifest Merge Conflicts
**Claude AI Prompt**:
```
I'm getting manifest merge conflicts:

Conflict details:
[PASTE MANIFEST MERGE ERRORS]

Please help me resolve these manifest conflicts while preserving my existing functionality.
```

### Issue 3: Dependency Version Conflicts
**Claude AI Prompt**:
```
I have dependency version conflicts after integration:

Gradle sync errors:
[PASTE GRADLE ERRORS]

Help me resolve these version conflicts and find compatible versions for all dependencies.
```

## 📝 Integration Checklist

### Pre-Integration
- [ ] ✅ Backup current project
- [ ] ✅ Create feature branch
- [ ] ✅ Install required VS Code extensions
- [ ] ✅ Setup Claude AI agents
- [ ] ✅ Review current codebase

### Core Integration  
- [ ] 🔄 Update build.gradle with new dependencies
- [ ] 🔄 Integrate AstralVuApplication class
- [ ] 🔄 Merge AndroidManifest.xml
- [ ] 🔄 Add CodecManager integration
- [ ] 🔄 Update dependency injection setup

### Feature Integration
- [ ] 🔄 Integrate enhanced VideoPlayerActivity
- [ ] 🔄 Update MainActivity with new features
- [ ] 🔄 Add network streaming dialogs
- [ ] 🔄 Integrate advanced settings
- [ ] 🔄 Setup intent handling system

### Testing & Validation
- [ ] ⏳ Test "Open with" functionality
- [ ] ⏳ Validate codec support
- [ ] ⏳ Test network streaming
- [ ] ⏳ Verify PiP functionality
- [ ] ⏳ Test adult content optimizations

### Deployment Prep
- [ ] ⏳ Update ProGuard rules
- [ ] ⏳ Test release build
- [ ] ⏳ Update app documentation
- [ ] ⏳ Prepare release notes
- [ ] ⏳ Create rollback plan

## 🎯 Success Metrics

After integration, verify these capabilities:
1. **Browser Integration** - Videos open from Chrome/Firefox/etc.
2. **Format Support** - 30+ video formats play correctly
3. **Adult Content** - Enhanced codecs work properly
4. **Network Streaming** - HLS/DASH streams play smoothly
5. **Performance** - No regression in playback performance
6. **Stability** - No crashes or memory leaks

## 📞 Getting Help

If you encounter issues during integration:

1. **Use Claude AI agents** with specific error messages
2. **Check VS Code Problems panel** for compilation errors
3. **Review Android Studio logs** for runtime issues
4. **Test on multiple devices** for compatibility
5. **Create minimal reproduction cases** for complex issues

Remember: Integration is iterative. Start with core functionality and gradually add advanced features.
