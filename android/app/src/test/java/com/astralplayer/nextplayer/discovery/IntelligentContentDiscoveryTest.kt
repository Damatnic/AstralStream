package com.astralplayer.nextplayer.discovery

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFalse

/**
 * Comprehensive tests for intelligent content discovery
 * Tests content indexing, recommendations, search, similarity discovery, and insights
 */
@RunWith(AndroidJUnit4::class)
class IntelligentContentDiscoveryTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var contentDiscovery: IntelligentContentDiscovery
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        contentDiscovery = IntelligentContentDiscovery(context)
    }

    @After
    fun tearDown() {
        contentDiscovery.cleanup()
    }

    @Test
    fun testContentDiscoveryInitialization() = runTest {
        // When
        val result = contentDiscovery.initialize()
        advanceUntilIdle()
        
        // Then
        assertNotNull("Initialization result should not be null", result)
        assertTrue("Content discovery should initialize successfully", result.success)
        assertTrue("Should have available features", result.availableFeatures.isNotEmpty())
        assertTrue("Initialization time should be set", result.initializationTime > 0)
        
        // Verify available features
        val expectedFeatures = listOf(
            DiscoveryFeature.CONTENT_INDEXING,
            DiscoveryFeature.PERSONALIZED_RECOMMENDATIONS,
            DiscoveryFeature.SEMANTIC_SEARCH,
            DiscoveryFeature.VISUAL_SEARCH,
            DiscoveryFeature.SIMILARITY_DISCOVERY,
            DiscoveryFeature.USER_BEHAVIOR_TRACKING,
            DiscoveryFeature.CONTENT_INSIGHTS
        )
        
        expectedFeatures.forEach { feature ->
            assertTrue("Should have $feature", result.availableFeatures.contains(feature))
        }
        
        // Verify state
        val state = contentDiscovery.discoveryState.value
        assertTrue("System should be initialized", state.isInitialized)
        assertTrue("Should have available features", state.availableFeatures.isNotEmpty())
        assertEquals("Indexed content count should match", result.indexedContentCount, state.totalIndexedContent)
    }

    @Test
    fun testContentScanningAndIndexing() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        val contentSources = listOf(
            ContentSource(
                id = "local_videos",
                name = "Local Videos",
                type = ContentSourceType.LOCAL_STORAGE,
                path = "/storage/emulated/0/Movies",
                scanEnabled = true
            ),
            ContentSource(
                id = "downloads",
                name = "Downloads",
                type = ContentSourceType.LOCAL_STORAGE,
                path = "/storage/emulated/0/Download",
                scanEnabled = true
            )
        )
        
        val scanConfig = ScanConfig(
            deepScan = true,
            extractMetadata = true,
            generateThumbnails = true,
            analyzeContent = true,
            detectDuplicates = true,
            allowDuplicates = false
        )
        
        // When
        val result = contentDiscovery.scanAndIndexContent(contentSources, scanConfig)
        
        // Then
        assertNotNull("Scan result should not be null", result)
        assertTrue("Content scan should succeed", result.success)
        assertTrue("Total scanned should be non-negative", result.totalScanned >= 0)
        assertTrue("Total indexed should be non-negative", result.totalIndexed >= 0)
        assertTrue("Total indexed should not exceed scanned", result.totalIndexed <= result.totalScanned)
        assertTrue("Duplicates found should be non-negative", result.duplicatesFound >= 0)
        assertNotNull("Errors list should not be null", result.errors)
        
        // Verify state update
        val state = contentDiscovery.discoveryState.value
        assertFalse("Scan should not be in progress after completion", state.scanInProgress)
        assertTrue("Last scan time should be recent", 
                  System.currentTimeMillis() - state.lastScanTime < 10000)
    }

    @Test
    fun testPersonalizedRecommendations() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        // Simulate some indexed content first
        val sources = listOf(
            ContentSource("test", "Test Source", ContentSourceType.LOCAL_STORAGE, "/test")
        )
        contentDiscovery.scanAndIndexContent(sources)
        advanceUntilIdle()
        
        val userId = "test_user_123"
        val context = RecommendationContext(
            timeOfDay = TimeOfDay.EVENING,
            dayOfWeek = DayOfWeek.FRIDAY,
            deviceType = DeviceType.MOBILE,
            networkType = NetworkType.WIFI,
            mood = Mood.RELAXED,
            socialContext = SocialContext.ALONE
        )
        
        // When
        val result = contentDiscovery.getPersonalizedRecommendations(userId, context, 10)
        
        // Then
        assertNotNull("Recommendation result should not be null", result)
        assertTrue("Recommendations should be generated successfully", result.success)
        assertTrue("Should have recommendations", result.recommendations.isNotEmpty())
        assertTrue("Should not exceed requested limit", result.recommendations.size <= 10)
        assertTrue("Diversity score should be valid", 
                  result.diversityScore >= 0f && result.diversityScore <= 1f)
        assertNotNull("Context info should be provided", result.contextInfo)
        assertTrue("Generation time should be set", result.generationTime > 0)
        
        // Verify recommendation structure
        result.recommendations.forEach { recommendation ->
            assertNotNull("Content item should not be null", recommendation.item)
            assertTrue("Score should be valid", 
                      recommendation.score >= 0f && recommendation.score <= 1f)
            assertNotNull("Reason should be provided", recommendation.reason)
            assertTrue("Confidence should be valid", 
                      recommendation.confidence >= 0f && recommendation.confidence <= 1f)
            assertNotNull("Recommendation source should be provided", recommendation.recommendationSource)
        }
        
        // Verify context info
        val contextInfo = result.contextInfo!!
        assertTrue("Context score should be valid", 
                  contextInfo.contextScore >= 0f && contextInfo.contextScore <= 1f)
        assertTrue("Confidence should be valid", 
                  contextInfo.confidence >= 0f && contextInfo.confidence <= 1f)
        assertNotNull("Relevant factors should not be null", contextInfo.relevantFactors)
    }

    @Test
    fun testContentSearch() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        // Index some content first
        val sources = listOf(
            ContentSource("test", "Test Source", ContentSourceType.LOCAL_STORAGE, "/test")
        )
        contentDiscovery.scanAndIndexContent(sources)
        advanceUntilIdle()
        
        val searchTypes = listOf(
            SearchType.KEYWORD,
            SearchType.SEMANTIC,
            SearchType.VISUAL,
            SearchType.AUDIO,
            SearchType.HYBRID
        )
        
        val query = "action movies"
        val filters = SearchFilters(
            categories = listOf(ContentCategory.ACTION, ContentCategory.ADVENTURE),
            minDuration = 60 * 60 * 1000L, // 1 hour
            maxDuration = 3 * 60 * 60 * 1000L, // 3 hours
            minQuality = 0.5f,
            sortBy = SortBy.RELEVANCE,
            sortOrder = SortOrder.DESCENDING
        )
        
        searchTypes.forEach { searchType ->
            // When
            val result = contentDiscovery.searchContent(query, searchType, filters, 20)
            
            // Then
            assertNotNull("Search result should not be null for $searchType", result)
            assertTrue("Search should succeed for $searchType", result.success)
            assertEquals("Query should match", query, result.query)
            assertEquals("Search type should match", searchType, result.searchType)
            assertTrue("Results should not exceed limit", result.results.size <= 20)
            assertTrue("Total results should be non-negative", result.totalResults >= 0)
            assertTrue("Search time should be set", result.searchTime > 0)
            
            // Verify search result items
            result.results.forEach { resultItem ->
                assertNotNull("Content item should not be null", resultItem.item)
                assertTrue("Relevance score should be valid", 
                          resultItem.relevanceScore >= 0f && resultItem.relevanceScore <= 1f)
                assertNotNull("Match type should be provided", resultItem.matchType)
                assertNotNull("Highlights should not be null", resultItem.highlights)
            }
        }
    }

    @Test
    fun testSimilarContentDiscovery() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        // Index content first
        val sources = listOf(
            ContentSource("test", "Test Source", ContentSourceType.LOCAL_STORAGE, "/test")
        )
        contentDiscovery.scanAndIndexContent(sources)
        advanceUntilIdle()
        
        val referenceItemId = "test_item_1"
        val similarityTypes = listOf(
            SimilarityType.CONTENT_BASED,
            SimilarityType.COLLABORATIVE,
            SimilarityType.METADATA,
            SimilarityType.VISUAL,
            SimilarityType.AUDIO,
            SimilarityType.HYBRID
        )
        
        similarityTypes.forEach { similarityType ->
            // When
            val result = contentDiscovery.discoverSimilarContent(referenceItemId, similarityType, 10)
            
            // Depending on implementation, this might succeed or fail based on whether test content exists
            assertNotNull("Similar content result should not be null for $similarityType", result)
            
            if (result.success) {
                assertNotNull("Reference item should be provided", result.referenceItem)
                assertTrue("Similar items should not exceed limit", result.similarItems.size <= 10)
                assertEquals("Similarity type should match", similarityType, result.similarityType)
                assertTrue("Discovery time should be set", result.discoveryTime > 0)
                
                // Verify similar items structure
                result.similarItems.forEach { similarItem ->
                    assertNotNull("Similar item should not be null", similarItem.item)
                    assertTrue("Similarity score should be valid", 
                              similarItem.similarityScore >= 0f && similarItem.similarityScore <= 1f)
                    assertNotNull("Similarity reasons should not be null", similarItem.similarityReasons)
                }
            } else {
                // If it fails, it should have an error message
                assertNotNull("Should have error message when failing", result.error)
            }
        }
    }

    @Test
    fun testUserInteractionTracking() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        val userId = "test_user_456"
        val interactions = listOf(
            UserInteraction(
                userId = userId,
                contentId = "content_1",
                type = InteractionType.VIEW,
                timestamp = System.currentTimeMillis(),
                duration = 30 * 60 * 1000L, // 30 minutes
                position = 15 * 60 * 1000L  // 15 minutes
            ),
            UserInteraction(
                userId = userId,
                contentId = "content_2",
                type = InteractionType.LIKE,
                timestamp = System.currentTimeMillis(),
                rating = 4.5f
            ),
            UserInteraction(
                userId = userId,
                contentId = "content_3",
                type = InteractionType.SHARE,
                timestamp = System.currentTimeMillis()
            )
        )
        
        interactions.forEach { interaction ->
            // When
            val result = contentDiscovery.trackUserInteraction(userId, interaction)
            
            // Then
            assertNotNull("Tracking result should not be null", result)
            assertTrue("Interaction tracking should succeed", result.success)
            assertNotNull("Updated profile should be provided", result.updatedProfile)
            assertTrue("Tracking time should be set", result.trackingTime > 0)
            
            // Verify updated profile
            val profile = result.updatedProfile!!
            assertEquals("User ID should match", userId, profile.userId)
            assertTrue("Last updated should be recent", 
                      System.currentTimeMillis() - profile.lastUpdated < 5000)
            assertTrue("Engagement score should be reasonable", 
                      profile.engagementScore >= 0f)
        }
    }

    @Test
    fun testContentInsightsGeneration() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        val analysisTypes = listOf(
            AnalysisType.POPULARITY,
            AnalysisType.TRENDS,
            AnalysisType.USER_BEHAVIOR,
            AnalysisType.CONTENT_GAPS,
            AnalysisType.COMPREHENSIVE
        )
        
        val timeRanges = listOf(
            TimeRange.LAST_24_HOURS,
            TimeRange.LAST_7_DAYS,
            TimeRange.LAST_30_DAYS,
            TimeRange.LAST_90_DAYS
        )
        
        analysisTypes.forEach { analysisType ->
            timeRanges.forEach { timeRange ->
                // When
                val result = contentDiscovery.generateContentInsights(analysisType, timeRange)
                
                // Then
                assertNotNull("Insights result should not be null for $analysisType", result)
                assertTrue("Insights generation should succeed for $analysisType", result.success)
                assertTrue("Should have insights for $analysisType", result.insights.isNotEmpty())
                assertEquals("Analysis type should match", analysisType, result.analysisType)
                assertEquals("Time range should match", timeRange, result.timeRange)
                assertTrue("Generation time should be set", result.generationTime > 0)
                
                // Verify insights structure
                result.insights.forEach { insight ->
                    assertNotNull("Insight type should not be null", insight.type)
                    assertNotNull("Title should not be null", insight.title)
                    assertNotNull("Description should not be null", insight.description)
                    assertNotNull("Data should not be null", insight.data)
                    assertTrue("Score should be valid", insight.score >= 0f && insight.score <= 1f)
                    assertTrue("Generated timestamp should be recent", 
                              System.currentTimeMillis() - insight.generatedAt < 10000)
                    assertTrue("Valid until should be in the future", insight.validUntil > insight.generatedAt)
                }
            }
        }
    }

    @Test
    fun testContentDeliveryOptimization() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        val userId = "test_user_789"
        val deliveryContexts = listOf(
            DeliveryContext(
                deviceType = DeviceType.MOBILE,
                networkType = NetworkType.CELLULAR,
                networkQuality = NetworkQuality.FAIR,
                batteryLevel = 0.3f,
                storageAvailable = 500 * 1024 * 1024L, // 500MB
                cpuLoad = 0.7f,
                memoryUsage = 0.8f,
                timeOfDay = TimeOfDay.MORNING
            ),
            DeliveryContext(
                deviceType = DeviceType.TV,
                networkType = NetworkType.WIFI,
                networkQuality = NetworkQuality.EXCELLENT,
                batteryLevel = 1f,
                storageAvailable = 50L * 1024 * 1024 * 1024, // 50GB
                cpuLoad = 0.2f,
                memoryUsage = 0.3f,
                timeOfDay = TimeOfDay.EVENING
            )
        )
        
        deliveryContexts.forEach { context ->
            // When
            val result = contentDiscovery.optimizeContentDelivery(userId, context)
            
            // Then
            assertNotNull("Optimization result should not be null", result)
            assertTrue("Content delivery optimization should succeed", result.success)
            assertNotNull("Optimal format should be determined", result.optimalFormat)
            assertNotNull("Quality settings should be provided", result.qualitySettings)
            assertNotNull("Caching strategy should be determined", result.cachingStrategy)
            assertTrue("Should have delivery recommendations", result.deliveryRecommendations.isNotEmpty())
            assertTrue("Performance gain should be non-negative", result.estimatedPerformanceGain >= 0f)
            assertTrue("Optimization time should be set", result.optimizationTime > 0)
            
            // Verify quality settings
            val qualitySettings = result.qualitySettings!!
            assertNotNull("Resolution should be set", qualitySettings.resolution)
            assertTrue("Bitrate should be positive", qualitySettings.bitrate > 0)
            assertTrue("Frame rate should be positive", qualitySettings.frameRate > 0f)
            
            // Verify delivery recommendations
            result.deliveryRecommendations.forEach { recommendation ->
                assertNotNull("Recommendation type should not be null", recommendation.type)
                assertNotNull("Recommendation text should not be null", recommendation.recommendation)
                assertTrue("Confidence should be valid", 
                          recommendation.confidence >= 0f && recommendation.confidence <= 1f)
                assertNotNull("Impact should be described", recommendation.impact)
            }
            
            // Verify context-appropriate optimizations
            when (context.networkQuality) {
                NetworkQuality.LOW, NetworkQuality.FAIR -> {
                    assertTrue("Should recommend lower quality for poor network", 
                              qualitySettings.bitrate < 5000000L)
                }
                NetworkQuality.EXCELLENT -> {
                    assertTrue("Should allow higher quality for excellent network", 
                              qualitySettings.bitrate >= 3000000L)
                }
                else -> {
                    // Any reasonable bitrate is acceptable
                    assertTrue("Bitrate should be reasonable", qualitySettings.bitrate > 0)
                }
            }
        }
    }

    @Test
    fun testDiscoveryMetricsRetrieval() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        // Perform some operations to populate metrics
        val sources = listOf(
            ContentSource("test", "Test Source", ContentSourceType.LOCAL_STORAGE, "/test")
        )
        contentDiscovery.scanAndIndexContent(sources)
        
        val userId = "metrics_user"
        contentDiscovery.getPersonalizedRecommendations(userId)
        contentDiscovery.searchContent("test query")
        
        advanceUntilIdle()
        
        // When
        val metrics = contentDiscovery.getDiscoveryMetrics()
        
        // Then
        assertNotNull("Metrics should not be null", metrics)
        assertTrue("Total indexed content should be non-negative", metrics.totalIndexedContent >= 0)
        assertTrue("Total user profiles should be non-negative", metrics.totalUserProfiles >= 0)
        assertTrue("Total content clusters should be non-negative", metrics.totalContentClusters >= 0)
        assertTrue("Average content quality should be valid", 
                  metrics.averageContentQuality >= 0f && metrics.averageContentQuality <= 1f)
        assertTrue("Indexing rate should be positive", metrics.indexingRate >= 0f)
        assertTrue("Recommendation accuracy should be valid", 
                  metrics.recommendationAccuracy >= 0f && metrics.recommendationAccuracy <= 1f)
        assertTrue("Search latency should be reasonable", metrics.searchLatency >= 0 && metrics.searchLatency <= 10000)
        assertTrue("User engagement should be valid", 
                  metrics.userEngagement >= 0f && metrics.userEngagement <= 1f)
        assertTrue("Content diversity should be valid", 
                  metrics.contentDiversity >= 0f && metrics.contentDiversity <= 1f)
        assertTrue("System load should be valid", 
                  metrics.systemLoad >= 0f && metrics.systemLoad <= 1f)
        assertTrue("Last update time should be recent", 
                  System.currentTimeMillis() - metrics.lastUpdateTime < 10000)
    }

    @Test
    fun testContentDiscoveryEventEmission() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        val events = mutableListOf<ContentDiscoveryEvent>()
        val job = launch {
            contentDiscovery.discoveryEvents.collect { event ->
                events.add(event)
            }
        }
        
        // When - Perform various discovery operations
        val sources = listOf(
            ContentSource("events_test", "Events Test Source", ContentSourceType.LOCAL_STORAGE, "/test")
        )
        contentDiscovery.scanAndIndexContent(sources)
        
        val userId = "event_user"
        contentDiscovery.getPersonalizedRecommendations(userId)
        contentDiscovery.searchContent("test search", SearchType.SEMANTIC)
        
        val interaction = UserInteraction(
            userId = userId,
            contentId = "test_content",
            type = InteractionType.VIEW,
            timestamp = System.currentTimeMillis()
        )
        contentDiscovery.trackUserInteraction(userId, interaction)
        
        advanceUntilIdle()
        job.cancel()
        
        // Then
        assertTrue("Should have emitted events", events.isNotEmpty())
        
        val hasSystemInitialized = events.any { it is ContentDiscoveryEvent.SystemInitialized }
        val hasScanCompleted = events.any { it is ContentDiscoveryEvent.ScanCompleted }
        val hasRecommendationsGenerated = events.any { it is ContentDiscoveryEvent.RecommendationsGenerated }
        val hasSearchPerformed = events.any { it is ContentDiscoveryEvent.SearchPerformed }
        val hasUserInteractionTracked = events.any { it is ContentDiscoveryEvent.UserInteractionTracked }
        
        assertTrue("Should have system initialized event", hasSystemInitialized)
        assertTrue("Should have scan completed event", hasScanCompleted)
        assertTrue("Should have recommendations generated event", hasRecommendationsGenerated)
        assertTrue("Should have search performed event", hasSearchPerformed)
        assertTrue("Should have user interaction tracked event", hasUserInteractionTracked)
    }

    @Test
    fun testSearchFiltersApplication() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        // Index content first
        val sources = listOf(
            ContentSource("filter_test", "Filter Test Source", ContentSourceType.LOCAL_STORAGE, "/test")
        )
        contentDiscovery.scanAndIndexContent(sources)
        advanceUntilIdle()
        
        val query = "test"
        val restrictiveFilters = SearchFilters(
            categories = listOf(ContentCategory.ACTION),
            minDuration = 2 * 60 * 60 * 1000L, // 2 hours
            maxDuration = 3 * 60 * 60 * 1000L, // 3 hours
            minQuality = 0.8f,
            languages = listOf("en"),
            contentRating = ContentRating.PG_13,
            sortBy = SortBy.QUALITY,
            sortOrder = SortOrder.DESCENDING
        )
        
        val permissiveFilters = SearchFilters(
            sortBy = SortBy.RELEVANCE,
            sortOrder = SortOrder.DESCENDING
        )
        
        // When
        val restrictiveResult = contentDiscovery.searchContent(query, SearchType.SEMANTIC, restrictiveFilters, 50)
        val permissiveResult = contentDiscovery.searchContent(query, SearchType.SEMANTIC, permissiveFilters, 50)
        
        // Then
        assertTrue("Restrictive search should succeed", restrictiveResult.success)
        assertTrue("Permissive search should succeed", permissiveResult.success)
        
        // Generally, restrictive filters should return fewer or equal results
        assertTrue("Restrictive filters should not return more results than permissive", 
                  restrictiveResult.totalResults <= permissiveResult.totalResults)
        
        // Verify filter application in results
        restrictiveResult.results.forEach { result ->
            assertTrue("Quality should meet minimum requirement", 
                      result.item.qualityScore >= restrictiveFilters.minQuality!!)
            
            if (result.item.metadata.duration > 0) {
                assertTrue("Duration should be within range", 
                          result.item.metadata.duration >= restrictiveFilters.minDuration!! &&
                          result.item.metadata.duration <= restrictiveFilters.maxDuration!!)
            }
            
            assertTrue("Category should match filter", 
                      restrictiveFilters.categories.contains(result.item.classification.primaryCategory) ||
                      restrictiveFilters.categories.isEmpty())
        }
    }

    @Test
    fun testContentDiscoveryStateTracking() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        // Initial state
        var state = contentDiscovery.discoveryState.value
        assertTrue("Should be initialized", state.isInitialized)
        assertEquals("Initial indexed content should be 0", 0, state.totalIndexedContent)
        assertFalse("Scan should not be in progress initially", state.scanInProgress)
        
        // Start scanning
        val sources = listOf(
            ContentSource("state_test", "State Test Source", ContentSourceType.LOCAL_STORAGE, "/test")
        )
        
        // Note: In a real implementation, we might be able to check intermediate states
        // For now, we'll verify the final state after scanning
        contentDiscovery.scanAndIndexContent(sources)
        advanceUntilIdle()
        
        state = contentDiscovery.discoveryState.value
        assertFalse("Scan should not be in progress after completion", state.scanInProgress)
        assertTrue("Last scan time should be recent", 
                  System.currentTimeMillis() - state.lastScanTime < 10000)
        
        // Generate recommendations to increase user profile count
        contentDiscovery.getPersonalizedRecommendations("state_user")
        advanceUntilIdle()
        
        state = contentDiscovery.discoveryState.value
        assertTrue("Should have at least one user profile", state.totalUserProfiles >= 1)
    }

    @Test
    fun testDataClassStructures() {
        // Test ContentItem construction and properties
        val contentMetadata = ContentMetadata(
            title = "Test Movie",
            description = "A test movie for unit testing",
            duration = 7200000L, // 2 hours
            fileSize = 2L * 1024 * 1024 * 1024, // 2GB
            format = "mp4",
            resolution = Pair(1920, 1080),
            frameRate = 30f,
            bitrate = 8000000L,
            audioChannels = 6,
            language = "en",
            tags = listOf("test", "movie", "action"),
            genres = listOf("Action", "Adventure")
        )
        
        val contentItem = ContentItem(
            id = "test_movie_1",
            uri = Uri.parse("file:///storage/test_movie.mp4"),
            metadata = contentMetadata,
            classification = ContentClassification(
                primaryCategory = ContentCategory.ACTION,
                secondaryCategories = listOf(ContentCategory.ADVENTURE),
                contentRating = ContentRating.PG_13
            ),
            qualityScore = 0.85f,
            viewCount = 150L,
            likeCount = 42L
        )
        
        assertEquals("Title should match", "Test Movie", contentItem.metadata.title)
        assertEquals("Duration should match", 7200000L, contentItem.metadata.duration)
        assertEquals("Quality score should match", 0.85f, contentItem.qualityScore)
        assertEquals("Primary category should match", ContentCategory.ACTION, contentItem.classification.primaryCategory)
        assertTrue("Should have action tag", contentItem.metadata.tags.contains("action"))
        
        // Test UserProfile construction
        val userProfile = UserProfile(
            userId = "test_user",
            preferences = UserPreferences(
                favoriteCategories = listOf(ContentCategory.ACTION, ContentCategory.COMEDY),
                preferredLanguages = listOf("en", "es"),
                contentRatingLimit = ContentRating.R
            ),
            behavior = UserBehavior(
                totalWatchTime = 50 * 60 * 60 * 1000L, // 50 hours
                averageSessionDuration = 45 * 60 * 1000L // 45 minutes
            ),
            demographics = UserDemographics(
                ageGroup = AgeGroup.ADULT,
                timezone = "UTC-5"
            ),
            createdAt = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis()
        )
        
        assertEquals("User ID should match", "test_user", userProfile.userId)
        assertTrue("Should have favorite categories", userProfile.preferences.favoriteCategories.isNotEmpty())
        assertEquals("Content rating limit should match", ContentRating.R, userProfile.preferences.contentRatingLimit)
        assertEquals("Age group should match", AgeGroup.ADULT, userProfile.demographics.ageGroup)
        
        // Test SearchFilters
        val searchFilters = SearchFilters(
            categories = listOf(ContentCategory.DOCUMENTARY, ContentCategory.EDUCATIONAL),
            minDuration = 30 * 60 * 1000L, // 30 minutes
            maxDuration = 2 * 60 * 60 * 1000L, // 2 hours
            minQuality = 0.6f,
            languages = listOf("en"),
            sortBy = SortBy.QUALITY,
            sortOrder = SortOrder.DESCENDING
        )
        
        assertTrue("Should have categories", searchFilters.categories.isNotEmpty())
        assertEquals("Min duration should match", 30 * 60 * 1000L, searchFilters.minDuration)
        assertEquals("Sort by should match", SortBy.QUALITY, searchFilters.sortBy)
        assertTrue("Should include English", searchFilters.languages.contains("en"))
    }

    @Test
    fun testErrorHandling() = runTest {
        // Test operations without initialization
        val uninitializedDiscovery = IntelligentContentDiscovery(context)
        
        val searchResult = uninitializedDiscovery.searchContent("test")
        assertFalse("Search should fail without initialization", searchResult.success)
        assertNotNull("Should have error message", searchResult.error)
        
        val recommendationResult = uninitializedDiscovery.getPersonalizedRecommendations("user")
        assertFalse("Recommendations should fail without initialization", recommendationResult.success)
        assertNotNull("Should have error message", recommendationResult.error)
        
        // Initialize for other tests
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        // Test invalid similarity discovery
        val similarityResult = contentDiscovery.discoverSimilarContent("nonexistent_item")
        assertFalse("Similarity discovery should fail for nonexistent item", similarityResult.success)
        assertNotNull("Should have error message", similarityResult.error)
        
        // Test metrics - should always work
        val metrics = contentDiscovery.getDiscoveryMetrics()
        assertNotNull("Metrics should always be available", metrics)
        
        // Test insights generation - should work even without much data
        val insights = contentDiscovery.generateContentInsights()
        assertTrue("Insights generation should succeed", insights.success)
        
        uninitializedDiscovery.cleanup()
    }

    @Test
    fun testConcurrentDiscoveryOperations() = runTest {
        contentDiscovery.initialize()
        advanceUntilIdle()
        
        // When - Perform multiple operations concurrently
        val operations = listOf(
            async { 
                contentDiscovery.scanAndIndexContent(listOf(
                    ContentSource("concurrent1", "Concurrent 1", ContentSourceType.LOCAL_STORAGE, "/test1")
                ))
            },
            async { contentDiscovery.getPersonalizedRecommendations("concurrent_user_1") },
            async { contentDiscovery.searchContent("concurrent search 1", SearchType.SEMANTIC) },
            async { contentDiscovery.searchContent("concurrent search 2", SearchType.KEYWORD) },
            async { contentDiscovery.generateContentInsights(AnalysisType.POPULARITY) }
        )
        
        val results = operations.awaitAll()
        
        // Then
        assertEquals("All operations should complete", 5, results.size)
        results.forEach { result ->
            assertNotNull("Each result should not be null", result)
            // Individual results may succeed or fail based on implementation and data availability
        }
    }
}