package com.gekkotron.kidquest.ui.celebrate

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.gekkotron.kidquest.App
import com.gekkotron.kidquest.data.ChildRepository
import com.gekkotron.kidquest.data.GoalRepository
import com.gekkotron.kidquest.data.PointRepository
import com.gekkotron.kidquest.startTestKoin
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CelebrationTest {
    private lateinit var koin: Koin

    @BeforeTest fun setUp() { koin = startTestKoin().koin }
    @AfterTest fun tearDown() { stopKoin() }

    @Test
    fun goalReachedShowsOverlayUntilTapped() = runComposeUiTest {
        val lea = runBlocking { koin.get<ChildRepository>().add("Léa", "🦊", 0xFFE57373) }
        runBlocking { koin.get<GoalRepository>().create("Lego", "🧱", 50, setOf(lea)) }
        setContent { App() }
        waitForIdle()

        runBlocking { koin.get<PointRepository>().apply(lea, 50, "+50") }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("celebration-goal").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Goal reached: Lego!").assertExists()

        onNodeWithTag("celebration-goal").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("celebration-goal").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun levelUpShowsBanner() = runComposeUiTest {
        val lea = runBlocking { koin.get<ChildRepository>().add("Léa", "🦊", 0xFFE57373) }
        setContent { App() }
        waitForIdle()
        runBlocking { koin.get<PointRepository>().apply(lea, 100, "+100") }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("celebration-level").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Léa reached level 2!", substring = true).assertExists()
    }
}
