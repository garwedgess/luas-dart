package com.wedgess.luas.presentation.timetable.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.ScheduledDeparture
import com.wedgess.luas.domain.timetable.TimetableStation
import com.wedgess.luas.domain.timetable.TimetableZone
import com.wedgess.luas.presentation.components.EmptyContent
import com.wedgess.luas.presentation.components.LoadingContent
import com.wedgess.luas.presentation.components.TransitStationCard
import com.wedgess.luas.presentation.components.sectionedlist.SectionedList
import com.wedgess.luas.presentation.components.sectionedlist.model.Section
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionItem
import com.wedgess.luas.presentation.components.sectionedlist.model.SectionedListState
import com.wedgess.luas.presentation.extensions.toClockTime
import com.wedgess.luas.presentation.timetable.TimetableUiState
import com.wedgess.luas.ui.theme.LuasGreenLine
import com.wedgess.luas.ui.theme.LuasRedLine
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TimetableScreen(
    state: TimetableUiState,
    onUpdate: () -> Unit,
    onStationSelect: (TimetableStation) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    showInfoAction: Boolean = true,
    onToggleFavourite: (String, Boolean) -> Unit = { _, _ -> }
) {
    var stationPicker by rememberSaveable(state.transport) { mutableStateOf(false) }
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var info by rememberSaveable { mutableStateOf(false) }
    var wholeDay by rememberSaveable(state.date, state.station?.id) { mutableStateOf(false) }
    var collapsed by rememberSaveable(state.transport, state.station?.id) { mutableStateOf(emptyList<String>()) }
    val today = state.now.atZone(TimetableZone).toLocalDate()
    val fromNow = state.date == today && !wholeDay
    var selectedLine by rememberSaveable(state.transport, state.station?.id) {
        mutableStateOf(initialTimetableLine(state))
    }
    val lineStations = state.stations.filter { it.lines.isEmpty() || selectedLine in it.lines }
    val groups = timetableSections(state, fromNow, selectedLine)
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            state.loading -> LoadingContent(stringResource(R.string.timetable_loading))
            state.feed == null -> TimetableDownload(state, onUpdate)
            else -> {
                if (state.transport == TransportType.LUAS) {
                    TimetableLineTabs(selectedLine) { line ->
                        selectedLine = line
                        state.stations.firstOrNull { line in it.lines }?.let(onStationSelect)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TransitStationCard(
                        station = state.station?.name.orEmpty(),
                        transport = state.transport,
                        description = stringResource(R.string.timetable_station, state.station?.name.orEmpty()),
                        onClick = { stationPicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    if (showInfoAction) {
                        IconButton(onClick = { info = true }) {
                            Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.timetable_info))
                        }
                    }
                }
                TimetableDateControls(
                    date = state.date,
                    onDateSelect = onDateSelect,
                    onOpenPicker = { datePicker = true },
                    showWholeDay = state.date == today,
                    wholeDay = wholeDay,
                    onWholeDay = { wholeDay = !wholeDay }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp))
                    Text(stringResource(R.string.timetable_short_notice), style = MaterialTheme.typography.labelMedium)
                }
                TimetableWarnings(state)
                if (!state.day.published || groups.isEmpty()) {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyContent(
                            stringResource(
                                if (!state.day.published) {
                                    R.string.timetable_not_published
                                } else {
                                    R.string.timetable_no_departures
                                }
                            )
                        )
                    }
                } else {
                    SectionedList(
                        modifier = Modifier.weight(1f),
                        state = SectionedListState(
                            groups,
                            groups.associate { it.id to (it.title !in collapsed) }.toPersistentMap()
                        ),
                        onSectionToggle = { id -> collapsed = toggleTimetableDirection(groups, id, collapsed) }
                    ) { departure -> ScheduledDepartureRow(departure, state.date) }
                }
            }
        }
    }
    if (info) TimetableInfoSheet(state, onUpdate, { info = false })
    if (stationPicker) {
        TimetableStationPicker(
            lineStations,
            {
                stationPicker = false
                onStationSelect(it)
            },
            { stationPicker = false },
            preferences = state.stationPreferences,
            transport = state.transport,
            onToggleFavourite = onToggleFavourite
        )
    }
    if (datePicker) {
        TimetableDatePicker(state.date, {
            datePicker = false
            onDateSelect(it)
        }, { datePicker = false })
    }
}

@Suppress("LongParameterList") // One callback for each date/filter control.
@Composable
private fun TimetableDateControls(
    date: LocalDate,
    onDateSelect: (LocalDate) -> Unit,
    onOpenPicker: () -> Unit,
    showWholeDay: Boolean,
    wholeDay: Boolean,
    onWholeDay: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedIconButton(onClick = { onDateSelect(date.minusDays(1)) }) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.timetable_previous)
            )
        }
        OutlinedButton(
            onClick = onOpenPicker,
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
        ) {
            Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")),
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall
            )
        }
        OutlinedIconButton(onClick = { onDateSelect(date.plusDays(1)) }) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.timetable_next)
            )
        }
        if (showWholeDay) {
            FilterChip(wholeDay, onWholeDay, label = { Text(stringResource(R.string.timetable_whole_day)) })
        }
    }
}

@Composable
private fun timetableSections(
    state: TimetableUiState,
    fromNow: Boolean,
    selectedLine: String
): ImmutableList<Section<ScheduledDeparture>> {
    val order = listOf("Northbound", "Southbound", "Outbound", "Inbound")
    return state.day.departures
        .filter { state.transport == TransportType.DART || it.line.equals(selectedLine, true) }
        .groupBy { it.direction }.entries
        .sortedBy { order.indexOf(it.key).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
        .mapIndexed { index, (direction, services) ->
            val shown = services.filter { !fromNow || it.scheduledAt >= state.now }
            Section(
                index.toLong(),
                direction,
                pluralStringResource(R.plurals.timetable_departure_count, shown.size, shown.size),
                shown.map { departure ->
                    val key = "${departure.serviceDate}-${departure.tripId}-" +
                        "${departure.stopSequence}-${departure.scheduledAt}"
                    SectionItem(key, departure)
                }.toImmutableList()
            )
        }.toImmutableList()
}

@Composable
private fun TimetableLineTabs(selectedLine: String, onSelect: (String) -> Unit) {
    val lines = listOf("Green", "Red")
    val selected = lines.indexOfFirst { it.equals(selectedLine, true) }.coerceAtLeast(0)
    val color = if (selected == 0) LuasGreenLine else LuasRedLine
    TabRow(
        selectedTabIndex = selected,
        indicator = { positions ->
            TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(positions[selected]), color = color)
        }
    ) {
        lines.forEachIndexed { index, line ->
            Tab(selected = selected == index, onClick = { onSelect(line) }, text = { Text(line) })
        }
    }
}

@Composable
private fun ScheduledDepartureRow(departure: ScheduledDeparture, date: LocalDate) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                departure.scheduledAt.toClockTime(),
                modifier = Modifier.weight(TIME_COLUMN_WEIGHT),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                departure.destination,
                modifier = Modifier.weight(DESTINATION_COLUMN_WEIGHT),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (departure.serviceDate != date) {
            Text(
                stringResource(R.string.timetable_service_day, departure.serviceDate.formatDate()),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
internal fun TimetableWarnings(state: TimetableUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state.feed != null && state.now.atZone(TimetableZone).toLocalDate() > state.feed.validUntil) {
            Text(
                stringResource(R.string.timetable_expired),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (state.updateFailed || state.loadFailed) {
            Text(
                stringResource(R.string.timetable_update_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (state.updating) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

internal fun LocalDate.formatDate(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

private const val TIME_COLUMN_WEIGHT = 0.3f
private const val DESTINATION_COLUMN_WEIGHT = 0.7f

private fun initialTimetableLine(state: TimetableUiState): String =
    state.station?.lines?.firstOrNull() ?: state.day.departures.firstOrNull()?.line ?: "Green"

private fun toggleTimetableDirection(
    groups: List<Section<ScheduledDeparture>>,
    id: Long,
    collapsed: List<String>
): List<String> = groups.firstOrNull { it.id == id }?.title?.let { title ->
    if (title in collapsed) collapsed - title else collapsed + title
} ?: collapsed
