#!/bin/bash

# 🚀 AstralStream Integration Automation Script
# This script automates the integration process with safety checks

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="AstralStream"
BACKUP_BRANCH="backup-pre-astralstream-$(date +%Y%m%d-%H%M%S)"
INTEGRATION_BRANCH="feature/astralstream-integration"
WORKSPACE_FILE=".vscode/astralstream.code-workspace"

# Functions
print_header() {
    echo -e "${PURPLE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${PURPLE}║                    🚀 AstralStream Integration                  ║${NC}"
    echo -e "${PURPLE}║                      Automation Script                         ║${NC}"
    echo -e "${PURPLE}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_step() {
    echo -e "${BLUE}📋 Step $1: $2${NC}"
    echo "────────────────────────────────────────────────────────────────"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

confirm_action() {
    echo -e "${YELLOW}❓ $1${NC}"
    read -p "Continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}❌ Operation cancelled by user${NC}"
        exit 1
    fi
}

check_prerequisites() {
    print_step "1" "Checking Prerequisites"
    
    # Check if in Android project
    if [ ! -f "build.gradle" ] && [ ! -f "app/build.gradle" ]; then
        print_error "Not in an Android project directory"
        exit 1
    fi
    print_success "Android project detected"
    
    # Check Git
    if ! command -v git &> /dev/null; then
        print_error "Git is not installed"
        exit 1
    fi
    print_success "Git is available"
    
    # Check if git repo
    if [ ! -d ".git" ]; then
        print_error "Not a Git repository"
        exit 1
    fi
    print_success "Git repository detected"
    
    # Check Gradle
    if [ ! -f "gradlew" ]; then
        print_error "Gradle wrapper not found"
        exit 1
    fi
    print_success "Gradle wrapper found"
    
    # Check VS Code
    if ! command -v code &> /dev/null; then
        print_warning "VS Code not found in PATH, but continuing..."
    else
        print_success "VS Code is available"
    fi
    
    # Check for uncommitted changes
    if [ -n "$(git status --porcelain)" ]; then
        print_warning "You have uncommitted changes"
        confirm_action "This script will create backups, but you should commit your changes first."
    fi
    
    echo ""
}

create_backup() {
    print_step "2" "Creating Safety Backup"
    
    # Create backup branch
    echo "Creating backup branch: $BACKUP_BRANCH"
    git checkout -b "$BACKUP_BRANCH"
    git add .
    git commit -m "🛡️ Safety backup before AstralStream integration" || echo "No changes to commit"
    git push origin "$BACKUP_BRANCH" 2>/dev/null || echo "Remote push failed, but local backup created"
    
    # Return to main branch
    git checkout main 2>/dev/null || git checkout master 2>/dev/null || {
        print_error "Could not find main or master branch"
        exit 1
    }
    
    print_success "Backup created: $BACKUP_BRANCH"
    echo ""
}

setup_integration_branch() {
    print_step "3" "Setting Up Integration Branch"
    
    # Create and switch to integration branch
    git checkout -b "$INTEGRATION_BRANCH" 2>/dev/null || {
        print_warning "Branch $INTEGRATION_BRANCH already exists, switching to it"
        git checkout "$INTEGRATION_BRANCH"
    }
    
    print_success "Integration branch ready: $INTEGRATION_BRANCH"
    echo ""
}

setup_vscode_workspace() {
    print_step "4" "Setting Up VS Code Workspace"
    
    # Create .vscode directory if it doesn't exist
    mkdir -p .vscode
    
    # Create workspace file (will be populated separately)
    echo "Setting up VS Code workspace configuration..."
    
    # Create tasks.json
    cat > .vscode/tasks.json << 'EOF'
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "🔄 Gradle Sync",
            "type": "shell",
            "command": "./gradlew",
            "args": ["--refresh-dependencies", "build"],
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            }
        },
        {
            "label": "🧹 Clean Build",
            "type": "shell", 
            "command": "./gradlew",
            "args": ["clean", "build"],
            "group": "build"
        },
        {
            "label": "🧪 Run Tests",
            "type": "shell",
            "command": "./gradlew",
            "args": ["test"],
            "group": "test"
        },
        {
            "label": "🎯 Lint Check",
            "type": "shell",
            "command": "./gradlew", 
            "args": ["lint"],
            "group": "test"
        }
    ]
}
EOF
    
    # Create settings.json
    cat > .vscode/settings.json << 'EOF'
{
    "kotlin.languageServer.enabled": true,
    "android.gradle.syncOnStart": true,
    "files.associations": {
        "*.kt": "kotlin",
        "*.gradle": "groovy"
    },
    "editor.formatOnSave": true,
    "editor.codeActionsOnSave": {
        "source.organizeImports": true,
        "source.fixAll": true
    },
    "claude.contextLength": 8000,
    "claude.autoComplete": true
}
EOF
    
    # Create extensions.json
    cat > .vscode/extensions.json << 'EOF'
{
    "recommendations": [
        "claude-ai.claude-dev",
        "mathiasfrohlich.kotlin",
        "eamodio.gitlens",
        "usernamehw.errorlens",
        "streetsidesoftware.code-spell-checker"
    ]
}
EOF
    
    print_success "VS Code workspace configured"
    echo ""
}

backup_current_files() {
    print_step "5" "Backing Up Current Implementation"
    
    # Create backup directory
    mkdir -p integration-backups/$(date +%Y%m%d-%H%M%S)
    BACKUP_DIR="integration-backups/$(date +%Y%m%d-%H%M%S)"
    
    # Backup key files that will be modified
    echo "Backing up current files to $BACKUP_DIR..."
    
    # Application class
    if [ -f "app/src/main/java/*/Application.kt" ] || [ -f "app/src/main/java/*/*/Application.kt" ]; then
        find app/src/main/java -name "*Application.kt" -exec cp {} "$BACKUP_DIR/" \;
        print_success "Application class backed up"
    fi
    
    # MainActivity
    if [ -f "app/src/main/java/*/MainActivity.kt" ] || [ -f "app/src/main/java/*/*/MainActivity.kt" ]; then
        find app/src/main/java -name "MainActivity.kt" -exec cp {} "$BACKUP_DIR/" \;
        print_success "MainActivity backed up"
    fi
    
    # Build files
    cp app/build.gradle "$BACKUP_DIR/build.gradle.backup" 2>/dev/null || echo "No app/build.gradle found"
    cp build.gradle "$BACKUP_DIR/project.build.gradle.backup" 2>/dev/null || echo "No project build.gradle found"
    
    # Manifest
    cp app/src/main/AndroidManifest.xml "$BACKUP_DIR/AndroidManifest.xml.backup" 2>/dev/null || echo "No AndroidManifest.xml found"
    
    print_success "Current files backed up to $BACKUP_DIR"
    echo ""
}

analyze_current_project() {
    print_step "6" "Analyzing Current Project Structure"
    
    echo "📊 Project Analysis:"
    echo "────────────────────"
    
    # Check current structure
    echo "• Android package: $(grep -r "package " app/src/main/java/ | head -1 | cut -d' ' -f2 | tr -d ';')"
    echo "• Application ID: $(grep "applicationId" app/build.gradle | cut -d'"' -f2)"
    echo "• Compile SDK: $(grep "compileSdk" app/build.gradle | grep -o '[0-9]*')"
    echo "• Min SDK: $(grep "minSdk" app/build.gradle | grep -o '[0-9]*')"
    echo "• Target SDK: $(grep "targetSdk" app/build.gradle | grep -o '[0-9]*')"
    
    # Check current dependencies
    echo ""
    echo "🔍 Current Key Dependencies:"
    echo "────────────────────────────"
    grep -E "(media3|exoplayer|androidx|hilt|compose)" app/build.gradle || echo "No major dependencies found"
    
    # Check activities
    echo ""
    echo "🎬 Current Activities:"
    echo "─────────────────────"
    find app/src/main/java -name "*Activity.kt" -exec basename {} \; | sort
    
    print_success "Project analysis complete"
    echo ""
}

prepare_integration_checklist() {
    print_step "7" "Preparing Integration Checklist"
    
    cat > INTEGRATION_CHECKLIST.md << 'EOF'
# 🚀 AstralStream Integration Checklist

## Phase 1: Dependencies & Build ⏳
- [ ] Update build.gradle with new dependencies
- [ ] Resolve any dependency conflicts  
- [ ] Add ProGuard rules for new libraries
- [ ] Test Gradle sync and build

## Phase 2: Core Integration ⏳
- [ ] Integrate AstralVuApplication class
- [ ] Enable Hilt dependency injection
- [ ] Add CodecManager initialization
- [ ] Update AndroidManifest.xml with intent filters

## Phase 3: Activity Integration ⏳
- [ ] Enhance VideoPlayerActivity with intent handling
- [ ] Update MainActivity with new features
- [ ] Add network streaming dialog
- [ ] Integrate codec information display

## Phase 4: UI & UX ⏳
- [ ] Apply consistent theming to new components
- [ ] Test responsive design on different screen sizes
- [ ] Verify accessibility compliance
- [ ] Test gesture controls and interactions

## Phase 5: Testing & Validation ⏳
- [ ] Test "Open with" functionality from browsers
- [ ] Verify adult content codec support
- [ ] Test network streaming (HLS, DASH, etc.)
- [ ] Validate Picture-in-Picture mode
- [ ] Performance testing and optimization

## Phase 6: Final Polish ⏳
- [ ] Update app documentation
- [ ] Prepare release notes
- [ ] Test on multiple devices
- [ ] Validate backward compatibility
- [ ] Performance regression testing

## 🆘 Rollback Plan
If integration fails:
1. Switch to backup branch: `git checkout BACKUP_BRANCH_NAME`
2. Review integration logs
3. Use Claude AI agents for troubleshooting
4. Incremental re-integration with smaller changes

## 📞 Getting Help
Use the specialized Claude AI agents:
- Integration Planning Agent - For strategy and timeline
- Code Integration Specialist - For merging code
- Dependency Resolution Master - For build issues
- Manifest Integration Expert - For AndroidManifest issues
- UI Integration Designer - For UI/UX concerns
- Testing & QA Specialist - For testing strategy
EOF
    
    print_success "Integration checklist created: INTEGRATION_CHECKLIST.md"
    echo ""
}

setup_claude_agents() {
    print_step "8" "Setting Up Claude AI Agent Templates"
    
    mkdir -p .vscode/claude-agents
    
    # Integration Planning Agent
    cat > .vscode/claude-agents/planning-agent.md << 'EOF'
# 📋 Integration Planning Agent

You are the Integration Planning Agent for AstralStream video player enhancement.

**Your Mission**: Create detailed, step-by-step integration plans that minimize risk and maximize success.

**Current Task**: [Specify your current need]

**Project Context**: 
- Current codebase: [Describe your current video player]
- Target enhancements: Enhanced codecs, "Open with" support, adult content optimization
- Timeline constraints: [Your deadlines]

**Required Output**:
1. Integration Strategy (high-level approach)
2. Detailed Timeline (with milestones)
3. Risk Assessment (potential issues + mitigation)
4. Success Criteria (measurable outcomes)
5. Rollback Plan (if things go wrong)
EOF
    
    # Code Integration Specialist
    cat > .vscode/claude-agents/code-specialist.md << 'EOF'
# 🔧 Code Integration Specialist

You are the Code Integration Specialist for AstralStream enhancement project.

**Your Mission**: Safely merge new code with existing codebases while preserving functionality.

**Integration Rules**:
1. NEVER break existing functionality
2. ALWAYS provide backward compatibility
3. PRESERVE user data and preferences
4. MAINTAIN performance standards
5. FOLLOW Android best practices

**Current Task**: [Specify: merge Application class, integrate activities, resolve conflicts, etc.]

**Required Output**:
1. Merged Code (complete, working implementation)
2. Conflict Resolution (explanation of changes)
3. Migration Steps (step-by-step instructions)  
4. Testing Checklist (verification steps)
5. Potential Issues (what to watch for)
EOF
    
    print_success "Claude AI agent templates created"
    echo ""
}

generate_next_steps() {
    print_step "9" "Generating Next Steps Guide"
    
    cat > NEXT_STEPS.md << 'EOF'
# 🎯 Next Steps for AstralStream Integration

## Immediate Actions (Today)

### 1. Open VS Code Workspace
```bash
code .vscode/astralstream.code-workspace
```

### 2. Install Required Extensions
- Claude Dev (AI assistant)
- Kotlin Language Support
- GitLens (Enhanced Git)
- Error Lens (Inline errors)

### 3. Start with Dependencies
Use the **Dependency Resolution Master** agent:

```
Dependency Resolution Master, I need help integrating AstralStream dependencies.

My current build.gradle (app):
[PASTE YOUR CURRENT BUILD.GRADLE HERE]

I need to add:
- Advanced Media3 components
- Hilt dependency injection  
- Cloud storage APIs
- AI/ML capabilities
- Enhanced codec support

Please provide the complete, optimized build.gradle with conflict resolution.
```

## Phase-by-Phase Integration

### Phase 1: Build & Dependencies (Day 1)
1. **Backup current build.gradle**: ✅ Already done by script
2. **Update dependencies**: Use Dependency Resolution Master agent
3. **Resolve conflicts**: Test gradle sync
4. **Update ProGuard rules**: For release builds

### Phase 2: Core Integration (Day 2)
1. **Application class**: Use Code Integration Specialist agent
2. **AndroidManifest.xml**: Use Manifest Integration Expert agent  
3. **Hilt setup**: Enable dependency injection
4. **Basic compilation test**: Ensure project builds

### Phase 3: Activity Integration (Day 3)
1. **VideoPlayerActivity**: Enhanced intent handling
2. **MainActivity**: New UI components
3. **Navigation**: Update app navigation
4. **Basic functionality test**: Core features work

### Phase 4: UI & Testing (Day 4-5)
1. **UI components**: Network stream dialog, codec info
2. **Theming**: Consistent Material Design
3. **Testing**: "Open with" functionality
4. **Performance**: Ensure no regressions

## 🔧 Troubleshooting Commands

```bash
# Clean and rebuild
./gradlew clean build

# Check dependencies
./gradlew app:dependencies

# Run tests
./gradlew test

# Install debug APK
./gradlew installDebug

# Create backup
git stash save "Emergency backup $(date)"
```

## 🆘 If Something Goes Wrong

1. **Switch to backup branch**:
   ```bash
   git checkout [BACKUP_BRANCH_NAME]
   ```

2. **Use Claude AI agents** for specific issues

3. **Check integration checklist** for missed steps

4. **Review logs** in VS Code Problems panel

## 📱 Testing "Open with" Functionality

### Test URLs:
- **MP4**: `https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4`
- **HLS**: `https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8`

### Test Process:
1. Install your app
2. Open Chrome browser
3. Navigate to test URL
4. Tap video to play
5. Look for "Open with AstralStream" option
6. Verify video plays with enhanced features

## 🎯 Success Criteria

✅ **Integration Complete When**:
- App builds without errors
- "Open with" works from browsers
- Enhanced codecs support adult content
- Network streaming works (HLS, DASH)
- Performance is maintained or improved
- All existing features still work

Remember: Take it step by step, use the Claude AI agents, and don't hesitate to ask for help!
EOF
    
    print_success "Next steps guide created: NEXT_STEPS.md"
    echo ""
}

finalize_setup() {
    print_step "10" "Finalizing Setup"
    
    # Commit the setup
    git add .
    git commit -m "🚀 Setup AstralStream integration environment

- Created VS Code workspace configuration
- Added Claude AI agent templates  
- Prepared integration checklist
- Generated next steps guide
- Backed up current implementation"
    
    print_success "Integration environment setup complete!"
    echo ""
    
    # Final summary
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                    ✅ Setup Complete!                          ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}📁 Files Created:${NC}"
    echo "   • .vscode/workspace configuration"
    echo "   • .vscode/claude-agents/ (AI agent templates)"
    echo "   • INTEGRATION_CHECKLIST.md"
    echo "   • NEXT_STEPS.md"
    echo "   • integration-backups/ (current code backup)"
    echo ""
    echo -e "${BLUE}🌟 What's Next:${NC}"
    echo "   1. Open VS Code: code ."
    echo "   2. Install recommended extensions"
    echo "   3. Follow NEXT_STEPS.md guide"
    echo "   4. Use Claude AI agents for integration"
    echo ""
    echo -e "${BLUE}🛡️ Safety:${NC}"
    echo "   • Backup branch: $BACKUP_BRANCH"
    echo "   • Integration branch: $INTEGRATION_BRANCH"
    echo "   • File backups: integration-backups/"
    echo ""
    echo -e "${GREEN}🚀 Ready to integrate AstralStream enhancements!${NC}"
    echo ""
}

# Main execution
main() {
    print_header
    
    # Check if user wants to continue
    confirm_action "This script will set up your project for AstralStream integration. It will create backups and prepare your development environment."
    
    # Execute all steps
    check_prerequisites
    create_backup
    setup_integration_branch
    setup_vscode_workspace
    backup_current_files
    analyze_current_project
    prepare_integration_checklist  
    setup_claude_agents
    generate_next_steps
    finalize_setup
    
    # Open VS Code if available
    if command -v code &> /dev/null; then
        echo -e "${BLUE}🚀 Opening VS Code...${NC}"
        code .
    fi
}

# Run the main function
main "$@"