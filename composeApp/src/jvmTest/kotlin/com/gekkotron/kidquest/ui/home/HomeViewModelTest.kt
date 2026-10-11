package com.gekkotron.kidquest.ui.home

import app.cash.turbine.test
import com.gekkotron.kidquest.data.ChildRepository
import com.gekkotron.kidquest.data.Goal
import com.gekkotron.kidquest.data.GoalRepository
import com.gekkotron.kidquest.data.GoalStatus
import com.gekkotron.kidquest.data.GoalSummary
import com.gekkotron.kidquest.data.PointRepository
import com.gekkotron.kidquest.data.SettingsRepository
import com.gekkotron.kidquest.startTestKoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private lateinit var koin: Koin
    private lateinit var vm: HomeViewModel

    @BeforeTest fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        koin = startTestKoin().koin
        vm = HomeViewModel(koin.get<ChildRepository>(), koin.get<GoalRepository>(), koin.get<PointRepository>(), koin.get<SettingsRepository>())
    }

    @AfterTest fun tearDown() { stopKoin(); Dispatchers.resetMain() }

    @Test
    fun quickAwardEmitsUndoEffectThenUndoRestores() = runTest {
        val lea = koin.get<ChildRepository>().add("Léa", "🦊", 0xFFE57373)
        backgroundScope.launch { vm.state.collect {} } // keep WhileSubscribed state hot
        vm.state.first { it.children.isNotEmpty() }    // name lookup in award() needs loaded state
        vm.effects.test {
            vm.applyQuick(lea, 20)
            val effect = awaitItem() as HomeEffect.Applied
            assertEquals("Léa", effect.childName)
            assertEquals(20, effect.delta)
            vm.state.first { s -> s.children.single().summary.balance == 20 }
            vm.undo(effect.eventId)
        }
        vm.state.first { s -> s.children.single().summary.balance == 0 }
    }

    @Test
    fun stateExposesSeedQuickAmountsAndActions() = runTest {
        val s = vm.state.first { !it.loading }
        assertEquals(listOf(10, 20, 100, -10), s.quickAmounts.map { it.points })
        assertEquals(5, s.actions.size)
    }

    @Test
    fun addChildRejectsBlankName() {
        assertFalse(vm.addChild("  ", "🦊", 0xFFE57373))
    }

    @Test
    fun closestGoalPrefersHighestUnfinishedThenAchieved() {
        fun g(id: Long, progress: Float, status: GoalStatus = GoalStatus.ACTIVE, member: Long = 1) = GoalSummary(
            Goal(id = id, title = "g$id", icon = "🎁", cost = 100, status = status, createdAt = 0),
            members = listOf(com.gekkotron.kidquest.data.Child(id = member, name = "c", avatar = "🦊", color = 0, createdAt = 0)),
            contributed = (progress * 100).toInt(), progress = progress,
        )
        val goals = listOf(g(1, 0.2f), g(2, 0.8f), g(3, 1f, GoalStatus.ACHIEVED), g(4, 0.9f, member = 2), g(5, 0.95f, GoalStatus.REDEEMED))
        assertEquals(2L, closestGoal(1, goals)?.goal?.id)
        assertEquals(3L, closestGoal(1, goals.filter { it.goal.id == 3L })?.goal?.id)
        assertEquals(null, closestGoal(9, goals))
    }
}
