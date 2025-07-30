package com.astralplayer.nextplayer.feature.player.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.astralplayer.nextplayer.R
import com.astralplayer.nextplayer.feature.settings.SpeedControlManager

/**
 * A popup menu for selecting video playback speed with presets and custom options.
 */
class PlaybackSpeedMenu(private val context: Context) {
    private lateinit var popupWindow: PopupWindow
    private lateinit var speedAdapter: SpeedAdapter
    private lateinit var speedControlManager: SpeedControlManager
    private var onSpeedSelectedListener: ((Float) -> Unit)? = null
    private var currentSpeed = 1.0f

    init {
        speedControlManager = SpeedControlManager(context)
        initializePopupWindow()
    }

    private fun initializePopupWindow() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.popup_playback_speed, null)

        // Set up the RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_speed_options)
        recyclerView.layoutManager = LinearLayoutManager(context)

        speedAdapter = SpeedAdapter(speedControlManager.getAllPresets(), currentSpeed) { speed ->
            currentSpeed = speed
            onSpeedSelectedListener?.invoke(speed)
            popupWindow.dismiss()
        }
        recyclerView.adapter = speedAdapter

        // Set header
        view.findViewById<TextView>(R.id.text_playback_speed_header).text = "Playback Speed"

        // Set up the "Add Custom" button
        val addCustomButton = view.findViewById<Button>(R.id.button_add_custom)
        addCustomButton.setOnClickListener {
            showAddCustomSpeedDialog()
        }

        // Create the popup window
        popupWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        // Make sure the popup window is dismissed when clicked outside
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
    }

    /**
     * Shows the playback speed selection popup anchored to the given view.
     */
    fun show(anchorView: View) {
        // Update current speed value
        speedAdapter.setCurrentSpeed(currentSpeed)

        popupWindow.showAtLocation(
            anchorView,
            Gravity.CENTER,
            0,
            0
        )
    }

    /**
     * Sets the current playback speed.
     */
    fun setCurrentSpeed(speed: Float) {
        currentSpeed = speed
    }

    /**
     * Gets the current playback speed.
     */
    fun getCurrentSpeed(): Float {
        return currentSpeed
    }

    /**
     * Sets a listener to be called when a new speed is selected.
     */
    fun setOnSpeedSelectedListener(listener: (Float) -> Unit) {
        onSpeedSelectedListener = listener
    }

    /**
     * Shows a dialog for adding a custom playback speed.
     */
    private fun showAddCustomSpeedDialog() {
        // In a real implementation, this would show a dialog with an EditText
        // For simplicity, we'll just add a speed of 1.8x if it doesn't exist
        val customSpeed = 1.8f

        if (speedControlManager.addCustomPreset(customSpeed)) {
            // Update the adapter with the new list
            speedAdapter.updateSpeeds(speedControlManager.getAllPresets())
            Toast.makeText(context, "Added ${customSpeed}x speed", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Speed ${customSpeed}x already exists", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Adapter for the playback speed options.
     */
    private inner class SpeedAdapter(
        private var speeds: List<Float>,
        private var currentSpeed: Float,
        private val onSpeedClick: (Float) -> Unit
    ) : RecyclerView.Adapter<SpeedAdapter.SpeedViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeedViewHolder {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_playback_speed, parent, false)
            return SpeedViewHolder(view)
        }

        override fun onBindViewHolder(holder: SpeedViewHolder, position: Int) {
            val speed = speeds[position]
            holder.bind(speed)
        }

        override fun getItemCount(): Int = speeds.size

        fun setCurrentSpeed(speed: Float) {
            currentSpeed = speed
            notifyDataSetChanged()
        }

        fun updateSpeeds(newSpeeds: List<Float>) {
            speeds = newSpeeds
            notifyDataSetChanged()
        }

        inner class SpeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val speedText: TextView = itemView.findViewById(R.id.text_speed)
            private val speedContainer: LinearLayout = itemView.findViewById(R.id.container_speed)

            fun bind(speed: Float) {
                // Format the speed text (e.g., "1.5x")
                speedText.text = "${speed}x"

                // Highlight current selection
                val isSelected = speed == currentSpeed
                val background = GradientDrawable()
                background.shape = GradientDrawable.RECTANGLE
                background.cornerRadius = 8f

                if (isSelected) {
                    background.setColor(ContextCompat.getColor(context, android.R.color.holo_blue_light))
                    speedText.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                } else {
                    background.setColor(ContextCompat.getColor(context, android.R.color.transparent))
                    speedText.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                }

                speedContainer.background = background

                // Set click listener
                itemView.setOnClickListener {
                    onSpeedClick(speed)
                }
            }
        }
    }
}
