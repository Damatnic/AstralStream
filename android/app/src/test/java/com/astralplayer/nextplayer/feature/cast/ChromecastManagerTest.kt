package com.astralplayer.nextplayer.feature.cast

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.mock
import android.content.Context

class ChromecastManagerTest {
    
    @Test
    fun testInitialState() = runTest {
        val context = mock(Context::class.java)
        val manager = ChromecastManager(context)
        
        assertFalse("Should not be connected initially", manager.isConnected.first())
        assertTrue("Should have no devices initially", manager.availableDevices.first().isEmpty())
    }
    
    @Test
    fun testStartDiscovery() = runTest {
        val context = mock(Context::class.java)
        val manager = ChromecastManager(context)
        
        manager.startDiscovery()
        
        val devices = manager.availableDevices.first()
        assertTrue("Should find mock devices", devices.isNotEmpty())
        assertTrue("Should contain Living Room TV", devices.contains("Living Room TV"))
    }
    
    @Test
    fun testConnect() = runTest {
        val context = mock(Context::class.java)
        val manager = ChromecastManager(context)
        
        manager.connect("Test Device")
        
        assertTrue("Should be connected after connect", manager.isConnected.first())
    }
}