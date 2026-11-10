package com.wedgess.luas.presentation.timetable.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.domain.timetable.TimetableStation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableStationPicker(
    stations: List<TimetableStation>,
    onSelect: (TimetableStation) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text(stringResource(R.string.station_search)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        val matches = stations.filter { it.name.contains(query.trim(), ignoreCase = true) }
        LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
            if (matches.isEmpty()) item { Text(stringResource(R.string.map_no_stations), Modifier.padding(16.dp)) }
            items(matches, key = { it.id }) { station ->
                TextButton(onClick = { onSelect(station) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(station.name)
                }
            }
        }
    }
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
