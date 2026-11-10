package com.wedgess.luas.presentation.map.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.Departures
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.ErrorContent
import com.wedgess.luas.presentation.components.FreshnessBadge
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.components.StaleDataBanner
import com.wedgess.luas.presentation.extensions.toClockTime
import com.wedgess.luas.presentation.map.model.MapStationState
import com.wedgess.luas.presentation.model.Compose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapStationSheet(
    state: MapStationState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = state.station.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            state.result.Compose(
                onLoading = { LoadingContent(stringResource(R.string.map_loading_departures)) },
                onEmpty = { EmptyContent(stringResource(R.string.map_no_departures)) },
                onError = { ErrorContent(stringResource(R.string.map_departures_error), onRetry = onRetry) },
                onOffline = { ErrorContent(stringResource(R.string.offline_title), onRetry = onRetry) },
                onSuccess = { departures -> MapDepartures(departures, state.isStale) }
            )
        }
    }
}

@Composable
private fun MapDepartures(departures: Departures, stale: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (stale) StaleDataBanner(departures.fetchedAt) else FreshnessBadge(departures.fetchedAt)
        if (departures.items.isEmpty()) {
            Text(stringResource(R.string.map_no_departures), modifier = Modifier.padding(vertical = 16.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (departures.message.isNotBlank()) {
                item { Text(departures.message, style = MaterialTheme.typography.bodyMedium) }
            }
            departures.items.groupBy { it.direction }.forEach { (direction, items) ->
                item {
                    Text(
                        direction.key,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                itemsIndexed(items) { index, departure ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(departure.destination, style = MaterialTheme.typography.titleMedium)
                            departure.scheduledAt?.let {
                                Text(
                                    stringResource(R.string.map_scheduled_at, it.toClockTime()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = departure.expectedAt?.let {
                                stringResource(
                                    if (stale) R.string.label_expected_at else R.string.map_expected_at,
                                    it.toClockTime()
                                )
                            } ?: departure.status.orEmpty(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (index < items.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}
