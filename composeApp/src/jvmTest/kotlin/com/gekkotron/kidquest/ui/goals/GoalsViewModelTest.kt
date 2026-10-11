package com.gekkotron.kidquest.ui.goals

import com.gekkotron.kidquest.data.ChildRepository
import com.gekkotron.kidquest.data.GoalRepository
import com.gekkotron.kidquest.data.GoalStatus
import com.gekkotron.kidquest.data.PointRepository
import com.gekkotron.kidquest.startTestKoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModelTest {
    private lateinit var koin: Koin

    @BeforeTest fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); koin = startTestKoin().koin }
    @AfterTest fun tearDown() { stopKoin(); Dispatchers.resetMain() }

    @Test
    fun redeemFlowWithAdjustedSplitThenRestart() = runTest {
        val kids = koin.get<ChildRepository>(); val goals = koin.get<GoalRepository>(); val points = koin.get<PointRepository>()
        val lea = kids.add("Léa", "🦊", 0xFFE57373); val tom = kids.add("Tom", "🐻", 0xFF64B5F6)
        val goal = goals.create("Cinema", "🎬", 1000, setOf(lea, tom))
        points.apply(lea, 800, "+800"); points.apply(tom, 200, "+200")

        val vm = GoalsViewModel(goals)
        assertEquals(listOf(goal), vm.state.first { !it.loading }.achieved.map { it.goal.id })

        vm.startRedeem(goal)
        val r = vm.state.first { it.redeem != null }.redeem!!
        assertEquals(mapOf(lea to "500", tom to "500"), r.shares)
        assertTrue(r.isValid)

        vm.updateShare(lea, "900")
        assertFalse(vm.state.value.redeem!!.isValid) // 1400
        vm.updateShare(tom, "abc")
        assertNull(vm.state.value.redeem!!.parsed)
        vm.updateShare(tom, "100")
        assertTrue(vm.state.value.redeem!!.isValid)

        vm.confirmRedeem()
        val after = vm.state.first { it.restartGoalId != null }
        assertNull(after.redeem)
        assertEquals(GoalStatus.REDEEMED, goals.get(goal)!!.status)
        assertEquals(-100, koin.get<com.gekkotron.kidquest.data.KidQuestDatabase>().pointEventDao().balance(lea))

        vm.restart()
        val restarted = vm.state.first { it.restartGoalId == null && it.active.isNotEmpty() }
        assertEquals("Cinema", restarted.active.single().goal.title)
    }

    @Test
    fun confirmIgnoredWhenSplitInvalid() = runTest {
        val kids = koin.get<ChildRepository>(); val goals = koin.get<GoalRepository>(); val points = koin.get<PointRepository>()
        val lea = kids.add("Léa", "🦊", 0xFFE57373)
        val goal = goals.create("Lego", "🧱", 100, setOf(lea))
        points.apply(lea, 100, "+100")
        val vm = GoalsViewModel(goals)
        vm.state.first { !it.loading }
        vm.startRedeem(goal)
        vm.state.first { it.redeem != null }
        vm.updateShare(lea, "50")
        vm.confirmRedeem()
        assertEquals(GoalStatus.ACHIEVED, goals.get(goal)!!.status)
    }
}
