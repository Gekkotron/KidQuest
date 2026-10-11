package com.gekkotron.kidquest.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class IconPickerTest {
    private val presets = listOf("🎁", "🎳", "🍔")

    @Test
    fun showsEveryPresetAndReportsSelection() = runComposeUiTest {
        val picked = mutableListOf<String>()
        setContent { MaterialTheme { IconPicker(presets, "🎁", { picked += it }) } }
        presets.forEach { onNodeWithText(it).assertIsDisplayed() }
        onNodeWithText("🎳").performClick()
        assertEquals(listOf("🎳"), picked)
    }

    @Test
    fun customEmojiFlow() = runComposeUiTest {
        val picked = mutableListOf<String>()
        setContent { MaterialTheme { IconPicker(presets, "🎁", { picked += it }, testTagPrefix = "t") } }
        onNodeWithTag("t-add").performClick()
        onNodeWithTag("t-custom").performTextInput("🦩")
        onNodeWithTag("t-custom-ok").performClick()
        assertEquals(listOf("🦩"), picked)
    }

    @Test
    fun selectedValueOutsidePresetsIsRendered() = runComposeUiTest {
        setContent { MaterialTheme { IconPicker(presets, "🦩", {}) } }
        onNodeWithText("🦩").assertIsDisplayed()
    }
}
