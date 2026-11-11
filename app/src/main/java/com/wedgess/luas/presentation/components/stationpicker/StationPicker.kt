package com.wedgess.luas.presentation.components.stationpicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wedgess.luas.R
import com.wedgess.luas.domain.model.TransportType
import com.wedgess.luas.presentation.components.TransitStationCard
import com.wedgess.luas.presentation.model.DropdownItem
import com.wedgess.luas.presentation.model.DropdownTrailingContent

@Suppress("LongParameterList") // Stateless UI: one callback per user action.
/** Replaces the dropdown: a button showing the selection that opens a searchable sheet with favourites and recents. */
@OptIn(ExperimentalMaterial3Api::class) // ModalBottomSheet is still marked experimental in material3 1.3.
@Composable
fun StationPicker(
    label: String,
    selected: DropdownItem,
    options: List<DropdownItem>,
    favourites: Set<String>,
    recents: List<String>,
    onSelect: (DropdownItem) -> Unit,
    onToggleFavourite: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    transport: TransportType = TransportType.LUAS,
    supportingContent: (@Composable () -> Unit)? = null
) {
    var open by remember { mutableStateOf(false) }
    TransitStationCard(
        station = selected.text,
        description = "$label: ${selected.text}",
        onClick = { open = true },
        modifier = modifier.fillMaxWidth(),
        transport = transport,
        supportingContent = supportingContent
    )
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
            val groups = groupStations(options, query, favourites, recents)
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                section(R.string.station_favourites, groups.favourites, favourites, onSelect, onToggleFavourite) {
                    open = false
                }
                section(R.string.station_recents, groups.recents, favourites, onSelect, onToggleFavourite) {
                    open = false
                }
                section(R.string.station_all, groups.all, favourites, onSelect, onToggleFavourite) { open = false }
            }
        }
    }
}

@Suppress("LongParameterList") // Stateless UI: one callback per user action.
private fun androidx.compose.foundation.lazy.LazyListScope.section(
    titleRes: Int,
    stations: List<DropdownItem>,
    favourites: Set<String>,
    onSelect: (DropdownItem) -> Unit,
    onToggleFavourite: (String, Boolean) -> Unit,
    onPicked: () -> Unit
) {
    if (stations.isEmpty()) return
    item(key = "header-$titleRes") {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    items(stations, key = { "$titleRes-${it.text}" }) { station ->
        val isFavourite = station.text in favourites
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSelect(station)
                    onPicked()
                }
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(station.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            (station as? DropdownTrailingContent)?.RenderTrailingContent()
            val description = stringResource(
                if (isFavourite) R.string.station_remove_favourite else R.string.station_add_favourite,
                station.text
            )
            IconButton(
                onClick = { onToggleFavourite(station.text, !isFavourite) },
                modifier = Modifier.semantics { contentDescription = description }
            ) {
                Icon(if (isFavourite) Icons.Default.Star else Icons.Outlined.StarBorder, contentDescription = null)
            }
        }
    }
}
