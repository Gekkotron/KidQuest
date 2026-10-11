package com.gekkotron.kidquest.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
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
import com.gekkotron.kidquest.data.PointAction
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.action_edit_title
import com.gekkotron.kidquest.resources.action_label
import com.gekkotron.kidquest.resources.action_points
import com.gekkotron.kidquest.resources.cancel
import com.gekkotron.kidquest.resources.quick_edit_title
import com.gekkotron.kidquest.resources.save
import com.gekkotron.kidquest.ui.components.formatSigned
import com.gekkotron.kidquest.ui.Palette
import com.gekkotron.kidquest.ui.components.IconPicker
import org.jetbrains.compose.resources.stringResource

@Composable
fun ActionEditDialog(initial: PointAction?, onDismiss: () -> Unit, onSave: (label: String, pointsText: String, icon: String) -> Unit) {
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var points by remember { mutableStateOf(initial?.points?.let(::formatSigned).orEmpty()) }
    var icon by remember { mutableStateOf(initial?.icon ?: Palette.actionIcons.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.action_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IconPicker(Palette.actionIcons, icon, { icon = it }, testTagPrefix = "action-icon")
                OutlinedTextField(label, { label = it }, singleLine = true, label = { Text(stringResource(Res.string.action_label)) }, modifier = Modifier.testTag("action-label"))
                OutlinedTextField(points, { points = it }, singleLine = true, label = { Text(stringResource(Res.string.action_points)) }, modifier = Modifier.testTag("action-points"))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(label, points, icon) },
                enabled = label.isNotBlank() && parsePoints(points) != null,
                modifier = Modifier.testTag("dialog-save"),
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

@Composable
fun QuickAmountDialog(initial: Int?, onDismiss: () -> Unit, onSave: (pointsText: String) -> Unit) {
    var points by remember { mutableStateOf(initial?.let(::formatSigned).orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.quick_edit_title)) },
        text = {
            OutlinedTextField(points, { points = it }, singleLine = true, label = { Text(stringResource(Res.string.action_points)) }, modifier = Modifier.testTag("quick-points"))
        },
        confirmButton = {
            TextButton(onClick = { onSave(points) }, enabled = parsePoints(points) != null, modifier = Modifier.testTag("dialog-save")) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
