package com.gekkotron.kidquest.ui.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.gekkotron.kidquest.App
import com.gekkotron.kidquest.startTestKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class HomeScreenTest {
    @BeforeTest fun setUp() { startTestKoin() }
    @AfterTest fun tearDown() { stopKoin() }

    @Test
    fun addChildAwardThenUndo() = runComposeUiTest {
        setContent { App() }
        onNodeWithTag("home-add-first-child").performClick()
        onNodeWithTag("child-name").performTextInput("Léa")
        onNodeWithTag("child-save").performClick()

        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Léa").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("balance-1", useUnmergedTree = true).assertTextEquals("0")

        onNodeWithTag("quick-1-20").performClick()
        // Keep the snackbar from timing out while the DB write is awaited.
        mainClock.autoAdvance = false
        waitUntil(timeoutMillis = 5_000) { mainClock.advanceTimeBy(16); onAllNodesWithText("Undo").fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 5_000) { mainClock.advanceTimeBy(16); onAllNodesWithText("20", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("balance-1", useUnmergedTree = true).assertTextEquals("20")
        onNodeWithText("Undo").performClick()
        mainClock.autoAdvance = true
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("0").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("balance-1", useUnmergedTree = true).assertTextEquals("0")
    }
}
