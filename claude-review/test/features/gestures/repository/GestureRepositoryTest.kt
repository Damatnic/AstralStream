package com.astralplayer.features.gestures.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astralplayer.astralstream.data.database.AstralStreamDatabase
import com.astralplayer.features.gestures.dao.GestureDao
import com.astralplayer.features.gestures.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class GestureRepositoryTest {

    private lateinit var database: AstralStreamDatabase
    private lateinit var gestureDao: GestureDao
    private lateinit var gestureRepository: GestureRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AstralStreamDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        gestureDao = database.gestureDao()
        gestureRepository = GestureRepository(gestureDao, context)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `create custom gesture saves correctly`() = runBlocking {
        val gesture = CustomGestureEntity(
            id = "test_gesture_1", 
            name = "Circle Swipe",
            type = GestureType.SWIPE_UP,
            action = GestureAction.VOLUME_UP,
            zone = GestureZone.CENTER,
            isEnabled = true,
            profileId = "default",
            description = "Swipe up in center to increase volume"
        )
        
        val result = gestureRepository.saveCustomGesture(gesture)
        
        assertTrue(result.isSuccess)
        
        // Verify saved in database
        val savedGesture = gestureDao.getCustomGesture("test_gesture_1")
        assertNotNull(savedGesture)
        assertEquals("Circle Swipe", savedGesture.name)
        assertEquals(GestureType.SWIPE_UP, savedGesture.type)
        assertEquals(GestureAction.VOLUME_UP, savedGesture.action)
        assertEquals(GestureZone.CENTER, savedGesture.zone)
        assertTrue(savedGesture.isEnabled)
    }

    @Test
    fun `get gestures by profile returns correct gestures`() = runBlocking {
        val profileId = "gaming_profile"
        
        // Create multiple gestures for different profiles
        val gestures = listOf(
            CustomGestureEntity(
                id = "gaming_1",
                name = "Quick Pause",
                type = GestureType.DOUBLE_TAP,
                action = GestureAction.PLAY_PAUSE,
                zone = GestureZone.CENTER,
                profileId = profileId
            ),
            CustomGestureEntity(
                id = "gaming_2", 
                name = "Fast Forward",
                type = GestureType.SWIPE_RIGHT,
                action = GestureAction.SEEK_FORWARD,
                zone = GestureZone.RIGHT,
                profileId = profileId
            ),
            CustomGestureEntity(
                id = "other_profile",
                name = "Other Gesture",
                type = GestureType.TAP,
                action = GestureAction.SHOW_INFO,
                zone = GestureZone.LEFT,
                profileId = "other_profile"
            )
        )
        
        gestures.forEach { gesture ->
            gestureRepository.saveCustomGesture(gesture)
        }
        
        val profileGestures = gestureRepository.getGesturesByProfile(profileId).first()
        
        assertEquals(2, profileGestures.size)
        assertTrue(profileGestures.all { it.profileId == profileId })
        assertTrue(profileGestures.any { it.name == "Quick Pause" })
        assertTrue(profileGestures.any { it.name == "Fast Forward" })
    }

    @Test
    fun `create gesture profile with default gestures`() = runBlocking {
        val profile = GestureProfileEntity(
            id = "movie_profile",
            name = "Movie Watching",
            description = "Optimized for movie watching",
            isDefault = false,
            isEnabled = true,
            createdAt = System.currentTimeMillis()
        )
        
        val result = gestureRepository.createProfile(profile)
        
        assertTrue(result.isSuccess)
        
        // Verify profile saved
        val savedProfile = gestureDao.getGestureProfile("movie_profile")
        assertNotNull(savedProfile)
        assertEquals("Movie Watching", savedProfile.name)
        assertFalse(savedProfile.isDefault)
        
        // Verify default gestures were created for profile
        val profileGestures = gestureDao.getGesturesByProfile("movie_profile")
        assertTrue(profileGestures.isNotEmpty())
        
        // Should have basic gestures like play/pause, volume, seeking
        val gestureActions = profileGestures.map { it.action }
        assertTrue(gestureActions.contains(GestureAction.PLAY_PAUSE))
        assertTrue(gestureActions.contains(GestureAction.VOLUME_UP))
        assertTrue(gestureActions.contains(GestureAction.VOLUME_DOWN))
    }

    @Test
    fun `update gesture settings modifies existing gesture`() = runBlocking {
        // First create a gesture
        val originalGesture = CustomGestureEntity(
            id = "update_test",
            name = "Original Name",
            type = GestureType.TAP,
            action = GestureAction.PLAY_PAUSE,
            zone = GestureZone.CENTER,
            sensitivity = 0.5f,
            isEnabled = true
        )
        
        gestureRepository.saveCustomGesture(originalGesture)
        
        // Update the gesture
        val updatedGesture = originalGesture.copy(
            name = "Updated Name",
            type = GestureType.DOUBLE_TAP,
            sensitivity = 0.8f,
            isEnabled = false
        )
        
        val result = gestureRepository.updateGesture(updatedGesture)
        
        assertTrue(result.isSuccess)
        
        // Verify changes
        val savedGesture = gestureDao.getCustomGesture("update_test")
        assertNotNull(savedGesture)
        assertEquals("Updated Name", savedGesture.name)
        assertEquals(GestureType.DOUBLE_TAP, savedGesture.type)
        assertEquals(0.8f, savedGesture.sensitivity)
        assertFalse(savedGesture.isEnabled)
        
        // Action and zone should remain the same
        assertEquals(GestureAction.PLAY_PAUSE, savedGesture.action)
        assertEquals(GestureZone.CENTER, savedGesture.zone)
    }

    @Test
    fun `delete gesture removes from database`() = runBlocking {
        val gesture = CustomGestureEntity(
            id = "delete_test",
            name = "To Delete",
            type = GestureType.SWIPE_LEFT,
            action = GestureAction.SEEK_BACKWARD,
            zone = GestureZone.LEFT
        )
        
        // Save gesture
        gestureRepository.saveCustomGesture(gesture)
        
        // Verify it exists
        val savedGesture = gestureDao.getCustomGesture("delete_test")
        assertNotNull(savedGesture)
        
        // Delete gesture
        val result = gestureRepository.deleteGesture("delete_test")
        
        assertTrue(result.isSuccess)
        
        // Verify it's gone
        val deletedGesture = gestureDao.getCustomGesture("delete_test")
        assertNull(deletedGesture)
    }

    @Test
    fun `record gesture history tracks usage`() = runBlocking {
        val gestureId = "history_test_gesture"
        val gesture = CustomGestureEntity(
            id = gestureId,
            name = "History Test",
            type = GestureType.PINCH_IN,
            action = GestureAction.ZOOM_OUT,
            zone = GestureZone.CENTER
        )
        
        gestureRepository.saveCustomGesture(gesture)
        
        // Record multiple uses
        repeat(5) { index ->
            gestureRepository.recordGestureUsage(
                gestureId = gestureId,
                success = index < 4, // 4 successful, 1 failed
                responseTime = 100L + index * 10,
                context = "video_playback"
            )
            Thread.sleep(10) // Ensure different timestamps
        }
        
        // Check usage statistics
        val history = gestureRepository.getGestureHistory(gestureId).first()
        assertEquals(5, history.size)
        
        val stats = gestureRepository.getGestureStatistics(gestureId)
        assertNotNull(stats)
        assertEquals(5, stats.totalUses)
        assertEquals(4, stats.successfulUses)
        assertEquals(0.8f, stats.successRate)
        assertTrue(stats.averageResponseTime > 100L)
        assertTrue(stats.averageResponseTime < 150L)
    }

    @Test
    fun `gesture conflict detection identifies overlapping gestures`() = runBlocking {
        val profileId = "conflict_test_profile"
        
        // Create conflicting gestures (same type and zone)
        val gesture1 = CustomGestureEntity(
            id = "conflict_1",
            name = "First Gesture",
            type = GestureType.SWIPE_UP,
            action = GestureAction.VOLUME_UP,
            zone = GestureZone.RIGHT,
            profileId = profileId
        )
        
        val gesture2 = CustomGestureEntity(
            id = "conflict_2",
            name = "Second Gesture", 
            type = GestureType.SWIPE_UP,
            action = GestureAction.BRIGHTNESS_UP,
            zone = GestureZone.RIGHT,
            profileId = profileId
        )
        
        gestureRepository.saveCustomGesture(gesture1)
        
        // Try to save conflicting gesture
        val result = gestureRepository.saveCustomGesture(gesture2)
        
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("conflict") == true)
        
        // Verify only first gesture was saved
        val savedGestures = gestureDao.getGesturesByProfile(profileId)
        assertEquals(1, savedGestures.size)
        assertEquals("conflict_1", savedGestures[0].id)
    }

    @Test
    fun `gesture shortcuts work with complex patterns`() = runBlocking {
        val shortcut = GestureShortcutEntity(
            id = "complex_shortcut",
            name = "Complex Pattern",
            description = "Multi-step gesture sequence",
            gesturePattern = listOf(
                GestureStep(GestureType.TAP, GestureZone.CENTER, 0),
                GestureStep(GestureType.SWIPE_UP, GestureZone.CENTER, 500),
                GestureStep(GestureType.DOUBLE_TAP, GestureZone.RIGHT, 1000)
            ),
            actions = listOf(
                GestureAction.PAUSE,
                GestureAction.VOLUME_UP,
                GestureAction.SHOW_INFO
            ),
            isEnabled = true,
            profileId = "advanced_profile"
        )
        
        val result = gestureRepository.saveGestureShortcut(shortcut)
        
        assertTrue(result.isSuccess)
        
        // Verify shortcut saved correctly
        val savedShortcut = gestureDao.getGestureShortcut("complex_shortcut")
        assertNotNull(savedShortcut)
        assertEquals("Complex Pattern", savedShortcut.name)
        assertEquals(3, savedShortcut.gesturePattern.size)
        assertEquals(3, savedShortcut.actions.size)
        
        // Verify pattern order
        assertEquals(GestureType.TAP, savedShortcut.gesturePattern[0].type)
        assertEquals(GestureType.SWIPE_UP, savedShortcut.gesturePattern[1].type)
        assertEquals(GestureType.DOUBLE_TAP, savedShortcut.gesturePattern[2].type)
        
        // Verify timing
        assertEquals(0L, savedShortcut.gesturePattern[0].delay)
        assertEquals(500L, savedShortcut.gesturePattern[1].delay)
        assertEquals(1000L, savedShortcut.gesturePattern[2].delay)
    }

    @Test
    fun `gesture zone detection works with coordinates`() = runBlocking {
        val detector = gestureRepository.createGestureDetector()
        
        // Test center zone detection
        val centerResult = detector.detectZone(500f, 400f, 1000, 800) // Center of 1000x800 screen
        assertEquals(GestureZone.CENTER, centerResult)
        
        // Test left zone detection  
        val leftResult = detector.detectZone(100f, 400f, 1000, 800)
        assertEquals(GestureZone.LEFT, leftResult)
        
        // Test right zone detection
        val rightResult = detector.detectZone(900f, 400f, 1000, 800)
        assertEquals(GestureZone.RIGHT, rightResult)
        
        // Test top zone detection
        val topResult = detector.detectZone(500f, 100f, 1000, 800)
        assertEquals(GestureZone.TOP, topResult)
        
        // Test bottom zone detection
        val bottomResult = detector.detectZone(500f, 700f, 1000, 800)
        assertEquals(GestureZone.BOTTOM, bottomResult)
    }

    @Test
    fun `backup and restore gestures preserves data`() = runBlocking {
        val profileId = "backup_test_profile"
        
        // Create test profile with gestures
        val profile = GestureProfileEntity(
            id = profileId,
            name = "Backup Test Profile",
            description = "For testing backup/restore",
            isDefault = false
        )
        
        val gesture = CustomGestureEntity(
            id = "backup_gesture",
            name = "Backup Gesture",
            type = GestureType.SWIPE_DOWN,
            action = GestureAction.VOLUME_DOWN,
            zone = GestureZone.LEFT,
            profileId = profileId
        )
        
        gestureRepository.createProfile(profile)
        gestureRepository.saveCustomGesture(gesture)
        
        // Create backup
        val backupResult = gestureRepository.exportGestureProfile(profileId)
        assertTrue(backupResult.isSuccess)
        
        val backupData = backupResult.getOrNull()
        assertNotNull(backupData)
        
        // Delete original data
        gestureRepository.deleteProfile(profileId)
        
        // Verify deleted
        val deletedProfile = gestureDao.getGestureProfile(profileId)
        assertNull(deletedProfile)
        
        // Restore from backup
        val restoreResult = gestureRepository.importGestureProfile(backupData)
        assertTrue(restoreResult.isSuccess)
        
        // Verify restored
        val restoredProfile = gestureDao.getGestureProfile(profileId)
        assertNotNull(restoredProfile)
        assertEquals("Backup Test Profile", restoredProfile.name)
        
        val restoredGesture = gestureDao.getCustomGesture("backup_gesture")
        assertNotNull(restoredGesture)
        assertEquals("Backup Gesture", restoredGesture.name)
        assertEquals(GestureAction.VOLUME_DOWN, restoredGesture.action)
    }

    @Test
    fun `gesture sensitivity affects detection threshold`() = runBlocking {
        val lowSensitivityGesture = CustomGestureEntity(
            id = "low_sensitivity",
            name = "Low Sensitivity Swipe",
            type = GestureType.SWIPE_RIGHT,
            action = GestureAction.SEEK_FORWARD,
            zone = GestureZone.CENTER,
            sensitivity = 0.3f // Low sensitivity
        )
        
        val highSensitivityGesture = CustomGestureEntity(
            id = "high_sensitivity",
            name = "High Sensitivity Swipe",
            type = GestureType.SWIPE_RIGHT,
            action = GestureAction.SEEK_FORWARD,
            zone = GestureZone.RIGHT,
            sensitivity = 0.9f // High sensitivity
        )
        
        gestureRepository.saveCustomGesture(lowSensitivityGesture)
        gestureRepository.saveCustomGesture(highSensitivityGesture)
        
        val detector = gestureRepository.createGestureDetector()
        
        // Test with small movement (should only trigger high sensitivity)
        val smallMovement = GestureInput(
            type = GestureType.SWIPE_RIGHT,
            startX = 500f,
            startY = 400f,
            endX = 520f, // Small movement
            endY = 400f,
            duration = 200L,
            velocity = 100f
        )
        
        val lowSensResult = detector.shouldTriggerGesture(lowSensitivityGesture, smallMovement)
        val highSensResult = detector.shouldTriggerGesture(highSensitivityGesture, smallMovement)
        
        assertFalse(lowSensResult) // Low sensitivity shouldn't trigger
        assertTrue(highSensResult) // High sensitivity should trigger
        
        // Test with large movement (should trigger both)
        val largeMovement = smallMovement.copy(
            endX = 600f, // Large movement
            velocity = 500f
        )
        
        val lowSensLargeResult = detector.shouldTriggerGesture(lowSensitivityGesture, largeMovement)
        val highSensLargeResult = detector.shouldTriggerGesture(highSensitivityGesture, largeMovement)
        
        assertTrue(lowSensLargeResult) // Both should trigger
        assertTrue(highSensLargeResult)
    }
}