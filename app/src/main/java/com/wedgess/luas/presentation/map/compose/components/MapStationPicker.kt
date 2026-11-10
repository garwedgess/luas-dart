package com.wedgess.luas.presentation.map.compose.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.LocationEntity

/** Keyboard and TalkBack alternative to tapping the map's rendered station markers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapStationPicker(
    stations: List<LocationEntity>,
    onSelect: (LocationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { open = true },
        enabled = stations.isNotEmpty(),
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(stringResource(R.string.map_find_station))
    }
    if (open) {
        var query by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { open = false }) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.station_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            val matches = stations.filter { it.name.contains(query.trim(), ignoreCase = true) }.sortedBy { it.name }
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                if (matches.isEmpty()) item { Text(stringResource(R.string.map_no_stations), Modifier.padding(16.dp)) }
                items(matches, key = { it.stationCode }) { station ->
                    TextButton(
                        onClick = {
                            open = false
                            onSelect(station)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) { Text(station.name) }
                }
            }
        }
    }
}
