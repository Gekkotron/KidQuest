package com.gekkotron.kidquest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF6750A4), secondary = Color(0xFFFF8A00), tertiary = Color(0xFF00A884),
    background = Color(0xFFFFFBF5), surface = Color(0xFFFFFBF5), error = Color(0xFFD32F2F),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFD0BCFF), secondary = Color(0xFFFFB86C), tertiary = Color(0xFF5FE0B8),
    error = Color(0xFFFF8A80),
)

@Composable
fun KidQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
