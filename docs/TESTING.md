# 🧪 AstralStream Testing Guide

## Overview

This guide covers the comprehensive testing strategy for AstralStream Elite, including unit tests, integration tests, UI tests, and performance benchmarks. We maintain **85%+ test coverage** across all modules.

## Test Structure

```
tests/
├── unit/                    # Unit tests (ViewModels, Use Cases, Utils)
├── integration/             # Integration tests (Database, API)
├── ui/                      # UI/Compose tests
├── performance/             # Performance benchmarks
└── testdata/               # Test videos, subtitles, fixtures
```

## Running Tests

### All Tests
```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew testDebugUnitTestCoverage

# Generate HTML coverage report
./gradlew jacocoTestReport
```

### Unit Tests Only
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew test --tests "com.astralplayer.viewmodel.MainViewModelTest"

# Run with verbose output
./gradlew test --info
```

### Instrumentation Tests
```bash
# Run on connected device/emulator
./gradlew connectedAndroidTest

# Run specific instrumentation test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.astralplayer.ui.VideoPlayerUITest
```

## Unit Testing

### ViewModels

Example test for VideoPlayerViewModel:

```kotlin
@ExperimentalCoroutinesApi
class VideoPlayerViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    @MockK
    private lateinit var playVideoUseCase: PlayVideoUseCase
    
    @MockK
    private lateinit var generateSubtitlesUseCase: GenerateSubtitlesUseCase
    
    private lateinit var viewModel: VideoPlayerViewModel
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = VideoPlayerViewModel(playVideoUseCase, generateSubtitlesUseCase)
    }
    
    @Test
    fun `playVideo should update state to playing`() = runTest {
        // Given
        val videoUri = "content://test/video.mp4"
        val expectedResult = Result.Success(
            PlaybackInfo(uri = videoUri, duration = 120000L)
        )
        
        coEvery { playVideoUseCase(videoUri) } returns flowOf(expectedResult)
        
        // When
        viewModel.playVideo(videoUri)
        advanceUntilIdle()
        
        // Then
        assertEquals(
            PlaybackState.Playing(position = 0L, duration = 120000L, bufferedPercentage = 0),
            viewModel.playbackState.value
        )
        coVerify { playVideoUseCase(videoUri) }
    }
    
    @Test
    fun `generateSubtitles should handle errors gracefully`() = runTest {
        // Given
        val error = SubtitleGenerationException("API key invalid")
        coEvery { 
            generateSubtitlesUseCase(any(), any(), any()) 
        } returns flowOf(Result.Error(error))
        
        // When
        viewModel.generateSubtitles()
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.subtitleState.value is SubtitleState.Error)
        assertEquals(error.message, (viewModel.subtitleState.value as SubtitleState.Error).message)
    }
}
```

### Use Cases

Example test for GenerateSubtitlesUseCase:

```kotlin
class GenerateSubtitlesUseCaseTest {
    
    @MockK
    private lateinit var subtitleRepository: SubtitleRepository
    
    @MockK
    private lateinit var aiServiceRepository: AIServiceRepository
    
    private lateinit var useCase: GenerateSubtitlesUseCase
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        useCase = GenerateSubtitlesUseCase(subtitleRepository, aiServiceRepository)
    }
    
    @Test
    fun `should return cached subtitles when available`() = runTest {
        // Given
        val videoUri = "test.mp4"
        val language = "en"
        val cachedSubtitles = listOf(
            Subtitle("1", 0L, 5000L, "Hello world", language)
        )
        
        coEvery { 
            subtitleRepository.getCachedSubtitles(videoUri, language) 
        } returns cachedSubtitles
        
        // When
        val result = useCase(videoUri, language).first()
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(cachedSubtitles, (result as Result.Success).data)
        coVerify(exactly = 0) { aiServiceRepository.generateWithBestProvider(any(), any()) }
    }
    
    @Test
    fun `should generate subtitles when cache miss`() = runTest {
        // Given
        val videoUri = "test.mp4"
        val language = "en"
        val generatedSubtitles = listOf(
            Subtitle("1", 0L, 5000L, "Generated text", language)
        )
        
        coEvery { 
            subtitleRepository.getCachedSubtitles(videoUri, language) 
        } returns null
        
        coEvery { 
            aiServiceRepository.generateWithBestProvider(videoUri, language) 
        } returns generatedSubtitles
        
        coEvery { 
            subtitleRepository.cacheSubtitles(any(), any(), any()) 
        } just Runs
        
        // When
        val results = useCase(videoUri, language).toList()
        
        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
        assertEquals(generatedSubtitles, (results[1] as Result.Success).data)
        coVerify { subtitleRepository.cacheSubtitles(videoUri, language, generatedSubtitles) }
    }
}
```

### Repository Tests

```kotlin
class VideoRepositoryImplTest {
    
    @get:Rule
    val databaseRule = DatabaseTestRule()
    
    private lateinit var repository: VideoRepositoryImpl
    private lateinit var videoDao: VideoDao
    
    @Before
    fun setup() {
        videoDao = databaseRule.database.videoDao()
        repository = VideoRepositoryImpl(videoDao)
    }
    
    @Test
    fun `getRecentVideos should return videos ordered by date`() = runTest {
        // Given
        val oldVideo = createVideo(title = "Old", addedDate = 1000L)
        val newVideo = createVideo(title = "New", addedDate = 2000L)
        
        videoDao.insertVideos(oldVideo, newVideo)
        
        // When
        val videos = repository.getRecentVideos().first()
        
        // Then
        assertEquals(2, videos.size)
        assertEquals("New", videos[0].title)
        assertEquals("Old", videos[1].title)
    }
}
```

## UI Testing

### Compose Tests

```kotlin
class VideoPlayerScreenTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    
    @Test
    fun videoPlayerControls_areDisplayed() {
        // Given
        val state = VideoPlayerState(
            isPlaying = false,
            currentPosition = 30000L,
            duration = 120000L
        )
        
        // When
        composeTestRule.setContent {
            EliteVideoPlayerScreen(
                state = state,
                onPlayPause = {},
                onSeek = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithTag("play_pause_button")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithTag("seek_bar")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("0:30 / 2:00")
            .assertIsDisplayed()
    }
    
    @Test
    fun subtitleButton_whenClicked_showsProviderDialog() {
        // When
        composeTestRule.setContent {
            EliteVideoPlayerScreen(
                state = VideoPlayerState(),
                onPlayPause = {},
                onGenerateSubtitles = {}
            )
        }
        
        // Click subtitle button
        composeTestRule
            .onNodeWithContentDescription("Generate Subtitles")
            .performClick()
        
        // Then
        composeTestRule
            .onNodeWithText("Select AI Provider")
            .assertIsDisplayed()
        
        // Verify all providers are shown
        listOf("Auto", "OpenAI", "Google", "Azure", "AssemblyAI", "Deepgram").forEach { provider ->
            composeTestRule
                .onNodeWithText(provider)
                .assertIsDisplayed()
        }
    }
}
```

### Screenshot Tests

```kotlin
class VideoPlayerScreenshotTest {
    
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight"
    )
    
    @Test
    fun videoPlayer_lightTheme() {
        paparazzi.snapshot {
            EliteVideoPlayerScreen(
                state = VideoPlayerState(
                    isPlaying = true,
                    currentPosition = 45000L,
                    duration = 180000L,
                    subtitles = listOf(
                        Subtitle("1", 44000L, 47000L, "This is a subtitle", "en")
                    )
                )
            )
        }
    }
    
    @Test
    fun videoPlayer_darkTheme() {
        paparazzi.snapshot(theme = "android:Theme.Material3.DayNight.NoActionBar") {
            EliteVideoPlayerScreen(
                state = VideoPlayerState(
                    isPlaying = true,
                    isDarkTheme = true
                )
            )
        }
    }
}
```

## Integration Testing

### Database Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class AstralStreamDatabaseTest {
    
    private lateinit var database: AstralStreamDatabase
    private lateinit var videoDao: VideoDao
    
    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AstralStreamDatabase::class.java
        ).build()
        videoDao = database.videoDao()
    }
    
    @After
    fun closeDatabase() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveVideo() = runTest {
        // Given
        val video = Video(
            id = "test-123",
            uri = "content://test/video.mp4",
            title = "Test Video",
            duration = 60000L
        )
        
        // When
        videoDao.insert(video)
        val retrieved = videoDao.getVideoById("test-123")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(video.title, retrieved?.title)
        assertEquals(video.duration, retrieved?.duration)
    }
}
```

### API Tests

```kotlin
class AIServiceApiTest {
    
    @get:Rule
    val mockWebServer = MockWebServer()
    
    private lateinit var api: OpenAIApi
    
    @Before
    fun setup() {
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(OpenAIApi::class.java)
    }
    
    @Test
    fun transcribeAudio_success() = runTest {
        // Given
        val mockResponse = """
            {
                "text": "Hello world",
                "segments": [
                    {
                        "start": 0.0,
                        "end": 2.0,
                        "text": "Hello world"
                    }
                ]
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
        )
        
        // When
        val result = api.transcribeAudio(
            audioFile = createTestAudioFile(),
            model = "whisper-1",
            language = "en"
        )
        
        // Then
        assertEquals("Hello world", result.text)
        assertEquals(1, result.segments.size)
    }
}
```

## Performance Testing

### Benchmark Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class VideoPlayerBenchmark {
    
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    
    @Test
    fun measureVideoStartup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                // Setup
                val intent = Intent(context, EnhancedVideoPlayerActivity::class.java).apply {
                    putExtra("video_uri", "content://test/video.mp4")
                }
            }
            
            // Measure
            context.startActivity(intent)
        }
    }
    
    @Test
    fun measureSubtitleGeneration() {
        val useCase = GenerateSubtitlesUseCase(
            mockSubtitleRepository,
            mockAIServiceRepository
        )
        
        benchmarkRule.measureRepeated {
            runBlocking {
                useCase("test.mp4", "en", AIProvider.OPENAI).collect()
            }
        }
    }
}
```

### Memory Leak Testing

```kotlin
class MemoryLeakTest {
    
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(EnhancedVideoPlayerActivity::class.java)
    
    @Test
    fun videoPlayer_noMemoryLeaks() {
        // Given
        var weakReference: WeakReference<EnhancedVideoPlayerActivity>? = null
        
        activityScenarioRule.scenario.onActivity { activity ->
            weakReference = WeakReference(activity)
            
            // Simulate video playback
            activity.setVideoUri("content://test/video.mp4")
            activity.play()
        }
        
        // When - Destroy activity
        activityScenarioRule.scenario.recreate()
        
        // Force garbage collection
        Runtime.getRuntime().gc()
        Thread.sleep(1000)
        Runtime.getRuntime().gc()
        
        // Then - Activity should be garbage collected
        assertNull(weakReference?.get())
    }
}
```

## Test Utilities

### Test Dispatcher Rule

```kotlin
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }
    
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

### Database Test Rule

```kotlin
class DatabaseTestRule : TestWatcher() {
    
    lateinit var database: AstralStreamDatabase
        private set
    
    override fun starting(description: Description) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AstralStreamDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }
    
    override fun finished(description: Description) {
        database.close()
    }
}
```

### Test Data Builders

```kotlin
object TestDataBuilder {
    
    fun createVideo(
        id: String = UUID.randomUUID().toString(),
        title: String = "Test Video",
        uri: String = "content://test/video.mp4",
        duration: Long = 60000L,
        addedDate: Long = System.currentTimeMillis()
    ) = Video(
        id = id,
        title = title,
        uri = uri,
        duration = duration,
        addedDate = addedDate
    )
    
    fun createSubtitle(
        text: String = "Test subtitle",
        startTime: Long = 0L,
        endTime: Long = 5000L,
        language: String = "en"
    ) = Subtitle(
        id = UUID.randomUUID().toString(),
        text = text,
        startTime = startTime,
        endTime = endTime,
        language = language
    )
    
    fun createPlaybackState(
        isPlaying: Boolean = true,
        position: Long = 0L,
        duration: Long = 120000L
    ) = if (isPlaying) {
        PlaybackState.Playing(position, duration, 0)
    } else {
        PlaybackState.Paused(position)
    }
}
```

## Code Coverage

### Coverage Requirements

- **Overall**: 85% minimum
- **ViewModels**: 90% minimum
- **Use Cases**: 95% minimum
- **Repositories**: 85% minimum
- **UI Components**: 80% minimum

### Viewing Coverage Reports

After running tests with coverage:

```bash
# Generate report
./gradlew jacocoTestReport

# Open in browser
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

### Excluding from Coverage

```kotlin
// Exclude data classes
@ExcludeFromCoverage
data class VideoMetadata(...)

// Exclude generated code
@Generated
class GeneratedBinding { }
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Run Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        
    - name: Run Unit Tests
      run: ./gradlew test
      
    - name: Generate Coverage Report
      run: ./gradlew jacocoTestReport
      
    - name: Upload Coverage
      uses: codecov/codecov-action@v3
      with:
        file: ./app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
        
    - name: Run Instrumentation Tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 29
        script: ./gradlew connectedAndroidTest
```

## Best Practices

1. **Test Naming**: Use descriptive names with backticks
   ```kotlin
   @Test
   fun `should return error when network unavailable`() { }
   ```

2. **Arrange-Act-Assert**: Structure tests clearly
   ```kotlin
   @Test
   fun testExample() {
       // Given (Arrange)
       val input = "test"
       
       // When (Act)
       val result = functionUnderTest(input)
       
       // Then (Assert)
       assertEquals(expected, result)
   }
   ```

3. **Mock Judiciously**: Only mock what you don't own
4. **Test Behavior**: Focus on what, not how
5. **Keep Tests Fast**: Use test doubles and in-memory databases
6. **Test Edge Cases**: Null values, empty collections, exceptions
7. **Avoid Flaky Tests**: Use proper synchronization for async code

## Troubleshooting

### Common Issues

1. **Flaky Coroutine Tests**
   ```kotlin
   // Use runTest instead of runBlocking
   @Test
   fun myTest() = runTest {
       // Test code
   }
   ```

2. **MockK Verification Failures**
   ```kotlin
   // Relax mocks for non-critical verifications
   @MockK(relaxed = true)
   private lateinit var repository: VideoRepository
   ```

3. **Compose Test Timing**
   ```kotlin
   // Wait for animations
   composeTestRule.mainClock.autoAdvance = false
   composeTestRule.mainClock.advanceTimeBy(500)
   ```

---

For more testing examples and patterns, see the [test directory](https://github.com/yourusername/AstralStream/tree/main/app/src/test) in the repository.