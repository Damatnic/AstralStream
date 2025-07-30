package com.astralplayer.nextplayer.voice

import android.content.Context
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class VoiceCommandManagerTest {

    private lateinit var mockContext: Context
    private lateinit var voiceCommandManager: VoiceCommandManager
    private lateinit var mockExecutor: VoiceCommandExecutor

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockExecutor = mockk(relaxed = true)
        voiceCommandManager = VoiceCommandManager(mockContext)
        voiceCommandManager.setCommandExecutor(mockExecutor)
    }

    @Test
    fun `initial state is not listening`() {
        assertFalse("Should not be listening initially", voiceCommandManager.isListening.value)
    }

    @Test
    fun `start listening updates state`() {
        voiceCommandManager.startListening()
        assertTrue("Should be listening", voiceCommandManager.isListening.value)
    }

    @Test
    fun `stop listening updates state`() {
        voiceCommandManager.startListening()
        voiceCommandManager.stopListening()
        assertFalse("Should not be listening", voiceCommandManager.isListening.value)
    }

    @Test
    fun `processes play command correctly`() {
        voiceCommandManager.processVoiceInput("play the video")
        
        assertEquals(VoiceCommand.PLAY, voiceCommandManager.lastCommand.value)
        verify { mockExecutor.executeVoiceCommand(VoiceCommand.PLAY) }
    }

    @Test
    fun `processes pause command correctly`() {
        voiceCommandManager.processVoiceInput("pause the video")
        
        assertEquals(VoiceCommand.PAUSE, voiceCommandManager.lastCommand.value)
        verify { mockExecutor.executeVoiceCommand(VoiceCommand.PAUSE) }
    }

    @Test
    fun `processes speed commands correctly`() {
        voiceCommandManager.processVoiceInput("speed up")
        assertEquals(VoiceCommand.SPEED_UP, voiceCommandManager.lastCommand.value)
        
        voiceCommandManager.processVoiceInput("slow down")
        assertEquals(VoiceCommand.SLOW_DOWN, voiceCommandManager.lastCommand.value)
        
        voiceCommandManager.processVoiceInput("normal speed")
        assertEquals(VoiceCommand.NORMAL_SPEED, voiceCommandManager.lastCommand.value)
    }

    @Test
    fun `ignores unrecognized commands`() {
        voiceCommandManager.processVoiceInput("do something random")
        assertNull("Should not recognize command", voiceCommandManager.lastCommand.value)
        verify(exactly = 0) { mockExecutor.executeVoiceCommand(any()) }
    }

    @Test
    fun `returns available commands list`() {
        val commands = voiceCommandManager.getAvailableCommands()
        
        assertTrue("Should have commands", commands.isNotEmpty())
        assertTrue("Should contain Play", commands.contains("Play"))
        assertTrue("Should contain Pause", commands.contains("Pause"))
    }
}