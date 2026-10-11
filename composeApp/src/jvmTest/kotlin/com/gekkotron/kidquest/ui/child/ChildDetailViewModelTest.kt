package com.gekkotron.kidquest.ui.child

import com.gekkotron.kidquest.data.ChildRepository
import com.gekkotron.kidquest.data.KidQuestDatabase
import com.gekkotron.kidquest.data.PointEvent
import com.gekkotron.kidquest.data.PointEventType
import com.gekkotron.kidquest.data.PointRepository
import com.gekkotron.kidquest.startTestKoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChildDetailViewModelTest {
    private lateinit var koin: Koin

    @BeforeTest fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); koin = startTestKoin().koin }
    @AfterTest fun tearDown() { stopKoin(); Dispatchers.resetMain() }

    private fun at(day: Int, hour: Int) = LocalDateTime(2026, 10, day, hour, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()

    @Test
    fun historyGroupedByDayNewestFirst_andUndoRestoreToggle() = runTest {
        val lea = koin.get<ChildRepository>().add("Léa", "🦊", 0xFFE57373)
        val dao = koin.get<KidQuestDatabase>().pointEventDao()
        dao.insert(PointEvent(childId = lea, delta = 50, type = PointEventType.AWARD, label = "Wipe", timestamp = at(10, 9)))
        dao.insert(PointEvent(childId = lea, delta = 20, type = PointEventType.AWARD, label = "+20", timestamp = at(11, 8)))
        val lastId = dao.insert(PointEvent(childId = lea, delta = -10, type = PointEventType.PENALTY, label = "−10", timestamp = at(11, 19)))

        val vm = ChildDetailViewModel(lea, koin.get(), koin.get<PointRepository>(), TimeZone.UTC)
        val s = vm.state.first { !it.loading && it.days.isNotEmpty() }
        assertEquals(listOf(LocalDate(2026, 10, 11), LocalDate(2026, 10, 10)), s.days.map { it.date })
        assertEquals(listOf("−10", "+20"), s.days.first().events.map { it.label })
        assertEquals(60, s.summary!!.balance)

        vm.undo(lastId)
        // history and totals are separate Room flows, so wait for both to reflect the change
        assertTrue(vm.state.first { st -> st.days.first().events.first().undone && st.summary!!.balance == 70 }.summary!!.balance == 70)
        vm.restore(lastId)
        assertEquals(60, vm.state.first { st -> !st.days.first().events.first().undone && st.summary!!.balance == 60 }.summary!!.balance)
    }
}
