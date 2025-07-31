# 🏗️ AstralStream Architecture Guide

## Overview

AstralStream follows **Clean Architecture** principles with clear separation of concerns and dependency rules. This document explains the architectural decisions and patterns used throughout the application.

## Architecture Layers

### 1. Presentation Layer

The outermost layer containing UI components and user interaction logic.

```
presentation/
├── player/
│   ├── EnhancedVideoPlayerActivity.kt
│   ├── EliteVideoPlayerScreen.kt
│   └── EnhancedVideoPlayerViewModel.kt
├── screens/
│   ├── HomeScreen.kt
│   ├── PlaylistScreen.kt
│   └── SettingsScreen.kt
└── components/
    ├── VideoThumbnail.kt
    └── EliteControls.kt
```

**Responsibilities:**
- UI rendering (Compose)
- User input handling
- ViewModel state observation
- Navigation

**Key Patterns:**
- MVVM (Model-View-ViewModel)
- Unidirectional Data Flow (UDF)
- Compose State Management

### 2. Domain Layer

The business logic layer, independent of any framework or external dependency.

```
domain/
├── model/
│   ├── VideoMetadata.kt
│   ├── Subtitle.kt
│   └── PlaybackState.kt
├── repository/
│   ├── VideoRepository.kt
│   ├── SubtitleRepository.kt
│   └── PlayerRepository.kt
└── usecase/
    ├── player/
    │   ├── PlayVideoUseCase.kt
    │   └── ControlPlaybackUseCase.kt
    └── subtitle/
        ├── GenerateSubtitlesUseCase.kt
        └── LoadSubtitlesUseCase.kt
```

**Responsibilities:**
- Business rules and logic
- Use case orchestration
- Domain model definitions
- Repository interfaces

**Key Principles:**
- No Android dependencies
- Pure Kotlin
- Testable business logic

### 3. Data Layer

Handles data operations and external service integrations.

```
data/
├── repository/
│   ├── VideoRepositoryImpl.kt
│   ├── SubtitleRepositoryImpl.kt
│   └── SettingsRepositoryImpl.kt
├── datasource/
│   ├── local/
│   │   ├── VideoDao.kt
│   │   └── AstralStreamDatabase.kt
│   └── remote/
│       ├── AIServiceApi.kt
│       └── CloudStorageApi.kt
└── model/
    ├── VideoEntity.kt
    └── mappers/
        └── VideoMapper.kt
```

**Responsibilities:**
- Repository implementations
- Data source management
- Entity-to-domain mapping
- Caching strategies

## Key Architectural Patterns

### 1. Dependency Injection

Using Hilt for compile-time safe dependency injection:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideVideoRepository(
        dao: VideoDao,
        api: VideoApi
    ): VideoRepository = VideoRepositoryImpl(dao, api)
}
```

### 2. Reactive Programming

Kotlin Coroutines and Flow for asynchronous operations:

```kotlin
class VideoPlayerViewModel @Inject constructor(
    private val playVideoUseCase: PlayVideoUseCase
) : ViewModel() {
    
    fun playVideo(uri: String) {
        viewModelScope.launch {
            playVideoUseCase(uri)
                .flowOn(Dispatchers.IO)
                .collect { result ->
                    when (result) {
                        is Success -> updateState(playing = true)
                        is Error -> handleError(result.exception)
                    }
                }
        }
    }
}
```

### 3. Repository Pattern

Abstracts data sources from business logic:

```kotlin
interface VideoRepository {
    suspend fun getVideo(id: String): Video?
    suspend fun getRecentVideos(): Flow<List<Video>>
    suspend fun savePlaybackPosition(id: String, position: Long)
}

class VideoRepositoryImpl(
    private val localDataSource: VideoDao,
    private val remoteDataSource: VideoApi
) : VideoRepository {
    // Implementation with caching strategy
}
```

### 4. Use Case Pattern

Encapsulates business logic in single-responsibility classes:

```kotlin
class GenerateSubtitlesUseCase @Inject constructor(
    private val subtitleRepository: SubtitleRepository,
    private val aiServiceRepository: AIServiceRepository
) {
    suspend operator fun invoke(
        videoUri: String,
        language: String = "en"
    ): Flow<Result> = flow {
        emit(Result.Loading)
        
        try {
            // Business logic here
            val subtitles = aiServiceRepository.generate(videoUri, language)
            subtitleRepository.save(subtitles)
            emit(Result.Success(subtitles))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }
}
```

## Data Flow

```
User Action → UI → ViewModel → UseCase → Repository → DataSource
     ↑                ↓           ↓           ↓           ↓
     └────────────────┴───────────┴───────────┴───────────┘
                          State Updates
```

## Module Structure

### Core Modules

- **:core:common** - Shared utilities and extensions
- **:core:ui** - Common UI components and themes
- **:core:data** - Base data layer components
- **:core:domain** - Base domain layer components

### Feature Modules

- **:feature:player** - Video player feature
- **:feature:subtitle** - Subtitle generation feature
- **:feature:settings** - App settings feature

## Testing Strategy

### Unit Tests
- ViewModels: 100% coverage
- Use Cases: 100% coverage
- Repositories: Mock data sources

### Integration Tests
- Database operations
- API integrations
- Feature flows

### UI Tests
- Compose test rules
- User journey tests
- Screenshot tests

## Performance Considerations

### Startup Performance
- Lazy initialization
- Parallel coroutine execution
- Baseline profiles

### Memory Management
- Proper lifecycle handling
- Image/video cache limits
- Leak monitoring

### Network Optimization
- Response caching
- Request batching
- Retry policies

## Security Architecture

### Network Security
- Certificate pinning
- TLS 1.3 enforcement
- API key rotation

### Data Security
- AES-256 encryption
- Android Keystore usage
- Biometric authentication

### Code Security
- ProGuard obfuscation
- String encryption
- Anti-tampering checks

## Best Practices

1. **Single Responsibility**: Each class has one reason to change
2. **Interface Segregation**: Depend on abstractions, not concretions
3. **Dependency Inversion**: High-level modules don't depend on low-level modules
4. **Open/Closed Principle**: Open for extension, closed for modification
5. **Liskov Substitution**: Derived classes must be substitutable for base classes

## Decision Records

### ADR-001: Clean Architecture
**Status**: Accepted  
**Context**: Need scalable, testable architecture  
**Decision**: Implement Clean Architecture with clear layer separation  
**Consequences**: More boilerplate but better maintainability  

### ADR-002: Jetpack Compose
**Status**: Accepted  
**Context**: Modern UI framework needed  
**Decision**: Use Compose for all new UI  
**Consequences**: Better performance, less code, reactive UI  

### ADR-003: Coroutines over RxJava
**Status**: Accepted  
**Context**: Async programming approach  
**Decision**: Use Kotlin Coroutines and Flow  
**Consequences**: Native Kotlin support, lighter dependency  

---

For implementation examples and detailed API documentation, see the [API Guide](API.md).