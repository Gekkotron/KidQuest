package com.gekkotron.kidquest.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface AppEvent {
    data class GoalAchieved(val goalId: Long, val title: String, val icon: String) : AppEvent
    data class LevelUp(val childId: Long, val childName: String, val level: Int) : AppEvent
}

/** App-wide one-shot events (celebrations). No replay: only live collectors see them. */
class AppEventBus {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    fun emit(event: AppEvent) {
        _events.tryEmit(event)
    }
}
