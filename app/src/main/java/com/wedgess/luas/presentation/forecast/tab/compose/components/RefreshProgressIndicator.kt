package com.wedgess.luas.presentation.forecast.tab.compose.components

import android.annotation.SuppressLint
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

@SuppressLint("ComposeModifierMissing")
@Composable
fun RefreshProgressIndicator(refreshProgress: Float) {
    val animatedRefreshProgress by animateFloatAsState(
        targetValue = refreshProgress,
        animationSpec = tween(durationMillis = 400),
        label = "progress"
    )
    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        progress = { animatedRefreshProgress },
        trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
        drawStopIndicator = {},
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp
    )
}
