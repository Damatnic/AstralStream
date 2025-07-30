package com.astralplayer.nextplayer.feature.player.ui

import android.content.Context
import com.astralplayer.nextplayer.R
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

/**
 * A popup menu that allows the user to select different aspect ratio modes.
 * This provides multiple aspect ratio options for video display customization.
 */
class AspectRatioMenu(private val context: Context) {

    private var onAspectRatioSelectedListener: ((AspectRatioMode) -> Unit)? = null
    private lateinit var popupWindow: PopupWindow
    private var currentMode = AspectRatioMode.FIT

    init {
        initializePopupWindow()
    }

    private fun initializePopupWindow() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.popup_aspect_ratio, null)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_aspect_ratio)

        // Set header
        view.findViewById<TextView>(R.id.text_aspect_ratio_header).text = "Aspect Ratio"

        // Create the popup window
        popupWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        // Create radio buttons for each aspect ratio mode
        for (mode in AspectRatioMode.values()) {
            val radioButton = RadioButton(context)
            radioButton.id = mode.ordinal
            radioButton.text = getDisplayName(mode)
            radioButton.isChecked = mode == currentMode

            val params = RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)

            radioGroup.addView(radioButton, params)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = AspectRatioMode.values()[checkedId]
            if (selectedMode != currentMode) {
                currentMode = selectedMode
                onAspectRatioSelectedListener?.invoke(selectedMode)
            }
            popupWindow.dismiss()
        }

        // Make sure the popup window is dismissed when clicked outside
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
    }

    /**
     * Shows the aspect ratio selection popup menu anchored to the given view.
     */
    fun show(anchorView: View) {
        // Update radio button selection to current mode
        val radioGroup = popupWindow.contentView.findViewById<RadioGroup>(R.id.radio_group_aspect_ratio)
        radioGroup.check(currentMode.ordinal)

        // Show the popup with a slight offset and gravity
        popupWindow.showAtLocation(
            anchorView,
            Gravity.CENTER,
            0,
            0
        )
    }

    /**
     * Sets the current aspect ratio mode.
     */
    fun setCurrentMode(mode: AspectRatioMode) {
        currentMode = mode
    }

    /**
     * Gets the current aspect ratio mode.
     */
    fun getCurrentMode(): AspectRatioMode {
        return currentMode
    }

    /**
     * Sets a listener to be called when a new aspect ratio is selected.
     */
    fun setOnAspectRatioSelectedListener(listener: (AspectRatioMode) -> Unit) {
        onAspectRatioSelectedListener = listener
    }

    private fun getDisplayName(mode: AspectRatioMode): String {
        return when (mode) {
            AspectRatioMode.FIT -> "Fit (Letterbox)"
            AspectRatioMode.FILL -> "Fill Screen"
            AspectRatioMode.STRETCH -> "Stretch"
            AspectRatioMode.ORIGINAL -> "Original Size"
            AspectRatioMode.ZOOM_16_9 -> "16:9"
            AspectRatioMode.ZOOM_4_3 -> "4:3"
            AspectRatioMode.CUSTOM -> "Custom Zoom"
        }
    }

    /**
     * Enum representing different aspect ratio display modes
     */
    enum class AspectRatioMode {
        FIT,        // Fit video within screen (may have black bars)
        FILL,       // Fill screen completely (may crop edges)
        STRETCH,    // Stretch to fill (distorts aspect ratio)
        ORIGINAL,   // Original pixel size
        ZOOM_16_9,  // Force 16:9 aspect ratio
        ZOOM_4_3,   // Force 4:3 aspect ratio
        CUSTOM      // Custom zoom level
    }
}
