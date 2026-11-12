package com.wedgess.luas.presentation.timetable.compose

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.StationPreferences
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.domain.timetable.TimetableStation
import com.wedgess.luas.presentation.components.stationpicker.StationPickerSheet
import com.wedgess.luas.presentation.model.DropdownItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Suppress("LongParameterList") // Reuses the Forecast sheet while adapting timetable station IDs.
@Composable
fun TimetableStationPicker(
    stations: List<TimetableStation>,
    onSelect: (TimetableStation) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    preferences: StationPreferences = StationPreferences(),
    transport: TransportType = TransportType.LUAS,
    onToggleFavourite: (String, Boolean) -> Unit = { _, _ -> }
) {
    val options = remember(stations) { stations.map(::TimetableStationOption) }
    StationPickerSheet(
        options = options,
        favourites = preferences.favourites,
        recents = preferences.recents,
        onSelect = { onSelect((it as TimetableStationOption).station) },
        onToggleFavourite = onToggleFavourite,
        onDismiss = onDismiss,
        modifier = modifier,
        transport = transport
    )
}

private data class TimetableStationOption(val station: TimetableStation) : DropdownItem {
    override val text: String get() = station.name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableDatePicker(
    date: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let {
                    onSelect(
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    )
                }
            }) { Text(stringResource(R.string.timetable_choose_date)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.alarm_cancel)) } }
    ) { DatePicker(state = picker) }
}
