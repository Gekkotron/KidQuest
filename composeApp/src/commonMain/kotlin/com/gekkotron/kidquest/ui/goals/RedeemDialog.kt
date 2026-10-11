package com.gekkotron.kidquest.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.cancel
import com.gekkotron.kidquest.resources.goals_redeem
import com.gekkotron.kidquest.resources.redeem_title
import com.gekkotron.kidquest.resources.redeem_total
import com.gekkotron.kidquest.ui.components.ChildAvatar
import org.jetbrains.compose.resources.stringResource

@Composable
fun RedeemDialog(state: RedeemState, onShare: (Long, String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.redeem_title, state.title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.members.forEach { child ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChildAvatar(child.avatar, child.color, 36.dp)
                        Text(child.name, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = state.shares[child.id].orEmpty(),
                            onValueChange = { onShare(child.id, it) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(110.dp).testTag("share-${child.id}"),
                        )
                    }
                }
                Text(
                    stringResource(Res.string.redeem_total, state.total, state.cost),
                    color = if (state.isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = state.isValid, modifier = Modifier.testTag("redeem-confirm")) {
                Text(stringResource(Res.string.goals_redeem))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
