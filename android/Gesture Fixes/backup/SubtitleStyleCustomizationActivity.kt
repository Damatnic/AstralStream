package com.astralplayer.nextplayer.feature.settings

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.astralplayer.nextplayer.R
import com.astralplayer.nextplayer.feature.subtitles.AdvancedSubtitleManager.SubtitleStyle

/**
 * Activity for customizing subtitle appearance including font, size,
 * color, background, position, and other style options.
 */
class SubtitleStyleCustomizationActivity : AppCompatActivity() {

    private lateinit var previewText: TextView
    private lateinit var fontSizeSeekBar: SeekBar
    private lateinit var fontSizeText: TextView
    private lateinit var textColorButton: Button
    private lateinit var outlineColorButton: Button
    private lateinit var backgroundColorButton: Button
    private lateinit var fontFamilySpinner: Spinner
    private lateinit var boldCheckBox: CheckBox
    private lateinit var italicCheckBox: CheckBox
    private lateinit var positionRadioGroup: RadioGroup
    private lateinit var marginSeekBar: SeekBar
    private lateinit var marginText: TextView

    private val currentStyle = SubtitleStyle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subtitle_style_customization)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Subtitle Style"

        // Initialize views
        initializeViews()

        // Load saved settings
        loadSavedStyle()

        // Setup listeners
        setupListeners()

        // Update preview
        updatePreview()
    }

    private fun initializeViews() {
        previewText = findViewById(R.id.text_subtitle_preview)
        fontSizeSeekBar = findViewById(R.id.seekbar_font_size)
        fontSizeText = findViewById(R.id.text_font_size)
        textColorButton = findViewById(R.id.button_text_color)
        outlineColorButton = findViewById(R.id.button_outline_color)
        backgroundColorButton = findViewById(R.id.button_background_color)
        fontFamilySpinner = findViewById(R.id.spinner_font_family)
        boldCheckBox = findViewById(R.id.checkbox_bold)
        italicCheckBox = findViewById(R.id.checkbox_italic)
        positionRadioGroup = findViewById(R.id.radio_group_position)
        marginSeekBar = findViewById(R.id.seekbar_margin)
        marginText = findViewById(R.id.text_margin)

        // Setup font family spinner
        val fontFamilies = arrayOf("Sans Serif", "Serif", "Monospace")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fontFamilies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontFamilySpinner.adapter = adapter

        // Configure seekbars
        fontSizeSeekBar.max = 40  // Max 40sp
        marginSeekBar.max = 100   // Max 100dp
    }

    private fun loadSavedStyle() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load font size
        currentStyle.textSize = prefs.getFloat(KEY_TEXT_SIZE, 16f)
        fontSizeSeekBar.progress = currentStyle.textSize.toInt()
        fontSizeText.text = "${currentStyle.textSize.toInt()}sp"

        // Load colors
        currentStyle.textColor = prefs.getInt(KEY_TEXT_COLOR, Color.WHITE)
        currentStyle.outlineColor = prefs.getInt(KEY_OUTLINE_COLOR, Color.BLACK)
        currentStyle.backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, Color.TRANSPARENT)

        // Update color button backgrounds
        textColorButton.setBackgroundColor(currentStyle.textColor)
        outlineColorButton.setBackgroundColor(currentStyle.outlineColor)
        backgroundColorButton.setBackgroundColor(currentStyle.backgroundColor)

        // Load font family
        currentStyle.fontFamily = prefs.getString(KEY_FONT_FAMILY, "sans-serif") ?: "sans-serif"
        fontFamilySpinner.setSelection(getFontFamilyIndex(currentStyle.fontFamily))

        // Load style
        currentStyle.bold = prefs.getBoolean(KEY_BOLD, false)
        currentStyle.italic = prefs.getBoolean(KEY_ITALIC, false)
        boldCheckBox.isChecked = currentStyle.bold
        italicCheckBox.isChecked = currentStyle.italic

        // Load position
        currentStyle.position = prefs.getInt(KEY_POSITION, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        when (currentStyle.position) {
            Gravity.TOP or Gravity.CENTER_HORIZONTAL -> positionRadioGroup.check(R.id.radio_top)
            Gravity.CENTER -> positionRadioGroup.check(R.id.radio_center)
            else -> positionRadioGroup.check(R.id.radio_bottom)
        }

        // Load margin
        currentStyle.verticalMargin = prefs.getInt(KEY_MARGIN, 50)
        marginSeekBar.progress = currentStyle.verticalMargin
        marginText.text = "${currentStyle.verticalMargin}dp"
    }

    private fun setupListeners() {
        // Font size listener
        fontSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val size = if (progress < 8) 8 else progress  // Minimum 8sp
                fontSizeText.text = "${size}sp"
                currentStyle.textSize = size.toFloat()
                updatePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // Color buttons
        textColorButton.setOnClickListener { showColorPickerDialog(COLOR_TYPE_TEXT) }
        outlineColorButton.setOnClickListener { showColorPickerDialog(COLOR_TYPE_OUTLINE) }
        backgroundColorButton.setOnClickListener { showColorPickerDialog(COLOR_TYPE_BACKGROUND) }

        // Font family spinner
        fontFamilySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentStyle.fontFamily = when (position) {
                    0 -> "sans-serif"
                    1 -> "serif"
                    2 -> "monospace"
                    else -> "sans-serif"
                }
                updatePreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Style checkboxes
        boldCheckBox.setOnCheckedChangeListener { _, isChecked ->
            currentStyle.bold = isChecked
            updatePreview()
        }

        italicCheckBox.setOnCheckedChangeListener { _, isChecked ->
            currentStyle.italic = isChecked
            updatePreview()
        }

        // Position radio group
        positionRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentStyle.position = when (checkedId) {
                R.id.radio_top -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                R.id.radio_center -> Gravity.CENTER
                else -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            updatePreview()
        }

        // Margin seekbar
        marginSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                marginText.text = "${progress}dp"
                currentStyle.verticalMargin = progress
                updatePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun showColorPickerDialog(colorType: Int) {
        // In a real implementation, this would use a color picker dialog
        // For simplicity, we'll just cycle through some predefined colors
        val colors = arrayOf(
            Color.WHITE, Color.YELLOW, Color.GREEN, Color.CYAN,
            Color.BLUE, Color.MAGENTA, Color.RED, Color.BLACK
        )

        val currentColor = when (colorType) {
            COLOR_TYPE_TEXT -> currentStyle.textColor
            COLOR_TYPE_OUTLINE -> currentStyle.outlineColor
            COLOR_TYPE_BACKGROUND -> currentStyle.backgroundColor
            else -> Color.WHITE
        }

        // Find current color index
        var colorIndex = colors.indexOf(currentColor)
        if (colorIndex == -1) colorIndex = 0

        // Get next color
        colorIndex = (colorIndex + 1) % colors.size
        val newColor = colors[colorIndex]

        // Apply the new color
        when (colorType) {
            COLOR_TYPE_TEXT -> {
                currentStyle.textColor = newColor
                textColorButton.setBackgroundColor(newColor)
            }
            COLOR_TYPE_OUTLINE -> {
                currentStyle.outlineColor = newColor
                outlineColorButton.setBackgroundColor(newColor)
            }
            COLOR_TYPE_BACKGROUND -> {
                currentStyle.backgroundColor = newColor
                backgroundColorButton.setBackgroundColor(newColor)
            }
        }

        updatePreview()
    }

    private fun updatePreview() {
        // Apply current style to preview text
        previewText.setTextSize(currentStyle.textSize)
        previewText.setTextColor(currentStyle.textColor)
        previewText.setShadowLayer(
            currentStyle.outlineWidth,
            1.5f,
            1.5f,
            currentStyle.outlineColor
        )
        previewText.setBackgroundColor(currentStyle.backgroundColor)

        // Set typeface
        val typeface = when (currentStyle.fontFamily) {
            "sans-serif" -> Typeface.SANS_SERIF
            "serif" -> Typeface.SERIF
            "monospace" -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT
        }

        var style = Typeface.NORMAL
        if (currentStyle.bold) style = style or Typeface.BOLD
        if (currentStyle.italic) style = style or Typeface.ITALIC

        previewText.typeface = Typeface.create(typeface, style)

        // Set gravity (position)
        previewText.gravity = currentStyle.position

        // In a real implementation, you would also adjust vertical margin
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            saveSettings()
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(KEY_TEXT_SIZE, currentStyle.textSize)
            putInt(KEY_TEXT_COLOR, currentStyle.textColor)
            putInt(KEY_OUTLINE_COLOR, currentStyle.outlineColor)
            putInt(KEY_BACKGROUND_COLOR, currentStyle.backgroundColor)
            putString(KEY_FONT_FAMILY, currentStyle.fontFamily)
            putBoolean(KEY_BOLD, currentStyle.bold)
            putBoolean(KEY_ITALIC, currentStyle.italic)
            putInt(KEY_POSITION, currentStyle.position)
            putInt(KEY_MARGIN, currentStyle.verticalMargin)
        }.apply()
    }

    override fun onBackPressed() {
        saveSettings()
        super.onBackPressed()
    }

    private fun getFontFamilyIndex(fontFamily: String): Int {
        return when (fontFamily) {
            "sans-serif" -> 0
            "serif" -> 1
            "monospace" -> 2
            else -> 0
        }
    }

    companion object {
        private const val PREFS_NAME = "subtitle_style_prefs"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_TEXT_COLOR = "text_color"
        private const val KEY_OUTLINE_COLOR = "outline_color"
        private const val KEY_BACKGROUND_COLOR = "background_color"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_BOLD = "bold"
        private const val KEY_ITALIC = "italic"
        private const val KEY_POSITION = "position"
        private const val KEY_MARGIN = "margin"

        private const val COLOR_TYPE_TEXT = 0
        private const val COLOR_TYPE_OUTLINE = 1
        private const val COLOR_TYPE_BACKGROUND = 2
    }
}
