package com.wedgess.luas.presentation.forecast.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * Time-driven indicator that fills over [intervalMillis] and restarts whenever [cycleKey] changes (a new fetch).
 * The animation lives entirely in Compose, so it needs no progress state from the ViewModel.
 */
@Composable
fun RefreshProgressIndicator(
    cycleKey: Any,
    intervalMillis: Long,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(cycleKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = intervalMillis.toInt(), easing = LinearEasing))
    }
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth(),
        progress = { progress.value },
        trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
        drawStopIndicator = {},
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp
    )
}
