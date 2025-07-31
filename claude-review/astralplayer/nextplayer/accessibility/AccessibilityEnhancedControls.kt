package com.astralplayer.nextplayer.accessibility

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.SeekBar
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityEventCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Accessibility-enhanced media controls with advanced features
 */
class AccessibilityEnhancedSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SeekBar(context, attrs, defStyleAttr) {

    private var accessibilityManager: AccessibilityManager? = null
    private var isAccessibilityFocused = false
    private var accessibilityValue = ""
    private var accessibilityDescription = ""
    
    // Enhanced touch targets
    private val minTouchTargetSize = (48 * context.resources.displayMetrics.density).toInt()
    private var enhancedTouchBounds = Rect()
    
    // Audio feedback
    private var lastAnnouncedProgress = -1
    private val announceThreshold = 5 // Announce every 5% change
    
    init {
        setupAccessibility()
    }
    
    fun setAccessibilityManager(manager: AccessibilityManager) {
        this.accessibilityManager = manager
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        // Ensure minimum touch target size
        val width = measuredWidth
        val height = maxOf(measuredHeight, minTouchTargetSize)
        
        setMeasuredDimension(width, height)
        
        // Calculate enhanced touch bounds
        val expandVertical = maxOf(0, (minTouchTargetSize - measuredHeight) / 2)
        enhancedTouchBounds.set(
            0,
            -expandVertical,
            width,
            measuredHeight + expandVertical
        )
    }
    
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        
        // Draw accessibility focus indicator
        if (isAccessibilityFocused && isFocused) {
            drawAccessibilityFocusIndicator(canvas)
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MINUS -> {
                val newProgress = maxOf(0, progress - 1)
                setProgressWithAnnouncement(newProgress, "Decreased")
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PLUS -> {
                val newProgress = minOf(max, progress + 1)
                setProgressWithAnnouncement(newProgress, "Increased")
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                val newProgress = minOf(max, progress + 10)
                setProgressWithAnnouncement(newProgress, "Increased by 10")
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                val newProgress = maxOf(0, progress - 10)
                setProgressWithAnnouncement(newProgress, "Decreased by 10")
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        updateAccessibilityValue()
        checkForProgressAnnouncement(progress)
    }
    
    private fun setupAccessibility() {
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                info.className = "android.widget.SeekBar"
                info.contentDescription = accessibilityDescription
                info.text = accessibilityValue
                
                // Add range information
                info.rangeInfo = AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
                    AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_INT,
                    0f, max.toFloat(), progress.toFloat()
                )
                
                // Add custom actions
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SET_PROGRESS)
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
                    "Increase value"
                ))
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
                    "Decrease value"
                ))
                
                // Set enhanced touch bounds
                info.setBoundsInParent(enhancedTouchBounds)
            }
            
            override fun performAccessibilityAction(
                host: View,
                action: Int,
                args: android.os.Bundle?
            ): Boolean {
                when (action) {
                    AccessibilityNodeInfo.ACTION_SET_PROGRESS -> {
                        val value = args?.getFloat(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,
                            progress.toFloat()
                        ) ?: progress.toFloat()
                        
                        setProgressWithAnnouncement(value.toInt(), "Set to")
                        return true
                    }
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> {
                        val newProgress = minOf(max, progress + getScrollIncrement())
                        setProgressWithAnnouncement(newProgress, "Increased")
                        return true
                    }
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> {
                        val newProgress = maxOf(0, progress - getScrollIncrement())
                        setProgressWithAnnouncement(newProgress, "Decreased")
                        return true
                    }
                }
                return super.performAccessibilityAction(host, action, args)
            }
            
            override fun onPopulateAccessibilityEvent(host: View, event: AccessibilityEvent) {
                super.onPopulateAccessibilityEvent(host, event)
                
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                    isAccessibilityFocused = true
                    invalidate() // Redraw to show focus indicator
                } else if (event.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED) {
                    isAccessibilityFocused = false
                    invalidate()
                }
            }
        })
        
        // Enable accessibility features
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
    }
    
    private fun setProgressWithAnnouncement(newProgress: Int, action: String) {
        progress = newProgress
        
        accessibilityManager?.let { manager ->
            GlobalScope.launch {
                manager.provideAudioFeedback(
                    UserAction.SEEK_FORWARD, // or SEEK_BACKWARD based on direction
                    "$action: $accessibilityValue",
                    FeedbackPriority.NORMAL
                )
                manager.provideHapticFeedback(HapticFeedbackType.LIGHT)
            }
        }
        
        announceForAccessibility("$action: $accessibilityValue")
    }
    
    private fun updateAccessibilityValue() {
        val percentage = if (max > 0) (progress * 100) / max else 0
        accessibilityValue = "$percentage percent"
    }
    
    private fun checkForProgressAnnouncement(currentProgress: Int) {
        val currentPercent = if (max > 0) (currentProgress * 100) / max else 0
        val lastPercent = if (max > 0) (lastAnnouncedProgress * 100) / max else 0
        
        if (kotlin.math.abs(currentPercent - lastPercent) >= announceThreshold) {
            announceForAccessibility(accessibilityValue)
            lastAnnouncedProgress = currentProgress
        }
    }
    
    private fun drawAccessibilityFocusIndicator(canvas: Canvas?) {
        canvas?.let {
            val focusPaint = Paint().apply {
                color = 0xFF4CAF50.toInt() // Green focus indicator
                style = Paint.Style.STROKE
                strokeWidth = 4f * context.resources.displayMetrics.density
            }
            
            val focusRect = Rect(0, 0, width, height)
            it.drawRect(focusRect, focusPaint)
        }
    }
    
    private fun getScrollIncrement(): Int {
        return maxOf(1, max / 20) // 5% increments
    }
    
    fun setAccessibilityDescription(description: String) {
        accessibilityDescription = description
        contentDescription = description
    }
    
    fun announceProgress() {
        announceForAccessibility(accessibilityValue)
    }
}

/**
 * Accessibility-enhanced media button with improved feedback
 */
class AccessibilityEnhancedMediaButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageButton(context, attrs, defStyleAttr) {

    private var accessibilityManager: AccessibilityManager? = null
    private var accessibilityLabel = ""
    private var accessibilityHint = ""
    private var currentState = ""
    
    // Enhanced touch targets
    private val minTouchTargetSize = (48 * context.resources.displayMetrics.density).toInt()
    
    init {
        setupAccessibility()
    }
    
    fun setAccessibilityManager(manager: AccessibilityManager) {
        this.accessibilityManager = manager
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        // Ensure minimum touch target size
        val width = maxOf(measuredWidth, minTouchTargetSize)
        val height = maxOf(measuredHeight, minTouchTargetSize) 
        
        setMeasuredDimension(width, height)
    }
    
    override fun performClick(): Boolean {
        val result = super.performClick()
        
        // Provide accessibility feedback
        accessibilityManager?.let { manager ->
            GlobalScope.launch {
                manager.provideAudioFeedback(
                    UserAction.PLAY, // This should be dynamic based on button type
                    accessibilityLabel,
                    FeedbackPriority.HIGH
                )
                manager.provideHapticFeedback(HapticFeedbackType.MEDIUM)
            }
        }
        
        announceForAccessibility("$accessibilityLabel activated")
        return result
    }
    
    private fun setupAccessibility() {
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                info.className = "android.widget.Button"
                info.contentDescription = getFullAccessibilityDescription()
                info.hintText = accessibilityHint
                
                // Add click action
                info.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                
                // Ensure minimum touch target
                ensureMinimumTouchTarget(info)
            }
            
            override fun performAccessibilityAction(
                host: View,
                action: Int,
                args: android.os.Bundle?
            ): Boolean {
                when (action) {
                    AccessibilityNodeInfo.ACTION_CLICK -> {
                        return performClick()
                    }
                }
                return super.performAccessibilityAction(host, action, args)
            }
        })
        
        // Enable accessibility features
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
        isClickable = true
    }
    
    private fun ensureMinimumTouchTarget(info: AccessibilityNodeInfoCompat) {
        val bounds = Rect()
        getBoundsOnScreen(bounds)
        
        if (bounds.width() < minTouchTargetSize || bounds.height() < minTouchTargetSize) {
            val expandX = maxOf(0, (minTouchTargetSize - bounds.width()) / 2)
            val expandY = maxOf(0, (minTouchTargetSize - bounds.height()) / 2)
            
            bounds.left -= expandX
            bounds.right += expandX
            bounds.top -= expandY
            bounds.bottom += expandY
            
            info.setBoundsInScreen(bounds)
        }
    }
    
    private fun getFullAccessibilityDescription(): String {
        return if (currentState.isNotEmpty()) {
            "$accessibilityLabel, $currentState"
        } else {
            accessibilityLabel
        }
    }
    
    fun setAccessibilityLabel(label: String) {
        accessibilityLabel = label
        contentDescription = getFullAccessibilityDescription()
    }
    
    fun setAccessibilityHint(hint: String) {
        accessibilityHint = hint
    }
    
    fun setCurrentState(state: String) {
        currentState = state
        contentDescription = getFullAccessibilityDescription()
    }
    
    fun announceStateChange(newState: String) {
        currentState = newState
        contentDescription = getFullAccessibilityDescription()
        announceForAccessibility("$accessibilityLabel $newState")
    }
}

/**
 * Accessibility-enhanced video player container
 */
class AccessibilityEnhancedVideoContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var accessibilityManager: AccessibilityManager? = null
    private var currentVideoInfo = ""
    private var playbackState = "stopped"
    private var accessibilityRegions = mutableListOf<AccessibilityRegion>()
    
    init {
        setupAccessibility()
    }
    
    fun setAccessibilityManager(manager: AccessibilityManager) {
        this.accessibilityManager = manager
    }
    
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Layout child views
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.layout(0, 0, r - l, b - t)
        }
        
        if (changed) {
            updateAccessibilityRegions()
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        // Measure all child views
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
        }
        
        setMeasuredDimension(width, height)
    }
    
    private fun setupAccessibility() {
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                info.className = "android.widget.VideoView"
                info.contentDescription = getVideoAccessibilityDescription()
                
                // Add custom actions for video control
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfo.ACTION_CLICK,
                    "Toggle play/pause"
                ))
                
                // Add regions for touch exploration
                for (region in accessibilityRegions) {
                    info.addChild(region.view)
                }
            }
            
            override fun performAccessibilityAction(
                host: View,
                action: Int,
                args: android.os.Bundle?
            ): Boolean {
                when (action) {
                    AccessibilityNodeInfo.ACTION_CLICK -> {
                        togglePlayPause()
                        return true
                    }
                }
                return super.performAccessibilityAction(host, action, args)
            }
        })
        
        // Enable accessibility features
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
        isClickable = true
    }
    
    private fun updateAccessibilityRegions() {
        accessibilityRegions.clear()
        
        // Define accessibility regions for different areas of the video player
        val centerRegion = AccessibilityRegion(
            bounds = Rect(width / 4, height / 4, width * 3 / 4, height * 3 / 4),
            description = "Video content area, double tap to play or pause",
            action = "play_pause"
        )
        
        val leftRegion = AccessibilityRegion(
            bounds = Rect(0, height / 4, width / 4, height * 3 / 4),
            description = "Seek backward area, double tap to rewind",
            action = "seek_backward"
        )
        
        val rightRegion = AccessibilityRegion(
            bounds = Rect(width * 3 / 4, height / 4, width, height * 3 / 4),
            description = "Seek forward area, double tap to fast forward",
            action = "seek_forward"
        )
        
        // Add regions (in a real implementation, these would be actual views)
        // accessibilityRegions.addAll(listOf(centerRegion, leftRegion, rightRegion))
    }
    
    private fun getVideoAccessibilityDescription(): String {
        return if (currentVideoInfo.isNotEmpty()) {
            "Video player: $currentVideoInfo, currently $playbackState"
        } else {
            "Video player, currently $playbackState"
        }
    }
    
    private fun togglePlayPause() {
        playbackState = if (playbackState == "playing") "paused" else "playing"
        announceForAccessibility("Video $playbackState")
        
        accessibilityManager?.let { manager ->
            GlobalScope.launch {
                val action = if (playbackState == "playing") UserAction.PLAY else UserAction.PAUSE
                manager.provideAudioFeedback(action, playbackState, FeedbackPriority.HIGH)
                manager.provideHapticFeedback(HapticFeedbackType.MEDIUM)
            }
        }
    }
    
    fun setVideoInfo(info: String) {
        currentVideoInfo = info
        contentDescription = getVideoAccessibilityDescription()
    }
    
    fun setPlaybackState(state: String) {
        val previousState = playbackState
        playbackState = state
        contentDescription = getVideoAccessibilityDescription()
        
        if (previousState != state) {
            announceForAccessibility("Video $state")
        }
    }
    
    fun announceVideoEvent(event: String) {
        announceForAccessibility(event)
    }
}

/**
 * Accessibility region for touch exploration
 */
data class AccessibilityRegion(
    val bounds: Rect,
    val description: String,
    val action: String,
    val view: View? = null
)

/**
 * Accessibility-enhanced control panel
 */
class AccessibilityEnhancedControlPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var accessibilityManager: AccessibilityManager? = null
    private val controls = mutableListOf<View>()
    private var currentFocusIndex = 0
    
    init {
        setupAccessibility()
    }
    
    fun setAccessibilityManager(manager: AccessibilityManager) {
        this.accessibilityManager = manager
        
        // Apply accessibility manager to all child controls
        for (control in controls) {
            when (control) {
                is AccessibilityEnhancedMediaButton -> control.setAccessibilityManager(manager)
                is AccessibilityEnhancedSeekBar -> control.setAccessibilityManager(manager)
            }
        }
    }
    
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Layout controls in a horizontal row
        val controlWidth = (r - l) / maxOf(1, childCount)
        val controlHeight = b - t
        
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val left = i * controlWidth
            val right = left + controlWidth
            child.layout(left, 0, right, controlHeight)
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        // Measure all children
        val childWidthSpec = MeasureSpec.makeMeasureSpec(width / maxOf(1, childCount), MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        
        for (i in 0 until childCount) {
            getChildAt(i).measure(childWidthSpec, childHeightSpec)
        }
        
        setMeasuredDimension(width, height)
    }
    
    override fun addView(child: View) {
        super.addView(child)
        controls.add(child)
        setupChildAccessibility(child)
    }
    
    private fun setupAccessibility() {
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                info.className = "android.widget.LinearLayout"
                info.contentDescription = "Media control panel with ${childCount} controls"
                
                // Add all children as accessible nodes
                for (i in 0 until childCount) {
                    info.addChild(getChildAt(i))
                }
            }
        })
        
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    
    private fun setupChildAccessibility(child: View) {
        child.isFocusable = true
        
        // Set up keyboard navigation
        val index = controls.indexOf(child)
        if (index > 0) {
            child.nextFocusLeftId = controls[index - 1].id
        }
        if (index < controls.size - 1 && index + 1 < controls.size) {
            child.nextFocusRightId = controls[index + 1].id
        }
        
        // Set up focus change listener
        child.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                currentFocusIndex = index
                announceControlFocus(child)
            }
        }
    }
    
    private fun announceControlFocus(control: View) {
        val announcement = when (control) {
            is AccessibilityEnhancedMediaButton -> "Focused on ${control.contentDescription}"
            is AccessibilityEnhancedSeekBar -> "Focused on progress bar at ${control.progress}%"
            else -> "Focused on ${control.contentDescription ?: "control"}"
        }
        
        announceForAccessibility(announcement)
    }
    
    fun focusNextControl() {
        if (controls.isNotEmpty()) {
            currentFocusIndex = (currentFocusIndex + 1) % controls.size
            controls[currentFocusIndex].requestFocus()
        }
    }
    
    fun focusPreviousControl() {
        if (controls.isNotEmpty()) {
            currentFocusIndex = if (currentFocusIndex > 0) currentFocusIndex - 1 else controls.size - 1
            controls[currentFocusIndex].requestFocus()
        }
    }
}