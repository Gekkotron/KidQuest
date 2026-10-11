package com.gekkotron.kidquest.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.cancel
import com.gekkotron.kidquest.resources.goals_redeem
import com.gekkotron.kidquest.resources.redeem_member_balance
import com.gekkotron.kidquest.resources.redeem_title
import com.gekkotron.kidquest.ui.components.ChildAvatar
import com.gekkotron.kidquest.ui.components.formatSigned
import org.jetbrains.compose.resources.stringResource

@Composable
fun RedeemDialog(state: RedeemState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.redeem_title, state.title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.members.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChildAvatar(m.child.avatar, m.child.color, 36.dp)
                        Column(Modifier.weight(1f)) {
                            Text(m.child.name)
                            Text(
                                stringResource(Res.string.redeem_member_balance, m.balance - state.cost),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(formatSigned(-state.cost), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("redeem-cost-${m.child.id}"))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("redeem-confirm")) {
                Text(stringResource(Res.string.goals_redeem))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
