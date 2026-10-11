package com.gekkotron.kidquest.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class ColorPickerTest {
    private val presets = listOf(0xFFFF1744L, 0xFF00E676L, 0xFF2979FFL)

    @Test
    fun showsEveryPresetAndReportsSelection() = runComposeUiTest {
        val picked = mutableListOf<Long>()
        setContent { MaterialTheme { ColorPicker(presets, presets[0], { picked += it }) } }
        presets.forEach { onNodeWithTag("color-${colorHexTag(it)}").assertIsDisplayed() }
        onNodeWithTag("color-${colorHexTag(presets[1])}").performClick()
        assertEquals(listOf(presets[1]), picked)
    }

    @Test
    fun customColorFlow() = runComposeUiTest {
        val picked = mutableListOf<Long>()
        setContent { MaterialTheme { ColorPicker(presets, presets[0], { picked += it }, testTagPrefix = "t") } }
        onNodeWithTag("t-add").performClick()
        onNodeWithTag("t-hue").assertIsDisplayed()
        onNodeWithTag("t-custom-ok").performClick()
        assertEquals(1, picked.size)
        assertEquals(0xFFL, (picked[0] shr 24) and 0xFFL)
        assertFalse(picked[0] in presets)
    }

    @Test
    fun selectedValueOutsidePresetsIsRendered() = runComposeUiTest {
        val custom = 0xFF123456L
        setContent { MaterialTheme { ColorPicker(presets, custom, {}) } }
        onNodeWithTag("color-${colorHexTag(custom)}").assertIsDisplayed()
    }
}
