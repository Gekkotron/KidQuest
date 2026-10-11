package com.gekkotron.kidquest.ui.goals

import com.gekkotron.kidquest.data.ChildRepository
import com.gekkotron.kidquest.data.GoalRepository
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GoalEditViewModelTest {
    private lateinit var koin: Koin

    @BeforeTest fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()); koin = startTestKoin().koin }
    @AfterTest fun tearDown() { stopKoin(); Dispatchers.resetMain() }

    @Test
    fun newGoalNeedsTitleCostAndMember() = runTest {
        val lea = koin.get<ChildRepository>().add("Léa", "🦊", 0xFFE57373)
        val vm = GoalEditViewModel(0L, koin.get(), koin.get())
        vm.state.first { it.children.isNotEmpty() }
        assertFalse(vm.state.value.canSave)
        vm.setTitle("Lego"); vm.setCost("0")
        assertFalse(vm.state.value.canSave)
        vm.setCost("1000")
        assertFalse(vm.state.value.canSave)
        vm.toggleMember(lea)
        assertTrue(vm.state.value.canSave)
        vm.save()
        vm.state.first { it.done }
        val created = koin.get<GoalRepository>().observeGoals().first().single()
        assertEquals("Lego", created.goal.title)
        assertEquals(1000, created.goal.cost)
    }

    @Test
    fun editLoadsExistingGoal() = runTest {
        val lea = koin.get<ChildRepository>().add("Léa", "🦊", 0xFFE57373)
        val id = koin.get<GoalRepository>().create("Cinema", "🎬", 2000, setOf(lea))
        val vm = GoalEditViewModel(id, koin.get(), koin.get())
        val s = vm.state.first { !it.isNew && it.title.isNotEmpty() }
        assertEquals(listOf("Cinema", "🎬", "2000"), listOf(s.title, s.icon, s.costText))
        assertEquals(setOf(lea), s.memberIds)
        assertTrue(s.canDelete)
    }
}
