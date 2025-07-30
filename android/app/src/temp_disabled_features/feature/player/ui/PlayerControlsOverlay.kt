package com.astralplayer.nextplayer.feature.player.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.astralplayer.nextplayer.R
import java.util.concurrent.TimeUnit

/**
 * Advanced player controls overlay with smooth animations, playback controls,
 * and customizable options.
 */
class PlayerControlsOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val rootView: ConstraintLayout
    private val playPauseButton: ImageButton
    private val seekBar: SeekBar
    private val timeElapsedText: TextView
    private val timeDurationText: TextView
    private val settingsButton: ImageButton
    private val lockButton: ImageButton
    private val aspectRatioButton: ImageButton
    private val audioTrackButton: ImageButton
    private val subtitleButton: ImageButton
    private val nextButton: ImageButton
    private val previousButton: ImageButton
    private val fullscreenButton: ImageButton
    private val backButton: ImageButton
    private val titleText: TextView

    private val hideHandler = Handler(Looper.getMainLooper())
    private val fadeOutDelay = 4000L // 4 seconds
    private val autoHideRunnable = Runnable { hide() }

    private var controlsVisible = true
    private var isLocked = false
    private var isPlaying = false
    private var listener: PlayerControlsListener? = null

    init {
        // Inflate the layout
        val inflater = LayoutInflater.from(context)
        rootView = inflater.inflate(R.layout.overlay_player_controls, this, true) as ConstraintLayout

        // Initialize views
        playPauseButton = findViewById(R.id.btn_play_pause)
        seekBar = findViewById(R.id.seek_bar)
        timeElapsedText = findViewById(R.id.text_time_elapsed)
        timeDurationText = findViewById(R.id.text_time_duration)
        settingsButton = findViewById(R.id.btn_settings)
        lockButton = findViewById(R.id.btn_lock)
        aspectRatioButton = findViewById(R.id.btn_aspect_ratio)
        audioTrackButton = findViewById(R.id.btn_audio_track)
        subtitleButton = findViewById(R.id.btn_subtitle)
        nextButton = findViewById(R.id.btn_next)
        previousButton = findViewById(R.id.btn_previous)
        fullscreenButton = findViewById(R.id.btn_fullscreen)
        backButton = findViewById(R.id.btn_back)
        titleText = findViewById(R.id.text_title)

        setupListeners()
    }

    private fun setupListeners() {
        playPauseButton.setOnClickListener {
            isPlaying = !isPlaying
            updatePlayPauseButton()
            listener?.onPlayPauseClicked(isPlaying)
            resetHideTimer()
        }

        settingsButton.setOnClickListener {
            listener?.onSettingsClicked()
            resetHideTimer()
        }

        lockButton.setOnClickListener {
            isLocked = !isLocked
            updateLockState()
            listener?.onLockStateChanged(isLocked)
            resetHideTimer()
        }

        aspectRatioButton.setOnClickListener {
            listener?.onAspectRatioClicked()
            resetHideTimer()
        }

        audioTrackButton.setOnClickListener {
            listener?.onAudioTrackClicked()
            resetHideTimer()
        }

        subtitleButton.setOnClickListener {
            listener?.onSubtitleClicked()
            resetHideTimer()
        }

        nextButton.setOnClickListener {
            listener?.onNextClicked()
            resetHideTimer()
        }

        previousButton.setOnClickListener {
            listener?.onPreviousClicked()
            resetHideTimer()
        }

        fullscreenButton.setOnClickListener {
            listener?.onFullscreenClicked()
            resetHideTimer()
        }

        backButton.setOnClickListener {
            listener?.onBackClicked()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateTimeElapsed(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                cancelHideTimer()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                listener?.onSeekTo(seekBar.progress.toLong())
                resetHideTimer()
            }
        })
    }

    fun show() {
        if (controlsVisible) return

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 300
            fillAfter = true
        }

        rootView.startAnimation(fadeIn)
        controlsVisible = true
        resetHideTimer()
    }

    fun hide() {
        if (!controlsVisible || isLocked) return

        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 300
            fillAfter = true
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    rootView.visibility = View.INVISIBLE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }

        rootView.startAnimation(fadeOut)
        controlsVisible = false
    }

    fun resetHideTimer() {
        if (isLocked) return

        cancelHideTimer()
        hideHandler.postDelayed(autoHideRunnable, fadeOutDelay)
        rootView.visibility = View.VISIBLE
    }

    fun cancelHideTimer() {
        hideHandler.removeCallbacks(autoHideRunnable)
    }

    fun setPlaybackState(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        updatePlayPauseButton()
    }

    fun setTitle(title: String) {
        titleText.text = title
    }

    fun setProgress(currentMs: Long, durationMs: Long) {
        seekBar.max = durationMs.toInt()
        seekBar.progress = currentMs.toInt()
        updateTimeElapsed(currentMs)
        updateTimeDuration(durationMs)
    }

    fun setControlsListener(listener: PlayerControlsListener) {
        this.listener = listener
    }

    fun showSubtitleIndicator(enabled: Boolean) {
        subtitleButton.colorFilter = if (enabled) {
            PorterDuffColorFilter(Color.parseColor("#42A5F5"), PorterDuff.Mode.SRC_IN)
        } else {
            null
        }
    }

    private fun updatePlayPauseButton() {
        playPauseButton.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    private fun updateLockState() {
        // Toggle button appearance
        lockButton.setImageResource(
            if (isLocked) android.R.drawable.ic_lock_lock else android.R.drawable.ic_menu_more
        )

        // Update visibility of other controls when locked
        val controlsVisibility = if (isLocked) View.GONE else View.VISIBLE
        playPauseButton.visibility = controlsVisibility
        seekBar.visibility = controlsVisibility
        settingsButton.visibility = controlsVisibility
        aspectRatioButton.visibility = controlsVisibility
        audioTrackButton.visibility = controlsVisibility
        subtitleButton.visibility = controlsVisibility
        nextButton.visibility = controlsVisibility
        previousButton.visibility = controlsVisibility
        fullscreenButton.visibility = controlsVisibility
    }

    private fun updateTimeElapsed(timeMs: Long) {
        timeElapsedText.text = formatTime(timeMs)
    }

    private fun updateTimeDuration(timeMs: Long) {
        timeDurationText.text = formatTime(timeMs)
    }

    private fun formatTime(timeMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    interface PlayerControlsListener {
        fun onPlayPauseClicked(isPlaying: Boolean)
        fun onSeekTo(positionMs: Long)
        fun onSettingsClicked()
        fun onLockStateChanged(isLocked: Boolean)
        fun onAspectRatioClicked()
        fun onAudioTrackClicked()
        fun onSubtitleClicked()
        fun onNextClicked()
        fun onPreviousClicked()
        fun onFullscreenClicked()
        fun onBackClicked()
    }
}
