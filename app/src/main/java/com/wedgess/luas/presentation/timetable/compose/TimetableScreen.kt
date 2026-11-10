package com.wedgess.luas.presentation.timetable.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.ScheduledDeparture
import com.wedgess.luas.domain.timetable.TimetableStation
import com.wedgess.luas.domain.timetable.TimetableZone
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.extensions.toClockTime
import com.wedgess.luas.presentation.timetable.TimetableUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Suppress("LongParameterList") // Stateless screen callbacks.
@Composable
fun TimetableScreen(
    state: TimetableUiState,
    onUpdate: () -> Unit,
    onStationSelect: (TimetableStation) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var stationPicker by rememberSaveable(state.transport) { mutableStateOf(false) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var wholeDay by rememberSaveable(state.date, state.station?.id) { mutableStateOf(false) }
    val today = state.now.atZone(TimetableZone).toLocalDate()
    val fromNow = state.date == today && !wholeDay
    val departures = state.day.departures.filter { !fromNow || it.scheduledAt >= state.now }
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        downloadStatus(state, onUpdate)
        when {
            state.loading -> item { LoadingContent(stringResource(R.string.timetable_loading)) }
            state.feed == null -> item { Text(stringResource(R.string.timetable_download_hint)) }
            else -> {
                if (today > state.feed.validUntil) {
                    item {
                        Text(stringResource(R.string.timetable_expired), color = MaterialTheme.colorScheme.error)
                    }
                }
                item {
                    Text(
                        stringResource(
                            R.string.timetable_coverage,
                            state.feed.validFrom.formatDate(),
                            state.feed.validUntil.formatDate()
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    OutlinedButton(onClick = { stationPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.timetable_station, state.station?.name.orEmpty()))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onDateSelect(state.date.minusDays(1)) }) {
                            Text(stringResource(R.string.timetable_previous))
                        }
                        OutlinedButton(onClick = { datePicker = true }, modifier = Modifier.weight(1f)) {
                            Text(state.date.formatDate())
                        }
                        TextButton(onClick = { onDateSelect(state.date.plusDays(1)) }) {
                            Text(stringResource(R.string.timetable_next))
                        }
                    }
                }
                if (state.date == today) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !wholeDay,
                                onClick = { wholeDay = false },
                                label = { Text(stringResource(R.string.timetable_from_time, state.now.toClockTime())) }
                            )
                            FilterChip(
                                selected = wholeDay,
                                onClick = { wholeDay = true },
                                label = { Text(stringResource(R.string.timetable_whole_day)) }
                            )
                        }
                    }
                }
                if (!state.day.published) {
                    item {
                        Text(
                            stringResource(R.string.timetable_not_published),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else if (departures.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.timetable_no_departures),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                items(
                    departures,
                    key = { "${it.serviceDate}-${it.tripId}-${it.stopSequence}-${it.scheduledAt}" }
                ) { departure ->
                    ScheduledDepartureRow(departure, state.date)
                }
            }
        }
        item {
            TextButton(
                onClick = { uriHandler.openUri("https://www.transportforireland.ie/transitData/PT_Data.html") }
            ) {
                Text(stringResource(R.string.timetable_attribution), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (stationPicker) {
        TimetableStationPicker(
            stations = state.stations,
            onSelect = {
                stationPicker = false
                onStationSelect(it)
            },
            onDismiss = { stationPicker = false }
        )
    }
    if (datePicker) {
        TimetableDatePicker(
            date = state.date,
            onSelect = {
                datePicker = false
                onDateSelect(it)
            },
            onDismiss = { datePicker = false }
        )
    }
}

private fun LazyListScope.downloadStatus(state: TimetableUiState, onUpdate: () -> Unit) {
    item {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                stringResource(
                    if (state.transport == TransportType.DART) {
                        R.string.timetable_dart_notice
                    } else {
                        R.string.timetable_scheduled_notice
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
    if (state.updating) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.timetable_updating), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    if (state.updateFailed || state.loadFailed) {
        item {
            Text(stringResource(R.string.timetable_update_failed), color = MaterialTheme.colorScheme.error)
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onUpdate, enabled = !state.updating) {
                Text(
                    stringResource(
                        if (state.feed == null) R.string.timetable_download else R.string.timetable_update
                    )
                )
            }
            state.feed?.let { feed ->
                Text(
                    stringResource(
                        R.string.timetable_downloaded,
                        feed.downloadedAt.atZone(TimetableZone).toLocalDate().formatDate()
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ScheduledDepartureRow(departure: ScheduledDeparture, date: LocalDate) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.timetable_scheduled_time, departure.scheduledAt.toClockTime()),
                style = MaterialTheme.typography.titleMedium
            )
            Text(departure.destination, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        Text("${departure.line} · ${departure.direction}", style = MaterialTheme.typography.bodySmall)
        if (departure.serviceDate != date) {
            Text(
                stringResource(R.string.timetable_service_day, departure.serviceDate.formatDate()),
                style = MaterialTheme.typography.bodySmall
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

private fun LocalDate.formatDate(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
