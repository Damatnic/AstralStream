package com.astralplayer.nextplayer.ml

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
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
 * Comprehensive tests for ML-powered content recommendation engine
 * Tests recommendation algorithms, user profiling, content analysis, and model training
 */
@RunWith(AndroidJUnit4::class)
class MLRecommendationEngineTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var recommendationEngine: MLContentRecommendationEngine
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        recommendationEngine = MLContentRecommendationEngine(context)
    }

    @After
    fun tearDown() {
        runTest {
            recommendationEngine.cleanup()
        }
    }

    @Test
    fun testRecommendationEngineInitialization() = runTest {
        // When
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        // Then
        val state = recommendationEngine.engineState.value
        assertTrue("Engine should be initialized", state.isInitialized)
        assertTrue("Initialization time should be set", state.initializationTime > 0)
        assertEquals("Initial model version should be 1", 1, state.modelVersion)
    }

    @Test
    fun testContentAnalysis() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val testUri = Uri.parse("https://example.com/test-video.mp4")
        val metadata = ContentMetadata(
            title = "Test Movie",
            description = "A test movie for analysis",
            genres = listOf("Action", "Adventure"),
            contentType = ContentType.MOVIE,
            duration = 7200000L, // 2 hours
            releaseYear = 2023,
            language = "en",
            director = "Test Director",
            cast = listOf("Actor 1", "Actor 2"),
            rating = 4.5f
        )
        
        // When
        val analysisResult = recommendationEngine.analyzeContent(testUri, metadata)
        
        // Then
        assertNotNull("Analysis result should not be null", analysisResult)
        assertEquals("Content ID should match", testUri.toString(), analysisResult.contentId)
        assertTrue("Should have extracted features", analysisResult.features.isNotEmpty())
        assertEquals("Genres should match", metadata.genres, analysisResult.genres)
        assertTrue("Analysis confidence should be reasonable", 
                  analysisResult.analysisConfidence >= 0.0f && analysisResult.analysisConfidence <= 1.0f)
    }

    @Test
    fun testUserInteractionRecording() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "test_user_123"
        val contentUri = Uri.parse("https://example.com/interaction-test.mp4")
        val interactionData = InteractionData(
            duration = 3600000L, // 1 hour
            completionRate = 0.85f,
            rating = 4.0f,
            context = mapOf("device" to "tablet", "time_of_day" to "evening")
        )
        
        // When
        recommendationEngine.recordInteraction(
            userId = userId,
            contentUri = contentUri,
            interactionType = InteractionType.VIEW,
            interactionData = interactionData
        )
        
        advanceUntilIdle()
        
        // Then - Verify interaction was recorded (through engine state or events)
        val engineState = recommendationEngine.engineState.value
        assertTrue("Engine should still be initialized after interaction", engineState.isInitialized)
    }

    @Test
    fun testPersonalizedRecommendations() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "personalized_user"
        
        // Setup user history with multiple interactions
        setupUserInteractionHistory(userId)
        
        // When
        val recommendationResult = recommendationEngine.getRecommendations(
            userId = userId,
            recommendationType = RecommendationType.PERSONALIZED,
            maxResults = 10
        )
        
        // Then
        assertNotNull("Recommendation result should not be null", recommendationResult)
        assertEquals("User ID should match", userId, recommendationResult.userId)
        assertEquals("Recommendation type should be personalized", 
                    RecommendationType.PERSONALIZED, recommendationResult.recommendationType)
        assertTrue("Should return recommendations within limit", 
                  recommendationResult.recommendations.size <= 10)
        assertTrue("Confidence should be reasonable", 
                  recommendationResult.confidence >= 0.0f && recommendationResult.confidence <= 1.0f)
        assertTrue("Should have a valid timestamp", recommendationResult.generatedAt > 0)
    }

    @Test
    fun testTrendingRecommendations() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "trending_user"
        
        // When
        val trendingRecommendations = recommendationEngine.getRecommendations(
            userId = userId,
            recommendationType = RecommendationType.TRENDING,
            maxResults = 15
        )
        
        // Then
        assertNotNull("Trending recommendations should not be null", trendingRecommendations)
        assertEquals("Should be trending type", 
                    RecommendationType.TRENDING, trendingRecommendations.recommendationType)
        assertTrue("Should return recommendations within limit", 
                  trendingRecommendations.recommendations.size <= 15)
        
        // Verify recommendation reasons
        val trendingReasons = trendingRecommendations.recommendations.map { it.reason }
        assertTrue("All recommendations should be trending-based", 
                  trendingReasons.all { it == RecommendationReason.TRENDING })
    }

    @Test
    fun testSimilarContentRecommendations() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "similarity_user"
        
        // Setup content for similarity testing
        setupSimilarContentScenario(userId)
        
        // When
        val similarRecommendations = recommendationEngine.getRecommendations(
            userId = userId,
            recommendationType = RecommendationType.SIMILAR_CONTENT,
            maxResults = 8
        )
        
        // Then
        assertNotNull("Similar content recommendations should not be null", similarRecommendations)
        assertEquals("Should be similar content type", 
                    RecommendationType.SIMILAR_CONTENT, similarRecommendations.recommendationType)
        assertTrue("Should return recommendations within limit", 
                  similarRecommendations.recommendations.size <= 8)
    }

    @Test
    fun testDiscoveryRecommendations() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "discovery_user"
        
        // Setup user with specific genre preferences
        setupUserWithGenrePreferences(userId, listOf("Action", "Comedy"))
        
        // When
        val discoveryRecommendations = recommendationEngine.getRecommendations(
            userId = userId,
            recommendationType = RecommendationType.DISCOVERY,
            maxResults = 12
        )
        
        // Then
        assertNotNull("Discovery recommendations should not be null", discoveryRecommendations)
        assertEquals("Should be discovery type", 
                    RecommendationType.DISCOVERY, discoveryRecommendations.recommendationType)
        assertTrue("Should return recommendations within limit", 
                  discoveryRecommendations.recommendations.size <= 12)
        
        // Verify discovery recommendations suggest different genres
        val recommendedGenres = discoveryRecommendations.recommendations
            .flatMap { it.metadata.genres }
            .toSet()
        
        assertTrue("Discovery should suggest unexplored genres", 
                  recommendedGenres.any { it !in listOf("Action", "Comedy") })
    }

    @Test
    fun testContinueWatchingRecommendations() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "continue_watching_user"
        
        // Setup incomplete viewing sessions
        setupIncompleteViewingSessions(userId)
        
        // When
        val continueWatchingRecs = recommendationEngine.getRecommendations(
            userId = userId,
            recommendationType = RecommendationType.CONTINUE_WATCHING,
            maxResults = 5
        )
        
        // Then
        assertNotNull("Continue watching recommendations should not be null", continueWatchingRecs)
        assertEquals("Should be continue watching type", 
                    RecommendationType.CONTINUE_WATCHING, continueWatchingRecs.recommendationType)
        
        // Verify all recommendations are continue watching type
        val continueReasons = continueWatchingRecs.recommendations.map { it.reason }
        assertTrue("All recommendations should be continue watching", 
                  continueReasons.all { it == RecommendationReason.CONTINUE_WATCHING })
    }

    @Test
    fun testContextualRecommendations() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "contextual_user"
        val currentContent = Uri.parse("https://example.com/current-movie.mp4")
        
        // Setup current content
        setupCurrentContentScenario(currentContent)
        
        // When
        val contextualRecs = recommendationEngine.getContextualRecommendations(
            userId = userId,
            currentContent = currentContent,
            playbackPosition = 1800000L, // 30 minutes in
            contextData = mapOf(
                "time_of_day" to "evening",
                "device" to "tv",
                "mood" to "relaxed"
            )
        )
        
        // Then
        assertNotNull("Contextual recommendations should not be null", contextualRecs)
        assertTrue("Should return contextual recommendations", contextualRecs.isNotEmpty())
        assertTrue("Should limit contextual recommendations", contextualRecs.size <= 10)
        
        // Verify recommendations are contextually relevant
        val hasContextualReasons = contextualRecs.any { 
            it.reason in listOf(
                RecommendationReason.SIMILAR_CONTENT,
                RecommendationReason.GENRE_MATCH
            )
        }
        assertTrue("Should have contextually relevant recommendations", hasContextualReasons)
    }

    @Test
    fun testContentSimilarityCalculation() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        // Setup two pieces of similar content
        val content1Uri = Uri.parse("https://example.com/action-movie1.mp4")
        val content2Uri = Uri.parse("https://example.com/action-movie2.mp4")
        
        val actionMetadata1 = ContentMetadata(
            title = "Action Movie 1",
            description = "High-octane action film",
            genres = listOf("Action", "Thriller"),
            contentType = ContentType.MOVIE,
            duration = 6600000L
        )
        
        val actionMetadata2 = ContentMetadata(
            title = "Action Movie 2",
            description = "Another thrilling action film",
            genres = listOf("Action", "Adventure"),
            contentType = ContentType.MOVIE,
            duration = 7200000L
        )
        
        // Analyze both pieces of content
        recommendationEngine.analyzeContent(content1Uri, actionMetadata1)
        recommendationEngine.analyzeContent(content2Uri, actionMetadata2)
        
        // When
        val similarity = recommendationEngine.getContentSimilarity(content1Uri, content2Uri)
        
        // Then
        assertTrue("Similarity should be a valid float", 
                  similarity >= 0.0f && similarity <= 1.0f)
        // Note: Without actual embeddings, we can't test exact similarity values
    }

    @Test
    fun testRecommendationExplanation() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "explanation_user"
        val contentId = "https://example.com/explainable-content.mp4"
        
        // Setup user profile and content
        setupUserProfileForExplanation(userId)
        setupContentForExplanation(contentId)
        
        // When
        val explanation = recommendationEngine.explainRecommendation(userId, contentId)
        
        // Then
        assertNotNull("Explanation should not be null", explanation)
        assertEquals("Content ID should match", contentId, explanation.contentId)
        assertNotNull("Should have explanation text", explanation.explanation)
        assertTrue("Should have explanation factors", explanation.factors.isNotEmpty())
        assertTrue("Confidence should be reasonable", 
                  explanation.confidence >= 0.0f && explanation.confidence <= 1.0f)
        
        // Verify explanation factors are valid
        explanation.factors.forEach { factor ->
            assertTrue("Factor weight should be valid", 
                      factor.weight >= 0.0f && factor.weight <= 1.0f)
            assertNotNull("Factor should have description", factor.description)
            assertNotNull("Factor should have type", factor.type)
        }
    }

    @Test
    fun testModelUpdateTrigger() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "model_update_user"
        
        // Record multiple interactions to trigger model update
        repeat(10) { index ->
            val contentUri = Uri.parse("https://example.com/content$index.mp4")
            val interactionData = InteractionData(
                duration = 1800000L + index * 100000L,
                completionRate = 0.7f + index * 0.02f,
                rating = 3.5f + index * 0.1f
            )
            
            recommendationEngine.recordInteraction(
                userId = userId,
                contentUri = contentUri,
                interactionType = InteractionType.VIEW,
                interactionData = interactionData
            )
        }
        
        // When - Force model update
        recommendationEngine.updateModel(userId, forceUpdate = true)
        advanceUntilIdle()
        
        // Then
        val state = recommendationEngine.engineState.value
        assertTrue("Model should have been updated", state.lastModelUpdate > 0)
    }

    @Test
    fun testRecommendationCaching() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "caching_user"
        
        // When - Get recommendations twice
        val firstRequest = recommendationEngine.getRecommendations(
            userId = userId,
            recommendationType = RecommendationType.PERSONALIZED,
            maxResults = 10
        )
        
        val secondRequest = recommendationEngine.getRecommendations(
            userId = userId,
            recommendationType = RecommendationType.PERSONALIZED,
            maxResults = 10
        )
        
        // Then - Second request should be faster (from cache)
        assertNotNull("First request should succeed", firstRequest)
        assertNotNull("Second request should succeed", secondRequest)
        assertEquals("Results should be identical from cache", 
                    firstRequest.recommendations.size, secondRequest.recommendations.size)
    }

    @Test
    fun testRecommendationMetrics() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        // Setup some test data
        setupTestDataForMetrics()
        
        // When
        val metrics = recommendationEngine.getRecommendationMetrics()
        
        // Then
        assertNotNull("Metrics should not be null", metrics)
        assertTrue("Total users should be non-negative", metrics.totalUsers >= 0)
        assertTrue("Total content should be non-negative", metrics.totalContent >= 0)
        assertTrue("Total interactions should be non-negative", metrics.totalInteractions >= 0)
        assertTrue("Average confidence should be valid", 
                  metrics.averageRecommendationConfidence >= 0.0f && 
                  metrics.averageRecommendationConfidence <= 1.0f)
        assertTrue("Model accuracy should be valid", 
                  metrics.modelAccuracy >= 0.0f && metrics.modelAccuracy <= 1.0f)
        assertTrue("Recommendation coverage should be valid", 
                  metrics.recommendationCoverage >= 0.0f && metrics.recommendationCoverage <= 1.0f)
        assertTrue("User engagement should be valid", 
                  metrics.userEngagement >= 0.0f && metrics.userEngagement <= 1.0f)
        assertTrue("Diversity score should be valid", 
                  metrics.diversityScore >= 0.0f && metrics.diversityScore <= 1.0f)
        assertTrue("Novelty score should be valid", 
                  metrics.noveltyScore >= 0.0f && metrics.noveltyScore <= 1.0f)
    }

    @Test
    fun testRecommendationDiversity() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "diversity_user"
        
        // Setup diverse content preferences
        setupDiverseUserProfile(userId)
        
        // When
        val recommendations = recommendationEngine.getRecommendations(
            userId = userId,
            recommendationType = RecommendationType.PERSONALIZED,
            maxResults = 20
        )
        
        // Then
        val recommendedGenres = recommendations.recommendations
            .flatMap { it.metadata.genres }
            .toSet()
        
        assertTrue("Should recommend diverse genres", recommendedGenres.size >= 3)
        
        // Verify content types are diverse
        val contentTypes = recommendations.recommendations
            .map { it.metadata.contentType }
            .toSet()
        
        assertTrue("Should recommend diverse content types", contentTypes.isNotEmpty())
    }

    @Test
    fun testConcurrentRecommendationRequests() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userIds = (1..5).map { "concurrent_user_$it" }
        
        // When - Make concurrent recommendation requests
        val jobs = userIds.map { userId ->
            async {
                recommendationEngine.getRecommendations(
                    userId = userId,
                    recommendationType = RecommendationType.PERSONALIZED,
                    maxResults = 10
                )
            }
        }
        
        val results = jobs.awaitAll()
        
        // Then
        assertEquals("All requests should complete", userIds.size, results.size)
        
        results.forEachIndexed { index, result ->
            assertNotNull("Result $index should not be null", result)
            assertEquals("User ID should match", userIds[index], result.userId)
            assertTrue("Should have recommendations", result.recommendations.isNotEmpty())
        }
    }

    @Test
    fun testRecommendationEventEmission() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val events = mutableListOf<RecommendationEvent>()
        val job = launch {
            recommendationEngine.recommendationEvents.collect { event ->
                events.add(event)
            }
        }
        
        val userId = "event_test_user"
        val contentUri = Uri.parse("https://example.com/event-test.mp4")
        
        // When - Perform various operations that should emit events
        recommendationEngine.recordInteraction(
            userId = userId,
            contentUri = contentUri,
            interactionType = InteractionType.LIKE,
            interactionData = InteractionData(1800000L, 0.8f, 4.5f)
        )
        
        recommendationEngine.getRecommendations(
            userId = userId,
            recommendationType = RecommendationType.PERSONALIZED
        )
        
        advanceUntilIdle()
        job.cancel()
        
        // Then
        assertTrue("Should have emitted events", events.isNotEmpty())
        
        val hasEngineInitialized = events.any { it is RecommendationEvent.EngineInitialized }
        assertTrue("Should have engine initialized event", hasEngineInitialized)
    }

    @Test
    fun testInvalidUserHandling() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val invalidUserId = ""
        
        // When
        val recommendations = recommendationEngine.getRecommendations(
            userId = invalidUserId,
            recommendationType = RecommendationType.PERSONALIZED
        )
        
        // Then - Should handle gracefully
        assertNotNull("Should handle invalid user gracefully", recommendations)
        assertEquals("User ID should be preserved", invalidUserId, recommendations.userId)
    }

    @Test
    fun testRecommendationWithContextFilters() = runTest {
        recommendationEngine.initialize()
        advanceUntilIdle()
        
        val userId = "context_filter_user"
        val contextFilters = RecommendationContext(
            timeOfDay = 20, // 8 PM
            deviceType = DeviceType.TV,
            location = "living_room",
            mood = "relaxed",
            socialContext = SocialContext.WITH_FAMILY
        )
        
        // When
        val recommendations = recommendationEngine.getRecommendations(
            userId = userId,
            recommendationType = RecommendationType.PERSONALIZED,
            maxResults = 15,
            contextFilters = contextFilters
        )
        
        // Then
        assertNotNull("Context-filtered recommendations should not be null", recommendations)
        assertEquals("Context should be preserved", contextFilters, recommendations.context)
        assertTrue("Should return recommendations", recommendations.recommendations.isNotEmpty())
    }

    // Helper methods for setting up test scenarios
    
    private suspend fun setupUserInteractionHistory(userId: String) {
        val contentUris = listOf(
            "https://example.com/action-movie.mp4",
            "https://example.com/comedy-show.mp4",
            "https://example.com/drama-series.mp4",
            "https://example.com/documentary.mp4",
            "https://example.com/thriller-movie.mp4"
        )
        
        contentUris.forEach { contentUri ->
            recommendationEngine.recordInteraction(
                userId = userId,
                contentUri = Uri.parse(contentUri),
                interactionType = InteractionType.VIEW,
                interactionData = InteractionData(
                    duration = 3600000L,
                    completionRate = 0.8f,
                    rating = 4.0f
                )
            )
        }
    }
    
    private suspend fun setupSimilarContentScenario(userId: String) {
        val similarMovies = listOf(
            ContentMetadata("Action Movie 1", "Action film", listOf("Action"), ContentType.MOVIE, 7200000L),
            ContentMetadata("Action Movie 2", "Another action film", listOf("Action"), ContentType.MOVIE, 6900000L),
            ContentMetadata("Comedy Show 1", "Funny show", listOf("Comedy"), ContentType.EPISODE, 1800000L)
        )
        
        similarMovies.forEachIndexed { index, metadata ->
            val uri = Uri.parse("https://example.com/similar-content-$index.mp4")
            recommendationEngine.analyzeContent(uri, metadata)
            
            recommendationEngine.recordInteraction(
                userId = userId,
                contentUri = uri,
                interactionType = InteractionType.VIEW,
                interactionData = InteractionData(metadata.duration / 2, 0.7f, 4.2f)
            )
        }
    }
    
    private suspend fun setupUserWithGenrePreferences(userId: String, preferredGenres: List<String>) {
        preferredGenres.forEach { genre ->
            val contentUri = Uri.parse("https://example.com/$genre-content.mp4")
            val metadata = ContentMetadata(
                title = "$genre Content",
                description = "Content in $genre genre",
                genres = listOf(genre),
                contentType = ContentType.MOVIE,
                duration = 7200000L
            )
            
            recommendationEngine.analyzeContent(contentUri, metadata)
            recommendationEngine.recordInteraction(
                userId = userId,
                contentUri = contentUri,
                interactionType = InteractionType.VIEW,
                interactionData = InteractionData(7200000L, 0.95f, 4.8f)
            )
        }
    }
    
    private suspend fun setupIncompleteViewingSessions(userId: String) {
        val incompleteContent = listOf(
            Triple("https://example.com/incomplete1.mp4", 0.3f, "Series Episode 1"),
            Triple("https://example.com/incomplete2.mp4", 0.6f, "Long Movie"),
            Triple("https://example.com/incomplete3.mp4", 0.4f, "Documentary")
        )
        
        incompleteContent.forEach { (uri, completionRate, title) ->
            val metadata = ContentMetadata(
                title = title,
                description = "Incomplete content",
                genres = listOf("Drama"),
                contentType = ContentType.MOVIE,
                duration = 7200000L
            )
            
            recommendationEngine.analyzeContent(Uri.parse(uri), metadata)
            recommendationEngine.recordInteraction(
                userId = userId,
                contentUri = Uri.parse(uri),
                interactionType = InteractionType.VIEW,
                interactionData = InteractionData(
                    duration = (7200000L * completionRate).toLong(),
                    completionRate = completionRate,
                    rating = 3.5f
                )
            )
        }
    }
    
    private suspend fun setupCurrentContentScenario(currentContent: Uri) {
        val metadata = ContentMetadata(
            title = "Current Movie",
            description = "Currently watching movie",
            genres = listOf("Action", "Adventure"),
            contentType = ContentType.MOVIE,
            duration = 8100000L
        )
        
        recommendationEngine.analyzeContent(currentContent, metadata)
    }
    
    private suspend fun setupUserProfileForExplanation(userId: String) {
        // Setup a user with clear preferences for explanation testing
        val actionContent = Uri.parse("https://example.com/user-action-pref.mp4")
        val actionMetadata = ContentMetadata(
            title = "Preferred Action Movie",
            description = "User's preferred content",
            genres = listOf("Action", "Thriller"),
            contentType = ContentType.MOVIE,
            duration = 7200000L
        )
        
        recommendationEngine.analyzeContent(actionContent, actionMetadata)
        recommendationEngine.recordInteraction(
            userId = userId,
            contentUri = actionContent,
            interactionType = InteractionType.VIEW,
            interactionData = InteractionData(7200000L, 1.0f, 5.0f)
        )
    }
    
    private suspend fun setupContentForExplanation(contentId: String) {
        val metadata = ContentMetadata(
            title = "Explainable Content",
            description = "Content for explanation testing",
            genres = listOf("Action", "Sci-Fi"),
            contentType = ContentType.MOVIE,
            duration = 7500000L
        )
        
        recommendationEngine.analyzeContent(Uri.parse(contentId), metadata)
    }
    
    private suspend fun setupTestDataForMetrics() {
        val testUsers = listOf("metrics_user_1", "metrics_user_2", "metrics_user_3")
        
        testUsers.forEach { userId ->
            repeat(3) { index ->
                val contentUri = Uri.parse("https://example.com/metrics-content-$userId-$index.mp4")
                val metadata = ContentMetadata(
                    title = "Metrics Content $index",
                    description = "Content for metrics testing",
                    genres = listOf("Drama", "Comedy").shuffled().take(1),
                    contentType = ContentType.MOVIE,
                    duration = 6600000L
                )
                
                recommendationEngine.analyzeContent(contentUri, metadata)
                recommendationEngine.recordInteraction(
                    userId = userId,
                    contentUri = contentUri,
                    interactionType = InteractionType.VIEW,
                    interactionData = InteractionData(
                        duration = 5000000L,
                        completionRate = 0.75f,
                        rating = 4.0f
                    )
                )
            }
        }
    }
    
    private suspend fun setupDiverseUserProfile(userId: String) {
        val diverseContent = listOf(
            Triple(listOf("Action", "Thriller"), ContentType.MOVIE, "Action Thriller"),
            Triple(listOf("Comedy", "Romance"), ContentType.MOVIE, "Romantic Comedy"),
            Triple(listOf("Documentary"), ContentType.DOCUMENTARY, "Nature Documentary"),
            Triple(listOf("Animation", "Family"), ContentType.MOVIE, "Animated Family Film"),
            Triple(listOf("Horror", "Supernatural"), ContentType.MOVIE, "Horror Movie")
        )
        
        diverseContent.forEachIndexed { index, (genres, type, title) ->
            val contentUri = Uri.parse("https://example.com/diverse-content-$index.mp4")
            val metadata = ContentMetadata(
                title = title,
                description = "Diverse content for testing",
                genres = genres,
                contentType = type,
                duration = 6900000L
            )
            
            recommendationEngine.analyzeContent(contentUri, metadata)
            recommendationEngine.recordInteraction(
                userId = userId,
                contentUri = contentUri,
                interactionType = InteractionType.VIEW,
                interactionData = InteractionData(6900000L, 0.85f, 4.2f)
            )
        }
    }
}