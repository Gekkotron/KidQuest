package com.gekkotron.kidquest.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gekkotron.kidquest.data.Child
import com.gekkotron.kidquest.data.GoalRepository
import com.gekkotron.kidquest.data.GoalStatus
import com.gekkotron.kidquest.data.GoalSummary
import com.gekkotron.kidquest.domain.RedeemSplitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RedeemState(
    val goalId: Long,
    val title: String,
    val cost: Int,
    val members: List<Child>,
    /** Raw text per child, as typed in the dialog. */
    val shares: Map<Long, String>,
) {
    /** null when any share is not a non-negative integer. */
    val parsed: Map<Long, Int>? = shares.mapValues { it.value.trim().toIntOrNull() }
        .takeIf { m -> m.values.all { it != null && it >= 0 } }
        ?.mapValues { it.value!! }
    val total: Int = shares.values.sumOf { it.trim().toIntOrNull() ?: 0 }
    val isValid: Boolean = parsed?.let { RedeemSplitter.isValid(cost, members.map { m -> m.id }.toSet(), it) } ?: false
}

data class GoalsUiState(
    val loading: Boolean = true,
    val active: List<GoalSummary> = emptyList(),
    val achieved: List<GoalSummary> = emptyList(),
    val redeemed: List<GoalSummary> = emptyList(),
    val redeem: RedeemState? = null,
    val restartGoalId: Long? = null,
)

class GoalsViewModel(private val goals: GoalRepository) : ViewModel() {

    private data class Dialogs(val redeem: RedeemState? = null, val restartGoalId: Long? = null)
    private val dialogs = MutableStateFlow(Dialogs())

    val state: StateFlow<GoalsUiState> = combine(goals.observeGoals(), dialogs) { all, d ->
        GoalsUiState(
            loading = false,
            active = all.filter { it.goal.status == GoalStatus.ACTIVE },
            achieved = all.filter { it.goal.status == GoalStatus.ACHIEVED },
            redeemed = all.filter { it.goal.status == GoalStatus.REDEEMED },
            redeem = d.redeem,
            restartGoalId = d.restartGoalId,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GoalsUiState())

    /** The Redeem button is only shown for loaded achieved goals, so the summary is always in state. */
    fun startRedeem(goalId: Long) {
        viewModelScope.launch {
            val summary = state.value.achieved.firstOrNull { it.goal.id == goalId } ?: return@launch
            val split = goals.defaultSplit(goalId)
            dialogs.update {
                it.copy(redeem = RedeemState(goalId, summary.goal.title, summary.goal.cost, summary.members, split.mapValues { e -> e.value.toString() }))
            }
        }
    }

    fun updateShare(childId: Long, text: String) {
        dialogs.update { d -> d.copy(redeem = d.redeem?.let { r -> r.copy(shares = r.shares + (childId to text)) }) }
    }

    fun cancelRedeem() = dialogs.update { it.copy(redeem = null) }

    fun confirmRedeem() {
        val r = dialogs.value.redeem ?: return
        val split = r.parsed?.takeIf { r.isValid } ?: return
        viewModelScope.launch {
            goals.redeem(r.goalId, split)
            dialogs.update { Dialogs(redeem = null, restartGoalId = r.goalId) }
        }
    }

    fun restart() {
        val id = dialogs.value.restartGoalId ?: return
        viewModelScope.launch {
            goals.restart(id)
            dialogs.update { it.copy(restartGoalId = null) }
        }
    }

    fun dismissRestart() = dialogs.update { it.copy(restartGoalId = null) }
}
