package com.rhs.waveview

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.pow
import kotlin.math.sin

object WaveViewDefaults {
    const val NumberOfWaves: Int = 3
    const val Frequency: Float = 2.0f
    const val Amplitude: Float = 0.15f
    const val PhaseShift: Float = -0.05f
    const val Density: Float = 5.0f
    const val PrimaryWaveLineWidth: Float = 3.0f
    const val SecondaryWaveLineWidth: Float = 1.0f
    val BackgroundColor: Color = Color.Black
    val WaveColor: Color = Color.White
    const val XAxisPositionMultiplier: Float = 0.5f
}

/**
 * Composable that draws an animated multi-wave sine pattern.
 *
 * @param modifier Modifier applied to the underlying Canvas.
 * @param numberOfWaves Number of stacked waves to draw.
 * @param frequency Frequency of the primary wave.
 * @param amplitude Amplitude of the primary wave (relative to view height).
 * @param phaseShift Amount the phase advances each animation frame.
 * @param density Horizontal sampling step in pixels. Smaller values produce smoother curves.
 * @param primaryWaveLineWidth Stroke width of the primary (first) wave.
 * @param secondaryWaveLineWidth Stroke width of secondary waves.
 * @param backgroundColor Color filled behind the waves.
 * @param waveColor Color of the wave fill.
 * @param xAxisPositionMultiplier Vertical position of the wave's x-axis as a fraction of height (0..1).
 * @param isPlaying Whether the wave should animate.
 * @param mask Optional [Painter] (e.g. a vector or raster drawable) whose alpha is used to clip
 *             the wave output. The painter is drawn at the full size of this WaveView, so the
 *             waves appear inside the painter's shape regardless of the WaveView's size.
 */
@Composable
fun WaveView(
    modifier: Modifier = Modifier,
    numberOfWaves: Int = WaveViewDefaults.NumberOfWaves,
    frequency: Float = WaveViewDefaults.Frequency,
    amplitude: Float = WaveViewDefaults.Amplitude,
    phaseShift: Float = WaveViewDefaults.PhaseShift,
    density: Float = WaveViewDefaults.Density,
    primaryWaveLineWidth: Float = WaveViewDefaults.PrimaryWaveLineWidth,
    secondaryWaveLineWidth: Float = WaveViewDefaults.SecondaryWaveLineWidth,
    backgroundColor: Color = WaveViewDefaults.BackgroundColor,
    waveColor: Color = WaveViewDefaults.WaveColor,
    xAxisPositionMultiplier: Float = WaveViewDefaults.XAxisPositionMultiplier,
    isPlaying: Boolean = true,
    mask: Painter? = null,
) {
    var phase by remember { mutableFloatStateOf(0f) }
    val path = remember { Path() }
    val maskPaint = remember { Paint().apply { blendMode = BlendMode.DstIn } }
    val boundedXAxis = xAxisPositionMultiplier.coerceIn(0f, 1f)

    LaunchedEffect(isPlaying, phaseShift) {
        if (isPlaying && phaseShift != 0f) {
            while (true) {
                withFrameNanos {
                    phase += phaseShift
                }
            }
        }
    }

    val canvasModifier = if (mask != null) {
        modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    } else {
        modifier
    }

    Canvas(modifier = canvasModifier) {
        drawWaves(
            path = path,
            phase = phase,
            numberOfWaves = numberOfWaves,
            frequency = frequency,
            amplitude = amplitude,
            density = density,
            primaryWaveLineWidth = primaryWaveLineWidth,
            secondaryWaveLineWidth = secondaryWaveLineWidth,
            backgroundColor = backgroundColor,
            waveColor = waveColor,
            xAxisPositionMultiplier = boundedXAxis,
        )

        if (mask != null) {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(
                    bounds = Rect(0f, 0f, size.width, size.height),
                    paint = maskPaint,
                )
                with(mask) { draw(size = size) }
                canvas.restore()
            }
        }
    }
}

private fun DrawScope.drawWaves(
    path: Path,
    phase: Float,
    numberOfWaves: Int,
    frequency: Float,
    amplitude: Float,
    density: Float,
    primaryWaveLineWidth: Float,
    secondaryWaveLineWidth: Float,
    backgroundColor: Color,
    waveColor: Color,
    xAxisPositionMultiplier: Float,
) {
    drawRect(color = backgroundColor)

    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f || numberOfWaves <= 0 || density <= 0f) return

    val xAxisPosition = height * xAxisPositionMultiplier
    val mid = width / 2f

    for (i in 0 until numberOfWaves) {
        val strokeWidth = if (i == 0) primaryWaveLineWidth else secondaryWaveLineWidth
        val progress = 1.0f - i.toFloat() / numberOfWaves
        val normedAmplitude = (1.5f * progress - 0.5f) * amplitude

        path.reset()
        var x = 0f
        while (x < width + density) {
            val scaling = (-(1.0 / mid * (x - mid)).pow(2.0) + 1).toFloat()
            val y = (scaling * amplitude * normedAmplitude
                    * sin(2 * Math.PI * (x / width) * frequency + phase * (i + 1)).toFloat()
                    ) + xAxisPosition
            if (x == 0f) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            x += density
        }
        path.lineTo(x, height)
        path.lineTo(0f, height)
        path.close()

        val alpha = if (i == 0) 1f else 1f / (i + 1)
        val tinted = waveColor.copy(alpha = waveColor.alpha * alpha)
        drawPath(path = path, color = tinted)
        drawPath(path = path, color = tinted, style = Stroke(width = strokeWidth))
    }
}
