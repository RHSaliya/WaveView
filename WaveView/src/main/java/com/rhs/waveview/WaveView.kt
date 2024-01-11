package com.rhs.waveview

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.Volatile
import kotlin.math.pow
import kotlin.math.sin


class WaveView : View {
    /**
     * Values used for drawing the wave. Initialized to default values.
     */
    var numberOfWaves = 0
    var phase = 0f
    var amplitude = 0f
    var frequency = 0f
    var phaseShift = 0f
    var density = 0f
    var primaryWaveLineWidth = 0f
    var secondaryWaveLineWidth = 0f
    var backgroundColor = 0
    var waveColor = 0
    var xAxisPositionMultiplier = 0f

    /**
     * Paint object for drawing the sine wave.
     */
    private var paint = Paint()

    /**
     * Path that defines the sine wave.
     */
    private var path = Path()

    /**
     * Mask bitmap for masking the wave.
     */
    private var mask: Bitmap? = null

    /**
     * Paint object for masking the wave.
     */
    private var maskPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    /**
     * Holds state for whether the wave is in motion
     */
    @Volatile
    private var isPlaying = AtomicBoolean(true)

    internal constructor(context: Context?, builder: Builder) : super(context) {
        setUpWithBuilder(builder)
    }

    constructor(context: Context?) : super(context) {
        setUp(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.WaveView)
        try {
            setUp(a)
        } finally {
            a.recycle()
        }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.WaveView)
        try {
            setUp(a)
        } finally {
            a.recycle()
        }
    }

    @Suppress("unused")
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.WaveView)
        try {
            setUp(a)
        } finally {
            a.recycle()
        }
    }

    /**
     * Initialize the variables to default values.
     */
    private fun setUp(typedArray: TypedArray?) {
        if (typedArray != null) {
            numberOfWaves = typedArray.getInt(
                R.styleable.WaveView_waveNumberOfWaves,
                DEFAULT_NUMBER_OF_WAVES
            )
            frequency = typedArray.getFloat(R.styleable.WaveView_waveFrequency, DEFAULT_FREQUENCY)
            amplitude = typedArray.getFloat(R.styleable.WaveView_waveAmplitude, DEFAULT_AMPLITUDE)
            phaseShift =
                typedArray.getFloat(R.styleable.WaveView_wavePhaseShift, DEFAULT_PHASE_SHIFT)
            density = typedArray.getFloat(R.styleable.WaveView_waveDensity, DEFAULT_DENSITY)
            primaryWaveLineWidth = typedArray.getFloat(
                R.styleable.WaveView_wavePrimaryLineWidth,
                DEFAULT_PRIMARY_LINE_WIDTH
            )
            secondaryWaveLineWidth = typedArray.getFloat(
                R.styleable.WaveView_waveSecondaryLineWidth,
                DEFAULT_SECONDARY_LINE_WIDTH
            )
            backgroundColor = typedArray.getColor(
                R.styleable.WaveView_waveBackgroundColor,
                DEFAULT_BACKGROUND_COLOR
            )
            val waveMaskId = typedArray.getResourceId(R.styleable.WaveView_waveMask, -1)
            if (waveMaskId != -1) {
                setMask(waveMaskId)
            }
            waveColor = typedArray.getColor(R.styleable.WaveView_waveColor, DEFAULT_WAVE_COLOR)
            xAxisPositionMultiplier = typedArray.getFloat(
                R.styleable.WaveView_waveXAxisPositionMultiplier,
                DEFAULT_X_AXIS_POSITION_MULTIPLIER
            )
            boundXAxisPositionMultiplier()
        } else {
            numberOfWaves = DEFAULT_NUMBER_OF_WAVES
            frequency = DEFAULT_FREQUENCY
            amplitude = DEFAULT_AMPLITUDE
            phaseShift = DEFAULT_PHASE_SHIFT
            density = DEFAULT_DENSITY
            primaryWaveLineWidth = DEFAULT_PRIMARY_LINE_WIDTH
            secondaryWaveLineWidth = DEFAULT_SECONDARY_LINE_WIDTH
            backgroundColor = DEFAULT_BACKGROUND_COLOR
            waveColor = DEFAULT_WAVE_COLOR
            xAxisPositionMultiplier = DEFAULT_X_AXIS_POSITION_MULTIPLIER
        }
        initPaintPath()
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private fun setUpWithBuilder(builder: Builder) {
        numberOfWaves = builder.numberOfWaves
        frequency = builder.frequency
        amplitude = builder.amplitude
        phase = builder.phase
        phaseShift = builder.phaseShift
        density = builder.density
        primaryWaveLineWidth = builder.primaryWaveLineWidth
        secondaryWaveLineWidth = builder.secondaryWaveLineWidth
        backgroundColor = builder.backgroundColor
        waveColor = builder.waveColor
        xAxisPositionMultiplier = builder.xAxisPositionMultiplier
        initPaintPath()
    }

    private fun initPaintPath() {
        paint.color = waveColor
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.isAntiAlias = true
    }

    /**
     * Method to restrict the xAxisPositionMultiplier to a value between 0 and 1 so that the wave
     * stays on screen.
     */
    private fun boundXAxisPositionMultiplier() {
        if (xAxisPositionMultiplier < 0) {
            xAxisPositionMultiplier = 0f
        } else if (xAxisPositionMultiplier > 1) {
            xAxisPositionMultiplier = 1f
        }
    }

    /**
     * Returns background color of the view.
     */
    fun getBackgroundColor(): Int {
        return backgroundColor
    }

    /**
     * Sets background color of the view.
     */
    override fun setBackgroundColor(backgroundColor: Int) {
        this.backgroundColor = backgroundColor
    }

    /**
     * Returns wave color of the view.
     */
    fun getWaveColor(): Int {
        return waveColor
    }

    /**
     * Sets wave color of the view.
     */
    fun setWaveColor(waveColor: Int) {
        this.waveColor = waveColor
        paint.color = waveColor
    }

    /**
     * Returns number of waves of the view.
     */
    var waveXAxisPositionMultiplier: Float
        get() = xAxisPositionMultiplier
        set(waveXAxisPositionMultiplier) {
            xAxisPositionMultiplier = waveXAxisPositionMultiplier
            boundXAxisPositionMultiplier()
        }

    /**
     * Returns if the wave is in motion.
     */
    fun isPlaying(): Boolean {
        return isPlaying.get()
    }

    /**
     * Play the wave.
     */
    fun play() {
        isPlaying.set(true)
    }

    /**
     * Pause the wave.
     */
    fun pause() {
        isPlaying.set(false)
    }

    /**
     * Set the mask for the wave.
     * @param bitmap The bitmap to be used as mask.
     */
    fun setMask(bitmap: Bitmap?) {
        mask = bitmap
        invalidate()
    }

    /**
     * Set the mask for the wave.
     * @param drawableId The drawable resource id to be used as mask.
     */
    fun setMask(drawableId: Int) {
        ContextCompat.getDrawable(context, drawableId)?.let { setMask(it) }
    }

    /**
     * Set the mask for the wave.
     * @param drawable The drawable to be used as mask.
     */
    fun setMask(drawable: Drawable) {
        post {
            val maskWidth = width.let {
                if (it > 0) it else drawable.intrinsicWidth
            }
            val maskHeight = height.let {
                if (it > 0) it else drawable.intrinsicHeight
            }
            mask = drawable.toBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
            invalidate()
        }
    }


    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(backgroundColor)

        // Prepare common values
        val xAxisPosition = height * xAxisPositionMultiplier
        val width = width.toFloat()
        val mid = (width / 2)
        for (i in 0 until numberOfWaves) {
            // Prepare variables for this wave
            paint.strokeWidth = if (i == 0) primaryWaveLineWidth else secondaryWaveLineWidth
            val progress = 1.0f - i.toFloat() / numberOfWaves
            val normedAmplitude = (1.5f * progress - 0.5f) * amplitude

            // Prepare path for this wave
            path.reset()
            var x = 0f
            while (x < width + density) {

                // We use a parable to scale the sinus wave, that has its peak in the middle of
                // the view.
                val scaling = (-(1 / mid * (x - mid)).toDouble().pow(2.0) + 1).toFloat()
                val y = ((scaling * amplitude * normedAmplitude
                        * sin(
                    2 * Math.PI * (x / width) * frequency
                            + phase * (i + 1)
                ))
                        + xAxisPosition).toFloat()
                if (x == 0f) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
                x += density
            }
            path.lineTo(x, height.toFloat())
            path.lineTo(0f, height.toFloat())
            path.close()

            // Set opacity for this wave fill
            paint.alpha = if (i == 0) 255 else 255 / (i + 1)

            // Draw wave
            canvas.drawPath(path, paint)
        }

        // Mask the wave
        mask?.let {
            val left = (width - it.width) / 2f
            val top = (height - it.height) / 2f
            canvas.drawBitmap(it, left, top, maskPaint)
        }

        if (isPlaying.get()) {
            phase += phaseShift
        }
        invalidate()
    }

    class Builder(context: Context?) {
        private val context: Context

        /**
         * Default is set at the beginning of Builder creation
         */
        var numberOfWaves = DEFAULT_NUMBER_OF_WAVES
        var phase = 0f // No Default value
        var amplitude = DEFAULT_AMPLITUDE
        var frequency = DEFAULT_FREQUENCY
        var phaseShift = DEFAULT_PHASE_SHIFT
        var density = DEFAULT_DENSITY
        var primaryWaveLineWidth = DEFAULT_PRIMARY_LINE_WIDTH
        var secondaryWaveLineWidth = DEFAULT_SECONDARY_LINE_WIDTH
        var backgroundColor = DEFAULT_BACKGROUND_COLOR
        var waveColor = DEFAULT_WAVE_COLOR
        var xAxisPositionMultiplier = DEFAULT_X_AXIS_POSITION_MULTIPLIER

        init {
            requireNotNull(context) { "Context cannot be null" }
            this.context = context
        }

        fun numberOfWaves(numberOfWaves: Int): Builder {
            this.numberOfWaves = numberOfWaves
            return this
        }

        fun phase(phase: Float): Builder {
            this.phase = phase
            return this
        }

        fun waveAmplitude(amplitude: Float): Builder {
            this.amplitude = amplitude
            return this
        }

        fun waveFrequency(frequency: Float): Builder {
            this.frequency = frequency
            return this
        }

        fun wavePhaseShift(phaseShift: Float): Builder {
            this.phaseShift = phaseShift
            return this
        }

        fun waveDensity(density: Float): Builder {
            this.density = density
            return this
        }

        fun primaryWaveLineWidth(primaryWaveLineWidth: Float): Builder {
            this.primaryWaveLineWidth = primaryWaveLineWidth
            return this
        }

        fun secondaryWaveLineWidth(secondaryWaveLineWidth: Float): Builder {
            this.secondaryWaveLineWidth = secondaryWaveLineWidth
            return this
        }

        fun waveBackgroundColor(backgroundColor: Int): Builder {
            this.backgroundColor = backgroundColor
            return this
        }

        fun waveColor(waveColor: Int): Builder {
            this.waveColor = waveColor
            return this
        }

        fun xAxisPositionMultiplier(xAxisPositionMultiplier: Float): Builder {
            this.xAxisPositionMultiplier = xAxisPositionMultiplier
            return this
        }

        fun build(): WaveView {
            return WaveView(context, this)
        }
    }

    companion object {
        /**
         * Default values for drawing the wave
         */
        private const val DEFAULT_NUMBER_OF_WAVES = 3
        private const val DEFAULT_FREQUENCY = 2.0f
        private const val DEFAULT_AMPLITUDE = 0.15f
        private const val DEFAULT_PHASE_SHIFT = -0.05f
        private const val DEFAULT_DENSITY = 5.0f
        private const val DEFAULT_PRIMARY_LINE_WIDTH = 3.0f
        private const val DEFAULT_SECONDARY_LINE_WIDTH = 1.0f
        private const val DEFAULT_BACKGROUND_COLOR = Color.BLACK
        private const val DEFAULT_WAVE_COLOR = Color.WHITE
        private const val DEFAULT_X_AXIS_POSITION_MULTIPLIER = 0.5f
    }
}