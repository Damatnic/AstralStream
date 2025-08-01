package com.astralplayer.nextplayer.collaboration

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
 * Comprehensive tests for real-time collaboration engine
 * Tests session management, playback synchronization, chat, annotations, and permissions
 */
@RunWith(AndroidJUnit4::class)
class RealTimeCollaborationEngineTest {

    private lateinit var context: Context
    private lateinit var testScope: TestScope
    private lateinit var collaborationEngine: RealTimeCollaborationEngine
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        collaborationEngine = RealTimeCollaborationEngine(context)
    }

    @After
    fun tearDown() {
        runTest {
            collaborationEngine.cleanup()
        }
    }

    @Test
    fun testCollaborationEngineInitialization() = runTest {
        // When
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        // Then
        val state = collaborationEngine.collaborationState.value
        assertTrue("Engine should be initialized", state.isInitialized)
        assertTrue("Initialization time should be set", state.initializationTime > 0)
        assertFalse("Should not be connected initially", state.isConnected)
        assertEquals("Initial active sessions should be 0", 0, state.activeSessions)
        assertFalse("Should not be hosting initially", state.isHosting)
    }

    @Test
    fun testSessionCreation() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val sessionConfig = SessionConfig(
            sessionName = "Test Watch Party",
            videoUri = Uri.parse("content://test/video.mp4"),
            maxParticipants = 10,
            isPrivate = false,
            enableChat = true,
            enableAnnotations = true,
            enableVoiceCall = true,
            enableScreenShare = false,
            hostPermissions = setOf(
                Permission.CONTROL_PLAYBACK,
                Permission.MANAGE_PARTICIPANTS,
                Permission.MODERATE_CHAT,
                Permission.CREATE_ANNOTATIONS
            )
        )
        
        // When
        val result = collaborationEngine.createSession(sessionConfig)
        
        // Then
        assertNotNull("Session creation result should not be null", result)
        assertTrue("Session creation should succeed", result.success)
        assertNotNull("Session ID should be generated", result.sessionId)
        assertTrue("Session ID should not be empty", result.sessionId!!.isNotEmpty())
        assertNotNull("Session should be created", result.session)
        assertEquals("Session name should match", sessionConfig.sessionName, result.session!!.sessionName)
        assertEquals("Max participants should match", sessionConfig.maxParticipants, result.session.maxParticipants)
        assertTrue("Session should be active", result.session.isActive)
        assertNotNull("Host user should be set", result.session.hostUserId)
    }

    @Test
    fun testSessionJoining() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        // Create a session first
        val sessionConfig = SessionConfig(
            sessionName = "Test Session",
            videoUri = Uri.parse("content://test/video.mp4"),
            maxParticipants = 5
        )
        val createdSession = collaborationEngine.createSession(sessionConfig)
        val sessionId = createdSession.sessionId!!
        
        val userInfo = UserInfo(
            userId = "test_user_2",
            displayName = "Test User 2",
            avatarUrl = "https://example.com/avatar2.jpg",
            preferences = UserPreferences()
        )
        
        // When
        val joinResult = collaborationEngine.joinSession(sessionId, userInfo)
        
        // Then
        assertNotNull("Join result should not be null", joinResult)
        assertTrue("Join should succeed", joinResult.success)
        assertEquals("Joined session ID should match", sessionId, joinResult.sessionId)
        assertNotNull("Session info should be provided", joinResult.sessionInfo)
        assertTrue("Should have default permissions", joinResult.permissions.isNotEmpty())
        assertTrue("Should have participant permission", 
                  joinResult.permissions.contains(Permission.VIEW_VIDEO))
    }

    @Test
    fun testPlaybackSynchronization() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        // Create session and join
        val sessionConfig = SessionConfig(
            sessionName = "Sync Test",
            videoUri = Uri.parse("content://test/video.mp4")
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        val playbackState = PlaybackState(
            position = 150000L, // 2.5 minutes
            isPlaying = true,
            playbackSpeed = 1.0f,
            timestamp = System.currentTimeMillis(),
            bufferedPosition = 180000L
        )
        
        // When
        collaborationEngine.syncPlayback(sessionId, playbackState)
        advanceUntilIdle()
        
        // Verify sync state is updated
        val sessionInfo = collaborationEngine.getSessionInfo(sessionId)
        assertNotNull("Session info should exist", sessionInfo)
        assertNotNull("Playback state should be synced", sessionInfo!!.currentPlaybackState)
        assertEquals("Position should be synced", playbackState.position, 
                    sessionInfo.currentPlaybackState!!.position)
        assertEquals("Playing state should be synced", playbackState.isPlaying, 
                    sessionInfo.currentPlaybackState.isPlaying)
    }

    @Test
    fun testChatFunctionality() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        // Create session
        val sessionConfig = SessionConfig(
            sessionName = "Chat Test",
            videoUri = Uri.parse("content://test/video.mp4"),
            enableChat = true
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        val chatMessage = ChatMessage(
            messageId = "msg_1",
            sessionId = sessionId,
            senderId = "user_1",
            senderDisplayName = "Test User",
            content = "Hello everyone!",
            timestamp = System.currentTimeMillis(),
            messageType = MessageType.TEXT
        )
        
        // When
        val sendResult = collaborationEngine.sendChatMessage(chatMessage)
        
        // Then
        assertNotNull("Send result should not be null", sendResult)
        assertTrue("Message send should succeed", sendResult.success)
        assertEquals("Message ID should match", chatMessage.messageId, sendResult.messageId)
        assertTrue("Delivery time should be set", sendResult.deliveryTime > 0)
    }

    @Test
    fun testAnnotationCreation() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        // Create session
        val sessionConfig = SessionConfig(
            sessionName = "Annotation Test",
            videoUri = Uri.parse("content://test/video.mp4"),
            enableAnnotations = true
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        val annotation = VideoAnnotation(
            annotationId = "anno_1",
            sessionId = sessionId,
            creatorId = "user_1",
            creatorDisplayName = "Test User",
            timestamp = 120000L, // 2 minutes
            content = "Great scene!",
            annotationType = AnnotationType.TEXT,
            position = AnnotationPosition(x = 0.5f, y = 0.3f),
            duration = 5000L,
            createdAt = System.currentTimeMillis()
        )
        
        // When
        val createResult = collaborationEngine.createAnnotation(annotation)
        
        // Then
        assertNotNull("Create result should not be null", createResult)
        assertTrue("Annotation creation should succeed", createResult.success)
        assertEquals("Annotation ID should match", annotation.annotationId, createResult.annotationId)
        assertTrue("Creation time should be set", createResult.creationTime > 0)
    }

    @Test
    fun testParticipantManagement() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        // Create session
        val sessionConfig = SessionConfig(
            sessionName = "Participant Test",
            videoUri = Uri.parse("content://test/video.mp4"),
            maxParticipants = 3
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        // Add participants
        val participant1 = SessionParticipant(
            userId = "user_1",
            displayName = "User One",
            role = ParticipantRole.PARTICIPANT,
            joinedAt = System.currentTimeMillis(),
            permissions = setOf(Permission.VIEW_VIDEO, Permission.SEND_CHAT),
            isOnline = true
        )
        
        val participant2 = SessionParticipant(
            userId = "user_2",
            displayName = "User Two",
            role = ParticipantRole.MODERATOR,
            joinedAt = System.currentTimeMillis(),
            permissions = setOf(Permission.VIEW_VIDEO, Permission.SEND_CHAT, Permission.MODERATE_CHAT),
            isOnline = true
        )
        
        // When
        collaborationEngine.addParticipant(sessionId, participant1)
        collaborationEngine.addParticipant(sessionId, participant2)
        advanceUntilIdle()
        
        val participants = collaborationEngine.getSessionParticipants(sessionId)
        
        // Then
        assertNotNull("Participants list should not be null", participants)
        assertTrue("Should have participants", participants!!.isNotEmpty())
        assertTrue("Should include added participants", participants.size >= 2)
        
        val foundParticipant1 = participants.find { it.userId == "user_1" }
        val foundParticipant2 = participants.find { it.userId == "user_2" }
        
        assertNotNull("Participant 1 should be found", foundParticipant1)
        assertNotNull("Participant 2 should be found", foundParticipant2)
        assertEquals("Participant 1 role should match", ParticipantRole.PARTICIPANT, foundParticipant1!!.role)
        assertEquals("Participant 2 role should match", ParticipantRole.MODERATOR, foundParticipant2!!.role)
    }

    @Test
    fun testPermissionManagement() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val sessionConfig = SessionConfig(
            sessionName = "Permission Test",
            videoUri = Uri.parse("content://test/video.mp4")
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        val userId = "test_user"
        val newPermissions = setOf(
            Permission.VIEW_VIDEO,
            Permission.CONTROL_PLAYBACK,
            Permission.CREATE_ANNOTATIONS
        )
        
        // When
        val updateResult = collaborationEngine.updateParticipantPermissions(sessionId, userId, newPermissions)
        
        // Then
        assertNotNull("Update result should not be null", updateResult)
        assertTrue("Permission update should succeed", updateResult.success)
        assertEquals("Updated permissions should match", newPermissions, updateResult.updatedPermissions)
        assertTrue("Update time should be set", updateResult.updateTime > 0)
    }

    @Test
    fun testVoiceCallManagement() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val sessionConfig = SessionConfig(
            sessionName = "Voice Test",
            videoUri = Uri.parse("content://test/video.mp4"),
            enableVoiceCall = true
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        val voiceConfig = VoiceCallConfig(
            enableEchoCancellation = true,
            enableNoiseSuppression = true,
            audioQuality = AudioQuality.HIGH,
            maxParticipants = 5
        )
        
        // When
        val startResult = collaborationEngine.startVoiceCall(sessionId, voiceConfig)
        
        // Then
        assertNotNull("Start result should not be null", startResult)
        assertTrue("Voice call start should succeed", startResult.success)
        assertNotNull("Call ID should be generated", startResult.callId)
        assertTrue("Start time should be set", startResult.startTime > 0)
        
        // Test stopping voice call
        val stopResult = collaborationEngine.stopVoiceCall(sessionId)
        assertNotNull("Stop result should not be null", stopResult)
        assertTrue("Voice call stop should succeed", stopResult.success)
    }

    @Test
    fun testScreenSharing() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val sessionConfig = SessionConfig(
            sessionName = "Screen Share Test",
            videoUri = Uri.parse("content://test/video.mp4"),
            enableScreenShare = true
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        val shareConfig = ScreenShareConfig(
            resolution = Pair(1920, 1080),
            frameRate = 30,
            bitrate = 2000000,
            enableAudio = true
        )
        
        // When
        val startResult = collaborationEngine.startScreenShare(sessionId, shareConfig)
        
        // Then
        assertNotNull("Start result should not be null", startResult)
        assertTrue("Screen share start should succeed", startResult.success)
        assertNotNull("Share ID should be generated", startResult.shareId)
        assertTrue("Start time should be set", startResult.startTime > 0)
        
        // Test stopping screen share
        val stopResult = collaborationEngine.stopScreenShare(sessionId)
        assertNotNull("Stop result should not be null", stopResult)
        assertTrue("Screen share stop should succeed", stopResult.success)
    }

    @Test
    fun testSharedPlaylistManagement() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val sessionConfig = SessionConfig(
            sessionName = "Playlist Test",
            videoUri = Uri.parse("content://test/video.mp4")
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        val playlistItem = SharedPlaylistItem(
            itemId = "item_1",
            videoUri = Uri.parse("content://test/video2.mp4"),
            title = "Test Video 2",
            duration = 180000L,
            addedByUserId = "user_1",
            addedByDisplayName = "Test User",
            addedAt = System.currentTimeMillis()
        )
        
        // When
        val addResult = collaborationEngine.addToSharedPlaylist(sessionId, playlistItem)
        
        // Then
        assertNotNull("Add result should not be null", addResult)
        assertTrue("Add to playlist should succeed", addResult.success)
        assertEquals("Item ID should match", playlistItem.itemId, addResult.itemId)
        assertTrue("Add time should be set", addResult.addTime > 0)
        
        // Test getting playlist
        val playlist = collaborationEngine.getSharedPlaylist(sessionId)
        assertNotNull("Playlist should not be null", playlist)
        assertTrue("Playlist should contain added item", 
                  playlist!!.items.any { it.itemId == playlistItem.itemId })
    }

    @Test
    fun testSessionLeaving() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val sessionConfig = SessionConfig(
            sessionName = "Leave Test",
            videoUri = Uri.parse("content://test/video.mp4")
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        // When
        val leaveResult = collaborationEngine.leaveSession(sessionId)
        
        // Then
        assertNotNull("Leave result should not be null", leaveResult)
        assertTrue("Leave should succeed", leaveResult.success)
        assertEquals("Left session ID should match", sessionId, leaveResult.sessionId)
        assertTrue("Leave time should be set", leaveResult.leaveTime > 0)
    }

    @Test
    fun testSessionDestroy() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val sessionConfig = SessionConfig(
            sessionName = "Destroy Test",
            videoUri = Uri.parse("content://test/video.mp4")
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        // When
        val destroyResult = collaborationEngine.destroySession(sessionId)
        
        // Then
        assertNotNull("Destroy result should not be null", destroyResult)
        assertTrue("Destroy should succeed", destroyResult.success)
        assertEquals("Destroyed session ID should match", sessionId, destroyResult.sessionId)
        assertTrue("Destroy time should be set", destroyResult.destroyTime > 0)
        
        // Verify session no longer exists
        val sessionInfo = collaborationEngine.getSessionInfo(sessionId)
        assertTrue("Session should no longer exist", sessionInfo == null || !sessionInfo.isActive)
    }

    @Test
    fun testCollaborationEventEmission() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val events = mutableListOf<CollaborationEvent>()
        val job = launch {
            collaborationEngine.collaborationEvents.collect { event ->
                events.add(event)
            }
        }
        
        // When - Perform various collaboration actions
        val sessionConfig = SessionConfig(
            sessionName = "Event Test",
            videoUri = Uri.parse("content://test/video.mp4")
        )
        collaborationEngine.createSession(sessionConfig)
        advanceUntilIdle()
        
        job.cancel()
        
        // Then
        assertTrue("Should have emitted events", events.isNotEmpty())
        
        val hasSystemInitialized = events.any { it is CollaborationEvent.SystemInitialized }
        val hasSessionCreated = events.any { it is CollaborationEvent.SessionCreated }
        
        assertTrue("Should have system initialized event", hasSystemInitialized)
        assertTrue("Should have session created event", hasSessionCreated)
    }

    @Test
    fun testConnectionManagement() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val connectionConfig = ConnectionConfig(
            serverUrl = "wss://collaboration.astralstream.com",
            enableReconnection = true,
            heartbeatInterval = 30000L,
            connectionTimeout = 10000L
        )
        
        // When
        val connectResult = collaborationEngine.connect(connectionConfig)
        
        // Then
        assertNotNull("Connect result should not be null", connectResult)
        assertTrue("Connection should succeed", connectResult.success)
        assertTrue("Connection time should be set", connectResult.connectionTime > 0)
        
        // Verify connection state
        val state = collaborationEngine.collaborationState.value
        assertTrue("Should be connected", state.isConnected)
        assertNotNull("Server URL should be set", state.serverUrl)
        
        // Test disconnection
        val disconnectResult = collaborationEngine.disconnect()
        assertNotNull("Disconnect result should not be null", disconnectResult)
        assertTrue("Disconnection should succeed", disconnectResult.success)
    }

    @Test
    fun testChatMessageHistory() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val sessionConfig = SessionConfig(
            sessionName = "Chat History Test",
            videoUri = Uri.parse("content://test/video.mp4"),
            enableChat = true
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        // Send multiple messages
        val messages = (1..5).map { index ->
            ChatMessage(
                messageId = "msg_$index",
                sessionId = sessionId,
                senderId = "user_1",
                senderDisplayName = "Test User",
                content = "Message $index",
                timestamp = System.currentTimeMillis() + index * 1000,
                messageType = MessageType.TEXT
            )
        }
        
        messages.forEach { message ->
            collaborationEngine.sendChatMessage(message)
        }
        advanceUntilIdle()
        
        // When
        val history = collaborationEngine.getChatHistory(sessionId, limit = 10)
        
        // Then
        assertNotNull("Chat history should not be null", history)
        assertTrue("Should have messages", history!!.isNotEmpty())
        assertEquals("Should have all sent messages", messages.size, history.size)
        
        // Verify messages are in chronological order
        history.zipWithNext { current, next ->
            assertTrue("Messages should be in chronological order", 
                      current.timestamp <= next.timestamp)
        }
    }

    @Test
    fun testAnnotationRetrieval() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val sessionConfig = SessionConfig(
            sessionName = "Annotation Retrieval Test",
            videoUri = Uri.parse("content://test/video.mp4"),
            enableAnnotations = true
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        // Create multiple annotations
        val annotations = (1..3).map { index ->
            VideoAnnotation(
                annotationId = "anno_$index",
                sessionId = sessionId,
                creatorId = "user_1",
                creatorDisplayName = "Test User",
                timestamp = index * 60000L, // 1 minute intervals
                content = "Annotation $index",
                annotationType = AnnotationType.TEXT,
                position = AnnotationPosition(x = 0.5f, y = 0.3f + index * 0.1f),
                duration = 3000L,
                createdAt = System.currentTimeMillis() + index * 1000
            )
        }
        
        annotations.forEach { annotation ->
            collaborationEngine.createAnnotation(annotation)
        }
        advanceUntilIdle()
        
        // When
        val retrievedAnnotations = collaborationEngine.getAnnotations(sessionId, startTime = 0L, endTime = 300000L)
        
        // Then
        assertNotNull("Annotations should not be null", retrievedAnnotations)
        assertTrue("Should have annotations", retrievedAnnotations!!.isNotEmpty())
        assertEquals("Should have all created annotations", annotations.size, retrievedAnnotations.size)
        
        // Verify annotations are sorted by timestamp
        retrievedAnnotations.zipWithNext { current, next ->
            assertTrue("Annotations should be sorted by timestamp", 
                      current.timestamp <= next.timestamp)
        }
    }

    @Test
    fun testMaxParticipantsLimit() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val sessionConfig = SessionConfig(
            sessionName = "Max Participants Test",
            videoUri = Uri.parse("content://test/video.mp4"),
            maxParticipants = 2
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        // Try to add participants up to the limit
        val user1 = UserInfo(userId = "user_1", displayName = "User 1")
        val user2 = UserInfo(userId = "user_2", displayName = "User 2")
        val user3 = UserInfo(userId = "user_3", displayName = "User 3")
        
        // When
        val join1 = collaborationEngine.joinSession(sessionId, user1)
        val join2 = collaborationEngine.joinSession(sessionId, user2)
        val join3 = collaborationEngine.joinSession(sessionId, user3) // Should fail
        
        // Then
        assertTrue("First user should join successfully", join1.success)
        assertTrue("Second user should join successfully", join2.success)
        assertFalse("Third user should not join (exceeds limit)", join3.success)
        assertEquals("Error should indicate session full", "Session is full", join3.errorMessage)
    }

    @Test
    fun testPrivateSessionAccess() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        val sessionConfig = SessionConfig(
            sessionName = "Private Session Test",
            videoUri = Uri.parse("content://test/video.mp4"),
            isPrivate = true,
            password = "secret123"
        )
        val session = collaborationEngine.createSession(sessionConfig)
        val sessionId = session.sessionId!!
        
        val userInfo = UserInfo(userId = "user_1", displayName = "User 1")
        
        // When - Try to join without password
        val joinWithoutPassword = collaborationEngine.joinSession(sessionId, userInfo)
        
        // When - Try to join with wrong password
        val joinWithWrongPassword = collaborationEngine.joinSession(sessionId, userInfo, password = "wrong")
        
        // When - Try to join with correct password
        val joinWithCorrectPassword = collaborationEngine.joinSession(sessionId, userInfo, password = "secret123")
        
        // Then
        assertFalse("Should not join without password", joinWithoutPassword.success)
        assertFalse("Should not join with wrong password", joinWithWrongPassword.success)
        assertTrue("Should join with correct password", joinWithCorrectPassword.success)
    }

    @Test
    fun testCollaborationStateTracking() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        // Initial state
        var state = collaborationEngine.collaborationState.value
        assertTrue("Should be initialized", state.isInitialized)
        assertFalse("Should not be connected initially", state.isConnected)
        assertEquals("Initial active sessions should be 0", 0, state.activeSessions)
        
        // Create session
        val sessionConfig = SessionConfig(
            sessionName = "State Test",
            videoUri = Uri.parse("content://test/video.mp4")
        )
        collaborationEngine.createSession(sessionConfig)
        advanceUntilIdle()
        
        // Check updated state
        state = collaborationEngine.collaborationState.value
        assertTrue("Should be hosting after creating session", state.isHosting)
        assertTrue("Active sessions should increase", state.activeSessions > 0)
        assertTrue("Last session activity should be recent", 
                  System.currentTimeMillis() - state.lastSessionActivity < 5000)
    }

    @Test
    fun testConcurrentSessionOperations() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        // When - Create multiple sessions concurrently
        val sessionJobs = (1..3).map { index ->
            async {
                val sessionConfig = SessionConfig(
                    sessionName = "Concurrent Session $index",
                    videoUri = Uri.parse("content://test/video$index.mp4")
                )
                collaborationEngine.createSession(sessionConfig)
            }
        }
        
        val sessionResults = sessionJobs.awaitAll()
        
        // Then
        assertEquals("All sessions should be created", 3, sessionResults.size)
        sessionResults.forEach { result ->
            assertNotNull("Each session result should not be null", result)
            assertTrue("Each session should be created successfully", result.success)
            assertNotNull("Each session should have an ID", result.sessionId)
        }
        
        // Verify all sessions are unique
        val sessionIds = sessionResults.mapNotNull { it.sessionId }.toSet()
        assertEquals("All session IDs should be unique", 3, sessionIds.size)
    }

    @Test
    fun testErrorHandling() = runTest {
        collaborationEngine.initialize()
        advanceUntilIdle()
        
        // Test joining non-existent session
        val userInfo = UserInfo(userId = "test_user", displayName = "Test User")
        val joinResult = collaborationEngine.joinSession("non_existent_session", userInfo)
        
        assertFalse("Should not join non-existent session", joinResult.success)
        assertNotNull("Should have error message", joinResult.errorMessage)
        assertTrue("Error message should indicate session not found", 
                  joinResult.errorMessage!!.contains("not found") || 
                  joinResult.errorMessage.contains("does not exist"))
        
        // Test invalid operations
        val invalidSyncResult = collaborationEngine.syncPlayback("invalid_session", PlaybackState(0L, false, 1.0f, System.currentTimeMillis()))
        assertFalse("Should not sync invalid session", invalidSyncResult.success)
        
        val invalidChatResult = collaborationEngine.sendChatMessage(
            ChatMessage(
                messageId = "msg_1",
                sessionId = "invalid_session",
                senderId = "user_1",
                senderDisplayName = "User",
                content = "Test",
                timestamp = System.currentTimeMillis(),
                messageType = MessageType.TEXT
            )
        )
        assertFalse("Should not send chat to invalid session", invalidChatResult.success)
    }
}