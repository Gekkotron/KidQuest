package com.gekkotron.kidquest.ui.settings

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
class SettingsViewModelTest {
    private lateinit var koin: Koin
    private lateinit var vm: SettingsViewModel

    @BeforeTest fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        koin = startTestKoin().koin
        vm = SettingsViewModel(koin.get(), koin.get())
    }
    @AfterTest fun tearDown() { stopKoin(); Dispatchers.resetMain() }

    @Test
    fun parsePointsAcceptsSignedForms() {
        assertEquals(20, parsePoints("20"))
        assertEquals(20, parsePoints(" +20 "))
        assertEquals(-10, parsePoints("-10"))
        assertEquals(-10, parsePoints("−10"))
        assertNull(parsePoints("0"))
        assertNull(parsePoints(""))
        assertNull(parsePoints("abc"))
    }

    @Test
    fun addPenaltyActionAndRejectInvalid() = runTest {
        assertFalse(vm.addAction("", "10", "🙂"))
        assertFalse(vm.addAction("Shouting", "0", "😠"))
        assertTrue(vm.addAction("Shouting", "−20", "😠"))
        val last = vm.state.first { it.actions.size == 6 }.actions.last()
        assertEquals("Shouting", last.label)
        assertEquals(-20, last.points)
    }

    @Test
    fun quickAmountAddAndUpdate() = runTest {
        assertTrue(vm.addQuickAmount("+50"))
        val added = vm.state.first { it.quickAmounts.size == 5 }.quickAmounts.last()
        assertTrue(vm.updateQuickAmount(added, "-5"))
        assertEquals(-5, vm.state.first { s -> s.quickAmounts.last().points == -5 }.quickAmounts.last().points)
    }

    @Test
    fun archiveAndLanguage() = runTest {
        assertTrue(vm.addChild("Léa", "🦊", 0xFFE57373))
        val lea = vm.state.first { it.children.isNotEmpty() }.children.single()
        vm.setArchived(lea.id, true)
        assertTrue(vm.state.first { it.children.single().archived }.children.single().archived)
        vm.setLanguage("fr")
        assertEquals("fr", vm.state.first { it.language == "fr" }.language)
        vm.setLanguage(null)
        assertNull(vm.state.first { it.language == null }.language)
    }
}
