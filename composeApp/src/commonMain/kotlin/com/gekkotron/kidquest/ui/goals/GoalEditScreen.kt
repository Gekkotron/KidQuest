package com.gekkotron.kidquest.ui.goals

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.back
import com.gekkotron.kidquest.resources.delete
import com.gekkotron.kidquest.resources.goal_cost
import com.gekkotron.kidquest.resources.goal_edit_title
import com.gekkotron.kidquest.resources.goal_members
import com.gekkotron.kidquest.resources.goal_title
import com.gekkotron.kidquest.resources.goals_new
import com.gekkotron.kidquest.resources.save
import com.gekkotron.kidquest.ui.Palette
import com.gekkotron.kidquest.ui.components.ChildAvatar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GoalEditScreen(goalId: Long, onDone: () -> Unit) {
    val vm: GoalEditViewModel = koinViewModel(key = "goal-edit-$goalId") { parametersOf(goalId) }
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.done) { if (s.done) onDone() }

    Column(Modifier.fillMaxSize().testTag("screen-goal-edit")) {
        TopAppBar(
            title = { Text(stringResource(if (s.isNew) Res.string.goals_new else Res.string.goal_edit_title)) },
            navigationIcon = { TextButton(onClick = onDone) { Text("‹ " + stringResource(Res.string.back)) } },
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(Palette.goalIcons) { icon ->
                    Text(
                        icon, style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.clickable { vm.setIcon(icon) }.padding(4.dp)
                            .then(if (icon == s.icon) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                    )
                }
            }
            OutlinedTextField(
                value = s.title, onValueChange = vm::setTitle, singleLine = true,
                label = { Text(stringResource(Res.string.goal_title)) },
                modifier = Modifier.fillMaxWidth().testTag("goal-title"),
            )
            OutlinedTextField(
                value = s.costText, onValueChange = vm::setCost, singleLine = true,
                label = { Text(stringResource(Res.string.goal_cost)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("goal-cost"),
            )
            Text(stringResource(Res.string.goal_members), style = MaterialTheme.typography.titleSmall)
            s.children.forEach { child ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.toggleMember(child.id) }.testTag("goal-member-${child.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = child.id in s.memberIds, onCheckedChange = { vm.toggleMember(child.id) })
                    ChildAvatar(child.avatar, child.color, 32.dp)
                    Text(child.name, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Button(onClick = vm::save, enabled = s.canSave, modifier = Modifier.fillMaxWidth().testTag("goal-save")) {
                Text(stringResource(Res.string.save))
            }
            if (!s.isNew && s.canDelete) {
                OutlinedButton(onClick = vm::delete, modifier = Modifier.fillMaxWidth().testTag("goal-delete")) {
                    Text(stringResource(Res.string.delete))
                }
            }
        }
    }
}
