package com.gekkotron.kidquest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin

private const val FULL_TURN_DEGREES = 360f
private val WHEEL_SIZE = 200.dp
private val SELECTOR_RADIUS = 6.dp
private val SELECTOR_OUTLINE = 2.dp
private val HUE_STOPS = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f)

/**
 * Maps a point relative to the wheel centre (screen coordinates, y pointing down) to hue and saturation.
 * Hue follows the sweep gradient: 0 at the right, increasing clockwise on screen.
 */
fun wheelPositionToHueSat(x: Float, y: Float, radius: Float): Pair<Float, Float> {
    val degrees = (atan2(y, x) * 180f / PI.toFloat() + FULL_TURN_DEGREES) % FULL_TURN_DEGREES
    val saturation = if (radius <= 0f) 0f else (hypot(x, y) / radius).coerceIn(0f, 1f)
    return degrees to saturation
}

@Composable
fun ColorWheel(
    hue: Float,
    saturation: Float,
    onChange: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnChange by rememberUpdatedState(onChange)
    Canvas(
        modifier.size(WHEEL_SIZE)
            .pointerInput(Unit) {
                fun report(p: Offset) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val (h, s) = wheelPositionToHueSat(p.x - c.x, p.y - c.y, min(size.width, size.height) / 2f)
                    currentOnChange(h, s)
                }
                detectTapGestures { report(it) }
            }
            .pointerInput(Unit) {
                fun report(p: Offset) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val (h, s) = wheelPositionToHueSat(p.x - c.x, p.y - c.y, min(size.width, size.height) / 2f)
                    currentOnChange(h, s)
                }
                detectDragGestures(
                    onDragStart = { report(it) },
                    onDrag = { change, _ -> report(change.position) },
                )
            },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f
        drawCircle(
            brush = Brush.sweepGradient(HUE_STOPS.map { Color.hsv(it % FULL_TURN_DEGREES, 1f, 1f) }, center),
            radius = radius, center = center,
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), center, radius),
            radius = radius, center = center,
        )
        val angle = hue * PI.toFloat() / 180f
        val dist = saturation.coerceIn(0f, 1f) * radius
        val pos = Offset(center.x + cos(angle) * dist, center.y + sin(angle) * dist)
        val r = SELECTOR_RADIUS.toPx()
        drawCircle(Color.Black.copy(alpha = 0.8f), radius = r + SELECTOR_OUTLINE.toPx(), center = pos)
        drawCircle(Color.White, radius = r, center = pos)
    }
}
