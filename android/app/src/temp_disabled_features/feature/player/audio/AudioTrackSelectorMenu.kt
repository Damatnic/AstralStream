package com.astralplayer.nextplayer.feature.player.audio

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.astralplayer.nextplayer.R

/**
 * A popup menu for selecting audio tracks in videos with multiple audio streams.
 * Supports language selection, audio formats, and channels.
 */
class AudioTrackSelectorMenu(private val context: Context) {

    data class AudioTrackInfo(
        val id: Int,
        val language: String,
        val format: String, // e.g., "AAC", "AC3", "DTS"
        val channels: Int,  // e.g., 2 for stereo, 6 for 5.1
        val bitrate: Int    // in kbps
    ) {
        fun getDisplayName(): String {
            val channelStr = when (channels) {
                1 -> "Mono"
                2 -> "Stereo"
                6 -> "5.1"
                8 -> "7.1"
                else -> "$channels ch"
            }

            return "$language - $format $channelStr ${bitrate}kbps"
        }
    }

    private var audioTracks = listOf<AudioTrackInfo>()
    private var selectedTrackId = -1
    private var onAudioTrackSelectedListener: ((Int) -> Unit)? = null
    private lateinit var popupWindow: PopupWindow

    init {
        initializePopupWindow()
    }

    private fun initializePopupWindow() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.popup_audio_track, null)

        // Set header
        view.findViewById<TextView>(R.id.text_audio_track_header).text = "Audio Tracks"

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
     * Sets the available audio tracks.
     */
    fun setAudioTracks(tracks: List<AudioTrackInfo>, selectedId: Int) {
        audioTracks = tracks
        selectedTrackId = selectedId
    }

    /**
     * Shows the audio track selection popup anchored to the given view.
     */
    fun show(anchorView: View) {
        // Re-inflate the content each time to update with current tracks
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.popup_audio_track, null)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_audio_track)

        // Create radio buttons for each audio track
        for (track in audioTracks) {
            val radioButton = RadioButton(context)
            radioButton.id = track.id
            radioButton.text = track.getDisplayName()
            radioButton.isChecked = track.id == selectedTrackId

            val params = RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)

            radioGroup.addView(radioButton, params)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != selectedTrackId) {
                selectedTrackId = checkedId
                onAudioTrackSelectedListener?.invoke(checkedId)
            }
            popupWindow.dismiss()
        }

        // Set header
        view.findViewById<TextView>(R.id.text_audio_track_header).text = "Audio Tracks"

        // Update the popup content
        popupWindow.contentView = view

        popupWindow.showAtLocation(
            anchorView,
            Gravity.CENTER,
            0,
            0
        )
    }

    /**
     * Gets the currently selected audio track ID.
     */
    fun getSelectedTrackId(): Int {
        return selectedTrackId
    }

    /**
     * Sets a listener to be called when a new audio track is selected.
     */
    fun setOnAudioTrackSelectedListener(listener: (Int) -> Unit) {
        onAudioTrackSelectedListener = listener
    }
}
