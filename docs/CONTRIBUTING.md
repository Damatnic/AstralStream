# 🤝 Contributing to AstralStream Elite

Thank you for your interest in contributing to AstralStream Elite! This guide will help you get started with contributing to our professional-grade video player.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Setup](#development-setup)
4. [Architecture Overview](#architecture-overview)
5. [Making Changes](#making-changes)
6. [Testing](#testing)
7. [Submitting Changes](#submitting-changes)
8. [Code Style](#code-style)
9. [Documentation](#documentation)
10. [Expert Agent System](#expert-agent-system)

## Code of Conduct

We are committed to providing a welcoming and inclusive environment. Please read and follow our code of conduct:

- Be respectful and inclusive
- Welcome newcomers and help them get started
- Focus on constructive criticism
- Respect differing viewpoints and experiences
- Show empathy towards other community members

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or newer
- JDK 11 or higher
- Git
- Android device or emulator (API 26+)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork:
   ```bash
   git clone https://github.com/yourusername/AstralStream.git
   cd AstralStream
   ```

3. Add upstream remote:
   ```bash
   git remote add upstream https://github.com/originalowner/AstralStream.git
   ```

## Development Setup

### 1. Install Dependencies

```bash
# Install Android SDK if not already installed
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### 2. Configure API Keys (Optional)

For AI features, create `local.properties`:
```properties
OPENAI_API_KEY=your_key_here
GOOGLE_AI_API_KEY=your_key_here
AZURE_SPEECH_API_KEY=your_key_here
ASSEMBLY_AI_API_KEY=your_key_here
DEEPGRAM_API_KEY=your_key_here
```

### 3. Build the Project

```bash
./gradlew build
```

### 4. Run Tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Architecture Overview

AstralStream follows Clean Architecture principles:

```
presentation/     # UI layer (Activities, Compose, ViewModels)
domain/          # Business logic (Use Cases, Domain Models)
data/            # Data layer (Repositories, APIs, Database)
core/            # Shared utilities and base classes
```

Key patterns:
- MVVM with Jetpack Compose
- Repository pattern
- Use Cases for business logic
- Dependency Injection with Hilt

## Making Changes

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

### 2. Follow the Architecture

When adding new features:

1. **Domain Layer First**: Define use cases and domain models
2. **Data Layer**: Implement repositories and data sources
3. **Presentation Layer**: Create UI and ViewModels
4. **Tests**: Write tests for each layer

### 3. Example: Adding a New Feature

```kotlin
// 1. Domain Layer - Use Case
class ShareVideoUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(videoId: String): Result<ShareableLink> {
        return try {
            val video = videoRepository.getVideo(videoId)
            val link = generateShareableLink(video)
            Result.Success(link)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

// 2. Data Layer - Repository Implementation
class VideoRepositoryImpl @Inject constructor(
    private val dao: VideoDao
) : VideoRepository {
    override suspend fun getVideo(id: String): Video {
        return dao.getVideoById(id) ?: throw VideoNotFoundException(id)
    }
}

// 3. Presentation Layer - ViewModel
class VideoShareViewModel @Inject constructor(
    private val shareVideoUseCase: ShareVideoUseCase
) : ViewModel() {
    fun shareVideo(videoId: String) {
        viewModelScope.launch {
            shareVideoUseCase(videoId).collect { result ->
                // Handle result
            }
        }
    }
}
```

## Testing

### Test Requirements

- **Unit Tests**: 90% coverage for ViewModels and Use Cases
- **Integration Tests**: Cover critical paths
- **UI Tests**: Test user journeys

### Writing Tests

```kotlin
// Unit Test Example
@Test
fun `shareVideo should generate shareable link`() = runTest {
    // Given
    val videoId = "test-123"
    val expectedLink = ShareableLink("https://share.link/test-123")
    
    coEvery { videoRepository.getVideo(videoId) } returns testVideo
    
    // When
    val result = shareVideoUseCase(videoId)
    
    // Then
    assertTrue(result is Result.Success)
    assertEquals(expectedLink, (result as Result.Success).data)
}

// UI Test Example
@Test
fun shareButton_whenClicked_showsShareDialog() {
    composeTestRule.setContent {
        VideoPlayerScreen(state = testState)
    }
    
    composeTestRule
        .onNodeWithContentDescription("Share")
        .performClick()
    
    composeTestRule
        .onNodeWithText("Share Video")
        .assertIsDisplayed()
}
```

## Submitting Changes

### 1. Commit Guidelines

Follow conventional commits:
```bash
feat: add video sharing functionality
fix: resolve playback issue on Android 10
docs: update API documentation
test: add tests for subtitle generation
refactor: extract video player logic to use case
```

### 2. Push Your Branch

```bash
git push origin feature/your-feature-name
```

### 3. Create Pull Request

1. Go to GitHub and create a pull request
2. Use the PR template
3. Link related issues
4. Ensure all checks pass

### 4. PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Tests added/updated
- [ ] Documentation updated
```

## Code Style

### Kotlin Style Guide

We follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) with these additions:

```kotlin
// File naming
VideoPlayerViewModel.kt  // PascalCase for classes
videoUtils.kt           // camelCase for files with functions

// Class structure
class ExampleClass {
    // Constants
    companion object {
        private const val CONSTANT = 100
    }
    
    // Properties
    private val property: String
    
    // Initialization
    init {
        // Init block
    }
    
    // Functions
    fun publicFunction() { }
    
    private fun privateFunction() { }
}

// Naming conventions
interface VideoPlayer           // Interfaces: PascalCase
class VideoPlayerImpl          // Implementations: append Impl
sealed class Result           // Sealed classes for states
data class VideoMetadata      // Data classes for models

// Function naming
fun calculateDuration(): Long  // Verb for actions
fun isPlaying(): Boolean      // is/has for boolean returns
suspend fun loadVideo()       // suspend for coroutines
```

### Code Organization

```kotlin
// Imports order
import android.*              // Android imports
import androidx.*            // AndroidX imports
import com.astralplayer.*    // App imports
import kotlinx.*             // Kotlin extensions
import java.*                // Java imports
import javax.*               // JavaX imports
import kotlin.*              // Kotlin imports

// Within a class
class ClassName {
    // 1. Companion object
    // 2. Properties
    // 3. Init blocks
    // 4. Constructors
    // 5. Override functions
    // 6. Public functions
    // 7. Private functions
    // 8. Inner classes
}
```

## Documentation

### Code Documentation

```kotlin
/**
 * Generates AI-powered subtitles for a video.
 * 
 * This use case handles subtitle generation using multiple AI providers
 * with automatic fallback support.
 *
 * @param videoUri The URI of the video to process
 * @param language Target language code (ISO 639-1)
 * @param provider AI provider to use, defaults to AUTO
 * @return Flow emitting the generation progress and result
 * 
 * @throws SubtitleGenerationException if all providers fail
 * @throws IllegalArgumentException if language code is invalid
 * 
 * @sample
 * ```
 * generateSubtitlesUseCase(
 *     videoUri = "content://video.mp4",
 *     language = "es",
 *     provider = AIProvider.OPENAI
 * ).collect { result ->
 *     when (result) {
 *         is Result.Success -> showSubtitles(result.data)
 *         is Result.Error -> showError(result.exception)
 *     }
 * }
 * ```
 */
class GenerateSubtitlesUseCase { }
```

### README Updates

When adding features, update relevant documentation:
- README.md - Feature list and examples
- API.md - Public API changes
- ARCHITECTURE.md - Architectural changes

## Expert Agent System

AstralStream uses an Expert Agent System for maintaining code quality:

### 1. TestCoverageAgent
- Maintains 85%+ test coverage
- Reviews PRs for test completeness
- Suggests missing test cases

### 2. ArchitectureAgent
- Ensures Clean Architecture compliance
- Reviews architectural decisions
- Validates dependency directions

### 3. SecurityAgent
- Reviews security implications
- Checks for vulnerabilities
- Validates encryption usage

### 4. PerformanceAgent
- Monitors performance impacts
- Reviews memory usage
- Validates startup time

### 5. DocumentationAgent
- Ensures documentation completeness
- Reviews API documentation
- Validates code comments

### Working with Agents

The agents automatically review PRs and provide feedback. Address their comments before merging.

## Development Tips

### 1. Use Android Studio Tools

- **Profiler**: Monitor performance
- **Layout Inspector**: Debug UI
- **Database Inspector**: View Room data

### 2. Debugging

```kotlin
// Use Timber for logging
Timber.d("Video playback started: $videoUri")
Timber.e(exception, "Failed to generate subtitles")

// Use breakpoints effectively
// Conditional breakpoints for specific cases
```

### 3. Performance

- Use Baseline Profiles for startup optimization
- Profile before and after changes
- Monitor memory usage

### 4. Stay Updated

```bash
# Sync with upstream
git fetch upstream
git checkout main
git merge upstream/main

# Update dependencies
./gradlew dependencyUpdates
```

## Getting Help

- **Discord**: [Join our community](https://discord.gg/astralstream)
- **Issues**: [Browse existing issues](https://github.com/yourusername/AstralStream/issues)
- **Discussions**: [Start a discussion](https://github.com/yourusername/AstralStream/discussions)

## Recognition

Contributors are recognized in:
- README.md contributors section
- Release notes
- Annual contributor spotlight

Thank you for contributing to AstralStream Elite! 🚀

---

**Questions?** Feel free to ask in discussions or reach out to the maintainers.