package com.gekkotron.kidquest.ui.goals

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.gekkotron.kidquest.data.Child
import com.gekkotron.kidquest.data.ChildRepository
import com.gekkotron.kidquest.startTestKoin
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GoalsUiTest {
    private lateinit var koin: Koin

    @BeforeTest fun setUp() { koin = startTestKoin().koin }
    @AfterTest fun tearDown() { stopKoin() }

    @Test
    fun redeemDialogShowsMinusCostPerMember() = runComposeUiTest {
        val kid = { id: Long, name: String -> Child(id = id, name = name, avatar = "🦊", color = 0xFFE57373, createdAt = id) }
        val state = RedeemState(1, "Cinema", 100, listOf(RedeemMember(kid(1, "Léa"), 150), RedeemMember(kid(2, "Tom"), 100)))
        var confirmed = false
        setContent { RedeemDialog(state, onConfirm = { confirmed = true }, onDismiss = {}) }
        onNodeWithTag("redeem-cost-1").assertTextEquals("−100")
        onNodeWithTag("redeem-cost-2").assertTextEquals("−100")
        onNodeWithTag("redeem-confirm").performClick()
        assertEquals(true, confirmed)
    }

    @Test
    fun costLabelSaysPointsPerChildWithTwoMembers() = runComposeUiTest {
        val kids = koin.get<ChildRepository>()
        val (lea, tom) = runBlocking { kids.add("Léa", "🦊", 0xFFE57373) to kids.add("Tom", "🐻", 0xFF64B5F6) }
        setContent { GoalEditScreen(0L, onDone = {}) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Léa").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("goal-member-$lea").performClick()
        assertEquals(0, onAllNodesWithText("Points per child").fetchSemanticsNodes().size)
        onNodeWithTag("goal-member-$tom").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Points per child").fetchSemanticsNodes().isNotEmpty() }
    }
}
