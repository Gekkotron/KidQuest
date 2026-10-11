package com.gekkotron.kidquest.ui.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gekkotron.kidquest.data.PointEvent
import com.gekkotron.kidquest.data.PointEventType
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.back
import com.gekkotron.kidquest.resources.child_lifetime
import com.gekkotron.kidquest.resources.child_redeemed
import com.gekkotron.kidquest.resources.child_restore
import com.gekkotron.kidquest.resources.child_streak
import com.gekkotron.kidquest.resources.home_level
import com.gekkotron.kidquest.resources.undo
import com.gekkotron.kidquest.ui.components.ChildAvatar
import com.gekkotron.kidquest.ui.components.GoalProgressBar
import com.gekkotron.kidquest.ui.components.PointsText
import com.gekkotron.kidquest.ui.components.formatSigned
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChildDetailScreen(childId: Long, onBack: () -> Unit) {
    val vm: ChildDetailViewModel = koinViewModel(key = "child-$childId") { parametersOf(childId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val summary = state.summary

    Column(Modifier.fillMaxSize().testTag("screen-child")) {
        TopAppBar(
            title = { Text(summary?.child?.name.orEmpty()) },
            navigationIcon = { TextButton(onClick = onBack) { Text("‹ " + stringResource(Res.string.back)) } },
        )
        if (summary != null) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ChildAvatar(summary.child.avatar, summary.child.color, 72.dp)
                Column(Modifier.padding(start = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PointsText(summary.balance, MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold))
                    Text(stringResource(Res.string.child_lifetime, summary.lifetime), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(Res.string.home_level, summary.level), style = MaterialTheme.typography.labelLarge)
                    GoalProgressBar(summary.levelProgress)
                    if (summary.streak > 0) Text(stringResource(Res.string.child_streak, summary.streak))
                }
            }
        }
        LazyColumn {
            state.days.forEach { day ->
                item(key = "day-${day.date}") {
                    Text(
                        day.date.toString(), style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                items(day.events, key = { it.id }) { e -> HistoryRow(e, onUndo = { vm.undo(e.id) }, onRestore = { vm.restore(e.id) }) }
            }
        }
    }
}

@Composable
private fun HistoryRow(e: PointEvent, onUndo: () -> Unit, onRestore: () -> Unit) {
    val time = Instant.fromEpochMilliseconds(e.timestamp).toLocalDateTime(TimeZone.currentSystemDefault()).time
    val strike = if (e.undone) TextDecoration.LineThrough else null
    ListItem(
        modifier = Modifier.testTag("event-${e.id}"),
        headlineContent = {
            Text(
                if (e.type == PointEventType.REDEEM) stringResource(Res.string.child_redeemed, e.label) else e.label,
                textDecoration = strike,
            )
        },
        supportingContent = { Text("${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}") },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatSigned(e.delta), fontWeight = FontWeight.Bold, textDecoration = strike)
                when {
                    !e.undone -> TextButton(onClick = onUndo, modifier = Modifier.testTag("undo-${e.id}")) { Text(stringResource(Res.string.undo)) }
                    e.type != PointEventType.REDEEM ->
                        TextButton(onClick = onRestore, modifier = Modifier.testTag("restore-${e.id}")) { Text(stringResource(Res.string.child_restore)) }
                }
            }
        },
    )
}
