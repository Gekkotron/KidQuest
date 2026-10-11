package com.gekkotron.kidquest.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gekkotron.kidquest.data.Child
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.cancel
import com.gekkotron.kidquest.resources.child_edit_title
import com.gekkotron.kidquest.resources.child_edit_title_new
import com.gekkotron.kidquest.resources.child_name
import com.gekkotron.kidquest.resources.save
import com.gekkotron.kidquest.ui.Palette
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChildEditDialog(initial: Child?, onDismiss: () -> Unit, onSave: (name: String, avatar: String, color: Long) -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var avatar by remember { mutableStateOf(initial?.avatar ?: Palette.avatars.first()) }
    var color by remember { mutableLongStateOf(initial?.color ?: Palette.childColors.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) Res.string.child_edit_title_new else Res.string.child_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text(stringResource(Res.string.child_name)) },
                    modifier = Modifier.testTag("child-name"),
                )
                IconPicker(Palette.avatars, avatar, { avatar = it }, testTagPrefix = "avatar")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palette.childColors.forEach { c ->
                        Column(
                            Modifier.clickable { color = c }
                                .then(if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                        ) { ChildAvatar(avatar = "", color = c, size = 32.dp) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), avatar, color) }, enabled = name.isNotBlank(), modifier = Modifier.testTag("child-save")) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
