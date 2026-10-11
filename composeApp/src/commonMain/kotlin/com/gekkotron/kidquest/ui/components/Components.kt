package com.gekkotron.kidquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** "+20" / "−10" (U+2212 minus sign, reads better for kids than a hyphen). */
fun formatSigned(points: Int): String = if (points >= 0) "+$points" else "−${-points}"

@Composable
fun ChildAvatar(avatar: String, color: Long, size: Dp = 48.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Color(color.toInt()).copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(avatar, fontSize = (size.value * 0.55f).sp)
    }
}

@Composable
fun PointsText(points: Int, style: TextStyle, modifier: Modifier = Modifier) {
    Text(
        text = points.toString(),
        style = style,
        color = if (points < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
fun GoalProgressBar(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
}
