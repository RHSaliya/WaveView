package com.rhs.waveview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize

/**
 * Rasterizes a [Painter] (including vector drawables) into an [ImageBitmap] suitable for use as
 * the `mask` parameter on [WaveView]. The result is cached across recompositions.
 */
@Composable
fun rememberPainterAsImageBitmap(
    painter: Painter,
    width: Dp,
    height: Dp,
): ImageBitmap {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    return remember(painter, width, height, density, layoutDirection) {
        val widthPx = with(density) { width.roundToPx() }.coerceAtLeast(1)
        val heightPx = with(density) { height.roundToPx() }.coerceAtLeast(1)
        rasterize(painter, IntSize(widthPx, heightPx), density, layoutDirection)
    }
}

internal fun rasterize(
    painter: Painter,
    size: IntSize,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
): ImageBitmap {
    val bitmap = ImageBitmap(size.width, size.height)
    val canvas = Canvas(bitmap)
    val drawSize = Size(size.width.toFloat(), size.height.toFloat())
    CanvasDrawScope().draw(density, layoutDirection, canvas, drawSize) {
        with(painter) {
            draw(size = drawSize)
        }
    }
    return bitmap
}
