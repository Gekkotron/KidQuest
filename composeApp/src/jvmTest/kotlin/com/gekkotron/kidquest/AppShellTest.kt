package com.gekkotron.kidquest

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppShellTest {
    @BeforeTest fun setUp() { startTestKoin() }
    @AfterTest fun tearDown() { stopKoin() }

    @Test
    fun bottomTabsNavigate() = runComposeUiTest {
        // Navigation lifecycle updates must run on the UI thread, so tab clicks go through runOnUiThread.
        setContent { App() }
        onNodeWithTag("screen-home").assertExists()
        runOnUiThread { onNodeWithTag("tab-goals").performClick() }
        onNodeWithTag("screen-goals").assertExists()
        runOnUiThread { onNodeWithTag("tab-settings").performClick() }
        onNodeWithTag("screen-settings").assertExists()
        runOnUiThread { onNodeWithTag("tab-home").performClick() }
        onNodeWithTag("screen-home").assertExists()
    }
}
