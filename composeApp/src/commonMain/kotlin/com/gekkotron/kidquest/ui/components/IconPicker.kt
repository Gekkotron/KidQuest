package com.gekkotron.kidquest.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.icon_custom
import com.gekkotron.kidquest.resources.ok
import org.jetbrains.compose.resources.stringResource

private const val MAX_CUSTOM_ICON_LENGTH = 16

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(
    presets: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "icon",
) {
    var adding by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }
    val options = if (selected in presets) presets else listOf(selected) + presets
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { icon ->
                Text(
                    icon, style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.clickable { onSelect(icon) }.padding(4.dp)
                        .then(if (icon == selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                )
            }
            Text(
                "+", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.clickable { adding = true }.padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("$testTagPrefix-add"),
            )
        }
        if (adding) {
            val trimmed = custom.trim()
            val valid = trimmed.isNotEmpty() && trimmed.length <= MAX_CUSTOM_ICON_LENGTH
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = custom, onValueChange = { custom = it }, singleLine = true,
                    label = { Text(stringResource(Res.string.icon_custom)) },
                    modifier = Modifier.testTag("$testTagPrefix-custom"),
                )
                TextButton(
                    onClick = { onSelect(trimmed); custom = ""; adding = false },
                    enabled = valid,
                    modifier = Modifier.testTag("$testTagPrefix-custom-ok"),
                ) { Text(stringResource(Res.string.ok)) }
            }
        }
    }
}
