package com.astralplayer.nextplayer.feature.player

import android.os.Handler
import android.os.Looper

/**
 * Manages A-B repeat functionality, allowing users to repeat specific
 * segments of a video continuously.
 */
class ABRepeatManager {
    private var pointA: Long = -1  // Start position in ms, -1 means not set
    private var pointB: Long = -1  // End position in ms, -1 means not set
    private var isActive = false
    private var listener: ABRepeatListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private var checkPositionRunnable: Runnable? = null
    private var currentPosition = 0L

    /**
     * Sets point A (start) of the repeat segment.
     * @param positionMs Position in milliseconds
     */
    fun setPointA(positionMs: Long) {
        pointA = positionMs
        pointB = -1  // Clear point B when setting new point A
        isActive = false

        listener?.onPointASet(positionMs)
    }

    /**
     * Sets point B (end) of the repeat segment.
     * @param positionMs Position in milliseconds
     * @return true if successfully set, false if invalid (e.g., if pointA not set or if B <= A)
     */
    fun setPointB(positionMs: Long): Boolean {
        if (pointA == -1L || positionMs <= pointA) {
            return false
        }

        pointB = positionMs
        isActive = true
        listener?.onPointBSet(positionMs)

        // Start checking position for loop
        startPositionChecking()

        return true
    }

    /**
     * Clears both repeat points and deactivates the repeat.
     */
    fun clearPoints() {
        pointA = -1
        pointB = -1
        isActive = false
        stopPositionChecking()
        listener?.onRepeatCleared()
    }

    /**
     * Updates the current playback position and checks if looping is needed.
     * Should be called regularly during playback.
     */
    fun updatePosition(positionMs: Long) {
        currentPosition = positionMs

        // If we're in active AB repeat mode and past point B, loop back to point A
        if (isActive && pointA != -1L && pointB != -1L && positionMs >= pointB) {
            listener?.onRepeatTriggered(pointA)
        }
    }

    /**
     * Starts the position checking loop.
     */
    private fun startPositionChecking() {
        stopPositionChecking() // Stop any existing loop

        checkPositionRunnable = Runnable {
            if (isActive) {
                updatePosition(currentPosition)
                handler.postDelayed(checkPositionRunnable!!, CHECK_INTERVAL_MS)
            }
        }

        handler.postDelayed(checkPositionRunnable!!, CHECK_INTERVAL_MS)
    }

    /**
     * Stops the position checking loop.
     */
    private fun stopPositionChecking() {
        checkPositionRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

    /**
     * Checks if AB repeat is active.
     */
    fun isActive(): Boolean {
        return isActive
    }

    /**
     * Gets point A (start) position in milliseconds.
     * @return Position in ms, or -1 if not set
     */
    fun getPointA(): Long {
        return pointA
    }

    /**
     * Gets point B (end) position in milliseconds.
     * @return Position in ms, or -1 if not set
     */
    fun getPointB(): Long {
        return pointB
    }

    /**
     * Sets a listener for AB repeat events.
     */
    fun setListener(listener: ABRepeatListener) {
        this.listener = listener
    }

    /**
     * Listener interface for AB repeat events.
     */
    interface ABRepeatListener {
        fun onPointASet(positionMs: Long)
        fun onPointBSet(positionMs: Long)
        fun onRepeatTriggered(jumpToPositionMs: Long)
        fun onRepeatCleared()
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 100L  // Check position every 100ms
    }
}
