package com.octone.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import mn.tck.semitone.PianoView
import mn.tck.semitone.PianoEngine
import mn.tck.semitone.Util
import kotlin.jvm.JvmName

class OctonePianoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : PianoView(context, attrs) {

    companion object {
        const val PREF_SCALE = "scale"
        const val PREF_SCALE_ROOT = "scale_root"
        const val PREF_LABEL_INTERVALS = "label_intervals"
        const val PREF_ROWS_TOP_DOWN = "rows_top_down"
        const val PREF_SCALE_DEFAULT = 0
        const val PREF_SCALE_ROOT_DEFAULT = 0
        const val PREF_LABEL_INTERVALS_DEFAULT = true
        const val PREF_ROWS_TOP_DOWN_DEFAULT = true
    }

    private lateinit var preferences: SharedPreferences

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
    var paused: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    // Local Float versions for drawing
    private var whiteWidthF: Float = 0f
    private var whiteHeightF: Float = 0f
    private var blackWidthF: Float = 0f
    private var blackHeightF: Float = 0f

    private val nameBlackPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textAlign = Paint.Align.CENTER
    }
    private val whiteScalePaint = Paint()
    private val blackScalePaint = Paint()
    private val whiteScalePaintRoot = Paint()
    private val blackScalePaintRoot = Paint()
    private val intervalWhitePaint = Paint()
    private val intervalBlackPaint = Paint()
    private var scaleColors: Array<Paint> = Array(12) { whitePaint }

    private val intervalNames: Array<String>
    private val noteNames: Array<String>

    private val whiteKeyGradient = Paint()
    private val blackKeyGradient = Paint()
    private val pressedKeyGradient = Paint()

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scaleBasePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scaleRootPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornerRadius = 8f

    init {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        intervalNames = resources.getStringArray(R.array.intervalNames)
        noteNames = resources.getStringArray(R.array.note_names)

        // Setup gradients for key textures
        whiteKeyGradient.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, whiteHeight.toFloat(),
            intArrayOf(
                ContextCompat.getColor(context, R.color.whiteKeyTop),
                ContextCompat.getColor(context, R.color.whiteKey)
            ),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )

        blackKeyGradient.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, blackHeight.toFloat(),
            intArrayOf(
                ContextCompat.getColor(context, R.color.blackKeyTop),
                ContextCompat.getColor(context, R.color.blackKey)
            ),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )

        pressedKeyGradient.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, whiteHeight.toFloat(),
            intArrayOf(
                ContextCompat.getColor(context, R.color.grey4),
                ContextCompat.getColor(context, R.color.grey3)
            ),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )

        // get orientation based configuration values for keyboard dimensions
        val orientation = getOrientationString()
        rows = preferences.getInt("piano_rows$orientation", 2)
        keys = preferences.getInt("piano_keys$orientation", 7)
        pitch = preferences.getInt("piano_pitch$orientation", 28)

        // Setup paints
        shadowPaint.color = ContextCompat.getColor(context, R.color.key_shadow)
        highlightPaint.color = ContextCompat.getColor(context, R.color.key_highlight)
        
        setupColors()

        // initialize from preferences
        setScale(preferences.getInt(PREF_SCALE, PREF_SCALE_DEFAULT), preferences.getInt(PREF_SCALE_ROOT, PREF_SCALE_ROOT_DEFAULT))
        sustain = preferences.getBoolean("sustain", true)
        labelnotes = preferences.getBoolean("labelnotes", true)
        labelc = preferences.getBoolean("labelc", true)
        labelIntervals = preferences.getBoolean(PREF_LABEL_INTERVALS, PREF_LABEL_INTERVALS_DEFAULT)
        rowsTopDown = preferences.getBoolean(PREF_ROWS_TOP_DOWN, PREF_ROWS_TOP_DOWN_DEFAULT)
        concert_a = try {
            preferences.getString("concert_a", "440")?.toInt() ?: 440
        } catch (e: NumberFormatException) {
            440
        }
    }

    private fun getOrientationString(): String {
        return if (rowsTopDown) "_top_down" else "_bottom_up"
    }

    private fun setupColors() {
        whitePaint.color = ContextCompat.getColor(context, R.color.whiteKey)
        grey1Paint.color = ContextCompat.getColor(context, R.color.blackKey)
        grey3Paint.color = ContextCompat.getColor(context, R.color.grey3)
        grey4Paint.color = ContextCompat.getColor(context, R.color.grey4)
        blackPaint.color = ContextCompat.getColor(context, R.color.blackKey)

        whiteScalePaint.color = ContextCompat.getColor(context, R.color.whiteScale)
        blackScalePaint.color = ContextCompat.getColor(context, R.color.blackScale)
        whiteScalePaintRoot.color = ContextCompat.getColor(context, R.color.whiteScaleRoot)
        blackScalePaintRoot.color = ContextCompat.getColor(context, R.color.blackScaleRoot)
        intervalWhitePaint.color = ContextCompat.getColor(context, R.color.intervalWhiteLabel)
        intervalBlackPaint.color = ContextCompat.getColor(context, R.color.intervalBlackLabel)
    }

    private fun getScaleColors(scale: Int): Pair<Int, Int> {
        return when (scale) {
            1 -> Pair(R.color.major_scale_base, R.color.major_scale_root)
            2 -> Pair(R.color.minor_scale_base, R.color.minor_scale_root)
            3 -> Pair(R.color.harmonic_minor_base, R.color.harmonic_minor_root)
            4 -> Pair(R.color.melodic_minor_base, R.color.melodic_minor_root)
            5 -> Pair(R.color.pent_major_base, R.color.pent_major_root)
            6 -> Pair(R.color.pent_minor_base, R.color.pent_minor_root)
            7 -> Pair(R.color.ionian_base, R.color.ionian_root)
            8 -> Pair(R.color.dorian_base, R.color.dorian_root)
            9 -> Pair(R.color.phrygian_base, R.color.phrygian_root)
            10 -> Pair(R.color.lydian_base, R.color.lydian_root)
            11 -> Pair(R.color.mixolydian_base, R.color.mixolydian_root)
            12 -> Pair(R.color.aeolian_base, R.color.aeolian_root)
            13 -> Pair(R.color.locrian_base, R.color.locrian_root)
            else -> Pair(R.color.whiteKey, R.color.whiteKey)
        }
    }

    fun setScale(newScale: Int, newRoot: Int) {
        scale = newScale
        rootNote = newRoot
        if (scale > 0) {
            val ta = resources.obtainTypedArray(R.array.scales)
            val scaleArray = resources.getIntArray(ta.getResourceId(scale, 0))
            val (baseColor, rootColor) = getScaleColors(scale)
            
            scaleBasePaint.color = ContextCompat.getColor(context, baseColor)
            scaleRootPaint.color = ContextCompat.getColor(context, rootColor)
            
            scaleColors = Array(12) { i ->
                val realKey = (i + rootNote) % 12
                when {
                    scaleArray[i] == 0 -> if (isBlack(realKey)) blackPaint else whitePaint
                    realKey == newRoot -> scaleRootPaint
                    else -> scaleBasePaint
                }
            }
            ta.recycle()
        } else {
            scaleColors = Array(12) { whitePaint }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width
        val height = height

        whiteWidth = width / keys
        whiteHeight = height / rows
        blackWidth = whiteWidth * 2 / 3
        blackHeight = whiteHeight / 2

        // Update gradients with new dimensions
        whiteKeyGradient.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, whiteHeight.toFloat(),
            intArrayOf(
                ContextCompat.getColor(context, R.color.whiteKeyTop),
                ContextCompat.getColor(context, R.color.whiteKey)
            ),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )

        blackKeyGradient.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, blackHeight.toFloat(),
            intArrayOf(
                ContextCompat.getColor(context, R.color.blackKeyTop),
                ContextCompat.getColor(context, R.color.blackKey)
            ),
            floatArrayOf(0f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )

        nameBlackPaint.textSize = Util.maxTextSize("G0", whiteWidth * 2 / 3).toFloat()
        intervalWhitePaint.textSize = Util.maxTextSize("P1", whiteWidth / 4).toFloat()
        intervalBlackPaint.textSize = Util.maxTextSize("P1", whiteWidth / 4).toFloat()

        for (row in 0 until rows) {
            val pitchrow = getPitchRow(row)
            for (key in 0 until keys) {
                val x = whiteWidth * key
                val y = whiteHeight * pitchrow
                val p = pitches[pitchrow][key]

                // Draw white key with rounded corners and shadow
                canvas.save()
                canvas.translate(x.toFloat(), y.toFloat())
                
                // Draw key shadow
                canvas.drawRoundRect(
                    OUTLINE.toFloat(),
                    OUTLINE * 2.toFloat(),
                    (whiteWidth - OUTLINE).toFloat(),
                    (whiteHeight - OUTLINE * 2 - YPAD).toFloat(),
                    cornerRadius,
                    cornerRadius,
                    shadowPaint
                )

                // Draw white key
                keyPaint.shader = LinearGradient(
                    0f, 0f,
                    0f, whiteHeight.toFloat(),
                    intArrayOf(
                        ContextCompat.getColor(context, R.color.key_highlight),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0.1f, 1f),
                    Shader.TileMode.CLAMP
                )

                canvas.drawRoundRect(
                    OUTLINE.toFloat(),
                    OUTLINE * 2.toFloat(),
                    (whiteWidth - OUTLINE).toFloat(),
                    (whiteHeight - OUTLINE * 2 - YPAD).toFloat(),
                    cornerRadius,
                    cornerRadius,
                    if (pressed[p]) grey4Paint else scaleColors[p % 12]
                )

                // Add highlight gradient
                if (!pressed[p]) {
                    canvas.drawRoundRect(
                        OUTLINE.toFloat(),
                        OUTLINE * 2.toFloat(),
                        (whiteWidth - OUTLINE).toFloat(),
                        (whiteHeight - OUTLINE * 2 - YPAD).toFloat(),
                        cornerRadius,
                        cornerRadius,
                        keyPaint
                    )
                }

                canvas.restore()

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

                // Draw black keys with similar effects
                if (hasBlackLeft(p)) {
                    canvas.save()
                    canvas.translate(x.toFloat(), y.toFloat())
                    
                    // Shadow
                    canvas.drawRoundRect(
                        (-blackWidth / 2).toFloat(),
                        0f,
                        0f,
                        blackHeight.toFloat(),
                        cornerRadius,
                        cornerRadius,
                        shadowPaint
                    )

                    // Key
                    keyPaint.shader = LinearGradient(
                        0f, 0f,
                        0f, blackHeight.toFloat(),
                        intArrayOf(
                            ContextCompat.getColor(context, R.color.key_highlight),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0.1f, 1f),
                        Shader.TileMode.CLAMP
                    )

                    canvas.drawRoundRect(
                        (-blackWidth / 2 + OUTLINE).toFloat(),
                        OUTLINE * 2.toFloat(),
                        -OUTLINE.toFloat(),
                        (blackHeight - OUTLINE).toFloat(),
                        cornerRadius,
                        cornerRadius,
                        if (pressed[p - 1]) grey1Paint else scaleColors[(p - 1) % 12]
                    )

                    if (!pressed[p - 1]) {
                        canvas.drawRoundRect(
                            (-blackWidth / 2 + OUTLINE).toFloat(),
                            OUTLINE * 2.toFloat(),
                            -OUTLINE.toFloat(),
                            (blackHeight - OUTLINE).toFloat(),
                            cornerRadius,
                            cornerRadius,
                            keyPaint
                        )
                    }

                    canvas.restore()
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
                    canvas.save()
                    canvas.translate(x.toFloat(), y.toFloat())
                    
                    // Shadow
                    canvas.drawRoundRect(
                        (whiteWidth - blackWidth / 2).toFloat(),
                        0f,
                        whiteWidth.toFloat(),
                        blackHeight.toFloat(),
                        cornerRadius,
                        cornerRadius,
                        shadowPaint
                    )

                    // Key
                    keyPaint.shader = LinearGradient(
                        0f, 0f,
                        0f, blackHeight.toFloat(),
                        intArrayOf(
                            ContextCompat.getColor(context, R.color.key_highlight),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0.1f, 1f),
                        Shader.TileMode.CLAMP
                    )

                    canvas.drawRoundRect(
                        (whiteWidth - blackWidth / 2 + OUTLINE).toFloat(),
                        OUTLINE * 2.toFloat(),
                        whiteWidth.toFloat(),
                        (blackHeight - OUTLINE).toFloat(),
                        cornerRadius,
                        cornerRadius,
                        if (pressed[p + 1]) grey1Paint else scaleColors[(p + 1) % 12]
                    )

                    if (!pressed[p + 1]) {
                        canvas.drawRoundRect(
                            (whiteWidth - blackWidth / 2 + OUTLINE).toFloat(),
                            OUTLINE * 2.toFloat(),
                            whiteWidth.toFloat(),
                            (blackHeight - OUTLINE).toFloat(),
                            cornerRadius,
                            cornerRadius,
                            keyPaint
                        )
                    }

                    canvas.restore()
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

    fun addRow(@Suppress("UNUSED_PARAMETER") v: View) {
        rows++
        updateParams(true)
    }

    fun removeRow(@Suppress("UNUSED_PARAMETER") v: View) {
        rows--
        updateParams(true)
    }

    fun addKey(@Suppress("UNUSED_PARAMETER") v: View) {
        keys++
        updateParams(true)
    }

    fun removeKey(@Suppress("UNUSED_PARAMETER") v: View) {
        keys--
        updateParams(true)
    }

    fun toggleLabelNotes() {
        labelnotes = !labelnotes
        preferences.edit().putBoolean("labelnotes", labelnotes).apply()
        invalidate()
    }

    fun toggleLabelC() {
        labelc = !labelc
        preferences.edit().putBoolean("labelc", labelc).apply()
        invalidate()
    }

    fun toggleLabelIntervals() {
        labelIntervals = !labelIntervals
        preferences.edit().putBoolean(PREF_LABEL_INTERVALS, labelIntervals).apply()
        invalidate()
    }

    fun toggleRowsTopDown() {
        rowsTopDown = !rowsTopDown
        preferences.edit().putBoolean(PREF_ROWS_TOP_DOWN, rowsTopDown).apply()
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

    fun octaveLeft(@Suppress("UNUSED_PARAMETER") v: View) {
        pitch = (pitch - keys).coerceAtLeast(0)
        updateParams(true)
    }

    fun pitchLeft(@Suppress("UNUSED_PARAMETER") v: View) {
        pitch--
        updateParams(true)
    }

    fun pitchRight(@Suppress("UNUSED_PARAMETER") v: View) {
        pitch++
        updateParams(true)
    }

    fun octaveRight(@Suppress("UNUSED_PARAMETER") v: View) {
        pitch = (pitch + keys).coerceAtMost(128 - rows * keys)
        updateParams(true)
    }

    fun reset(@Suppress("UNUSED_PARAMETER") v: View) {
        val orientation = getOrientationString()
        rows = preferences.getInt("piano_rows$orientation", 2)
        keys = preferences.getInt("piano_keys$orientation", 7)
        pitch = preferences.getInt("piano_pitch$orientation", 28)
        updateParams(true)
    }

    override fun updateParams(updatePitches: Boolean) {
        if (!::preferences.isInitialized) {
            super.updateParams(updatePitches)
            return
        }
        
        // Ensure valid bounds for piano parameters
        rows = rows.coerceIn(1, 5)
        keys = keys.coerceIn(7, 21)
        pitch = pitch.coerceIn(0, 128 - rows * keys)
        
        val orientation = getOrientationString()
        preferences.edit()
            .putInt("piano_rows$orientation", rows)
            .putInt("piano_keys$orientation", keys)
            .putInt("piano_pitch$orientation", pitch)
            .apply()
        super.updateParams(updatePitches)
    }

    private fun getPitchRow(row: Int): Int {
        return if (rowsTopDown) row else rows - 1 - row
    }

    private fun isBlack(p: Int): Boolean = p % 12 in listOf(1, 3, 6, 8, 10)
} 