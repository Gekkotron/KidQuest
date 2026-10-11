package com.gekkotron.kidquest.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gekkotron.kidquest.data.GoalStatus
import com.gekkotron.kidquest.data.GoalSummary
import com.gekkotron.kidquest.data.PointAction
import com.gekkotron.kidquest.data.QuickAmount
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.home_actions_title
import com.gekkotron.kidquest.resources.home_add_first_child
import com.gekkotron.kidquest.resources.home_empty_title
import com.gekkotron.kidquest.resources.home_level
import com.gekkotron.kidquest.resources.home_more
import com.gekkotron.kidquest.resources.home_ready
import com.gekkotron.kidquest.resources.snackbar_applied
import com.gekkotron.kidquest.resources.undo
import com.gekkotron.kidquest.ui.components.ChildAvatar
import com.gekkotron.kidquest.ui.components.ChildEditDialog
import com.gekkotron.kidquest.ui.components.GoalProgressBar
import com.gekkotron.kidquest.ui.components.PointsText
import com.gekkotron.kidquest.ui.components.formatSigned
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(snackbarHostState: SnackbarHostState, onOpenChild: (Long) -> Unit, vm: HomeViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var addingChild by remember { mutableStateOf(false) }
    var sheetFor by remember { mutableStateOf<ChildCard?>(null) }

    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            if (effect is HomeEffect.Applied) {
                val result = snackbarHostState.showSnackbar(
                    message = getString(Res.string.snackbar_applied, formatSigned(effect.delta), effect.childName),
                    actionLabel = getString(Res.string.undo),
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) vm.undo(effect.eventId)
            }
        }
    }

    Box(Modifier.fillMaxSize().testTag("screen-home")) {
        if (!state.loading && state.children.isEmpty()) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👨‍👩‍👧‍👦", style = MaterialTheme.typography.displayMedium)
                Text(stringResource(Res.string.home_empty_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.padding(8.dp))
                Button(onClick = { addingChild = true }, modifier = Modifier.testTag("home-add-first-child")) {
                    Text(stringResource(Res.string.home_add_first_child))
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.goals.isNotEmpty()) {
                    item { GoalsStrip(state.goals) }
                }
                items(state.children, key = { it.summary.child.id }) { card ->
                    ChildCardView(
                        card = card,
                        quickAmounts = state.quickAmounts,
                        onQuick = { vm.applyQuick(card.summary.child.id, it) },
                        onMore = { sheetFor = card },
                        onOpen = { onOpenChild(card.summary.child.id) },
                    )
                }
            }
        }
    }

    if (addingChild) {
        ChildEditDialog(initial = null, onDismiss = { addingChild = false }) { name, avatar, color ->
            if (vm.addChild(name, avatar, color)) addingChild = false
        }
    }

    sheetFor?.let { card ->
        ActionsSheet(card, state.actions, onDismiss = { sheetFor = null }) { action ->
            vm.applyAction(card.summary.child.id, action)
            sheetFor = null
        }
    }
}

@Composable
private fun GoalsStrip(goals: List<GoalSummary>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(goals, key = { it.goal.id }) { g ->
            Card(Modifier.width(160.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${g.goal.icon} ${g.goal.title}", maxLines = 1, fontWeight = FontWeight.SemiBold)
                    GoalProgressBar(g.progress)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        g.members.forEach { Text(it.avatar) }
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (g.goal.status == GoalStatus.ACHIEVED) stringResource(Res.string.home_ready) else "${g.contributed}/${g.goal.cost}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChildCardView(
    card: ChildCard,
    quickAmounts: List<QuickAmount>,
    onQuick: (Int) -> Unit,
    onMore: () -> Unit,
    onOpen: () -> Unit,
) {
    val s = card.summary
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(s.child.color.toInt()).copy(alpha = 0.30f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
                ChildAvatar(s.child.avatar, s.child.color, 56.dp)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(s.child.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(Res.string.home_level, s.level) + if (s.streak > 0) "  🔥 ${s.streak}" else "",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                PointsText(
                    s.balance, MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    Modifier.testTag("balance-${s.child.id}"),
                )
            }
            card.closestGoal?.let { g ->
                Text("${g.goal.icon} ${g.goal.title}", style = MaterialTheme.typography.labelMedium)
                GoalProgressBar(g.progress)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quickAmounts.forEach { q ->
                    FilledTonalButton(onClick = { onQuick(q.points) }, modifier = Modifier.testTag("quick-${s.child.id}-${q.points}")) {
                        Text(formatSigned(q.points), style = MaterialTheme.typography.titleMedium)
                    }
                }
                TextButton(onClick = onMore, modifier = Modifier.testTag("more-${s.child.id}")) {
                    Text(stringResource(Res.string.home_more))
                }
            }
        }
    }
}

@Composable
private fun ActionsSheet(card: ChildCard, actions: List<PointAction>, onDismiss: () -> Unit, onPick: (PointAction) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(Res.string.home_actions_title, card.summary.child.name),
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyColumn {
            items(actions, key = { it.id }) { a ->
                ListItem(
                    leadingContent = { Text(a.icon, style = MaterialTheme.typography.headlineSmall) },
                    headlineContent = { Text(a.label) },
                    trailingContent = { Text(formatSigned(a.points), fontWeight = FontWeight.Bold) },
                    modifier = Modifier.clickable { onPick(a) }.testTag("action-${a.id}"),
                )
            }
        }
    }
}
