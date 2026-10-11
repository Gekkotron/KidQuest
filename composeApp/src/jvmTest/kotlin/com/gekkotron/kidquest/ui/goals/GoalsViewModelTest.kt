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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModelTest {
    private lateinit var koin: Koin

    @BeforeTest fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); koin = startTestKoin().koin }
    @AfterTest fun tearDown() { stopKoin(); Dispatchers.resetMain() }

    @Test
    fun redeemFlowChargesEveryMemberThenRestart() = runTest {
        val kids = koin.get<ChildRepository>(); val goals = koin.get<GoalRepository>(); val points = koin.get<PointRepository>()
        val lea = kids.add("Léa", "🦊", 0xFFE57373); val tom = kids.add("Tom", "🐻", 0xFF64B5F6)
        val goal = goals.create("Cinema", "🎬", 1000, setOf(lea, tom))
        points.apply(lea, 1200, "+1200"); points.apply(tom, 1000, "+1000")

        val vm = GoalsViewModel(goals)
        assertEquals(listOf(goal), vm.state.first { !it.loading }.achieved.map { it.goal.id })

        vm.startRedeem(goal)
        val r = vm.state.first { it.redeem != null }.redeem!!
        assertEquals(1000, r.cost)
        assertEquals(listOf(lea to 1200, tom to 1000), r.members.map { it.child.id to it.balance })

        vm.confirmRedeem()
        val after = vm.state.first { it.restartGoalId != null }
        assertNull(after.redeem)
        assertEquals(GoalStatus.REDEEMED, goals.get(goal)!!.status)
        val db = koin.get<com.gekkotron.kidquest.data.KidQuestDatabase>().pointEventDao()
        assertEquals(200, db.balance(lea))
        assertEquals(0, db.balance(tom))

        vm.restart()
        val restarted = vm.state.first { it.restartGoalId == null && it.active.isNotEmpty() }
        assertEquals("Cinema", restarted.active.single().goal.title)
    }

    @Test
    fun cancelRedeemClosesDialogWithoutCharging() = runTest {
        val kids = koin.get<ChildRepository>(); val goals = koin.get<GoalRepository>(); val points = koin.get<PointRepository>()
        val lea = kids.add("Léa", "🦊", 0xFFE57373)
        val goal = goals.create("Lego", "🧱", 100, setOf(lea))
        points.apply(lea, 100, "+100")
        val vm = GoalsViewModel(goals)
        vm.state.first { !it.loading }
        vm.startRedeem(goal)
        vm.state.first { it.redeem != null }
        vm.cancelRedeem()
        assertNull(vm.state.value.redeem)
        assertEquals(GoalStatus.ACHIEVED, goals.get(goal)!!.status)
    }
}
