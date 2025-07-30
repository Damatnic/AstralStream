# Contributing to AstralStream

Thank you for your interest in contributing to AstralStream! We welcome contributions from developers of all skill levels.

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 21+ (Android 5.0+)
- Kotlin 1.9.0+
- Gradle 8.0+
- Git

### Setting up the Development Environment

1. **Fork the repository**
   ```bash
   # Click the "Fork" button on GitHub, then clone your fork
   git clone https://github.com/YOUR_USERNAME/AstralStream.git
   cd AstralStream
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned `AstralStream` folder
   - Select the `android` folder within the project

3. **Sync and Build**
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

## 🛠 Development Guidelines

### Code Style
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use **Jetpack Compose** for UI components
- Follow **MVVM architecture** patterns
- Use meaningful variable and function names
- Add comments for complex logic

### Architecture Principles
- **Single Responsibility**: Each class/function should have one clear purpose
- **Data Flow**: Use `StateFlow` and `LiveData` for reactive programming
- **Dependency Injection**: Use constructor injection where possible
- **Repository Pattern**: Separate data logic from UI logic

### Key Components to Understand
- **MxStyleGestureDetector**: Handles all gesture recognition
- **SimpleEnhancedPlayerViewModel**: Main state management
- **SettingsRepository**: Persistent settings storage
- **BubbleQuickSettingsMenu**: Floating UI controls
- **LongPressSpeedOverlay**: Visual feedback system

## 🎯 Contribution Types

### 🐛 Bug Fixes
1. Check existing issues first
2. Create a new issue if none exists
3. Fork and create a branch: `fix/issue-description`
4. Make your changes with tests
5. Submit a pull request

### ✨ New Features
1. **Discuss first** - Open an issue to discuss the feature
2. Follow the [Phase Implementation Plan](README.md#phase-implementation-plan)
3. Create a branch: `feature/feature-name`
4. Implement with proper testing
5. Update documentation if needed

### 📚 Documentation
- Improve README, code comments, or wiki
- Create tutorials or guides
- Fix typos or clarity issues

### 🧪 Testing
- Add unit tests for new functionality
- Add integration tests for complex features
- Test on different Android versions and devices

## 🔄 Development Workflow

### Branch Naming Convention
- `feature/feature-name` - New features
- `fix/bug-description` - Bug fixes
- `docs/update-description` - Documentation updates
- `refactor/component-name` - Code refactoring
- `test/test-description` - Testing improvements

### Commit Message Format
```
type(scope): description

- feat: A new feature
- fix: A bug fix
- docs: Documentation changes
- style: Code style changes (formatting, etc.)
- refactor: Code refactoring
- test: Adding or updating tests
- chore: Maintenance and build changes

Examples:
feat(gestures): add progressive speed control
fix(settings): resolve memory leak in DataStore
docs(readme): update installation instructions
```

### Pull Request Process

1. **Before Creating PR**
   - Ensure all tests pass: `./gradlew test`
   - Build successfully: `./gradlew assembleDebug`
   - Follow code style guidelines
   - Update documentation if needed

2. **Creating the PR**
   - Use a clear, descriptive title
   - Reference related issues: "Fixes #123"
   - Describe what changes were made and why
   - Include screenshots for UI changes
   - Add test results if applicable

3. **PR Template**
   ```markdown
   ## Description
   Brief description of changes
   
   ## Type of Change
   - [ ] Bug fix
   - [ ] New feature
   - [ ] Documentation update
   - [ ] Refactoring
   
   ## Related Issues
   Fixes #123
   
   ## Testing
   - [ ] Unit tests pass
   - [ ] Manual testing completed
   - [ ] Tested on different devices
   
   ## Screenshots (if applicable)
   
   ## Checklist
   - [ ] Code follows style guidelines
   - [ ] Self-review completed
   - [ ] Documentation updated
   - [ ] Tests added/updated
   ```

## 🧪 Testing Guidelines

### Running Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# UI tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.astralplayer.nextplayer.ui.tests
```

### Writing Tests
- **Unit tests** for ViewModels and Repository classes
- **Integration tests** for gesture detection
- **UI tests** for user interactions
- **Edge case testing** for unusual inputs

### Test Categories
- **Gesture Tests**: Verify long press, swipe, and tap gestures
- **Speed Memory Tests**: Verify settings persistence
- **UI Tests**: Verify component rendering and interactions
- **Performance Tests**: Verify smooth playback and responsiveness

## 📋 Issue Guidelines

### Reporting Bugs
```markdown
**Bug Description**
Clear description of the issue

**Steps to Reproduce**
1. Step one
2. Step two
3. Step three

**Expected Behavior**
What should happen

**Actual Behavior**
What actually happens

**Environment**
- Device: [e.g., Samsung Galaxy S21]
- Android Version: [e.g., 12]
- App Version: [e.g., 1.0.0]

**Screenshots/Logs**
Include relevant screenshots or log output
```

### Feature Requests
```markdown
**Feature Description**
Clear description of the proposed feature

**Problem Statement**
What problem does this solve?

**Proposed Solution**
How should this feature work?

**Alternatives Considered**
Other approaches you've considered

**Additional Context**
Any other relevant information
```

## 🏆 Recognition

Contributors will be recognized in:
- README.md contributors section
- Release notes for significant contributions
- Special mentions for outstanding contributions

### Contributor Levels
- **Code Contributors**: Direct code contributions
- **Documentation Contributors**: Improve docs and guides
- **Community Contributors**: Help with issues and discussions
- **Beta Testers**: Test new features and report issues

## 📞 Getting Help

- **GitHub Discussions**: General questions and feature discussions
- **GitHub Issues**: Bug reports and specific problems
- **Code Reviews**: Learn from feedback on your PRs

## 🎯 Priority Areas

We're especially looking for help with:

### High Priority
- **Performance optimization** - Smooth playback improvements
- **Gesture accuracy** - Better touch detection and response
- **Memory management** - Reduce app memory footprint
- **Battery optimization** - Improve power efficiency

### Medium Priority
- **UI/UX improvements** - Better user experience
- **Accessibility features** - Screen reader support, high contrast
- **Testing coverage** - More comprehensive test suite
- **Documentation** - Better guides and examples

### Future Features
- **Statistics dashboard** - Usage analytics and insights
- **Export/Import settings** - Backup and restore functionality
- **AI-powered features** - Smart speed recommendations
- **Cross-device sync** - Settings synchronization

## 🔒 Security

If you discover a security vulnerability, please:
1. **DO NOT** open a public issue
2. Email the maintainers directly
3. Provide detailed information about the vulnerability
4. Allow time for the issue to be addressed before disclosure

Thank you for contributing to AstralStream! 🚀