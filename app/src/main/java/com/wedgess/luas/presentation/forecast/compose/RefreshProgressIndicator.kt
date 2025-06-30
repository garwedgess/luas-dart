package com.wedgess.luas.presentation.forecast.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun RefreshProgressIndicator(
    refreshProgress: Float,
    modifier: Modifier = Modifier
) {
    val animatedRefreshProgress by animateFloatAsState(
        targetValue = refreshProgress,
        animationSpec = tween(durationMillis = 200),
        label = "progress"
    )
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth(),
        progress = { animatedRefreshProgress },
        trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
        drawStopIndicator = {},
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp
    )
}
