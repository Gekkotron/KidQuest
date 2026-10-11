package com.gekkotron.kidquest.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsRepositoryTest {
    private val f = Fixture()

    @AfterTest fun tearDown() = f.db.close()

    @Test
    fun deletingActionKeepsHistoryLabel() = runTest {
        val lea = f.child()
        val wipe = f.settings.observeActions().first().first()
        f.points.apply(lea, wipe.points, wipe.label, actionId = wipe.id)
        f.settings.deleteAction(wipe)
        assertEquals(4, f.settings.observeActions().first().size)
        assertEquals("Wipe the table", f.points.history(lea).first().single().label)
    }

    @Test
    fun moveActionSwapsWithNeighbour() = runTest {
        val before = f.settings.observeActions().first()
        f.settings.moveAction(before[1].id, up = true)
        val after = f.settings.observeActions().first()
        assertEquals(listOf(before[1].id, before[0].id), after.take(2).map { it.id })
        f.settings.moveAction(after.first().id, up = true) // already first: no-op
        assertEquals(after.map { it.id }, f.settings.observeActions().first().map { it.id })
    }

    @Test
    fun addedActionGoesLast() = runTest {
        f.settings.addAction("Feed the cat", 30, "🐱")
        assertEquals("Feed the cat", f.settings.observeActions().first().last().label)
    }

    @Test
    fun quickAmountsAddMoveDelete() = runTest {
        f.settings.addQuickAmount(5)
        var amounts = f.settings.observeQuickAmounts().first()
        assertEquals(listOf(10, 20, 100, -10, 5), amounts.map { it.points })
        f.settings.moveQuickAmount(amounts.last().id, up = true)
        amounts = f.settings.observeQuickAmounts().first()
        assertEquals(listOf(10, 20, 100, 5, -10), amounts.map { it.points })
        f.settings.deleteQuickAmount(amounts.first())
        assertEquals(listOf(20, 100, 5, -10), f.settings.observeQuickAmounts().first().map { it.points })
    }

    @Test
    fun languageRoundTrip() = runTest {
        assertNull(f.settings.observeLanguage().first())
        f.settings.setLanguage("fr")
        assertEquals("fr", f.settings.observeLanguage().first())
        f.settings.setLanguage(null)
        assertNull(f.settings.observeLanguage().first())
    }
}
