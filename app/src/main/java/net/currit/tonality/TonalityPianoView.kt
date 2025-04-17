package net.currit.tonality

import android.content.Context
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import mn.tck.semitone.PianoView
import mn.tck.semitone.Util
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import kotlin.jvm.JvmName
import net.currit.tonality.BuildConfig

class TonalityPianoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : PianoView(context, attrs) {

    enum class THEME {
        STANDARD,
        LESS_COLORS
    }

    companion object {
        const val PREF_SCALE = "scale"
        const val PREF_SCALE_ROOT = "scale_root"
        const val PREF_LABEL_INTERVALS = "label_intervals"
        const val PREF_ROWS_TOP_DOWN = "rows_top_down"
        const val PREF_THEME = "keyboard_theme"
        const val PREF_SCALE_DEFAULT = 0
        const val PREF_SCALE_ROOT_DEFAULT = 0
        const val PREF_LABEL_INTERVALS_DEFAULT = true
        const val PREF_ROWS_TOP_DOWN_DEFAULT = true // true = lower notes on top, false = lower notes on bottom
        val PREF_THEME_DEFAULT = THEME.STANDARD
    }

    @get:JvmName("getScaleValue")
    protected var scale: Int = 0
        private set
    @get:JvmName("getRootNoteValue")
    protected var rootNote: Int = 0
        private set
    @get:JvmName("getLabelIntervalsValue")
    protected var labelIntervals: Boolean = false
        private set
    @get:JvmName("getRowsTopDownValue")
    protected var rowsTopDown: Boolean = false
        private set
    var theme: THEME = THEME.STANDARD
        private set
    var paused: Boolean = false
        private set

    // Local Float versions for drawing
    private var whiteWidthF: Float = 0f
    private var whiteHeightF: Float = 0f
    private var blackWidthF: Float = 0f
    private var blackHeightF: Float = 0f

    private val nameBlackPaint = Paint()
    private val whiteScalePaint = Paint()
    private val blackScalePaint = Paint()
    private val whiteScalePaintRoot = Paint()
    private val blackScalePaintRoot = Paint()
    private val intervalWhitePaint = Paint()
    private val intervalBlackPaint = Paint()
    private var scaleColors: Array<Paint>? = null

    private lateinit var intervalNames: Array<String>
    private lateinit var noteNames: Array<String>

    init {
        // get orientation based configuration values for keyboard dimensions
        val orientation = getOrientationString()
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        rows = sp.getInt("piano_rows$orientation", 2)
        keys = sp.getInt("piano_keys$orientation", 7)
        pitch = sp.getInt("piano_pitch$orientation", 28)

        // setup colors
        theme = THEME.valueOf(sp.getString(PREF_THEME, PREF_THEME_DEFAULT.name)!!)
        setupColors()

        // initialize from preferences
        setScale(sp.getInt(PREF_SCALE, PREF_SCALE_DEFAULT), sp.getInt(PREF_SCALE_ROOT, PREF_SCALE_ROOT_DEFAULT))
        sustain = sp.getBoolean("sustain", false)
        labelnotes = sp.getBoolean("labelnotes", true)
        labelc = sp.getBoolean("labelc", true)
        labelIntervals = sp.getBoolean(PREF_LABEL_INTERVALS, PREF_LABEL_INTERVALS_DEFAULT)
        rowsTopDown = sp.getBoolean(PREF_ROWS_TOP_DOWN, PREF_ROWS_TOP_DOWN_DEFAULT)
        concert_a = 440 // set a default to avoid DIV0 errors
        try {
            concert_a = sp.getString("concert_a", "440")?.toInt() ?: 440
        } catch (e: NumberFormatException) {
            concert_a = 440
        }

        // get interval and note names for labelling
        intervalNames = resources.getStringArray(R.array.intervalNames)
        noteNames = resources.getStringArray(R.array.noteNames)

        // finally set internal arrays and stuff according to configuration values
        updateParams(false)
    }

    protected fun setupColors() {
        if (theme == THEME.LESS_COLORS) {
            whitePaint.color = ContextCompat.getColor(context, R.color.yellowWhiteKey)
            blackPaint.color = ContextCompat.getColor(context, R.color.yellowBlackKey)
            whiteScalePaint.color = ContextCompat.getColor(context, R.color.yellowWhiteScale)
            blackScalePaint.color = ContextCompat.getColor(context, R.color.yellowBlackScale)
            whiteScalePaintRoot.color = ContextCompat.getColor(context, R.color.yellowWhiteScaleRoot)
            blackScalePaintRoot.color = ContextCompat.getColor(context, R.color.yellowBlackScaleRoot)
            intervalWhitePaint.color = ContextCompat.getColor(context, R.color.yellowIntervalWhiteLabel)
            intervalBlackPaint.color = ContextCompat.getColor(context, R.color.yellowIntervalBlackLabel)
        } else {
            whitePaint.color = ContextCompat.getColor(context, R.color.whiteKey)
            blackPaint.color = ContextCompat.getColor(context, R.color.blackKey)
            whiteScalePaint.color = ContextCompat.getColor(context, R.color.whiteScale)
            blackScalePaint.color = ContextCompat.getColor(context, R.color.blackScale)
            whiteScalePaintRoot.color = ContextCompat.getColor(context, R.color.whiteScaleRoot)
            blackScalePaintRoot.color = ContextCompat.getColor(context, R.color.blackScaleRoot)
            intervalWhitePaint.color = ContextCompat.getColor(context, R.color.intervalWhiteLabel)
            intervalBlackPaint.color = ContextCompat.getColor(context, R.color.intervalBlackLabel)
        }

        // remaining colors
        nameBlackPaint.color = ContextCompat.getColor(context, R.color.black)
        nameBlackPaint.textAlign = Paint.Align.CENTER
    }

    private fun getOrientationString(): String {
        return if (context.resources.configuration.orientation == ORIENTATION_PORTRAIT)
            "_portrait"
        else
            "_landscape"
    }

    /**
     * Replaces updateParams from PianoView to properly handle screen orientation
     *
     * @param inval if true, update preferences for rows, keys and pitch
     */
    override fun updateParams(inval: Boolean) {
        pitches = Array(rows) { IntArray(keys) }

        var p = 0
        for (i in 0 until pitch) p += if (hasBlackRight(p)) 2 else 1
        for (row in 0 until rows) {
            val pitchrow = getPitchRow(row)
            for (key in 0 until keys) {
                pitches[pitchrow][key] = p
                p += if (hasBlackRight(p)) 2 else 1
            }
        }

        if (inval) {
            val orientation = getOrientationString()

            val editor = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putInt("piano_rows$orientation", rows)
            editor.putInt("piano_keys$orientation", keys)
            editor.putInt("piano_pitch$orientation", pitch)
            editor.apply()

            invalidate()
        }
    }

    fun setRoot(newRoot: Int) {
        setScale(scale, newRoot)
    }

    fun setScale(newScale: Int) {
        setScale(newScale, rootNote)
    }

    fun setScale(newScale: Int, newRoot: Int) {
        scale = newScale
        rootNote = newRoot
        if (scale > 0) {
            // get scale array from resources and adjust colors used for drawing the keyboard accordingly
            val ta = resources.obtainTypedArray(R.array.scales)
            val scaleArray = resources.getIntArray(ta.getResourceId(scale, 0))
            scaleColors = Array(12) { i ->
                val realKey = (i + rootNote) % 12
                when {
                    scaleArray[i] == 0 -> if (isBlack(realKey)) blackPaint else whitePaint
                    realKey == newRoot -> if (isBlack(realKey)) blackScalePaintRoot else whiteScalePaintRoot
                    else -> if (isBlack(realKey)) blackScalePaint else whiteScalePaint
                }
            }
            ta.recycle()
        } else {
            // set default colors if no scale is selected
            scaleColors = arrayOf(
                whitePaint, blackPaint, whitePaint, blackPaint, whitePaint, whitePaint,
                blackPaint, whitePaint, blackPaint, whitePaint, blackPaint, whitePaint
            )
        }

        // store in preferences
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putInt(PREF_SCALE, scale)
        editor.putInt(PREF_SCALE_ROOT, rootNote)
        editor.apply()

        invalidate()
    }

    /**
     * Override the original pianoview onDraw to be able to highlight a scale
     *
     * @param canvas Canvas to draw the keyboard(s) on
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width
        val height = height

        whiteWidth = width / keys
        whiteHeight = height / rows
        blackWidth = whiteWidth * 2 / 3
        blackHeight = whiteHeight / 2
        nameBlackPaint.textSize = Util.maxTextSize("G0", whiteWidth * 2 / 3).toFloat()
        intervalWhitePaint.textSize = Util.maxTextSize("P1", whiteWidth / 4).toFloat()
        intervalBlackPaint.textSize = Util.maxTextSize("P1", whiteWidth / 4).toFloat()

        for (row in 0 until rows) {
            val pitchrow = getPitchRow(row)
            for (key in 0 until keys) {
                val x = whiteWidth * key
                val y = whiteHeight * pitchrow
                val p = pitches[pitchrow][key]

                // key frame
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + whiteWidth).toFloat(), (y + whiteHeight - YPAD).toFloat(), grey3Paint)
                // white key
                canvas.drawRect(
                    (x + OUTLINE).toFloat(),
                    (y + OUTLINE * 2).toFloat(),
                    (x + whiteWidth - OUTLINE).toFloat(),
                    (y + whiteHeight - OUTLINE * 2 - YPAD).toFloat(),
                    if (pressed[p]) grey4Paint else scaleColors!![p % 12]
                )

                if (labelnotes && (!labelc || p % 12 == 0)) {
                    canvas.drawText(
                        "${noteNames[p % 12]}${p / 12 - 1}",
                        (x + whiteWidth / 2).toFloat(),
                        (y + whiteHeight * 4f / 5f).toFloat(),
                        nameBlackPaint
                    )
                }

                if (labelIntervals) {
                    canvas.drawText(
                        intervalNames[(p - rootNote) % 12],
                        (x + whiteWidth / 2).toFloat(),
                        (y + whiteHeight - 2f * YPAD.toFloat()).toFloat(),
                        intervalWhitePaint
                    )
                }

                if (hasBlackLeft(p)) {
                    // key frame
                    canvas.drawRect(
                        (x - blackWidth / 2).toFloat(),
                        y.toFloat(),
                        x.toFloat(),
                        (y + blackHeight).toFloat(),
                        grey3Paint
                    )
                    // black key
                    canvas.drawRect(
                        (x - blackWidth / 2 + OUTLINE).toFloat(),
                        (y + OUTLINE * 2).toFloat(),
                        (x - OUTLINE).toFloat(),
                        (y + blackHeight - OUTLINE).toFloat(),
                        if (pressed[p - 1]) grey1Paint else scaleColors!![(p - 1) % 12]
                    )
                    if (labelIntervals) {
                        canvas.drawText(
                            intervalNames[(p - rootNote - 1) % 12],
                            (x - blackWidth / 4).toFloat(),
                            (y + blackHeight - YPAD).toFloat(),
                            intervalBlackPaint
                        )
                    }
                }

                if (hasBlackRight(p)) {
                    // key frame
                    canvas.drawRect(
                        (x + whiteWidth - blackWidth / 2).toFloat(),
                        y.toFloat(),
                        (x + whiteWidth).toFloat(),
                        (y + blackHeight).toFloat(),
                        grey3Paint
                    )
                    // black key
                    canvas.drawRect(
                        (x + whiteWidth - blackWidth / 2 + OUTLINE).toFloat(),
                        (y + OUTLINE * 2).toFloat(),
                        (x + whiteWidth - OUTLINE).toFloat(),
                        (y + blackHeight - OUTLINE).toFloat(),
                        if (pressed[p + 1]) grey1Paint else scaleColors!![(p + 1) % 12]
                    )
                    if (labelIntervals) {
                        canvas.drawText(
                            intervalNames[(p - rootNote + 1) % 12],
                            (x + whiteWidth - blackWidth / 4).toFloat(),
                            (y + blackHeight - YPAD).toFloat(),
                            intervalBlackPaint
                        )
                    }
                }
            }
        }
    }

    fun addRow(v: View) {
        rows++
        updateParams(true)
    }

    fun removeRow(v: View) {
        rows--
        updateParams(true)
    }

    fun addKey(v: View) {
        keys++
        updateParams(true)
    }

    fun removeKey(v: View) {
        keys--
        updateParams(true)
    }

    fun octaveLeft(v: View) {
        pitch -= 12
        updateParams(true)
    }

    fun pitchLeft(v: View) {
        pitch--
        updateParams(true)
    }

    fun pitchRight(v: View) {
        pitch++
        updateParams(true)
    }

    fun octaveRight(v: View) {
        pitch += 12
        updateParams(true)
    }

    fun resetPiano(v: View) {
        rows = 2
        keys = 7
        pitch = 28
        updateParams(true)
    }

    fun toggleLabelNotes() {
        labelnotes = !labelnotes
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putBoolean("labelnotes", labelnotes)
        editor.apply()
        invalidate()
    }

    fun toggleLabelC() {
        labelc = !labelc
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putBoolean("labelc", labelc)
        editor.apply()
        invalidate()
    }

    fun toggleLabelIntervals() {
        labelIntervals = !labelIntervals
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putBoolean(PREF_LABEL_INTERVALS, labelIntervals)
        editor.apply()
        invalidate()
    }

    fun toggleRowsTopDown() {
        rowsTopDown = !rowsTopDown
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putBoolean(PREF_ROWS_TOP_DOWN, rowsTopDown)
        editor.apply()
        invalidate()
    }

    fun toggleTheme() {
        theme = if (theme == THEME.STANDARD) THEME.LESS_COLORS else THEME.STANDARD
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putString(PREF_THEME, theme.name)
        editor.apply()
        setupColors()
        invalidate()
    }

    @JvmName("getScale")
    fun getScale(): Int = scale

    @JvmName("getRootNote")
    fun getRootNote(): Int = rootNote

    @JvmName("getLabelIntervals")
    fun isLabelIntervals(): Boolean = labelIntervals

    @JvmName("getRowsTopDown")
    fun getRowsTopDown(): Boolean = rowsTopDown

    fun isLabelNotes(): Boolean = labelnotes

    fun isLabelC(): Boolean = labelc

    fun isPaused(): Boolean = paused

    private fun isBlack(p: Int): Boolean = p % 12 in listOf(1, 3, 6, 8, 10)

    private fun getPitchRow(row: Int): Int = if (rowsTopDown) row else rows - 1 - row
} 