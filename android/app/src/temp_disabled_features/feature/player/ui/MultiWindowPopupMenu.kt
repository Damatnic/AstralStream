package com.astralplayer.nextplayer.feature.player.ui

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.astralplayer.nextplayer.R

/**
 * Popup menu for multi-window display options like Picture-in-Picture
 * and floating window modes.
 */
class MultiWindowPopupMenu(private val context: Context) {

    private lateinit var popupWindow: PopupWindow
    private var listener: MultiWindowOptionListener? = null

    init {
        initializePopupWindow()
    }

    private fun initializePopupWindow() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.popup_multi_window, null)

        // Create the popup window
        popupWindow = PopupWindow(
            view,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        // Setup option cards
        val pipCard = view.findViewById<CardView>(R.id.card_pip_mode)
        val floatingCard = view.findViewById<CardView>(R.id.card_floating_window)
        val splitScreenCard = view.findViewById<CardView>(R.id.card_split_screen)

        // Set header
        view.findViewById<TextView>(R.id.text_multi_window_header).text = "Display Mode"

        // Set up click listeners
        pipCard.setOnClickListener {
            listener?.onPictureInPictureSelected()
            popupWindow.dismiss()
        }

        floatingCard.setOnClickListener {
            listener?.onFloatingWindowSelected()
            popupWindow.dismiss()
        }

        splitScreenCard.setOnClickListener {
            listener?.onSplitScreenSelected()
            popupWindow.dismiss()
        }

        // Make sure the popup window is dismissed when clicked outside
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
    }

    /**
     * Shows the multi-window options popup anchored to the given view.
     */
    fun show(anchorView: View) {
        popupWindow.showAtLocation(
            anchorView,
            Gravity.CENTER,
            0,
            0
        )
    }

    /**
     * Sets a listener for multi-window option selection.
     */
    fun setListener(listener: MultiWindowOptionListener) {
        this.listener = listener
    }

    interface MultiWindowOptionListener {
        fun onPictureInPictureSelected()
        fun onFloatingWindowSelected()
        fun onSplitScreenSelected()
    }
}
