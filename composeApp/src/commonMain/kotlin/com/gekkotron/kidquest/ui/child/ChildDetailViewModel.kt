package com.gekkotron.kidquest.ui.child

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gekkotron.kidquest.data.ChildRepository
import com.gekkotron.kidquest.data.ChildSummary
import com.gekkotron.kidquest.data.PointEvent
import com.gekkotron.kidquest.data.PointRepository
import com.gekkotron.kidquest.domain.DailyProgress
import com.gekkotron.kidquest.domain.DayMapper
import com.gekkotron.kidquest.domain.ProgressSeries
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

data class HistoryDay(val date: LocalDate, val events: List<PointEvent>)

data class ChildDetailUiState(
    val loading: Boolean = true,
    val summary: ChildSummary? = null,
    val days: List<HistoryDay> = emptyList(),
    val chart: List<DailyProgress> = emptyList(),
)

class ChildDetailViewModel(
    private val childId: Long,
    children: ChildRepository,
    private val points: PointRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val clock: Clock = Clock.System,
    private val chartWindowDays: Int = 14,
) : ViewModel() {

    val state: StateFlow<ChildDetailUiState> = combine(
        children.observeSummaries(includeArchived = true),
        points.history(childId),
    ) { summaries, history ->
        val today = DayMapper.toLocalDate(clock.now().toEpochMilliseconds(), timeZone)
        ChildDetailUiState(
            loading = false,
            summary = summaries.firstOrNull { it.child.id == childId },
            // history is already newest-first; groupBy keeps encounter order
            days = history.groupBy { DayMapper.toLocalDate(it.timestamp, timeZone) }.map { (d, e) -> HistoryDay(d, e) },
            chart = ProgressSeries.daily(history, today, timeZone, chartWindowDays),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChildDetailUiState())

    fun undo(eventId: Long) {
        viewModelScope.launch { points.undo(eventId) }
    }

    fun restore(eventId: Long) {
        viewModelScope.launch { points.restore(eventId) }
    }
}
