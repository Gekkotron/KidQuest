package com.gekkotron.kidquest.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gekkotron.kidquest.data.GoalStatus
import com.gekkotron.kidquest.data.GoalSummary
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.goals_empty
import com.gekkotron.kidquest.resources.goals_new
import com.gekkotron.kidquest.resources.goals_redeem
import com.gekkotron.kidquest.resources.goals_tab_achieved
import com.gekkotron.kidquest.resources.goals_tab_active
import com.gekkotron.kidquest.resources.goals_tab_redeemed
import com.gekkotron.kidquest.resources.restart_body
import com.gekkotron.kidquest.resources.restart_no
import com.gekkotron.kidquest.resources.restart_title
import com.gekkotron.kidquest.resources.restart_yes
import com.gekkotron.kidquest.ui.components.GoalProgressBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GoalsScreen(onNew: () -> Unit, onEdit: (Long) -> Unit, vm: GoalsViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(Res.string.goals_tab_active, Res.string.goals_tab_achieved, Res.string.goals_tab_redeemed)
    val shown = when (tab) { 0 -> state.active; 1 -> state.achieved; else -> state.redeemed }

    Box(Modifier.fillMaxSize().testTag("screen-goals")) {
        Column {
            PrimaryTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, label ->
                    val count = when (i) { 0 -> state.active.size; 1 -> state.achieved.size; else -> state.redeemed.size }
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text("${stringResource(label)} ($count)") })
                }
            }
            if (shown.isEmpty()) {
                Text(stringResource(Res.string.goals_empty), modifier = Modifier.padding(24.dp))
            }
            LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shown, key = { it.goal.id }) { g ->
                    GoalCard(g, onEdit = { onEdit(g.goal.id) }, onRedeem = { vm.startRedeem(g.goal.id) })
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNew,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("goals-new"),
        ) { Text("＋ " + stringResource(Res.string.goals_new)) }
    }

    state.redeem?.let { r ->
        RedeemDialog(r, onConfirm = vm::confirmRedeem, onDismiss = vm::cancelRedeem)
    }
    if (state.restartGoalId != null) {
        AlertDialog(
            onDismissRequest = vm::dismissRestart,
            title = { Text(stringResource(Res.string.restart_title)) },
            text = { Text(stringResource(Res.string.restart_body)) },
            confirmButton = { TextButton(onClick = vm::restart, modifier = Modifier.testTag("restart-yes")) { Text(stringResource(Res.string.restart_yes)) } },
            dismissButton = { TextButton(onClick = vm::dismissRestart) { Text(stringResource(Res.string.restart_no)) } },
        )
    }
}

@Composable
private fun GoalCard(g: GoalSummary, onEdit: () -> Unit, onRedeem: () -> Unit) {
    val editable = g.goal.status != GoalStatus.REDEEMED
    Card(Modifier.fillMaxWidth().then(if (editable) Modifier.clickable(onClick = onEdit) else Modifier)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(g.goal.icon, style = MaterialTheme.typography.headlineMedium)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(g.goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (g.members.size < 2) Text(g.members.joinToString(" ") { "${it.avatar} ${it.name}" }, style = MaterialTheme.typography.bodySmall)
                }
                if (g.members.size == 1) {
                    Text("${g.memberProgress.single().balance.coerceIn(0, g.goal.cost)}/${g.goal.cost}", style = MaterialTheme.typography.labelLarge)
                }
                            }
            if (g.goal.status != GoalStatus.REDEEMED) GoalProgressBar(g.progress)
            if (g.members.size > 1) MemberProgressList(g)
            if (g.goal.status == GoalStatus.ACHIEVED) {
                Button(onClick = onRedeem, modifier = Modifier.testTag("redeem-${g.goal.id}")) { Text("🎁 " + stringResource(Res.string.goals_redeem)) }
            }
        }
    }
}

@Composable
private fun MemberProgressList(g: GoalSummary) {
    val kids = g.members.associateBy { it.id }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        g.memberProgress.forEach { m ->
            val child = kids[m.childId] ?: return@forEach
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(child.avatar)
                GoalProgressBar(m.progress, Modifier.weight(1f))
                Text("${m.balance.coerceIn(0, g.goal.cost)}/${g.goal.cost}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("member-progress-${g.goal.id}-${m.childId}"))
            }
        }
    }
}
