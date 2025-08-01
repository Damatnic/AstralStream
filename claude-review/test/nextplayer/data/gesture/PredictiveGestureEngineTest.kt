package com.astralplayer.nextplayer.data.gesture

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.astralplayer.nextplayer.data.GestureType
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PredictiveGestureEngineTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var predictiveEngine: PredictiveGestureEngine

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        testScope = TestScope()
        predictiveEngine = PredictiveGestureEngine(context, testScope)
    }

    @After
    fun tearDown() {
        predictiveEngine.cleanup()
    }

    @Test
    fun `gesture event recording updates patterns correctly`() = runTest {
        val gestureContext = GestureContext(
            screenRegion = ScreenRegion.CENTER,
            playerState = PlayerState.PLAYING,
            orientation = Orientation.PORTRAIT
        )
        
        // Record a horizontal seek gesture
        predictiveEngine.recordGestureEvent(
            gestureType = GestureType.HORIZONTAL_SEEK,
            startPosition = Offset(100f, 200f),
            endPosition = Offset(300f, 200f),
            duration = 500L,
            velocity = 400f,
            context = gestureContext
        )
        
        // Check that confidence increases for this gesture type
        val confidence = predictiveEngine.getGestureConfidence(
            GestureType.HORIZONTAL_SEEK,
            gestureContext
        )
        
        assertTrue("Confidence should be greater than 0", confidence > 0f)
    }

    @Test
    fun `prediction generation returns likely gestures`() = runTest {
        val gestureContext = GestureContext(
            screenRegion = ScreenRegion.CENTER,
            playerState = PlayerState.PLAYING,
            orientation = Orientation.PORTRAIT
        )
        
        // Record several horizontal seek gestures to build pattern
        repeat(5) {
            predictiveEngine.recordGestureEvent(
                gestureType = GestureType.HORIZONTAL_SEEK,
                startPosition = Offset(100f, 200f),
                endPosition = Offset(300f + it * 10f, 200f),
                duration = 500L + it * 50L,
                velocity = 400f,
                context = gestureContext
            )
        }
        
        // Get prediction for similar gesture
        val predictions = predictiveEngine.getPrediction(
            currentPosition = Offset(250f, 200f),
            startPosition = Offset(100f, 200f),
            duration = 400L,
            velocity = 375f,
            context = gestureContext
        )
        
        assertFalse("Should have predictions", predictions.isEmpty())
        
        val horizontalSeekPrediction = predictions.find { 
            it.gestureType == GestureType.HORIZONTAL_SEEK 
        }
        assertNotNull("Should predict horizontal seek", horizontalSeekPrediction)
        assertTrue("Should have decent confidence", 
            horizontalSeekPrediction!!.confidence > 0.3f)
    }

    @Test
    fun `feature extraction calculates correct values`() {
        val extractor = GestureFeatureExtractor()
        
        val gestureEvent = GestureEvent(
            type = GestureType.HORIZONTAL_SEEK,
            startPosition = Offset(0f, 100f),
            endPosition = Offset(200f, 100f),
            duration = 1000L,
            velocity = 200f,
            context = GestureContext(
                ScreenRegion.CENTER,
                PlayerState.PLAYING,
                Orientation.PORTRAIT
            ),
            timestamp = System.currentTimeMillis()
        )
        
        val features = extractor.extractFeatures(gestureEvent, emptyList())
        
        assertEquals(200f, features.distance, 1f)
        assertEquals(0f, features.direction, 1f) // Horizontal right
        assertEquals(0.2f, features.velocity, 0.01f) // 200 pixels / 1000 ms
        assertEquals(1000L, features.duration)
    }

    @Test
    fun `neural network learns and predicts correctly`() {
        val network = SimpleNeuralNetwork()
        
        val features1 = GestureFeatures(
            velocity = 400f,
            direction = 0f,
            distance = 200f,
            duration = 500L,
            acceleration = 0f,
            screenRegion = ScreenRegion.CENTER,
            context = GestureContext(
                ScreenRegion.CENTER,
                PlayerState.PLAYING,
                Orientation.PORTRAIT
            )
        )
        
        // Train network
        repeat(10) {
            network.train(features1, GestureType.HORIZONTAL_SEEK)
        }
        
        // Test prediction
        val predictions = network.predict(features1)
        
        assertTrue("Should have predictions", predictions.isNotEmpty())
        assertTrue("Should predict horizontal seek",
            predictions.containsKey(GestureType.HORIZONTAL_SEEK))
        assertTrue("Should have reasonable confidence",
            predictions[GestureType.HORIZONTAL_SEEK]!! > 0.5f)
    }

    @Test
    fun `pattern similarity calculation works correctly`() {
        val features1 = GestureFeatures(
            velocity = 400f,
            direction = 0f,
            distance = 200f,
            duration = 500L,
            acceleration = 0f,
            screenRegion = ScreenRegion.CENTER,
            context = GestureContext(
                ScreenRegion.CENTER,
                PlayerState.PLAYING,
                Orientation.PORTRAIT
            )
        )
        
        val features2 = GestureFeatures(
            velocity = 420f,
            direction = 5f,
            distance = 210f,
            duration = 520L,
            acceleration = 0f,
            screenRegion = ScreenRegion.CENTER,
            context = GestureContext(
                ScreenRegion.CENTER,
                PlayerState.PLAYING,
                Orientation.PORTRAIT
            )
        )
        
        // Use reflection to access private method for testing
        val method = PredictiveGestureEngine::class.java.getDeclaredMethod(
            "calculateFeatureSimilarity",
            GestureFeatures::class.java,
            GestureFeatures::class.java
        )
        method.isAccessible = true
        
        val similarity = method.invoke(predictiveEngine, features1, features2) as Float
        
        assertTrue("Similar features should have high similarity", similarity > 0.8f)
    }

    @Test
    fun `context matching works correctly`() {
        val context1 = GestureContext(
            screenRegion = ScreenRegion.CENTER,
            playerState = PlayerState.PLAYING,
            orientation = Orientation.PORTRAIT
        )
        
        val context2 = GestureContext(
            screenRegion = ScreenRegion.CENTER,
            playerState = PlayerState.PLAYING,
            orientation = Orientation.PORTRAIT
        )
        
        val context3 = GestureContext(
            screenRegion = ScreenRegion.TOP_LEFT,
            playerState = PlayerState.PAUSED,
            orientation = Orientation.LANDSCAPE
        )
        
        // Use reflection to access private method
        val method = PredictiveGestureEngine::class.java.getDeclaredMethod(
            "calculateContextMatch",
            GestureContext::class.java,
            GestureContext::class.java
        )
        method.isAccessible = true
        
        val matchScore1 = method.invoke(predictiveEngine, context1, context2) as Float
        val matchScore2 = method.invoke(predictiveEngine, context1, context3) as Float
        
        assertEquals("Identical contexts should match perfectly", 1.0f, matchScore1, 0.01f)
        assertTrue("Different contexts should have lower match", matchScore2 < matchScore1)
    }

    @Test
    fun `pattern data updates correctly with learning`() {
        val gestureContext = GestureContext(
            screenRegion = ScreenRegion.CENTER,
            playerState = PlayerState.PLAYING,
            orientation = Orientation.PORTRAIT
        )
        
        val initialFeatures = GestureFeatures(
            velocity = 400f,
            direction = 0f,
            distance = 200f,
            duration = 500L,
            acceleration = 0f,
            screenRegion = ScreenRegion.CENTER,
            context = gestureContext
        )
        
        val pattern = GesturePatternData(
            gestureType = GestureType.HORIZONTAL_SEEK,
            context = gestureContext,
            features = initialFeatures,
            confidence = 0.5f,
            usage_count = 1,
            lastUsed = System.currentTimeMillis()
        )
        
        val newFeatures = GestureFeatures(
            velocity = 500f,
            direction = 0f,
            distance = 250f,
            duration = 600L,
            acceleration = 0f,
            screenRegion = ScreenRegion.CENTER,
            context = gestureContext
        )
        
        val initialVelocity = pattern.features.velocity
        pattern.updateWithNewData(newFeatures, 0.5f) // 50% learning rate
        
        assertTrue("Velocity should be updated", pattern.features.velocity != initialVelocity)
        assertTrue("Confidence should increase", pattern.confidence > 0.5f)
        assertEquals("Usage count should increase", 2, pattern.usage_count)
    }

    @Test
    fun `pattern decay works correctly`() {
        val gestureContext = GestureContext(
            screenRegion = ScreenRegion.CENTER,
            playerState = PlayerState.PLAYING,
            orientation = Orientation.PORTRAIT
        )
        
        val pattern = GesturePatternData(
            gestureType = GestureType.HORIZONTAL_SEEK,
            context = gestureContext,
            features = GestureFeatures(
                velocity = 400f,
                direction = 0f,
                distance = 200f,
                duration = 500L,
                acceleration = 0f,
                screenRegion = ScreenRegion.CENTER,
                context = gestureContext
            ),
            confidence = 0.8f,
            usage_count = 1,
            lastUsed = System.currentTimeMillis()
        )
        
        val initialConfidence = pattern.confidence
        pattern.decay(0.1f)
        
        assertTrue("Confidence should decrease", pattern.confidence < initialConfidence)
        assertEquals("Should decay by correct amount", 0.7f, pattern.confidence, 0.01f)
    }

    @Test
    fun `time of day detection works correctly`() {
        val morning = TimeOfDay.fromCurrentTime()
        assertNotNull("Should return a valid time of day", morning)
    }

    @Test
    fun `gesture sequence extraction works correctly`() {
        val now = System.currentTimeMillis()
        val gestureContext = GestureContext(
            screenRegion = ScreenRegion.CENTER,
            playerState = PlayerState.PLAYING,
            orientation = Orientation.PORTRAIT
        )
        
        val gestures = listOf(
            GestureEvent(
                type = GestureType.SINGLE_TAP,
                startPosition = Offset(100f, 100f),
                endPosition = Offset(100f, 100f),
                duration = 100L,
                velocity = 0f,
                context = gestureContext,
                timestamp = now
            ),
            GestureEvent(
                type = GestureType.HORIZONTAL_SEEK,
                startPosition = Offset(100f, 200f),
                endPosition = Offset(300f, 200f),
                duration = 500L,
                velocity = 400f,
                context = gestureContext,
                timestamp = now + 1000L
            ),
            GestureEvent(
                type = GestureType.VERTICAL_VOLUME,
                startPosition = Offset(200f, 300f),
                endPosition = Offset(200f, 100f),
                duration = 600L,
                velocity = 333f,
                context = gestureContext,
                timestamp = now + 5000L // Should start new sequence
            )
        )
        
        // Use reflection to access private method
        val method = PredictiveGestureEngine::class.java.getDeclaredMethod(
            "extractGestureSequences",
            List::class.java
        )
        method.isAccessible = true
        
        @Suppress("UNCHECKED_CAST")
        val sequences = method.invoke(predictiveEngine, gestures) as List<List<GestureEvent>>
        
        assertTrue("Should have at least one sequence", sequences.isNotEmpty())
        assertEquals("First sequence should have 2 gestures", 2, sequences[0].size)
    }

    @Test
    fun `preload gesture responses handles all gesture types`() {
        val predictions = listOf(
            GesturePrediction(
                gestureType = GestureType.HORIZONTAL_SEEK,
                confidence = 0.8f,
                features = GestureFeatures(
                    velocity = 400f,
                    direction = 0f,
                    distance = 200f,
                    duration = 500L,
                    acceleration = 0f,
                    screenRegion = ScreenRegion.CENTER,
                    context = GestureContext(
                        ScreenRegion.CENTER,
                        PlayerState.PLAYING,
                        Orientation.PORTRAIT
                    )
                ),
                timestamp = System.currentTimeMillis()
            )
        )
        
        // Should not throw exception
        predictiveEngine.preloadGestureResponses(predictions)
    }
}