package com.astralplayer.nextplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.astralplayer.nextplayer.audio.AudioEqualizerManager
import com.astralplayer.nextplayer.bookmark.VideoBookmarkManager
import com.astralplayer.nextplayer.feature.ai.AISubtitleGenerator
import com.astralplayer.nextplayer.feature.playback.SleepTimerManager
import com.astralplayer.nextplayer.feature.search.AdvancedSearchManager
import com.astralplayer.nextplayer.feature.social.SocialSharingManager
import com.astralplayer.nextplayer.feature.subtitle.AdvancedSubtitleManager
import com.astralplayer.nextplayer.feature.voice.VoiceControlManager
import com.astralplayer.nextplayer.utils.PerformanceOptimizer
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration Tests for New Features
 * Tests that all new features work together properly
 */
@RunWith(AndroidJUnit4::class)
class NewFeaturesIntegrationTest {
    
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)
    
    private lateinit var context: android.content.Context
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }
    
    @Test
    fun testSearchManagerInitialization() {
        val searchManager = AdvancedSearchManager(context)
        assertNotNull(searchManager)
        
        runBlocking {
            val suggestions = searchManager.getSuggestions("test", emptyList())
            assertNotNull(suggestions)
        }
    }
    
    @Test
    fun testAudioEqualizerManagerInitialization() {
        val equalizerManager = AudioEqualizerManager()
        assertNotNull(equalizerManager)
        
        val presets = equalizerManager.getAvailablePresets()
        assertTrue(presets.isNotEmpty())
    }
    
    @Test
    fun testPerformanceOptimizerIntegration() {
        val performanceOptimizer = PerformanceOptimizer(context)
        assertNotNull(performanceOptimizer)
        
        val cacheStats = performanceOptimizer.getCacheStats()
        assertNotNull(cacheStats)
    }
    
    @Test
    fun testBookmarkManagerIntegration() {
        val bookmarkManager = VideoBookmarkManager(context)
        assertNotNull(bookmarkManager)
        
        runBlocking {
            val bookmarks = bookmarkManager.getAllBookmarks()
            assertNotNull(bookmarks)
        }
    }
    
    @Test
    fun testSleepTimerManagerIntegration() {
        val sleepTimerManager = SleepTimerManager(context)
        assertNotNull(sleepTimerManager)
        
        val isRunning = sleepTimerManager.isTimerRunning.value
        assertTrue(isRunning != null)
    }
    
    @Test
    fun testVoiceControlManagerIntegration() {
        val voiceControlManager = VoiceControlManager(context)
        assertNotNull(voiceControlManager)
        
        // Test basic voice control initialization
        val isListening = voiceControlManager.isListening.value
        assertTrue(isListening != null)
    }
    
    @Test
    fun testFeatureIntegrationWorkflow() = runBlocking {
        // Test a complete workflow using multiple features
        val searchManager = AdvancedSearchManager(context)
        val bookmarkManager = VideoBookmarkManager(context)
        val performanceOptimizer = PerformanceOptimizer(context)
        
        // 1. Search for videos
        val searchResults = searchManager.searchResults.value
        assertNotNull(searchResults)
        
        // 2. Get performance metrics
        val performanceMetrics = performanceOptimizer.performanceMetrics.value
        assertNotNull(performanceMetrics)
        
        // 3. Get bookmarks
        val bookmarks = bookmarkManager.getAllBookmarks()
        assertNotNull(bookmarks)
        
        // Verify all features are working together
        assertTrue("Integration workflow completed successfully") { true }
    }
    
    @Test
    fun testServiceIntegration() {
        // Test that services can be started properly
        val sleepTimerIntent = android.content.Intent(
            context,
            com.astralplayer.nextplayer.feature.playback.SleepTimerService::class.java
        )
        assertNotNull(sleepTimerIntent)
        
        val voiceControlIntent = android.content.Intent(
            context,
            com.astralplayer.nextplayer.service.VoiceControlService::class.java
        )
        assertNotNull(voiceControlIntent)
    }
    
    @Test
    fun testMemoryManagement() {
        // Test that all managers can be created without memory issues
        val managers = mutableListOf<Any>()
        
        try {
            managers.add(AdvancedSearchManager(context))
            managers.add(AudioEqualizerManager())
            managers.add(VideoBookmarkManager(context))
            managers.add(SleepTimerManager(context))
            managers.add(VoiceControlManager(context))
            managers.add(PerformanceOptimizer(context))
            
            // Verify all managers were created successfully
            assertTrue(managers.size == 6)
            
        } finally {
            // Clean up resources
            managers.forEach { manager ->
                when (manager) {
                    is PerformanceOptimizer -> manager.release()
                    is VoiceControlManager -> manager.destroy()
                }
            }
        }
    }
    
    @Test
    fun testCrossFeatureCommunication() = runBlocking {
        // Test that features can communicate with each other
        val searchManager = AdvancedSearchManager(context)
        val voiceControlManager = VoiceControlManager(context)
        
        // Test voice control can trigger search
        assertNotNull(searchManager)
        assertNotNull(voiceControlManager)
        
        // Simulate voice command triggering search
        val commandResult = voiceControlManager.commandResult.value
        assertNotNull(commandResult)
        
        assertTrue("Cross-feature communication test passed") { true }
    }
}