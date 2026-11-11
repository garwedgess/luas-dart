package com.wedgess.luas.presentation.forecast.compose.actions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.presentation.forecast.compose.ForecastRefreshCycle
import java.time.Clock
import java.time.Duration

@Composable
fun ForecastRefreshButton(
    cycle: ForecastRefreshCycle?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    clock: Clock = Clock.systemUTC()
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(cycle, clock) {
        if (cycle == null || cycle.intervalMillis <= 0) {
            progress.snapTo(0f)
        } else {
            val elapsed = Duration.between(cycle.fetchedAt, clock.instant()).toMillis()
            val remaining = (cycle.intervalMillis - elapsed).coerceIn(0L, cycle.intervalMillis)
            progress.snapTo(remaining.toFloat() / cycle.intervalMillis)
            progress.animateTo(0f, tween(remaining.toInt(), easing = LinearEasing))
        }
    }
    IconButton(onClick = onRefresh, modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                strokeWidth = 2.dp,
                gapSize = 0.dp
            )
            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.forecast_refresh))
        }
    }
}
