package com.wedgess.luas.presentation.forecast.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wedgess.luas.presentation.components.FreshnessBadge
import com.wedgess.luas.presentation.components.NextDepartureHeader
import com.wedgess.luas.presentation.components.StaleDataBanner
import java.time.Instant

@Composable
fun ForecastSummary(fetchedAt: Instant, stale: Boolean, next: NextDepartureSummary?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (stale) StaleDataBanner(fetchedAt)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!stale) FreshnessBadge(fetchedAt)
            next?.let {
                NextDepartureHeader(it.destination, it.dueMins, it.expectedTime, stale, Modifier.weight(1f))
            }
        }
    }
}
