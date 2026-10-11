package com.gekkotron.kidquest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gekkotron.kidquest.data.ChildRepository
import com.gekkotron.kidquest.data.ChildSummary
import com.gekkotron.kidquest.data.GoalRepository
import com.gekkotron.kidquest.data.GoalStatus
import com.gekkotron.kidquest.data.GoalSummary
import com.gekkotron.kidquest.data.PointAction
import com.gekkotron.kidquest.data.PointRepository
import com.gekkotron.kidquest.data.QuickAmount
import com.gekkotron.kidquest.data.SettingsRepository
import com.gekkotron.kidquest.ui.components.formatSigned
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChildCard(val summary: ChildSummary, val closestGoal: GoalSummary?)

data class HomeUiState(
    val loading: Boolean = true,
    val children: List<ChildCard> = emptyList(),
    val goals: List<GoalSummary> = emptyList(),
    val quickAmounts: List<QuickAmount> = emptyList(),
    val actions: List<PointAction> = emptyList(),
)

sealed interface HomeEffect {
    data class Applied(val eventId: Long, val childName: String, val delta: Int) : HomeEffect
}

/** Highest-progress unfinished goal of the child, else an achieved one, else null. Redeemed goals are ignored. */
fun closestGoal(childId: Long, goals: List<GoalSummary>): GoalSummary? {
    val mine = goals.filter { g -> g.goal.status != GoalStatus.REDEEMED && g.members.any { it.id == childId } }
    return mine.filter { it.goal.status == GoalStatus.ACTIVE }.maxByOrNull { it.progress }
        ?: mine.firstOrNull { it.goal.status == GoalStatus.ACHIEVED }
}

class HomeViewModel(
    private val children: ChildRepository,
    goals: GoalRepository,
    private val points: PointRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    val state: StateFlow<HomeUiState> = combine(
        children.observeSummaries(),
        goals.observeGoals(),
        settings.observeQuickAmounts(),
        settings.observeActions(),
    ) { kids, allGoals, quick, actions ->
        val open = allGoals.filter { it.goal.status != GoalStatus.REDEEMED }
        HomeUiState(
            loading = false,
            children = kids.map { ChildCard(it, closestGoal(it.child.id, open)) },
            goals = open,
            quickAmounts = quick,
            actions = actions,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun applyQuick(childId: Long, points: Int) = award(childId, points, formatSigned(points), null)

    fun applyAction(childId: Long, action: PointAction) = award(childId, action.points, action.label, action.id)

    fun undo(eventId: Long) {
        viewModelScope.launch { points.undo(eventId) }
    }

    fun addChild(name: String, avatar: String, color: Long): Boolean {
        if (name.isBlank()) return false
        viewModelScope.launch { children.add(name, avatar, color) }
        return true
    }

    private fun award(childId: Long, delta: Int, label: String, actionId: Long?) {
        viewModelScope.launch {
            val id = points.apply(childId, delta, label, actionId)
            val name = state.value.children.firstOrNull { it.summary.child.id == childId }?.summary?.child?.name.orEmpty()
            _effects.send(HomeEffect.Applied(id, name, delta))
        }
    }
}
