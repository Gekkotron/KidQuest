package com.gekkotron.kidquest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gekkotron.kidquest.domain.DailyProgress
import kotlin.math.max

private val PenaltyBar = Color(0xFFF48FB1)
private val RedeemBar = Color(0xFFB71C1C)

@Composable
fun ProgressChart(
    series: List<DailyProgress>,
    awardColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
) {
    val axis = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (series.isEmpty()) return@Canvas
        val maxMag = max(1, series.maxOf { max(it.award, it.penalty + it.redeem) }).toFloat()
        val gap = 2.dp.toPx()
        val slot = size.width / series.size
        val barWidth = (slot - gap).coerceAtLeast(1f)
        val baselineY = size.height / 2f
        val halfHeight = size.height / 2f - 2.dp.toPx()
        drawLine(
            color = axis,
            start = Offset(0f, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 1.dp.toPx(),
        )
        series.forEachIndexed { i, d ->
            val x = i * slot + gap / 2f
            if (d.award > 0) {
                val h = (d.award / maxMag) * halfHeight
                drawRect(awardColor, Offset(x, baselineY - h), Size(barWidth, h))
            }
            var below = baselineY
            if (d.penalty > 0) {
                val h = (d.penalty / maxMag) * halfHeight
                drawRect(PenaltyBar, Offset(x, below), Size(barWidth, h))
                below += h
            }
            if (d.redeem > 0) {
                val h = (d.redeem / maxMag) * halfHeight
                drawRect(RedeemBar, Offset(x, below), Size(barWidth, h))
            }
        }
    }
}
