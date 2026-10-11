package com.gekkotron.kidquest.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gekkotron.kidquest.data.Child
import com.gekkotron.kidquest.data.ChildRepository
import com.gekkotron.kidquest.data.GoalRepository
import com.gekkotron.kidquest.data.GoalStatus
import com.gekkotron.kidquest.ui.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalEditUiState(
    val isNew: Boolean = true,
    val title: String = "",
    val icon: String = Palette.goalIcons.first(),
    val costText: String = "",
    val memberIds: Set<Long> = emptySet(),
    val children: List<Child> = emptyList(),
    val canDelete: Boolean = false,
    val done: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() && (costText.toIntOrNull() ?: 0) > 0 && memberIds.isNotEmpty()
}

class GoalEditViewModel(
    private val goalId: Long,
    private val goals: GoalRepository,
    children: ChildRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GoalEditUiState(isNew = goalId == 0L))
    val state: StateFlow<GoalEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (goalId != 0L) {
                val goal = goals.get(goalId)
                if (goal != null) {
                    _state.update {
                        it.copy(
                            isNew = false, title = goal.title, icon = goal.icon, costText = goal.cost.toString(),
                            memberIds = goals.memberIds(goalId).toSet(), canDelete = goal.status != GoalStatus.REDEEMED,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            children.observeAll().collect { all ->
                // Archived children stay visible only if already members.
                _state.update { s -> s.copy(children = all.filter { !it.archived || it.id in s.memberIds }) }
            }
        }
    }

    fun setTitle(value: String) = _state.update { it.copy(title = value) }
    fun setIcon(value: String) = _state.update { it.copy(icon = value) }
    fun setCost(value: String) = _state.update { it.copy(costText = value.filter(Char::isDigit).take(7)) }
    fun toggleMember(childId: Long) = _state.update {
        it.copy(memberIds = if (childId in it.memberIds) it.memberIds - childId else it.memberIds + childId)
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val cost = s.costText.toInt()
            if (s.isNew) goals.create(s.title, s.icon, cost, s.memberIds) else goals.update(goalId, s.title, s.icon, cost, s.memberIds)
            _state.update { it.copy(done = true) }
        }
    }

    fun delete() {
        if (!_state.value.canDelete) return
        viewModelScope.launch {
            goals.delete(goalId)
            _state.update { it.copy(done = true) }
        }
    }
}
