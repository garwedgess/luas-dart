package com.wedgess.luas.presentation.forecast.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.presentation.extensions.toClockTime
import com.wedgess.luas.ui.theme.TransitLiveGreen
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable
fun ForecastLiveStatus(fetchedAt: Instant, intervalMillis: Long, modifier: Modifier = Modifier) {
    var remaining by remember(fetchedAt) { mutableLongStateOf(0L) }
    LaunchedEffect(fetchedAt, intervalMillis) {
        do {
            remaining = (intervalMillis - Duration.between(fetchedAt, Instant.now()).toMillis())
                .coerceIn(0L, intervalMillis)
            if (remaining > 0) delay(MILLIS_PER_SECOND)
        } while (remaining > 0)
    }
    val description = stringResource(R.string.freshness_live_description, fetchedAt.toClockTime())
    Row(
        modifier = modifier.semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(Modifier.size(8.dp), shape = CircleShape, color = TransitLiveGreen) {}
        Text(
            stringResource(
                R.string.forecast_refresh_countdown,
                (remaining + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Stored forecasts deliberately leave the live indicator empty. */
@Composable
fun ForecastStationStatus(fetchedAt: Instant, stale: Boolean, intervalMillis: Long, modifier: Modifier = Modifier) {
    if (!stale) ForecastLiveStatus(fetchedAt, intervalMillis, modifier)
}

private const val MILLIS_PER_SECOND = 1_000L
