package com.gekkotron.kidquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.color_custom
import com.gekkotron.kidquest.resources.ok
import org.jetbrains.compose.resources.stringResource

private const val DEFAULT_CUSTOM_HUE = 300f
private const val DEFAULT_CUSTOM_SATURATION = 0.9f
private const val RGB_MASK = 0xFFFFFFL

fun colorHexTag(color: Long): String = (color and RGB_MASK).toString(16).padStart(6, '0').uppercase()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    presets: List<Long>,
    selected: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "color",
) {
    var adding by remember { mutableStateOf(false) }
    var hue by remember { mutableFloatStateOf(DEFAULT_CUSTOM_HUE) }
    var saturation by remember { mutableFloatStateOf(DEFAULT_CUSTOM_SATURATION) }
    val options = if (selected in presets) presets else listOf(selected) + presets
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { c ->
                Column(
                    Modifier.clickable { onSelect(c); adding = false }
                        .then(if (c == selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                        .testTag("$testTagPrefix-${colorHexTag(c)}")
                ) { ChildAvatar(avatar = "", color = c, size = 32.dp) }
            }
            Box(
                Modifier.size(32.dp).clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { adding = true }
                    .testTag("$testTagPrefix-add"),
                contentAlignment = Alignment.Center,
            ) { Text("+", style = MaterialTheme.typography.titleMedium) }
        }
        if (adding) {
            val picked = Color.hsv(hue, saturation, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.color_custom), style = MaterialTheme.typography.labelMedium)
                ColorWheel(
                    hue = hue, saturation = saturation,
                    onChange = { h, s -> hue = h; saturation = s },
                    modifier = Modifier.testTag("$testTagPrefix-wheel"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(picked))
                    TextButton(
                        onClick = { onSelect(picked.toArgb().toLong() and 0xFFFFFFFFL); adding = false },
                        modifier = Modifier.testTag("$testTagPrefix-custom-ok"),
                    ) { Text(stringResource(Res.string.ok)) }
                }
            }
        }
    }
}
