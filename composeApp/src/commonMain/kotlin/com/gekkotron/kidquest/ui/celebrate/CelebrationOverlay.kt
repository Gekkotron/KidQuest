package com.gekkotron.kidquest.ui.celebrate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gekkotron.kidquest.data.AppEvent
import com.gekkotron.kidquest.data.AppEventBus
import com.gekkotron.kidquest.resources.Res
import com.gekkotron.kidquest.resources.celebrate_goal
import com.gekkotron.kidquest.resources.celebrate_level
import com.gekkotron.kidquest.resources.celebrate_tap
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun CelebrationHost(bus: AppEventBus = koinInject()) {
    val queue = remember { mutableStateListOf<AppEvent>() }
    LaunchedEffect(bus) { bus.events.collect { queue.add(it) } }

    when (val current = queue.firstOrNull()) {
        is AppEvent.GoalAchieved -> GoalOverlay(current) { queue.removeAt(0) }
        is AppEvent.LevelUp -> LevelBanner(current) { if (queue.firstOrNull() === current) queue.removeAt(0) }
        null -> Unit
    }
}

@Composable
private fun GoalOverlay(event: AppEvent.GoalAchieved, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onDismiss).testTag("celebration-goal"),
        contentAlignment = Alignment.Center,
    ) {
        Confetti(Modifier.fillMaxSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎉 ${event.icon}", style = MaterialTheme.typography.displayLarge)
            Text(
                stringResource(Res.string.celebrate_goal, event.title),
                style = MaterialTheme.typography.headlineMedium, color = Color.White,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp),
            )
            Text(stringResource(Res.string.celebrate_tap), color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun LevelBanner(event: AppEvent.LevelUp, onDismiss: () -> Unit) {
    LaunchedEffect(event) {
        delay(3_000)
        onDismiss()
    }
    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss).testTag("celebration-level"),
        ) {
            Text(
                "⭐ " + stringResource(Res.string.celebrate_level, event.childName, event.level),
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp),
            )
        }
    }
}

private data class Particle(val x: Float, val startY: Float, val speed: Float, val phase: Float, val size: Float, val color: Color)

private val confettiColors = listOf(Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF69F0AE), Color(0xFF40C4FF), Color(0xFFE040FB))

/** One finite 3 s pass of falling confetti (finite so UI tests can reach idle). */
@Composable
private fun Confetti(modifier: Modifier) {
    val particles = remember {
        List(90) {
            Particle(
                x = Random.nextFloat(), startY = -Random.nextFloat() * 0.5f, speed = 0.6f + Random.nextFloat() * 0.8f,
                phase = Random.nextFloat() * 6.28f, size = 8f + Random.nextFloat() * 10f, color = confettiColors.random(),
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(durationMillis = 3_000, easing = LinearEasing)) }
    Canvas(modifier) {
        particles.forEach { p ->
            val y = (p.startY + t.value * p.speed * 1.5f) * size.height
            val x = (p.x + sin(t.value * 12f + p.phase) * 0.03f) * size.width
            drawRect(p.color, topLeft = Offset(x, y), size = Size(p.size, p.size * 0.6f))
        }
    }
}
