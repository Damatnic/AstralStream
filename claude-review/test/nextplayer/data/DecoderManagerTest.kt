package com.astralplayer.nextplayer.data

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class DecoderManagerTest {

    private lateinit var mockContext: Context
    private lateinit var decoderManager: DecoderManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        decoderManager = DecoderManager(mockContext)
    }

    @Test
    fun `sets decoder preference correctly`() {
        decoderManager.setDecoderPreference(MimeTypes.VIDEO_H264, DecoderPreference.HARDWARE_ONLY)
        
        val preference = decoderManager.getDecoderForCodec(MimeTypes.VIDEO_H264)
        assertEquals(DecoderPreference.HARDWARE_ONLY, preference)
    }

    @Test
    fun `sets global decoder preference correctly`() {
        decoderManager.setGlobalDecoderPreference(DecoderPreference.SOFTWARE_ONLY)
        
        val settings = decoderManager.decoderSettings.value
        assertEquals(DecoderPreference.SOFTWARE_ONLY, settings.globalPreference)
    }

    @Test
    fun `falls back to global preference when codec not set`() {
        decoderManager.setGlobalDecoderPreference(DecoderPreference.HARDWARE_ONLY)
        
        val preference = decoderManager.getDecoderForCodec(MimeTypes.VIDEO_VP9)
        assertEquals(DecoderPreference.HARDWARE_ONLY, preference)
    }

    @Test
    fun `creates renderers factory correctly`() {
        val factory = decoderManager.createRenderersFactory()
        assertNotNull("Should create factory", factory)
    }

    @Test
    fun `analyzes decoder for media item`() {
        val mediaItem = mockk<MediaItem>(relaxed = true)
        val mockConfig = mockk<MediaItem.LocalConfiguration>(relaxed = true)
        
        every { mediaItem.localConfiguration } returns mockConfig
        every { mockConfig.mimeType } returns MimeTypes.VIDEO_H264
        
        val analysis = decoderManager.getDecoderInfo(mediaItem)
        
        assertEquals(MimeTypes.VIDEO_H264, analysis.codec)
        assertNotNull("Should have recommendation", analysis.recommendedDecoder)
        assertNotNull("Should have reason", analysis.reason)
    }

    @Test
    fun `detects available decoders`() {
        val decoders = decoderManager.availableDecoders.value
        assertTrue("Should have decoders", decoders.isNotEmpty())
        
        val h264Decoder = decoders.find { it.codec == MimeTypes.VIDEO_H264 }
        assertNotNull("Should have H264 decoder info", h264Decoder)
    }

    @Test
    fun `resets to defaults correctly`() {
        decoderManager.setGlobalDecoderPreference(DecoderPreference.HARDWARE_ONLY)
        decoderManager.setDecoderPreference(MimeTypes.VIDEO_H264, DecoderPreference.SOFTWARE_ONLY)
        
        decoderManager.resetToDefaults()
        
        val settings = decoderManager.decoderSettings.value
        assertEquals(DecoderPreference.AUTO, settings.globalPreference)
        assertTrue("Should clear codec settings", settings.codecSettings.isEmpty())
    }
}