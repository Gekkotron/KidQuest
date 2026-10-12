package com.gekkotron.kidquest.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class BackupRepositoryTest {
    private val src = Fixture()
    private val dst = Fixture()

    @AfterTest fun tearDown() { src.db.close(); dst.db.close() }

    @Test
    fun roundTripsChildrenGoalsEventsAndSettings() = runTest {
        val lea = src.child(name = "Léa", createdAt = 100)
        val tim = src.child(name = "Tim", createdAt = 200)
        src.points.apply(lea, 20, "+20", actionId = null)
        src.points.apply(tim, -5, "-5", actionId = null)
        val goal = src.rawGoal(cost = 100, lea, tim, title = "Bowling")
        src.settings.setLanguage("fr")
        src.settings.setThemeMode(ThemeMode.DARK)

        val json = BackupRepository(src.db).export()

        BackupRepository(dst.db).import(json)
        val kids = dst.db.childDao().observeAll().first()
        assertEquals(listOf("Léa", "Tim"), kids.map { it.name })
        assertEquals(20, dst.db.pointEventDao().balance(lea))
        assertEquals(-5, dst.db.pointEventDao().balance(tim))
        assertEquals(setOf(lea, tim), dst.db.goalDao().memberIds(goal).toSet())
        assertEquals("Bowling", dst.db.goalDao().get(goal)?.title)
        assertEquals("fr", dst.settings.observeLanguage().first())
        assertEquals(ThemeMode.DARK, dst.settings.observeThemeMode().first())
    }

    @Test
    fun importReplacesExistingData() = runTest {
        src.child(name = "Léa", createdAt = 100)
        val json = BackupRepository(src.db).export()

        dst.child(name = "Keep me", createdAt = 50)
        BackupRepository(dst.db).import(json)
        val names = dst.db.childDao().observeAll().first().map { it.name }
        assertEquals(listOf("Léa"), names)
    }

    @Test
    fun importRejectsFutureSchemaVersion() = runTest {
        val repo = BackupRepository(dst.db)
        val payload = """{"schemaVersion":99,"exportedAt":0,"data":{"children":[],"goals":[],"goalMembers":[],"pointEvents":[],"pointActions":[],"quickAmounts":[],"appSettings":[]}}"""
        val error = assertFailsWith<IllegalArgumentException> { repo.import(payload) }
        assertTrue(error.message.orEmpty().contains("schema"))
    }
}
