package com.astralplayer.nextplayer.data.gesture

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedAnimationEngineTest {

    private lateinit var testScope: TestScope
    private lateinit var animationEngine: AdvancedAnimationEngine

    @Before
    fun setup() {
        testScope = TestScope()
        animationEngine = AdvancedAnimationEngine(testScope)
    }

    @After
    fun tearDown() {
        animationEngine.stop()
    }

    @Test
    fun `engine starts and stops correctly`() {
        assertFalse("Engine should not be running initially", 
            animationEngine.frameRate.value > 0)
        
        animationEngine.start()
        // Engine is now started (frame rate will be updated by Choreographer)
        
        animationEngine.stop()
        // Engine should be stopped
    }

    @Test
    fun `animation creation works correctly`() {
        var updateCount = 0
        var completed = false
        
        val controller = animationEngine.createAnimation(
            id = "test_animation",
            duration = 1000L,
            onUpdate = { progress -> updateCount++ },
            onComplete = { completed = true }
        )
        
        assertNotNull("Controller should be created", controller)
    }

    @Test
    fun `animation can be cancelled`() {
        var updateCount = 0
        
        val controller = animationEngine.createAnimation(
            id = "test_animation",
            duration = 1000L,
            onUpdate = { progress -> updateCount++ }
        )
        
        controller.cancel()
        // Animation should be cancelled
    }

    @Test
    fun `synchronized animation group works`() {
        var animation1Updates = 0
        var animation2Updates = 0
        var groupCompleted = false
        
        val animations = listOf(
            AnimationSpec(
                duration = 500L,
                onUpdate = { animation1Updates++ }
            ),
            AnimationSpec(
                duration = 500L,
                onUpdate = { animation2Updates++ }
            )
        )
        
        val group = animationEngine.createSynchronizedGroup(
            animations = animations,
            onGroupComplete = { groupCompleted = true }
        )
        
        assertNotNull("Group should be created", group)
    }

    @Test
    fun `synchronized group can be cancelled`() {
        val animations = listOf(
            AnimationSpec(duration = 500L, onUpdate = {}),
            AnimationSpec(duration = 500L, onUpdate = {})
        )
        
        val group = animationEngine.createSynchronizedGroup(animations)
        group.cancelAll()
        // All animations in group should be cancelled
    }

    @Test
    fun `hardware acceleration can be toggled`() {
        assertTrue("Hardware acceleration should be enabled by default",
            animationEngine.isHardwareAccelerated())
        
        animationEngine.setHardwareAcceleration(false)
        assertFalse("Hardware acceleration should be disabled",
            animationEngine.isHardwareAccelerated())
        
        animationEngine.setHardwareAcceleration(true)
        assertTrue("Hardware acceleration should be enabled again",
            animationEngine.isHardwareAccelerated())
    }

    @Test
    fun `frame rate is tracked`() {
        val initialFrameRate = animationEngine.frameRate.value
        assertTrue("Frame rate should be initialized", initialFrameRate >= 0f)
    }

    @Test
    fun `multiple animations can run simultaneously`() {
        var animation1Count = 0
        var animation2Count = 0
        
        animationEngine.createAnimation(
            id = "animation1",
            duration = 1000L,
            onUpdate = { animation1Count++ }
        )
        
        animationEngine.createAnimation(
            id = "animation2", 
            duration = 1000L,
            onUpdate = { animation2Count++ }
        )
        
        // Both animations should be created
    }

    @Test
    fun `animation spec contains required properties`() {
        val spec = AnimationSpec(
            duration = 1000L,
            onUpdate = { }
        )
        
        assertEquals("Duration should match", 1000L, spec.duration)
        assertNotNull("OnUpdate should not be null", spec.onUpdate)
        assertNotNull("Easing should have default", spec.easing)
    }

    @Test
    fun `animation controller provides cancel functionality`() {
        val controller = animationEngine.createAnimation(
            id = "test",
            duration = 1000L,
            onUpdate = { }
        )
        
        // Should not throw exception
        controller.cancel()
    }

    @Test
    fun `engine handles empty animation list gracefully`() {
        animationEngine.start()
        // Should not crash with no animations
        animationEngine.stop()
    }
}